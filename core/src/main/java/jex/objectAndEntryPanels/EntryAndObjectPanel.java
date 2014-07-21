package jex.objectAndEntryPanels;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import plugins.selector.QuickSelector;
import signals.SSCenter;

public class EntryAndObjectPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	private QuickSelector quickSelector;
	private JEXDataPanel objectLabelPanel;
	
	private JLabel title1, title2;
	private JPanel objectPanel;
	private JPanel headerPane1, headerPane2;
	
	public EntryAndObjectPanel()
	{
		// Setup updating links
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTION, this, "rebuild", (Class[]) null);
		
		initialize();
	}
	
	private void initialize()
	{
		// Build the selector header
		title1 = new JLabel("BROWSE/SELECT ENTRIES");
		headerPane1 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane1.setBackground(DisplayStatics.menuBackground);
		title1.setFont(FontUtility.boldFont);
		headerPane1.add(title1);
		
		// Build the quickSelector
		this.quickSelector = new QuickSelector();
		
		// Build the selector header
		title2 = new JLabel("BROWSE/SELECT OBJECTS");
		title2.setFont(FontUtility.boldFont);
		headerPane2 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane2.setBackground(DisplayStatics.menuBackground);
		headerPane2.add(title2);
		
		// Build the objects panel
		objectLabelPanel = new JEXDataPanel();
		objectLabelPanel.whenToGetObjects(JEXStatics.jexManager, JEXManager.AVAILABLEOBJ, "objectsChanged");
		objectLabelPanel.whereToGetObjects(JEXStatics.jexManager, "getObjects");
		objectLabelPanel.whereToRemoveObjects(JEXStatics.jexDBManager, "removeDataListFromEntry");
		objectLabelPanel.whereToDuplicateObjects(JEXStatics.jexDBManager, "saveDataInEntries");
		
		// Create the scroll panel
		objectPanel = objectLabelPanel.panel();
		objectPanel.setBorder(BorderFactory.createEmptyBorder());
		
		// Place the objects
		this.setBackground(DisplayStatics.lightBackground);
		this.setLayout(new MigLayout("center,flowy,ins 2", "[center,grow]", "[]1[0:0,fill,grow 45]1[]1[0:0,grow 55]"));
		this.add(headerPane1, "growx");
		this.add(this.quickSelector.panel(), "grow");
		this.add(headerPane2, "growx");
		this.add(objectPanel, "grow");
		
		rebuild();
	}
	
	public void rebuild()
	{   
		
	}
	
}
