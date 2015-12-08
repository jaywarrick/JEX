package function.plugin.plugins.adhesion;

import java.io.File;

import jex.statics.JEXStatics;

import org.scijava.plugin.Plugin;

import rtools.R;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
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
		name="2. Filter Tracks (R)",
		menuPath="Adhesion",
		visible=true,
		description="Filter tracks based upon length, startframe, and end frame. (AND used between conditions)"
		)
public class FilterTrackList_R extends JEXPlugin {

	public FilterTrackList_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="R TrackList", type=MarkerConstants.TYPE_FILE, description=".RData file of the TrackList to be filtered.", optional=false)
	JEXData roiFileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Length Min", description="The minimum length (in frames, inclusive) of a track that is to be kept.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	int lengthMin;

	@ParameterMarker(uiOrder=2, name="Length Max", description="The maximum length (in frames, inclusive) of a track that is to be kept.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4095.0")
	int lengthMax;

	@ParameterMarker(uiOrder=3, name="Start Frame Min", description="The maximum starting frame index of a track that is to be kept (i.e., keep if track ends at or after this frame).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	int startMin;
	
	@ParameterMarker(uiOrder=4, name="Start Frame Max", description="The maximum starting frame index of a track that is to be kept (i.e., keep if track ends at or after this frame).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	int startMax;

	@ParameterMarker(uiOrder=5, name="End Frame Min", description="The minimum ending frame index of a track that is to be kept (i.e., keep if track ends at or after this frame).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="65535.0")
	int endMin;
	
	@ParameterMarker(uiOrder=6, name="End Frame Max", description="The minimum ending frame index of a track that is to be kept (i.e., keep if track ends at or after this frame).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="65535.0")
	int endMax;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="R TrackList (filtered)", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant .RData file of the filtered R TrackList,", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(roiFileData == null || !roiFileData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}

		int count = 0, total = 4, percentage = 0;
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
		
		// Get rid of short tracks (i.e., cells that are too hard to follow for long periods of time like ones that go on and off screen)
		R.eval("trackList$filterTracks(fun = trackLengthFilter, min=" + lengthMin + ", max=" + lengthMax + ")");
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		// Get rid of tracks based on when they start or stop
		R.eval("trackList$filterTracks(fun = trackFrameFilter, startMin=" + startMin + ", startMax=" + startMax + ", endMin=" + endMin + ", endMax=" + endMax + ")");
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}
		
		// Save
		String trackListPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("RData");
		R.eval("save(list=c('trackList'), file=" + R.pathString(trackListPath) + ")");
		output = FileWriter.makeFileObject("temp", null, trackListPath);
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Return status
		return true;
	}
}
