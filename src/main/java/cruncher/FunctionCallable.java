package cruncher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import jex.statics.JEXStatics;
import logs.Logs;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;

public class FunctionCallable implements Callable<Integer> {
	
	JEXFunction function;
	JEXEntry entry;
	HashMap<String,JEXData> inputs;
	Set<JEXData> outputData;
	Ticket parent;
	
	public FunctionCallable(JEXFunction function, JEXEntry entry, HashMap<String,JEXData> inputs, Ticket parent)
	{
		this.function = function;
		this.entry = entry;
		this.inputs = inputs;
		this.parent = parent;
	}
	
	@Override
	public Integer call() throws Exception
	{
		// Run the function for this FunctionCallable's entry.
		this.outputData = new HashSet<JEXData>();
		
		this.parent.startedFunctionCallable();
		
		if(this.parent.isCanceled())
		{
			this.parent.finishedFunctionCallable();
			return -1;
		}
		
		try
		{
			// Run the JEXFunction
			this.outputData = this.run(this.entry, this.function);
			
			this.parent.finishedFunctionCallable();
		}
		catch (Exception e)
		{
			JEXStatics.statusBar.setStatusText("Error running " + this.function.getFunctionName() + " on entry " + this.entry.getEntryExperiment() + ": " + this.entry.getTrayX() + "," + this.entry.getTrayY());
			e.printStackTrace();
			return 0;
		}
		
		return 1;
	}
	
	/**
	 * Return the data objects outputted by the function
	 * 
	 * @return
	 */
	public Set<JEXData> getOutputtedData()
	{
		return this.outputData;
	}
	
	/**
	 * Returns the function object used to run this function
	 * 
	 * @return
	 */
	public JEXFunction getFunctionObject()
	{
		return this.function;
	}
	
	/**
	 * Returns the entry on which this function was run
	 * 
	 * @return
	 */
	public JEXEntry getEntry()
	{
		return this.entry;
	}
	
	private Set<JEXData> run(JEXEntry entry, JEXFunction func)
	{
		Logs.log("Running the function", 1, this);
		HashSet<JEXData> result = func.run(entry, this.inputs);
		return result;
	}
}
