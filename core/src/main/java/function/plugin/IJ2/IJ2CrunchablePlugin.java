package function.plugin.IJ2;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.Future;

import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import net.imagej.Dataset;
import net.imagej.display.DataView;
import net.imagej.display.ImageDisplay;

import org.scijava.command.CommandInfo;
import org.scijava.command.CommandModule;
import org.scijava.module.ModuleItem;

import tables.DimTable;
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
public class IJ2CrunchablePlugin extends JEXCrunchable {
	
	public String name = null;
	public String info = "";
	public String toolbox = "ImageJ";
	public boolean showInList = true;
	public boolean allowMultiThreading = true;
	// Don't need parameters variable because there is already one to use.
	public boolean isInputValidityCheckingEnabled = false;
	public CommandInfo command = null;
	
	public IJ2CrunchablePlugin(CommandInfo command)
	{
		this.setCommand(command);
	}
	
	private void setCommand(CommandInfo command)
	{
		this.command = command;
		this.name = command.getTitle();
		this.toolbox = IJ2PluginUtility.getToolboxString("ImageJ", command.getMenuPath());
		this.info = command.getMenuPath().getMenuString();
		if(this.parameters == null)
		{
			this.parameters = new ParameterSet();
		}
		this.parameters = IJ2PluginUtility.getDefaultJEXParameters(command);
	}
	
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
		return this.name;
	}
	
	/**
	 * This method returns a string explaining what this method does This is purely informational and will display in JEX
	 * 
	 * @return Information string
	 */
	@Override
	public String getInfo()
	{
		return this.info;
	}
	
	/**
	 * This method defines in which group of function this function will be shown in... Toolboxes (choose one, caps matter): Visualization, Image processing, Custom Cell Analysis, Cell tracking, Image tools Stack processing, Data Importing, Custom
	 * image analysis, Matlab/Octave
	 * 
	 */
	@Override
	public String getToolbox()
	{
		return this.toolbox;
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
		Vector<TypeName> inputs = new Vector<TypeName>();
		for (ModuleItem<?> item : this.command.inputs())
		{
			inputs.addAll(IJ2PluginUtility.getTypeNamesForIJ2IOItem(item));
		}
		TypeName[] inputNames = inputs.toArray(new TypeName[] {});
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
		Vector<TypeName> outputs = new Vector<TypeName>();
		for (ModuleItem<?> item : this.command.outputs())
		{
			Vector<TypeName> tempOutputs = IJ2PluginUtility.getTypeNamesForIJ2IOItem(item);
			if(tempOutputs.size() > 0)
			{
				outputs.add(tempOutputs.get(0)); // Hack for now so that no ROIs show up in the outputs.
			}
		}
		this.defaultOutputNames = outputs.toArray(new TypeName[] {});
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
		return IJ2PluginUtility.getDefaultJEXParameters(this.command);
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
		// Gather parameters
		Vector<Object> commandParameters = IJ2PluginUtility.getIJ2Parameters(this.parameters, this.command);
		
		// Run the function
		DimTable table = getDimTableForIteration(inputs);
		double count = 0;
		int percentage = 0;
		double total = table.mapCount();
		TreeMap<String,Object> tempOutput = new TreeMap<String,Object>();
		TreeMap<String,TreeMap<DimensionMap,Object>> outputs = new TreeMap<String,TreeMap<DimensionMap,Object>>();
		for (DimensionMap map : table.getMapIterator())
		{
			try
			{
				tempOutput = processCommand(map, inputs, this.command, commandParameters);
				if(tempOutput != null)
				{
					for (String outputName : tempOutput.keySet())
					{
						TreeMap<DimensionMap,Object> output = outputs.get(outputName);
						if(output == null)
						{
							output = new TreeMap<DimensionMap,Object>();
							outputs.put(outputName, output);
						}
						output.put(map, tempOutput.get(outputName));
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return false;
			}
			
			count = count + 1;
			percentage = (int) (100 * ((count) / (total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(outputs.size() == 0)
		{
			return false;
		}
		
		// Set the outputs
		for (int i = 0; i < this.defaultOutputNames.length; i++)
		{
			String commandOutputName = this.defaultOutputNames[i].getName();
			JEXData output = IJ2PluginUtility.getJEXDataForOutput(this.outputNames[i].getName(), this.command.getOutput(commandOutputName).getType(), outputs.get(commandOutputName));
			this.realOutputs.add(output);
		}
		
		// Return status
		return true;
	}
	
	public static DimTable getDimTableForIteration(HashMap<String,JEXData> inputs)
	{
		DimTable ret = new DimTable();
		for (String name : inputs.keySet())
		{
			JEXData temp = inputs.get(name);
			if(temp != null && temp.getTypeName().getType().equals(JEXData.IMAGE))
			{
				DimTable table = temp.getDimTable();
				ret = DimTable.union(ret, table);
			}
		}
		return ret;
	}
	
	public static TreeMap<String,Object> processCommand(DimensionMap map, HashMap<String,JEXData> inputs, CommandInfo command, Vector<Object> commandParameters) throws Exception
	{
		TreeMap<String,Object> ret = new TreeMap<String,Object>();
		
		Vector<Object> commandInputs = IJ2PluginUtility.getIJ2Inputs(map, inputs, command);
		Vector<Object> commandItems = new Vector<Object>();
		commandItems.addAll(commandInputs);
		commandItems.addAll(commandParameters);
		
		Future<CommandModule> result = IJ2PluginUtility.ij().command().run(command, true, commandItems.toArray(new Object[] {}));
		CommandModule c = result.get();
		Map<String,Object> cOutputs = c.getOutputs();
		for (Entry<String,Object> e : cOutputs.entrySet())
		{
			Object output = null;
			if(e.getValue() instanceof Dataset)
			{
				Dataset d = (Dataset) e.getValue();
				output = JEXWriter.saveImage(d);
			}
			else if(e.getValue() instanceof ImageDisplay)
			{
				ImageDisplay display = (ImageDisplay) e.getValue();
				if(display.size() > 0)
				{
					DataView view = display.get(0);
					Dataset d = (Dataset) view.getData();
					output = JEXWriter.saveImage(d);
				}
			}
			else
			// if(e.getValue() instanceof String || e.getValue() instanceof Number || e.getValue() instanceof File || e.getValue() instanceof Object)
			{
				output = e.getValue().toString();
			}
			if(output != null)
			{
				ret.put(e.getKey(), output);
			}
		}
		
		return ret;
	}
	
	public static String saveAdjustedImage(String imagePath, double oldMin, double oldMax, double newMin, double newMax, double gamma, int bitDepth)
	{
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
		{
			return null;
		}
		ImagePlus im = new ImagePlus(imagePath);
		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat(); // should be a float processor
		
		// Adjust the image
		FunctionUtility.imAdjust(imp, oldMin, oldMax, newMin, newMax, gamma);
		
		// Save the results
		String imPath = JEXWriter.saveImage(imp, bitDepth);
		//		ImagePlus toSave = FunctionUtility.makeImageToSave(imp, "false", bitDepth, gamma);
		//		String imPath = JEXWriter.saveImage(toSave);
		im.flush();
		
		// return temp filePath
		return imPath;
	}
}
