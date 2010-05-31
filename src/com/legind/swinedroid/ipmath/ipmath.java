package com.legind.swinedroid.ipmath;

public class ipmath{
	public static String longToString(long ipLong){
		return Integer.toString((int) ((ipLong % Math.pow(256, 4)) / Math.pow(256, 3))) + "." + Integer.toString((int) ((ipLong % Math.pow(256, 3)) / Math.pow(256, 2))) + "." + Integer.toString((int) ((ipLong % Math.pow(256, 2)) / 256)) + "." + Integer.toString((int) (ipLong % 256));
	}
}