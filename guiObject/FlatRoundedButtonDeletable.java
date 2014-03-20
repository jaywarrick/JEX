package guiObject;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;

public class FlatRoundedButtonDeletable extends JPanel implements MouseListener {
	
	private static final long serialVersionUID = 1L;
	// INTERFACE VARIABLES
	private String name;
	private List<ActionListener> listeners;
	
	// GUI VARIABLES
	public Color background = DisplayStatics.lightBackground;
	public Color normalBack = DisplayStatics.lightBackground;
	public Color mouseOverBack = DisplayStatics.lightMouseOverBackground;
	public Color selectedBack = DisplayStatics.background;
	public Color temporaryGround = normalBack;
	private boolean isSelected = false;
	private boolean enabled = true;
	private JLabel nameLabel;
	
	public FlatRoundedButtonDeletable(String name)
	{
		this.name = name;
		this.initialize();
	}
	
	public void initialize()
	{
		this.setBackground(normalBack);
		// this.setLayout(new FlowLayout());
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.setAlignmentX(CENTER_ALIGNMENT);
		
		nameLabel = new JLabel(name);
		nameLabel.setFont(FontUtility.defaultFonts);
		nameLabel.setAlignmentY(CENTER_ALIGNMENT);
		// nameLabel.setAlignmentY(TOP_ALIGNMENT);
		nameLabel.setVerticalTextPosition(SwingConstants.TOP);
		nameLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		nameLabel.setBackground(Color.blue);
		
		this.add(Box.createHorizontalStrut(7));
		this.add(nameLabel);
		this.add(Box.createHorizontalStrut(7));
		this.addMouseListener(this);
		
		listeners = new ArrayList<ActionListener>(0);
	}
	
	public String getText()
	{
		return this.name;
	}
	
	/**
	 * Set the status of this button
	 * 
	 * @param b
	 */
	public void setPressed(boolean b)
	{
		this.isSelected = b;
		refresh();
	}
	
	/**
	 * Return whether the button is selected or not
	 * 
	 * @return status of the button
	 */
	public boolean isPressed()
	{
		return this.isSelected;
	}
	
	/**
	 * Enable / disable unselection of the button
	 * 
	 * @param enabled
	 */
	public void enableUnselection(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	/**
	 * Add an action listener to this button
	 * 
	 * @param id
	 */
	public void addActionListener(ActionListener id)
	{
		listeners.add(id);
	}
	
	private void refresh()
	{
		if(isSelected)
		{
			temporaryGround = selectedBack;
		}
		else
		{
			temporaryGround = normalBack;
		}
		this.repaint();
		// this.updateUI();
	}
	
	/**
	 * Paint this componement with cool colors
	 */
	@Override
	protected void paintComponent(Graphics g)
	{
		int x = 2;
		int y = 2;
		int w = getWidth() - 3;
		int h = getHeight() - 3;
		int arc = 15;
		
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(background);
		g2.fillRect(x, y, w, h);
		
		g2.setColor(temporaryGround);
		g2.fillRoundRect(x, y, w, h, arc, arc);
		
		g2.setColor(Color.black);
		g2.drawString("x", w - 7, 2);
		
		g2.dispose();
	}
	
	public void mouseClicked(MouseEvent e)
	{}
	
	public void mouseEntered(MouseEvent e)
	{
		// Point p = e.getPoint();
		Point p = e.getPoint();
		if(p.x < this.getWidth() - 8)
			return;
		
		if(!isSelected)
		{
			temporaryGround = mouseOverBack;
			this.repaint();
		}
	}
	
	public void mouseExited(MouseEvent e)
	{
		if(isSelected)
			temporaryGround = selectedBack;
		else
			temporaryGround = normalBack;
		this.repaint();
	}
	
	public void mousePressed(MouseEvent e)
	{
		if(!isSelected)
		{
			temporaryGround = selectedBack;
			this.repaint();
		}
	}
	
	public void mouseReleased(MouseEvent e)
	{
		Point p = e.getPoint();
		if(p.x < this.getWidth() - 8)
			return;
		
		if(enabled)
			isSelected = !isSelected;
		
		ActionEvent event = new ActionEvent(this, 0, null);
		for (ActionListener id : listeners)
		{
			id.actionPerformed(event);
		}
		
		if(isSelected)
		{
			temporaryGround = selectedBack;
		}
		else
		{
			temporaryGround = mouseOverBack;
		}
		this.repaint();
	}
	
}
