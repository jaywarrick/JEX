package function.plugin.old;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
import jex.statics.Octave;
import miscellaneous.CSVList;
import miscellaneous.LSVList;
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
public class JEX_OscillatoryAdhesionAnalysis3 extends JEXCrunchable {
	
	public static Octave octave = null;
	
	public JEX_OscillatoryAdhesionAnalysis3()
	{}
	
	public Octave getOctave(String workingDirectory)
	{
		if(octave == null)
		{
			octave = new Octave(workingDirectory);
		}
		return octave;
	}
	
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
		String result = "Oscillatory Adhesion Analysis (3)";
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
		String result = "Use the difference images and an ROI to measure the extent of adhesion in that ROI.";
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
		TypeName[] inputNames = new TypeName[3];
		inputNames[0] = new TypeName(IMAGE, "Difference Images");
		inputNames[1] = new TypeName(FILE, "Octave Summary File");
		inputNames[2] = new TypeName(ROI, "ROI");
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
		this.defaultOutputNames[0] = new TypeName(FILE, "New Octave Summary File");
		
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
		Parameter p1 = new Parameter("Dummy Parameter", "Placeholder param.", Parameter.DROPDOWN, new String[] { "True" }, 0);
		// Parameter p4 = new
		// Parameter("New Max","Image Intensity Value","65535.0");
		// Parameter p5 = new
		// Parameter("Gamma","0.1-5.0, value of 1 results in no change","1.0");
		// Parameter p6 = new
		// Parameter("Output Bit Depth","Depth of the outputted image",FormLine.DROPDOWN,new
		// String[] {"8","16","32"},1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		// parameterArray.addParameter(p2);
		// parameterArray.addParameter(p3);
		// parameterArray.addParameter(p4);
		// parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6);
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
		JEXData imageData = inputs.get("Difference Images");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		DimTable originalDimTable = imageData.getDimTable().copy();
		TreeMap<DimensionMap,String> imagePaths = ImageReader.readObjectToImagePathTable(imageData);
		
		// Collect the inputs
		JEXData fileData = inputs.get("Octave Summary File");
		if(fileData == null || !fileData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}
		String currentMFilePath = Database.DataReader.FileReader.readFileObject(fileData);
		
