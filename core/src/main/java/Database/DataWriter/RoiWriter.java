package Database.DataWriter;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import image.roi.ROIPlus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tables.DimensionMap;

public class RoiWriter {
	
	/**
	 * Make a simple, zero dimensional ROI data object
	 */
	public static JEXData makeRoiObject(String objectName, ROIPlus roi)
	{
		return makeRoiObject(objectName,null,roi);
	}
	
	/**
	 * Make a simple, zero dimensional ROI data object
	 */
	public static JEXData makeRoiObject(String objectName, String objectFlavor, ROIPlus roi)
	{
		JEXData result = new JEXData(new Type(JEXData.ROI,objectFlavor), objectName);
		result.put(JEXEntry.INFO, "No info inputed");
		result.put(JEXEntry.DATE, miscellaneous.DateUtility.getDate());
		result.put(JEXEntry.MODIFDATE, miscellaneous.DateUtility.getDate());
		
		String value = roi.getPointList().pointListString();
		String pattern = roi.getPattern().pointListString();
		JEXDataSingle ds = new JEXDataSingle();
		ds.put(JEXDataSingle.ROITYPE, "" + roi.type);
		ds.put(JEXDataSingle.POINTLIST, value);
		ds.put(JEXDataSingle.PATTERN, pattern);
		
		DimensionMap map = new DimensionMap();
		result.addData(map, ds);
		result.put("roiType", "" + roi.type);
		
		return result;
	}
	
	/**
	 * Make a one dimensional ROI object with a list of Rois at indexes given by the string array INDEXES for a dimension name DIMENSIONNAME
	 * 
	 * @param objectName
	 * @param rois
	 * @param indexes
	 * @param dimensionName
	 * @return
	 */
	public static JEXData makeRoiObject(String objectName, ROIPlus[] rois, String[] indexes, String dimensionName)
	{
		HashMap<DimensionMap,ROIPlus> datamap = new HashMap<DimensionMap,ROIPlus>();
		for (int index = 0; index < rois.length; index++)
		{
			ROIPlus roi = rois[index];
			String indValue = indexes[index];
			DimensionMap map = new DimensionMap();
			map.put(dimensionName, indValue);
			datamap.put(map, roi);
		}
		
		JEXData result = makeRoiObject(objectName, datamap);
		return result;
	}
	
	/**
	 * Make a one dimensional ROI object with a list of Rois at integer indexes for a dimension name DIMENSIONNAME
	 * 
	 * @param objectName
	 * @param rois
	 * @param dimensionName
	 * @return
	 */
	public static JEXData makeRoiObject(String objectName, ROIPlus[] rois, String dimensionName)
	{
		if(rois.length == 0)
		{
			return null;
		}
		
		HashMap<DimensionMap,ROIPlus> datamap = new HashMap<DimensionMap,ROIPlus>();
		for (int index = 0; index < rois.length; index++)
		{
			ROIPlus roi = rois[index];
			DimensionMap map = new DimensionMap();
			map.put(dimensionName, "" + index);
			datamap.put(map, roi);
		}
		
		JEXData result = makeRoiObject(objectName, datamap);
		return result;
	}
	
	/**
	 * Make a one dimensional ROI object with a list of Rois at integer indexes for a dimension name DIMENSIONNAME
	 * 
	 * @param objectName
	 * @param roi
	 * @param dimensionName
	 * @return
	 */
	public static JEXData makeRoiObject(String objectName, Map<DimensionMap,ROIPlus> rois)
	{
		if(rois.size() == 0)
		{
			return null;
		}
		
		JEXData data = new JEXData(JEXData.ROI, objectName);
		data.put(JEXEntry.INFO, "No info inputed");
		data.put(JEXEntry.DATE, miscellaneous.DateUtility.getDate());
		data.put(JEXEntry.MODIFDATE, miscellaneous.DateUtility.getDate());
		
		for (DimensionMap map : rois.keySet())
		{
			ROIPlus roi = rois.get(map);
			String value = roi.getPointList().pointListString();
			String pattern = roi.getPattern().pointListString();
			JEXDataSingle ds = new JEXDataSingle();
			ds.put(JEXDataSingle.ROITYPE, "" + roi.type);
			ds.put(JEXDataSingle.POINTLIST, value);
			ds.put(JEXDataSingle.PATTERN, pattern);
			
			data.addData(map, ds);
			// result.put("roiType",""+roi.type);
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}

	/**
	 * Make a one dimensional ROI object from a map of ROI lists
	 * The ROIs will be indexed with a default roiID dimension added to the DimensionMap
	 * 
	 * @param objectName
	 * @param roi
	 * @param dimensionName
	 * @return
	 */
	public static JEXData makeRoiObjectFromListMap(String objectName, Map<DimensionMap,List<ROIPlus>> rois)
	{
		if(rois.size() == 0)
		{
			return null;
		}
		
		JEXData data = new JEXData(JEXData.ROI, objectName);
		data.put(JEXEntry.INFO, "No info inputed");
		data.put(JEXEntry.DATE, miscellaneous.DateUtility.getDate());
		data.put(JEXEntry.MODIFDATE, miscellaneous.DateUtility.getDate());
		
		for (DimensionMap map : rois.keySet())
		{
			// Get the list of rois at this location in the treemap
			List<ROIPlus> roiList = rois.get(map);
			
			// Start the roiID counter
			int roiID = 0;
			
			for (ROIPlus roi: roiList)
			{
				// Copy the dimensionmap to create a new one with the roiID in it
				DimensionMap mapDuplicate = map.copy();
				mapDuplicate.put("roiID", ""+roiID);
				roiID ++;
				
				// Create a jexdatasingle for this roi and add it to the datamap
				String value = roi.getPointList().pointListString();
				String pattern = roi.getPattern().pointListString();
				JEXDataSingle ds = new JEXDataSingle();
				ds.put(JEXDataSingle.ROITYPE, "" + roi.type);
				ds.put(JEXDataSingle.POINTLIST, value);
				ds.put(JEXDataSingle.PATTERN, pattern);
				data.addData(mapDuplicate, ds);
			}
			
		}
		
		if(data.getDataMap().size() == 0)
		{
			return null;
		}
		return data;
	}
	
}
