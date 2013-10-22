package edu.arizona.sirls.etc.site.server.rpc;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.text.DateFormat;
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

import edu.arizona.sirls.etc.site.client.Authentication;
import edu.arizona.sirls.etc.site.client.AuthenticationToken;
import edu.arizona.sirls.etc.site.server.Configuration;
import edu.arizona.sirls.etc.site.shared.rpc.IAuthenticationService;
import edu.arizona.sirls.etc.site.shared.rpc.IFileAccessService;
import edu.arizona.sirls.etc.site.shared.rpc.IFileService;
import edu.arizona.sirls.etc.site.shared.rpc.IMatrixGenerationService;
import edu.arizona.sirls.etc.site.shared.rpc.ITaskService;
import edu.arizona.sirls.etc.site.shared.rpc.MatrixGenerationTaskRun;
import edu.arizona.sirls.etc.site.shared.rpc.MessageResult;
import edu.arizona.sirls.etc.site.shared.rpc.db.Glossary;
import edu.arizona.sirls.etc.site.shared.rpc.db.GlossaryDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.MatrixGenerationConfiguration;
import edu.arizona.sirls.etc.site.shared.rpc.db.MatrixGenerationConfigurationDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.Task;
import edu.arizona.sirls.etc.site.shared.rpc.db.TaskDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.TaskStage;
import edu.arizona.sirls.etc.site.shared.rpc.db.TaskStageDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.TaskType;
import edu.arizona.sirls.etc.site.shared.rpc.db.TaskTypeDAO;
import edu.arizona.sirls.etc.site.shared.rpc.db.User;
import edu.arizona.sirls.etc.site.shared.rpc.db.UserDAO;
import edu.arizona.sirls.etc.site.shared.rpc.file.XMLFileFormatter;
import edu.arizona.sirls.etc.site.shared.rpc.matrixGeneration.BracketValidator;
import edu.arizona.sirls.etc.site.shared.rpc.matrixGeneration.LearnInvocation;
import edu.arizona.sirls.etc.site.shared.rpc.matrixGeneration.ParseInvocation;
import edu.arizona.sirls.etc.site.shared.rpc.matrixGeneration.PreprocessedDescription;
import edu.arizona.sirls.etc.site.shared.rpc.matrixGeneration.TaskStageEnum;

public class MatrixGenerationService extends RemoteServiceServlet implements IMatrixGenerationService  {

	private static final long serialVersionUID = -7871896158610489838L;
	private IAuthenticationService authenticationService = new AuthenticationService();
	private IFileAccessService fileAccessService = new FileAccessService();
	private ITaskService taskService = new TaskService();
	private IFileService fileService = new FileService();
	private XMLFileFormatter xmlFileFormatter = new XMLFileFormatter();
	private BracketValidator bracketValidator = new BracketValidator();
	private int maximumThreads = 10;
	private ListeningExecutorService executorService;
	private Map<Integer, ListenableFuture<LearnResult>> activeLearnFutures = new HashMap<Integer, ListenableFuture<LearnResult>>();
	private Map<Integer, ListenableFuture<ParseResult>> activeParseFutures = new HashMap<Integer, ListenableFuture<ParseResult>>();
	private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM-dd-yyyy");
	
	public MatrixGenerationService() {
		executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
	}
	
