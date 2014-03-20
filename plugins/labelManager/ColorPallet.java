package plugins.labelManager;

import java.awt.Color;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import logs.Logs;
import miscellaneous.CSVList;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class ColorPallet {
	
	public static final String SIG_ColorSelected_Null = "SIG_ColorSelected_Null";
	
	private JPanel panel;
	public static Color[] colors = ColorChip.colors;
	private int size = 30;
	private ColorChip[] chips = new ColorChip[colors.length];
	
	public ColorPallet()
	{
		panel = new JPanel();
		panel.setLayout(new MigLayout("flowx, ins 0, gap 0", "[fill,center,grow]", "[fill,center," + size + "]"));
		panel.setBackground(DisplayStatics.lightBackground);
		initializeColorChips();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	private void initializeColorChips()
	{
		for (int i = 0; i < colors.length; i++)
		{
			chips[i] = new ColorChip();
			chips[i].setColor(i);
			SSCenter.defaultCenter().connect(chips[i], ColorChip.SIG_ColorSelected_IntINDEX, this, "_selectColor", new Class[] { Integer.class });
			this.panel.add(chips[i].panel(), "grow");
		}
	}
	
	public Color getSelectedColor()
	{
		for (ColorChip c : chips)
		{
			if(c.isSelected())
				return c.getColor();
		}
		return null;
	}
	
	public void setSelectedColor(Integer index)
	{
		int count = 0;
		for (ColorChip chip : chips)
		{
			if(count != index)
			{
				chip.setSelected(false);
				chip.panel().repaint();
			}
			else
			{
				chip.setSelected(true);
				chip.panel().repaint();
			}
			count = count + 1;
		}
	}
	
	public void setSelectedColor(String csvColor)
	{
		if(csvColor == null || !hasColor(csvColor))
		{
			this.setSelectedColor(-1);
			Logs.log("Null string or can't find a matching color in the color pallet", 0, this);
		}
		else
		{
			Color c = stringToColor(csvColor); // returns null if csv isn't
			// formed well (uses try loop in
			// conversion)
			if(c == null)
			{
				// Shouldn't occur because first call to hasColor should catch
				// this condition.
				this.setSelectedColor(-1);
				Logs.log("Can't find a matching color in the color pallet", 0, this);
			}
			else
			{
				Logs.log("ColorPallet:setSelectedColor - " + ColorPallet.getColorIndex(c), 0, ColorPallet.class);
				this.setSelectedColor(ColorPallet.getColorIndex(c));
			}
		}
	}
	
	/**
	 * return false if the color is not found
	 * 
	 * @param csvColor
	 * @return
	 */
	public static boolean hasColor(String csvColor)
	{
		Color toMatch = stringToColor(csvColor);
		if(toMatch == null)
			return false;
		for (Color color : colors)
		{
			if(color.equals(toMatch))
				return true;
		}
		return false;
	}
	
	/**
	 * return the color based on the r,g,b value in the csv string. Returns null if something goes wrong in parsing
	 * 
	 * @param csvColor
	 * @return
	 */
	public static Color stringToColor(String csvColor)
	{
		Color c = null;
		try
		{
			CSVList colorCSV = new CSVList(csvColor);
			c = new Color(Integer.parseInt(colorCSV.get(0)), Integer.parseInt(colorCSV.get(1)), Integer.parseInt(colorCSV.get(2)));
		}
		catch (Exception e)
		{
			Logs.log("ColorPallet:stringToColor - Couldn't conver the csv string to a color!", 0, ColorPallet.class);
		}
		return c;
	}
	
	/**
	 * return the color based on the r,g,b value in the csv string. Returns null if c is null.
	 * 
	 * @param csvColor
	 * @return
	 */
	public static String colorToString(Color c)
	{
		if(c == null)
			return null;
		try
		{
			return "" + c.getRed() + "," + c.getGreen() + "," + c.getBlue();
		}
		catch (Exception e)
		{
			Logs.log("ColorPallet:stringToColor - Couldn't conver the csv string to a color!", 0, ColorPallet.class);
		}
		return null;
	}
	
	// Called upon selection of a chip using the signaling center.
	public void _selectColor(Integer index)
	{
		this.setSelectedColor(index);
		SSCenter.defaultCenter().emit(this, SIG_ColorSelected_Null, (Object[]) null);
	}
	
	public static String getColorString(Integer index)
	{
		Color temp = getColor(index);
		return "" + temp.getRed() + "," + temp.getGreen() + "," + temp.getBlue();
	}
	
	public static int getColorIndex(Color color)
	{
		if(color == null)
			return -1;
		int count = 0;
		for (Color c : colors)
		{
			if(c.equals(color))
				return count;
			count = count + 1;
		}
		return -1;
	}
	
	public static Color getColor(Integer index)
	{
		return ColorChip.getColor(index);
	}
	
}
