package jex.jexTabPanel.jexStatisticsPanel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import logs.Logs;

public class GroupCell extends JPanel implements MouseListener {
	
	private static final long serialVersionUID = 1L;
	private Color background = DisplayStatics.lightBackground;
	private Color temporaryGround = DisplayStatics.lightBackground;
	private Color border = DisplayStatics.dividerColor;
	
	private JLabel title = new JLabel("");
	private String entryID;
	private boolean isSelected = false;
	
	public GroupCell(Group g)
	{
		// this.entryID = g.groupID;
		this.entryID = g.entry.getEntryID();
		if(entryID == null)
		{
			Logs.log("Initializing a cell with null entry ", 2, this);
		}
		else
		{
			Logs.log("Initializing a cell with entry " + entryID, 2, this);
		}
		initialize();
		
		this.setTitle(g.toDisplay);
		rebuild();
	}
	
	/**
	 * Initialize the panel
	 */
	private void initialize()
	{
		this.setBackground(background);
		this.setLayout(new BorderLayout());
		this.addMouseListener(this);
		
		title.setMaximumSize(new Dimension(80, 20));
		title.setAlignmentX(CENTER_ALIGNMENT);
		title.setHorizontalAlignment(JLabel.CENTER);
	}
	
	/**
	 * Refresh the display
	 */
	public void refresh()
	{
		// Determine a color due to a label
		background = DisplayStatics.lightBackground;
		temporaryGround = background;
		
		// Determine a color due to selection
		if(isSelected)
		{
			Logs.log("Setting border color to selected color", 2, this);
			border = DisplayStatics.selectedDividerColor;
			border = new Color(51, 255, 255);
		}
		else
		{
			Logs.log("Setting border color to unselected color", 2, this);
			border = DisplayStatics.dividerColor;
		}
		
		this.repaint();
	}
	
	/**
	 * Set the title of this arraycell
	 * 
	 * @param titleStr
	 */
	public void setTitle(String titleStr)
	{
		title.setText(titleStr);
		title.repaint();
	}
	
	/**
	 * Set the entry contained in this array cell
	 * 
	 * @param entry
	 */
	public void setEntry(String entryID)
	{
		this.entryID = entryID;
	}
	
	/**
	 * Return the entry contained in this array cell
	 * 
	 * @return
	 */
	public String getEntry()
	{
		return this.entryID;
	}
	
	public void setWidth(int width)
	{
		this.setPreferredSize(new Dimension(width + 8, 24));
		this.setMaximumSize(new Dimension(width + 8, 24));
		this.repaint();
	}
	
	/**
	 * Rebuild this arraycell Contacts the Selection manager to get the data typename to display and uses the JEXDataViewFactory to create a sub view
	 */
	public void rebuild()
	{
		JPanel centerPane = new JPanel();
		centerPane.setLayout(new BorderLayout());
		centerPane.setBackground(Color.BLUE);
		
		this.removeAll();
		this.add(title, BorderLayout.CENTER);
		this.add(Box.createRigidArea(new Dimension(4, 4)), BorderLayout.LINE_START);
		this.add(Box.createRigidArea(new Dimension(4, 4)), BorderLayout.LINE_END);
		this.add(Box.createRigidArea(new Dimension(2, 2)), BorderLayout.PAGE_START);
		this.add(Box.createRigidArea(new Dimension(2, 2)), BorderLayout.PAGE_END);
		// this.add(centerPane, BorderLayout.CENTER);
		this.updateUI();
	}
	
	/**
	 * Paint this componement with cool colors
	 */
	protected void paintComponent(Graphics g)
	{
		int x = 1;
		int y = 1;
		int w = getWidth() - 3;
		int h = getHeight() - 3;
		int arc = 10;
		
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(temporaryGround);
		g2.fillRoundRect(x, y, w, h, arc, arc);
		
		g2.setStroke(new BasicStroke(2f));
		g2.setColor(border);
		g2.drawRoundRect(x, y, w, h, arc, arc);
		
		g2.dispose();
	}
	
	public void mouseClicked(MouseEvent e)
	{}
	
	public void mouseEntered(MouseEvent e)
	{
		temporaryGround = DisplayStatics.selectedLightBBackground;
		this.repaint();
	}
	
	public void mouseExited(MouseEvent e)
	{
		temporaryGround = background;
		this.repaint();
	}
	
	public void mousePressed(MouseEvent e)
	{}
	
	public void mouseReleased(MouseEvent e)
	{
		if(isSelected)
		{
			Logs.log("Unselecting " + entryID, 1, this);
			this.isSelected = false;
		}
		else
		{
			Logs.log("Selecting entry " + entryID, 1, this);
			this.isSelected = true;
		}
		
	}
}
