package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.singleCellAnalysis.SingleCellUtility;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.text.DecimalFormat;
import java.util.HashMap;
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
public class JEX_SingleCell_MeasureTrackPoints extends JEXCrunchable {
	
	public JEX_SingleCell_MeasureTrackPoints()
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
		String result = "Measure Track Points";
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
		String result = "Function that allows you to measure intensity with a defineable ellipse or rectangle at locations defined by a track roi.";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(ROI, "Tracks");
		inputNames[1] = new TypeName(IMAGE, "Image");
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
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(FILE, "Data Table");
		
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
		Parameter p3 = new Parameter("Type", "Type of roi", Parameter.DROPDOWN, new String[] { "Rectangle", "Ellipse", "Line", "Point" }, 0);
		Parameter p4 = new Parameter("ROI Width", "Width of ROI in pixels (ignored for Point ROI)", "10");
		Parameter p5 = new Parameter("ROI Height", "Height of ROI in pixels (ignored for Point ROI)", "10");
		Parameter p6 = new Parameter("ROI Origin", "What part of the roi should be placed at the indicated points (i.e. does the array of points indicate where the upper-left corner should be placed?", Parameter.DROPDOWN, new String[] { ROIPlus.ORIGIN_CENTER, ROIPlus.ORIGIN_UPPERLEFT, ROIPlus.ORIGIN_UPPERRIGHT, ROIPlus.ORIGIN_LOWERRIGHT, ROIPlus.ORIGIN_LOWERLEFT }, 0);
		Parameter p7 = new Parameter("\"Time\" Dim Name", "Name of the \"Time\" Dimension in the image", "Time");
		Parameter p8 = new Parameter("\"Track\" Dim Name", "Name of the \"Track\" Dimension in the roi", "Track");
		Parameter p9 = new Parameter("\"Color\" Dim Name", "Name of the \"Color\" Dimension in the roi", "Color");
		Parameter p10 = new Parameter("Blue Color Value", "Value of the Color Dim for Blue", "0");
		Parameter p11 = new Parameter("Green Color Value", "Value of the Color Dim for Green", "1");
		Parameter p12 = new Parameter("Red Color Value", "Value of the Color Dim for Red", "2");
		Parameter p13 = new Parameter("Nominal Offset Value Of Images", "Value which has been added to the images to avoid clipping negative values.", "100");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p9);
		parameterArray.addParameter(p8);
		parameterArray.addParameter(p10);
		parameterArray.addParameter(p11);
		parameterArray.addParameter(p12);
		parameterArray.addParameter(p13);
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
	
	public String timeDimName, colorDimName, trackDimName, red, green, blue;
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		// Collect the inputs
		JEXData roiData = inputs.get("Tracks");
		if(roiData == null || !roiData.getTypeName().getType().matches(JEXData.ROI))
			return false;
		
		// Collect the inputs
		JEXData imageData = inputs.get("Image");
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
			return false;
		
		// Gather parameters
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
		int roiWidth = Integer.parseInt(parameters.getValueOfParameter("ROI Width"));
		int roiHeight = Integer.parseInt(parameters.getValueOfParameter("ROI Height"));
		String roiOrigin = parameters.getValueOfParameter("ROI Origin");
		timeDimName = parameters.getValueOfParameter("\"Time\" Dim Name"); // Typically
		// "Time"
		// or
		// "T"
		trackDimName = parameters.getValueOfParameter("\"Track\" Dim Name");
		colorDimName = parameters.getValueOfParameter("\"Color\" Dim Name");
		blue = parameters.getValueOfParameter("Blue Color Value");
		green = parameters.getValueOfParameter("Green Color Value");
		red = parameters.getValueOfParameter("Red Color Value");
		double nominal = Double.parseDouble(parameters.getValueOfParameter("Nominal Offset Value Of Images"));
		
		// Create useful Dim's and DimTable's
		DimTable imageTable = imageData.getDimTable();
		DimTable roiTable = roiData.getDimTable();
		DimTable trackTable = new DimTable();
		Dim trackDim = roiTable.getDimWithName(trackDimName);
		trackTable.add(trackDim.copy());
		DimTable dataTable = imageTable.copy();
		dataTable.removeDimWithName(colorDimName);
		dataTable.add(trackDim.copy());
		dataTable.add(new Dim("Measurement", new String[] { SingleCellUtility.r, SingleCellUtility.g, SingleCellUtility.b, SingleCellUtility.x, SingleCellUtility.y }));
		
		// Get the input data
		TreeMap<DimensionMap,ROIPlus> rois = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,String> paths = ImageReader.readObjectToImagePathTable(imageData);
		
		// Write the beginning of the csv file
		JEXTableWriter writer = new JEXTableWriter(outputNames[0].getName(), "arff");
		writer.writeNumericTableHeader(dataTable);
		String fullPath = writer.getPath();
		
		// Initialize loop variables
		ROIPlus trackRoi;
		Roi imageJRoi;
		ImageStatistics stats;
		ImagePlus im;
		DecimalFormat formatD = new DecimalFormat("##0.000");
		int count = 0;
		int total = imageTable.mapCount();
		int percentage = 0;
		PointList pattern;
		ROIPlus roip;
		DimensionMap templateRoiDimensionMap = rois.firstEntry().getKey().copy();
		// Perform the measurements and record the data IN THE ORDER of the
		// dataTable dimension iterator (i.e. dataTable.getIterator());
		try
		{
			
			JEXStatics.statusBar.setStatusText("Measuring " + trackDim.size() + " tracks in " + imageTable.mapCount() + " total images.");
			Logs.log("Measuring " + trackDim.size() + " tracks in " + imageTable.mapCount() + " total images.", 0, this);
			for (DimensionMap imMap : imageTable.getMapIterator())
			{
				if(this.isCanceled())
				{
					return false;
				}
				im = new ImagePlus(paths.get(imMap));
				String time = imMap.get(timeDimName);
				for (DimensionMap trackMap : trackTable.getMapIterator())
				{
					// Generate the roi map to use
					templateRoiDimensionMap.putAll(trackMap);
					
					// Get the point roi that represents the track
					trackRoi = rois.get(templateRoiDimensionMap);
					
					if(trackRoi != null)
					{
						// Create a copy of the templateRoi with the desired
						// shape (ellipse or rect).
						IdPoint trackStart = trackRoi.getPointList().get(0);
						roip = ROIPlus.makeRoi(trackStart.x, trackStart.y, roiOrigin, roiType, roiWidth, roiHeight);
						if(roip == null || roip.getPointList().size() == 0)
						{
							Logs.log("Couldn't get the points to make the template roi!", 9, this);
							JEXStatics.statusBar.setStatusText("Couldn't get the points to make the roi!");
							return false;
						}
						
						// Check to see if there is a pattern defined for this
						// roi that should be taken into consideration
						// Grab the point with the id that matches the track dim
						// name (typically "TIME", whereas crossDimName is
						// typically "COLOR")
						// Translate the newRoi to the appropriate position in
						// the track
						pattern = trackRoi.getPattern();
						int timeInt = Integer.parseInt(time);
						IdPoint pointToMeasure = new IdPoint(-1, -1, 0);
						for (IdPoint p : pattern)
						{
							if(p.id == timeInt)
							{
								roip.getPointList().translate(p.x, p.y);
								pointToMeasure.x = trackStart.x + p.x;
								pointToMeasure.y = trackStart.y + p.y;
								break;
							}
						}
						
						if(pointToMeasure.x != -1)
						{
							imageJRoi = roip.getRoi();
							im.setRoi(imageJRoi);
							stats = im.getStatistics(Measurements.MEAN);
							if(stats != null)
							{
								String dataString = formatD.format(stats.mean);
								if(dataString != null && !dataString.equals(""))
								{
									// DimensionMap for storing data (Color will
									// detected and removed in writeData
									// function)
									DimensionMap map = imMap.copy();
									map.putAll(trackMap);
									
									// Write the data to the ongoing file
									this.writeData(writer, map, stats, pointToMeasure, nominal);
								}
							}
						}
					}
				}
				im.flush();
				im = null;
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / (double) total));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
			writer.close();
			
			JEXData output1 = FileWriter.makeFileObject(outputNames[0].getName(), null, fullPath);
			
			// Set the outputs
			realOutputs.add(output1);
			
			// Return status
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			if(writer != null)
			{
				writer.close();
			}
			return false;
		}
	}
	
	private void writeData(JEXTableWriter writer, DimensionMap map, ImageStatistics stats, IdPoint p, double nominal)
	{
		DimensionMap mapToSave = map.copy();
		String color = map.remove(colorDimName);
		
		// Write the data to the ongoing file
		if(color.equals("0"))
		{
			mapToSave.put("Measurement", SingleCellUtility.x);
			writer.writeData(mapToSave, new Double(p.x));
			mapToSave.put("Measurement", SingleCellUtility.y);
			writer.writeData(mapToSave, new Double(p.y));
			mapToSave.put("Measurement", SingleCellUtility.b);
			writer.writeData(mapToSave, new Double(stats.mean - nominal));
		}
		else if(color.equals("1"))
		{
			mapToSave.put("Measurement", SingleCellUtility.g);
			writer.writeData(mapToSave, new Double(stats.mean - nominal));
		}
		else
		{
			mapToSave.put("Measurement", SingleCellUtility.r);
			writer.writeData(mapToSave, new Double(stats.mean - nominal));
		}
	}
}
