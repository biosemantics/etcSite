package edu.arizona.sirls.etc.site.shared.rpc.db;

import java.io.Serializable;
import java.util.Date;

public class Task implements Serializable  {

	private static final long serialVersionUID = 5053756810897454852L;
	private boolean resumable;
	private String name;
	private TaskStage taskStage;
	private User user;
	private int id;
	private Date created;
	private boolean completed;
	
	public Task() { }

	public Task(int id, User user, TaskStage taskStage, String name, boolean resumable, boolean completed, Date created) {
		this.id = id;
		this.user = user;
		this.taskStage = taskStage;
		this.name = name;
		this.resumable = resumable;
		this.completed = completed;
		this.created = created;
	}

	public boolean isResumable() {
		return resumable;
	}

	public void setResumable(boolean resumable) {
		this.resumable = resumable;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TaskStage getTaskStage() {
		return taskStage;
	}

	public void setTaskStage(TaskStage taskStage) {
		this.taskStage = taskStage;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}
	
		
}