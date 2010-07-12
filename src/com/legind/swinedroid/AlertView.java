package com.legind.swinedroid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

import com.legind.Dialogs.ErrorMessageHandler;
import com.legind.sqlite.AlertDbAdapter;
import com.legind.sqlite.ServerDbAdapter;
import com.legind.ssl.CertificateInspect.CertificateInspect;
import com.legind.swinedroid.xml.AlertXMLHandler;
import com.legind.swinedroid.xml.XMLHandlerException;

public class AlertView extends Activity{
	private Long mRowId;
	private ProgressDialog pd;
	private AlertXMLHandler mAlertXMLHandler;
	private ServerDbAdapter mDbHelper;
	private long mCid;
	private long mSid;
	private String mSigName;
	private InetAddress mIpSrc;
	private InetAddress mIpDst;
	private String mDate;
	private String mTime;
	private byte mSigPriority;
	private int mProtocol;
	private String mHostname;
	private String mInterfaceName;
	private String mPayload;
	private int mSport;
	private int mDport;
	private byte mType;
	private byte mCode;
	private String mHostText;
	private int mPortInt;
	private String mUsernameText;
	private String mPasswordText;
	private final String LOG_TAG = "com.legind.swinedroid.AlertView";
	private boolean mGotAlert;
	AlertDisplayRunnable alertRunnable;
	private static final int ACTIVITY_HASH_DIALOG = 0;
	private static final String KEY_PROTOCOL = "protocol";
	private static final String KEY_HOSTNAME = "hostname";
	private static final String KEY_INTERFACE_NAME = "interface_name";
	private static final String KEY_PAYLOAD = "payload";
	private static final String KEY_SPORT = "sport";
	private static final String KEY_DPORT = "dport";
	private static final String KEY_TYPE = "type";
	private static final String KEY_CODE = "code";
	private static final int GENERAL_INFO_TABLE_ID = 1;
	private static final int IP_INFO_TABLE_ID = 2;
	private static final int PROTO_INFO_TABLE_ID = 3;
	private static final int PAYLOAD_INFO_TABLE_ID = 4;
	private ImageView alertIcon;
	private TextView alertText;
	private LayoutInflater inflater;
	private LinearLayout layout;
	private RelativeLayout relativeLayout;
	
	private class AlertDisplayRunnable implements Runnable {
		private Context mCtx;
		private final int DOCUMENT_VALID = 0;
		private final int IO_ERROR = 1;
		private final int XML_ERROR = 2;
		private final int SERVER_ERROR = 3;
		private final int CERT_ERROR = 4;
		private ErrorMessageHandler mEMH;
		
		

		/**
		 * Constructor for AlertDisplayRunnable.  Runnable becomes context-aware
		 */
		public AlertDisplayRunnable(){
			// Display all errors on the AlertList ListActivity
			Context errorMessageContext = AlertList.LA;
			mEMH = new ErrorMessageHandler(errorMessageContext,
					findViewById(R.id.server_edit_error_layout_root));
		}
		
		/**
		 * Send an XML request to the XML handler.  If an error occurs, send a message
		 * to the handler with the appropriate error code
		 */
		public void run() {
			try {
				Cursor server = mDbHelper.fetch(mRowId);
				startManagingCursor(server);
				String mMD5 = server.getString(server
						.getColumnIndexOrThrow(ServerDbAdapter.KEY_MD5));
				String mSHA1 = server.getString(server
						.getColumnIndexOrThrow(ServerDbAdapter.KEY_SHA1));
				mAlertXMLHandler.openWebTransportConnection(mHostText, mPortInt);
				CertificateInspect serverCertificateInspect = new CertificateInspect(mAlertXMLHandler.getWebTransportConnection().getServerCertificate());
				String mServerCertMD5 = serverCertificateInspect.generateFingerprint("MD5");
				String mServerCertSHA1 = serverCertificateInspect.generateFingerprint("SHA1"); 
				if(!mServerCertSHA1.equals(mSHA1) || !mServerCertMD5.equals(mMD5)){
					Message msg = Message.obtain();
					msg.setTarget(handler);
					msg.what = CERT_ERROR;
					msg.obj = new Object[]{mServerCertSHA1, mServerCertMD5, (mSHA1 == null && mMD5 == null ? false : true)};
					msg.sendToTarget();
				} else {
					// construct the GET arguments string, send it to the XML handler
					String extraArgs = "cid=" + mCid + "&sid=" + mSid;
					mAlertXMLHandler.createElement(mCtx, mUsernameText, mPasswordText, "alert", extraArgs);
					handler.sendEmptyMessage(DOCUMENT_VALID);
				}
			} catch (IOException e) {
				Log.e(LOG_TAG, e.toString());
				handler.sendEmptyMessage(IO_ERROR);
			} catch (SAXException e) {
				Log.e(LOG_TAG, e.toString());
				handler.sendEmptyMessage(XML_ERROR);
			} catch (XMLHandlerException e){
				Log.e(LOG_TAG, e.toString());
				Message msg = Message.obtain();
				msg.setTarget(handler);
				msg.what = SERVER_ERROR;
				msg.obj = e.getMessage();
				msg.sendToTarget();
			}
		}

