package function.plugin.old;

import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.HashMap;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;

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
public class JEX_ImageCalculator extends JEXCrunchable {
	
	public JEX_ImageCalculator()
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
		String result = "Image Calculator";
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
		String result = "Perform pixel-to-pixel math on two images.";
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
		String toolbox = "Image processing";
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
		return true;
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
		inputNames[0] = new TypeName(IMAGE, "Image A");
		inputNames[1] = new TypeName(IMAGE, "Image B");
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
		defaultOutputNames = new TypeName[1];
		defaultOutputNames[0] = new TypeName(IMAGE, "Calculated Image");
		
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
		// Parameter p0 = new
		// Parameter("Dummy Parameter","Lets user know that the function has been selected.",FormLine.DROPDOWN,new
		// String[] {"true"},0);
		Parameter p1 = new Parameter("Math Operation", "Math operation to perform using the pixel information from Image A and B.", Parameter.DROPDOWN, new String[] { "A+B", "A-B", "A*B", "A/B", "|A-B|", "MAX", "MIN", "AVERAGE", "AND", "OR", "XOR", "COPY", "COPY Transparent 0" }, 4);
		Parameter p2 = new Parameter("Output Bit-Depth", "Bit-Depth of the output image", Parameter.DROPDOWN, new String[] { "8", "16", "32" }, 2);
		
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		// parameterArray.addParameter(p0);
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		;
		
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
		return false;
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
		JEXData imageAData = inputs.get("Image A");
		if(imageAData == null || !imageAData.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		// Collect the inputs
		JEXData imageBData = inputs.get("Image B");
		if(imageBData == null || !imageBData.getTypeName().getType().equals(JEXData.IMAGE))
			return false;
		
		// Gather parameters
		int bitDepth = Integer.parseInt(parameters.getValueOfParameter("Output Bit-Depth"));
		String method = parameters.getValueOfParameter("Math Operation");
		int methodInt = Blitter.DIVIDE;
		if(method.equals("A+B"))
			methodInt = Blitter.ADD;
		else if(method.equals("AND"))
			methodInt = Blitter.AND;
		else if(method.equals("AVERAGE"))
			methodInt = Blitter.AVERAGE;
		else if(method.equals("COPY"))
			methodInt = Blitter.COPY;
		else if(method.equals("COPY_ZERO_TRANSPARENT"))
			methodInt = Blitter.COPY_ZERO_TRANSPARENT;
		else if(method.equals("|A-B|"))
			methodInt = Blitter.DIFFERENCE;
		else if(method.equals("A/B"))
			methodInt = Blitter.DIVIDE;
		else if(method.equals("MAX"))
			methodInt = Blitter.MAX;
		else if(method.equals("MIN"))
			methodInt = Blitter.MIN;
		else if(method.equals("A*B"))
			methodInt = Blitter.MULTIPLY;
		else if(method.equals("OR"))
			methodInt = Blitter.OR;
		else if(method.equals("A-B"))
			methodInt = Blitter.SUBTRACT;
		else if(method.equals("XOR"))
			methodInt = Blitter.XOR;
		
		// Run the function
		TreeMap<DimensionMap,String> imageAMap = ImageReader.readObjectToImagePathTable(imageAData);
		TreeMap<DimensionMap,String> imageBMap = ImageReader.readObjectToImagePathTable(imageBData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		if(imageAMap.size() == 1)
		{
			if(this.isCanceled())
			{
				return false;
			}
			ImagePlus savedImA = new ImagePlus(imageAMap.firstEntry().getValue());
			FloatProcessor savedImpA = (FloatProcessor) savedImA.getProcessor().convertToFloat();
			FloatProcessor impA;
			for (DimensionMap map : imageBMap.keySet())
			{
				impA = new FloatProcessor(savedImpA.getWidth(), savedImpA.getHeight(), (float[]) savedImpA.getPixelsCopy(), null);
				String pathB = imageBMap.get(map);
				if(pathB == null)
					continue;
				ImagePlus imB = new ImagePlus(pathB);
				
				FloatProcessor ipB = (FloatProcessor) imB.getProcessor().convertToFloat();
				
				FloatBlitter blit = new FloatBlitter(impA);
				blit.copyBits(ipB, 0, 0, methodInt);
				ImageProcessor toSave = impA;
				if(bitDepth == 8)
				{
					toSave = impA.convertToByte(false);
				}
				else if(bitDepth == 16)
				{
					toSave = impA.convertToShort(false);
				}
				
				String path = JEXWriter.saveImage(toSave);
				
				if(path != null)
				{
					outputImageMap.put(map, path);
				}
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageAMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}
		else if(imageBMap.size() == 1)
		{
			ImagePlus savedImB = new ImagePlus(imageBMap.firstEntry().getValue());
			FloatProcessor savedImpB = (FloatProcessor) savedImB.getProcessor().convertToFloat();
			FloatProcessor impB;
			for (DimensionMap map : imageAMap.keySet())
			{
				if(this.isCanceled())
				{
					return false;
				}
				impB = new FloatProcessor(savedImpB.getWidth(), savedImpB.getHeight(), (float[]) savedImpB.getPixelsCopy(), null);
				String pathA = imageAMap.get(map);
				if(pathA == null)
					continue;
				ImagePlus imA = new ImagePlus(pathA);
				
				FloatProcessor impA = (FloatProcessor) imA.getProcessor().convertToFloat();
				
				FloatBlitter blit = new FloatBlitter(impA);
				blit.copyBits(impB, 0, 0, methodInt);
				ImageProcessor toSave = impA;
				if(bitDepth == 8)
				{
					toSave = impA.convertToByte(false);
				}
				else if(bitDepth == 16)
				{
					toSave = impA.convertToShort(false);
				}
				
				String path = JEXWriter.saveImage(toSave);
				
				if(path != null)
				{
					outputImageMap.put(map, path);
				}
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageAMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}
		else
		{
			for (DimensionMap map : imageAMap.keySet())
			{
				if(this.isCanceled())
				{
					return false;
				}
				ImagePlus imA = new ImagePlus(imageAMap.get(map));
				String pathB = imageBMap.get(map);
				if(pathB == null)
					continue;
				ImagePlus imB = new ImagePlus(pathB);
				FloatProcessor ipA = (FloatProcessor) imA.getProcessor().convertToFloat();
				FloatProcessor ipB = (FloatProcessor) imB.getProcessor().convertToFloat();
				
				FloatBlitter blit = new FloatBlitter(ipA);
				blit.copyBits(ipB, 0, 0, methodInt);
				ImageProcessor toSave = ipA;
				if(bitDepth == 8)
				{
					toSave = ipA.convertToByte(false);
				}
				else if(bitDepth == 16)
				{
					toSave = ipA.convertToShort(false);
				}
				
				String path = JEXWriter.saveImage(toSave);
				
				if(path != null)
				{
					outputImageMap.put(map, path);
				}
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageAMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}
		
		if(outputImageMap.size() == 0)
		{
			return false;
		}
		
		JEXData output1 = ImageWriter.makeImageStackFromPaths(outputNames[0].getName(), outputImageMap);
		
		// Set the outputs
		realOutputs.add(output1);
		
		// Return status
		return true;
	}
	
}
