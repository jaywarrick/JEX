package jex.utilities;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.TreeMap;


/**
 * Class for working with moving averages and other "window" operations
 * @author jaywarrick
 *
 */
public class NDataWindow {
	
	public TreeMap<String,LinkedList<Double>> xl;
	public TreeMap<String,Double> total;
	
	public Integer frames = null;
	public int ticker = -1;
	
	public NDataWindow(int frames)
	{
		this.frames = frames;
		xl = new TreeMap<String,LinkedList<Double>>();
		total = new TreeMap<String,Double>();
	}
	
	public void addMeasurement(String measurement, Double value, int ticker)
	{
		this.ticker = ticker;
		LinkedList<Double> measurements = xl.get(measurement);
		if(measurements != null && measurements.size() == this.frames) // then
		// remove
		// the
		// oldest
		// value
		// from
		// the
		// list
		// and
		// from
		// the
		// total
		{
			Double oldX = measurements.remove();
			total.put(measurement, total.get(measurement) - oldX);
		}
		
		// add the new value to the list and to the total
		if(measurements == null) // create a linked list if one doesn't already
		// exist and add it to xl
		{
			measurements = new LinkedList<Double>();
			xl.put(measurement, measurements);
		}
		measurements.offer(value);
		if(total.get(measurement) == null)
		{
			total.put(measurement, 0.0);
		}
		total.put(measurement, total.get(measurement) + value);
	}
	
	public int getTicker()
	{
		return this.ticker;
	}
	
	public int size()
	{
		return this.xl.size();
	}
	
	public Double avg(String key)
	{
		return total.get(key) / ((double) this.frames);
	}
	
	public TreeMap<String,Double> avg()
	{
		TreeMap<String,Double> ret = new TreeMap<String,Double>();
		for (Entry<String,Double> e : this.total.entrySet())
		{
			ret.put(e.getKey(), e.getValue() / ((double) this.frames));
		}
		return ret;
	}
	
	public TreeMap<String,Double> dx()
	{
		TreeMap<String,Double> ret = new TreeMap<String,Double>();
		for (Entry<String,LinkedList<Double>> measurement : xl.entrySet())
		{
			ret.put(measurement.getKey(), measurement.getValue().peekLast() - measurement.getValue().peekFirst());
		}
		return ret;
	}
	
	public Double r(String keyX, String keyY)
	{
		Double dx = xl.get(keyX).peekLast() - xl.get(keyX).peekFirst();
		Double dy = xl.get(keyY).peekLast() - xl.get(keyY).peekFirst();
		return Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
	}
	
	public Double l(String keyX, String keyY)
	{
		Double l = 0.0;
		for (int i = 1; i < xl.get(keyX).size(); i++)
		{
			Double dx = xl.get(keyX).get(i - 1) - xl.get(keyX).get(i);
			Double dy = xl.get(keyY).get(i - 1) - xl.get(keyY).get(i);
			l = l + Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
		}
		return l;
	}
	
	public boolean isFilled(String measurement)
	{
		if(measurement == null)
		{
			return xl.firstEntry().getValue().size() == this.frames;
		}
		return xl.get(measurement).size() == this.frames;
	}
	
}