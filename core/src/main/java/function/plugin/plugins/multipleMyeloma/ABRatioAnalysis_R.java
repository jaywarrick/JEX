package function.plugin.plugins.multipleMyeloma;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import miscellaneous.StringUtility;
import rtools.R;
import rtools.ScriptRepository;

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
		name="A/B Ratio Analysis (R)",
		menuPath="Analysis",
		visible=true,
		description="From basically any compiled and reorganized table -> Determine A/B (or log(A/B)) and plot clustered histograms."
		)
public class ABRatioAnalysis_R extends JEXPlugin {

	public ABRatioAnalysis_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Data Table (compiled)", type=MarkerConstants.TYPE_FILE, description="Table of (R)ed and (G)reen fluorescence data in arff format.", optional=false)
	JEXData fileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Manual Threshold", description="The threshold of the ratio (e.g., 1) or log-ratio (e.g., 0) to manually determine percents of two subpopulations (p1 (lo) and p2 (hi)).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double logRatioThreshold;

	@ParameterMarker(uiOrder=2, name="n Clusters", description="In parallel, use EM Clustering to automatically define thresholds between 'n' populations (p1, ..., pn)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	int nClusters;

	@ParameterMarker(uiOrder=0, name="Group By", description="Names of columns in the compiled data table by which to group results. (e.g., 'Experiment, Array.X, Array.Y, Location')", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Experiment, Array.X, Array.Y")
	String byString;

	@ParameterMarker(uiOrder=4, name="A Column Name", description="The name of the column in the compiled table containing numerator values.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="A")
	String Acol;

	@ParameterMarker(uiOrder=5, name="B Column Name", description="The name of the column in the compiled table containing denominator values.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="B")
	String Bcol;

	@ParameterMarker(uiOrder=6, name="Random Number Generator Seed", description="Seed (integer) used to initialize EM Clustering.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="54321")
	String seed;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Individual Ratio Histograms", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The clustered histograms of A/B for each individual grouping (i.e., entry and separated dimension value)", enabled=true)
	JEXData indRatioOutput;

	@OutputMarker(uiOrder=2, name="Individual log(Ratio) Histograms", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The clustered histograms of ln(A/B) for each individual grouping (i.e., entry and separated dimension value)", enabled=true)
	JEXData indLogRatioOutput;

	@OutputMarker(uiOrder=3, name="Overall Ratio Histogram", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The overall clustered histograms of A/B containing all cells (i.e., all entries and values of the separated dimension)", enabled=true)
	JEXData overallRatioOutput;

	@OutputMarker(uiOrder=4, name="Overall log(Ratio) Histogram", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The overall clustered histograms of ln(A/B) containing all cells (i.e., all entries and values of the separated dimension)", enabled=true)
	JEXData overallLogRatioOutput;

	@OutputMarker(uiOrder=5, name="Summary Table (Cluster)", type=MarkerConstants.TYPE_FILE, flavor="", description="The subpopulation fractions calculated via clustering for each subset of the compiled data (i.e., percents of p1, ..., pn).", enabled=true)
	JEXData summaryOutputCluster;

	@OutputMarker(uiOrder=6, name="Summary Table (Manual)", type=MarkerConstants.TYPE_FILE, flavor="", description="The subpopulation fractions calculated via the provided threshold for each subset of the compiled data (i.e., percents of p1, ..., pn).", enabled=true)
	JEXData summaryOutputManual;

	@OutputMarker(uiOrder=7, name="Single-Cell Table", type=MarkerConstants.TYPE_FILE, flavor="", description="The A and B information for each cell in the compiled data.", enabled=true)
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
		if(fileData == null || !fileData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}

		// Collect Parameters
		CSVList byCSVList = new CSVList();
		if(byString != null && !byString.equals(""))
		{
			byCSVList = StringUtility.getCSVListAndRemoveWhiteSpaceOnEnds(byString);
		}
		String byStatement = this.getByStatment(byCSVList);

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

		R.eval("results <- analyzeRatio(compiledTablePath=" + R.quotedPath(FileReader.readFileObject(fileData)) + ", outputFolder=jexTempRFolder, logRatioThreshold=" + logRatioThreshold + ", nClusters=" + nClusters + ", Acol=" + R.sQuote(Acol) + ", Bcol=" + R.sQuote(Bcol) + ", by=" + byStatement + ")");
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
		this.summaryOutputCluster = R.getCharacterVariableAsJEXDataFileObject("results$summaryTable.cluster", false);
		this.summaryOutputManual = R.getCharacterVariableAsJEXDataFileObject("results$summaryTable.manual", false);
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
	
	private String getByStatment(CSVList sortingLabels)
	{
		CSVList temp = new CSVList();
		for(String item : sortingLabels)
		{
			temp.add(R.sQuote(item));
		}
		return("c(" + temp.toString() + ")");
	}

}
