package cruncher;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.Canceler;
import miscellaneous.LSVList;
import signals.SSCenter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.ParameterSet;
import Database.Definition.TypeName;
import function.JEXCrunchable;

public class Ticket implements Callable<Integer>, Canceler{
	
	public static String SIG_TicketFinished_NULL = "SIG_TicketFinished_NULL";
	public static String SIG_TicketUpdated_NULL = "SIG_TicketUpdated_NULL";
	public static String SIG_TicketStarted_NULL = "SIG_TicketStarted_NULL";
	public static final String[] CRUNCHFLAG_NAMES = new String[]{"FAILED","SUCCESS","CANCELED"};
	public static final String[] TICKETFLAG_NAMES = new String[]{"FAILED","SUCCESS","CANCELED","JAVA ERROR"};
	public static final Integer CRUNCHFLAG_FAILED = 0, CRUNCHFLAG_SUCCESS = 1, CRUNCHFLAG_CANCELED = 2;
	public static final Integer TICKETFLAG_FAILED = 0, TICKETFLAG_SUCCESS = 1, TICKETFLAG_CANCELED = 2, TICKETFLAG_ERROR = 4; 
	
	TreeMap<JEXEntry,JEXFunction> runList;
	TreeMap<JEXEntry,Future<Integer>> futures;
	TreeMap<JEXEntry,Integer> flags; // -2 not run, -1 canceled, 0 failed, 1 success
	JEXCrunchable cr;
	ParameterSet firstParamSet;
	TreeMap<JEXEntry,Set<JEXData>> outputList = new TreeMap<JEXEntry,Set<JEXData>>();
	int functionsStarted = 0, functionsFinished = 0;
	String startTime = "", endTime = "";
	private volatile boolean canceled = false;
	
	public boolean autoSave;
	public Batch parent;
	
	public Ticket(JEXFunction function, TreeSet<JEXEntry> entries, boolean autoSave)
	{
		
		// Duplicate the functions and make a run list for making a ticket
		this.runList = new TreeMap<JEXEntry,JEXFunction>();
		this.flags = new TreeMap<JEXEntry,Integer>();
		for (JEXEntry entry : entries)
		{
			JEXFunction func = function.duplicate();
			func.cruncher.setCanceler(this);
			this.runList.put(entry, func);
		}
		
		this.autoSave = autoSave;
		
		// ///////////////////////////////////////////
		// Initialize Ticket with function information
		// So later edits in gui don't affect ticket
		// ///////////////////////////////////////////
		
		// Get the experimental data crunch
		JEXFunction func = this.runList.firstEntry().getValue();
		this.cr = func.getCrunch();
		
		// Gather the paramset for sharing if necessary
		if(this.firstParamSet == null)
		{
			this.firstParamSet = func.getParameters();
		}
		
	}
	
	public void setParentBatch(Batch batch)
	{
		this.parent = batch;
	}
	
	public void setAutoSave(boolean autoSave)
	{
		this.autoSave = autoSave;
	}
	
	public boolean getAutoSave()
	{
		return this.autoSave;
	}
	
	public synchronized void startedFunctionCallable()
	{
		this.functionsStarted = this.functionsStarted + 1;
		SSCenter.defaultCenter().emit(this, SIG_TicketUpdated_NULL, (Object[]) null);
	}
	
	public synchronized void finishedFunctionCallable()
	{
		this.functionsFinished = this.functionsFinished + 1;
		SSCenter.defaultCenter().emit(this, SIG_TicketUpdated_NULL, (Object[]) null);
	}
	
	public int size()
	{
		return this.runList.size();
	}
	
	public TreeMap<JEXEntry,Set<JEXData>> getOutputList()
	{
		return this.outputList;
	}
	
	private void setOutputtedData(TreeMap<JEXEntry,Set<JEXData>> outputList)
	{
		this.outputList = outputList;
	}
	
