package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;
import guiObject.JParameterPanel;
import ij.ImagePlus;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import miscellaneous.StatisticsUtility;

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
public class JEX_BondingDistance extends JEXCrunchable {
	
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
		String result = "Bond strength analysis";
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
		String result = "Determing bond energy";
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
		String toolbox = "Custom image analysis";
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
		defaultOutputNames[0] = new TypeName(VALUE, "Mean delamination distance");
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
		if(!data1.getTypeName().getType().matches(JEXData.IMAGE))
			return false;
		
		// Run the function
		BondStrengthAnalysisFunction graphFunc = new BondStrengthAnalysisFunction(entry, data1, outputNames, parameters);
		
		// Set the outputs
		realOutputs.add(graphFunc.output1);
		realOutputs.add(graphFunc.output2);
		
		// Return status
		return true;
	}
}

class BondStrengthAnalysisFunction implements ActionListener, ImageDisplayController {
	
	// Outputs
	public JEXData output1;
	public JEXData output2;
	
	// Gui
	JDialog functionFrame;
	JPanel contentPane;
	ImageDisplay imageDisplay;
	JPanel rightpanel;
	JPanel paramPanel;
	JButton restartButton;
	JButton doneButton;
	JParameterPanel p1;
	
	// Parameters
	double mpp = 1;
	
	// Variables used during the function steps
	private ImagePlus im;
	private PointList currentPointList;
	private Point linePoint1;
	private Point linePoint2;
	private ROIPlus lineRoi;
	private ROIPlus guideLineRoi1;
	private ROIPlus guideLineRoi2;
	private ROIPlus pListRoi;
	
	// Input
	JEXEntry entry;
	JEXData data;
	ParameterSet params;
	TypeName[] outputNames;
	
	BondStrengthAnalysisFunction(JEXEntry entry, JEXData data, TypeName[] outputNames, ParameterSet parameters)
	{
		// Pass the variables
		this.entry = entry;
		this.data = data;
		this.params = parameters;
		this.outputNames = outputNames;
		
		// //// Get params
		getParams();
		
		// Prepare function
		String imagePath = ImageReader.readObjectToImagePath(data);
		im = new ImagePlus(imagePath);
		
		// Prepare the graphics
		imageDisplay = new ImageDisplay(this, "Bond strenght analysis");
		displayImage(im);
		
		// center panel
		contentPane = new JPanel();
		contentPane.setBackground(DisplayStatics.background);
		contentPane.setLayout(new BorderLayout());
		contentPane.add(imageDisplay, BorderLayout.CENTER);
		
		// Param panel
		p1 = new JParameterPanel(parameters.getParameter("Micron per pixel"));
		p1.panel().setBackground(DisplayStatics.background);
		paramPanel = new JPanel();
		paramPanel.setBackground(DisplayStatics.background);
		paramPanel.setLayout(new BoxLayout(paramPanel, BoxLayout.PAGE_AXIS));
		paramPanel.add(p1.panel());
		paramPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		// Buttons
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
		rightpanel.add(restartButton);
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
		mpp = Double.parseDouble(params.getValueOfParameter("Micron per pixel"));
	}
	
	/**
	 * Display the image at location INDEX
	 * 
	 * @param index
	 */
	private void displayImage(ImagePlus im)
	{
		imageDisplay.setImage(im);
	}
	
	private void makeValueTable()
	{
		List<Double> distances = new ArrayList<Double>(0);
		
		if(currentPointList == null)
			currentPointList = new PointList();
		if(linePoint1 == null || linePoint2 == null)
			return;
		
		double x1 = linePoint1.getX();
		double y1 = linePoint1.getY();
		double x2 = linePoint2.getX();
		double y2 = linePoint2.getY();
		for (Point p : currentPointList)
		{
			double px = p.getX();
			double py = p.getY();
			double pixelDistance = Line2D.ptLineDist(x1, y1, x2, y2, px, py);
			double distance = mpp * pixelDistance;
			distances.add(distance);
		}
		double meanDistance = StatisticsUtility.mean(distances);
		
		String[] columnName = new String[] { "Distance" };
		HashMap<String,List<Double>> columnData = new HashMap<String,List<Double>>();
		columnData.put("Distance", distances);
		
		output1 = ValueWriter.makeValueObject(outputNames[0].getName(), "" + meanDistance);
		output2 = ValueWriter.makeValueTableFromDoubleList(outputNames[1].getName(), columnName, columnData);
	}
	
