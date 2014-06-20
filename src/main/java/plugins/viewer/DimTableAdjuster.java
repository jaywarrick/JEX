package plugins.viewer;

import java.awt.Color;
import java.util.List;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import signals.SSCenter;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

public class DimTableAdjuster {
	
	public static final String SIG_DimensionMapAdjusted_NULL = "SIG_DimensionMapAdjusted_NULL";
	public static final String SIG_SmashedDimTableChanged_NULL = "SIG_SmashedDimTableChanged_NULL";
	public static final String SIG_ScrollFinished_NULL = "SIG_ScrollFinished_NULL";
	public static final String SIG_ScrollStarted_NULL = "SIG_ScrollStarted_NULL";
	
	// GUI variable
	private Vector<DimScrollBar> bars;
	private Color background = DisplayStatics.background;
	private JPanel panel;
	private List<Object[]> tempOverallState;
	private boolean toggleEnabled = true;
	
	public DimTableAdjuster()
	{
		this.bars = new Vector<DimScrollBar>(0);
		this.panel = new JPanel();
		this.panel.setBackground(this.background);
		this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.PAGE_AXIS));
	}
	
	public DimTableAdjuster(List<Dim> dimTable)
	{
		this();
		this.setDimTable(dimTable);
	}
	
	public void setDimTable(List<Dim> dimTable)
	{
		this.removeAllDims();
		if(dimTable == null)
		{
			return;
		}
		for (Dim d : dimTable)
		{
			if(d.size() > 0)
			{
				this.addDim(d);
			}
		}
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void addDim(Dim dim)
	{
		this.insertDim(dim, this.bars.size());
		this.panel.revalidate();
		this.panel.repaint();
	}
	
	public void removeDim(String dimName)
	{
		for (DimScrollBar b : this.bars)
		{
			if(b.dim().name().equals(dimName))
			{
				this.removeDim(b);
				this.panel.revalidate();
				this.panel.repaint();
				return;
			}
		}
	}
	
	public void removeAllDims()
	{
		for (DimScrollBar bar : this.bars)
		{
			SSCenter.defaultCenter().disconnect(bar, DimScrollBar.SIG_ValueChanged_Null, this);
			SSCenter.defaultCenter().disconnect(bar, DimScrollBar.SIG_Toggled_Null, this);
			SSCenter.defaultCenter().disconnect(bar, DimScrollBar.SIG_ScrollFinished_NULL, this);
			SSCenter.defaultCenter().disconnect(bar, DimScrollBar.SIG_ScrollStarted_NULL, this);
		}
		this.bars.clear();
		this.panel.removeAll();
	}
	
	public void insertDim(Dim dim, int index)
	{
		// Add HyperBar
		DimScrollBar bar = new DimScrollBar(dim, true);
		bar.setBackgroundColor(DisplayStatics.menuBackground);
		
		SSCenter.defaultCenter().connect(bar, DimScrollBar.SIG_ValueChanged_Null, this, "dimensionMapChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(bar, DimScrollBar.SIG_Toggled_Null, this, "smashedDimTableChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(bar, DimScrollBar.SIG_ScrollFinished_NULL, this, "scrollFinished", (Class[]) null);
		SSCenter.defaultCenter().connect(bar, DimScrollBar.SIG_ScrollStarted_NULL, this, "scrollStarted", (Class[]) null);
		this.bars.add(index, bar);
		bar.setToggleEnabled(this.toggleEnabled);
		this.panel.add(bar.panel(), index);
		
		this.panel.revalidate();
		this.panel.repaint();
	}
	
	private void removeDim(DimScrollBar b)
	{
		SSCenter.defaultCenter().disconnect(b, DimScrollBar.SIG_ValueChanged_Null, this);
		SSCenter.defaultCenter().disconnect(b, DimScrollBar.SIG_Toggled_Null, this);
		SSCenter.defaultCenter().disconnect(b, DimScrollBar.SIG_ScrollFinished_NULL, this);
		this.bars.remove(b);
		this.panel.remove(b.panel());
		
		this.panel.revalidate();
		this.panel.repaint();
	}
	
	public void indexDim(int dimNumber, int direction)
	{
		if(dimNumber < 0 || dimNumber >= this.bars.size())
		{
			return;
		}
		DimScrollBar bar = this.bars.get(dimNumber);
		if(direction > 0)
		{
			bar.setIndex(bar.indexInt() + 1);
		}
		else if(direction < 0)
		{
			bar.setIndex(bar.indexInt() - 1);
		}
	}
	
	public DimensionMap dimensionMap()
	{
		DimensionMap d = new DimensionMap();
		for (DimScrollBar b : this.bars)
		{
			d.put(b.dim().name(), b.indexString());
		}
		return d;
	}
	
	public List<Dim> dimensionTable()
	{
		List<Dim> dimTable = new Vector<Dim>();
		for (DimScrollBar b : this.bars)
		{
			dimTable.add(b.dim());
		}
		return dimTable;
	}
	
	public DimTable smashedDimTable()
	{
		DimTable ret = new DimTable();
		Vector<Dim> dims = new Vector<Dim>();
		for (DimScrollBar d : this.bars)
		{
			dims.add(d.smashedDim());
		}
		ret = new DimTable(dims);
		return ret;
	}
	
	public void storeOverallState()
	{
		if(this.tempOverallState == null)
		{
			this.tempOverallState = new Vector<Object[]>();
		}
		else
		{
			this.tempOverallState.clear();
		}
		DimScrollBar bar;
		for (int i = 0; i < this.bars.size(); i++)
		{
			bar = this.bars.get(i);
			this.tempOverallState.add(new Object[] { bar.dim().name(), bar.indexInt(), bar.state() });
		}
	}
	
	public void recallOverallState()
	{
		this.setOverallState(this.tempOverallState);
	}
	
	public List<Object[]> getStoredOverallState()
	{
		return this.tempOverallState;
	}
	
	public void setOverallState(List<Object[]> overallState)
	{
		DimScrollBar bar;
		for (int i = 0; i < this.bars.size(); i++)
		{
			bar = this.bars.get(i);
			for (Object[] state : overallState)
			{
				if(bar.dim().name().equals(state[0]))
				{
					bar.setIndex((Integer) state[1]);
					bar.setState((Integer) state[2]);
					break;
				}
			}
		}
	}
	
	public int size()
	{
		return this.dimensionTable().size();
	}
	
	public void resetPosition()
	{
		// boolean causedChanges = false;
		for (DimScrollBar b : this.bars)
		{
			if(b.indexInt() != 0)
			{
				b.setIndex(0);
				// causedChanges = true;
			}
		}
		// if(causedChanges)
		// this.dimensionMapChanged();
	}
	
	public void setDimensionMap(DimensionMap map)
	{
		// boolean causedChanges = false;
		for (int i = 0; i < this.bars.size(); i++)
		{
			DimScrollBar b = this.bars.get(i);
			String value = map.get(b.dim().name());
			if(value != null) // set what we can
			{
				b.setValue(value);
				// causedChanges = true;
			}
		}
		// if(causedChanges)
		// this.dimensionMapChanged();
	}
	
	public void setToggleEnabled(boolean enabled)
	{
		this.toggleEnabled = enabled;
		for (DimScrollBar bar : this.bars)
		{
			bar.setToggleEnabled(enabled);
		}
		this.panel().repaint();
	}
	
	public void dimensionMapChanged()
	{
		// Logs.log("Dimension map changed", 0, this);
		SSCenter.defaultCenter().emit(this, DimTableAdjuster.SIG_DimensionMapAdjusted_NULL, (Object[]) null);
	}
	
	public void smashedDimTableChanged()
	{
		// Logs.log("Smashed DimTable changed", 0, this);
		SSCenter.defaultCenter().emit(this, DimTableAdjuster.SIG_SmashedDimTableChanged_NULL, (Object[]) null);
	}
	
	public void scrollFinished()
	{
		SSCenter.defaultCenter().emit(this, SIG_ScrollFinished_NULL, (Object[]) null);
	}
	
	public void scrollStarted()
	{
		SSCenter.defaultCenter().emit(this, SIG_ScrollStarted_NULL, (Object[]) null);
	}
	
}
