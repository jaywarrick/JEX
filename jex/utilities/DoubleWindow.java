package jex.utilities;

import java.util.TreeMap;
import java.util.Vector;

import tables.DimTable;
import tables.DimensionMap;
import tables.Table;

public class DoubleWindow {
	
	private int oldest = 0, newest = 0;
	private double[] window;
	public double total = 0, min = Double.MAX_VALUE, max = Double.MIN_VALUE, minAvg = min, maxAvg = max;
	
	public DoubleWindow(int maxElements)
	{
		window = new double[maxElements];
	}
	
	/**
	 * A fixed size queue that returns an object if it is kicked off the front of the queue due to size limitations
	 * 
	 * @param o
	 * @return
	 */
	public Double add(double value)
	{
		// Add the new value
		
		Double ret = null;
		int oldMod = oldest % window.length;
		if((newest - oldest) == (window.length - 1)) // then the oldest needs to
		// catch up to the newest
		{
			ret = window[oldMod];
			oldest = oldest + 1;
			total = total - ret;
			newest = newest + 1;
			window[newest % window.length] = value;
			total = total + value;
		}
		else
		{
			if(oldest == 0 && newest == 0)
			{
				oldest = 1;
			}
			newest = newest + 1;
			window[newest % window.length] = value;
			total = total + value;
		}
		
		// Update stats
		min = Math.min(min, value);
		max = Math.max(max, value);
		
		if(this.isFull())
		{
			minAvg = Math.min(minAvg, this.avg());
			maxAvg = Math.max(maxAvg, this.avg());
		}
		
		return ret;
	}
	
	public boolean isFull()
	{
		return this.count() == this.size();
	}
	
	public int count()
	{
		if(oldest == 0 && newest == 0)
		{
			return 0;
		}
		else
		{
			return newest - oldest + 1;
		}
	}
	
	public int overallLength()
	{
		return newest;
	}
	
	public int size()
	{
		return window.length;
	}
	
	public double avg()
	{
		return this.total / ((double) this.count());
	}
	
	// public Object remove()
	// {
	// if (oldest==newest )
	// {
	// throw new EmptyQueueException();
	// }
	// oldest= (oldest+ 1) % queue.length;
	// return queue[oldest];
	// }
	
	/**
	 * Returns a TreeMap<String,Object> with min (Double), max (Double), minAvg (Double), maxAvg (Double), halfRange (Double), halfRangeTime (Double), halfAvgRange (Double), halfAvgRangeTime (Double), avg (TreeMap<DimensionMap,Double>), and time
	 * (Vector<Double>) as objects with those key names
	 * 
	 * @param data
	 * @param filteredTable
	 * @param windowSize
	 * @return
	 */
	public static TreeMap<String,Object> getWindowedData(Table<Double> data, DimensionMap filter, int windowSize, String timeDimName)
	{
		TreeMap<DimensionMap,Double> avg = new TreeMap<DimensionMap,Double>();
		Vector<Double> time = new Vector<Double>();
		DoubleWindow window = new DoubleWindow(windowSize);
		DimTable filteredTable = data.dimTable.getSubTable(filter);
		for (DimensionMap map : filteredTable.getMapIterator())
		{
			Double temp = data.getData(map);
			if(temp != null)
			{
				window.add(temp);
				if(window.isFull())
				{
					avg.put(map, window.avg());
					time.add(Double.parseDouble(map.get(timeDimName)));
				}
			}
		}
		
		// if(window.overallLength() < 150)
		// {
		// return null;
		// }
		DimensionMap halfRange = null;
		DimensionMap halfAvgRange = null;
		double hRange = window.min + Math.pow(10, Math.log10(window.max - window.min) / 2);
		double hAvgRange = window.minAvg + Math.pow(10, Math.log10(window.maxAvg - window.minAvg) / 2);
		Double temp = 0.0;
		for (DimensionMap map : filteredTable.getMapIterator())
		{
			temp = avg.get(map);
			if(halfRange != null && halfAvgRange != null)
				break;
			if(temp != null)
			{
				if(temp >= hRange && halfRange == null)
				{
					halfRange = map.copy();
				}
				if(temp >= hAvgRange && halfAvgRange == null)
				{
					halfAvgRange = map.copy();
				}
			}
		}
		
		TreeMap<String,Object> ret = new TreeMap<String,Object>();
		ret.put("min", window.min);
		ret.put("max", window.max);
		ret.put("minAvg", window.minAvg);
		ret.put("maxAvg", window.maxAvg);
		ret.put("halfRange", halfRange);
		if(halfRange == null)
		{
			// Logs.log("Help", 0,
			// DoubleWindow.class.getSimpleName());
			return null;
		}
		ret.put("halfRangeTime", Double.parseDouble(halfRange.get(timeDimName)));
		ret.put("halfAvgRange", halfAvgRange);
		if(halfAvgRange == null)
		{
			// Logs.log("Help", 0,
			// DoubleWindow.class.getSimpleName());
			return null;
		}
		ret.put("halfAvgRangeTime", Double.parseDouble(halfAvgRange.get(timeDimName)));
		ret.put("avg", avg);
		ret.put("time", time);
		
		return ret;
	}
	
}
