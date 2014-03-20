package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.process.ImageStatistics;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.arrayView.ImageDisplay;
import jex.arrayView.ImageDisplayController;
import jex.statics.DisplayStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
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
public class JEX_Migration_AverageMigrationDistance extends ExperimentalDataCrunch {
	
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
		String result = "Migration distance";
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
		String result = "Determing the average migration distance in a Y channel laminar flow device";
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
		
		inputNames[0] = new TypeName(IMAGE, "Image to analyze");
		
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
		defaultOutputNames[0] = new TypeName(VALUE, "Mean migration distance");
		defaultOutputNames[1] = new TypeName(VALUE, "Distance table");
		
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
		Parameter p1 = new Parameter("Micron per pixel", "Size of a pixel", "1");
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
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
		JEXData data1 = inputs.get("Image to analyze");
		if(!data1.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Run the function
		MigrationDistanceAnalysisFunction graphFunc = new MigrationDistanceAnalysisFunction(entry, data1, outputNames, parameters);
		
		// Set the outputs
		realOutputs.add(graphFunc.output1);
		realOutputs.add(graphFunc.output2);
		
		// Return status
		return true;
	}
}

class MigrationDistanceAnalysisFunction implements ActionListener, ImageDisplayController {
	
	// Outputs
	public JEXData output1;
	public JEXData output2;
	
	// Gui
	JDialog functionFrame;
	JPanel contentPane;
	ImageDisplay imageDisplay;
	JPanel rightpanel;
	JPanel paramPanel;
	
	JButton previousButton;
	JButton nextButton;
	
	JButton restartButton;
	JButton doneButton;
	JParameterPanel p1;
	
	// Parameters
	double mpp = 1;
	
	// Variables used during the function steps
	private TreeMap<DimensionMap,String> imageMap;
	private TreeMap<DimensionMap,ROIPlus> roiMap;
	private TreeMap<DimensionMap,Double> areaMap;
	private DimensionMap currentDim;
	private ImagePlus im;
	private PointList currentPList;
	private double meanVariation;
	
	// Input
	JEXEntry entry;
	JEXData data;
	ParameterSet params;
	TypeName[] outputNames;
	
	MigrationDistanceAnalysisFunction(JEXEntry entry, JEXData data, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.entry = entry;
		this.data = data;
		this.params = parameters;
		this.outputNames = outputNames;
		
		// Initialize the variables
		resetVariables();
		
		// Extract the variables
		imageMap = ImageReader.readObjectToImagePathTable(data);
		currentDim = imageMap.firstKey();
		im = new ImagePlus(imageMap.get(currentDim));
		
		// //// Get params
		getParams();
		
		// Prepare the graphics
		imageDisplay = new ImageDisplay(this, "Migration Images");
		displayImage(currentDim);
		
		// Initialize the dialog box
		initialize();
	}
	
	// ----------------------------------------------------
	// --------- NORMAL FUNCTION --------------------------
	// ----------------------------------------------------
	
