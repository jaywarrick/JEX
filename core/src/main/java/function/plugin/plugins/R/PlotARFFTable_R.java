package function.plugin.plugins.R;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.LabelReader;
import Database.Definition.TypeName;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.JEXCSVWriter;
import miscellaneous.Pair;
import miscellaneous.StringUtility;
import rtools.R;
import rtools.ScriptRepository;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author jaywarrick
 * 
 */
@Plugin(
		type = JEXPlugin.class,
		name="Plot ARFF File (R)",
		menuPath="R",
		visible=true,
		description="Gather label information into the table, open web-based data browser."
		)
public class PlotARFFTable_R extends JEXPlugin {

	public PlotARFFTable_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="ARFF Table", type=MarkerConstants.TYPE_FILE, description="ARFF Table to be plotted.", optional=false)
	JEXData fileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Additional Labels", description="Names of labels in these entries by which to sort the data in the compiled results table (comma separated, no extra spaces near commas, case sensitive).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Valid")
	String sortingLabelsCSVString;

	@ParameterMarker(uiOrder=2, name="Add Experiment, X, and Y Info?", description="Whether to add columns with the Experiment name and Array X and Y locations in the database.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean addExperimentInfo;

	@ParameterMarker(uiOrder=4, name="Reorg: Id Col Name(s)", description="Comma separated list of column names that identify unique rows for the resultant table (i.e., the unique identifiers for each row of information like Id, Experiment, Array.X, Array.Y).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Id,Label")
	String idCols;

	@ParameterMarker(uiOrder=5, name="Reorg: Measurement Col Name(s)", description="Comma separate list of column names of the ARFF table that describes the measurements in the 'Value' column(s). (Almost always 'Measurement')", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Measurement,MaskChannel_ImageChannel")
	String nameCols;

	@ParameterMarker(uiOrder=6, name="Reorg: Value Col Name(s)", description="Name of the column of the ARFF table that stores the values of each 'Measurement'. (Almost always 'Value')", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Value")
	String valueCols;

	/////////// Define Outputs ///////////

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	JEXCSVWriter writer = null;
	public Set<String> header = null;

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(fileData == null || !fileData.getTypeName().getType().equals(JEXData.FILE))	
		{
			return false;
		}

		// Collect Parameters
		CSVList sortingLabels = new CSVList();
		if(sortingLabelsCSVString != null && !sortingLabelsCSVString.equals(""))
		{
			sortingLabels = StringUtility.getCSVListAndRemoveWhiteSpaceOnEnds(sortingLabelsCSVString);
		}
		List<Pair<String,String>> newCols = new Vector<>();
		if(addExperimentInfo)
		{
			newCols.add(new Pair<String,String>("Experiment", optionalEntry.getEntryExperiment()));
			newCols.add(new Pair<String,String>("Array.X", ""+optionalEntry.getTrayX()));
			newCols.add(new Pair<String,String>("Array.Y", ""+optionalEntry.getTrayY()));
		}
		for (String labelName : sortingLabels)
		{
			JEXData label = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, labelName), optionalEntry);
			if(label != null)
			{
				newCols.add(new Pair<String,String>(labelName, LabelReader.readLabelValue(label)));
			}
			else
			{
				Logs.log("No label named '" + labelName + "' could be found in Experiment: " + optionalEntry.getEntryExperiment() + ", X: " + optionalEntry.getTrayX() + ", Y: " + optionalEntry.getTrayY(), this);
			}
		}

		TreeMap<DimensionMap,String> tables = FileReader.readObjectToFilePathTable(fileData);
		double count = 0;
		double total = 4;
		double percentage = 0;

		R.initializeWorkspace();
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", ".Rprofile");
		R.load("foreign");
		R.load("data.table");
		
		/////////////////// Read the data
		if(this.isCanceled())
		{
			return false;
		}
		JEXStatics.statusBar.setStatusText("Reading the table...");
		R.eval("temp <- data.table(read.arff(file=" + R.quotedPath(tables.firstEntry().getValue()) + "))");
		count = count + 1;
		percentage = 100 * count / total;
		JEXStatics.statusBar.setProgressPercentage((int) percentage);
		

		/////////////////// Append additional labels
		if(this.isCanceled())
		{
			return false;
		}
		JEXStatics.statusBar.setStatusText("Appending labels as columns to the table...");
		for(Pair<String,String> newCol : newCols)
		{
			R.eval("temp$" + newCol.p1 + " <- " + R.sQuote(newCol.p2));
		}
		count = count + 1;
		percentage = 100 * count / total;
		JEXStatics.statusBar.setProgressPercentage((int) percentage);

		/////////////////// Reorganize if needed
		if(this.isCanceled())
		{
			return false;
		}
		JEXStatics.statusBar.setStatusText("Reorganizing the table...");
		String measurements = this.getRText(nameCols);
		String ids = this.getRText(idCols);
		String values = this.getValuesText(valueCols);
		if(measurements == null)
		{
			JEXDialog.messageDialog("This function requires at least one 'Measurement' column name. (see ?dcast of data.table in R, formula = Id ~ Measurment and value.var= Value). Aborting.");
			return false;
		}
		if(ids == null)
		{
			JEXDialog.messageDialog("This function requires at least one 'Id' column name. (see ?dcast of data.table in R, formula = Id ~ Measurment and value.var= Value). Aborting.");
			return false;
		}
		if(values == null)
		{
			values = "NULL"; // this causes R to guess what the value column is.
		}
		R.eval("shinyData <- dcast(temp, " + ids + " ~ " + measurements + ", value.var = c(" + values + "))");
		count = count + 1;
		percentage = 100 * count / total;
		JEXStatics.statusBar.setProgressPercentage((int) percentage);
		
		/////////////////// Start the data browser
		JEXStatics.statusBar.setStatusText("Starting the Browser...");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", "DataBrowser/ui.R");
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", "DataBrowser/server.R");
		R.eval("shinyApp(ui=myUI, server=myServer)");
		count = count + 1;
		percentage = 100 * count / total;
		JEXStatics.statusBar.setProgressPercentage((int) percentage);

		/////////////////// Return status
		return true;
	}

	public String getRText(String param)
	{
		CSVList items = StringUtility.getCSVListAndRemoveWhiteSpaceOnEnds(param);
		StringBuilder sb = new StringBuilder();
		for(int i = 0 ; i < items.size() - 1; i ++)
		{
			sb.append(items.get(i));
			sb.append("+");
		}
		if(items.size() > 0)
		{
			sb.append(items.lastElement());
		}

		String ret = sb.toString();
		if(ret.equals(""))
		{
			return null;
		}
		return ret;
	}

	public String getValuesText(String param)
	{
		CSVList items = StringUtility.getCSVListAndRemoveWhiteSpaceOnEnds(param);
		StringBuilder sb = new StringBuilder();
		for(int i = 0 ; i < items.size() - 1; i ++)
		{
			sb.append(R.sQuote(items.get(i)));
			sb.append(",");
		}
		if(items.size() > 0)
		{
			sb.append(R.sQuote(items.lastElement()));
		}
		String ret = sb.toString();
		if(ret.equals(""))
		{
			return null;
		}
		return ret;
	}
}