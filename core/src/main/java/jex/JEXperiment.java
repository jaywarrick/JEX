package jex;

import Database.SingleUserDatabase.JEXDBIO;
import cruncher.Cruncher;
import guiObject.DialogGlassPane;
import guiObject.SignalMenuButton;
import icons.IconRepository;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import jex.jexTabPanel.JEXTabPanelController;
import jex.jexTabPanel.creationPanel.JEXCreationPanelController;
import jex.jexTabPanel.jexDistributionPanel.JEXDistributionPanelController;
import jex.jexTabPanel.jexFunctionPanel.JEXFunctionPanelController;
import jex.jexTabPanel.jexLabelPanel.JEXLabelPanelController;
import jex.jexTabPanel.jexPluginPanel.JEXPluginPanelController;
import jex.jexTabPanel.jexStatisticsPanel.JEXStatisticsPanelController;
import jex.jexTabPanel.jexViewPanel.JEXViewPanelController;
import jex.objectAndEntryPanels.EntryAndObjectPanel;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import jex.statics.KeyStatics;
import jex.statics.PrefsUtility;
import logs.Logs;
import plugins.labelManager.DatabaseLabelManager;
import preferences.XPreferencePanelController;
import preferences.XPreferences;
import rtools.R;
import signals.SSCenter;
import updates.Updater;

public class JEXperiment extends JFrame implements ActionListener, WindowListener, WindowFocusListener, KeyEventDispatcher {
	
	private static final long serialVersionUID = 1L;
	
	// Peripheral GUI elements
	protected AboutBox aboutBox;
	protected JDialog prefsDialog;
	private JEXLogOnPanel logOnFrame;
	private JEXDatabaseChooser dbChooser;
	
	public JEXperiment()
	{
		// Start the statics class
		JEXStatics.main = this;
		
		// Load the manager
		JEXManager jexManager = new JEXManager();
		JEXStatics.jexManager = jexManager;
		
		// Load the database manager
		JEXDatabaseManager jexDBManager = new JEXDatabaseManager();
		JEXStatics.jexDBManager = jexDBManager;
		
		// Load the icon repository
		IconRepository iconRepository = new IconRepository();
		JEXStatics.iconRepository = iconRepository;
		
		// Load the status bar
		StatusBar statusBar = new StatusBar();
		JEXStatics.statusBar = statusBar;
		
		// Load temporary preferences that does not come from a user file (will
		// be overwritten upon login)
		PrefsUtility.preferences = new XPreferences();
		PrefsUtility.initializeAnyNewPrefs();
		
		// Load the function cruncher
		Cruncher cruncher = new Cruncher();
		JEXStatics.cruncher = cruncher;
		// DatabaseEmulatorPool emulatorPool = new DatabaseEmulatorPool();
		// JEXStatics.emulatorPool = emulatorPool;
		
		// Load the label color code
		JEXLabelColorCode labelColorCode = new JEXLabelColorCode();
		JEXStatics.labelColorCode = labelColorCode;
		JEXStatics.labelManager = new DatabaseLabelManager();
		
		// Set the current panel to null
		JEXStatics.currentPane = null;
		
		// Properties of this window
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setVisible(false);
		this.setTitle("Je'Xperiment - Databasing made for the biologist");
		
		// initialize all the windows
		this.initialize();
		
		// End of loading display main JEX window
		this.showLogOnFrame(true);
	}
	
	/**
	 * Show a frame with user file loading options
	 * 
	 * @param display
	 */
	public void showLogOnFrame(boolean display)
	{
		if(display)
		{
			this.logOnFrame = new JEXLogOnPanel();
			
			this.logOnFrame.setUndecorated(false);
			// logOnFrame.setBounds(300, 150, 700, 500);
			this.logOnFrame.setBounds(300, 300, 700, 250);
			this.logOnFrame.setVisible(true);
		}
		else
		{
			this.logOnFrame.dispose();
		}
	}
	
	/**
	 * Show a panel with all the available databases
	 * 
	 * @param display
	 */
	public void showDatabaseChooserFrame(boolean display)
	{
		if(display)
		{
			this.dbChooser = new JEXDatabaseChooser();
			
			this.dbChooser.rebuild();
			this.dbChooser.setUndecorated(false);
			this.dbChooser.setBounds(300, 150, 700, 500);
			this.dbChooser.setVisible(true);
		}
		else
		{
			this.dbChooser.dispose();
		}
	}
	
	/**
	 * Displays a waiting cursor on the main jex window
	 * 
	 * @param waiting
	 */
	public void setWaitingCursor(boolean waiting)
	{
		if(waiting)
		{
			Logs.log("setting cursor to waiting mode", 1, this);
			Cursor hourglassCursor = new Cursor(Cursor.WAIT_CURSOR);
			this.setCursor(hourglassCursor);
		}
		else
		{
			// Cursor normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);
			Logs.log("setting cursor to normal mode", 1, this);
			this.setCursor(null);
		}
	}
	
	/**
	 * Enable the display of the main jex window
	 * 
	 * @param display
	 */
	public void showMainJEXWindow(boolean display)
	{
		if(display)
		{
			this.setVisible(true);
		}
		else
		{
			this.setVisible(false);
		}
	}
	
	// ----------------------------------------------------
	// --------- PLOT THE WINDOWS AND DRAW GUI ------------
	// ----------------------------------------------------
	
	// JEX views
	public JPanel menuPane = new JPanel();
	public JPanel centerPane = new JPanel();
	public JSplitPane menuSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	public JSplitPane centerSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	
	public EntryAndObjectPanel leftPanel; // LEFT
	// PANEL
	
