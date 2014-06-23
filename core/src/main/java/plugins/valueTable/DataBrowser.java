package plugins.valueTable;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import jex.statics.DisplayStatics;
import signals.SSCenter;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

public class DataBrowser implements Scrollable {
	
	public static final long serialVersionUID = 1L;
	
	// static
	public static final String SIG_VALUEDimensionMapChanged_NULL = "SIG_VALUEDimensionMapChanged_NULL";
	public static final String SIG_VALUESmashedDimTableChanged_NULL = "SIG_VALUESmashedDimTableChanged_NULL";
	public static final String SIG_VALUEScrollFinished_NULL = "SIG_VALUEScrollFinished_NULL";
	public static final String SIG_EntryChanged_NULL = "SIG_EntryChanged_NULL";
	public static final String SIG_SmashedEntriesChanged_NULL = "SIG_SmashedEntriesChanged_NULL";
	public static final String SIG_EntryScrollFinished_NULL = "SIG_EntryScrollFinished_NULL";
	
	public static final String SIG_ScrollStarted_NULL = "SIG_ScrollStarted_NULL";
	
	// GUI variable
	private DimTableAdjuster imageBars;
	private DimScrollBar entryBar;
	private JPanel panel;
	private boolean entryBarIsVisible;
	
	public DataBrowser()
	{
		
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.PAGE_AXIS));
	}
	
	/**
	 * Rebuild the entry span
	 * 
	 * @param entries
	 */
	public void setScrollBars(int numEntries, List<Dim> dimTable)
	{
		this.setImageDimTable(dimTable);
		
		this.setEntries(numEntries);
		
		this.repaint();
	}
	
	public void setImageDimTable(List<Dim> dimTable)
	{
		if(this.imageBars == null)
		{
			this.imageBars = new DimTableAdjuster(dimTable); // null handled in
			// constructor
			SSCenter.defaultCenter().connect(this.imageBars, DimTableAdjuster.SIG_DimensionMapAdjusted_NULL, this, "valueDimChanged", (Class[]) null);
			SSCenter.defaultCenter().connect(this.imageBars, DimTableAdjuster.SIG_SmashedDimTableChanged_NULL, this, "valueSmashedDimTableChanged", (Class[]) null);
			SSCenter.defaultCenter().connect(this.imageBars, DimTableAdjuster.SIG_ScrollFinished_NULL, this, "valueScrollFinished", (Class[]) null);
			SSCenter.defaultCenter().connect(this.imageBars, DimTableAdjuster.SIG_ScrollStarted_NULL, this, "scrollStarted", (Class[]) null);
			this.panel.add(this.imageBars.panel());
		}
		else
		{
			this.imageBars.setDimTable(dimTable); // set method handles null
			// argument
		}
	}
	
	public void setEntries(int numEntries)
	{
		if(this.entryBar == null)
		{
			this.entryBar = new DimScrollBar(new Dim("Entry", numEntries), true);
			this.entryBar.setBackgroundColor(DisplayStatics.menuBackground);
			SSCenter.defaultCenter().connect(this.entryBar, DimScrollBar.SIG_ValueChanged_Null, this, "entryChanged", (Class[]) null);
			SSCenter.defaultCenter().connect(this.entryBar, DimScrollBar.SIG_Toggled_Null, this, "smashedEntriesChanged", (Class[]) null);
			SSCenter.defaultCenter().connect(this.entryBar, DimScrollBar.SIG_ScrollFinished_NULL, this, "entryScrollFinished", (Class[]) null);
			SSCenter.defaultCenter().connect(this.entryBar, DimScrollBar.SIG_ScrollStarted_NULL, this, "scrollStarted", (Class[]) null);
			
			if(numEntries > 1)
			{
				this.panel.add(this.entryBar.panel());
			}
		}
		else
		{
			if(!entryBarIsVisible && numEntries > 1)
			{
				this.panel.add(this.entryBar.panel());
				entryBarIsVisible = true;
			}
			else if(entryBarIsVisible && numEntries <= 1)
			{
				this.panel.remove(this.entryBar.panel());
				entryBarIsVisible = false;
			}
			this.entryBar.setDim(new Dim("Entry", numEntries));
		}
	}
	
	public void repaint()
	{
		this.panel.setPreferredSize(new Dimension(200, this.size() * DimScrollBar.height()));
		this.panel.revalidate();
		this.panel.repaint();
	}
	
	public void indexValueDim(int dimNumber, int direction)
	{
		this.imageBars.indexDim(dimNumber, direction);
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void setEntry(int index)
	{
		this.entryBar.setIndex(index);
	}
	
	public int currentEntry()
	{
		return this.entryBar.indexInt();
	}
	
	public void storeDimState()
	{
		this.imageBars.storeOverallState();
	}
	
	public void recallDimState()
	{
		this.imageBars.recallOverallState();
	}
	
	public void setDimState(List<Object[]> overallState)
	{
		this.imageBars.setOverallState(overallState);
	}
	
	public DimensionMap currentImageDimMap()
	{
		return this.imageBars.dimensionMap();
	}
	
	public DimTable smashedImageDimTable()
	{
		return this.imageBars.smashedDimTable();
	}
	
	public Dim smashedEntries()
	{
		return this.entryBar.smashedDim();
	}
	
	public void scrollStarted()
	{
		SSCenter.defaultCenter().emit(this, SIG_ScrollStarted_NULL, (Object[]) null);
	}
	
	/**
	 * Call when signal of entry changed detected
	 */
	public void entryChanged()
	{
		// Emit signal of experiment tree change
		// Logs.log("Send signal of entry changed", 1, this);
		SSCenter.defaultCenter().emit(this, DataBrowser.SIG_EntryChanged_NULL, (Object[]) null);
	}
	
	public void entryScrollFinished()
	{
		SSCenter.defaultCenter().emit(this, DataBrowser.SIG_EntryScrollFinished_NULL, (Object[]) null);
	}
	
	public void smashedEntriesChanged()
	{
		SSCenter.defaultCenter().emit(this, DataBrowser.SIG_SmashedEntriesChanged_NULL, (Object[]) null);
	}
	
	/**
	 * Call when signal of image dimTable changed detected
	 */
	public void valueDimChanged()
	{
		// Emit signal of experiment tree change
		// Logs.log("Send signal of dimension changed", 1,
		// this);
		SSCenter.defaultCenter().emit(this, DataBrowser.SIG_VALUEDimensionMapChanged_NULL, (Object[]) null);
	}
	
	public void valueScrollFinished()
	{
		SSCenter.defaultCenter().emit(this, SIG_VALUEScrollFinished_NULL, (Object[]) null);
	}
	
	public void valueSmashedDimTableChanged()
	{
		SSCenter.defaultCenter().emit(this, DataBrowser.SIG_VALUESmashedDimTableChanged_NULL, (Object[]) null);
	}
	
	public void setToggleEnabled(boolean enabled)
	{
		this.entryBar.setToggleEnabled(enabled);
		this.imageBars.setToggleEnabled(enabled);
	}
	
	public int size()
	{
		int ret = this.imageBars.size();
		if(entryBarIsVisible)
		{
			ret = ret + 1;
		}
		return ret;
		
	}
	
	public Dimension getPreferredScrollableViewportSize()
	{
		return this.panel.getPreferredSize();
	}
	
	public int getScrollableBlockIncrement(Rectangle bounds, int direction, int units)
	{
		return this.getScrollableUnitIncrement(bounds, direction, units);
	}
	
	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}
	
	public boolean getScrollableTracksViewportWidth()
	{
		return true;
	}
	
	public int getScrollableUnitIncrement(Rectangle bounds, int direction, int units)
	{
		if(direction == SwingConstants.VERTICAL)
		{
			if(units < 0)
				return (int) (bounds.height * 0.1);
		}
		return 0;
	}
	
}
