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
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.JEXCSVWriter;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;

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
		name="Convert ARFF to CSV",
		menuPath="Table Tools",
		visible=false,
		description="Convert an arff table to csv format."
		)
public class ConvertARFFToCSV extends JEXPlugin {

	public ConvertARFFToCSV()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="ARFF Table", type=MarkerConstants.TYPE_FILE, description="ARFF Table to convert to csv format.", optional=false)
	JEXData fileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Additional Labels", description="Names of labels to add to the table from the database (comma separated, no extra spaces near commas, case sensitive).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Valid,Substrate,Cell")
	String labelsCSVString;

	@ParameterMarker(uiOrder=2, name="Add Experiment, X, and Y Info?", description="Whether to add columns with the Experiment name and Array X and Y locations in the database.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean addExperimentInfo;

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
		if(labelsCSVString != null && !labelsCSVString.equals(""))
		{
			sortingLabels = new CSVList(labelsCSVString);
		}

		// Save the data.
		DimensionMap compiledMap = new DimensionMap();
		TreeMap<DimensionMap,Double> compiledData = new TreeMap<>();

		if(addExperimentInfo)
		{
			compiledMap = new DimensionMap();
			compiledMap.put("Experiment", optionalEntry.getEntryExperiment());
			// compiledMap.put("Array Name", entry.getEntryTrayName());
			compiledMap.put("Array X", "" + optionalEntry.getTrayX());
			compiledMap.put("Array Y", "" + optionalEntry.getTrayY());
		}
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

		for (Entry<DimensionMap,Double> e : tableDatas.entrySet())
		{
			DimensionMap map = e.getKey().copy();
			map.putAll(compiledMap);
			compiledData.put(map, e.getValue());
		}

		String path = JEXCSVWriter.writeDoubleTable(compiledData);

		this.output = FileWriter.makeFileObject(output.getTypeName().getName(), null, path);

		// Return status
		return true;
	}
}