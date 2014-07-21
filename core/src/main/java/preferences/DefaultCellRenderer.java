package preferences;

import java.awt.Color;
import java.awt.Component;
import java.awt.SystemColor;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * DefaultCellRenderer.<br>
 * 
 */
public class DefaultCellRenderer extends DefaultTableCellRenderer implements ListCellRenderer {
	
	private static final long serialVersionUID = 1L;
	
	private ObjectRenderer objectRenderer = new DefaultObjectRenderer();
	
	private Color oddBackgroundColor = SystemColor.window;
	private Color evenBackgroundColor = SystemColor.window;
	private boolean showOddAndEvenRows = true;
	
	public void setOddBackgroundColor(Color c)
	{
		oddBackgroundColor = c;
	}
	
	public void setEvenBackgroundColor(Color c)
	{
		evenBackgroundColor = c;
	}
	
	public void setShowOddAndEvenRows(boolean b)
	{
		showOddAndEvenRows = b;
	}
	
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		
		setBorder(null);
		
		if(isSelected)
		{
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
		}
		else
		{
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		
		setValue(value);
		
		return this;
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		
		if(showOddAndEvenRows && !isSelected)
		{
			if(row % 2 == 0)
			{
				setBackground(oddBackgroundColor);
			}
			else
			{
				setBackground(evenBackgroundColor);
			}
		}
		
		setValue(value);
		
		return this;
	}
	
	@Override
	public void setValue(Object value)
	{
		String text = convertToString(value);
		Icon icon = convertToIcon(value);
		
		setText(text == null ? "" : text);
		setIcon(icon);
	}
	
	protected String convertToString(Object value)
	{
		return objectRenderer.getText(value);
	}
	
	protected Icon convertToIcon(Object value)
	{
		return null;
	}
	
}
