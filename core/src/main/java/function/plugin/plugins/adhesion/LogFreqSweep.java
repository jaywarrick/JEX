package function.plugin.plugins.adhesion;

import java.util.TreeSet;
import java.util.Vector;

import logs.Logs;

public class LogFreqSweep {
	
	double fi=1.0, ff=1.0, tf=1.0; // [Hz], [Hz], [s]
	
	public LogFreqSweep(double fi, double ff, double tf)
	{
		this.fi = fi;
		this.ff = ff;
		this.tf = tf;
	}
	
	public static Vector<Double> getTimes(double nFrames, double frameTime)
	{
		Vector<Double> ret = new Vector<Double>();
		for(int i = 1; i <= nFrames; i++)
		{
			ret.add((i-1)*frameTime);
		}
		return ret;
	}
	
	public Vector<Double> getFrequencies(Vector<Double> times)
	{
		Vector<Double> ret = new Vector<Double>();
		for(Double t : times)
		{
			ret.add(Math.pow(fi*(ff/fi),(t/tf)));
		}
		
		return ret;
	}
	
	public TreeSet<Integer> getTrueFrames(double nFrames, double frameTime, double desiredFramesPerPeriod)
	{
		Vector<Double> actualTimes = getTimes(nFrames, frameTime);
		Vector<Double> idealTimes = getDesiredSampleTimes(desiredFramesPerPeriod);
		TreeSet<Integer> ret = new TreeSet<Integer>();
		for(double it : idealTimes)
		{
			int nearestIndex = 0;
			double prevDist = Double.MAX_VALUE;
			double minDist = Double.MAX_VALUE;
			for(int i = 0; i < actualTimes.size(); i++)
			{
				double dist = Math.abs((it-actualTimes.get(i)));
				if(dist < minDist)
				{
					minDist = dist;
					nearestIndex = i;
				}
				if(prevDist < dist)
				{
					break;
				}
				else
				{
					prevDist = dist;
				}
			}
			ret.add(nearestIndex);
		}
		return ret;
		
	}
	
	public Vector<Double> getDesiredSampleTimes(double desiredFramesPerPeriod)
	{
		double periodInterval = 4.0/desiredFramesPerPeriod;
		Vector<Double> ret = new Vector<Double>();
		int frame = 1;
		while(true)
		{
			Double t = getTimeForPeriodDistance((frame-1)*periodInterval);
			if(t == null || !Double.isFinite(t) || t > tf)
			{
				break;
			}
			else
			{
				ret.add(t);
			}
			frame = frame + 1;
		}
		return ret;
	}
	
	public Double getTimeForPeriodDistance(double T)
	{
		try
		{
			if(ff == fi)
			{
				double ret = T/(4*fi);
				return ret;
			}
			else
			{
				double ret = (tf*Math.log((Math.log(ff/fi)*T)/(4*fi*tf*1)+1))/Math.log(ff/fi);
				return ret;
			}
		}
		catch(Exception e)
		{
			Logs.log("Couldn't find a time for this period distance... " + T, this);
			return null;
		}
	}
	

}
