package function.plugin.plugins.microwell;

import java.util.TreeMap;
import java.util.Vector;

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
import function.singleCellAnalysis.MicrowellTools;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
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
		name="Microwell Convolver",
		menuPath="Microwells",
		visible=true,
		description="Convolve the image with kernels to create spots for detecting microwells with the 'Microwell Finder' function."
		)
public class MicrowellConvolver extends JEXPlugin {

	public MicrowellConvolver()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Test Run?", description="Just run this for the first image in each well?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean isTest;
	
	@ParameterMarker(uiOrder=2, name="Channel Dim NAME (optional)", description="Name of the 'Channel' dimension. (leave blank if no channel dim)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String colorDimName;
	
	@ParameterMarker(uiOrder=3, name="Channel Dim VALUE", description="Value of the 'Time' dimension to convolve (leave blank to process all)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String colorVal;
	
	@ParameterMarker(uiOrder=4, name="Time Dim NAME (optional)", description="Name of the 'Time' dimension. (leave blank if no time dim)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String timeDimName;
	
	@ParameterMarker(uiOrder=5, name="Time Dim VALUE", description="Value of the 'Time' dimension to convolve (leave blank to process all)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String timeVal;
	
	@ParameterMarker(uiOrder=6, name="Kernel Smoothing Radius", description="Radius of gaussian filtering to apply to the image and kernel prior to convolving. (Set to 0 to skip) (Can be helpful for both BF and Fluor)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double radius;
	
	@ParameterMarker(uiOrder=7, name="Edge Prefilter?", description="Apply edge filter prior to convolution? (Typically needed for Fluor only)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean edgeFilter;
	
	@ParameterMarker(uiOrder=8, name="Kernel Outer Radius (white)", description="The half width/height of the whole kernel (white).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="24")
	int outerRad;
	
	@ParameterMarker(uiOrder=9, name="Kernel Inner Radius (black)", description="The half width/height of the black box in the center of the kernel.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="11")
	int innerRad;
	
	@ParameterMarker(uiOrder=10, name="Invert Kernel?", description="Whether to invert the colors of the kernel. (Typically false for edgefiltered images and true for finding dark edges (e.g., in BF))", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean inverted;
	
	@ParameterMarker(uiOrder=11, name="Normalize?", description="Normalize the image. (Valueable for seeing results, but can add variability to microwell finding, so typically false for final use.)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean norm;
	
	@ParameterMarker(uiOrder=12, name="Output Bit Depth", description="Depth of the resultant image.", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=2)
	int depth;
	
	@ParameterMarker(uiOrder=13, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Kernel Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The kernel being used for convolution.", enabled=true)
	JEXData imageKernel;
	
	@OutputMarker(uiOrder=2, name="Gaussian Filtered Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The image after gaussian filtering.", enabled=true)
	JEXData imageRadiusFiltered;
	
	@OutputMarker(uiOrder=3, name="Edge Filtered Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The image after edge filtering.", enabled=true)
	JEXData imageEdgeFiltered;
	
	@OutputMarker(uiOrder=4, name="Convolved Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant convolved image", enabled=true)
	JEXData imageConvolved;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry entry)
	{
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		DimTable filterTable = new DimTable(this.filterDimTableString);
				
		// Get the sub-DimTable of only the BF images at one timepoint for convolving.
		DimTable imageTable = imageData.getDimTable();
		DimTable subTable = null;
		if(imageTable.getDimWithName(timeDimName) == null)
		{
			if(imageTable.getDimWithName(colorDimName) == null)
			{
				subTable = imageTable.copy();
			}
			else
			{
				subTable = imageTable.getSubTable(new DimensionMap(colorDimName + "=" + colorVal));
			}
		}
		else
		{
			if(imageTable.getDimWithName(colorDimName) == null)
			{
				subTable = imageTable.getSubTable(new DimensionMap(timeDimName + "=" + timeVal));
			}
			else
			{
				subTable = imageTable.getSubTable(new DimensionMap(colorDimName + "=" + colorVal + "," + timeDimName + "=" + timeVal));
			}
		}
		
		// Run the function
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		String kernelImage = null;
		String radiusFilteredImage = null;
		String edgeFilteredImage = null;
		String convolutionImage = null;
		
		int count = 0;
		int total = subTable.mapCount();
		FloatProcessor kernel = MicrowellTools.makeKernel(this.outerRad, this.innerRad, this.inverted, true, this.radius);
		kernel.resetMinAndMax();
		JEXStatics.statusBar.setProgressPercentage(0);
		for (DimensionMap map : subTable.getMapIterator())
		{
			if(filterTable.testMapAsExclusionFilter(map))
			{
				Logs.log("Skipping the processing and saving of " + map.toString(), this);
				count = count + 1;
				int percentage = (int) (100 * ((double) (count) / ((double) images.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				continue;
			}
			
			if(this.isCanceled())
			{
				return false;
			}
			
			// Get the image
			String path = images.get(map);
			FloatProcessor imp = (FloatProcessor) (new ImagePlus(path)).getProcessor().convertToFloat();
			
			// Convolve the image
			Vector<FloatProcessor> results = MicrowellTools.filterAndConvolve(imp, this.radius, this.edgeFilter, kernel, this.isTest);
			
			// Save the image
			ImagePlus im = FunctionUtility.makeImageToSave(imp, this.norm, 1, this.depth, false);
			String finalPath = JEXWriter.saveImage(im);
			DimensionMap outMap = map.copy();
			if(!colorDimName.equals(""))
			{
				outMap.remove(colorDimName);
			}
			if(!timeDimName.equals(""))
			{
				outMap.remove(timeDimName);
			}
			
			outputMap.put(outMap, finalPath);
			
			if(isTest)
			{
				Vector<String> paths = new Vector<String>();
				for (FloatProcessor fp : results)
				{
					String temp = JEXWriter.saveImage(fp);
					if(temp != null)
					{
						paths.add(temp);
						// (new ImagePlus(temp, fp)).show();
					}
					else
					{
						// (new ImagePlus("", fp)).show();
						paths.add("");
					}
				}
				convolutionImage = finalPath;
				kernelImage = paths.get(0);
				radiusFilteredImage = paths.get(1);
				edgeFilteredImage = paths.get(2);
				Logs.log("Finished Convolution Test", this);
				JEXStatics.statusBar.setProgressPercentage(100);
				break;
			}
			
			// Update the status
			count++;
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			int percentage = (int) (100 * ((double) count / (double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		// Set the outputs
		if(isTest)
		{
			this.imageKernel = ImageWriter.makeImageObject("duh", kernelImage);
			this.imageRadiusFiltered = ImageWriter.makeImageObject("duh", radiusFilteredImage);
			this.imageEdgeFiltered = ImageWriter.makeImageObject("duh", edgeFilteredImage);
		}
		
		this.imageConvolved = ImageWriter.makeImageStackFromPaths("duh", outputMap);
		
		// Return status
		return true;
	}
}
