package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ValueReader;
import Database.DataWriter.MovieWriter;
import Database.DataWriter.TrackWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import function.tracker.MonteCarloWalker;
import image.roi.Trajectory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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
public class JEX_Migration_ModelTrack extends JEXCrunchable {
	
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
		String result = "Generate tracks";
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
		String result = "Generate random tracks from biased brownian motion.";
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
		TypeName[] inputNames = new TypeName[4];
		inputNames[0] = new TypeName(VALUE, "Mean Velocity");
		inputNames[1] = new TypeName(VALUE, "Mean Angle");
		inputNames[2] = new TypeName(VALUE, "Velocity variance");
		inputNames[3] = new TypeName(VALUE, "Angle variance");
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
		defaultOutputNames[0] = new TypeName(TRACK, "Model Tracks");
		defaultOutputNames[1] = new TypeName(MOVIE, "Movie");
		
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
		Parameter p1 = new Parameter("Number cells", "Number of random walkers", "10");
		Parameter p2 = new Parameter("Steps", "Number of time steps", "100");
		Parameter p3 = new Parameter("Seconds Per Step", "Seconds between each frame", "1");
		
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
		// Collect the inputs
		JEXData data1 = inputs.get("Mean Velocity");
		Type type1 = data1.getTypeName().getType();
		if(!type1.matches(JEXData.VALUE) || !type1.matches(JEXData.LABEL))
			return false;
		
		JEXData data2 = inputs.get("Mean Angle");
		Type type2 = data2.getTypeName().getType();
		if(!type2.matches(JEXData.VALUE) || !type2.matches(JEXData.LABEL))
			return false;
		
		JEXData data3 = inputs.get("Velocity variance");
		Type type3 = data3.getTypeName().getType();
		if(!type3.matches(JEXData.VALUE) || !type3.matches(JEXData.LABEL))
			return false;
		
		JEXData data4 = inputs.get("Angle variance");
		Type type4 = data4.getTypeName().getType();
		if(!type4.matches(JEXData.VALUE) || !type4.matches(JEXData.LABEL))
			return false;
		
		String mvStr = null;
		if(type1.equals(JEXData.VALUE))
			mvStr = ValueReader.readValueObject(data1);
		if(type1.equals(JEXData.LABEL))
			mvStr = ValueReader.readValueObject(data1);
		
		String maStr = null;
		if(type2.equals(JEXData.VALUE))
			maStr = ValueReader.readValueObject(data2);
		if(type2.equals(JEXData.LABEL))
			maStr = ValueReader.readValueObject(data2);
		
		String vvStr = null;
		if(type3.equals(JEXData.VALUE))
			vvStr = ValueReader.readValueObject(data3);
		if(type3.equals(JEXData.LABEL))
			vvStr = ValueReader.readValueObject(data3);
		
		String vaStr = null;
		if(type4.equals(JEXData.VALUE))
			vaStr = ValueReader.readValueObject(data4);
		if(type4.equals(JEXData.LABEL))
			vaStr = ValueReader.readValueObject(data4);
		
		// Run the function
		float velocity = Float.parseFloat(mvStr);
		float angle = Float.parseFloat(maStr);
		float sigmaA = Float.parseFloat(vaStr);
		float sigmaV = Float.parseFloat(vvStr);
		
		// ///// Collecting parameters
		int nb = Integer.parseInt(parameters.getValueOfParameter("Number cells"));
		int nbSteps = Integer.parseInt(parameters.getValueOfParameter("Steps"));
		float deltaT = Float.parseFloat(parameters.getValueOfParameter("Seconds Per Step"));
		
		// ///// Starting function
		System.out.println("   JEX_ModelTrack ---> Making the random walkers... ");
		MonteCarloWalker[] walkers = makeNWalkers(nb, nbSteps, MonteCarloWalker.BIASED_RANDOM_ANGLE_AND_VELOCITY, velocity, angle, sigmaV, sigmaA, deltaT);
		
		// make trajectories
		System.out.println("   JEX_ModelTrack ---> Making the trajectories... ");
		List<Trajectory> trajectories = new ArrayList<Trajectory>(0);
		for (MonteCarloWalker mcw : walkers)
		{
			Trajectory traj = mcw.makeTrajectory(mcw.walk);
			traj.update();
			String print = traj.pmapList();
			Logs.log("Trajectory = " + print, 1, this);
			trajectories.add(traj);
		}
		
		// Make the JEXTrack object
		JEXData output1 = TrackWriter.makeTracksObject(outputNames[0].getName(), trajectories);
		