	@Override
	public Integer call() throws Exception
	{
		try
		{
			Logs.log("Running new ticket ", 1, this);
			
			this.startTime = DateFormat.getDateTimeInstance().format(new Date());
			SSCenter.defaultCenter().emit(this, SIG_TicketStarted_NULL, (Object[]) null);
			
			// Stop if canceled;
			if(this.canceled == true)
			{
				return this.finish(TICKETFLAG_CANCELED);
			}
			
			// If the experimental data crunch is null then stop right here
			if(this.cr == null)
			{
				Logs.log("Why the heck would this EVER be null?!?!", this);
			}
			
			// Gather inputs and submit the functioncallables to the executor
			// service
			this.futures = new TreeMap<JEXEntry,Future<Integer>>();
			TreeMap<JEXEntry,FunctionCallable> fcs = new TreeMap<JEXEntry,FunctionCallable>();
			for (JEXEntry entry : this.runList.keySet())
			{
				Logs.log("Submitting new function to the cruncher", 1, this);
				JEXStatics.statusBar.setStatusText("Submitting new function to the cruncher");
				
				JEXFunction func = this.runList.get(entry);
				
				// Allow functions to share a paramset if they are not multithreaded
				// Needed for semiManual actions.
				if(!this.cr.allowMultithreading())
				{
					func.setParameters(this.firstParamSet);
				}
				
				// Gather the inputs and run
				// We gather inputs in the ticket because upon running we want to
				// grab inputs that may have been created with a ticket that was
				// submitted at the "same time" as this ticket
				FunctionCallable fc = this.getFunctionCallable(func, entry);
				Logs.log("Running entry: " + entry.toString(), 0, this);
				if(fc != null)
				{
					fcs.put(entry, fc);
					Future<Integer> future = JEXStatics.cruncher.runFunction(fc, this.cr.allowMultithreading());
					this.futures.put(entry,future);
				}
			}
			
			// Collect outputs and wait for them to finish
			@SuppressWarnings("unused")
			int done = 0;
			for (JEXEntry e : this.futures.keySet())
			{
				if(this.canceled == true)
				{
					this.finish(this.getTicketFlag());
				}
				// -1 for cancel, 0 for fail, 1 for success
				this.flags.put(e, this.futures.get(e).get());
			}
			
			// Collect outputted data
			TreeMap<JEXEntry,Set<JEXData>> output = new TreeMap<JEXEntry,Set<JEXData>>();
			for (JEXEntry entry : fcs.keySet())
			{
				if(this.canceled == true)
				{
					return this.finish(this.getTicketFlag());
				}
				// Collect the data objects
				Set<JEXData> datas = fcs.get(entry).getOutputtedData();
				
				output.put(entry, datas);
			}
			
			// Finalize the ticket
			this.setOutputtedData(output);
			if(this.cr != null)
			{
				this.cr.finalizeTicket(this);
			}
			
			// Interact with JEX through a single synchronized function within
			// cruncher
			// to ensure thread-safe behavior
			return this.finish(this.getTicketFlag());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return this.finish(TICKETFLAG_ERROR);
		}
	}
	
	public Integer finish(Integer flag)
	{
		JEXStatics.cruncher.finishTicket(this);
		this.endTime = DateFormat.getDateTimeInstance().format(new Date());
		SSCenter.defaultCenter().emit(this, Ticket.SIG_TicketFinished_NULL, (Object[]) null);
		this.parent.finishedTicket(this);
		return flag;
	}
	
	public boolean isCanceled()
	{
		return this.parent.isCanceled();
	}
	
	/** 
	 * If not entries have been run yet, return null.
	 * 
	 * If any entries have been run, return canceled if we find a canceled flag, if not then return failed if we find a failed flag, if not then return success
	 * @return
	 */
	public Integer getTicketFlag()
	{
		if(this.flags.size() == 0)
		{
			return null;
		}
		boolean foundFailed = false;
		boolean foundCanceled = false;
		for(Integer i : this.flags.values())
		{
			if(i == CRUNCHFLAG_FAILED)
			{
				foundFailed = true;
			}
			if(i == CRUNCHFLAG_CANCELED)
			{
				foundCanceled = true;
			}
		}
		
		if(foundCanceled)
		{
			return TICKETFLAG_CANCELED;
		}
		else if(foundFailed)
		{
			return TICKETFLAG_FAILED;
		}
		else
		{
			return TICKETFLAG_SUCCESS;
		}
	}
	
	/**
	 * Run the JEXfunction function on the pre-set entry entry
	 * 
	 * @param function
	 */
	public synchronized FunctionCallable getFunctionCallable(JEXFunction function, JEXEntry entry)
	{
		// Collect and verify the existence of the inputs
		TreeMap<String,TypeName> inputs = function.getInputs();
		
		// Collect the inputs
		HashMap<String,JEXData> collectedInputs = new HashMap<String,JEXData>();
		for (String inputName : inputs.keySet())
		{
			// Get the input TypeName
			TypeName tn = inputs.get(inputName);
			
			// Prepare the JEXData for the input to the function
			JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(tn, entry);
			
			// // If the data is null yet someone defined a typeName, print
			// error and return
			// if (tn != null && data == null)
			// {
			// Logs.log("Missing input data "+tn.toString(), 0,
			// this);
			// JEXStatics.statusBar.setStatusText("Missing input "+tn.toString());
			// return null;
			// }
			
			// Set the data as input to the function (a null tn indicates the
			// input is supposed to be optional)
			collectedInputs.put(inputName, data);
		}
		
		// Run the function
		FunctionCallable fc = new FunctionCallable(function.duplicate(), entry, collectedInputs, this);
		return fc;
		
	}
	
	public void printTicketFlags()
	{
		LSVList lines = new LSVList();
		lines.add("");
		for(JEXEntry e : this.flags.keySet())
		{
			lines.add("\tEntry X:" + e.getTrayX() + " Y:" + e.getTrayY() + " " + Ticket.CRUNCHFLAG_NAMES[this.flags.get(e)]);
		}
		Logs.log(lines.toString(), this);
	}
}
