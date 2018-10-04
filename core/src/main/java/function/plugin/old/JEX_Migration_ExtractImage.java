package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import cruncher.Ticket;
import function.GraphicalCrunchingEnabling;
import function.GraphicalFunctionWrap;
import function.ImagePanel;
import function.ImagePanelInteractor;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Image;
import java.awt.Point;
import java.util.HashMap;
import java.util.List;

import jex.utilities.FunctionUtility;
import logs.Logs;

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
public class JEX_Migration_ExtractImage extends JEXCrunchable {
	
	public static Image cellImage;
	
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
		String result = "1. Extract Image";
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
		String result = "Extract an image from a larger image.";
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
		inputNames[0] = new TypeName(IMAGE, "Source Image");
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
		defaultOutputNames[0] = new TypeName(IMAGE, "Extracted Image");
		
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
		Parameter p1 = new Parameter("Only once", "Extract the image only once and distribute it to all the other entries", Parameter.DROPDOWN, new String[] { "true", "false" }, 0);
		
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
		JEXData data = inputs.get("Source Image");
		if(!data.getTypeName().getType().matches(JEXData.IMAGE))
			return false;
		List<String> imset = ImageReader.readObjectToImagePathList(data);
		
		// get the parameters
		String autoStr = parameters.getValueOfParameter("Only once");
		boolean auto = Boolean.parseBoolean(autoStr);
		
		// Run the function
		ImagePlus im = null;
		if(auto && JEX_Migration_ExtractImage.cellImage != null)
		{
			im = new ImagePlus("", cellImage);
		}
		else
		{
			VisualExtractionHelperFunction graphFunc = new VisualExtractionHelperFunction(entry, imset, outputNames, parameters);
			graphFunc.doit();
			im = new ImagePlus("", cellImage);
		}
		
		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat();
		// String localDir = JEXWriter.getEntryFolder(entry);
		// String newFileName = FunctionUtility.getNextName(localDir,
		// "Croped.TIF", "Ex");
		// String finalPath = localDir + File.separator + newFileName;
		// Logs.log("Saving in "+finalPath,1,this);
		// FunctionUtility.imSave(imp, "false", 16, finalPath);
		
		// Save the image
		Logs.log("Saving image", 1, this);
		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", 16);
		String imPath = JEXWriter.saveImage(toSave);
		
		JEXData output = ImageWriter.makeImageObject(outputNames[0].getName(), imPath);
		
		// Set the outputs
		realOutputs.add(output);
		
		// Return status
		return true;
	}
	
	@Override
	public void finalizeTicket(Ticket t)
	{
		cellImage = null;
	}
}

class VisualExtractionHelperFunction implements GraphicalCrunchingEnabling, ImagePanelInteractor {
	
	ImagePanel imagepanel;
	GraphicalFunctionWrap wrap;
	List<String> images;
	int atStep = 0;
	boolean auto = true;
	Point first = null;
	Point second = null;
	int roiX = -1;
	int roiY = -1;
	int roiW = -1;
	int roiH = -1;
	Roi roi = null;
	ImagePlus im = null;
	
	ParameterSet params;
	List<String> imset;
	
	VisualExtractionHelperFunction(JEXEntry entry, List<String> imset, TypeName[] outputNames, ParameterSet params)
	{
		// Pass the variables
		this.imset = imset;
		this.images = imset;
		this.params = params;
		
		// //// Get params
		String autoStr = params.getValueOfParameter("Only once");
		auto = Boolean.parseBoolean(autoStr);
		
		// Prepare the graphics
		im = new ImagePlus(images.get(0));
		Logs.log("Placing in imagepanel " + images.get(0), 1, this);
		imagepanel = new ImagePanel(this, "Select Cell");
		imagepanel.setImage(im);
		
		wrap = new GraphicalFunctionWrap(this, params);
		wrap.addStep(0, "Select cell", new String[] { "Only once" });
		wrap.setInCentralPanel(imagepanel);
	}
	
	/**
	 * Run the function and open the graphical interface
	 * 
	 * @return the ROI data
	 */
	public boolean doit()
	{
		boolean b = wrap.start();
		return b;
	}
	
	public void runStep(int index)
	{}
	
	public void runNext()
	{}
	
	public void runPrevious()
	{}
	
	public int getStep()
	{
		return atStep;
	}
	
	public void startIT()
	{
		wrap.displayUntilStep();
	}
	
	/**
	 * Apply the roi to all other images
	 */
	public void finishIT()
	{
		im.setRoi(roi);
		ImageProcessor imp = im.getProcessor().crop();
		JEX_Migration_ExtractImage.cellImage = imp.createImage();
	}
	
	public void loopNext()
	{}
	
	public void loopPrevious()
	{}
	
	public void recalculate()
	{}
	
	public void clickedPoint(Point p)
	{}
	
	public void pressedPoint(Point p)
	{
		first = p;
		second = null;
		imagepanel.setRoi(null);
		roi = null;
	}
	
	public void mouseMoved(Point p)
	{
		second = p;
		
		roiW = Math.abs(second.x - first.x);
		roiH = Math.abs(second.y - first.y);
		roiX = Math.min(second.x, first.x);
		roiY = Math.min(second.y, first.y);
		roi = new Roi(roiX, roiY, roiW, roiH);
		imagepanel.setRoi(roi);
	}
	
}