	@Override
	public MatrixGenerationTaskRun start(AuthenticationToken authenticationToken, String taskName, 
			String input, String glossaryName) {
		try {
			int numberOfInputFiles = fileService.getDirectoriesFiles(authenticationToken, input).size();
			Glossary glossary = GlossaryDAO.getInstance().getGlossary(glossaryName);
			MatrixGenerationConfiguration matrixGenerationConfiguration = new MatrixGenerationConfiguration();
			matrixGenerationConfiguration.setInput(input);	
			matrixGenerationConfiguration.setGlossary(glossary);
			matrixGenerationConfiguration.setNumberOfInputFiles(numberOfInputFiles);
			matrixGenerationConfiguration = MatrixGenerationConfigurationDAO.getInstance().addMatrixGenerationConfiguration(matrixGenerationConfiguration);
			
			edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum taskType = edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.MATRIX_GENERATION;
			TaskType dbTaskType = TaskTypeDAO.getInstance().getTaskType(taskType);
			TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(dbTaskType, TaskStageEnum.INPUT);
			User user = UserDAO.getInstance().getUser(authenticationToken.getUsername());
			Task task = new Task();
			task.setName(taskName);
			task.setResumable(true);
			task.setUser(user);
			task.setTaskStage(taskStage);
			task.setConfiguration(matrixGenerationConfiguration.getConfiguration());
			task.setTaskType(dbTaskType);
			task = taskService.addTask(authenticationToken, task);
			
			taskStage = TaskStageDAO.getInstance().getTaskStage(dbTaskType, TaskStageEnum.PREPROCESS_TEXT);
			task.setTaskStage(taskStage);
			TaskDAO.getInstance().updateTask(task);

			fileService.setInUse(authenticationToken, true, input, task);
			return new MatrixGenerationTaskRun(matrixGenerationConfiguration, task);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public List<PreprocessedDescription> preprocess(AuthenticationToken authenticationToken, MatrixGenerationTaskRun matrixGenerationTaskRun) {
		List<PreprocessedDescription> result = new LinkedList<PreprocessedDescription>();
		
		if(authenticationService.isValidSession(authenticationToken).getResult()) { 
			try {
				Task task = matrixGenerationTaskRun.getTask();
				TaskType taskType = TaskTypeDAO.getInstance().getTaskType(edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.MATRIX_GENERATION);
				TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.PREPROCESS_TEXT);
				task.setTaskStage(taskStage);
				TaskDAO.getInstance().updateTask(task);
				//do preprocessing here, return result immediately or always only return an invocation
				//and make user come back when ready?
				String inputDirectory = matrixGenerationTaskRun.getConfiguration().getInput();
				
				if(fileService.isDirectory(authenticationToken, inputDirectory)) {
					List<String> files = fileService.getDirectoriesFiles(authenticationToken, inputDirectory);
					for(String file : files) {
						String description = getDescription(authenticationToken, inputDirectory + "//" + file);
						if(!bracketValidator.validate(description)) {
							PreprocessedDescription preprocessedDescription = new PreprocessedDescription(
									inputDirectory + "//" + file,
									file, 0,
									bracketValidator.getBracketCountDifferences(description));
							result.add(preprocessedDescription);
						}	
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	@Override
	public LearnInvocation learn(AuthenticationToken authenticationToken, final MatrixGenerationTaskRun matrixGenerationTaskRun) {
		if(authenticationService.isValidSession(authenticationToken).getResult()) { 
			try {
				int numberOfSentences = getNumberOfSentences();
				int numberOfWords = getNumberOfWords();
				
				final MatrixGenerationConfiguration matrixGenerationConfiguration = matrixGenerationTaskRun.getConfiguration();
				//browser back button may invoke another "learn"
				if(activeLearnFutures.containsKey(matrixGenerationConfiguration.getConfiguration().getId())) {
					return new LearnInvocation(numberOfSentences, numberOfWords);
				} else {
					final Task task = TaskDAO.getInstance().getTask(matrixGenerationConfiguration.getConfiguration());
					final TaskType taskType = TaskTypeDAO.getInstance().getTaskType(edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.MATRIX_GENERATION);
					TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.LEARN_TERMS);
					task.setTaskStage(taskStage);
					task.setResumable(false);
					TaskDAO.getInstance().updateTask(task);
					
					String glossary = matrixGenerationConfiguration.getGlossary().getName();
					String input = matrixGenerationConfiguration.getInput();
					String tablePrefix = String.valueOf(task.getId());
					String debugFile = "workspace" + File.separator + task.getId() + File.separator + "debug.log";
					String errorFile = "workspace" + File.separator + task.getId() + File.separator + "error.log";
					String source = input; //maybe something else later
					String user = authenticationToken.getUsername();
					String bioportalUserId = UserDAO.getInstance().getUser(authenticationToken.getUsername()).getBioportalUserId();
					String bioportalAPIKey = UserDAO.getInstance().getUser(authenticationToken.getUsername()).getBioportalAPIKey();
					ILearn learn = new Learn(authenticationToken, glossary, input, tablePrefix, debugFile, errorFile, source, user, bioportalUserId, bioportalAPIKey);
					final ListenableFuture<LearnResult> futureResult = executorService.submit(learn);
					activeLearnFutures.put(matrixGenerationConfiguration.getConfiguration().getId(), futureResult);
					futureResult.addListener(new Runnable() {
					     	public void run() {
					     		try {
					     			activeLearnFutures.remove(matrixGenerationConfiguration.getConfiguration().getId());
					     			matrixGenerationConfiguration.setOtoId(futureResult.get().getOtoId());
									MatrixGenerationConfigurationDAO.getInstance().updateMatrixGenerationConfiguration(matrixGenerationConfiguration);
									TaskStage newTaskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.REVIEW_TERMS);
									task.setTaskStage(newTaskStage);
									task.setResumable(true);
									TaskDAO.getInstance().updateTask(task);
								} catch (Exception e) {
									e.printStackTrace();
								}
					     	}
					     }, executorService);
					
					return new LearnInvocation(numberOfSentences, numberOfWords);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private int getNumberOfSentences() {
		return 5423;
	}

	private int getNumberOfWords() {
		return 21259;
	}
	
	@Override
	public MatrixGenerationTaskRun review(AuthenticationToken authenticationToken, MatrixGenerationTaskRun matrixGenerationTaskRun) {
		if(authenticationService.isValidSession(authenticationToken).getResult()) {
			try {
				Task task = matrixGenerationTaskRun.getTask();
				TaskType taskType = TaskTypeDAO.getInstance().getTaskType(edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.MATRIX_GENERATION);
				TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.REVIEW_TERMS);
				task.setTaskStage(taskStage);
				TaskDAO.getInstance().updateTask(task);
				MatrixGenerationConfiguration configuration = 
						MatrixGenerationConfigurationDAO.getInstance().getMatrixGenerationConfiguration(
								matrixGenerationTaskRun.getConfiguration().getConfiguration().getId());
				
				return new MatrixGenerationTaskRun(configuration, task);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	public ParseInvocation parse(final AuthenticationToken authenticationToken, final MatrixGenerationTaskRun matrixGenerationTaskRun) {
		if(authenticationService.isValidSession(authenticationToken).getResult()) {
			try {
				final MatrixGenerationConfiguration matrixGenerationConfiguration = matrixGenerationTaskRun.getConfiguration();
				if(activeParseFutures.containsKey(matrixGenerationConfiguration.getConfiguration().getId())) {
					return new ParseInvocation();
				} else {
					final Task task = TaskDAO.getInstance().getTask(matrixGenerationConfiguration.getConfiguration());
					final TaskType taskType = TaskTypeDAO.getInstance().getTaskType(edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.MATRIX_GENERATION);
					TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.PARSE_TEXT);
					task.setTaskStage(taskStage);
					task.setResumable(false);
					TaskDAO.getInstance().updateTask(task);
					
					String glossary = matrixGenerationConfiguration.getGlossary().getName();
					String input = matrixGenerationConfiguration.getInput();
					String tablePrefix = String.valueOf(task.getId());
					String debugFile = "workspace" + File.separator + task.getId() + File.separator + "debug.log";
					String errorFile = "workspace" + File.separator + task.getId() + File.separator + "error.log";
					String source = input; //maybe something else later
					String user = authenticationToken.getUsername();
					String bioportalUserId = UserDAO.getInstance().getUser(authenticationToken.getUsername()).getBioportalUserId();
					String bioportalAPIKey = UserDAO.getInstance().getUser(authenticationToken.getUsername()).getBioportalAPIKey();
					IParse parse = new Parse(authenticationToken, glossary, input, tablePrefix, debugFile, errorFile, source, user, bioportalUserId, bioportalAPIKey);
					final ListenableFuture<ParseResult> futureResult = executorService.submit(parse);
					activeParseFutures.put(matrixGenerationConfiguration.getConfiguration().getId(), futureResult);
					futureResult.addListener(new Runnable() {
						@Override
						public void run() {
							try {
								activeParseFutures.remove(matrixGenerationConfiguration.getConfiguration().getId());
								task.setResumable(true);
								TaskStage newTaskStage = TaskStageDAO.getInstance().getTaskStage(taskType, TaskStageEnum.OUTPUT);
								task.setTaskStage(newTaskStage);
								TaskDAO.getInstance().updateTask(task);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}, executorService);
					
					return new ParseInvocation();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public boolean output(AuthenticationToken authenticationToken, MatrixGenerationTaskRun matrixGenerationTaskRun) {
		boolean result = false;
		if(authenticationService.isValidSession(authenticationToken).getResult()) {
			try {				
				Task task = matrixGenerationTaskRun.getTask();
				MatrixGenerationConfiguration matrixGenerationConfiguration = matrixGenerationTaskRun.getConfiguration();
				matrixGenerationConfiguration.setOutput(matrixGenerationConfiguration.getInput() + "_" + task.getName());
				MatrixGenerationConfigurationDAO.getInstance().updateMatrixGenerationConfiguration(matrixGenerationConfiguration);

				String outputDirectory = matrixGenerationConfiguration.getOutput();
				String target = outputDirectory.substring(0, outputDirectory.lastIndexOf("//"));
				String newDirectory = outputDirectory.substring(outputDirectory.lastIndexOf("//"), outputDirectory.length());
				
				//find a suitable targetDirectory
				MessageResult message = fileService.createDirectory(authenticationToken, target, newDirectory);
				if(!message.isSucceeded()) {
					String date = dateTimeFormat.format(new Date());
					newDirectory = newDirectory + "_" + date;
					message = fileService.createDirectory(authenticationToken, target, newDirectory);
					int i = 1;
					while(!message.isSucceeded()) {
						newDirectory = newDirectory + "_" + i++;
						message = fileService.createDirectory(authenticationToken, target, newDirectory);
					}
				}
		
				//copy the output files to the target directory
				File outDirectory = new File("workspace" + File.separator + task.getId() + File.separator + "out");
				File targetDirectory = new File(Configuration.fileBase + "//" + authenticationToken.getUsername() + "//" + 
						target + "//" + newDirectory);
				for(File outFile : outDirectory.listFiles()) {
					File targetFile = new File(targetDirectory, outFile.getName());
					Files.copy(outFile, targetFile);
				}
				
				//update task
				task.setResumable(false);
				task.setComplete(true);
				task.setCompleted(new Date());
				TaskDAO.getInstance().updateTask(task);
				
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	@Override
	public MatrixGenerationTaskRun goToTaskStage(AuthenticationToken authenticationToken, MatrixGenerationTaskRun matrixGenerationTaskRun, TaskStageEnum taskStageEnum) {
		if(authenticationService.isValidSession(authenticationToken).getResult()) {
			try {
				Task task = matrixGenerationTaskRun.getTask();
				TaskType taskType = TaskTypeDAO.getInstance().getTaskType(edu.arizona.sirls.etc.site.shared.rpc.TaskTypeEnum.MATRIX_GENERATION);
				TaskStage taskStage = TaskStageDAO.getInstance().getTaskStage(taskType, taskStageEnum);
				task.setTaskStage(taskStage);
				task.setResumable(true);
				task.setComplete(false);
				task.setCompleted(null);
				TaskDAO.getInstance().updateTask(task);
				MatrixGenerationConfiguration configuration = 
						MatrixGenerationConfigurationDAO.getInstance().getMatrixGenerationConfiguration(
								matrixGenerationTaskRun.getConfiguration().getConfiguration().getId());
				return new MatrixGenerationTaskRun(configuration, task);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}	

	@Override
	public String getDescription(AuthenticationToken authenticationToken, String target) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			String fileContent = fileAccessService.getFileContent(authenticationToken, target);
			Document document = db.parse(new InputSource(new ByteArrayInputStream(fileContent.getBytes("UTF-8"))));
			
			XPath xPath = XPathFactory.newInstance().newXPath();
			Node node = (Node)xPath.evaluate("/treatment/description",
			        document.getDocumentElement(), XPathConstants.NODE);
			return node.getTextContent();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	private String replaceDescription(String content, String description) {
		try {
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
		} catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public boolean setDescription(AuthenticationToken authenticationToken, String target, String description) {
		boolean result = false;
		if(authenticationService.isValidSession(authenticationToken).getResult()) {
			String content = fileAccessService.getFileContent(authenticationToken, target);
			String newContent = replaceDescription(content, description);
			result = fileAccessService.setFileContent(authenticationToken, target, newContent);
		}
		return result;
	}

	@Override
	public MatrixGenerationTaskRun getLatestResumable(AuthenticationToken authenticationToken) {
		if(authenticationService.isValidSession(authenticationToken).getResult()) {
			try {
				User user = UserDAO.getInstance().getUser(authenticationToken.getUsername());
				List<Task> tasks = TaskDAO.getInstance().getUsersTasks(user.getId());
				for(Task task : tasks) {
					if(task.isResumable()) {
						MatrixGenerationConfiguration configuration = MatrixGenerationConfigurationDAO.getInstance().getMatrixGenerationConfiguration(task.getConfiguration().getId());
						return new MatrixGenerationTaskRun(configuration, task);
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public MatrixGenerationTaskRun getMatrixGenerationTaskRun(AuthenticationToken authenticationToken, Task task) {
		if(authenticationService.isValidSession(authenticationToken).getResult()) {
			try {
				task = TaskDAO.getInstance().getTask(task.getId());
				MatrixGenerationConfiguration configuration = 
						MatrixGenerationConfigurationDAO.getInstance().getMatrixGenerationConfiguration(task.getConfiguration().getId());
				return new MatrixGenerationTaskRun(configuration, task);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public void cancel(AuthenticationToken authenticationToken, Task task) {
		if(authenticationService.isValidSession(authenticationToken).getResult()) {
			try {
				MatrixGenerationTaskRun matrixGenerationTaskRun = getMatrixGenerationTaskRun(authenticationToken, task);
				MatrixGenerationConfiguration matrixGenerationConfiguration = matrixGenerationTaskRun.getConfiguration();
				fileService.setInUse(authenticationToken, false, matrixGenerationConfiguration.getInput(), task);
				MatrixGenerationConfigurationDAO.getInstance().remove(matrixGenerationConfiguration);
				TaskDAO.getInstance().removeTask(task);
				switch(task.getTaskStage().getTaskStageEnum()) {
				case INPUT:
					break;
				case LEARN_TERMS:
					if(activeLearnFutures.containsKey(matrixGenerationConfiguration.getConfiguration().getId())) {
						ListenableFuture<LearnResult> learnFuture = activeLearnFutures.get(matrixGenerationConfiguration.getConfiguration().getId());
						learnFuture.cancel(true);
					}
					break;
				case OUTPUT:
					break;
				case PARSE_TEXT:
					if(activeParseFutures.containsKey(matrixGenerationConfiguration.getConfiguration().getId())) {
						ListenableFuture<ParseResult> parseFuture = activeParseFutures.get(matrixGenerationConfiguration.getConfiguration().getId());
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
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void destroy() {
		this.executorService.shutdownNow();
		super.destroy();
	}
}
