package function.plugin.plugins.tableTools;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.LabelReader;
import Database.DataWriter.FileWriter;
import Database.Definition.TypeName;
import cruncher.Ticket;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.JEXCSVReader;
import miscellaneous.JEXCSVWriter;
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
		name="Compile CSV Tables",
		menuPath="Table Tools",
		visible=true,
		description="Compile results of CSV files across different entries in the database. (Table must have a header row!)"
		)
public class CompileCSVTables extends JEXPlugin {

	public CompileCSVTables()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="CSV Table", type=MarkerConstants.TYPE_FILE, description="ARFF Table to be compiles.", optional=false)
	JEXData fileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Sorting Labels", description="Names of labels in these entries by which to sort the data in the compiled results table (comma separated, no extra spaces near commas, case sensitive).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Valid,Substrate,Cell")
	String sortingLabelsCSVString;
	
	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Compiled Table", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant compiled table", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	public static TreeMap<DimensionMap,String> compiledData = null;

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(fileData == null || !fileData.getTypeName().getType().equals(JEXData.FILE))	
		{
			return false;
		}

		TreeMap<DimensionMap,String> tables = FileReader.readObjectToFilePathTable(fileData);
		TreeMap<DimensionMap,String> tableDatas = new TreeMap<>();
		double count = 0;
		double total = tables.size();
		double percentage = 0;
		for (DimensionMap map : tables.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			TreeMap<DimensionMap,String> tableData = JEXCSVReader.getCSVTable(tables.get(map), true);
			for (Entry<DimensionMap,String> e : tableData.entrySet())
			{
				DimensionMap map2 = e.getKey().copy();
				map2.putAll(map);
				tableDatas.put(map2, e.getValue());
			}
			count = count + 1;
			percentage = 100 * count / total;
			JEXStatics.statusBar.setProgressPercentage((int) percentage);
		}

		// Collect Parameters
		CSVList sortingLabels = new CSVList();
		if(sortingLabelsCSVString != null && !sortingLabelsCSVString.equals(""))
		{
			sortingLabels = new CSVList(sortingLabelsCSVString);
		}

		// Save the data.
		DimensionMap compiledMap = new DimensionMap();
		if(compiledData == null)
		{
			compiledData = new TreeMap<>();
		}
		compiledMap = new DimensionMap();
		compiledMap.put("Experiment", optionalEntry.getEntryExperiment());
		// compiledMap.put("Array Name", entry.getEntryTrayName());
		compiledMap.put("Array X", "" + optionalEntry.getTrayX());
		compiledMap.put("Array Y", "" + optionalEntry.getTrayY());
		for (String labelName : sortingLabels)
		{
			JEXData label = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, labelName), optionalEntry);
			if(label != null)
			{
				compiledMap.put(labelName, LabelReader.readLabelValue(label));
			}
			else
			{
				Logs.log("No label named '" + labelName + "' could be found in Experiment: " + optionalEntry.getEntryExperiment() + ", X: " + optionalEntry.getTrayX() + ", Y: " + optionalEntry.getTrayY(), this);
			}
		}

		for (Entry<DimensionMap,String> e : tableDatas.entrySet())
		{
			DimensionMap map = e.getKey().copy();
			map.putAll(compiledMap);
			compiledData.put(map, e.getValue());
		}

		// Return status
		return true;
	}

	public void finalizeTicket(Ticket ticket)
	{
		if(compiledData == null)
		{
			return;
		}
		// Write the file and make a JEXData
		// Put the final JEXData in all the entries
		TreeMap<JEXEntry,Set<JEXData>> outputList = ticket.getOutputList();
		for (JEXEntry entry : outputList.keySet())
		{
			String path = JEXCSVWriter.write(compiledData);
			JEXData data = FileWriter.makeFileObject(ticket.getOutputNames()[0].getName(),null, path);
			Set<JEXData> set = outputList.get(entry);
			set.clear();
			set.add(data);
		}

		// Remember to set static variable to null again so this data isn't carried over to other function runs
		compiledData = null;
	}
}