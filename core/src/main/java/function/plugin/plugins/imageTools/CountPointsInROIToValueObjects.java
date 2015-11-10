package function.plugin.plugins.imageTools;

import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Point;
import java.awt.Shape;
import java.util.Iterator;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import miscellaneous.Canceler;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import Database.DataWriter.ValueWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

/**
 * Counts the number of points which fall within a given regional ROI for all regions and images.
 * Also counts the total points, including those outside of the regional ROIs.
 *
 * The same as JEX_ImageTools_CountPointsInROI, but in new Plugin version
 *
 * Exports results as Value objects
 * 
 * @author jaywarrick, updated to new Plugin version by daveniles
 *
 */
@Plugin(
		type = JEXPlugin.class,
		name="Count Points in ROI To Value Objects",
		menuPath="Image Tools",
		visible=true,
		description="Counts the number of points in an image array " +
				"and the number within a regional ROI, " +
				"and exports the results as File objects."
		)
public class CountPointsInROIToValueObjects extends JEXPlugin {

	// Define a constructor that takes no arguments.
	public CountPointsInROIToValueObjects()
	{}

	/////////// Define Inputs ///////////
	@InputMarker(uiOrder=1, name="Point ROI", type=MarkerConstants.TYPE_ROI, description="Points to be counted", optional=false)
	JEXData pointData;

	@InputMarker(uiOrder=2, name="Region ROI (Optional)", type=MarkerConstants.TYPE_ROI, description="Indicates which points should be included", optional=false)
	JEXData regionData;


	/////////// Define Parameters ///////////
	@ParameterMarker(uiOrder=1, name="Dummy Parameter", description="Lets the user know that the function has been selected", ui=MarkerConstants.UI_DROPDOWN, choices={"True"}, defaultChoice=0)
	String dummy;
	

	/////////// Define Outputs ///////////
	@OutputMarker(uiOrder=1, name="Contained ROI Points", type=MarkerConstants.TYPE_ROI, 
			flavor="", description="Points counted", enabled=true)
	JEXData output1;

	@OutputMarker(uiOrder=2, name="Region Counts", type=MarkerConstants.TYPE_VALUE, 
			flavor="", description="Number of points in each region of Region ROI (if applicable)", enabled=false)
	JEXData output2;

	@OutputMarker(uiOrder=3, name="Total Region Count", type=MarkerConstants.TYPE_VALUE, 
			flavor="", description="Total number of points across all regions in Region ROI (if applicable)", enabled=false)
	JEXData output3;

	@OutputMarker(uiOrder=4, name="Image Counts", type=MarkerConstants.TYPE_VALUE, 
			flavor="", description="Number of points within each image", enabled=false)
	JEXData output4;

	@OutputMarker(uiOrder=5, name="Total Image Count", type=MarkerConstants.TYPE_VALUE, 
			flavor="", description="Total number of points across all images", enabled=false)
	JEXData output5;

	
	// No multithreading
	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	
	// 
	@SuppressWarnings("unchecked")
	@Override
	public boolean run(JEXEntry entry)
	{

		TreeMap<String, Object> results = calculate(pointData, regionData, this.canceler);

		// Unpack the results
		TreeMap<DimensionMap,ROIPlus> outputROI = (TreeMap<DimensionMap,ROIPlus>) results.get("outputROI");
		TreeMap<DimensionMap,String> regionCounts = (TreeMap<DimensionMap,String>) results.get("regionCounts");
		TreeMap<DimensionMap,String> imageCounts = (TreeMap<DimensionMap,String>)results.get("imageCounts");
		TreeMap<DimensionMap,String> totalRegionCount = (TreeMap<DimensionMap,String>) results.get("totalRegionCount");
		TreeMap<DimensionMap,String> totalImageCount = (TreeMap<DimensionMap,String>) results.get("totalImageCount");
		
		// Set outputs
		this.output1 = RoiWriter.makeRoiObject(this.output1.name, outputROI);
		this.output2 = ValueWriter.makeValueTable(this.output2.name, regionCounts);
		this.output3 = ValueWriter.makeValueTable(this.output3.name, totalRegionCount);
		this.output4 = ValueWriter.makeValueTable(this.output4.name, imageCounts);
		this.output5 = ValueWriter.makeValueTable(this.output5.name, totalImageCount);

		// Return status
		return true;
	}

