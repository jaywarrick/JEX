package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import image.roi.Trajectory;
import image.roi.Vect;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jex.arrayView.ImageDisplay;
import jex.arrayView.ImageDisplayController;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.MovieWriter;
import Database.DataWriter.TrackWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import ch.randelshofer.media.quicktime.QuickTimeOutputStream;
import function.ExperimentalDataCrunch;
import guiObject.JParameterPanel;

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
public class JEX_Migration_ManualTrack extends ExperimentalDataCrunch {
	
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
		String result = "5. Manual tracking";
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
		String result = "Tools for manual tracking";
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
		TypeName[] inputNames = new TypeName[1];
		
		inputNames[0] = new TypeName(IMAGE, "Timelapse");
		
		return inputNames;
	}
	
	/**
	 * Return the number of outputs returned by this function
	 * 
	 * @return name of outputs
	 */
	@Override
	public TypeName[] getOutputs()
	{
		defaultOutputNames = new TypeName[2];
		defaultOutputNames[0] = new TypeName(TRACK, "Tracks");
		defaultOutputNames[1] = new TypeName(MOVIE, "Track Movie");
		
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
		Parameter p1 = new Parameter("Frames before", "Number of frames to follow the cell before the current frame", "15");
		Parameter p2 = new Parameter("Frames after", "Number of frames to follow the cell after the current frame", "15");
		Parameter p3 = new Parameter("Semi-auto", "Enhance using semi-auto tracking", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p4 = new Parameter("Cell Radius", "Radius of a cell", "15");
		Parameter p5 = new Parameter("Max. Mov.", "Maximum cell movement", "5");
		
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
		JEXData data1 = inputs.get("Timelapse");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Run the function
		ManualTrackingFunction graphFunc = new ManualTrackingFunction(entry, data1, outputNames, parameters);
		
		// Set the outputs
		realOutputs.add(graphFunc.output1);
		realOutputs.add(graphFunc.output2);
		
		// Return status
		return true;
	}
}

class ManualTrackingFunction implements ActionListener, AdjustmentListener, ListSelectionListener, ImageDisplayController {
	
	// Outputs
	public JEXData output1;
	public JEXData output2;
	
	// Gui
	JDialog functionFrame;
	JPanel contentPane;
	JScrollBar timeBar;
	ImageDisplay imageDisplay;
	JPanel rightpanel;
	JPanel paramPanel;
	JList trajecList;
	JButton addTrajectoryButton;
	JButton removeTrajectoryButton;
	JButton doneButton;
	JParameterPanel p1;
	JParameterPanel p2;
	JParameterPanel p3;
	JParameterPanel p4;
	JParameterPanel p5;
	
	// Parameters
	int before = 15;
	int after = 15;
	int radius = 15;
	int movement = 5;
	boolean semiAuto = false;
	
	// Variables used during the function steps
	private ImagePlus im;
	private int index = 0;
	private Trajectory currentTrack;
	
	// Input
	JEXEntry entry;
	JEXData data;
	ParameterSet params;
	TypeName[] outputNames;
	List<String> images;
	Trajectory[] trajectories;
	
