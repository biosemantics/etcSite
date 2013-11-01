package edu.arizona.sirls.etc.site.client;

import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import edu.arizona.sirls.etc.site.client.event.IETCSiteEvent;
import edu.arizona.sirls.etc.site.client.event.FileManagerEvent;
import edu.arizona.sirls.etc.site.client.event.FileManagerEventHandler;
import edu.arizona.sirls.etc.site.client.event.HelpEvent;
import edu.arizona.sirls.etc.site.client.event.HelpEventHandler;
import edu.arizona.sirls.etc.site.client.event.HomeEvent;
import edu.arizona.sirls.etc.site.client.event.HomeEventHandler;
import edu.arizona.sirls.etc.site.client.event.LoginEvent;
import edu.arizona.sirls.etc.site.client.event.LoginEventHandler;
import edu.arizona.sirls.etc.site.client.event.LogoutEvent;
import edu.arizona.sirls.etc.site.client.event.LogoutEventHandler;
import edu.arizona.sirls.etc.site.client.event.MarkupReviewEvent;
import edu.arizona.sirls.etc.site.client.event.MarkupReviewEventHandler;
import edu.arizona.sirls.etc.site.client.event.MatrixGenerationEvent;
import edu.arizona.sirls.etc.site.client.event.MatrixGenerationEventHandler;
import edu.arizona.sirls.etc.site.client.event.ResumableTasksEvent;
import edu.arizona.sirls.etc.site.client.event.SettingsEvent;
import edu.arizona.sirls.etc.site.client.event.SettingsEventHandler;
import edu.arizona.sirls.etc.site.client.event.TaskManagerEvent;
import edu.arizona.sirls.etc.site.client.event.TaskManagerEventHandler;
import edu.arizona.sirls.etc.site.client.event.TaxonomyComparisonEvent;
import edu.arizona.sirls.etc.site.client.event.TaxonomyComparisonEventHandler;
import edu.arizona.sirls.etc.site.client.event.TreeGenerationEvent;
import edu.arizona.sirls.etc.site.client.event.TreeGenerationEventHandler;
import edu.arizona.sirls.etc.site.client.event.VisualizationEvent;
import edu.arizona.sirls.etc.site.client.event.VisualizationEventHandler;
import edu.arizona.sirls.etc.site.client.presenter.FooterPresenter;
import edu.arizona.sirls.etc.site.client.presenter.HelpPresenter;
import edu.arizona.sirls.etc.site.client.presenter.LoggedInHeaderPresenter;
import edu.arizona.sirls.etc.site.client.presenter.LoggedOutHeaderPresenter;
import edu.arizona.sirls.etc.site.client.presenter.LoginPresenter;
import edu.arizona.sirls.etc.site.client.presenter.MainMenuPresenter;
import edu.arizona.sirls.etc.site.client.presenter.MessageResumeOrStartPresenter;
import edu.arizona.sirls.etc.site.client.presenter.SettingsPresenter;
import edu.arizona.sirls.etc.site.client.presenter.StartMenuPresenter;
import edu.arizona.sirls.etc.site.client.presenter.StartPresenter;
import edu.arizona.sirls.etc.site.client.presenter.TaskManagerPresenter;
import edu.arizona.sirls.etc.site.client.presenter.annotationReview.AnnotationReviewPresenter;
import edu.arizona.sirls.etc.site.client.presenter.annotationReview.ResultPresenter;
import edu.arizona.sirls.etc.site.client.presenter.annotationReview.SearchPresenter;
import edu.arizona.sirls.etc.site.client.presenter.annotationReview.XMLEditorPresenter;
import edu.arizona.sirls.etc.site.client.presenter.fileManager.FileManagerPresenter;
import edu.arizona.sirls.etc.site.client.presenter.matrixGeneration.InputMatrixGenerationPresenter;
import edu.arizona.sirls.etc.site.client.presenter.matrixGeneration.LearnMatrixGenerationPresenter;
import edu.arizona.sirls.etc.site.client.presenter.matrixGeneration.OutputMatrixGenerationPresenter;
import edu.arizona.sirls.etc.site.client.presenter.matrixGeneration.ParseMatrixGenerationPresenter;
import edu.arizona.sirls.etc.site.client.presenter.matrixGeneration.PreprocessMatrixGenerationPresenter;
import edu.arizona.sirls.etc.site.client.presenter.matrixGeneration.ReviewMatrixGenerationPresenter;
import edu.arizona.sirls.etc.site.client.presenter.taxonomyComparison.TaxonomyComparisonPresenter;
import edu.arizona.sirls.etc.site.client.presenter.treeGeneration.TreeGenerationPresenter;
import edu.arizona.sirls.etc.site.client.presenter.visualization.VisualizationPresenter;
import edu.arizona.sirls.etc.site.client.view.FooterView;
import edu.arizona.sirls.etc.site.client.view.HelpView;
import edu.arizona.sirls.etc.site.client.view.LoggedInHeaderView;
import edu.arizona.sirls.etc.site.client.view.LoggedOutHeaderView;
import edu.arizona.sirls.etc.site.client.view.LoginView;
import edu.arizona.sirls.etc.site.client.view.MainMenuView;
import edu.arizona.sirls.etc.site.client.view.MessageResumeOrStartView;
import edu.arizona.sirls.etc.site.client.view.SettingsView;
import edu.arizona.sirls.etc.site.client.view.SettingsViewImpl;
import edu.arizona.sirls.etc.site.client.view.StartMenuView;
import edu.arizona.sirls.etc.site.client.view.StartView;
import edu.arizona.sirls.etc.site.client.view.TaskManagerView;
import edu.arizona.sirls.etc.site.client.view.annotationReview.AnnotationReviewViewImpl;
import edu.arizona.sirls.etc.site.client.view.annotationReview.ResultViewImpl;
import edu.arizona.sirls.etc.site.client.view.annotationReview.SearchViewImpl;
import edu.arizona.sirls.etc.site.client.view.annotationReview.XMLEditorViewImpl;
import edu.arizona.sirls.etc.site.client.view.fileManager.FileManagerView;
import edu.arizona.sirls.etc.site.client.view.matrixGeneration.InputMatrixGenerationView;
import edu.arizona.sirls.etc.site.client.view.matrixGeneration.LearnMatrixGenerationView;
import edu.arizona.sirls.etc.site.client.view.matrixGeneration.OutputMatrixGenerationView;
import edu.arizona.sirls.etc.site.client.view.matrixGeneration.ParseMatrixGenerationView;
import edu.arizona.sirls.etc.site.client.view.matrixGeneration.PreprocessMatrixGenerationView;
import edu.arizona.sirls.etc.site.client.view.matrixGeneration.ReviewMatrixGenerationView;
import edu.arizona.sirls.etc.site.client.view.taxonomyComparison.TaxonomyComparisonView;
import edu.arizona.sirls.etc.site.client.view.treeGeneration.TreeGenerationView;
import edu.arizona.sirls.etc.site.client.view.visualization.VisualizationView;
import edu.arizona.sirls.etc.site.shared.rpc.AuthenticationResult;
import edu.arizona.sirls.etc.site.shared.rpc.IAuthenticationService;
import edu.arizona.sirls.etc.site.shared.rpc.IAuthenticationServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.IFileAccessService;
import edu.arizona.sirls.etc.site.shared.rpc.IFileAccessServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.IFileFormatService;
import edu.arizona.sirls.etc.site.shared.rpc.IFileFormatServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.IFileSearchService;
import edu.arizona.sirls.etc.site.shared.rpc.IFileSearchServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.IFileService;
import edu.arizona.sirls.etc.site.shared.rpc.IFileServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.IMatrixGenerationService;
import edu.arizona.sirls.etc.site.shared.rpc.IMatrixGenerationServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.ITaskService;
import edu.arizona.sirls.etc.site.shared.rpc.ITaskServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.ITaxonomyComparisonService;
import edu.arizona.sirls.etc.site.shared.rpc.ITaxonomyComparisonServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.ITreeGenerationService;
import edu.arizona.sirls.etc.site.shared.rpc.ITreeGenerationServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.IUserService;
import edu.arizona.sirls.etc.site.shared.rpc.IUserServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.IVisualizationService;
import edu.arizona.sirls.etc.site.shared.rpc.IVisualizationServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.MatrixGenerationTaskRun;
import edu.arizona.sirls.etc.site.shared.rpc.db.MatrixGenerationConfiguration;
import edu.arizona.sirls.etc.site.shared.rpc.db.Task;
import edu.arizona.sirls.etc.site.shared.rpc.matrixGeneration.TaskStageEnum;

