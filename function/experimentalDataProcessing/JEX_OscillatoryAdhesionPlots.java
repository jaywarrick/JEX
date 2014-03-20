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
public class JEX_OscillatoryAdhesionPlots extends ExperimentalDataCrunch {
	
	public static Octave octave = null;
	
	public JEX_OscillatoryAdhesionPlots()
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
		String result = "Oscillatory Adhesion Plots";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(FILE, "Octave Summary File");
		inputNames[1] = new TypeName(LABEL, "Working Directory");
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
		this.defaultOutputNames = new TypeName[4];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Plot");
		this.defaultOutputNames[1] = new TypeName(FILE, "Plot Points");
		this.defaultOutputNames[2] = new TypeName(VALUE, "Curve Fit Results");
		this.defaultOutputNames[3] = new TypeName(FILE, "Curve Fit Results CSV");
		
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
		Parameter p1 = new Parameter("Initial Tau50", "Initial guess for Tau50", "0.0005");
		Parameter p2 = new Parameter("Initial Sigma", "Initial guess for Sigma", "0.45");
		Parameter p3 = new Parameter("Initial Max % Adhesion", "Initial guess for Initial Max % Adhesion", "100");
		Parameter p4 = new Parameter("Plot Xmin", "Xmin on plot", "0.0005");
		Parameter p5 = new Parameter("Plot Xmax", "Xmax on plot", "0.15");
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
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
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
		JEXData mFileData = inputs.get("Octave Summary File");
		if(mFileData == null || !mFileData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}
		String mFilePath = FileReader.readFileObject(mFileData);
		
		// Collect Parameters
		Double tau50i = Double.parseDouble(this.parameters.getValueOfParameter("Initial Tau50"));
		Double sigmai = Double.parseDouble(this.parameters.getValueOfParameter("Initial Sigma"));
		Double ymini = Double.parseDouble(this.parameters.getValueOfParameter("Initial Max % Adhesion"));
		ymini = 1 - ymini / 100.0;
		Double xmin = Double.parseDouble(this.parameters.getValueOfParameter("Plot Xmin"));
		Double xmax = Double.parseDouble(this.parameters.getValueOfParameter("Plot Xmax"));
		
		Object[] results = this.analyzeInOctave(wd, tau50i, sigmai, ymini, xmin, xmax, mFilePath);
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
		
		JEXData output2 = FileWriter.makeFileObject(this.outputNames[1].getName(), null, (String) results[1]);
		TreeMap<DimensionMap,Double> fitResults = new TreeMap<DimensionMap,Double>();
		DimensionMap map = new DimensionMap();
		map.put("Measurement", "Tau50");
		fitResults.put(map.copy(), (Double) results[2]);
		map.put("Measurement", "Sigma");
		fitResults.put(map.copy(), (Double) results[3]);
		map.put("Measurement", "Ymin");
		fitResults.put(map.copy(), (Double) results[4]);
		map.put("Measurement", "R^2");
		fitResults.put(map.copy(), (Double) results[5]);
		JEXData output3 = ValueWriter.makeValueTableFromDouble(this.outputNames[2].getName(), fitResults);
		output3.setDimTable(new DimTable(fitResults));
		JEXData output4 = FileWriter.makeFileObject(this.outputNames[3].getName(), null, (String) results[6]);
		
		// Set the outputs
		this.realOutputs.add(output1);
		this.realOutputs.add(output2);
		this.realOutputs.add(output3);
		this.realOutputs.add(output4);
		
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
	private Object[] analyzeInOctave(String wd, double tau50i, double sigmai, double ymini, double xmin, double xmax, String mFilePath)
	{
		String wdString = "cd " + R.quotedPath(wd) + ";";
		String pinString = "pin = [" + tau50i + "," + sigmai + "," + ymini + "];";
		if(ymini == 0.0)
		{
			// force to zero
			pinString = "pin = [" + tau50i + "," + sigmai + "];";
		}
		String sourceMFileString = "source(" + R.quotedPath(mFilePath) + ");";
		String plotPointsFileString = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("txt");
		String plotFileNameString = FileUtility.getFileNameWithExtension(JEXWriter.getUniqueRelativeTempPath("png"));
		String resultsFileString = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("txt");
		
		LSVList out = new LSVList();
		out.add("clear all;");
		out.add("pkg load all;");
		out.add(wdString);
		out.add("addToPATH('/opt/local/bin');");
		out.add("addToPATH('/opt/local/sbin');");
		out.add(pinString);
		out.add(sourceMFileString);
		out.add("plotPointsFilePath = " + R.quotedPath(plotPointsFileString) + ";");
		out.add("plotFileName = " + R.quotedPath(plotFileNameString) + ";");
		out.add("resultsFilePath = " + R.quotedPath(resultsFileString) + ";");
		out.add("csvwrite(plotPointsFilePath, [tau' adhesion']);");
		out.add("clf;");
		out.add("hold on;");
		out.add("h = semilogx(tau,100*(1-adhesion),'-b','linewidth',5);");
		out.add("jplot('Adhesion vs Shear Stress', 'Shear Stress [Pa]', 'Percent Adhered [%]', 'Helvetica', 12);");
		out.add("[f,fit.p,kvg,iter,corp,covp,covr,stdresid,Z,fit.r2] = leasqr(tau', adhesion', pin, \"pLogNorm\");");
		out.add("csvwrite(resultsFilePath, [fit.p' fit.r2']);");
		out.add("tauMin = " + xmin + ";");
		out.add("tauMax = " + xmax + ";");
		out.add("newTau = logspace(log10(tauMin),log10(tauMax),150);");
		out.add("h = semilogx(newTau,100*(1-pLogNorm(newTau,fit.p)),'-r','linewidth',5);");
		out.add("axis([tauMin,1.25*tauMax,-10,110]);");
		out.add("drawnow();");
		out.add("saveas(h,plotFileName);");
		out.add("hold off;");
		out.add("fit;");
		
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
		Double tau50 = Double.parseDouble(fields.get(0));
		Double sigma = Double.parseDouble(fields.get(1));
		Double ymin = 0.0;
		Double r2 = -1.0;
		if(ymini != 0.0)
		{
			// if not forced to zero get the fitted value
			ymin = Double.parseDouble(fields.get(2));
			r2 = Double.parseDouble(fields.get(3));
		}
		else
		{
			// if forced then R^2 is at index 2
			r2 = Double.parseDouble(fields.get(2));
		}
		
		if(finalPlotFile.exists())
		{
			return new Object[] { finalPlotFilePath, plotPointsFileString, tau50, sigma, ymin, r2, resultsFileString };
		}
		else
		{
			return new Object[] { null, plotPointsFileString, tau50, sigma, ymin, r2, resultsFileString };
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
