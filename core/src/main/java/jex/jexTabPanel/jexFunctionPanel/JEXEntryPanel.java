package jex.jexTabPanel.jexFunctionPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import miscellaneous.StringUtility;
import signals.SSCenter;
import Database.DBObjects.JEXEntry;

public class JEXEntryPanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	// Model variables
	private TreeSet<JEXEntry> entryList;
	private List<JEXEntryPanelLine> panelList;
	
	// Main gui components
	private JScrollPane scroll;
	private JPanel entryPane;
	
	public JEXEntryPanel()
	{
		this.panelList = new ArrayList<JEXEntryPanelLine>(0);
		
		// Setup updating links
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTION, this, "rebuild", (Class[]) null);
		
		// Create the entry panel
		this.entryPane = new JPanel();
		this.entryPane.setBackground(DisplayStatics.lightBackground);
		this.entryPane.setLayout(new BoxLayout(this.entryPane, BoxLayout.PAGE_AXIS));
		
		// Create the scroll panel
		this.scroll = new JScrollPane(this.entryPane);
		this.scroll.setBorder(BorderFactory.createEmptyBorder());
		
		this.setBackground(DisplayStatics.lightBackground);
		// this.setLayout(new
		// MigLayout("center,flowy,ins 2","[center,grow]","[]1[0:0,fill,grow 33]1[]1[0:0,grow 67]"));
		this.setLayout(new BorderLayout());
		this.add(this.scroll);
		
		this.rebuild();
	}
	
	/**
	 * Rebuild the objectpanel list in case the array has changed
	 */
	public void rebuild()
	{
		// Get the itnv fromt the database
		this.entryList = JEXStatics.jexManager.getSelectedEntries();
		this.entryPane.removeAll();
		this.entryPane.setAlignmentX(LEFT_ALIGNMENT);
		this.entryPane.setBackground(DisplayStatics.lightBackground);
		
		// If the object list is empty or null
		// then add a message signaling it to the user
		if(this.entryList == null || this.entryList.size() == 0)
		{
			Logs.log("Rebuilding the entry panel: found 0 entries", 2, this);
			
			// create a label
			JLabel label = new JLabel("No Entries");
			label.setFont(FontUtility.italicFonts);
			
			// Create a panel
			JPanel temp = new JPanel();
			temp.setLayout(new BorderLayout());
			temp.add(label);
			temp.setPreferredSize(new Dimension(40, 20));
			
			// add it to the main panel
			this.entryPane.add(temp);
		}
		else
		{
			Logs.log("Rebuilding the entry panel: found " + this.entryList.size() + " entries", 2, this);
			
			// create the list of panels
			this.panelList = new ArrayList<JEXEntryPanelLine>(0);
			
			// Create the entry map
			TreeMap<String,TreeSet<JEXEntry>> entryMap = this.sortEntries(this.entryList);
			List<String> expNames = new Vector<String>();
			expNames.addAll(entryMap.keySet());
			StringUtility.sortStringList(expNames);
			
			// Loop through the entryMap
			for (String expName : expNames)
			{
				// Create the experiment label
				JLabel expLabel = new JLabel(expName);
				expLabel.setFont(FontUtility.boldFont);
				expLabel.setAlignmentX(LEFT_ALIGNMENT);
				this.entryPane.add(expLabel);
				
				// Loop through the entries
				TreeSet<JEXEntry> theEntries = entryMap.get(expName);
				for (JEXEntry e : theEntries)
				{
					
					// Create a object panel line to display the object type
					// name
					JEXEntryPanelLine newobjectPanel = new JEXEntryPanelLine(e);
					newobjectPanel.setAlignmentX(LEFT_ALIGNMENT);
					
					// add it to the main panel and the object panel list
					this.panelList.add(newobjectPanel);
					this.entryPane.add(newobjectPanel);
				}
				
				// // Loop through the trays
				// TreeSet<JEXEntry> trayMap = entryMap.get(expName);
				// for (String trayName: trayMap.keySet())
				// {
				// // Create the tray label
				// JLabel arrayLabel = new JLabel("     "+trayName);
				// arrayLabel.setFont(FontUtility.boldFont);
				// arrayLabel.setAlignmentX(LEFT_ALIGNMENT);
				// entryPane.add(arrayLabel);
				//
				//
				//
				// // add a spacer for visual purposes
				// entryPane.add(Box.createVerticalStrut(5));
				// }
				//
				// add a spacer for visual purposes
				this.entryPane.add(Box.createVerticalStrut(10));
			}
		}
		this.entryPane.add(Box.createVerticalGlue());
		
		// repaint the main panel
		this.refresh();
		this.entryPane.invalidate();
		this.entryPane.validate();
		this.entryPane.repaint();
	}
	
	/**
	 * Refresh the objectpanel list in case the selection has changed
	 */
	public void refresh()
	{
		Logs.log("Refreshing object Panel", 2, this);
		for (JEXEntryPanelLine o : this.panelList)
		{
			o.refresh();
		}
	}
	
	/**
	 * Create a entry map sorting entries with exp Name and Tray Name
	 * 
	 * @param entries
	 * @return
	 */
	private TreeMap<String,TreeSet<JEXEntry>> sortEntries(TreeSet<JEXEntry> entries)
	{
		// Create the tree map
		TreeMap<String,TreeSet<JEXEntry>> entryMap = new TreeMap<String,TreeSet<JEXEntry>>();
		
		// Loop through the entries and add the entries in the map
		for (JEXEntry entry : entries)
		{
			// Get the keys
			String expName = entry.getEntryExperiment();
			// String arrayname = entry.getEntryTrayName();
			
			// Place the entry in the map and create the objects if necessary
			TreeSet<JEXEntry> trayMap = entryMap.get(expName);
			if(trayMap == null)
			{
				trayMap = new TreeSet<JEXEntry>();
				entryMap.put(expName, trayMap);
			}
			
			trayMap.add(entry);
		}
		
		return entryMap;
	}
	
	// //// ACTIONS
	
	public void doAction(String actionString)
	{}
	
	/**
	 * Call when object list has changed
	 */
	public void entriesChanged()
	{
		this.rebuild();
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{}
	
	public void diplayPanel()
	{}
	
	public void stopDisplayingPanel()
	{}
	
}
