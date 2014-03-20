package jex.utilities;

import java.util.TreeMap;

import miscellaneous.Pair;

public class DataWindows {
	
	TreeMap<Integer,DataWindow> dataWindows = new TreeMap<Integer,DataWindow>();
	int frames = 1;
	int ticker = -1;
	
	public DataWindows(int frames)
	{
		this.frames = frames;
	}
	
	public void increment()
	{
		this.ticker = this.ticker + 1;
	}
	
	public Pair<Double,Double> avgXY()
	{
		double runningTotalX = 0;
		double runningTotalY = 0;
		double count = 0;
		for (Integer key : dataWindows.keySet())
		{
			DataWindow window = dataWindows.get(key);
			if(window.isFilled())
			{
				runningTotalX = runningTotalX + window.avgX();
				runningTotalY = runningTotalY + window.avgY();
				count = count + 1;
			}
		}
		if(count == 0)
			return null;
		Double x = runningTotalX / count;
		Double y = runningTotalY / count;
		return new Pair<Double,Double>(x, y);
	}
	
	public double getAvgIndex()
	{
		return ((double) (this.ticker + (this.ticker - (this.frames - 1)))) / 2;
	}
	
	public void addPoint(Integer windowKey, Double x, Double y)
	{
		DataWindow temp = this.dataWindows.get(windowKey);
		if(temp == null)
		{
			temp = new DataWindow(this.frames);
			this.dataWindows.put(windowKey, temp);
		}
		temp.addPoint(x, y);
	}
	
	public DataWindow getWindow(Integer track)
	{
		return this.dataWindows.get(track);
	}
	
	public void removeWindow(Integer track)
	{
		this.dataWindows.remove(track);
	}
	
}