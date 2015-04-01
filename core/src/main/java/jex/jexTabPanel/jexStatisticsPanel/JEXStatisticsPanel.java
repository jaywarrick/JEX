package jex.jexTabPanel.jexStatisticsPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import signals.SSCenter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ValueReader;
import Database.Definition.DimensionGroupMap;
import Database.Definition.TypeName;

public class JEXStatisticsPanel extends JPanel implements ListSelectionListener {
	
	private static final long serialVersionUID = 1L;
	private DefaultTableModel model;
	private JTable table;
	private HashMap<Integer,HashMap<Integer,JEXEntry>> entryTable;
	
	public JEXStatisticsPanel()
	{
		// Connect to changes in the filtering / value to be displayed
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.STATSRESULTS, this, "statsChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.STATSVALUEOBJ, this, "statsChanged", (Class[]) null);
		
		this.initialize();
		this.makeTable();
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		SSCenter.defaultCenter().disconnect(this);
	}
	
	private void initialize()
	{
		this.model = new DefaultTableModel();
		
		this.setBackground(DisplayStatics.background);
		this.setLayout(new BorderLayout());
	}
	
	public void statsChanged()
	{
		this.makeTable();
		this.makeGUI();
	}
	
	public DefaultTableModel model()
	{
		if(this.model == null)
		{
			return new DefaultTableModel();
		}
		return this.model;
	}
	
	public void makeGUI()
	{
		Logs.log("Rebuilding the stats panel", 1, this);
		
		this.table = new JTable();
		this.table.setModel(this.model());
		this.table.setBackground(DisplayStatics.background);
		this.table.setForeground(Color.white);
		this.table.setBorder(BorderFactory.createEmptyBorder());
		this.table.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.table.setCellSelectionEnabled(true);
		
		// add the listeners
		// table.getColumnModel().getSelectionModel().addListSelectionListener(this);
		this.table.getSelectionModel().addListSelectionListener(this);
		
		MultiLineHeaderRenderer renderer = new MultiLineHeaderRenderer();
		Enumeration<TableColumn> columns = this.table.getColumnModel().getColumns();
		while (columns.hasMoreElements())
		{
			columns.nextElement().setHeaderRenderer(renderer);
		}
		
		if(this.table.getColumnCount() < 5)
		{
			this.table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		}
		else
		{
			this.table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			javax.swing.table.TableColumn column = null;
			for (int i = 0; i < this.table.getColumnCount(); i++)
			{
				column = this.table.getColumnModel().getColumn(i);
				column.setPreferredWidth(120);
			}
		}
		
		JScrollPane scrollPane = new JScrollPane(this.table);
		scrollPane.setBackground(DisplayStatics.background);
		scrollPane.getViewport().setBackground(DisplayStatics.background);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		this.table.setFillsViewportHeight(true);
		
		// Display the table
		this.removeAll();
		this.add(scrollPane, BorderLayout.CENTER);
		this.invalidate();
		this.validate();
		this.repaint();
	}
	
	public void makeTable()
	{
		Logs.log("Rebuilding the stats table", 1, this);
		
		TreeMap<DimensionGroupMap,Set<JEXEntry>> groupedEntries = JEXStatics.jexManager.getStatisticsGroupedEntries();
		TreeMap<DimensionGroupMap,List<Group>> dataTable = new TreeMap<DimensionGroupMap,List<Group>>();
		
		// Max dimension of the data column
		int datalength = 0;
		
		// Fill the data table
		for (DimensionGroupMap dim : groupedEntries.keySet())
		{
			
			// Get the selected data object to represent
			TypeName selected = JEXStatics.jexManager.getSelectedStatisticsObject();
			
			// Get the list of entries in this group
			Set<JEXEntry> entries = groupedEntries.get(dim);
			
			// Get the data out of the entry list
			List<Group> data = this.extractData(selected, entries);
			
			// Update the dimension length
			datalength = Math.max(data.size(), datalength);
			
			// Add the data to the table
			dataTable.put(dim, data);
		}
		
		// Fill the table model
		this.model = new DefaultTableModel();
		int j = 0;
		
		// -----------------------------------
		// Create a column with all the headers
		Vector<String> headerColumnData = new Vector<String>(0);
		
		// Make the column grouping display
		// if the table is empty, skip this step
		if(dataTable != null && dataTable.keySet().iterator().hasNext())
		{
			// Get the first group
			DimensionGroupMap groupList = dataTable.keySet().iterator().next();
			for (TypeName tn : groupList.keySet())
			{
				String tnStr = tn.getName();
				headerColumnData.add(tnStr);
			}
			headerColumnData.add("");
			
			// Add empty spaces so that all the statistics appear at the same
			// level
			for (int i = 0; i < datalength; i++)
			{
				headerColumnData.add("" + i);
			}
			
			headerColumnData.add("");
			headerColumnData.add("----");
			headerColumnData.add("Mean");
			headerColumnData.add("St. Dev.");
			headerColumnData.add("Min");
			headerColumnData.add("Max");
			
			// Make the JTabel column
			this.model.addColumn("Table", headerColumnData);
		}
		
		// -----------------------------------
		// Add the columns with the data
		j = 0;
		this.entryTable = new HashMap<Integer,HashMap<Integer,JEXEntry>>();
		for (DimensionGroupMap dim : dataTable.keySet())
		{
			// Get the data column
			List<Group> data = dataTable.get(dim);
			
			// Make the column to display
			Vector<String> columnData = new Vector<String>(0);
			List<String> values = new ArrayList<String>(0);
			
			// Make the column grouping display
			for (TypeName tn : dim.keySet())
			{
				String value = dim.get(tn);
				columnData.add(value);
			}
			columnData.add("");
			
			// Add the values
			int index = 0;
			for (Group g : data)
			{
				columnData.add(g.toDisplay);
				values.add(g.toDisplay);
				
				// put into the entry table
				HashMap<Integer,JEXEntry> row = this.entryTable.get(index);
				
				// if row table is null add it
				if(row == null)
				{
					row = new HashMap<Integer,JEXEntry>();
					this.entryTable.put(index, row);
				}
				
				// Add the column entry
				row.put(j, g.entry);
				index++;
			}
			
			// Add empty spaces so that all the statistics appear at the same
			// level
			for (int i = data.size() - 1; i < datalength; i++)
			{
				columnData.add("");
			}
			
			// Add minmax to the data in the column
			List<String> minmax = calculateStatistics(values);
			for (String str : minmax)
			{
				columnData.add(str);
			}
			
			// Make the multiLine column header
			String columnHeader = "Group " + j;
			j++;
			
			// Make the JTabel column
			this.model.addColumn(columnHeader, columnData);
		}
	}
	
