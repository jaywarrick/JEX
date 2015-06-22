package function.plugin.plugins.Export;

import java.io.IOException;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
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
	
	@ParameterMarker(uiOrder=1, name="Pipeline", description="CellProfiler Pipeline to be used", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String pipelinePath;
	
	@ParameterMarker(uiOrder=2, name="Image Directory", description="Directory in which to export image object", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String imageDirectory;
	
	@ParameterMarker(uiOrder=3, name="Output Directory", description="Location to export data from CellProfiler", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
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
		
		// TODO export image object to user specified folder
		
		// TODO generate a list of images from that image object to use for --file-list
		
		// TODO figure out where CellProfiler.exe is on system
		
		// TODO string together a command to send to cmd
		
		// TODO reroute pipeline back to JEX to import those newly generated tables and images
		
		return true;
	}

}
