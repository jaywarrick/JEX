package function.plugin.plugins.imageProcessing;

import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import jex.statics.JEXStatics;
import logs.Logs;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

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
		name="Find Edges",
		menuPath="Image Processing",
		visible=true,
		description="Output an image highlighting the edges in the source image using ImageJ's sobel filter."
		)
public class FindEdges extends JEXPlugin {

	public FindEdges()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be processed.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Invert image before?", description="Inverts the LUT before proceeding to the edge detection?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean invertBefore;

	@ParameterMarker(uiOrder=2, name="Invert image after?", description="Inverts the LUT afteredge detection?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean invertAfter;

	@ParameterMarker(uiOrder=3, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;

	@ParameterMarker(uiOrder=4, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean keepExcluded;

	@ParameterMarker(uiOrder=5, name="Output Bit Depth", description="What bit depth should the output be saved as.", ui=MarkerConstants.UI_DROPDOWN, choices={"8","16","32"}, defaultChoice=2)
	int outputBitDepth;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Edges", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant sobel filtered image", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	Dim filterDim = null;

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}

		DimTable filterTable = new DimTable(this.filterDimTableString);

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(this.imageData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();

		int count = 0;
		int total = imageMap.size();
		int percentage = 0;
		JEXStatics.statusBar.setProgressPercentage(0);
		String tempPath = null;
		for (DimensionMap map : imageMap.keySet())
		{
			// Check if canceled
			if(this.isCanceled())
			{
				return false;
			}
			
			if(filterTable.testMapAsExclusionFilter(map))
			{
				// Then the image fits the exclusion filter
				if(this.keepExcluded)
				{
					Logs.log("Skipping the processing of " + map.toString(), this);
					ImagePlus out = new ImagePlus(imageMap.get(map));
					tempPath = JEXWriter.saveImage(out); // Don't convert the bitDepth of this output since it is "not be in processed".
					if(tempPath != null)
					{
						outputMap.put(map, tempPath);
					}
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
				else
				{
					Logs.log("Skipping the processing and saving of " + map.toString(), this);
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
				continue;
			}

			String path = imageMap.get(map);

			// get the image
			ImagePlus im = new ImagePlus(path);
			FloatProcessor fp = (FloatProcessor) im.getProcessor().convertToFloat(); 

			// invert if needed
			if(this.invertBefore)
				fp.invertLut();

			// Do the edge detection
			fp.findEdges();

			// Do the invert if needed
			if(this.invertAfter)
				fp.invertLut();

			// //// Save the results
			String finalPath = JEXWriter.saveImage(fp, this.outputBitDepth);
			outputMap.put(map.copy(), finalPath);
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;

			// Status bar
			percentage = (int) (100 * ((double) count / (double) imageMap.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}

		// Set the outputs
		this.output = ImageWriter.makeImageStackFromPaths("temp", outputMap);
		output.setDataObjectInfo("Edge detected using the Edge detection Function");

		// Return status
		return true;
	}
}
