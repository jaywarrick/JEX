package function.plugin.old;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.FileUtility;
import miscellaneous.SimpleFileFilter;
import miscellaneous.StringUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;

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
public class JEX_ScanSlideImportData extends JEXCrunchable {
	
	public static final int _3X7 = 21, _4X7 = 28, _5X7 = 35, _3X8 = 24, _4X8 = 32, _5X8 = 40, _3X9 = 27, _4X9 = 36, _5X9 = 45;
	public static final int pos = 1, loc = 3, wav = 2, t = 4;
	
	public JEX_ScanSlideImportData()
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
		String result = "Import ScanSlide Data Set";
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
		String result = "Function that takes a folder of images renamed from the scan-slide function and creates an image (Dim: ImRow, ImCol, T).";
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
		this.defaultOutputNames = new TypeName[1];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Image Set");
		
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
		Parameter p0 = new Parameter("Folder Path", "Folder in which to find Data", "/Volumes/shared");
		// Parameter p1 = new
		// Parameter("Multi-Folder Import","Try to parse each folder within this folder into each entry",FormLine.DROPDOWN,new
		// String[]{"true","false"},1);
		// Parameter p2 = new
		// Parameter("Outlier p-value","Statistical significance for determining an outlier","0.05");
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
		Logs.log("Importing data from folder: " + folder.getPath(), 1, this);
		if(folder == null || !folder.exists() || !folder.isDirectory())
		{
			JEXStatics.statusBar.setStatusText("Error reading path: " + folder.getPath());
			return false;
		}
		
		// Get image files
		List<File> imageFiles = FileUtility.getSortedFileList(folder.listFiles(new SimpleFileFilter(new String[] { "tif" })));
		
		// Determine how to group the images for each timepoint
		TreeMap<String,TreeMap<String,TreeMap<String,TreeMap<String,File>>>> posMap = this.getFileMap(imageFiles);
		TreeMap<String,TreeMap<String,TreeMap<String,File>>> locMap = posMap.get("" + (entry.getTrayY() + 1));
		if(locMap == null)
		{
			return false;
		}
		int numLocs = locMap.size();
		int numWavs = locMap.firstEntry().getValue().size();
		int numTs = locMap.firstEntry().getValue().firstEntry().getValue().size();
		int[] rcs = this.getNumRowsAndCols(numLocs);
		int rows = rcs[0];
		int cols = rcs[1];
		
		// Set up the status bar variables
		int totalFiles = numLocs * numWavs * numTs;
		int count = 0;
		int percentage = 0;
		
		// Set up the dimTable
		Dim ImRow = new Dim("ImRow", 1, rows);
		Dim ImCol = new Dim("ImCol", 1, cols);
		Dim Wavelength = new Dim("Wavelength", 1, numWavs);
		Dim Time = new Dim("Time", 1, numTs);
		
		// Make the dim table
		DimTable table = new DimTable();
		table.add(Time);
		table.add(ImRow);
		table.add(ImCol);
		table.add(Wavelength);
		
