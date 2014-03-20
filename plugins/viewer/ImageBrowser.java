package plugins.viewer;

import guiObject.ListManager;
import ij.util.Java2;
import image.roi.PointList;
import image.roi.ROIDisplayable;
import image.roi.ROIPlus;
import image.roi.ROIPlusSet;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
import plugins.plugin.PlugIn;
import plugins.plugin.PlugInController;
import signals.SSCenter;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DBObjects.JEXEntry;
import Database.DBObjects.JEXLabel;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.RoiWriter;
import Database.Definition.Type;
import Database.Definition.TypeName;
import Database.SingleUserDatabase.JEXWriter;

public class ImageBrowser implements PlugInController {
	
	public static final String ROI_LIST_CHANGE = "ROI_LIST_CHANGE";
	public static final String SIG_PointsChanged_NULL = "SIG_PointsChanged_NULL";
	
	// Variables
	private Vector<JEXEntry> entries;
	private TypeName imageTN;
	private Vector<String> availableRois; // List
	// of
	// ROI
	// names
	private List<String> filteredRois = null; // List
	// of
	// ROI
	// names
	// that
	// have
	// an
	// appropriate
	// dimTable
	private boolean smashMode = false;
	private ROIPlusSet roi = null;
	private static ChangeSet<ROIPlusSet> saveList = new ChangeSet<ROIPlusSet>();
	private boolean autosave = false;
	private boolean liveScroll = true;
	private boolean scrolling = false;
	// private String tempRoiSelection = null;
	// private boolean deletingRoiFlag = false;
	private String imageFilePath = null;
	private Rectangle imageRect = new Rectangle();
	private static ROIPlus clipBoardROIPlus = null;
	private DecimalFormat format = new DecimalFormat("0.###");
	
	// Gui
	private PlugIn dialog;
	private JSplitPane main;
	private DataBrowser dataBrowser;
	private ListManager roiManager;
	private ImageDisplayController display;
	private LimitAdjuster limitAdjuster;
	private RoiMenu roiMenu;
	private ActionMenu actionMenu;
	private EntryMenu entryMenu;
	private StatusBar statusBar;
	// Organizational JPanels
	private MasterDisplayPane masterDisplayPane;
	private MasterControlPane masterControlPane;
	
	// //////////////////////////////////////
	// ////////// Constructors //////////////
	// //////////////////////////////////////
	
