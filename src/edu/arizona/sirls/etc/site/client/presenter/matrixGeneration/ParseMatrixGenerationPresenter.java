package edu.arizona.sirls.etc.site.client.presenter.matrixGeneration;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Widget;

import edu.arizona.sirls.etc.site.client.Authentication;
import edu.arizona.sirls.etc.site.client.event.TaskManagerEvent;
import edu.arizona.sirls.etc.site.client.event.matrixGeneration.OutputMatrixGenerationEvent;
import edu.arizona.sirls.etc.site.client.event.matrixGeneration.ReviewMatrixGenerationEvent;
import edu.arizona.sirls.etc.site.client.view.LoadingPopup;
import edu.arizona.sirls.etc.site.shared.rpc.IMatrixGenerationServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.ITaskServiceAsync;
import edu.arizona.sirls.etc.site.shared.rpc.db.MatrixGenerationConfiguration;
import edu.arizona.sirls.etc.site.shared.rpc.matrixGeneration.ParseInvocation;

public class ParseMatrixGenerationPresenter {

	public interface Display {
		//Button getNextButton();
		Widget asWidget();
		Anchor getTaskManagerAnchor();
		void setResumableStatus();
		HasClickHandlers getResumableClickable();
		void setNonResumableStatus();
	}

	private Display display;
	private HandlerManager eventBus;
	private IMatrixGenerationServiceAsync matrixGenerationService;
	private MatrixGenerationConfiguration matrixGenerationConfiguration;
	private LoadingPopup loadingPopup = new LoadingPopup();
	private ITaskServiceAsync taskService;

	public ParseMatrixGenerationPresenter(HandlerManager eventBus,
			Display display, IMatrixGenerationServiceAsync matrixGenerationService, 
			ITaskServiceAsync taskService) {
		this.eventBus = eventBus;
		this.display = display;
		this.matrixGenerationService = matrixGenerationService;
		this.taskService = taskService;
		bind();
	}

	private void bind() {
		display.getTaskManagerAnchor().addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				eventBus.fireEvent(new TaskManagerEvent());
			}
		});
		/*display.getNextButton().setEnabled(false);
		display.getNextButton().addClickHandler(new ClickHandler() { 
			@Override
			public void onClick(ClickEvent event) { 
				eventBus.fireEvent(new OutputMatrixGenerationEvent());
			}
		});*/
	}

	public void go(final HasWidgets content, MatrixGenerationConfiguration matrixGenerationConfiguration) {
		loadingPopup.start();
		display.setNonResumableStatus();
		this.matrixGenerationConfiguration = matrixGenerationConfiguration;
		matrixGenerationService.parse(Authentication.getInstance().getAuthenticationToken(), matrixGenerationConfiguration, new AsyncCallback<ParseInvocation>() {
			@Override
			public void onFailure(Throwable caught) {
				caught.printStackTrace();
				loadingPopup.stop();
			}
			@Override
			public void onSuccess(ParseInvocation result) {		
				content.clear();
				content.add(display.asWidget());
				loadingPopup.stop();
			}
		});
		
		Timer timer = new Timer() {
	        public void run() {
	        	refresh();
	        }
		};
		timer.scheduleRepeating(5000);
	}
	
	private void refresh() {
		taskService.isResumable(Authentication.getInstance().getAuthenticationToken(), 
				matrixGenerationConfiguration.getTask(),
				new AsyncCallback<Boolean>() {
				@Override
				public void onFailure(Throwable caught) {
					caught.printStackTrace();
				}
				@Override
				public void onSuccess(Boolean isResumable) {
					if(isResumable) {
						display.setResumableStatus();
						display.getResumableClickable().addClickHandler(new ClickHandler() {
							@Override
							public void onClick(ClickEvent event) {
								eventBus.fireEvent(new OutputMatrixGenerationEvent());
							}
						});
					}
				}
		});
	}

}
