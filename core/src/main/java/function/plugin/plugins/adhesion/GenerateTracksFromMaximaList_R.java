package function.plugin.plugins.adhesion;

import java.io.File;

import jex.statics.JEXStatics;

import org.scijava.plugin.Plugin;

import rtools.R;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.FileWriter;
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
 */

@Plugin(
		type = JEXPlugin.class,
		name="Generate Tracks from MaximaList (R)",
		menuPath="Adhesion",
		visible=true,
		description="Filter tracks based upon length, startframe, and end frame. (AND used between conditions)"
		)
public class GenerateTracksFromMaximaList_R extends JEXPlugin {

	public GenerateTracksFromMaximaList_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="R MaximaList", type=MarkerConstants.TYPE_FILE, description=".RData file of the TrackList to be filtered.", optional=false)
	JEXData roiFileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=2, name="t=0 Frame", description="First frame of oscillatory motion that should be treated as time=0 (i.e., t=0).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int t0_Frame;

	@ParameterMarker(uiOrder=4, name="Time Per Frame", description="Time between frames of the timelapse. [seconds]", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.035")
	double timePerFrame;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="R TrackList", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant .RData file of the filtered R TrackList,", enabled=true)
	JEXData trackListOutput;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(!JEXPlugin.isInputValid(roiFileData, JEXData.FILE))
		{
			return false;
		}

		int count = 0, total = 5, percentage = 0;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		R.eval("rm(list=ls())"); // Start with a clean slate and start R session if necessary
		AdhesionUtility.loadAdhesionScripts(); // Load necessary R scripts

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		// Load the R TrackList
		String path = FileReader.readFileObject(roiFileData);
		R.eval("load(file=" + R.quotedPath(path) + ")");
		R.eval("maximaList <- maximaList$copy()");
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		// Get the TrackList and save it
		R.eval("trackList <- maximaList$getTrackList(t0_Frame=" + t0_Frame + ", timePerFrame=" + timePerFrame + ")");
		String trackListPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("RData");
		R.eval("save(list=c('trackList'), file=" + R.quotedPath(trackListPath) + ")");
		trackListOutput = FileWriter.makeFileObject("temp", null, trackListPath);

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Return status
		return true;
	}
}
