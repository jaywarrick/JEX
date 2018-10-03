package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import miscellaneous.VectorUtility;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
import weka.core.converters.JEXTableWriter;


/**
 * This is a JEXperiment function template
 * To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions
 * 2. Place the file in the Functions/SingleDataPointFunctions folder
 * 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types
 * The specific API for these can be found in the main JEXperiment folder.
 * These API provide methods to retrieve data from these objects,
 * create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 *
 */
public class JEX_PlaqueAnalysis_MeasurePlaqueLocations extends JEXCrunchable{

	public static String MEAN = "1", AREA = "2", MIN = "3", MAX = "4", STDEV = "5", MEDIAN = "6", X = "7", Y = "8";

	//	// Write the data to the ongoing file
	//	map.put("Measurement", "1");
	//	writer.writeData(map, new Double(stats.mean));
	//	map.put("Measurement", "2");
	//	writer.writeData(map, new Double(stats.area));
	//	map.put("Measurement", "3");
	//	writer.writeData(map, new Double(stats.min));
	//	map.put("Measurement", "4");
	//	writer.writeData(map, new Double(stats.max));
	//	map.put("Measurement", "5");
	//	writer.writeData(map, new Double(stats.stdDev));
	//	map.put("Measurement", "6");
	//	writer.writeData(map, new Double(stats.median));
	//	map.put("Measurement", "7");
	//	writer.writeData(map, new Double(p.x));
	//	map.put("Measurement", "8");
	//	writer.writeData(map, new Double(p.y));

	public JEX_PlaqueAnalysis_MeasurePlaqueLocations(){}

	// ----------------------------------------------------
	// --------- INFORMATION ABOUT THE FUNCTION -----------
	// ----------------------------------------------------

	/**
	 * Returns the name of the function
	 * 
	 * @return Name string
	 */
	@Override
	public String getName() {
		String result = "Measure Plaque Locations";
		return result;
	}

	/**
	 * This method returns a string explaining what this method does
	 * This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	@Override
	public String getInfo() {
		String result = "Function that uses a Point Roi to determine the rough center of a plaque. The rough center is used to locate the first infected cells and the center of a the new plaque.";
		return result;
	}

	/**
	 * This method defines in which group of function this function 
	 * will be shown in... 
	 * Toolboxes (choose one, caps matter):
	 * Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools
	 * Stack processing, Data Importing, Custom image analysis, Matlab/Octave
	 * 
	 */
	@Override
	public String getToolbox() {
		String toolbox = "Plaque Analysis";
		return toolbox;
	}

	/**
	 * This method defines if the function appears in the list in JEX
	 * It should be set to true expect if you have good reason for it
	 * 
	 * @return true if function shows in JEX
	 */
	@Override
	public boolean showInList() {
		return true;
	}

	/**
	 * Returns true if the user wants to allow multithreding
	 * @return
	 */
	@Override
	public boolean allowMultithreading()
	{
		return false;
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
	public TypeName[] getInputNames(){
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(ROI,"Point ROI");
		inputNames[1] = new TypeName(FILE,"Time Files");
		return inputNames;
	}

	/**
	 * Return the array of output names defined for this function
	 * @return
	 */
	@Override
	public TypeName[] getOutputs(){
		defaultOutputNames = new TypeName[2];
		defaultOutputNames[0] = new TypeName(FILE,"Plaque Center Table");
		defaultOutputNames[1] = new TypeName(ROI,"Plaque Center ROI");

		if (outputNames == null) return defaultOutputNames;
		return outputNames;
	}

	/**
	 * Returns a list of parameters necessary for this function 
	 * to run...
	 * Every parameter is defined as a line in a form that provides 
	 * the ability to set how it will be displayed to the user and 
	 * what options are available to choose from
	 * The simplest FormLine can be written as:
	 * FormLine p = new FormLine(parameterName);
	 * This will provide a text field for the user to input the value
	 * of the parameter named parameterName
	 * More complex displaying options can be set by consulting the 
	 * FormLine API
	 * 
	 * @return list of FormLine to create a parameter panel
	 */
	@Override
	public ParameterSet requiredParameters() {
		Parameter p1 = new Parameter("Color","Color to analyze.", Parameter.DROPDOWN, new String[]{"R","G","B"},0);
		Parameter p2 = new Parameter("Cell Threshold","Threshold over which a cell is considered as positive (i.e., activated, infected, etc).","15");
		Parameter p3 = new Parameter("Center Error","The radius within which to search for positive cells for determining the center of the plaque.","300");
		Parameter p4 = new Parameter("Avg of n","Number of positive cells for determining the plaque center","5");
		Parameter p5 = new Parameter("Skip first m times","Number of initial time frames to skip before looking for plaque centers","0");

		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);

		return parameterArray;
	}


	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------

