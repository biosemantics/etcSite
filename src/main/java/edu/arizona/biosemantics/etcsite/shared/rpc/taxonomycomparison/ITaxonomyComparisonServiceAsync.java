package edu.arizona.biosemantics.etcsite.shared.rpc.taxonomycomparison;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.arizona.biosemantics.common.biology.TaxonGroup;
import edu.arizona.biosemantics.etcsite.shared.model.Task;
import edu.arizona.biosemantics.etcsite.shared.model.file.FileTreeItem;
import edu.arizona.biosemantics.etcsite.shared.model.file.FolderTreeItem;
import edu.arizona.biosemantics.etcsite.shared.model.taxonomycomparison.TaskStageEnum;
import edu.arizona.biosemantics.etcsite.shared.rpc.HasTaskException;
import edu.arizona.biosemantics.etcsite.shared.rpc.IHasTasksServiceAsync;
import edu.arizona.biosemantics.etcsite.shared.rpc.auth.AuthenticationToken;
import edu.arizona.biosemantics.euler.alignment.shared.model.Articulation;
import edu.arizona.biosemantics.euler.alignment.shared.model.Collection;
import edu.arizona.biosemantics.euler.alignment.shared.model.RunOutput;

public interface ITaxonomyComparisonServiceAsync extends IHasTasksServiceAsync {
	
	public void runMirGeneration(AuthenticationToken token, Task task,
			Collection collection, AsyncCallback<Task> asyncCallback);

	public void getInputVisualization(AuthenticationToken token, Task task,
			Collection collection, AsyncCallback<String> callback);

	public void getMirGenerationResult(AuthenticationToken token, Task task, AsyncCallback<RunOutput> callback);

	public void goToTaskStage(AuthenticationToken token, Task task,
			TaskStageEnum taskStageEnum, AsyncCallback<Task> callback);
	

	public void getCollection(AuthenticationToken token, Task task, AsyncCallback<Collection> callback);

	public void saveCollection(AuthenticationToken token, Task task, Collection collection, AsyncCallback<Void> callback);

	public void exportArticulations(AuthenticationToken token, Task task, Collection collection, AsyncCallback<String> callback);
	
	public void getTaxonomies(AuthenticationToken token, FolderTreeItem folder, AsyncCallback<List<String>> callback);

	public void getMachineArticulations(AuthenticationToken token, Task task, Collection collection, double threshold,
			AsyncCallback<List<Articulation>> asyncCallback);

	public void startFromCleantax(AuthenticationToken token, String taskName, String input, String taxonGroup, 
			String ontology, String termReview1, String termReview2, AsyncCallback<Task> callback);

	public void startFromSerializedModels(AuthenticationToken token,
			String taskName, String inputFolderPath1, String inputFolderPath2, 
			String taxonGroup, String ontology, String termReview1, String termReview2,
			AsyncCallback<Task> asyncCallback);

	public void isValidCleanTaxInput(AuthenticationToken token,
			String inputFolderPath, AsyncCallback<Boolean> asyncCallback);

	public void isValidModelInput(AuthenticationToken token,
			String inputFolderPath, AsyncCallback<Boolean> asyncCallback);

}
