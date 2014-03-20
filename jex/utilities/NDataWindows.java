package jex.utilities;

import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Class for working with moving averages and other "window" operations... keeping track of multiple windows at a time.
 * @author jaywarrick
 *
 */
public class NDataWindows {
	
	TreeMap<Integer,NDataWindow> dataWindows = new TreeMap<Integer,NDataWindow>();
	int frames = 1;
	int ticker = -1;
	
	public NDataWindows(int frames)
	{
		this.frames = frames;
	}
	
	public TreeMap<Integer,NDataWindow> getDataWindows()
	{
		return this.dataWindows;
	}
	
	public void increment()
	{
		this.ticker = this.ticker + 1;
	}
	
	public TreeMap<String,Double> avg()
	{
		TreeMap<String,Double> ret = new TreeMap<String,Double>();
		double count = 0;
		for (Integer cell : dataWindows.keySet())
		{
			NDataWindow window = dataWindows.get(cell);
			if(window.isFilled(null)) // if the window for the first measurement
			// is full the rest should be full too :-)
			// right?
			{
				for (Entry<String,Double> e : window.avg().entrySet())
				{
					ret.put(e.getKey(), ret.get(e.getKey()) + e.getValue());
				}
				count = count + 1;
			}
		}
		if(count == 0)
			return null;
		for (Entry<String,Double> e : ret.entrySet())
		{
			e.setValue(e.getValue() / count);
		}
		return ret;
	}
	
	public double getAvgIndex()
	{
		return ((double) (this.ticker + (this.ticker - (this.frames - 1)))) / 2;
	}
	
	public void addMeasurement(Integer cell, String measurement, Double value)
	{
		NDataWindow temp = this.dataWindows.get(cell);
		if(temp == null)
		{
			temp = new NDataWindow(this.frames);
			this.dataWindows.put(cell, temp);
		}
		temp.addMeasurement(measurement, value, this.ticker);
	}
	
	public boolean isWindowCurrent(Integer cell)
	{
		return this.getTicker() == this.getWindow(cell).getTicker();
	}
	
	public int getTicker()
	{
		return this.ticker;
	}
	
	public int size()
	{
		return this.dataWindows.size();
	}
	
	public NDataWindow getWindow(Integer cell)
	{
		return this.dataWindows.get(cell);
	}
	
	public void removeWindow(Integer cell)
	{
		this.dataWindows.remove(cell);
	}
}