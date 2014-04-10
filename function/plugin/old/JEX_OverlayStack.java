package function.plugin.old;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
import miscellaneous.Canceler;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;

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
public class JEX_OverlayStack extends JEXCrunchable {
	
	public static final String LOG = "Log", LINEAR = "Linear";
	
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
		String result = "Image Stack Overlay";
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
		String result = "Overlay images along a single dimension assign them a color channel";
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
		String toolbox = "Stack processing";
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
		inputNames[0] = new TypeName(IMAGE, "Image Set");
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
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Overlay Set");
		
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
		Parameter p1 = new Parameter("Dim Name", "Name of dim that contains the images to overlay.", "Color");
		Parameter p2 = new Parameter("RED Dim Value", "Value of dim containing the RED image.", "");
		Parameter p3 = new Parameter("RED Min", "Value in the RED image to map to 0 intensity.", "0");
		Parameter p4 = new Parameter("RED Max", "Value in the RED image to map to 255 intensity.", "65535");
		Parameter p5 = new Parameter("GREEN Dim Value", "Value of dim containing the RED image.", "");
		Parameter p6 = new Parameter("GREEN Min", "Value in the GREEN image to map to 0 intensity.", "0");
		Parameter p7 = new Parameter("GREEN Max", "Value in the GREEN image to map to 255 intensity.", "65535");
		Parameter p8 = new Parameter("BLUE Dim Value", "Value of dim containing the RED image.", "");
		Parameter p9 = new Parameter("BLUE Min", "Value in the BLUE image to map to 0 intensity.", "0");
		Parameter p10 = new Parameter("BLUE Max", "Value in the BLUE image to map to 255 intensity.", "65535");
		Parameter p11 = new Parameter("RGB Scale", "Linear or log scaling of R, G, and B channels", Parameter.DROPDOWN, new String[] { "Linear", "Log" }, 1);
		Parameter p12 = new Parameter("BF Dim Value", "Value of dim containing the RED image.", "");
		Parameter p13 = new Parameter("BF Min", "Value in the BF image to map to 0 intensity.", "0");
		Parameter p14 = new Parameter("BF Max", "Value in the BF image to map to 255 intensity.", "65535");
		Parameter p15 = new Parameter("BF Scale", "Linear or log scaling of bf channel", Parameter.DROPDOWN, new String[] { "Linear", "Log" }, 0);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
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
		parameterArray.addParameter(p12);
		parameterArray.addParameter(p13);
		parameterArray.addParameter(p14);
		parameterArray.addParameter(p15);
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
		JEXData data1 = inputs.get("Image Set");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// //// Get params
		String dimName = this.parameters.getValueOfParameter("Dim Name");
		String bfDim = this.parameters.getValueOfParameter("BF Dim Value");
		double bfMin = Double.parseDouble(this.parameters.getValueOfParameter("BF Min"));
		double bfMax = Double.parseDouble(this.parameters.getValueOfParameter("BF Max"));
		String rDim = this.parameters.getValueOfParameter("RED Dim Value");
		double rMin = Double.parseDouble(this.parameters.getValueOfParameter("RED Min"));
		double rMax = Double.parseDouble(this.parameters.getValueOfParameter("RED Max"));
		String gDim = this.parameters.getValueOfParameter("GREEN Dim Value");
		double gMin = Double.parseDouble(this.parameters.getValueOfParameter("GREEN Min"));
		double gMax = Double.parseDouble(this.parameters.getValueOfParameter("GREEN Max"));
		String bDim = this.parameters.getValueOfParameter("BLUE Dim Value");
		double bMin = Double.parseDouble(this.parameters.getValueOfParameter("BLUE Min"));
		double bMax = Double.parseDouble(this.parameters.getValueOfParameter("BLUE Max"));
		String bfScale = this.parameters.getValueOfParameter("BF Scale");
		String rgbScale = this.parameters.getValueOfParameter("RGB Scale");
		
		// Run the function
		TreeMap<DimensionMap,String> images1 = ImageReader.readObjectToImagePathTable(data1);
		TreeMap<DimensionMap,String> outputMap = overlayStack(images1, dimName, rDim, gDim, bDim, bfDim, rMin, rMax, gMin, gMax, bMin, bMax, bfMin, bfMax, rgbScale, bfScale, this);
		
