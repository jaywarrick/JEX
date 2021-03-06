package function.plugin.plugins.imageProcessing;

import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.OptimizationData;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.plugins.featureExtraction.FeatureUtils;
import ij.ImagePlus;
import ij.plugin.filter.GaussianBlur;
import ij.process.ImageProcessor;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.StatisticsUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableWriter;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Unmixing Parameter Extraction",
		menuPath="Image Processing",
		visible=true,
		description="Find parameters for subtracting bleed-through from background channels into another. Final = Signal - alpha1*bg1 - alpha2*bg2 ... - beta. Typically do BG subtraction first, then run. Can subtract bleed-through from multiple channels."
		)
public class UnmixingParameterExtraction extends JEXPlugin implements Comparator<PointValuePair> {
	
	public UnmixingParameterExtraction()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Multichannel Images", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	@InputMarker(uiOrder=2, name="ROI Region to Sample (optional)", type=MarkerConstants.TYPE_ROI, description="Region of image the to use for fitting the parameters.", optional=true)
	JEXData roiData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Penalty Factor, A", description="A only penalizes negative intensities. For x < 0: Penalty = A*(abs(x)+abs(x)^B). For x > 0: Penalty = (abs(x)+abs(x)^B) (i.e., )", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10")
	double factorA;
	
	@ParameterMarker(uiOrder=2, name="Penalty Exponent, B", description="B penalizes both positive and negative intensities. For x < 0: Penalty = A*(abs(x)+abs(x)^B). For x > 0: Penalty = (abs(x)+abs(x)^B)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.5")
	double factorB;
	
	@ParameterMarker(uiOrder=3, name="Channel Dim Name", description="Which dimension represents the channel dimension in the dataset?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String channelDimName;
	
	@ParameterMarker(uiOrder=4, name="Signal Channel", description="Which channel has the desired signal?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String signalValue;
	
	@ParameterMarker(uiOrder=5, name="Bleeding Channels", description="Which channels need to be subtracted from the signal channel? Enter is as CSV list. (Usually shorter wavelength channels bleed into longer wavelength channels)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String bgValues;
	
	@ParameterMarker(uiOrder=6, name="Pixel Sample Size", description="Which channels need to be subtracted from the signal channel? Enter is as CSV list. (Usually shorter wavelength channels bleed into longer wavelength channels)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="3000")
	int pixels;
	
	@ParameterMarker(uiOrder=7, name="Calculate N Times", description="If desired, one can calculate the unmixing parameters for all images (default, 0) or for just N sets of the multi-channel images (enter number here).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	int N;
	
	@ParameterMarker(uiOrder=8, name="Presmoothing Radius", description="If desired, presmooth the image before calculating the parameters (reducing the influence of noise). (Use 0 to skip step).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
	double radius;
	
	@ParameterMarker(uiOrder=9, name="Report Median Results", description="If desired, summarize results by channel using the median of results.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean summarize;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Unmixing Parameters", type=MarkerConstants.TYPE_FILE, flavor="", description="Table of unmixing parameters (Alphas and a Beta) where Final = Signal - Alpha1*bg1 - Alpha2*bg2 ... - Beta", enabled=true)
	JEXData outputData;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}
	
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		FeatureUtils fu = new FeatureUtils();
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,Double> outputParams = new TreeMap<DimensionMap,Double>();
		
		DimTable itrDT = imageData.getDimTable().getSubTable(channelDimName);
		int total = Math.min(itrDT.mapCount(), this.N);
		Dim channelDim = imageData.getDimTable().getDimWithName(channelDimName);
		
		// Get Channel Names to work with
		if(!channelDim.dimValueSet.contains(this.signalValue))
		{
			JEXDialog.messageDialog("Channel name '" + this.signalValue + "' not found. Aborting.");
			return false;
		}
		CSVList bgNames = new CSVList(this.bgValues);
		for(String bgName : bgNames)
		{
			if(!channelDim.dimValueSet.contains(bgName))
			{
				JEXDialog.messageDialog("Channel name '" + bgName + "' not found. Aborting.");
				return false;
			}
		}
		ImageProcessor[] impa = new ImageProcessor[1 + bgNames.size()];
		
		ROIPlus roi = null;
		int count = 0;
		for(DimensionMap map : itrDT.getMapIterator())
		{
			if(this.N > 0 && count >= this.N)
			{
				continue;
			}
			if(this.isCanceled())
			{
				return false;
			}
			
			roi = roiMap.get(map.copyAndSet(channelDimName + "=" + this.signalValue));
			impa[0] = new ImagePlus(imageMap.get(map.copyAndSet(channelDimName + "=" + this.signalValue))).getProcessor();
			for(int i = 0; i < bgNames.size(); i++)
			{
				impa[i+1] = new ImagePlus(imageMap.get(map.copyAndSet(channelDimName + "=" + bgNames.get(i)))).getProcessor();
				if(this.radius > 0)
				{
					// //// Begin Actual Function
					GaussianBlur gb = new GaussianBlur();
					gb.blurGaussian(impa[i+1], radius, radius, 0.0002); // Default accuracy = 0.0002
				}
			}
			
			if(this.isCanceled())
			{
				return false;
			}

			Vector<Vector<Double>> samples = fu.hexagonallySampleNFromImages(roi, true, this.pixels, impa);
			
			SimplexOptimizer optimizer = new SimplexOptimizer(1e-5, 1e-5);
			try
			{
				PointValuePair optimum = optimizer.optimize(new MaxEval(1000),
                        new ObjectiveFunction(new ErrorFunction(this.factorA, this.factorB, samples)),
                        GoalType.MINIMIZE,
                        new InitialGuess(new double[1+bgNames.size()]),
                        new NelderMeadSimplex(1+bgNames.size()));
				for(int i = 0; i < bgNames.size(); i++)
		        {
		        		outputParams.put(map.copyAndSet(channelDimName + "=" + bgNames.get(i)), optimum.getPointRef()[i]);
		        }
		        outputParams.put(map.copyAndSet(channelDimName + "=Beta"), optimum.getPointRef()[bgNames.size()]);
		        
			}
			catch(TooManyEvaluationsException e)
			{
				for(int i = 0; i < bgNames.size(); i++)
		        {
		        		outputParams.put(map.copyAndSet(channelDimName + "=" + bgNames.get(i)), Double.NaN);
		        }
		        outputParams.put(map.copyAndSet(channelDimName + "=Beta"), Double.NaN);
				continue;
			}
	        
			Logs.log("Finished processing " + count + " of " + total + ".", 1, this);
			count++;
			
			// Status bar
			int percentage = (int) (100 * ((double) count / (double) total));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		
		if(this.summarize)
		{
			TreeMap<DimensionMap,Double> newOutput = new TreeMap<>();
			// find the median of the parameter values.
			for(String bg : bgNames)
			{
				Double median = StatisticsUtility.median(Table.getFilteredData(outputParams, channelDimName + "=" + bg));
				newOutput.put(new DimensionMap(channelDimName + "=" + bg), median);
			}
			Double median = StatisticsUtility.median(Table.getFilteredData(outputParams, channelDimName + "=Beta"));
			newOutput.put(new DimensionMap(channelDimName + "=Beta"), median);
			outputParams = newOutput;
		}
		
		// Set the outputs
		String path = JEXTableWriter.writeTable("Correction Parameters", outputParams);
		this.outputData = FileWriter.makeFileObject("temp", null, path);
		
		// Return status
		return true;
	}

	@Override
	public int compare(PointValuePair o1, PointValuePair o2)
	{
		return(o1.getValue().compareTo(o2.getValue()));
	}
}

class ErrorFunction implements MultivariateFunction, OptimizationData
{
	
	Vector<double[]> data;
	double factorA = 1, factorB=1;
	int samples = 0;

	public ErrorFunction(double factorA, double factorB, Vector<Vector<Double>> data)
	{
		this.factorA = factorA;
		this.factorB = factorB;
		
		// Load the data
		this.data = new Vector<>(data.size());
		for(int i = 0; i < data.size(); i++)
		{
			double[] da = new double[data.get(i).size()];
			Vector<Double> v = data.get(i);
			for(int j = 0; j < v.size(); j++)
			{
				da[j] = v.get(j);
			}
			this.data.add(da);
		}
		
		// Save the sample size
		this.samples = this.data.get(0).length;
	}
	
	/**
	 * Within point, we will have the parameters needed to fit the data.
	 */
	@Override
	public double value(double[] point)
	{
		double ret = 0;
		double[] ts = this.getTrueSignal(point);
		for(int i = 0; i < samples; i++)
		{
			ret = ret + this.getHeavisideWeights(ts[i]);
		}
		return ret;
	}
	
	private double getHeavisideWeights(double x)
	{
		if(x < 0)
		{
			return(this.factorA*(-x + Math.pow(-x,this.factorB)));
		}
		else
		{
			return(x+Math.pow(x, this.factorB));
		}
		//return Math.abs(x*(1+(this.penalty-1)/(1+Math.exp(-2*this.penalty*(-1*x-(1)/(2*this.penalty)*Math.log((this.penalty-1)/(1.1-1)-1) )))));
	}
	
	public double[] getTrueSignal(double[] point)
	{
		double[] ret = Arrays.copyOf(this.data.get(0), this.samples);
		for(int i=1; i < data.size(); i++)
		{
			// Just subtract off the fraction of this background.
			for(int j=0; j < ret.length; j++)
			{
				ret[j] = ret[j] - point[i-1]*this.data.get(i)[j];
			}
		}
		// then do the subtraction of the offset term (the last term in point)
		for(int j=0; j < this.samples; j++)
		{
			ret[j] = ret[j] + point[point.length-1];
		}
		return ret;
	}
}
