package plugins.valueTable;

import icons.IconRepository;

import java.awt.Color;
import java.awt.Image;

import javax.swing.JPanel;

import jex.statics.JEXStatics;
import logs.Logs;
import signals.SSCenter;
import tables.Dim;

public class DimScrollBar {
	
	private static final int panelHeight = 35;
	private static final int panelOffset = 4;
	private static Image toggleHere = null, toggleLeft = null, toggleRight = null, toggleAll = null;
	
	public static final String SIG_ValueChanged_Null = "SIG_ValueChanged_Null";
	public static final String SIG_Toggled_Null = "SIG_Toggled_Null";
	public static final String SIG_ScrollFinished_NULL = "SIG_ScrollFinished_NULL";
	public static final String SIG_ScrollStarted_NULL = "SIG_ScrollStarted_NULL";
	
	private ScrollPainter scroll;
	private Dim dim;
	private boolean withToggle;
	
	public DimScrollBar(Dim d, boolean withToggle)
	{
		this.dim = d;
		this.withToggle = withToggle;
		this.initializeDim();
		this.setState(ScrollPainter.HERE);
	}
	
	private void initializeDim()
	{
		this.scroll = new ScrollPainter(15, this.dim.size(), this.withToggle);
		this.scroll.setNameText(this.dim.name());
		this.scroll.setVerticalOffset(panelOffset);
		this.scroll.setHeight(panelHeight);
		if(toggleHere == null)
		{
			toggleHere = JEXStatics.iconRepository.getImageWithName(IconRepository.TOGGLE_HERE, this.scroll.toggleWidth(), this.scroll.toggleWidth());
		}
		if(toggleLeft == null)
		{
			toggleLeft = JEXStatics.iconRepository.getImageWithName(IconRepository.TOGGLE_LEFT, this.scroll.toggleWidth(), this.scroll.toggleWidth());
		}
		if(toggleRight == null)
		{
			toggleRight = JEXStatics.iconRepository.getImageWithName(IconRepository.TOGGLE_RIGHT, this.scroll.toggleWidth(), this.scroll.toggleWidth());
		}
		if(toggleAll == null)
		{
			toggleAll = JEXStatics.iconRepository.getImageWithName(IconRepository.TOGGLE_ALL, this.scroll.toggleWidth(), this.scroll.toggleWidth());
		}
		this.scroll.setToggleHereImage(toggleHere);
		this.scroll.setToggleLeftImage(toggleLeft);
		this.scroll.setToggleRightImage(toggleRight);
		this.scroll.setToggleAllImage(toggleAll);
		this.indexChanged();
		SSCenter.defaultCenter().connect(this.scroll, ScrollPainter.SIG_IndexChanged_NULL, this, "indexChanged", (Class[]) null);
		SSCenter.defaultCenter().connect(this.scroll, ScrollPainter.SIG_Toggled_NULL, this, "toggled", (Class[]) null);
		SSCenter.defaultCenter().connect(this.scroll, ScrollPainter.SIG_ScrollFinished_NULL, this, "scrollFinished", (Class[]) null);
		SSCenter.defaultCenter().connect(this.scroll, ScrollPainter.SIG_ScrollStarted_NULL, this, "scrollStarted", (Class[]) null);
	}
	
	public JPanel panel()
	{
		return this.scroll.panel();
	}
	
	public Dim smashedDim()
	{
		Dim ret = null;
		if(this.state() == ScrollPainter.ALL)
		{
			ret = new Dim(this.dim().name(), this.dim().values());
		}
		else if(this.state() == ScrollPainter.LEFT)
		{
			ret = new Dim(this.dim().name(), this.dim().valuesUpThrough(this.indexInt()));
		}
		else if(this.state() == ScrollPainter.RIGHT)
		{
			ret = new Dim(this.dim().name(), this.dim().valuesStartingAt(this.indexInt()));
		}
		else
		{
			ret = new Dim(this.dim().name(), new String[] { this.indexString() });
		}
		return ret;
	}
	
	public void setToggleEnabled(boolean enabled)
	{
		this.scroll.setToggleEnabled(enabled);
	}
	
	public void indexChanged()
	{
		this.repaint();
		SSCenter.defaultCenter().emit(this, SIG_ValueChanged_Null, (Object[]) null);
	}
	
	public void scrollFinished()
	{
		SSCenter.defaultCenter().emit(this, SIG_ScrollFinished_NULL, (Object[]) null);
	}
	
	public void scrollStarted()
	{
		SSCenter.defaultCenter().emit(this, SIG_ScrollStarted_NULL, (Object[]) null);
	}
	
	public void repaint()
	{
		this.scroll.setValueText(this.dim.valueAt(this.scroll.index()));
		this.scroll.setEndText("[" + (this.scroll.index() + 1) + " of " + this.dim.size() + "]");
	}
	
	public void toggled()
	{
		Logs.log("toggled", 0, this);
		SSCenter.defaultCenter().emit(this, DimScrollBar.SIG_Toggled_Null, (Object[]) null);
	}
	
	public Dim dim()
	{
		return this.dim;
	}
	
	public void setDim(Dim d)
	{
		this.scroll.setNumPositions(d.size());
		this.dim = d;
		this.repaint();
	}
	
	public String indexString()
	{
		return this.dim.valueAt(this.scroll.index());
	}
	
	public int state()
	{
		return this.scroll.state();
	}
	
	public void setState(int state)
	{
		this.scroll.setState(state);
	}
	
	public void setBackgroundColor(Color newColor)
	{
		this.scroll.backColor = newColor;
	}
	
	public int indexInt()
	{
		return this.scroll.index();
	}
	
	public void setIndex(int index)
	{
		this.scroll.setIndex(index);
		this.scroll.setValueText(this.dim.valueAt(this.scroll.index()));
		this.scroll.setEndText("[" + (this.scroll.index() + 1) + " of " + this.dim.size() + "]");
	}
	
	public void setValue(String value)
	{
		int index = this.dim.index(value);
		this.setIndex(index);
	}
	
	public static int height()
	{
		return panelHeight;
	}
	
}
