package plugins.viewer;

import guiObject.PaintComponentDelegate;
import guiObject.PixelComponentDisplay;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;

public class StatusBar implements PaintComponentDelegate {
	
	private PixelComponentDisplay panel;
	private String text;
	
	public StatusBar()
	{
		this.panel = new PixelComponentDisplay(this);
		this.panel.setBackground(DisplayStatics.background);
		this.text = "";
	}
	
	public void setText(String text)
	{
		this.text = text;
		this.panel.repaint();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void paintComponent(Graphics2D g2)
	{
		g2.setColor(DisplayStatics.background);
		g2.fill(this.panel.getBoundsLocal());
		
		g2.setColor(Color.WHITE);
		g2.drawString(this.text, 5, 10);
	}
	
}
