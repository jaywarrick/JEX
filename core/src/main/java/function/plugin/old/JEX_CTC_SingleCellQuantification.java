package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.Definition.ParameterSet;
import Database.Definition.Type;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.process.Blitter;
import ij.process.ByteBlitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
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
public class JEX_CTC_SingleCellQuantification extends JEXCrunchable {
	
	public JEX_CTC_SingleCellQuantification()
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
		String result = "CTC Single Cell Quantification";
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
		String result = "Use the cropped points images to quantify cell attributes.";
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
		this.defaultOutputNames[0] = new TypeName(FILE, "Cell Measurements");
		
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
		// Parameter pa2 = new Parameter("Dummy Parameter", "The type of measurement to perform", Parameter.DROPDOWN, operations, 0);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		// parameterArray.addParameter(pa1);
		// parameterArray.addParameter(pa2);
		
		return parameterArray;
	}
	
	public static String[] binaryMeasures = new String[] { "AREA", "PERIMETER", "CIRCULARITY" };
	public static String[] grayscaleMeasures = new String[] { "MEAN", "MEDIAN", "MIN", "MAX", "SUM", "STDDEV", "VARIANCE" };
	
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
			// String op = this.parameters.getValueOfParameter("Measurement");
			
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
				this.runStuff(map, maximaMap, segMap, maskMap, imageMap, results, this);
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
	
	public void runStuff(DimensionMap map, TreeMap<DimensionMap,ROIPlus> maximaMap, TreeMap<DimensionMap,String> segMap, TreeMap<DimensionMap,String> maskMap, TreeMap<DimensionMap,String> imageMap, TreeMap<DimensionMap,Double> results, Canceler canceler)
	{
		// Get the Maxima
		ROIPlus maxima = maximaMap.get(map);
		
		// Make the mask image impMask
		// ByteProcessor impMask = (ByteProcessor) (new ImagePlus(maskMap.get(map)).getProcessor().convertToByte(false));
		// ByteProcessor impSeg = (ByteProcessor) (new ImagePlus(segMap.get(map)).getProcessor().convertToByte(false));
		ByteProcessor impSeg = (ByteProcessor) (new ImagePlus(segMap.get(map))).getProcessor();
		ByteProcessor impMask = (ByteProcessor) (new ImagePlus(maskMap.get(map))).getProcessor();
		ByteBlitter blit = new ByteBlitter(impSeg);
		blit.copyBits(impMask, 0, 0, Blitter.AND);
		FloatProcessor impImage = (FloatProcessor) (new ImagePlus(imageMap.get(map))).getProcessor().convertToFloat();
		Wand wand = new Wand(impSeg);
		Wand wand2 = new Wand(impMask);
		Vector<Double> measurements;
		for (IdPoint p : maxima.getPointList())
		{
			if(canceler.isCanceled())
			{
				return;
			}
			if(impSeg.getPixel(p.x, p.y) == 255) // if we land on a cell that made it through thresholding
			{
				wand.autoOutline(p.x, p.y); // outline it
				wand2.autoOutline(p.x, p.y);
				boolean partOfCellClump = !this.selectionsAreEqual(wand, wand2); // If the segmented and unsegmented masks do not agree on the roi, then this cell is part of a clump.
				if(wand.npoints > 0)
				{
					Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON); // The roi helps for using getLength() (DON'T USE Roi.TRACED_ROI., IT SCREWS UP THE Polygon OBJECTS!!!! Bug emailed to ImageJ folks)
					Polygon poly = new Polygon(wand.xpoints, wand.ypoints, wand.npoints); // The polygon helps for using contains()
					Rectangle r = roi.getBounds();
					measurements = new Vector<Double>();
					for (int i = r.x; i < r.x + r.width; i++)
					{
						for (int j = r.y; j < r.y + r.height; j++)
						{
							// innerBoundary
							if(poly.contains(i, j) && impSeg.getPixelValue(i, j) == 255)
							{
								measurements.add((double) impImage.getPixelValue(i, j));
								// Logs.log("In - " + innerT, this);
							}
						}
					}
					
					impMask.setRoi(roi);
					ImageStatistics stats = ImageStatistics.getStatistics(impMask, ImageStatistics.AREA & ImageStatistics.PERIMETER & ImageStatistics.CIRCULARITY & ImageStatistics.ELLIPSE, null);
					if(measurements.size() > 0)
					{
						DimensionMap resultsMap = map.copy();
						resultsMap.put("Id", "" + p.id);
						
						resultsMap.put("Measurement", "X");
						results.put(resultsMap.copy(), (double) p.x);
						resultsMap.put("Measurement", "Y");
						results.put(resultsMap.copy(), (double) p.y);
						resultsMap.put("Measurement", "AREA");
						results.put(resultsMap.copy(), stats.area);
						resultsMap.put("Measurement", "PERIMETER");
						results.put(resultsMap.copy(), roi.getLength());
						resultsMap.put("Measurement", "CIRCULARITY");
						results.put(resultsMap.copy(), 4.0 * Math.PI * (stats.area / (Math.pow(roi.getLength(), 2))));
						resultsMap.put("Measurement", "ELLIPSE MAJOR");
						results.put(resultsMap.copy(), stats.major);
						resultsMap.put("Measurement", "ELLIPSE MINOR");
						results.put(resultsMap.copy(), stats.minor);
						resultsMap.put("Measurement", "MEAN");
						results.put(resultsMap.copy(), StatisticsUtility.mean(measurements));
						resultsMap.put("Measurement", "MEDIAN");
						results.put(resultsMap.copy(), StatisticsUtility.median(measurements));
						resultsMap.put("Measurement", "SUM");
						results.put(resultsMap.copy(), StatisticsUtility.sum(measurements));
						resultsMap.put("Measurement", "MIN");
						results.put(resultsMap.copy(), StatisticsUtility.min(measurements));
						resultsMap.put("Measurement", "MAX");
						results.put(resultsMap.copy(), StatisticsUtility.max(measurements));
						resultsMap.put("Measurement", "STDDEV");
						results.put(resultsMap.copy(), StatisticsUtility.stdDev(measurements));
						resultsMap.put("Measurement", "VARIANCE");
						results.put(resultsMap.copy(), StatisticsUtility.variance(measurements));
						resultsMap.put("Measurement", "CLUMP");
						results.put(resultsMap.copy(), (double) (partOfCellClump ? 1 : 0));
					}
				}
			}
		}
	}
	
	public boolean selectionsAreEqual(Wand w1, Wand w2)
	{
		if(w1.npoints == w2.npoints)
		{
			for (int i = 0; i < w1.npoints; i++)
			{
				if(w1.xpoints[i] != w2.xpoints[i])
				{
					return false;
				}
			}
			for (int i = 0; i < w1.npoints; i++)
			{
				if(w1.ypoints[i] != w2.ypoints[i])
				{
					return false;
				}
			}
			return true;
		}
		return false;
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