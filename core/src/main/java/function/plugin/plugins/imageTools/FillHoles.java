package function.plugin.plugins.imageTools;

import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXReader;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.plugins.featureExtraction.FeatureUtils;
import jex.statics.JEXStatics;
import net.imagej.ops.morphology.fillHoles.DefaultFillHoles;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.logic.BitType;
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
		name="Fill Holes",
		menuPath="Image Tools",
		visible=true,
		description="Remove holes in regions of mask."
		)
public class FillHoles extends JEXPlugin {

	public FillHoles()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Mask Image", type=MarkerConstants.TYPE_IMAGE, description="Mask image to be adjusted (blobs are white on black).", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Color of Regions", description="Which color are the regions (black or white).", ui=MarkerConstants.UI_DROPDOWN, choices={"White","Black"}, defaultChoice=0)
	String colorOfRegions;
	
	//	@ParameterMarker(uiOrder=5, name="Connectedness", description="Whether to parse 4-connected or 8-connected pixel regions.", ui=MarkerConstants.UI_DROPDOWN, choices={"4-Connected", "8-Connected"})
	//	String connectedness;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Filled Mask Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData output;
	
	private FeatureUtils utils = new FeatureUtils();
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		DefaultFillHoles<BitType> fh = null;
		boolean regionsAreWhite = true;
		if(colorOfRegions.equals("Black"))
		{
			regionsAreWhite = false;
		}
		
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		//		boolean fourConnected = connectedness.equals("4-Connected");
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
				return false;
			}
			
			// Call helper method
			Img<UnsignedByteType> tempMask = JEXReader.getSingleImage(imageMap.get(map), 0d);
			if(!regionsAreWhite)
			{
				utils.invert(tempMask);
			}
			RandomAccessibleInterval<BitType> tempMask2 = utils.convertRealToBooleanTypeRAI(tempMask, new BitType(true));
			//			utils.show(tempMask);
			if(fh == null)
			{
				fh = IJ2PluginUtility.ij().op().op(DefaultFillHoles.class, (RandomAccessibleInterval<BitType>) null, tempMask2);
			}
			tempMask2 = fh.calculate(tempMask2);
			String path = JEXWriter.saveImage(utils.convertBooleanTypeToByteRAI(tempMask2));
			outputImageMap.put(map, path);
			
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
