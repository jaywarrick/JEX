package function.plugin.plugins.roi;

import java.awt.Rectangle;
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
import function.plugin.plugins.featureExtraction.FeatureUtils;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import tables.Dim;
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
		name="Register Region ROI",
		menuPath="ROI",
		visible=true,
		description="Use the crop roi output from image registration to move/register a region roi (helpful for maintaining a consistent region of interest in jittery timelpase)."
		)
public class RegisterRegionRoi extends JEXPlugin {

	public RegisterRegionRoi()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Region ROI", type=MarkerConstants.TYPE_ROI, description="The Region ROI to be moved/registered.", optional=false)
	JEXData regionData;
	
	@InputMarker(uiOrder=2, name="Crop ROI", type=MarkerConstants.TYPE_ROI, description="ROI defining the relative positions to move the Region ROI (see Crop ROI output of Register Multicolor Image - Roi)", optional=false)
	JEXData cropData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Time Dim Name", description="Name of the 'Time' dimension.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Time")
	String timeDimName;
	//	
	//	@ParameterMarker(uiOrder=6, name="Move to origin first?", description="Move the ROI to the origin first before any scale/translate/rotate operation? (typically false, primarily use to move )", ui=MarkerConstants.UI_DROPDOWN, choices={"No","Yes - UL Corner", "Yes - Center"}, defaultChoice=0)
	//	String moveToOrigin;
	//	
	//	@ParameterMarker(uiOrder=1, name="Scale", description="Multiplier to scale locations/size of all ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	//	double scale;
	//	
	//	@ParameterMarker(uiOrder=2, name="Rotate (deg, CCW)", description="angle of rotation around the point indicated in ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	//	double rotation;
	//	
	//	@ParameterMarker(uiOrder=3, name="Translate X", description="X displacement", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	//	double deltaX;
	//	
	//	@ParameterMarker(uiOrder=4, name="Translate Y", description="Y displacement", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	//	double deltaY;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Registered Region", type=MarkerConstants.TYPE_ROI, flavor="", description="The roi containing the registered maxima.", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	FeatureUtils utils = new FeatureUtils();
	
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		cropData.getDataMap();
		if(cropData == null || !cropData.getTypeName().getType().matches(JEXData.ROI))
		{
			JEXDialog.messageDialog("A Median Deltas ROI must be provided. Aborting.", this);
			return false;
		}
		
		regionData.getDataMap();
		if(regionData == null || !regionData.getTypeName().getType().matches(JEXData.ROI))
		{
			JEXDialog.messageDialog("A Region ROI must be provided. Aborting.", this);
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,ROIPlus> cropMap = RoiReader.readObjectToRoiMap(cropData);
		TreeMap<DimensionMap,ROIPlus> regionMap = RoiReader.readObjectToRoiMap(regionData);
		TreeMap<DimensionMap,ROIPlus> adjusted = new TreeMap<DimensionMap,ROIPlus>();
		Rectangle rect = this.getBoundingRegionOfCropRoi(cropMap);
		int count = 0, percentage = 0;
		DimTable all = regionData.getDimTable();
		Dim timeDim = all.getDimWithName(this.timeDimName);
		DimTable allButTime = all.getSubTable(this.timeDimName);
		for (DimensionMap map : allButTime.getMapIterator())
		{
			boolean first = true;
			ROIPlus firstCrop = null;
			for(String t : timeDim.values())
			{

				DimensionMap map2 = map.copyAndSet(this.timeDimName + "=" + t);
				// Copy the region
				ROIPlus regionROI = regionMap.get(map2).copy();
				ROIPlus cropROI = cropMap.get(map2);
				
				if(regionROI == null || cropROI == null)
				{
					Logs.log("Either no maxima roi or no crop roi at " + map2 + ". Skipping.", this);
					continue;
				}
				
				if(first)
				{
					firstCrop = cropROI;
				}
				
				this.registerRegionRoi(regionROI, cropROI, rect, firstCrop);
				
				// Save the translated copy
				adjusted.put(map2, regionROI);
				first = false;
				
				// Update progress
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) regionMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}
		if(adjusted.size() == 0)
		{
			return false;
		}

		this.output = RoiWriter.makeRoiObject("temp", adjusted);

		// Return status
		return true;
	}
	
	public Rectangle getBoundingRegionOfCropRoi(TreeMap<DimensionMap,ROIPlus> cropMap)
	{
		PointList pl = new PointList();
		for(ROIPlus crop : cropMap.values())
		{
			pl.addAll(crop.getPointList());
		}
		return pl.getBounds();
	}
	
	public void registerRegionRoi(ROIPlus region, ROIPlus cropROI, Rectangle cropBounds, ROIPlus firstCrop)
	{
		IdPoint centerBounds = PointList.getCenter(cropBounds);
		IdPoint centerCrop = cropROI.getPointList().getCenter();
		IdPoint centerFirst = firstCrop.getPointList().getCenter();
		
		int dx = (centerCrop.x - centerBounds.x) - (centerFirst.x - centerBounds.x);
		int dy = (centerCrop.y - centerBounds.y) - (centerFirst.y - centerBounds.y);
		
		// Translate the points to the new origin.
		region.getPointList().translate(dx, dy);
	}
}
