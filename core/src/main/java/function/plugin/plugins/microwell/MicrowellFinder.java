package function.plugin.plugins.microwell;

import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.old.JEX_SingleCell_MicrowellFinder;
import function.singleCellAnalysis.MicrowellTools;
import ij.ImagePlus;
import ij.process.ImageProcessor;
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
 */

@Plugin(
		type = JEXPlugin.class,
		name="Microwell Finder",
		menuPath="Microwells",
		visible=true,
		description="Find microwells from a microwell convolution image."
		)
public class MicrowellFinder extends JEXPlugin {

	public MicrowellFinder()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Convolved Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	@InputMarker(uiOrder=2, name="Regio ROI", type=MarkerConstants.TYPE_ROI, description="Region search for microwells.", optional=true)
	JEXData roiData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Conv. Threshold", description="Minimum hieght of a maximum.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double convThresh;
	
	@ParameterMarker(uiOrder=2, name="Conv. Tolerance", description="Minimum depth of valleys between maxima", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.5")
	double convTol;
	
	@ParameterMarker(uiOrder=3, name="Grid Spacing", description="Approximate number of pixels between microwell centers.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="75")
	int gridSpacing;
	
	@ParameterMarker(uiOrder=4, name="Grid Finding Tolerance [%]", description="Tolerance or estimated % error in microwell spacing (e.g. due to tilted image)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10.0")
	double gridTol;
	
	@ParameterMarker(uiOrder=5, name="Remove Wells On Grid Borders?", description="Whether or not to assume that a well exists between two wells (i.e. there is a row or column gap)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean excludeGridBorder;
	
	@ParameterMarker(uiOrder=6, name="Interpolate Missing Wells?", description="Whether or not to assume that a well exists between two wells (i.e. there is a row or column gap)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean interpolate;
	
	@ParameterMarker(uiOrder=7, name="Grid Smoothing Tolerance [%]", description="Smooth grid locations that don't fit the overall pattern to within X % of the grid spacing\n(negative values or values that result in 0 pixels tolerance result in skipping this step)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4.0")
	double smoothingTolerance;
	
	@ParameterMarker(uiOrder=7, name="Minimum Number of Wells", description="The minimum number of wells to find grouped together to consider the lattice of wells as the correct lattice", ui=MarkerConstants.UI_TEXTFIELD, defaultText="40")
	int minNumberOfWells;
	
	@ParameterMarker(uiOrder=8, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Microwells", type=MarkerConstants.TYPE_ROI, flavor="", description="An ROI of points indicating locations of microwells.", enabled=true)
	JEXData output;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry entry)
	{
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		DimTable filterTable = new DimTable(filterDimTableString);
		
		try
		{
			// Collect the inputs
			boolean roiProvided = false;
			if(roiData != null && roiData.getTypeName().getType().matches(JEXData.ROI))
			{
				roiProvided = true;
			}
			
			TreeMap<DimensionMap,ROIPlus> roiMap;
			// Run the function
			if(roiProvided)
			{
				roiMap = RoiReader.readObjectToRoiMap(roiData);
			}
			else
			{
				roiMap = new TreeMap<DimensionMap,ROIPlus>();
			}
			TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
			TreeMap<DimensionMap,ROIPlus> outputRoiMap = new TreeMap<DimensionMap,ROIPlus>();
			int count = 0, percentage = 0;
			ROIPlus roip;
			int counter = 0;
			for (DimensionMap map : imageMap.keySet())
			{
				if(filterTable.testMapAsExclusionFilter(map))
				{
					Logs.log("Skipping " + map.toString(), this);
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
					JEXStatics.statusBar.setProgressPercentage(percentage);
					continue;
				}
				if(this.isCanceled())
				{
					return false;
				}
				ImageProcessor imp = (new ImagePlus(imageMap.get(map))).getProcessor();
				roip = roiMap.get(map);
				
				Logs.log("Finding microwells for " + map, 0, JEX_SingleCell_MicrowellFinder.class);
				PointList points = MicrowellTools.findMicrowellCentersInConvolvedImage(imp, roip, convThresh, convTol, gridSpacing, gridTol, excludeGridBorder, minNumberOfWells, interpolate, smoothingTolerance);
				
				ROIPlus newRoip = new ROIPlus(points, ROIPlus.ROI_POINT);
				
				DimensionMap mapToSave = map.copy();
				
				outputRoiMap.put(mapToSave, newRoip);
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				counter = counter + 1;
				imp = null;
			}
			if(outputRoiMap.size() == 0)
			{
				return false;
			}
			
			this.output = RoiWriter.makeRoiObject("temp", outputRoiMap);
			
			// Return status
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}	
	}
	
	//	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	//	{
	//		// Get image data
	//		File f = new File(imagePath);
	//		if(!f.exists())
	//		{
	//			return null;
	//		}
	//		ImagePlus im = new ImagePlus(imagePath);
	//		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
	//		
	//		// Adjust the image
	//		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
	//		
	//		// Save the results
	//		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
	//		String imPath = JEXWriter.saveImage(toSave);
	//		im.flush();
	//		
	//		// return the filepath
	//		return imPath;
	//	}
}
