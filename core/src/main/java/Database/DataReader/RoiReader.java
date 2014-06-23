package Database.DataReader;

import image.roi.ROIPlus;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;

public class RoiReader {
	
	/**
	 * Get the roi stored in the data object
	 * 
	 * @param data
	 * @return
	 */
	public static ROIPlus readObjectToRoi(JEXData data)
	{
		if(data == null || !data.getDataObjectType().equals(JEXData.ROI))
			return null;
		JEXDataSingle ds = data.getFirstSingle();
		String pattern = ds.get(JEXDataSingle.PATTERN);
		String ptList = ds.get(JEXDataSingle.POINTLIST);
		String rtype = ds.get(JEXDataSingle.ROITYPE);
		ROIPlus roi = new ROIPlus(ptList, rtype);
		roi.setPattern(pattern);
		return roi;
	}
	
	/**
	 * Read the JEXData roi object into a roi array
	 * 
	 * @param data
	 * @return
	 */
	public static ROIPlus[] readObjectToRoiArray(JEXData data)
	{
		List<ROIPlus> resultList = new ArrayList<ROIPlus>(0);
		if(data == null || !data.getDataObjectType().equals(JEXData.ROI))
			return resultList.toArray(new ROIPlus[0]);
		
		for (JEXDataSingle ds : data.getDataMap().values())
		{
			String pattern = ds.get(JEXDataSingle.PATTERN);
			String ptList = ds.get(JEXDataSingle.POINTLIST);
			String rtype = ds.get(JEXDataSingle.ROITYPE);
			ROIPlus roi = new ROIPlus(ptList, rtype);
			roi.setPattern(pattern);
			resultList.add(roi);
		}
		
		ROIPlus[] result = resultList.toArray(new ROIPlus[0]);
		return result;
	}
	
	/**
	 * Read the JEXData roi object into a roi array
	 * 
	 * @param data
	 * @return
	 */
	public static List<ROIPlus> readObjectToRoiList(JEXData data)
	{
		List<ROIPlus> resultList = new ArrayList<ROIPlus>(0);
		if(data == null || !data.getDataObjectType().equals(JEXData.ROI))
			return resultList;
		
		for (JEXDataSingle ds : data.getDataMap().values())
		{
			String pattern = ds.get(JEXDataSingle.PATTERN);
			String ptList = ds.get(JEXDataSingle.POINTLIST);
			String rtype = ds.get(JEXDataSingle.ROITYPE);
			ROIPlus roi = new ROIPlus(ptList, rtype);
			roi.setPattern(pattern);
			resultList.add(roi);
		}
		
		return resultList;
	}
	
	/**
	 * Read all the rois in the roi object into a hashable table
	 * 
	 * @param data
	 * @return
	 */
	public static TreeMap<DimensionMap,ROIPlus> readObjectToRoiMap(JEXData data)
	{
		TreeMap<DimensionMap,ROIPlus> result = new TreeMap<DimensionMap,ROIPlus>();
		if(data == null || !data.getDataObjectType().equals(JEXData.ROI))
			return result;
		for (DimensionMap map : data.getDataMap().keySet())
		{
			JEXDataSingle ds = data.getData(map);
			String pattern = ds.get(JEXDataSingle.PATTERN);
			String ptList = ds.get(JEXDataSingle.POINTLIST);
			String rtype = ds.get(JEXDataSingle.ROITYPE);
			ROIPlus roi = new ROIPlus(ptList, rtype);
			roi.setPattern(pattern);
			result.put(map, roi);
		}
		return result;
	}
	
}
