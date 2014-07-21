package guiObject;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;

public class JRoundedPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	// Variables
	private JPanel contentPane;
	
	private int arcRadius = 10;
	private int spacing = 5;
	private boolean left = false;
	private boolean right = false;
	private boolean top = false;
	private boolean bottom = false;
	
	private Color outterBackGround = DisplayStatics.background;
	private Color innerBackGround = DisplayStatics.lightBackground;
	private Color border = DisplayStatics.dividerColor;
	
	public JRoundedPanel()
	{
		
		// initialize
		initialize();
		refresh();
	}
	
	public void initialize()
	{
		this.setLayout(new BorderLayout());
	}
	
	/**
	 * Set the panel that will be in the center of this rounded panel
	 * 
	 * @param thisContentPane
	 */
	public void setCenterPanel(JPanel thisContentPane)
	{
		this.contentPane = thisContentPane;
		this.refresh();
	}
	
	/**
	 * Refresh the display
	 */
	public void refresh()
	{
		this.setBackground(innerBackGround);
		this.removeAll();
		
		if(!right)
			this.add(Box.createRigidArea(new Dimension(spacing, spacing)), BorderLayout.LINE_END);
		if(!left)
			this.add(Box.createRigidArea(new Dimension(spacing, spacing)), BorderLayout.LINE_START);
		if(!top)
			this.add(Box.createRigidArea(new Dimension(spacing, spacing)), BorderLayout.PAGE_START);
		if(!bottom)
			this.add(Box.createRigidArea(new Dimension(spacing, spacing)), BorderLayout.PAGE_END);
		
		if(contentPane != null)
		{
			this.add(contentPane, BorderLayout.CENTER);
		}
		else
		{
			contentPane = new JPanel();
			contentPane.setBackground(innerBackGround);
			this.add(contentPane, BorderLayout.CENTER);
		}
		
		this.invalidate();
		this.validate();
		this.repaint();
	}
	
	/**
	 * Set wether the centerpanel is close to the egde or with a gap
	 * 
	 * @param left
	 * @param right
	 * @param top
	 * @param bottom
	 */
	public void setcloseSpacing(boolean left, boolean right, boolean top, boolean bottom)
	{
		this.left = left;
		this.right = right;
		this.top = top;
		this.bottom = bottom;
		
		refresh();
	}
	
	/**
	 * Set the background color
	 * 
	 * @param backgroundColor
	 */
	public void setInnerBackgroundColor(Color backgroundColor)
	{
		this.innerBackGround = backgroundColor;
		this.refresh();
	}
	
	/**
	 * Set the background color
	 * 
	 * @param backgroundColor
	 */
	public void setOutterBackgroundColor(Color backgroundColor)
	{
		this.outterBackGround = backgroundColor;
		this.refresh();
	}
	
	/**
	 * Set the arc radius
	 * 
	 * @param arcRadius
	 */
	public void setArc(int arcRadius)
	{
		this.arcRadius = arcRadius;
		this.repaint();
	}
	
	/**
	 * Paint this componement with cool colors
	 */
	@Override
	protected void paintComponent(Graphics g)
	{
		int x = 1;
		int y = 1;
		int w = getWidth() - 3;
		int h = getHeight() - 3;
		
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setColor(outterBackGround);
		g2.fillRect(0, 0, getWidth(), getHeight());
		
		g2.setColor(innerBackGround);
		g2.fillRoundRect(x, y, w, h, arcRadius, arcRadius);
		
		g2.setStroke(new BasicStroke(2f));
		g2.setColor(border);
		g2.drawRoundRect(x, y, w, h, arcRadius, arcRadius);
		
		g2.dispose();
	}
}
