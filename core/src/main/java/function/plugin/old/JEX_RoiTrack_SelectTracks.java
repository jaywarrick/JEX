package function.plugin.old;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import logs.Logs;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiTrackReader;
import Database.DataWriter.RoiTrackWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import function.roitracker.RoiTrack;
import function.roitracker.RoiTrackStatistics;

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
public class JEX_RoiTrack_SelectTracks extends JEXCrunchable {
	
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
		String result = "Select Roi Tracks";
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
		String result = "Select a sub set of roi tracks based on criteria such as velocity, length or directionality.";
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
		String toolbox = "Roi tracking";
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
		inputNames[0] = new TypeName(ROITRACK, "Tracks to Process");
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return number of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(ROITRACK, "Selected roi tracks");
		
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
		Parameter p1 = new Parameter("% Longest", "Keep only the N% longest tracks (-1 keeps all)", "-1");
		Parameter p2 = new Parameter("% Fastest", "Keep only the N% fastest tracks (-1 keeps all)", "-1");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		return parameterArray;
	}
	
	// ----------------------------------------------------
	// --------- ERROR CHECKING METHODS -------------------
	// ----------------------------------------------------
	
	/**
	 * Returns the status of the input validity checking It is HIGHLY recommended to implement input checking however this can be over-rided by returning false If over-ridden ANY batch function using this function will not be able perform error
	 * checking...
	 * 
	 * @return true if input checking is on
	 */
	@Override
	public boolean isInputValidityCheckingEnabled()
	{
		return true;
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
		Logs.log("Collecting inputs", 1, this);
		JEXData data = inputs.get("Tracks to Process");
		if(!data.getTypeName().getType().equals(JEXData.ROI_TRACK)) return false;
		
		// Run the function
		Logs.log("Running the function", 1, this);
		
		// Read the data
		List<RoiTrack> tracks = RoiTrackReader.readObjectToRoiTrackList(data);
		
		// Get the parameters
		int longest = Integer.parseInt(parameters.getValueOfParameter("% Longest"));
		int fastest = Integer.parseInt(parameters.getValueOfParameter("% Fastest"));
		
		// Calculate the number from the percentage
		longest = (int) (tracks.size() * ((double) longest / 100));
		fastest = (int) (tracks.size() * ((double) fastest / 100));
		
		// Sort by longest
		if(longest > 0)
		{
			// Rank the tracks
			RoiTrackComparator longComp = new RoiTrackComparator(RoiTrackComparator.LENGTH);
			Collections.sort(tracks, longComp);
			
			// Selected the LONGEST longest tracks
			List<RoiTrack> temp = new ArrayList<RoiTrack>(0);
			for (int i = 0; i < longest; i++)
			{
				temp.add(tracks.get(i));
			}
			tracks = temp;
		}
		
		// Sort by fastest
		if(fastest > 0)
		{
			// Rank the tracks
			RoiTrackComparator fastComp = new RoiTrackComparator(RoiTrackComparator.VELOCITY);
			Collections.sort(tracks, fastComp);

			// Selected the FASTEST fastest tracks
			List<RoiTrack> temp = new ArrayList<RoiTrack>(0);
			for (int i = 0; i < fastest; i++)
			{
				temp.add(tracks.get(i));
			}
			tracks = temp;
		}
		
		// Create string and saveString
		DimensionMap dimTemplate = data.getDataMap().firstKey();
		JEXData output = RoiTrackWriter.makeRoiTrackObject(this.outputNames[0].getName(), dimTemplate, tracks);
		output.setDataObjectInfo("Tracks filted with function SELECT TRACKS");
		realOutputs.add(output);
		
		// Return status
		return true;
	}
	
}

class RoiTrackComparator implements Comparator<RoiTrack> {
	
	static int LENGTH = 0;
	static int VELOCITY = 1;
	static int MOSTDIR = 2;
	static int LESSDIR = 3;
	
	private int mode = 0;
	
	public RoiTrackComparator(int mode)
	{
		this.mode = mode;
	}
	
	public int compare(RoiTrack track1, RoiTrack track2)
	{
		if(mode == LENGTH)
		{
			if(track1.size() > track2.size()) return -1;
			if(track1.size() == track2.size()) return 0;
			return 1;
		}
		
		RoiTrackStatistics stat1 = new RoiTrackStatistics(track1);
		RoiTrackStatistics stat2 = new RoiTrackStatistics(track2);
		
		stat1.startAnalysis();
		stat2.startAnalysis();
		
		if(mode == VELOCITY)
		{
			if(stat1.meanVelocity > stat2.meanVelocity) return -1;
			if(stat1.meanVelocity == stat2.meanVelocity) return 0;
			return 1;
		}
		else if(mode == MOSTDIR)
		{
			if(stat1.CI > stat2.CI) return -1;
			if(stat1.CI == stat2.CI) return 0;
			return 1;
		}
		else if(mode == LESSDIR)
		{
			if(stat1.CI < stat2.CI) return -1;
			if(stat1.CI == stat2.CI) return 0;
			return 1;
		}
		
		return 0;
	}
	
}
