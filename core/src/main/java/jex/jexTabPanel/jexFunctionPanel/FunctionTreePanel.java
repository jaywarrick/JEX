package jex.jexTabPanel.jexFunctionPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Scrollable;

public class FunctionTreePanel extends JPanel implements Scrollable {
	
	private static final long serialVersionUID = 1L;
	
	public FunctionTreePanel(FunctionTree functionTree)
	{
		super();
		this.setBackground(Color.WHITE);
		this.setLayout(new BorderLayout());
		this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.add(functionTree);
	}
	
	public void initialize()
	{   
		
	}
	
	public Dimension getPreferredScrollableViewportSize()
	{
		return this.getPreferredSize();
	}
	
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return this.getScrollableUnitIncrement(visibleRect, orientation, direction) * 5;
	}
	
	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}
	
	public boolean getScrollableTracksViewportWidth()
	{
		return false;
	}
	
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
	{
		return 15;
	}
	
}
