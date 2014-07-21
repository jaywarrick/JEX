package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.Type;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ByteBlitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import miscellaneous.Canceler;
import miscellaneous.Pair;
import miscellaneous.StatisticsUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
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
public class JEX_CTC_SingleCellColocalization extends JEXCrunchable {
	
	public JEX_CTC_SingleCellColocalization()
	{}
	
	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------
	
	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	@Override
	public String getName()
	{
		String result = "CTC Single Cell Colocalization";
		return result;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	@Override
	public String getInfo()
	{
		String result = "Calculate the spatial correlation coefficient between colors in masked/identified regions.";
		return result;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
	@Override
	public String getToolbox()
	{
		String toolbox = "CTC Tools";
		return toolbox;
	}
	
	/**
	 * This method defines if the function appears in the list in JEX It should be set to true expect if you have good reason for it
	 * 
	 * @return true if function shows in JEX
	 */
	@Override
	public boolean showInList()
	{
		return true;
	}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	@Override
	public boolean allowMultithreading()
	{
		return true;
	}
	
	// ----------------------------------------------------
	// --------- INPUT OUTPUT DEFINITIONS -----------------
	// ----------------------------------------------------
	
	/**
	 * Return the array of input names
	 * 
	 * @return array of input names
	 */
	@Override
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[4];
		inputNames[0] = new TypeName(ROI, "Maxima");
		inputNames[1] = new TypeName(IMAGE, "Segmented Image");
		inputNames[2] = new TypeName(IMAGE, "Mask Image");
		inputNames[3] = new TypeName(IMAGE, "Image To Quantify");
		return inputNames;
	}
	
	/**
	 * Return the array of output names defined for this function
	 * 
	 * @return
	 */
	@Override
	public TypeName[] getOutputs()
	{
		this.defaultOutputNames = new TypeName[1];
		this.defaultOutputNames[0] = new TypeName(FILE, "Cell Measurements");
		
		if(this.outputNames == null)
		{
			return this.defaultOutputNames;
		}
		return this.outputNames;
	}
	
	/**
	 * Returns a list of parameters necessary for this function to run... Every parameter is defined as a line in a form that provides the ability to set how it will be displayed to the user and what options are available to choose from The simplest
	 * FormLine can be written as: FormLine p = new FormLine(parameterName); This will provide a text field for the user to input the value of the parameter named parameterName More complex displaying options can be set by consulting the FormLine API
	 * 
	 * @return list of FormLine to create a parameter panel
	 */
	@Override
	public ParameterSet requiredParameters()
	{
		// (ImageProcessor ip, double tolerance, double threshold, int
		// outputType, boolean excludeOnEdges, boolean isEDM, Roi roiArg,
		// boolean lightBackground)
		Parameter p0 = new Parameter("Color Dim Name", "Name of the color dimension.", "Color");
		Parameter p1 = new Parameter("Median Filter Radius", "Radius of the median filter to apply before quantification (Use a value of 0 to avoid applying the filter).", "0");
		Parameter p2 = new Parameter("Artificial BG Level", "Enter the amount that was artificially added to the BG during BG Correction. This will be subtracted before analysis.", "100");
		Parameter p3 = new Parameter("Method", "Which method to use to calculate the colocalization.", Parameter.DROPDOWN, new String[] { "Max-Norm", "Min-Contain" }, 1);
		Parameter p4 = new Parameter("Window Offset", "The offeset between the signal and window when fitting the window under the signal curve using the Min-Contain method only.\nLeave blank to use the artificial bg level.", "");
		// String[] {"true"},0);
		// Parameter pa1 = new Parameter("Color Dim Name", "Name of the color dimension.", "Color");
		// Parameter pa2 = new Parameter("Dummy Parameter", "The type of measurement to perform", Parameter.DROPDOWN, operations, 0);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		// parameterArray.addParameter(pa1);
		// parameterArray.addParameter(pa2);
		
		return parameterArray;
	}
	
	public static String[] binaryMeasures = new String[] { "AREA", "PERIMETER", "CIRCULARITY" };
	public static String[] grayscaleMeasures = new String[] { "MEAN", "MEDIAN", "MIN", "MAX", "SUM", "STDDEV", "VARIANCE" };
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-ridden by returning false If over-ridden ANY batch function using this function will not be able perform error
	 * checking...
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return false;
	}
	