	// Panel size
	public int topLeftX = 60;
	public int topLeftY = 20;
	public int windowWidth = 1150;
	public int windowHeight = 700;
	
	/**
	 * Initialize the GUI
	 */
	public void initialize()
	{
		// Setup panel
		this.setBounds(this.topLeftX, this.topLeftY, this.windowWidth, this.windowHeight);
		this.setResizable(true);
		this.setTitle("Je'Xperiment - Databasing made for the biologist");
		
		// Get the content panel
		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.setBackground(DisplayStatics.background);
		
		// Add the menu bar
		this.createMenuBar();
		contentPane.add(this.menuPane, BorderLayout.PAGE_START);
		
		// Prepare the panels in the main split pane
		this.centerPane.setBackground(DisplayStatics.background);
		this.centerPane.setLayout(new BorderLayout());
		this.centerPane.add(Box.createRigidArea(new Dimension(50, 50)));
		
		// Prepare the left split pane
		this.leftPanel = new EntryAndObjectPanel();
		this.leftPanel.setPreferredSize(new Dimension(300, 300));
		
		// Prepare the right split panel
		this.centerSplitPane.setBackground(DisplayStatics.background);
		this.centerSplitPane.setBorder(null);
		this.centerSplitPane.setDividerLocation(300);
		this.centerSplitPane.setDividerSize(6);
		this.centerSplitPane.setResizeWeight(1.0);
		this.centerSplitPane.setLeftComponent(this.centerPane);
		this.centerSplitPane.setRightComponent(new JPanel());
		
		// Add the right split panel
		this.menuSplitPane.setBackground(DisplayStatics.background);
		this.menuSplitPane.setBorder(null);
		this.menuSplitPane.setDividerLocation(200);
		this.menuSplitPane.setDividerSize(6);
		this.menuSplitPane.setResizeWeight(0.0);
		this.menuSplitPane.setLeftComponent(this.leftPanel);
		this.menuSplitPane.setRightComponent(this.centerSplitPane);
		contentPane.add(this.menuSplitPane, BorderLayout.CENTER);
		
		// Add the status bar
		new Thread(JEXStatics.statusBar).start();
		contentPane.add(JEXStatics.statusBar, BorderLayout.PAGE_END);
		
		// Set the key listener
		this.setQuickKeys();
	}
	
	// menu items
	public SignalMenuButton save = new SignalMenuButton();
	
	public SignalMenuButton experimentViewButton = new SignalMenuButton();
	public SignalMenuButton arrayViewButton = new SignalMenuButton();
	
	public SignalMenuButton fileDistributionButton = new SignalMenuButton();
	public SignalMenuButton labelDistributionButton = new SignalMenuButton();
	public SignalMenuButton batchFunctionButton = new SignalMenuButton();
	public SignalMenuButton statAnalysis = new SignalMenuButton();
	public SignalMenuButton pluginsButton = new SignalMenuButton();
	
	public SignalMenuButton update = new SignalMenuButton();
	public SignalMenuButton prefsButton = new SignalMenuButton();
	public SignalMenuButton clearButton = new SignalMenuButton();
	
