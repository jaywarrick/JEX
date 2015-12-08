package function.plugin.plugins.adhesion;

import java.io.File;

import jex.statics.JEXStatics;

import org.scijava.plugin.Plugin;

import rtools.R;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.FileWriter;
import Database.SingleUserDatabase.JEXDataIO;
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
		name="1. Track Oscillatory Motion (R)",
		menuPath="Adhesion",
		visible=true,
		description="Track oscillatory motion of cell locations stored in a MaximaList R object."
		)
public class TrackOscillatoryMotion_R extends JEXPlugin {

	public TrackOscillatoryMotion_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Maxima", type=MarkerConstants.TYPE_ROI, description="Maxima to apply tracking to.", optional=false)
	JEXData roiData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Time Dim Name", description="Name of the 'Time' dimension.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Time")
	String timeDimName;

	@ParameterMarker(uiOrder=2, name="t=0 Frame", description="First frame of oscillatory motion that should be treated as time=0 (i.e., t=0).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int t0_Frame;

	@ParameterMarker(uiOrder=3, name="Last Frame", description="Last frame of oscillatory motion to track backwards from (towards the first frame). Specify integer frame or '-1' for last possible frame.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="-1")
	int lastFrame;

	@ParameterMarker(uiOrder=4, name="Time Per Frame", description="Time between frames of the timelapse. [seconds]", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.035")
	double timePerFrame;

	@ParameterMarker(uiOrder=5, name="Max Distance", description="Maximum distance to consider for linking two points in subsequent frames.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="150.0")
	double maxDist;

	@ParameterMarker(uiOrder=6, name="Primary direction (x,y,z)", description="Primary direction for tracking in terms of vector direction components (typically 1,0,0 for x direction, sign doesn't matter).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1,0,0")
	double direction;

	@ParameterMarker(uiOrder=7, name="Directionality", description="How much to weight the primary direction in terms of linking points. A value of 10 penalizes 'movement' perpendicular to the primary direction by a multiple of 10.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10")
	double directionality;

	@ParameterMarker(uiOrder=8, name="Uniformity distance threshold", description="Should all points move 'as one' together? If so, set this to a value >= 0. If a link is made that is longer than this distance (pixels), it must move in the average direction of the population or the link is removed as a possibility.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	double uniformityDistThresh;

	@ParameterMarker(uiOrder=9, name="Precision", description="Number of digits to keep after decimal place during calculations of distances.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int digits;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="R TrackList", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant R TrackList", enabled=true)
	JEXData maximaListOutput;

	@OutputMarker(uiOrder=2, name="R MaximaList", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant R MaximaList", enabled=true)
	JEXData trackListOutput;
	
	@OutputMarker(uiOrder=3, name="Tracked Maxima ROI", type=MarkerConstants.TYPE_ROI, flavor="", description="The resultant JEX ROI Maxima with tracked id's.", enabled=true)
	JEXData roiOutput;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		int total = 10, count = 0, percentage = 0;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Validate the input data
		if(roiData == null || !roiData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}

		R.eval("rm(list=ls())"); // Start with a clean slate and also ensure that the Rsession is running by calling eval at least once first before sourcing files.
		AdhesionUtility.loadAdhesionScripts(); // source the adhesion files

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Load the ROI
		String path = JEXWriter.getDatabaseFolder() + File.separator + roiData.getDetachedRelativePath();
		R.eval("maximaList <- new('MaximaList')");
		R.eval("maximaList$initializeWithJEXROIFile(path=" + R.quotedPath(path) + ", timeDimName=" + R.sQuote(timeDimName) + ")");

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Track the points
		if(lastFrame < 0)
		{
			R.eval("startFrame <- max(as.numeric(names(maximaList$maxima)))");
		}
		else
		{
			R.eval("startFrame <- " + lastFrame + ")");
		}
		R.eval("maximaList$trackBack(startFrame=startFrame, endFrame=" + t0_Frame + ", maxDist=" + maxDist + ", direction=c(" + direction + "), directionality=" + directionality + ", uniformityDistThresh=" + uniformityDistThresh + ", digits=" + digits + ")");

		count = count + 4;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Save the tracked maximaList to file
		String maximaListPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("RData");
		R.eval("save(list=c('maximaList'), file=" + R.pathString(maximaListPath) + ")");
		maximaListOutput = FileWriter.makeFileObject("temp", null, maximaListPath);
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		
		// Save the JEX ROI to file
		String roiPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("jxd");
		R.eval("maximaList$saveROI(file=" + R.pathString(roiPath) + ")");
		JEXData data = new JEXData(JEXData.ROI, "temp");
		JEXDataIO.loadJXD(data, roiPath);
		roiOutput = data;

		// Get the TrackList and save it
		R.eval("maximaList$getTrackList(t0_Frame=" + t0_Frame + ", timePerFrame=" + timePerFrame + ")");
		String trackListPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("RData");
		R.eval("save(list=c('trackList'), file=" + R.pathString(trackListPath) + ")");
		trackListOutput = FileWriter.makeFileObject("temp", null, trackListPath);

		count = count + 3;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Return status
		return true;
	}
}