	/**
	 * Returns the status of the input validity checking
	 * It is HIGHLY recommended to implement input checking
	 * however this can be over-ridden by returning false
	 * If over-ridden ANY batch function using this function 
	 * will not be able perform error checking... 
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled(){
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
		// Collect the inputs
		JEXData roiData = inputs.get("Point ROI");
		if (roiData == null || !roiData.getTypeName().getType().matches(JEXData.ROI)) return false;

		// Collect the inputs
		JEXData fileData = inputs.get("Time Files");
		if (fileData == null || !fileData.getTypeName().getType().matches(JEXData.FILE)) return false;

		// Gather parameters
		String color = parameters.getValueOfParameter("Color");
		double threshold = Double.parseDouble(parameters.getValueOfParameter("Cell Threshold"));
		double centerError = Double.parseDouble(parameters.getValueOfParameter("Center Error"));
		int n  = Integer.parseInt(parameters.getValueOfParameter("Avg of n"));
		int m  = Integer.parseInt(parameters.getValueOfParameter("Skip first m times"));

		// Create useful Dim's and DimTable's
		DimTable timeTable = fileData.getDimTable();
		Dim timeDim = timeTable.get(0); // Time should be the only dim in the dimTable.

		// Get the input data
		TreeMap<DimensionMap,ROIPlus> rois = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,String> paths = FileReader.readObjectToFilePathTable(fileData);

		// Gather all the points in the point ROI as they represent all the plaque centers
		Vector<Plaque> plqs = new Vector<Plaque>();
		int idCounter = 0;
		for(Entry<DimensionMap,ROIPlus> e : rois.entrySet())
		{
			PointList centerPoints = e.getValue().getPointList();
			for(IdPoint p : centerPoints)
			{
				if(this.isCanceled())
				{
					return false;
				}
				Plaque plq = new Plaque();
				plq.oldCenter = p.copy();
				plq.tend = Integer.parseInt(e.getKey().get(timeDim.dimName)); // The final time equals the value of the Time dim for this pointlist
				plq.id = idCounter;
				plqs.add(plq);
				idCounter = idCounter + 1;
			}
		}

		// For each plaque determine the true center.
		// Start at time zero and go forward, looping through each plaque until you successfully find a new center for each plaque
		double timeCount = 0, timeTotal = timeTable.mapCount();
		double plqCount = 0, plqTotal = plqs.size();
		double subunit = 100.0/timeTotal;
		int progress = 0;
		JEXStatics.statusBar.setProgressPercentage(progress);
		for(DimensionMap map : timeTable.getMapIterator()) // for each timepoint
		{
			timeCount = timeCount + 1;
			if(timeCount > m && !this.allPlaquesFinished(plqs))
			{
				plqCount = 0;
				
				// read the data for this timepoint
				if(this.isCanceled())
				{
					return false;
				}
				Table<Double> data = JEXTableReader.getNumericTable(paths.get(map));
				
				// Find all the cells in this timepoint that are positive.
				if(this.isCanceled())
				{
					return false;
				}
				Vector<DimensionMap> positiveCells = Plaque.getPositiveCells(data, color, threshold);
				
				progress =  (int) (subunit*(timeCount-1) + 0.5*subunit);
				JEXStatics.statusBar.setProgressPercentage(progress);
				
				for(Plaque plq : plqs) // for each plaque at each timepoint
				{
					plqCount = plqCount + 1;
					if(plq.newCenter == null) // if we haven't already found a new center
					{
						if(this.isCanceled())
						{
							return false;
						}
						plq.calculateNewCenter(data, positiveCells, centerError, n);
						
						if(plq.newCenter != null) // if a new center was found, mark the time as the start time.
						{
							plq.tstart = Integer.parseInt(map.get(timeDim.dimName));
						}
					}
					progress =  (int) (subunit*(timeCount-1) + 0.5*subunit + 0.5*subunit*plqCount/plqTotal);
					JEXStatics.statusBar.setProgressPercentage(progress);
				}
			}
		}


		// Create a roi that represents the center of all the plaques
		// resent the ids in case some plaque centers couldn't be calculated
		PointList newCenters = new PointList();
		idCounter = 0;
		Vector<Plaque> finalPlqs = new Vector<Plaque>();
		for(Plaque plq : plqs)
		{
			if(plq.newCenter != null)
			{
				if(this.isCanceled())
				{
					return false;
				}
				newCenters.add(plq.newCenter);
				plq.id = idCounter;
				finalPlqs.add(plq);
				idCounter = idCounter + 1;
			}
		}
		
		if(this.isCanceled())
		{
			return false;
		}
		
		TreeMap<DimensionMap,Double> toWrite = new TreeMap<DimensionMap,Double>();
		for(Plaque plq : finalPlqs)
		{
			toWrite.put(plq.getData(), 0.0);
		}
		String path = JEXTableWriter.writeTable("Plaques", toWrite);
		JEXData output1 = FileWriter.makeFileObject(outputNames[0].getName(), null, path);
		
		
		ROIPlus roi = new ROIPlus(newCenters, ROIPlus.ROI_POINT);
		JEXData output2 = RoiWriter.makeRoiObject(outputNames[1].getName(), roi);

		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);

		// Return status
		return true;
	}
	
	public boolean allPlaquesFinished(Vector<Plaque> plqs)
	{
		for(Plaque plq : plqs)
		{
			if(plq.newCenter == null)
			{
				return false;
			}
		}
		return true;
	}

}

class Plaque {

	public IdPoint oldCenter;
	public Point newCenter = null;
	public int id;
	public int tstart;
	public int tend;

	public Vector<String> cellIds = new Vector<String>();

	public boolean isEmpty()
	{
		return this.cellIds.size() == 0;
	}

	public void addCell(String id)
	{
		this.cellIds.add(id);
	}
	
	public static Vector<DimensionMap> getPositiveCells(Table<Double> data, String color, double threshold)
	{		
		TreeMap<DimensionMap,Double> colorData = Table.getFilteredData(data.data, "Measurement=" + color);
		Vector<DimensionMap> ret = new Vector<DimensionMap>();
		
		for(Entry<DimensionMap,Double> e : colorData.entrySet())
		{
			double intensity = e.getValue();
			if(intensity >= threshold) // if the cell is bright enough
			{
				ret.add(e.getKey().copy());
			}
		}

		return ret;
	}
	
	public void calculateNewCenter(Table<Double> data, Vector<DimensionMap> positiveCells, double centerError, int n)
	{
		PointList points = new PointList();
		double radiusSquared = centerError*centerError;
		for(DimensionMap map : positiveCells)
		{
			DimensionMap newMap = map.copy();
			
			newMap.put("Measurement", "X");
			double x = data.getData(newMap);
			newMap.put("Measurement", "Y");
			double y = data.getData(newMap);
			double dx = x - this.oldCenter.x;
			double dy = y - this.oldCenter.y;
			double distanceSquared = dx*dx+dy*dy;
			if(distanceSquared < radiusSquared) // and the cell is close enough
			{
				int id = Integer.parseInt(newMap.get("Point"));
				points.add((int)x, (int)y, id); // add it to the list of cells that might help calculate the center.
			}
		}
		
		if(points.size() >= n) // if we find enough points, then calculate the center and return it
		{
			Vector<Double> xs = new Vector<Double>();
			Vector<Double> ys = new Vector<Double>();
			
			for(IdPoint p : points)
			{
				xs.add((double) p.x);
				ys.add((double) p.y);
			}
			
			int meanX = Math.round((float) VectorUtility.mean(xs)); 
			int meanY = Math.round((float) VectorUtility.mean(ys));
			
			this.newCenter = new Point(meanX, meanY);
		}
				
	}
	
	public DimensionMap getData()
	{
		DimensionMap map = new DimensionMap();
		map.put("Id", ""+this.id);
		map.put("X", ""+this.newCenter.x);
		map.put("Y", ""+this.newCenter.y);
		map.put("tstart", ""+this.tstart);
		map.put("tend", ""+this.tend);
		
		return map;		
	}

}

