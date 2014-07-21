package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import ij.ImagePlus;

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
public class JEX_OscillatoryImportData extends JEXCrunchable {
	
	public JEX_OscillatoryImportData()
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
		String result = "Import Oscillatory Data Set";
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
		defaultOutputNames = new TypeName[3];
		defaultOutputNames[0] = new TypeName(IMAGE, "Image Sets");
		defaultOutputNames[1] = new TypeName(IMAGE, "Calibration Stack");
		defaultOutputNames[2] = new TypeName(FILE, "Log File");
		
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
		Parameter p0 = new Parameter("Folder Path", "Folder in which to find Data", "/Volumes/BeebeBig");
		Parameter p1 = new Parameter("Multi-Folder Import", "Try to parse each folder within this folder into each entry", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p2 = new Parameter("Folder Base Name", "Name of the channel folder without the number (used for multi-folder import only)", "Channel ");
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
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
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
		File folder = new File(parameters.getValueOfParameter("Folder Path"));
		String folderBaseName = parameters.getValueOfParameter("Folder Base Name");
		
		if(folder == null || !folder.exists() || !folder.isDirectory())
		{
			JEXStatics.statusBar.setStatusText("Error reading path: " + folder.getPath());
			return false;
		}
		boolean folderMode = Boolean.parseBoolean(parameters.getValueOfParameter("Multi-Folder Import"));
		
		// Multi Folder Mode
		if(folderMode)
		{
			String folderBasePath = folder.getAbsolutePath();
			String folderFullPath = folderBasePath + File.separator + folderBaseName + (entry.getTrayY() + 1);
			folder = new File(folderFullPath);
			if(folder == null || !folder.exists() || !folder.isDirectory())
			{
				JEXStatics.statusBar.setStatusText("Error reading path: " + folder.getPath());
				return false;
			}
			return importDataForChannelFolder(folder);
		}
		else
		{
			return importDataForChannelFolder(folder);
		}
	}
	
	private boolean importDataForChannelFolder(File folder)
	{
		Logs.log("Importing data from folder: " + folder.getPath(), 1, this);
		// Get image files
		List<File> imageFiles = FileUtility.getSortedFileList(folder.listFiles(new SimpleFileFilter(new String[] { "tif" })));
		
		// Get Calibration Images
		File calibrationFolder = new File(folder.getPath() + File.separator + "Calibration");
		List<File> calibrationImages = FileUtility.getSortedFileList(calibrationFolder.listFiles(new SimpleFileFilter(new String[] { "tif" })));
		
		// Get the log file
		List<File> logFiles = FileUtility.getSortedFileList(folder.listFiles(new SimpleFileFilter(new String[] { "log" })));
		if(logFiles.size() == 0)
			return false;
		File logFile = logFiles.get(0);
		
		// Set up counter for status bar
		int totalFiles = imageFiles.size() + calibrationImages.size() + 1;
		
		// Create Dim with times
		Dim T = this.getTimes(logFile);
		if(T == null)
			return false;
		
		// Determine how to group the images for each timepoint
		List<List<File>> fileLists = getSeparatedFileLists(imageFiles);
		Dim Z = new Dim("Z", fileLists.get(fileLists.size() - 1).size());
		
		// Make the dim table for the cell motion images
		DimTable table = new DimTable();
		table.add(T);
		table.add(Z); // Last dim is largest
		
		// Put the images into a map in the right order and cull the bad images
		TreeMap<DimensionMap,String> imageMap = new TreeMap<DimensionMap,String>();
		List<DimensionMap> maps;
		List<String> times = T.values();
		String time = null;
		int zIndex = 0, tIndex = 0, count = 0, percentage = 0;
		ImagePlus temp;
		String path, newPath;
		for (DimensionMap map : table.getDimensionMaps())
		{
			time = map.get("T");
			zIndex = Integer.parseInt(map.get("Z")) - 1; // z starts at 1
			tIndex = times.indexOf(time);
			if(zIndex < fileLists.get(tIndex).size())
			{
				path = fileLists.get(tIndex).get(zIndex).getPath();
				newPath = JEXWriter.saveFile(new File(path));
				imageMap.put(map, newPath);
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / (double) totalFiles));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}
		
