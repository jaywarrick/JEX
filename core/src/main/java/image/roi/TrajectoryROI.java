package image.roi;

import java.util.TreeMap;

import tables.DimensionMap;

public class TrajectoryROI {
	
	public TreeMap<DimensionMap,ROIPlus> roiMap;
	
	public TrajectoryROI(TreeMap<DimensionMap,ROIPlus> roiMap)
	{
		// test if the roiMap is valid for a trajectory
		boolean test = isRoiMapATrajectory(roiMap);
		if(!test)
			return;
		
		this.roiMap = roiMap;
	}
	
	public void setRoiMap(TreeMap<DimensionMap,ROIPlus> roiMap)
	{
		this.roiMap = roiMap;
	}
	
	public TreeMap<DimensionMap,ROIPlus> getRoiMap()
	{
		return this.roiMap;
	}
	
	// ----------------------------------------------------------
	// ------ TESTER ------------------
	// ----------------------------------------------------------
	
	/**
	 * Tests if the dimension of the DimensionMap is 1, i.e. a stack and if all the dimensions have the same name
	 */
	private boolean isRoiMapATrajectory(TreeMap<DimensionMap,ROIPlus> roiMap)
	{
		boolean result = true;
		String dimName = null;
		for (DimensionMap map : roiMap.keySet())
		{
			if(map.size() != 1)
			{
				result = false;
				break;
			}
			
			String thisDim = map.getDimensionArray()[0];
			if(dimName == null)
			{
				dimName = thisDim;
			}
			else if(!dimName.equals(thisDim))
			{
				result = false;
				break;
			}
		}
		return result;
	}
	
	// ----------------------------------------------------------
	// ------ FIND POINTS AND FOLLOW THE TRACK ------------------
	// ----------------------------------------------------------
	
	// ----------------------------------------------------------
	// ------ STATISTICS AND MEASURES----------------------------
	// ----------------------------------------------------------
	
}
