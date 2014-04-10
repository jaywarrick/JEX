package function.plugin.old;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import image.roi.IdPoint;
import image.roi.PointList;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.StatisticsUtility;
import tables.Dim;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.imageUtility.TurboReg_;

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
public class JEX_ImageTools_FindImageAlignments extends JEXCrunchable {
	
	public static final int LtoR = 0, TtoB = 1;
	
	public JEX_ImageTools_FindImageAlignments()
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
		String result = "Find Image Alignments";
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
		String result = "Function that allows you to stitch an image ARRAY into a single image using two GUESSES for image alignment objects. Turbo Reg is used to refine the guess.";
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
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(IMAGE, "Images");
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
		this.defaultOutputNames = new TypeName[2];
		this.defaultOutputNames[0] = new TypeName(VALUE, "Hor. Alignment");
		this.defaultOutputNames[1] = new TypeName(VALUE, "Ver. Alignment");
		
		if(this.outputNames == null)
		{
			return this.defaultOutputNames;
		}
		return this.outputNames;
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
		Parameter p7 = new Parameter("ImRow Dim Name", "Name of the dimension that indicates the rows", "ImRow");
		Parameter p8 = new Parameter("ImCol Dim Name", "Name of the dimension that indicates the cols", "ImCol");
		Parameter p9 = new Parameter("Color Dim Name", "Name of the dimension that indicates the color (leave black if not applicable)", "Color");
		Parameter p10 = new Parameter("Color Dim Value", "Value of the color dimension that should be used to guide stitching", "0");
		Parameter p11 = new Parameter("Horizontal Overlap", "Approximate number of pixels overlapping between columns.", "100");
		Parameter p12 = new Parameter("Vertical Overlap", "Approximate number of pixels overlapping between row.", "100");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p10);
		parameterArray.addParameter(p11);
		parameterArray.addParameter(p12);
		
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
		JEXData imageData = inputs.get("Images");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Collect saving parameters
		String imRow = this.parameters.getValueOfParameter("ImRow Dim Name");
		String imCol = this.parameters.getValueOfParameter("ImCol Dim Name");
		String colorDim = this.parameters.getValueOfParameter("Color Dim Name");
		String colorVal = this.parameters.getValueOfParameter("Color Dim Value");
		Integer hOver = Integer.parseInt(this.parameters.getValueOfParameter("Horizontal Overlap"));
		Integer vOver = Integer.parseInt(this.parameters.getValueOfParameter("Vertical Overlap"));
		
		// Run the function
		// Get the Partial DimTable and iterate through it and stitch.
		DimensionMap firstMap = imageData.getDimTable().getDimensionMaps().get(0);
		firstMap.remove(imRow);
		firstMap.remove(imCol);
		if(!colorDim.equals(""))
		{
			firstMap.put(colorDim, colorVal);
		}
		Dim rows = imageData.getDimTable().getDimWithName(imRow);
		Dim cols = imageData.getDimTable().getDimWithName(imCol);
		PointList horMoves = new PointList();
		PointList verMoves = new PointList();
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(imageData);
		TurboReg_ reg = new TurboReg_();
		int width = 0;
		int height = 0;
		int count = 0, total = imageData.getDimTable().getSubTable(firstMap.copy()).mapCount();
		for (DimensionMap targetMap : imageData.getDimTable().getMapIterator(firstMap.copy()))
		{
			if(this.isCanceled())
			{
				return false;
			}
			// Looping through ImRow and ImCol for the color of interest
			// Use TurboReg to assess
			if(!targetMap.get(imRow).equals(rows.max()))
			{
				ImagePlus targetIm = new ImagePlus(images.get(targetMap));
				DimensionMap sourceMap = targetMap.copy();
				int nextRow = Integer.parseInt(targetMap.get(imRow)) + 1;
				sourceMap.put(imRow, "" + nextRow);
				ImagePlus sourceIm = new ImagePlus(images.get(sourceMap));
				width = targetIm.getWidth();
				height = targetIm.getHeight();
				Rectangle targetRect = new Rectangle(0, height - vOver, width - 1, vOver - 1); // Bottom of target image
				Rectangle sourceRect = new Rectangle(0, 0, width - 1, vOver - 1); // Top of source image
				ImageProcessor targetImp = targetIm.getProcessor();
				targetImp.setRoi(targetRect);
				targetIm.setProcessor(targetImp.crop());
				ImageProcessor sourceImp = sourceIm.getProcessor();
				sourceImp.setRoi(sourceRect);
				sourceIm.setProcessor(sourceImp.crop());
				// int[] targetCrop = new int[] { 0, 0, width - 1, height - 1 }; // Bottom of target image
				// int[] sourceCrop = new int[] { 0, 0, width - 1, height - 1 }; // Top of source image
				int[] targetCrop = new int[] { 0, 0, targetRect.width, targetRect.height };
				int[] sourceCrop = new int[] { 0, 0, sourceRect.width, sourceRect.height };
				reg.alignImages(sourceIm, sourceCrop, targetIm, targetCrop, TurboReg_.TRANSLATION, false);
				double dx = reg.getSourcePoints()[0][0];
				double dy = reg.getSourcePoints()[0][1];
				verMoves.add((int) dx, (int) dy);
				Logs.log(targetMap.toString() + " Vertical Align Result: " + (int) dx + "," + (int) dy, 0, this);
			}
			
			if(this.isCanceled())
			{
				return false;
			}
			if(!targetMap.get(imCol).equals(cols.max()))
			{
				ImagePlus targetIm = new ImagePlus(images.get(targetMap));
				DimensionMap sourceMap = targetMap.copy();
				int nextCol = Integer.parseInt(targetMap.get(imCol)) + 1;
				sourceMap.put(imCol, "" + nextCol);
				ImagePlus sourceIm = new ImagePlus(images.get(sourceMap));
				width = targetIm.getWidth();
				height = targetIm.getHeight();
				Rectangle targetRect = new Rectangle(width - hOver, 0, hOver - 1, height - 1); // RHS of target image
				Rectangle sourceRect = new Rectangle(0, 0, hOver - 1, height - 1); // LHS of source image
				ImageProcessor targetImp = targetIm.getProcessor();
				targetImp.setRoi(targetRect);
				targetIm.setProcessor(targetImp.crop());
				ImageProcessor sourceImp = sourceIm.getProcessor();
				sourceImp.setRoi(sourceRect);
				sourceIm.setProcessor(sourceImp.crop());
				// int[] targetCrop = new int[] { 0, 0, width - 1, height - 1 }; // Bottom of target image
				// int[] sourceCrop = new int[] { 0, 0, width - 1, height - 1 }; // Top of source image
				int[] targetCrop = new int[] { 0, 0, targetRect.width, targetRect.height };
				int[] sourceCrop = new int[] { 0, 0, sourceRect.width, sourceRect.height };
				
				reg.alignImages(sourceIm, sourceCrop, targetIm, targetCrop, TurboReg_.TRANSLATION, false);
				double dx = reg.getSourcePoints()[0][0];
				double dy = reg.getSourcePoints()[0][1];
				horMoves.add((int) dx, (int) dy);
				Logs.log(targetMap.toString() + " Horizontal Align Result: " + (int) dx + "," + (int) dy, 0, this);
			}
			// // Update the display
			count = count + 1;
			int percentage = (int) (100 * ((double) (count) / ((double) total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Find the median moves
		Vector<Double> dxs = new Vector<Double>();
		Vector<Double> dys = new Vector<Double>();
		for (IdPoint p : horMoves)
		{
			dxs.add((double) p.x);
			dys.add((double) p.y);
		}
		int horDx = width - hOver - StatisticsUtility.median(dxs).intValue();
		int horDy = StatisticsUtility.median(dys).intValue();
		
		dxs = new Vector<Double>();
		dys = new Vector<Double>();
		for (IdPoint p : verMoves)
		{
			dxs.add((double) p.x);
			dys.add((double) p.y);
		}
		int verDx = StatisticsUtility.median(dxs).intValue();
		int verDy = height - vOver - StatisticsUtility.median(dys).intValue();
		
		String horMove = horDx + "," + horDy;
		String verMove = verDx + "," + verDy;
		
		JEXData horData = ValueWriter.makeValueObject(this.outputNames[0].getName(), horMove);
		JEXData verData = ValueWriter.makeValueObject(this.outputNames[1].getName(), verMove);
		
		// Set the outputs
		this.realOutputs.add(horData);
		this.realOutputs.add(verData);
		
		// Return status
		return true;
	}
}
