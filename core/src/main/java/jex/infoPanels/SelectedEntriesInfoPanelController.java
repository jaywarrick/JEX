package jex.infoPanels;

import Database.DBObjects.JEXEntry;

import java.awt.Color;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.JEXStatics;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class SelectedEntriesInfoPanelController extends InfoPanelController {
	
	// variables
	TreeSet<JEXEntry> selectedEntries;
	String numberEntries = "0";
	String entryList = "";
	
	public SelectedEntriesInfoPanelController()
	{
		// pass variables
		selectionChanged();
		
		// Sign up to dbInfo change signals
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.SELECTION, this, "selectionChanged");
	}
	
	public void selectionChanged()
	{
		selectedEntries = JEXStatics.jexManager.getSelectedEntries();
		
		// Make number of entries
		numberEntries = (selectedEntries == null) ? "0" : "" + selectedEntries.size();
		
		// Make entry list
		entryList = "";
		if(selectedEntries != null)
		{
			for (JEXEntry entry : selectedEntries)
			{
				entryList = (entryList.equals("")) ? entry.getEntryID() : entryList + "," + entry.getEntryID();
			}
		}
		
		// Refresh the gui
		SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.INFOPANELS_EXP, (Object[]) null);
		SSCenter.defaultCenter().emit(JEXStatics.jexManager, JEXManager.INFOPANELS_ARR, (Object[]) null);
	}
	
	public InfoPanel panel()
	{
		return new SelectedEntriesInfoPanel();
	}
	
	class SelectedEntriesInfoPanel extends InfoPanel {
		
		private static final long serialVersionUID = 1L;
		
		SelectedEntriesInfoPanel()
		{
			// Make the gui
			makePanel();
		}
		
		private void makePanel()
		{
			// make the repository label
			JLabel label1 = new JLabel("# of entries:");
			// label1.setBackground(transparent);
			label1.setForeground(Color.white);
			
			JLabel label2 = new JLabel(numberEntries);
			// label2.setBackground(transparent);
			label2.setForeground(Color.white);
			
			// Make the database label
			JLabel label3 = new JLabel("Database:");
			// label3.setBackground(transparent);
			label3.setForeground(Color.white);
			
			JLabel label4 = new JLabel(entryList);
			label4.setToolTipText(entryList);
			// label4.setBackground(transparent);
			label4.setForeground(Color.white);
			
			// make the center panel
			JPanel centerPane = new JPanel();
			centerPane.setLayout(new MigLayout("ins 0"));
			centerPane.setBackground(InfoPanel.centerPaneBGColor);
			
			centerPane.add(label1, "height 15,width 60");
			centerPane.add(label2, "height 15,width ::160,growx,wrap");
			centerPane.add(label3, "height 15,width 60");
			centerPane.add(label4, "height 15,width ::160,growx,wrap");
			
			// set the info panel
			this.setTitle("Selected entries");
			this.setCenterPanel(centerPane);
		}
	}
}
