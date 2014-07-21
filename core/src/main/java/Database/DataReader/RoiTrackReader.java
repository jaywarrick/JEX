package Database.DataReader;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import function.roitracker.RoiTrack;
import image.roi.ROIPlus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import tables.DimensionMap;

public class RoiTrackReader {
	
	/**
	 * Return a list of RoiTracks from a jexdata
	 * @param data
	 * @return
	 */
	public static TreeMap<Integer,RoiTrack> readObjectToRoiTrackMap(JEXData data)
	{
		// Test the data type for error checking
		if(data == null || !data.getDataObjectType().equals(JEXData.ROI_TRACK)) return null;
		
		// Setup the variables
		TreeMap<Integer,RoiTrack> result = new TreeMap<Integer,RoiTrack>();
		
		// Loop through the dimension map
		for (DimensionMap map : data.getDataMap().keySet())
		{
			// Get the datasingle
			JEXDataSingle ds = data.getData(map);
			
			// Get the track information
			String trackIDStr = ds.get(JEXDataSingle.TRACK_ID);
			int trackID       = Integer.parseInt(trackIDStr);
			
			// IS there a track that already matches the id
			RoiTrack track = result.get(trackID);
			if (track == null) 
			{
				track = new RoiTrack(new TreeMap<Integer, ROIPlus>());
				track.setTrackID(trackID);
				result.put(trackID, track);
			}
			
			// Get the roi information
			String pattern = ds.get(JEXDataSingle.PATTERN);
			String ptList  = ds.get(JEXDataSingle.POINTLIST);
			String rtype   = ds.get(JEXDataSingle.ROITYPE);
			
			// Make the roi
			ROIPlus roi = new ROIPlus(ptList, rtype);
			roi.setPattern(pattern);
			
			// Get the roi information to put into the track
			String roiIDStr = ds.get(JEXDataSingle.TRACK_DIMENSION_NAME);
			int roiID       = Integer.parseInt(map.get(roiIDStr));
			
			// Add the roi to the track
			track.addRoi(roi, roiID);
		}
		
		return result;
	}
	
	/**
	 * Return a list of RoiTracks from a jexdata
	 * @param data
	 * @return
	 */
	public static List<RoiTrack> readObjectToRoiTrackList(JEXData data)
	{
		// Test the data type for error checking
		if(data == null || !data.getDataObjectType().equals(JEXData.ROI_TRACK)) return null;
		
		// Setup the variables
		TreeMap<Integer,RoiTrack> result = new TreeMap<Integer,RoiTrack>();
		
		// Loop through the dimension map
		for (DimensionMap map : data.getDataMap().keySet())
		{
			// Get the datasingle
			JEXDataSingle ds = data.getData(map);
			
			// Get the track information
			String trackIDStr = ds.get(JEXDataSingle.TRACK_ID);
			int trackID       = Integer.parseInt(trackIDStr);
			
			// IS there a track that already matches the id
			RoiTrack track = result.get(trackID);
			if (track == null) 
			{
				track = new RoiTrack(new TreeMap<Integer, ROIPlus>());
				track.setTrackID(trackID);
				result.put(trackID, track);
			}
			
			// Get the roi information
			String pattern = ds.get(JEXDataSingle.PATTERN);
			String ptList  = ds.get(JEXDataSingle.POINTLIST);
			String rtype   = ds.get(JEXDataSingle.ROITYPE);
			
			// Make the roi
			ROIPlus roi = new ROIPlus(ptList, rtype);
			roi.setPattern(pattern);
			
			// Get the roi information to put into the track
			String roiIDStr = ds.get(JEXDataSingle.TRACK_DIMENSION_NAME);
			int roiID       = Integer.parseInt(map.get(roiIDStr));
			
			// Add the roi to the track
			track.addRoi(roi, roiID);
		}
		
		// Make a list of RoiTracks
		ArrayList<RoiTrack> ret = new ArrayList<RoiTrack>(0);
		Set<Integer> keys = result.keySet();
		for (int key: keys)
		{
			RoiTrack track = result.get(key);
			ret.add(track);
		}
		
		return ret;
	}
}
