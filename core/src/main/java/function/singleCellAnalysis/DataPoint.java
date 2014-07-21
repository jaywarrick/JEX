package function.singleCellAnalysis;

import java.util.Map.Entry;
import java.util.TreeMap;

public class DataPoint extends TreeMap<String,Double> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public String time;
	public int id;
	
	public String toString()
	{
		StringBuilder s = new StringBuilder();
		s.append("{ID=");
		s.append("" + this.id);
		s.append(", ");
		for (Entry<String,Double> e : this.entrySet())
		{
			s.append(e.getKey());
			s.append("=");
			s.append(e.getValue());
			s.append(", ");
		}
		s.append("Time=");
		s.append(this.time);
		s.append("}");
		return s.toString();
	}
	
}
