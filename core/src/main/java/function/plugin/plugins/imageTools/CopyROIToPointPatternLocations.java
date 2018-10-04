package function.plugin.plugins.imageTools;

import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import image.roi.PointList;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
import logs.Logs;
import tables.DimTable;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */
@Plugin(
		type = JEXPlugin.class,
		name="Copy ROI to Point Pattern Locations",
		menuPath="Image Tools",
		visible=true,
		description="'Copy' an ROI to locations defined by a point ROI to create a patterned ROI (i.e., the 'pattern' of the points is saved into the ROI)."
		)
public class CopyROIToPointPatternLocations extends JEXPlugin {

	public CopyROIToPointPatternLocations()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="ROI to Copy", type=MarkerConstants.TYPE_ROI, description="ROI to be copied. Be sure a template exists for each image / dimension map.", optional=false)
	JEXData roiData;
	
	@InputMarker(uiOrder=2, name="Point ROI", type=MarkerConstants.TYPE_ROI, description="Locations where the ROI is to be copied to.", optional=false)
	JEXData pointRoiData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=5, name="ROI Origin", description="Which location within the roi should be aligned to the point locations?", ui=MarkerConstants.UI_DROPDOWN, choices={ ROIPlus.ORIGIN_CENTER, ROIPlus.ORIGIN_UPPERLEFT, ROIPlus.ORIGIN_UPPERRIGHT, ROIPlus.ORIGIN_LOWERRIGHT, ROIPlus.ORIGIN_LOWERLEFT }, defaultChoice=0)
	String roiOrigin;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Pattern ROI", type=MarkerConstants.TYPE_ROI, flavor="", description="The resultant patterned ROI object", enabled=true)
	JEXData output;

	
	/**
	 * Perform the algorithm here
	 * 
	 */
	public boolean run(JEXEntry entry)
	{

		JEXStatics.statusBar.setProgressPercentage(0);

		// Collect the inputs
		if(this.roiData == null || !this.roiData.getTypeName().getType().matches(JEXData.ROI))
			return false;
		if(this.pointRoiData == null || !this.pointRoiData.getTypeName().getType().matches(JEXData.ROI))
			return false;

		// Run the function
		DimTable pointRoiTable = pointRoiData.getDimTable();
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,ROIPlus> pointRoiMap = RoiReader.readObjectToRoiMap(pointRoiData);
		int count = 0, percentage = 0, total = pointRoiTable.getDimensionMaps().size();

		// Make the rois and put them in the arrayRoiMap
		TreeMap<DimensionMap,ROIPlus> arrayRoiMap = new TreeMap<DimensionMap,ROIPlus>();
		for (DimensionMap roiM : pointRoiMap.keySet())
		{

			// Get the array Locations
			ROIPlus patternRoi = pointRoiMap.get(roiM);
			if(patternRoi == null || patternRoi.pointList.size() == 0)
			{
				Logs.log("Couldn't read the roi that defines the array or no points defined for this image: " + roiM.toString(), this);
				JEXStatics.statusBar.setStatusText("Couldn't read the roi that defines the arrayor no points defined for this image: " + roiM.toString());
				continue;
			}
			PointList pattern = patternRoi.getPointList();

			// Get the ROI to be copied.
			ROIPlus roip = roiMap.get(roiM);
			
			ROIPlus roip2 = roip.copy();
			
			// Put the center at of the roi at the first pattern point.
			roip2.getPointList().setCenter(pattern.get(0));
			
			// Now shift the roi according to which part of the roi should be positioned over the point
			this.shift(roip2.getPointList());
			
			// Now create a pattern point list which always start with 0,0 (i.e., no translation relative to the current position of the roi) and is relative from there
			PointList relativePattern = pattern.copy();
			relativePattern.translate(-1*relativePattern.get(0).x, -1*relativePattern.get(0).y);
			
			// Now set the new patter point list as the pattern for this roi
			roip2.setPattern(relativePattern);
			
			// save the new patterned roi
			arrayRoiMap.put(roiM, roip2);

			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		this.output = RoiWriter.makeRoiObject("dummy", arrayRoiMap);
		
		// Return status
		return true;
	}
	
	private void shift(PointList pl)
	{
		int width = pl.getBounds().width;
		int height = pl.getBounds().height;
		
		if(roiOrigin.equals(ROIPlus.ORIGIN_CENTER))
		{
			// Don't do anything
		}
		else if(roiOrigin.equals(ROIPlus.ORIGIN_LOWERLEFT))
		{
			pl.translate(width / 2, -height / 2);
		}
		else if(roiOrigin.equals(ROIPlus.ORIGIN_LOWERRIGHT))
		{
			pl.translate(-width / 2, -height / 2);
		}
		else if(roiOrigin.equals(ROIPlus.ORIGIN_UPPERLEFT))
		{
			pl.translate(width / 2, height / 2);
		}
		else if(roiOrigin.equals(ROIPlus.ORIGIN_UPPERRIGHT))
		{
			pl.translate(-width / 2, height / 2);
		}
	}

	//	private ROIPlus getRoi(String roiOrigin, int roiType, int roiWidth, int roiHeight)
	//	{
	//		PointList pl = new PointList();
	//		Point p1 = new Point(0, 0);
	//		Point p2 = new Point(roiWidth, roiHeight);
	//
	//		ROIPlus ret = null;
	//
	//		if(roiType == ROIPlus.ROI_POINT)
	//		{
	//			pl.add(p1);
	//			ret = new ROIPlus(pl, roiType);
	//			return ret;
	//		}
	//		else
	//		{
	//			pl.add(p1);
	//			pl.add(p2);
	//		}
	//
	//		double width = (double) roiWidth;
	//		double height = (double) roiHeight;
	//
	//		if(roiOrigin.equals("Center"))
	//		{
	//			pl.translate(-width / 2, -height / 2);
	//		}
	//		else if(roiOrigin.equals("Upper-Right"))
	//		{
	//			pl.translate(-width, 0);
	//		}
	//		else if(roiOrigin.equals("Lower-Left"))
	//		{
	//			pl.translate(0, -height);
	//		}
	//		else if(roiOrigin.equals("Lower-Right"))
	//		{
	//			pl.translate(-width, -height);
	//		}
	//
	//		ret = new ROIPlus(pl, roiType);
	//		return ret;
	//	}
}
