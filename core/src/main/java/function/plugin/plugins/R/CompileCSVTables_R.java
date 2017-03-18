package function.plugin.plugins.R;

import java.io.File;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.exec.OS;
import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataWriter.FileWriter;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;
import rtools.R;
import tables.DimensionMap;
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
		name="Compile CSV Tables (R)",
		menuPath="R",
		visible=true,
		description="Compile results of CSV files across different entries in the database. (Table must have a header row!)"
		)
public class CompileCSVTables_R extends JEXPlugin {

	public CompileCSVTables_R()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="CSV Table", type=MarkerConstants.TYPE_FILE, description="ARFF Table to be compiles.", optional=false)
	JEXData fileData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=7, name="Windows R Memory Limit (MB)", description="Set a new memory limit (in MB) on a windows machine if you get an R error that says cannot allocate memory for ... (try 4000 or so?)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2000")
	int memoryLimit;

	@ParameterMarker(uiOrder=8, name="Save in First Entry Only?", description="Should the resulting table be stored in all the selected entries or just the first (i.e., the entry with the smallest X position, then smallest Y position)?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean firstOnly;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Compiled Table", type=MarkerConstants.TYPE_FILE, flavor="", description="The resultant compiled table", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	public static Vector<String> allTables = null;

	@Override
	public boolean run(JEXEntry optionalEntry)
	{		
		if(allTables == null)
		{
			allTables = new Vector<String>();
		}

		// Validate the input data
		if(fileData == null || !fileData.getTypeName().getType().equals(JEXData.FILE))	
		{
			return false;
		}

		// Gather all the files to combine
		TreeMap<DimensionMap,String> tables = FileReader.readObjectToFilePathTable(fileData);
		for (DimensionMap map : tables.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			allTables.add(tables.get(map));
		}

		// Return status
		return true;
	}

	public void finalizeTicket(Ticket ticket)
	{
		double count = 0;
		double total = allTables.size();
		if(firstOnly)
		{
			total = total + 1;
		}
		else
		{
			total = total + ticket.getOutputList().size();
		}
		double percentage = 0;

		if(allTables == null || allTables.size() == 0)
		{
			return;
		}

		// For each file, read it into R
		R.initializeWorkspace();
		R.load("data.table");
		if(OS.isFamilyWindows())
		{
			R.eval("memory.limit(size = " + memoryLimit + ")");
		}
		R.eval("tableList <- list()");
		int i = 1;
		for(String path : allTables)
		{
			R.eval("tableList[['" + i + "']] <- fread(input=" + R.quotedPath(path) + ", integer64='double')");
			count = count + 1;
			percentage = 100 * count / total;
			JEXStatics.statusBar.setProgressPercentage((int) percentage);

			i = i + 1;
		}
		R.eval("x <- rbindlist(tableList, use.names=TRUE)");

		count = count + 1;
		percentage = 100 * count / total;
		JEXStatics.statusBar.setProgressPercentage((int) percentage);

		// Write the file and make a JEXData
		// Put the final JEXData in all the entries
		TreeMap<JEXEntry,Set<JEXData>> outputList = ticket.getOutputList();
		if(firstOnly)
		{
			JEXEntry first = null;
			for(JEXEntry entry : outputList.keySet())
			{
				if(first == null)
				{
					first = entry;
				}
				if(entry.getTrayX() == first.getTrayX())
				{
					if(entry.getTrayY() < first.getTrayY())
					{
						first = entry;
					}
				}
				else if(entry.getTrayX() < first.getTrayX())
				{
					first = entry;
				}
			}
			String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath(JEXTableWriter.CSV_FILE);
			R.eval("write.csv(x, file=" + R.quotedPath(path) + ", row.names=FALSE)");
			JEXData data = FileWriter.makeFileObject(ticket.getOutputNames()[0].getName(),null, path);
			Set<JEXData> set = outputList.get(first);
			set.clear();
			set.add(data);
		}
		else
		{
			for (JEXEntry entry : outputList.keySet())
			{
				String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath(JEXTableWriter.CSV_FILE);
				R.eval("write.csv(x, file=" + R.quotedPath(path) + ", row.names=FALSE)");
				JEXData data = FileWriter.makeFileObject(ticket.getOutputNames()[0].getName(),null, path);
				Set<JEXData> set = outputList.get(entry);
				set.clear();
				set.add(data);

				count = count + 1;
				percentage = 100 * count / total;
				JEXStatics.statusBar.setProgressPercentage((int) percentage);
			}
		}
		R.eval("rm(list=ls())");

		// Remember to reset the static variable again so this data isn't carried over to other function runs
		allTables = null;
	}
}