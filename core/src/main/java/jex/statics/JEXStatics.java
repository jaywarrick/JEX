package jex.statics;

import function.plugin.IJ2.IJ2PluginUtility;
import icons.IconRepository;

import java.util.Vector;

import javax.swing.JFileChooser;

import jex.JEXDatabaseManager;
import jex.JEXLabelColorCode;
import jex.JEXMainView;
import jex.JEXManager;
import jex.JEXperiment;
import jex.StatusBar;
import jex.jexTabPanel.JEXTabPanelController;
import jex.jexTabPanel.creationPanel.JEXCreationPanelController;
import jex.jexTabPanel.jexDistributionPanel.JEXDistributionPanelController;
import jex.jexTabPanel.jexFunctionPanel.JEXFunctionPanelController;
import jex.jexTabPanel.jexLabelPanel.JEXLabelPanelController;
import jex.jexTabPanel.jexNotesPanel.JEXNotesPanelController;
import jex.jexTabPanel.jexPluginPanel.JEXPluginPanelController;
import jex.jexTabPanel.jexStatisticsPanel.JEXStatisticsPanelController;
import jex.jexTabPanel.jexViewPanel.JEXViewPanelController;
import plugins.labelManager.DatabaseLabelManager;
import plugins.plugin.PlugInController;
import cruncher.Cruncher;

public class JEXStatics {
	
	// Plugins
	public static IJ2PluginUtility ij2;
	
	// Graphical icon repository
	public static IconRepository iconRepository;
	
	// JEXperiment controller
	public static JEXManager jexManager;
	public static JEXDatabaseManager jexDBManager;
	
	// The main JEXperiment panel
	public static JEXperiment main;
	
	// The different views
	public static JEXCreationPanelController creationPane;
	public static JEXDistributionPanelController distribPane;
	public static JEXLabelPanelController labelPane;
	public static JEXNotesPanelController notesPane;
	public static JEXFunctionPanelController functionPane;
	public static JEXStatisticsPanelController statPane;
	public static JEXViewPanelController viewPane;
	public static JEXPluginPanelController pluginPane;
	public static JEXTabPanelController currentPane;
	
	// JEXperiment gui items, e.g. the status bar, main views, etc...
	public static StatusBar statusBar;
	public static JEXMainView welcomePane;
	public static Vector<PlugInController> plugins = new Vector<PlugInController>();
	
	// Function cruncher
	public static Cruncher cruncher;
	
	// Gui and color
	public static JEXLabelColorCode labelColorCode;
	public static DatabaseLabelManager labelManager;
	
	// File chooser
	public static JFileChooser fileChooser = new JFileChooser();
	
	// FileManagement
	public static JEXDBFileManager fileManager = null;
}