public class MySitePresenter implements SitePresenter, ValueChangeHandler<String> {

	private final IAuthenticationServiceAsync authenticationService = GWT.create(IAuthenticationService.class);
	private final IFileServiceAsync fileService = GWT.create(IFileService.class);
	private final IFileFormatServiceAsync fileFormatService = GWT.create(IFileFormatService.class);
	private final IFileAccessServiceAsync fileAccessService = GWT.create(IFileAccessService.class);
	private final IFileSearchServiceAsync fileSearchService = GWT.create(IFileSearchService.class);
	private final ITaskServiceAsync taskService = GWT.create(ITaskService.class);
	private final IMatrixGenerationServiceAsync matrixGenerationService = GWT.create(IMatrixGenerationService.class);
	private final ITreeGenerationServiceAsync treeGenerationService = GWT.create(ITreeGenerationService.class);
	private final ITaxonomyComparisonServiceAsync taxonomyComparisonService = GWT.create(ITaxonomyComparisonService.class);
	private final IVisualizationServiceAsync visualizationService = GWT.create(IVisualizationService.class);
	private final IUserServiceAsync userService = GWT.create(IUserService.class);
	
	private HandlerManager eventBus;
	
	private HasWidgets footer;
	private HasWidgets content;
	private HasWidgets header;
	private HasWidgets menu;

