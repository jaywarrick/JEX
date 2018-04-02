package jex.jexTabPanel.jexDistributionPanel;

import java.awt.Point;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.JPanel;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Experiment;
import Database.Definition.Type;
import cruncher.ImportThread;
import function.plugin.plugins.dataEditing.SplitObjectToSeparateEntries;
import guiObject.DialogGlassPane;
import jex.JEXManager;
import jex.jexTabPanel.JEXTabPanelController;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.ArrayUtility;
import miscellaneous.Pair;
import signals.SSCenter;
import tables.DimensionMap;

public class JEXDistributionPanelController extends JEXTabPanelController {

	// Other controllers
	public DistributorArray importController;
	public JEXDistributionRightPanel fileController;
	public FileListPanel flistpane;

	// Navigation
	public Experiment curTray = null;
	public HashMap<Point,Vector<Pair<DimensionMap,String>>> files;

	// Variables
	public HashMap<Integer,DimensionSelector> dimensions;
	public int dimension = 0;

	public JEXDistributionPanelController()
	{
		this.importController = new DistributorArray(this);
		this.fileController = new JEXDistributionRightPanel(this);
		this.files = new HashMap<Point,Vector<Pair<DimensionMap,String>>>();
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.NAVIGATION, this, "navigationChanged", (Class[]) null);

	}

	// //////////////////
	// SIGNAL METHODS //
	// //////////////////

	public void navigationChanged()
	{
		// Clear changes
		this.files.clear();

		// If tray viewed, change array view
		this.curTray = null;
		String viewedExp = JEXStatics.jexManager.getViewedExperiment();

		if(viewedExp != null)
		{
			TreeMap<String,Experiment> expTree = JEXStatics.jexManager.getExperimentTree();
			this.curTray = expTree.get(viewedExp);
		}

		if(this.importController != null)
		{
			this.importController.navigationChanged();
		}
	}

	public void fileListChanged()
	{
		List<File> files = this.flistpane.files2Distribute;
		this.fileController.setFileList(files);
	}

	public void addFiles2Distribute(List<File> files2Distribute)
	{
		if(this.flistpane == null)
		{
			this.flistpane = new FileListPanel(this);
		}
		if(this.flistpane.files2Distribute == null)
		{
			this.flistpane.files2Distribute = new java.util.ArrayList<File>();
		}

		for (File f : files2Distribute)
		{
			this.flistpane.files2Distribute.add(f);
		}
		this.fileController.setFileList(this.flistpane.files2Distribute);
	}

	// ///////////////////////
	// PREPARATION METHODS //
	// ///////////////////////

	/**
	 * Open the file selector panel
	 */
	public void selecFiles()
	{
		DialogGlassPane diagPanel = new DialogGlassPane("Choose files");
		diagPanel.setSize(350, 500);

		if(this.flistpane == null)
		{
			this.flistpane = new FileListPanel(this);
		}
		diagPanel.setCentralPanel(this.flistpane);

		JEXStatics.main.displayGlassPane(diagPanel, true);
	}

	/**
	 * Deal the files
	 */
	public void dealFiles()
	{
		TreeMap<DimensionMap,Integer> lut = SplitObjectToSeparateEntries.getLookUpTable(this.fileController.getStartPt(), this.importController.getNumberRows(), this.importController.getNumberColumns(), this.fileController.getFirstMoveHorizontal(), this.fileController.getSnaking());
		TreeMap<DimensionMap,Integer> lut_ticker = SplitObjectToSeparateEntries.getLookUpTable("UL", this.importController.getNumberRows(), this.importController.getNumberColumns(), true, false);
		TreeMap<Integer, DimensionMap> tul = new TreeMap<>();
		for(Entry<DimensionMap,Integer> e : lut.entrySet())
		{
			tul.put(e.getValue(), e.getKey());
		}

		// clear();

		// Set up the vectors for the ticking
		String[] dimVector = new String[this.dimensions.size()];
		int[] maxIncr = new int[this.dimensions.size()];
		int[] initial = new int[this.dimensions.size()];
		int[] increments = new int[this.dimensions.size()];
		int[] compteur = new int[this.dimensions.size()];
		int rowLoc = -1;
		int colLoc = -1;
		for (int i = 0, len = this.dimensions.size(); i < len; i++)
		{
			DimensionSelector selector = this.dimensions.get(new Integer(this.dimensions.size() - 1 - i));
			dimVector[i] = selector.getDimensionName();
			maxIncr[i] = selector.getDimensionSize() - 1;
			if(dimVector[i].equals("Array Column"))
			{
				maxIncr[i] = this.importController.getNumberRows() - 1;
				if(colLoc != -1)
				{
					colLoc = -2;
				}
				else
				{
					colLoc = i;
				}
			}
			if(dimVector[i].equals("Array Row"))
			{
				maxIncr[i] = this.importController.getNumberColumns() - 1;
				if(rowLoc != -1)
				{
					rowLoc = -2;
				}
				else
				{
					rowLoc = i;
				}
			}
			increments[i] = 1;
			initial[i] = 0;
			compteur[i] = 0;
		}

		// Use this to keep track of how many we have distributed to a location.
		HashMap<String,Integer> numDistributed = new HashMap<String,Integer>();

		// Test if the counter can perform
		if(rowLoc < 0 || colLoc < 0)
		{
			Logs.log("File distribution impossible... Must select array and row dimensions once and only once", 1, this);
			JEXStatics.statusBar.setStatusText("Error: Must select array and row dimensions once and only once");
			return;
		}

		// Initialize numDistributed
		for (int y = 0; y <= maxIncr[rowLoc]; y++)
		{
			for (int x = 0; x <= maxIncr[colLoc]; x++)
			{
				numDistributed.put("" + x + "," + y, 0);
			}
		}
		// Calculate max to distribute per entry
		// (i.e. use the dimension limits to calculate how many files can be
		// held in each well
		// if every dimension value were to be used)
		// Use this instead of checking what arrays have been visited
		// like we did before. We can't use that method because we have to index
		// through each
		// entry multiple times to deal out all the files instead of just going
		// into each well
		// and putting everything in there all at once.
		int maxDist = 1;
		for (int i : maxIncr)
		{
			maxDist = maxDist * (i + 1);
		}
		maxDist = maxDist / (maxIncr[rowLoc] + 1);
		maxDist = maxDist / (maxIncr[colLoc] + 1);

		// Do the dealing
		// Check to see if there are any wells selected. If not we risk
		// an infinite loop below during dealing where we skip non-selected
		// wells.
		Set<JEXEntry> selected = JEXStatics.jexManager.getSelectedEntries();
		if(selected == null || selected.size() == 0)
		{
			return;
		}

		this.files = new HashMap<Point,Vector<Pair<DimensionMap,String>>>();
		Iterator<Entry<Integer,DimensionMap>> itr = tul.entrySet().iterator();
		Iterator<Entry<DimensionMap,Integer>> itr_ticker = lut_ticker.entrySet().iterator();
		for (File f : this.flistpane.files2Distribute)
		{
			/////////////////////////
			if(!itr.hasNext())
			{
				itr = tul.entrySet().iterator();
			}
			if(!itr_ticker.hasNext())
			{
				itr_ticker = lut_ticker.entrySet().iterator();
			}
			int cellX = compteur[colLoc];
			int cellY = compteur[rowLoc];
			Integer wellIndex = lut_ticker.get(new DimensionMap("R=" + cellY + ",C=" + cellX));
			DimensionMap tulMap = tul.get(wellIndex);
			cellX = Integer.parseInt(tulMap.get("C"));
			cellY = Integer.parseInt(tulMap.get("R"));
			////////////////////////

//			int cellX = compteur[colLoc];
//			int cellY = compteur[rowLoc];

			// if the cell at location cellX and cellY is not valid skip it and
			// go to the next valid cell
			// We know from the test about 10 lines up that there is at least
			// one cell selected
			// so we don't risk an infinite loop and don't need to test what
			// locations we have visited previously.
			if(!this.hasValidCell())
			{
				JEXDialog.messageDialog("None of the array entries displayed are selected. Please select entries or navigate to a dataset with selected entries.", this);
				return;
			}
			while (!this.isValidCell(cellX, cellY))
			{
				// Go to the next cell in the array
				Logs.log("Skipped cell at location " + cellX + "-" + cellY, 1, this);

				/////////////////////////
				if(!itr.hasNext())
				{
					itr = tul.entrySet().iterator();
				}
				if(!itr_ticker.hasNext())
				{
					itr_ticker = lut_ticker.entrySet().iterator();
				}
				compteur = ArrayUtility.getNextCompteur(compteur, initial, maxIncr, increments);
				cellX = compteur[colLoc];
				cellY = compteur[rowLoc];
				wellIndex = lut_ticker.get(new DimensionMap("R=" + cellY + ",C=" + cellX));
				tulMap = tul.get(wellIndex);
				cellX = Integer.parseInt(tulMap.get("C"));
				cellY = Integer.parseInt(tulMap.get("R"));
				////////////////////////

//				compteur = ArrayUtility.getNextCompteur(compteur, initial, maxIncr, increments);
//				cellX = compteur[colLoc];
//				cellY = compteur[rowLoc];
			}

			// Check to see how many files we've distributed to this location.
			// If it
			// is greater than maxDist, then stop because there are no more
			// slots for
			// any more files in any entry (i.e. avoid overdistributing)
			int curCount = numDistributed.get("" + cellX + "," + cellY);
			if(curCount >= maxDist)
			{
				break;
			}

			// Fill the treemap of files
			Vector<Pair<DimensionMap,String>> richFileMap = this.files.get(new Point(cellX, cellY));
			if(richFileMap == null)
			{
				richFileMap = new Vector<Pair<DimensionMap,String>>();
				this.files.put(new Point(cellX, cellY), richFileMap);
			}
			DimensionMap map = this.makeMap(compteur, dimVector);
			richFileMap.add(new Pair<DimensionMap,String>(map, f.getAbsolutePath()));

			// Update the number of files that have distributed to this
			// location.
			numDistributed.put("" + cellX + "," + cellY, curCount + 1);
			compteur = ArrayUtility.getNextCompteur(compteur, initial, maxIncr, increments);
		}

		// Refresh the displayed array
		this.importController.setFileArray(this.files);
		Logs.log("Finished dropping files.", 0, this);
	}

	/**
	 * Reset the file list
	 */
	public void clear()
	{
		this.files = new HashMap<Point,Vector<Pair<DimensionMap,String>>>();
		this.importController.setFileArray(this.files);
	}

	/**
	 * Return if cell at location x and y accepts drops
	 * 
	 * @param x
	 * @param y
	 * @return true if cell accepts drops
	 */
	private boolean isValidCell(int x, int y)
	{
		HashMap<Point,Boolean> selectionArray = this.importController.getSelectionArray();
		Boolean selected = selectionArray.get(new Point(x, y));
		if(selected == null)
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	/**
	 * Return if cell at any location accepts drops
	 * 
	 * @param x
	 * @param y
	 * @return true if cell accepts drops
	 */
	private boolean hasValidCell()
	{
		HashMap<Point,Boolean> selectionArray = this.importController.getSelectionArray();
		for(Boolean b : selectionArray.values())
		{
			if(b)
			{
				return true;
			}
		}
		return false;
	}

	// ////////////////////
	// CREATION METHODS //
	// ////////////////////

	/**
	 * Create the labels that have been dropped
	 */
	public void createObjects()
	{
		if(this.curTray == null)
		{
			Logs.log("Couldn't create objects because no tray is selected.", 0, this);
			JEXStatics.statusBar.setStatusText("Couldn't create objects because no tray is selected.");
			return;
		}

		// Do the real distribution
		String objectName = this.fileController.getObjectName();
		String objectInfo = this.fileController.getObjectInfo();
		String objectType = this.fileController.getObjectType();
		Type oType = null;
		if(objectType.equals(JEXData.IMAGE.toString()))
		{
			oType = JEXData.IMAGE;
		}
		if(objectType.equals(JEXData.FILE.toString()))
		{
			oType = JEXData.FILE;
		}
		if(objectType.equals(JEXData.MOVIE.toString()))
		{
			oType = JEXData.MOVIE;
		}
		if(objectType.equals(JEXData.ROI.toString()))
		{
			oType = JEXData.ROI;
		}
		if(objectType.equals(JEXData.VALUE.toString()))
		{
			oType = JEXData.VALUE;
		}
		if(objectType.equals(JEXData.LABEL.toString()))
		{
			oType = JEXData.LABEL;
		}

		TreeMap<JEXEntry,TreeMap<DimensionMap,String>> importObject = new TreeMap<JEXEntry,TreeMap<DimensionMap,String>>();
		for (int x = 0; x < this.importController.getNumberColumns(); x++)
		{
			for (int y = 0; y < this.importController.getNumberRows(); y++)
			{
				TreeMap<Integer,JEXEntry> columnEntries = this.curTray.get(y);
				if(columnEntries == null)
				{
					continue;
				}
				JEXEntry e = columnEntries.get(x);
				if(e == null || !this.isValidCell(y, x)) // Switched x and y somewhere, so have to switch here.
				{
					continue;
				}

				Vector<Pair<DimensionMap,String>> files2Drop = this.files.get(new Point(y, x));  // Also have to switch x and y here too

				if(files2Drop != null)
				{
					TreeMap<DimensionMap,String> temp = new TreeMap<DimensionMap,String>();
					for(Pair<DimensionMap,String> file: files2Drop)
					{
						temp.put(file.p1, file.p2);
					}
					importObject.put(e, temp);
				}
			}
		}
		ImportThread importThread = new ImportThread(objectName, oType, objectInfo, importObject, this.fileController.getFilterTable());
		importThread.setTileParameters(this.fileController.getOverlap(), this.fileController.getRows(), this.fileController.getCols());
		JEXStatics.cruncher.runGuiTask(importThread);
	}

	/**
	 * Grab a file from a list of args
	 * 
	 * @param args
	 * @return rich file with dimension info
	 */
	private DimensionMap makeMap(int[] compteur, String[] dimVector)
	{
		DimensionMap map = new DimensionMap();

		// int index = 1;
		for (int i = 0, len = compteur.length; i < len; i++)
		{
			String dimName = dimVector[i];
			if(dimName.equals("Array Row") || dimName.equals("Array Column"))
			{
				continue;
			}

			String dn = dimName;
			Integer dv = compteur[i];
			map.put(dn, "" + dv);

			// index++;
		}

		return map;
	}

	// ////

	// ////
	// //// JEXTabPanel interface
	// ////

	@Override
	public JPanel getMainPanel()
	{
		return this.importController.panel();
	}

	@Override
	public JPanel getLeftPanel()
	{
		return null;
	}

	@Override
	public JPanel getRightPanel()
	{
		return this.fileController;
	}

	@Override
	public void closeTab()
	{
		if(this.fileController != null)
		{
			this.fileController.deInitialize();
		}
		if(this.importController != null)
		{
			this.importController.deInitialize();
		}
		this.importController = null;
		this.fileController = null;
	}

	@Override
	public int getFixedPanelWidth()
	{
		return this.fixedPanelWidth;
	}

	@Override
	public void setFixedPanelWidth(int width)
	{
		this.fixedPanelWidth = width;
	}

	@Override
	public double getResizeWeight()
	{
		return this.resizeWeight;
	}
}