	public static TreeMap<String,Object> calculate(JEXData pointData, JEXData regionData, Canceler canceler)
	{
		// Validate Point ROI (required)
		if(pointData == null || !pointData.getTypeName().getType().equals(JEXData.ROI))
			return null;

		// Validate Region ROI (optional)
		if(regionData != null)
			if(!regionData.getTypeName().getType().equals(JEXData.ROI))
				return null;

		// Initialize some variables
		int progressCount = 0,   progressPercent = 0;	// For monitoring progress
		int totalImageCount = 0, totalRegionCount = 0;	// Final totals across all images and ROI regions
		ROIPlus point, region;							
		Shape regionShape;								
		TreeMap<DimensionMap,ROIPlus> outputROI = new TreeMap<DimensionMap,ROIPlus>();	// Contained points
		TreeMap<DimensionMap,String> regionCounts = new TreeMap<DimensionMap,String>();	// Counts for each region
		TreeMap<DimensionMap,String> imageCounts = new TreeMap<DimensionMap,String>(); 	// Counts for each image	
		TreeMap<DimensionMap,String> mapTotalRegionCount = new TreeMap<DimensionMap,String>();	// Final total converted to a map
		TreeMap<DimensionMap,String> mapTotalImageCount = new TreeMap<DimensionMap,String>();	// Final total converted to a map
		TreeMap<String,Object> out = new TreeMap<String,Object>();	// Contains all counts
		
		// Read in the ROIs
		TreeMap<DimensionMap,ROIPlus> pointROI = RoiReader.readObjectToRoiMap(pointData);
		TreeMap<DimensionMap,ROIPlus> regionROI = RoiReader.readObjectToRoiMap(regionData);

		// Loop through the items in the n-Dimensional object
		for (DimensionMap map : pointROI.keySet())
		{
			// Cancel the function if applicable
			if(canceler.isCanceled())
			{
				return null;
			}

			DimensionMap mapToSave = map.copy();
			point = pointROI.get(map);
			int singleImageCount = 0;
			int singleRegionCount = 0;

			// If no Region ROI was specified, count everything.
			// -1 is used to indicate value doesn't exist.  I should change this later to something better.
			if(regionData==null)
			{
				totalRegionCount = -1;
				PointList pl = new PointList();
				for (Point p : point.pointList)
				{
					pl.add(p);
					singleImageCount = singleImageCount + 1;
					totalImageCount = totalImageCount + 1;
				}
				mapToSave.put("SubRegionNumber", ""+ singleRegionCount);
				outputROI.put(mapToSave.copy(), new ROIPlus(pl, ROIPlus.ROI_POINT));
				regionCounts.put(mapToSave.copy(), "" + (-1));
				imageCounts.put(mapToSave.copy(), "" + singleImageCount);
			}
			// Otherwise count within each region
			else
			{
				region = regionROI.get(map);	

				// Loop through each region in the image
				Iterator<ROIPlus> itrRoi = region.patternRoiIterator();
				while(itrRoi.hasNext())
				{
					ROIPlus subRegion = itrRoi.next();
					regionShape = subRegion.getShape();
					PointList pl = new PointList();
					for (Point p : point.pointList)
					{
						if(regionShape.contains(p))
						{
							pl.add(p);
							totalRegionCount = totalRegionCount + 1;
						}
						singleImageCount = singleImageCount + 1;
						totalImageCount = totalImageCount + 1;
					}
					mapToSave.put("SubRegionNumber", ""+singleRegionCount);
					outputROI.put(mapToSave.copy(), new ROIPlus(pl, ROIPlus.ROI_POINT));
					regionCounts.put(mapToSave.copy(), "" + pl.size());
					imageCounts.put(mapToSave.copy(), "" + singleImageCount);
					singleRegionCount = singleRegionCount + 1;
				}
			}

			// Update the user interface with progress
			progressCount = progressCount + 1;
			progressPercent = (int) (100 * ((double) (progressCount) / ((double) pointROI.size())));
			JEXStatics.statusBar.setProgressPercentage(progressPercent);
		}
			
		DimensionMap map = new DimensionMap();	// temporary
		mapTotalImageCount.put(map.copy(),""+totalImageCount);
		mapTotalRegionCount.put(map.copy(), ""+totalRegionCount);
			
		// Pack everything up and return it.
		out.put("outputROI", outputROI);
		out.put("regionCounts", regionCounts);
		out.put("totalRegionCount", mapTotalRegionCount);
		out.put("imageCounts", imageCounts);
		out.put("totalImageCount", mapTotalImageCount);		
		
		return out;
	}
}
