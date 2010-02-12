/**
 * Copyright (C) 2010 - William Budington
 * Licensed under GPLv3
 * 
 * Adapted from AChartEngine - Copyright (C) 2009 SC 4ViewSoft SRL
 * Licensed under the Apache License, Version 2.0
 */
package org.achartengine.chartlib;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.graphics.Color;
import android.view.View;

public class AlertChart extends AbstractChart {
	private String mTitleString;
	private String mXAxisString;
	private int mHighPoint;
	private int mLowPoint;
	public LinkedList<AlertMoment> alertMoments;

	public class AlertMoment{
		public String mLabel;
		public int mHigh;
		public int mMedium;
		public int mLow;
	}
	
	public void addAlertMoment(){
		alertMoments.add(new AlertMoment());
	}
	
	public void setLastMomentHighAlert(int point){
		alertMoments.getLast().mHigh = point;
		if(point > mHighPoint)
			mHighPoint = point;
		if(point < mLowPoint)
			mLowPoint = point;
	}
	
	public void setLastMomentMediumAlert(int point){
		alertMoments.getLast().mMedium = point;
		if(point > mHighPoint)
			mHighPoint = point;
		if(point < mLowPoint)
			mLowPoint = point;
	}
	
	public void setLastMomentLowAlert(int point){
		alertMoments.getLast().mLow = point;
		if(point > mHighPoint)
			mHighPoint = point;
		if(point < mLowPoint)
			mLowPoint = point;
	}
	
	public void setLastMomentLabel(String label){
		alertMoments.getLast().mLabel = label;
	}
	
	public AlertChart(){
		alertMoments = new LinkedList<AlertMoment>();
	}
	
	public void setTitleString(String titleString){
		mTitleString = titleString;
	}
	
	public void setXAxisString(String xAxisString){
		mXAxisString = xAxisString;
	}
	
	/**
	* Executes the chart demo.
	* @param context the context
	* @return the built intent
	*/
	public View execute(Context context) {
		String[] titles = new String[] { "High", "Medium", "Low"};
		List<double[]> x = new ArrayList<double[]>();
		for(int i = 0; i < titles.length; i++) {
			double[] xDoubles = new double[alertMoments.size()];
			for(int i2 = 0; i2 < alertMoments.size(); i2++){
				xDoubles[i2] = i2 + 1; 
			}
			x.add(xDoubles);
		}
		
		int[] colors = new int[] { Color.RED, Color.YELLOW, Color.GREEN};
		PointStyle[] styles = new PointStyle[] {PointStyle.CIRCLE, PointStyle.DIAMOND, PointStyle.TRIANGLE};
		XYMultipleSeriesRenderer renderer = buildRenderer(colors, styles);
		
		List<double[]> values = new ArrayList<double[]>();
		double[] highValueDoubles = new double[alertMoments.size()];
		double[] mediumValueDoubles = new double[alertMoments.size()];
		double[] lowValueDoubles = new double[alertMoments.size()];
		ListIterator<AlertMoment> itr = alertMoments.listIterator();
		while(itr.hasNext()){
			int i = itr.nextIndex();
			AlertMoment thisAlertMoment = (AlertMoment) itr.next();
			highValueDoubles[i] = thisAlertMoment.mHigh;
			mediumValueDoubles[i] = thisAlertMoment.mMedium;
			lowValueDoubles[i] = thisAlertMoment.mLow;
			renderer.addTextLabel(i + 1, thisAlertMoment.mLabel);
		}
		values.add(highValueDoubles);
		values.add(mediumValueDoubles);
		values.add(lowValueDoubles);
		int length = renderer.getSeriesRendererCount();
		for (int i = 0; i < length; i++) {
			((XYSeriesRenderer) renderer.getSeriesRendererAt(i)).setFillPoints(true);
		}
		setChartSettings(renderer, mTitleString, mXAxisString, "Alerts", 0.5, alertMoments.size() + 0.5, 0, mHighPoint + (mHighPoint * .1), Color.LTGRAY, Color.GRAY);
		renderer.setYLabels(8);
		View view = ChartFactory.getLineChartView(context, buildDataset(titles, x, values), renderer);

	    return view;
	}

}
