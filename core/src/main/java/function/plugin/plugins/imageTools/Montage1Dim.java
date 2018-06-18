package function.plugin.plugins.imageTools;

import java.awt.Color;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.imageUtility.jMontageMaker;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import jex.statics.JEXDialog;
import jex.utilities.ImageUtility;
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
		name="Montage 1 Dim",
		menuPath="Image Tools > Stitching",
		visible=true,
		description="Creates a montage for each image along a single dimension of a multi-dimensional image set (e.g., a montage of all the colors for each time in a time series)."
		)
public class Montage1Dim extends JEXPlugin {

	public Montage1Dim()
	{}

	/////////// Define Inputs here ///////////

	@InputMarker(uiOrder=1, name="Image Stack", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=0, name="Dim Name to Montage", description="Name of the dimension within the image set that contains the list of images to put into each montage.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Color")
	String dimName;

	@ParameterMarker(uiOrder=1, name="Size Scale Factor", description="Amount to scale the image before creating montage.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double scale;

	@ParameterMarker(uiOrder=5, name="Number of Columns", description="Number of columns to make the montage.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	int cols;

	@ParameterMarker(uiOrder=7, name="Pixel Spacing", description="Number of pixels between each image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5")
	int spacing;

	@ParameterMarker(uiOrder=8, name="Background/Border Color", description="Color of pixels for blank regions of montage (i.e., missing images and borders)", ui=MarkerConstants.UI_DROPDOWN, choices={"White","Black","Gray","Yellow"}, defaultChoice=0)
	String borderColor;

	@ParameterMarker(uiOrder=9, name="Label Color", description="Color of pixels used for the labels", ui=MarkerConstants.UI_DROPDOWN, choices={"White","Black","Gray","Yellow"}, defaultChoice=1)
	String labelColor;
	
	@ParameterMarker(uiOrder=10, name="Force Colored Image Output", description="Color of pixels used for the labels / borders", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean forceColor;

	@ParameterMarker(uiOrder=11, name="Add Labels?", description="Whether to add labels corresponding to the dim values or not.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean labels;
	
	@ParameterMarker(uiOrder=12, name="Font Size", description="Font size of the labels", ui=MarkerConstants.UI_TEXTFIELD, defaultText="18")
	int fontSize;
	
	@ParameterMarker(uiOrder=13, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;

	/////////// Define Outputs here ///////////

	@OutputMarker(uiOrder=1, name="Montage Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant montaged image (set)", enabled=true)
	JEXData outputData;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------

	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Collect the inputs
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		DimTable table = imageData.getDimTable();
		Dim stackDim = table.getDimWithName(dimName);
		
		if(stackDim == null)
		{
			JEXDialog.messageDialog("Couldn't find dimension with specified name.");
			return false;
		}
		
		DimTable filterTable = new DimTable(this.filterDimTableString);

		// Run the function
		// Get the Partial DimTable and iterate through it and stitch.
		DimTable partialTable = table.copy();
		partialTable.remove(stackDim);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		if(partialTable.size() == 0)
		{
			// Get the list of maps to montage
			List<DimensionMap> mapsToGet = table.getDimensionMaps();

			// Then make montage
			ImagePlus montage = makeMontageFromJEXStack(mapsToGet, imageMap, cols);

			// Save the montage
			String path = JEXWriter.saveImage(montage);

			// Return the result to the database
			outputData = ImageWriter.makeImageObject("temp", path);
		}
		else
		{
			for (DimensionMap partialMap : partialTable.getDimensionMaps())
			{
				if(this.isCanceled())
				{
					return false;
				}
				List<DimensionMap> mapsToGet = this.getMapsForStitching(stackDim, partialMap, imageMap, filterTable);

				// Then make montage
				ImagePlus montage = makeMontageFromJEXStack(mapsToGet, imageMap, cols);

				// Save the montage
				if(montage != null)
				{
					String path = JEXWriter.saveImage(montage);	
					outputMap.put(partialMap, path);
				}
			}

			// Set the outputs
			outputData = ImageWriter.makeImageStackFromPaths("temp", outputMap);
		}

		// Return status
		return true;
	}

	public ImagePlus makeMontageFromJEXStack(List<DimensionMap> maps, TreeMap<DimensionMap,String> files, int cols)
	{
		ImagePlus im = ImageUtility.makeImagePlusStackFromJEXStack(maps, files);
		
		if(im == null)
		{
			return null;
		}

		int rows = (int) Math.ceil(((double) maps.size())/((double) cols));

		ImagePlus ret = jMontageMaker.makeMontage(im, cols, rows, this.scale, 1, maps.size(), 1, this.spacing, this.labels, false, this.getColor(this.labelColor), this.getColor(this.borderColor), this.fontSize, true, this.forceColor);
		return ret;
	}

	private List<DimensionMap> getMapsForStitching(Dim stackDim, DimensionMap partialMap, TreeMap<DimensionMap,String> images, DimTable filterTable)
	{
		List<DimensionMap> ret = new Vector<DimensionMap>();
		for (int z = 0; z < stackDim.size(); z++)
		{
			DimensionMap imageMap = partialMap.copy();
			imageMap.put(stackDim.name(), stackDim.valueAt(z));
			if(images.get(imageMap) != null && !filterTable.testMapAsExclusionFilter(imageMap))
			{
				Logs.log("Adding: " + imageMap.toString() + " ---> " + images.get(imageMap), this);
				ret.add(imageMap);
			}
		}
		return ret;
	}

	private Color getColor(String color)
	{
		if(color.equals("White")) return Color.WHITE;
		if(color.equals("Black")) return Color.BLACK;
		if(color.equals("Gray")) return Color.GRAY;
		if(color.equals("Yellow")) return Color.YELLOW;
		else return Color.WHITE;
	}
}
