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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

import com.legind.Dialogs.ErrorMessageHandler;
import com.legind.sqlite.AlertDbAdapter;
import com.legind.sqlite.ServerDbAdapter;
import com.legind.swinedroid.NetworkRunnable.NetworkRunnable;
import com.legind.swinedroid.NetworkRunnable.NetworkRunnableRequires;
import com.legind.swinedroid.xml.AlertXMLHandler;
import com.legind.swinedroid.xml.XMLHandlerException;
import com.legind.web.WebTransport.WebTransportException;

public class AlertView extends Activity implements NetworkRunnableRequires{
	private Long mRowId;
	private ProgressDialog pd;
	private ProgressDialog pdRDNS;
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
	private final String LOG_TAG = "com.legind.swinedroid.AlertView";
	private boolean mGotAlert;
	private ResolveRDNSRunnable resolveRunnable;
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
	private static final int COPY_ID = 0;
	private static final int RDNS_ID = 1;
	private Menu alertViewMenu;
	private ImageView alertIcon;
	private TextView alertText;
	private LayoutInflater inflater;
	private LinearLayout layout;
	private RelativeLayout relativeLayout;
	private ClipboardManager clipboard;
	private String clipboardText;
	private TableLayout ipTableLayout;
    public static Activity A = null;
    private ErrorMessageHandler mEMH;
    private NetworkRunnable mNetRun = null;

	private class ResolveRDNSRunnable implements Runnable {
		private String ipSrcRDNS;
		private String ipDstRDNS;
		
		public void run(){
			ipSrcRDNS = mIpSrc.getHostName();
			ipDstRDNS = mIpDst.getHostName();
			handler.sendEmptyMessage(0);
		}
		
		private Handler handler = new Handler(){
			@Override
			public void handleMessage(Message msg){
        		String[] ipLabels = {"Src RDNS", "Dst RDNS"};
        		String ipSrcRDNSDisplay = ipSrcRDNS.equals(mIpSrc.getHostAddress()) ? "Could Not Resolve" : ipSrcRDNS;
        		String ipDstRDNSDisplay = ipDstRDNS.equals(mIpDst.getHostAddress()) ? "Could Not Resolve" : ipDstRDNS; 
				String[] ipValues = {ipSrcRDNSDisplay, ipDstRDNSDisplay};
				clipboardText = clipboardText.replace("###RDNS###","\nSrc RDNS: " + ipSrcRDNSDisplay + "\nDst RDNS: " + ipDstRDNSDisplay);
				addRowsToTable(ipLabels, ipValues, ipTableLayout);
				alertViewMenu.removeItem(RDNS_ID);
				pdRDNS.dismiss();
	        	CharSequence text = "RDNS info added to alert overview.";
	        	int duration = Toast.LENGTH_SHORT;
	
	        	Toast toast = Toast.makeText(AlertView.A, text, duration);
	        	toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
	        	toast.show();
			}
		};
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try{
			super.onCreate(savedInstanceState);
			A = this;
			mNetRun = new NetworkRunnable(this);
	    	// Open up the XML and db handlers
			mAlertXMLHandler = new AlertXMLHandler();
			mDbHelper = new ServerDbAdapter(this);
			mDbHelper.open();
			inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			layout = (LinearLayout) inflater.inflate(R.layout.alert_view, (ViewGroup) findViewById(R.id.alert_view_layout_root));
			relativeLayout = (RelativeLayout) layout.findViewById(R.id.alert_view_relative_layout);
			mEMH = new ErrorMessageHandler(this,
					findViewById(R.id.server_edit_error_layout_root));
			setContentView(layout);
			
			// initial value of mGotAlert is false
			mGotAlert = false;
			
			// create the runnable
			resolveRunnable = new ResolveRDNSRunnable();
	
			alertIcon = (ImageView) layout.findViewById(R.id.alert_view_icon);
			alertText = (TextView) layout.findViewById(R.id.alert_view_sig_name_text);
			
			clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
	
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
			
			mNetRun.startRequestService();
		} catch (UnknownHostException e) {
			Log.w(LOG_TAG,e.toString());
		}
	}
    
