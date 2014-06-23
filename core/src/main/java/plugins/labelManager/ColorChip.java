package plugins.labelManager;

import guiObject.PaintComponentDelegate;
import guiObject.PixelComponentDisplay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import jex.statics.DisplayStatics;
import signals.SSCenter;

public class ColorChip implements PaintComponentDelegate, MouseListener {
	
	public static final String SIG_ColorSelected_IntINDEX = "SIG_ColorSelected_IntINDEX";
	
	private int inset = 4;
	private boolean selected;
	private PixelComponentDisplay panel;
	private Color color = Color.WHITE, line = Color.BLACK;
	private Stroke stroke = new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER);
	
	private static Color R = new Color(255, 150, 150), O = new Color(255, 215, 150), G = new Color(150, 255, 150), C = new Color(200, 255, 255), B = new Color(150, 150, 255), M = new Color(255, 150, 255);
	private static Color LR = new Color(170, 0, 0), LO = new Color(170, 155, 0), LG = new Color(0, 170, 0), LC = new Color(0, 170, 170), LB = new Color(0, 0, 170), LM = new Color(170, 0, 170);
	public static Color[] colors = { R, O, C, G, B, M, LR, LO, LC, LG, LB, LM };
	
	public ColorChip()
	{
		panel = new PixelComponentDisplay(this);
		panel.addMouseListener(this);
	}
	
	public void setColor(int index)
	{
		this.color = getColor(index);
	}
	
	public Color getColor()
	{
		return this.color;
	}
	
	public boolean isSelected()
	{
		return this.selected;
	}
	
	public int getColorIndex()
	{
		int index = 0;
		for (Color c : colors)
		{
			if(this.color.equals(c))
			{
				return index;
			}
			index = index + 1;
		}
		return -1;
	}
	
	public void setSelected(boolean selected)
	{
		this.selected = selected;
		this.panel.repaint();
	}
	
	public PixelComponentDisplay panel()
	{
		return this.panel;
	}
	
	public void paintComponent(Graphics2D g2)
	{
		Rectangle rect = this.panel.getBoundsLocal();
		g2.setColor(DisplayStatics.lightBackground);
		g2.fill(rect);
		rect.height = rect.height - 2 * inset;
		rect.width = rect.width - 2 * inset;
		rect.x = rect.x + inset;
		rect.y = rect.y + inset;
		g2.setColor(color);
		g2.fillRect(rect.x, rect.y, rect.width, rect.height);
		if(selected)
		{
			g2.setStroke(stroke);
			g2.setColor(line);
			g2.drawRect(rect.x, rect.y, rect.width, rect.height);
		}
		g2.dispose();
	}
	
	public void mouseClicked(MouseEvent arg0)
	{}
	
	public void mouseEntered(MouseEvent arg0)
	{}
	
	public void mouseExited(MouseEvent arg0)
	{}
	
	public void mousePressed(MouseEvent arg0)
	{}
	
	public void mouseReleased(MouseEvent arg0)
	{
		SSCenter.defaultCenter().emit(this, SIG_ColorSelected_IntINDEX, new Object[] { new Integer(this.getColorIndex()) });
	}
	
	public static Color getColor(int index)
	{
		index = index % (colors.length);
		return colors[index];
	}
}
