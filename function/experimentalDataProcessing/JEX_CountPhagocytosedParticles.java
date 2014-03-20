package function.experimentalDataProcessing;

import image.roi.ROIPlus;

import java.util.HashMap;
import java.util.TreeMap;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.ValueWriter;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
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
public class JEX_CountPhagocytosedParticles extends ExperimentalDataCrunch {
	
	public JEX_CountPhagocytosedParticles()
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
		String result = "Count phagocytosed particles";
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
		String result = "Function that allows you to count the number of particle per cell and cells containing a phagocytosed particle.";
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
		String toolbox = "Custom Cell Analysis";
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
		inputNames[0] = new TypeName(ROI, "Spore ROI");
		inputNames[1] = new TypeName(ROI, "Cell ROI");
		inputNames[2] = new TypeName(ROI, "Cell without spores ROI");
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
		defaultOutputNames = new TypeName[2];
		defaultOutputNames[0] = new TypeName(VALUE, "Spores per cell");
		defaultOutputNames[1] = new TypeName(VALUE, "Cells with spores");
		
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
		// Make an array of the parameters and return it
		ParameterSet parameterArray = new ParameterSet();
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
		JEXData sporeRoiData = inputs.get("Spore ROI");
		if(sporeRoiData == null || !sporeRoiData.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Collect the inputs
		JEXData cellRoiData = inputs.get("Cell ROI");
		if(cellRoiData == null || !cellRoiData.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Collect the inputs
		JEXData cellWithoutRoiData = inputs.get("Cell without spores ROI");
		if(cellWithoutRoiData == null || !cellWithoutRoiData.getTypeName().getType().equals(JEXData.ROI))
			return false;
		
		// Run the function
		TreeMap<DimensionMap,ROIPlus> sporeRois = RoiReader.readObjectToRoiMap(sporeRoiData);
		TreeMap<DimensionMap,ROIPlus> cellRois = RoiReader.readObjectToRoiMap(cellRoiData);
		TreeMap<DimensionMap,ROIPlus> cellNOTRois = RoiReader.readObjectToRoiMap(cellWithoutRoiData);
		
		// Set the variables
		int index = 0;
		double sporesPerCell = 0;
		double cellWithSpores = 0;
		
		// Loop through the rois
		for (DimensionMap dim : sporeRois.keySet())
		{
			// Get the rois
			ROIPlus sporeRoi = sporeRois.get(dim);
			ROIPlus cellRoi = cellRois.get(dim);
			ROIPlus cellNOTRoi = cellNOTRois.get(dim);
			if(sporeRoi == null || cellRoi == null || cellNOTRoi == null)
				continue;
			
			// Count the cells
			int sporeNb = sporeRoi.getPointList().size();
			int cellNb = cellRoi.getPointList().size();
			int cellNotNb = cellNOTRoi.getPointList().size();
			
			// Update the variables
			index++;
			sporesPerCell = sporesPerCell + ((double) sporeNb / (double) cellNb);
			cellWithSpores = cellWithSpores + (((double) cellNb - cellNotNb) / cellNb);
		}
		
		// Finalize variables
		sporesPerCell = sporesPerCell / (index);
		cellWithSpores = 100 * cellWithSpores / (index);
		
		JEXData output1 = ValueWriter.makeValueObject(outputNames[0].getName(), "" + sporesPerCell);
		JEXData output2 = ValueWriter.makeValueObject(outputNames[1].getName(), "" + cellWithSpores);
		
		// Set the outputs
		realOutputs.add(output1);
		realOutputs.add(output2);
		
		// Return status
		return true;
	}
}
