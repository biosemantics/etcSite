package edu.arizona.biosemantics.etcsite.server.rpc.taxonomycomparison;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import edu.arizona.biosemantics.common.log.LogLevel;
import edu.arizona.biosemantics.etcsite.server.Configuration;
import edu.arizona.biosemantics.etcsite.server.Emailer;
import edu.arizona.biosemantics.etcsite.server.db.DAOManager;
import edu.arizona.biosemantics.etcsite.server.rpc.auth.AdminAuthenticationToken;
import edu.arizona.biosemantics.etcsite.server.rpc.file.FileService;
import edu.arizona.biosemantics.etcsite.server.rpc.file.permission.FilePermissionService;
import edu.arizona.biosemantics.etcsite.shared.model.AbstractTaskConfiguration;
import edu.arizona.biosemantics.etcsite.shared.model.ShortUser;
import edu.arizona.biosemantics.etcsite.shared.model.Task;
import edu.arizona.biosemantics.etcsite.shared.model.TaskStage;
import edu.arizona.biosemantics.etcsite.shared.model.TaskType;
import edu.arizona.biosemantics.etcsite.shared.model.TaxonomyComparisonConfiguration;
import edu.arizona.biosemantics.etcsite.shared.model.taxonomycomparison.TaskStageEnum;
import edu.arizona.biosemantics.etcsite.shared.rpc.auth.AuthenticationToken;
import edu.arizona.biosemantics.etcsite.shared.rpc.file.CopyFilesFailedException;
import edu.arizona.biosemantics.etcsite.shared.rpc.file.CreateDirectoryFailedException;
import edu.arizona.biosemantics.etcsite.shared.rpc.file.IFileService;
import edu.arizona.biosemantics.etcsite.shared.rpc.file.permission.IFilePermissionService;
import edu.arizona.biosemantics.etcsite.shared.rpc.file.permission.PermissionDeniedException;
import edu.arizona.biosemantics.etcsite.shared.rpc.taxonomycomparison.ITaxonomyComparisonService;
import edu.arizona.biosemantics.etcsite.shared.rpc.taxonomycomparison.TaxonomyComparisonException;
import edu.arizona.biosemantics.euler.alignment.shared.model.Model;
import edu.arizona.biosemantics.euler.alignment.shared.model.PossibleWorld;
import edu.arizona.biosemantics.euler.alignment.shared.model.RunOutput;
import edu.arizona.biosemantics.euler.alignment.shared.model.RunOutputType;
import edu.arizona.biosemantics.euler.alignment.shared.model.Taxonomies;
import edu.arizona.biosemantics.euler.alignment.shared.model.Taxonomy;
import edu.arizona.biosemantics.euler.io.EulerInputFileWriter;
import edu.arizona.biosemantics.euler.io.TaxonomyFileReader;

public class TaxonomyComparisonService extends RemoteServiceServlet implements ITaxonomyComparisonService {
	
	private String tempFiles = Configuration.tempFiles + File.separator + "taxonomyComparison";
	private IFileService fileService = new FileService();
	//private IFileFormatService fileFormatService = new FileFormatService();
	//private IFileAccessService fileAccessService = new FileAccessService();
	private IFilePermissionService filePermissionService = new FilePermissionService();
	private ListeningExecutorService executorService;
	private Map<Integer, ListenableFuture<Void>> activeProcessFutures = new HashMap<Integer, ListenableFuture<Void>>();
	private Map<Integer, MIRGeneration> activeProcess = new HashMap<Integer, MIRGeneration>();
	private DAOManager daoManager = new DAOManager();
	private Emailer emailer = new Emailer();
	