	public ImageBrowser()
	{
		// Logs.log("Is EDT? " +
		// SwingUtilities.isEventDispatchThread(), 0, this);
		this.initializeLimitAdjuster();
		this.initializeMenus();
		this.initializeRoiManager();
		this.initializeDisplay();
		this.initializeDataBrowser();
		this.initializeMain();
		this.initizalizeDialog();
		this.setQuickKeys();
		
		// Trigger any needed refreshing
		this.roiListChange();
		
		this.setVisible(true);
		
		// Make appropriate connections
		SSCenter.defaultCenter().connect(this.limitAdjuster, LimitAdjuster.SIG_limitsChanged_NULL, this, "setLimits", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.AVAILABLEOBJ, this, "roiListChange", (Class[]) null);
		SSCenter.defaultCenter().connect(this.roiMenu, RoiMenu.SIG_AddRoi_intType, this, "addRoi", new Class[] { Integer.class });
		SSCenter.defaultCenter().connect(this.actionMenu, ActionMenu.SIG_DeleteRoi_NULL, this, "deleteRoi", (Class[]) null);
		SSCenter.defaultCenter().connect(this.actionMenu, ActionMenu.SIG_SetMode_intType, this, "setMode", new Class[] { Integer.class });
		SSCenter.defaultCenter().connect(this.actionMenu, ActionMenu.SIG_ToggleSmashMode_NULL, this, "toggleSmashMode", (Class[]) null);
		SSCenter.defaultCenter().connect(this.entryMenu, EntryMenu.SIG_Save_NULL, this, "save", (Class[]) null);
		SSCenter.defaultCenter().connect(this.entryMenu, EntryMenu.SIG_Revert_NULL, this, "revert", (Class[]) null);
		SSCenter.defaultCenter().connect(this.entryMenu, EntryMenu.SIG_Distribute_NULL, this, "distribute", (Class[]) null);
		SSCenter.defaultCenter().connect(this.entryMenu, EntryMenu.SIG_Export_NULL, this, "export", (Class[]) null);
		SSCenter.defaultCenter().connect(this.entryMenu, EntryMenu.SIG_ToggleLiveScroll_NULL, this, "toggleLiveScrolling", (Class[]) null);
		SSCenter.defaultCenter().connect(this.entryMenu, EntryMenu.SIG_ToggleValid_NULL, this, "toggleEntryValid", (Class[]) null);
		SSCenter.defaultCenter().connect(this.roiManager, ListManager.SIG_SelectionChanged_NULL, this, "roiSelectionChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.display, ImageDisplayController.SIG_RoiEdited_NULL, this, "roiEdited", (Class[]) null);
		SSCenter.defaultCenter().connect(this.display, ImageDisplayController.SIG_StatusUpdated_NULL, this, "statusUpdated", (Class[]) null);
		SSCenter.defaultCenter().connect(this.display, ImageDisplayController.SIG_RoiFinished_NULL, this, "roiFinished", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_ImageDimensionMapChanged_NULL, this, "imageDimChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_EntryChanged_NULL, this, "entryChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_ImageSmashedDimTableChanged_NULL, this, "pointsChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_SmashedEntriesChanged_NULL, this, "pointsChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_RoiSmashedDimTableChanged_NULL, this, "pointsChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_RoiDimensionMapChanged_NULL, this, "roiDimChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_EntryScrollFinished_NULL, this, "entryScrollFinished", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_ImageScrollFinished_NULL, this, "imageScrollFinished", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_RoiScrollFinished_NULL, this, "roiScrollFinished", (Class[]) null);
		SSCenter.defaultCenter().connect(this.dataBrowser, DataBrowser.SIG_ScrollStarted_NULL, this, "scrollStarted", (Class[]) null);
		SSCenter.defaultCenter().connect(saveList, ChangeSet.SIG_Updated_NULL, this, "saveListUpdated", (Class[]) null);
		SSCenter.defaultCenter().connect(ImageBrowser.class, SIG_PointsChanged_NULL, this, "pointsChanged", (Class[]) null);
	}
	
	public ImageBrowser(TreeSet<JEXEntry> entries, TypeName tn)
	{
		this();
		this.setDBSelection(entries, tn);
	}
	
	// //////////////////////////////////////
	// //////////Initializers //////////////
	// //////////////////////////////////////
	
	private void initizalizeDialog()
	{
		this.dialog = new PlugIn(this);
		this.dialog.setBounds(100, 100, 800, 600);
		this.dialog.setDefaultCloseOperation(PlugIn.DISPOSE_ON_CLOSE);
		this.dialog.getContentPane().setBackground(DisplayStatics.background);
		this.dialog.getContentPane().setLayout(new BorderLayout());
		this.dialog.getContentPane().add(this.main, BorderLayout.CENTER);
	}
	
	private void initializeLimitAdjuster()
	{
		this.limitAdjuster = new LimitAdjuster();
		this.limitAdjuster.setBounds(0, 4095);
		this.limitAdjuster.setLowerAndUpper(0, 4095);
	}
	
	private void initializeMenus()
	{
		this.roiMenu = new RoiMenu();
		this.actionMenu = new ActionMenu();
		this.entryMenu = new EntryMenu();
	}
	
	private void initializeRoiManager()
	{
		this.roiManager = new ListManager();
	}
	
	private void initializeDisplay()
	{
		this.display = new ImageDisplayController();
		this.display.panel().requestFocusInWindow();
	}
	
	private void initializeDataBrowser()
	{
		this.dataBrowser = new DataBrowser();
		this.dataBrowser.setScrollBars(1, null, null);
	}
	
	private void initializeMain()
	{
		this.main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.main.setResizeWeight(0);
		this.main.setBackground(DisplayStatics.background);
		this.availableRois = new Vector<String>(0);
		this.statusBar = new StatusBar();
	}
	
	// //////////////////////////////////////
	// ////////// Actions //////////////
	// //////////////////////////////////////
	
	public void setVisible(boolean visible)
	{
		this.dialog.setVisible(visible);
	}
	
	public void setDBSelection(TreeSet<JEXEntry> entries, TypeName tn)
	{
		this.entries = new Vector<JEXEntry>(entries);
		this.imageTN = tn;
		
		DimTable imageDimTable = this.getImageDimTable(entries.first());
		DimTable roiDimTable = null;
		
		this.dataBrowser.setScrollBars(this.entries.size(), imageDimTable, roiDimTable);
		// this.imageDim = this.dataBrowser.currentImageDimMap();
		
		this.dialog.invalidate();
		this.dialog.setTitle(this.imageTN.getName());
		this.entryChanged();
		// if(this.masterDisplayPane == null) this.masterDisplayPane = new
		// VerticalBoxLayoutPane(new JPanel[]{this.display.panel(),
		// this.statusBar.panel(), this.limitAdjuster.panel()});
		if(this.masterDisplayPane == null)
		{
			this.masterDisplayPane = new MasterDisplayPane(this.display, this.limitAdjuster, this.statusBar);
		}
		if(this.masterControlPane == null)
		{
			this.masterControlPane = new MasterControlPane(this.roiMenu, this.roiManager, this.actionMenu, this.dataBrowser, this.entryMenu);
		}
		this.main.setLeftComponent(this.masterControlPane.panel());
		this.main.setRightComponent(this.masterDisplayPane.panel());
		
		this.dialog.validate();
		this.dialog.repaint();
	}
	
	public JEXEntry currentEntry()
	{
		int entryIndex = this.dataBrowser.currentEntry();
		if(this.entries == null)
		{
			return null;
		}
		return this.entries.get(entryIndex);
	}
	
	private void setROIPlusSet()
	{
		this.roi = this.getROIPlusSet(this.currentEntry(), this.roiManager.getSelectedItem());
	}
	
	public void setMode(Integer mode)
	{
		if(this.display.mode() == ImageDisplayController.MODE_CREATE && mode != ImageDisplayController.MODE_CREATE)
		{
			this.setROIPlusSet();
			this.display.setMode(mode);
			this.pointsChanged();
		}
		this.display.setMode(mode);
		this.actionMenu.setMode(mode);
	}
	
	public void toggleSmashMode()
	{
		if(this.smashMode)
		{
			this.smashMode = false;
		}
		else
		{
			this.smashMode = true;
		}
		this.signalPointsChanged();
		this.actionMenu.setToggled(this.smashMode);
	}
	
	public void setLimits()
	{
		if(this.display.hasImage())
		{
			this.display.setLimits(this.limitAdjuster.lowerNum(), this.limitAdjuster.upperNum());
		}
	}
	
	public void addRoi(Integer type)
	{
		String roiName = this.getNextName(type);
		for (JEXEntry e : this.entries)
		{
			DimTable dimTable = this.getImageDimTable(e);
			if(dimTable == null)
			{
				continue;
			}
			ROIPlusSet roi = new ROIPlusSet();
			roi.setType(type);
			roi.setName(roiName);
			List<DimensionMap> maps = dimTable.getDimensionMaps();
			for (DimensionMap map : maps)
			{
				roi.setEditablePointsForDimension(new PointList(), map);
			}
			// Can only keep one roi per entry when we need to keep multiple
			saveList.updateObject(e, roi.name(), roi);
		}
		this.setFilteredRois();
		this.roiManager.setSelectedItem(roiName);
		this.roiSelectionChanged();
	}
	
	public void deleteRoi()
	{
		String roiNameToDelete = this.roiManager.getSelectedItem();
		if(roiNameToDelete == null)
		{
			return;
		}
		TreeMap<JEXEntry,Set<JEXData>> dataArray = new TreeMap<JEXEntry,Set<JEXData>>();
		Set<JEXData> tempSet;
		for (JEXEntry e : this.entries)
		{
			tempSet = new HashSet<JEXData>();
			JEXData roiData = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.ROI, roiNameToDelete), e);
			if(roiData != null)
			{
				tempSet.add(roiData);
				dataArray.put(e, tempSet);
			}
		}
		// deletingRoiFlag = true;
		JEXStatics.jexDBManager.removeDataListFromEntry(dataArray);
		
