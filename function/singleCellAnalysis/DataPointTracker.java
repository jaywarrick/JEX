package function.singleCellAnalysis;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import jex.utilities.ImageUtility;
import miscellaneous.Pair;
import tables.Dim;
import tables.DimensionMap;
import tables.Table;

public class DataPointTracker implements Comparator<Pair<DataPoint,DataPoint>> {
	
	public double W = 0.5, sigR = 1, sigG = 1, sigB = 1, Dmax = Double.MAX_VALUE;
	public int n = 5, assignableID = 1;
	
	public double medianD = 0; // key
	// =
	// time,
	// value
	// =
	// median
	// d
	public double medianFC = 0; // key
	// =
	// time,
	// value
	// =
	// median
	// fc
	public String histD = null;
	public String histFC = null;
	
	public Vector<DataPoint> p1s = new Vector<DataPoint>();
	public Vector<DataPoint> p2s = new Vector<DataPoint>();
	public Vector<Pair<DataPoint,DataPoint>> pairs;
	
	/**
	 * This should be used only for creating and dummy instance for sorting (i.e. Collections.sort(X, usingThisDummyInstance))
	 */
	
	public void setParams(double W, double sigR, double sigG, double sigB, int n, double Dmax)
	{
		this.W = W;
		this.sigR = sigR;
		this.sigG = sigG;
		this.sigB = sigB;
		this.n = n;
		this.Dmax = Dmax;
	}
	
	// ///////////////// MAIN METHODS FOR GENERAL USE ///////////////////
	
	public String addTimePoint(Table<Double> timePoint, String time, String pointDimName, boolean createHistograms)
	{
		this.p1s = p2s;
		this.p2s = DataPointTracker.convertTable(timePoint, time, pointDimName);
		String path = null;
		if(p1s.size() == 0)
		{
			// Shortcut for first set of points added.
			this.pairs = new Vector<Pair<DataPoint,DataPoint>>();
			for (DataPoint p2 : p2s)
			{
				p2.id = this.assignableID;
				this.assignableID = this.assignableID + 1;
				pairs.add(new Pair<DataPoint,DataPoint>(null, p2));
			}
		}
		else
		{
			this.createPairs(createHistograms); // returns path to a histogram
			// plot of the distances;
			this.resolveConflicts();
			this.assignFinalIDs();
		}
		return path;
	}
	
	public TreeMap<DimensionMap,Double> getTrackedTimePoint()
	{
		TreeMap<DimensionMap,Double> data = new TreeMap<DimensionMap,Double>();
		TreeMap<DimensionMap,Double> rows;
		for (DataPoint p : p2s)
		{
			rows = DataPointTracker.makeTableRowsForDataPoint(p, "Track");
			for (Entry<DimensionMap,Double> e : rows.entrySet())
			{
				data.put(e.getKey(), e.getValue());
			}
		}
		return data;
	}
	
	// ///////////////// METHODS FOR TRACKING DATA POINTS ///////////////
	
	public void createPairs(boolean createHistograms)
	{
		this.pairs = new Vector<Pair<DataPoint,DataPoint>>();
		Vector<Double> ds = new Vector<Double>();
		Vector<Double> fcs = new Vector<Double>();
		for (DataPoint p2 : p2s)
		{
			DataPoint p1 = this.getPairForP2(p1s, p2);
			double d = this.getDistance(p1, p2);
			double fc = this.getFoldChange(p1, p2);
			ds.add(d);
			fcs.add(fc);
			if(p1 != null && d <= Dmax)
			{
				pairs.add(new Pair<DataPoint,DataPoint>(p1, p2));
			}
			else
			{
				pairs.add(new Pair<DataPoint,DataPoint>(null, p2));
			}
		}
		
		Collections.sort(ds);
		Collections.sort(fcs);
		this.medianD = ds.get(ds.size() / 2);
		this.medianFC = fcs.get(fcs.size() / 2);
		if(createHistograms)
		{
			this.histD = ImageUtility.makeHistogramPlot(ds, 0, 20, 0.5, 50, false, this.medianD);
			this.histFC = ImageUtility.makeHistogramPlot(fcs, 0, 0.75, 0.5, 50, false, this.medianFC);
		}
		else
		{
			this.histD = null;
			this.histFC = null;
		}
	}
	
	public void resolveConflicts()
	{
		// Sort the pairs list into ascending order of distance between the
		// points within the pair
		this.sortPairs();
		
		// Take the first occurrence of each
		Vector<Pair<DataPoint,DataPoint>> resolved = new Vector<Pair<DataPoint,DataPoint>>();
		for (Pair<DataPoint,DataPoint> pair : pairs)
		{
			// If this pair connects a point p1 with a point p2 that is already
			// connected to a different point p1
			// (i.e., already exists in the resolved list), eliminate the
			// connection from p1 to p2
			if(hasConflict(resolved, pair))
			{
				pair.p2 = null; // Eliminate the conflicting connection with p2
				// that already exists in a different pair
				resolved.add(pair);
			}
			else
			{
				resolved.add(pair);
			}
		}
		
		this.pairs = resolved;
	}
	
