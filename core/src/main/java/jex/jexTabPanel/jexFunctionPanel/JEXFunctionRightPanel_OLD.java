package jex.jexTabPanel.jexFunctionPanel;

import Database.SingleUserDatabase.tnvi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jex.objectAndEntryPanels.JEXDataPanel;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class JEXFunctionRightPanel_OLD extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	private JEXEntryPanel entryPanel;
	private JEXDataPanel virtualObjectPanel;
	
	private JLabel title1, title2;
	private JScrollPane scroll;
	private JPanel headerPane1, headerPane2;
	
	private JButton resetButton;
	private JButton saveButton;
	
	JEXFunctionRightPanel_OLD()
	{
		initialize();
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(this);
	}
	
	private void initialize()
	{
		// Build the selector header
		title1 = new JLabel("ENTRY RUN LIST");
		title1.setFont(FontUtility.boldFont);
		headerPane1 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane1.setBackground(DisplayStatics.menuBackground);
		headerPane1.add(title1);
		
		// Build the quickSelector
		this.entryPanel = new JEXEntryPanel();
		
		// Build the selector header
		title2 = new JLabel("FUNCTION OUTPUT LIST");
		title2.setFont(FontUtility.boldFont);
		headerPane2 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane2.setBackground(DisplayStatics.menuBackground);
		headerPane2.add(title2);
		
		// Make the buttons
		resetButton = new JButton();
		resetButton.setText("Delete all");
		resetButton.addActionListener(this);
		
		saveButton = new JButton();
		saveButton.setText("Save all");
		saveButton.addActionListener(this);
		
		// Build the objects panel
		virtualObjectPanel = new JEXDataPanel(JEXStatics.jexManager.getTNVI());
		virtualObjectPanel.whenToGetObjects(JEXStatics.jexManager.getTNVI(), tnvi.SIG_ObjectsChanged_NULL, "objectsChanged");
		virtualObjectPanel.whereToGetObjects(JEXStatics.jexManager, "getTNVI");
		
		// Create the scroll panel
		scroll = virtualObjectPanel.scroll();
		scroll.setBorder(BorderFactory.createEmptyBorder());
		
		// Place the objects
		this.setBackground(DisplayStatics.lightBackground);
		this.setLayout(new MigLayout("center,flowy,ins 2", "[center,grow]", "[]1[0:0,fill,grow 50]1[]1[0:0,grow 50]1[]1[]"));
		this.add(headerPane1, "growx");
		this.add(entryPanel, "grow");
		this.add(headerPane2, "growx");
		this.add(scroll, "grow");
		this.add(resetButton, "growx");
		this.add(saveButton, "growx");
		
		rebuild();
	}
	
	public void rebuild()
	{   
		
	}
	
	public void actionPerformed(ActionEvent e)
	{
		// if (e.getSource() == this.resetButton)
		// {
		// JEXStatics.emulatorPool.reset();
		// }
		// else if (e.getSource() == this.saveButton)
		// {
		// // Save all the objects in the temp DB into the real DB
		// JEXStatics.emulatorPool.saveToDatabase();
		//
		// // Delete the temp DB as some objects may not be linked to the proper
		// file anymore
		// JEXStatics.emulatorPool.reset();
		// }
	}
}