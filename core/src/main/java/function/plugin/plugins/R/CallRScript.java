package function.plugin.plugins.R;

import java.io.File;
import java.util.TreeMap;

import logs.Logs;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.scijava.plugin.Plugin;

import rtools.R;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

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

@Plugin(
		type = JEXPlugin.class,
		name="Call R Script",
		menuPath="R",
		visible=true,
		description="Pass JEXData's to an R script with each JEXData as a data.frame table.\nUse 'jexTempRFolder' and 'jexDBFolder' variable for directory in which to create files and read database files from respectively."
		)
public class CallRScript extends JEXPlugin {

	public CallRScript()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(name="data1", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data1 for use in the provided R command.", optional=true)
	JEXData data1;
	
	@InputMarker(name="data2", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data2 for use in the provided R command.", optional=true)
	JEXData data2;
	
	@InputMarker(name="data3", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data3 for use in the provided R command.", optional=true)
	JEXData data3;
	
	@InputMarker(name="data4", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data4 for use in the provided R command.", optional=true)
	JEXData data4;
	
	@InputMarker(name="data5", type=MarkerConstants.TYPE_ANY, description="JEXData to be passed as a data.frame and called data5 for use in the provided R command.", optional=true)
	JEXData data5;
	
	/////////// Define Parameters ///////////
	
	public final String scriptDefaultValue = "### Some pointers... ###\n\n" + 
        
		"# jexTempRFolder is a temp folder in which you can save files freely using your\n" +
        "# own naming convetions (e.g.,\n" +
        "# copy.file(from='/Users/myName/myFolder/myFile.pdf',\n" +
        "# 	to=paste(jexTempRFolder, '/myFile.pdf'))\n\n" +
        
        "# Use single quotes for all strings in these scripts to avoid parsing issues\n\n" +
        
        "# jexDBFolder is the base folder from with all paths in JEXData objects are relative\n" +
        "# to. Thus, if you read a file path from a data object such as\n" +
        "# '/Cell_x0_y1/File-Results.txt' from within 'data1', the actual\n" +
        "# full file path is '<jexDBFolder>/Cell_x0_y1/File-Results.txt'\n\n" +
        
        "# data1... data5 are special variable names that can be used to pass data from\n" +
        "# JEX to R. These objects are the same as the .jxd objects within the database\n" +
        "# and typically refer to files or contain data such as point information within\n" +
        "# an ROI. These objects are given to the user as data.frames using the library\n" +
        "# 'foreign' using the 'read.arff' function. If one of these variables such as 'data1',\n" +
        "# contains a list of files, you can access them by calling data1$Value[i] where i\n" +
        "# is you index of interest.\n\n" +
        
        "# You can also save data back to JEX by providing a list of file names using\n" +
        "# the reserved variable names fileList1, fileList2, imageList1, and imageList2.\n" +
        "# The file lists are for any type of file while the image lists are interpreted as \n" +
        "# image files. For example, 'imageList1 <- c(filepath1, filepath2)' will result\n" +
        "# in an image object in JEX that has 1 dimension named 'i' for index with\n" +
        "# filepath1 at i=0 and filepath2 at i=1. These images can then be viewed in\n" +
        "# JEX's built in image viewer.\n\n" +
        
        "# It is also good to make sure that all attempts to plot are 'shut off' before trying \n" +
        "# to do any plotting. Plotting commands can get stranded if the script errors\n" +
        "# before calling 'dev.off()' to close the plot file (e.g. pdf, jpeg, tif) before ending.\n" +
        "# This is accomplished with a single call to graphics.off()\n\n" +
        
        "# library(foreign) is required for passing in the JEX data objects and is part of the\n" +
        "# normal R distribution (at least with RStudio).\n\n" +
        
        "# Happy scripting...\n\n" +
        
        "graphics.off()\n\n" +
        
        "x <- (0:1000)/100\n" +
        "y <- cos(x)\n" +
        "temp1 <- paste(jexTempRFolder, '/RPlot.jpeg', sep='')\n" +
        "jpeg(file=temp1)\n" +
        "plot(x,y)\n" +
        "dev.off()\n\n" +
        
        "x <- (0:1000)/100\n" +
        "y <- sin(x)\n" +
        "temp2 <- paste(jexTempRFolder, '/RPlot2.jpeg', sep='')\n" +
        "jpeg(file=temp2)\n" +
        "plot(x,y)\n" +
        "dev.off()\n\n" +
        
        "imageList1 <- c(temp1, temp2)\n";
	
	@ParameterMarker(uiOrder=1, name="Script", description="Script of commands to run in R environment using data1... data5, jexTempRFolder, and jexDBFolder as potential inputs and fileList1, fileList2, imageList1, and imageList2 as specially interpreted objects that can be translated back into JEXData outputs.", ui=MarkerConstants.UI_SCRIPT, defaultText=scriptDefaultValue)
	String script;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(name="fileList1", type=MarkerConstants.TYPE_FILE, flavor="", description="File object output populated by collecting the variable named 'fileList1' from the R workspace after the script.", enabled=true)
	JEXData fileList1;
	
	@OutputMarker(name="fileList2", type=MarkerConstants.TYPE_FILE, flavor="", description="File object output populated by collecting the variable named 'fileList2' from the R workspace after the script.", enabled=true)
	JEXData fileList2;
	
	@OutputMarker(name="imageList1", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Image object output populated by collecting the variable named 'imageList1' from the R workspace after the script.", enabled=true)
	JEXData imageList1;
	
	@OutputMarker(name="imageList2", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Image object output populated by collecting the variable named 'imageList2' from the R workspace after the script.", enabled=true)
	JEXData imageList2;
	
	@Override
	public int getMaxThreads()
	{
		return 1; // R doesn't like multiple threads
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		initializeWorkspace();
		initializeData(data1, "data1");
		initializeData(data2, "data2");
		initializeData(data3, "data3");
		initializeData(data4, "data4");
		initializeData(data5, "data5");
		
		R.eval(script);
		
		fileList1 = getOutput("fileList1");
		fileList2 = getOutput("fileList2");
		imageList1 = getOutput("imageList1");
		imageList2 = getOutput("imageList2");
		
		// Return status
		return true;
	}
	
	public static void initializeWorkspace()
	{
		R.eval("temp <- 0"); // Dummy command to get the R connection up an running.
		R.endPlot();
		R.eval("rm(list=ls())");
		R.load("foreign");
		String tempPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getTempFolderName() + File.separator + "RScriptTempFolder";
		File tempFolder = new File(tempPath);
		if(!tempFolder.exists())
		{
			tempFolder.mkdirs();
		}
		R.eval("jexTempRFolder <- " + R.quotedPath(tempPath));
		
		String dbPath = JEXWriter.getDatabaseFolder();
		R.eval("jexDBFolder <- " + R.quotedPath(dbPath));
	}
	
	public static void initializeData(JEXData data, String name)
	{
		if(data == null)
		{
			return;
		}
		String path = JEXWriter.getDatabaseFolder() + File.separator + data.getDetachedRelativePath();
		R.eval(name + " <- read.arff(" + R.quotedPath(path) + ")");
	}
	
	public static JEXData getOutput(String name)
	{
		TreeMap<DimensionMap,String> files = new TreeMap<DimensionMap,String>();
		
		boolean image = false;
		if(name.substring(0, 1).equals("i"))
		{
			image = true;
		}
		
		REXP workspaceVariables = R.eval("ls()");
		boolean found = false;
		try
		{
			String[] vars = workspaceVariables.asStrings();
			for(String var : vars)
			{
				if(var.equals(name))
				{
					found = true;
					break;
				}
			}
			if(!found)
			{
				return null;
			}
		}
		catch (REXPMismatchException e1)
		{
			Logs.log("Couldn't get workspace variables as list of names.", CallRScript.class);
			e1.printStackTrace();
			return null;
		}
		REXP fileObject = R.eval(name);
		String[] fileStrings = null;
		try
		{
			fileStrings = fileObject.asStrings();
			int i = 0;
			for(String s : fileStrings)
			{
				String fixedString = s.replaceAll("/", File.separator); // Might have to figure out Pattern.quote(File.separator) stuff for windows.
				files.put(new DimensionMap("i=" + i), fixedString);
				i++;
			}
			JEXData ret = null;
			if(image)
			{
				ret = ImageWriter.makeImageStackFromPaths("dummy", files);
			}
			else
			{
				ret = FileWriter.makeFileObject("dummy", null, files);
			}
			
			return ret;
		}
		catch (REXPMismatchException e)
		{
			Logs.log("Couldn't convert " + name + " to String[]", CallRScript.class);
			e.printStackTrace();
		}
		return null;
	}
}
