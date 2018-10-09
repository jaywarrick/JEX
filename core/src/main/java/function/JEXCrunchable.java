package function;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Parameter;
import Database.Definition.ParameterSet;
import Database.Definition.Type;
import Database.Definition.TypeName;
import cruncher.Ticket;
import function.plugin.mechanism.JEXPluginInfo;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.Cancelable;
import miscellaneous.Canceler;

public abstract class JEXCrunchable implements Canceler, Cancelable {
	
	// Several statics
	public static int INPUTERROR = -1;
	public static int OUTPUTERROR = -2;
	public static int PARAMETERERROR = -3;
	public static int INPUTSOK = 1;
	public static int OUTPUTSOK = 2;
	public static int PARAMETERSOK = 3;
	public static int READYTORUN = 10;
	public static String THREADS = JEXPluginInfo.THREADS;
	
	// Convenience data types
	public static Type IMAGE     = JEXData.IMAGE;
	public static Type FILE      = JEXData.FILE;
	public static Type MOVIE     = JEXData.MOVIE;
	public static Type SOUND     = JEXData.SOUND;
	public static Type VALUE     = JEXData.VALUE;
	public static Type LABEL     = JEXData.LABEL;
	public static Type FUNCTION  = JEXData.FUNCTION_OLD;
	public static Type ROI       = JEXData.ROI;
	public static Type HIERARCHY = JEXData.HIERARCHY;
	public static Type TRACK     = JEXData.TRACK;
	public static Type ROITRACK  = JEXData.ROI_TRACK;
	
	// Class variables
	protected TreeMap<String,TypeName> inputs;
	protected TypeName[] outputNames;
	protected TypeName[] defaultOutputNames;
	protected ParameterSet parameters;
	protected HashSet<JEXData> realOutputs = new HashSet<JEXData>();
	
	protected int inputStatus = INPUTERROR;
	protected int parameterStatus = PARAMETERERROR;
	protected int outputStatus = OUTPUTERROR;
	
	private Canceler canceler = null;
	
	// Class methods required to be written by the user in the function template
	public abstract boolean showInList();
	
	public abstract boolean isInputValidityCheckingEnabled();
	
	public abstract String getName();
	
	public abstract String getInfo();
	
	public abstract String getToolbox();
	
	public abstract TypeName[] getInputNames();
	
	public abstract ParameterSet requiredParameters();
	
	// run before starting
	public void prepareEntry()
	{
		this.realOutputs = new HashSet<JEXData>();
	}
	
	// run after ending
	public void finalizeEntry()
	{
		JEXStatics.statusBar.setProgressPercentage(0);
	}
	
	public void prepareTicket()
	{   
		
	}
	
	public void finalizeTicket(Ticket ticket)
	{   
		
	}
	
	/**
	 * Set and check the inputs of this function
	 * 
	 * @param inputs
	 * @return an integer status of the input setting
	 */
	public int setInputs(TreeMap<String,TypeName> inputs)
	{
		this.inputs = inputs;
		this.inputStatus = INPUTERROR;
		
		if(inputs.size() != this.getInputNames().length)
		{
			this.inputStatus = INPUTERROR;
			return INPUTERROR;
		}
		if(this.isInputValidityCheckingEnabled())
		{
			for (TypeName tn : inputs.values())
			{
				if(tn == null)
				{
					Logs.log("This input is not set", 1, this);
				}
				else
				{
					Logs.log("Set input name:" + tn.getName() + " type:" + tn.getType() + " dimension:" + tn.getDimension(), 1, this);
				}
			}
			this.inputStatus = this.checkInputs();
		}
		
		return this.inputStatus;
	}
	
	/**
	 * Set the input of a given name
	 * 
	 * @param inputName
	 * @param input
	 * @return an integer status of the input setting
	 */
	public int setInput(String inputName, TypeName input)
	{
		this.inputStatus = INPUTERROR;
		this.inputs.put(inputName, input);
		
		if(this.inputs.size() != this.getInputNames().length)
		{
			this.inputStatus = INPUTERROR;
			return INPUTERROR;
		}
		if(this.isInputValidityCheckingEnabled())
		{
			this.checkInputs();
		}
		
		return this.inputStatus;
	}
	
