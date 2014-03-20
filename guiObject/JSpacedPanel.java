package guiObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;

public class JSpacedPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	// Variables
	// private Color backgroundColor = DisplayStatics.background;
	private Color backgroundColor = DisplayStatics.background;
	private Color foregroundColor = DisplayStatics.lightBackground;
	
	// Hidden variables
	private JPanel thisContentPane = new JPanel();
	private JPanel menuPanel;
	private JPanel statusbar;
	private JPanel centerPane;
	private JPanel leftPane;
	private boolean left = false;
	private boolean right = false;
	private boolean top = false;
	private boolean bottom = false;
	private int sideSpace = 5;
	
	public JSpacedPanel()
	{
		initiate();
	}
	
	protected void initiate()
	{
		this.setLayout(new BorderLayout());
		this.setBackground(backgroundColor);
		thisContentPane.setLayout(new BorderLayout());
		thisContentPane.setBackground(foregroundColor);
		
		this.add(thisContentPane, BorderLayout.CENTER);
		if(!right)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.LINE_END);
		if(!left)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.LINE_START);
		if(!top)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.PAGE_START);
		if(!bottom)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.PAGE_END);
	}
	
	/**
	 * Set the menu panel on top of the panel
	 * 
	 * @param menuPanel
	 */
	public void setMenuPanel(JPanel menuPanel)
	{
		this.menuPanel = menuPanel;
		this.refresh();
	}
	
	/**
	 * Set the status bar at the bottom of the panel
	 * 
	 * @param statusbar
	 */
	public void setStatus(JPanel statusbar)
	{
		this.statusbar = statusbar;
		this.refresh();
	}
	
	/**
	 * Set the center panel of the panel
	 * 
	 * @param centerPane
	 */
	public void setCenterPanel(JPanel centerPane)
	{
		this.centerPane = centerPane;
		this.refresh();
	}
	
	/**
	 * Set the left menu panel of the panel
	 * 
	 * @param leftPane
	 */
	public void setLeftPanel(JPanel leftPane)
	{
		this.leftPane = leftPane;
		this.refresh();
	}
	
	/**
	 * Refresh the display
	 */
	public void refresh()
	{
		thisContentPane.removeAll();
		if(menuPanel != null)
			thisContentPane.add(menuPanel, BorderLayout.PAGE_START);
		if(centerPane != null)
			thisContentPane.add(centerPane, BorderLayout.CENTER);
		if(statusbar != null)
			thisContentPane.add(statusbar, BorderLayout.PAGE_END);
		if(leftPane != null)
			thisContentPane.add(leftPane, BorderLayout.LINE_START);
		this.repaint();
	}
	
	/**
	 * Set the spacing rules on each side of the panel
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
		
		this.removeAll();
		this.add(thisContentPane, BorderLayout.CENTER);
		if(!right)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.LINE_END);
		if(!left)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.LINE_START);
		if(!top)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.PAGE_START);
		if(!bottom)
			this.add(Box.createRigidArea(new Dimension(sideSpace, sideSpace)), BorderLayout.PAGE_END);
		
		this.repaint();
	}
}
