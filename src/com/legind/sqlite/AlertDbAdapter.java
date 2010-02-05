package com.legind.sqlite;

import java.sql.Timestamp;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class AlertDbAdapter extends DbAdapter{
	
	public static final String KEY_SID = "sid";
	public static final String KEY_CID = "cid";
	public static final String KEY_IP_SRC = "ip_src";
	public static final String KEY_IP_DST = "ip_dst";
	public static final String KEY_SIG_PRIORITY = "sig_priority";
	public static final String KEY_SIG_NAME = "sig_priority";
	public static final String KEY_TIMESTAMP = "timestamp";

	private SQLiteDatabase mDb;
	
	/**
	 * Database creation sql statement
	 */
	private static final String DATABASE_CREATE =
	        "create table alerts (_id integer primary key autoincrement, "
	                + "sid int not null, cid int not null, ip_src int not null, ip_dst int not null, sig_priority smallint not null, sig_name varchar not null, timestamp varchar not null);";
	
	private static final String DATABASE_TABLE = "alerts";
	private static final int DATABASE_VERSION = 0;
	private static final String[] FIELDS_STRING = {KEY_ROWID, KEY_SID, KEY_CID, KEY_IP_SRC, KEY_IP_DST, KEY_SIG_PRIORITY, KEY_SIG_NAME, KEY_TIMESTAMP};
	
	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx the Context within which to work
	 */
	public AlertDbAdapter(Context ctx) {
		super(ctx, DATABASE_VERSION, DATABASE_CREATE, DATABASE_TABLE, FIELDS_STRING);
	}
	
	
	/**
	 * Create a new alert using the host, port, username, and password provided. If the alert is
	 * successfully created return the new rowId for that alert, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param host the hostname of the alert
	 * @param port the port for which the swinedroid alert is connected
	 * @param username the username for the swinedroid alert
	 * @param password the password for the swinedroid alert
	 * @return rowId or -1 if failed
	 */
	public long createAlert(long sid, long cid, long ipSrc, long ipDst, byte sigPriority, String sigName, Timestamp timestamp) {
	    ContentValues initialValues = new ContentValues();
	    String timestampString;
	    timestampString = timestamp.toString();
	    initialValues.put(KEY_SID, sid);
	    initialValues.put(KEY_CID, cid);
	    initialValues.put(KEY_IP_SRC, ipSrc);
	    initialValues.put(KEY_IP_DST, ipDst);
	    initialValues.put(KEY_SIG_PRIORITY, sigPriority);
	    initialValues.put(KEY_SIG_NAME, sigName);
	    initialValues.put(KEY_TIMESTAMP, timestampString);
	    return mDb.insert(DATABASE_TABLE, null, initialValues);
	}
}
