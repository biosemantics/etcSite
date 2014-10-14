package edu.arizona.biosemantics.etcsite.client.content.taskManager;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;
import com.sencha.gxt.widget.core.client.Dialog.PredefinedButton;
import com.sencha.gxt.widget.core.client.box.MessageBox;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent.SelectHandler;

import edu.arizona.biosemantics.etcsite.client.common.Alerter;
import edu.arizona.biosemantics.etcsite.client.common.Authentication;
import edu.arizona.biosemantics.etcsite.client.content.user.IUserSelectView;
import edu.arizona.biosemantics.etcsite.client.content.user.IUsersView;
import edu.arizona.biosemantics.etcsite.client.content.user.UserSelectPresenter;
import edu.arizona.biosemantics.etcsite.client.content.user.UserSelectPresenter.ISelectListener;
import edu.arizona.biosemantics.etcsite.client.content.user.UsersPresenter;
import edu.arizona.biosemantics.etcsite.client.event.FailedTasksEvent;
import edu.arizona.biosemantics.etcsite.client.event.ResumableTasksEvent;
import edu.arizona.biosemantics.etcsite.shared.model.Share;
import edu.arizona.biosemantics.etcsite.shared.model.ShortUser;
import edu.arizona.biosemantics.etcsite.shared.model.Task;
import edu.arizona.biosemantics.etcsite.shared.rpc.matrixGeneration.IMatrixGenerationServiceAsync;
import edu.arizona.biosemantics.etcsite.shared.rpc.semanticmarkup.ISemanticMarkupServiceAsync;
import edu.arizona.biosemantics.etcsite.shared.rpc.task.ITaskServiceAsync;

public class TaskManagerPresenter implements ITaskManagerView.Presenter {
	
	private IUserSelectView.Presenter userSelectPresenter;
	private IUsersView.Presenter usersPresenter;
	private Map<Task, Set<ShortUser>> inviteesForOwnedTasks = new HashMap<Task, Set<ShortUser>>();
	private ITaskServiceAsync taskService;
	private ITaskManagerView view;
	private ISemanticMarkupServiceAsync semanticMarkupService;
	private IMatrixGenerationServiceAsync matrixGenerationService;
	private PlaceController placeController;
	private EventBus eventBus;
	private ResumeTaskPlaceMapper resumeTaskPlaceMapper;
	
	@Inject 
	public TaskManagerPresenter(final ITaskManagerView view, PlaceController placeController, 
			@Named("Tasks")EventBus eventBus,
			final ITaskServiceAsync taskService, 
			ISemanticMarkupServiceAsync semanticMarkupService,
			IMatrixGenerationServiceAsync matrixGenerationService, ResumeTaskPlaceMapper resumeTaskPlaceMapper, 
			IUserSelectView.Presenter userSelectPresenter, IUsersView.Presenter usersPresenter) {
		this.view = view;
		this.view.setPresenter(this);
		this.placeController = placeController;
		this.eventBus = eventBus;
		this.userSelectPresenter = userSelectPresenter;
		this.usersPresenter = usersPresenter;
		this.taskService = taskService;
		this.semanticMarkupService = semanticMarkupService;
		this.matrixGenerationService = matrixGenerationService;
		this.resumeTaskPlaceMapper = resumeTaskPlaceMapper;
		
		eventBus.addHandler(ResumableTasksEvent.TYPE, new ResumableTasksEvent.ResumableTasksEventHandler() {
			@Override
			public void onResumableTaskEvent(ResumableTasksEvent resumableTasksEvent) {
				for(Task task : resumableTasksEvent.getTasks().values()) {
					view.updateTaskData(new TaskData(task, inviteesForOwnedTasks.get(task)));
				}
			}
		});
		eventBus.addHandler(FailedTasksEvent.TYPE, new FailedTasksEvent.FailedTasksEventHandler() {
			@Override
			public void onFailedTasksEvent(FailedTasksEvent failedTasksEvent) {
				for(Task task : failedTasksEvent.getTasks().values()) {
					view.updateTaskData(new TaskData(task, inviteesForOwnedTasks.get(task)));
				}
			}
		});
	}

