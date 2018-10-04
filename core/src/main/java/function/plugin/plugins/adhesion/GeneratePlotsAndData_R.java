package function.plugin.plugins.adhesion;

import java.io.File;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;
import rtools.R;
import tables.DimensionMap;

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

	@ParameterMarker(uiOrder=1, name="Velocity Threshold [pixels/s]", description="The velocity below which a cell is considered adhered (typical values are around 2-5)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="3.0")
	double velocityThreshold;
	
	@ParameterMarker(uiOrder=2, name="Initial 'mu' Search Range", description="The 'lower, upper, number-of-increments' range (log scale incremented) to grid search for 1-pop fit before least-squares optimization.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0001,0.5,50")
	String muRange;
	
	@ParameterMarker(uiOrder=3, name="Initial 'sigma' Search Range", description="The 'lower, upper, increment' range to grid search for 1-pop fit before least-squares optimization.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.01,1.51,0.01")
	String sigmaRange;
	
	@ParameterMarker(uiOrder=4, name="Alpha Guess", description="The fraction of the total population that represents the lower adhesion strength population", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.9")
	double alphaGuess;
	
	@ParameterMarker(uiOrder=5, name="mu2 Guess", description="The mean frequency near which the stronger adhesion population is expected to adhere. Leave as '-1' (all caps) to automatically generate a reasonable guess of 1 sigma above whatever is determined for the single population.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="-1")
	double mu2Guess;
	
	@ParameterMarker(uiOrder=6, name="sigma2 Guess", description="The log normal sigma of the population (i.e., it is log(sigma) of a standard distribution). Leave as -1 to automatically guess the same sigma as found for the lower strength adhesion population.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="-1")
	double sigma2Guess;
	
	@ParameterMarker(uiOrder=7, name="Number of Cores (OSX/Unix only)", description="Number of processor cores to use during grid search of initial guess for the single population model fit. (WINDOWS ONLY ALLOWS 1)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double cores;
	
	@ParameterMarker(uiOrder=8, name="Windows R Memory (MB)", description="Specify the amount of memory alotted to R on windows machines. Default is around 2000 (MB). Add more if needed (i.e., if you get a memory allocation error). 4000 should work on most machines.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4000")
	int memory;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Phase Shift Plots", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Plots of the track velocities overlaid with a predicted velocity to illustrate whether we have fit the cell motion well in terms of when the cell switches direction.", enabled=true)
	JEXData outputPhasePlots;

	@OutputMarker(uiOrder=2, name="Percent Adhered vs Frequency Plot", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Plots of the percent adhered vs frequency, and different plots of the fitted curves.", enabled=true)
	JEXData outputAdhesionPlot;

	@OutputMarker(uiOrder=3, name="Percent Adhered vs Frequency Table", type=MarkerConstants.TYPE_FILE, flavor="", description="Table of the percent adhered vs frequency data along with fitted values.", enabled=true)
	JEXData outputAdhesionPercentageTable;
	
	@OutputMarker(uiOrder=4, name="Fitted Results Table", type=MarkerConstants.TYPE_FILE, flavor="", description="Table of the fitted parameters of adhesion curve.", enabled=true)
	JEXData outputFittedParametersTable;
	
	@OutputMarker(uiOrder=5, name="R TrackList (analyzed)", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant R TrackList after analyzing for percent adhered (saved as a .RData file).", enabled=true)
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
		if(roiFileData == null || !roiFileData.getTypeName().getType().matches(JEXData.FILE))
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
		R.eval("memory.limit(size=" + memory + ")"); // Sometimes we need a bit of extra memory beyond the default 2Gb.
		R.eval("library(ParticleTracking)");
		//AdhesionUtility.loadAdhesionScripts(); // Load necessary R scripts

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
		// getSweep <- function(amplitude=1, phaseShift=0, offset=0, sin=FALSE, ti=0, fi=2, ff=0.1, sweepDuration=300, t=seq(0,300,0.05), guess=NULL, calcVelocity=TRUE, flipped=TRUE)
		R.eval("fitCurveData <- getSweep(amplitude=bestFit$par[['amplitude']], phaseShift=bestFit$par[['phaseShift']], offset=0, sin=trackList$meta$sin, ti=bestFit$par[['ti']], fi=trackList$meta$fi, ff=trackList$meta$ff, sweepDuration=trackList$meta$sweepDuration, t=trackList$meta$tAll, guess=NULL, calcVelocity=TRUE, flipped=TRUE)");

		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		// Plot the trackList velocities and the fitted curve
		String plotPath = R.startPlot("tif", 7, 5, 600, 10, "Arial", "lzw");
		// plotTrackList = function(slot='vx', fun=NULL, rel=FALSE, ...)
		R.eval("trackList$plotTrackList(slot='vxs', ylim=c(-500,500), rel=FALSE, validOnly=FALSE, xlim=c(0,300))");
		R.eval("lines(fitCurveData$t, fitCurveData$v, col='red')");
		R.endPlot();
		phasePlots.put(new DimensionMap("i=0"), plotPath);
		plotPath = R.startPlot("tif", 7, 5, 600, 10, "Arial", "lzw");
		// plotTrackList = function(slot='vx', fun=NULL, rel=FALSE, ...)
		R.eval("trackList$plotTrackList(slot='vxs', ylim=c(-500,500), rel=FALSE, validOnly=FALSE, xlim=c(0,5))");
		R.eval("lines(fitCurveData$t, fitCurveData$v, col='red')");
		R.endPlot();
		phasePlots.put(new DimensionMap("i=1"), plotPath);
		plotPath = R.startPlot("tif", 7, 5, 600, 10, "Arial", "lzw");
		// plotTrackList = function(slot='vx', fun=NULL, rel=FALSE, ...)
		R.eval("trackList$plotTrackList(slot='vxs', ylim=c(-50,50), rel=FALSE, validOnly=FALSE, xlim=c(125,300))");
		R.eval("lines(fitCurveData$t, fitCurveData$v, col='red')");
		R.endPlot();
		phasePlots.put(new DimensionMap("i=2"), plotPath);
		this.outputPhasePlots = ImageWriter.makeImageStackFromPaths("temp", phasePlots);

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
		// getPercentAdhered = function(velocityThreshold=3)
		R.eval("adhesionResults <- trackList$getPercentAdhered(velocityThreshold=" + velocityThreshold + ", slot='vxs')");
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}

		// Fit the data
		// getFrequencies <- function(t=seq(0,300,0.035), fi=1, ff=0.01, duration=300)
		R.eval("adhesionResults$f <- getFrequencies(adhesionResults$time, fi=trackList$meta$fi, ff=trackList$meta$ff, duration=trackList$meta$sweepDuration)$f");
		// fitLogNormGS <- function(x, y, mu=lseq(0.0001, 0.5, 50), sigma=seq(0.01,1.51,0.01), alphaGuess=0.9, mu2Guess=-1, sigma2Guess=-1, method='L-BFGS-B', cores=1)
		R.eval("fitResults <- fitLogNormGS(x=adhesionResults$f, y=adhesionResults$percentAdhered/100, mu=lseq(" + muRange + "), sigma=seq(" + sigmaRange + "), alphaGuess=" + alphaGuess + ", mu2Guess=" + mu2Guess + ", sigma2Guess=" + sigma2Guess + ", cores=" + cores + ")");
		R.eval("fit1y <- do.call(logNorm, c(list(x=adhesionResults$f), fitResults$par1))");
		R.eval("fit2y <- do.call(logNorm2, c(list(x=adhesionResults$f), fitResults$par2))");
		R.eval("adhesionResults$fit1y <- fit1y");
		R.eval("adhesionResults$fit2y <- fit2y");
		R.eval("finalResults <- as.data.frame(as.list(c(fitResults$par1, fitResults$par2, r2_single=fitResults$r2_single, r2_double=fitResults$r2_double, limitFlag=fitResults$limitFlag)))");
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}
		
		// Plot the data
		plotPath = R.startPlot("tif", 5, 3.5, 600, 10, "Arial", "lzw");
		R.eval("plot(adhesionResults$f, adhesionResults$percentAdhered/100, pch=21, cex=0.5, col=rgb(0,0,0,0), bg=rgb(0,0,0,0), log='x', xlab='Frequency [Hz]', ylab='Fraction of Population Adhered', main='')");
		R.eval("lines(adhesionResults$f, fit1y, lwd=3, col='red')");
		R.eval("lines(adhesionResults$f, fit2y, lwd=3, col='blue')");
		R.eval("points(adhesionResults$f, adhesionResults$percentAdhered/100, pch=21, cex=0.5, col=rgb(0,0,0,0), bg=rgb(0,0,0,0.5), log='x', xlab='Frequency [Hz]', ylab='Fraction of Population Adhered', main='')");
		R.eval("legend('topright', c('1 Pop.','2 Pop.'), lty=c(1,1), lwd=c(3,3),col=c('red','blue'))");
		R.endPlot();
		outputAdhesionPlot = ImageWriter.makeImageObject("temp", plotPath);
		
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
		outputAdhesionPercentageTable = FileWriter.makeFileObject("temp", null, tablePath);
		tablePath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("csv");
		R.eval("write.csv(finalResults, file=" + R.quotedPath(tablePath) + ", row.names=FALSE)");
		outputFittedParametersTable = FileWriter.makeFileObject("temp", null, tablePath);

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
