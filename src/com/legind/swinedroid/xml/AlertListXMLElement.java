package com.legind.swinedroid.xml;

import java.sql.Timestamp;

public class AlertListXMLElement extends Object {
	/* TODO: Change long types to "unsigned" int types (a bit less heavy) */
	public long sid;
	public long cid;
	public long ip_src;
	public long ip_dst;
	public byte sig_priority;
	public String sig_name;
	public Timestamp timestamp;
}