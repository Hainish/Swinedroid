package com.legind.sqlite;

import java.sql.Timestamp;

import android.content.ContentValues;
import android.content.Context;

public class AlertDbAdapter extends DbAdapter{
	
	public static final String KEY_SID = "sid";
	public static final String KEY_CID = "cid";
	public static final String KEY_IP_SRC = "ip_src";
	public static final String KEY_IP_DST = "ip_dst";
	public static final String KEY_SIG_PRIORITY = "sig_priority";
	public static final String KEY_SIG_NAME = "sig_name";
	public static final String KEY_TIMESTAMP = "timestamp";
	
	/**
	 * Database creation sql statement
	 */
	
	private static final String DATABASE_TABLE = "alerts";
	private static final String[] FIELDS_STRING = {KEY_SID, KEY_CID, KEY_IP_SRC, KEY_IP_DST, KEY_SIG_PRIORITY, KEY_SIG_NAME, KEY_TIMESTAMP};
	
	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx the Context within which to work
	 */
	public AlertDbAdapter(Context ctx) {
		super(ctx, DATABASE_TABLE, FIELDS_STRING);
	}
	
	
	/**
	 * Create a new alert using the host, port, username, and password provided. If the alert is
	 * successfully created return the new rowId for that alert, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param sid the sid of the alert
	 * @param cid the cid of the alert
	 * @param ipSrc the source IP of the alert
	 * @param ipDst the destination IP of the alert
	 * @param sigPriority the alert level
	 * @param sigName the alert name / label
	 * @param timestamp when the alert occurred
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
	    return super.mDb.insert(DATABASE_TABLE, null, initialValues);
	}
}