	protected MainMenuPresenter mainMenuPresenter;
	protected StartMenuPresenter startMenuPresenter;
	private FooterPresenter footerPresenter;
	protected LoggedInHeaderPresenter loggedInHeaderPresenter;
	protected LoggedOutHeaderPresenter loggedOutHeaderPresenter;
	protected VisualizationPresenter visualizationPresenter;
	protected TaxonomyComparisonPresenter taxonomyComparisonPresenter;
	protected TreeGenerationPresenter treeGenerationPresenter;
	protected OutputMatrixGenerationPresenter outputMatrixGenerationPresenter;
	protected ParseMatrixGenerationPresenter parseMatrixGenerationPresenter;
	protected ReviewMatrixGenerationPresenter reviewMatrixGenerationPresenter;
	protected LearnMatrixGenerationPresenter learnMatrixGenerationPresenter;
	protected PreprocessMatrixGenerationPresenter preprocessMatrixGenerationPresenter;
	protected InputMatrixGenerationPresenter inputMatrixGenerationPresenter;
	protected SettingsPresenter settingsPresenter;
	protected FileManagerPresenter fileManagerPresenter;
	protected TaskManagerPresenter taskManagerPresenter;
	protected LoginPresenter loginPresenter;
	protected HelpPresenter helpPresenter;
	protected StartPresenter startPresenter;
	
	private TaskManager taskManager = new TaskManager();
	protected AnnotationReviewPresenter annotationReviewPresenter;
	protected XMLEditorPresenter xmlEditorPresenter;
	protected ResultPresenter resultPresenter;
	protected SearchPresenter searchPresenter;
	private Timer resumableTasksTimer;
	

	public MySitePresenter(final HandlerManager eventBus) {
		this.eventBus = eventBus;
		bind();
	}
	
