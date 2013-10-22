package edu.arizona.sirls.etc.site.shared.rpc.db;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Share implements Serializable {

	private static final long serialVersionUID = 4818830111979976800L;
	private int id;
	private Task task;
	private List<User> invitees;
	private Date created;
	
	public Share() { }
	
	public Share(int id, Task task, List<User> invitees, Date created) {
		super();
		this.id = id;
		this.task = task;
		this.invitees = invitees;
		this.created = created;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Task getTask() {
		return task;
	}
	public void setTask(Task task) {
		this.task = task;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}

	public List<User> getInvitees() {
		return invitees;
	}

	public void setInvitees(List<User> invitees) {
		this.invitees = invitees;
	}
	
}
