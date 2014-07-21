package jex.jexTabPanel.jexFunctionPanel;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXWorkflow;
import Database.Definition.TypeName;
import cruncher.JEXFunction;
import guiObject.DialogGlassPane;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import jex.ErrorMessagePane;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
import net.miginfocom.swing.MigLayout;

public class JEXFunctionPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	// GUI
	private JSplitPane centerPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	protected FunctionListPanel centerTopHalf;
	public FunctionParameterPanel paramPanel;
	public JPanel centerBottomHalf;
	// private FunctionPreviewPanel functionPreviewPanel ;
	
	// Function list
	public static List<FunctionBlockPanel> functionList = new ArrayList<FunctionBlockPanel>(0);
	
	JEXFunctionPanel()
	{
		initialize();
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		// SSCenter.defaultCenter().disconnect(this);
	}
	
	private void initialize()
	{
		// functionPreviewPanel = new FunctionPreviewPanel();
		// Create the parameters panel
		this.paramPanel = new FunctionParameterPanel();
		this.centerBottomHalf = new JPanel();
		this.centerBottomHalf.setLayout(new MigLayout("flowx, ins 0", "[fill, grow, 10:1000]0", "[fill, grow]"));
		this.centerBottomHalf.setBackground(DisplayStatics.lightBackground);
		this.centerBottomHalf.add(JEXStatics.cruncher.batchList.panel(), "grow");
		this.centerBottomHalf.add(paramPanel.panel(), "grow");
		
		centerTopHalf = new FunctionListPanel(this);
		
		centerPane.setBackground(DisplayStatics.background);
		centerPane.setBorder(BorderFactory.createEmptyBorder());
		centerPane.setLeftComponent(centerTopHalf.panel());
		centerPane.setRightComponent(centerBottomHalf);
		centerPane.setDividerLocation(250);
		centerPane.setDividerSize(6);
		centerPane.setResizeWeight(0);
		
		this.setLayout(new BorderLayout());
		this.setBackground(DisplayStatics.background);
		this.add(centerPane);
		this.repaint();
	}
	
	// //// Getters and setters
	
	/**
	 * Return the function list
	 */
	public List<FunctionBlockPanel> getFunctionPanels()
	{
		return JEXFunctionPanel.functionList;
	}
	
	/**
	 * Move the function functionBlock up one position
	 * 
	 * @param functionBlock
	 */
	public void upOne(FunctionBlockPanel functionBlock)
	{
		// Get the index of the function to move up
		int index = getIndex(functionBlock);
		if(index == -1)
			return;
		
		// Make the new index of the function
		int newIndex = index - 1;
		if(newIndex < 0)
			newIndex = 0;
		
		// Move the function
		functionList.remove(index);
		functionList.add(newIndex, functionBlock);
		
		// Refresh display
		centerTopHalf.rebuildList();
	}
	
	/**
	 * Move the function functionBlock down one position
	 * 
	 * @param functionBlock
	 */
	public void downOne(FunctionBlockPanel functionBlock)
	{
		// Get the index of the function to move down
		int index = getIndex(functionBlock);
		if(index == -1)
			return;
		
		// Make the new index of the function
		int newIndex = index + 1;
		if(newIndex >= functionList.size())
			newIndex = functionList.size() - 1;
		
		// Move the function
		functionList.remove(index);
		functionList.add(newIndex, functionBlock);
		
		// Refresh display
		centerTopHalf.rebuildList();
	}
	
	/**
	 * Delete the function
	 * 
	 * @param functionBlock
	 */
	public void delete(FunctionBlockPanel functionBlock)
	{
		// Get the index of the function to delete
		int index = getIndex(functionBlock);
		if(index == -1)
			return;
		
		// Remove the function
		JEXFunctionPanel.functionList.remove(index);
		
		// Refresh display
		centerTopHalf.rebuildList();
	}
	
	/**
	 * Return the index of function in functionblockpanel functionBlack
	 * 
	 * @param functionBlack
	 * @return
	 */
	private int getIndex(FunctionBlockPanel functionBlack)
	{
		for (int i = 0; i < functionList.size(); i++)
		{
			FunctionBlockPanel fb = functionList.get(i);
			if(fb == functionBlack)
				return i;
		}
		return -1;
	}
	
	/**
	 * Add a function to the list
	 * 
	 * @param function
	 */
	public FunctionBlockPanel addFunction(JEXFunction function)
	{
		// Create the new function block
		FunctionBlockPanel newFunc = new FunctionBlockPanel(this);
		newFunc.setFunction(function);
		
		// add it to the list
		JEXFunctionPanel.functionList.add(newFunc);
		
		// rebuild the display
		centerTopHalf.rebuildList();
		
		// set the selected function
		this.selectFunction(newFunc);
		
		// return the functionblock
		return newFunc;
	}
	
	/**
	 * Run the function FUNCTION
	 * 
	 * @param function
	 */
	public void runOneFunction(JEXFunction function, boolean autoSave)
	{
		// Get the entries to run the function on
		TreeSet<JEXEntry> entries = null;
		entries = JEXStatics.jexManager.getSelectedEntries();
		if(entries == null || entries.size() == 0)
		{
			Logs.log("No selected entries to run the function", 1, this);
			JEXStatics.statusBar.setStatusText("No selected entries");
			
			DialogGlassPane diagPanel = new DialogGlassPane("Warning");
			diagPanel.setSize(400, 200);
			
			ErrorMessagePane errorPane = new ErrorMessagePane("You need to select at least one entry in the database to run this function");
			diagPanel.setCentralPanel(errorPane);
			
			JEXStatics.main.displayGlassPane(diagPanel, true);
			return;
		}
		if(function == null || functionList.size() == 0)
		{
			Logs.log("No functions were specified", 1, this);
			JEXStatics.statusBar.setStatusText("No functions were specified");
			
			DialogGlassPane diagPanel = new DialogGlassPane("Warning");
			diagPanel.setSize(400, 200);
			
			ErrorMessagePane errorPane = new ErrorMessagePane("You need to select at least one function to run");
			diagPanel.setCentralPanel(errorPane);
			
			JEXStatics.main.displayGlassPane(diagPanel, true);
			return;
		}
		
		// / Else ///
		// Loop through the functions
		JEXWorkflow workflow = new JEXWorkflow("JEXWorkflow Temp Name");
		// Get the function into a workflow object
		workflow.add(function.duplicate());
		
		JEXStatics.cruncher.runWorkflow(workflow, entries, autoSave);
		Logs.log(function.getFunctionName() + " submitted", 0, this);
	}
	
	/**
	 * Run all the functions in the list one by one for all entries available
	 */
	public void runAllFunctions(boolean autoSave)
	{
		// Get the entries to run the function on
		TreeSet<JEXEntry> entries = null;
		entries = JEXStatics.jexManager.getSelectedEntries();
		if(entries == null || entries.size() == 0)
		{
			Logs.log("No selected entries to run the function", 1, this);
			JEXStatics.statusBar.setStatusText("No selected entries");
			
			DialogGlassPane diagPanel = new DialogGlassPane("Warning");
			diagPanel.setSize(400, 200);
			
			ErrorMessagePane errorPane = new ErrorMessagePane("You need to select at least one entry in the database to run this function");
			diagPanel.setCentralPanel(errorPane);
			
			JEXStatics.main.displayGlassPane(diagPanel, true);
			return;
		}
		if(functionList.size() == 0)
		{
			Logs.log("No functions were specified", 1, this);
			JEXStatics.statusBar.setStatusText("No functions were specified");
			
			DialogGlassPane diagPanel = new DialogGlassPane("Warning");
			diagPanel.setSize(400, 200);
			
			ErrorMessagePane errorPane = new ErrorMessagePane("You need to select at least one function to run");
			diagPanel.setCentralPanel(errorPane);
			
			JEXStatics.main.displayGlassPane(diagPanel, true);
			return;
		}
		
		// / Else ///
		// Loop through the functions
		JEXWorkflow workflow = new JEXWorkflow("JEXWorkflow Temp Name");
		for (FunctionBlockPanel fb : functionList)
		{
			// Get the function
			JEXFunction function = fb.getFunction();
			workflow.add(function.duplicate());
		}
		
		JEXStatics.cruncher.runWorkflow(workflow, entries, autoSave);
		
		// Save off workflow object
		String firstFunc = workflow.get(0).getFunctionName();
		String lastFunc = null;
		if(workflow.size() > 1)
		{
			lastFunc = workflow.get(workflow.size() - 1).getFunctionName();
		}
		String workflowName = null;
		if(lastFunc != null)
		{
			workflowName = JEXStatics.jexDBManager.getUniqueObjectName(entries, JEXData.WORKFLOW, firstFunc + " - " + lastFunc);
		}
		else
		{
			workflowName = JEXStatics.jexDBManager.getUniqueObjectName(entries, JEXData.WORKFLOW, firstFunc);
		}
		
		TreeMap<JEXEntry,JEXData> toAdd = new TreeMap<JEXEntry,JEXData>();
		for (JEXEntry entry : entries)
		{
			workflow.setName(workflowName);
			JEXData work = workflow.toJEXData();
			toAdd.put(entry, work);
		}
		JEXStatics.jexDBManager.saveDataInEntries(toAdd);
	}
	
	/**
	 * Select the function form functionblockpanel fb
	 * 
	 * @param fb
	 */
	public void selectFunction(FunctionBlockPanel fb)
	{
		this.centerTopHalf.selectFunction(fb);
	}
	
	/**
	 * Returns the selected function
	 * 
	 * @return
	 */
	public FunctionBlockPanel getSelectedFunction()
	{
		return this.centerTopHalf.getSelectedFunction();
	}
	
	/**
	 * Save the list of functions into an arff file
	 * 
	 * @param Path
	 */
	public void saveFunctionList(File file)
	{
		// Loop through the functions
		JEXWorkflow toSave = new JEXWorkflow(FileUtility.getFileNameWithoutExtension(file.getPath()));
		for (FunctionBlockPanel fb : JEXFunctionPanel.functionList)
		{
			// Get the function object
			JEXFunction function = fb.getFunction();
			toSave.add(function);
		}
		toSave.saveWorkflow(file);
	}
	
	/**
	 * Load function located at path FILE
	 * 
	 * @param file
	 */
	public void loadWorkflowFile(File file)
	{
		JEXWorkflow workflow = JEXWorkflow.loadWorkflowFile(file);
		loadWorkflow(workflow);
	}
	
	/**
	 * Load function located at path FILE
	 * 
	 * @param file
	 */
	public void loadWorkflow(JEXWorkflow workflow)
	{
		for (JEXFunction func : workflow)
		{
			FunctionBlockPanel fb = addFunction(func);
			fb.testFunction();
		}
	}
	
	/**
	 * Return true if the typename is set for the output of one of the existing functions
	 * 
	 * @param tn
	 * @return
	 */
	public boolean isDataOutputOfFunction(TypeName tn)
	{
		for (FunctionBlockPanel fb : JEXFunctionPanel.functionList)
		{
			JEXFunction function = fb.getFunction();
			
			// Get the set names
			TreeMap<Integer,TypeName> outputs = function.getExpectedOutputs();
			
			// Get the normal names
			for (Integer index : outputs.keySet())
			{
				if(tn.equals(outputs.get(index)))
					return true;
			}
		}
		
		return false;
	}
	
	public boolean isAutoSaveSelected()
	{
		return this.centerTopHalf.isAutoSaveSelected();
	}
	
}