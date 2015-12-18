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
import Database.Definition.Parameter;
import Database.Definition.TypeName;
import cruncher.Ticket;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
import weka.core.converters.JEXTableWriter;

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
		name="Compile ARFF Tables",
		menuPath="Table Tools",
		visible=true,
		description="Compile results in ARFF files across different entries in the database."
		)
public class CompileArffTables extends JEXPlugin {

	public CompileArffTables()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="ARFF Table", type=MarkerConstants.TYPE_FILE, description="ARFF Table to be compiles.", optional=false)
	JEXData fileData;

	/////////// Define Parameters ///////////

	Parameter p0 = new Parameter("Sorting Labels", "Names of labels in these entries by which to sort the data in the compiled results table (comma separated, no extra spaces near commas, case sensitive).", "Valid,Substrate,Cell");

	@ParameterMarker(uiOrder=1, name="Sorting Labels", description="Names of labels in these entries by which to sort the data in the compiled results table (comma separated, no extra spaces near commas, case sensitive).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Valid,Substrate,Cell")
	String sortingLabelsCSVString;

	@ParameterMarker(uiOrder=2, name="Save As...", description="Choose a file extension.", ui=MarkerConstants.UI_DROPDOWN, choices={"arff","csv","txt"}, defaultChoice=0)
	String fileExtension;

	@ParameterMarker(uiOrder=3, name="If CSV, Reorganize Table?", description="(REQUIRES R/RSERVE!) If saving as a CSV, should the table be 'reorganized' to put each 'Measurement' as a column of the table?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean reorganize;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Compiled ARFF Tablee", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant compiled ARFF Table", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	public static TreeMap<DimensionMap,Double> compiledData = null;

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(!JEXPlugin.isInputValid(fileData, JEXData.FILE))	
		{
			return false;
		}

		TreeMap<DimensionMap,String> tables = FileReader.readObjectToFilePathTable(fileData);
		TreeMap<DimensionMap,Double> tableDatas = new TreeMap<DimensionMap,Double>();
		double count = 0;
		double total = tables.size();
		double percentage = 0;
		for (DimensionMap map : tables.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			Table<Double> tableData = JEXTableReader.getNumericTable(tables.get(map));
			for (Entry<DimensionMap,Double> e : tableData.data.entrySet())
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
			compiledData = new TreeMap<DimensionMap,Double>();
		}
		compiledMap = new DimensionMap();
		compiledMap.put("Experiment", optionalEntry.getEntryExperiment());
		// compiledMap.put("Array Name", entry.getEntryTrayName());
		compiledMap.put("Array X", "" + optionalEntry.getTrayX());
		compiledMap.put("Array Y", "" + optionalEntry.getTrayY());
		for (String labelName : sortingLabels)
		{
			JEXData label = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, labelName), optionalEntry);
			compiledMap.put(labelName, LabelReader.readLabelValue(label));
		}

		for (Entry<DimensionMap,Double> e : tableDatas.entrySet())
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
			String path = JEXTableWriter.writeTable(output.getTypeName().getName(), new DimTable(compiledData), compiledData, JEXTableWriter.TXT_FILE);
			JEXData data = FileWriter.makeFileObject(output.getTypeName().getName(),null, path);
			Set<JEXData> set = outputList.get(entry);
			set.clear();
			set.add(data);
		}

		compiledData = null;
	}
}