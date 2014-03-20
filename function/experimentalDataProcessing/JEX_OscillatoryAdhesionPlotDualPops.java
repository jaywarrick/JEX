package function.experimentalDataProcessing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.Octave;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.LSVList;
import rtools.R;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.LabelReader;
import Database.DataReader.ValueReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXReader;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
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
public class JEX_OscillatoryAdhesionPlotDualPops extends ExperimentalDataCrunch {
	
	public static Octave octave = null;
	
	public JEX_OscillatoryAdhesionPlotDualPops()
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
		String result = "Oscillatory Adhesion Plot Dual Populations";
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
		String result = "Plot the data and curve fit.";
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
		inputNames[0] = new TypeName(FILE, "Plot Points");
		inputNames[1] = new TypeName(VALUE, "Curve Fit Results");
		inputNames[2] = new TypeName(LABEL, "Working Directory");
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
		this.defaultOutputNames = new TypeName[3];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Dual Plot");
		this.defaultOutputNames[1] = new TypeName(VALUE, "Dual Fit Results");
		this.defaultOutputNames[2] = new TypeName(FILE, "Dual Fit Results CSV");
		
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
		Parameter p1 = new Parameter("Initial Alpha", "Initial guess for Alpha", "0.8");
		Parameter p2 = new Parameter("Initial Tau50", "Initial guess for Tau50 of second population", "0.03");
		Parameter p3 = new Parameter("Initial Sigma", "Initial guess for Sigma of second population", "0.1");
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
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
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
		JEXData wdData = inputs.get("Working Directory");
		if(wdData == null || !wdData.getTypeName().getType().equals(JEXData.LABEL))
		{
			return false;
		}
		String wd = LabelReader.readLabelValue(wdData);
		
		// Collect the inputs
		JEXData dataData = inputs.get("Plot Points");
		if(dataData == null || !dataData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}
		String dataPath = FileReader.readFileObject(dataData);
		
		// Collect the inputs
		JEXData fitData = inputs.get("Curve Fit Results");
		if(fitData == null || !fitData.getTypeName().getType().equals(JEXData.VALUE))
		{
			return false;
		}
		TreeMap<DimensionMap,Double> fit = ValueReader.readObjectToDoubleTable(fitData);
		DimensionMap map = new DimensionMap();
		map.put("Measurement", "Tau50");
		Double tau50_1 = fit.get(map);
		map.put("Measurement", "Sigma");
		Double sigma_1 = fit.get(map);
		
		// Collect Parameters
		Double alphai = Double.parseDouble(this.parameters.getValueOfParameter("Initial Alpha"));
		Double tau50_2 = Double.parseDouble(this.parameters.getValueOfParameter("Initial Tau50"));
		Double sigma_2 = Double.parseDouble(this.parameters.getValueOfParameter("Initial Sigma"));
		
		Object[] results = this.analyzeInOctave(wd, alphai, tau50_1, sigma_1, tau50_2, sigma_2, dataPath);
		if(results == null)
		{
			return false;
		}
		
		// Save data.
		JEXData output1 = null;
		if(results[0] != null)
		{
			output1 = ImageWriter.makeImageObject(this.outputNames[0].getName(), (String) results[0]);
		}
		
		TreeMap<DimensionMap,Double> fitResults = new TreeMap<DimensionMap,Double>();
		map = new DimensionMap();
		map.put("Measurement", "Alpha");
		fitResults.put(map.copy(), (Double) results[1]);
		map.put("Measurement", "Tau50_1");
		fitResults.put(map.copy(), (Double) results[2]);
		map.put("Measurement", "Sigma_1");
		fitResults.put(map.copy(), (Double) results[3]);
		map.put("Measurement", "Tau50_2");
		fitResults.put(map.copy(), (Double) results[4]);
		map.put("Measurement", "Sigma_2");
		fitResults.put(map.copy(), (Double) results[5]);
		map.put("Measurement", "R^2");
		fitResults.put(map.copy(), (Double) results[6]);
		JEXData output2 = ValueWriter.makeValueTableFromDouble(this.outputNames[1].getName(), fitResults);
		output2.setDimTable(new DimTable(fitResults));
		JEXData output3 = FileWriter.makeFileObject(this.outputNames[2].getName(), null, (String) results[7]);
		
		// Set the outputs
		this.realOutputs.add(output1);
		this.realOutputs.add(output2);
		this.realOutputs.add(output3);
		
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
	 * @return Object[]{finalPlotFilePath, plotPointsFileString, tau50, sigma, r2, resultsFileString};
	 */
	private Object[] analyzeInOctave(String wd, double alphai, double tau50_1, double sigma_1, double tau50_2, double sigma_2, String dataFilePath)
	{
		String wdString = "cd " + R.quotedPath(wd) + ";";
		String pinString = "pin = [" + alphai + "," + tau50_1 + "," + sigma_1 + "," + tau50_2 + "," + sigma_2 + "];";
		String plotFileNameString = FileUtility.getFileNameWithExtension(JEXWriter.getUniqueRelativeTempPath("png"));
		String resultsFileString = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("txt");
		
		LSVList out = new LSVList();
		out.add("clear all;");
		out.add("pkg load all;");
		out.add(wdString);
		out.add("addToPATH('/opt/local/bin');");
		out.add("addToPATH('/opt/local/sbin');");
		out.add(pinString);
		out.add("dataFilePath = " + R.quotedPath(dataFilePath) + ";");
		out.add("plotFileName = " + R.quotedPath(plotFileNameString) + ";");
		out.add("resultsFilePath = " + R.quotedPath(resultsFileString) + ";");
		out.add("dualPopFit(pin, dataFilePath, plotFileName, resultsFilePath);");
		
		Octave oct = this.getOctave(wd);
		oct.runCommand(out.toString());
		String finalPlotFilePath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getTempFolderName() + File.separator + plotFileNameString;
		File finalPlotFile = new File(finalPlotFilePath);
		File initialPlotFile = new File(wd + File.separator + plotFileNameString);
		if(initialPlotFile.exists())
		{
			try
			{
				JEXWriter.copy(initialPlotFile, finalPlotFile);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			Logs.log("Couldn't make the plot", 0, this);
		}
		ArrayList<ArrayList<String>> results = JEXReader.readCSVFileToDataMap(new File(resultsFileString));
		ArrayList<String> fields = results.get(0);
		Double alpha = Double.parseDouble(fields.get(0));
		Double tau501 = Double.parseDouble(fields.get(1));
		Double sigma1 = Double.parseDouble(fields.get(2));
		Double tau502 = Double.parseDouble(fields.get(3));
		Double sigma2 = Double.parseDouble(fields.get(4));
		Double r2 = Double.parseDouble(fields.get(5));
		
		if(finalPlotFile.exists())
		{
			return new Object[] { finalPlotFilePath, alpha, tau501, sigma1, tau502, sigma2, r2, resultsFileString };
		}
		else
		{
			return new Object[] { null, alpha, tau501, sigma1, tau502, sigma2, r2, resultsFileString };
		}
	}
	
	@Override
	public void finalizeTicket(Ticket ticket)
	{
		if(octave != null)
		{
			octave.close();
			octave = null;
		}
	}
}
