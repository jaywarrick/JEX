package plugins.viewer;

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
	
	// static
	
	public static final String SIG_RoiDimensionMapChanged_NULL = "SIG_RoiDimensionMapChanged_NULL";
	public static final String SIG_RoiSmashedDimTableChanged_NULL = "SIG_RoiSmashedDimTableChanged_NULL";
	public static final String SIG_RoiScrollFinished_NULL = "SIG_RoiScrollFinished_NULL";
	public static final String SIG_ImageDimensionMapChanged_NULL = "SIG_ImageDimensionMapChanged_NULL";
	public static final String SIG_ImageSmashedDimTableChanged_NULL = "SIG_ImageSmashedDimTableChanged_NULL";
	public static final String SIG_ImageScrollFinished_NULL = "SIG_ImageScrollFinished_NULL";
	public static final String SIG_EntryChanged_NULL = "SIG_EntryChanged_NULL";
	public static final String SIG_SmashedEntriesChanged_NULL = "SIG_SmashedEntriesChanged_NULL";
	public static final String SIG_EntryScrollFinished_NULL = "SIG_EntryScrollFinished_NULL";
	
	public static final String SIG_ScrollStarted_NULL = "SIG_ScrollStarted_NULL";
	
	// GUI variable
	private DimTableAdjuster roiBars;
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
	public void setScrollBars(int numEntries, List<Dim> dimTable, List<Dim> roiDimTable)
	{
		this.setImageDimTable(dimTable);
		
		this.setRoiDimTable(roiDimTable);
		
		this.setEntries(numEntries);
		
		this.repaint();
	}
	
	public void setRoiDimTable(List<Dim> dimTable)
	{
		// Only add dimensions that aren't already included in the image
		// DimTable
		DimTable temp = new DimTable();
		DimensionMap imageMap = this.currentImageDimMap();
		if(dimTable != null)
		{
			for (Dim d : dimTable)
			{
				if(imageMap.get(d.name()) == null)
				{
					temp.add(d);
				}
			}
		}
		
		if(this.roiBars == null)
		{
			this.roiBars = new DimTableAdjuster(temp);
			SSCenter.defaultCenter().connect(this.roiBars, DimTableAdjuster.SIG_DimensionMapAdjusted_NULL, this, "roiDimChanged", (Class[]) null);
			SSCenter.defaultCenter().connect(this.roiBars, DimTableAdjuster.SIG_SmashedDimTableChanged_NULL, this, "roiSmashedDimTableChanged", (Class[]) null);
			SSCenter.defaultCenter().connect(this.roiBars, DimTableAdjuster.SIG_ScrollFinished_NULL, this, "roiScrollFinished", (Class[]) null);
			SSCenter.defaultCenter().connect(this.roiBars, DimTableAdjuster.SIG_ScrollStarted_NULL, this, "scrollStarted", (Class[]) null);
			this.panel.add(this.roiBars.panel(), 0);
		}
		else
		{
			this.roiBars.setDimTable(temp);
		}
	}
	
	public void setImageDimTable(List<Dim> dimTable)
	{
		if(this.imageBars == null)
		{
			this.imageBars = new DimTableAdjuster(dimTable); // null handled in
			// constructor
			SSCenter.defaultCenter().connect(this.imageBars, DimTableAdjuster.SIG_DimensionMapAdjusted_NULL, this, "imageDimChanged", (Class[]) null);
			SSCenter.defaultCenter().connect(this.imageBars, DimTableAdjuster.SIG_SmashedDimTableChanged_NULL, this, "imageSmashedDimTableChanged", (Class[]) null);
			SSCenter.defaultCenter().connect(this.imageBars, DimTableAdjuster.SIG_ScrollFinished_NULL, this, "imageScrollFinished", (Class[]) null);
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
			if(!this.entryBarIsVisible && numEntries > 1)
			{
				this.panel.add(this.entryBar.panel());
				this.entryBarIsVisible = true;
			}
			else if(this.entryBarIsVisible && numEntries <= 1)
			{
				this.panel.remove(this.entryBar.panel());
				this.entryBarIsVisible = false;
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
	
	public void indexImageDim(int dimNumber, int direction)
	{
		this.imageBars.indexDim(dimNumber, direction);
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	// public void setDim(DimensionMap map)
	// {
	// this.dimBars.setDimensionMap(map);
	// // Roi bars?
	// }
	
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
		this.roiBars.storeOverallState();
		// this.tempEntryIndex = this.entryBar.indexInt();
	}
	
	public void recallDimState()
	{
		this.imageBars.recallOverallState();
		this.roiBars.recallOverallState();
		// this.entryBar.setIndex(this.tempEntryIndex);
	}
	
	public List<Object[]> getStoredDimState()
	{
		List<Object[]> ret = this.roiBars.getStoredOverallState();
		ret.addAll(this.imageBars.getStoredOverallState());
		return ret;
	}
	
	public void setDimState(List<Object[]> overallState)
	{
		this.imageBars.setOverallState(overallState);
		this.roiBars.setOverallState(overallState);
	}
	
	public DimensionMap currentImageDimMap()
	{
		return this.imageBars.dimensionMap();
	}
	
	public DimensionMap currentRoiDimMap()
	{
		DimensionMap ret = this.currentImageDimMap();
		DimensionMap temp = this.roiBars.dimensionMap();
		for (String dimName : temp.keySet())
		{
			ret.put(dimName, temp.get(dimName));
		}
		return ret;
	}
	
	public DimTable smashedImageDimTable()
	{
		return this.imageBars.smashedDimTable();
	}
	
	public DimTable smashedRoiDimTable()
	{
		DimTable im = this.imageBars.smashedDimTable();
		DimTable roi = this.roiBars.smashedDimTable();
		return DimTable.union(im, roi);
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
	public void imageDimChanged()
	{
		// Emit signal of experiment tree change
		// Logs.log("Send signal of dimension changed", 1,
		// this);
		SSCenter.defaultCenter().emit(this, DataBrowser.SIG_ImageDimensionMapChanged_NULL, (Object[]) null);
	}
	
	public void imageScrollFinished()
	{
		SSCenter.defaultCenter().emit(this, SIG_ImageScrollFinished_NULL, (Object[]) null);
	}
	
	public void imageSmashedDimTableChanged()
	{
		SSCenter.defaultCenter().emit(this, DataBrowser.SIG_ImageSmashedDimTableChanged_NULL, (Object[]) null);
	}
	
	/**
	 * Call when signal of roi dimTable changed detected
	 */
	public void roiDimChanged()
	{
		// Emit signal of experiment tree change
		// Logs.log("Send signal of dimension changed", 1,
		// this);
		SSCenter.defaultCenter().emit(this, DataBrowser.SIG_RoiDimensionMapChanged_NULL, (Object[]) null);
	}
	
	public void roiScrollFinished()
	{
		SSCenter.defaultCenter().emit(this, SIG_RoiScrollFinished_NULL, (Object[]) null);
	}
	
	public void roiSmashedDimTableChanged()
	{
		SSCenter.defaultCenter().emit(this, DataBrowser.SIG_ImageSmashedDimTableChanged_NULL, (Object[]) null);
	}
	
	public void setToggleEnabled(boolean enabled)
	{
		this.entryBar.setToggleEnabled(enabled);
		this.imageBars.setToggleEnabled(enabled);
		this.roiBars.setToggleEnabled(enabled);
	}
	
	public int size()
	{
		int ret = this.roiBars.size() + this.imageBars.size();
		if(this.entryBarIsVisible)
		{
			ret = ret + 1;
		}
		return ret;
		
	}
	
	@Override
	public Dimension getPreferredScrollableViewportSize()
	{
		return this.panel.getPreferredSize();
	}
	
	@Override
	public int getScrollableBlockIncrement(Rectangle bounds, int direction, int units)
	{
		return this.getScrollableUnitIncrement(bounds, direction, units);
	}
	
	@Override
	public boolean getScrollableTracksViewportHeight()
	{
		return false;
	}
	
	@Override
	public boolean getScrollableTracksViewportWidth()
	{
		return true;
	}
	
	@Override
	public int getScrollableUnitIncrement(Rectangle bounds, int direction, int units)
	{
		if(direction == SwingConstants.VERTICAL)
		{
			if(units < 0)
			{
				return (int) (bounds.height * 0.1);
			}
		}
		return 0;
	}
	
}