	/**
	 * Create the top menu bar
	 */
	public void createMenuBar()
	{
		int spacing = 8;
		this.menuPane.setBackground(DisplayStatics.menuBackground);
		this.menuPane.setPreferredSize(new Dimension(300, 60));
		this.menuPane.setLayout(new BoxLayout(this.menuPane, BoxLayout.LINE_AXIS));
		
		Image greyBox = JEXStatics.iconRepository.boxImage(30, 30, DisplayStatics.dividerColor);
		
		Image iconSave = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_SAVE, 30, 30);
		Image iconOverSave = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_SAVE, 30, 30);
		Image iconPressedSave = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_SAVE, 30, 30);
		this.save.setBackgroundColor(DisplayStatics.menuBackground);
		this.save.setForegroundColor(DisplayStatics.lightBackground);
		this.save.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.save.setSize(new Dimension(100, 50));
		this.save.setImage(iconSave);
		this.save.setMouseOverImage(iconOverSave);
		this.save.setMousePressedImage(iconPressedSave);
		this.save.setDisabledImage(greyBox);
		this.save.setText("Save");
		SSCenter.defaultCenter().connect(this.save, SignalMenuButton.SIG_ButtonClicked_NULL, this, "save", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.save);
		
		this.menuPane.add(Box.createHorizontalGlue());
		
		Image iconCreation = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_CREATE_UNSELECTED, 30, 30);
		Image iconOverCreation = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_CREATE_UNSELECTED, 30, 30);
		Image iconPressedCreation = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_CREATE_UNSELECTED, 30, 30);
		this.experimentViewButton.setBackgroundColor(DisplayStatics.menuBackground);
		this.experimentViewButton.setForegroundColor(DisplayStatics.lightBackground);
		this.experimentViewButton.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.experimentViewButton.setSize(new Dimension(100, 50));
		this.experimentViewButton.setImage(iconCreation);
		this.experimentViewButton.setMouseOverImage(iconOverCreation);
		this.experimentViewButton.setMousePressedImage(iconPressedCreation);
		this.experimentViewButton.setDisabledImage(greyBox);
		this.experimentViewButton.setText("Datasets");
		SSCenter.defaultCenter().connect(this.experimentViewButton, SignalMenuButton.SIG_ButtonClicked_NULL, this, "displayCreationPane", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.experimentViewButton);
		
		Image iconView = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_ARRAYVIEW_UNSELECTED, 30, 30);
		Image iconOverView = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_ARRAYVIEW_UNSELECTED, 30, 30);
		Image iconPressedView = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_ARRAYVIEW_UNSELECTED, 30, 30);
		this.arrayViewButton.setBackgroundColor(DisplayStatics.menuBackground);
		this.arrayViewButton.setForegroundColor(DisplayStatics.lightBackground);
		this.arrayViewButton.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.arrayViewButton.setSize(new Dimension(100, 50));
		this.arrayViewButton.setImage(iconView);
		this.arrayViewButton.setMouseOverImage(iconOverView);
		this.arrayViewButton.setMousePressedImage(iconPressedView);
		this.arrayViewButton.setText("Array");
		SSCenter.defaultCenter().connect(this.arrayViewButton, SignalMenuButton.SIG_ButtonClicked_NULL, this, "displayViewPane", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.arrayViewButton);
		
		Image iconDistribution = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_DISTRIBUTE_UNSELECTED, 30, 30);
		Image iconOverDistribution = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_DISTRIBUTE_UNSELECTED, 30, 30);
		Image iconPressedDistribution = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_DISTRIBUTE_UNSELECTED, 30, 30);
		this.fileDistributionButton.setBackgroundColor(DisplayStatics.menuBackground);
		this.fileDistributionButton.setForegroundColor(DisplayStatics.lightBackground);
		this.fileDistributionButton.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.fileDistributionButton.setSize(new Dimension(100, 50));
		this.fileDistributionButton.setImage(iconDistribution);
		this.fileDistributionButton.setMouseOverImage(iconOverDistribution);
		this.fileDistributionButton.setMousePressedImage(iconPressedDistribution);
		this.fileDistributionButton.setDisabledImage(greyBox);
		this.fileDistributionButton.setText("Import");
		SSCenter.defaultCenter().connect(this.fileDistributionButton, SignalMenuButton.SIG_ButtonClicked_NULL, this, "displayDistributionPane", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.fileDistributionButton);
		
		Image iconLabel = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_LABEL_UNSELECTED, 30, 30);
		Image iconOverLabel = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_LABEL_UNSELECTED, 30, 30);
		Image iconPressedLabel = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_LABEL_UNSELECTED, 30, 30);
		this.labelDistributionButton.setBackgroundColor(DisplayStatics.menuBackground);
		this.labelDistributionButton.setForegroundColor(DisplayStatics.lightBackground);
		this.labelDistributionButton.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.labelDistributionButton.setSize(new Dimension(100, 50));
		this.labelDistributionButton.setImage(iconLabel);
		this.labelDistributionButton.setMouseOverImage(iconOverLabel);
		this.labelDistributionButton.setMousePressedImage(iconPressedLabel);
		this.labelDistributionButton.setDisabledImage(greyBox);
		this.labelDistributionButton.setText("Label");
		SSCenter.defaultCenter().connect(this.labelDistributionButton, SignalMenuButton.SIG_ButtonClicked_NULL, this, "displayLabelPane", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.labelDistributionButton);
		
		Image iconFunction = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_FUNCTION_UNSELECTED, 30, 30);
		Image iconOverFunction = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_FUNCTION_UNSELECTED, 30, 30);
		Image iconPressedFunction = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_FUNCTION_UNSELECTED, 30, 30);
		this.batchFunctionButton.setBackgroundColor(DisplayStatics.menuBackground);
		this.batchFunctionButton.setForegroundColor(DisplayStatics.lightBackground);
		this.batchFunctionButton.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.batchFunctionButton.setSize(new Dimension(100, 50));
		this.batchFunctionButton.setImage(iconFunction);
		this.batchFunctionButton.setMouseOverImage(iconOverFunction);
		this.batchFunctionButton.setMousePressedImage(iconPressedFunction);
		this.batchFunctionButton.setDisabledImage(greyBox);
		this.batchFunctionButton.setText("Process");
		SSCenter.defaultCenter().connect(this.batchFunctionButton, SignalMenuButton.SIG_ButtonClicked_NULL, this, "displayFunctionPane", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.batchFunctionButton);
		
		Image iconStats = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_STATS_UNSELECTED, 30, 30);
		Image iconOverStats = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_STATS_UNSELECTED, 30, 30);
		Image iconPressedStats = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_STATS_UNSELECTED, 30, 30);
		this.statAnalysis.setBackgroundColor(DisplayStatics.menuBackground);
		this.statAnalysis.setForegroundColor(DisplayStatics.lightBackground);
		this.statAnalysis.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.statAnalysis.setSize(new Dimension(100, 50));
		this.statAnalysis.setImage(iconStats);
		this.statAnalysis.setMouseOverImage(iconOverStats);
		this.statAnalysis.setMousePressedImage(iconPressedStats);
		this.statAnalysis.setDisabledImage(greyBox);
		this.statAnalysis.setText("Statistics");
		SSCenter.defaultCenter().connect(this.statAnalysis, SignalMenuButton.SIG_ButtonClicked_NULL, this, "displayStatisticsPane", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.statAnalysis);
		
		Image iconPlugins = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_PLUGIN_UNSELECTED, 30, 30);
		Image iconOverPlugins = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_PLUGIN_UNSELECTED, 30, 30);
		Image iconPressedPlugins = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_PLUGIN_UNSELECTED, 30, 30);
		this.pluginsButton.setBackgroundColor(DisplayStatics.menuBackground);
		this.pluginsButton.setForegroundColor(DisplayStatics.lightBackground);
		this.pluginsButton.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.pluginsButton.setSize(new Dimension(100, 50));
		this.pluginsButton.setImage(iconPlugins);
		this.pluginsButton.setMouseOverImage(iconOverPlugins);
		this.pluginsButton.setMousePressedImage(iconPressedPlugins);
		this.pluginsButton.setDisabledImage(greyBox);
		this.pluginsButton.setText("Plugins");
		SSCenter.defaultCenter().connect(this.pluginsButton, SignalMenuButton.SIG_ButtonClicked_NULL, this, "displayPluginsPane", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.pluginsButton);
		
		this.menuPane.add(Box.createHorizontalGlue());
		
		Image iconUpdate = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_UPDATE, 30, 30);
		Image iconOverUpdate = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_UPDATE_OVER, 30, 30);
		Image iconPressedUpdate = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_UPDATE_CLICK, 30, 30);
		this.update.setBackgroundColor(DisplayStatics.menuBackground);
		this.update.setForegroundColor(DisplayStatics.lightBackground);
		this.update.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.update.setSize(new Dimension(100, 50));
		this.update.setImage(iconUpdate);
		this.update.setMouseOverImage(iconOverUpdate);
		this.update.setMousePressedImage(iconPressedUpdate);
		this.update.setDisabledImage(greyBox);
		this.update.setText("Update JEX");
		SSCenter.defaultCenter().connect(this.update, SignalMenuButton.SIG_ButtonClicked_NULL, this, "update", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.update);
		
		Image iconPreferences = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_PREFS, 30, 30);
		Image iconOverPreferences = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_PREFS_OVER, 30, 30);
		Image iconPressedPreferences = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_PREFS_CLICK, 30, 30);
		this.prefsButton.setBackgroundColor(DisplayStatics.menuBackground);
		this.prefsButton.setForegroundColor(DisplayStatics.lightBackground);
		this.prefsButton.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.prefsButton.setSize(new Dimension(100, 50));
		this.prefsButton.setImage(iconPreferences);
		this.prefsButton.setMouseOverImage(iconOverPreferences);
		this.prefsButton.setMousePressedImage(iconPressedPreferences);
		this.prefsButton.setText("Preferences");
		SSCenter.defaultCenter().connect(this.prefsButton, SignalMenuButton.SIG_ButtonClicked_NULL, this, "openPreferences", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.prefsButton);
		
		Image iconClear = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_CLEAR, 30, 30);
		Image iconOverClear = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_CLEAR_OVER, 30, 30);
		Image iconPressedClear = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_CLEAR_CLICK, 30, 30);
		this.clearButton.setBackgroundColor(DisplayStatics.menuBackground);
		this.clearButton.setForegroundColor(DisplayStatics.lightBackground);
		this.clearButton.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		this.clearButton.setSize(new Dimension(100, 50));
		this.clearButton.setImage(iconClear);
		this.clearButton.setMouseOverImage(iconOverClear);
		this.clearButton.setMousePressedImage(iconPressedClear);
		this.clearButton.setText("Clean DB");
		SSCenter.defaultCenter().connect(this.clearButton, SignalMenuButton.SIG_ButtonClicked_NULL, this, "cleanDB", (Class[]) null);
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(this.clearButton);
		
		this.menuPane.add(Box.createHorizontalStrut(spacing));
		this.menuPane.add(Box.createHorizontalStrut(spacing));
	}
	
	public void unselectedAllIcons()
	{
		Image iconCreation = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_CREATE_UNSELECTED, 30, 30);
		this.experimentViewButton.setImage(iconCreation);
		this.experimentViewButton.setMouseOverImage(iconCreation);
		this.experimentViewButton.setMousePressedImage(iconCreation);
		
		Image iconDistribution = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_DISTRIBUTE_UNSELECTED, 30, 30);
		this.fileDistributionButton.setImage(iconDistribution);
		this.fileDistributionButton.setMouseOverImage(iconDistribution);
		this.fileDistributionButton.setMousePressedImage(iconDistribution);
		
		Image iconLabel = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_LABEL_UNSELECTED, 30, 30);
		this.labelDistributionButton.setImage(iconLabel);
		this.labelDistributionButton.setMouseOverImage(iconLabel);
		this.labelDistributionButton.setMousePressedImage(iconLabel);
		
		Image iconFunction = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_FUNCTION_UNSELECTED, 30, 30);
		this.batchFunctionButton.setImage(iconFunction);
		this.batchFunctionButton.setMouseOverImage(iconFunction);
		this.batchFunctionButton.setMousePressedImage(iconFunction);
		
		Image iconStats = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_STATS_UNSELECTED, 30, 30);
		this.statAnalysis.setImage(iconStats);
		this.statAnalysis.setMouseOverImage(iconStats);
		this.statAnalysis.setMousePressedImage(iconStats);
		
		Image iconPlugins = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_PLUGIN_UNSELECTED, 30, 30);
		this.pluginsButton.setImage(iconPlugins);
		this.pluginsButton.setMouseOverImage(iconPlugins);
		this.pluginsButton.setMousePressedImage(iconPlugins);
		
		Image iconView = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_ARRAYVIEW_UNSELECTED, 30, 30);
		this.arrayViewButton.setImage(iconView);
		this.arrayViewButton.setMouseOverImage(iconView);
		this.arrayViewButton.setMousePressedImage(iconView);
	}
	
	public void displayCreationPane()
	{
		// Reset views
		this.resetViews();
		
		// Create a contoller
		JEXCreationPanelController creationPane = new JEXCreationPanelController();
		JEXStatics.creationPane = creationPane;
		
		// set current pane
		JEXStatics.currentPane = JEXStatics.creationPane;
		
		// Set the new panel size
		JEXStatics.creationPane.imposeSplitPaneOptions(this.centerSplitPane);
		JEXTabPanelController.setLeftPanelWidth(this.menuSplitPane.getDividerLocation());
		
		// Change the icons
		this.unselectedAllIcons();
		Image iconCreation = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_CREATE_SELECTED, 30, 30);
		this.experimentViewButton.setImage(iconCreation);
		this.experimentViewButton.setMouseOverImage(iconCreation);
		this.experimentViewButton.setMousePressedImage(iconCreation);
		
		Logs.log("Displaying the creation view", 0, this);
		this.changeMainView(JEXStatics.creationPane.getMainPanel());
		this.changeLeftView(JEXStatics.creationPane.getLeftPanel());
		this.changeRightView(JEXStatics.creationPane.getRightPanel());
		
	}
	
	public void displayDistributionPane()
	{
		// if(JEXStatics.currentPane != null)
		// JEXStatics.currentPane.saveSplitPaneOptions(centerSplitPane);
		// JEXStatics.currentPane = JEXStatics.distribPane;
		// JEXStatics.distribPane.imposeSplitPaneOptions(centerSplitPane);
		// JEXTabPanelController.setLeftPanelWidth(menuSplitPane.getDividerLocation());
		
		// Reset views
		this.resetViews();
		
		// Create a contoller
		JEXDistributionPanelController distribPane = new JEXDistributionPanelController();
		JEXStatics.distribPane = distribPane;
		
		// set current pane
		JEXStatics.currentPane = JEXStatics.distribPane;
		
		// Set the new panel size
		JEXStatics.distribPane.imposeSplitPaneOptions(this.centerSplitPane);
		JEXTabPanelController.setLeftPanelWidth(this.menuSplitPane.getDividerLocation());
		
		// Change the icons
		this.unselectedAllIcons();
		Image iconDistribution = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_DISTRIBUTE_SELECTED, 30, 30);
		this.fileDistributionButton.setImage(iconDistribution);
		this.fileDistributionButton.setMouseOverImage(iconDistribution);
		this.fileDistributionButton.setMousePressedImage(iconDistribution);
		
		Logs.log("Displaying the distribution panel", 0, this);
		this.changeMainView(JEXStatics.distribPane.getMainPanel());
		this.changeLeftView(JEXStatics.distribPane.getLeftPanel());
		this.changeRightView(JEXStatics.distribPane.getRightPanel());
	}
	
	public void displayLabelPane()
	{
		// if(JEXStatics.currentPane != null)
		// JEXStatics.currentPane.saveSplitPaneOptions(centerSplitPane);
		// JEXStatics.currentPane = JEXStatics.labelPane;
		// JEXStatics.labelPane.imposeSplitPaneOptions(centerSplitPane);
		// JEXTabPanelController.setLeftPanelWidth(menuSplitPane.getDividerLocation());
		
		// Reset views
		this.resetViews();
		
		// Create a contoller
		JEXLabelPanelController labelPane = new JEXLabelPanelController();
		JEXStatics.labelPane = labelPane;
		
		// set current pane
		JEXStatics.currentPane = JEXStatics.labelPane;
		
		// Set the new panel size
		JEXStatics.labelPane.imposeSplitPaneOptions(this.centerSplitPane);
		JEXTabPanelController.setLeftPanelWidth(this.menuSplitPane.getDividerLocation());
		
		// Change the icons
		this.unselectedAllIcons();
		Image iconLabel = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_LABEL_SELECTED, 30, 30);
		this.labelDistributionButton.setImage(iconLabel);
		this.labelDistributionButton.setMouseOverImage(iconLabel);
		this.labelDistributionButton.setMousePressedImage(iconLabel);
		
		Logs.log("Displaying the label panel", 0, this);
		this.changeMainView(JEXStatics.labelPane.getMainPanel());
		this.changeLeftView(JEXStatics.labelPane.getLeftPanel());
		this.changeRightView(JEXStatics.labelPane.getRightPanel());
	}
	
	public void displayViewPane()
	{
		// Reset views
		this.resetViews();
		
		// Create a contoller
		JEXViewPanelController viewPane = new JEXViewPanelController();
		JEXStatics.viewPane = viewPane;
		
		// set current pane
		JEXStatics.currentPane = JEXStatics.viewPane;
		
		// Set the new panel size
		JEXStatics.viewPane.imposeSplitPaneOptions(this.centerSplitPane);
		JEXTabPanelController.setLeftPanelWidth(this.menuSplitPane.getDividerLocation());
		
		// Change the icons
		this.unselectedAllIcons();
		Image iconNotes = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_ARRAYVIEW_SELECTED, 30, 30);
		this.arrayViewButton.setImage(iconNotes);
		this.arrayViewButton.setMouseOverImage(iconNotes);
		this.arrayViewButton.setMousePressedImage(iconNotes);
		
		Logs.log("Displaying the view panel", 0, this);
		this.changeMainView(JEXStatics.viewPane.getMainPanel());
		this.changeLeftView(JEXStatics.viewPane.getLeftPanel());
		this.changeRightView(JEXStatics.viewPane.getRightPanel());
		
	}
	
	public void displayFunctionPane()
	{
		// if(JEXStatics.currentPane != null)
		// JEXStatics.currentPane.saveSplitPaneOptions(centerSplitPane);
		// JEXStatics.currentPane = JEXStatics.functionPane;
		// JEXStatics.functionPane.imposeSplitPaneOptions(centerSplitPane);
		// JEXTabPanelController.setLeftPanelWidth(menuSplitPane.getDividerLocation());
		
		// Reset views
		this.resetViews();
		
		// Create a contoller
		if(JEXStatics.functionPane == null)
		{
			JEXStatics.functionPane = new JEXFunctionPanelController();
		}
		
		// set current pane
		JEXStatics.currentPane = JEXStatics.functionPane;
		
		// Set the new panel size
		JEXStatics.functionPane.imposeSplitPaneOptions(this.centerSplitPane);
		JEXTabPanelController.setLeftPanelWidth(this.menuSplitPane.getDividerLocation());
		
		// Change the icons
		this.unselectedAllIcons();
		Image iconFunction = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_FUNCTION_SELECTED, 30, 30);
		this.batchFunctionButton.setImage(iconFunction);
		this.batchFunctionButton.setMouseOverImage(iconFunction);
		this.batchFunctionButton.setMousePressedImage(iconFunction);
		
		Logs.log("Displaying the processing panel", 0, this);
		this.changeMainView(JEXStatics.functionPane.getMainPanel());
		this.changeLeftView(JEXStatics.functionPane.getLeftPanel());
		this.changeRightView(JEXStatics.functionPane.getRightPanel());
	}
	
	public void displayPluginsPane()
	{
		// if(JEXStatics.currentPane != null)
		// JEXStatics.currentPane.saveSplitPaneOptions(centerSplitPane);
		// JEXStatics.currentPane = JEXStatics.pluginPane;
		// JEXStatics.pluginPane.imposeSplitPaneOptions(centerSplitPane);
		// JEXTabPanelController.setLeftPanelWidth(menuSplitPane.getDividerLocation());
		
		// Reset views
		this.resetViews();
		
		// Create a contoller
		JEXPluginPanelController pluginPane = new JEXPluginPanelController();
		JEXStatics.pluginPane = pluginPane;
		
		// set current pane
		JEXStatics.currentPane = JEXStatics.pluginPane;
		
		// Set the new panel size
		JEXStatics.pluginPane.imposeSplitPaneOptions(this.centerSplitPane);
		JEXTabPanelController.setLeftPanelWidth(this.menuSplitPane.getDividerLocation());
		
		// Change the icons
		this.unselectedAllIcons();
		Image iconPlugins = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_PLUGIN_SELECTED, 30, 30);
		this.pluginsButton.setImage(iconPlugins);
		this.pluginsButton.setMouseOverImage(iconPlugins);
		this.pluginsButton.setMousePressedImage(iconPlugins);
		
		Logs.log("Displaying the plugin panel", 0, this);
		this.changeMainView(JEXStatics.pluginPane.getMainPanel());
		this.changeLeftView(JEXStatics.pluginPane.getLeftPanel());
		this.changeRightView(JEXStatics.pluginPane.getRightPanel());
	}
	
	/**
	 * Switch to the statistics display
	 */
	public void displayStatisticsPane()
	{
		// if(JEXStatics.currentPane != null)
		// JEXStatics.currentPane.saveSplitPaneOptions(centerSplitPane);
		
		// Reset views
		this.resetViews();
		
		// Create a contoller
		JEXStatisticsPanelController statPane = new JEXStatisticsPanelController();
		JEXStatics.statPane = statPane;
		
		// set current pane
		JEXStatics.currentPane = JEXStatics.statPane;
		
		// Set the new panel size
		JEXStatics.statPane.imposeSplitPaneOptions(this.centerSplitPane);
		JEXTabPanelController.setLeftPanelWidth(this.menuSplitPane.getDividerLocation());
		
		// Change the icons
		this.unselectedAllIcons();
		Image iconStats = JEXStatics.iconRepository.getImageWithName(IconRepository.MAIN_STATS_SELECTED, 30, 30);
		this.statAnalysis.setImage(iconStats);
		this.statAnalysis.setMouseOverImage(iconStats);
		this.statAnalysis.setMousePressedImage(iconStats);
		
		// Display the panels
		Logs.log("Displaying the statistics builder", 0, this);
		this.changeMainView(JEXStatics.statPane.getMainPanel());
		this.changeLeftView(JEXStatics.statPane.getLeftPanel());
		this.changeRightView(JEXStatics.statPane.getRightPanel());
	}
	
	public void resetViews()
	{
		// Send close signal to current panel
		if(JEXStatics.currentPane != null)
		{
			JEXStatics.currentPane.closeTab();
		}
		
		// Save the current size settings
		if(JEXStatics.currentPane != null)
		{
			JEXStatics.currentPane.saveSplitPaneOptions(this.centerSplitPane);
		}
		
		// Set to null every controller
		JEXStatics.creationPane = null;
		JEXStatics.distribPane = null;
		JEXStatics.labelPane = null;
		JEXStatics.notesPane = null;
		JEXStatics.statPane = null;
		JEXStatics.viewPane = null;
		JEXStatics.pluginPane = null;
	}
	
	/**
	 * Replace center view by the chosen view
	 * 
	 * @param newView
	 */
	private void changeMainView(JPanel newView)
	{
		Logs.log("Changing view", 0, this);
		
		this.centerPane.removeAll();
		this.centerPane.add(newView, BorderLayout.CENTER);
		this.centerPane.revalidate();
		this.centerPane.repaint();
	}
	
	/**
	 * Change the panel in the left side of the display
	 * 
	 * @param leftView
	 */
	private void changeLeftView(JPanel leftView)
	{
		Logs.log("Changing left view", 0, this);
		if(leftView == null)
		{
			this.menuSplitPane.setLeftComponent(this.leftPanel);
			this.menuSplitPane.setDividerLocation(JEXViewPanelController.getLeftPanelWidth());
		}
		else
		{
			this.menuSplitPane.setLeftComponent(leftView);
		}
		
		this.repaint();
	}
	
	/**
	 * Change the panel in the left side of the display
	 * 
	 * @param leftView
	 */
	private void changeRightView(JPanel rightView)
	{
		Logs.log("Changing right view", 0, this);
		if(rightView == null)
		{
			this.centerSplitPane.setRightComponent(new JPanel());
		}
		else
		{
			this.centerSplitPane.setRightComponent(rightView);
		}
		// This is to refresh views to the appropriate navigation depth
		// We do it here because we always load the RightView last
		SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.NAVIGATION, (Object[]) null);
		this.repaint();
	}
	
	/**
	 * Display a panel on the glass pane
	 * 
	 * @param pane
	 * @return
	 */
	public boolean displayGlassPane(JPanel pane, boolean on)
	{
		
		if(pane == null)
		{
			Component c = this.getGlassPane();
			c.setVisible(false);
			return false;
		}
		
		if(on)
		{
			pane.setOpaque(true);
			this.setGlassPane(pane);
			pane.setVisible(true);
			return true;
		}
		else
		{
			pane.setOpaque(false);
			this.setGlassPane(pane);
			pane.setVisible(false);
			return true;
		}
		
	}
	
	/**
	 * Open the user file at location FILE
	 * 
	 * @param file
	 */
	public void openUser(File file)
	{
		Logs.log("Opening user file " + file.getName(), 0, this);
		
		// Load the user file
		boolean done = JEXStatics.jexManager.logOn(file);
		if(!done)
		{
			return;
		}
		
		// Change the panel
		JEXStatics.main.displayViewPane();
	}
	
	// ----------------------------------------------------
	// --------- GENERAL METHODS --------------------------
	// ----------------------------------------------------
	
	/**
	 * Quit the program
	 */
	public void quit()
	{
		if(R.isConnected())
		{
			R.close();
		}
		this.dispose();
		System.exit(0);
	}
	
	/**
	 * Save the opened database
	 */
	public void save()
	{
		Logs.log("Saving requested", 1, this);
		// String consolidateStr =
		// JEXStatics.userPreferences.get("Consolidate Database", "false");
		// Boolean consolidate = Boolean.parseBoolean(consolidateStr);
		JEXStatics.jexManager.saveCurrentDatabase();
	}
	
	/**
	 * Attempt to update JEX
	 */
	public void update()
	{
		Logs.log("Update requested", 1, this);
		// String consolidateStr =
		// JEXStatics.userPreferences.get("Consolidate Database", "false");
		// Boolean consolidate = Boolean.parseBoolean(consolidateStr);
		JEXStatics.statusBar.setStatusText("Attempting to update JEX...");
		Updater.attemptJEXUpdate();
	}
	
	/**
	 * Make a bookmark
	 */
	public void bookmark()
	{
		if(!JEXStatics.jexManager.isLoggedOn())
		{
			return;
		}
		if(JEXStatics.jexManager.getCurrentDatabase() == null)
		{
			return;
		}
		
		DialogGlassPane diagPanel = new DialogGlassPane("Warning");
		diagPanel.setSize(400, 200);
		
		ErrorMessagePane errorPane = new ErrorMessagePane("Bookmarks are not implemented yet... ");
		diagPanel.setCentralPanel(errorPane);
		
		JEXStatics.main.displayGlassPane(diagPanel, true);
	}
	
	/**
	 * Open preference panel
	 */
	public void openPreferences()
	{
		// Reload from file to get rid of any unsaved changes
		PrefsUtility.reloadPrefs();
		XPreferencePanelController prefs = new XPreferencePanelController(PrefsUtility.getUserPrefs(), true);
		this.prefsDialog = new JDialog(this, "JEX Preferences", Dialog.ModalityType.APPLICATION_MODAL);
		this.prefsDialog.addWindowListener(this);
		this.prefsDialog.getContentPane().setLayout(new BorderLayout());
		this.prefsDialog.getContentPane().add(prefs.getPanel(), BorderLayout.CENTER);
		SSCenter.defaultCenter().connect(prefs, XPreferencePanelController.SIG_Save_NULL, this, "savePrefs", (Class[]) null);
		SSCenter.defaultCenter().connect(prefs, XPreferencePanelController.SIG_Cancel_NULL, this, "cancelPrefs", (Class[]) null);
		this.prefsDialog.setSize(500, 500);
		this.prefsDialog.setVisible(true);
		
	}
	
	public void cleanDB()
	{
		JEXDBIO.cleanDB(JEXStatics.jexManager.getCurrentDatabase());
	}
	
	// ----------------------------------------------------
	// --------- EVENT HANDLING FUNCTIONS -----------------
	// ----------------------------------------------------
	
	@Override
	public void actionPerformed(ActionEvent e)
	{}
	
	public void savePrefs()
	{
		PrefsUtility.savePrefs();
	}
	
	public void cancelPrefs()
	{
		this.prefsDialog.setVisible(false);
		PrefsUtility.reloadPrefs();
		this.prefsDialog.dispatchEvent(new WindowEvent(this.prefsDialog, WindowEvent.WINDOW_CLOSING));
	}
	
	private void setQuickKeys()
	{
		// Allow JEX to prelisten to KeyEvents to capture modifier key states
		// before processing by components in focus
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
		
		// Always add the actions to the action map of the menuPane because that
		// is the one that never changes
		// If it does ever change, call setQuickKeys again to reconnect quick
		// keys
		KeyStroke stroke;
		
		// Save action
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.META_DOWN_MASK, false);
		this.menuPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "save");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, false);
		this.menuPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "save");
		this.menuPane.getActionMap().put("save", new ActionSave());
		
		// Other actions
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.META_DOWN_MASK, false);
		this.menuPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "abortGuiTask");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK, false);
		this.menuPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "abortGuiTask");
		this.menuPane.getActionMap().put("abortGuiTask", new ActionAbortGuiTask());
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.META_DOWN_MASK, false);
		this.menuPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "abortCrunch");
		stroke = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK, false);
		this.menuPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, "abortCrunch");
		this.menuPane.getActionMap().put("abortCrunch", new ActionAbortCrunch());
	}
	
	public void keyTyped(KeyEvent e)
	{}
	
	public void keyReleased(KeyEvent e)
	{}
	
	@Override
	public void windowActivated(WindowEvent e)
	{}
	
	@Override
	public void windowClosed(WindowEvent e)
	{
		if(e.getSource() == this)
		{
			Logs.log("Dispose the window and exit", 0, this);
			this.quit();
		}
		else if(e.getSource() == this.prefsDialog)
		{
			PrefsUtility.reloadPrefs();
		}
		
	}
	
	@Override
	public void windowClosing(WindowEvent e)
	{}
	
	@Override
	public void windowDeactivated(WindowEvent e)
	{}
	
	@Override
	public void windowDeiconified(WindowEvent e)
	{}
	
	@Override
	public void windowIconified(WindowEvent e)
	{}
	
	@Override
	public void windowOpened(WindowEvent e)
	{}
	
	// -------------------------------------
	// --------- Main functions ---------
	// -------------------------------------
	public static void main(final String args[])
	{
		Logs.log("Checking command line arguments...", JEXperiment.class);
		for (String arg : args)
		{
			if(arg.equals("-fromJar") || Updater.runningFromJar())
			{
				Logs.log("Found '-fromJar' arg or detected running from jar... setting error to print to std out of console.", JEXperiment.class);
				System.setErr(System.out);
			}
		}
		Runnable runner = new Runnable() {
			
			@Override
			public void run()
			{
				// set default font
				Font font = new Font("sans serif", Font.PLAIN, 11);
				UIManager.put("Button.font", font);
				UIManager.put("ToggleButton.font", font);
				UIManager.put("RadioButton.font", font);
				UIManager.put("CheckBox.font", font);
				UIManager.put("ColorChooser.font", font);
				UIManager.put("ComboBox.font", font);
				UIManager.put("Label.font", font);
				UIManager.put("List.font", font);
				UIManager.put("MenuBar.font", font);
				UIManager.put("MenuItem.font", font);
				UIManager.put("RadioButtonMenuItem.font", font);
				UIManager.put("CheckBoxMenuItem.font", font);
				UIManager.put("Menu.font", font);
				UIManager.put("PopupMenu.font", font);
				UIManager.put("OptionPane.font", font);
				UIManager.put("Panel.font", font);
				UIManager.put("ProgressBar.font", font);
				UIManager.put("ScrollPane.font", font);
				UIManager.put("Viewport.font", font);
				UIManager.put("TabbedPane.font", font);
				UIManager.put("Table.font", font);
				UIManager.put("TableHeader.font", font);
				UIManager.put("TextField.font", font);
				UIManager.put("PasswordField.font", font);
				UIManager.put("TextArea.font", font);
				UIManager.put("TextPane.font", font);
				UIManager.put("EditorPane.font", font);
				UIManager.put("TitledBorder.font", font);
				UIManager.put("ToolBar.font", font);
				UIManager.put("ToolTip.font", font);
				UIManager.put("Tree.font", font);
				UIManager.put("Label.font", font);
				JEXperiment jex = new jex.JEXperiment();
				
				if(args != null && args.length > 0)
				{
					String arg1 = args[0];
					File file = new File(arg1);
					if(file.exists())
					{
						jex.openUser(file);
					}
				}
			}
		};
		SwingUtilities.invokeLater(runner);
	}
	
	@Override
	public void windowGainedFocus(WindowEvent arg0)
	{
		this.getRootPane().requestFocusInWindow();
	}
	
	@Override
	public void windowLostFocus(WindowEvent arg0)
	{
		// TODO Auto-generated method stub
		
	}
	
	@SuppressWarnings("serial")
	public class ActionSave extends AbstractAction {
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JEXStatics.main.save();
			Logs.log("Saving", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionAbortGuiTask extends AbstractAction {
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JEXStatics.cruncher.stopGuiTask = true;
			Logs.log("Aborting Gui Task!", 0, this);
		}
	}
	
	@SuppressWarnings("serial")
	public class ActionAbortCrunch extends AbstractAction {
		
		@Override
		public void actionPerformed(ActionEvent e)
		{
			JEXStatics.cruncher.stopCrunch = true;
			Logs.log("Aborting Crunch!", 0, this);
		}
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent e)
	{
		KeyStatics.captureModifiers(e);
		// KeyStatics.printModifiers();
		return false; // Allow others to respond to the keyEvent
	}
	
}
