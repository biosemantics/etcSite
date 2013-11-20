package edu.arizona.sirls.etc.site.server.rpc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import edu.arizona.sirls.etc.site.server.Configuration;
import edu.arizona.sirls.etc.site.shared.rpc.AuthenticationResult;
import edu.arizona.sirls.etc.site.shared.rpc.AuthenticationToken;
import edu.arizona.sirls.etc.site.shared.rpc.IAuthenticationService;
import edu.arizona.sirls.etc.site.shared.rpc.IFileAccessService;
import edu.arizona.sirls.etc.site.shared.rpc.IFileService;
import edu.arizona.sirls.etc.site.shared.rpc.ISemanticMarkupService;
import edu.arizona.sirls.etc.site.shared.rpc.ITaskService;
import edu.arizona.sirls.etc.site.shared.rpc.SemanticMarkupTaskRun;
import edu.arizona.sirls.etc.site.shared.rpc.RPCResult;
import edu.arizona.sirls.etc.site.shared.rpc.TaskStageEnum;
import edu.arizona.sirls.etc.site.shared.rpc.db.ConfigurationDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.Glossary;
import edu.arizona.sirls.etc.site.shared.rpc.db.GlossaryDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.SemanticMarkupConfiguration;
import edu.arizona.sirls.etc.site.shared.rpc.db.SemanticMarkupConfigurationDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.Share;
import edu.arizona.sirls.etc.site.shared.rpc.db.ShareDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.ShortUser;
import edu.arizona.sirls.etc.site.shared.rpc.db.Task;
import edu.arizona.sirls.etc.site.shared.rpc.db.TaskDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.TaskStage;
import edu.arizona.sirls.etc.site.shared.rpc.db.TaskStageDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.TaskType;
import edu.arizona.sirls.etc.site.shared.rpc.db.TaskTypeDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.UserDAO;
import edu.arizona.sirls.etc.site.shared.rpc.file.XMLFileFormatter;
import edu.arizona.sirls.etc.site.shared.rpc.semanticMarkup.BracketValidator;
import edu.arizona.sirls.etc.site.shared.rpc.semanticMarkup.LearnInvocation;
import edu.arizona.sirls.etc.site.shared.rpc.semanticMarkup.ParseInvocation;
import edu.arizona.sirls.etc.site.shared.rpc.semanticMarkup.PreprocessedDescription;

public class SemanticMarkupService extends RemoteServiceServlet implements ISemanticMarkupService  {

	private static final long serialVersionUID = -7871896158610489838L;
	private IAuthenticationService authenticationService = new AuthenticationService();
	private IFileAccessService fileAccessService = new FileAccessService();
	private ITaskService taskService = new TaskService();
	private IFileService fileService = new FileService();
	private BracketValidator bracketValidator = new BracketValidator();
	private int maximumThreads = 10;
	private ListeningExecutorService executorService;
	private Map<Integer, ListenableFuture<LearnResult>> activeLearnFutures = new HashMap<Integer, ListenableFuture<LearnResult>>();
	private Map<Integer, ListenableFuture<ParseResult>> activeParseFutures = new HashMap<Integer, ListenableFuture<ParseResult>>();

	
	public SemanticMarkupService() {
		executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
	}
	
	@Override
	public RPCResult<SemanticMarkupTaskRun> start(AuthenticationToken authenticationToken, String taskName, String filePath, String glossaryName) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		
		if(!authResult.isSucceeded()) 
			return new RPCResult<SemanticMarkupTaskRun>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<SemanticMarkupTaskRun>(false, "Authentication failed");
		
