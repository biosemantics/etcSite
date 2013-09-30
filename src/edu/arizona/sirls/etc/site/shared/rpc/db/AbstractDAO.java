package edu.arizona.sirls.etc.site.shared.rpc.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public abstract class AbstractDAO {

	private String databaseName;
	private String databaseUser;
	private String databasePassword;
	protected Connection connection;

	public AbstractDAO() throws IOException, ClassNotFoundException {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Properties properties = new Properties(); 
		properties.load(loader.getResourceAsStream("config.properties"));
		this.databaseName = properties.getProperty("databaseName");
		this.databaseUser = properties.getProperty("databaseUser");
		this.databasePassword = properties.getProperty("databasePassword");
		Class.forName("com.mysql.jdbc.Driver");
	}
	
	public PreparedStatement executeSQL(String sql) throws SQLException {
		PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		preparedStatement.execute();
		return preparedStatement;
	}
	
	public PreparedStatement prepareStatement(String sql) throws SQLException {
		return connection.prepareStatement(sql);
	}
	
	public void openConnection() throws SQLException {
		//this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + databaseName, databaseUser, databasePassword);
		this.connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + databaseName + 
				"?connecttimeout=0&sockettimeout=0&autoreconnect=true", 
				databaseUser, databasePassword);
	}
	
	public void closeConnection() throws SQLException {
		this.connection.close();
	}
}