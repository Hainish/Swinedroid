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
	private int DATABASE_VERSION;
	private String DATABASE_CREATE;
	private String DATABASE_TABLE;
	private String[] FIELDS_STRING;
	
	private final Context mCtx;
	
	private class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			
			db.execSQL(DATABASE_CREATE);
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database `" + DATABASE_TABLE + "` from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
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
	public DbAdapter(Context ctx, int databaseVersion, String databaseCreate, String databaseTable, String[] fieldsString) {
		DATABASE_TABLE = databaseTable;
		DATABASE_CREATE = databaseCreate;
		DATABASE_VERSION = databaseVersion;
		FIELDS_STRING = fieldsString;
	    this.mCtx = ctx;
	}
	
	/**
	 * Open the alert database. If it cannot be opened, try to create a new
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
	 * Delete the alert with the given rowId
	 * 
	 * @param rowId id of alert to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean delete(long rowId) {
	
	    return mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}
	
	/**
	 * Delete the alert with the given rowId
	 * 
	 * @param rowId id of alert to delete
	 * @return true if deleted, false otherwise
	 */
	public boolean deleteAll() {
	
	    return mDb.delete(DATABASE_TABLE, null, null) > 0;
	}
	
	/**
	 * Return a Cursor over the list of all alerts in the database
	 * 
	 * @return Cursor over all alerts
	 */
	public Cursor fetchAll() {
		String[] fieldsWithRow = new String[FIELDS_STRING.length+1];
		fieldsWithRow[0] = KEY_ROWID;
		System.arraycopy(FIELDS_STRING, 0, fieldsWithRow, fieldsWithRow.length, FIELDS_STRING.length);
	    return mDb.query(DATABASE_TABLE, fieldsWithRow, null, null, null, null, null);
	}
	
	/**
	 * Return a Cursor positioned at the alert that matches the given rowId
	 * 
	 * @param rowId id of alert to retrieve
	 * @return Cursor positioned to matching alert, if found
	 * @throws SQLException if note could not be found/retrieved
	 */
	public Cursor fetch(long rowId) throws SQLException {
		String[] fieldsWithRow = new String[FIELDS_STRING.length+1];
		fieldsWithRow[0] = KEY_ROWID;
		System.arraycopy(FIELDS_STRING, 0, fieldsWithRow, fieldsWithRow.length, FIELDS_STRING.length);
	
	    Cursor mCursor =
	
	            mDb.query(true, DATABASE_TABLE, fieldsWithRow, KEY_ROWID + "=" + rowId, null,
	                    null, null, null, null);
	    if (mCursor != null) {
	        mCursor.moveToFirst();
	    }
	    return mCursor;
	
	}
}