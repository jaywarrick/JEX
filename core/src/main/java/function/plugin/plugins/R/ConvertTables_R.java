package function.plugin.plugins.R;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.exec.OS;
import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.LabelReader;
import Database.DataWriter.FileWriter;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
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
		name="Convert Tables (R)",
		menuPath="R",
		visible=true,
		description="Gather label information into the table, optionally reorganize tables and save as csv."
		)
public class ConvertTables_R extends JEXPlugin {

	public ConvertTables_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Table", type=MarkerConstants.TYPE_FILE, description="ARFF or CSV Table(s) to be converted.", optional=false)
	JEXData fileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Additional Labels", description="Names of labels in these entries by which to sort the data in the compiled results table (comma separated, no extra spaces near commas, case sensitive).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Valid")
	String labelsCSVString;

	@ParameterMarker(uiOrder=2, name="Input File Type", description="Choose format of the data in the file (not necessarily the same as the extension, such as csv format in .txt file).", ui=MarkerConstants.UI_DROPDOWN, choices={"arff","csv"}, defaultChoice=0)
	String inputType;
	
	@ParameterMarker(uiOrder=2, name="Save As...", description="Choose a file type/extension to save the file.", ui=MarkerConstants.UI_DROPDOWN, choices={"arff","csv"}, defaultChoice=0)
	String fileExtension;

	@ParameterMarker(uiOrder=3, name="Add Experiment, X, and Y Info?", description="Whether to add columns with the Experiment name and Array X and Y locations in the database.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean addExperimentInfo;

	@ParameterMarker(uiOrder=4, name="Reorganize table from long to wide format?", description="Whether or not to convert the table from long format (one column for all measurement names) to wide format (one column for each measurement).", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean reorganize;

	@ParameterMarker(uiOrder=5, name="Reorg: Measurement Col Name(s)", description="Comma separate list of column names of the ARFF table that describes the measurements in the 'Value' column(s). (Almost always 'Measurement')", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Measurement")
	String nameCols;

	@ParameterMarker(uiOrder=6, name="Reorg: Value Col Name(s)", description="Name of the column of the ARFF table that stores the values of each 'Measurement'. (Almost always 'Value')", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Value")
	String valueCols;

	@ParameterMarker(uiOrder=7, name="Reorg: Id Col Name(s)", description="Usually leave blank to grab 'the rest' of the columns. Comma separated list of column names that identify unique rows for the resultant table (i.e., the unique identifiers for each row of information like Id, Experiment, Array.X, Array.Y).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String idCols;

	@ParameterMarker(uiOrder=8, name="Windows R Memory Limit (MB)", description="Set a new memory limit (in MB) on a windows machine if you get an R error that says cannot allocate memory for ... (try 4000 or so?)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2000")
	int memoryLimit;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Converted Table", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant converted table", enabled=true)
	JEXData output;

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
		if(labelsCSVString != null && !labelsCSVString.equals(""))
		{
			sortingLabels = StringUtility.getCSVListAndRemoveWhiteSpaceOnEnds(labelsCSVString);
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
		TreeMap<DimensionMap,String> outputTables = new TreeMap<>();
		double count = 0;
		double total = tables.size()*4;
		double percentage = 0;

		R.initializeWorkspace();
		ScriptRepository.sourceGitHubFile("jaywarrick", "R-General", "master", ".Rprofile");
		R.load("foreign");
		R.load("data.table");
		if(OS.isFamilyWindows())
		{
			R.eval("memory.limit(size = " + memoryLimit + ")");
		}
		for (DimensionMap map : tables.keySet())
		{
			/////////////////// Read the data
			if(this.isCanceled())
			{
				return false;
			}
			String tempPath = tables.get(map);
			if(inputType.equals("arff"))
			{
				R.eval("temp <- data.table(read.arff(file=" + R.quotedPath(tempPath) + "))");
			}
			else
			{
				R.eval("temp <- fread(input=" + R.quotedPath(tempPath) + "))");
			}
			count = count + 1;
			percentage = 100 * count / total;
			JEXStatics.statusBar.setProgressPercentage((int) percentage);

			/////////////////// Append additional labels
			if(this.isCanceled())
			{
				return false;
			}
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
			if(reorganize)
			{
				R.reorganize("temp", idCols, nameCols, valueCols, null);
			}
			count = count + 1;
			percentage = 100 * count / total;
			JEXStatics.statusBar.setProgressPercentage((int) percentage);

			/////////////////// Save and store path
			if(this.isCanceled())
			{
				return false;
			}
			String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath(this.fileExtension);
			if(this.fileExtension.equals("arff"))
			{
				R.eval("write.arff(temp, file=" + R.quotedPath(path) + ")");
			}
			else
			{
				// txt or csv
				R.eval("write.csv(temp, file=" + R.quotedPath(path) + ", row.names=FALSE)");
			}
			outputTables.put(map, path);
			count = count + 1;
			percentage = 100 * count / total;
			JEXStatics.statusBar.setProgressPercentage((int) percentage);
		}
		this.output = FileWriter.makeFileObject("temp", null, outputTables);

		// Return status
		return true;
	}

	/**
	 * Return a "formula" version of the comma separated string of variables
	 * 
	 * i.e., turn "A,B , C, D , E" (note handles spaces) into "A+B+C+D+E"
	 * 
	 * @param param
	 * @return
	 */
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