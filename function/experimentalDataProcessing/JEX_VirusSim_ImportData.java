package function.experimentalDataProcessing;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import rtools.R;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
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
public class JEX_VirusSim_ImportData extends ExperimentalDataCrunch {
	
	public JEX_VirusSim_ImportData()
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
		String result = "Import Virus Simulation Data Set";
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
		String result = "Function that takes a folder and creates an image (Dim: T, Z), a log file, and an Image Stack used for Calibration. T is set based on info in the log file.";
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
	 * This method defines if the function appears in the list in JEX It should be set to true except if you have good reason for it
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
		this.defaultOutputNames = new TypeName[2];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Plots");
		this.defaultOutputNames[1] = new TypeName(FILE, "Data");
		
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
		Parameter p0 = new Parameter("Folder Path", "Folder in which to find Data", Parameter.FILECHOOSER, "/Users/jaywarrick/Desktop/JavaModelOutputs");
		// Parameter p3 = new
		// Parameter("New Min","Image Intensity Value","0.0");
		// Parameter p4 = new
		// Parameter("New Max","Image Intensity Value","65535.0");
		// Parameter p5 = new
		// Parameter("Gamma","0.1-5.0, value of 1 results in no change","1.0");
		// Parameter p6 = new
		// Parameter("Output Bit Depth","Depth of the outputted image",FormLine.DROPDOWN,new
		// String[] {"8","16","32"},1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		// parameterArray.addParameter(p1);
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
		// Collect Params
		File folder = new File(this.parameters.getValueOfParameter("Folder Path"));
		
		if(folder == null || !folder.exists() || !folder.isDirectory())
		{
			JEXStatics.statusBar.setStatusText("Error reading path: " + folder.getPath());
			return false;
		}
		
		{
			return this.importDataForFolder(folder);
		}
	}
	
	private boolean importDataForFolder(File folder)
	{
		Logs.log("Importing data from folder: " + folder.getPath(), 1, this);
		// Get image files
		// Table<String> plots = JEXTableReader.getStringTable(folder.getAbsolutePath() + File.separator + "plots.arff");
		Table<String> data = JEXTableReader.getStringTable(folder.getAbsolutePath() + File.separator + "data.arff");
		String scriptFilePath = folder.getAbsolutePath() + File.separator + "plotScript.R";
		
		JEXData plotData = this.makePlots(data, scriptFilePath);
		JEXData fileData = FileWriter.makeFileObject(this.outputNames[1].getName(), null, data.data);
		
		// Set the outputs
		this.realOutputs.add(plotData);
		this.realOutputs.add(fileData);
		
		// Return status
		return true;
	}
	
	public JEXData makePlots(Table<String> data, String scriptFilePath)
	{
		// Prepare R
		R.setwd(FileUtility.getFileParent(scriptFilePath));
		// R.source(scriptFilePath);
		R.eval("source(" + R.quotedPath(scriptFilePath) + ")");
		
		// Plot the results
		TreeMap<DimensionMap,String> plotPaths = new TreeMap<DimensionMap,String>();
		for (DimensionMap map : data.dimTable.getMapIterator())
		{
			String plotPath = null;
			try
			{
				plotPath = DirectoryManager.getUniqueAbsoluteTempPath("jpeg");
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
			if(plotPath != null)
			{
				R.eval("plotScript(" + R.quotedPath(data.getData(map)) + "," + R.quotedPath(plotPath) + ")");
				plotPaths.put(map, plotPath);
			}
		}
		JEXData plotData = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), plotPaths);
		return plotData;
	}
	
}