package jex.jexTabPanel.jexStatisticsPanel;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Vector;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

/**
 * @version 1.0 11/09/98
 */
public class MultiLineHeaderRenderer extends JList<String> implements TableCellRenderer {
	
	private static final long serialVersionUID = 1L;
	
	public MultiLineHeaderRenderer()
	{
		setOpaque(true);
		setForeground(UIManager.getColor("TableHeader.foreground"));
		setBackground(UIManager.getColor("TableHeader.background"));
		setBorder(UIManager.getBorder("TableHeader.cellBorder"));
		ListCellRenderer<? super String> renderer = getCellRenderer();
		((JLabel) renderer).setHorizontalAlignment(JLabel.CENTER);
		setCellRenderer(renderer);
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		setFont(table.getFont());
		String str = (value == null) ? "" : value.toString();
		BufferedReader br = new BufferedReader(new StringReader(str));
		String line;
		Vector<String> v = new Vector<String>();
		try
		{
			while ((line = br.readLine()) != null)
			{
				v.addElement(line);
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		setListData(v);
		return this;
	}
}