	private void initialize()
	{
		// center panel
		contentPane = new JPanel();
		contentPane.setBackground(DisplayStatics.background);
		contentPane.setLayout(new BorderLayout());
		contentPane.add(imageDisplay, BorderLayout.CENTER);
		
		// Param panel
		p1 = new JParameterPanel(params.getParameter("Micron per pixel"));
		p1.panel().setBackground(DisplayStatics.background);
		paramPanel = new JPanel();
		paramPanel.setBackground(DisplayStatics.background);
		paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.PAGE_AXIS));
		paramPanel.add(p1.panel());
		paramPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		// Buttons
		previousButton = new JButton("Previous Image");
		previousButton.addActionListener(this);
		previousButton.setMaximumSize(new Dimension(150, 20));
		previousButton.setPreferredSize(new Dimension(150, 20));
		previousButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		nextButton = new JButton("Next Image");
		nextButton.addActionListener(this);
		nextButton.setMaximumSize(new Dimension(150, 20));
		nextButton.setPreferredSize(new Dimension(150, 20));
		nextButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		restartButton = new JButton("Restart");
		restartButton.addActionListener(this);
		restartButton.setMaximumSize(new Dimension(150, 20));
		restartButton.setPreferredSize(new Dimension(150, 20));
		restartButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		doneButton = new JButton("Finish");
		doneButton.addActionListener(this);
		doneButton.setMaximumSize(new Dimension(150, 20));
		doneButton.setPreferredSize(new Dimension(150, 20));
		doneButton.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		JLabel ptitle = new JLabel("Parameters");
		ptitle.setAlignmentX(Component.LEFT_ALIGNMENT);
		ptitle.setFont(FontUtility.boldFont);
		
		rightpanel = new JPanel();
		rightpanel.setBackground(DisplayStatics.background);
		rightpanel.setLayout(new BoxLayout(rightpanel, BoxLayout.PAGE_AXIS));
		rightpanel.add(ptitle);
		rightpanel.add(paramPanel);
		rightpanel.add(Box.createVerticalStrut(15));
		rightpanel.add(previousButton);
		rightpanel.add(nextButton);
		rightpanel.add(Box.createVerticalStrut(15));
		rightpanel.add(restartButton);
		rightpanel.add(Box.createVerticalStrut(15));
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
	
	/**
	 * Reset variables
	 */
	private void resetVariables()
	{
		imageMap = ImageReader.readObjectToImagePathTable(data);
		roiMap = new TreeMap<DimensionMap,ROIPlus>();
		areaMap = new TreeMap<DimensionMap,Double>();
		currentDim = imageMap.firstKey();
		currentPList = null;
		meanVariation = 0;
	}
	
	/**
	 * Retrieve the parameters of the function
	 */
	private void getParams()
	{
		mpp = Double.parseDouble(params.getValueOfParameter("Micron per pixel"));
	}
	
	/**
	 * Display the image at location INDEX
	 * 
	 * @param index
	 */
	private void displayImage(DimensionMap dim)
	{
		// Display the image
		im = new ImagePlus(imageMap.get(currentDim));
		imageDisplay.setImage(im);
		
		// Display the polygon roi
		ROIPlus polygon = new ROIPlus(this.currentPList, ROIPlus.ROI_POLYGON);
		imageDisplay.setActiveRoi(polygon);
	}
	
	/**
	 * Return the average height of the roi
	 * 
	 * @return
	 */
	private double getRoiHeight(ROIPlus roip, ImagePlus image)
	{
		// Make an imageJ roi
		Roi imageJRoi = roip.getRoi();
		
		// Set the roi on an ImagePLus
		image.setRoi(imageJRoi);
		
		// Get an imagestatistics object and set the measurements to perform
		int measurements = Measurements.MEAN + Measurements.AREA + Measurements.MIN_MAX + Measurements.STD_DEV + Measurements.MEDIAN;
		ImageStatistics stats = image.getStatistics(measurements);
		
		// Record the area
		double width = stats.roiWidth;
		double area = stats.area;
		double meanHeight = mpp * area / width;
		
		return meanHeight;
	}
	
	private void saveCurrentRoi()
	{
		// Save the roi
		if(this.currentPList == null)
			return;
		ROIPlus polygon = new ROIPlus(this.currentPList, ROIPlus.ROI_POLYGON);
		roiMap.put(currentDim, polygon.copy());
	}
	
	private void makeValueTable()
	{
		// Setup the variables
		int index = 0;
		double minHeight = Double.MAX_VALUE;
		double maxHeight = Double.MIN_VALUE;
		List<Double> distances = new ArrayList<Double>(0);
		
		// Loop though the images and get the image and the ROI
		for (DimensionMap dim : imageMap.keySet())
		{
			ImagePlus image = new ImagePlus(imageMap.get(dim));
			ROIPlus polygon = roiMap.get(dim);
			
			if(polygon != null)
			{
				// Add the height variables
				double height = this.getRoiHeight(polygon, image);
				this.areaMap.put(currentDim, height);
				
				// calculate the variation
				index++;
				minHeight = Math.min(height, minHeight);
				maxHeight = Math.max(height, maxHeight);
				distances.add(height);
			}
		}
		
		// Calculate the variation and make output 1
		this.meanVariation = (maxHeight - minHeight) / (index);
		output1 = ValueWriter.makeValueObject(outputNames[0].getName(), "" + meanVariation);
		
		// Make the table output 2
		String[] columnName = new String[] { "Distance" };
		HashMap<String,List<Double>> columnData = new HashMap<String,List<Double>>();
		columnData.put("Distance", distances);
		output2 = ValueWriter.makeValueTableFromDoubleList(outputNames[1].getName(), columnName, columnData);
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING ---------------------------
	// ----------------------------------------------------
	/**
	 * Grab the parameters from the visual display again
	 */
	public void validateParameters()
	{
		p1.validate();
	}
	
	/**
	 * perform button click actions
	 */
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == previousButton)
		{
			// Save the current roi in the list
			saveCurrentRoi();
			this.currentPList = null;
			
			// Go to the previous image
			DimensionMap lowerKey = imageMap.lowerKey(currentDim);
			if(lowerKey != null)
				currentDim = lowerKey;
			displayImage(currentDim);
		}
		if(e.getSource() == nextButton)
		{
			// Save the current roi in the list
			saveCurrentRoi();
			this.currentPList = null;
			
			// Go to the previous image
			DimensionMap higherKey = imageMap.higherKey(currentDim);
			if(higherKey != null)
			{
				currentDim = higherKey;
			}
			else
			{
				makeValueTable();
				functionFrame.setVisible(false);
				functionFrame.dispose();
			}
			displayImage(currentDim);
		}
		if(e.getSource() == restartButton)
		{
			Logs.log("Restart", 1, this);
			this.resetVariables();
			this.currentDim = imageMap.firstKey();
			this.currentPList = null;
			displayImage(currentDim);
		}
		if(e.getSource() == doneButton)
		{
			Logs.log("Finishing function...", 1, this);
			saveCurrentRoi();
			makeValueTable();
			functionFrame.setVisible(false);
			functionFrame.dispose();
		}
	}
	
	/**
	 * Perform image click actions
	 */
	public void clickedPoint(Point p)
	{
		validateParameters();
		
		// Create point list if null
		if(this.currentPList == null)
		{
			currentPList = new PointList();
		}
		
		// Add point to point list
		currentPList.add(p);
		
		// Display
		this.displayImage(currentDim);
	}
	
	public void rightClickedPoint(Point p)
	{}
	
	public void extendedRectangle(Rectangle r)
	{}
}
