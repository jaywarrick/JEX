package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import image.roi.ROIPlus;
import image.roi.Trajectory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import jex.statics.JEXStatics;
import logs.Logs;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataReader.RoiTrackReader;
import Database.DataReader.TrackReader;
import Database.DataWriter.MovieWriter;
import Database.DataWriter.RoiTrackWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import ch.randelshofer.media.quicktime.QuickTimeOutputStream;
import function.ExperimentalDataCrunch;
import function.roitracker.RoiTrack;
import function.roitracker.RoiTrackMovieMaker;
import function.tracker.TracksMovieMaker;

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
public class JEX_RoiTrack_MakeMovie extends ExperimentalDataCrunch {
	
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
		String result = "Make Roi Track Movie";
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
		String result = "Make a movie from roi tracks and a timelapse image";
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
		TypeName[] inputNames = new TypeName[2];
		inputNames[0] = new TypeName(JEXData.ROI_TRACK, "Tracks");
		inputNames[1] = new TypeName(IMAGE, "Timelapse");
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
		defaultOutputNames[0] = new TypeName(MOVIE, "Movie");
		
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
		Parameter p1 = new Parameter("Frame Rate", "Frames per second", "7");
		Parameter p2 = new Parameter("Compression Rate", "Compression quality", "0.9");
		Parameter p3 = new Parameter("Image Scale", "Bin the images for smaller movie", "1");
		Parameter p4 = new Parameter("Track Color", "Choose the color to represent tracks", Parameter.DROPDOWN, new String[] { "Default" , "Yellow", "Red", "Green", "Blue", "Black" }, 0);
		Parameter p5 = new Parameter("Roi color", "Choose the color to represent rois", Parameter.DROPDOWN, new String[] { "Default" , "Yellow", "Red", "Green", "Blue", "Black" }, 0);
		
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
		
		JEXData data1 = inputs.get("Tracks");
		if(!data1.getTypeName().getType().equals(JEXData.ROI_TRACK))
			return false;
		
		JEXData data2 = inputs.get("Timelapse");
		if(!data2.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Collect the parameters
		int frameRate         = Integer.parseInt(parameters.getValueOfParameter("Frame Rate"));
		float compressionRate = Float.parseFloat(parameters.getValueOfParameter("Compression Rate"));
		int imageScale        = Integer.parseInt(parameters.getValueOfParameter("Image Scale"));
		String trackColorStr  = parameters.getValueOfParameter("Track Color");
		String roiColorStr    = parameters.getValueOfParameter("Roi color");
		
		// Get the color chosen
		Color trackColor = RoiTrackMovieMaker.DEFAULT_TRACK_COLOR;
		if(trackColorStr.equals("Yellow"))
			trackColor = Color.YELLOW;
		else if(trackColorStr.equals("Red"))
			trackColor = Color.RED;
		else if(trackColorStr.equals("Green"))
			trackColor = Color.GREEN;
		else if(trackColorStr.equals("Blue"))
			trackColor = Color.BLUE;
		else if(trackColorStr.equals("Black"))
			trackColor = Color.BLACK;
		else if(trackColorStr.equals("Default"))
				trackColor = null;

		// Get the color chosen
		Color roiColor = RoiTrackMovieMaker.DEFAULT_ROI_COLOR;
		if(roiColorStr.equals("Yellow"))
			roiColor = Color.YELLOW;
		else if(roiColorStr.equals("Red"))
			roiColor = Color.RED;
		else if(roiColorStr.equals("Green"))
			roiColor = Color.GREEN;
		else if(roiColorStr.equals("Blue"))
			roiColor = Color.BLUE;
		else if(roiColorStr.equals("Black"))
			roiColor = Color.BLACK;
		else if(roiColorStr.equals("Default"))
			roiColor = null;
		
		// Read the inputs
		List<RoiTrack> tracks = RoiTrackReader.readObjectToRoiTrackList(data1);
		List<String>   images = ImageReader.readObjectToImagePathList(data2);
		Logs.log("Found " + tracks.size(), 1, this);
		
		// Run the function
		Logs.log("Running the function", 1, this);
		
		// Create the movie path
		String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("avi");
		
		// Setup up the movie maker
		RoiTrackMovieMaker movieMaker = new RoiTrackMovieMaker(path, images, tracks);
		movieMaker.setShowTrail(true);
		movieMaker.setCompression(compressionRate);
		movieMaker.setFrameRate(frameRate);
		movieMaker.setBinning(imageScale);
		movieMaker.setRoiColor(roiColor);
		movieMaker.setTrackColor(trackColor);

		// Make the movie
		path = movieMaker.makeMovie();
		Logs.log("Saving the movie to path " + path, 1, this);
		
		// Make the JEX outputs
		JEXData output1 = MovieWriter.makeMovieObject(this.outputNames[0].getName(), path);

		// Set the outputs
		realOutputs.add(output1);

		// Return status
		return true;
	}
	
}
