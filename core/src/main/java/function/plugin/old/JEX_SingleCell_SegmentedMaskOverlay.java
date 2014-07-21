package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.ByteProcessor;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

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
public class JEX_SingleCell_SegmentedMaskOverlay extends JEXCrunchable {
	
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
		String result = "Segmented Mask Overlay";
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
		String result = "Overlay color images with the segmentation and boundary masks";
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
		String toolbox = "Single Cell Analysis";
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
		inputNames[0] = new TypeName(IMAGE, "Image");
		inputNames[1] = new TypeName(IMAGE, "Segmented Image");
		inputNames[2] = new TypeName(IMAGE, "Mask Image");
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		this.defaultOutputNames = new TypeName[1];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Overlay");
		
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
		Parameter p0a = new Parameter("DEFAULT Color", "What should the color be for images without a color dimension", Parameter.DROPDOWN, new String[] { "R", "G", "B" }, 0);
		Parameter p0b = new Parameter("DEFAULT Min", "Value in the DEFAULT image to map to 0 intensity. (blank assumes image min)", "0");
		Parameter p0c = new Parameter("DEFAULT Max", "Value in the DEFAULT image to map to 0 intensity. (blank assumes image max)", "65535");
		Parameter p1 = new Parameter("Color Dim Name", "Name of the color dim for multicolor image sets", "Color");
		Parameter p2 = new Parameter("RED Dim Value", "Value of dim containing the RED image.", "");
		Parameter p3 = new Parameter("RED Min", "Value in the RED image to map to 0 intensity. (blank assumes image min)", "0");
		Parameter p4 = new Parameter("RED Max", "Value in the RED image to map to 255 intensity. (blank assumes image max)", "65535");
		Parameter p5 = new Parameter("GREEN Dim Value", "Value of dim containing the RED image.", "");
		Parameter p6 = new Parameter("GREEN Min", "Value in the GREEN image to map to 0 intensity. (blank assumes image min)", "0");
		Parameter p7 = new Parameter("GREEN Max", "Value in the GREEN image to map to 255 intensity. (blank assumes image max)", "65535");
		Parameter p8 = new Parameter("BLUE Dim Value", "Value of dim containing the RED image.", "");
		Parameter p9 = new Parameter("BLUE Min", "Value in the BLUE image to map to 0 intensity. (blank assumes image min)", "0");
		Parameter p10 = new Parameter("BLUE Max", "Value in the BLUE image to map to 255 intensity. (blank assumes image max)", "65535");
		Parameter p11 = new Parameter("RGB Scale", "Linear or log scaling of R, G, and B channels", Parameter.DROPDOWN, new String[] { "Linear", "Log" }, 1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0a);
		parameterArray.addParameter(p0b);
		parameterArray.addParameter(p0c);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p10);
		parameterArray.addParameter(p11);
		return parameterArray;
	}
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-rided by returning false If over-ridden ANY batch function using this function will not be able perform error
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
		JEXData imageData = inputs.get("Image");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		JEXData segData = inputs.get("Segmented Image");
		if(segData == null || !segData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		JEXData innerData = inputs.get("Mask Image");
		if(innerData == null || !innerData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// //// Get params
		String defaultColor = this.parameters.getValueOfParameter("DEFAULT Color");
		String val = this.parameters.getValueOfParameter("DEFAULT Min");
		Double v = null;
		if(!val.equals(""))
		{
			v = Double.parseDouble(val);
		}
		Double defaultMin = v;
		val = this.parameters.getValueOfParameter("DEFAULT Max");
		v = null;
		if(!val.equals(""))
		{
			v = Double.parseDouble(val);
		}
		Double defaultMax = v;
		String dimName = this.parameters.getValueOfParameter("Color Dim Name");
		String rDim = this.parameters.getValueOfParameter("RED Dim Value");
		val = this.parameters.getValueOfParameter("RED Min");
		v = null;
		if(!val.equals(""))
		{
			v = Double.parseDouble(val);
		}
		Double rMin = v;
		val = this.parameters.getValueOfParameter("RED Max");
		v = null;
		if(!val.equals(""))
		{
			v = Double.parseDouble(val);
		}
		Double rMax = v;
		val = this.parameters.getValueOfParameter("GREEN Min");
		v = null;
		if(!val.equals(""))
		{
			v = Double.parseDouble(val);
		}
		Double gMin = v;
		val = this.parameters.getValueOfParameter("GREEN Max");
		v = null;
		if(!val.equals(""))
		{
			v = Double.parseDouble(val);
		}
		Double gMax = v;
		val = this.parameters.getValueOfParameter("BLUE Min");
		v = null;
		if(!val.equals(""))
		{
			v = Double.parseDouble(val);
		}
		Double bMin = v;
		val = this.parameters.getValueOfParameter("BLUE Max");
		v = null;
		if(!val.equals(""))
		{
			v = Double.parseDouble(val);
		}
		Double bMax = v;
		String gDim = this.parameters.getValueOfParameter("GREEN Dim Value");
		String bDim = this.parameters.getValueOfParameter("BLUE Dim Value");
		String rgbScale = this.parameters.getValueOfParameter("RGB Scale");
		
		// Run the function
		
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> segs = ImageReader.readObjectToImagePathTable(segData);
		TreeMap<DimensionMap,String> masks = ImageReader.readObjectToImagePathTable(innerData);
		TreeMap<DimensionMap,String> overlayers = new TreeMap<DimensionMap,String>();
		
		if(rDim.equals("") && bDim.equals("") && gDim.equals(""))
		{
			dimName = "Color";
			if(defaultColor.equals("R"))
			{
				rDim = defaultColor;
				rMin = defaultMin;
				rMax = defaultMax;
			}
			if(defaultColor.equals("G"))
			{
				gDim = defaultColor;
				gMin = defaultMin;
				gMax = defaultMax;
			}
			if(defaultColor.equals("B"))
			{
				bDim = defaultColor;
				bMin = defaultMin;
				bMax = defaultMax;
			}
			for (Entry<DimensionMap,String> e : images.entrySet())
			{
				DimensionMap newMap = e.getKey().copy();
				newMap.put(dimName, defaultColor);
				overlayers.put(newMap.copy(), e.getValue());
			}
		}
		else
		{
			overlayers.putAll(images);
		}
		
		for (DimensionMap map : segData.getDimTable().getMapIterator())
		{
			DimensionMap newMap = map.copy();
			newMap.put(dimName, "MASK");
			
			ByteProcessor seg = (ByteProcessor) (new ImagePlus(segs.get(map))).getProcessor();
			ByteProcessor mask = (ByteProcessor) (new ImagePlus(masks.get(map))).getProcessor();
			
			seg.invert();
			mask.multiply(0.50);
			seg.copyBits(mask, 0, 0, Blitter.ADD);
			String path = JEXWriter.saveImage(seg);
			
			overlayers.put(newMap, path);
		}
		
		TreeMap<DimensionMap,String> outputMap = JEX_OverlayStack.overlayStack(overlayers, dimName, rDim, gDim, bDim, "MASK", rMin, rMax, gMin, gMax, bMin, bMax, new Double(0), new Double(255), rgbScale, JEX_OverlayStack.LINEAR, this);
		
		// Set the outputs
		JEXData output = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), outputMap);
		this.realOutputs.add(output);
		
		// Return status
		return true;
	}
	
}