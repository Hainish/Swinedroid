package com.legind.swinedroid;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.LinearLayout.LayoutParams;

public class AlertSearch extends Activity{
	private Spinner mSpinner;
	private EditText mSearchTerm;
	private TextView mStartDateText;
	private TextView mEndDateText;
	private TextView mStartTimeText;
	private TextView mEndTimeText;
	private Button mSearchButton;
	private Button mCancelButton;
	private Long mRowId;

	private int mCurrentYear;
	private int mCurrentMonth;
	private int mCurrentDay;
	private int mCurrentHour;
	private int mCurrentMinute;
	
	private int mStartYear;
	private int mStartMonth;
	private int mStartDay;
	private int mStartHour;
	private int mStartMinute;

	private int mEndYear;
	private int mEndMonth;
	private int mEndDay;
	private int mEndHour;
	private int mEndMinute;

	static final int START_DATE_DIALOG_ID = 0;
	static final int END_DATE_DIALOG_ID = 1;
	static final int START_TIME_DIALOG_ID = 3;
	static final int END_TIME_DIALOG_ID = 4;

	private final int ACTIVITY_ALERT_LIST = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// get rid of title, also set the layout to fill parent.  this doesn't function properly in the layout XML
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.alert_search);
		getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

        // get the current date
        final Calendar c = Calendar.getInstance();
        mCurrentYear = c.get(Calendar.YEAR);
        mCurrentMonth = c.get(Calendar.MONTH);
        mCurrentDay = c.get(Calendar.DAY_OF_MONTH);
        mCurrentHour = c.get(Calendar.HOUR_OF_DAY);
        mCurrentMinute = c.get(Calendar.MINUTE);
        
		mSpinner = (Spinner) findViewById(R.id.alert_level_spinner);
	    mSearchTerm = (EditText) findViewById(R.id.search_term_edit_text);
	    mStartDateText = (TextView) findViewById(R.id.startDateText);
	    mEndDateText = (TextView) findViewById(R.id.endDateText);
	    mStartTimeText = (TextView) findViewById(R.id.startTimeText);
	    mEndTimeText = (TextView) findViewById(R.id.endTimeText);
	    mSearchButton = (Button) findViewById(R.id.search_button);
	    mCancelButton = (Button) findViewById(R.id.cancel_button);
	    // connect the spinner to the appropriate dropdown xml
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.alert_levels, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    mSpinner.setAdapter(adapter);

		if(savedInstanceState != null){
			if(!savedInstanceState.getBoolean(ServerDbAdapter.KEY_ROWID + "_null")){
				mRowId = savedInstanceState.getLong(ServerDbAdapter.KEY_ROWID);
			} else {
				mRowId = null;
			}
			// get some variables from the saved instance state.  make sure to update time *first*
			mStartYear = savedInstanceState.getInt("mStartYear");
			mStartMonth = savedInstanceState.getInt("mStartMonth");
			mStartDay = savedInstanceState.getInt("mStartDay");
			mStartHour = savedInstanceState.getInt("mStartHour");
			mStartMinute = savedInstanceState.getInt("mStartMinute");
			if(mStartYear > 0){
		        updateStartTimeText();
		        updateStartDateText();
			}
			mEndYear = savedInstanceState.getInt("mEndYear");
			mEndMonth = savedInstanceState.getInt("mEndMonth");
			mEndDay = savedInstanceState.getInt("mEndDay");
			mEndHour = savedInstanceState.getInt("mEndHour");
			mEndMinute = savedInstanceState.getInt("mEndMinute");
			if(mEndYear > 0){
		        updateEndTimeText();
		        updateEndDateText();
			}
		} else {
			mRowId = null;
		}
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ServerDbAdapter.KEY_ROWID)
					: null;
		}
		
		// set up the click listeners...
		mStartDateText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(START_DATE_DIALOG_ID);
			}
		});
		mEndDateText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(END_DATE_DIALOG_ID);
			}
		});
		// don't open a TimePicker if the date hasn't been picked yet
		mStartTimeText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(mStartYear > 0)
					showDialog(START_TIME_DIALOG_ID);
			}
		});
		mEndTimeText.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(mEndYear > 0)
					showDialog(END_TIME_DIALOG_ID);
			}
		});
		mSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// send the current state of all search elements
	        	Intent i = new Intent(AlertSearch.this, AlertList.class);
	        	i.putExtra(ServerDbAdapter.KEY_ROWID, mRowId);
	        	i.putExtra("mSpinnerText", mSpinner.getSelectedItem().toString());
	        	i.putExtra("mSearchTermText", mSearchTerm.getText().toString());
	    		i.putExtra("mStartYear", mStartYear);
	    		i.putExtra("mStartMonth", mStartMonth);
	    		i.putExtra("mStartDay", mStartDay);
	    		i.putExtra("mStartHour", mStartHour);
	    		i.putExtra("mStartMinute", mStartMinute);
	    		i.putExtra("mEndYear", mEndYear);
	    		i.putExtra("mEndMonth", mEndMonth);
	    		i.putExtra("mEndDay", mEndDay);
	    		i.putExtra("mEndHour", mEndHour);
	    		i.putExtra("mEndMinute", mEndMinute);
	        	startActivityForResult(i, ACTIVITY_ALERT_LIST);
				finish();
			}
		});
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(mRowId != null){
			outState.putBoolean(ServerDbAdapter.KEY_ROWID + "_null", false);
			outState.putLong(ServerDbAdapter.KEY_ROWID, mRowId);
		} else {
			outState.putBoolean(ServerDbAdapter.KEY_ROWID + "_null", true);
		}
		outState.putInt("mStartYear", mStartYear);
		outState.putInt("mStartMonth", mStartMonth);
		outState.putInt("mStartDay", mStartDay);
		outState.putInt("mStartHour", mStartHour);
		outState.putInt("mStartMinute", mStartMinute);
		outState.putInt("mEndYear", mEndYear);
		outState.putInt("mEndMonth", mEndMonth);
		outState.putInt("mEndDay", mEndDay);
		outState.putInt("mEndHour", mEndHour);
		outState.putInt("mEndMinute", mEndMinute);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id){
			case START_DATE_DIALOG_ID:
				return new DatePickerDialog(this,
					mStartDateSetListener,
					mCurrentYear, mCurrentMonth, mCurrentDay);
			case END_DATE_DIALOG_ID:
				return new DatePickerDialog(this,
					mEndDateSetListener,
					mCurrentYear, mCurrentMonth, mCurrentDay);
			case START_TIME_DIALOG_ID:
				return new TimePickerDialog(this,
					mStartTimeSetListener, mCurrentHour, mCurrentMinute, false);
			case END_TIME_DIALOG_ID:
				return new TimePickerDialog(this,
					mEndTimeSetListener, mCurrentHour, mCurrentMinute, false);
		}
		return null;
	}

    private void updateStartDateText() {
        mStartDateText.setText(
            new StringBuilder()
                    // Month is 0 based so add 1
                    .append(mStartMonth + 1).append("-")
                    .append(mStartDay).append("-")
                    .append(mStartYear).append(" "));
        if(mStartTimeText.length() == 0){
            mStartTimeText.setText("00:00");
            mStartHour = 0;
            mStartMinute = 0;
        }
        	
    }
    
    private void updateEndDateText() {
        mEndDateText.setText(
            new StringBuilder()
                    // Month is 0 based so add 1
                    .append(mEndMonth + 1).append("-")
                    .append(mEndDay).append("-")
                    .append(mEndYear).append(" "));
        if(mEndTimeText.length() == 0){
            mEndTimeText.setText("00:00");
            mEndHour = 0;
            mEndMinute = 0;
        }
    }

    private void updateStartTimeText() {
        mStartTimeText.setText(
            new StringBuilder()
            	.append(pad(mStartHour)).append(":")
            	.append(pad(mStartMinute)));
	}

    private void updateEndTimeText() {
        mEndTimeText.setText(
            new StringBuilder()
            	.append(pad(mEndHour)).append(":")
            	.append(pad(mEndMinute)));
	}

	private DatePickerDialog.OnDateSetListener mStartDateSetListener =
		new DatePickerDialog.OnDateSetListener() {
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				mStartYear = year;
				mStartMonth = monthOfYear;
				mStartDay = dayOfMonth;
				updateStartDateText();
			}
    };
	
	private DatePickerDialog.OnDateSetListener mEndDateSetListener =
		new DatePickerDialog.OnDateSetListener() {
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				mEndYear = year;
				mEndMonth = monthOfYear;
				mEndDay = dayOfMonth;
				updateEndDateText();
			}
    };
	    
    private TimePickerDialog.OnTimeSetListener mStartTimeSetListener =
		new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				mStartHour = hourOfDay;
				mStartMinute = minute;
				updateStartTimeText();
			}
		};
    
    private TimePickerDialog.OnTimeSetListener mEndTimeSetListener =
		new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				mEndHour = hourOfDay;
				mEndMinute = minute;
				updateEndTimeText();
			}
		};
    
    private static String pad(int c) {
        if (c >= 10)
            return String.valueOf(c);
        else
            return "0" + String.valueOf(c);
    }

}