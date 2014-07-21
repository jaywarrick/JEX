package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.Type;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.gui.Wand;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import image.roi.IdPoint;
import image.roi.ROIPlus;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import miscellaneous.Canceler;
import miscellaneous.StatisticsUtility;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;

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
public class JEX_SingleCell_SegmentedMaskQuantification extends JEXCrunchable {
	
	public JEX_SingleCell_SegmentedMaskQuantification()
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
		String result = "Segmented Mask Quantification";
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
		String result = "Measure in a mask, segmented using a segmented image, using id numbers given my a maxima ROI.";
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
		TypeName[] inputNames = new TypeName[4];
		inputNames[0] = new TypeName(ROI, "Maxima");
		inputNames[1] = new TypeName(IMAGE, "Segmented Image");
		inputNames[2] = new TypeName(IMAGE, "Mask Image");
		inputNames[3] = new TypeName(IMAGE, "Image To Quantify");
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
		this.defaultOutputNames[0] = new TypeName(FILE, "Boundary Measurements");
		
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
		// (ImageProcessor ip, double tolerance, double threshold, int
		// outputType, boolean excludeOnEdges, boolean isEDM, Roi roiArg,
		// boolean lightBackground)
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		// Parameter pa1 = new Parameter("Color Dim Name", "Name of the color dimension.", "Color");
		Parameter pa2 = new Parameter("Measurement", "The type of measurement to perform", Parameter.DROPDOWN, operations, 0);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		// parameterArray.addParameter(pa1);
		parameterArray.addParameter(pa2);
		
		return parameterArray;
	}
	
	public static String[] operations = new String[] { "MEAN", "MEDIAN", "MIN", "MAX", "SUM", "STDDEV", "VARIANCE" };
	
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
		try
		{
			/*
			 * inputNames[2] = new TypeName(IMAGE, "Outer Boundary Image"); inputNames[3] = new TypeName(IMAGE, "Inner Boundary Image"); inputNames[4] = new TypeName(IMAGE, "Image To Quantify");
			 */
			// Collect the inputs
			JEXData maximaData = getInputAs(inputs, "Maxima", ROI);
			if(maximaData == null)
			{
				return false;
			}
			JEXData segData = getInputAs(inputs, "Segmented Image", IMAGE);
			if(segData == null)
			{
				return false;
			}
			JEXData maskData = getInputAs(inputs, "Mask Image", IMAGE);
			if(maskData == null)
			{
				return false;
			}
			JEXData imageData = getInputAs(inputs, "Image To Quantify", IMAGE);
			if(imageData == null)
			{
				return false;
			}
			
			// Gather parameters
			String op = this.parameters.getValueOfParameter("Measurement");
			
			DimTable imageTable = imageData.getDimTable();
			
			TreeMap<DimensionMap,ROIPlus> maximaMap = RoiReader.readObjectToRoiMap(maximaData);
			TreeMap<DimensionMap,String> segMap = ImageReader.readObjectToImagePathTable(segData);
			TreeMap<DimensionMap,String> maskMap = ImageReader.readObjectToImagePathTable(maskData);
			TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
			TreeMap<DimensionMap,Double> results = new TreeMap<DimensionMap,Double>();
			
			double count = 0;
			double total = imageTable.mapCount();
			
			for (DimensionMap map : imageTable.getMapIterator())
			{
				if(this.isCanceled())
				{
					return false;
				}
				this.runStuff(op, map, maximaMap, segMap, maskMap, imageMap, results, this);
				count = count + 1;
				JEXStatics.statusBar.setProgressPercentage((int) (100 * count / total));
			}
			
			String resultsFile = JEXTableWriter.writeTable(this.outputNames[0].getName(), results);
			JEXData output0 = FileWriter.makeFileObject(this.outputNames[0].getName(), resultsFile);
			
			// Set the outputs
			this.realOutputs.add(output0);
			
			// Return status
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		
	}
	
	public void runStuff(String op, DimensionMap map, TreeMap<DimensionMap,ROIPlus> maximaMap, TreeMap<DimensionMap,String> segMap, TreeMap<DimensionMap,String> maskMap, TreeMap<DimensionMap,String> imageMap, TreeMap<DimensionMap,Double> results, Canceler canceler)
	{
		// Get the Maxima
		ROIPlus maxima = maximaMap.get(map);
		
		// For each maxima, get the segmented region roi
		ByteProcessor segImp = (ByteProcessor) (new ImagePlus(segMap.get(map))).getProcessor();
		ByteProcessor maskImp = (ByteProcessor) (new ImagePlus(maskMap.get(map))).getProcessor();
		FloatProcessor imageImp = (FloatProcessor) (new ImagePlus(imageMap.get(map))).getProcessor().convertToFloat();
		Wand wand = new Wand(segImp);
		Vector<Double> measurements;
		for (IdPoint p : maxima.getPointList())
		{
			if(canceler.isCanceled())
			{
				return;
			}
			wand.autoOutline(p.x, p.y);
			// double outerT = 0, innerT = 0;
			if(wand.npoints > 0)
			{
				Polygon poly = new Polygon(wand.xpoints, wand.ypoints, wand.npoints);
				Rectangle r = poly.getBounds();
				measurements = new Vector<Double>();
				for (int i = r.x; i < r.x + r.width; i++)
				{
					for (int j = r.y; j < r.y + r.height; j++)
					{
						// innerBoundary
						if(poly.contains(i, j) && maskImp.getPixelValue(i, j) == 255)
						{
							measurements.add((double) imageImp.getPixelValue(i, j));
							// Logs.log("In - " + innerT, this);
						}
					}
				}
				
				if(measurements.size() > 0)
				{
					double measurement = 0;
					
					// String[] operations = new String[] { "MEAN", "MEDIAN", "MIN", "MAX", "SUM", "STD", "VAR" };
					if(op.equals("MEAN"))
					{
						measurement = StatisticsUtility.mean(measurements);
					}
					else if(op.equals("MEDIAN"))
					{
						measurement = StatisticsUtility.median(measurements);
					}
					else if(op.equals("SUM"))
					{
						measurement = StatisticsUtility.sum(measurements);
					}
					else if(op.equals("MIN"))
					{
						measurement = StatisticsUtility.min(measurements);
					}
					else if(op.equals("MAX"))
					{
						measurement = StatisticsUtility.max(measurements);
					}
					else if(op.equals("STDDEV"))
					{
						measurement = StatisticsUtility.stdDev(measurements);
					}
					else if(op.equals("VARIANCE"))
					{
						measurement = StatisticsUtility.variance(measurements);
					}
					
					// Store the outer and inner measurements
					DimensionMap resultsMap = map.copy();
					resultsMap.put("ID", "" + p.id);
					results.put(resultsMap.copy(), measurement);
				}
			}
		}
	}
	
	public static JEXData getInputAs(HashMap<String,JEXData> inputs, String name, Type type)
	{
		JEXData data = inputs.get(name);
		if(data == null || !data.getTypeName().getType().equals(type))
		{
			return null;
		}
		return data;
	}
	
	// private String saveAdjustedImage(String imagePath, double oldMin, double
	// oldMax, double newMin, double newMax, double gamma, int bitDepth)
	// {
	// // Get image data
	// File f = new File(imagePath);
	// if(!f.exists()) return null;
	// ImagePlus im = new ImagePlus(imagePath);
	// FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat();
	// // should be a float processor
	//
	// // Adjust the image
	// FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
	//
	// // Save the results
	// ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false",
	// bitDepth);
	// String imPath = JEXWriter.saveImage(toSave);
	// im.flush();
	//
	// // return temp filePath
	// return imPath;
	// }
}