	// ----------------------------------------------------
	// --------- THE ACTUAL MEAT OF THIS FUNCTION ---------
	// ----------------------------------------------------
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		try
		{
			/*
			 * inputNames[2] = new TypeName(IMAGE, "Outer Boundary Image"); inputNames[3] = new TypeName(IMAGE, "Inner Boundary Image"); inputNames[4] = new TypeName(IMAGE, "Image To Quantify");
			 */
			// Collect the inputs
			JEXData maximaData = getInputAs(inputs, "Maxima", ROI);
			if(maximaData == null)
			{
				return false;
			}
			JEXData segData = getInputAs(inputs, "Segmented Image", IMAGE);
			boolean segProvided = true;
			if(segData == null)
			{
				segProvided = false;
			}
			JEXData maskData = getInputAs(inputs, "Mask Image", IMAGE);
			if(maskData == null)
			{
				return false;
			}
			JEXData imageData = getInputAs(inputs, "Image To Quantify", IMAGE);
			if(imageData == null)
			{
				return false;
			}
			
			// Gather parameters
			String colorDimName = this.parameters.getValueOfParameter("Color Dim Name");
			double filterRadius = Double.parseDouble(this.parameters.getValueOfParameter("Median Filter Radius"));
			double bg = Double.parseDouble(this.parameters.getValueOfParameter("Artificial BG Level"));
			String method = this.parameters.getValueOfParameter("Method");
			String offsetString = this.parameters.getValueOfParameter("Window Offset");
			double offset = bg;
			if(!offsetString.equals(""))
			{
				Double.parseDouble(offsetString);
			}
			
			// Set color thresholds
			
			DimTable imageTable = imageData.getDimTable();
			
			TreeMap<DimensionMap,ROIPlus> maximaMap = RoiReader.readObjectToRoiMap(maximaData);
			TreeMap<DimensionMap,String> segMap = ImageReader.readObjectToImagePathTable(segData);
			TreeMap<DimensionMap,String> maskMap = ImageReader.readObjectToImagePathTable(maskData);
			TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
			TreeMap<DimensionMap,Double> results = new TreeMap<DimensionMap,Double>();
			
			Dim colorDim = imageTable.getDimWithName(colorDimName);
			DimTable colorlessTable = imageTable.copy();
			colorlessTable.removeDimWithName(colorDimName);
			Vector<DimensionMap> colorCombos = this.getPossibleCombinations(colorDim);
			double count = 0;
			if(colorlessTable.size() > 0)
			{
				double total = colorlessTable.mapCount() * colorCombos.size();
				for (DimensionMap map : colorlessTable.getMapIterator())
				{
					for (DimensionMap colors : colorCombos)
					{
						if(this.isCanceled())
						{
							return false;
						}
						TreeMap<DimensionMap,Double> temp = this.runStuff(method, colorDimName, bg, offset, segProvided, colors, map, maximaMap, segMap, maskMap, imageMap, filterRadius, this);
						results.putAll(temp);
						count = count + 1;
						JEXStatics.statusBar.setProgressPercentage((int) (100 * count / total));
					}
				}
			}
			else
			{
				double total = colorCombos.size();
				for (DimensionMap colors : colorCombos)
				{
					if(this.isCanceled())
					{
						return false;
					}
					TreeMap<DimensionMap,Double> temp = this.runStuff(method, colorDimName, bg, offset, segProvided, colors, new DimensionMap(), maximaMap, segMap, maskMap, imageMap, filterRadius, this);
					results.putAll(temp);
					count = count + 1;
					JEXStatics.statusBar.setProgressPercentage((int) (100 * count / total));
				}
			}
			
			String resultsFile = JEXTableWriter.writeTable(this.outputNames[0].getName(), results);
			JEXData output0 = FileWriter.makeFileObject(this.outputNames[0].getName(), resultsFile);
			
			// Set the outputs
			this.realOutputs.add(output0);
			
			// Return status
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		
	}
	
	public double productSum(double[] x, double[] y)
	{
		double[] temp = new double[x.length];
		for (int i = 0; i < x.length; i++)
		{
			temp[i] = x[i] * y[i];
		}
		return StatisticsUtility.sum(temp);
	}
	
