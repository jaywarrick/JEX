package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import miscellaneous.FileUtility;
import miscellaneous.SimpleFileFilter;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.ExperimentalDataCrunch;

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
public class JEX_CTC_ImportImagesFromNISFolder extends ExperimentalDataCrunch {
	
	public JEX_CTC_ImportImagesFromNISFolder()
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
		String result = "Import CTC Images From NIS Folder";
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
		String result = "Import images from a NIS Elements ND Acquisition (multiple, colors, times, locations, large image arrays... no Z stacks yet) from tif stacks to individual images";
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
		String toolbox = "CTC Tools";
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
		
		this.defaultOutputNames[0] = new TypeName(IMAGE, "IMAGES");
		
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
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		parameterArray.addParameter(p1a);
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
		File inFolder = new File(inDir);
		
		// Gather and sort the input files
		JEXStatics.statusBar.setStatusText("Sorting files to convert. May take minutes...");
		// File[] images = inFolder.listFiles(new SimpleFileFilter(new String[]{"tif"}));
		List<File> images = FileUtility.getSortedFileList(inFolder.listFiles(new SimpleFileFilter(new String[] { "tif" })));
		
		// Save the data and create the database object
		JEXStatics.statusBar.setStatusText("Converting files.");
		// Run the function
		double count = 0;
		double total = images.size();
		int percentage = 0;
		JEXStatics.statusBar.setProgressPercentage(percentage);
		// TreeMap<Integer,TreeMap<DimensionMap,String>> importedImages = new TreeMap<Integer,TreeMap<DimensionMap,String>>();
		TreeMap<DimensionMap,String> compiledImages = new TreeMap<DimensionMap,String>();
		
		for (File f : images)
		{
			if(this.isCanceled())
			{
				return false;
			}
			ImagePlus im = new ImagePlus(f.getAbsolutePath());
			DimensionMap baseMap = this.getMapFromPath(f.getAbsolutePath());
			for (int s = 1; s < im.getStackSize() + 1; s++)
			{
				if(this.isCanceled())
				{
					return false;
				}
				// TreeMap<DimensionMap,String> colorSet = importedImages.get(s);
				// if(colorSet == null)
				// {
				// colorSet = new TreeMap<DimensionMap,String>();
				// importedImages.put(s, colorSet);
				// }
				im.setSlice(s);
				
				// Save image into map specific to this color
				// String imagePath = JEXWriter.saveImage(im.getProcessor());
				// colorSet.put(baseMap.copy(), imagePath);
				
				// Save image into map for a multicolor image object for easier viewing etc.
				String imagePath2 = JEXWriter.saveImage(im.getProcessor());
				DimensionMap mapWithColor = baseMap.copy();
				mapWithColor.put("Color", "" + s);
				compiledImages.put(mapWithColor, imagePath2);
				
			}
			count = count + 1;
			percentage = (int) (100 * ((count) / (total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		JEXData output0 = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), compiledImages);
		// JEXData output1 = ImageWriter.makeImageStackFromPaths(this.outputNames[1].getName(), importedImages.get(1));
		// JEXData output2 = ImageWriter.makeImageStackFromPaths(this.outputNames[2].getName(), importedImages.get(2));
		// JEXData output3 = ImageWriter.makeImageStackFromPaths(this.outputNames[3].getName(), importedImages.get(3));
		// JEXData output4 = ImageWriter.makeImageStackFromPaths(this.outputNames[4].getName(), importedImages.get(4));
		
		// Set the outputs
		this.realOutputs.add(output0);
		// this.realOutputs.add(output1);
		// this.realOutputs.add(output2);
		// this.realOutputs.add(output3);
		// this.realOutputs.add(output4);
		
		// Return status
		return true;
	}
	
	public DimensionMap getMapFromPath(String filePath)
	{
		String name = FileUtility.getFileNameWithoutExtension(filePath);
		String[] names = name.split("_");
		String x = names[1].substring(1);
		String y = names[2].substring(1);
		x = Integer.valueOf(x).toString();
		y = Integer.valueOf(y).toString();
		return new DimensionMap("ImRow=" + y + ",ImCol=" + x);
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
