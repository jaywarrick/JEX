package preferences;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;
import javax.swing.UIManager;

/**
 * ColorCellRenderer. <br>
 * 
 */
public class ColorCellRenderer extends DefaultCellRenderer {
	
	private static final long serialVersionUID = 1L;
	
	public static String toHex(Color color)
	{
		String red = Integer.toHexString(color.getRed());
		String green = Integer.toHexString(color.getGreen());
		String blue = Integer.toHexString(color.getBlue());
		
		if(red.length() == 1)
		{
			red = "0" + red;
		}
		if(green.length() == 1)
		{
			green = "0" + green;
		}
		if(blue.length() == 1)
		{
			blue = "0" + blue;
		}
		return ("#" + red + green + blue).toUpperCase();
	}
	
	@Override
	protected String convertToString(Object value)
	{
		if(value == null)
		{
			return null;
		}
		
		Color color = (Color) value;
		return "R:" + color.getRed() + " G:" + color.getGreen() + " B:" + color.getBlue() + " - " + toHex(color);
	}
	
	@Override
	protected Icon convertToIcon(Object value)
	{
		if(value == null)
		{
			return null;
		}
		
		return new ColorIcon((Color) value);
	}
	
	private static class ColorIcon implements Icon {
		
		private Color color;
		
		public ColorIcon(Color color)
		{
			this.color = color;
		}
		
		public int getIconHeight()
		{
			return 10;
		}
		
		public int getIconWidth()
		{
			return 20;
		}
		
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			Color oldColor = g.getColor();
			
			if(color != null)
			{
				g.setColor(color);
				g.fillRect(x, y, getIconWidth(), getIconHeight());
			}
			
			g.setColor(UIManager.getColor("controlDkShadow"));
			g.drawRect(x, y, getIconWidth(), getIconHeight());
			
			g.setColor(oldColor);
		}
	}
}