		for (JEXEntry e : this.entries)
		{
			saveList.removeChangeMap(e, roiNameToDelete);
		}
		this.saveListUpdated();
		this.statusBar.setText("Deleted roi.");
	}
	
	public void toggleLiveScrolling()
	{
		if(this.liveScroll)
		{
			this.liveScroll = false;
			this.entryMenu.setLiveScroll(this.liveScroll);
		}
		else
		{
			this.liveScroll = true;
			this.entryMenu.setLiveScroll(this.liveScroll);
		}
	}
	
	public void toggleEntryValid()
	{
		JEXData validLabel = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.LABEL, JEXEntry.VALID), this.currentEntry());
		JEXLabel newValidLabel = new JEXLabel(validLabel.getTypeName().getName(), validLabel.getFirstSingle().get(JEXDataSingle.VALUE), validLabel.getFirstSingle().get(JEXDataSingle.UNIT));
		Boolean isValid = new Boolean(newValidLabel.getLabelValue());
		if(isValid)
		{
			newValidLabel.setLabelValue("false");
			this.entryMenu.setValid(false);
			JEXStatics.jexManager.setEntryValid(this.currentEntry(), false);
		}
		else
		{
			newValidLabel.setLabelValue("true");
			this.entryMenu.setValid(true);
			JEXStatics.jexManager.setEntryValid(this.currentEntry(), true);
		}
	}
	
	public void save()
	{
		int numChanges = saveList.size();
		this.commitSaveList();
		this.statusBar.setText("Saved " + numChanges + " changed rois.");
	}
	
	private String _exportToDirectory(TreeMap<JEXEntry,TreeMap<DimensionMap,String>> exportMap)
	{
		Java2.setSystemLookAndFeel();
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int returnVal = fc.showOpenDialog(this.dialog);
		String directory = null;
		if(returnVal == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				directory = fc.getSelectedFile().getCanonicalPath();
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return null;
			}
			if(directory == null)
			{
				return null;
			}
			for (JEXEntry e : exportMap.keySet())
			{
				File eDir = new File(directory + File.separator + e.getEntryExperiment());
				eDir.mkdir();
				TreeMap<DimensionMap,String> files = exportMap.get(e);
				for (DimensionMap map : files.keySet())
				{
					String source = files.get(map);
					if(source == null)
					{
						continue;
					}
					String mapString = map.toString().replaceAll("\\,", "_");
					mapString = mapString.replace("=", "");
					String fileName = e.getTrayX() + "_" + e.getTrayY() + "_" + FileUtility.getFileNameWithExtension(source);
					try
					{
						JEXWriter.copy(new File(source), new File(eDir.getCanonicalPath() + File.separator + fileName));
					}
					catch (IOException e1)
					{
						e1.printStackTrace();
						return null;
					}
				}
			}
		}
		return directory;
		
	}
	
	public void export()
	{
		// Custom button text
		JCheckBox checkbox = new JCheckBox("Delete Singleton Dimensions?");
		Object[] options = { checkbox, "Database", "Folder", "Cancel" };
		int n = JOptionPane.showOptionDialog(this.dialog, "Export to Database or Folder?", "Export Images", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
		if(n == 1)
		{
			JEXStatics.jexDBManager.saveDataListInEntries(this._createJEXDatasForExport(checkbox.isSelected()), false);
			Logs.log("Exported new Image based on " + this.imageTN.getName() + " into the database.", 0, this);
			this.statusBar.setText("Exported sub-image.");
		}
		else if(n == 2)
		{
			String directory = this._exportToDirectory(this._getPathsForFileCopy());
			
			if(directory != null)
			{
				Logs.log("Exported new Image based on " + this.imageTN.getName() + " to folder " + directory, 0, this);
				this.statusBar.setText("Exported sub-image.");
			}
			else
			{
				Logs.log("Export canceled or error occured during export.", 0, this);
				this.statusBar.setText("Export cancelled or error occured during export.");
			}
		}
	}
	
	private TreeMap<JEXEntry,TreeMap<DimensionMap,String>> _getPathsForFileCopy()
	{
		List<JEXEntry> smashedEntries = this.smashedEntries();
		DimTable smashedTable;
		DimTableAdjuster temp = new DimTableAdjuster();
		this.dataBrowser.storeDimState();
		List<Object[]> state = this.dataBrowser.getStoredDimState();
		JEXData imageData;
		TreeMap<JEXEntry,TreeMap<DimensionMap,String>> exportMap = new TreeMap<JEXEntry,TreeMap<DimensionMap,String>>();
		for (JEXEntry e : smashedEntries)
		{
			temp.setDimTable(this.getImageDimTable(e));
			temp.setOverallState(state);
			smashedTable = temp.smashedDimTable();
			
			imageData = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.imageTN, e);
			TreeMap<DimensionMap,String> fileTable = ImageReader.readObjectToImagePathTable(imageData);
			TreeMap<DimensionMap,String> newFileTable = new TreeMap<DimensionMap,String>();
			for (DimensionMap map : smashedTable.getDimensionMaps())
			{
				newFileTable.put(map, fileTable.get(map));
			}
			exportMap.put(e, newFileTable);
		}
		return exportMap;
	}
	
	private TreeMap<JEXEntry,Set<JEXData>> _createJEXDatasForExport(boolean deleteSingletonDims)
	{
		TypeName newTN = new TypeName(this.imageTN.getType(), this.imageTN.getName() + " Exported");
		String newName = JEXStatics.jexManager.getNextAvailableTypeNameInEntries(newTN, this.entries).getName();
		List<JEXEntry> smashedEntries = this.smashedEntries();
		DimTable smashedTable;
		DimTableAdjuster temp = new DimTableAdjuster();
		this.dataBrowser.storeDimState();
		List<Object[]> state = this.dataBrowser.getStoredDimState();
		JEXData imageData;
		TreeMap<JEXEntry,Set<JEXData>> exportMap = new TreeMap<JEXEntry,Set<JEXData>>();
		HashSet<JEXData> tempSet;
		for (JEXEntry e : smashedEntries)
		{
			temp.setDimTable(this.getImageDimTable(e));
			temp.setOverallState(state);
			smashedTable = temp.smashedDimTable();
			
			TreeSet<String> singletons = new TreeSet<String>();
			if(deleteSingletonDims)
			{
				for (Dim d : smashedTable)
				{
					if(d.size() == 1)
					{
						singletons.add(d.dimName);
					}
				}
			}
			
			imageData = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.imageTN, e);
			TreeMap<DimensionMap,String> fileTable = ImageReader.readObjectToImagePathTable(imageData);
			TreeMap<DimensionMap,String> newFileTable = new TreeMap<DimensionMap,String>();
			for (DimensionMap map : smashedTable.getDimensionMaps())
			{
				DimensionMap toSave = map.copy();
				if(deleteSingletonDims)
				{
					for (String d : singletons)
					{
						toSave.remove(d);
					}
				}
				newFileTable.put(toSave, fileTable.get(map));
			}
			
			if(deleteSingletonDims)
			{
				for (String d : singletons)
				{
					smashedTable.removeDimWithName(d);
				}
			}
			
			JEXData newImageData = ImageWriter.makeImageStackFromPaths(newName, newFileTable);
			newImageData.setDimTable(smashedTable);
			tempSet = new HashSet<JEXData>();
			tempSet.add(newImageData);
			exportMap.put(e, tempSet);
		}
		return exportMap;
	}
	
	/**
	 * Removes only those JEXData which are found to match the OLD item in the changeMaps Adds all NEW items in the changeMaps. Clears the changeList when done.
	 */
	public void commitSaveList()
	{
		String tempRoiSelection = this.roiManager.getSelectedItem();
		TreeMap<JEXEntry,Set<JEXData>> removeMap = new TreeMap<JEXEntry,Set<JEXData>>();
		TreeMap<JEXEntry,Set<JEXData>> addMap = new TreeMap<JEXEntry,Set<JEXData>>();
		if(saveList.size() == 0)
		{
			Logs.log("No changes to save. Database is up-to-date.", 0, this);
		}
		else
		{
			Set<JEXData> tempSet = null;
			for (JEXEntry e : this.entries)
			{
				for (ChangeMap<ROIPlusSet> changeMap : saveList.getChangeMaps(e))
				{
					if(changeMap == null)
					{
						continue;
					}
					ROIPlusSet roiToReplace = changeMap.oldest();
					ROIPlusSet roiToSave = changeMap.newest();
					JEXData oldRoiData, roiData;
					if(roiToSave != null && roiToSave.name() != null)
					{
						oldRoiData = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.ROI, roiToReplace.name()), e);
						DimTable dimTable = null;
						if(oldRoiData != null)
						{
							dimTable = oldRoiData.getDimTable();
						}
						else
						// it was created while looking at image and matches
						// image dims
						{
							dimTable = this.getImageDimTable(e);
						}
						
						// Copy over only the stuff that matches the dimTable
						// (i.e. nothing outside original dimensions)
						HashMap<DimensionMap,ROIPlus> temp = new HashMap<DimensionMap,ROIPlus>();
						for (DimensionMap map : dimTable.getDimensionMaps())
						{
							ROIPlus roi = roiToSave.getRoiMap().get(map);
							if(roi != null)
							{
								temp.put(map, roi);
							}
						}
						
						// Save the info
						roiData = RoiWriter.makeRoiObject(roiToSave.name(), temp);
						roiData.setDimTable(dimTable);
						tempSet = addMap.get(e);
						if(tempSet == null)
						{
							tempSet = new HashSet<JEXData>();
						}
						tempSet.add(roiData);
						addMap.put(e, tempSet);
						if(roiToReplace != null && roiToReplace.name() != null)
						{
							roiData = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.ROI, roiToReplace.name()), e);
							if(roiData != null)
							{
								tempSet = removeMap.get(e);
								if(tempSet == null)
								{
									tempSet = new HashSet<JEXData>();
								}
								tempSet.add(roiData);
								removeMap.put(e, tempSet);
							}
						}
					}
				}
			}
			
			JEXStatics.jexDBManager.removeDataListFromEntry(removeMap);
			// JEXStatics.jexManager.addDataListToEntries(addMap, true);
			JEXStatics.jexDBManager.saveDataListInEntries(addMap, true);
			this.roiManager.setSelectedItem(tempRoiSelection);
			this.roiSelectionChanged();
			Logs.log("Saved " + saveList.size() + " changed Rois.", 0, this);
			saveList.clear();
		}
		this.setMode(ImageDisplayController.MODE_VIEW);
	}
	
	public void copyROIPlus()
	{
		// PointList pl = this.display.getEditablePoints();
		DimensionMap map = this.dataBrowser.currentRoiDimMap();
		ROIPlus roiToCopy = this.roi.getRoiMap().get(map);
		// if(pl != null && pl.size() != 0)
		if(roiToCopy != null && roiToCopy.getPointList().size() != 0)
		{
			clipBoardROIPlus = roiToCopy.copy();// new
			// ROIPlus(this.display.getEditablePoints(),
			// this.display.getRoiType());
			this.statusBar.setText("Copied roi points.");
		}
		else
		{
			Logs.log("Couldn't copy, empty or null roi", 0, this);
			this.statusBar.setText("No roi active to copy.");
		}
	}
	
	public void pasteROIPlus()
	{
		if(clipBoardROIPlus == null || this.display.getRoiType() == ROIPlus.ROI_UNDEFINED || this.roi.type() != clipBoardROIPlus.type)
		{
			Logs.log("Can't paste roi points. Clipboard empty, no roi selected to paste into, or type doesn't match", 0, this);
			this.statusBar.setText("Can't paste roi points. Clipboard empty, no roi selected to paste into, or type doesn't match.");
		}
		else
		{
			this.roi.setRoiForDimension(clipBoardROIPlus.copy(), this.dataBrowser.currentRoiDimMap());
			saveList.updateObject(this.currentEntry(), this.roi.name(), this.roi);
			this.signalPointsChanged();
			this.statusBar.setText("Pasted roi points.");
		}
	}
	
	/**
	 * Called by roiEdited which is called upon signal from display controller
	 */
	private void putChangesToPointsInChangeMap()
	{
		if(this.roi != null && this.roi.type() != ROIPlus.ROI_UNDEFINED)
		{
			this.roi.setEditablePointsForDimension(this.display.getEditablePoints(), this.dataBrowser.currentRoiDimMap());
			saveList.updateObject(this.currentEntry(), this.roi.name(), this.roi);
		}
		else
		{
			Logs.log("Couldn't save changes to points in " + this.roi.name(), 0, this);
		}
	}
	
	public void changedRoiName(String oldName, String newName)
	{
		ROIPlusSet temp;
		for (JEXEntry e : this.entries)
		{
			temp = this.getROIPlusSet(e, oldName);
			saveList.updateObject(e, oldName, temp); // put the roi in the
			// changeMap if not already
			// there
			temp.setName(newName); // change the name of the object
			saveList.updateObjectWithChangedName(e, temp, oldName, newName); // cause
		}
	}
	
	public void revert()
	{
		saveList.clear();
		this.statusBar.setText("Reverted to saved rois.");
	}
	
	private boolean distributeROIPlusIn(ROIDisplayable rois, DimTable smashedDimTable, ROIPlus roi)
	{
		if(clipBoardROIPlus == null || rois.type() == ROIPlus.ROI_UNDEFINED || roi.type == ROIPlus.ROI_UNDEFINED || rois.type() != clipBoardROIPlus.type)
		{
			Logs.log("Can't distribute roi points. Clipboard empty, no roi with selected name in entry, no roi selected to distribute into, or type doesn't match", 0, this);
			return false;
		}
		else
		{
			List<DimensionMap> maps = smashedDimTable.getDimensionMaps();
			for (DimensionMap map : maps)
			{
				rois.setRoiForDimension(roi, map);
			}
			return true;
		}
	}
	
	private void distributeROIPlus()
	{
		if(clipBoardROIPlus == null)
		{
			return;
		}
		if(this.roiManager.getSelectedItem() == null)
		{
			return;
		}
		List<JEXEntry> smashedE = this.smashedEntries();
		this.dataBrowser.storeDimState();
		List<Object[]> state = this.dataBrowser.getStoredDimState();
		DimTableAdjuster temp = new DimTableAdjuster();
		for (JEXEntry e : smashedE)
		{
			DimTable table = this.getImageDimTable(e);
			temp.setDimTable(table);
			temp.setOverallState(state);
			table = temp.smashedDimTable();
			ROIPlusSet roi = this.getROIPlusSet(e, this.roiManager.getSelectedItem());
			if(this.distributeROIPlusIn(roi, table, clipBoardROIPlus))
			{
				saveList.updateObject(e, roi.name(), roi);
			}
		}
		this.roi = this.getROIPlusSet(this.currentEntry(), this.roiManager.getSelectedItem());
	}
	
	private List<JEXEntry> smashedEntries()
	{
		List<JEXEntry> ret = new Vector<JEXEntry>();
		Dim smashedEntries = this.dataBrowser.smashedEntries();
		for (int i = 0; i < smashedEntries.size(); i++)
		{
			ret.add(this.entries.get((new Integer(smashedEntries.valueAt(i))) - 1));
		}
		return ret;
	}
	
	public void distribute()
	{
		this.distributeROIPlus();
		this.signalPointsChanged();
		this.statusBar.setText("Distributed roi points.");
	}
	
	private DimTable getImageDimTable(JEXEntry e)
	{
		DimTable ret = null;
		JEXData imageData = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.imageTN, e);
		if(imageData != null)
		{
			ret = imageData.getDimTable();
		}
		else
		{
			ret = null;
		}
		return ret;
	}
	
	private DimTable getRoiDimTable(JEXEntry e)
	{
		DimTable ret = null;
		String roiName = this.roiManager.getSelectedItem();
		if(roiName == null)
		{
			return ret;
		}
		// saveList should have the most up-to-date roi information
		ChangeMap<ROIPlusSet> updatedRoiInfo = saveList.getChangeMap(e, roiName);
		JEXData roiData = null;
		if(updatedRoiInfo != null)
		{
			roiData = RoiWriter.makeRoiObject(roiName, updatedRoiInfo.newest().getRoiMap());
			if(roiData != null)
			{
				ret = roiData.getDimTable();
			}
		}
		else
		// else check the database for the roi information
		{
			TypeName temp = new TypeName(JEXData.ROI, roiName);
			roiData = JEXStatics.jexManager.getDataOfTypeNameInEntry(temp, e);
			if(roiData == null)
			{
				ret = null;
			}
			else
			{
				ret = roiData.getDimTable();
			}
		}
		return ret;
	}
	
	private ROIPlusSet getROIPlusSet(JEXEntry e, String name)
	{
		// Get the selected name in the RoiManager and associated JEXData from
		// the saveList if there or from the database if not
		ChangeMap<ROIPlusSet> ret = saveList.getChangeMap(e, name);
		
		if(ret != null)
		{
			return ret.newest();
		}
		
		return this.getROIPlusSetFromDB(e, name);
	}
	
	private ROIPlusSet getROIPlusSetFromDB(JEXEntry e, String name)
	{
		ROIPlusSet ret;
		// Else go get it from the database
		if(name == null)
		{
			ret = new ROIPlusSet();
			ret.setName(null);
			return ret;
		}
		JEXData roiData = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.ROI, name), e);
		
		// Get a set of ROIPlus's in the form of a ROIDisplayable from the
		// JEXData if possible
		if(roiData == null)
		{
			ret = new ROIPlusSet();
			ret.setName(null);
			return ret;
		}
		
		TreeMap<DimensionMap,ROIPlus> potentialRois = new TreeMap<DimensionMap,ROIPlus>();
		potentialRois = RoiReader.readObjectToRoiMap(roiData);
		ret = new ROIPlusSet(potentialRois);
		ret.setName(name);
		return ret;
	}
	
	public void saveListUpdated()
	{
		this.setFilteredRois();
		this.setROIPlusSet();
		this.pointsChanged();
	}
	
	public void setFilteredRois()
	{
		// Gather roi names from database
		JEXEntry currentEntry = this.currentEntry();
		if(currentEntry == null)
		{
			return;
		}
		DimTable imageDimTable = this.getImageDimTable(currentEntry);
		if(this.filteredRois == null)
		{
			this.filteredRois = new Vector<String>();
		}
		else
		{
			this.filteredRois.clear();
		}
		JEXData roiData;
		for (String roiName : this.availableRois)
		{
			roiData = JEXStatics.jexManager.getDataOfTypeNameInEntry(new TypeName(JEXData.ROI, roiName), this.currentEntry());
			if(imageDimTable != null && roiData != null)// &&
			// DimTable.intersect(roiData.getDimTable(),
			// imageDimTable).matches(imageDimTable))
			{
				this.filteredRois.add(roiName);
			}
		}
		
		// gather roi names from saveList and overwrite any oldNames with
		// appropriate new names
		boolean nameChanged, existsInFilteredRois;
		String newName, oldName;
		List<ChangeMap<ROIPlusSet>> changes = saveList.getChangeMaps(this.currentEntry());
		for (ChangeMap<ROIPlusSet> changeMap : changes)
		{
			newName = changeMap.newest().name();
			oldName = changeMap.oldest().name();
			nameChanged = !(oldName.equals(newName));
			existsInFilteredRois = this.filteredRois.contains(oldName);
			
			// if old name exist in filteredRois and name changed, change name
			// if old name does not exist in filtered rois, add new name;
			if(this.filteredRois.contains(oldName) && nameChanged)
			{
				this.filteredRois.remove(oldName);
				this.filteredRois.add(newName);
			}
			else if(!existsInFilteredRois)
			{
				roiData = RoiWriter.makeRoiObject(newName, changeMap.newest().getRoiMap());
				if(imageDimTable != null && roiData != null && DimTable.intersect(roiData.getDimTable(), imageDimTable).matches(imageDimTable))
				{
					this.filteredRois.add(newName);
				}
			}
			// else don't add to list or change any names
		}
		
		String currentRoiName = this.roiManager.getSelectedItem();
		this.roiManager.setItems(this.filteredRois);
		this.roiManager.setSelectedItem(currentRoiName); // selects nothing if
		// can't be found
	}
	
	private String getNextName(int type)
	{
		String ret = "";
		if(type == ROIPlus.ROI_ELLIPSE)
		{
			ret = ret + "Ellipse ";
		}
		else if(type == ROIPlus.ROI_LINE)
		{
			ret = ret + "Line ";
		}
		else if(type == ROIPlus.ROI_POINT)
		{
			ret = ret + "Points ";
		}
		else if(type == ROIPlus.ROI_POLYGON)
		{
			ret = ret + "Polygon ";
		}
		else if(type == ROIPlus.ROI_POLYLINE)
		{
			ret = ret + "Polyline ";
		}
		else
		// (type == ROIPlus.ROI_RECT)
		{
			ret = ret + "Rectangle ";
		}
		int i = 1;
		while (this.availableRois.contains(ret + i) || saveList.containsName(ret + i))
		{
			i++;
		}
		ret = ret + i;
		return ret;
	}
	
	// //////////////////////////////////////
	// ////////// Reactions //////////////
	// //////////////////////////////////////
	
	public void scrollStarted()
	{
		this.scrolling = true;
	}
	
	public void entryChanged()
	{
		if(this.scrolling && !this.liveScroll)
		{
			return;
		}
		JEXEntry entry = this.currentEntry();
		DimTable imageDimTable = this.getImageDimTable(entry);
		DimTable roiDimTable = this.getRoiDimTable(entry);
		this.dataBrowser.storeDimState();
		this.dataBrowser.setImageDimTable(imageDimTable);
		this.dataBrowser.setRoiDimTable(roiDimTable);
		this.dataBrowser.recallDimState(); // set what we can
		this.dataBrowser.repaint();
		this.entryMenu.setValid(JEXStatics.jexManager.getEntryValidity(entry));
		this.setFilteredRois();
		this.imageChanged();
		this.setROIPlusSet();
		this.signalPointsChanged();
	}
	
	public void entryScrollFinished()
	{
		this.scrolling = false;
		this.entryChanged();
	}
	
	public void imageDimChanged()
	{
		if(this.scrolling && !this.liveScroll)
		{
			return;
		}
		this.imageChanged();
		this.signalPointsChanged();
	}
	
	public void imageScrollFinished()
	{
		this.scrolling = false;
		this.imageDimChanged();
	}
	
	public void roiDimChanged()
	{
		if(this.scrolling && !this.liveScroll)
		{
			return;
		}
		this.signalPointsChanged();
	}
	
	public void roiScrollFinished()
	{
		this.scrolling = false;
		this.roiDimChanged();
	}
	
	public void roiEdited()
	{
		this.putChangesToPointsInChangeMap();
		// this.signalPointsChanged();
	}
	
	public void roiFinished()
	{
		this.statusBar.setText("ROI closed/finished.");
	}
	
	public void pointsChanged()
	{
		List<PointList> smashedPoints = new Vector<PointList>();
		if(this.smashMode)
		{
			List<DimensionMap> maps = this.dataBrowser.smashedRoiDimTable().getDimensionMaps();
			HashSet<PointList> alreadyAdded = new HashSet<PointList>();
			for (JEXEntry e : this.smashedEntries())
			{
				ROIPlusSet roiForSmashing = this.getROIPlusSet(e, this.roi.name());
				for (DimensionMap map : maps)
				{
					PointList editable = roiForSmashing.getEditablePointsForDimension(map);
					
					if(editable != null && !alreadyAdded.contains(editable))
					{
						smashedPoints.add(editable);
						alreadyAdded.add(editable);
					}
					List<PointList> patternedRois = roiForSmashing.getPatternedRoisForDimension(map);
					for (PointList patternedRoi : patternedRois)
					{
						if(!alreadyAdded.contains(patternedRoi))
						{
							smashedPoints.add(patternedRoi);
							alreadyAdded.add(patternedRoi);
						}
					}
				}
			}
		}
		
		PointList editablePoints = this.roi.getEditablePointsForDimension(this.dataBrowser.currentRoiDimMap());
		this.display.setRoiDisplay(this.roi.type(), editablePoints, smashedPoints); // causes
		// repaint
		// of
		// display
		if(editablePoints == null || editablePoints.size() == 0)
		{
			this.setMode(ImageDisplayController.MODE_CREATE);
		}
		else if(this.display.mode() == ImageDisplayController.MODE_CREATE)
		{
			this.setMode(ImageDisplayController.MODE_MOVE);
		}
	}
	
	public void signalPointsChanged()
	{
		SSCenter.defaultCenter().emit(ImageBrowser.class, SIG_PointsChanged_NULL, (Object[]) null);
	}
	
	public void roiSelectionChanged() // called upon change in the selectedRoi
	{
		this.dataBrowser.setRoiDimTable(this.getRoiDimTable(this.currentEntry()));
		this.dataBrowser.repaint();
		this.setROIPlusSet();
		this.signalPointsChanged();
		// this.entryChanged();
	}
	
	public void imageChanged() // Called each time the entry, DimensionMap;
	{
		// Load the appropriate image according to the DimensionMap and entry
		JEXData imageData = JEXStatics.jexManager.getDataOfTypeNameInEntry(this.imageTN, this.currentEntry());
		if(imageData == null)
		{
			this.display.setImage(null);
			this.statusUpdated();
			return;
		}
		
		JEXDataSingle ds = imageData.getData(this.dataBrowser.currentImageDimMap());
		if(ds != null)
		{
			this.imageFilePath = ImageReader.readImagePath(ds);
			// this.imageFilePath = ds.get(JEXDataSingle.FOLDERNAME) +
			// File.separator + ds.get(JEXDataSingle.FILENAME);
			
			this.display.setImage(this.imageFilePath);
			this.imageRect = this.display.imageRect();
			this.statusUpdated();
			
			// Set the limits of the image so that you can see something
			double displayMin = this.display.minDisplayIntensity();
			double displayMax = this.display.maxDisplayIntensity();
			double imageMax = this.display.maxImageIntensity();
			double imageMin = this.display.minImageIntensity();
			int bitDepth = this.display.bitDepth();
			int n = 0;
			double limMin = 0, limMax = 255;
			if(imageMin < 0 || imageMax > 65535) // then 32 bit
			{
				limMin = imageMin;
				limMax = imageMax;
				n = 10000;
			}
			else if(bitDepth == 32 && imageMax <= 5)
			{
				limMin = 0;
				limMax = 5;
				n = 10000;
			}
			else if(imageMax <= 255)
			{
				// Don't do anything, already set for 8 bit image
			}
			else if(imageMax <= 4095)
			{
				limMin = 0;
				limMax = 4095;
			}
			else
			// <= 35535
			{
				limMin = 0;
				limMax = 65535;
			}
			
			if(n == 0)
			{
				this.limitAdjuster.setBounds((int) limMin, (int) limMax);
			}
			else
			{
				this.limitAdjuster.setBounds(limMin, limMax, n);
			}
			
			this.limitAdjuster.setLowerAndUpper(displayMin, displayMax);
			this.display.setLimits(this.limitAdjuster.lowerNum(), this.limitAdjuster.upperNum());
		}
		else
		{
			this.display.setImage(null);
			this.statusUpdated();
			return;
		}
		
	}
	
	public void statusUpdated()
	{
		if(!this.display.hasImage())
		{
			this.statusBar.setText("");
		}
		else
		{
			Point p = this.display.currentImageLocation();
			Rectangle roi = this.display.roiRect();
			String roiRect = "";
			if(roi != null)
			{
				roiRect = "Roi:" + roi.x + "," + roi.y + "," + roi.width + "," + roi.height;
			}
			this.statusBar.setText(FileUtility.getFileNameWithExtension(this.imageFilePath) + "   [" + this.imageRect.width + "X" + this.imageRect.height + "]  " + roiRect + "  loc:" + (p.x + 1) + "," + (p.y + 1) + "  Int:" + this.format.format(this.display.getPixelIntensity()));
			// this.statusBar.setText(this.imageFilePath + "  [" +
			// this.imageRect.width + "X" + this.imageRect.height + "]  " +
			// roiRect + "  loc:" + (p.x + 1) + "," + (p.y + 1) + "  Int:" +
			// this.display.getPixelIntensity());
		}
	}
	
	/**
	 * Reacts to JEXManager.AVAILABLEOBJ signals
	 */
	public void roiListChange()
	{
		// Get the list of ROIs
		TreeMap<Type,TreeMap<String,TreeMap<String,Set<JEXEntry>>>> TNVI = JEXStatics.jexManager.getFilteredTNVI();
		TreeMap<String,TreeMap<String,Set<JEXEntry>>> roiNVI = TNVI.get(JEXData.ROI);
		
		this.availableRois = new Vector<String>(0);
		if(roiNVI != null)
		{
			for (String roiName : roiNVI.keySet())
			{
				this.availableRois.add(roiName);
			}
		}
		this.setFilteredRois(); // sets rois in roiManager as well
		
		// // Logs.log("Send signal of roi list change", 1,
		// this);
		// if(this.tempRoiSelection != null || this.deletingRoiFlag)
		// {
		// this.roiManager.setSelectedItem(this.tempRoiSelection);
		// this.roiSelectionChanged();
		// }
		this.roiSelectionChanged();
		// this.tempRoiSelection = null;
		// this.deletingRoiFlag = false;
	}
	
	// //////////////////////////////////////
	// ////////// PlugIn Stuff //////////////
	// //////////////////////////////////////
	
	private void setQuickKeys()
	{
		// Always add the actions to the action map of the menuPane because that
		// is the one that never changes
		// If it does ever change, call setQuickKeys again to reconnect quick
		// keys
		KeyStroke stroke;
		InputMap inputs = this.roiMenu.panel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actions = this.roiMenu.panel().getActionMap();
		
		// Nudge Left action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false);
		inputs.put(stroke, "nudgeLeft");
		actions.put("nudgeLeft", new ActionNudgeLeft(this));
		
		// Nudge Up action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false);
		inputs.put(stroke, "nudgeUp");
		actions.put("nudgeUp", new ActionNudgeUp(this));
		
		// Nudge Right action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false);
		inputs.put(stroke, "nudgeRight");
		actions.put("nudgeRight", new ActionNudgeRight(this));
		
		// Nudge Down action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false);
		inputs.put(stroke, "nudgeDown");
		actions.put("nudgeDown", new ActionNudgeDown(this));
		
		// Finish Roi action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);
		inputs.put(stroke, "finishRoi");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false);
		inputs.put(stroke, "finishRoi");
		actions.put("finishRoi", new ActionFinishRoi(this));
		
		// Delete Roi action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, false);
		inputs.put(stroke, "deleteRoi");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, false);
		inputs.put(stroke, "deleteRoi");
		actions.put("deleteRoi", new ActionDeleteRoi(this));
		
		// Zoom In action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.META_DOWN_MASK, false);
		inputs.put(stroke, "zoomIn");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK, false);
		inputs.put(stroke, "zoomIn");
		actions.put("zoomIn", new ActionZoomIn(this));
		
		// Zoom Out action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.META_DOWN_MASK, false);
		inputs.put(stroke, "zoomOut");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK, false);
		inputs.put(stroke, "zoomOut");
		actions.put("zoomOut", new ActionZoomOut(this));
		
		// Save action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.META_DOWN_MASK, false);
		inputs.put(stroke, "save");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, false);
		inputs.put(stroke, "save");
		actions.put("save", new ActionSave(this));
		
		// Close action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.META_DOWN_MASK, false);
		inputs.put(stroke, "close");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK, false);
		inputs.put(stroke, "close");
		actions.put("close", new ActionClose(this));
		
		// Revert action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.META_DOWN_MASK, false);
		inputs.put(stroke, "revert");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK, false);
		inputs.put(stroke, "revert");
		actions.put("revert", new ActionRevert(this));
		
		// Copy action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK, false);
		inputs.put(stroke, "copy");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK, false);
		inputs.put(stroke, "copy");
		actions.put("copy", new ActionCopy(this));
		
		// Paste action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK, false);
		inputs.put(stroke, "paste");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK, false);
		inputs.put(stroke, "paste");
		actions.put("paste", new ActionPaste(this));
		
		// Distribute Paste
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK + KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "distribute");
		
		// View Mode hotkey
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, 0, false);
		inputs.put(stroke, "view mode");
		actions.put("view mode", new ActionView(this));
		
		// Move Mode hotkey
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_X, 0, false);
		inputs.put(stroke, "move mode");
		actions.put("move mode", new ActionMove(this));
		
		// Create Mode hotkey
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, 0, false);
		inputs.put(stroke, "create mode");
		actions.put("create mode", new ActionCreate(this));
		
		// Smash Mode hotkey
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, 0, false);
		inputs.put(stroke, "smash mode");
		actions.put("smash mode", new ActionSmash(this));
		
		// Indexing Actions
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, 0, false);
		inputs.put(stroke, "index entry forward");
		actions.put("index entry forward", new ActionIndexEntry(this, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index entry backward");
		actions.put("index entry backward", new ActionIndexEntry(this, -1));
		
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_1, 0, false);
		inputs.put(stroke, "index dim 0 forward");
		actions.put("index dim 0 forward", new ActionIndexDim(this, 0, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_1, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index dim 0 backward");
		actions.put("index dim 0 backward", new ActionIndexDim(this, 0, -1));
		
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_2, 0, false);
		inputs.put(stroke, "index dim 1 forward");
		actions.put("index dim 1 forward", new ActionIndexDim(this, 1, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_2, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index dim 1 backward");
		actions.put("index dim 1 backward", new ActionIndexDim(this, 1, -1));
		
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_3, 0, false);
		inputs.put(stroke, "index dim 2 forward");
		actions.put("index dim 2 forward", new ActionIndexDim(this, 2, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_3, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index dim 2 backward");
		actions.put("index dim 2 backward", new ActionIndexDim(this, 2, -1));
		
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_4, 0, false);
		inputs.put(stroke, "index dim 3 forward");
		actions.put("index dim 3 forward", new ActionIndexDim(this, 3, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_4, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index dim 3 backward");
		actions.put("index dim 3 backward", new ActionIndexDim(this, 3, -1));
		
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_5, 0, false);
		inputs.put(stroke, "index dim 4 forward");
		actions.put("index dim 4 forward", new ActionIndexDim(this, 4, 1));
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_5, KeyEvent.SHIFT_DOWN_MASK, false);
		inputs.put(stroke, "index dim 4 backward");
		actions.put("index dim 4 backward", new ActionIndexDim(this, 4, -1));
		
	}
	
	@SuppressWarnings("serial")
	public class ActionNudgeLeft extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionNudgeLeft(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.display.nudgeLeft();
			Logs.log("Nudge Left Signal", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionNudgeUp extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionNudgeUp(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.display.nudgeUp();
			Logs.log("Nudge Up Signal", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionNudgeRight extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionNudgeRight(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.display.nudgeRight();
			Logs.log("Nudge Right Signal", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionNudgeDown extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionNudgeDown(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.display.nudgeDown();
			Logs.log("Nudge Down Signal", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionFinishRoi extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionFinishRoi(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.display.finishRoi();
			Logs.log("Finish Roi Signal", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionDeleteRoi extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionDeleteRoi(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.display.deleteRoi();
			Logs.log("Delete Roi Signal", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionZoomIn extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionZoomIn(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			if(e.getActionCommand().equals("+"))
			{
				this.im.display.zoomIn();
				Logs.log("Zoom In Signal", 0, this);
			}
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionZoomOut extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionZoomOut(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.display.zoomOut();
			Logs.log("Zoom Out Signal", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionSave extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionSave(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.save();
			Logs.log("Saving Rois of Image Browser", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionClose extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionClose(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.dialog.dispose();
			Logs.log("Closing Image Browser", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionRevert extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionRevert(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.revert();
			this.im.entryChanged();
			Logs.log("Reverting to Rois Stored In Database", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionCopy extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionCopy(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Logs.log("Copy points", 0, this);
			this.im.copyROIPlus();
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionPaste extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionPaste(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Logs.log("Paste points", 0, this);
			this.im.pasteROIPlus();
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionDistribute extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionDistribute(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			Logs.log("Paste points", 0, this);
			this.im.distribute();
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionView extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionView(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.setMode(ImageDisplayController.MODE_VIEW);
			this.im.statusBar.setText("Switched to View Mode");
			Logs.log("Setting to View Mode", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionMove extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionMove(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.setMode(ImageDisplayController.MODE_MOVE);
			this.im.statusBar.setText("Switched to Move Mode");
			Logs.log("Setting to Move Mode", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionCreate extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionCreate(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.setMode(ImageDisplayController.MODE_CREATE);
			this.im.statusBar.setText("Switched to Create Mode");
			Logs.log("Setting to Create Mode", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionSmash extends AbstractAction {
		
		private ImageBrowser im;
		
		public ActionSmash(ImageBrowser im)
		{
			this.im = im;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.im.toggleSmashMode();
			this.im.statusBar.setText("Toggled Smash Mode");
			Logs.log("Toggle Smash Mode", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionIndexEntry extends AbstractAction {
		
		private ImageBrowser im;
		private int direction;
		
		public ActionIndexEntry(ImageBrowser im, int direction)
		{
			this.im = im;
			this.direction = direction;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			int amount = 0;
			if(this.direction > 0)
			{
				amount = 1;
			}
			else if(this.direction < 0)
			{
				amount = -1;
			}
			this.im.dataBrowser.setEntry(this.im.dataBrowser.currentEntry() + amount);
			this.im.entryChanged();
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionIndexDim extends AbstractAction {
		
		private ImageBrowser im;
		private int direction;
		private int dim;
		
		public ActionIndexDim(ImageBrowser im, int dim, int direction)
		{
			this.im = im;
			this.direction = direction;
			this.dim = dim;
		}
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			int amount = 0;
			if(this.direction > 0)
			{
				amount = 1;
			}
			else if(this.direction < 0)
			{
				amount = -1;
			}
			this.im.dataBrowser.indexImageDim(this.dim, amount);
			this.im.imageDimChanged();
		}
	}
	
	/**
	 * Called upon closing of the window
	 */
	@Override
	public void finalizePlugIn()
	{
		if(this.autosave)
		{
			this.save();
		}
		else
		{
			this.revert();
		}
	}
	
	@Override
	public PlugIn plugIn()
	{
		return this.dialog;
	}
}
