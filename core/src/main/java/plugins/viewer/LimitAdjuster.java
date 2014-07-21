package plugins.viewer;

import java.awt.Color;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import signals.SSCenter;
import tables.Dim;

public class LimitAdjuster {
	
	private DimScrollBar lowerBar = null, upperBar = null;
	private JPanel panel;
	
	public static final String SIG_limitsChanged_NULL = "SIG_limitsChanged_NULL";
	
	public LimitAdjuster()
	{
		this.panel = new JPanel();
		this.panel.setBackground(Color.RED);
		this.panel.setLayout(new MigLayout("flowy,ins 0", "[fill,grow]", "[]0[]"));
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void setBounds(int min, int max)
	{
		int tempmin = this.lowerIndex();
		int tempmax = this.upperIndex();
		
		if(this.upperBar == null)
		{
			// Create one
			this.upperBar = new DimScrollBar(new Dim("Max", 0, max), false);
			this.panel.add(this.upperBar.panel(), "growx");
			SSCenter.defaultCenter().connect(this.upperBar, DimScrollBar.SIG_ValueChanged_Null, this, "upperChanged", (Class[]) null);
		}
		else
		{
			this.upperBar.setDim(new Dim("Max", 0, max));
		}
		
		if(this.lowerBar == null)
		{
			// Create one
			this.lowerBar = new DimScrollBar(new Dim("Min", 0, max), false);
			this.panel.add(this.lowerBar.panel(), "growx");
			SSCenter.defaultCenter().connect(this.lowerBar, DimScrollBar.SIG_ValueChanged_Null, this, "lowerChanged", (Class[]) null);
		}
		else
		{
			this.lowerBar.setDim(new Dim("Min", 0, max));
		}
		
		this.setLowerAndUpper(tempmin, tempmax);
		
	}
	
	public void setBounds(double min, double max, int n)
	{
		int tempmin = this.lowerIndex();
		int tempmax = this.upperIndex();
		
		double lowerNum = Double.parseDouble(this.lowerBar.dim().valueAt(tempmin));
		double upperNum = Double.parseDouble(this.upperBar.dim().valueAt(tempmax));
		
		if(this.upperBar == null)
		{
			this.upperBar = new DimScrollBar(new Dim("Max", 0, 255), false);
			this.panel.add(this.upperBar.panel(), "growx");
			SSCenter.defaultCenter().connect(this.upperBar, DimScrollBar.SIG_ValueChanged_Null, this, "upperChanged", (Class[]) null);
		}
		this.upperBar.setDim(new Dim("Max", min, max, n));
		
		if(this.lowerBar == null)
		{
			// Create one
			this.lowerBar = new DimScrollBar(new Dim("Min", 0, 255), false);
			this.panel.add(this.lowerBar.panel(), "growx");
			SSCenter.defaultCenter().connect(this.lowerBar, DimScrollBar.SIG_ValueChanged_Null, this, "lowerChanged", (Class[]) null);
		}
		this.lowerBar.setDim(new Dim("Min", min, max, n));
		
		this.setLowerAndUpper(lowerNum, upperNum);
	}
	
	public int lowerIndex()
	{
		if(this.lowerBar == null)
		{
			return -1;
		}
		return this.lowerBar.indexInt();
	}
	
	public double lowerNum()
	{
		return Double.parseDouble(this.lowerBar.dim().valueAt(this.lowerIndex()));
	}
	
	public int upperIndex()
	{
		if(this.upperBar == null)
		{
			return -1;
		}
		return this.upperBar.indexInt();
	}
	
	public double upperNum()
	{
		return Double.parseDouble(this.upperBar.dim().valueAt(this.upperIndex()));
	}
	
	public void lowerChanged()
	{
		if(this.lowerBar.indexInt() > this.upperBar.indexInt())
		{
			this.lowerBar.setIndex(this.upperBar.indexInt());
		}
		SSCenter.defaultCenter().emit(this, SIG_limitsChanged_NULL, (Object[]) null);
	}
	
	public void upperChanged()
	{
		if(this.lowerBar.indexInt() > this.upperBar.indexInt())
		{
			this.upperBar.setIndex(this.lowerBar.indexInt());
		}
		SSCenter.defaultCenter().emit(this, SIG_limitsChanged_NULL, (Object[]) null);
	}
	
	public void setLowerAndUpper(int lowerIndex, int upperIndex)
	{
		if(lowerIndex < 0 || lowerIndex > this.lowerBar.dim().size())
		{
			lowerIndex = 0;
		}
		if(lowerIndex < 0 || lowerIndex > this.upperBar.dim().size())
		{
			upperIndex = this.upperBar.dim().size();
		}
		this.lowerBar.setIndex(lowerIndex);
		this.upperBar.setIndex(upperIndex);
	}
	
	public void setLowerAndUpper(double lowerNum, double upperNum)
	{
		int lower = this.nearestIndex(this.lowerBar, lowerNum);
		int upper = this.nearestIndex(this.upperBar, upperNum);
		if(lower > upper)
		{
			lower = upper;
		}
		this.lowerBar.setIndex(lower);
		this.upperBar.setIndex(upper);
	}
	
	public static double maxNum(DimScrollBar bar)
	{
		return Double.parseDouble(bar.dim().min());
	}
	
	public static double minNum(DimScrollBar bar)
	{
		return Double.parseDouble(bar.dim().max());
	}
	
	private int nearestIndex(DimScrollBar bar, double num)
	{
		if(num <= maxNum(bar))
		{
			return 0;
		}
		if(num >= minNum(bar))
		{
			return bar.dim().size() - 1;
		}
		
		// else
		for (int i = 0; i < bar.dim().size(); i++)
		{
			if(Double.parseDouble(bar.dim().valueAt(i)) >= num)
			{
				return i;
			}
		}
		return 0;
	}
}
