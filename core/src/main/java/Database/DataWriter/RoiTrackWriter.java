package Database.DataWriter;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import function.roitracker.RoiTrack;
import image.roi.ROIPlus;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import tables.DimensionMap;

public class RoiTrackWriter {
	
	/**
	 * Make a JEXData from a list of tracks
	 * 	
	 * @param objectName
	 * @param templateMap
	 * @param tracks
	 * @return
	 */
	public static JEXData makeRoiTrackObject(String objectName, DimensionMap templateMap, TreeMap<Integer,RoiTrack> tracks)
	{
		if(tracks == null || tracks.size() == 0)
		{
			return null;
		}
		
		// Make the larger data structure
		JEXData data = new JEXData(JEXData.ROI_TRACK, objectName);
		data.put(JEXEntry.INFO, "No info inputed");
		data.put(JEXEntry.DATE, miscellaneous.DateUtility.getDate());
		data.put(JEXEntry.MODIFDATE, miscellaneous.DateUtility.getDate());
		
		// Loop through the tracks
		Set<Integer> trackkeys = tracks.keySet();
		for (int trackkey: trackkeys)
		{
			// Get the roi
			RoiTrack track = tracks.get(trackkeys);
			
			// Get the roiplus list
			TreeMap<Integer,ROIPlus> roiMap = track.getRoiMap();
			Set<Integer> keys = roiMap.keySet();
			
			// Loop through the rois
			for (int key: keys)
			{
				// Get the roi
				ROIPlus roi = roiMap.get(key);
				
				// Make the dimension map for this roi
				DimensionMap map = null;
				if (templateMap == null) map = new DimensionMap();
				else map = templateMap.copy();
				
				// Put the trackID and roiID in the map
				map.put(track.getTrackDimensionName(), ""+key);
				map.put(JEXDataSingle.TRACK_ID, ""+trackkey);	
				
				// Make a jexdatasingle
				String value     = roi.getPointList().pointListString();
				String pattern   = roi.getPattern().pointListString();
				JEXDataSingle ds = new JEXDataSingle();
				ds.put(JEXDataSingle.ROITYPE, "" + roi.type);
				ds.put(JEXDataSingle.POINTLIST, value);
				ds.put(JEXDataSingle.PATTERN, pattern);
				ds.put(JEXDataSingle.TRACK_DIMENSION_NAME, track.getTrackDimensionName());
				ds.put(JEXDataSingle.TRACK_ID, "" + trackkey);
				
				// add to the jexdata
				data.addData(map, ds);
			}
		}
		
		if(data.datamap.size() == 0)
		{
			return null;
		}
		return data;
	}
	

	/**
	 * Make a JEXData from a list of tracks
	 * 	
	 * @param objectName
	 * @param templateMap
	 * @param tracks
	 * @return
	 */
	public static JEXData makeRoiTrackObject(String objectName, DimensionMap templateMap, List<RoiTrack> tracks)
	{
		if(tracks == null || tracks.size() == 0)
		{
			return null;
		}
		
		// Make the larger data structure
		JEXData data = new JEXData(JEXData.ROI_TRACK, objectName);
		data.put(JEXEntry.INFO, "No info inputed");
		data.put(JEXEntry.DATE, miscellaneous.DateUtility.getDate());
		data.put(JEXEntry.MODIFDATE, miscellaneous.DateUtility.getDate());
		
		// Loop through the tracks
		int trackkey = 0 ;
		for (RoiTrack track: tracks)
		{
			// Get the roiplus list
			TreeMap<Integer,ROIPlus> roiMap = track.getRoiMap();
			Set<Integer> keys = roiMap.keySet();
			
			// Loop through the rois
			for (int key: keys)
			{
				// Get the roi
				ROIPlus roi = roiMap.get(key);
				
				// Make the dimension map for this roi
				DimensionMap map = null;
				if (templateMap == null) map = new DimensionMap();
				else map = templateMap.copy();
				
				// Put the trackID and roiID in the map
				map.put(track.getTrackDimensionName(), ""+key);
				map.put(JEXDataSingle.TRACK_ID, ""+trackkey);	
				
				// Make a jexdatasingle
				String value     = roi.getPointList().pointListString();
				String pattern   = roi.getPattern().pointListString();
				JEXDataSingle ds = new JEXDataSingle();
				ds.put(JEXDataSingle.ROITYPE, "" + roi.type);
				ds.put(JEXDataSingle.POINTLIST, value);
				ds.put(JEXDataSingle.PATTERN, pattern);
				ds.put(JEXDataSingle.TRACK_DIMENSION_NAME, track.getTrackDimensionName());
				ds.put(JEXDataSingle.TRACK_ID, "" + trackkey);
				
				// add to the jexdata
				data.addData(map, ds);
			}
			
			// Increment the trackID
			trackkey += 1;
		}
		
		if(data.datamap.size() == 0)
		{
			return null;
		}
		return data;
	}
}