	/**
	 * Display only the image
	 */
	private void displayNothing()
	{
		imageDisplay.setRois(null);
		imageDisplay.setActiveRoi(null);
		this.displayImage(im);
	}
	
	/**
	 * Display the image and the guide lines
	 */
	private void drawGuideLines()
	{
		if(linePoint2 == null || linePoint1 == null)
			return;
		PointList line = new PointList();
		line.add(linePoint1);
		line.add(linePoint2);
		lineRoi = new ROIPlus(line, ROIPlus.ROI_POLYLINE);
		
		int dX = (int) (linePoint2.getX() - linePoint1.getX());
		int dY = (int) (linePoint1.getY() - linePoint2.getY());
		PointList guideLine1 = new PointList();
		guideLine1.add(new Point((int) linePoint1.getX(), (int) linePoint1.getY()));
		guideLine1.add(new Point((int) linePoint1.getX() + 1 * dY, (int) linePoint1.getY() + 1 * dX));
		guideLine1.add(new Point((int) linePoint1.getX() + 2 * dY, (int) linePoint1.getY() + 2 * dX));
		guideLine1.add(new Point((int) linePoint1.getX() + 3 * dY, (int) linePoint1.getY() + 3 * dX));
		PointList guideLine2 = new PointList();
		guideLine2.add(new Point((int) linePoint2.getX(), (int) linePoint2.getY()));
		guideLine2.add(new Point((int) linePoint2.getX() + 1 * dY, (int) linePoint2.getY() + 1 * dX));
		guideLine2.add(new Point((int) linePoint2.getX() + 2 * dY, (int) linePoint2.getY() + 2 * dX));
		guideLine2.add(new Point((int) linePoint2.getX() + 3 * dY, (int) linePoint2.getY() + 3 * dX));
		guideLineRoi1 = new ROIPlus(guideLine1, ROIPlus.ROI_POLYLINE);
		guideLineRoi2 = new ROIPlus(guideLine2, ROIPlus.ROI_POLYLINE);
		
		this.displayImage(im);
		imageDisplay.setRois(null);
		imageDisplay.addRoi(lineRoi, Color.red);
		imageDisplay.addRoi(guideLineRoi1, Color.cyan);
		imageDisplay.addRoi(guideLineRoi2, Color.cyan);
		imageDisplay.setActiveRoi(null);
	}
	
	/**
	 * Display the image, the guide lines and the points
	 */
	private void displayPoints()
	{
		pListRoi = new ROIPlus(currentPointList, ROIPlus.ROI_POINT);
		this.displayImage(im);
		imageDisplay.setRois(null);
		imageDisplay.addRoi(lineRoi);
		imageDisplay.addRoi(guideLineRoi1);
		imageDisplay.addRoi(guideLineRoi2);
		imageDisplay.addRoi(pListRoi);
		imageDisplay.setActiveRoi(pListRoi);
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
		if(e.getSource() == restartButton)
		{
			Logs.log("Restart", 1, this);
			this.linePoint1 = null;
			this.linePoint2 = null;
			displayImage(im);
			displayNothing();
		}
		if(e.getSource() == doneButton)
		{
			Logs.log("Finishing function...", 1, this);
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
		getParams();
		
		// is first point set?
		if(this.linePoint1 == null)
		{
			this.linePoint1 = p;
			this.linePoint2 = null;
			this.currentPointList = null;
			displayNothing();
		}
		else if(this.linePoint2 == null)
		{
			this.linePoint2 = p;
			this.currentPointList = null;
			drawGuideLines();
		}
		else
		{
			if(currentPointList == null)
				currentPointList = new PointList();
			currentPointList.add(p);
			displayPoints();
		}
	}
	
	public void rightClickedPoint(Point p)
	{}
	
	public void extendedRectangle(Rectangle r)
	{}
}
