package function.plugin.plugins.adhesion;

import java.io.File;
import java.util.TreeMap;

import jex.statics.JEXStatics;

import org.scijava.plugin.Plugin;

import rtools.R;
import tables.DimensionMap;
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
		name="4. Generate Plots and Data (R)",
		menuPath="Adhesion",
		visible=true,
		description="Apply oscillatory frequency and frequency sweep to TrackList data."
		)
public class GeneratePlotsAndData_R extends JEXPlugin {

	public GeneratePlotsAndData_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="R TrackList", type=MarkerConstants.TYPE_FILE, description=".RData file of the TrackList to be filtered.", optional=false)
	JEXData roiFileData;

	/////////// Define Parameters ///////////

	// duh <- trackList$getPercentAdhered(velocityThreshold=1)
	// tau <- getShearStress(f=getFrequencies(duh$time)$f, pixelAmplitude = 180, mu=0.0052)
	// 
	// plot(tau, duh$percentAdhered/100, log='x')
	// bestFit1 <- fitLogNorm(tau, duh$percentAdhered/100, thresh=0.1, SANN=TRUE)
	// lines(tau, do.call(logNorm, c(list(x=tau), bestFit1$par)), lwd=3, col='red')
	// bestFit2 <- fitLogNorm2(tau, duh$percentAdhered/100, guess=guess, alpha=0.1, newMu=0.01, newSigma=1, SANN=FALSE)
	// lines(tau, do.call(logNorm2, c(list(x=tau), bestFit2$par)), lwd=3, col='blue')

	//	getFrequencies <- function(t=seq(0,300,0.035), fi=1, ff=0.01, duration=300)
	//	{
	//	     return(data.frame(t=t, f=fi*(ff/fi)^(t/duration)))
	//	}
	//
	//	getShearStress <- function(f, pixelAmplitude, h=200e-6, mu=0.00078, mag=4)
	//	{
	//	     micronAmplitude <- (6.45e-6/mag)*pixelAmplitude
	//	     uMax <- 4*micronAmplitude*f
	//	     return((2/3)*uMax*(6*mu)/h)
	//	}

	@ParameterMarker(uiOrder=1, name="Velocity Threshold [pixels/s]", description="The velocity below which a cell is considered adhered (typical values are around 2-5)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="3.0")
	double velocityThreshold;
	
	@ParameterMarker(uiOrder=1, name="Alpha Guess", description="The fraction of the total population that represents the lower adhesion strength population", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.9")
	double alphaGuess;
	
	@ParameterMarker(uiOrder=1, name="mu2 Guess", description="The mean frequency near which the stronger adhesion population is expected to adhere. Leave as 'NULL' (all caps) to automatically generate a reasonable guess of 1 sigma above whatever is determined for the single population.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="NULL")
	double mu2Guess;
	
	@ParameterMarker(uiOrder=1, name="sigma2 Guess", description="The log normal sigma of the population (i.e., it is log(sigma) of a standard distribution). Leave as -1 to automatically guess the same sigma as found for the lower strength adhesion population.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="-1")
	double sigma2Guess;
	
	@ParameterMarker(uiOrder=1, name="Number of Cores (OSX/Unix only)", description="Number of processor cores to use during grid search of initial guess for the single population model fit. (WINDOWS ONLY ALLOWS 1)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double cores;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Phase Shift Plots", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Plots of the track velocities overlaid with a predicted velocity to illustrate whether we have fit the cell motion well in terms of when the cell switches direction.", enabled=true)
	JEXData outputPhase;

	@OutputMarker(uiOrder=2, name="Percent Adhered vs Frequency Plot", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Plots of the percent adhered vs frequency, and different plots of the fitted curves.", enabled=true)
	JEXData outputAdhesionPlot;

	@OutputMarker(uiOrder=3, name="Percent Adhered vs Frequency Table", type=MarkerConstants.TYPE_FILE, flavor="", description="Table of the percent adhered vs frequency data along with fitted values.", enabled=true)
	JEXData outputAdhesionTable;
	
	@OutputMarker(uiOrder=4, name="R TrackList (analyzed)", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant R TrackList after analyzing for percent adhered (saved as a .RData file).", enabled=true)
	JEXData outputTrackList;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		TreeMap<DimensionMap,String> phasePlots = new TreeMap<DimensionMap,String>();

		// Validate the input data
		if(roiFileData == null || !roiFileData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}

		int count = 0, total = 8, percentage = 0;
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
		R.eval("trackList <- trackList$copy()"); // This is to refresh the class definition of the trackList in case it has been updated.

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		//		fitCurveData <- getSweep(amplitude=bestFit$par[['amplitude']], phaseShift=bestFit$par[['phaseShift']], sweepDuration=(1/bestFit$par[['timeScalingFactor']])*trackList$sweepDuration, offset=0, sin=trackList$sin, ti=bestFit$par[['ti']], fi=trackList$fi, ff=trackList$ff, t=trackList$tAll, guess=NULL)
		//		trackList$plotTrackList(slot='vx', ylim=c(-500,500), rel=FALSE, validOnly=FALSE, xlim=c(0,5))
		//		lines(fitCurveData$t, fitCurveData$v, col='red')
		//		trackList$plotTrackList(slot='vx', rel=FALSE, ylim=c(-50,50), validOnly=FALSE, xlim=c(125,300))
		//		lines(fitCurveData$t, fitCurveData$v, col='red')
		//		trackList$plotTrackList(slot='vx', rel=FALSE, ylim=c(-500,500), validOnly=FALSE, xlim=c(0,300))
		//		lines(fitCurveData$t, fitCurveData$v, col='red')

		R.eval("bestFit <- trackList$meta$bestFit");
		R.eval("fitCurveData <- getSweep(amplitude=bestFit$par[['amplitude']], phaseShift=bestFit$par[['phaseShift']], sweepDuration=(1/bestFit$par[['timeScalingFactor']])*trackList$meta$sweepDuration, offset=0, sin=trackList$meta$sin, ti=bestFit$par[['ti']], fi=trackList$meta$fi, ff=trackList$meta$ff, t=trackList$meta$tAll, guess=NULL)");

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		// Plot the trackList velocities and the fitted curve
		String plotPath = R.startPlot("tif", 4, 3, 600, 10, "Arial", "lzw");
		R.eval("trackList$plotTrackList(slot='vx', ylim=c(-500,500), rel=FALSE, validOnly=FALSE, xlim=c(0,300))");
		R.eval("lines(fitCurveData$t, fitCurveData$v, col='red')");
		R.endPlot();
		phasePlots.put(new DimensionMap("i=0"), plotPath);
		plotPath = R.startPlot("tif", 4, 3, 600, 10, "Arial", "lzw");
		R.eval("trackList$plotTrackList(slot='vx', ylim=c(-500,500), rel=FALSE, validOnly=FALSE, xlim=c(0,5))");
		R.eval("lines(fitCurveData$t, fitCurveData$v, col='red')");
		R.endPlot();
		phasePlots.put(new DimensionMap("i=1"), plotPath);
		plotPath = R.startPlot("tif", 4, 3, 600, 10, "Arial", "lzw");
		R.eval("trackList$plotTrackList(slot='vx', ylim=c(-500,500), rel=FALSE, validOnly=FALSE, xlim=c(125,300))");
		R.eval("lines(fitCurveData$t, fitCurveData$v, col='red')");
		R.endPlot();
		phasePlots.put(new DimensionMap("i=2"), plotPath);

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		//		results = trackList$getPercentAdhered(velocityThreshold=5)
		//		//pdf(file=paste0('/Users/jaywarrick/Documents/MMB/Projects/Adhesion/MM AMD/',well,'_PercentAdhered.pdf'), width=6, height=4)
		//		plot(results$time, results$percentAdhered, xlab='Time [s]', ylab='Percent Adhered [%]', pch=20, cex=0.75, ylim=c(0,100))

		// Get the percent adhered
		R.eval("adhesionResults = trackList$getPercentAdhered(velocityThreshold=" + velocityThreshold + ")");
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		// Fit the data
		R.eval("adhesionResults$f <- getFrequencies(adhesionResults$time)$f");
		R.eval("fitResults <- fitLogNormGS(x=adhesionResults$f, y=adhesionResults$percentAdhered/100, alphaGuess=" + alphaGuess + ", mu2Guess=" + mu2Guess + ", sigma2Guess=" + sigma2Guess + ", cores=" + cores + ")");
		R.eval("fit1y <- do.call(logNorm, c(list(x=adhesionResults$f), fitResults$par1))");
		R.eval("fit2y <- do.call(logNorm2, c(list(x=adhesionResults$f), fitResults$par2))");
		R.eval("adhesionResults$fit1y <- fit1y");
		R.eval("adhesionResults$fit2y <- fit2y");
		R.eval("finalResults <- c(fitResults$par1, fitResults$par2, fitResults$r2_single, fitResults$r2_double, fitResults$limitFlag)");
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}
		
		// Plot the data
		plotPath = R.startPlot("tif", 5, 3.5, 600, 10, "Arial", "lzw");
		R.eval("plot(adhesionResults$f, adhesionResults$percentAdhered/100, pch=21, cex=0.5, col=rgb(0,0,0,0.5), log='x', xlab='Frequency [Hz]', ylab='Fraction of Population Adhered', main='')");
		R.eval("lines(x, fit1y, lwd=3, col='red')");
		R.eval("lines(x, fit2y, lwd=3, col='blue')");
		R.endPlot();
		outputAdhesionPlot = FileWriter.makeFileObject("temp", null, plotPath);
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}
		
		// Save the adhesion data as tables
		String tablePath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("csv");
		R.eval("write.csv(adhesionResults, file=" + R.quotedPath(tablePath) + ", row.names=FALSE)");
		outputTrackList = FileWriter.makeFileObject("temp", null, tablePath);
		tablePath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("csv");
		R.eval("write.csv(finalResults, file=" + R.quotedPath(tablePath) + ", row.names=FALSE)");
		outputTrackList = FileWriter.makeFileObject("temp", null, tablePath);

		// Remember to save the updated TrackList object.
		R.eval("trackList$meta$adhesionResults <- adhesionResults"); // save results into the trackList object
		R.eval("trackList$meta$fitResults <- fitResults"); // save results into the trackList object
		R.eval("trackList$meta$finalResults <- finalResults"); // save results into the trackList object
		String trackListPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("RData");
		R.eval("save(list=c('trackList'), file=" + R.quotedPath(trackListPath) + ")");
		outputTrackList = FileWriter.makeFileObject("temp", null, trackListPath);

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Return status
		return true;
	}
}
