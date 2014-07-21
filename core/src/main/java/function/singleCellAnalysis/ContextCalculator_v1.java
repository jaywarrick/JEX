package function.singleCellAnalysis;

import image.roi.HashedPointList;
import image.roi.IdPoint;
import image.roi.PointList;

import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import miscellaneous.Pair;
import tables.DimensionMap;

public class ContextCalculator_v1 implements ContextCalculator {
	
	public HashedPointList locations;
	TreeMap<Integer,TreeMap<String,Double>> data = new TreeMap<Integer,TreeMap<String,Double>>();
	String trackDim;
	
	public ContextCalculator_v1(TreeMap<DimensionMap,Double> timePt, String trackDim, String colorDim, double radius)
	{
		this.trackDim = trackDim;
		DimensionMap map;
		JEXStatics.statusBar.setStatusText("ContextCalculator_v1 --> Gathering data: 0%");
		double count = 0, tot = timePt.entrySet().size();
		if(colorDim != null) // Then it is an old Data Table
		{
			for (Entry<DimensionMap,Double> e : timePt.entrySet())
			{
				map = e.getKey();
				Integer cell = Integer.parseInt(map.get(trackDim));
				String measurement = map.get("Measurement");
				String color = map.get(colorDim);
				if(measurement.equals("7"))
				{
					this.add(cell, SingleCellUtility.x, e.getValue());
				}
				else if(measurement.equals("8"))
				{
					this.add(cell, SingleCellUtility.y, e.getValue());
				}
				else if(measurement.equals("1"))
				{
					if(color.equals("0"))
					{
						this.add(cell, SingleCellUtility.b, e.getValue());
					}
					else if(color.equals("1"))
					{
						this.add(cell, SingleCellUtility.g, e.getValue());
					}
					else
					// (color.equals("2"))
					{
						this.add(cell, SingleCellUtility.r, e.getValue());
					}
				} // else don't do anything
			}
			JEXStatics.statusBar.setStatusText("ContextCalculator_v1 --> Gathering data: " + ((int) 100 * count / tot) + "%");
		}
		else
		// all information is contained in "Measurement" dim
		{
			JEXStatics.statusBar.setStatusText("ContextCalculator_v1 --> Gathering data: 0%");
			for (Entry<DimensionMap,Double> e : timePt.entrySet())
			{
				map = e.getKey();
				Integer cell = Integer.parseInt(map.get(trackDim));
				String measurement = map.get("Measurement");
				this.add(cell, measurement, e.getValue());
			}
			JEXStatics.statusBar.setStatusText("ContextCalculator_v1 --> Gathering data: " + ((int) 100 * count / tot) + "%");
		}
		
		// make the HashedPointList of the xy locations
		PointList temp = new PointList();
		JEXStatics.statusBar.setStatusText("ContextCalculator_v1 --> Hashing xy locations");
		for (Entry<Integer,TreeMap<String,Double>> e : data.entrySet())
		{
			temp.add(new IdPoint(e.getValue().get(SingleCellUtility.x).intValue(), e.getValue().get(SingleCellUtility.y).intValue(), e.getKey()));
		}
		this.locations = new HashedPointList(temp);
		
		count = 0;
		tot = data.entrySet().size();
		JEXStatics.statusBar.setStatusText("ContextCalculator_v1 --> Calculating contextual info"); // :
		// " + ((int) (100*count/tot)) + "%");
		for (Entry<Integer,TreeMap<String,Double>> e : data.entrySet())
		{
			this.getContextForCell(e.getKey(), radius);
			// JEXStatics.statusBar.setStatusText("ContextCalculator_v1 --> Calculating contextual info: "
			// + ((int) (100*count/tot)) + "%");
			count = count + 1;
		}
	}
	
	public void getContextForCell(Integer cell, double radius)
	{
		Vector<Pair<IdPoint,Double>> neighbors = this.locations.getNeighbors(this.locations.idHash.get(cell), radius, false);
		double green = 0.0;
		double red = 0.0;
		double blue = 0.0;
		TreeMap<String,Double> measurements;
		if(neighbors != null)
		{
			for (Pair<IdPoint,Double> p : neighbors)
			{
				if(p.p1.id != cell) // Don't count yourself
				{
					measurements = this.data.get(p.p1.id);
					blue = blue + measurements.get(SingleCellUtility.b) / p.p2;
					green = green + measurements.get(SingleCellUtility.g) / p.p2;
					red = red + measurements.get(SingleCellUtility.r) / p.p2;
				}
			}
		}
		TreeMap<String,Double> pt = data.get(cell);
		pt.put(SingleCellUtility.bN, blue);
		pt.put(SingleCellUtility.gN, green);
		pt.put(SingleCellUtility.rN, red);
	}
	
	public void add(Integer cell, String key, Double val)
	{
		TreeMap<String,Double> pt = data.get(cell);
		if(pt == null)
		{
			pt = new TreeMap<String,Double>();
			data.put(cell, pt);
		}
		pt.put(key, val);
	}
	
	public TreeMap<DimensionMap,Double> getContextForCell(String cell)
	{
		TreeMap<DimensionMap,Double> ret = new TreeMap<DimensionMap,Double>();
		DimensionMap map = new DimensionMap(trackDim + "=" + cell);
		TreeMap<String,Double> measurements = data.get(Integer.parseInt(cell));
		if(measurements == null)
			return ret;
		for (Entry<String,Double> e : measurements.entrySet())
		{
			ret.put(map.copyAndSet("Measurement=" + e.getKey()), e.getValue());
		}
		return ret;
	}
	
}
