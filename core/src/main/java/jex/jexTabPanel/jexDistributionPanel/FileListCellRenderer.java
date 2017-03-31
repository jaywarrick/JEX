package jex.jexTabPanel.jexDistributionPanel;

import java.awt.Component;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class FileListCellRenderer extends JLabel implements ListCellRenderer<File> {
	
	private static final long serialVersionUID = 1L;
	public File file = null;
	
	// This is the only method defined by ListCellRenderer.
	// We just reconfigure the JLabel each time we're called.
	
	public Component getListCellRendererComponent(JList<? extends File> list, 
			File value, // value to display
			int index, // cell index
			boolean isSelected, // is the cell selected
			boolean cellHasFocus) // the list and the cell have the focus
	{
		String s = "";
		s = ((File) value).getName();
		file = ((File) value);
		setText(s);
		
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
		setEnabled(list.isEnabled());
		setFont(list.getFont());
		setOpaque(true);
		return this;
	}
}