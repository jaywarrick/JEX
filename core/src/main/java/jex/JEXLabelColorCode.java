package jex;

import java.awt.Color;
import java.util.Random;
import java.util.TreeMap;

import logs.Logs;
import plugins.labelManager.ColorPallet;
import Database.DBObjects.JEXLabel;

public class JEXLabelColorCode {
	
	private Random random = new Random();
	private TreeMap<String,TreeMap<String,Color>> labelMap;
	
	public static ColorPallet colorPallet = new ColorPallet();
	
	public JEXLabelColorCode()
	{
		labelMap = new TreeMap<String,TreeMap<String,Color>>();
	}
	
	/**
	 * Return the color saved for a particular label
	 * 
	 * @param label
	 * @return color of label
	 */
	public Color getColorForLabel(String name, String value)
	{
		// Color result = Color.getHSBColor( random.nextFloat(), 1.0F, 1.0F );
		// return result;
		
		// get the TreeMap for this label, make one if it doesn't exist...
		TreeMap<String,Color> thisLabelMap = labelMap.get(name);
		if(thisLabelMap == null)
		{
			Logs.log("Didn't find a hash map for this label name " + name, 1, this);
			thisLabelMap = new TreeMap<String,Color>();
			labelMap.put(name, thisLabelMap);
		}
		
		// get the color for this label, make one if it doesn't exist
		Color color = thisLabelMap.get(value);
		if(color == null)
		{
			Logs.log("Didn't find a color for this label value " + value, 1, this);
			color = Color.getHSBColor(random.nextFloat(), 0.85F, 1.0F);
			thisLabelMap.put(value, color);
		}
		
		return color;
	}
	
	/**
	 * Return the color saved for a particular label
	 * 
	 * @param label
	 * @return color of label
	 */
	public Color getColorForLabel(JEXLabel label)
	{
		// Color result = Color.getHSBColor( random.nextFloat(), 1.0F, 1.0F );
		// return result;
		
		// get the TreeMap for this label, make one if it doesn't exist...
		TreeMap<String,Color> thisLabelMap = labelMap.get(label.getDataObjectName());
		if(thisLabelMap == null)
		{
			Logs.log("Didn't find a hash map for this label name " + label.getDataObjectName(), 1, this);
			thisLabelMap = new TreeMap<String,Color>();
			labelMap.put(label.getDataObjectName(), thisLabelMap);
		}
		
		// get the color for this label, make one if it doesn't exist
		Color color = thisLabelMap.get(label.getLabelValue());
		if(color == null)
		{
			Logs.log("Didn't find a color for this label value " + label.toString(), 1, this);
			color = Color.getHSBColor(random.nextFloat(), 0.85F, 1.0F);
			thisLabelMap.put(label.getLabelValue(), color);
		}
		
		return color;
	}
	
	/**
	 * Add a label to the dictionary and have it assigned a new color
	 * 
	 * @param label
	 */
	public void addLabelForColoring(JEXLabel label)
	{
		// get the TreeMap for this label, make one if it doesn't exist...
		TreeMap<String,Color> thisLabelMap = labelMap.get(label.getDataObjectName());
		if(thisLabelMap == null)
		{
			thisLabelMap = new TreeMap<String,Color>();
			labelMap.put(label.getDataObjectName(), thisLabelMap);
		}
		
		// get the color for this label, make one if it doesn't exist
		Color color = thisLabelMap.get(label.getLabelValue());
		if(color == null)
		{
			color = Color.getHSBColor(random.nextFloat(), 0.5F, 0.5F);
			thisLabelMap.put(label.getLabelValue(), color);
		}
	}
	
	/**
	 * Set the color for the label LABEL
	 * 
	 * @param label
	 * @param color
	 */
	public void setColorForLabel(JEXLabel label, Color color)
	{
		// get the TreeMap for this label, make one if it doesn't exist...
		TreeMap<String,Color> thisLabelMap = labelMap.get(label.getDataObjectName());
		if(thisLabelMap == null)
		{
			thisLabelMap = new TreeMap<String,Color>();
			labelMap.put(label.getDataObjectName(), thisLabelMap);
		}
		thisLabelMap.put(label.getLabelValue(), color);
	}
	
	/**
	 * Set the color for the label LABEL
	 * 
	 * @param label
	 * @param color
	 */
	public void setColorForLabel(String labelName, String labelValue, Color color)
	{
		// get the TreeMap for this label, make one if it doesn't exist...
		TreeMap<String,Color> thisLabelMap = labelMap.get(labelName);
		if(thisLabelMap == null)
		{
			thisLabelMap = new TreeMap<String,Color>();
			labelMap.put(labelName, thisLabelMap);
		}
		thisLabelMap.put(labelValue, color);
	}
	
	/**
	 * Returns the model of the JEXLabelColorCode
	 * 
	 * @return
	 */
	public TreeMap<String,TreeMap<String,Color>> getLabelMap()
	{
		return this.labelMap;
	}
}
