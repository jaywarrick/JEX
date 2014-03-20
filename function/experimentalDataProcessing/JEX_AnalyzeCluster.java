package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.FloatProcessor;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;

import java.util.HashMap;
import java.util.TreeMap;

import logs.Logs;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.ExperimentalDataCrunch;
import function.imageUtility.jBackgroundSubtracter;

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
public class JEX_AnalyzeCluster extends ExperimentalDataCrunch {
	
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
		String result = "Analyze clusters";
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
		String result = "Analyze the number and size of cell clusters in an fluorescent microscope image. Optional pre-filtering to remove small patches";
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
		String toolbox = "Custom Cell Analysis";
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
		TypeName[] inputNames = new TypeName[2];
		
		inputNames[0] = new TypeName(IMAGE, "Image to process");
		inputNames[1] = new TypeName(ROI, "Optional ROI");
		
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
		defaultOutputNames = new TypeName[3];
		defaultOutputNames[0] = new TypeName(VALUE, "Number of Clusters");
		defaultOutputNames[1] = new TypeName(VALUE, "Total area");
		defaultOutputNames[2] = new TypeName(IMAGE, "Outline image");
		
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
		Parameter p1 = new Parameter("Background radius", "Radius of the rolling ball for background substract, -1 for no filtering", "50.0");
		Parameter p3 = new Parameter("Min cluster size", "Minimum cluster size", "40.0");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p3);
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
		JEXData data1 = inputs.get("Image to process");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(data1);
		
		JEXData data2 = inputs.get("Optional ROI");
		ROIPlus roi = null;
		if(data2.getTypeName().getType().equals(JEXData.ROI))
		{
			roi = RoiReader.readObjectToRoi(data2);
		}
		
		// //// Get params
		double radius = Double.parseDouble(parameters.getValueOfParameter("Background radius"));
		double minSize = Double.parseDouble(parameters.getValueOfParameter("Min cluster size"));
		
		// Run the function
		int count = 0;
		int total = imageMap.size();
		TreeMap<DimensionMap,String> meanMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> nbMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		
		for (DimensionMap dim : imageMap.keySet())
		{
			// Get the image
			String path = imageMap.get(dim);
			// File fpath = new File(path);
			ImagePlus im = new ImagePlus(path);
			
			// get the image
			FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should
			// be
			// a
			// float
			// processor
			
			// Subtract background
			jBackgroundSubtracter bS = new jBackgroundSubtracter();
			bS.setup("", im);
			jBackgroundSubtracter.radius = radius; // default rolling ball
			// radius
			jBackgroundSubtracter.lightBackground = false;
			jBackgroundSubtracter.createBackground = false;
			jBackgroundSubtracter.useParaboloid = false; // use
			// "Sliding Paraboloid"
			// instead of rolling
			// ball algorithm
			jBackgroundSubtracter.doPresmooth = false;
			bS.run(imp);
			
			// Crop image
			if(roi != null)
			{
				ij.gui.Roi imroi = roi.getRoi();
				java.awt.Rectangle rect = imroi.getBounds();
				imp.setRoi(rect);
				imp = (FloatProcessor) imp.crop();
			}
			
			// Threshold image
			ij.process.ByteProcessor bimp = (ij.process.ByteProcessor) imp.duplicate().convertToByte(true);
			int measurement = Measurements.MEDIAN | Measurements.STD_DEV | Measurements.MEAN;
			ImageStatistics stats = ImageStatistics.getStatistics(bimp, measurement, null);
			
			double meanInt = stats.mean;
			double stdev = stats.stdDev;
			double threshold = meanInt + 1.5 * stdev;
			
			bimp.threshold((int) threshold);
			bimp.invertLut();
			
			// Analyze clusters
			ResultsTable rT = new ResultsTable();
			int options = ParticleAnalyzer.SHOW_OUTLINES;
			int measure = Measurements.AREA | Measurements.CIRCULARITY | Measurements.PERIMETER;
			ParticleAnalyzer pA = new ParticleAnalyzer(options, measure, rT, minSize, Double.POSITIVE_INFINITY);
			pA.setHideOutputImage(true);
			pA.analyze(new ImagePlus("", bimp));
			
			int nb = rT.getColumn(0).length;
			int lastColumn = rT.getLastColumn();
			float mean = 0;
			for (int i = 0; i < lastColumn; i++)
			{
				String cName = rT.getColumnHeading(i);
				if(cName.equals("Area"))
				{
					float[] values = rT.getColumn(i);
					for (float f : values)
					{
						mean = mean + f;
					}
				}
			}
			
			nbMap.put(dim, "" + nb);
			meanMap.put(dim, "" + mean);
			
			ImagePlus outIm = pA.getOutputImage();
			ij.process.ByteProcessor outImp = (ij.process.ByteProcessor) outIm.getProcessor().convertToByte(true);
			
			// //// Save the results
			// String localDir = JEXWriter.getEntryFolder(entry);
			// String newFileName = FunctionUtility.getNextName(localDir,
			// fpath.getName(), "Outline");
			// String outputImagePath = localDir + File.separator + newFileName;
			// FunctionUtility.imSave(outImp, outputImagePath);
			// outputImageMap.put(dim, outputImagePath);
			
			// Save the result
			String savePath = JEXWriter.saveImage(outImp);
			outputImageMap.put(dim, savePath);
			
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;
		}
		
		JEXData output1 = ValueWriter.makeValueTable(outputNames[0].getName(), nbMap);
		JEXData output2 = ValueWriter.makeValueTable(outputNames[1].getName(), meanMap);
		JEXData output3 = ImageWriter.makeImageStackFromPaths(outputNames[2].getName(), imageMap);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		
		// Return status
		return true;
	}
}