	public double[] getweights(Vector<Double> x, Vector<Double> y)
	{
		double[] ret = new double[x.size()];
		double w;
		for (int i = 0; i < x.size(); i++)
		{
			w = Math.max(x.get(i), y.get(i));
			ret[i] = w;
		}
		return ret;
	}
	
	public double weightedProductSum(double[] weights, double[] xdiffs, double[] ydiffs)
	{
		double[] temp = new double[xdiffs.length];
		for (int i = 0; i < xdiffs.length; i++)
		{
			temp[i] = weights[i] * xdiffs[i] * ydiffs[i];
		}
		return StatisticsUtility.sum(temp);
	}
	
	public double[] normDiffs(Vector<Double> nums)
	{
		double mu = StatisticsUtility.mean(nums);
		double[] diffs = new double[nums.size()];
		int i = 0;
		for (Double d : nums)
		{
			diffs[i] = d - mu;
			i++;
		}
		return diffs;
	}
	
	public double weightedMean(double[] weights, Vector<Double> x)
	{
		double numer = 0;
		double denom = 0;
		for (int i = 0; i < x.size(); i++)
		{
			numer = numer + weights[i] * x.get(i);
			denom = denom + weights[i];
		}
		return numer / denom;
	}
	
	public double[] weightedNormDiffs(double[] weights, Vector<Double> x)
	{
		double muX = this.weightedMean(weights, x);
		double[] diffs = new double[x.size()];
		for (int i = 0; i < x.size(); i++)
		{
			diffs[i] = (x.get(i) - muX);
			i++;
		}
		return diffs;
	}
	
	public Vector<DimensionMap> getPossibleCombinations(Dim d)
	{
		Vector<DimensionMap> ret = new Vector<DimensionMap>();
		for (int i = 0; i < d.size() - 1; i++)
		{
			for (int j = i + 1; j < d.size(); j++)
			{
				DimensionMap map = new DimensionMap();
				map.put("1", d.valueAt(i));
				map.put("2", d.valueAt(j));
				ret.add(map);
			}
		}
		return ret;
	}
	