	private void bind() {
	    History.addValueChangeHandler(this);

	    eventBus.addHandler(LogoutEvent.TYPE,
	        new LogoutEventHandler() {
	          public void onLogout(LogoutEvent event) {
	            logout();
	          }
	        });
	    
	    eventBus.addHandler(LoginEvent.TYPE,
		        new LoginEventHandler() {
		          public void onLogin(LoginEvent event) {
		            login(event.getUsername(), event.getSessionID());
		          }
		        });
	    
	    eventBus.addHandler(HomeEvent.TYPE,
		    	new HomeEventHandler() {
		          public void onHome(HomeEvent event) {
		        	  taskManager.removeActiveTaskConfiguration();
		        	  addToHistory(event);
		          }
		        });
	    
	    eventBus.addHandler(MarkupReviewEvent.TYPE, new MarkupReviewEventHandler() {
			@Override
			public void onMarkupReview(MarkupReviewEvent event) {
				addToHistory(event);
			}
	    });
	    
	    eventBus.addHandler(TaskManagerEvent.TYPE,
		    	new TaskManagerEventHandler() {
		          public void onTaskManager(TaskManagerEvent event) {
		        	  taskManager.removeActiveTaskConfiguration();
		        	  addToHistory(event);
		          }
		        });
	    
	    eventBus.addHandler(FileManagerEvent.TYPE,
		    	new FileManagerEventHandler() {
		          public void onFileManager(FileManagerEvent event) {
		        	  taskManager.removeActiveTaskConfiguration();
		        	  addToHistory(event);
		          }
		        });
	    
	    eventBus.addHandler(HelpEvent.TYPE,
		    	new HelpEventHandler() {
		          public void onHelp(HelpEvent event) {
		        	  taskManager.removeActiveTaskConfiguration();
		        	  addToHistory(event);
		          }
		        });
	    
	    eventBus.addHandler(SettingsEvent.TYPE,
		    	new SettingsEventHandler() {
		          public void onSettings(SettingsEvent event) {
		        	  taskManager.removeActiveTaskConfiguration();
		        	  addToHistory(event);
		          }
		        });
	    
	    eventBus.addHandler(MatrixGenerationEvent.TYPE,
	    	new MatrixGenerationEventHandler() {
	    	
	    	  private MessageResumeOrStartView messageResumeOrStartView = new MessageResumeOrStartView();
		    	
	          public void onMatrixGeneration(final MatrixGenerationEvent matrixGenerationEvent) {	
	        	  if(!matrixGenerationEvent.hasTaskConfiguration()) {
		        	  matrixGenerationService.getLatestResumable(Authentication.getInstance().getAuthenticationToken(),
								new AsyncCallback<MatrixGenerationTaskRun>() {
							@Override
							public void onFailure(Throwable caught) {
								caught.printStackTrace();
							}
							@Override
							public void onSuccess(final MatrixGenerationTaskRun latestResumable) {
								if(latestResumable != null) {
									MessageResumeOrStartPresenter messageResumeOrStartPresenter = 
											new MessageResumeOrStartPresenter(messageResumeOrStartView, "Resumable Task", new ClickHandler() {
												@Override
												public void onClick(ClickEvent event) {
													//resume
													matrixGenerationEvent.setTaskConfiguration(latestResumable);
													taskManager.setActiveTaskRun(latestResumable);
													addToHistory(matrixGenerationEvent);
												}
											}, new ClickHandler() {
												@Override
												public void onClick(ClickEvent event) {
													addToHistory(matrixGenerationEvent);
												}
											});
									messageResumeOrStartPresenter.setMessage("You have a resumable Matrix Generation Task. Do you want to resume it or start a new task?");
									messageResumeOrStartPresenter.go();
								} else {
									addToHistory(matrixGenerationEvent);
								}
							}
		        	  });
	        	  } else {
	        		  taskManager.setActiveTaskRun(matrixGenerationEvent.getTaskConfiguration());
	        		  addToHistory(matrixGenerationEvent);
	        	  }
	          }
	    });
	    
	    eventBus.addHandler(TreeGenerationEvent.TYPE,
		    	new TreeGenerationEventHandler() {
		          public void onTreeGeneration(TreeGenerationEvent event) {
		        	  taskManager.setActiveTaskRun(event.getTaskConfiguration());
		        	  addToHistory(event);
		          }
		        });
	    
	    eventBus.addHandler(TaxonomyComparisonEvent.TYPE,
		    	new TaxonomyComparisonEventHandler() {
		          public void onTaxonomyComparison(TaxonomyComparisonEvent event) {
		        	  taskManager.setActiveTaskRun(event.getTaskConfiguration());
		        	  addToHistory(event);
		          }
		        });
	    
	    eventBus.addHandler(VisualizationEvent.TYPE,
		    	new VisualizationEventHandler() {
		          public void onVisualization(VisualizationEvent event) {
		        	  taskManager.setActiveTaskRun(event.getTaskConfiguration());
		        	  addToHistory(event);
		          }
		        });
	}
	
	protected void addToHistory(IETCSiteEvent event) {
		if(!event.requiresLogin() || Authentication.getInstance().isSet())
			//don't fire the same history state twice in a row
			if(History.getToken().equals(event.getHistoryState().toString()))
				History.fireCurrentHistoryState();
			else
				History.newItem(event.getHistoryState().toString());
		else
			presentLogin(event);
	}

