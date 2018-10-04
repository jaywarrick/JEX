package function.plugin.old;

import java.util.HashMap;
import java.util.TreeMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.plugin.plugins.imageProcessing.RankFilters2;
import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.process.Blitter;
import ij.process.ImageProcessor;
import jex.statics.JEXStatics;
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
public class JEX_ImageFilters extends JEXCrunchable {

	public static String MEAN = "mean", MIN = "min", MAX = "max", MEDIAN = "median", VARIANCE = "variance", STDEV = "std. dev.", OUTLIERS="outliers", DESPECKLE="despeckle", REMOVE_NAN="remove NaN", OPEN="open", CLOSE="close", OPEN_TOPHAT="open top-hat", CLOSE_TOPHAT="close top-hat", SUM="sum", SNR="Signal:Noise Ratio", NSR="Noise:Signal Ratio", GAUSSIAN="Gaussian";
	public static String OP_NONE = "None", OP_LOG = "Natural Log", OP_EXP="Exp", OP_SQRT="Square Root", OP_SQR="Square", OP_INVERT="Invert";

	public JEX_ImageFilters()
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
		String result = "Image Filters";
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
		String result = "Use a predefined image filter and specify the filter radius.";
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
		String toolbox = "Image processing";
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
		return true;
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
		inputNames[0] = new TypeName(IMAGE, "Image");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Filtered Image");

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
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		Parameter p1 = new Parameter("Filter Type", "Type of filter to apply.", Parameter.DROPDOWN, new String[] { MEAN, MIN, MAX, MEDIAN, VARIANCE, STDEV, OUTLIERS, DESPECKLE, REMOVE_NAN, OPEN, CLOSE, OPEN_TOPHAT, CLOSE_TOPHAT, SUM, SNR, NSR, GAUSSIAN}, 0);
		Parameter p7 = new Parameter("Exclusion Filter Table", "<DimName>=<Val1>,<Val2>,...<Valn>;<DimName2>=<Val1>,<Val2>,...<Valn>, Specify the dimension and dimension values to exclude. Leave blank to process all.", "");
		Parameter p8 = new Parameter("Keep Unprocessed Images?", "Should the images within the object that are exlcluded from analysis by the Dimension Filter be kept in the result?", Parameter.CHECKBOX, true);
		Parameter p2 = new Parameter("Radius", "Radius of filter in pixels.", "2.0");
		Parameter p3 = new Parameter("Output Bit-Depth", "Bit-Depth of the output image", Parameter.DROPDOWN, new String[] { "8", "16", "32" }, 2);
		Parameter p4 = getNumThreadsParameter(10, 6);
		Parameter p5 = new Parameter("Post-math Operation", "Choose a post math operation to perform if desired. Otherwise, leave as 'None'.", Parameter.DROPDOWN, new String[] { OP_NONE, OP_LOG, OP_EXP, OP_SQRT, OP_SQR, OP_INVERT}, 0);
		Parameter p6 = new Parameter("Post-multiplier", "Value to multiply by after processing and any math operation and before saving.", "1.0");
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);

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
		return false;
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
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
			return false;

		// Gather parameters
		double radius = Double.parseDouble(parameters.getValueOfParameter("Radius"));
		String method = parameters.getValueOfParameter("Filter Type");
		String filterString = parameters.getValueOfParameter("Exclusion Filter Table");
		DimTable filterTable = new DimTable(filterString);
		
		boolean keepUnprocessed = Boolean.parseBoolean(parameters.getValueOfParameter("Keep Unprocessed Images?"));
		int bitDepth = Integer.parseInt(parameters.getValueOfParameter("Output Bit-Depth"));
		String mathOp = parameters.getValueOfParameter("Post-math Operation");
		double mult = Double.parseDouble(parameters.getValueOfParameter("Post-multiplier"));

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			
			if(filterTable.testMapAsExclusionFilter(map))
			{
				// Then skip, but save unprocessed image if necessary
				if(keepUnprocessed)
				{
					String path = JEXWriter.saveImage(new ImagePlus(imageMap.get(map)));
					outputImageMap.put(map, path);
				}
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				continue;
			}
			
			ImagePlus im = new ImagePlus(imageMap.get(map));
			ImageProcessor ip = im.getProcessor().convertToFloat();
			ImageProcessor orig = null;
			
			if(method.equals(OPEN_TOPHAT) || method.equals(CLOSE_TOPHAT) || method.equals(SNR) || method.equals(NSR))
			{
				orig = ip.duplicate();
			}

			// //// Begin Actual Function
			RankFilters2 rF = new RankFilters2();
			rF.rank(ip, radius, getMethodInt(method));
			GaussianBlur gb = new GaussianBlur();
			
			if(method.equals(OPEN_TOPHAT) || method.equals(CLOSE_TOPHAT))
			{
				orig.copyBits(ip, 0, 0, Blitter.SUBTRACT);
				ip = orig;
				orig = null;
			}
			else if(method.equals(GAUSSIAN))
			{
				gb.blurGaussian(ip, radius, radius, 0.0002); // Default accuracy = 0.0002
			}
			else if(method.equals(NSR))
			{
				rF.rank(orig, radius, RankFilters2.STDEV);
				orig.copyBits(ip, 0, 0, Blitter.DIVIDE);
				ip = orig;
				orig = null;
			}
			else if(method.equals(SNR))
			{
				rF.rank(orig, radius, RankFilters2.STDEV);
				ip.copyBits(orig, 0, 0, Blitter.DIVIDE);
				orig = null;
			}
			
			if(!mathOp.equals(OP_NONE))
			{
				if(mathOp.equals(OP_EXP))
				{
					ip.exp();
				}
				else if(mathOp.equals(OP_LOG))
				{
					ip.ln();
				}
				else if(mathOp.equals(OP_SQR))
				{
					ip.sqr();
				}
				else if(mathOp.equals(OP_SQRT))
				{
					ip.sqrt();
				}
				else if(mathOp.equals(OP_INVERT))
				{
					ip.resetMinAndMax();
					ip.invert();
				}
			}
			if(mult != 1.0)
			{
				ip.multiply(mult);
			}
			
			ip = JEXWriter.convertToBitDepthIfNecessary(ip, bitDepth);
			String path = JEXWriter.saveImage(ip);

			if(path != null)
			{
				outputImageMap.put(map, path);
			}

			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(outputImageMap.size() == 0)
		{
			return false;
		}

		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), outputImageMap);

		// Set the outputs
		realOutputs.add(output1);

		// Return status
		return true;
	}
	
	public static int getMethodInt(String method)
	{
		int methodInt = RankFilters2.MEAN;
		if(method.equals(MIN))
		{
			methodInt = RankFilters2.MIN;
		}
		else if(method.equals(MAX))
		{
			methodInt = RankFilters2.MAX;
		}
		else if(method.equals(MEDIAN))
		{
			methodInt = RankFilters2.MEDIAN;
		}
		else if(method.equals(VARIANCE))
		{
			methodInt = RankFilters2.VARIANCE;
		}
		else if(method.equals(STDEV))
		{
			methodInt = RankFilters2.STDEV;
		}
		else if(method.equals(OUTLIERS))
		{
			methodInt = RankFilters2.OUTLIERS;
		}
		else if(method.equals(DESPECKLE))
		{
			methodInt = RankFilters2.DESPECKLE;
		}
		else if(method.equals(REMOVE_NAN))
		{
			methodInt = RankFilters2.REMOVE_NAN;
		}
		else if(method.equals(OPEN) || method.equals(OPEN_TOPHAT))
		{
			methodInt = RankFilters2.OPEN;
		}
		else if(method.equals(CLOSE) || method.equals(CLOSE_TOPHAT))
		{
			methodInt = RankFilters2.CLOSE;
		}
		else if(method.equals(SUM))
		{
			methodInt = RankFilters2.SUM;
		}
		else if(method.equals(SNR) || method.equals(NSR))
		{
			methodInt = RankFilters2.MEAN;
		}
		return methodInt;
	}

}
