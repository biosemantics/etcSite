package edu.arizona.sirls.etc.site.shared.rpc.db;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class DatasetPrefixDAO {

	private static DatasetPrefixDAO instance;

	public static DatasetPrefixDAO getInstance() {
		if(instance == null)
			instance = new DatasetPrefixDAO();
		return instance;
	}

	public DatasetPrefix getDatasetPrefix(String prefix) throws SQLException, ClassNotFoundException, IOException {
		DatasetPrefix datasetPrefix = null;
		Query query = new Query("SELECT * FROM datasetprefixes WHERE prefix = '" + prefix + "'");
		ResultSet result = query.execute();
		while(result.next()) {
			prefix = result.getString(1);
			String glossaryVersion = result.getString(2);
			int otoId = result.getInt(3);
			Date created = result.getTimestamp(4);
			datasetPrefix = new DatasetPrefix(prefix, glossaryVersion, otoId, created);
		}
		query.close();
		return datasetPrefix;
	}

}