		// Collect the inputs
		JEXData roiData = inputs.get("ROI");
		if(roiData == null || !roiData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		
		// Collect Parameters
		String dimName = "Z";
		
		// Get the subDimTable (i.e. the dimtable without the different Z's)
		Dim dimToProject = originalDimTable.getDimWithName(dimName);
		if(dimToProject == null)
		{
			return false;
		}
		DimTable subDimTable = originalDimTable.copy();
		subDimTable.removeDimWithName(dimName);
		
		TreeMap<DimensionMap,List<Double>> signalMap = new TreeMap<DimensionMap,List<Double>>();
		int total = subDimTable.mapCount(), count = 0;
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap timeMap : subDimTable.getMapIterator())
		{
			if(JEXStatics.cruncher.stopCrunch)
			{
				return false;
			}
			
			// Get maps for zstack at this time
			List<DimensionMap> zMaps = this.getAllStackMaps(timeMap, dimToProject);
			
			// Get baseline results... Object[] = {DimensionMap of image used
			// for reference image, Double reference measurement, Double
			// threshold}
			ImagePlus temp;
			ImageStatistics stats;
			List<Double> imDiffs = new Vector<Double>(0);
			for (DimensionMap zMap : zMaps)
			{
				String path = imagePaths.get(zMap);
				if(path != null)
				{
					// Get Data
					temp = new ImagePlus(path);
					
					// Get the difference signal;
					ROIPlus roip = roiMap.get(zMap);
					Roi imageJRoi = roip.getRoi();
					temp.setRoi(imageJRoi);
					stats = temp.getStatistics(Measurements.MEAN);
					imDiffs.add(stats.mean);
				}
			}
			
			// Store results
			signalMap.put(timeMap, imDiffs);
			
			// Update progress bar
			count = count + 1;
			int percentage = (int) (100 * ((double) (count) / (double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		LSVList header = getHeader(6, currentMFilePath);
		
		String newMFilePath = this.analyzeInOctave(subDimTable.getDimensionMaps(), header, signalMap);
		if(newMFilePath == null)
		{
			return false;
		}
		
		JEXData output1 = FileWriter.makeFileObject(this.outputNames[0].getName(), null, newMFilePath);
		
		// Set the outputs
		this.realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	/**
	 * Performs analysis is octave and returns the results.
	 * 
	 * @param timeMaps
	 * @param baselineMap
	 * @param signalMap
	 * @param threshMap
	 * @param fStart
	 * @param fEnd
	 * @param cal
	 * @param wd
	 * @return mFilePath (Octave Summary File)
	 */
	private String analyzeInOctave(List<DimensionMap> timeMaps, LSVList header, TreeMap<DimensionMap,List<Double>> signalMap)
	{
		DecimalFormat formatD = new DecimalFormat("##0.000");
		LSVList signals = new LSVList();
		CSVList signal = new CSVList();
		int counter = 1;
		for (DimensionMap timeMap : timeMaps)
		{
			// Get the signal values.
			for (Double value : signalMap.get(timeMap))
			{
				signal.add(formatD.format(value));
			}
			signals.add("data(" + counter + ").diffStackRow = [" + signal.toString() + "];");
			signal.clear();
			counter = counter + 1;
		}
		
		String signalString = signals.toString();
		
		header.add(signalString);
		header.add("adhesion = adhesionCurvesDiffAll(data,baseline);");
		header.add("[t, f, tau, adhesion] = getLogData(adhesion, time, [], fStart, fEnd, cal, 500);");
		
		String mFilePath = JEXWriter.saveText(header.toString(), "m");
		
		return mFilePath;
	}
	
	private List<DimensionMap> getAllStackMaps(DimensionMap map, Dim dimToProject)
	{
		List<DimensionMap> ret = new Vector<DimensionMap>();
		for (int i = 0; i < dimToProject.size(); i++)
		{
			ret.add(this.getAStackMap(map, dimToProject, i));
		}
		return ret;
	}
	
	private DimensionMap getAStackMap(DimensionMap map, Dim dimToProject, int indexOfValue)
	{
		DimensionMap ret = map.copy();
		ret.put(dimToProject.name(), dimToProject.valueAt(indexOfValue));
		return ret;
	}
	
	public static String parseVariableText(String variable, String path)
	{
		String thisLine;
		BufferedReader br = null;
		// Open the file for reading
		try
		{
			br = new BufferedReader(new FileReader(path));
			while ((thisLine = br.readLine()) != null)
			{
				if(thisLine.startsWith(variable))
				{
					;
				}
				return thisLine;
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(br != null)
			{
				try
				{
					br.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	
	public static Vector<Double> parseVector(String variable, String path)
	{
		Vector<Double> nums = new Vector<Double>();
		String[] frags1 = parseVariableText(variable, path).split("[");
		String[] frags2 = frags1[1].split("]");
		CSVList data = new CSVList(frags2[0]);
		for (String num : data)
		{
			nums.add(Double.parseDouble(num));
		}
		return nums;
		
	}
	
	public static LSVList getHeader(int numberOfLines, String path)
	{
		LSVList ret = new LSVList();
		BufferedReader br = null;
		// Open the file for reading
		try
		{
			int counter = 0;
			String thisLine;
			br = new BufferedReader(new FileReader(path));
			while ((thisLine = br.readLine()) != null && counter < numberOfLines)
			{
				ret.add(thisLine);
				counter = counter + 1;
			}
			br.close();
			if(counter != numberOfLines)
			{
				return null;
			}
			return ret;
		}
		catch (IOException e)
		{
			System.err.println("Error: " + e);
			if(br != null)
			{
				try
				{
					br.close();
				}
				catch (IOException e1)
				{
					e1.printStackTrace();
				}
			}
		}
		return null;
	}
}