		try {
			RPCResult<List<String>> directoriesFilesResult = fileService.getDirectoriesFiles(authenticationToken, filePath);
			if(!directoriesFilesResult.isSucceeded()) {
				return new RPCResult<SemanticMarkupTaskRun>(false, directoriesFilesResult.getMessage());
			}
			int numberOfInputFiles = directoriesFilesResult.getData().size();
			Glossary glossary = GlossaryDAO.getInstance().getGlossary(glossaryName);
			SemanticMarkupConfiguration semanticMarkupConfiguration = new SemanticMarkupConfiguration();
			semanticMarkupConfiguration.setInput(filePath);	
			semanticMarkupConfiguration.setGlossary(glossary);
			semanticMarkupConfiguration.setNumberOfInputFiles(numberOfInputFiles);
			semanticMarkupConfiguration = SemanticMarkupConfigurationDAO.getInstance().addSemanticMarkupConfiguration(semanticMarkupConfiguration);
			
			edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum taskType = edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.SEMANTIC_MARKUP;
			TaskType dbTaskType = TaskTypeDAO.getInstance().getTaskType(taskType);
			TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(dbTaskType, TaskStageEnum.INPUT);
			ShortUser user = UserDAO.getInstance().getShortUser(authenticationToken.getUsername());
			Task task = new Task();
			task.setName(taskName);
			task.setResumable(true);
			task.setUser(user);
			task.setTaskStage(taskStage);
			task.setConfiguration(semanticMarkupConfiguration.getConfiguration());
			task.setTaskType(dbTaskType);
			
			RPCResult<Task> addTaskResult = taskService.addTask(authenticationToken, task);
			if(!addTaskResult.isSucceeded())
				return new RPCResult<SemanticMarkupTaskRun>(false, addTaskResult.getMessage());
			task = addTaskResult.getData();
			
			taskStage = TaskStageDAO.getInstance().getTaskStage(dbTaskType, TaskStageEnum.PREPROCESS_TEXT);
			task.setTaskStage(taskStage);
			TaskDAO.getInstance().updateTask(task);

			fileService.setInUse(authenticationToken, true, filePath, task);
			return new RPCResult<SemanticMarkupTaskRun>(true, new SemanticMarkupTaskRun(semanticMarkupConfiguration, task));
		} catch (Exception e) {
			e.printStackTrace();
			return new RPCResult<SemanticMarkupTaskRun>(false, "Internal Server Error");
		}
	}
	
	@Override
	public RPCResult<List<PreprocessedDescription>> preprocess(AuthenticationToken authenticationToken, SemanticMarkupTaskRun semanticMarkupTaskRun) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<List<PreprocessedDescription>>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<List<PreprocessedDescription>>(false, "Authentication failed");
		
		try {
			List<PreprocessedDescription> result = new LinkedList<PreprocessedDescription>();
			Task task = semanticMarkupTaskRun.getTask();
			TaskType taskType = TaskTypeDAO.getInstance().getTaskType(edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.SEMANTIC_MARKUP);
			TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.PREPROCESS_TEXT);
			task.setTaskStage(taskStage);
			TaskDAO.getInstance().updateTask(task);
			//do preprocessing here, return result immediately or always only return an invocation
			//and make user come back when ready?
			String inputDirectory = semanticMarkupTaskRun.getConfiguration().getInput();
			
			RPCResult<Boolean> isDirectoryResult = fileService.isDirectory(authenticationToken, inputDirectory);
			if(isDirectoryResult.isSucceeded() && isDirectoryResult.getData()) {
				RPCResult<List<String>> resultDirectoryFiles = fileService.getDirectoriesFiles(authenticationToken, inputDirectory);
				if(resultDirectoryFiles.isSucceeded()) {
					for(String file : resultDirectoryFiles.getData()) {
						RPCResult<String> descriptionResult = getDescription(authenticationToken, inputDirectory + File.separator + file);
						if(descriptionResult.isSucceeded()) {
							String description = descriptionResult.getData();
							if(!bracketValidator.validate(description)) {
								PreprocessedDescription preprocessedDescription = new PreprocessedDescription(
										inputDirectory + File.separator + file,
										file, 0,
										bracketValidator.getBracketCountDifferences(description));
								result.add(preprocessedDescription);
							}	
						}
					}
				}
			}
			return new RPCResult<List<PreprocessedDescription>>(true, result);
		} catch(Exception e) {
			e.printStackTrace();
			return new RPCResult<List<PreprocessedDescription>>(false, "Internal Server Error");
		}
	}
	
	@Override
	public RPCResult<LearnInvocation> learn(AuthenticationToken authenticationToken, final SemanticMarkupTaskRun semanticMarkupTaskRun) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<LearnInvocation>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<LearnInvocation>(false, "Authentication failed");
		
		try {
			int numberOfSentences = getNumberOfSentences();
			int numberOfWords = getNumberOfWords();
			
			final SemanticMarkupConfiguration semanticMarkupConfiguration = semanticMarkupTaskRun.getConfiguration();
			//browser back button may invoke another "learn"
			if(activeLearnFutures.containsKey(semanticMarkupConfiguration.getConfiguration().getId())) {
				return new RPCResult<LearnInvocation>(true, new LearnInvocation(numberOfSentences, numberOfWords));
			} else {
				final Task task = TaskDAO.getInstance().getTask(semanticMarkupConfiguration.getConfiguration());
				final TaskType taskType = TaskTypeDAO.getInstance().getTaskType(edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.SEMANTIC_MARKUP);
				TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.LEARN_TERMS);
				task.setTaskStage(taskStage);
				task.setResumable(false);
				TaskDAO.getInstance().updateTask(task);
				
				String glossary = semanticMarkupConfiguration.getGlossary().getName();
				String input = semanticMarkupConfiguration.getInput();
				String tablePrefix = String.valueOf(task.getId());
				String debugFile = "workspace" + File.separator + task.getId() + File.separator + "debug.log";
				String errorFile = "workspace" + File.separator + task.getId() + File.separator + "error.log";
				String source = input; //maybe something else later
				String user = authenticationToken.getUsername();
				String bioportalUserId = UserDAO.getInstance().getUser(authenticationToken.getUsername()).getBioportalUserId();
				String bioportalAPIKey = UserDAO.getInstance().getUser(authenticationToken.getUsername()).getBioportalAPIKey();
				ILearn learn = new Learn(authenticationToken, glossary, input, tablePrefix, debugFile, errorFile, source, user, bioportalUserId, bioportalAPIKey);
				final ListenableFuture<LearnResult> futureResult = executorService.submit(learn);
				activeLearnFutures.put(semanticMarkupConfiguration.getConfiguration().getId(), futureResult);
				futureResult.addListener(new Runnable() {
				     	public void run() {
				     		try {
				     			activeLearnFutures.remove(semanticMarkupConfiguration.getConfiguration().getId());
				     			if(!futureResult.isCancelled()) {
				     				LearnResult result = futureResult.get();
					     			semanticMarkupConfiguration.setOtoUploadId(result.getOtoUploadId());
					     			semanticMarkupConfiguration.setOtoSecret(result.getOtoSecret());
									SemanticMarkupConfigurationDAO.getInstance().updateSemanticMarkupConfiguration(semanticMarkupConfiguration);
									TaskStage newTaskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.REVIEW_TERMS);
									task.setTaskStage(newTaskStage);
									task.setResumable(true);
									TaskDAO.getInstance().updateTask(task);
				     			}
							} catch (Exception e) {
								e.printStackTrace();
							}
				     	}
				     }, executorService);
				
				return new RPCResult<LearnInvocation>(true, new LearnInvocation(numberOfSentences, numberOfWords));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new RPCResult<LearnInvocation>(false, "Internal Server Error");
		}
	}

	private int getNumberOfSentences() {
		return 5423;
	}

	private int getNumberOfWords() {
		return 21259;
	}
	
	@Override
	public RPCResult<SemanticMarkupTaskRun> review(AuthenticationToken authenticationToken, SemanticMarkupTaskRun semanticMarkupTaskRun) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<SemanticMarkupTaskRun>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<SemanticMarkupTaskRun>(false, "Authentication failed");
		try {
			Task task = semanticMarkupTaskRun.getTask();
			TaskType taskType = TaskTypeDAO.getInstance().getTaskType(edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.SEMANTIC_MARKUP);
			TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.REVIEW_TERMS);
			task.setTaskStage(taskStage);
			TaskDAO.getInstance().updateTask(task);
			SemanticMarkupConfiguration configuration = 
					SemanticMarkupConfigurationDAO.getInstance().getSemanticMarkupConfiguration(
							semanticMarkupTaskRun.getConfiguration().getConfiguration().getId());
			
			return new RPCResult<SemanticMarkupTaskRun>(true, new SemanticMarkupTaskRun(configuration, task));
		} catch(Exception e) {
			e.printStackTrace();
			return new RPCResult<SemanticMarkupTaskRun>(false, "Internal Server Error");
		}
	}
	
	@Override
	public RPCResult<ParseInvocation> parse(final AuthenticationToken authenticationToken, final SemanticMarkupTaskRun semanticMarkupTaskRun) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<ParseInvocation>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<ParseInvocation>(false, "Authentication failed");
		
		try {
			final SemanticMarkupConfiguration semanticMarkupConfiguration = semanticMarkupTaskRun.getConfiguration();
			if(activeParseFutures.containsKey(semanticMarkupConfiguration.getConfiguration().getId())) {
				return new RPCResult<ParseInvocation>(true, new ParseInvocation());
			} else {
				final Task task = TaskDAO.getInstance().getTask(semanticMarkupConfiguration.getConfiguration());
				final TaskType taskType = TaskTypeDAO.getInstance().getTaskType(edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.SEMANTIC_MARKUP);
				TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.PARSE_TEXT);
				task.setTaskStage(taskStage);
				task.setResumable(false);
				TaskDAO.getInstance().updateTask(task);
				
				String glossary = semanticMarkupConfiguration.getGlossary().getName();
				String input = semanticMarkupConfiguration.getInput();
				String tablePrefix = String.valueOf(task.getId());
				String debugFile = "workspace" + File.separator + task.getId() + File.separator + "debug.log";
				String errorFile = "workspace" + File.separator + task.getId() + File.separator + "error.log";
				String source = input; //maybe something else later
				String user = authenticationToken.getUsername();
				String bioportalUserId = UserDAO.getInstance().getUser(authenticationToken.getUsername()).getBioportalUserId();
				String bioportalAPIKey = UserDAO.getInstance().getUser(authenticationToken.getUsername()).getBioportalAPIKey();
				IParse parse = new Parse(authenticationToken, glossary, input, tablePrefix, debugFile, errorFile, source, user, bioportalUserId, bioportalAPIKey);
				final ListenableFuture<ParseResult> futureResult = executorService.submit(parse);
				activeParseFutures.put(semanticMarkupConfiguration.getConfiguration().getId(), futureResult);
				futureResult.addListener(new Runnable() {
					@Override
					public void run() {
						try {
							activeParseFutures.remove(semanticMarkupConfiguration.getConfiguration().getId());
							if(!futureResult.isCancelled()) {
								task.setResumable(true);
								TaskStage newTaskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.OUTPUT);
								task.setTaskStage(newTaskStage);
								TaskDAO.getInstance().updateTask(task);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}, executorService);
				
				return new RPCResult<ParseInvocation>(true, new ParseInvocation());
			}
				
		} catch (Exception e) {
			e.printStackTrace();
			return new RPCResult<ParseInvocation>(false, "Internal Server Error");
		}
	}

	@Override
	public RPCResult<Void> output(AuthenticationToken authenticationToken, SemanticMarkupTaskRun semanticMarkupTaskRun) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<Void>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<Void>(false, "Authentication failed");
		
		try {
			Task task = semanticMarkupTaskRun.getTask();
			SemanticMarkupConfiguration semanticMarkupConfiguration = semanticMarkupTaskRun.getConfiguration();
			semanticMarkupConfiguration.setOutput(semanticMarkupConfiguration.getInput() + "_" + task.getName());
			SemanticMarkupConfigurationDAO.getInstance().updateSemanticMarkupConfiguration(semanticMarkupConfiguration);

			String outputDirectory = semanticMarkupConfiguration.getOutput();			
			RPCResult<String> outputDirectoryParentResult = fileService.getParent(authenticationToken, outputDirectory);
			RPCResult<String> outputDirectoryNameResult = fileService.getFileName(authenticationToken, outputDirectory);
			if(!outputDirectoryParentResult.isSucceeded() || !outputDirectoryNameResult.isSucceeded())
				return new RPCResult<Void>(false, outputDirectoryParentResult.getMessage());
			
			//find a suitable destination filePath
			RPCResult<String> createDirectoryResult = fileService.createDirectoryForcibly(authenticationToken, outputDirectoryParentResult.getData(), outputDirectoryNameResult.getData());
			if(!createDirectoryResult.isSucceeded()) 
				return new RPCResult<Void>(false, createDirectoryResult.getMessage());
			
			//copy the output files to the directory
			String charaParserOutputDirectory = "workspace" + File.separator + task.getId() + File.separator + "out";			
			RPCResult<Void> copyResult = fileService.copyFiles(new AdminAuthenticationToken(), charaParserOutputDirectory, createDirectoryResult.getData());
			if(!copyResult.isSucceeded())
				return copyResult;
			
			//update task
			task.setResumable(false);
			task.setComplete(true);
			task.setCompleted(new Date());
			TaskDAO.getInstance().updateTask(task);
			
			return new RPCResult<Void>(true);
		} catch (Exception e) {
			e.printStackTrace();
			return new RPCResult<Void>(false, "Internal Server Error");
		}
	}
	
	@Override
	public RPCResult<SemanticMarkupTaskRun> goToTaskStage(AuthenticationToken authenticationToken, SemanticMarkupTaskRun semanticMarkupTaskRun, TaskStageEnum taskStageEnum) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<SemanticMarkupTaskRun>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<SemanticMarkupTaskRun>(false, "Authentication failed");
		
		try {
			Task task = semanticMarkupTaskRun.getTask();
			TaskType taskType = TaskTypeDAO.getInstance().getTaskType(edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.SEMANTIC_MARKUP);
			TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(taskType, taskStageEnum);
			task.setTaskStage(taskStage);
			task.setResumable(true);
			task.setComplete(false);
			task.setCompleted(null);
			TaskDAO.getInstance().updateTask(task);
			SemanticMarkupConfiguration configuration = 
					SemanticMarkupConfigurationDAO.getInstance().getSemanticMarkupConfiguration(
							semanticMarkupTaskRun.getConfiguration().getConfiguration().getId());
			return new RPCResult<SemanticMarkupTaskRun>(true, 
					new SemanticMarkupTaskRun(configuration, task));
		} catch(Exception e) {
			e.printStackTrace();
			return new RPCResult<SemanticMarkupTaskRun>(false, "Internal Server Error");
		}
	}	

	@Override
	public RPCResult<String> getDescription(AuthenticationToken authenticationToken, String filePath) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<String>(false, authResult.getMessage(), "");
		if(!authResult.getData().getResult())
			return new RPCResult<String>(false, "Authentication failed", "");
		
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			RPCResult<String> fileContentResult = fileAccessService.getFileContent(authenticationToken, filePath);
			if(fileContentResult.isSucceeded()) {
				String fileContent = fileContentResult.getData();
				Document document = db.parse(new InputSource(new ByteArrayInputStream(fileContent.getBytes("UTF-8"))));
				XPath xPath = XPathFactory.newInstance().newXPath();
				Node node = (Node)xPath.evaluate("/treatment/description",
				        document.getDocumentElement(), XPathConstants.NODE);
				return new RPCResult<String>(true, "", node.getTextContent());
			}
			return new RPCResult<String>(false, fileContentResult.getMessage(), "");
		} catch(Exception e) {
			e.printStackTrace();
			return new RPCResult<String>(false, "Internal Server Error", "");
		}
	}
	
	private String replaceDescription(String content, String description) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(new InputSource(new ByteArrayInputStream(content.getBytes("UTF-8"))));
		
		XPath xPath = XPathFactory.newInstance().newXPath();
		Node node = (Node)xPath.evaluate("/treatment/description",
		        document.getDocumentElement(), XPathConstants.NODE);
		node.setTextContent(description);

		DOMSource domSource = new DOMSource(document);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);
		//return xmlFileFormatter.format(writer.toString());
		return writer.toString();
	}

	@Override
	public RPCResult<Void> setDescription(AuthenticationToken authenticationToken, String filePath, String description) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<Void>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<Void>(false, "Authentication failed");
		
		RPCResult<String> fileContentResult = fileAccessService.getFileContent(authenticationToken, filePath);
		if(fileContentResult.isSucceeded()) {
			String content = fileContentResult.getData();		
			String newContent;
			try {
				newContent = replaceDescription(content, description);
				RPCResult<Void> setContentResult = fileAccessService.setFileContent(authenticationToken, filePath, newContent);
				if(!setContentResult.isSucceeded()) 
					return new RPCResult<Void>(false, setContentResult.getMessage());
				return new RPCResult<Void>(true);
			} catch (Exception e) {
				e.printStackTrace();
				return new RPCResult<Void>(false, "Internal Server Error");
			}
		}
		return new RPCResult<Void>(false, fileContentResult.getMessage());
	}

	@Override
	public RPCResult<SemanticMarkupTaskRun> getLatestResumable(AuthenticationToken authenticationToken) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<SemanticMarkupTaskRun>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<SemanticMarkupTaskRun>(false, "Authentication failed");
		
		try {
			ShortUser user = UserDAO.getInstance().getShortUser(authenticationToken.getUsername());
			List<Task> tasks = TaskDAO.getInstance().getOwnedTasks(user.getId());
			for(Task task : tasks) {
				if(task.isResumable()) {
					SemanticMarkupConfiguration configuration = SemanticMarkupConfigurationDAO.getInstance().getSemanticMarkupConfiguration(task.getConfiguration().getId());
					return new RPCResult<SemanticMarkupTaskRun>(true,
							new SemanticMarkupTaskRun(configuration, task));
				}
			}
			return new RPCResult<SemanticMarkupTaskRun>(false, "No resumable task found");
		} catch(Exception e) {
			e.printStackTrace();
			return new RPCResult<SemanticMarkupTaskRun>(false, "Internal Server Error");
		}
	}

	@Override
	public RPCResult<SemanticMarkupTaskRun> getSemanticMarkupTaskRun(AuthenticationToken authenticationToken, Task task) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<SemanticMarkupTaskRun>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<SemanticMarkupTaskRun>(false, "Authentication failed");
		
		try {
			task = TaskDAO.getInstance().getTask(task.getId());
			SemanticMarkupConfiguration configuration = 
					SemanticMarkupConfigurationDAO.getInstance().getSemanticMarkupConfiguration(task.getConfiguration().getId());
			return new RPCResult<SemanticMarkupTaskRun>(true, 
					new SemanticMarkupTaskRun(configuration, task));
		} catch(Exception e) {
			e.printStackTrace();
			return new RPCResult<SemanticMarkupTaskRun>(false, "Internal Server Error");
		}
	}

	@Override
	public RPCResult<Void> cancel(AuthenticationToken authenticationToken, Task task) {
		RPCResult<AuthenticationResult> authResult = authenticationService.isValidSession(authenticationToken);
		if(!authResult.isSucceeded()) 
			return new RPCResult<Void>(false, authResult.getMessage());
		if(!authResult.getData().getResult())
			return new RPCResult<Void>(false, "Authentication failed");
		
		try {
			RPCResult<SemanticMarkupTaskRun> semanticMarkupTaskRunResult = 
					getSemanticMarkupTaskRun(authenticationToken, task);
			if(!semanticMarkupTaskRunResult.isSucceeded()) 
				return new RPCResult<Void>(false, semanticMarkupTaskRunResult.getMessage());
			SemanticMarkupTaskRun semanticMarkupTaskRun = semanticMarkupTaskRunResult.getData();
			SemanticMarkupConfiguration semanticMarkupConfiguration = semanticMarkupTaskRun.getConfiguration();
						
			//remove matrix generation configuration
			if(semanticMarkupConfiguration != null) {
				if(semanticMarkupConfiguration.getInput() != null)
					fileService.setInUse(authenticationToken, false, semanticMarkupConfiguration.getInput(), task);
				SemanticMarkupConfigurationDAO.getInstance().remove(semanticMarkupConfiguration);
			}
			
			//remove task
			if(task != null) {
				TaskDAO.getInstance().removeTask(task);
				if(task.getConfiguration() != null)
					
					//remove configuration
					ConfigurationDAO.getInstance().remove(task.getConfiguration());
			
				//cancel possible futures
				if(task.getTaskStage() != null) {
					switch(task.getTaskStage().getTaskStageEnum()) {
					case INPUT:
						break;
					case LEARN_TERMS:
						if(activeLearnFutures.containsKey(semanticMarkupConfiguration.getConfiguration().getId())) {
							ListenableFuture<LearnResult> learnFuture = activeLearnFutures.get(semanticMarkupConfiguration.getConfiguration().getId());
							learnFuture.cancel(true);
						}
						break;
					case OUTPUT:
						break;
					case PARSE_TEXT:
						if(activeParseFutures.containsKey(semanticMarkupConfiguration.getConfiguration().getId())) {
							ListenableFuture<ParseResult> parseFuture = activeParseFutures.get(semanticMarkupConfiguration.getConfiguration().getId());
							parseFuture.cancel(true);
						}
						break;
					case PREPROCESS_TEXT:
						break;
					case REVIEW_TERMS:
						break;
					default:
						break;
					}
				}
			}
			return new RPCResult<Void>(true);
		} catch (Exception e) {
			e.printStackTrace();
			return new RPCResult<Void>(false, "Internal Server Error");
		}
	}

	@Override
	public void destroy() {
		this.executorService.shutdownNow();
		super.destroy();
	}
}