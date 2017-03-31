package preferences;

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * BooleanCellRenderer. <br>
 * 
 */
public class BooleanCellRenderer extends JCheckBox implements TableCellRenderer, ListCellRenderer<Boolean> {
	
	private static final long serialVersionUID = 1L;
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		
		if(isSelected)
		{
			setBackground(table.getSelectionBackground());
			setForeground(table.getSelectionForeground());
		}
		else
		{
			setBackground(table.getBackground());
			setForeground(table.getForeground());
		}
		
		setSelected(Boolean.TRUE.equals(value));
		
		return this;
	}
	
	public Component getListCellRendererComponent(JList<? extends Boolean> list, Boolean value, int index, boolean isSelected, boolean cellHasFocus)
	{
		
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
		
		setSelected(value);
		
		return this;
	}
}