		/**
		 *  Catch and display any errors sent to the handler, otherwise populate all alerts
		 */
		private Handler handler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				/* We must dismiss the ProgressDialogue if it is the initial alerts population,
				 * and show the previous view for the switcher for additional alerts.
				 */
				pd.dismiss();
				switch(message.what){
					case IO_ERROR:
						mEMH.DisplayErrorMessage("Could not connect to server.  Please ensure that your settings are correct and try again later.");
					break;
					case XML_ERROR:
						mEMH.DisplayErrorMessage("Server responded with an invalid XML document.  Please try again later.");
					break;
					case SERVER_ERROR:
						mEMH.DisplayErrorMessage((String) message.obj);
					break;
					case CERT_ERROR:
						/*
						 * If there is a certificate mismatch, display the ServerHashDialog activity
						 */
						// must set this to true, in case onPause happens for child activity
						mGotAlert = true;
						Object[] messageObject = (Object[]) message.obj;
			        	Intent i = new Intent(AlertView.this, ServerHashDialog.class);
			        	i.putExtra("SHA1", (String) messageObject[0]);
			        	i.putExtra("MD5", (String) messageObject[1]);
			        	i.putExtra("CERT_INVALID", (Boolean) messageObject[2]);
						startActivityForResult(i, ACTIVITY_HASH_DIALOG);
					break;
					case DOCUMENT_VALID:
						mGotAlert = true;
						mProtocol = mAlertXMLHandler.alert.protocol;
						mHostname = mAlertXMLHandler.alert.hostname;
						mInterfaceName = mAlertXMLHandler.alert.interface_name;
						mPayload = mAlertXMLHandler.alert.payload;
						mSport = mAlertXMLHandler.alert.sport;
						mDport = mAlertXMLHandler.alert.dport;
						mType = mAlertXMLHandler.alert.type;
						mCode = mAlertXMLHandler.alert.code;
						fillData();
					break;
				}
				if(message.what != DOCUMENT_VALID && message.what != CERT_ERROR){
					finish();
				}

			}
		};
		
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try{
			super.onCreate(savedInstanceState);
	    	// Open up the XML and db handlers
			mAlertXMLHandler = new AlertXMLHandler();
			mDbHelper = new ServerDbAdapter(this);
			mDbHelper.open();
			inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			layout = (LinearLayout) inflater.inflate(R.layout.alert_view, (ViewGroup) findViewById(R.id.alert_view_layout_root));
			relativeLayout = (RelativeLayout) layout.findViewById(R.id.alert_view_relative_layout);
			setContentView(layout);
			
			// initial value of mGotAlert is false
			mGotAlert = false;
			
			// create the runnables
			alertRunnable = new AlertDisplayRunnable();
	
			alertIcon = (ImageView) layout.findViewById(R.id.alert_view_icon);
			alertText = (TextView) layout.findViewById(R.id.alert_view_sig_name_text);
	
			if(savedInstanceState != null){
				// if we have a savedInstanceState, load the strings directly
				mRowId = savedInstanceState.getLong(ServerDbAdapter.KEY_ROWID);
		        mCid = savedInstanceState.getLong(AlertDbAdapter.KEY_CID);
		        mSid = savedInstanceState.getLong(AlertDbAdapter.KEY_SID);
		        mSigName = savedInstanceState.getString(AlertDbAdapter.KEY_SIG_NAME);
		        mIpSrc = InetAddress.getByAddress(savedInstanceState.getByteArray(AlertDbAdapter.KEY_IP_SRC));
		        mIpDst = InetAddress.getByAddress(savedInstanceState.getByteArray(AlertDbAdapter.KEY_IP_DST));
		        mDate = savedInstanceState.getString("date");
		        mTime = savedInstanceState.getString("time");
				mSigPriority = savedInstanceState.getByte(AlertDbAdapter.KEY_SIG_PRIORITY);
				mProtocol = savedInstanceState.getInt(AlertView.KEY_PROTOCOL);
				mHostname = savedInstanceState.getString(AlertView.KEY_HOSTNAME);
				mInterfaceName = savedInstanceState.getString(AlertView.KEY_INTERFACE_NAME);
				mPayload = savedInstanceState.getString(AlertView.KEY_PAYLOAD);
				mSport = savedInstanceState.getInt(AlertView.KEY_SPORT);
				mDport = savedInstanceState.getInt(AlertView.KEY_DPORT);
				mType = savedInstanceState.getByte(AlertView.KEY_TYPE);
				mCode = savedInstanceState.getByte(AlertView.KEY_CODE);
				mGotAlert = savedInstanceState.getBoolean("mGotAlert");
			} else {
				Bundle extras = getIntent().getExtras();
				if(extras != null){
					// if we have an intent, construct the strings from chosen fields.  add 1 to months
					mRowId = extras.getLong(ServerDbAdapter.KEY_ROWID);
					mCid = extras.getLong(AlertDbAdapter.KEY_CID);
					mSid = extras.getLong(AlertDbAdapter.KEY_SID);
					mSigName = extras.getString(AlertDbAdapter.KEY_SIG_NAME);
					mIpSrc = InetAddress.getByAddress(extras.getByteArray(AlertDbAdapter.KEY_IP_SRC));
					mIpDst = InetAddress.getByAddress(extras.getByteArray(AlertDbAdapter.KEY_IP_DST));
					mDate = extras.getString("date");
					mTime = extras.getString("time");
					mSigPriority = extras.getByte(AlertDbAdapter.KEY_SIG_PRIORITY);
				}
			}
	
			if (mRowId != null) {
				Cursor server = mDbHelper.fetch(mRowId);
				startManagingCursor(server);
				mHostText = server.getString(server
						.getColumnIndexOrThrow(ServerDbAdapter.KEY_HOST));
				mPortInt = server.getInt(server
						.getColumnIndexOrThrow(ServerDbAdapter.KEY_PORT));
				mUsernameText = server.getString(server
						.getColumnIndexOrThrow(ServerDbAdapter.KEY_USERNAME));
				mPasswordText = server.getString(server
						.getColumnIndexOrThrow(ServerDbAdapter.KEY_PASSWORD));
			}
	
			if(!mGotAlert){
				// Display the progress dialog first
				pd = ProgressDialog.show(this, "", "Connecting. Please wait...", true);
				Thread thread = new Thread(alertRunnable);
				thread.start();
			} else {
		    	fillData();
			}
		} catch (UnknownHostException e) {
			Log.w(LOG_TAG,e.toString());
		}
	}
    
	@Override
	protected void onDestroy() {
		mDbHelper.close();
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// save not only server row, but all the search strings
		if(!mGotAlert)
			pd.dismiss();
		outState.putLong(ServerDbAdapter.KEY_ROWID, mRowId);
		outState.putLong(AlertDbAdapter.KEY_CID, mCid);
		outState.putLong(AlertDbAdapter.KEY_SID, mSid);
		outState.putString(AlertDbAdapter.KEY_SIG_NAME, mSigName);
		outState.putByteArray(AlertDbAdapter.KEY_IP_SRC, mIpSrc.getAddress());
		outState.putByteArray(AlertDbAdapter.KEY_IP_DST, mIpDst.getAddress());
		outState.putString("date", mDate);
		outState.putString("time", mTime);
		outState.putByte(AlertDbAdapter.KEY_SIG_PRIORITY, mSigPriority);
		outState.putInt(AlertView.KEY_PROTOCOL, mProtocol);
		outState.putString(AlertView.KEY_HOSTNAME, mHostname);
		outState.putString(AlertView.KEY_INTERFACE_NAME, mInterfaceName);
		outState.putString(AlertView.KEY_PAYLOAD, mPayload);
		outState.putInt(AlertView.KEY_SPORT, mSport);
		outState.putInt(AlertView.KEY_DPORT, mDport);
		outState.putByte(AlertView.KEY_TYPE, mType);
		outState.putByte(AlertView.KEY_CODE, mCode);
		outState.putBoolean("mGotAlert", mGotAlert);
	}
	
	protected void fillData(){
		switch(mSigPriority){
			case 1:
				alertIcon.setImageResource(R.drawable.low_large);
			break;
			case 2:
				alertIcon.setImageResource(R.drawable.warn_large);
			break;
			case 3:
				alertIcon.setImageResource(R.drawable.high_large);
			break;
		}
		alertText.setText(mSigName);

		TableLayout generalTableLayout = createInfoTable("General", R.id.alert_view_sig_name_text, AlertView.GENERAL_INFO_TABLE_ID); 
		String[] generalLabels = {"Date", "Time", "Sensor Address", "Interface"};
		String[] generalValues = {mDate, mTime, mHostname, mInterfaceName};
		addRowsToTable(generalLabels, generalValues, generalTableLayout);
		
		TableLayout ipTableLayout = createInfoTable("IP", AlertView.GENERAL_INFO_TABLE_ID, AlertView.IP_INFO_TABLE_ID); 
		String[] ipLabels = {"Src Address", "Dst Address"};
		String[] ipValues = {mIpSrc.getHostAddress(), mIpDst.getHostAddress()};
		addRowsToTable(ipLabels, ipValues, ipTableLayout);
		Log.w("STRINNNG",Integer.toString(mProtocol));
		String  protocol = null;
		String[] protocolLabels = null;
		String[] protocolValues = null;
		switch(mProtocol){
			case AlertXMLHandler.PROTO_ICMP:
				protocol = "ICMP";
				String typeString = "";
				String codeString = "";
				switch(mType){
					case 0: typeString = "Echo Reply"; break; 
					case 3:
						typeString = "Destination Unreachable";
						switch(mCode){
							case 0: codeString = "Network Unreachable"; break;
							case 1: codeString = "Host Unreachable"; break;
							case 2: codeString = "Protocol Unreachable"; break;
							case 3: codeString = "Port Unreachable"; break;
							case 4: codeString = "Fragmentation Needed/DF set"; break;
							case 5: codeString = "Source Route Failed"; break;
							case 6: codeString = "Destination Network Unknown"; break;
							case 7: codeString = "Destination Host Unknown"; break;
							case 8: codeString = "Source Host Isolated"; break;
							case 9: codeString = "Communication with Destination Network is Administratively Prohibited"; break;
							case 10: codeString = "Communication with Destination Host is Administratively Prohibited"; break;
							case 11: codeString = "Destination Network Unreachable for Type of Service"; break;
							case 12: codeString = "Destination Host Unreachable for Type of Service"; break;
							case 13: codeString = "Packet filtered"; break;
							case 14: codeString = "Precedence violation"; break;
							case 15: codeString = "Precedence cut off"; break;
						}
					break;
					case 4: typeString = "Source Quench"; break;
					case 5:
						typeString = "Redirect";
						switch(mCode){
							case 0: codeString = "Redirect Datagram for the Network"; break;
							case 1: codeString = "Redirect Datagram for the Host"; break;
							case 2: codeString = "Redirect Datagram for the Type of Service and Network"; break;
							case 3: codeString = "Redirect Datagram for the Type of Service and Host"; break;
						}
					break;
					case 6:
						typeString = "Alternate Host Address";
						switch(mCode){ case 0: codeString = "Alternate Address for Host"; break; }
					break;
					case 8: typeString = "Echo Request"; break;
					case 9: typeString = "Router Advertisement"; break;
					case 10: typeString = "Router Solicitation"; break;
					case 11: typeString = "Time Exceeded"; break;
					case 12: typeString = "Parameter Problem"; break;
					case 13: typeString = "Timestamp Request"; break;
					case 14: typeString = "Timestamp Reply"; break;
					case 15: typeString = "Information Request"; break;
					case 16: typeString = "Information Reply"; break;
					case 17: typeString = "Address Mask Request"; break;
					case 18: typeString = "Address Mask Reply"; break;
					case 30: typeString = "Traceroute"; break;
					case 31: typeString = "Datagram Conversion Error"; break;
					case 40:
						typeString = "Redirect";
						switch(mCode){
							case 0: codeString = "Bad SPI"; break;
							case 1: codeString = "Authentication Failed"; break;
							case 2: codeString = "Redirect Datagram for the Type of Service and Network"; break;
							case 3: codeString = "Decryption Failed"; break;
							case 4: codeString = "Need Authentication"; break;
							case 5: codeString = "Need Authorization"; break;
						}
					break;
				}
				protocolLabels = new String[]{"Type", "Code"};
				protocolValues = new String[]{String.valueOf(mType) + (typeString.length() > 0 ? " (" + typeString + ")" : ""), String.valueOf(mCode) + (codeString.length() > 0 ? " (" + codeString + ")" : "")};
			break;
			case AlertXMLHandler.PROTO_TCP:
				protocol = "TCP";
				protocolLabels = new String[]{"Src Port", "Dst Port"};
				protocolValues = new String[]{String.valueOf(mSport), String.valueOf(mDport)};
			break;
			case AlertXMLHandler.PROTO_UDP:
				protocol = "UDP";
				protocolLabels = new String[]{"Src Port", "Dst Port"};
				protocolValues = new String[]{String.valueOf(mSport), String.valueOf(mDport)};
			break;
		}
		if(protocol != null){
			TableLayout protocolTableLayout = createInfoTable(protocol, AlertView.IP_INFO_TABLE_ID, AlertView.PROTO_INFO_TABLE_ID);
			addRowsToTable(protocolLabels, protocolValues, protocolTableLayout);
		}
		if(mPayload != null){
			int parentId;
			if(protocol != null){
				parentId = AlertView.PROTO_INFO_TABLE_ID;
			} else {
				parentId = AlertView.IP_INFO_TABLE_ID;
			}
			TableLayout payloadTableLayout = createInfoTable("Payload", parentId, AlertView.PAYLOAD_INFO_TABLE_ID);
			addPayloadRowsToTable(mPayload, payloadTableLayout);
		}
	}
	
	TableLayout createInfoTable(String label, int below, int id){
		RelativeLayout infoTable = (RelativeLayout) inflater.inflate(R.layout.alert_view_info_table, (ViewGroup) findViewById(R.id.alert_view_info_table_layout_root));
		TextView infoTableLabel = (TextView) infoTable.findViewById(R.id.alert_view_info_table_label);
		LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.BELOW, below);
		infoTableLabel.setText(label);
		infoTable.setLayoutParams(params);
		infoTable.setId(id);
		relativeLayout.addView(infoTable);

		TableLayout tableLayout = (TableLayout) infoTable.findViewById(R.id.alert_view_info_table_tablelayout);
		return tableLayout;
	}
	
	void addRowsToTable(String[] labels, String[] values, TableLayout tableLayout){
		TableRow row = null;
		TableRow.LayoutParams paramsLeft = null;
		TableRow.LayoutParams paramsRight = null;
		TextView leftCell = null;
		TextView rightCell = null;
		for(int x = 0; x < labels.length; x++){
			row = new TableRow(this);
			row.setWeightSum(1.0f);
			paramsLeft = new TableRow.LayoutParams(1);
			paramsLeft.weight = .4f;
			paramsLeft.gravity = Gravity.LEFT;
			paramsLeft.width = 0;
			paramsRight = new TableRow.LayoutParams(2);
			paramsRight.weight = .6f;
			paramsRight.gravity = Gravity.LEFT;
			paramsRight.width = 0;
			leftCell = new TextView(this);
			leftCell.setText(labels[x]);
			leftCell.setTextColor(Color.WHITE);
			leftCell.setLayoutParams(paramsLeft);
			rightCell = new TextView(this);
			rightCell.setText(values[x]);
			rightCell.setTextColor(Color.WHITE);
			rightCell.setLayoutParams(paramsRight);
			row.addView(leftCell);
			row.addView(rightCell);
			tableLayout.addView(row);
		}
	}
	
	void addPayloadRowsToTable(String hexString, TableLayout tableLayout){
		try {
			byte[] asciiBytes = new BigInteger(hexString, 16).toByteArray();
			String asciiString;
			asciiString = new String(asciiBytes, "US-ASCII");
			int positionTracker = 0;
			TableRow row = null;
			TableRow.LayoutParams paramsCell[] = new TableRow.LayoutParams[12];
			TextView cell[] =  new TextView[12];
			while(hexString.length() > positionTracker){
				row = new TableRow(this);
				row.setWeightSum(1.0f);
				for(int i = 0; i < 12; i++){
					paramsCell[i] = new TableRow.LayoutParams();
					cell[i] = new TextView(this);
					paramsCell[i].column = i + 1;
					if(i < 5){
						paramsCell[i].weight = .1f;
					} else if(i == 5){
						paramsCell[i].weight = .2f;
					} else {
						paramsCell[i].weight = .05f;
					}
					paramsCell[i].gravity = Gravity.LEFT;
					paramsCell[i].width = 0;
					if(i < 6){
						if(hexString.length() > positionTracker + i*2 + 1){
							cell[i].setText(hexString.substring(positionTracker + i*2, positionTracker + i*2 + 2));
						}
					} else {
						if(hexString.length() > positionTracker + (i - 6)*2 + 1){
							if(hexString.length() > positionTracker + (i - 6)*2 + 1){
								cell[i].setText(asciiString.substring(positionTracker / 2 + (i - 6), positionTracker / 2 + (i - 6) + 1).replace("\n", ""));
							}
						}
					}
					cell[i].setTextColor(Color.WHITE);
					cell[i].setLayoutParams(paramsCell[i]);
					row.addView(cell[i]);
				}
				positionTracker += 12;
				tableLayout.addView(row);
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			Log.w(LOG_TAG, e.toString());
		}
	}
}