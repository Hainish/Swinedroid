package com.legind.swinedroid;

import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
	    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.alert_levels, android.R.layout.simple_spinner_item);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    mSpinner.setAdapter(adapter);

		mRowId = savedInstanceState != null ? savedInstanceState
				.getLong(ServerDbAdapter.KEY_ROWID) : null;
		if (mRowId == null) {
			Bundle extras = getIntent().getExtras();
			mRowId = extras != null ? extras.getLong(ServerDbAdapter.KEY_ROWID)
					: null;
		}
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
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

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