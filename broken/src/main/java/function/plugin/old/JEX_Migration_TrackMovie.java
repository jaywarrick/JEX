package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.TrackReader;
import Database.DataWriter.MovieWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.tracker.TracksMovieMaker;
import ij.ImagePlus;
import ij.process.ImageProcessor;
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

import org.monte.media.quicktime.QuickTimeOutputStream;

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
public class JEX_Migration_TrackMovie extends JEXCrunchable {
	
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
		String result = "8. Make track movie";
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
		String result = "Make a movie from tracks and an optional timelapse image";
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
		String toolbox = "Migration";
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
		inputNames[0] = new TypeName(TRACK, "Tracks to visualize");
		inputNames[1] = new TypeName(IMAGE, "Timelapse stack");
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
		defaultOutputNames[0] = new TypeName(MOVIE, "Tracked Movie");
		
		if(outputNames == null)
			return defaultOutputNames;
		return outputNames;
	}
	
	// /**
	// * Return the array of output names
	// *
	// * @return array of output names
	// */
	// public String[] getOutputNames(){
	// String[] inputNames = new String[getNumberOfInputs()];
	// inputNames[0] = "Tracks to Analyze";
	// return inputNames;
	// }
	
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
		Parameter p4 = new Parameter("Track Color", "Choose the color to represent tracks", Parameter.DROPDOWN, new String[] { "Yellow", "Red", "Green", "Blue", "Black" }, 0);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		parameterArray.addParameter(p4);
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
		
		JEXData data1 = inputs.get("Tracks to visualize");
		if(!data1.getTypeName().getType().equals(JEXData.TRACK))
			return false;
		
		JEXData data2 = inputs.get("Timelapse stack");
		if(!data2.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Read the inputs
		List<Trajectory> trajectories = TrackReader.readObjectToTrajectories(data1);
		String[] imPaths = ImageReader.readObjectToImagePathStack(data2);
		
		// Run the function
		Logs.log("Running the function", 1, this);
		String moviePath = makeMovieAndSaveToPath(imPaths, trajectories);
		
		// Make the JEXTrack object
		JEXData movie = MovieWriter.makeMovieObject(outputNames[0].getName(), moviePath);
		realOutputs.add(movie);
		
		// String vhPath = JEXWriter.getDatabaseFolder() + File.separator +
		// JEXWriter.getUniqueTempPath("avi");
		// makeMovieAndSaveToPath(trajectories,imPaths,vhPath);
		//
		// // Collect the outputs
		// Logs.log("Collecting outputs", 1, this);
		// JEXData movie = MovieWriter.makeMovieObject(outputNames[0], vhPath,
		// "Mov");
		// realOutputs.add(movie);
		
		// Return status
		return true;
	}
	
	public String makeMovieAndSaveToPath(String[] imset, List<Trajectory> trajectories)
	{
		// Get the parameters
		String binStr = parameters.getValueOfParameter("Image Scale");
		String trackStr = parameters.getValueOfParameter("Track Scale");
		
		int binImage = (binStr == null) ? 1 : Integer.parseInt(binStr);
		int binTrack = (trackStr == null) ? 1 : Integer.parseInt(trackStr);
		
		String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("avi");
		Logs.log("Saving the movie to path " + path, 1, this);
		
		TracksMovieMaker movieMaker = new TracksMovieMaker(path, imset, trajectories);
		movieMaker.setImageBinning(binImage);
		movieMaker.setTrackBinning(binTrack);
		movieMaker.setCellRadius(1);
		movieMaker.makeMovie();
		
		return path;
	}
	
	public void makeMovieAndSaveToPath(List<Trajectory> trajectories, String[] imList, String path)
	{
		// ------------------------------
		// save the movie of the tracking
		File outMovieFile = new File(path);
		QuickTimeOutputStream newStream = null;
		try
		{
			QuickTimeOutputStream.VideoFormat format = QuickTimeOutputStream.VideoFormat.values()[0];
			newStream = new QuickTimeOutputStream(outMovieFile, format);
		}
		catch (IOException e)
		{
			System.out.println("   Tracker.TrackExtend ---> Not possible to create movie ... ");
		}
		
		int binning = 1;
		try
		{
			binning = Integer.parseInt(parameters.getValueOfParameter("Image Scale"));
		}
		catch (java.lang.NumberFormatException e)
		{}
		float compression = Float.parseFloat(parameters.getValueOfParameter("Compression Rate"));
		
		// add each image one by one
		JEXStatics.statusBar.setProgressPercentage(0);
		for (int k = 0, len = imList.length; (k < len); k++)
		{
			ImagePlus imk = new ImagePlus(imList[k]);
			BufferedImage bimage = trackImage(k, binning, imk, trajectories);
			
			if(k == 0)
			{
				newStream.setVideoCompressionQuality(compression);
				newStream.setTimeScale(8);
			}
			
			try
			{
				newStream.writeFrame(bimage, 1);
				System.out.println("   Tracker.TrackExtend ---> Writing frame " + k);
			}
			catch (IOException e)
			{
				System.out.println("   Tracker.TrackExtend ---> Not possible to write frame " + k);
			}
			
			// Status bar
			int percentage = (int) (100 * ((double) k / (double) imList.length));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		try
		{
			newStream.finish();
			newStream.close();
		}
		catch (IOException e)
		{
			System.out.println("   Tracker.TrackExtend ---> Not possible to finalize movie ");
		}
		System.out.println("   Tracker.TrackExtend ---> Tracks movie saved in " + outMovieFile.getPath());
	}
	
	/**
	 * Return a buffered image with the tracks printed on the image IMPLUS in a pretty manner
	 * 
	 * @param i
	 * @param implus
	 * @return
	 */
	private BufferedImage trackImage(int i, int binning, ImagePlus implus, List<Trajectory> trajectories)
	{
		ImageProcessor imp = implus.getProcessor();
		imp = imp.resize((implus.getWidth() / binning));
		
		// Get the color chosen
		String colorStr = parameters.getValueOfParameter("Track Color");
		Color color = Color.YELLOW;
		if(colorStr.equals("Yellow"))
			color = Color.YELLOW;
		else if(colorStr.equals("Red"))
			color = Color.RED;
		else if(colorStr.equals("Green"))
			color = Color.GREEN;
		else if(colorStr.equals("Blue"))
			color = Color.BLUE;
		else if(colorStr.equals("Black"))
			color = Color.BLACK;
		
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(imp.getWidth(), imp.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(imp.getBufferedImage(), 0, 0, null);
		
		// plot current points
		for (int k = 0, len = trajectories.size(); k < len; k++)
		{
			Trajectory trajK = trajectories.get(k);
			
			g.setColor(color);
			int frame1 = trajK.initial();
			Point f2 = trajK.getPoint(frame1);
			Point s2 = f2;
			
			// Plot first point
			while (s2 != null && frame1 < i)
			{
				Point toDisplay1 = new Point((int) (f2.getX() / binning), (int) (f2.getY() / binning));
				Point toDisplay2 = new Point((int) (s2.getX() / binning), (int) (s2.getY() / binning));
				
				g.fillOval(toDisplay1.x, toDisplay1.y, 3, 3);
				g.drawLine(toDisplay1.x, toDisplay1.y, toDisplay2.x, toDisplay2.y);
				
				f2 = s2;
				frame1 = trajK.next(frame1);
				s2 = trajK.getPoint(frame1);
			}
			
		}
		
		g.dispose();
		
		return bimage;
	}
	
}