		// Put the images into a map in the right order and cull the bad images
		TreeMap<DimensionMap,String> imageMap = new TreeMap<DimensionMap,String>();
		int[] rc = new int[] { 1, 1 };
		for (String locKey : locMap.keySet())
		{
			TreeMap<String,TreeMap<String,File>> wavMap = locMap.get(locKey);
			DimensionMap locDM = new DimensionMap();
			locDM.put("ImRow", "" + rc[0]);
			locDM.put("ImCol", "" + rc[1]);
			for (String wavKey : wavMap.keySet())
			{
				TreeMap<String,File> tMap = wavMap.get(wavKey);
				DimensionMap wavDM = locDM.copy();
				wavDM.put("Wavelength", wavKey);
				for (String tKey : tMap.keySet())
				{
					DimensionMap tDM = wavDM.copy();
					tDM.put("Time", tKey);
					String newPath = JEXWriter.saveFile(tMap.get(tKey));
					imageMap.put(tDM, newPath);
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / (double) totalFiles));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
			}
			Logs.log("Position: " + locKey + "RC: " + rc[0] + "," + rc[1], 0, this);
			rc = this.getNextRCPosition(numLocs, rc[0], rc[1]);
		}
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(this.outputNames[0].getName(), imageMap);
		output1.setDimTable(table);
		JEXStatics.statusBar.setProgressPercentage(100);
		
		// Set the outputs
		this.realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	private TreeMap<String,TreeMap<String,TreeMap<String,TreeMap<String,File>>>> getFileMap(List<File> imageFiles)
	{
		String filename;
		TreeMap<String,TreeMap<String,TreeMap<String,TreeMap<String,File>>>> posMap = new TreeMap<String,TreeMap<String,TreeMap<String,TreeMap<String,File>>>>(new StringUtility());
		for (File f : imageFiles)
		{
			filename = FileUtility.getFileNameWithoutExtension(f.getPath());
			String[] tokens = filename.split("_"); // Scan0001, p, w, s, t
			String position = tokens[pos].substring(1);
			String location = tokens[loc].substring(1);
			String wavelength = tokens[wav].substring(1);
			String time = tokens[t].substring(1);
			
			TreeMap<String,TreeMap<String,TreeMap<String,File>>> locMap = posMap.get(position);
			if(locMap == null)
			{
				locMap = new TreeMap<String,TreeMap<String,TreeMap<String,File>>>(new StringUtility());
				posMap.put(position, locMap);
			}
			TreeMap<String,TreeMap<String,File>> wavMap = locMap.get(location);
			if(wavMap == null)
			{
				wavMap = new TreeMap<String,TreeMap<String,File>>(new StringUtility());
				locMap.put(location, wavMap);
			}
			TreeMap<String,File> tMap = wavMap.get(wavelength);
			if(tMap == null)
			{
				tMap = new TreeMap<String,File>(new StringUtility());
				wavMap.put(wavelength, tMap);
			}
			tMap.put(time, f);
		}
		
		return posMap;
	}
	
	@SuppressWarnings("unused")
	private Dim getTimes(File logFile)
	{
		try
		{
			// Open the file that is the first
			// command line parameter
			FileInputStream fstream = new FileInputStream(logFile);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			
			// Read File Line By Line
			int line = 1;
			String[] subTimes;
			Integer hours, minutes, seconds;
			List<String> recordedTimes = new Vector<String>();
			while ((strLine = br.readLine()) != null)
			{
				if(line > 1)
				{
					CSVList lineItems = new CSVList(strLine);
					String time = lineItems.get(0);
					time = time.substring(1, time.length() - 1);
					subTimes = time.split(":");
					hours = new Integer(subTimes[0]);
					minutes = new Integer(subTimes[1]);
					seconds = new Integer(subTimes[2]);
					seconds = seconds + 60 * minutes + 60 * 60 * hours;
					recordedTimes.add(seconds.toString());
				}
				
				line = line + 1;
			}
			Dim ret = new Dim("T", recordedTimes);
			// Close the input stream
			in.close();
			return ret;
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
		return null;
	}
	
	private int[] getNextRCPosition(int _RXC, int curRow, int curCol)
	{
		int[] rcs = this.getNumRowsAndCols(_RXC);
		int rows = rcs[0];
		int cols = rcs[1];
		
		// Use this code if the order snakes throught he slide
		// boolean isOddCol = (curCol % 2 == 1);
		// boolean isLastInCol = ((isOddCol && curRow == rows) || (!isOddCol &&
		// curRow == 1));
		// int nextCol = curCol;
		// int nextRow = curRow;
		// if(isLastInCol)
		// {
		// nextCol = nextCol + 1;
		// }
		// else if(isOddCol)
		// {
		// nextRow = nextRow + 1;
		// }
		// else
		// {
		// nextRow = nextRow - 1;
		// }
		
		// Else use this for top to bottom order
		boolean isLastInCol = (curRow == rows);
		int nextCol = curCol;
		int nextRow = curRow;
		if(isLastInCol)
		{
			nextCol = nextCol + 1;
			nextRow = 1;
		}
		else
		{
			nextRow = nextRow + 1;
		}
		
		if(nextRow > rows || nextRow < 1 || nextCol > cols)
		{
			return new int[] { -1, -1 };
		}
		
		return new int[] { nextRow, nextCol };
		
	}
	
	private int[] getNumRowsAndCols(int _RXC)
	{
		int rows = 0;
		int cols = 0;
		if(_RXC == _3X7 || _RXC == _4X7 || _RXC == _5X7)
		{
			cols = 7;
		}
		if(_RXC == _3X8 || _RXC == _4X8 || _RXC == _5X9)
		{
			cols = 8;
		}
		if(_RXC == _3X9 || _RXC == _4X9 || _RXC == _5X9)
		{
			cols = 9;
		}
		if(_RXC == _3X7 || _RXC == _3X8 || _RXC == _3X9)
		{
			rows = 3;
		}
		if(_RXC == _4X7 || _RXC == _4X8 || _RXC == _4X9)
		{
			rows = 4;
		}
		if(_RXC == _5X7 || _RXC == _5X8 || _RXC == _5X9)
		{
			rows = 5;
		}
		return new int[] { rows, cols };
	}
	
}
