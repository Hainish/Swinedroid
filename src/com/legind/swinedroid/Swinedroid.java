package com.legind.swinedroid;

import com.legind.sqlite.ServerDbAdapter;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Swinedroid extends ListActivity {
    private static final int ACTIVITY_CREATE=0;
    private static final int ACTIVITY_EDIT=1;
    private static final int ACTIVITY_VIEW=2;
    
    private static final int INSERT_ID = Menu.FIRST;
    private static final int DELETE_ID = Menu.FIRST + 1;
    private static final int EDIT_ID = Menu.FIRST + 2;

    private ServerDbAdapter mDbHelper;
    public static ListActivity LA = null;
    
    /** Called when the activity is first created. */
    @Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	LA = this;
    	setContentView(R.layout.server_list);
    	mDbHelper = new ServerDbAdapter(this);
    	mDbHelper.open();
    	fillData();
    	registerForContextMenu(getListView());
	}

    private void fillData() {
        // Get all of the rows from the database and create the item list
    	Cursor serversCursor = mDbHelper.fetchAll();
        startManagingCursor(serversCursor);
        
        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{ServerDbAdapter.KEY_HOST};
        
        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{R.id.text1};
        
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter servers = 
        	    new SimpleCursorAdapter(this, R.layout.server_row, serversCursor, from, to);
        setListAdapter(servers);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, INSERT_ID, 0, R.string.menu_insert);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
        	case INSERT_ID:
        		createServer();
        		return true;
        }
       
        return super.onMenuItemSelected(featureId, item);
    }
	
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, EDIT_ID, 0, R.string.menu_edit);
        menu.add(0, DELETE_ID, 1, R.string.menu_delete);
	}

    @Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch(item.getItemId()) {
    	case DELETE_ID:
	        mDbHelper.delete(info.id);
	        fillData();
	        return true;
    	case EDIT_ID:
    		Intent i = new Intent(this, ServerEdit.class);
    		i.putExtra(ServerDbAdapter.KEY_ROWID, info.id);
    		startActivityForResult(i, ACTIVITY_EDIT);
		}
		return super.onContextItemSelected(item);
	}
	
    private void createServer() {
        Intent i = new Intent(this, ServerEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, ServerView.class);
        i.putExtra(ServerDbAdapter.KEY_ROWID, id);
        startActivityForResult(i, ACTIVITY_VIEW);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();
    }
}