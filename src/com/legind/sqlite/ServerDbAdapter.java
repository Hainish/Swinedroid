package com.legind.sqlite;

import android.content.ContentValues;
import android.content.Context;

public class ServerDbAdapter extends DbAdapter{
	
	public static final String KEY_HOST = "host";
	public static final String KEY_PORT = "port";
	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";
	
	private static final String DATABASE_TABLE = "servers";
	private static final String[] FIELDS_STRING = {KEY_HOST, KEY_PORT, KEY_USERNAME, KEY_PASSWORD};
	
	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx the Context within which to work
	 */
	public ServerDbAdapter(Context ctx) {
		super(ctx, DATABASE_TABLE, FIELDS_STRING);
	}
	
	
	/**
	 * Create a new server using the host, port, username, and password provided. If the server is
	 * successfully created return the new rowId for that server, otherwise return
	 * a -1 to indicate failure.
	 * 
	 * @param host the hostname of the server
	 * @param port the port for which the swinedroid server is connected
	 * @param username the username for the swinedroid server
	 * @param password the password for the swinedroid server
	 * @return rowId or -1 if failed
	 */
	public long createServer(String host, int port, String username, String password) {
	    ContentValues initialValues = new ContentValues();
	    if(host.length() > 128)
	    	host = host.substring(0,127);
	    if(port > 65535 || port < 1)
	    	port = 65535;
	    if(username.length() > 128)
	    	username = username.substring(0,127);
	    if(password.length() > 128)
	    	password = password.substring(0,127);
	    initialValues.put(KEY_HOST, host);
	    initialValues.put(KEY_PORT, port);
	    initialValues.put(KEY_USERNAME, username);
	    initialValues.put(KEY_PASSWORD, password);
	    return super.mDb.insert(DATABASE_TABLE, null, initialValues);
	}
	
	/**
	 * Update the note using the details provided. The note to be updated is
	 * specified using the rowId, and it is altered to use the title and body
	 * values passed in
	 * 
	 * @param rowId id of server to update
	 * @param title value to set server title to
	 * @param body value to set server body to
	 * @return true if the server was successfully updated, false otherwise
	 */
	public boolean updateServer(long rowId, String host, int port, String username, String password) {
	    ContentValues args = new ContentValues();
	    if(host.length() > 128)
	    	host = host.substring(0,127);
	    if(port > 65535 || port < 1)
	    	port = 65535;
	    if(username.length() > 128)
	    	username = username.substring(0,127);
	    if(password.length() > 128)
	    	password = password.substring(0,127);
	    args.put(KEY_HOST, host);
	    args.put(KEY_PORT, port);
	    args.put(KEY_USERNAME, username);
	    args.put(KEY_PASSWORD, password);
	
	    return mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
}
