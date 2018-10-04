package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ValueReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import image.roi.PointList;

import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import miscellaneous.CSVList;
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
public class JEX_ImageTools_Stitch_RectCoord extends JEXCrunchable {
	
	public static final int LtoR = 0, TtoB = 1;
	
	public JEX_ImageTools_Stitch_RectCoord()
	{}
	
	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------
	
	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	@Override
	public String getName()
	{
		String result = "Stitch 1 Dim Using Alignments";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	@Override
	public String getInfo()
	{
		String result = "Function that allows you to stitch an image ARRAY into a single image using two image alignment objects.";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
	@Override
	public String getToolbox()
	{
		String toolbox = "Image tools";
		return toolbox;
	}
	
	/**
	 * This method defines if the function appears in the list in JEX It should be set to true expect if you have good reason for it
	 * 
	 * @return true if function shows in JEX
	 */
	@Override
	public boolean showInList()
	{
		return true;
	}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	@Override
	public boolean allowMultithreading()
	{
		return false;
	}
	
	// ----------------------------------------------------
	// --------- INPUT OUTPUT DEFINITIONS -----------------
	// ----------------------------------------------------
	
	/**
	 * Return the array of input names
	 * 
	 * @return array of input names
	 */
	@Override
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[3];
		inputNames[0] = new TypeName(VALUE, "Horizontal Image Alignment");
		inputNames[1] = new TypeName(VALUE, "Vertical Image Alignment");
		inputNames[2] = new TypeName(IMAGE, "Image Stack");
		return inputNames;
	}
	
	/**
	 * Return the array of output names defined for this function
	 * 
	 * @return
	 */
	@Override
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(IMAGE, "Stitched Image");
		
		if(outputNames == null)
			return defaultOutputNames;
		return outputNames;
	}
	
	/**
	 * Returns a list of parameters necessary for this function to run... Every parameter is defined as a line in a form that provides the ability to set how it will be displayed to the user and what options are available to choose from The simplest
	 * FormLine can be written as: FormLine p = new FormLine(parameterName); This will provide a text field for the user to input the value of the parameter named parameterName More complex displaying options can be set by consulting the FormLine API
	 * 
	 * @return list of FormLine to create a parameter panel
	 */
	@Override
	public ParameterSet requiredParameters()
	{
		Parameter p0 = new Parameter("Scale", "The stitched image size will be scaled by this factor.", "1.0");
		Parameter p1 = new Parameter("Output Bit Depth", "Bit depth to save the image as.", Parameter.DROPDOWN, new String[] { "8", "16" }, 1);
		Parameter p2 = new Parameter("Normalize Intensities Fit Bit Depth", "Scale intensities to go from 0 to max value determined by new bit depth (\'true\' overrides intensity multiplier).", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p3 = new Parameter("Intensity Multiplier", "Number to multiply all intensities by before converting to new bitDepth.", "1");
		Parameter p4 = new Parameter("Rows", "Number of rows in stitched image", "1");
		Parameter p5 = new Parameter("Columns", "Number of columns in stitched image", "1");
		Parameter p6 = new Parameter("Image Order", "Order in which the images were taken", Parameter.DROPDOWN, new String[] { "Row First LtoR", "Column First TtoB" }, 0);
		Parameter p7 = new Parameter("Stack Dim Name", "Name of the dimension within the image set that contains the list of images to stitch.", "Z");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		
		return parameterArray;
	}
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-ridden by returning false If over-ridden ANY batch function using this function will not be able perform error
	 * checking...
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return true;
	}
	
	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData valueData = inputs.get("Horizontal Image Alignment");
		if(valueData == null || !valueData.getTypeName().getType().matches(JEXData.VALUE))
			return false;
		CSVList alignmentInfoHor = new CSVList(ValueReader.readValueObject(valueData));
		int horDxImage = Integer.parseInt(alignmentInfoHor.get(0));
		int horDyImage = Integer.parseInt(alignmentInfoHor.get(1));
		
