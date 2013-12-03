package edu.arizona.sirls.etc.site.client.view.taskManager;

import java.util.Set;

import edu.arizona.sirls.etc.site.shared.rpc.db.ShortUser;
import edu.arizona.sirls.etc.site.shared.rpc.db.Task;

public class TaskData {

	private Task task;
	private Set<ShortUser> invitees;
	
	public TaskData(Task task, Set<ShortUser> invitees) {
		super();
		this.task = task;
		this.invitees = invitees;
	}
	public Task getTask() {
		return task;
	}
	public void setTask(Task task) {
		this.task = task;
	}
	public Set<ShortUser> getInvitees() {
		return invitees;
	}
	public void setInvitees(Set<ShortUser> invitees) {
		this.invitees = invitees;
	}
	
	
	
}
