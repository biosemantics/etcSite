package edu.arizona.biosemantics.etcsite.client.content.treeGeneration;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import edu.arizona.biosemantics.etcsite.client.common.ImageLabel;
import edu.arizona.biosemantics.etcsite.shared.model.treegeneration.TaskStageEnum;

public class SubMenu extends Composite {

	private static SubmenuUiBinder uiBinder = GWT.create(SubmenuUiBinder.class);

	interface SubmenuUiBinder extends UiBinder<Widget, SubMenu> {
	}
	
	@UiField
	ImageLabel input;
	
	@UiField
	ImageLabel view;
	
	public SubMenu() {
		initWidget(uiBinder.createAndBindUi(this));
	}

	public void setStep(TaskStageEnum taskStage) {
		switch(taskStage) {
		case INPUT:
			input.setImage("images/Enumeration_1.gif");
			break;
		case VIEW:
			view.setImage("images/Enumeration_2.gif");
			break;
		default:
			input.setImage("images/Enumeration_1.gif");
		}
	}

}
