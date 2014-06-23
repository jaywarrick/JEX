package jex.jexTabPanel.jexFunctionPanel;

import guiObject.FlatRoundedStaticButton;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import Database.DBObjects.JEXEntry;

public class JEXEntryPanelLine extends JPanel implements ActionListener, MouseListener {
	
	private static final long serialVersionUID = 1L;
	
	// Input Panel
	private FlatRoundedStaticButton viewButton;
	private FlatRoundedStaticButton removeButton;
	
	// Class Variables
	private JEXEntry entry;
	private Color foregroundColor = DisplayStatics.lightBackground;
	
	public JEXEntryPanelLine(JEXEntry entry)
	{
		this.entry = entry;
		
		// Connect to the label selection listener
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.VIEWEDENTRY, this, "refresh", (Class[]) null);
		Logs.log("Connected to database object selection signal", 2, this);
		
		initialize();
		refresh();
	}
	
	/**
	 * Initialize the panel
	 */
	private void initialize()
	{
		this.setLayout(new MigLayout("flowx, ins 0 10 0 0", "[fill,grow]10[]10[]", "[]"));
		this.setBackground(DisplayStatics.lightBackground);
		
		viewButton = new FlatRoundedStaticButton("View");
		viewButton.enableUnselection(false);
		viewButton.addActionListener(this);
		
		removeButton = new FlatRoundedStaticButton("Remove");
		removeButton.enableUnselection(false);
		removeButton.addActionListener(this);
	}
	
	/**
	 * Refresh the panel
	 */
	public void refresh()
	{
		this.removeAll();
		this.setMaximumSize(new Dimension(800, 20));
		this.setMinimumSize(new Dimension(40, 20));
		
		JEXEntry selectedEntry = JEXStatics.jexManager.getViewedEntry();
		if(selectedEntry == null || !selectedEntry.equals(entry))
		{
			viewButton.normalBack = foregroundColor;
			viewButton.setPressed(false);
		}
		else
		{
			viewButton.normalBack = DisplayStatics.dividerColor;
			viewButton.setPressed(true);
		}
		
		String labelStr = "          " + entry.getTrayX() + " - " + entry.getTrayY();
		JLabel label = new JLabel(labelStr);
		label.setFont(FontUtility.defaultFont);
		label.setBackground(DisplayStatics.lightBackground);
		label.setOpaque(true);
		this.add(label, "growx, width 25:25:");
		this.add(removeButton.panel());
		this.add(viewButton.panel());
		
		this.updateUI();
	}
	
	/**
	 * Return the typename in this object panel
	 * 
	 * @return typename
	 */
	public JEXEntry getEntry()
	{
		return this.entry;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == viewButton)
		{
			JEXEntry selectedEntry = JEXStatics.jexManager.getViewedEntry();
			if(selectedEntry == null || !selectedEntry.equals(entry))
			{
				JEXStatics.jexManager.setViewedEntry(entry);
			}
			else
			{
				JEXStatics.jexManager.setViewedEntry(null);
			}
		}
		else if(e.getSource() == removeButton)
		{
			JEXStatics.jexManager.removeEntryFromSelection(entry);
		}
	}
	
	public void mouseClicked(MouseEvent e)
	{}
	
	public void mouseEntered(MouseEvent e)
	{}
	
	public void mouseExited(MouseEvent e)
	{}
	
	public void mousePressed(MouseEvent e)
	{}
	
	public void mouseReleased(MouseEvent e)
	{}
	
}
