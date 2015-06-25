package function.plugin.plugins.Export;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;

import org.apache.commons.io.FileUtils;
import org.scijava.plugin.Plugin;

import rtools.ScriptRepository;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;

/* HELPFUL CLASSES:
 * ImageReader
 * ImageWriter
 * ScriptRepository.runSysCommand(String[] cmds)
 */

/*
 * SPECIFICATIONS:
 * Export image object to user specified folder
 * Generate a list of images from that image object to use for --file-list
 * Figure out where CellProfiler.exe is on system
 * String together a command to send to cmd
 * User specifies images path (images exported from JEX)
 * User specified output path (tables and any images exported from CP)
 * Reroute pipeline to JEX to import those newly generated tables and images
 */

@Plugin(
		type = JEXPlugin.class,
		name="CellProfiler",
		menuPath="CellProfiler",
		visible=true,
		description="Process images with CellProfiler"
		)
/**
 * A function that takes a JEX image object and runs it through a CellProfiler 
 * pipeline. Requires CellProfiler 2.1.2 and later because --file-list 
 * command-line switch is needed (https://github.com/CellProfiler/CellProfiler/
 * wiki/Adapting-CellProfiler-to-a-LIMS-environment)
 * 
 * @author Tom Huibregtse, June 2015
 *
 */
public class RunCellProfiler extends JEXPlugin {

	public RunCellProfiler() {}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=0, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image object to send to CellProfiler.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="CellProfiler Executable", description="The CellProfiler.exe file", ui=MarkerConstants.UI_FILECHOOSER, defaultText="C:\\Program Files\\CellProfiler\\CellProfiler.exe")
	String CPExecPath;

	@ParameterMarker(uiOrder=1, name="Pipeline", description="CellProfiler Pipeline to be used", ui=MarkerConstants.UI_FILECHOOSER, defaultText="C:\\Users\\Tom\\Desktop\\ExampleHuman\\ExampleHuman.cppipe")
	String pipelinePath;
	
	@ParameterMarker(uiOrder=2, name="File List Directory", description="Directory in which to save the file list", ui=MarkerConstants.UI_FILECHOOSER, defaultText="C:\\Users\\Tom\\Desktop\\RunCellProfiler")
	String fileListPath;

	@ParameterMarker(uiOrder=3, name="Image Directory", description="Directory in which to export the image object", ui=MarkerConstants.UI_FILECHOOSER, defaultText="C:\\Users\\Tom\\Desktop\\RunCellProfiler\\Image Directory")
	String imageDirectory;

	@ParameterMarker(uiOrder=4, name="Output Directory", description="Directory in which to save data exported from CellProfiler", ui=MarkerConstants.UI_FILECHOOSER, defaultText="C:\\Users\\Tom\\Desktop\\RunCellProfiler\\Output Directory")
	String outputDirectory;

	/////////// Define Outputs ///////////

	// No Outputs

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {
		
		JEXStatics.statusBar.setProgressPercentage(0);

		// validate the input data
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// TODO export image object to user specified folder
//		ImageReader.readObjectToImagePath(imageData);
//		File imageOut = new File(imageDirectory);

		// TODO create the file list for --file-list command line switch
		String[] imageList = ImageReader.readObjectToImagePathStack(imageData);

		// Cancel the function as soon as possible if the user has hit the cancel button
		// Perform this inside loops to check as often as possible.
		if(this.isCanceled())
		{
			return false;
		}

		File imageFileList = new File(fileListPath + File.separator + "FileList.lsp");
		PrintWriter fileListWriter = null;
		try
		{
			fileListWriter = new PrintWriter(imageFileList);

			for (String imagePath: imageList) {
				fileListWriter.println(imagePath);
			}
		}
		catch (IOException e)
		{
			Logs.log("Couldn't copy the files to list.", this);
			e.printStackTrace();
		} finally {
			fileListWriter.close();
		}

		JEXStatics.statusBar.setProgressPercentage(25);

		// TODO figure out where CellProfiler.exe is on system
		// default is C:\Program Files\CellProfiler\CellProfiler.exe

		// TODO string together a command and send to cmd
		String [] cmds = {CPExecPath,"-c","-r","--file-list",
				imageFileList.getAbsolutePath(),"-o",outputDirectory,"-p",
				pipelinePath};

		ScriptRepository.runSysCommand(cmds);


		JEXStatics.statusBar.setProgressPercentage(50);
		// TODO reroute pipeline back to JEX to import those newly generated tables and images

		// Cancel the function as soon as possible if the user has hit the cancel button
		// Perform this inside loops to check as often as possible.
		if(this.isCanceled())
		{
			return false;
		}

		return true;
	}

}
