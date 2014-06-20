package jex.utilities;

import java.util.LinkedList;
import java.util.TreeMap;
import java.util.Vector;

import miscellaneous.Pair;
import tables.DimensionMap;

public class DataWindow {
	
	public LinkedList<Double> xl;
	public LinkedList<Double> yl;
	public Double xTotal = 0.0;
	public Double yTotal = 0.0;
	
	public Integer frames = null;
	
	public DataWindow(int frames)
	{
		this.frames = frames;
		xl = new LinkedList<Double>();
		yl = new LinkedList<Double>();
	}
	
	public void addPoint(Double x, Double y)
	{
		if(this.isFilled())
		{
			Double oldX = xl.remove();
			Double oldY = yl.remove();
			xTotal = xTotal - oldX;
			yTotal = yTotal - oldY;
		}
		xl.offer(x);
		yl.offer(y);
		xTotal = xTotal + x;
		yTotal = yTotal + y;
	}
	
	public int size()
	{
		return this.xl.size();
	}
	
	public Double avgX()
	{
		return xTotal / ((double) frames);
	}
	
	public Double avgY()
	{
		return yTotal / ((double) frames);
	}
	
	public Double dx()
	{
		return (xl.peekLast() - xl.peekFirst()); // xf - xi
	}
	
	public Double dy()
	{
		return (yl.peekLast() - yl.peekFirst()); // yf - yi
	}
	
	public Double r()
	{
		return Math.sqrt(Math.pow(this.dx(), 2) + Math.pow(this.dy(), 2));
	}
	
	public Double l()
	{
		Double l = 0.0;
		for (int i = 1; i < this.xl.size(); i++)
		{
			l = l + Math.sqrt(Math.pow(this.xl.get(i - 1) - this.xl.get(i), 2) + Math.pow(this.yl.get(i - 1) - this.yl.get(i), 2));
		}
		return l;
	}
	
	public boolean isFilled()
	{
		return this.xl.size() == this.frames;
	}
	
	public static Pair<Vector<Double>,Vector<Double>> getMovingAverage(String timeDimName, int frames, TreeMap<DimensionMap,Double> data)
	{
		Pair<Vector<Double>,Vector<Double>> ret = new Pair<Vector<Double>,Vector<Double>>(new Vector<Double>(), new Vector<Double>());
		DataWindow window = new DataWindow(frames);
		for (DimensionMap map : data.keySet())
		{
			Double time = Double.parseDouble(map.get(timeDimName));
			window.addPoint(time, data.get(map));
			if(window.isFilled())
			{
				ret.p1.add(window.avgX());
				ret.p2.add(window.avgY());
			}
		}
		return ret;
	}
	
}