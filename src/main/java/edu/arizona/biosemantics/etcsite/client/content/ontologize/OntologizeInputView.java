package edu.arizona.biosemantics.etcsite.client.content.ontologize;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import edu.arizona.biosemantics.common.biology.TaxonGroup;

public class OntologizeInputView extends Composite implements IOntologizeInputView {

	private static OntologizeViewUiBinder uiBinder = GWT.create(OntologizeViewUiBinder.class);

	interface OntologizeViewUiBinder extends UiBinder<Widget, OntologizeInputView> {
	}

	private Presenter presenter;
	
	@UiField
	TextBox taskNameTextBox;
	
	@UiField
	Label inputLabel;
	
	@UiField
	ListBox glossaryListBox;
	
	@UiField
	Button nextButton;
	
	@UiField
	SubMenu subMenu;
	
	/*@UiField
	RadioButton selectOntologyRadio;

	@UiField
	VerticalPanel selectOntologyPanel;
	
	@UiField
	Label ontologyLabel;
	
	@UiField
	RadioButton createOntologyRadio;*/

	@UiField
	VerticalPanel createOntologyPanel;
	
	@UiField
	TextBox ontologyPrefixTextBox;
	
	@Inject
	public OntologizeInputView() {
		super();
		initWidget(uiBinder.createAndBindUi(this));
		for(TaxonGroup taxonGroup : TaxonGroup.values()) {
			this.glossaryListBox.addItem(taxonGroup.getDisplayName());
		}
		//selectOntologyPanel.setVisible(false);
		//createOntologyPanel.setVisible(false);
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@UiHandler("inputButton") 
	public void onInputSelect(ClickEvent event) {
		presenter.onInputSelect();
	}
	
	@UiHandler("nextButton")
	public void onSearchClick(ClickEvent event) {
		presenter.onNext();
    }
	
	/*@UiHandler("selectOntologyRadio")
	public void onSelectOntologyRadio(ClickEvent event) {
		createOntologyPanel.setVisible(false);
		selectOntologyPanel.setVisible(true);
	}
	
	@UiHandler("createOntologyRadio")
	public void onCreateOntologyRadio(ClickEvent event) {
		createOntologyPanel.setVisible(true);
		selectOntologyPanel.setVisible(false);
	}
	
	@UiHandler("ontologyButton")
	public void onOntologyButton(ClickEvent event) {
		presenter.onOntologySelect();
	}*/

	@Override
	public String getTaskName() {
		return this.taskNameTextBox.getText();
	}

	@Override
	public void setFilePath(String path) {
		this.inputLabel.setText(path);
	}

	@Override
	public void setEnabledNext(boolean value) {
		this.nextButton.setEnabled(value);
	}
	
	@Override
	public void resetFields(){
		this.taskNameTextBox.setText("");
	}

	/*@Override
	public boolean isSelectOntology() {
		return selectOntologyRadio.getValue();
	}*/
	
	@Override
	public String getTaxonGroup() {
		return glossaryListBox.getItemText(glossaryListBox.getSelectedIndex());
	}

	@Override
	public String getOntologyPrefix() {
		return ontologyPrefixTextBox.getValue();
	}

	/*@Override
	public boolean isCreateOntology() {
		return createOntologyRadio.getValue();
	}

	@Override
	public void setOntologyFilePath(String path) {
		this.ontologyLabel.setText(path);
	}*/
}
