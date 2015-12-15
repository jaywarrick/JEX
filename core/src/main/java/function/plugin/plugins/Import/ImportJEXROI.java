package function.plugin.plugins.Import;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.SingleUserDatabase.JEXDataIO;
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
		name="Import JEX ROI",
		menuPath="Import",
		visible=true,
		description="Import a JEX ROI from a file."
		)
public class ImportJEXROI extends JEXPlugin {

	public ImportJEXROI()
	{}

	/////////// Define Inputs ///////////


	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="ROI File", description="ROI file, typically a .jxd file in arff format.", ui=MarkerConstants.UI_FILECHOOSER, defaultText="~/")
	String roiFile;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Imported ROI", type=MarkerConstants.TYPE_ROI, flavor="", description="The resultant imported ROI", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Get the roi
		JEXData data = new JEXData(JEXData.ROI, "temp");
		JEXDataIO.loadJXD(data, roiFile);
		output = data;
		
		return true;
	}
}
