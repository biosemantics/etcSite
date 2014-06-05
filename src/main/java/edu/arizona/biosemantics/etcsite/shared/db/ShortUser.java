package edu.arizona.biosemantics.etcsite.shared.db;

import java.io.Serializable;

public class ShortUser implements Serializable {
	
	// There used to be 2 unique keys: id and name. Now the integer id is the only unique key. 
	// But to maintain compatibility, we let shortUser keep both an id and a name field. 
	// the name field is simply the id field as a string. 
	
	private static final long serialVersionUID = 9014388467462637993L;
	private int id;
	private String name;
	
	public ShortUser() { }
	
	public ShortUser(int id, String name) {
		this.id = id;
		this.name = name;
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
	
	@Override
	public int hashCode() {
		return id;
	}
	
	@Override
	public boolean equals(Object object) {
		if(object == null)
			return false;
		if (getClass() != object.getClass()) {
	        return false;
	    }
		ShortUser user = (ShortUser)object;
		if(user.getId()==this.id)
			return true;
		return false;
	}

}
