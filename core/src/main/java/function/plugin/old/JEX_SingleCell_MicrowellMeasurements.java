package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.imageUtility.AutoThresholder;
import ij.ImagePlus;
import ij.plugin.filter.RankFilters;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import image.roi.HashedPointList;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import jex.utilities.ImageUtility;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.LSVList;
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
public class JEX_SingleCell_MicrowellMeasurements extends JEXCrunchable {
	
	public JEX_SingleCell_MicrowellMeasurements()
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
		String result = "Microwell Measurements";
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
		String result = "Counts cells in each microwell and provides stats on microwells.";
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
		String toolbox = "Single Cell Analysis";
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
		TypeName[] inputNames = new TypeName[3];
		inputNames[0] = new TypeName(ROI, "Maxima");
		inputNames[1] = new TypeName(ROI, "Microwells");
		inputNames[2] = new TypeName(IMAGE, "Images");
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
		this.defaultOutputNames = new TypeName[7];
		this.defaultOutputNames[0] = new TypeName(FILE, "Microwell Cell Count Stats");
		this.defaultOutputNames[1] = new TypeName(FILE, "Microwell Intensities");
		this.defaultOutputNames[2] = new TypeName(ROI, "Filtered Maxima");
		this.defaultOutputNames[3] = new TypeName(FILE, "Error Log");
		this.defaultOutputNames[4] = new TypeName(ROI, "Border 0");
		this.defaultOutputNames[5] = new TypeName(ROI, "Border 1");
		this.defaultOutputNames[6] = new TypeName(ROI, "Border 2");
		
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
		Parameter p1 = new Parameter("Measure Intensities?", "Count AND measure intensities (true) or count only and don't measure intensities (false)?", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		Parameter p2 = new Parameter("Microwell ROI Width", "Width/Height of the microwell ROI, establishing border 0 (for measurements R0 & R0_CellMax etc.)", "55");
		Parameter p3 = new Parameter("Microwell ROI Border Width 1", "Width outside the microwell ROI / border 0, establishing border 1 (for measurements R1 etc.)", "3");
		Parameter p3b = new Parameter("Microwell ROI Border Width 2", "Width outside border 1, establishing border 2 (for measurements R2, etc.)", "5");
		// Parameter p3 = new Parameter("Type","Type of roi",Parameter.DROPDOWN, new String[]{"Rectangle","Ellipse","Line","Point"},0);
		Parameter p4 = new Parameter("BG Radius", "Radius of the mean filter for smoothing the background for background estimation", "2.5");
		// Parameter p4b = new
		// Parameter("BG Range","(UpperLimit - LowerLimit), the range in which to search around the image median for finding the mode of the background histogram.\nRecommended is 50 to make sure to encompass the histogram peak.\nThe range will be slightly lower than that observed in the image because of the BG radius filter applied prior to looking at the histogram.","256");
		// Parameter p4c = new
		// Parameter("BG Bins","Number of bins to have over the BG Range for determining the peak in the histogram of the background intensity pixels.\nWe take the weighted average of the peak bin and the the bins on either side to calculate a more accurate estimate of the background.","50");
		Parameter p5 = new Parameter("Cell Radius", "Radius of the mean filter for quantification that should largely encompass the cell", "16");
		Parameter p7 = new Parameter("'Time' Dim Name", "Name of the 'Time' Dimension in the image", "Time");
		Parameter p8 = new Parameter("Name for 'Microwell' Dim", "Name of the Dimension to create for the microwell index", "ROI");
		Parameter p9 = new Parameter("'Color' Dim Name", "Name of the 'Color' Dimension in the roi", "Color");
		Parameter p10a = new Parameter("Maxima Color Value", "Value of the Color Dim for Nuclear Stain Images", "1");
		Parameter p10 = new Parameter("Blue Color Value", "Value of the Color Dim for Nuclear Stain Images", "1");
		Parameter p11 = new Parameter("Green Color Value", "Value of the Color Dim for Green", "2");
		Parameter p12 = new Parameter("Red Color Value", "Value of the Color Dim for Red", "3");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p3b);
		parameterArray.addParameter(p4);
		// parameterArray.addParameter(p4b);
		// parameterArray.addParameter(p4c);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p10a);
		parameterArray.addParameter(p10);
		parameterArray.addParameter(p11);
		parameterArray.addParameter(p12);
		return parameterArray;
	}
	
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
	
	public String timeDimName, colorDimName, microwellDimName;
	public String minMethod = null, maxMethod = null;
	public CSVList thresh_0Range = null, thresh_1Range = null;
	public TreeMap<DimensionMap,Double> countStats = null;
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData maximaData = inputs.get("Maxima");
		if(maximaData == null || !maximaData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		// Collect the inputs
		JEXData imageData = inputs.get("Images");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Collect the inputs
		JEXData microwellData = inputs.get("Microwells");
		if(microwellData == null || !microwellData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		// Gather parameters
		boolean measureIntensities = Boolean.parseBoolean(this.parameters.getValueOfParameter("Measure Intensities?"));
		int rw1 = 0;
		String borderWidthString1 = this.parameters.getValueOfParameter("Microwell ROI Border Width 1");
		if(borderWidthString1 != null && !borderWidthString1.equals(""))
		{
			;
		}
		{
			rw1 = Integer.parseInt(borderWidthString1);
		}
		int rw2 = 0;
		String borderWidthString2 = this.parameters.getValueOfParameter("Microwell ROI Border Width 2");
		if(borderWidthString2 != null && !borderWidthString2.equals(""))
		{
			;
		}
		{
			rw2 = Integer.parseInt(borderWidthString2);
		}
		int w0 = Integer.parseInt(this.parameters.getValueOfParameter("Microwell ROI Width"));
		ROIPlus microwellROI = new ROIPlus(new Rectangle(0, 0, w0, w0));
		double modeRadius = Double.parseDouble(this.parameters.getValueOfParameter("BG Radius"));
		// double modeRange = Double.parseDouble(parameters.getValueOfParameter("BG Range"));
		// int modeBins = Integer.parseInt(parameters.getValueOfParameter("BG Bins"));
		double cellRadius = Double.parseDouble(this.parameters.getValueOfParameter("Cell Radius"));
		this.microwellDimName = this.parameters.getValueOfParameter("Name for 'Microwell' Dim");
		this.timeDimName = this.parameters.getValueOfParameter("'Time' Dim Name"); // Typically "Time" or "T"
		this.colorDimName = this.parameters.getValueOfParameter("'Color' Dim Name");
		String maximaColor = this.parameters.getValueOfParameter("Maxima Color Value");
		String blueColor = this.parameters.getValueOfParameter("Blue Color Value");
		String greenColor = this.parameters.getValueOfParameter("Green Color Value");
		String redColor = this.parameters.getValueOfParameter("Red Color Value");
		
		// Get the input data
		TreeMap<DimensionMap,ROIPlus> maxima = RoiReader.readObjectToRoiMap(maximaData);
		TreeMap<DimensionMap,ROIPlus> microwells = RoiReader.readObjectToRoiMap(microwellData);
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(imageData);
		
		Dim timeDim = imageData.getDimTable().getDimWithName(this.timeDimName);
		Dim colorDim = imageData.getDimTable().getDimWithName(this.colorDimName);
		DimTable frozenTimeAndColorTable = imageData.getDimTable().copy();
		frozenTimeAndColorTable = frozenTimeAndColorTable.getSubTable(new DimensionMap(this.timeDimName + "=" + timeDim.dimValues.get(0) + "," + this.colorDimName + "=" + maximaColor));
		
		// Initialize loop variables
		ROIPlus microwellROIPoints = null;
		int count = 0;
		int total = frozenTimeAndColorTable.mapCount() * timeDim.dimValues.size();
		int percentage = 0;
		
		// This will loop through any other dimensions that exist beyond the TIME and COLOR (e.g. ImRow & ImCol)
		// Save a results files for each of these loops but save an avgCounts results file for everything as well as a filtered ROIs object
		TreeMap<DimensionMap,ROIPlus> filteredROIs = new TreeMap<DimensionMap,ROIPlus>();
		this.countStats = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,String> resultsPaths = new TreeMap<DimensionMap,String>();
		LSVList errorLog = new LSVList();
		ROIPlus roi0 = null;
		ROIPlus roi1 = null;
		ROIPlus roi2 = null;
		for (DimensionMap frozenTimeAndColorMap : frozenTimeAndColorTable.getMapIterator())
		{
			if(this.isCanceled())
			{
				return false;
			}
			
			TreeMap<DimensionMap,Double> measurementResults = new TreeMap<DimensionMap,Double>();
			for (String time : timeDim.dimValues)
			{
				if(this.isCanceled())
				{
					return false;
				}
				
				// For each frozenTimeAndColorMap we need to loop through the pattern roi of the microwell roi and measure count cells and filter the maxima point roi
				
				// Create a DimMap with the current time info
				DimensionMap currentMapWithNuclearColor = frozenTimeAndColorMap.copy();
				currentMapWithNuclearColor.put(this.timeDimName, time);
				
				// Create a colorless dimensionMap for the filtered Rois and the intensity measurements
				DimensionMap colorlessMap = currentMapWithNuclearColor.copy();
				colorlessMap.remove(this.colorDimName);
				
				// Create a colorless and timeless dimensionMap for the avgCounts
				DimensionMap colorlessAndTimelessMap = colorlessMap.copy();
				colorlessAndTimelessMap.remove(this.timeDimName);
				
				// Get the microwell and maxima information and hash the maxima information for determining what microwell each point is located in
				microwellROIPoints = microwells.get(currentMapWithNuclearColor);
				if(microwellROIPoints == null)
				{
					microwellROIPoints = new ROIPlus(new PointList(),ROIPlus.ROI_POINT);
				}
				ROIPlus maximaRoi = maxima.get(currentMapWithNuclearColor);
				PointList maximaPts = new PointList();
				if(maximaRoi != null)
				{
					maximaPts = maximaRoi.getPointList();
				}
				HashedPointList sortedMaxima = new HashedPointList(maximaPts);
				
				// Filter the maxima to retain only those in the microwells and keep a running total of points over the times.
				PointList filteredPoints = new PointList();
				
				for (IdPoint p : microwellROIPoints.getPointList())
				{
					// Get the list of points in this particular microwell
					microwellROI.getPointList().setCenter(p);
					PointList pointsInMicrowell = sortedMaxima.getPointsInRect(microwellROI.getPointList().getBounds());
					
					// Alter the colorless and timeless dimension map for saving the average count information
					DimensionMap avgMap = colorlessAndTimelessMap.copy();
					avgMap.put(this.microwellDimName, "" + p.id);
					avgMap.put("Measurement", "Count Avg");
					DimensionMap maxMap = avgMap.copy();
					maxMap.put("Measurement", "Count Max");
					DimensionMap xMap = avgMap.copy();
					xMap.put("Measurement", "X");
					DimensionMap yMap = avgMap.copy();
					yMap.put("Measurement", "Y");
					this.countStats.put(xMap, (double) p.x);
					this.countStats.put(yMap, (double) p.y);
					
					Double avgTotal = this.countStats.get(avgMap);
					Double max = this.countStats.get(maxMap);
					if(avgTotal == null)
					{
						avgTotal = 0.0;
						max = 0.0;
					}
					int n = 0;
					if(pointsInMicrowell != null)
					{
						n = pointsInMicrowell.size();
						filteredPoints.addAll(pointsInMicrowell);
					}
					avgTotal = avgTotal + n;
					max = Math.max(max, n);
					this.countStats.put(avgMap, avgTotal);
					this.countStats.put(maxMap, max);
				}
				
				// Place the filteredPoints into a ROIPlus and save into a TreeMap for output
				ROIPlus filteredROI = new ROIPlus(filteredPoints, ROIPlus.ROI_POINT);
				filteredROIs.put(colorlessMap, filteredROI);
				
				if(measureIntensities)
				{
					// Measure the filtered points for each of the colors and save the data
					for (String color : colorDim.dimValues)
					{
						if(this.isCanceled())
						{
							return false;
						}
						
						// Create the current DimensionMap
						DimensionMap currentMap = currentMapWithNuclearColor.copy();
						currentMap.put(this.colorDimName, color);
						
						// Set the base of the measurement name to match the color being analyzed for
						// the colorlessMap for saving intensity information
						DimensionMap baseResultsMap = colorlessMap.copy();
						if(blueColor != null && !blueColor.equals("") && color.equals(blueColor))
						{
							baseResultsMap.put("Measurement", "B");
						}
						else if(greenColor != null && !greenColor.equals("") && color.equals(greenColor))
						{
							baseResultsMap.put("Measurement", "G");
						}
						else if(redColor != null && !redColor.equals("") && color.equals(redColor))
						{
							baseResultsMap.put("Measurement", "R");
						}
						else
						{
							continue;
						}
						
						// Get the current image
						String path = images.get(currentMap);
						Logs.log(currentMap.toString() + " - " + path, this);
						FloatProcessor imp1 = (FloatProcessor) new ImagePlus(path).getProcessor().convertToFloat();
						FloatProcessor imp2 = new FloatProcessor(imp1.getWidth(), imp1.getHeight(), (float[]) imp1.getPixelsCopy(), null);
						FloatProcessor imp3 = new FloatProcessor(imp1.getWidth(), imp1.getHeight(), (float[]) imp1.getPixelsCopy(), null);
						
						if(microwellROIPoints.getPointList().size() > 0)
						{
							// Filter the image using the given radius
							RankFilters rF = new RankFilters();
							rF.rank(imp2, modeRadius, JEX_StackProjection.getMethodInt(JEX_Filters.MEAN));
							// double median = ImageStatistics.getStatistics(imp2, ImageStatistics.MEDIAN, null).median;
							rF.rank(imp3, cellRadius, JEX_StackProjection.getMethodInt(JEX_Filters.MEAN));
							
							// Measure all the microwells and points in each microwell for this image
							for (IdPoint p : microwellROIPoints.getPointList())
							{
								// Create the results dimension map
								baseResultsMap.put(this.microwellDimName, "" + p.id);
								
								// Set the microwellROI to be centered on this location.
								microwellROI.getPointList().setCenter(p);
								
								// Measure intensities for this microwell and place in the measurementResults table
								String errorMessage = this.measureMicrowell(measurementResults, imp1, imp2, imp3, microwellROI.getPointList().getBounds(), rw1, rw2, baseResultsMap, entry);
								if(errorMessage != null)
								{
									errorLog.add(errorMessage);
								}
							}
						}
					}
					String resultsPath = JEXTableWriter.writeTable("Microwell Results", measurementResults);
					resultsPaths.put(colorlessAndTimelessMap, resultsPath);
				}
				
				// Update the status
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / (double) (total)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}
		
		// Save the error log
		String errorPath = null;
		if(errorLog.size() > 0)
		{
			errorPath = JEXWriter.saveText(errorLog.toString(), "txt");
		}
		else
		{
			errorPath = JEXWriter.saveText("No errors", "txt");
		}
		
		// Use the microwell point information to create rectangluar ROIs for viewing on the images.
		// 1 for each of the border sizes
		TreeMap<DimensionMap,ROIPlus> roiData0 = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,ROIPlus> roiData1 = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,ROIPlus> roiData2 = new TreeMap<DimensionMap,ROIPlus>();
		for (DimensionMap map : microwells.keySet())
		{
			ROIPlus microwellsInOneImage = microwells.get(map);
			if(microwellsInOneImage != null && microwellsInOneImage.getPointList().size() > 0)
			{
				PointList centers = microwellsInOneImage.getPointList().copy();
				roi0 = new ROIPlus(new Rectangle(0, 0, w0, w0));
				roi1 = new ROIPlus(new Rectangle(0, 0, w0 + rw1 * 2, w0 + rw1 * 2));
				roi2 = new ROIPlus(new Rectangle(0, 0, w0 + rw1 * 2 + rw2 * 2, w0 + rw1 * 2 + rw2 * 2));
				roi0.getPointList().setCenter(centers.get(0).copy());
				roi1.getPointList().setCenter(centers.get(0).copy());
				roi2.getPointList().setCenter(centers.get(0).copy());
				centers.translate(-1 * centers.get(0).x, -1 * centers.get(0).y);
				roi0.setPattern(centers.copy());
				roi1.setPattern(centers.copy());
				roi2.setPattern(centers.copy());
				roiData0.put(map, roi0);
				roiData1.put(map, roi1);
				roiData2.put(map, roi2);
			}
			
		}
		
		String countStatsPath = JEXTableWriter.writeTable("Cell Count Stats", this.countStats);
		JEXData output0 = FileWriter.makeFileObject(this.outputNames[0].getName(), null, countStatsPath);
		JEXData output2 = RoiWriter.makeRoiObject(this.outputNames[2].getName(), filteredROIs);
		JEXData output3 = FileWriter.makeFileObject(this.outputNames[3].getName(), null, errorPath);
		JEXData output4 = RoiWriter.makeRoiObject(this.outputNames[4].getName(), roiData0);
		JEXData output5 = RoiWriter.makeRoiObject(this.outputNames[5].getName(), roiData1);
		JEXData output6 = RoiWriter.makeRoiObject(this.outputNames[6].getName(), roiData2);
		
		this.realOutputs.add(output0);
		this.realOutputs.add(output2);
		this.realOutputs.add(output3);
		this.realOutputs.add(output4);
		this.realOutputs.add(output5);
		this.realOutputs.add(output6);
		
		if(measureIntensities)
		{
			JEXData output1 = FileWriter.makeFileObject(this.outputNames[1].getName(), null, resultsPaths);
			this.realOutputs.add(output1);
		}
		
		// Return status
		return true;
	}
	
	/**
	 * Measure the microwell and return a string that helps log any errors for later perusal to clean up data.
	 * 
	 * @param measurementResults
	 * @param imp1
	 * @param imp2
	 * @param imp3
	 * @param rect
	 * @param borderWidth1
	 * @param borderWidth2
	 * @param baseResultsMap
	 * @return
	 */
	public String measureMicrowell(TreeMap<DimensionMap,Double> measurementResults, ImageProcessor imp1, ImageProcessor imp2, ImageProcessor imp3, Rectangle rect, int borderWidth1, int borderWidth2, DimensionMap baseResultsMap, JEXEntry entry)
	{
		String errorMessage = null;
		
		// Create the dimensionMap for the results table
		DimensionMap resultsMap = baseResultsMap.copy();
		
		// ////////////////////////////////////////////////////////////
		// //////////// Calculate the mode of the background //////////
		// ////////////////////////////////////////////////////////////
		
		// Estimate the background in the microwell from the histogram mode.
		// Obtain first estimate of the mode.
		imp2.setRoi(rect);
		imp2.setHistogramRange(0, 0); // Resets the histogram range to use the min and max of the image/roi
		imp2.setHistogramSize(256);
		// imp2.setH
		ImageStatistics tempStats = ImageStatistics.getStatistics(imp2, ImageStatistics.MIN_MAX + ImageStatistics.MODE, null);
		ImageStatistics tempStats1 = tempStats;
		// String histogramPlot1 = ImageUtility.makeHistogramPlot(tempStats.histogram, false);
		// double tempd1 = tempStats.dmode;
		// double tempMin = tempStats.min;
		// double tempMax = tempStats.max;
		
		// Recalculate the histogram using the filtered imp2 image
		double histMin = tempStats.dmode - 130;
		double histMax = tempStats.dmode + 130;
		int histN = 75;
		imp2.setHistogramRange(histMin, histMax); // center around the mode and go between the min and (mode plus difference between mode and min, an estimate of the noise in the background)
		imp2.setHistogramSize(histN); // Use appropriate bin size for bell curve
		tempStats = ImageStatistics.getStatistics(imp2, ImageStatistics.MIN_MAX + ImageStatistics.MODE, null); // Refine the estimate of the mode
		ImageStatistics tempStats2 = tempStats;
		////		String histogramPlot2 = ImageUtility.makeHistogramPlot(tempStats.histogram, false);
		////		double tempd2 = tempStats.dmode;
		//		// Recalculate the histogram using the filtered imp2 image
		//		histMin = tempStats.dmode - 25;
		//		histMax = tempStats.dmode + 25;

		//		imp2.setHistogramRange(histMin, histMax); // center around the mode and go between the min and (mode plus difference between mode and min, an estimate of the noise in the background)
		//		imp2.setHistogramSize(11); // Use appropriate bin size for bell curve
		//		//		JEXStatics.logManager.log("Histogram Stats: " + histMin + ", " + histMax + ", " + filteredStats.dmode, 0, this);
		//		tempStats = ImageStatistics.getStatistics(imp2, ImageStatistics.MIN_MAX + ImageStatistics.MODE, null); // Refine the estimate of the mode
		//		ImageStatistics tempStats3 = tempStats;
		////		String histogramPlot3 = ImageUtility.makeHistogramPlot(tempStats.histogram, false);
		////		double tempd3 = tempStats.dmode;
		int[] hist = tempStats.histogram;
		//		if(Integer.parseInt(baseResultsMap.get("Time")) > 45)
		//		{
		//			String histogramPlot1 = ImageUtility.makeHistogramPlot(tempStats1.histogram, false);
		//			String histogramPlot2 = ImageUtility.makeHistogramPlot(tempStats2.histogram, false);
		//		}

		//Do a weighted average of the bins at and immediately surrounding the mode to refine the estimate of the mode/peak of the histogram (mode/background filtered image)
		double mode = 0, count = 0;
		double binSize = (histMax-histMin)/histN;
		if(tempStats.mode == 0)
		{
			String histogramPlot1 = ImageUtility.makeHistogramPlot(tempStats1.histogram, false);
			String histogramPlot2 = ImageUtility.makeHistogramPlot(tempStats2.histogram, false);
			// String histogramPlot3 = ImageUtility.makeHistogramPlot(tempStats3.histogram, false);
			Logs.log("Potential bad 0_Mode measurement!!! X=" + entry.getTrayX() + " Y=" + entry.getTrayY() + " " + baseResultsMap.toString() + ". See temp histogram plots " + histogramPlot1 + " & " + histogramPlot2 + ".", 0, this);
			errorMessage = "Check Entry X=" + entry.getTrayX() + " Y=" + entry.getTrayY() + " " + baseResultsMap.toString() + ". See temp histogram plots " + histogramPlot1 + " & " + histogramPlot2 + ".";
			mode = histMin + 0.5 * binSize;
			count = 1;
		}
		else
		{
			for (int i = tempStats.mode - 1; i < tempStats.mode + 1; i++)
			{
				count = count + hist[i];
				mode = mode + hist[i] * (histMin + (i + 0.5) * binSize);
			}
		}
		mode = mode / count;
		resultsMap.put("Measurement", baseResultsMap.get("Measurement") + "0_Mode");
		measurementResults.put(resultsMap.copy(), mode); // Mode of mean filtered Microwell
		
		// ////////////////////////////////////////////////////////////
		// //////////// Measure the single cell intensity ////////////
		// ////////////////////////////////////////////////////////////
		
		// Measure the cell intensity as the max of the filtered imp3 image in the microwell rect (cell max filtered image)
		imp3.setRoi(rect);
		tempStats = ImageStatistics.getStatistics(imp3, ImageStatistics.MIN_MAX, null);
		resultsMap.put("Measurement", baseResultsMap.get("Measurement") + "0_CellMax");
		measurementResults.put(resultsMap.copy(), tempStats.max); // Max of mean filtered Microwell
		
		// ////////////////////////////////////////////////////////////
		// //////////// Measure the raw microwell intensity //////////
		// ////////////////////////////////////////////////////////////
		
		// Measure the microwell mean intensity for the different borders using imp1 (unfiltered image)
		imp1.setRoi(rect);
		tempStats = ImageStatistics.getStatistics(imp1, ImageStatistics.MEAN, null);
		resultsMap.put("Measurement", baseResultsMap.get("Measurement") + "0");
		measurementResults.put(resultsMap.copy(), tempStats.mean); // Mean from microwell
		
		// Measure the microwell with the added border width
		if(borderWidth1 != 0) // It's ok if the width is negative
		{
			Rectangle r = rect.getBounds();
			r.x = r.x - borderWidth1;
			r.y = r.y - borderWidth1;
			r.width = r.width + borderWidth1;
			r.height = r.height + borderWidth1;
			imp1.setRoi(r);
			tempStats = ImageStatistics.getStatistics(imp1, ImageStatistics.MEAN, null);
			resultsMap.put("Measurement", baseResultsMap.get("Measurement") + "1");
			measurementResults.put(resultsMap.copy(), tempStats.mean); // Mean from microwell plus borderwidth
		}
		
		// Measure the microwell with the added border width
		if(borderWidth2 != 0) // It's ok if the width is negative
		{
			Rectangle r = rect.getBounds();
			r.x = r.x - borderWidth1 - borderWidth2;
			r.y = r.y - borderWidth1 - borderWidth2;
			r.width = r.width + borderWidth1 + borderWidth2;
			r.height = r.height + borderWidth1 + borderWidth2;
			imp1.setRoi(r);
			tempStats = ImageStatistics.getStatistics(imp1, ImageStatistics.MEAN, null);
			resultsMap.put("Measurement", baseResultsMap.get("Measurement") + "2");
			measurementResults.put(resultsMap.copy(), tempStats.mean); // Mean from microwell plus borderwidth
		}
		
		return errorMessage;
	}
	
	public static double getUpperMeanDiff(FloatProcessor imp, double threshold)
	{
		float[] pixels = (float[]) imp.getPixels();
		double upperTotal = 0;
		double upperCount = 0;
		for (float pixel : pixels)
		{
			if(pixel > threshold)
			{
				upperCount++;
				upperTotal += pixel;
			}
		}
		
		return (upperTotal / upperCount - threshold);
	}
	
	public static double getMinErrorThreshold(FloatProcessor imp, double histMin, double histMax, int nBins)
	{
		// INTRAmean error Minimization and INTERmean error maximization (2-populations with different mean and variance)
		// Jay Warrick altered for non 256 bin histograms and float pixels
		// Kittler and J. Illingworth, "Minimum error thresholding," Pattern Recognition, vol. 19, pp. 41-47, 1986.
		// C. A. Glasbey, "An analysis of histogram-based thresholding algorithms," CVGIP: Graphical Models and Image Processing, vol. 55, pp. 532-537, 1993.
		// Ported to ImageJ plugin by G.Landini from Antti Niemisto's Matlab code (GPL)
		// Original Matlab code Copyright (C) 2004 Antti Niemisto
		// See http://www.cs.tut.fi/~ant/histthresh/ for an excellent slide presentation
		// and the original Matlab code.
		
		AutoThresholder t = new AutoThresholder();
		imp.setHistogramRange(histMin, histMax);
		imp.setHistogramSize(nBins);
		
		ImageStatistics stats = ImageStatistics.getStatistics(imp, ImageStatistics.MIN_MAX, null);
		int[] histogram = stats.histogram;
		histMin = stats.min;
		histMax = stats.max;
		double binSize = (histMax - histMin) / nBins;
		int threshold = histMean(histogram);
		
		int Tprev = -2;
		double mu, nu, p, q, sigma2, tau2, w0, w1, w2, sqterm, temp;
		// int counter=1;
		while (threshold != Tprev)
		{
			// Calculate some statistics.
			mu = t.B(histogram, threshold) / t.A(histogram, threshold);
			nu = (t.B(histogram, (histogram.length - 1)) - t.B(histogram, threshold)) / (t.A(histogram, (histogram.length - 1)) - t.A(histogram, threshold));
			p = t.A(histogram, threshold) / t.A(histogram, (histogram.length - 1));
			q = (t.A(histogram, (histogram.length - 1)) - t.A(histogram, threshold)) / t.A(histogram, (histogram.length - 1));
			sigma2 = t.C(histogram, threshold) / t.A(histogram, threshold) - (mu * mu);
			tau2 = (t.C(histogram, (histogram.length - 1)) - t.C(histogram, threshold)) / (t.A(histogram, (histogram.length - 1)) - t.A(histogram, threshold)) - (nu * nu);
			
			// The terms of the quadratic equation to be solved.
			w0 = 1.0 / sigma2 - 1.0 / tau2;
			w1 = mu / sigma2 - nu / tau2;
			w2 = (mu * mu) / sigma2 - (nu * nu) / tau2 + t.log10((sigma2 * (q * q)) / (tau2 * (p * p)));
			
			// If the next threshold would be imaginary, return with the current one.
			sqterm = (w1 * w1) - w0 * w2;
			if(sqterm < 0)
			{
				Logs.log("MinError(I): not converging. Try Ignore black/white options", 2, JEX_SingleCell_MicrowellMeasurements.class.getSimpleName());
				return threshold;
			}
			
			// The updated threshold is the integer part of the solution of the quadratic equation.
			Tprev = threshold;
			temp = (w1 + Math.sqrt(sqterm)) / w0;
			
			if(Double.isNaN(temp))
			{
				Logs.log("MinError(I): NaN, not converging. Try Ignore black/white options", 2, JEX_SingleCell_MicrowellMeasurements.class.getSimpleName());
				threshold = Tprev;
			}
			else
			{
				threshold = (int) Math.floor(temp);
				// IJ.log("Iter: "+ counter+++"  t:"+threshold);
			}
		}
		return histMin + threshold * binSize;
	}
	
	public static int histMean(int[] data)
	{
		// Taken from Autothresholder class of ij to be able to handle non-256 bin histograms
		// C. A. Glasbey, "An analysis of histogram-based thresholding algorithms,"
		// CVGIP: Graphical Models and Image Processing, vol. 55, pp. 532-537, 1993.
		//
		// The threshold is the mean of the greyscale data
		int threshold = -1;
		double tot = 0, sum = 0;
		for (int i = 0; i < data.length; i++)
		{
			tot += data[i];
			sum += (i * data[i]);
		}
		threshold = (int) Math.floor(sum / tot);
		return threshold;
	}
	
}
