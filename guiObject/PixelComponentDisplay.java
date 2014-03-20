package guiObject;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;

public class PixelComponentDisplay extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	// display variables
	private PaintComponentDelegate delegate;
	private PaintComponentDelegate delegate2;
	
	public PixelComponentDisplay(PaintComponentDelegate delegate)
	{
		this.delegate = delegate;
		this.delegate2 = null;
		this.initialize();
	}
	
	public PixelComponentDisplay(PaintComponentDelegate delegate, PaintComponentDelegate delegate2)
	{
		this.delegate = delegate;
		this.delegate2 = delegate2;
		this.initialize();
	}
	
	/**
	 * Initialize panel
	 */
	private void initialize()
	{
		this.setBackground(DisplayStatics.background);
		this.setLayout(new BorderLayout());
		// this.setDoubleBuffered(false);
	}
	
	public void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g;
		this.delegate.paintComponent(g2);
		if(this.delegate2 != null)
		{
			this.delegate2.paintComponent(g2);
		}
	}
	
	public Rectangle getBoundsLocal()
	{
		Rectangle ret = super.getBounds();
		ret.x = 0;
		ret.y = 0;
		return ret;
		
	}
}