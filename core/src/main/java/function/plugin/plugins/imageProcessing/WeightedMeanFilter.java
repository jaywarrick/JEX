package function.plugin.plugins.imageProcessing;

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
import function.plugin.plugins.featureExtraction.FeatureUtils;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.StatisticsUtility;
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
		name="Weighted Mean Filtering",
		menuPath="Image Processing",
		visible=true,
		description="Calculate a weighted mean using the variance of the image as the inverse of the weight."
		)
public class WeightedMeanFilter extends JEXPlugin {

	public WeightedMeanFilter()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Mean Filter Radius", description="Radius of the weighted mean filter. Should be large enough to encompass features of interest.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50")
	double meanRadius;

	@ParameterMarker(uiOrder=2, name="Variance Filter Radius", description="Radius of the variance filter used to generate the pixel weights. Keep small to preserve edges.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.7")
	double varRadius;
	
	@ParameterMarker(uiOrder=3, name="Weight Scaling Factor", description="Typically 0.5-2 but can increase or decrease to make the weighting more drastic or less drastic, respectively.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2.0")
	double weightScale;

	@ParameterMarker(uiOrder=4, name="Perform Subtraction?", description="Should the filtered image be subtracted from the original?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean subtract;
	
	@ParameterMarker(uiOrder=5, name="Save Weight Image?", description="Should the image weights be saved as a separate image?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean saveWeights;

	@ParameterMarker(uiOrder=6, name="Nominal Value to Add Back", description="Nominal value to add to all pixels after background subtraction because some image formats don't allow negative numbers. (Use following notation to specify different parameters for differen dimension values, '<Dim Name>'=<val1>,<val2>,<val3>' e.g., 'Channel=0,100,100'. The values will be applied in that order for the ordered dim values.) ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	String nominal;

	double nominalVal;

	@ParameterMarker(uiOrder=7, name="Output Bit Depth", description="What bit depth should the output be saved as.", ui=MarkerConstants.UI_DROPDOWN, choices={"8","16","32"}, defaultChoice=1)
	int outputBitDepth;

	@ParameterMarker(uiOrder=8, name="Exclusion Filter DimTable", description="Exclude combinatoins of Dimension Names and values. (Use following notation '<DimName1>=<a1,a2,...>;<DimName2>=<b1,b2,...>' e.g., 'Channel=0,100,100; Time=1,2,3,4,5' (spaces are ok).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String exclusionFilterString;

	@ParameterMarker(uiOrder=9, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean keepExcluded;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Image (BG)", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData output;
	
	@OutputMarker(uiOrder=2, name="Weights", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData weightOutput;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		DimTable filterTable = new DimTable(this.exclusionFilterString);

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

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> outputWeightMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;
		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
				Logs.log("Function canceled.", this);
				return false;
			}

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

			ImagePlus im = new ImagePlus(imageMap.get(map));
			FloatProcessor ret = im.getProcessor().convertToFloatProcessor();
			FloatProcessor w = (FloatProcessor) ret.duplicate();
			FloatProcessor subtracted = null;
			if(subtract)
			{
				subtracted = (FloatProcessor) ret.duplicate();
			}

			// Get the variance filtered image
			RankFilters2 rF = new RankFilters2();
			rF.rank(w, varRadius, RankFilters2.VARIANCE);
			
			// Find the median of the whole WEIGHTS image (w) using a small subsample (use sqrt of pixel variance
			// values (i.e., stdev) because that is what we'll be working with in the next step.
			FeatureUtils utils = new FeatureUtils();
			Vector<Double> sampleOfWeights = utils.hexagonallySampleN(w, null, true, 300);
			double[] temp = new double[sampleOfWeights.size()];
			for(int i = 0; i < temp.length; i++)
			{
				temp[i] = Math.sqrt(sampleOfWeights.get(i));
			}
			
			// Use the adaptive median here for fun but can probably use
			// regular median. The adaptive median just approximates the
			// median of the "majority" of the pixel population more
			// closely (i.e., it approximates mode without bins etc)
			double med = StatisticsUtility.adaptiveMedian(temp);
			
			// Get the mad of the image as a way to appropriately scale the image weights
			double mad = StatisticsUtility.mad(med, temp); // Calculate mad relative to the adaptive med.

			// Debug
			// FileUtility.showImg(w, true);
			
			// Take the square root of the variance to get std dev which is
			// less volatile than variance and is scaled appropriately for the job.
			// Then the weight becomes the difference between the median variance and the actual variance
			// and is scaled by dividing by the mad to essentially go between 0 to 1 for typical bg pixels
			// and  > 1 for non bg pixels.
			// Then we transform the weights using 1/(1+w^scalingFactor). 
			// this approach is better behaved that 1/x^scalingFactor because 
			// a scaled variance of zero computes to 1 instead of infinity.
			// The scaling factor just determines how gradually it transitions from
			// 1 @ scaled variance < 1, to 0 @ at variance > 1. Higher value, harder
			// transition right around the cutoff of 1.
			float[] pixels = (float[]) w.getPixels();
			if(this.weightScale == 1.0)
			{
				for(int i = 0; i < pixels.length; i++)
				{
					pixels[i] = (float) (1.0/(1.0 + (Math.abs(Math.sqrt(pixels[i])-med)/mad ) ));
				}
			}
			else
			{
				for(int i = 0; i < pixels.length; i++)
				{
					pixels[i] = (float) (1.0/(1.0 + Math.pow((Math.abs(Math.sqrt(pixels[i])-med)/mad ), 2) ));
				}
			}
			
			// Save the weighting image to see how filter radii affect weighting.
			if(this.saveWeights)
			{
				String tempWeightPath = JEXWriter.saveImage(w, 32);
				outputWeightMap.put(map, tempWeightPath);
			}
			
			// Multiply the original image by the weights
			ret.copyBits(w, 0, 0, Blitter.MULTIPLY);

			// Evaluate the local sums for w and ret
			rF.rank(w, meanRadius, RankFilters2.SUM);
			rF.rank(ret, meanRadius, RankFilters2.SUM);

			// Perform the final division to get weighted means for each pixel.
			ret.copyBits(w, 0, 0, Blitter.DIVIDE);
			
			// Perform subtraction if desired.
			double nominal = nominals.get(map);
			if(subtract)
			{
				subtracted.copyBits(ret, 0, 0, Blitter.SUBTRACT);
				ret = subtracted;
				if(nominal != 0)
					ret.add(nominal);
			}

			// Save the image
			tempPath = JEXWriter.saveImage(ret, outputBitDepth);
			outputImageMap.put(map, tempPath);
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(outputImageMap.size() == 0)
		{
			return false;
		}

		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);
		
		if(saveWeights)
		{
			this.weightOutput = ImageWriter.makeImageStackFromPaths("temp", outputWeightMap);
		}

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
}
