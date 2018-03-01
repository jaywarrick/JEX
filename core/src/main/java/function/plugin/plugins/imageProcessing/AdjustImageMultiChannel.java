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
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.StringUtility;
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
		name="Adjust Image Intensities (Multi-Channel)",
		menuPath="Image Processing",
		visible=true,
		description="Adjust defined intensities in the original image to be new defined intensities, scaling all other intensities accordingly."
		)
public class AdjustImageMultiChannel extends JEXPlugin {

	public AdjustImageMultiChannel()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Channel Dim Name", description="Name of the 'Channel' dimension for this image object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String channelDimName;
	
	@ParameterMarker(uiOrder=2, name="Old Min", description="Current 'min' to be mapped to new min value. Comma separated list of image intensity values or one value to apply to all channels.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	String oldMins;
	
	@ParameterMarker(uiOrder=3, name="Old Max", description="Current 'max' to be mapped to new max value. Comma separated list of image intensity values or one value to apply to all channels.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4095.0")
	String oldMaxs;
	
	@ParameterMarker(uiOrder=4, name="New Min", description="New value for current 'min'. Comma separated list of image intensity values or one value to apply to all channels.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	String newMins;
	
	@ParameterMarker(uiOrder=5, name="New Max", description="New value for current 'max'. Comma separated list of image intensity values or one value to apply to all channels.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="65535.0")
	String newMaxs;
	
	@ParameterMarker(uiOrder=6, name="Gamma", description="0.1-5.0, value of 1 results in no change. Comma separated list of values or one value to apply to all channels.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	String gammas;
	
	@ParameterMarker(uiOrder=7, name="Output Bit Depth", description="Depth of the outputted image for all channels.", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	int bitDepth;
	
	@ParameterMarker(uiOrder=8, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;
	
	@ParameterMarker(uiOrder=9, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean keepExcluded;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Adjusted Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData output;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry entry)
	{
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		if(oldMins == null || oldMins.equals("") || oldMaxs == null || oldMaxs.equals("") || newMins == null || newMins.equals("") || newMaxs == null || newMaxs.equals(""))
		{
			JEXDialog.messageDialog("All intensity values must be specified. None can be left blank. Aborting.");
			return false;
		}
		
		if(imageData.getDimTable().getDimWithName(channelDimName) == null)
		{
			JEXDialog.messageDialog("A dimension with the name: " + channelDimName + "' does not exist in '" + imageData.getTypeName().getName() + "' in Entry: X" + entry.getTrayX() + " Y" + entry.getTrayY() + ". Aborting.");
			return false;
		}
		
		try
		{
			TreeMap<DimensionMap,Double> oldMinsMap = StringUtility.getCSVStringAsDoubleTreeMapForDimTable(channelDimName + "=" + oldMins, imageData.getDimTable());
			TreeMap<DimensionMap,Double> oldMaxsMap = StringUtility.getCSVStringAsDoubleTreeMapForDimTable(channelDimName + "=" + oldMaxs, imageData.getDimTable());
			TreeMap<DimensionMap,Double> newMinsMap = StringUtility.getCSVStringAsDoubleTreeMapForDimTable(channelDimName + "=" + newMins, imageData.getDimTable());
			TreeMap<DimensionMap,Double> newMaxsMap = StringUtility.getCSVStringAsDoubleTreeMapForDimTable(channelDimName + "=" + newMaxs, imageData.getDimTable());
			TreeMap<DimensionMap,Double> gammasMap = StringUtility.getCSVStringAsDoubleTreeMapForDimTable(channelDimName + "=" + gammas, imageData.getDimTable());
			
			if(oldMinsMap == null || oldMaxsMap == null || newMinsMap == null || newMaxsMap == null || gammasMap == null)
			{
				return false;
			}
			
			DimTable filterTable = new DimTable(this.filterDimTableString);
			
			// Run the function
			TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
			TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
			int count = 0, percentage = 0;
			String tempPath;
			for (DimensionMap map : imageMap.keySet())
			{
				if(filterTable.testMapAsExclusionFilter(map))
				{
					if(this.keepExcluded)
					{
						Logs.log("Skipping the processing of " + map.toString(), this);
						ImagePlus out = new ImagePlus(imageMap.get(map));
						tempPath = JEXWriter.saveImage(out);
						if(tempPath != null)
						{
							outputImageMap.put(map, tempPath);
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
				// Check if canceled
				if(this.isCanceled())
				{
					return false;
				}
				
				// Call helper method
				tempPath = saveAdjustedImage(imageMap.get(map), oldMinsMap.get(map), oldMaxsMap.get(map), newMinsMap.get(map), newMaxsMap.get(map), gammasMap.get(map), bitDepth);
				if(tempPath != null)
				{
					outputImageMap.put(map, tempPath);
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
		catch(NumberFormatException e)
		{
			JEXDialog.messageDialog("Couldn't parse one of the number values provided.");
			e.printStackTrace();
			return false;
		}		
	}
	
	//	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	//	{
	//		// Get image data
	//		File f = new File(imagePath);
	//		if(!f.exists())
	//		{
	//			return null;
	//		}
	//		ImagePlus im = new ImagePlus(imagePath);
	//		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
	//		
	//		// Adjust the image
	//		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
	//		
	//		// Save the results
	//		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
	//		String imPath = JEXWriter.saveImage(toSave);
	//		im.flush();
	//		
	//		// return the filepath
	//		return imPath;
	//	}
}
