package edu.arizona.biosemantics.etcsite.client.content.home;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class HomeMainView extends Composite implements IHomeMainView {

	private static HomeMainViewUiBinder uiBinder = GWT.create(HomeMainViewUiBinder.class);

	interface HomeMainViewUiBinder extends UiBinder<Widget, HomeMainView> {
	}

	private Presenter presenter;
	
	public HomeMainView() {
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	@UiHandler("semanticMarkupButton")
	public void onSemanticMarkup(ClickEvent event) {
		presenter.onSemanticMarkup();
	}
	
	@UiHandler("matrixGenerationButton")
	public void onMatrixGeneration(ClickEvent event) {
		presenter.onMatrixGeneration();
	}
	
	@UiHandler("treeGenerationButton")
	public void onTreeGeneration(ClickEvent event) {
		presenter.onTreeGeneration();
	}
	
	@UiHandler("taxonomyComparisonButton")
	public void onTaxonomyComparison(ClickEvent event) {
		presenter.onTaxonomyComparison();
	}
	
	/*@UiHandler("visualizationButton")
	public void onVisualization(ClickEvent event) {
		presenter.onVisualization();
	} 
	
	@UiHandler("pipelineButton")
	public void onPipeline(ClickEvent event) {
		presenter.onPipeline();
	} 
	*/
	@UiHandler("ontologizeButton")
	public void onOntologize(ClickEvent event) {
		presenter.onOntologize();
	}
	
	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}
}