		// Set the outputs
		JEXData output = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), outputMap);
		output.setDataObjectInfo("Overaly performed using Image Overlay Along a Dimension Function");
		this.realOutputs.add(output);
		
		// Return status
		return true;
	}
	
	private static void resetIm(ImagePlus im)
	{
		if(im != null)
		{
			im.flush();
			im = null;
		}
	}
	
	public static ByteProcessor fixImage(String rgbScale, FloatProcessor imp, FloatProcessor bfImp, FloatBlitter blitter)
	{
		if(imp == null)
		{
			return null;
		}
		ByteProcessor ret = null;
		ImagePlus im = null;
		if(rgbScale.equals("Log"))
		{
			imp.log();
			imp.multiply(255 / Math.log(255));
		}
		if(bfImp != null)
		{
			blitter = new FloatBlitter(imp);
			blitter.copyBits(bfImp, 0, 0, FloatBlitter.ADD);
		}
		im = FunctionUtility.makeImageToSave(imp, "false", 8);
		ret = (ByteProcessor) im.getProcessor();
		
		im.flush();
		im = null;
		
		return ret;
	}
	
	public static TreeMap<DimensionMap,String> overlayStack(TreeMap<DimensionMap,String> images1, String dimName, String rDim, String gDim, String bDim, String bfDim, Double rMin, Double rMax, Double gMin, Double gMax, Double bMin, Double bMax, Double bfMin, Double bfMax, String rgbScale, String bfScale, Canceler canceler)
	{
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		
		DimTable reducedTable = new DimTable(images1);
		reducedTable.removeDimWithName(dimName);
		List<DimensionMap> maps = reducedTable.getDimensionMaps();
		
		int count = 0;
		int total = maps.size();
		FloatBlitter blitter = null;
		for (DimensionMap map : maps)
		{
			if(canceler.isCanceled())
			{
				return null;
			}
			// Make dims to get
			DimensionMap bfMap = map.copy();
			bfMap.put(dimName, bfDim);
			DimensionMap rMap = map.copy();
			rMap.put(dimName, rDim);
			DimensionMap gMap = map.copy();
			gMap.put(dimName, gDim);
			DimensionMap bMap = map.copy();
			bMap.put(dimName, bDim);
			
			// get the paths
			String bfPath = null;
			if(!bfDim.equals(""))
			{
				bfPath = images1.get(bfMap);
			}
			String rPath = null;
			if(!rDim.equals(""))
			{
				rPath = images1.get(rMap);
			}
			String gPath = null;
			if(!gDim.equals(""))
			{
				gPath = images1.get(gMap);
			}
			String bPath = null;
			if(!bDim.equals(""))
			{
				bPath = images1.get(bMap);
			}
			
			// get the images and image processors
			ImagePlus im = null, bfIm = null;
			ByteProcessor rImp = null, gImp = null, bImp = null;
			FloatProcessor imp = null, bfImp = null;
			Integer w = null, h = null;
			
			// Fix Brightfield
			if(bfPath != null)
			{
				bfIm = new ImagePlus(bfPath);
				bfImp = (FloatProcessor) bfIm.getProcessor().convertToFloat(); // should be a float processor
				FunctionUtility.imAdjust(bfImp, bfMin, bfMax, 0d, 255d, 1d);
				if(bfScale.equals("Log"))
				{
					bfImp.log();
					bfImp.multiply(255 / Math.log(255));
				}
				if(w == null)
				{
					w = bfImp.getWidth();
					h = bfImp.getHeight();
				}
			}
			
			// Fix Red
			imp = null;
			if(rPath != null)
			{
				im = new ImagePlus(rPath);
				imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
				FunctionUtility.imAdjust(imp, rMin, rMax, 0d, 255d, 1d);
				if(w == null)
				{
					w = imp.getWidth();
					h = imp.getHeight();
				}
				im.flush();
				im = null;
			}
			rImp = fixImage(rgbScale, imp, bfImp, blitter);
			imp = null;
			blitter = null;
			
			// Fix Green
			if(gPath != null)
			{
				im = new ImagePlus(gPath);
				imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
				FunctionUtility.imAdjust(imp, gMin, gMax, 0d, 255d, 1d);
				if(w == null)
				{
					w = imp.getWidth();
					h = imp.getHeight();
				}
				im.flush();
				im = null;
			}
			gImp = fixImage(rgbScale, imp, bfImp, blitter);
			imp = null;
			blitter = null;
			
			// Fix Blue
			if(bPath != null)
			{
				im = new ImagePlus(bPath);
				imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
				FunctionUtility.imAdjust(imp, bMin, bMax, 0d, 255d, 1d);
				if(w == null)
				{
					w = imp.getWidth();
					h = imp.getHeight();
				}
				im.flush();
				im = null;
			}
			bImp = fixImage(rgbScale, imp, bfImp, blitter);
			imp = null;
			blitter = null;
			
			// Clear memory associated with BF
			resetIm(bfIm);
			bfImp = null;
			
			// //// Begin Actual Function
			byte[] r = null, g = null, b = null;
			ColorProcessor cp = new ColorProcessor(w, h);
			if(rImp != null)
			{
				r = (byte[]) rImp.getPixels();
			}
			if(gImp != null)
			{
				g = (byte[]) gImp.getPixels();
			}
			if(bImp != null)
			{
				b = (byte[]) bImp.getPixels();
			}
			if(rImp == null)
			{
				r = new byte[w * h];
			}
			if(gImp == null)
			{
				g = new byte[w * h];
			}
			if(bImp == null)
			{
				b = new byte[w * h];
			}
			cp.setRGB(r, g, b);
			// //// End Actual Function
			
			// //// Save the results
			String finalPath = JEXWriter.saveImage(cp);
			outputMap.put(map.copy(), finalPath);
			Logs.log("Finished processing " + (count + 1) + " of " + total + ".", 1, canceler);
			count++;
			
			// Status bar
			int percentage = (int) (100 * ((double) count / (double) maps.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		return outputMap;
		
	}
}