	@Override
	public void onShare(TaskData taskData) {
		final Task task = taskData.getTask();
		final Share share = new Share();
		share.setTask(task);
		taskService.getInvitees(Authentication.getInstance().getToken(), task, new AsyncCallback<Set<ShortUser>>() {			
			@Override
			public void onSuccess(Set<ShortUser> result) {
				usersPresenter.refresh();
				//usersPresenter.setSelected(result);
				userSelectPresenter.show(new ISelectListener() {
					@Override
					public void onSelect(Set<ShortUser> users) {
						inviteesForOwnedTasks.put(task, users);
						view.updateTaskData(new TaskData(task, users));						
						share.setInvitees(users);
						taskService.addOrUpdateShare(Authentication.getInstance().getToken(), share, new AsyncCallback<Share>() {
							@Override
							public void onSuccess(Share result) {}

							@Override
							public void onFailure(Throwable caught) {
								Alerter.failedToAddOrUpdateShare(caught);
							} 				
						});
					}
				});
			}
			@Override
			public void onFailure(Throwable caught) {
				Alerter.failedToGetInvitees(caught);
			}
		});
	}

	@Override
	public void onDelete(final List<TaskData> list){
		MessageBox confirm = Alerter.confirmTaskDelete(list.size());
		confirm.getButton(PredefinedButton.YES).addSelectHandler(new SelectHandler() {

			@Override
			public void onSelect(SelectEvent event) {
				boolean deleteShared = false; 
				for (final TaskData data: list) {
					if(data.getTask().getUser().getId() == Authentication.getInstance().getUserId() && !data.getInvitees().isEmpty()) {
						deleteShared = true;
						break;
					}
				}
				if (deleteShared){
					MessageBox confirm = Alerter.confirmSharedTasksDelete();
					confirm.getButton(PredefinedButton.YES).addSelectHandler(new SelectHandler() {
						@Override
						public void onSelect(SelectEvent event) {
							for (final TaskData data: list) {
								final Task task = data.getTask();
								
								if(task.getUser().getId() != Authentication.getInstance().getUserId()){
									taskService.removeMeFromShare(Authentication.getInstance().getToken(), task, new AsyncCallback<Void>() {
										@Override
										public void onSuccess(Void result) {
											inviteesForOwnedTasks.remove(task);
											view.removeTaskData(data);
										}

										@Override
										public void onFailure(Throwable caught) {
											Alerter.failedToRemoveMeFromShare(caught);
										}
									});
								} else {
									cancelTask(data);
								}
							}
						}
					});
				} else {
					for (final TaskData data: list) {
						final Task task = data.getTask();
						
						if(task.getUser().getId() != Authentication.getInstance().getUserId()){ //user is not the owner
							taskService.removeMeFromShare(Authentication.getInstance().getToken(), task, new AsyncCallback<Void>() {
								@Override
								public void onSuccess(Void result) {
									inviteesForOwnedTasks.remove(task);
									view.removeTaskData(data);
								}

								@Override
								public void onFailure(Throwable caught) {
									Alerter.failedToRemoveMeFromShare(caught);
								}
							});
						} else {
							cancelTask(data);
						}
					}
				}
				view.resetSelection();
			}
			
		});
	}
	
