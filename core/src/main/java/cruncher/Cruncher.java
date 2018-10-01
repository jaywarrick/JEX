package cruncher;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXWorkflow;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.Canceler;
import miscellaneous.Pair;

/**
 * Note that Canceler only refers to GUI Tasks for now.
 * @author jay_warrick
 *
 */
public class Cruncher implements Canceler {
	
	public volatile boolean stopCrunch = false;
	public volatile boolean stopGuiTask = false;
	
	public static int STANDARD_NUM_THREADS = 4;
	
	public BatchPanelList batchList = new BatchPanelList();
	
	List<Callable<Integer>> guiTasks;
	Vector<Pair<String,Vector<Ticket>>> tickets; // Tickets
	// organized
	// by
	// Workflow
	// and
	// entry
	public final ExecutorService guiTicketQueue = Executors.newFixedThreadPool(1);
	private final ExecutorService ticketQueue = Executors.newFixedThreadPool(1);
	private ExecutorService multiFunctionQueue = Executors.newFixedThreadPool(5);
	private ExecutorService singleFunctionQueue = Executors.newFixedThreadPool(1);
	private JEXWorkflow workflowToUpdate = null;
	private TreeSet<JEXEntry> entriesToUpdate = new TreeSet<>();
	private boolean updateAutoSaving = false;
	
	public Cruncher()
	{
		this.tickets = new Vector<Pair<String,Vector<Ticket>>>(0);
		this.guiTasks = new Vector<Callable<Integer>>(0);
	}
	
	public BatchPanelList getBatchViewer()
	{
		return this.batchList;
	}
	
	public void runUpdate()
	{
		this.runWorkflow(this.workflowToUpdate, this.entriesToUpdate, this.updateAutoSaving, false);
	}
	
	public void cancelUpdating()
	{
		this.workflowToUpdate = null;
		this.entriesToUpdate.clear();
		this.updateAutoSaving = false;
		
	}
	
	public void runWorkflow(JEXWorkflow workflow, TreeSet<JEXEntry> entries, boolean autoSave, boolean autoUpdate)
	{
		// Save info in case we need to run this again during updates
		if(autoUpdate && workflow.get(0).getFunctionName().equals("Import Virtual Image Updates"))
		{
			this.workflowToUpdate.addAll(workflow);
			this.entriesToUpdate.addAll(entries);
			this.updateAutoSaving = autoSave;
		}
		
		Batch batch = new Batch();
		boolean first = true;
		for (JEXFunction function : workflow)
		{
			if(first)
			{
				
			}
			Ticket ticket = new Ticket(function, entries, autoSave);
			batch.add(ticket);
		}
		this.batchList.add(batch);
		
		for (Ticket ticket : batch)
		{
			this.runTicket(ticket);
		}
	}
	
	public void runTicket(Ticket ticket)
	{
		Logs.log("Added ticket to running queue ", 1, this);
		JEXStatics.statusBar.setStatusText("Added ticket to running queue ");
		this.ticketQueue.submit(ticket);
	}
	
	public void setNumThreads(Integer numThreads)
	{
		this.multiFunctionQueue.shutdown();
		if(numThreads == null)
		{
			this.multiFunctionQueue = Executors.newFixedThreadPool(STANDARD_NUM_THREADS);
		}
		else
		{
			this.multiFunctionQueue = Executors.newFixedThreadPool(numThreads);
		}
	}
	
	public Future<Integer> runFunction(FunctionCallable function)
	{
		Logs.log("Added function to cruncher queue ", 1, this);
		JEXStatics.statusBar.setStatusText("Added function to cruncher queue ");
		Future<Integer> result = null;
		if(function.getFunctionObject().getCrunch().allowMultithreading())
		{
			result = this.multiFunctionQueue.submit(function);
		}
		else
		{
			result = this.singleFunctionQueue.submit(function);
		}
		return result;
	}
	
	public synchronized void finishTicket(Ticket ticket)
	{
		String str = "Crunch canceled, failed, or created no objects. No changes made.";
		if(ticket == null)
		{
			JEXStatics.statusBar.setStatusText(str);
			Logs.log(str, 0, this);
			return;
		}
		TreeMap<JEXEntry,Set<JEXData>> outputList = ticket.outputList;
		if(outputList == null || outputList.size() == 0)
		{
			JEXStatics.statusBar.setStatusText(str);
			Logs.log(str, 0, this);
			return;
		}
		JEXStatics.statusBar.setStatusText("Function successful. Creating output objects.");
		Logs.log("Function successful. Creating output objects.", 0, this);
		JEXStatics.jexDBManager.saveDataListInEntries(outputList, true);
		
		if(ticket.getAutoSave())
		{
			JEXStatics.main.save();
		}
	}
	
	public Future<Object> runGuiTask(Callable<Object> guiTask)
	{
		this.stopGuiTask = false;
		Logs.log("Added GUI task to running queue. Use Ctrl(CMD)+G to cancel the current task. ", 1, this);
		JEXStatics.statusBar.setStatusText("Added GUI task to running queue. Use Ctrl(CMD)+G to cancel the current task. ");
		return this.guiTicketQueue.submit(guiTask);
	}
	
	public synchronized void finishImportThread(ImportThread importThread)
	{
		String str = "Import canceled, failed, or no objects created. No changes made.";
		boolean successful = false;
		if(importThread == null)
		{
			JEXStatics.statusBar.setStatusText(str);
			Logs.log(str, 0, this);
			return;
		}
		TreeMap<JEXEntry,JEXData> toAdd = importThread.toAdd;
		if(toAdd == null || toAdd.size() == 0)
		{
			JEXStatics.statusBar.setStatusText(str);
			Logs.log(str, 0, this);
			return;
		}
		successful = JEXStatics.jexDBManager.saveDataInEntries(toAdd);
		if(successful)
		{
			JEXStatics.statusBar.setStatusText("Objects imported successfully");
			JEXStatics.jexManager.saveCurrentDatabase();
		}
		else
		{
			JEXStatics.statusBar.setStatusText("Import failed or created no objects. No changes made.");
			Logs.log(str, 0, this);
		}
	}
	
	public synchronized void finishExportThread(ExportThread exportThread)
	{
		String str = "Export canceled, failed, or no objects created. No changes made.";
		if(exportThread == null)
		{
			JEXStatics.statusBar.setStatusText(str);
			Logs.log(str, 0, this);
			return;
		}
		JEXStatics.statusBar.setStatusText("Objects exported successfully");
	}

	@Override
	public boolean isCanceled() {
		return this.stopGuiTask;
	}
	
}
