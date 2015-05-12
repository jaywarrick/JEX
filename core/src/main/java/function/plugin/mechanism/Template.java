// Define package name as "plugins" as show here
package function.plugin.mechanism;

// Import needed classes here 
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

// Specify plugin characteristics here
@Plugin(
		type = JEXPlugin.class,
		name="Template Function",
		menuPath="Templates",
		visible=true,
		description="Example of a file to define a JEX function/plugin. Doesn't do anything but print the parameters provided by the user."
		)
public class Template extends JEXPlugin {

	// Define a constructor that takes no arguments.
	public Template()
	{}
	
	/////////// Define Inputs here ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData inputData;
	
	/////////// Define Parameters here ///////////
	
	@ParameterMarker(uiOrder=6, name="Checkbox", description="A simple checkbox for entering true/false variables.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean checkbox;
	
	@ParameterMarker(uiOrder=1, name="Value", description="This textbox is automatically parsed for conversion to whatever primitive type this annotation is associated with (e.g., String, double, int, etc).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double value;
	
	@ParameterMarker(uiOrder=2, name="Choice", description="A dropdown provides a set number of choices, and is used to assign a String value.", ui=MarkerConstants.UI_DROPDOWN, choices={"Choice 0", "Choice 1", "Choice 2"}, defaultChoice=0)
	String choice;
	
	@ParameterMarker(uiOrder=3, name="New Min", description="Opens a file chooser dialog box and returns the path String of the user choice (file or folder).", ui=MarkerConstants.UI_FILECHOOSER, defaultText="~/")
	String path;
	
	@ParameterMarker(uiOrder=4, name="Password", description="Passwords don't show in the user interface and are not saved anywhere by JEX (e.g., when you save a workflow). Useful for scripts that send emails or upload/download from storage sites.", ui=MarkerConstants.UI_PASSWORD, defaultText="")
	String password;
	
	@ParameterMarker(uiOrder=5, name="Script", description="The script interface provides a multi-line textbox to input code to pass along to your function providing greater flexibility beyond a simple text box.", ui=MarkerConstants.UI_SCRIPT, defaultText="# Replace with user code #")
	String script;
	
	/////////// Define Outputs here ///////////
	
	// See Database.Definition.OutpuMarker for types of inputs that are supported (File, Image, Value, ROI...)
	@OutputMarker(uiOrder=1, name="Output Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData outputData;
	
	// Define threading capability here (set to 1 if using non-final static variables shared between function instances).
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	// Code the actions of the plugin here using comments for significant sections of code to enhance readability as shown here
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(inputData == null || !inputData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		// Run the function
		// Read in the input with one of the many "reader" classes (see package "Database.DataReader")
		TreeMap<DimensionMap,String> inputMap = ImageReader.readObjectToImagePathTable(inputData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		
		// Loop through the items in the n-Dimensional object
		for (DimensionMap map : inputMap.keySet())
		{
			// Cancel the function as soon as possible if the user has hit the cancel button
			// Perform this inside loops to check as often as possible.
			if(this.isCanceled())
			{
				return false;
			}
			
			// Print the value contained in the input map.
			Logs.log(inputMap.get(map), this);
			
			// Print parameter values by calling a helper method
			String pathToSave = printParameters(checkbox, value, choice, path, password, script);
			
			// Store returned data
			outputMap.put(map, pathToSave);
			
			// Update the user interface with progress
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) inputMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(outputMap.size() == 0)
		{
			return false;
		}
		
		// Set output (see package "Database.DataWriter" for other types of data that can be created)
		this.outputData = ImageWriter.makeImageStackFromPaths("tempName", outputMap);
		
		// Return status
		return true;
	}
	
	// Helper functions should go below the run function
	public String printParameters(boolean checkbox, double value, String choice, String path, String password, String script)
	{
		Logs.log(""+checkbox, this);
		Logs.log(""+value, this);
		Logs.log(""+choice, this);
		Logs.log(""+path, this);
		Logs.log(""+password, this);
		Logs.log(""+script, this);
		return(path);
	}
}
