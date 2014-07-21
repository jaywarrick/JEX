package function.plugin.old;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
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
public class JEX_ExportMultipleFiles extends JEXCrunchable {
	
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
		String result = "Export Multiple Table Files";
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
		String result = "Export multiple files to a folder";
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
		String toolbox = "Table Tools";
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
		TypeName[] inputNames = new TypeName[10];
		inputNames[0] = new TypeName(FILE, "File 1");
		inputNames[1] = new TypeName(FILE, "File 2");
		inputNames[2] = new TypeName(FILE, "File 3");
		inputNames[3] = new TypeName(FILE, "File 4");
		inputNames[4] = new TypeName(FILE, "File 5");
		inputNames[5] = new TypeName(FILE, "File 6");
		inputNames[6] = new TypeName(FILE, "File 7");
		inputNames[7] = new TypeName(FILE, "File 8");
		inputNames[8] = new TypeName(FILE, "File 9");
		inputNames[9] = new TypeName(FILE, "File 10");
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
		this.defaultOutputNames = new TypeName[0];
		
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
		Parameter p1 = new Parameter("Folder Path", "Location to which the files will be copied", Parameter.FILECHOOSER, "");
		Parameter p2 = new Parameter("File Extension", "Extension to put on the file", Parameter.DROPDOWN, new String[] { "csv", "arff", "txt" }, 0);
		// Parameter p3 = new Parameter("File Prefix", "Prefix to put on the file", "File");
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
		parameterArray.addParameter(p1);
		parameterArray.addParameter(p2);
		// parameterArray.addParameter(p3);
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
		Vector<JEXData> datas = new Vector<JEXData>();
		JEXData temp = inputs.get("File 1");
		if(temp != null)
		{
			if(!temp.getTypeName().getType().equals(JEXData.FILE))
			{
				return false;
			}
			datas.add(temp);
		}
		temp = inputs.get("File 2");
		if(temp != null)
		{
			if(!temp.getTypeName().getType().equals(JEXData.FILE))
			{
				return false;
			}
			datas.add(temp);
		}
		temp = inputs.get("File 3");
		if(temp != null)
		{
			if(!temp.getTypeName().getType().equals(JEXData.FILE))
			{
				return false;
			}
			datas.add(temp);
		}
		temp = inputs.get("File 4");
		if(temp != null)
		{
			if(!temp.getTypeName().getType().equals(JEXData.FILE))
			{
				return false;
			}
			datas.add(temp);
		}
		temp = inputs.get("File 5");
		if(temp != null)
		{
			if(!temp.getTypeName().getType().equals(JEXData.FILE))
			{
				return false;
			}
			datas.add(temp);
		}
		temp = inputs.get("File 6");
		if(temp != null)
		{
			if(!temp.getTypeName().getType().equals(JEXData.FILE))
			{
				return false;
			}
			datas.add(temp);
		}
		temp = inputs.get("File 7");
		if(temp != null)
		{
			if(!temp.getTypeName().getType().equals(JEXData.FILE))
			{
				return false;
			}
			datas.add(temp);
		}
		temp = inputs.get("File 8");
		if(temp != null)
		{
			if(!temp.getTypeName().getType().equals(JEXData.FILE))
			{
				return false;
			}
			datas.add(temp);
		}
		temp = inputs.get("File 9");
		if(temp != null)
		{
			if(!temp.getTypeName().getType().equals(JEXData.FILE))
			{
				return false;
			}
			datas.add(temp);
		}
		temp = inputs.get("File 10");
		if(temp != null)
		{
			if(!temp.getTypeName().getType().equals(JEXData.FILE))
			{
				return false;
			}
			datas.add(temp);
		}
		
		// //// Get params
		// int depth =
		// Integer.parseInt(parameters.getValueOfParameter("Output Bit Depth"));
		String folderPath = this.parameters.getValueOfParameter("Folder Path");
		// String prefix = this.parameters.getValueOfParameter("File Prefix");
		String ext = this.parameters.getValueOfParameter("File Extension");
		
		File folder = new File(folderPath);
		// Run the function
		int n = 1;
		for (JEXData data : datas)
		{
			TreeMap<DimensionMap,String> filePaths = FileReader.readObjectToFilePathTable(data);
			
			if(!folder.exists())
			{
				folder.mkdirs();
			}
			
			int count = 0;
			int total = filePaths.size();
			JEXStatics.statusBar.setProgressPercentage(0);
			for (DimensionMap dim : filePaths.keySet())
			{
				String path = filePaths.get(dim);
				File f = new File(path);
				String fileName = f.getName();
				String newFilePath = folder.getAbsolutePath() + File.separator + data.name + " - " + FileUtility.getFileNameWithoutExtension(fileName) + "." + ext;
				
				try
				{
					JEXWriter.copy(f, new File(newFilePath));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				
				Logs.log("File Object " + n + ": Finished processing " + count + " of " + total + ".", 1, this);
				
			}
			// Status bar
			int percentage = (int) (100 * ((double) n / (double) datas.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
			n = n + 1;
		}
		
		// Return status
		return true;
	}
}