		// Collect the inputs
		valueData = inputs.get("Vertical Image Alignment");
		if(valueData == null || !valueData.getTypeName().getType().matches(JEXData.VALUE))
			return false;
		CSVList alignmentInfoVer = new CSVList(ValueReader.readValueObject(valueData));
		int verDxImage = Integer.parseInt(alignmentInfoVer.get(0));
		int verDyImage = Integer.parseInt(alignmentInfoVer.get(1));
		
		// Collect saving parameters
		int bitDepth = Integer.parseInt(parameters.getValueOfParameter("Output Bit Depth"));
		boolean normalize = Boolean.parseBoolean(parameters.getValueOfParameter("Normalize Intensities Fit Bit Depth"));
		double multiplier = Double.parseDouble(parameters.getValueOfParameter("Intensity Multiplier"));
		double scale = Double.parseDouble(parameters.getValueOfParameter("Scale"));
		String stackDimName = parameters.getValueOfParameter("Stack Dim Name");
		int rows = (int) Double.parseDouble(parameters.getValueOfParameter("Rows"));
		int cols = (int) Double.parseDouble(parameters.getValueOfParameter("Columns"));
		int order = (parameters.getValueOfParameter("Image Order").equals("Row First LtoR")) ? LtoR : TtoB;
		
		// Collect the inputs
		JEXData imageData = inputs.get("Image Stack");
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
			return false;
		DimTable table = imageData.getDimTable();
		Dim stackDim = table.getDimWithName(stackDimName);
		
		// Run the function
		// Get the Partial DimTable and iterate through it and stitch.
		DimTable partialTable = table;
		partialTable.remove(stackDim);
		Map<DimensionMap,String> stitchedImageFilePaths = new HashMap<DimensionMap,String>();
		for (DimensionMap partialMap : partialTable.getDimensionMaps())
		{
			List<DimensionMap> mapsToGet = getMapsForStitching(stackDim, partialMap);
			PointList imageCoords = this.getMovements(horDxImage, horDyImage, verDxImage, verDyImage, rows, cols, order, scale);
			File stitchedFile = JEX_ImageTools_Stitch_Coord.stitch(entry, imageData, mapsToGet, imageCoords, scale, normalize, multiplier, bitDepth);
			stitchedImageFilePaths.put(partialMap, stitchedFile.getAbsolutePath());
		}
		
		// Set the outputs
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), stitchedImageFilePaths);
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	private List<DimensionMap> getMapsForStitching(Dim stackDim, DimensionMap partialMap)
	{
		List<DimensionMap> ret = new Vector<DimensionMap>();
		for (int z = 0; z < stackDim.size(); z++)
		{
			DimensionMap imageMap = partialMap.copy();
			imageMap.put(stackDim.name(), stackDim.valueAt(z));
			ret.add(imageMap);
		}
		return ret;
	}
	
	private PointList getMovements(int horDxImage, int horDyImage, int verDxImage, int verDyImage, int rows, int cols, int order, double scale)
	{
		PointList ret = new PointList();
		if(order == TtoB) // Go through col first (i.e. index row first)
		{
			for (int c = 0; c < cols; c++)
			{
				for (int r = 0; r < rows; r++)
				{
					ret.add(r * verDxImage + c * horDxImage, r * verDyImage + c * horDyImage);
				}
			}
		}
		else
		// Go through row first (i.e. index col first)
		{
			for (int r = 0; r < rows; r++)
			{
				for (int c = 0; c < cols; c++)
				{
					ret.add(r * verDxImage + c * horDxImage, r * verDyImage + c * horDyImage);
				}
			}
		}
		
		// Scale and put bounding rectangle at 0,0
		ret.scale(scale);
		Rectangle rect = ret.getBounds();
		ret.translate(-1 * rect.x, -1 * rect.y);
		System.out.println(ret.getBounds());
		return ret;
	}
}