		// Create the calibration image stack
		Dim ZCal = new Dim("Z", calibrationImages.size());
		
		// Make the dim table for the calibration images
		DimTable calTable = new DimTable();
		calTable.add(ZCal);
		
		// Put the calibration images in a map
		TreeMap<DimensionMap,String> calMap = new TreeMap<DimensionMap,String>();
		maps = calTable.getDimensionMaps();
		int fileCounter = 0;
		for (DimensionMap map : maps)
		{
			path = calibrationImages.get(fileCounter).getPath();
			temp = new ImagePlus(path);
			newPath = JEXWriter.saveImage(temp);
			calMap.put(map, newPath);
			fileCounter = fileCounter + 1;
			percentage = (int) (100 * ((double) (count + fileCounter) / (double) totalFiles));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), imageMap);
		output1.setDimTable(table); // The last dim table will be the largest
		JEXData output2 = ImageWriter.makeImageStackFromPaths(outputNames[1].getName(), calMap);
		output2.setDimTable(calTable);
		JEXData output3 = FileWriter.makeFileObject(outputNames[2].getName(), null, logFile.getPath());
		JEXStatics.statusBar.setProgressPercentage(100);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		realOutputs.add(output3);
		
		// Return status
		return true;
	}
	
	private List<List<File>> getSeparatedFileLists(List<File> imageFiles)
	{
		String prefix, filename;
		List<String> filePrefixes = new Vector<String>();
		for (File f : imageFiles)
		{
			filename = FileUtility.getFileNameWithoutExtension(f.getPath());
			prefix = filename;
			while (Character.isDigit(prefix.charAt(prefix.length() - 1)))
			{
				prefix = prefix.substring(0, prefix.length() - 1);
			}
			if(!filePrefixes.contains(prefix))
			{
				filePrefixes.add(prefix);
			}
		}
		List<List<File>> fileLists = new Vector<List<File>>();
		for (String pre : filePrefixes)
		{
			List<File> matchingFiles = getSortedFilesWithPrefix(pre, imageFiles);
			if(matchingFiles.size() == 0) // Something went wrong
			{
				Logs.log("Couldn't find matching prefix", 0, this);
			}
			else
			{
				fileLists.add(matchingFiles);
			}
		}
		return fileLists;
	}
	
	private static List<File> getSortedFilesWithPrefix(String prefix, List<File> imageFiles)
	{
		List<File> ret = new Vector<File>();
		int prefixLength = prefix.length();
		for (File f : imageFiles)
		{
			String filename = f.getName();
			if(filename.length() >= prefixLength)
			{
				if(filename.substring(0, prefixLength).equals(prefix))
				{
					ret.add(f);
				}
			}
		}
		return ret;
	}
	
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
}

