package edu.arizona.sirls.etc.site.client.view.users;

import java.util.List;
import java.util.Set;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;

import edu.arizona.sirls.etc.site.shared.rpc.db.ShortUser;
import edu.arizona.sirls.etc.site.shared.rpc.db.Task;

public class UsersViewImpl extends Composite implements UsersView {

	private static UsersViewUiBinder uiBinder = GWT.create(UsersViewUiBinder.class);

	@UiTemplate("UsersView.ui.xml")
	interface UsersViewUiBinder extends UiBinder<Widget, UsersViewImpl> {
	}
		
	@UiField(provided = true)
	CellList<ShortUser> usersList;
	//@UiField
	//SuggestBox suggestBox;
	
	private Presenter presenter;
	private MultiSelectionModel<ShortUser> selectionModel;
	private ListDataProvider<ShortUser> dataProvider;
	private ProvidesKey<ShortUser> userKeyProvider = new ProvidesKey<ShortUser>() {
		@Override
		public Object getKey(ShortUser item) {
			return item == null ? null : item.getId();
		}
	};
	
	public UsersViewImpl() {
		dataProvider = new ListDataProvider<ShortUser>();
		usersList = createUsersList();
	    dataProvider.addDataDisplay(usersList);	
		initWidget(uiBinder.createAndBindUi(this));
	}

	private CellList<ShortUser> createUsersList() {
		usersList = new CellList<ShortUser>(new ShortUserCell(), userKeyProvider);
		selectionModel = new MultiSelectionModel<ShortUser>(userKeyProvider);
		usersList.setSelectionModel(selectionModel);
	    return usersList;
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void setUsers(List<ShortUser> users) {
		List<ShortUser> tasksList = dataProvider.getList();
		tasksList.clear();
		tasksList.addAll(users);
	}

	@Override
	public Set<ShortUser> getSelectedUsers() {
		return selectionModel.getSelectedSet();
	}

	@Override
	public void setSelectedUsers(Set<ShortUser> selectedUsers) {
		for(ShortUser shortUser : selectedUsers) {
			selectionModel.setSelected(shortUser, true);
		}
	}

}
