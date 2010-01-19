package com.legind.swinedroid.xml;

import java.sql.Timestamp;

public class AlertListXMLElement extends Object {
	/* TODO: Change long types to "unsigned" int types (a bit less heavy) */
	public long sid;
	public long cid;
	public long ipSrc;
	public long ipDst;
	public byte sigPriority;
	public String sigName;
	public Timestamp timestamp;
}