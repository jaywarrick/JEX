package Database.DataWriter;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.SingleUserDatabase.JEXWriter;
import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.Trajectory;
import image.roi.TrajectoryROI;
import image.roi.XTrajectorySet;
import miscellaneous.XMLUtility;
import tables.DimensionMap;

public class TrackWriter {
	
	
	/**
	 * Create a track object from an track ROI
	 *
	 */
	public static JEXData makeTracksObject(String objectName, TreeMap<DimensionMap,ROIPlus> trackRoi)
	{
		Vector<Trajectory> tList = new Vector<>();
		for(Entry<DimensionMap,ROIPlus> e : trackRoi.entrySet())
		{
			PointList toAdd = e.getValue().pattern.copy();
			toAdd.translate(e.getValue().getPointList().firstElement().x, e.getValue().getPointList().firstElement().y);
			Trajectory toAdd2 = new Trajectory(toAdd);
			tList.add(toAdd2);
		}
		return makeTracksObject(objectName, tList);
	}
	
	/**
	 * Create a new object containing a list of cell trajectories and save the list of trajectories in path PATHTOSAVETRACKS
	 */
	public static JEXData makeTracksObject(String objectName, List<Trajectory> tracks)
	{
		JEXData result = new JEXData(JEXData.TRACK, objectName);
		
		XTrajectorySet trajSet = new XTrajectorySet();
		for (Trajectory traj : tracks)
		{
			Trajectory newTraj = (Trajectory) traj.clone();
			trajSet.addTrajectory(newTraj);
		}
		
		String xmlStr = XMLUtility.toHardXML(trajSet);
		String filePath = JEXWriter.saveText(xmlStr, "xml");
		JEXDataSingle ds = FileWriter.saveFileDataSingle(filePath);
		if(ds == null)
		{
			return null;
		}
		result.addData(new DimensionMap(), ds);
		
		return result;
	}
	
	/**
	 * Create a new object containing a list of cell trajectories and save the list of trajectories in path PATHTOSAVETRACKS
	 */
	public static JEXData makeTracksObject(String objectName, Trajectory[] tracks)
	{
		JEXData result = new JEXData(JEXData.TRACK, objectName);
		
		XTrajectorySet trajSet = new XTrajectorySet();
		for (Trajectory traj : tracks)
		{
			Trajectory newTraj = (Trajectory) traj.clone();
			trajSet.addTrajectory(newTraj);
		}
		
		String xmlStr = XMLUtility.toHardXML(trajSet);
		String filePath = JEXWriter.saveText(xmlStr, "xml");
		JEXDataSingle ds = FileWriter.saveFileDataSingle(filePath);
		if(ds == null)
		{
			return null;
		}
		result.addData(new DimensionMap(), ds);
		
		return result;
	}
	
	/**
	 * Create a new object
	 * 
	 * @param objectName
	 * @param roiMap
	 * @return
	 */
	public static JEXData makeTrajectoryROI(String objectName, TrajectoryROI roiMap)
	{
		JEXData result = RoiWriter.makeRoiObject(objectName, roiMap.getRoiMap());
		return result;
	}
}