	public TaxonomyComparisonService() {
		executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Configuration.maxActiveTaxonomyComparison));
		try {
			fileService.createDirectory(new AdminAuthenticationToken(), Configuration.tempFiles, "taxonomyComparison", false);			
		} catch (PermissionDeniedException | CreateDirectoryFailedException e) {
			log(LogLevel.ERROR, "Couldn't create cache for taxonomy comparison", e);
		}
	}
	
	@Override
	protected void doUnexpectedFailure(Throwable t) {
		String message = "Unexpected failure";
		log(message, t);
	    log(LogLevel.ERROR, "Unexpected failure", t);
	    super.doUnexpectedFailure(t);
	}
	
	@Override
	public Task start(AuthenticationToken authenticationToken, String taskName, String input) throws TaxonomyComparisonException {	
		boolean isShared = filePermissionService.isSharedFilePath(authenticationToken.getUserId(), input);
		String fileName = null;
		try {
			fileName = fileService.getFileName(authenticationToken, input);
		} catch (PermissionDeniedException e) {
			log(LogLevel.ERROR, "Permission denied to read "+fileName);
			throw new TaxonomyComparisonException();
		}
		if(isShared) {
			String destination;
			try {
				destination = fileService.createDirectory(authenticationToken, Configuration.fileBase + File.separator + authenticationToken.getUserId(), 
						fileName, true);
			} catch (PermissionDeniedException | CreateDirectoryFailedException e) {
				throw new TaxonomyComparisonException();
			}
			try {
				fileService.copyFiles(authenticationToken, input, destination);
			} catch (CopyFilesFailedException | PermissionDeniedException e) {
				throw new TaxonomyComparisonException();
			}
			input = destination;
		}
		
		TaxonomyComparisonConfiguration config = new TaxonomyComparisonConfiguration();
		config.setInput(input);	
		config.setOutput(config.getInput() + "_output_by_TaxC_task_" + taskName);
		config = daoManager.getTaxonomyComparisonConfigurationDAO().addTaxonomyComparisonConfiguration(config);
		
		edu.arizona.biosemantics.etcsite.shared.model.TaskTypeEnum taskType = edu.arizona.biosemantics.etcsite.shared.model.TaskTypeEnum.TAXONOMY_COMPARISON;
		TaskType dbTaskType = daoManager.getTaskTypeDAO().getTaskType(taskType);
		TaskStage taskStage = daoManager.getTaskStageDAO().getTaxonomyComparisonTaskStage(TaskStageEnum.INPUT.toString());
		ShortUser user = daoManager.getUserDAO().getShortUser(authenticationToken.getUserId());
		Task task = new Task();
		task.setName(taskName);
		task.setResumable(true);
		task.setUser(user);
		task.setTaskStage(taskStage);
		task.setTaskConfiguration(config);
		task.setTaskType(dbTaskType);
		
		task = daoManager.getTaskDAO().addTask(task);
		taskStage = daoManager.getTaskStageDAO().getTaxonomyComparisonTaskStage(TaskStageEnum.ALIGN.toString());
		task.setTaskStage(taskStage);
		daoManager.getTaskDAO().updateTask(task);
		

		try {
			fileService.createDirectory(new AdminAuthenticationToken(), tempFiles, String.valueOf(task.getId()), false);
		} catch(PermissionDeniedException | CreateDirectoryFailedException e) {
			log(LogLevel.ERROR, "can not create cache directory.");
			throw new TaxonomyComparisonException(task);
		}
		createModelFromInput(authenticationToken, task);
			
		try {
			fileService.setInUse(authenticationToken, true, input, task);
		} catch (PermissionDeniedException e) {
			log(LogLevel.ERROR, "can not set file in use for "+input+ ".");
			throw new TaxonomyComparisonException(task);
		}
		return task;
	}
	
	private Model createModelFromInput(AuthenticationToken token, Task task) throws TaxonomyComparisonException {
		TaxonomyComparisonConfiguration config = this.getTaxonomyComparisonConfiguration(task);
		String input = config.getInput();
		File dir = new File(input);
		Taxonomies taxonomies = new Taxonomies();
		for(File file : dir.listFiles()) {
			TaxonomyFileReader reader = new TaxonomyFileReader(file.getAbsolutePath());
			Taxonomy taxonomy;
			try {
				taxonomy = reader.read();
			} catch (Exception e) {
				log(LogLevel.ERROR, "Couldn't read taxonomy in file " + file.getAbsolutePath(), e);
				throw new TaxonomyComparisonException();
			}
			taxonomies.add(taxonomy);
		}
		Model model = new Model(taxonomies);
		saveModel(token, task, model);
		return model;
	}
	
	@Override
	public Model getModel(AuthenticationToken token, Task task) throws TaxonomyComparisonException {
		try {
			Model model = unserializeModel(this.tempFiles + File.separator + task.getId() + File.separator + "Model.ser");
			return model;
		} catch (ClassNotFoundException | IOException e) {
			log(LogLevel.ERROR, "Couldn't get model", e);
			throw new TaxonomyComparisonException(task);
		}
		
	}
	
	@Override 
	public void saveModel(AuthenticationToken token, Task task, Model model) throws TaxonomyComparisonException {
		try {
			serializeModel(model, this.tempFiles + File.separator + task.getId() + File.separator + "Model.ser");
		} catch (IOException e) {
			log(LogLevel.ERROR, "Couldn't serialize model to disk", e);
			throw new TaxonomyComparisonException(task);
		}
	}
	
	private void serializeModel(Model model, String file) throws IOException {
		try(ObjectOutput output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
			output.writeObject(model);
		}
	}
	
	private Model unserializeModel(String file) throws FileNotFoundException, IOException, ClassNotFoundException {
		try(ObjectInput input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
			Model model = (Model)input.readObject();
			return model;
		}
	}
	

	@Override
	public Task runMirGeneration(final AuthenticationToken authenticationToken, final Task task, Model model) throws TaxonomyComparisonException {
		final TaxonomyComparisonConfiguration config = getTaxonomyComparisonConfiguration(task);
		
		//browser back button may invoke another "learn"
		if(activeProcessFutures.containsKey(config.getConfiguration().getId())) {
			return task;
		} else {
			final TaskType taskType = daoManager.getTaskTypeDAO().getTaskType(edu.arizona.biosemantics.etcsite.shared.model.TaskTypeEnum.TAXONOMY_COMPARISON);
			TaskStage taskStage = daoManager.getTaskStageDAO().getTaxonomyComparisonTaskStage(TaskStageEnum.ANALYZE.toString());
			task.setTaskStage(taskStage);
			task.setResumable(false);
			daoManager.getTaskDAO().updateTask(task);
			
			final String eulerInputFile = tempFiles + File.separator + task.getId() + File.separator + "input.txt";
			try {
				writeEulerInput(eulerInputFile, model);
			} catch(IOException e) {
				log(LogLevel.ERROR, "Couldn't write euler input to file " , e);
			}
			
			final String outputDir = tempFiles + File.separator + task.getId() + File.separator + "output";
			try {
				fileService.deleteFile(new AdminAuthenticationToken(), outputDir);
				fileService.createDirectory(new AdminAuthenticationToken(), tempFiles + File.separator + task.getId(), "output", false);
			} catch(Exception e) {
				log(LogLevel.ERROR, "Couldn't set up output directory", e);
				throw new TaxonomyComparisonException(task);
			}
				
			//final MIRGeneration mirGeneration = new InJvmMIRGeneration(eulerInputFile, outputDir);
			final MIRGeneration mirGeneration = new DummyMIRGeneration(eulerInputFile, outputDir);
			activeProcess.put(config.getConfiguration().getId(), mirGeneration);
			final ListenableFuture<Void> futureResult = executorService.submit(mirGeneration);
			this.activeProcessFutures.put(config.getConfiguration().getId(), futureResult);
			futureResult.addListener(new Runnable() {
			    	public void run() {	
			     		try {
			     			MIRGeneration mirGeneration = activeProcess.remove(config.getConfiguration().getId());
				    		ListenableFuture<Void> futureResult = activeProcessFutures.remove(config.getConfiguration().getId());
				     		if(mirGeneration.isExecutedSuccessfully()) {
				     			if(!futureResult.isCancelled()) {
									TaskStage newTaskStage = daoManager.getTaskStageDAO().getTaxonomyComparisonTaskStage(TaskStageEnum.ANALYZE_COMPLETE.toString());
									task.setTaskStage(newTaskStage);
									task.setResumable(true);
									daoManager.getTaskDAO().updateTask(task);
									
									// send an email to the user who owns the task.
									sendFinishedTaxonomyComparisonEmail(task);
				     			}
				     		} else {
				     			task.setFailed(true);
								task.setFailedTime(new Date());
								daoManager.getTaskDAO().updateTask(task);
				     		}
			     		} catch(Throwable t) {
			     			log(LogLevel.ERROR, t.getMessage()+"\n"+org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(t));
			     			task.setFailed(true);
							task.setFailedTime(new Date());
							daoManager.getTaskDAO().updateTask(task);
			     		}
			     	}

					
			     }, executorService);
			
			return task;
		}
	}
	
	private void writeEulerInput(String eulerInputFile, Model model) throws IOException {
		EulerInputFileWriter eulerInputFileWriter = new EulerInputFileWriter(eulerInputFile);
		eulerInputFileWriter.write(model);
	}

	private void sendFinishedTaxonomyComparisonEmail(Task task) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getInputVisualization(AuthenticationToken token, Task task, Model model) {
		return "url";
	}

	
	@Override
	public Task getLatestResumable(AuthenticationToken authenticationToken) {
		ShortUser user = daoManager.getUserDAO().getShortUser(authenticationToken.getUserId());
		List<Task> tasks = daoManager.getTaskDAO().getOwnedTasks(user.getId());
		for(Task task : tasks) {
			if(task != null && task.isResumable() && 
					task.getTaskType().getTaskTypeEnum().equals(edu.arizona.biosemantics.etcsite.shared.model.TaskTypeEnum.TAXONOMY_COMPARISON)) {
						return task;
			}
		}
		return null;
	}

	@Override
	public void cancel(AuthenticationToken authenticationToken, Task task) throws TaxonomyComparisonException {
		final TaxonomyComparisonConfiguration config = getTaxonomyComparisonConfiguration(task);
						
		//remove taxonomy comparison configuration
		if(config.getInput() != null)
			try {
				fileService.setInUse(authenticationToken, false, config.getInput(), task);
			} catch (PermissionDeniedException e) {
				throw new TaxonomyComparisonException(task);
			}
		daoManager.getTaxonomyComparisonConfigurationDAO().remove(config);
		
		//remove task
		daoManager.getTaskDAO().removeTask(task);
		if(task.getConfiguration() != null)
			//remove configuration
			daoManager.getConfigurationDAO().remove(task.getConfiguration().getConfiguration());
	
		//cancel possible futures
		if(task.getTaskStage() != null) {
			switch(TaskStageEnum.valueOf(task.getTaskStage().getTaskStage())) {
			case INPUT:
				break;
			case ALIGN:
				break;
			case ANALYZE:
				if(activeProcessFutures.containsKey(config.getConfiguration().getId())) {
					ListenableFuture<Void> processFuture = this.activeProcessFutures.get(config.getConfiguration().getId());
					processFuture.cancel(true);
				}
				if(activeProcess.containsKey(config.getConfiguration().getId())) {
					activeProcess.get(config.getConfiguration().getId()).destroy();
				}
				break; 
			case ANALYZE_COMPLETE:
				break;
			default:
				break;
			}
		}
	}

	private TaxonomyComparisonConfiguration getTaxonomyComparisonConfiguration(Task task) throws TaxonomyComparisonException {
		final AbstractTaskConfiguration configuration = task.getConfiguration();
		if(!(configuration instanceof TaxonomyComparisonConfiguration))
			throw new TaxonomyComparisonException(task);
		final TaxonomyComparisonConfiguration taxonomyComparisonConfiguration = (TaxonomyComparisonConfiguration)configuration;
		return taxonomyComparisonConfiguration;
	}

	@Override
	public boolean isValidInput(AuthenticationToken token, String inputFile) {
		return true;
	}

	@Override
	public RunOutput getMirGenerationResult(AuthenticationToken token, Task task) {
		//final AbstractTaskConfiguration configuration = task.getConfiguration();
		TaskStage newTaskStage = daoManager.getTaskStageDAO().getTaxonomyComparisonTaskStage(TaskStageEnum.ANALYZE.toString());
		task.setTaskStage(newTaskStage);
		task.setResumable(true);
		daoManager.getTaskDAO().updateTask(task);
		
		File outputDir = new File(tempFiles + File.separator + task.getId() + File.separator + "output" + File.separator + "6-PWs-pdf");
		List<PossibleWorld> possibleWorldFiles = new LinkedList<PossibleWorld>();
		for(File file : outputDir.listFiles()) {
			if(file.isFile()) {
				possibleWorldFiles.add(new PossibleWorld(file.getAbsolutePath(), file.getName()));
			}
		}	
		if(possibleWorldFiles.size() == 1)
			return new RunOutput(RunOutputType.ONE, possibleWorldFiles);
		if(possibleWorldFiles.size() > 1)
			return new RunOutput(RunOutputType.MULTIPLE, possibleWorldFiles);
		return new RunOutput(RunOutputType.CONFLICT);
	}
	


	@Override
	public Task goToTaskStage(AuthenticationToken token, Task task, TaskStageEnum taskStageEnum) {
		TaskType taskType = daoManager.getTaskTypeDAO().getTaskType(edu.arizona.biosemantics.etcsite.shared.model.TaskTypeEnum.TAXONOMY_COMPARISON);
		TaskStage taskStage = daoManager.getTaskStageDAO().getTaxonomyComparisonTaskStage(taskStageEnum.toString());
		task.setTaskStage(taskStage);
		task.setResumable(true);
		task.setComplete(false);
		task.setCompleted(null);
		daoManager.getTaskDAO().updateTask(task);
		return task;
	}
	
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		TaxonomyComparisonException exc = new TaxonomyComparisonException();
		try(ObjectOutput output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File("test"))))) {
			output.writeObject(exc);
		}
	}
}