	private void logout() {
		Authentication.getInstance().destroy();
		this.presentHeader(false);
		eventBus.fireEvent(new HomeEvent());
	}
	
	protected void login(String username, String sessionID) {
		Authentication.getInstance().setUsername(username);
		Authentication.getInstance().setSessionID(sessionID);
		this.presentHeader(true);
	}
	
	@Override
	public void go(HasWidgets header, HasWidgets menu, HasWidgets content, HasWidgets footer) {
	    this.header = header;
	    this.menu = menu;
	    this.content = content;
	    this.footer = footer;
	    
	    if ("".equals(History.getToken())) {
	      History.newItem(HistoryState.HOME.toString());
	    }
	    else {
	      History.fireCurrentHistoryState();
	    }
	}

	@Override
	public void onValueChange(ValueChangeEvent<String> event) {
		final HistoryState historyState = HistoryState.valueOf(event.getValue());
		
		this.authenticationService.isValidSession(Authentication.getInstance().getAuthenticationToken(), 
				new AsyncCallback<AuthenticationResult>() {
					@Override
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}
					@Override
					public void onSuccess(AuthenticationResult result) {
						manageResumableTasksTimer(result.getResult());
						presentHeader(result.getResult());
						presentMenu(historyState);
						presentContent(historyState);
						presentFooter();
					}
		});
	}
	
	private void manageResumableTasksTimer(boolean authenticated) {
		if(authenticated) {
			if(this.resumableTasksTimer == null) {
				this.resumableTasksTimer = new Timer() {
			        public void run() {
			        	taskService.getResumableTasks(Authentication.getInstance().getAuthenticationToken(), new AsyncCallback<Map<Integer, Task>>() {
			    			@Override
			    			public void onFailure(Throwable caught) {
			    				caught.printStackTrace();
			    			}
			    			@Override
			    			public void onSuccess(Map<Integer, Task> result) {
			    				eventBus.fireEvent(new ResumableTasksEvent(result));
			    			} 
			    		});
			        }
				};
				this.resumableTasksTimer.scheduleRepeating(1000);
			}
		} else {
			if(this.resumableTasksTimer != null)
				this.resumableTasksTimer.cancel();
		}
	}
	
	private void presentLogin(final IETCSiteEvent event) {
		GWT.runAsync(new RunAsyncCallback() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}
			public void onSuccess() {
				if (loginPresenter == null) 
					loginPresenter = new LoginPresenter(eventBus, new LoginView(), 
							authenticationService, 
							"You have to login to use this functionality");
				loginPresenter.setTarget(event.getGwtEvent());
				loginPresenter.go();
			}
		});
	}

	private void presentMenu(HistoryState historyState) {
		if (historyState != null) {
			switch(historyState) {
				case HOME:
					showStartMenu();
					break;
				default:
					if(Authentication.getInstance().isSet()) {
						GWT.runAsync(new RunAsyncCallback() {
							public void onFailure(Throwable caught) {
								caught.printStackTrace();
							}
							public void onSuccess() {					
								if (mainMenuPresenter == null) {
									mainMenuPresenter = new MainMenuPresenter(eventBus, new MainMenuView());
								}
								mainMenuPresenter.go(menu);
							}
						});
					} else {
						showStartMenu();
					}
			}
		}
	}

	private void showStartMenu() {
		GWT.runAsync(new RunAsyncCallback() {
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
			}
			public void onSuccess() {					
				if (startMenuPresenter == null) {
					startMenuPresenter = new StartMenuPresenter(eventBus, new StartMenuView());
				}
				startMenuPresenter.go(menu);
			}
		});
	}

	private void presentContent(HistoryState historyState) {
		if (historyState != null) {
			//switch on non-/authentication requiring states first
			switch(historyState) {
			case HOME:
				GWT.runAsync(new RunAsyncCallback() {
					public void onFailure(Throwable caught) {
					}
					public void onSuccess() {
						// lazily initialize our views, and keep them around to
						// be reused
						//							
						if (startPresenter == null) {
							startPresenter = new StartPresenter(eventBus, new StartView());
						}
						startPresenter.go(content);
					}
				});
				break;
			case MARKUP_REVIEW:
				GWT.runAsync(new RunAsyncCallback() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}
					public void onSuccess() {
						if(annotationReviewPresenter == null) {
							HandlerManager annotationReviewEventbus = new HandlerManager(null);
							SearchViewImpl searchView = new SearchViewImpl();
							searchPresenter = new SearchPresenter(annotationReviewEventbus, searchView, fileService, fileSearchService);
							ResultViewImpl resultView = new ResultViewImpl();
							resultPresenter = new ResultPresenter(annotationReviewEventbus, resultView);
							XMLEditorViewImpl xmlEditorView = new XMLEditorViewImpl();
							xmlEditorPresenter = new XMLEditorPresenter(annotationReviewEventbus, xmlEditorView, fileAccessService, fileFormatService, fileSearchService);
							AnnotationReviewViewImpl annotationReviewView = new AnnotationReviewViewImpl(searchView, resultView, xmlEditorView, fileAccessService, fileFormatService, fileSearchService);
							annotationReviewPresenter = new AnnotationReviewPresenter(annotationReviewEventbus, annotationReviewView);
						}
						annotationReviewPresenter.go(content);
					}
				});
				break;
			case HELP:
				GWT.runAsync(new RunAsyncCallback() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}
					public void onSuccess() {
						if (helpPresenter == null) {
							helpPresenter = new HelpPresenter(eventBus, new HelpView());
						}
						helpPresenter.go(content);
					}
				});
				break;
			case TASK_MANAGER:
				GWT.runAsync(new RunAsyncCallback() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}
					public void onSuccess() {
						if (taskManagerPresenter == null) {
							taskManagerPresenter = new TaskManagerPresenter(eventBus, 
									new TaskManagerView(), taskService, matrixGenerationService, 
									treeGenerationService, taxonomyComparisonService, visualizationService, userService);
						}
						taskManagerPresenter.go(content);
					}
				});
				break;
			case FILE_MANAGER:
				GWT.runAsync(new RunAsyncCallback() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}
					public void onSuccess() {
						if (fileManagerPresenter == null) {
							fileManagerPresenter = new FileManagerPresenter(eventBus, new FileManagerView(), fileService);
						}
						fileManagerPresenter.go(content);
					}
				});
				break;
			case SETTINGS:
				GWT.runAsync(new RunAsyncCallback() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}
					public void onSuccess() {
						if (settingsPresenter == null) {
							settingsPresenter = new SettingsPresenter(eventBus, new SettingsViewImpl());
						}
						settingsPresenter.go(content);
					}
				});
				break;
			case MATRIX_GENERATION:
				GWT.runAsync(new RunAsyncCallback() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}
					public void onSuccess() {
						if(!taskManager.hasActiveTaskRun() || !(taskManager.getActiveTaskRun() instanceof MatrixGenerationTaskRun)) {
							if (inputMatrixGenerationPresenter == null) {
								inputMatrixGenerationPresenter = new InputMatrixGenerationPresenter(
										eventBus, new InputMatrixGenerationView(), fileService, matrixGenerationService);
							}
							inputMatrixGenerationPresenter.go(content);
						} else {
							final MatrixGenerationTaskRun matrixGenerationTask = (MatrixGenerationTaskRun)taskManager.getActiveTaskRun();
							taskService.isComplete(Authentication.getInstance().getAuthenticationToken(), matrixGenerationTask.getTask(), 
									new AsyncCallback<Boolean>() {
								@Override
								public void onFailure(Throwable caught) {
									caught.printStackTrace();
								}
								@Override
								public void onSuccess(Boolean result) {
									if(result) {
										if (inputMatrixGenerationPresenter == null) {
											inputMatrixGenerationPresenter = new InputMatrixGenerationPresenter(
													eventBus, new InputMatrixGenerationView(), fileService, matrixGenerationService);
										}
										inputMatrixGenerationPresenter.go(content);
									} else {
										matrixGenerationService.getMatrixGenerationTaskRun(Authentication.getInstance().getAuthenticationToken(), 
												matrixGenerationTask.getTask(), 
												new AsyncCallback<MatrixGenerationTaskRun>() {
													@Override
													public void onFailure(Throwable caught) {
														caught.printStackTrace();
													}
					
													@Override
													public void onSuccess(MatrixGenerationTaskRun matrixGenerationTask) {
														TaskStageEnum taskStage = matrixGenerationTask.getTask().getTaskStage().getTaskStageEnum();
														switch(taskStage) {
														case PREPROCESS_TEXT:
															if (preprocessMatrixGenerationPresenter == null) {
																preprocessMatrixGenerationPresenter = 									
																		new PreprocessMatrixGenerationPresenter(eventBus, 
																				new PreprocessMatrixGenerationView(), matrixGenerationService);
															}
															preprocessMatrixGenerationPresenter.go(content, matrixGenerationTask);
															break;
														case LEARN_TERMS:
															if (learnMatrixGenerationPresenter == null) {
																learnMatrixGenerationPresenter = new 
																		LearnMatrixGenerationPresenter(eventBus, 
																				new LearnMatrixGenerationView(), matrixGenerationService);
															}
															learnMatrixGenerationPresenter.go(content, matrixGenerationTask);
															break;
														case REVIEW_TERMS:
															if (reviewMatrixGenerationPresenter == null) {
																reviewMatrixGenerationPresenter = 
																		new ReviewMatrixGenerationPresenter(eventBus, 
																				new ReviewMatrixGenerationView(), matrixGenerationService);
															}
															reviewMatrixGenerationPresenter.go(content, matrixGenerationTask);
															break;
														case PARSE_TEXT:
															if (parseMatrixGenerationPresenter == null) {
																parseMatrixGenerationPresenter = new ParseMatrixGenerationPresenter(
																		eventBus, new ParseMatrixGenerationView(), matrixGenerationService);
															}
															parseMatrixGenerationPresenter.go(content, matrixGenerationTask);
															break;
														case OUTPUT:
															if (outputMatrixGenerationPresenter == null) {
																outputMatrixGenerationPresenter = new 
																		OutputMatrixGenerationPresenter(eventBus, 
																				new OutputMatrixGenerationView(), fileService, matrixGenerationService);
															}
															outputMatrixGenerationPresenter.go(content, matrixGenerationTask);
															break;
														default:
															if (inputMatrixGenerationPresenter == null) {
																inputMatrixGenerationPresenter = new InputMatrixGenerationPresenter(
																		eventBus, new InputMatrixGenerationView(), fileService, matrixGenerationService);
															}
															inputMatrixGenerationPresenter.go(content);
															break;
														}
													}
											});
									}
								}
							});
						}
					}
				});
				break;
			case TREE_GENERATION:
				GWT.runAsync(new RunAsyncCallback() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}
					public void onSuccess() {
						if (treeGenerationPresenter == null) {
							treeGenerationPresenter = new TreeGenerationPresenter(eventBus, 
									new TreeGenerationView());
						}
						treeGenerationPresenter.go(content);
					}
				});
				break;
			case TAXONOMY_COMPARISON:
				GWT.runAsync(new RunAsyncCallback() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}
					public void onSuccess() {
						if (taxonomyComparisonPresenter == null) {
							taxonomyComparisonPresenter = new TaxonomyComparisonPresenter(eventBus, 
									new TaxonomyComparisonView());
						}
						taxonomyComparisonPresenter.go(content);
					}
				});
				break;
			case VISUALIZATION:
				GWT.runAsync(new RunAsyncCallback() {
					public void onFailure(Throwable caught) {
						caught.printStackTrace();
					}
					public void onSuccess() {
						if (visualizationPresenter == null) {
							visualizationPresenter = new VisualizationPresenter(eventBus, 
									new VisualizationView());
						}
						visualizationPresenter.go(content);
					}
				});
				break;
			default:
			}
		}
	}

	private void presentHeader(boolean authenticated) {
		if(authenticated) {
			if(loggedInHeaderPresenter == null)
				loggedInHeaderPresenter = new LoggedInHeaderPresenter(eventBus, new LoggedInHeaderView(), taskService, taskManager);
			loggedInHeaderPresenter.go(header);
		} else {
			if(loggedOutHeaderPresenter == null)
				loggedOutHeaderPresenter = new LoggedOutHeaderPresenter(eventBus, new LoggedOutHeaderView(), 
						authenticationService);
			loggedOutHeaderPresenter.go(header);
		}
	}

	private void presentFooter() {
		if(footerPresenter == null)
			footerPresenter = new FooterPresenter(eventBus, new FooterView());
		footerPresenter.go(footer);
	}
	
}
