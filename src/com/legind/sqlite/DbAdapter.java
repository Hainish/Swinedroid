package com.legind.sqlite;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbAdapter {
	public static final String KEY_ROWID = "_id";
	
	private DatabaseHelper mDbHelper;
	protected SQLiteDatabase mDb;
	
	private static final String TAG = "DbAdapter";
	
	private static final String DATABASE_NAME = "data";
	private static final int DATABASE_VERSION = 14;
	private String dbTable;
	private String[] fieldsString;
	static final String HEXES = "0123456789ABCDEF";

	/**
	 * Database creation sql statement
	 */
	private static final String[] DATABASE_CREATE_STATEMENTS = {
		"create table alerts (_id integer primary key autoincrement, sid int not null, cid int not null, ip_src blob not null, ip_dst blob not null, sig_priority smallint not null, sig_name varchar not null, timestamp varchar(19) not null);",
		"create table servers (_id integer primary key autoincrement, host varchar not null, port int not null, username varchar not null, password varchar not null, md5 varchar, sha1 varchar);"
	};
	
	private static final String[] DATABASE_UPGRADE_STATEMENTS = {
		"DROP TABLE IF EXISTS alerts;",
		DATABASE_CREATE_STATEMENTS[0]
	};

	private final Context mCtx;
	
	private class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			for(String dbCreateStatement : DATABASE_CREATE_STATEMENTS)
			db.execSQL(dbCreateStatement);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
			for(String dbUpgradeStatement : DATABASE_UPGRADE_STATEMENTS)
				db.execSQL(dbUpgradeStatement);
		}
	}
	
	/**
	 * Constructor - takes the context to allow the database to be
	 * opened/created
	 * 
	 * @param ctx the Context within which to work
	 * @param databaseVersion the version of the child table
	 * @param databaseCreate the create statement
	 * @param databaseTable the table of the child
	 */
	public DbAdapter(Context ctx, String dbTableLocal, String[] fieldsStringLocal) {
		dbTable = dbTableLocal;
		fieldsString = fieldsStringLocal;
	    mCtx = ctx;
	}
	
	/**
	 * Open the database. If it cannot be opened, try to create a new
	 * instance of the database. If it cannot be created, throw an exception to
	 * signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an
	 *         initialization call)
	 * @throws SQLException if the database could be neither opened or created
	 */
	public DbAdapter open() throws SQLException {
	    mDbHelper = new DatabaseHelper(mCtx);
	    mDb = mDbHelper.getWritableDatabase();
	    return this;
	}
	
	public void close() {
	    mDbHelper.close();
	}
	
	/**
	 * Delete the row with the given rowId
	 * 
	 * @param rowId id of row to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean delete(long rowId) {
	    return mDb.delete(dbTable, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	/**
	 * Delete all rows in table
	 * 
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteAll() {
	    return mDb.delete(dbTable, null, null) > 0;
	}
	
	/**
	 * Return a Cursor over the list of all rows in the table
	 * 
	 * @return Cursor over all alerts
	 */
	public Cursor fetchAll() {
		String[] fieldsWithRow = new String[fieldsString.length+1];
		fieldsWithRow[0] = KEY_ROWID;
		System.arraycopy(fieldsString, 0, fieldsWithRow, 1, fieldsString.length);
	    return mDb.query(dbTable, fieldsWithRow, null, null, null, null, null);
	}
	
	/**
	 * Return a Cursor positioned at the row that matches the given rowId
	 * 
	 * @param rowId id of alert to retrieve
	 * @return Cursor positioned to matching row, if found
	 * @throws SQLException if row could not be found/retrieved
	 */
	public Cursor fetch(long rowId) throws SQLException {
		String[] fieldsWithRow = new String[fieldsString.length+1];
		fieldsWithRow[0] = KEY_ROWID;
		System.arraycopy(fieldsString, 0, fieldsWithRow, 1, fieldsString.length);
	
	    Cursor mCursor =
	
	            mDb.query(true, dbTable, fieldsWithRow, KEY_ROWID + "=" + rowId, null,
	                    null, null, null, null);
	    if (mCursor != null) {
	        mCursor.moveToFirst();
	    }
	    return mCursor;
	
	}
	
	public static String getHex( byte [] raw ) {
		if ( raw == null ) {
			return null;
		}
		final StringBuilder hex = new StringBuilder( 2 * raw.length );
		for ( final byte b : raw ) {
			hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}
}