		// Make the movie object
		String newMoviePath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("avi");
		Logs.log("Saving the movie to path " + newMoviePath, 1, this);
		this.makeMovie(walkers, newMoviePath);
		JEXData output2 = MovieWriter.makeMovieObject(outputNames[1].getName(), newMoviePath);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		
		// Return status
		return true;
	}
	
	public MonteCarloWalker[] makeNWalkers(int n, int nbstep, int nature, float velocityBias, float angleBias, float sigmaX, float sigmaY, float deltaT)
	{
		MonteCarloWalker[] walkers = new MonteCarloWalker[n];
		for (int i = 0; i < n; i++)
		{
			// Make 2D gaussian random walks
			// MonteCarloWalker mcw = new
			// MonteCarloWalker(velocityBias,angleBias,sigmaX,sigmaY,deltaT);
			// Make 2D random walk with velocity and anlge picked randomly
			MonteCarloWalker mcw = new MonteCarloWalker(nature, velocityBias, sigmaX, angleBias, sigmaY, deltaT);
			
			mcw.walk(nbstep);
			walkers[i] = mcw;
		}
		return walkers;
	}
	
	public BufferedImage makeImage(MonteCarloWalker[] walkers, int index)
	{
		int width = 800;
		int height = 600;
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(width, height, type);
		Graphics g = bimage.createGraphics();
		g.setColor(Color.white);
		g.drawRect(0, 0, width, height);
		
		int minX = 0;
		int minY = 0;
		int maxX = 0;
		int maxY = 0;
		for (MonteCarloWalker mcw : walkers)
		{
			Rectangle r = mcw.getBounds();
			int X1 = (int) r.getX();
			int Y1 = (int) r.getY();
			int X2 = (int) r.getWidth() + X1;
			int Y2 = (int) r.getHeight() + Y1;
			minX = (minX > X1) ? X1 : minX;
			maxX = (maxX < X2) ? X2 : maxX;
			minY = (minY > Y1) ? Y1 : minY;
			maxY = (maxY < Y2) ? Y2 : maxY;
		}
		double setDX = width;
		double setDY = height;
		double thisDX = (maxX - minX);
		double thisDY = (maxY - minY);
		double scaleX = setDX / thisDX;
		double scaleY = setDY / thisDY;
		double scale = Math.min(scaleX, scaleY);
		if(scaleX > scaleY)
		{
			scale = scaleY;
			minX = minX - (int) (((width - thisDX * scaleY) / 2) / scaleY);
		}
		else
		{
			scale = scaleX;
			minY = minY - (int) (((height - thisDY * scaleX) / 2) / scaleX);
		}
		
		Random random = new Random();
		for (int i = 0, len = walkers.length; i < len; i++)
		{
			Color nextColor = Color.getHSBColor(random.nextFloat(), 1.0F, 1.0F);
			// System.out.println("   JEX_ModelTrack ---> Adding track "+i+" at frame "+index+" to movie, with color "+nextColor+"... ");
			walkers[i].displayShadeTrack(g, index, scale, height, minX, minY, nextColor);
		}
		
		g.dispose();
		return bimage;
	}
	
	/**
	 * Save the movie of the tracking
	 * 
	 * @param path
	 */
	public void makeMovie(MonteCarloWalker[] walkers, String outAVIPath)
	{
		// ------------------------------
		// save the movie of the tracking
		File outMovieFile = new File(outAVIPath);
		QuickTimeOutputStream newStream = null;
		try
		{
			QuickTimeOutputStream.VideoFormat format = QuickTimeOutputStream.VideoFormat.values()[0];
			newStream = new QuickTimeOutputStream(outMovieFile, format);
		}
		catch (IOException e)
		{
			System.out.println("   JEX_ModelTrack ---> Not possible to create movie ... ");
		}
		
		// add each image one by one
		int nbFrame = walkers[0].walk.size();
		for (int k = 0; (k < nbFrame); k++)
		{
			BufferedImage bimage = makeImage(walkers, k);
			
			if(k == 0)
			{
				newStream.setVideoCompressionQuality((float) 0.9);
				newStream.setTimeScale(8);
			}
			
			try
			{
				newStream.writeFrame(bimage, 1);
				System.out.println("   JEX_ModelTrack ---> Writing frame " + k);
			}
			catch (IOException e)
			{
				System.out.println("   JEX_ModelTrack ---> Not possible to write frame " + k);
			}
		}
		try
		{
			newStream.finish();
			newStream.close();
		}
		catch (IOException e)
		{
			System.out.println("   JEX_ModelTrack ---> Not possible to finalize movie ");
		}
		System.out.println("   JEX_ModelTrack ---> Tracks movie saved in " + outMovieFile.getPath());
	}
}