// class AdjustImageHelperFunction implements GraphicalCrunchingEnabling,
// ImagePanelInteractor{
// ImagePanel imagepanel ;
// GraphicalFunctionWrap wrap ;
// DimensionMap[] dimensions ;
// String[] images ;
// ImagePlus im ;
// FloatProcessor imp ;
// int index = 0 ;
// int atStep = 0 ;
//
// boolean auto = false;
// double oldMin = 0;
// double oldMax = 1000;
// double newMin = 0;
// double newMax = 100;
// double gamma = 0.5;
// int depth = 16;
//
// ParameterSet params;
// JEXData imset;
// JEXEntry entry;
// String[] outputNames;
// public JEXData output;
//
// AdjustImageHelperFunction(JEXData imset, JEXEntry entry, String[]
// outputNames, ParameterSet parameters){
// // Pass the variables
// this.imset = imset;
// this.params = parameters;
// this.entry = entry;
// this.outputNames = outputNames;
//
// ////// Get params
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// TreeMap<DimensionMap,JEXDataSingle> map = imset.getDataMap();
// int length = map.size();
// images = new String[length];
// dimensions = new DimensionMap[length];
// int i = 0;
// for (DimensionMap dim: map.keySet()){
// JEXDataSingle ds = map.get(dim);
// String path = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
// ds.get(JEXDataSingle.FILENAME);
// dimensions[i] = dim;
// images[i] = path;
// i ++;
// }
//
// // Prepare the graphics
// imagepanel = new ImagePanel(this,"Adjust image");
// displayImage(index);
// wrap = new GraphicalFunctionWrap(this,params);
// wrap.addStep(0, "Select roi", new String[]
// {"Automatic","Old Min","Old Max","New Min","New Max","Output Bit Depth"});
// wrap.setInCentralPanel(imagepanel);
// wrap.setDisplayLoopPanel(true);
// }
//
// private void displayImage(int index){
// ImagePlus im = new ImagePlus(images[index]);
// imagepanel.setImage(im);
// }
//
// /**
// * Run the function and open the graphical interface
// * @return the ROI data
// */
// public JEXData doit(){
// if (!auto){
// wrap.start();
// }
// else {
// finishIT();
// }
//
// return output;
// }
//
// public void runStep(int index) {
// // Get the new parameters
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// // prepare the images for calculation
// ImagePlus im = new ImagePlus(images[index]);
// imagepanel.setImage(im);
// imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a
// float processor
//
// adjustImage();
//
// imagepanel.setImage(new ImagePlus("",imp));
// }
// public void runNext(){
// atStep = atStep+1;
// if (atStep > 0) atStep = 0;
// }
// public void runPrevious(){
// atStep = atStep-1;
// if (atStep < 0) atStep = 0;
// }
// public int getStep(){ return atStep;}
//
// public void loopNext(){
// index = index + 1;
//
// if (index >= images.length-1) index = images.length-1;
// if (index < 0) index = 0;
//
// runStep(index);
// }
// public void loopPrevious(){
// index = index - 1;
//
// if (index >= images.length-1) index = images.length-1;
// if (index < 0) index = 0;
//
// runStep(index);
// }
// public void recalculate(){}
//
// public void startIT() {
// wrap.displayUntilStep();
// }
// /**
// * Apply the roi to all other images
// */
// public void finishIT() {
// auto = Boolean.parseBoolean(params.getValueOfParameter("Automatic"));
// oldMin = Double.parseDouble(params.getValueOfParameter("Old Min"));
// oldMax = Double.parseDouble(params.getValueOfParameter("Old Max"));
// newMin = Double.parseDouble(params.getValueOfParameter("New Min"));
// newMax = Double.parseDouble(params.getValueOfParameter("New Max"));
// gamma = Double.parseDouble(params.getValueOfParameter("Gamma"));
// depth = Integer.parseInt(params.getValueOfParameter("Output Bit Depth"));
//
// output = new DefaultJEXData(JEXData.IMAGE,outputNames[0],"Adjusted image");
//
// // Run the function
// TreeMap<DimensionMap,JEXDataSingle> map = imset.getDataMap();
// int count = 0;
// int total = map.size();
// JEXStatics.statusBar.setProgressPercentage(0);
// for (DimensionMap dim: map.keySet()){
// JEXDataSingle ds = map.get(dim);
// String imagePath = ds.get(JEXDataSingle.FOLDERNAME) + File.separator +
// ds.get(JEXDataSingle.FILENAME);
// File imageFile = new File(imagePath);
// String imageName = imageFile.getName();
//
// // get the image
// im = new ImagePlus(imagePath);
// imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a
// float processor
//
// ////// Begin Actual Function
// adjustImage();
// ////// End Actual Function
//
// ////// Save the results
// String localDir = JEXStatics.jexManager.getLocalFolder(entry);
// String newFileName = FunctionUtility.getNextName(localDir, imageName, "A");
// String newImagePath = localDir + File.separator + newFileName;
// FunctionUtility.imSave(imp, "false", depth, newImagePath);
//
// JEXDataSingle outputds = new DefaultJEXDataSingle();
// outputds.put(JEXDataSingle.FOLDERNAME, localDir);
// outputds.put(JEXDataSingle.FILENAME, newFileName);
// output.addData(dim,outputds);
// Logs.log("Finished processing " + count + " of " + total +
// ".",1,this);
// count++;
//
// // Status bar
// int percentage = (int) (100 * ((double) count/ (double)map.size()));
// JEXStatics.statusBar.setProgressPercentage(percentage);
// }
//
//
// }
//
// private void adjustImage(){
// FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
// }
//
//
// public void clickedPoint(Point p) {}
// public void pressedPoint(Point p) {}
// public void mouseMoved(Point p){}
//
// }

