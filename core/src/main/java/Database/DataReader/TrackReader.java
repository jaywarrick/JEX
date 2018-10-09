package Database.DataReader;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import image.roi.ROIPlus;
import image.roi.Trajectory;
import image.roi.TrajectoryROI;
import image.roi.XTrajectorySet;

import java.io.File;
import java.util.List;
import java.util.TreeMap;

import tables.DimensionMap;

public class TrackReader {
	
	/**
	 * Get the track path stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static String readTrackObject(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.TRACK))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String result = FileReader.readToPath(ds);
		
		// String folder = ds.get(JEXDataSingle.FOLDERNAME);
		// String fileName = ds.get(JEXDataSingle.FILENAME);
		// String result = folder + File.separator + fileName;
		
		return result;
	}
	
	/**
	 * Get the track path stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static XTrajectorySet readObjectToTrajectorySet(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.TRACK))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String result = FileReader.readToPath(ds);
		
		// String folder = ds.get(JEXDataSingle.FOLDERNAME);
		// String fileName = ds.get(JEXDataSingle.FILENAME);
		// String result = folder + File.separator + fileName;
		
		File fPath = new File(result);
		if(!fPath.exists())
			return null;
		XTrajectorySet set = XTrajectorySet.getTrajectoriesFromPath(result);
		return set;
	}
	
	/**
	 * Get the track path stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static List<Trajectory> readObjectToTrajectories(JEXData data)
	{
		if(!data.getDataObjectType().matches(JEXData.TRACK))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String result = FileReader.readToPath(ds);
		// String folder = ds.get(JEXDataSingle.FOLDERNAME);
		// String fileName = ds.get(JEXDataSingle.FILENAME);
		// String result = folder + File.separator + fileName;
		
		File fPath = new File(result);
		if(!fPath.exists())
			return null;
		XTrajectorySet set = XTrajectorySet.getTrajectoriesFromPath(result);
		List<Trajectory> trajectories = set.getTrajectories();
		return trajectories;
	}
	
	/**
	 * Returns a trajectoryROI from a JEXData, null if it doesn't work
	 * 
	 * @param data
	 * @return
	 */
	public static TrajectoryROI readObjectToTrajectoryROI(JEXData data)
	{
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(data);
		TrajectoryROI result = new TrajectoryROI(roiMap);
		return result;
	}
	
}
