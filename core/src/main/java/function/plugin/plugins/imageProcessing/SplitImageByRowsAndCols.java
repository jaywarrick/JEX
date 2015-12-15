package function.plugin.plugins.imageProcessing;

import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.io.File;
import java.util.Map.Entry;
import java.util.TreeMap;

import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
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
import function.plugin.plugins.Import.ImportImages_SCIFIO;

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
		name="Split Image by Rows and Cols",
		menuPath="Image Processing",
		visible=true,
		description="Divide up the original image by rows and columns into separate images."
		)
public class SplitImageByRowsAndCols extends JEXPlugin {

	public SplitImageByRowsAndCols()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Rows", description="Image Intensity Value", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	int rows;

	@ParameterMarker(uiOrder=3, name="New Row Dim Name", description="Image Intensity Value", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	String rowName;

	@ParameterMarker(uiOrder=2, name="Cols", description="Image Intensity Value", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4095.0")
	int cols;

	@ParameterMarker(uiOrder=3, name="New Col Dim Name", description="Image Intensity Value", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	String colName;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Split Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant split images", enabled=true)
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
		if(!JEXPlugin.isInputValid(imageData, JEXData.IMAGE))
		{
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		for (DimensionMap map : imageMap.keySet())
		{
			if(!(new File(imageMap.get(map))).exists())
			{
				JEXDialog.messageDialog("Couldn't find image at: " + imageMap.get(map) + ". Continuing upon acceptance of this message.");
				continue;
			}
			ImagePlus im = new ImagePlus(imageMap.get(map));
			

			// For each image split it if necessary
			if(rows * cols > 1)
			{
				TreeMap<DimensionMap,ImageProcessor> splitImages = ImportImages_SCIFIO.splitRowsAndCols(im.getProcessor(), rows, cols, rowName, colName, this);
				// The above might return null because of being canceled. Catch cancel condition and move on.
				if(this.isCanceled())
				{
					return false;
				}
				DimensionMap newMap = map.copy();
				for(Entry<DimensionMap,ImageProcessor> e : splitImages.entrySet())
				{
					String filename = JEXWriter.saveImage(e.getValue());
					newMap.putAll(e.getKey());
					outputImageMap.put(newMap.copy(),filename);
					Logs.log(newMap.toString() + " :: " + filename, this);
				}
				splitImages.clear();
			}
			else
			{
				JEXDialog.messageDialog("User must select more than 1 row or col to split the image by, currently set to 1 row and 1 column.");
				return false;
			}

			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(outputImageMap.size() == 0)
		{
			return false;
		}

		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);

		// Return status
		return true;
	}
}
