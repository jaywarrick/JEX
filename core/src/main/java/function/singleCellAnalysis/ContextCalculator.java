package function.singleCellAnalysis;

import java.util.TreeMap;

import tables.DimensionMap;

public interface ContextCalculator {
	
	public static String INFO = "Info";
	
	public TreeMap<DimensionMap,Double> getContextForCell(String cellID);
	
}
