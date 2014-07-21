package recycling;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import image.roi.HashedPointList;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.ROIPlus.PatternRoiIterator;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
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
public class JEX_SingleCell_CountMeasureCellsInMicrowells extends JEXCrunchable {
	
	public JEX_SingleCell_CountMeasureCellsInMicrowells()
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
		String result = "Count and Measure Cells in Microwells";
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
		String result = "Function that allows you to count the number of points in a point roi that fall into the array roi that defines microwell boundaries and outputs a table of that information.";
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
		defaultOutputNames = new TypeName[3];
		defaultOutputNames[0] = new TypeName(FILE, "Microwell Cell Count Stats");
		defaultOutputNames[1] = new TypeName(FILE, "Microwell Intensities");
		defaultOutputNames[2] = new TypeName(ROI, "Filtered Maxima");
		
		if(outputNames == null)
			return defaultOutputNames;
		return outputNames;
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
		Parameter p2 = new Parameter("Microwell Border Width", "Width outside the microwell ROI to measure to obtain the background", "3");
		Parameter p3 = new Parameter("Type", "Type of roi", Parameter.DROPDOWN, new String[] { "Rectangle", "Ellipse", "Line", "Point" }, 0);
		Parameter p4 = new Parameter("ROI Width", "Width of ROI in pixels (ignored for Point ROI)", "10");
		Parameter p5 = new Parameter("ROI Height", "Height of ROI in pixels (ignored for Point ROI)", "10");
		Parameter p6 = new Parameter("ROI Origin", "What part of the roi should be placed at the indicated points (i.e. does the array of points indicate where the upper-left corner should be placed?", Parameter.DROPDOWN, new String[] { ROIPlus.ORIGIN_CENTER, ROIPlus.ORIGIN_UPPERLEFT, ROIPlus.ORIGIN_UPPERRIGHT, ROIPlus.ORIGIN_LOWERRIGHT, ROIPlus.ORIGIN_LOWERLEFT }, 0);
		Parameter p7 = new Parameter("'Time' Dim Name", "Name of the 'Time' Dimension in the image", "Time");
		Parameter p8 = new Parameter("Name for 'Microwell' Dim", "Name of the Dimension to create for the microwell index", "Microwell");
		Parameter p9 = new Parameter("'Color' Dim Name", "Name of the 'Color' Dimension in the roi", "Color");
		Parameter p10 = new Parameter("Blue Color Value", "Value of the Color Dim for Nuclear Stain Images", "1");
		Parameter p11 = new Parameter("Green Color Value", "Value of the Color Dim for Green", "2");
		Parameter p12 = new Parameter("Red Color Value", "Value of the Color Dim for Red", "3");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p9);
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
			return false;
		
		// Collect the inputs
		JEXData imageData = inputs.get("Images");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Collect the inputs
		JEXData microwellData = inputs.get("Microwells");
		if(microwellData == null || !microwellData.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Gather parameters
		boolean measureIntensities = Boolean.parseBoolean(parameters.getValueOfParameter("Measure Intensities?"));
		String type = parameters.getValueOfParameter("Type");
		int roiType = ROIPlus.ROI_RECT;
		if(type.equals("Ellipse"))
		{
			roiType = ROIPlus.ROI_ELLIPSE;
		}
		else if(type.equals("Line"))
		{
			roiType = ROIPlus.ROI_LINE;
		}
		else if(type.equals("Point"))
		{
			roiType = ROIPlus.ROI_POINT;
		}
		int borderWidth = 0;
		String borderWidthString = parameters.getValueOfParameter("Microwell Border Width");
		if(borderWidthString != null && !borderWidthString.equals(""))
			;
		{
			borderWidth = Integer.parseInt(parameters.getValueOfParameter("Microwell Border Width"));
		}
		
		int roiWidth = Integer.parseInt(parameters.getValueOfParameter("ROI Width"));
		int roiHeight = Integer.parseInt(parameters.getValueOfParameter("ROI Height"));
		String roiOrigin = parameters.getValueOfParameter("ROI Origin");
		microwellDimName = parameters.getValueOfParameter("Name for 'Microwell' Dim");
		timeDimName = parameters.getValueOfParameter("'Time' Dim Name"); // Typically
		// "Time"
		// or
		// "T"
		colorDimName = parameters.getValueOfParameter("'Color' Dim Name");
		String blueColor = parameters.getValueOfParameter("Blue Color Value");
		String greenColor = parameters.getValueOfParameter("Green Color Value");
		String redColor = parameters.getValueOfParameter("Red Color Value");
		
		// Get the input data
		TreeMap<DimensionMap,ROIPlus> maxima = RoiReader.readObjectToRoiMap(maximaData);
		TreeMap<DimensionMap,ROIPlus> microwells = RoiReader.readObjectToRoiMap(microwellData);
		TreeMap<DimensionMap,String> images = ImageReader.readObjectToImagePathTable(imageData);
		
		Dim timeDim = microwellData.getDimTable().getDimWithName(timeDimName);
		Dim colorDim = microwellData.getDimTable().getDimWithName(colorDimName);
		DimTable frozenTimeAndColorTable = microwellData.getDimTable().getSubTable(new DimensionMap(timeDimName + "=" + timeDim.dimValues.get(0) + "," + colorDimName + "=" + blueColor));
		
		// Initialize loop variables
		ROIPlus microwellROI = null;
		int count = 0;
		int total = frozenTimeAndColorTable.mapCount() * timeDim.dimValues.size();
		int percentage = 0;
		
		// This will loop through any other dimensions that exist beyond the
		// TIME and COLOR (e.g. ImRow & ImCol)
		// Save a results files for each of these loops but save an avgCounts
		// results file for everything as well as a filtered ROIs object
		TreeMap<DimensionMap,ROIPlus> filteredROIs = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,Double> countStats = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,String> resultsPaths = new TreeMap<DimensionMap,String>();
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
				
				// For each frozenTimeAndColorMap we need to loop through the
				// pattern roi of the microwell roi and measure count cells and
				// filter the maxima point roi
				
				// Create a DimMap with the current time info
				DimensionMap currentMapWithNuclearColor = frozenTimeAndColorMap.copy();
				currentMapWithNuclearColor.put(timeDimName, time);
				
				// Create a colorless dimensionMap for the filtered Rois and the
				// intensity measurements
				DimensionMap colorlessMap = currentMapWithNuclearColor.copy();
				colorlessMap.remove(colorDimName);
				
				// Create a colorless and timeless dimensionMap for the
				// avgCounts
				DimensionMap colorlessAndTimelessMap = colorlessMap.copy();
				colorlessAndTimelessMap.remove(timeDimName);
				
				// Get the microwell and maxima information and hash the maxima
				// information for determining what microwell each point is
				// located in
				microwellROI = microwells.get(currentMapWithNuclearColor);
				ROIPlus maximaRoi = maxima.get(currentMapWithNuclearColor);
				if(maximaRoi == null)
				{
					Logs.log("Help me.", 0, this);
				}
				PointList maximaPts = maximaRoi.getPointList();
				HashedPointList sortedMaxima = new HashedPointList(maximaPts);
				
				// Filter the maxima to retain only those in the microwells and
				// keep a running total of points over the times.
				PointList filteredPoints = new PointList();
				PatternRoiIterator microwellROIItr = microwellROI.new PatternRoiIterator(microwellROI);
				while (microwellROIItr.hasNext())
				{
					// Get the list of points in this particular microwell
					ROIPlus region = microwellROIItr.next();
					IdPoint regionCenter = PointList.getCenter(region.getPointList().getBounds());
					PointList pointsInMicrowell = sortedMaxima.getPointsInRect(region.getPointList().getBounds());
					
					// Alter the colorless and timeless dimension map for saving
					// the average count information
					DimensionMap avgMap = colorlessAndTimelessMap.copy();
					avgMap.put(microwellDimName, "" + microwellROIItr.currentPatternPoint().id);
					avgMap.put("Measurement", "Count Avg");
					DimensionMap maxMap = avgMap.copy();
					maxMap.put("Measurement", "Count Max");
					DimensionMap xMap = avgMap.copy();
					xMap.put("Measurement", "X");
					DimensionMap yMap = avgMap.copy();
					yMap.put("Measurement", "Y");
					countStats.put(xMap, (double) regionCenter.x);
					countStats.put(yMap, (double) regionCenter.y);
					
					Double avgTotal = countStats.get(avgMap);
					Double max = countStats.get(maxMap);
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
					avgTotal = avgTotal + (double) n;
					max = Math.max(max, n);
					countStats.put(avgMap, avgTotal);
					countStats.put(maxMap, max);
				}
				
				if(measureIntensities)
				{
					// Place the filteredPoints into a ROIPlus and save into a
					// TreeMap for output
					ROIPlus filteredROI = new ROIPlus(filteredPoints, ROIPlus.ROI_POINT);
					filteredROIs.put(colorlessMap, filteredROI);
					
					// Measure the filtered points for each of the colors and
					// save the data
					for (String color : colorDim.dimValues)
					{
						if(this.isCanceled())
						{
							return false;
						}
						
						// Create the current DimensionMap
						DimensionMap currentMap = currentMapWithNuclearColor.copy();
						currentMap.put(colorDimName, color);
						
						// Set the base of the measurement name to match the
						// color being analyzed for
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
						ImageProcessor imp = new ImagePlus(path).getProcessor();
						
						// Measure all the microwells and points in each
						// microwell for this image
						microwellROIItr = microwellROI.new PatternRoiIterator(microwellROI);
						while (microwellROIItr.hasNext())
						{
							// Get the list of points in this particular
							// microwell
							Rectangle rect = microwellROIItr.next().getPointList().getBounds();
							
							// Create the results dimension map
							baseResultsMap.put(microwellDimName, "" + microwellROIItr.currentPatternPoint().id);
							
							// Measure intensities for this microwell and place
							// in the measurementResults table
							measureMicrowell(measurementResults, imp, rect, borderWidth, baseResultsMap);
							
							// Get the points in this microwell and measure the
							// intensities in this image
							PointList pointsInMicrowell = sortedMaxima.getPointsInRect(rect);
							if(pointsInMicrowell != null)
							{
								double mean = 0;
								double pCount = 0;
								
								// Measure the "Ellipse around the maxima"
								for (IdPoint p : pointsInMicrowell)
								{
									ROIPlus roi = ROIPlus.makeRoi(p.x, p.y, roiOrigin, roiType, roiWidth, roiHeight);
									imp.setRoi(roi.getRoi());
									ImageStatistics stats = ImageStatistics.getStatistics(imp, ImageStatistics.MEAN, null);
									mean = mean + stats.mean;
									pCount = pCount + 1;
								}
								DimensionMap resultsMap = baseResultsMap.copy();
								resultsMap.put("Measurement", baseResultsMap.get("Measurement") + "0");
								measurementResults.put(resultsMap, mean / pCount); // Mean
								// from
								// maxima
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
		
		// Convert total point counts to average point counts
		for (Entry<DimensionMap,Double> e : countStats.entrySet())
		{
			if(e.getKey().get("Measurement").equals("Count Avg"))
			{
				e.setValue(e.getValue() / timeDim.dimValues.size());
			}
		}
		
		String countStatsPath = JEXTableWriter.writeTable("Cell Count Stats", countStats);
		JEXData output1 = FileWriter.makeFileObject(outputNames[0].getName(), countStatsPath);
		realOutputs.add(output1);
		
		if(measureIntensities)
		{
			JEXData output2 = FileWriter.makeFileTable(outputNames[1].getName(), resultsPaths);
			JEXData output3 = RoiWriter.makeRoiObject(outputNames[2].getName(), filteredROIs);
			realOutputs.add(output2);
			realOutputs.add(output3);
		}
		
		// Return status
		return true;
	}
	
	public void measureMicrowell(TreeMap<DimensionMap,Double> measurementResults, ImageProcessor imp, Rectangle rect, int borderWidth, DimensionMap baseResultsMap)
	{
		// Create the dimensionMap for the results table
		DimensionMap resultsMap = baseResultsMap.copy();
		resultsMap.put("Measurement", baseResultsMap.get("Measurement") + "1");
		
		// Measure the microwell
		imp.setRoi(rect);
		ImageStatistics stats = ImageStatistics.getStatistics(imp, ImageStatistics.MEAN, null);
		measurementResults.put(resultsMap.copy(), stats.mean); // Mean from
		// microwell
		
		// Measure the microwell with the added border width
		if(borderWidth != 0) // It's ok if the width is negative
		{
			Rectangle r = rect.getBounds();
			r.x = r.x - borderWidth;
			r.y = r.y - borderWidth;
			r.width = r.width + borderWidth;
			r.height = r.height + borderWidth;
			imp.setRoi(r);
			stats = ImageStatistics.getStatistics(imp, ImageStatistics.MEAN, null);
			resultsMap.put("Measurement", baseResultsMap.get("Measurement") + "2");
			measurementResults.put(resultsMap.copy(), stats.mean); // Mean from
			// microwell
			// plus
			// borderwidth
		}
	}
	
}
