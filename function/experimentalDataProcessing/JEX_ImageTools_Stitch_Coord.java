package function.experimentalDataProcessing;

import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import image.roi.IdPoint;
import image.roi.PointList;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import logs.Logs;
import miscellaneous.CSVList;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.ImageReader;
import Database.DataReader.ValueReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.ExperimentalDataCrunch;

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
public class JEX_ImageTools_Stitch_Coord extends ExperimentalDataCrunch {
	
	public JEX_ImageTools_Stitch_Coord()
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
		String result = "Stitch Image Using Coordinates";
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
		String result = "Function that allows you to stitch a stack of images into a single image using a file that specifies coordinates.";
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
		TypeName[] inputNames = new TypeName[3];
		inputNames[0] = new TypeName(VALUE, "Image Alignment");
		inputNames[1] = new TypeName(VALUE, "Stage Movements");
		inputNames[2] = new TypeName(IMAGE, "Image Stack");
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
		this.defaultOutputNames = new TypeName[1];
		this.defaultOutputNames[0] = new TypeName(IMAGE, "Stitched Image");
		
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
		Parameter p0 = new Parameter("Scale", "The stitched image size will be scaled by this factor.", "1.0");
		Parameter p1 = new Parameter("Output Bit Depth", "Bit depth to save the image as.", Parameter.DROPDOWN, new String[] { "8", "16" }, 1);
		Parameter p2 = new Parameter("Normalize Intensities Fit Bit Depth", "Scale intensities to go from 0 to max value determined by new bit depth (\'true\' overrides intensity multiplier).", Parameter.DROPDOWN, new String[] { "true", "false" }, 1);
		Parameter p3 = new Parameter("Intensity Multiplier", "Number to multiply all intensities by before converting to new bitDepth.", "1");
		// Parameter p4 = new
		// Parameter("New Max","Image Intensity Value","65535.0");
		// Parameter p5 = new
		// Parameter("Gamma","0.1-5.0, value of 1 results in no change","1.0");
		// Parameter p6 = new
		// Parameter("Output Bit Depth","Depth of the outputted image",FormLine.DROPDOWN,new
		// String[] {"8","16","32"},1);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		parameterArray.addParameter(p3);
		// parameterArray.addParameter(p4);
		// parameterArray.addParameter(p5);
		// parameterArray.addParameter(p6);
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
		JEXData valueData = inputs.get("Image Alignment");
		if(valueData == null || !valueData.getTypeName().getType().equals(JEXData.VALUE))
		{
			return false;
		}
		CSVList alignmentInfo = new CSVList(ValueReader.readValueObject(valueData));
		
		// Collect the inputs
		JEXData fileData = inputs.get("Stage Movements");
		if(fileData == null || !fileData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}
		File stageFile = FileReader.readFileObjectToFile(fileData);
		double scale = Double.parseDouble(this.parameters.getValueOfParameter("Scale"));
		PointList imageDisplacements = this.convertStageMovements(stageFile, Integer.parseInt(alignmentInfo.get(0)), Integer.parseInt(alignmentInfo.get(1)), Integer.parseInt(alignmentInfo.get(2)), scale);
		
		// Collect saving parameters
		int bitDepth = Integer.parseInt(this.parameters.getValueOfParameter("Output Bit Depth"));
		boolean normalize = Boolean.parseBoolean(this.parameters.getValueOfParameter("Normalize Intensities Fit Bit Depth"));
		double multiplier = Double.parseDouble(this.parameters.getValueOfParameter("Intensity Multiplier"));
		
		// Collect the inputs
		JEXData imageData = inputs.get("Image Stack");
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		List<DimensionMap> imageDimMaps = imageData.getDimTable().getDimensionMaps();
		
		// Run the function
		File stitchedFile = stitch(entry, imageData, imageDimMaps, imageDisplacements, scale, normalize, multiplier, bitDepth);
		if(stitchedFile == null)
		{
			return false;
		}
		
		// Set the outputs
		this.realOutputs.add(ImageWriter.makeImageObject(this.outputNames[0].getName(), stitchedFile.getAbsolutePath()));
		
