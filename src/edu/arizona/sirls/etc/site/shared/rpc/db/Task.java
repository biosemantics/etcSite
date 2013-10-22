package edu.arizona.sirls.etc.site.shared.rpc.db;

import java.io.Serializable;
import java.util.Date;

public class Task implements Serializable  {

	private static final long serialVersionUID = 5053756810897454852L;
	private int id;
	private String name;
	private TaskType taskType;
	private TaskStage taskStage;
	private Configuration configuration;
	private User user;
	private boolean resumable;
	private boolean complete;
	private Date completed;
	private Date created;
	
	public Task() { }
	
	public Task(int id, String name, TaskType taskType, TaskStage taskStage, Configuration configuration, User user, boolean resumable, boolean complete, Date completed, Date created) {
		super();
		this.id = id;
		this.name = name;
		this.taskType = taskType;
		this.taskStage = taskStage;
		this.configuration = configuration;
		this.user = user;
		this.resumable = resumable;
		this.complete = complete;
		this.completed = completed;
		this.created = created;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TaskType getTaskType() {
		return taskType;
	}

	public void setTaskType(TaskType taskType) {
		this.taskType = taskType;
	}

	public TaskStage getTaskStage() {
		return taskStage;
	}

	public void setTaskStage(TaskStage taskStage) {
		this.taskStage = taskStage;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public boolean isResumable() {
		return resumable;
	}

	public void setResumable(boolean resumable) {
		this.resumable = resumable;
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	public Date getCompleted() {
		return completed;
	}

	public void setCompleted(Date completed) {
		this.completed = completed;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

}
