package function.roitracker;

import ij.gui.Roi;
import image.roi.ROIPlus;
import image.roi.Vect;
import image.roi.VectSet;

import java.awt.Color;
import java.awt.Point;
import java.util.HashMap;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import jex.utilities.ROIUtility;

public class RoiTrack {
	
	// Statics
	public static int POINT = Roi.POINT;
	public static int RECTANGLE = Roi.RECTANGLE;
	public static int POLYGON = Roi.POLYGON;
	public static int ELIPSE = Roi.OVAL;
	public static int LINE = Roi.LINE;
	public static int ROI = -1;
	public static String DEFAULT_TIMELAPSE_DIMENSION_NAME = "T";
	
	// Core data
	TreeMap<Integer,ROIPlus> trackMap;
	String trackDimensionName = DEFAULT_TIMELAPSE_DIMENSION_NAME;
	int trackID = -1;
	
	// Display data
	TreeMap<Integer,Color> colorMap;
	
	// Type of rois in the track
	private int roiType = ROI;
	
	public RoiTrack(TreeMap<Integer,ROIPlus> trackMap)
	{
		this.trackMap = trackMap;
	}
	
	public RoiTrack(HashMap<Integer,ROIPlus> trackMap)
	{
		this.trackMap = new TreeMap<Integer,ROIPlus>();
		
		for (int frame : trackMap.keySet())
		{
			ROIPlus roip = trackMap.get(frame);
			this.trackMap.put(frame, roip);
		}
	}
	
	public RoiTrack(ROIPlus firstRoi, int frame)
	{
		this.trackMap = new TreeMap<Integer,ROIPlus>();
		this.trackMap.put(frame, firstRoi);
	}
	
	// ----------------------------------------------------
	// ------------- ADD AND REMOVE -----------------------
	// ----------------------------------------------------
	
	public void addRoi(ROIPlus roip, int key)
	{
		this.trackMap.put(key, roip);
	}
	
	public int getRoiType()
	{
		if(this.trackMap == null || this.trackMap.size() == 0)
		{
			this.roiType = ROI;
			return this.roiType;
		}
		
		ROIPlus roip = this.first();
		int type = roip.getRoi().getType();
		
		if(type == Roi.LINE)
		{
			this.roiType = LINE;
		}
		else if(type == Roi.OVAL)
		{
			this.roiType = ELIPSE;
		}
		else if(type == Roi.POINT)
		{
			this.roiType = POINT;
		}
		else if(type == Roi.POLYGON)
		{
			this.roiType = POLYGON;
		}
		else if(type == Roi.RECTANGLE)
		{
			this.roiType = RECTANGLE;
		}
		else
		{
			this.roiType = ROI;
		}
		
		return this.roiType;
	}
	
	// ----------------------------------------------------
	// ------------- SETTINGS and I/O ---------------------
	// ----------------------------------------------------
	
	public int size()
	{
		if(this.trackMap == null)
		{
			return 0;
		}
		return this.trackMap.size();
	}
	
	public void setColorMap(TreeMap<Integer,Color> colorMap)
	{
		this.colorMap = colorMap;
	}
	
	public void setColor(int key, Color color)
	{
		if(this.colorMap == null)
		{
			this.colorMap = new TreeMap<Integer,Color>();
		}
		this.colorMap.put(key, color);
	}
	
	public TreeMap<Integer,Color> getColorMap()
	{
		if(this.colorMap == null)
		{
			this.colorMap = new TreeMap<Integer,Color>();
		}
		return this.colorMap;
	}
	
	public Color getColor(int key)
	{
		if(this.colorMap == null || this.colorMap.size() == 0)
		{
			return null;
		}
		return this.colorMap.get(key);
	}
	
	public int getTrackID()
	{
		return this.trackID;
	}
	
	public void setTrackID(int trackID)
	{
		this.trackID = trackID;
	}
	
	public ROIPlus first()
	{
		int first = this.trackMap.firstKey();
		return this.trackMap.get(first);
	}
	
	public int firstKey()
	{
		return this.trackMap.firstKey();
	}
	
	public ROIPlus last()
	{
		int last = this.trackMap.lastKey();
		return this.trackMap.get(last);
	}
	
	public int lastKey()
	{
		return this.trackMap.lastKey();
	}
	
	public ROIPlus next(int key)
	{
		if(this.trackMap == null)
		{
			return null;
		}
		
		int nextKey = this.nextKey(key);
		if(nextKey == -1)
		{
			return null;
		}
		
		ROIPlus roi = this.trackMap.get(nextKey);
		return roi;
	}
	
	public int nextKey(int key)
	{
		if(this.trackMap == null)
		{
			return -1;
		}
		
		NavigableMap<Integer,ROIPlus> subMap = this.trackMap.subMap(key, false, this.lastKey(), true);
		if(subMap.size() == 0)
		{
			return -1;
		}
		
		int nextKey = subMap.firstKey();
		return nextKey;
	}
	
	public ROIPlus getRoi(int key)
	{
		if(this.trackMap == null)
		{
			return null;
		}
		return this.trackMap.get(key);
	}
	
	public TreeMap<Integer,ROIPlus> getRoiMap()
	{
		return this.trackMap;
	}
	
	public void setTrackDimensionName(String trackDimensionName)
	{
		this.trackDimensionName = trackDimensionName;
	}
	
	public String getTrackDimensionName()
	{
		return this.trackDimensionName;
	}
	
	// Track statistics
	
	public VectSet getVectors(boolean average)
	{
		VectSet result = new VectSet();
		
		Set<Integer> keys = this.trackMap.keySet();
		for (Integer key : keys)
		{
			// Get the roi
			ROIPlus roi1 = this.trackMap.get(key);
			
			// Get the next roi
			int nextkey = this.nextKey(key);
			ROIPlus roi2 = this.trackMap.get(nextkey);
			
			// if one of the rois is null skip
			if(roi1 == null || roi2 == null)
			{
				continue;
			}
			
			// Get the points
			Point p1 = ROIUtility.getRectangleCenter(roi1);
			Point p2 = ROIUtility.getRectangleCenter(roi2);
			
			// Make the vector
			double dx = (p2.getX() - p1.getX());
			double dy = (p2.getY() - p1.getY());
			Vect v = new Vect(dx, dy);
			
			// If the averaging flag is true then average the vector
			if(average)
			{
				v.multiply((nextkey - key));
			}
			
			// add the vector to the list
			result.add(v);
		}
		
		return result;
	}
	
}
