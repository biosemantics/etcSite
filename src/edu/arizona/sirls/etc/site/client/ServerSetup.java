package edu.arizona.sirls.etc.site.client;

import edu.arizona.sirls.etc.site.shared.rpc.Setup;

public class ServerSetup {

	private static ServerSetup instance;
	
	public static ServerSetup getInstance() {
		if(instance == null)
			instance = new ServerSetup(); 
		return instance;
	}
	
	private Setup setup;

	private ServerSetup() { 	}
		
	public Setup getSetup() {
		return setup;
	}

	public void setSetup(Setup setup) {
		this.setup = setup;
	}
	
	public boolean hasSetup() {
		return this.setup != null;
	}
	
}