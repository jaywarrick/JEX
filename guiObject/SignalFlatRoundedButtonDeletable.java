package guiObject;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jex.statics.DisplayStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import signals.SSCenter;

public class SignalFlatRoundedButtonDeletable extends JPanel implements MouseListener {
	
	private static final long serialVersionUID = 1L;
	
	// statics
	public static final String SIG_ButtonClicked_NULL = "clicked";
	
	// INTERFACE VARIABLES
	private String name;
	
	// GUI VARIABLES
	public Color background = DisplayStatics.lightBackground;
	public Color normalBack = DisplayStatics.lightBackground;
	public Color mouseOverBack = DisplayStatics.lightMouseOverBackground;
	public Color selectedBack = DisplayStatics.background;
	public Color temporaryGround = normalBack;
	public Font font = FontUtility.defaultFonts;
	private boolean isSelected = false;
	private boolean enabled = true;
	private JLabel nameLabel;
	
	public SignalFlatRoundedButtonDeletable(String name)
	{
		this.name = name;
		this.addMouseListener(this);
		
		// initialize
		this.initialize();
	}
	
	public void initialize()
	{
		this.setBackground(normalBack);
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.setAlignmentX(CENTER_ALIGNMENT);
		
		nameLabel = new JLabel(name);
		nameLabel.setFont(font);
		nameLabel.setAlignmentY(CENTER_ALIGNMENT);
		nameLabel.setVerticalTextPosition(SwingConstants.TOP);
		nameLabel.setHorizontalTextPosition(SwingConstants.CENTER);
		nameLabel.setBackground(Color.blue);
		
		this.add(Box.createHorizontalGlue());
		this.add(nameLabel);
		this.add(Box.createHorizontalGlue());
		this.addMouseListener(this);
	}
	
	/**
	 * Return the text in the button
	 * 
	 * @return
	 */
	public String getText()
	{
		return this.name;
	}
	
	/**
	 * Set the font of the label
	 */
	public void setLabelFont(Font font)
	{
		this.font = font;
		nameLabel.setFont(font);
		this.repaint();
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
	 * Refresh
	 */
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
	}
	
	/**
	 * Paint this componement with cool colors
	 */
	@Override
	protected void paintComponent(Graphics g)
	{
		
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(background);
		g2.fillRect(0, 0, this.getWidth(), this.getHeight());
		
		int x = 2;
		int y = 2;
		int w = getWidth() - 3;
		int h = getHeight() - 3;
		int arc = 15;
		
		g2.setColor(background);
		g2.fillRect(x, y, w, h);
		
		g2.setColor(temporaryGround);
		g2.fillRoundRect(x, y, w, h, arc, arc);
		
		// g2.setColor(Color.black);
		// g2.drawString("x", w-7, 2);
		
		g2.dispose();
	}
	
	public void mouseClicked(MouseEvent e)
	{}
	
	public void mouseEntered(MouseEvent e)
	{
		// Point p = e.getPoint();
		
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
		// Point p = e.getPoint();
		
		if(enabled)
			isSelected = !isSelected;
		
		if(isSelected)
		{
			temporaryGround = selectedBack;
		}
		else
		{
			temporaryGround = mouseOverBack;
		}
		this.repaint();
		
		SSCenter.defaultCenter().emit(this, SIG_ButtonClicked_NULL, (Object[]) null);
		Logs.log("Button clicked", 1, this);
	}
	
}
