package function.plugin.plugins.tableTools;

import java.util.TreeMap;

import jex.statics.JEXStatics;
import miscellaneous.JEXCSVReader;

import org.scijava.plugin.Plugin;

import tables.DimTableBuilder;
import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.FileWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;

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
		name="Convert CSV to ARFF",
		menuPath="Table Tools",
		visible=true,
		description="Convert a CSV Table that has a header row to an ARFF table."
		)
public class ConvertCSVToArff extends JEXPlugin {

	public ConvertCSVToArff()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="CSV Table", type=MarkerConstants.TYPE_FILE, description="Table file to be converted.", optional=false)
	JEXData csvData;
	
	/////////// Define Parameters ///////////
	
	// No parameters necessary
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="ARFF Table", type=MarkerConstants.TYPE_FILE, flavor="", description="Output ARFF formatted table file.", enabled=true)
	JEXData output;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(csvData == null || !csvData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> csvMap = FileReader.readObjectToFilePathTable(csvData);
		TreeMap<DimensionMap,String> arffMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		for (DimensionMap map : csvMap.keySet())
		{
			tempPath = csvMap.get(map);
			String newPath = this.convertFile(tempPath);
			arffMap.put(map, newPath);
			
			// Update progress bar
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) csvMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(arffMap.size() == 0)
		{
			return false;
		}
		
		this.output = FileWriter.makeFileObject("temp",null,arffMap);
		
		// Return status
		return true;
	}
	
	public String convertFile(String csvPath)
	{
		JEXCSVReader reader = new JEXCSVReader(csvPath, true);
		
		// In order to write an Arff table we need to build a DimTable
		// We can't keep all the data in memory as it might be too large so just build DimTable for now.
		DimTableBuilder builder = new DimTableBuilder();
		while(!reader.isEOF())
		{
			builder.add(reader.readRowToDimensionMapString().p1);
		}
		reader.close();
		
		// Now that we have the DimTable we can transfer each row of the csv to the arff file after writing the header.
		reader = new JEXCSVReader(csvPath, true);
		JEXTableWriter arffWriter = new JEXTableWriter("FeatureTable", "arff");
		arffWriter.writeNumericTableHeader(builder.getDimTable());
		while(!reader.isEOF())
		{
			miscellaneous.Pair<DimensionMap,String> result = reader.readRowToDimensionMapString();
			arffWriter.writeData(result.p1, Double.parseDouble(result.p2));
		}
		
		// Close and save the data.
		reader.close();
		String arffPath = arffWriter.getPath();
		arffWriter.close();
		return arffPath;
	}
}