	@Override
	public void onDelete(final TaskData taskData) {
		final Task task = taskData.getTask();
		
		MessageBox confirm = Alerter.confirmTaskDelete(task.getName());
		confirm.getButton(PredefinedButton.YES).addSelectHandler(new SelectHandler() {
			@Override
			public void onSelect(SelectEvent event) {
				if(task.getUser().getId() == Authentication.getInstance().getUserId()) {
					if(!taskData.getInvitees().isEmpty()) {
						MessageBox confirm = Alerter.confirmSharedTaskDelete();
						confirm.getButton(PredefinedButton.YES).addSelectHandler(new SelectHandler() {
							@Override
							public void onSelect(SelectEvent event) {
								cancelTask(taskData);
								view.resetSelection();
							}
						});
					} else {
						cancelTask(taskData);
						view.resetSelection();
					}
				} else {
					taskService.removeMeFromShare(Authentication.getInstance().getToken(), task, new AsyncCallback<Void>() {
						@Override
						public void onSuccess(Void result) {
							inviteesForOwnedTasks.remove(task);
							view.removeTaskData(taskData);
						}

						@Override
						public void onFailure(Throwable caught) {
							Alerter.failedToRemoveMeFromShare(caught);
						}
					});
				}
			}
		});
	}
	
	private void cancelTask(final TaskData taskData) {
		taskService.cancelTask(Authentication.getInstance().getToken(), taskData.getTask(), new AsyncCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				inviteesForOwnedTasks.remove(taskData.getTask());
				view.removeTaskData(taskData);
			}

			@Override
			public void onFailure(Throwable caught) {
				Alerter.failedToCancelTask(caught);
			}
		});
	}

	@Override
	public void onRewind(final TaskData taskData) {
		switch(taskData.getTask().getTaskType().getTaskTypeEnum()) {
		case SEMANTIC_MARKUP:
			semanticMarkupService.goToTaskStage(Authentication.getInstance().getToken(), taskData.getTask(), 
					edu.arizona.biosemantics.etcsite.shared.model.semanticmarkup.TaskStageEnum.REVIEW_TERMS ,new AsyncCallback<Task>() {
				@Override
				public void onSuccess(Task result) {
					placeController.goTo(new edu.arizona.biosemantics.etcsite.client.content.semanticMarkup.SemanticMarkupReviewPlace(result));
				}

				@Override
				public void onFailure(Throwable caught) {
					Alerter.failedToGoToTaskStage(caught);
				}
			});
			break;
		case MATRIX_GENERATION:
			matrixGenerationService.goToTaskStage(Authentication.getInstance().getToken(), taskData.getTask(), 
					edu.arizona.biosemantics.etcsite.shared.model.matrixgeneration.TaskStageEnum.REVIEW, new AsyncCallback<Task>() {
				@Override
				public void onSuccess(Task result) {
					placeController.goTo(new edu.arizona.biosemantics.etcsite.client.content.matrixGeneration.MatrixGenerationReviewPlace(result));
				}

				@Override
				public void onFailure(Throwable caught) {
					Alerter.failedToGoToTaskStage(caught);
				}
			});
			break;
		}
	}

	@Override
	public void onResume(TaskData taskData) {
		placeController.goTo(resumeTaskPlaceMapper.getPlace(taskData.getTask()));
	}

	@Override
	public IsWidget getView() {
		return view;
	}

	@Override
	public void refresh() {
		view.resetSelection();
		taskService.getAllTasks(Authentication.getInstance().getToken(), new AsyncCallback<List<Task>>() {
			@Override
			public void onSuccess(final List<Task> taskResult) {
				taskService.getInviteesForOwnedTasks(Authentication.getInstance().getToken(), new AsyncCallback<Map<Task, Set<ShortUser>>>() {
					@Override
					public void onSuccess(Map<Task, Set<ShortUser>> result) {
						inviteesForOwnedTasks = result;
						List<TaskData> taskData = new LinkedList<TaskData>();
						for(Task task : taskResult)
							taskData.add(new TaskData(task, result.get(task)));
						view.setTaskData(taskData);
					}

					@Override
					public void onFailure(Throwable caught) {
						Alerter.failedToGetInvitees(caught);
					}
				});
			}

			@Override
			public void onFailure(Throwable caught) {
				Alerter.failedToGetAllTasks(caught);
			}
		});
	}
}