		// Return status
		return true;
	}
	
	public static File stitch(JEXEntry entry, JEXData imageData, List<DimensionMap> imageDimMaps, PointList imageDisplacements, double scale, boolean normalize, double multiplier, int bits)
	{
		// /// prepare a blank image on which to copy the others
		ImagePlus original = new ImagePlus(ImageReader.readObjectToImagePath(imageData, imageDimMaps.get(0)));
		double imSizeX = original.getWidth() * scale;
		double imSizeY = original.getHeight() * scale;
		Rectangle rect = imageDisplacements.getBounds();
		int totalWidth = (rect.width + ((int) imSizeX));
		int totalHeight = (rect.height + ((int) imSizeY));
		FloatProcessor stitchIP = null;
		stitchIP = new FloatProcessor(totalWidth, totalHeight);
		ImagePlus stitch = new ImagePlus("Stitch", stitchIP);
		
		PointList xy = imageDisplacements;
		int count = 0;
		Iterator<DimensionMap> itr = imageDimMaps.iterator();
		Iterator<IdPoint> itrXY = xy.iterator();
		int percentage;
		FloatProcessor imp;
		ImagePlus im;
		while (itr.hasNext() && itrXY.hasNext())
		{
			DimensionMap map = itr.next();
			// //// Prepare float processor
			Logs.log("Getting file " + ImageReader.readObjectToImagePath(imageData, map) + " in entry " + entry.getTrayX() + "," + entry.getTrayY() + " for dim " + map.toString(), 0, JEX_ImageTools_Stitch_Coord.class);
			im = new ImagePlus(ImageReader.readObjectToImagePath(imageData, map));
			imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should
			// be a
			// float
			// processor
			
			// //// Begin Actual Function
			if(scale != 1.0)
			{
				imp.setInterpolationMethod(ImageProcessor.BILINEAR);
				imp = (FloatProcessor) imp.resize((int) imSizeX, (int) imSizeY);
			}
			Point p = itrXY.next();
			// System.out.println(p);
			stitchIP.copyBits(imp, p.x, p.y, Blitter.COPY);
			// //// End Actual Function
			
			count = count + 1;
			percentage = (int) (100 * ((count) / ((double) imageDimMaps.size() + 1)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
			im.flush();
		}
		
		// //// Save the results
		stitch.setProcessor("Stitch", stitchIP);
		count = count + 1;
		percentage = (int) (100 * ((count) / ((double) imageDimMaps.size() + 2)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		
		// Save the resulting image file
		ImagePlus toSave = FunctionUtility.makeImageToSave(stitchIP, normalize, 1.0, bits);
		String imPath = JEXWriter.saveImage(toSave);
		File result = new File(imPath);
		
		count = count + 1;
		percentage = (int) (100 * ((count) / ((double) imageDimMaps.size() + 2)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
		
		return result;
	}
	
	private PointList convertStageMovements(File stageMovements, int dxImage, int dyImage, int imageIndex, double scale)
	{
		PointList stagePoints = null;
		try
		{
			File input = stageMovements;
			if(!input.exists())
			{
				return null;
			}
			FileInputStream fstream = new FileInputStream(input);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			// Read File Line By Line
			while ((strLine = br.readLine()) != null)
			{
				stagePoints = new PointList(strLine);
				if(stageMovements != null)
				{
					break;
				}
			}
			// Close the input stream
			in.close();
		}
		catch (Exception e)
		{
			System.err.println("Error: " + e.getMessage());
			return null;
		}
		if(stagePoints == null)
		{
			return null;
		}
		
		int dxStage = stagePoints.get(imageIndex).x;
		int dyStage = stagePoints.get(imageIndex).y;
		
		// //// Begin Actual Function
		double xS = dxStage;
		double yS = dyStage;
		double xP = dxImage;
		double yP = dyImage;
		
		double rS = Math.sqrt((xS * xS + yS * yS));
		double rP = Math.sqrt((xP * xP + yP * yP));
		double thetaS = Math.atan2(yS, xS);
		double thetaP = Math.atan2(yP, xP);
		double dtheta = thetaP - thetaS;
		// System.out.println(thetaS);
		// System.out.println(thetaP);
		// System.out.println(dtheta);
		double ratio = rP / rS;
		// System.out.println("Ratio to Include:" + ratio);
		
		Point temp = null;
		PointList ret = new PointList();
		int newX = 0, newY = 0;
		double r = 1;
		double theta = 0;
		double xt = 0, yt = 0;
		Iterator<IdPoint> itr = stagePoints.iterator();
		while (itr.hasNext())
		{
			temp = itr.next();
			xt = (temp.x);
			yt = (temp.y);
			theta = Math.atan2(yt, xt) + dtheta; // Multiply dy by -1 to account
			// for reversed coordinates of
			// microscope stage.
			r = ratio * Math.sqrt(yt * yt + xt * xt);
			newX = (int) (r * Math.cos(theta));
			newY = (int) (r * Math.sin(theta));
			ret.add(newX, newY);
		}
		
		PointList imageCoords = new PointList();
		imageCoords.add(0, 0);
		for (int i = 0; i < ret.size(); i++)
		{
			imageCoords.add(imageCoords.get(i).x + ret.get(i).x, imageCoords.get(i).y + ret.get(i).y);
		}
		
		// Scale and put bounding rectangle at 0,0
		imageCoords.scale(scale);
		Rectangle rect = imageCoords.getBounds();
		imageCoords.translate(-1 * rect.x, -1 * rect.y);
		System.out.println(imageCoords.getBounds());
		
		return imageCoords;
	}
}
