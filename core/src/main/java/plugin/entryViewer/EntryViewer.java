package plugin.entryViewer;

import Database.DBObjects.JEXEntry;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import jex.JEXManager;
import jex.objectAndEntryPanels.JEXDataPanel;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import plugins.plugin.PlugIn;
import plugins.plugin.PlugInController;
import signals.SSCenter;

public class EntryViewer implements PlugInController {
	
	// Gui
	private PlugIn dialog;
	private JSplitPane main;
	private EntryInfoPanel infoPanel;
	private JPanel centerPanel;
	private JPanel leftPanel;
	private JLabel title1, title2;
	private JPanel headerPane1, headerPane2;
	
	// Variables
	private JEXEntry entry;
	private JEXDataPanel objects;
	
	public EntryViewer(JEXEntry entry)
	{
		initizalizeDialog();
		initialize();
		
		this.setVisible(true);
		
		// Make appropriate connections
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.AVAILABLEOBJ, this, "viewedEntryChanged", (Class[]) null);
	}
	
	// //////////////////////////////////////
	// //////////Initializers //////////////
	// //////////////////////////////////////
	
	/**
	 * initialize the panel
	 */
	private void initialize()
	{
		this.dialog.getContentPane().removeAll();
		
		// Make the center split panel
		main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		main.setBackground(DisplayStatics.background);
		main.setBorder(null);
		main.setDividerLocation(300);
		main.setDividerSize(6);
		main.setResizeWeight(1.0);
		
		// Make the left panel
		makeLeftPane();
		
		// Make the centerPanel panel
		centerPanel = new JPanel();
		centerPanel.setBackground(DisplayStatics.lightBackground);
		
		// fill the split panel
		main.setLeftComponent(leftPanel);
		main.setRightComponent(centerPanel);
		
		// place in the content panel
		this.dialog.getContentPane().add(main, BorderLayout.CENTER);
		this.dialog.getContentPane().invalidate();
		this.dialog.getContentPane().validate();
		this.dialog.getContentPane().repaint();
	}
	
	/**
	 * create the left panel
	 */
	private void makeLeftPane()
	{
		// Build the selector header
		title1 = new JLabel("DATA ENTRY INFO");
		headerPane1 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane1.setBackground(DisplayStatics.menuBackground);
		title1.setFont(FontUtility.boldFont);
		headerPane1.add(title1);
		
		// Build the quickSelector
		infoPanel = new EntryInfoPanel(entry);
		
		// Build the selector header
		title2 = new JLabel("DATA IN THE ENTRY");
		title2.setFont(FontUtility.boldFont);
		headerPane2 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane2.setBackground(DisplayStatics.menuBackground);
		headerPane2.add(title2);
		
		// Build the objects panel
		objects = new JEXDataPanel();
		objects.whenToGetObjects(JEXStatics.jexManager, JEXManager.AVAILABLEOBJ, "objectsChanged");
		objects.whereToGetObjects(JEXStatics.jexManager, "getObjects");
		objects.whereToRemoveObjects(JEXStatics.jexDBManager, "removeDataListFromEntry");
		objects.whereToDuplicateObjects(JEXStatics.jexDBManager, "saveDataInEntries");
		
		// Create the scroll panel
		JPanel opane = objects.panel();
		opane.setBorder(BorderFactory.createEmptyBorder());
		
		// Place the objects
		leftPanel = new JPanel();
		leftPanel.setBackground(DisplayStatics.lightBackground);
		leftPanel.setLayout(new MigLayout("center,flowy,ins 2", "[center,grow]", "[]1[0:0,fill,grow 33]1[]1[0:0,grow 67]"));
		leftPanel.add(headerPane1, "growx");
		leftPanel.add(infoPanel, "grow");
		leftPanel.add(headerPane2, "growx");
		leftPanel.add(opane, "grow");
	}
	
	/**
	 * initialize the dialog panel
	 */
	private void initizalizeDialog()
	{
		this.dialog = new PlugIn(this);
		this.dialog.setBounds(100, 100, 800, 600);
		this.dialog.setDefaultCloseOperation(PlugIn.DISPOSE_ON_CLOSE);
		this.dialog.getContentPane().setBackground(DisplayStatics.background);
		this.dialog.getContentPane().setLayout(new BorderLayout());
		this.dialog.getContentPane().add(new JPanel(), BorderLayout.CENTER);
	}
	
	// //////////////////////////////////////
	// //////////Actions //////////////
	// //////////////////////////////////////
	
	public void setVisible(boolean visible)
	{
		this.dialog.setVisible(visible);
	}
	
	// //////////////////////////////////////
	// //////////PlugIn Stuff //////////////
	// //////////////////////////////////////
	
	/**
	 * Called upon closing of the window
	 */
	public void finalizePlugIn()
	{   
		
	}
	
	public PlugIn plugIn()
	{
		return this.dialog;
	}
}
