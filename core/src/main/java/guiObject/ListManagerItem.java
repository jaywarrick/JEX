package guiObject;

import Database.DBObjects.JEXData;
import Database.Definition.TypeName;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;

public class ListManagerItem extends JPanel implements ActionListener, MouseListener {
	
	private static final long serialVersionUID = 1L;
	
	// variables
	private String itemName;
	private ListManager parent;
	private boolean viewed = false;
	
	// gui
	private TypeNameButton dataButton;
	private FlatRoundedStaticButton viewButton;
	private Color foregroundColor = DisplayStatics.lightBackground;
	
	public ListManagerItem(String roiName, ListManager parent)
	{
		this.itemName = roiName;
		this.parent = parent;
		initialize();
	}
	
	/**
	 * Initialize the panel
	 */
	private void initialize()
	{
		this.setLayout(new MigLayout("flowx, ins 0", "[24]5[fill,grow]5[]", "[0:0,24]"));
		this.setBackground(foregroundColor);
		
		viewButton = new FlatRoundedStaticButton("View");
		viewButton.enableUnselection(false);
		viewButton.addActionListener(this);
		
		dataButton = new TypeNameButton(new TypeName(JEXData.ROI, itemName));
		
		this.add(dataButton);
		this.add(new JLabel(itemName), "growx, width 25:25:");
		this.add(viewButton.panel());
	}
	
	/**
	 * Get the ROI name
	 * 
	 * @return
	 */
	public String getItemName()
	{
		return this.itemName;
	}
	
	/**
	 * Set the viewing status of the ROI
	 * 
	 * @param viewed
	 */
	public void setViewed(boolean viewed)
	{
		this.viewed = viewed;
		if(viewed)
		{
			viewButton.normalBack = DisplayStatics.dividerColor;
			viewButton.setPressed(true);
		}
		else
		{
			viewButton.normalBack = foregroundColor;
			viewButton.setPressed(false);
		}
	}
	
	/**
	 * Return the viewing status of the ROI
	 * 
	 * @return
	 */
	public boolean isViewed()
	{
		return this.viewed;
	}
	
	/**
	 * Handle button clicks
	 */
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == viewButton)
		{
			if(viewed)
			{
				Logs.log("Unselecting Item " + itemName, 1, this);
				parent._setSelectedItem(null);
			}
			else
			{
				Logs.log("Selecting Item " + itemName, 1, this);
				parent._setSelectedItem(this.itemName);
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
	
	public String toString()
	{
		return this.itemName;
	}
	
}
