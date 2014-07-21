package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.MovieWriter;
import Database.DataWriter.RoiTrackWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.roitracker.RoiTrack;
import function.roitracker.RoiTrackExtend;
import function.roitracker.RoiTrackMovieMaker;
import ij.ImagePlus;
import image.roi.ROIPlus;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import logs.Logs;
import tables.DimensionMap;

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
public class JEX_RoiTrack_TrackRois extends JEXCrunchable {
	
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
		String result = "Track Rois";
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
		String result = "Link Roi positions in a timelapse image together to form tracks.";
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
		inputNames[0] = new TypeName(ROI, "Cell rois");
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
		defaultOutputNames = new TypeName[2];
		defaultOutputNames[0] = new TypeName(JEXData.ROI_TRACK, "RoiTracks");
		defaultOutputNames[1] = new TypeName(JEXData.MOVIE, "Raw movie");
		
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
		Parameter p1 = new Parameter("Max Displacement", "Maximum diplacement of a cell in pixels", "40");
		Parameter p2 = new Parameter("Max Dissapearance", "Maximum number of frames a cell can dissapear", "2");
		Parameter p3 = new Parameter("Use interpolation", "Predict where the next roi should be, for better tracking of constantly migrating cells", "false");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
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
		// Collect the jex inputs
		JEXData data1 = inputs.get("Cell rois");
		if(!data1.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		JEXData data2 = inputs.get("Timelapse");
		if(!data2.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Transform the JEX inputs into imageJ inputs
		TreeMap<DimensionMap,String> pathMap = ImageReader.readObjectToImagePathTable(data2);
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(data1);
		
		// Get the new parameters
		int maxDisplacement   = Integer.parseInt(parameters.getValueOfParameter("Max Displacement"));
		int maxDissapear      = Integer.parseInt(parameters.getValueOfParameter("Max Dissapearance"));
		boolean interpolation = Boolean.parseBoolean(parameters.getValueOfParameter("Use interpolation"));
		
		// Create the roi tracker class
		RoiTrackExtend extender = new RoiTrackExtend();
		extender.setMaxDisplacement(maxDisplacement);
		extender.setMaxDissapearance(maxDissapear);
		extender.setUseInterpolation(interpolation);
		
		// Loop through the images
		Set<DimensionMap> set    = pathMap.keySet() ;
		ArrayList<String> images = new ArrayList<String>(0);
		int frame                = 0 ;
		DimensionMap dimTemplate = null;
		for(DimensionMap dim: set)
		{
			// Get the image
			String imPath = pathMap.get(dim);
			images.add(imPath);
			ImagePlus im = new ImagePlus(imPath);
			
			// Get the Rois that match this image
			List<ROIPlus> rois = getRoisMatchingTheDimensionMap(dim, roiMap);
			
			// Extend the tracks by the found rois
			extender.extendRoiTracks(rois, im, frame);
			
			// frame update
			frame += 1;
			Logs.log("Track extended to frame "+frame, 1, this);
			
			// Update the dimTemplate
			if (dim != null) dimTemplate = dim;
		}
		
		// Get the tracks
		List<RoiTrack> tracks = extender.getTracks();
		
		// Make the raw movie
		String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("avi");
		RoiTrackMovieMaker movieMaker = new RoiTrackMovieMaker(path, images, tracks);
		movieMaker.setShowTrail(true);
		path = movieMaker.makeMovie();
		Logs.log("Saving the movie to path " + path, 1, this);
		
		// Make the JEX outputs
		JEXData output1 = RoiTrackWriter.makeRoiTrackObject(this.outputNames[0].getName(), dimTemplate, tracks);
		JEXData output2 = MovieWriter.makeMovieObject(this.outputNames[1].getName(), path);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		
		// Return status
		return true;
	}
	
	/**
	 * Returns the list of ROIs for an image at a dimensionMap DIM
	 * 
	 * @param dim
	 * @param roiMap
	 * @return
	 */
	private List<ROIPlus> getRoisMatchingTheDimensionMap(DimensionMap dim, TreeMap<DimensionMap,ROIPlus> roiMap)
	{
		ArrayList<ROIPlus> rois = new ArrayList<ROIPlus>(0);
		
		if (roiMap == null) return rois;
		
		// Loop through the roiMap and keep the entries for which the dimensionmaps overlap
		Set<DimensionMap> keys = roiMap.keySet();
		for (DimensionMap key : keys)
		{
			// Test the inclusion of DIM in KEY
			if (dimensionMapIncludes(key, dim))
			{
				rois.add(roiMap.get(key));
			}
		}
		
		return rois;
	}
	
	/**
	 * Returns true when the dimensionMap SMALLMAP is fully included in the dimensionMap LARGEMAP
	 * 
	 * @param largeMap
	 * @param smallMap
	 * @return
	 */
	private boolean dimensionMapIncludes(DimensionMap largeMap, DimensionMap smallMap)
	{
		// Check nullity
		if (largeMap == null || smallMap == null) return false;

		// Setup variable
		boolean inclusion = true;
		
		// loop through the dimensions
		for (String key: smallMap.keySet())
		{
			// get the value
			String value2 = smallMap.get(key);
			
			// Get the value for the large Map
			String value1 = largeMap.get(key);
			
			if (!value2.equals(value1)) inclusion = false;
		}
		
		return inclusion;
	}
	
}

