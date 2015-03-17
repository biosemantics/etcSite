package edu.arizona.biosemantics.etcsite.server.db;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import edu.arizona.biosemantics.common.log.LogLevel;
import edu.arizona.biosemantics.etcsite.server.Configuration;
import edu.arizona.biosemantics.etcsite.shared.model.ShortUser;
import edu.arizona.biosemantics.etcsite.shared.model.User;

public class UserDAO {

	public ShortUser getShortUser(int userId) {
		User user = this.getUser(userId);
		return createShortUser(user);
	}

	private ShortUser createShortUser(User user) {
		return new ShortUser(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), 
				user.getAffiliation(), user.getOpenIdProvider(), user.getOpenIdProviderId(), 
				user.getBioportalUserId(), user.getBioportalAPIKey(), user.getOtoAccountEmail(),
				user.getMatrixGenerationEmailChk(),user.getTreeGenerationEmailChk(),
				user.getTextCaptureEmailChk(),user.getTaxonomyComparisonEmailChk());
	}

	public User getUser(int id) {
		User user = null;
		try(Query query = new Query("SELECT * FROM etcsite_users WHERE id = ?")) {
			query.setParameter(1, id);
			ResultSet result = query.execute();
			while (result.next()) {
				user = createUser(result);
			}
			User usr= null;
			usr=getEmailPreference(id);
			if(usr==null )
			{
			user.setMatrixGenerationEmailChk(true);
			user.setTaxonomyComparisonEmailChk(true);
			user.setTextCaptureEmailChk(true);
			user.setTreeGenerationEmailChk(true);	
			}
			else
			{
			user.setMatrixGenerationEmailChk(usr.getMatrixGenerationEmailChk());
			user.setTaxonomyComparisonEmailChk(usr.getTaxonomyComparisonEmailChk());
			user.setTextCaptureEmailChk(usr.getTextCaptureEmailChk());
			user.setTreeGenerationEmailChk(usr.getTreeGenerationEmailChk());
			}
		}catch(Exception e) {
			log(LogLevel.ERROR, "Couldn't get user", e);
		}
		return user;
	}

	private User createUser(ResultSet result) throws SQLException {
		int id = result.getInt(1);
		String openIdProviderId = result.getString(2);
		String openIdProvider = result.getString(3);
		String password = result.getString(4);
		String firstName = result.getString(5);
		String lastName = result.getString(6);
		String email = result.getString(7);
		String affiliation = result.getString(8);
		String bioportalUserId = result.getString(9);
		String bioportalAPIKey = result.getString(10);
		String otoAccountEmail = result.getString(11);
		String otoAuthenticationToken = result.getString(12);
		Date created = result.getTimestamp(13);

		return new User(id, openIdProviderId, openIdProvider, password,
				firstName, lastName, email, affiliation, bioportalUserId,
				bioportalAPIKey, otoAccountEmail, otoAuthenticationToken, created);
	}

	public User getUser(String email) {
		User user = null;
		try(Query query = new Query("SELECT * FROM etcsite_users WHERE email = ?")) {
			query.setParameter(1, email);
			ResultSet result = query.execute();
			while (result.next()) {
				user = createUser(result);
			}
		}catch(Exception e) {
			log(LogLevel.ERROR, "Couldn't get user", e);
		}
		return user;
	}
	
	public ShortUser getShortUser(String email) {
		User user = getUser(email);
		if(user != null) {
			ShortUser shortUser = new ShortUser(user);
			return shortUser;
		}
		return null;
	}
	
	public User insert(User user) {
		User result = null;
		try(Query query = new Query(
			"INSERT INTO `etcsite_users`(`openIdProviderId`, `openidprovider`, `password`, `firstname`, `lastname`, " +
			"`email`, `affiliation`, `bioportaluserid`, `bioportalapikey`, `otoaccountemail`, `otoauthenticationtoken`) VALUES" +
			" (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");) {
			query.setParameter(1, user.getOpenIdProviderId());
			query.setParameter(2, user.getOpenIdProvider());
			query.setParameter(3, user.getPassword());
			query.setParameter(4, user.getFirstName());
			query.setParameter(5, user.getLastName());
			query.setParameter(6, user.getEmail());
			query.setParameter(7, user.getAffiliation());
			query.setParameter(8, user.getBioportalUserId());
			query.setParameter(9, user.getBioportalAPIKey());
			query.setParameter(10, user.getOtoAccountEmail());
			query.setParameter(11, user.getOtoAuthenticationToken());
			query.execute();
			ResultSet generatedKeys = query.getGeneratedKeys();
			if(generatedKeys.next()) {
				result = this.getUser(generatedKeys.getInt(1));
			}
		} catch(Exception e) {
			log(LogLevel.ERROR, "Couldn't add user", e);
		}
		
		if(result != null) {
			String filename = Configuration.fileBase + File.separator + result.getId();
			new File(filename).mkdirs();
		}
			
		return result;
	}
	
	public void update(User user) {
		try (Query query = new Query("UPDATE etcsite_users SET openidproviderid = ?, openidprovider = ?, password = ?, "
				+ "firstname = ?, lastname = ?, email = ?, affiliation = ?, bioportaluserid = ?, bioportalapikey = ?, otoaccountemail = ?, otoauthenticationtoken = ? WHERE id = ?")) {
			query.setParameter(1, user.getOpenIdProviderId());
			query.setParameter(2, user.getOpenIdProvider());
			query.setParameter(3, user.getPassword());
			query.setParameter(4, user.getFirstName());
			query.setParameter(5, user.getLastName());
			query.setParameter(6, user.getEmail());
			query.setParameter(7, user.getAffiliation());
			query.setParameter(8, user.getBioportalUserId());
			query.setParameter(9, user.getBioportalAPIKey());
			query.setParameter(10, user.getOtoAccountEmail());
			query.setParameter(11, user.getOtoAuthenticationToken());
			query.setParameter(12, user.getId());
			query.execute();
			serializeEmailPreference(user);
		} catch(Exception e) {
			log(LogLevel.ERROR, "Couldn't update matrix generation configuration", e);
		}
	}
	private void serializeEmailPreference( User user )
	{
		String fileName = user.getId()+".ser";
		//User usr= new User(Name,LastName,email,matrixGenerationEmail,textCaptureEmail,treeGenerationEmail,taxonomyComparisonEmail);
		String file= Configuration.etcFiles + File.separator + "profiles" + File.separator +  fileName;
		try(ObjectOutput output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
			output.writeObject(user);
		}
		catch(Exception ex){
			log(LogLevel.ERROR, ex.getMessage(), ex);
			 //super.doUnexpectedFailure(ex);
			 
		}
	}
	public User getEmailPreference(int userId ) {
		User usr = null;  
		try {
			String fileName = userId+".ser";
			String file= Configuration.etcFiles + File.separator + "profiles" + File.separator +  fileName;
			ObjectInput input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
			 usr = (User)input.readObject();
			return usr;
			} 
		catch(ClassNotFoundException | IOException e) {
			log(LogLevel.ERROR, e.getMessage(), e);
			// super.doUnexpectedFailure(e);
			 return usr;
		}
			
	}
	
	public List<ShortUser> getUsers() {
		List<ShortUser> result = new LinkedList<ShortUser>();
		try(Query query = new Query("SELECT * FROM etcsite_users")) {
			ResultSet resultSet = query.execute();
			while (resultSet.next()) {
				result.add(createShortUser(createUser(resultSet)));
			}
		} catch(Exception e) {
			log(LogLevel.ERROR, "Couldn't get users", e);
		}
		return result;
	}

	public List<ShortUser> getUsersWithout(int userId) {
		List<ShortUser> result = new LinkedList<ShortUser>();
		try(Query query = new Query("SELECT * FROM etcsite_users WHERE id != ?")) {
			query.setParameter(1, userId);
			ResultSet resultSet = query.execute();
			while (resultSet.next()) {
				result.add(createShortUser(createUser(resultSet)));
			}
		}catch(Exception e) {
			log(LogLevel.ERROR, "Couldn't get users without given", e);
		}
		return result;
	}

	public boolean hasUser(String email) {
		User user = this.getUser(email);
//		return this.getUser(email) != null;
		if (user !=null)
			return true;
		else return false;
	}

}