	/**
	 * Return the set input list for this function
	 * 
	 * @return
	 */
	public TreeMap<String,TypeName> getInputs()
	{
		return this.inputs;
	}
	
	/**
	 * Set and check the parameters of this function
	 * 
	 * @param parameters
	 * @returnan integer status of the parameters of the function
	 */
	public int setParameters(ParameterSet parameters)
	{
		this.parameters = parameters;
		
		ParameterSet params = this.requiredParameters();
		for (Parameter param : params.getParameters())
		{
			String p = parameters.getValueOfParameter(param.title);
			if(p == null)
			{
				return PARAMETERERROR;
			}
		}
		
		return PARAMETERSOK;
	}
	
	/**
	 * Check the inputs and runability of the function
	 * 
	 * @return
	 */
	public int checkInputs()
	{
		
		return INPUTSOK;
	}
	
	/**
	 * Set the names of all the outputs
	 * 
	 * @param outputNames
	 * @return An integer status of output name setting
	 */
	public void setOutputs(TreeMap<Integer,TypeName> outputs)
	{
		if(this.outputNames == null)
		{
			TypeName[] defaultOutputs = this.getOutputs();
			this.outputNames = new TypeName[defaultOutputs.length];
			for(int i = 0; i < defaultOutputs.length; i++)
			{
				this.outputNames[i] = defaultOutputs[i].duplicate();
			}
		}
		for (int i = 0; i < this.outputNames.length; i++)
		{
			TypeName tn = this.outputNames[i];
			tn.setName(outputs.get(i).getName());
		}
	}
	
	/**
	 * Return the array of output names defined for this function
	 * 
	 * @return
	 */
	public abstract TypeName[] getOutputs();
	
	/**
	 * Returns true if the user wants to allow multithreading
	 * 
	 * @return
	 */
	public abstract boolean allowMultithreading();
	
	/**
	 * If multithreading is allowed, return the number of threads on which that this function should allow.
	 * 
	 * @return
	 */
	public Integer numThreads()
	{
		if(this.allowMultithreading())
		{
			String numThreads = this.requiredParameters().getValueOfParameter(JEXCrunchable.THREADS);
			if(numThreads != null)
			{
				return new Integer(numThreads);
			}
			else
			{
				return null;
			}
		}
		else
		{
			return new Integer(1);
		}
	}
	
	/**
	 * Set the progress indicator of this function
	 * 
	 * @param progress
	 */
	public void setProgress(int progress)
	{   
		
	}
	
	/**
	 * Return an array of output data objects for this function
	 * 
	 * @return Outputs of this array
	 */
	public HashSet<JEXData> getRealOutputs()
	{
		return this.realOutputs;
	}
	
	public abstract boolean run(JEXEntry entry, HashMap<String,JEXData> inputs);
	
	public void setCanceler(Canceler canceler)
	{
		this.canceler = canceler;
	}
	
	public Canceler getCanceler()
	{
		return this.canceler;
	}
	
	public boolean isCanceled()
	{
		return this.canceler.isCanceled();
	}
	
	public static boolean isInputValid(JEXData data, Type type)
	{
		if(data != null && type.matches(JEXData.ANY))
		{
			return true;
		}
		if(data == null || !data.getTypeName().getType().matches(type))
		{
			return false;
		}
		return true;
	}
	
	private static String[] getStringArrayIntegerList(int min, int max)
	{
		String[] ret = new String[max - min + 1];
		for(int i = 0; i <= max-min; i++)
		{
			ret[i] = "" + (i + min);
		}
		return ret;
	}
	
	public static Parameter getNumThreadsParameter(int maxThreads, int numThreads)
	{
		return new Parameter(THREADS, "Number of parallel threads to run this function on.", Parameter.DROPDOWN, getStringArrayIntegerList(1, maxThreads), numThreads-1);
	}

	public Boolean getInputUpdatableStatus(String inputName)
	{
		return true;
	}
}
