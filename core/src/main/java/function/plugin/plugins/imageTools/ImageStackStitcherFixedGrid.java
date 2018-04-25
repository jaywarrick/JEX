package function.plugin.plugins.imageTools;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.LabelReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import image.roi.PointList;
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
		name="Image Stack Stitcher (Fixed Grid)",
		menuPath="Image Tools > Stitching",
		visible=true,
		description="Function that allows you to stitch an image STACK into a single image by specifying a width and height to a grid spacing. Similar to Montage 1 Dim but can handle variably sized images and doesn't allow labels."
		)
public class ImageStackStitcherFixedGrid extends JEXPlugin {

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=3, name="Images", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;


	/////////// Define Parameters ///////////


	@ParameterMarker(uiOrder=1, name="Location Dim Name", description="Name of the row dimension in the imageset.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Location")
	String locDimName;

	@ParameterMarker(uiOrder=2, name="Number of Columns", description="Number of columns that make up the stitched image. (Can also specify a label name)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	String colsString;

	@ParameterMarker(uiOrder=3, name="Starting Point", description="In what corner is the first image of each image group to be stitched.", ui=MarkerConstants.UI_DROPDOWN, choices={"UL", "UR", "LL", "LR"}, defaultChoice=0)
	String startPt;

	@ParameterMarker(uiOrder=4, name="Horizontal First Movement?", description="From the start point, which direction is the next image, (checked = horizontal, unchecked = vertical)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean horizontal;

	@ParameterMarker(uiOrder=5, name="Snaking Path?", description="From the start point, which direction is the next image", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean snaking;

	@ParameterMarker(uiOrder=6, name="Grid Width", description="Horizontal grid spacing (before scaling) on which to place the upper left corner of each image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	int gridWidth;
	
	@ParameterMarker(uiOrder=7, name="Grid Height", description="Vertical grid spacing (before scaling) on which to place the upper left corner of each image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	int gridHeight;
	
	@ParameterMarker(uiOrder=8, name="Number of Pages", description="Number of pages to separate the result into.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int pages;
	
	@ParameterMarker(uiOrder=9, name="Size Scale", description="How much to multiply the image size (0.5 reduces image to half its original size)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double scale;

	@ParameterMarker(uiOrder=10, name="Intensity Multiplier", description="How much to multiply the image size (0.5 reduces image to half its original size)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double multiplier;

	@ParameterMarker(uiOrder=11, name="Output Bit Depth", description="Depth of the outputted image", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	int bitDepth;
	
	@ParameterMarker(uiOrder=12, name="Stitched Image BG Intensity", description="Intensity to set for the background of the stitched image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	double background;

	@ParameterMarker(uiOrder=13, name="Normalize Intensities Fit Bit Depth", description="Scale intensities to go from 0 to max value determined by new bit depth (\'true\' overrides intensity multiplier).", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean normalize;
	
	@ParameterMarker(uiOrder=14, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;


	////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Stitched Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant stitched image", enabled=true)
	JEXData output;

	public ImageStackStitcherFixedGrid()
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

	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry)
	{
		// Collect the inputs
		int horDxImage = this.gridWidth;
		int horDyImage = 0;

		int verDxImage = 0;
		int verDyImage = this.gridHeight;

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
		DimTable table = imageData.getDimTable();
		
		DimTable filterTable = new DimTable(this.filterDimTableString);

		// Get the number of columns
		Integer cols = -1;
		JEXData colsLabel = null;
		try
		{
			cols = Integer.parseInt(colsString);
		}
		catch(NumberFormatException e)
		{
			try
			{
				colsLabel = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, colsString), entry);
				if(colsLabel == null)
				{
					JEXDialog.messageDialog("Couldn't parse '" + colsString + "' to a number or to an existing label in this directory");
				}
				cols = Integer.parseInt(LabelReader.readLabelValue(colsLabel));
			}
			catch(NumberFormatException e2)
			{
				JEXDialog.messageDialog("Couldn't parse '" + colsString + "' to a number, so looked and found a corresponding label but couldn't parse the label value to an integer. Label value: " + LabelReader.readLabelValue(colsLabel));
			}
		}

		Dim locDim = table.getDimWithName(locDimName);
		if(locDim == null)
		{
			JEXDialog.messageDialog("No dimension called '" + locDimName + "' in the object for this entry: X" + entry.getTrayX() + " Y" + entry.getTrayY());
			return false;
		}
		int rows = (int)Math.ceil(((double) locDim.size())/((double) cols));
		Integer rowsPerPage = (int)Math.ceil(((double) rows)/((double) pages));

		// Run the function
		Vector<PointList> imageCoords = getPageMovements(horDxImage, horDyImage, verDxImage, verDyImage, rows, cols, horizontal, snaking, startPt, scale, pages);

		// Remove the row and col Dim's from the DimTable and iterate through it
		// and stitch.
		table.remove(locDim);
		Map<DimensionMap,String> stitchedImageFilePaths = new HashMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		for (DimensionMap partialMap : table.getDimensionMaps())
		{
			if(filterTable.testMapAsExclusionFilter(partialMap))
			{
				Logs.log("Skipping the processing and saving of " + partialMap.toString(), this);
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) table.mapCount())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				continue;
			}
			
			if(this.isCanceled())
			{
				return false;
			}
			try
			{
				List<DimensionMap> mapsToGet = ImageStackStitcher.getMapsForStitching(locDim, partialMap);
				for(int p = 0; p < pages; p++)
				{
					List<DimensionMap> pageOfMapsToGet = getPageOfMapsToGet(mapsToGet, cols, rowsPerPage, p);
					File stitchedFile = ImageStackStitcher.stitch(entry, imageData, pageOfMapsToGet, imageCoords.get(p), scale, normalize, multiplier, bitDepth, background, gridWidth, gridHeight, rowsPerPage);
					DimensionMap toSave = partialMap.copy();
					if(pages > 1)
					{
						toSave.put("Page", ""+p);
					}
					stitchedImageFilePaths.put(toSave, stitchedFile.getAbsolutePath());
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}

		}

		output = ImageWriter.makeImageStackFromPaths("temp", stitchedImageFilePaths);

		// Return status
		return true;
	}
	
	private List<DimensionMap> getPageOfMapsToGet(List<DimensionMap> mapsToGet, int cols, int rowsPerPage, int page)
	{
		// Assume pages start at 0
		int first = (page)*rowsPerPage * cols;
		int onePastLast = (page + 1)*rowsPerPage * cols;
		Vector<DimensionMap> pageOfMapsToGet = new Vector<>();
		for(int i=first; i < onePastLast && i < mapsToGet.size(); i++)
		{
			pageOfMapsToGet.add(mapsToGet.get(i));
		}
		return pageOfMapsToGet;
	}
	
	public static Vector<PointList> getPageMovements(int horDxImage, int horDyImage, int verDxImage, int verDyImage, int rows, int cols, boolean horizontal, boolean snaking, String startPt, double scale, int pages)
	{
		Vector<PointList> ret = new Vector<>();
		Integer rowsPerPage = (int)Math.ceil(((double) rows)/((double) pages));
		for(int p = 0; p < pages; p++)
		{
			ret.add(ImageStackStitcher.getMovements(horDxImage, horDyImage, verDxImage, verDyImage, rowsPerPage.intValue(), cols, horizontal, snaking, startPt, scale));
		}
		return ret;
	}
}