	ManualTrackingFunction(JEXEntry entry, JEXData data, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.entry = entry;
		this.data = data;
		this.params = parameters;
		this.outputNames = outputNames;
		
		// //// Get params
		getParams();
		
		// Prepare function
		images = ImageReader.readObjectToImagePathList(data);
		
		// Prepare the graphics
		imageDisplay = new ImageDisplay(this, "Manual track");
		index = 0;
		displayImage(index);
		
		// Scroll bar
		timeBar = new JScrollBar(Adjustable.HORIZONTAL, 0, 1, 0, images.size());
		timeBar.addAdjustmentListener(this);
		
		// center panel
		contentPane = new JPanel();
		contentPane.setBackground(DisplayStatics.background);
		contentPane.setLayout(new BorderLayout());
		contentPane.add(imageDisplay, BorderLayout.CENTER);
		contentPane.add(timeBar, BorderLayout.PAGE_END);
		
		// Param panel
		p1 = new JParameterPanel(parameters.getParameter("Semi-auto"));
		p2 = new JParameterPanel(parameters.getParameter("Cell Radius"));
		p3 = new JParameterPanel(parameters.getParameter("Frames before"));
		p4 = new JParameterPanel(parameters.getParameter("Frames after"));
		p5 = new JParameterPanel(parameters.getParameter("Max. Mov."));
		p1.panel().setBackground(DisplayStatics.background);
		p2.panel().setBackground(DisplayStatics.background);
		p3.panel().setBackground(DisplayStatics.background);
		p4.panel().setBackground(DisplayStatics.background);
		p5.panel().setBackground(DisplayStatics.background);
		paramPanel = new JPanel();
		paramPanel.setBackground(DisplayStatics.background);
		paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.PAGE_AXIS));
		paramPanel.add(p1.panel());
		paramPanel.add(p2.panel());
		paramPanel.add(p3.panel());
		paramPanel.add(p4.panel());
		paramPanel.add(p5.panel());
		paramPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		// Trajectory list
		trajecList = new JList();
		trajecList.setAlignmentX(Component.LEFT_ALIGNMENT);
		trajecList.setCellRenderer(new TrajectoryCellRenderer());
		trajecList.getSelectionModel().addListSelectionListener(this);
		
		// Buttons
		addTrajectoryButton = new JButton("Add track");
		addTrajectoryButton.addActionListener(this);
		addTrajectoryButton.setMaximumSize(new Dimension(150, 20));
		addTrajectoryButton.setPreferredSize(new Dimension(150, 20));
		addTrajectoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		removeTrajectoryButton = new JButton("Remove track");
		removeTrajectoryButton.addActionListener(this);
		removeTrajectoryButton.setMaximumSize(new Dimension(150, 20));
		removeTrajectoryButton.setPreferredSize(new Dimension(150, 20));
		removeTrajectoryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		doneButton = new JButton("Finish");
		doneButton.addActionListener(this);
		doneButton.setMaximumSize(new Dimension(150, 20));
		doneButton.setPreferredSize(new Dimension(150, 20));
		doneButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		JLabel ptitle = new JLabel("Parameters");
		ptitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		ptitle.setFont(FontUtility.boldFont);
		JLabel ttitle = new JLabel("Trajectories");
		ttitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		ttitle.setFont(FontUtility.boldFont);
		
		rightpanel = new JPanel();
		rightpanel.setBackground(DisplayStatics.background);
		rightpanel.setLayout(new BoxLayout(rightpanel, BoxLayout.PAGE_AXIS));
		rightpanel.add(ptitle);
		rightpanel.add(paramPanel);
		rightpanel.add(ttitle);
		rightpanel.add(trajecList);
		rightpanel.add(addTrajectoryButton);
		rightpanel.add(removeTrajectoryButton);
		rightpanel.add(doneButton);
		rightpanel.add(Box.createVerticalGlue());
		
		functionFrame = new JDialog();
		functionFrame.setBounds(50, 50, 900, 700);
		functionFrame.setModal(true);
		functionFrame.getContentPane().setLayout(new BorderLayout());
		functionFrame.getContentPane().add(contentPane, BorderLayout.CENTER);
		functionFrame.getContentPane().add(rightpanel, BorderLayout.LINE_END);
		functionFrame.setVisible(true);
	}
	
	// ----------------------------------------------------
	// --------- NORMAL FUNCTION --------------------------
	// ----------------------------------------------------
	
	/**
	 * Retrieve the parameters of the function
	 */
	private void getParams()
	{
		semiAuto = Boolean.parseBoolean(params.getValueOfParameter("Semi-auto"));
		radius = (int) Double.parseDouble(params.getValueOfParameter("Cell Radius"));
		before = (int) Double.parseDouble(params.getValueOfParameter("Frames before"));
		after = (int) Double.parseDouble(params.getValueOfParameter("Frames after"));
		movement = (int) Double.parseDouble(params.getValueOfParameter("Max. Mov."));
	}
	
	/**
	 * Display the image at location INDEX
	 * 
	 * @param index
	 */
	private void displayImage(int index)
	{
		im = new ImagePlus(images.get(index));
		imageDisplay.setImage(im);
	}
	
	/**
	 * Get the list of trajectories
	 * 
	 * @return
	 */
	private Trajectory[] getTrajectories()
	{
		if(trajectories == null)
		{
			trajectories = new Trajectory[0];
		}
		return trajectories;
	}
	
	/**
	 * Set trajectories
	 * 
	 * @param trajectories
	 */
	private void setTrajectories(Trajectory[] trajectories)
	{
		this.trajectories = trajectories;
	}
	
	/**
	 * Add a trajectory to the list
	 * 
	 * @param track
	 */
	private void addTrajectoryToList(Trajectory track)
	{
		// Increase the trajectory size
		Trajectory[] trajs = Arrays.copyOf(getTrajectories(), getTrajectories().length + 1);
		trajs[trajs.length - 1] = track;
		setTrajectories(trajs);
		
		// reset the JList
		DefaultListModel newModel = new DefaultListModel();
		for (Trajectory t : trajs)
		{
			newModel.addElement(t);
		}
		trajecList.setModel(newModel);
		trajecList.repaint();
	}
	
	/**
	 * Make the movie of the tracking
	 */
	public void makeMovie()
	{
		String path = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("avi");
		Logs.log("Saving the movie to path " + path, 1, this);
		
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
			Logs.log("Not possible to create movie ... ", 1, this);
		}
		
		int binning = 2;
		float compression = (float) 0.7;
		
		// add each image one by one
		for (int k = 0, len = images.size(); (k < len); k++)
		{
			ImagePlus imk = new ImagePlus(images.get(k));
			BufferedImage bimage = trackImage(k, binning, imk, getTrajectories());
			
			if(k == 0)
			{
				newStream.setVideoCompressionQuality(compression);
				newStream.setTimeScale(8);
			}
			
			try
			{
				newStream.writeFrame(bimage, 1);
				Logs.log("Writing frame " + k, 1, this);
			}
			catch (IOException e)
			{
				Logs.log("Not possible to write frame " + k, 1, this);
			}
			
			// Status bar
			int percentage = (int) (100 * ((double) k / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		try
		{
			newStream.finish();
			newStream.close();
		}
		catch (IOException e)
		{
			Logs.log("Not possible to finalize movie ", 1, this);
		}
		Logs.log("Tracks movie saved in " + outMovieFile.getPath(), 1, this);
		
		// Make the JEXTrack object
		output2 = MovieWriter.makeMovieObject(outputNames[1].getName(), path);
	}
	
	/**
	 * Make the tracks object
	 */
	public void makeTracks()
	{
		output1 = TrackWriter.makeTracksObject(outputNames[0].getName(), getTrajectories());
	}
	
	/**
	 * Return a buffered image with the tracks printed on the image IMPLUS in a pretty manner
	 * 
	 * @param i
	 * @param implus
	 * @return
	 */
	private BufferedImage trackImage(int i, int binning, ImagePlus implus, Trajectory[] trajectories)
	{
		ImageProcessor imp = implus.getProcessor();
		imp = imp.resize((implus.getWidth() / binning));
		
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(imp.getWidth(), imp.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(imp.getBufferedImage(), 0, 0, null);
		
		// plot current points
		for (int k = 0, len = trajectories.length; k < len; k++)
		{
			
			Trajectory trajK = trajectories[k];
			List<Point> trajAllPoints = trajK.getPointsAfter(i);
			
			for (int kk = 0, length = trajAllPoints.size(); kk < length; kk++)
			{
				Point newP = trajAllPoints.get(kk);
				int shade = 255 * (kk) / (length);
				Color c = new Color(shade, shade, 255);
				g.setColor(c);
				g.drawRect(newP.x - 2, newP.y - 2, 2 * 2, 2 * 2);
			}
			
			g.setColor(Color.YELLOW);
			int frame1 = trajK.initial();
			Point f2 = trajK.getPoint(frame1);
			Point s2 = f2;
			
			while (s2 != null)
			{
				g.fillOval(f2.x, f2.y, 3, 3);
				g.drawLine(f2.x, f2.y, s2.x, s2.y);
				f2 = s2;
				frame1 = trajK.next(frame1);
				s2 = trajK.getPoint(frame1);
				if(frame1 > i)
					s2 = null;
			}
			
		}
		
		g.dispose();
		
		return bimage;
	}
	
	// ----------------------------------------------------
	// --------- SEMI AUTOMATIC ---------------------------
	// ----------------------------------------------------
	
	/**
	 * Extract an image or radius CELLRADIUS around point P in image IMAGE
	 * 
	 * @param image
	 * @param p
	 * @param cellRadius
	 * @return
	 */
	private ImagePlus extractImageAroundPoint(ImagePlus image, Point p, int cellRadius)
	{
		ImageProcessor resultIMP = image.getProcessor().duplicate();
		
		// set the roi in the big image
		resultIMP.setRoi(new Rectangle(p.x - cellRadius, p.y - cellRadius, 2 * cellRadius + 1, 2 * cellRadius + 1));
		
		// crop the big image
		resultIMP = resultIMP.crop();
		
		return new ImagePlus("", resultIMP);
	}
	
	/**
	 * Return the convolution score matrix of image CELLIMAGE in image IMAGE, around point P in a radius AREARADIUS
	 * 
	 * @param image
	 * @param cellImage
	 * @param p
	 * @param areaRadius
	 * @return matrix
	 */
	private Vect convolveNeutrophilAroundPointInImage(ImagePlus image, ImagePlus cellImage, Point p, int areaRadius)
	{
		// Setup the variables
		int maxi = 0;
		int maxj = 0;
		float currentmax = 0;
		ImageProcessor iimp = image.getProcessor();
		ImageProcessor cimp = cellImage.getProcessor();
		
		// Convolve around the area of reasearch
		for (int i = -areaRadius; i <= areaRadius; i++)
		{
			for (int j = -areaRadius; j <= areaRadius; j++)
			{
				
				// Get the score of the convolution
				Point pp = new Point(p.x + i - cellImage.getWidth() / 2, p.y + j - cellImage.getHeight() / 2);
				float score = scoreConvolution(iimp, cimp, pp);
				
				// Check if it is larger than the previous max
				// if it is, update the new max
				if(score > currentmax)
				{
					maxi = i;
					maxj = j;
					currentmax = score;
				}
			}
		}
		
		// return the matrix
		return new Vect(maxi, maxj);
	}
	
	/**
	 * Return the score of the convolution of image CELLIMAGE in image IMAGE at location P
	 * 
	 * @param image
	 * @param cellImage
	 * @param p
	 * @return
	 */
	private float scoreConvolution(ImageProcessor image, ImageProcessor cellImage, Point p)
	{
		// setup result
		float result = 0;
		
		// dimensions
		int w = cellImage.getWidth();
		int h = cellImage.getHeight();
		int maxW = image.getWidth();
		int maxH = image.getHeight();
		
		// normalization area
		float area = cellImage.getWidth() * cellImage.getHeight();
		
		// normalize
		float mean1 = 0;
		float mean2 = 0;
		for (int i = 0; i < w; i++)
		{
			for (int j = 0; j < h; j++)
			{
				int currentX = p.x + i;
				int currentY = p.y + j;
				
				// Find the multiplication of the pixels...
				// if outside of image return 0
				if(currentX > 0 && currentX < maxW && currentY > 0 && currentY < maxH)
					mean1 = mean1 + image.getPixel(currentX, currentY);
				
				mean2 = mean2 + cellImage.getPixel(i, j);
			}
		}
		mean1 = mean1 / area;
		mean2 = mean2 / area;
		
		// Do the convolution
		for (int i = 0; i < w; i++)
		{
			for (int j = 0; j < h; j++)
			{
				int currentX = p.x + i;
				int currentY = p.y + j;
				
				// Find the multiplication of the pixels...
				// if outside of image return 0
				float f = 0;
				if(currentX > 0 && currentX < maxW && currentY > 0 && currentY < maxH)
					f = (image.getPixel(currentX, currentY) - mean1) * (cellImage.getPixel(i, j) - mean2);
				else
					f = 0;
				
				// add pixel multiplcation to score
				result = result + f / area;
			}
		}
		
		return result;
	}
	
	/**
	 * Track the current neutrophil
	 */
	private Trajectory trackNeutrophilForwardFromPoint(Point startingPoint, int index, int nbFrames, Trajectory currentTrack)
	{
		// Setup the gliding variables
		ImagePlus image1 = new ImagePlus(images.get(index));
		ImagePlus currentCell = extractImageAroundPoint(image1, startingPoint, radius);
		Point maxPoint = startingPoint;
		
		int endFrame = Math.min(images.size(), index + nbFrames);
		for (int i = index + 1; i < endFrame; i++)
		{
			// Get the next image
			ImagePlus image = new ImagePlus(images.get(i));
			
			// Convolve the cell in the search area of the next image
			Vect maxDisp = convolveNeutrophilAroundPointInImage(image, currentCell, maxPoint, movement);
			
			// Find the maximum displacement
			maxPoint = new Point(maxPoint.x + (int) maxDisp.dX, maxPoint.y + (int) maxDisp.dY);
			
			// Add to trail
			currentTrack.add(maxPoint, i);
			
			// Extract next cell image to convolve
			currentCell = extractImageAroundPoint(image, maxPoint, radius);
			
			// If the track left the outter ROI then stop the track
			int outterRadius = (3 * radius);
			boolean pointOutside = (maxPoint.getX() < outterRadius);
			pointOutside = pointOutside || (maxPoint.getY() < outterRadius);
			pointOutside = pointOutside || (maxPoint.getX() > image.getWidth() - outterRadius);
			pointOutside = pointOutside || (maxPoint.getY() > image.getHeight() - outterRadius);
			if(pointOutside)
				break;
		}
		return currentTrack;
	}
	
	/**
	 * Track the current neutrophil
	 */
	private Trajectory trackNeutrophilBackwardFromPoint(Point startingPoint, int index, int nbFrames, Trajectory currentTrack)
	{
		// Setup the gliding variables
		ImagePlus image1 = new ImagePlus(images.get(index));
		ImagePlus currentCell = extractImageAroundPoint(image1, startingPoint, radius);
		Point maxPoint = startingPoint;
		
		int endFrame = Math.max(0, index - nbFrames);
		for (int i = index - 1; i >= endFrame; i--)
		{
			// Get the next image
			ImagePlus image = new ImagePlus(images.get(i));
			
			// Convolve the cell in the search area of the next image
			Vect maxDisp = convolveNeutrophilAroundPointInImage(image, currentCell, maxPoint, movement);
			
			// Find the maximum displacement
			maxPoint = new Point(maxPoint.x + (int) maxDisp.dX, maxPoint.y + (int) maxDisp.dY);
			
			// Add to trail
			currentTrack.add(maxPoint, i);
			
			// Extract next cell image to convolve
			currentCell = extractImageAroundPoint(image, maxPoint, radius);
			
			// If the track left the outter ROI then stop the track
			int outterRadius = (3 * radius);
			boolean pointOutside = (maxPoint.getX() < outterRadius);
			pointOutside = pointOutside || (maxPoint.getY() < outterRadius);
			pointOutside = pointOutside || (maxPoint.getX() > image.getWidth() - outterRadius);
			pointOutside = pointOutside || (maxPoint.getY() > image.getHeight() - outterRadius);
			if(pointOutside)
				break;
		}
		return currentTrack;
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING ---------------------------
	// ----------------------------------------------------
	
	public void validateParameters()
	{
		p1.validate();
		p2.validate();
		p3.validate();
		p4.validate();
		p5.validate();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == addTrajectoryButton)
		{
			Logs.log("Add a new track...", 1, this);
			addTrajectoryToList(currentTrack);
			
			// reset the graphics
			currentTrack = null;
			imageDisplay.displayTrajectory(currentTrack);
			
			// go back to first frame
			index = 0;
			timeBar.setValue(index);
			displayImage(index);
		}
		if(e.getSource() == removeTrajectoryButton)
		{
			Logs.log("Remove selected track...", 1, this);
			int selected = trajecList.getSelectedIndex();
			if(selected >= 0 && selected < getTrajectories().length)
			{
				if(getTrajectories().length == 0)
					return;
				Trajectory[] newTrajs = new Trajectory[getTrajectories().length - 1];
				Trajectory[] oldTrajs = getTrajectories();
				
				int i = 0;
				for (int j = 0; j < oldTrajs.length; j++)
				{
					Trajectory t = oldTrajs[j];
					if(j == selected)
						continue;
					newTrajs[i] = t;
					i++;
				}
				setTrajectories(newTrajs);
				
				// reset the JList
				DefaultListModel newModel = new DefaultListModel();
				for (Trajectory t : getTrajectories())
				{
					newModel.addElement(t);
				}
				trajecList.setModel(newModel);
				trajecList.repaint();
			}
		}
		if(e.getSource() == doneButton)
		{
			Logs.log("Finishing function...", 1, this);
			makeMovie();
			makeTracks();
			functionFrame.setVisible(false);
			functionFrame.dispose();
		}
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		// int selected = e.getFirstIndex();
		int selected = trajecList.getSelectedIndex();
		if(selected >= 0 && selected < getTrajectories().length)
		{
			Trajectory track = this.getTrajectories()[selected];
			currentTrack = track;
			imageDisplay.displayTrajectory(currentTrack);
			imageDisplay.repaint();
		}
	}
	
	public void adjustmentValueChanged(AdjustmentEvent e)
	{
		int value = timeBar.getValue();
		if(value >= images.size())
			index = images.size() - 1;
		else if(value < 0)
			index = 0;
		else
			index = value;
		
		Logs.log("Moving to image " + index, 1, this);
		displayImage(index);
	}
	
	public void clickedPoint(Point p)
	{
		validateParameters();
		getParams();
		
		if(this.semiAuto)
		{
			currentTrack = new Trajectory();
			currentTrack.add(p, index);
			currentTrack = trackNeutrophilForwardFromPoint(p, index, this.after, currentTrack);
			currentTrack = trackNeutrophilBackwardFromPoint(p, index, this.before, currentTrack);
			imageDisplay.displayTrajectory(currentTrack);
			imageDisplay.setRadius(radius);
			imageDisplay.setClickedPoint(p);
			imageDisplay.repaint();
		}
		else
		{
			// Add to track and create if necessary
			if(currentTrack == null)
				currentTrack = new Trajectory();
			currentTrack.add(p, index);
			
			// Go to next image
			index = index + 1;
			if(index >= images.size())
				index = images.size() - 1;
			imageDisplay.displayTrajectory(currentTrack);
			imageDisplay.setClickedPoint(null);
			timeBar.setValue(index);
			displayImage(index);
		}
	}
	
	public void rightClickedPoint(Point p)
	{
		if(currentTrack == null)
			return;
		else
		{
			addTrajectoryToList(currentTrack);
			
			// reset the graphics
			currentTrack = null;
			imageDisplay.displayTrajectory(currentTrack);
			
			// go back to first frame
			index = 0;
			timeBar.setValue(index);
			displayImage(index);
		}
	}
	
	public void extendedRectangle(Rectangle r)
	{}
}

// ----------------------------------------------------
// --------- FILE LIST RENDERER -----------------------
// ----------------------------------------------------
/**
 * Renderer to display only the names of files not the whole path
 * 
 * @author erwinberthier
 * 
 */
class TrajectoryCellRenderer extends JLabel implements ListCellRenderer {
	
	private static final long serialVersionUID = 1L;
	
	// This is the only method defined by ListCellRenderer.
	// We just reconfigure the JLabel each time we're called.
	
	public Component getListCellRendererComponent(JList list, Object value, // value
			// to
			// display
			int index, // cell index
			boolean isSelected, // is the cell selected
			boolean cellHasFocus) // the list and the cell have the focus
	{
		String s = "";
		if(value instanceof Trajectory)
		{
			s = "Trajectory " + index;
		}
		else
		{
			s = "Trajectory";
		}
		setText(s);
		
		if(isSelected)
		{
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}
		else
		{
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		setEnabled(list.isEnabled());
		setFont(list.getFont());
		setOpaque(true);
		return this;
	}
}
