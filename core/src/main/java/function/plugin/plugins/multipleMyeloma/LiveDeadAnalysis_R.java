package function.plugin.plugins.multipleMyeloma;

import jex.statics.JEXStatics;

import org.scijava.plugin.Plugin;

import rtools.R;
import rtools.ScriptRepository;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
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
		name="Live Dead Analysis (R)",
		menuPath="Multiple Myeloma",
		visible=true,
		description="From intensity data gathered using Find Maxima Segmentation -> Measure Maxima (i.e., R, G, B measurements), determine G/R and log(G/R) and quantify percent live/dead."
		)
public class LiveDeadAnalysis_R extends JEXPlugin {

	public LiveDeadAnalysis_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Data Table (compiled)", type=MarkerConstants.TYPE_FILE, description="Table of (R)ed and (G)reen fluorescence data in arff format.", optional=false)
	JEXData fileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="log(G/R) Threshold", description="The threshold of log((G+101)/(R+101)) to determine live vs dead (typically default to 0, i.e., G/R=1)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double logRatioThreshold;

	@ParameterMarker(uiOrder=2, name="n Clusters", description="In parallel, use EM Clustering to automatically define thresholds between 'n' populations (typically 2, live and dead)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	int nClusters;

	@ParameterMarker(uiOrder=3, name="Location Dim Name (if any)", description="If results should be broken down further by an image dimension such as 'Location', enter that name here or leave blank.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String location;
	
	@ParameterMarker(uiOrder=4, name="Random Number Generator Seed", description="Seed (integer) used to initialize EM Clustering.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="54321")
	String seed;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Individual Ratio Histograms", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant .RData file of the filtered R TrackList,", enabled=true)
	JEXData indRatioOutput;
	
	@OutputMarker(uiOrder=1, name="Individual log(Ratio) Histograms", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant .RData file of the filtered R TrackList,", enabled=true)
	JEXData indLogRatioOutput;
	
	@OutputMarker(uiOrder=1, name="Overall Ratio Histogram", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant .RData file of the filtered R TrackList,", enabled=true)
	JEXData overallRatioOutput;
	
	@OutputMarker(uiOrder=1, name="Overall log(Ratio) Histogram", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant .RData file of the filtered R TrackList,", enabled=true)
	JEXData overallLogRatioOutput;
	
	@OutputMarker(uiOrder=1, name="Summary Table", type=MarkerConstants.TYPE_FILE, flavor="", description="The live dead fractions for each subset of the compiled data.", enabled=true)
	JEXData summaryOutput;
	
	@OutputMarker(uiOrder=1, name="Single-Cell Table", type=MarkerConstants.TYPE_FILE, flavor="", description="The live dead information for each cell in the compiled data.", enabled=true)
	JEXData singleCellOutput;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(fileData == null || !fileData.getTypeName().getType().matches(JEXData.FILE))
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
		R.initializeWorkspace(); // This creates jexDbFolder and jexTempRFolder
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", ".Rprofile");; // Load necessary R scripts
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-MultipleMyeloma", "master", "NewLiveDead.R");; // This defines the function analyzeLiveDead(compiledTablePath, jexFolder, logRatioThreshold=0, nClusters=2, locationDimension='Location')
		
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}
		
		R.eval("results <- analyzeLiveDead(compiledTablePath=" + R.quotedPath(FileReader.readFileObject(fileData)) + ", jexFolder=jexTempRFolder, logRatioThreshold=" + logRatioThreshold + ", nClusters=" + nClusters + ", locationDimension=" + R.sQuote(location)+ ")");
		// The function should return(list(indRatioHistograms, indLogRatioHistograms, overallRatioHistogram, overallLogRatioHistogram, summaryTable, singleCellTable, updatedTable, clusterResults))
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		if(this.isCanceled())
		{
			return false;
		}
		
		// Save
		this.indRatioOutput = R.getCharacterVectorAsJEXDataFileObject("results$indRatioHistograms", true);
		this.indLogRatioOutput = R.getCharacterVectorAsJEXDataFileObject("results$indLogRatioHistograms", true);
		this.overallRatioOutput = R.getCharacterVariableAsJEXDataFileObject("results$overallRatioHistogram", true);
		this.overallLogRatioOutput = R.getCharacterVariableAsJEXDataFileObject("results$overallLogRatioHistogram", true);
		this.summaryOutput = R.getCharacterVariableAsJEXDataFileObject("results$summaryTable", false);
		this.singleCellOutput = R.getCharacterVariableAsJEXDataFileObject("results$singleCellTable", false);
		
		//		String trackListPath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("RData");
		//		R.eval("save(list=c('trackList'), file=" + R.pathString(trackListPath) + ")");
		//		output = FileWriter.makeFileObject("temp", null, trackListPath);
		
		count = count + 1;
		percentage = (int) (100 * ((double) (count) / (total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);

		// Return status
		return true;
	}
	
}
