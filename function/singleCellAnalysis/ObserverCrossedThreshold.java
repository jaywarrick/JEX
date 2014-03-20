package function.singleCellAnalysis;

import java.util.Map.Entry;
import java.util.TreeMap;

import tables.DimensionMap;

public class ObserverCrossedThreshold implements Observer {
	
	public TreeMap<String,Boolean> observations = new TreeMap<String,Boolean>();
	
	public ObserverCrossedThreshold(String trackDim, String timeDim, String colorDim, String colorValue, TreeMap<DimensionMap,Double> dataT1, TreeMap<DimensionMap,Double> dataT2, double threshold)
	{
		String T2Key = dataT2.firstEntry().getKey().get(timeDim);
		for (Entry<DimensionMap,Double> e : dataT1.entrySet())
		{
			DimensionMap mapT1 = e.getKey();
			if(!mapT1.get(colorDim).equals(colorValue))
			{
				continue;
			}
			DimensionMap mapT2 = mapT1.copy();
			mapT2.put(timeDim, T2Key);
			Double dataPointT1 = e.getValue();
			Double dataPointT2 = dataT2.get(mapT2);
			if(dataPointT2 == null)
			{
				continue;
			}
			if(dataPointT1 < threshold && dataPointT2 > threshold)
			{
				observations.put(mapT2.get(trackDim), true);
			}
			else
			{
				observations.put(mapT2.get(trackDim), false);
			}
		}
	}
	
	public boolean wasEventObservedForCell(String cellID)
	{
		return this.observations.get(cellID);
	}
	
}
