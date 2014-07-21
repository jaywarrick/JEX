package jex.jexTabPanel.jexStatisticsPanel;

import Database.DBObjects.JEXData;
import Database.Definition.TypeName;
import guiObject.FlatRoundedStaticButton;
import guiObject.TypeNameButton;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class ValuePanel extends JPanel implements ActionListener, MouseListener {
	
	private static final long serialVersionUID = 1L;
	
	// Input Panel
	private TypeNameButton dataButton;
	private FlatRoundedStaticButton viewButton;
	private boolean isViewed = false;
	
	// Class Variables
	private ValueListPanel parent;
	private String valueName;
	
	public ValuePanel(String valueName, ValueListPanel parent)
	{
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.STATSVALUEOBJ, this, "valueViewedChange", (Class[]) null);
		
		this.valueName = valueName;
		this.parent = parent;
		
		initialize();
		refresh();
	}
	
	/**
	 * Initialize the panel
	 */
	private void initialize()
	{
		this.setLayout(new MigLayout("flowx, ins 0", "[24]5[fill,grow]5[]", "[0:0,24]"));
		this.setBackground(DisplayStatics.lightBackground);
		this.addMouseListener(this);
		
		viewButton = new FlatRoundedStaticButton("View");
		viewButton.enableUnselection(false);
		viewButton.addActionListener(this);
		
		dataButton = new TypeNameButton(new TypeName(JEXData.VALUE, this.valueName));
	}
	
	public void valueViewedChange()
	{
		TypeName tn = JEXStatics.jexManager.getSelectedStatisticsObject();
		if(tn == null)
			setIsViewed(false);
		else
		{
			if(tn.getType() == null || tn.getName() == null)
				setIsViewed(false);
			else if(tn.getType().equals(JEXData.VALUE) && tn.getName().equals(valueName))
				setIsViewed(true);
			else
				setIsViewed(false);
		}
	}
	
	/**
	 * Set whether this object is ticked for view or not
	 * 
	 * @param isTicked
	 */
	public void setIsViewed(boolean isViewed)
	{
		this.isViewed = isViewed;
		if(!this.isViewed)
		{
			viewButton.setPressed(false);
		}
		else
		{
			viewButton.setPressed(true);
		}
	}
	
	/**
	 * Refresh the panel
	 */
	public void refresh()
	{
		
		this.removeAll();
		
		if(!this.isViewed)
		{
			viewButton.setPressed(false);
		}
		else
		{
			viewButton.setPressed(true);
		}
		
		this.add(dataButton);
		this.add(new JLabel(valueName), "growx, width 25:25:");
		this.add(viewButton.panel());
		
		this.revalidate();
		this.repaint();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == viewButton)
		{
			if(!this.isViewed)
			{
				parent.setViewedObject(valueName);
			}
			else
			{
				parent.setViewedObject(null);
			}
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
