package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.SimpleFileFilter;
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
public class JEX_ConvertNISElementsTiffs extends JEXCrunchable {
	
	public JEX_ConvertNISElementsTiffs()
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
		String result = "Convert NIS Elements Tiffs";
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
		String result = "Conver images from a NIS Elements ND Acquisition (multiple, colors, times, locations, large image arrays... no Z stacks yet) from stacks to individual images";
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
		String toolbox = "Data Importing";
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
		TypeName[] inputNames = new TypeName[0];
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
		this.defaultOutputNames = new TypeName[1];
		
		this.defaultOutputNames[0] = new TypeName(VALUE, "Conversion Success");
		
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
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		Parameter p1a = new Parameter("Input Directory", "Location of the multicolor Tiff images", Parameter.FILECHOOSER, "");
		Parameter p1b = new Parameter("Output Directory", "Location of the multicolor Tiff images", Parameter.FILECHOOSER, "");
		
		Parameter p2 = new Parameter("ImRows", "Number of rows in 'Large Image Array'", "1");
		Parameter p3 = new Parameter("ImCols", "Number of columns in 'Large Image Array'", "1");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		parameterArray.addParameter(p1a);
		parameterArray.addParameter(p1b);
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
		// Gather parameters
		String inDir = this.parameters.getValueOfParameter("Input Directory");
		String outDir = this.parameters.getValueOfParameter("Output Directory");
		
		int imRows = Integer.parseInt(this.parameters.getValueOfParameter("ImRows"));
		int imCols = Integer.parseInt(this.parameters.getValueOfParameter("ImCols"));
		
		// Get the images
		File inFolder = new File(inDir);
		File outFolder = new File(outDir);
		if(!outFolder.exists())
		{
			outFolder.mkdirs();
		}
		
		JEXStatics.statusBar.setStatusText("Sorting files to convert. May take minutes...");
		//File[] images = inFolder.listFiles(new SimpleFileFilter(new String[]{"tif"}));
		List<File> images = FileUtility.getSortedFileList(inFolder.listFiles(new SimpleFileFilter(new String[] { "tif" })));
		
		JEXStatics.statusBar.setStatusText("Converting files.");
		// Run the function
		DecimalFormat format = new DecimalFormat("0000000000");
		int counter = 0;
		double count = 0;
		double total = images.size();
		int percentage = 0;
		JEXStatics.statusBar.setProgressPercentage(percentage);
		long fileSize = 0;
		for (File f : images)
		{
			if(this.isCanceled())
			{
				return false;
			}
			ImagePlus im = new ImagePlus(f.getAbsolutePath());
			String baseName = FileUtility.getFileNameWithoutExtension(f.getAbsolutePath());
			for (int s = 1; s < im.getStackSize() + 1; s++)
			{
				if(this.isCanceled())
				{
					return false;
				}
				im.setSlice(s);
				TreeSet<DimensionMap> splitDims = getSplitDims(imRows, imCols);
				TreeMap<DimensionMap,ImageProcessor> splitImages = null;
				for (DimensionMap map : splitDims)
				{
					if(this.isCanceled())
					{
						return false;
					}
					String pathToSaveTo = outDir + File.separator + baseName + "_" + format.format(counter) + "_Color" + (s - 1) + "_ImRow" + map.get("ImRow") + "_ImCol" + map.get("ImCol") + ".tif";
					File toSave = new File(pathToSaveTo);
					if(counter == 0)
					{
						if(splitImages == null)
						{
							splitImages = splitRowsAndCols(im.getProcessor(), imRows, imCols);
						}
						FileSaver fs = new FileSaver(new ImagePlus("temp", splitImages.get(map)));
						Logs.log("Writing " + pathToSaveTo, this);
						fs.saveAsTiff(pathToSaveTo);
						fileSize = toSave.length();
					}
					else if(!toSave.exists() || toSave.length() != fileSize)
					{
						if(splitImages == null)
						{
							splitImages = splitRowsAndCols(im.getProcessor(), imRows, imCols);
						}
						FileSaver fs = new FileSaver(new ImagePlus("temp", splitImages.get(map)));
						Logs.log("Writing " + pathToSaveTo, this);
						fs.saveAsTiff(pathToSaveTo);
					}
					else
					{
						// Skip because the full file is already there.
						Logs.log("Skipping " + pathToSaveTo, this);
					}
					
					counter = counter + 1;
				}
			}
			count = count + 1;
			percentage = (int) (100 * ((count) / (total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		
		
		JEXData output0 = ValueWriter.makeValueObject(this.outputNames[0].getName(), "TRUE");
		
		// Set the outputs
		this.realOutputs.add(output0);
		
		// Return status
		return true;
	}
	
	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	{
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
		{
			return null;
		}
		ImagePlus im = new ImagePlus(imagePath);
		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should
		// be
		// a
		// float
		// processor
		
		// Adjust the image
		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
		
		// Save the results
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String imPath = JEXWriter.saveImage(toSave);
		im.flush();
		
		// return temp filePath
		return imPath;
	}
	
	public static TreeSet<DimensionMap> getSplitDims(int rows, int cols)
	{
		TreeSet<DimensionMap> ret = new TreeSet<DimensionMap>();
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				
				ret.add(new DimensionMap("ImRow=" + r + ",ImCol=" + c));
			}
		}
		return ret;
	}
	
	public static TreeMap<DimensionMap,ImageProcessor> splitRowsAndCols(ImageProcessor imp, int rows, int cols)
	{
		TreeMap<DimensionMap,ImageProcessor> ret = new TreeMap<DimensionMap,ImageProcessor>();
		
		int wAll = imp.getWidth();
		int hAll = imp.getHeight();
		int w = wAll / cols;
		int h = hAll / rows;
		
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				int x = c * w;
				int y = r * h;
				Rectangle rect = new Rectangle(x, y, w, h);
				imp.setRoi(rect);
				ImageProcessor toCopy = imp.crop();
				ImageProcessor toSave = imp.createProcessor(w, h);
				toSave.copyBits(toCopy, 0, 0, Blitter.COPY);
				ret.put(new DimensionMap("ImRow=" + r + ",ImCol=" + c), toSave);
			}
		}
		
		return ret;
	}
}
