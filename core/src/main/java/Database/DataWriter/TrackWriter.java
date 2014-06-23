package Database.DataWriter;

import image.roi.Trajectory;
import image.roi.TrajectoryROI;
import image.roi.XTrajectorySet;

import java.util.List;

import miscellaneous.XMLUtility;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.SingleUserDatabase.JEXWriter;

public class TrackWriter {
	
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
