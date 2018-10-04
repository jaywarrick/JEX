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
		name="3. Apply Oscillatory Track Metadata (R)",
		menuPath="Adhesion",
		visible=true,
		description="Apply oscillatory frequency and frequency sweep to TrackList data."
		)
public class ApplyOscillatoryTrackMetadata_R extends JEXPlugin {

	public ApplyOscillatoryTrackMetadata_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="R TrackList", type=MarkerConstants.TYPE_FILE, description=".RData file of the TrackList to be filtered.", optional=false)
	JEXData roiFileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Sinusoidal or Square Wave", description="Is the oscillatory signal base upon a sinusoid (vs. square wave)? (Usually false)", ui=MarkerConstants.UI_DROPDOWN, choices={"Sinusoidal","Square"}, defaultChoice=1)
	String sin;

	@ParameterMarker(uiOrder=2, name="Initial Frequency [Hz]", description="Initial frequency of the frequency sweep in [Hz].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double fi;

	@ParameterMarker(uiOrder=3, name="Final Frequency [Hz]", description="Final frequency of the frequency sweep in [Hz].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.01")
	double ff;

	@ParameterMarker(uiOrder=4, name="Sweep Duration [s]", description="The duration of the frequency sweep, which is typically shorter than the duration of the timelapse.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="300")
	double duration;

	@ParameterMarker(uiOrder=5, name="Number of computer cores (Must be 1 for Windows)", description="Number of computer cores to process the data with. (Most computers have at least 2 and commonly 4, virtual cores count)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int cores;
	
	@ParameterMarker(uiOrder=6, name="Vel. Smoothing: Preferred Distance", description="The minimum preferred distance (in pixels) a cell should move before quantifying average velocity during a sequence of frames (i.e., want appreciable movement for less noisy velocity calcs)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10")
	double minDistance;
	
	@ParameterMarker(uiOrder=7, name="Vel. Smoothing: Max Frames", description="The maximum number of frames to average over (i.e., we don't want to lose temporal resolution)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="15")
	int maxFrames;
	
	@ParameterMarker(uiOrder=8, name="Fraction of Wave Valid", description="What fraction of the time between zero crossings should be used to quantify velocity and adhesion (e.g., center 60% -> enter a fraction of 0.6)? We don't want to determine if cells are adhered while cells are switching directions. ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.6")
	double validFraction;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="R TrackList (oscillatory)", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant .RData file of the oscillatory R TrackList.", enabled=true)
	JEXData output;
	
	@OutputMarker(uiOrder=2, name="BulkFitResults", type=MarkerConstants.TYPE_FILE, flavor="", description="The results of finding the phase shift of the fitted curve.", enabled=true)
	JEXData outputBulkFitResults;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(roiFileData == null || !roiFileData.getTypeName().getType().matches(JEXData.FILE))
		{
			return false;
		}

		int count = 0, total = 7, percentage = 0;
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
		R.eval("trackList <- trackList$copy()");

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}
		
		if(sin.equals("Sinusoidal"))
		{
			sin = "TRUE";
		}
		else
		{
			sin="FALSE";
		}
		// Have to do these temp variables because it didn't work 'inline'. Probably a concurrent modification thing or a reference thing.
		R.eval("t0_Frame <- trackList$meta$t0_Frame");
		R.eval("timePerFrame <- trackList$meta$timePerFrame");
		R.eval("trackList$setOscillatoryMeta(sin=" + sin + ", fi=" + fi + ", ff=" + ff + ", sweepDuration=" + duration + ", t0_Frame=t0_Frame, timePerFrame=timePerFrame)");

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}		
		R.eval("bestFit <- getBulkPhaseShiftGS(trackList, cores=" + cores + ")");
		R.eval("trackList$meta$bestFit <- bestFit");
		String tablePath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("csv");
		R.eval("write.csv(as.data.frame(as.list(bestFit$par)), file=" + R.quotedPath(tablePath) + ", row.names=FALSE)");
		outputBulkFitResults = FileWriter.makeFileObject("temp", null, tablePath);
				
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		// Smooth the velocities (i.e., average over multiple frames) to get a more accurate measure, especially for slow moving cells
		// We need the bestFit information to estimate the speed of cells over time
		// 'dist' refers to the distance in pixels that we want to see the cell travel before quantifying its velocity
		// 'maxWidth' refers to the maximum number of frames to average together to estimate velocity (i.e., it's great to try and use more frames but we don't want to use too many)
		R.eval("trackList$smoothVelocities(fit=bestFit, dist=" + minDistance + ", maxWidth=" + maxFrames + ")");

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}
		
		// Given the flow switches direction and attached cells can 'wobble', we only want to gage whether as cell is adhered
		// after it is done wobbling. To do this we set which frames are 'valid' for determining whether the cell is adhered or not.
		// We need the 'bestFit' to estimate where the changes in flow direction are.
		// We then want to wait before quantifying because the cell could be wobbling so 'validStart' dictates when
		// after the change in flow direction we can see if the cell is adhered or not. 'validStart' is expressed as a
		// fraction of the interval between changed in flow direction (i.e., validStart=0.1 says to wait 10% of the time between direction changes to mark time frames as valid)
		// 'validEnd' is also a fraction of the same interval. Frames between 'validStart' and 'validEnd' within each interval bounded by changes in flow direction
		// are recorded as being valid.
		double validStart = (1-validFraction)/2;
		double validEnd = (1 - validStart);
		R.eval("trackList$calculateValidFrames(fit=bestFit, validStart=" + validStart + ", validEnd=" + validEnd + ")");
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		// Save
		String trackListPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("RData");
		R.eval("save(list=c('trackList'), file=" + R.quotedPath(trackListPath) + ")");
		output = FileWriter.makeFileObject("temp", null, trackListPath);

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Return status
		return true;
	}
}
