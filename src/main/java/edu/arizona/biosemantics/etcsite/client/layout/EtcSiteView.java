package edu.arizona.biosemantics.etcsite.client.layout;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import edu.arizona.biosemantics.etcsite.client.menu.IMenuView;
import edu.arizona.biosemantics.etcsite.client.menu.MenuView;

public class EtcSiteView extends Composite implements IEtcSiteView {

	private static EtcSiteUiBinder uiBinder = GWT.create(EtcSiteUiBinder.class);

	interface EtcSiteUiBinder extends UiBinder<Widget, EtcSiteView> {
	}
	
	@UiField
	SimplePanel topPanel;

	@UiField
	SimplePanel menuPanel;
	
	@UiField
	FocusPanel contentPanel;

	@UiField
	DockLayoutPanel dockLayoutPanel;
	
	@UiField
	FocusPanel headerPanel;
	
	@UiField
	ScrollPanel contentScrollPanel;
	
	private Presenter presenter;
	
	public EtcSiteView() {
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	@Override
	public void setTop(IsWidget content) {
		topPanel.setWidget(content);
	}
	
	@Override
	public void setContent(IsWidget content) {
		contentPanel.setWidget(content);
	}
	 
	@Override
	public void setMenu(IsWidget menu) {
		menuPanel.setWidget(menu);
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public AcceptsOneWidget getContentContainer() {
		return this.contentPanel;
	}

	@Override
	public AcceptsOneWidget getMenuContainer() {
		return this.menuPanel;
	}

	@Override
	public AcceptsOneWidget getTopContainer() {
		return this.topPanel;
	}

	@Override
	public void setHeaderSize(int size) {
		dockLayoutPanel.setWidgetSize(headerPanel, size);
	}
		
	@UiHandler("headerPanel") 
	public void onMouseOverMenu(MouseOverEvent event) {
		if(menuPanel.getWidget() instanceof IMenuView) {
			expandMenu();
		}
	}
	
	@UiHandler("headerPanel") 
	public void onMouseOutMenu(MouseOutEvent event) {
		if(menuPanel.getWidget() instanceof IMenuView) {
			//not in the area above of browser content, e.g. tabls, url
			//if(event.getY() >= 0) {
			//	collapseMenu();
			//}
			/*System.out.println("x/y" + event.getX() + " " +  event.getY());
			System.out.println("relative x/y" + event.getRelativeX(this.getElement()) + " " +  event.getRelativeY(this.getElement()));
			System.out.println("client x/y" + event.getClientX() + " " +  event.getClientY());
			System.out.println("screen x/y" + event.getScreenX() + " " +  event.getScreenY()); */
			collapseMenu();
		}
	}
	
	/**
	 * Necessary, in case mouse leaves menu via browser menu and comes back to content without crossing the menu again 
	 * e.g. dual-screen can move mouse around
	 * @param event
	 */
	/*@UiHandler("contentPanel") 
	public void onMouseOverContent(MouseOverEvent event) {
		if(menuPanel.getWidget() instanceof IMenuView) {
			System.out.println("mouse over content");
			collapseMenu();
		}
	}*/

	private void collapseMenu() {
		dockLayoutPanel.forceLayout(); //makes fast mouse movement not to collapse the menu without animation (for some reason)
		dockLayoutPanel.setWidgetSize(headerPanel, 38);
		dockLayoutPanel.animate(500);
	}
	
	private void expandMenu() {
		dockLayoutPanel.forceLayout(); //makes fast mouse movement not to expand the menu without animation (for some reason)
		dockLayoutPanel.setWidgetSize(headerPanel, 100);
		dockLayoutPanel.animate(500);
	}
}
