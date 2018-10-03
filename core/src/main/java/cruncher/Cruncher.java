package cruncher;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXWorkflow;
import Database.DataReader.ImageReader;
import Database.Definition.TypeName;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.Canceler;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import miscellaneous.SimpleFileFilter;
import miscellaneous.StringUtility;
import tables.DimTable;
import tables.DimensionMap;
import weka.core.converters.JEXTableReader;

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
	private ExecutorService updaterService = Executors.newFixedThreadPool(1);

	private String dirToWatch;
	private DirectoryWatcher directoryWatcher;
	private Future<?> updaterFuture;
	private boolean canUpdate = false;
	private boolean updating = false;

	private TreeSet<JEXEntry> entriesToUpdate;
	private JEXWorkflow workflowToUpdate;
	private TypeName objectToUpdate;
	private boolean updateAutoSaving = false;
	private String timeToken;
	private CSVList xyTokens;
	private String fileExtension;

	private TreeSet<String> timesCompleted;

	public Cruncher()
	{
		// Initialize updater variables
		this.entriesToUpdate = new TreeSet<>();
		this.workflowToUpdate = new JEXWorkflow("Workflow Updater");
		this.directoryWatcher = new DirectoryWatcher();
		this.timesCompleted = new TreeSet<>(new StringUtility());
		this.initUpdaterService();

		// Initialize workflow variables
		this.tickets = new Vector<Pair<String,Vector<Ticket>>>(0);
		this.guiTasks = new Vector<Callable<Integer>>(0);
	}

	public BatchPanelList getBatchViewer()
	{
		return this.batchList;
	}

	public synchronized void runUpdate()
	{
		if(canUpdate)
		{
			Logs.log("Running Update." + this.updateAutoSaving, this);

			TreeMap<DimensionMap,String> files = this.getFilesInUpdateDirectory();

			TreeMap<String,TreeMap<JEXEntry,TreeMap<DimensionMap,String>>> queue = new TreeMap<>(new StringUtility());
			for(JEXEntry entry : this.entriesToUpdate)
			{
				// Get files already in database.
				TreeMap<DimensionMap,String> inputFiles = this.getFilesForEntry(entry);

				// Get files in the directory that pertain to this entry
				TreeMap<DimensionMap,String> entryFiles = this.getFilesForEntry(files, inputFiles);

				// Get files in the directory that pertain to new timepoints
				TreeMap<DimensionMap,String> newFiles = this.getNewFilesForEntry(entryFiles, inputFiles);

				// Get the complete new timepoints that exist for this object.
				DimTable template = new DimTable(inputFiles);
				this.timesCompleted.addAll(template.getDimWithName(timeToken).dimValues);
				template.removeDimWithName(timeToken);
				TreeMap<String, TreeMap<DimensionMap, String>> timepoints = getNewCompleteTimePoints(template, newFiles);

				for(String t : timepoints.keySet())
				{
					TreeMap<JEXEntry, TreeMap<DimensionMap,String>> data = queue.get(t);
					if(data == null)
					{
						data = new TreeMap<>();
					}
					data.put(entry, timepoints.get(t));
					queue.put(t, data);
				}
			}

			// For each time, if all the entries have complete timepoints, then submit them to the cruncher queue
			for(String t : queue.keySet())
			{
				if(queue.get(t).size() == this.entriesToUpdate.size() && !timesCompleted.contains(t))
				{
					Logs.log("Found a new completed timepoint (" + timeToken + "=" + t + ") for updating. Submitting workflow.", this);
					// Then submit a workflow.
					this.runWorkflow(this.getCopyOfWorkflow(), this.entriesToUpdate, this.updateAutoSaving, false);
					timesCompleted.add(t);
				}
			}

			if(!updating)
			{
				try
				{
					// Then call 'runUpdate' whenever a new file is added to this directory
					this.directoryWatcher.watch(this.dirToWatch);
					updating = true;
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}		

		//this.runWorkflow(this.workflowToUpdate, this.entriesToUpdate, this.updateAutoSaving, false);
	}

	private TreeMap<DimensionMap,String> getFilesInUpdateDirectory()
	{
		File dir = new File(this.dirToWatch);
		List<File> pendingImageFiles = FileUtility.getSortedFileList(dir.listFiles(new SimpleFileFilter(new String[] { this.fileExtension })));
		TreeMap<DimensionMap,String> files = new TreeMap<DimensionMap,String>();
		for(File f : pendingImageFiles)
		{
			DimensionMap fileDM = StringUtility.getMapFromPath(f.getAbsolutePath());
			if(fileDM != null)
			{
				files.put(fileDM, f.getAbsolutePath());
			}
		}
		return files;
	}

	private TreeMap<DimensionMap,String> getFilesForEntry(JEXEntry entry)
	{
		// Determine which times have already been run.
		JEXData input = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.objectToUpdate, entry);
		TreeMap<DimensionMap,String> temp = ImageReader.readObjectToImagePathTable(input);
		TreeMap<DimensionMap,String> ret = new TreeMap<>();
		for(Entry<DimensionMap,String> e : temp.entrySet())
		{
			DimensionMap pathDM = StringUtility.getMapFromPath(e.getValue());
			ret.put(pathDM, e.getValue());
		}
		return ret;
	}

	private TreeMap<DimensionMap,String> getFilesForEntry(TreeMap<DimensionMap,String> files, TreeMap<DimensionMap,String> inputFiles)
	{
		DimensionMap filter = new DimensionMap();
		DimensionMap template = inputFiles.firstKey();
		for(String xyToken : this.xyTokens)
		{
			filter.put(xyToken, template.get(xyToken));
		}
		TreeMap<DimensionMap,String> ret = JEXTableReader.filter(files, filter);
		return ret;
	}

	private TreeMap<DimensionMap,String> getNewFilesForEntry(TreeMap<DimensionMap,String> entryFiles, TreeMap<DimensionMap,String> inputFiles)
	{
		TreeMap<DimensionMap, String> ret = new TreeMap<>();
		for(Entry<DimensionMap,String> e : entryFiles.entrySet())
		{
			if(!inputFiles.containsKey(e.getKey()))
			{
				ret.put(e.getKey().copy(), e.getValue());
			}
		}
		return ret;
	}

	private TreeMap<String,TreeMap<DimensionMap,String>> getNewCompleteTimePoints(DimTable template, TreeMap<DimensionMap,String> newFiles)
	{
		// Organize files by time
		TreeMap<String,TreeMap<DimensionMap,String>> ret = new TreeMap<>();
		DimTable newDT = new DimTable(newFiles);
		for(DimTable timepoint : newDT.getSubTableIterator(timeToken))
		{
			// If this timepoint has already been submitted, skip this time.
			if(this.timesCompleted.contains(timepoint.getDimWithName(timeToken).valueAt(0)))
			{
				continue;
			}
			TreeMap<DimensionMap,String> timepointFiles = new TreeMap<>();
			for(DimensionMap map : timepoint.getMapIterator())
			{
				timepointFiles.put(map, newFiles.get(map));
			}
			ret.put(timepoint.getDimWithName(timeToken).valueAt(0), timepointFiles);
		}

		// Only keep timepoints that are complete
		Vector<String> timesToRemove = new Vector<>();
		for(String s : ret.keySet())
		{
			if(ret.get(s).size() < template.mapCount())
			{
				timesToRemove.add(s);
			}
		}
		for(String s : timesToRemove)
		{
			ret.remove(s);
		}

		return ret;
	}

	private JEXWorkflow getCopyOfWorkflow()
	{
		JEXWorkflow workflow = new JEXWorkflow("JEXWorkflow Temp Name");
		for (JEXFunction f : this.workflowToUpdate)
		{
			workflow.add(f.duplicate());
		}
		return workflow;
	}

	public void runWorkflow(JEXWorkflow workflow, TreeSet<JEXEntry> entries, boolean autoSave, boolean autoUpdate)
	{
		if(workflow.size() == 0)
		{
			Logs.log("No functions to run. Ignoring request.", this);
			return;
		}

		// Check if we should setup atuo-updating or not
		JEXFunction function0 = workflow.get(0);
		if(autoUpdate && function0.getFunctionName().equals("Import Virtual Image Updates"))
		{
			this.setUpCruncherForUpdating(workflow, entries, autoSave);
			this.runUpdate();
		}
		else
		{
			// Just run the workflow
			Batch batch = new Batch();
			for (JEXFunction function : workflow)
			{
				// Then create a ticket for this function and set of entries.
				Ticket ticket = new Ticket(function, entries, autoSave);
				batch.add(ticket);
			}
			this.batchList.add(batch);

			for (Ticket ticket : batch)
			{
				this.runTicket(ticket);
			}
		}
	}

	private void setUpCruncherForUpdating(JEXWorkflow workflow, TreeSet<JEXEntry> entries, boolean autoSave)
	{
		Logs.log("Attempting to set up Auto-Updating.", this);
		this.stopUpdater();

		JEXData input = null;
		JEXFunction function0 = workflow.get(0);
		for(JEXEntry entry : entries)
		{
			if(input == null)
			{
				input = JEXStatics.jexManager.getDataOfTypeNameInEntry(function0.inputs.firstEntry().getValue(), entry);
			}
			else
			{
				break;
			}
		}
		// If the data is virtual
		if(input != null && input.hasVirtualFlavor())
		{
			this.objectToUpdate = function0.inputs.firstEntry().getValue();
			this.workflowToUpdate.addAll(workflow);
			this.entriesToUpdate.addAll(entries);
			this.timesCompleted.clear();
			String path = ImageReader.readObjectToImagePath(input);
			String timeString = function0.parameters.getParameter("Time String Token").getValue();
			String xyString = function0.parameters.getParameter("XY String Token(s)").getValue();
			this.updateAutoSaving = autoSave;
			if(!timeString.equals(""))
			{
				this.dirToWatch = FileUtility.getFileParent(path);
				this.timeToken = StringUtility.removeWhiteSpaceOnEnds(timeString);
				this.xyTokens = StringUtility.getCSVListAndRemoveWhiteSpaceOnEnds(xyString);
				this.fileExtension = FileUtility.getFileNameExtension(path);
				this.canUpdate = true;
				this.updating = false;
			}
		}
		else
		{
			JEXDialog.messageDialog("Couldn't find an appropriate data in the entries for setting up auto-updating. Aborting.", this);
			return;
		}
	}

	private void initUpdaterService()
	{
		if(this.updaterFuture == null)
		{
			this.updaterFuture = this.updaterService.submit(this.directoryWatcher);
		}   
	}

	public void clearUpdaterVariables()
	{
		this.entriesToUpdate.clear();
		this.workflowToUpdate.clear();
		this.objectToUpdate = null;
		this.updateAutoSaving = false;
		this.fileExtension = null;
		this.timeToken = null;
		this.xyTokens = null;

		// abort watcher
		this.dirToWatch = null;
		this.canUpdate = false;
		this.updating = false;

		this.timesCompleted.clear();
	}

	public void stopUpdater()
	{
		this.clearUpdaterVariables();
		this.updaterFuture.cancel(true);
		this.updaterFuture = null;
		this.directoryWatcher.reset();
		this.initUpdaterService();
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
