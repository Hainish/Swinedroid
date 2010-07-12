package com.legind.sqlite;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.ListIterator;

import android.content.ContentValues;
import android.content.Context;

import com.legind.swinedroid.xml.AlertListXMLElement;

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
	public long createAlert(long sid, long cid, InetAddress ipSrc, InetAddress ipDst, byte sigPriority, String sigName, Timestamp timestamp) {
	    ContentValues initialValues = new ContentValues();
	    String timestampString;
	    timestampString = timestamp.toString().substring(0,19);
	    initialValues.put(KEY_SID, sid);
	    initialValues.put(KEY_CID, cid);
	    initialValues.put(KEY_IP_SRC, ipSrc.getAddress());
	    initialValues.put(KEY_IP_DST, ipDst.getAddress());
	    initialValues.put(KEY_SIG_PRIORITY, sigPriority);
	    initialValues.put(KEY_SIG_NAME, sigName);
	    initialValues.put(KEY_TIMESTAMP, timestampString);
	    return super.mDb.insert(DATABASE_TABLE, null, initialValues);
	}
	
	/**
	 * Create new alerts from a linked list of AlertListXMLElements.  If the sql
	 * statement is executed, return true, otherwise return false.
	 * 
	 * @param alertList a linked list of AlertListXMLElements
	 * @return true if sql is executed, false otherwise
	 */

	public Boolean createAlertsFromAlertList(LinkedList<AlertListXMLElement> alertList) {
		// if the alertList is empty, exit with -1
		if(alertList.isEmpty())
			return false;
		StringBuffer insertStringBuffer = new StringBuffer("INSERT INTO alerts (sid, cid, ip_src, ip_dst, sig_priority, sig_name, timestamp) SELECT sid, cid, ip_src, ip_dst, sig_priority, sig_name, timestamp FROM ("); 
		// iterate through the list of alerts, preparing a set of properties, send them to the database
		ListIterator<AlertListXMLElement> itr = alertList.listIterator();
		AlertListXMLElement firstAlertListXMLElement = (AlertListXMLElement) itr.next();
		insertStringBuffer.append("SELECT " + String.valueOf(firstAlertListXMLElement.sid) + " AS sid, " + String.valueOf(firstAlertListXMLElement.cid) + " AS cid, x'" + getHex(firstAlertListXMLElement.ipSrc.getAddress()) + "' AS ip_src, x'" + getHex(firstAlertListXMLElement.ipDst.getAddress()) + "' AS ip_dst, " + String.valueOf(firstAlertListXMLElement.sigPriority) + " AS sig_priority, \"" + firstAlertListXMLElement.sigName.replace("\"","\"\"") + "\" AS sig_name, \"" + firstAlertListXMLElement.timestamp.toString().substring(0,19) + "\" AS timestamp, " + String.valueOf(itr.nextIndex()) + " AS sort_index");
		while(itr.hasNext()){
			AlertListXMLElement thisAlertListXMLElement = (AlertListXMLElement) itr.next();
			insertStringBuffer.append(" UNION SELECT " + String.valueOf(thisAlertListXMLElement.sid) + " AS sid, " + String.valueOf(thisAlertListXMLElement.cid) + " AS cid, x'" + getHex(thisAlertListXMLElement.ipSrc.getAddress()) + "' AS ip_src, x'" + getHex(thisAlertListXMLElement.ipDst.getAddress()) + "' AS ip_dst, " + String.valueOf(thisAlertListXMLElement.sigPriority) + " AS sig_priority, \"" + thisAlertListXMLElement.sigName.replace("\"","\"\"") + "\" AS sig_name, \"" + thisAlertListXMLElement.timestamp.toString().substring(0,19) + "\" AS timestamp, " + String.valueOf(itr.nextIndex()) + " AS sort_index");
		}
		insertStringBuffer.append(") ORDER BY sort_index ASC");
		super.mDb.execSQL(insertStringBuffer.toString());
		return true;
    }

}
