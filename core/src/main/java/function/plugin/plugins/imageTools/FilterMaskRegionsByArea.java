package function.plugin.plugins.imageTools;

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
import function.plugin.plugins.featureExtraction.FeatureUtils;
import jex.statics.JEXStatics;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
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
		name="Filter Mask Regions by Area",
		menuPath="Image Tools",
		visible=true,
		description="Remove regions from the black and white mask that are greater or less than (inclusive) than the specified pixel area."
		)
public class FilterMaskRegionsByArea extends JEXPlugin {

	public FilterMaskRegionsByArea()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Mask Image", type=MarkerConstants.TYPE_IMAGE, description="Mask image to be adjusted (blobs are white on black).", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Color of Regions to Filter", description="Which color regions should be removed from the black and white image.", ui=MarkerConstants.UI_DROPDOWN, choices={"White","Black"}, defaultChoice=0)
	String colorToFilter;
	
	@ParameterMarker(uiOrder=2, name="Keep or Remove Filtered Regions", description="Should the regions that fit the filter be kept or removed?", ui=MarkerConstants.UI_DROPDOWN, choices={"Remove","Keep"}, defaultChoice=0)
	String keepOrRemove;
	
	@ParameterMarker(uiOrder=3, name="Min Area (pixels, inclusive)", description="The minimum pixel area of a blob to be kept [inclusive].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	int minSize;
	
	@ParameterMarker(uiOrder=4, name="Max Area (pixels, inclusive)", description="The minimum pixel area of a blob to be kept [inclusive, if(value <= 0) then(value = Integer.MAX_VALUE)].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	int maxSize;
	
	@ParameterMarker(uiOrder=5, name="Connectedness", description="Whether to parse 4-connected or 8-connected pixel regions.", ui=MarkerConstants.UI_DROPDOWN, choices={"4-Connected", "8-Connected"})
	String connectedness;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Filtered Mask Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData output;
	
	private FeatureUtils utils = new FeatureUtils();
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		boolean filterWhite = true;
		if(colorToFilter.equals("Black"))
		{
			filterWhite = false;
		}
		
		boolean keep = false;
		if(keepOrRemove.equals("Keep"))
		{
			keep = true;
		}
		
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		boolean fourConnected = connectedness.equals("4-Connected");
		
		if(maxSize <= 0)
		{
			maxSize = Integer.MAX_VALUE;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		for (DimensionMap map : imageMap.keySet())
		{
			// Call helper method
			Img<UnsignedByteType> tempMask = utils.filterMaskRegions(imageMap.get(map), (int) minSize, (int) maxSize, fourConnected, filterWhite, keep);
			if(tempMask != null)
			{
				tempPath = JEXWriter.saveImage(tempMask);
				if(tempPath != null)
				{
					outputImageMap.put(map, tempPath);
				}
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