	@Override
	protected void onDestroy() {
		mNetRun.close();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mNetRun.certificateActivityResult(requestCode, resultCode, intent, ACTIVITY_HASH_DIALOG);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        alertViewMenu = menu; 
        menu.add(0, COPY_ID, 0, R.string.menu_copy_alert);
        menu.add(0, RDNS_ID, 0, R.string.menu_rdns_lookup);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()){
        	case COPY_ID:
                clipboard.setText(clipboardText.replace("###RDNS###", ""));
        		Context context = getApplicationContext();
	        	CharSequence text = "Alert copied to clipboard.";
	        	int duration = Toast.LENGTH_SHORT;
	        	Toast toast = Toast.makeText(context, text, duration);
	        	toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 0);
	        	toast.show();
        	break;
        	case RDNS_ID:
        		pdRDNS = ProgressDialog.show(this, "", "Resolving RDNS...", true);
        		Thread thread = new Thread(resolveRunnable);
        		thread.start();
        	break;
        }
        return super.onMenuItemSelected(featureId, item);
    }
	
	protected void fillData(){
		clipboardText = "Signature Name: " + mSigName + "\nSeverity: ";
		alertText.setText(mSigName);
		switch(mSigPriority){
			case 1:
				alertIcon.setImageResource(R.drawable.low_large);
				clipboardText += "Low";
			break;
			case 2:
				alertIcon.setImageResource(R.drawable.warn_large);
				clipboardText += "Medium";
			break;
			case 3:
				alertIcon.setImageResource(R.drawable.high_large);
				clipboardText += "High";
			break;
		}

		TableLayout generalTableLayout = createInfoTable("General", R.id.alert_view_sig_name_text, AlertView.GENERAL_INFO_TABLE_ID); 
		String[] generalLabels = {"Date", "Time", "Sensor Address", "Interface"};
		String[] generalValues = {mDate, mTime, mHostname, mInterfaceName};
		clipboardText += "\n\nDate: " + mDate + "\nTime: " + mTime + "\nSensor Address: " + mHostname + "\nInterface: " + mInterfaceName;
		addRowsToTable(generalLabels, generalValues, generalTableLayout);

		ipTableLayout = createInfoTable("IP", AlertView.GENERAL_INFO_TABLE_ID, AlertView.IP_INFO_TABLE_ID); 
		String[] ipLabels = {"Src Address", "Dst Address"};
		String[] ipValues = {mIpSrc.getHostAddress(), mIpDst.getHostAddress()};
		clipboardText += "\n\nIP Layer\nSrc Address: " + mIpSrc.getHostAddress() + "\nDst Address: " + mIpDst.getHostAddress() + "###RDNS###";
		addRowsToTable(ipLabels, ipValues, ipTableLayout);
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
				clipboardText += "\n\nICMP Layer\nType: " + String.valueOf(mType) + (typeString.length() > 0 ? " (" + typeString + ")" : "") + "\nCode: " + String.valueOf(mCode) + (codeString.length() > 0 ? " (" + codeString + ")" : "");
			break;
			case AlertXMLHandler.PROTO_TCP:
				protocol = "TCP";
				protocolLabels = new String[]{"Src Port", "Dst Port"};
				protocolValues = new String[]{String.valueOf(mSport), String.valueOf(mDport)};
				clipboardText += "\n\nTCP Layer\nSrc Port: " + String.valueOf(mSport) + "\nDst Port: " + String.valueOf(mDport);
			break;
			case AlertXMLHandler.PROTO_UDP:
				protocol = "UDP";
				protocolLabels = new String[]{"Src Port", "Dst Port"};
				protocolValues = new String[]{String.valueOf(mSport), String.valueOf(mDport)};
				clipboardText += "\n\nUDP Layer\nSrc Port: " + String.valueOf(mSport) + "\nDst Port: " + String.valueOf(mDport);
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
			clipboardText += "\n\nPayload (Hex):\n" + hexString;
			clipboardText += "\n\nPayload (ASCII):\n" + asciiString;
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
			Log.w(LOG_TAG, e.toString());
		}
	}

	public void callHashDialog(Intent i) {
		startActivityForResult(i, ACTIVITY_HASH_DIALOG);
	}

	public OnCancelListener getCancelListener() {
		return new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				mDbHelper.close();
				finish();
				return;
			}
		};
	}

	public Context getContext() {
		return this;
	}

	public ErrorMessageHandler getEMH() {
		return mEMH;
	}

	public Long getRowId() {
		return mRowId;
	}

	public void onBoundRequestSet() {
		if(!mGotAlert){
			// Display the progress dialog first
			pd = ProgressDialog.show(AlertView.this, "", "Connecting. Please wait...", true);
			Thread thread = new Thread(mNetRun);
			thread.start();
		} else {
	    	fillData();
		}
	}

	public void onCertErrorBegin() {
		mGotAlert = true;
	}

	public void onCertificateInspectVerified() throws IOException, SAXException, XMLHandlerException, WebTransportException {
		// construct the GET arguments string, send it to the XML handler
		String extraArgs = "cid=" + mCid + "&sid=" + mSid;
		mAlertXMLHandler.createElement(mNetRun.getBoundRequest(), "alert", extraArgs);
	}

	public void onDocumentValidReturned() {
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
	}

	public void onHandleMessageBegin() {
		pd.dismiss();
	}
}