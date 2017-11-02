package function.plugin.plugins.imageProcessing;

import java.io.File;
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
import function.plugin.plugins.medianFilterHelpers.FastMedian;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
import miscellaneous.CSVList;
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
		name="Fast Median Background Subtraction",
		menuPath="Image Processing",
		visible=true,
		description="Fast median filter background subtraction that uses a square shaped kernal instead of round like ImageJ's filters. (~> 20 times faster)"
		)
public class FastMedianBackgroundSubtract extends JEXPlugin {

	public FastMedianBackgroundSubtract()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Kernal Width", description="Pixel width of the kernal. If > 100, then probably use the 'Super Fast Median Filter' instead.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5")
	int kernalWidth;
	
	@ParameterMarker(uiOrder=2, name="Nominal Value to Add Back", description="Nominal value to add to all pixels after background subtraction because some image formats don't allow negative numbers. (Use following notation to specify different parameters for differen dimension values, '<Dim Name>'=<val1>,<val2>,<val3>' e.g., 'Channel=0,100,100'. The values will be applied in that order for the ordered dim values.) ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	String nominal;
	
	@ParameterMarker(uiOrder=3, name="Exclusion Filter DimTable", description="Exclude combinatoins of Dimension Names and values. (Use following notation '<DimName1>=<a1,a2,...>;<DimName2>=<b1,b2,...>' e.g., 'Channel=0,100,100; Time=1,2,3,4,5' (spaces are ok).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String exclusionFilterString;
	
	@ParameterMarker(uiOrder=4, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
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
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Get the nominal add back values (typically different for fluorescence and brightfield
		TreeMap<DimensionMap,Double> nominals = getNominals();
		if(nominals == null)
		{
			return false;
		}
		
		DimTable filterTable = new DimTable(this.exclusionFilterString);
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		for (DimensionMap map : imageMap.keySet())
		{
			if(filterTable.testDimensionMapUsingDimTableAsFilter(map))
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
					
				}
				else
				{
					Logs.log("Skipping the processing and saving of " + map.toString(), this);
				}
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				continue;
			}
			if(this.isCanceled())
			{
				Logs.log("Function canceled.", this);
				return false;
			}
			// Call helper method
			
			FloatProcessor fp = null;
			ImageProcessor ip = (new ImagePlus(imageMap.get(map))).getProcessor();
			ImageProcessor ip2 = FastMedian.process(ip, kernalWidth);
			int bitDepth = ip.getBitDepth();
			fp = (FloatProcessor) ip.convertToFloat();
			ip2 = ip2.convertToFloat();
			double nominalD = nominals.get(map);
			ip2.subtract(nominalD);
			fp.copyBits(ip2, 0, 0, Blitter.SUBTRACT);
			ImagePlus out = FunctionUtility.makeImageToSave(fp, "false", bitDepth);
			tempPath = JEXWriter.saveImage(out);
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
	
	public TreeMap<DimensionMap,Double> getNominals()
	{
		TreeMap<DimensionMap, Double> ret = new TreeMap<>();
		Dim nominalDim = null;
		Double nominalD = 0.0;
		try
		{
			nominalD = Double.parseDouble(nominal);
			ret.put(new DimensionMap(), nominalD);
			return ret;
		}
		catch(Exception e)
		{
			String[] temp = nominal.split("=");
			if(temp.length != 2)
			{
				JEXDialog.messageDialog("Couldn't parse the parameter '" + nominal + "' into a number or into format '<Dim Name>=<val1>,<val2>...'. Aborting.", this);
				return null;
			}
			nominalDim = imageData.getDimTable().getDimWithName(temp[0]);
			CSVList temp2 = new CSVList(temp[1]);
			if(nominalDim.size() != 1 && nominalDim.size() != temp2.size())
			{
				JEXDialog.messageDialog("The number of nominal addback values did not match the number of values in the dimension '" + nominalDim.dimName + "'. Aborting.", this);
				return null;
			}
			for(int i=0; i < temp2.size(); i++)
			{
				try
				{
					Double toGet = Double.parseDouble(temp2.get(i));
					ret.put(new DimensionMap(nominalDim.dimName + "=" + nominalDim.dimValues.get(i)), toGet);
				}
				catch(Exception e2)
				{
					JEXDialog.messageDialog("Couldn't parse the value '" + temp2.get(i) + "' to type double. Aborting.", this);
					return null;
				}
				
			}
			return ret;
		}
	}
	
	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	{
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
		{
			return null;
		}
		ImagePlus im = new ImagePlus(imagePath);
		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
		
		// Adjust the image
		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
		
		// Save the results
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth);
		String imPath = JEXWriter.saveImage(toSave);
		im.flush();
		
		// return the filepath
		return imPath;
	}
}