	public void assignFinalIDs()
	{
		for (Pair<DataPoint,DataPoint> pair : pairs)
		{
			if(pair.p1 == null)
			{
				pair.p2.id = assignableID;
				assignableID = assignableID + 1;
			}
		}
	}
	
	public void sortPairs()
	{
		// Resort resolved list before returning
		Collections.sort(this.pairs, this);
	}
	
	public DataPoint getPairForP2(Vector<DataPoint> p1s, DataPoint p2)
	{
		double minDist = Double.MAX_VALUE;
		DataPoint closest = null;
		for (DataPoint p1 : p1s)
		{
			Double dist = this.calculateDiffMetric_v1(p1, p2);
			if(dist < minDist)
			{
				closest = p1;
				minDist = dist;
			}
		}
		return closest;
	}
	
	private static boolean hasConflict(Vector<Pair<DataPoint,DataPoint>> pairs, Pair<DataPoint,DataPoint> p)
	{
		if(p.p2 == null)
		{
			return false;
		}
		for (Pair<DataPoint,DataPoint> pair : pairs)
		{
			if(pair.p2.id == p.p2.id)
			{
				return true;
			}
		}
		return false;
	}
	
	// //////////// DIFFERENCE METRIC ///////////////
	
	public Double calculateDiffMetric_v1(DataPoint p1, DataPoint p2)
	{
		Double ret = W * getDistance(p1, p2) + (1 - W) * getFoldChange(p1, p2);
		return ret;
	}
	
	public double getDistance(DataPoint p1, DataPoint p2)
	{
		if(p1 == null)
		{
			return Double.MIN_VALUE;
		}
		else if(p2 == null)
		{
			return Double.MAX_VALUE;
		}
		double dX = Math.pow(p2.get(SingleCellUtility.x) - p1.get(SingleCellUtility.x), 2);
		double dY = Math.pow(p2.get(SingleCellUtility.y) - p1.get(SingleCellUtility.y), 2);
		return Math.sqrt(dX + dY);
		
	}
	
	public double getFoldChange(DataPoint p1, DataPoint p2)
	{
		if(p1 == null)
		{
			return Double.MIN_VALUE;
		}
		else if(p2 == null)
		{
			return Double.MAX_VALUE;
		}
		double dR = Math.abs((p2.get(SingleCellUtility.r) - p1.get(SingleCellUtility.r)) / (p1.get(SingleCellUtility.r) + n * sigR));
		double dG = Math.abs((p2.get(SingleCellUtility.g) - p1.get(SingleCellUtility.g)) / (p1.get(SingleCellUtility.g) + n * sigG));
		double dB = Math.abs((p2.get(SingleCellUtility.b) - p1.get(SingleCellUtility.b)) / (p1.get(SingleCellUtility.b) + n * sigB));
		Double ret = (dR + dG + dB);
		return ret;
	}
	
	// ////////////// COMPARE METHOD FOR SORTING PAIRS OF DATAPOINTS
	// /////////////
	
	public int compare(Pair<DataPoint,DataPoint> pair1, Pair<DataPoint,DataPoint> pair2)
	{
		return (int) Math.signum(this.calculateDiffMetric_v1(pair1.p1, pair1.p2) - this.calculateDiffMetric_v1(pair2.p1, pair2.p2));
	}
	
	// ///////////// HELPER METHODS FOR WORKING WITH DATA ///////////////////
	
	public static Vector<DataPoint> convertTable(Table<Double> data, String time, String pointDimName)
	{
		Vector<DataPoint> ret = new Vector<DataPoint>();
		Dim pointDim = data.dimTable.getDimWithName(pointDimName);
		DataPoint p;
		for (String pointID : pointDim.dimValues)
		{
			p = DataPointTracker.makeDataPoint(data, time, pointDimName, Integer.parseInt(pointID));
			if(p != null)
			{
				ret.add(p);
			}
		}
		return ret;
	}
	
	public static DataPoint makeDataPoint(Table<Double> data, String time, String pointDimName, int pointID)
	{
		DataPoint ret = new DataPoint();
		ret.id = pointID;
		ret.time = time;
		for (DimensionMap map : data.dimTable.getMapIterator(new DimensionMap(pointDimName + "=" + pointID)))
		{
			Double val = data.getData(map);
			if(val == null)
			{
				return null;
			}
			ret.put(map.get("Measurement"), val);
		}
		return ret;
	}
	
	public static TreeMap<DimensionMap,Double> makeTableRowsForDataPoint(DataPoint p, String pointDimName)
	{
		TreeMap<DimensionMap,Double> ret = new TreeMap<DimensionMap,Double>();
		DimensionMap map = new DimensionMap();
		map.put("Time", p.time);
		map.put(pointDimName, "" + p.id);
		
		for (Entry<String,Double> val : p.entrySet())
		{
			map.put("Measurement", val.getKey());
			ret.put(map.copy(), val.getValue());
		}
		
		return ret;
	}
	
}