	public List<Group> extractData(TypeName tn, Set<JEXEntry> entries)
	{
		List<Group> result = new ArrayList<Group>(0);
		
		if(entries == null)
		{
			return result;
		}
		for (JEXEntry entry : entries)
		{
			Group g = null;
			if(tn == null || tn.getName() == null || tn.getType() == null)
			{
				// g = new Group(entry.getEntryID(),"","-");
				g = new Group(entry, "", "-");
			}
			else if(!tn.getType().equals(JEXData.VALUE))
			{
				// g = new Group(entry.getEntryID(),"","-");
				g = new Group(entry, "", "-");
			}
			else
			{
				JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(tn, entry);
				if(data == null)
				{
					continue;
				}
				String value = ValueReader.readValueObject(data);
				String vTod = (value == null) ? "-" : value;
				// g = new Group(entry.getEntryID(),"",vTod);
				g = new Group(entry, "", vTod);
			}
			
			result.add(g);
		}
		
		return result;
	}
	
	/**
	 * Return the statistics on a vector of strings v, in order min, max, mean and stdDev
	 */
	public static List<String> calculateStatistics(List<String> v)
	{
		if(v.size() == 0)
		{
			List<String> result = new ArrayList<String>(0);
			result.add("-----");
			result.add("-");
			result.add("-");
			result.add("-");
			result.add("-");
			return result;
		}
		
		DescriptiveStatistics statVect = new DescriptiveStatistics(v.size());
		String nullString = "-";
		for (int i = 0, len = v.size(); (i < len); i++)
		{
			String str = v.get(i);
			if(str == null || str.equals("-"))
			{
				v.set(i, nullString);
				continue;
			}
			try
			{
				double d = Double.parseDouble(v.get(i));
				statVect.addValue(d);
			}
			catch (NumberFormatException e)
			{
				continue;
			}
		}
		Double min = statVect.getMin();
		Double max = statVect.getMax();
		Double mean = statVect.getMean();
		Double variance = statVect.getVariance();
		Double stdDev = Math.sqrt(variance);
		
		List<String> result = new ArrayList<String>(0);
		result.add("-----");
		result.add("" + mean.toString());
		result.add("" + stdDev.toString());
		result.add("" + min.toString());
		result.add("" + max.toString());
		return result;
	}
	
	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		int col = this.table.getSelectedColumn();
		int row = this.table.getSelectedRow();
		if(row != -1 && col != -1)
		{
			// Get entry at that location in the table
			HashMap<Integer,JEXEntry> r = this.entryTable.get(row - 1);
			if(r != null)
			{
				JEXEntry selectedEntry = r.get(col - 1);
				if(selectedEntry != null)
				{
					Logs.log("Clicked on cell " + row + " - " + col + "... Found entry " + selectedEntry.getEntryID(), 1, this);
					// new EntryViewer(selectedEntry);
				}
			}
		}
	}
	
	class TableEntryCellRenderer extends DefaultTableCellRenderer {
		
		private static final long serialVersionUID = 1L;
		
		public TableEntryCellRenderer()
		{   
			
		}
		
		@Override
		public Component getTableCellRendererComponent(JTable tree, Object value, boolean sel, boolean hasFocus, int row, int col)
		{
			
			if(value instanceof String)
			{
				return super.getTableCellRendererComponent(JEXStatisticsPanel.this.table, value, sel, hasFocus, row, col);
			}
			else if(value instanceof JEXEntry)
			{
				TypeName tn = JEXStatics.jexManager.getSelectedObject();
				String valueStr = "-";
				if(tn.getType().equals(JEXData.VALUE))
				{
					JEXData data = JEXStatics.jexManager.getDataOfTypeNameInEntry(tn, ((JEXEntry) value));
					if(data != null)
					{
						valueStr = ValueReader.readValueObject(data);
						valueStr = (valueStr == null) ? "-" : valueStr;
					}
				}
				
				return super.getTableCellRendererComponent(JEXStatisticsPanel.this.table, valueStr, sel, hasFocus, row, col);
			}
			
			return this;
		}
		
	}
}
