package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.singleCellAnalysis.TrackHash;
import image.roi.HashedPointList;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import miscellaneous.Pair;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableReader;

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
public class JEX_ImageTools_TrackPoints extends JEXCrunchable {
	
	public JEX_ImageTools_TrackPoints()
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
		String result = "Track Points";
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
		String result = "Use a nearest neighbor approach for creating tracks from point rois in each frame of a stack (i.e. along one dimension)";
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
		String toolbox = "Image tools";
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
	public TypeName[] getInputNames()
	{
		TypeName[] inputNames = new TypeName[1];
		inputNames[0] = new TypeName(ROI, "Points");
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
		this.defaultOutputNames = new TypeName[2];
		this.defaultOutputNames[0] = new TypeName(ROI, "Tracks Roi");
		this.defaultOutputNames[1] = new TypeName(ROI, "Maxima (Tracked)");
		
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
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		Parameter p3 = new Parameter("Radius of Search", "The number of pixels left, rigth, up, and down to search for a nearest neighbor match.", "25");
		Parameter p4 = new Parameter("Time Dim", "Name of the dimension along which points will be linked (typicall time)", "Time");
		Parameter p5 = new Parameter("Track Dim Name", "Tracks will be grouped into new rois and indexed with a new dim of this name.", "Track");
		Parameter p6 = new Parameter("Max Track Start Time Index", "Remove tracks that start after this time index (leave blank to ignore)", "");
		Parameter p7 = new Parameter("Min Track End Time Index", "Remove tracks that end before this time index (leave blank to ignore)", "");
		Parameter p8 = new Parameter("Min Track Duration", "Remove tracks that exist for fewer than this many time indicies (leave blank to ignore)", "");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
		parameterArray.addParameter(p5);
		parameterArray.addParameter(p6);
		parameterArray.addParameter(p7);
		parameterArray.addParameter(p8);
		
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
	
	/**
	 * Perform the algorithm here
	 * 
	 */
	@Override
	public boolean run(JEXEntry entry, HashMap<String,JEXData> inputs)
	{
		
		JEXStatics.statusBar.setProgressPercentage(0);
		
		// Collect the inputs
		JEXData roiData = inputs.get("Points");
		if(roiData == null || !roiData.getTypeName().getType().matches(JEXData.ROI))
		{
			return false;
		}
		
		// Gather parameters
		int radius = Integer.parseInt(this.parameters.getValueOfParameter("Radius of Search"));
		String timeDimName = this.parameters.getValueOfParameter("Time Dim");
		String trackDimName = this.parameters.getValueOfParameter("Track Dim Name");
		String trackStartString = this.parameters.getValueOfParameter("Max Track Start Time Index");
		Integer trackStart = null;
		if(trackStartString != null && !trackStartString.equals(""))
		{
			trackStart = Integer.parseInt(trackStartString);
		}
		String trackEndString = this.parameters.getValueOfParameter("Min Track End Time Index");
		Integer trackEnd = null;
		if(trackEndString != null && !trackEndString.equals(""))
		{
			trackEnd = Integer.parseInt(trackEndString);
		}
		String trackLengthString = this.parameters.getValueOfParameter("Min Track Duration");
		Integer trackLength = null;
		if(trackLengthString != null && !trackLengthString.equals(""))
		{
			trackLength = Integer.parseInt(trackLengthString);
		}
		
		// Run the function
		DimTable roiTable = roiData.getDimTable();
		Dim timeDim = roiTable.getDimWithName(timeDimName);
		// DimTable timeTable = new DimTable();
		// timeTable.add(timeDim);
		roiTable.remove(timeDim);
		
		List<DimensionMap> loopDims = roiTable.getDimensionMaps();
		// List<DimensionMap> timeDims = timeTable.getDimensionMaps();
		
		TreeMap<DimensionMap,ROIPlus> maximaRois = RoiReader.readObjectToRoiMap(roiData);
		int count = 0, percentage = 0;
		
		// Rois currently contains all the points for a given time, reorganize
		// into tracks by finding nearest neighbors
		// Loop through time and for each time, compare to next time and choose
		// nearest neighbors and store in temp list of neighbors
		TreeMap<DimensionMap,ROIPlus> trackRois = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,ROIPlus> newMaximaRois = new TreeMap<DimensionMap,ROIPlus>();
		if(loopDims.size() == 0)
		{
			Pair<TreeMap<DimensionMap,ROIPlus>,TreeMap<DimensionMap,ROIPlus>> results = JEX_ImageTools_TrackPoints.trackPointsForAGivenLoopMap(maximaRois, new DimensionMap(), trackDimName, timeDim, radius, trackStart, trackEnd, trackLength);
			trackRois = results.p1;
			newMaximaRois = results.p2;
		}
		else
		{
			for (DimensionMap loopMap : loopDims)
			{
				Pair<TreeMap<DimensionMap,ROIPlus>,TreeMap<DimensionMap,ROIPlus>> results = JEX_ImageTools_TrackPoints.trackPointsForAGivenLoopMap(maximaRois, loopMap, trackDimName, timeDim, radius, trackStart, trackEnd, trackLength);
				trackRois.putAll(results.p1);
				newMaximaRois.putAll(results.p2);
				
				// Update status indicator
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) loopDims.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}
		
		JEXData output0 = RoiWriter.makeRoiObject(this.outputNames[0].getName(), trackRois);
		JEXData output1 = RoiWriter.makeRoiObject(this.outputNames[1].getName(), newMaximaRois);
		
		// Set the outputs
		this.realOutputs.add(output0);
		this.realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
	public static Pair<TreeMap<DimensionMap,ROIPlus>,TreeMap<DimensionMap,ROIPlus>> trackPointsForAGivenLoopMap(TreeMap<DimensionMap,ROIPlus> maximaRois, DimensionMap loopMap, String trackDimName, Dim timeDim, Integer radius, Integer start, Integer end, Integer length)
	{
		// Rois currently contains all the points for a given time, reorganize
		// into tracks by finding nearest neighbors
		// Loop through time and for each time, compare to next time and choose
		// nearest neighbors and store in temp list of neighbors
		TreeMap<DimensionMap,ROIPlus> trackRois = new TreeMap<DimensionMap,ROIPlus>();
		TreeMap<DimensionMap,ROIPlus> newMaximaRois = new TreeMap<DimensionMap,ROIPlus>();
		PointList lastPoints = null, thisPoints = null;
		DimensionMap thisDim = null, lastDim = null;
		TrackHash tracks = new TrackHash();
		for (int nt = 1; nt < timeDim.size(); nt++)
		{
			// Create the dimension maps for getting data for tracking
			thisDim = new DimensionMap();
			thisDim.putAll(loopMap);
			thisDim.put(timeDim.dimName, timeDim.dimValues.get(nt));
			thisPoints = maximaRois.get(thisDim).getPointList();
			lastDim = new DimensionMap();
			lastDim.putAll(loopMap);
			lastDim.put(timeDim.dimName, timeDim.dimValues.get(nt - 1));
			lastPoints = maximaRois.get(lastDim).getPointList();
			
			// Pair the points from the last timepoint to this timepoint
			List<Pair<IdPoint,IdPoint>> pairs = HashedPointList.getNearestNeighbors(lastPoints, thisPoints, radius, false);
			pairs = HashedPointList.filterConflicts(pairs);
			
			// Add the appropriate points to the accruing tracks
			for (Pair<IdPoint,IdPoint> pair : pairs)
			{
				IdPoint lastPoint = pair.p1;
				IdPoint thisPoint = pair.p2;
				lastPoint.id = nt - 1;
				
				if(thisPoint != null)
				{
					thisPoint.id = nt;
					tracks.putPoint(loopMap, lastPoint, thisPoint);
				}
				// Logs.log(pair.toString(), 0, this);
			}
			// Logs.log("\n", 0, this);
			
			// Update the hash table in tracks to be keyed by the newest
			// points added to the pointLists
			tracks.updateTracks(loopMap);
		}
		
		// Convert the PointLists to PointRois and save them in trackRois
		int trackNum = 1;
		// Store the PointLists by track number as we create the trackRois
		TreeMap<DimensionMap,IdPoint> newMaximaPointLists = new TreeMap<DimensionMap,IdPoint>();
		for (PointList pl : tracks.getTracksList(loopMap)) // getTrackList
		// sorts by position
		// of first point;
		{
			boolean keep = true;
			if(start != null && pl.get(0).id > start)
			{
				keep = false;
			}
			if(end != null && pl.lastElement().id < end)
			{
				keep = false;
			}
			if(length != null && pl.size() < length)
			{
				keep = false;
			}
			
			if(keep)
			{
				DimensionMap newDim = new DimensionMap();
				newDim.putAll(loopMap);
				// newDim.put(timeDimName, timeDims.get(0).get(timeDimName));
				newDim.put(trackDimName, "" + trackNum);
				PointList temp = new PointList();
				temp.add(pl.get(0));
				ROIPlus roi = new ROIPlus(temp, ROIPlus.ROI_POINT);
				roi.setPattern(pl.copy());
				trackRois.put(newDim.copy(), roi);
				
				for (IdPoint p : pl)
				{
					newDim.put(timeDim.dimName, "" + p.id);
					newMaximaPointLists.put(newDim.copy(), p.copy());
				}
				trackNum = trackNum + 1;
			}
		}
		
		// Make new maxima rois with id's that actually match for different times
		for (String time : timeDim.dimValues)
		{
			// for each time
			DimensionMap newMap = loopMap.copy();
			newMap.put(timeDim.dimName, "" + time);
			
			TreeMap<DimensionMap,IdPoint> filteredPoints = JEXTableReader.filter(newMaximaPointLists, newMap);
			PointList temp = new PointList();
			for (Entry<DimensionMap,IdPoint> e : filteredPoints.entrySet())
			{
				IdPoint p = e.getValue().copy();
				p.id = Integer.parseInt(e.getKey().get(trackDimName));
				temp.add(p);
			}
			ROIPlus newMaximaRoi = new ROIPlus(temp, ROIPlus.ROI_POINT);
			newMaximaRois.put(newMap, newMaximaRoi);
		}
		
		return new Pair<TreeMap<DimensionMap,ROIPlus>,TreeMap<DimensionMap,ROIPlus>>(trackRois, newMaximaRois);
	}
}