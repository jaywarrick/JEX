package function.plugin.plugins.imageTools;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import cruncher.Ticket;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import image.roi.PointList;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import miscellaneous.StatisticsUtility;
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
		name="Make Entry Montage",
		menuPath="Image Tools > Stitching",
		visible=true,
		description="Function that allows you to stitch an image STACK into a single image by specifying a width and height to a grid spacing. Similar to Montage 1 Dim but can handle variably sized images and doesn't allow labels."
		)
public class MakeEntryMontage extends JEXPlugin {

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=3, name="Images", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;


	/////////// Define Parameters ///////////


	//	@ParameterMarker(uiOrder=1, name="Location Dim Name", description="Name of the row dimension in the imageset.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Location")
	//	String locDimName;
	//
	//	@ParameterMarker(uiOrder=2, name="Number of Columns", description="Number of columns that make up the stitched image. (Can also specify a label name)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	//	String colsString;

	//	@ParameterMarker(uiOrder=3, name="Starting Point", description="In what corner is the first image of each image group to be stitched.", ui=MarkerConstants.UI_DROPDOWN, choices={"UL", "UR", "LL", "LR"}, defaultChoice=0)
	//	String startPt = "UL";

	//	@ParameterMarker(uiOrder=4, name="Horizontal First Movement?", description="From the start point, which direction is the next image, (checked = horizontal, unchecked = vertical)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	//	boolean horizontal = true;

	//	@ParameterMarker(uiOrder=5, name="Snaking Path?", description="From the start point, which direction is the next image", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	//	boolean snaking = false;

	@ParameterMarker(uiOrder=6, name="Grid Width", description="Horizontal grid spacing (before scaling) on which to place the upper left corner of each image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	int gridWidth;

	@ParameterMarker(uiOrder=7, name="Grid Height", description="Vertical grid spacing (before scaling) on which to place the upper left corner of each image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	int gridHeight;

	//	@ParameterMarker(uiOrder=8, name="Number of Pages", description="Number of pages to separate the result into.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	//	int pages;

	@ParameterMarker(uiOrder=9, name="Size Scale", description="How much to multiply the image size (0.5 reduces image to half its original size)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double scale;

	@ParameterMarker(uiOrder=10, name="Intensity Multiplier", description="How much to multiply the image intensity (0.5 reduces image to half its original size)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double multiplier;

	@ParameterMarker(uiOrder=11, name="Output Bit Depth", description="Depth of the outputted image", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	int bitDepth;

	@ParameterMarker(uiOrder=12, name="Stitched Image BG Intensity", description="Intensity to set for the background of the stitched image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	double background;

	@ParameterMarker(uiOrder=13, name="Normalize Intensities Fit Bit Depth", description="Scale intensities to go from 0 to max value determined by new bit depth (\'true\' overrides intensity multiplier).", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean normalize;


	////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Stitched Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant stitched image", enabled=true)
	JEXData output;

	public MakeEntryMontage()
	{}

	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------


	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------

	public static TreeMap<DimensionMap,String> allImages = new TreeMap<>();
	public static boolean horizontal = true;
	public static boolean snaking = false;
	public static String startPt = "UL";
	public static double finalScale = 1;


	/**
	 * Perform the algorithm here
	 * 
	 */
	public boolean run(JEXEntry entry)
	{
		// Collect the inputs
		if(imageData == null)
		{
			JEXDialog.messageDialog("No object by that name exists for this entry: X" + entry.getTrayX() + " Y" + entry.getTrayY());
			return false;
		}
		// Collect the inputs
		if(!imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			JEXDialog.messageDialog("The object provided is not an 'Image' object.");
			return false;
		}

		TreeMap<DimensionMap,String> entryImages = ImageReader.readObjectToImagePathTable(imageData);
		for(Entry<DimensionMap,String> e : entryImages.entrySet())
		{
			DimensionMap toStore = e.getKey().copyAndSet("ArrayX=" + entry.getTrayX() + ",ArrayY=" + entry.getTrayY());
			allImages.put(toStore, e.getValue());
		}

		finalScale = scale;
		return true;
	}


	public TreeMap<DimensionMap,String> runStitch()
	{
		// Collect the inputs
		int horDxImage = this.gridWidth;
		int horDyImage = 0;

		int verDxImage = 0;
		int verDyImage = this.gridHeight;

		// Get the number of columns
		DimTable table = new DimTable(allImages);
		Dim colDim = table.getDimWithName("ArrayX");
		Dim rowDim = table.getDimWithName("ArrayY");

		double xMin = StatisticsUtility.min(colDim.doubleValues());
		double xMax = StatisticsUtility.max(colDim.doubleValues());
		double yMin = StatisticsUtility.min(rowDim.doubleValues());
		double yMax = StatisticsUtility.max(rowDim.doubleValues());

		if(rowDim == null || colDim == null)
		{
			return null;
		}
		int rows = (int) (yMax-yMin) + 1;
		int cols = (int) (xMax-xMin) + 1;

		// Run the function
		ImageStitcher is = new ImageStitcher();
		PointList imageCoords = is.getMovements(horDxImage, horDyImage, verDxImage, verDyImage, rows, cols, horizontal, snaking, startPt, scale);

		// Remove the row and col Dim's from the DimTable and iterate through it
		// and stitch.
		table.remove(rowDim);
		table.remove(colDim);
		TreeMap<DimensionMap,String> stitchedImageFilePaths = new TreeMap<DimensionMap,String>();
		for (DimensionMap partialMap : table.getDimensionMaps())
		{
			if(this.isCanceled())
			{
				return null;
			}
			try
			{
				List<DimensionMap> mapsToGet = is.getMapsForStitching(rowDim, colDim, partialMap);
				// Progress bar updating is handled in ImageStitcher.stitch(...)
				File stitchedFile = ImageStitcher.stitch("All", "All", allImages, mapsToGet, imageCoords, scale, normalize, multiplier, bitDepth);
				stitchedImageFilePaths.put(partialMap, stitchedFile.getAbsolutePath());
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}

		}

		return stitchedImageFilePaths;
	}

	// run after ending
	public void finalizeTicket(Ticket ticket)
	{
		TreeMap<DimensionMap,String> imageMapToSave = this.runStitch();
		TreeMap<JEXEntry,Set<JEXData>> outputList = ticket.getOutputList();
		JEXEntry first = null;
		for(JEXEntry entry : outputList.keySet())
		{
			if(first == null)
			{
				first = entry;
			}
			if(entry.getTrayX() == first.getTrayX())
			{
				if(entry.getTrayY() < first.getTrayY())
				{
					first = entry;
				}
			}
			else if(entry.getTrayX() < first.getTrayX())
			{
				first = entry;
			}
		}
		Set<JEXData> set = outputList.get(first);
		set.clear();
		JEXData toSave = ImageWriter.makeImageStackFromPaths(ticket.getOutputNames()[0].getName(), imageMapToSave);
		set.add(toSave);
		// Remember to reset the static variable again so this data isn't carried over to other function runs
		allImages.clear();
		JEXStatics.statusBar.setProgressPercentage(0);
	}


}
