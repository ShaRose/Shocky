package pl.shockah.shocky.sql;

import org.json.JSONObject;
import pl.shockah.HTTPQuery;
import pl.shockah.shocky.Data;

public class SQL {
	public static String raw(String query) {return queryRaw(query);}
	public static JSONObject select(QuerySelect query) {return queryJSON(query.getSQLQuery());}
	public static String insert(QueryInsert query) {return queryInsertId(query.getSQLQuery());}
	public static String delete(QueryDelete query) {return queryRaw(query.getSQLQuery());}
	public static String update(QueryUpdate query) {return queryRaw(query.getSQLQuery());}
	
	public static String queryRaw(String query) {
		HTTPQuery q = new HTTPQuery(Data.config.getString("main-sqlurl"),HTTPQuery.Method.POST);
		q.connect(true,true);
		q.write(HTTPQuery.parseArgs("type","raw","q",query,"eval",getEval()));
		String s = q.readWhole();
		q.close();
		return s;
	}
	public static String queryInsertId(String query) {
		HTTPQuery q = new HTTPQuery(Data.config.getString("main-sqlurl"),HTTPQuery.Method.POST);
		q.connect(true,true);
		q.write(HTTPQuery.parseArgs("type","insertid","q",query,"eval",getEval()));
		String s = q.readWhole();
		q.close();
		return s;
	}
	public static JSONObject queryJSON(String query) {
		HTTPQuery q = new HTTPQuery(Data.config.getString("main-sqlurl"),HTTPQuery.Method.POST);
		q.connect(true,true);
		q.write(HTTPQuery.parseArgs("type","json","q",query,"eval",getEval()));
		try {
			JSONObject j = new JSONObject(q.readWhole());
			q.close();
			return j;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public static String getTable(String name) {
		return Data.config.getString("main-sqlprefix")+name;
	}
	
	private static String getEval() {
		String host = Data.config.getString("main-sqlhost").replace("\"","\\\"");
		String user = Data.config.getString("main-sqluser").replace("\"","\\\"");
		String pass = Data.config.getString("main-sqlpass").replace("\"","\\\"");
		String db = Data.config.getString("main-sqldb").replace("\"","\\\"");
		return "$db = array(\"host\"=>\""+host+"\",\"user\"=>\""+user+"\",\"pass\"=>\""+pass+"\",\"db\"=>\""+db+"\");";
	}
}