	public TreeMap<String,Object> getPixelValues(Wand wand, IdPoint p, ByteProcessor impMask, FloatProcessor impImage1, FloatProcessor impImage2)
	{
		Vector<Double> m1 = null;
		Vector<Double> m2 = null;
		PointList pts = null;
		if(impMask.getPixel(p.x, p.y) == 255) // if we land on a cell that made it through thresholding
		{
			wand.autoOutline(p.x, p.y); // outline it
			if(wand.npoints > 0)
			{
				Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON); // The roi helps for using getLength() (DON'T USE Roi.TRACED_ROI., IT SCREWS UP THE Polygon OBJECTS!!!! Bug emailed to ImageJ folks)
				Polygon poly = new Polygon(wand.xpoints, wand.ypoints, wand.npoints); // The polygon helps for using contains()
				Rectangle r = roi.getBounds();
				m1 = new Vector<Double>();
				m2 = new Vector<Double>();
				pts = new PointList();
				for (int i = r.x; i < r.x + r.width; i++)
				{
					for (int j = r.y; j < r.y + r.height; j++)
					{
						// innerBoundary
						if(poly.contains(i, j) && impMask.getPixelValue(i, j) == 255)
						{
							m1.add((double) impImage1.getPixelValue(i, j));
							m2.add((double) impImage2.getPixelValue(i, j));
							pts.add(i, j);
							// Logs.log("In - " + innerT, this);
						}
					}
				}
			}
		}
		TreeMap<String,Object> ret = new TreeMap<String,Object>();
		ret.put("m1", m1);
		ret.put("m2", m2);
		ret.put("xy", pts);
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	public TreeMap<DimensionMap,Double> runStuff(String method, String colorDimName, double bg, double offset, boolean segProvided, DimensionMap colors, DimensionMap map, TreeMap<DimensionMap,ROIPlus> maximaMap, TreeMap<DimensionMap,String> segMap, TreeMap<DimensionMap,String> maskMap, TreeMap<DimensionMap,String> imageMap, double filterRadius, Canceler canceler)
	{
		TreeMap<DimensionMap,Double> results = new TreeMap<DimensionMap,Double>();
		
		// Get the Maxima
		ROIPlus maxima = maximaMap.get(map);
		
		// Make the mask image impMask
		ByteProcessor impMask = (ByteProcessor) (new ImagePlus(maskMap.get(map)).getProcessor().convertToByte(false));
		if(segProvided)
		{
			ByteProcessor impSeg = (ByteProcessor) (new ImagePlus(segMap.get(map)).getProcessor().convertToByte(false));
			ByteBlitter blit = new ByteBlitter(impMask);
			blit.copyBits(impSeg, 0, 0, Blitter.AND);
		}
		DimensionMap coloredMap1 = map.copyAndSet(colorDimName + "=" + colors.get("1"));
		DimensionMap coloredMap2 = map.copyAndSet(colorDimName + "=" + colors.get("2"));
		FloatProcessor impImage1 = (FloatProcessor) (new ImagePlus(imageMap.get(coloredMap1))).getProcessor().convertToFloat();
		FloatProcessor impImage2 = (FloatProcessor) (new ImagePlus(imageMap.get(coloredMap2))).getProcessor().convertToFloat();
		RankFilters rf = new RankFilters();
		if(filterRadius > 0)
		{
			rf.rank(impImage1, filterRadius, RankFilters.MEDIAN);
			rf.rank(impImage2, filterRadius, RankFilters.MEDIAN);
		}
		Wand wand = new Wand(impMask);
		for (IdPoint p : maxima.getPointList())
		{
			if(canceler.isCanceled())
			{
				return results;
			}
			TreeMap<String,Object> o = this.getPixelValues(wand, p, impMask, impImage1, impImage2);
			if(o.get("m1") != null)
			{
				Vector<Double> m1 = (Vector<Double>) o.get("m1");
				Vector<Double> m2 = (Vector<Double>) o.get("m2");
				PointList pts = (PointList) o.get("xy");
				
				if(m1.size() > 0)
				{
					// Use each signal as a window on the other and calculate the window fraction
					Pair<Double,Double> frac12 = null, frac21 = null;
					
					if(method.equals("Max-Norm"))
					{
						frac12 = StatisticsUtility.windowFraction2(StatisticsUtility.add(-bg, m1), StatisticsUtility.add(-bg, m2));
						frac21 = StatisticsUtility.windowFraction2(StatisticsUtility.add(-bg, m2), StatisticsUtility.add(-bg, m1));
					}
					else
					{
						frac12 = StatisticsUtility.windowFraction(StatisticsUtility.add(-bg, m1), StatisticsUtility.add(-bg, m2), offset);
						frac21 = StatisticsUtility.windowFraction(StatisticsUtility.add(-bg, m2), StatisticsUtility.add(-bg, m1), offset);
					}
					
					DimensionMap toSave = map.copy();
					toSave.put("Id", "" + p.id);
					toSave.put("Measurement", "Sig_" + colors.get("1") + colors.get("2") + "_Tot");
					results.put(toSave.copy(), frac12.p1);
					toSave.put("Measurement", "Sig_" + colors.get("1") + colors.get("2") + "_Window");
					results.put(toSave.copy(), frac12.p2);
					toSave.put("Measurement", "Sig_" + colors.get("2") + colors.get("1") + "_Tot");
					results.put(toSave.copy(), frac21.p1);
					toSave.put("Measurement", "Sig_" + colors.get("2") + colors.get("1") + "_Window");
					results.put(toSave.copy(), frac21.p2);
					toSave.put("Measurement", "n");
					results.put(toSave.copy(), (double) m1.size());
					
					// // Pearson's Correlation Coefficient
					// double[] term1a = this.normDiffs(m1);
					// double[] term1b = this.normDiffs(m2);
					// double term1 = this.productSum(term1a, term1b);
					// double term2a = this.productSum(term1a, term1a);
					// double term2b = this.productSum(term1b, term1b);
					// double R = term1 / Math.sqrt(term2a * term2b);
					// DimensionMap toSave = map.copy();
					// toSave.put("Id", "" + p.id);
					// toSave.put("Measurement", "R_" + colors.get("1") + colors.get("2"));
					// results.put(toSave, R);
					
					// // Weighted Pearson's Correlation Coefficient
					// double[] w = this.getweights(m1, m2);
					// double[] term1a = this.weightedNormDiffs(w, m1);
					// double[] term1b = this.weightedNormDiffs(w, m2);
					// double term1 = this.weightedProductSum(w, term1a, term1b);
					// double term2a = this.weightedProductSum(w, term1a, term1a);
					// double term2b = this.weightedProductSum(w, term1b, term1b);
					// double R = term1 / Math.sqrt(term2a * term2b);
					// DimensionMap toSave = map.copy();
					// toSave.put("Id", "" + p.id);
					// toSave.put("Measurement", "R_" + colors.get("1") + colors.get("2"));
					// results.put(toSave, R);
					
					// // ICQ
					// double muA = StatisticsUtility.median(m1);
					// double muB = StatisticsUtility.median(m2);
					// double count = 0;
					// double tot = m1.size();
					// boolean keepTie = false;
					// boolean a = false;
					// boolean b = false;
					// for (int i = 0; i < m1.size(); i++)
					// {
					// if(canceler.isCanceled())
					// {
					// return results;
					// }
					// if(keepTie && (m1.get(i) >= muA))
					// {
					// a = true;
					// keepTie = false;
					// }
					// else if(!keepTie && (m1.get(i) > muA))
					// {
					// a = true;
					// keepTie = true;
					// }
					// if(keepTie && (m2.get(i) >= muB))
					// {
					// b = true;
					// keepTie = false;
					// }
					// else if(!keepTie && (m2.get(i) > muB))
					// {
					// b = true;
					// keepTie = true;
					// }
					// if((a && b) || (!a && !b))
					// {
					// count = count + 1;
					// }
					// }
					// DimensionMap toSave = map.copy();
					// toSave.put("Id", "" + p.id);
					// toSave.put("Measurement", "R_" + colors.get("1") + colors.get("2"));
					// results.put(toSave.copy(), 2 * (count / tot - 0.5));
					// toSave.put("Measurement", "n");
					// results.put(toSave.copy(), tot);
					
					// // Sum of absolute difference of max normalized intensities
					// double[] m1norm = StatisticsUtility.normalize(StatisticsUtility.add(-1 * bg, m1), true);
					// double[] m2norm = StatisticsUtility.normalize(StatisticsUtility.add(-1 * bg, m2), true);
					// double diffSum = StatisticsUtility.sum(StatisticsUtility.abs(StatisticsUtility.diff(m1norm, m2norm)));
					// DimensionMap toSave = map.copy();
					// toSave.put("Id", "" + p.id);
					// toSave.put("Measurement", "NormAbsDiff_" + colors.get("1") + colors.get("2"));
					// results.put(toSave.copy(), diffSum);
					// toSave.put("Measurement", "n");
					// results.put(toSave.copy(), (double) m1.size());
				}
			}
		}
		return results;
	}
	
	public double[] normalize(Vector<Double> x, boolean maxOnly)
	{
		double[] ret = new double[x.size()];
		
		if(maxOnly)
		{
			double max = StatisticsUtility.max(x);
			for (int i = 0; i < x.size(); i++)
			{
				ret[i] = x.get(i) / max;
			}
		}
		
		return ret;
	}
	
	public static JEXData getInputAs(HashMap<String,JEXData> inputs, String name, Type type)
	{
		JEXData data = inputs.get(name);
		if(data == null || !data.getTypeName().getType().equals(type))
		{
			return null;
		}
		return data;
	}
	
	// private String saveAdjustedImage(String imagePath, double oldMin, double
	// oldMax, double newMin, double newMax, double gamma, int bitDepth)
	// {
	// // Get image data
	// File f = new File(imagePath);
	// if(!f.exists()) return null;
	// ImagePlus im = new ImagePlus(imagePath);
	// FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat();
	// // should be a float processor
	//
	// // Adjust the image
	// FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
	//
	// // Save the results
	// ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false",
	// bitDepth);
	// String imPath = JEXWriter.saveImage(toSave);
	// im.flush();
	//
	// // return temp filePath
	// return imPath;
	// }
}