package tables;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;

import miscellaneous.CSVList;
import miscellaneous.Copiable;
import miscellaneous.StringUtility;
import weka.core.Attribute;

public class Dim implements Copiable<Dim> {
	
	public final String dimName;
	public final Vector<String> dimValues;
	public TreeSet<String> dimValueSet;
	
	public Dim(String dimName, String[] dimValues)
	{
		this(dimName, Arrays.asList(dimValues));
	}
	
	private void updateDimValueSet()
	{
		this.dimValueSet = new TreeSet<String>(new StringUtility());
		this.dimValueSet.addAll(this.dimValues);
	}
	
	public Dim(String dimName, Collection<String> values)
	{
		this.dimName = dimName;
		this.dimValues = new Vector<String>();
		this.dimValues.addAll(values);
		this.updateDimValueSet();
	}
	
	public Dim(Attribute att)
	{
		this.dimName = att.name();
		this.dimValues = new Vector<String>();
		for (int i = 0; i < att.numValues(); i++)
		{
			this.dimValues.add(att.value(i));
		}
		this.updateDimValueSet();
	}
	
	public Dim(String csvString)
	{
		CSVList csv = new CSVList(csvString);
		this.dimName = csv.get(0);
		this.dimValues = new Vector<String>();
		this.dimValues.addAll(csv.subList(1, csv.size()));
		this.updateDimValueSet();
	}
	
	public Dim(String name, int max)
	{
		this(name, 1, max);
	}
	
	public Dim(String name, int min, int max)
	{
		this.dimName = name;
		Vector<String> v = new Vector<String>((max - min + 1));
		for (int i = min; i < max + 1; i++)
		{
			v.add("" + i);
		}
		this.dimValues = v;
		this.updateDimValueSet();
	}
	
	public Dim(String name, double min, double max, int steps)
	{
		DecimalFormat format = new DecimalFormat("0.00");
		this.dimName = name;
		double interval = ((max - min) / (steps - 1));
		Vector<String> v = new Vector<String>(steps);
		for (int i = 0; i < steps; i++)
		{
			v.add(format.format(((i * interval) + min)));
		}
		this.dimValues = v;
		this.updateDimValueSet();
	}
	
	public Dim(String dimName, String csvValues)
	{
		this.dimName = dimName;
		this.dimValues = new CSVList(csvValues);
		this.updateDimValueSet();
	}
	
	public String name()
	{
		return this.dimName;
	}
	
	public List<String> values()
	{
		return new Vector<String>(this.dimValues);
	}
	
	public Vector<Double> doubleValues()
	{
		Vector<Double> ret = new Vector<Double>();
		for (String s : this.dimValues)
		{
			ret.add(Double.parseDouble(s));
		}
		return ret;
	}
	
	/**
	 * index is exclusive
	 * 
	 * @param index
	 * @return
	 */
	public List<String> valuesUpThrough(int index)
	{
		if(index > this.size() - 1)
		{
			index = this.size() - 1;
		}
		else if(index < 0)
		{
			index = 0;
		}
		return this.dimValues.subList(0, index + 1);
	}
	
	/**
	 * index is inclusive
	 * 
	 * @param index
	 * @return
	 */
	public List<String> valuesStartingAt(int index)
	{
		if(index > this.size() - 1)
		{
			index = this.size() - 1;
		}
		else if(index < 0)
		{
			index = 0;
		}
		return this.dimValues.subList(index, this.size());
	}
	
	public String[] valueArray()
	{
		String[] result = this.dimValues.toArray(new String[0]);
		return result;
	}
	
	public String valueAt(int index)
	{
		return this.dimValues.get(index);
	}
	
	public int index(String value)
	{
		return this.dimValues.indexOf(value);
	}
	
	public String min()
	{
		return this.dimValues.get(0);
	}
	
	public String max()
	{
		return this.dimValues.get(this.size() - 1);
	}
	
	public int size()
	{
		return this.dimValues.size();
	}
	
	/**
	 * returned Dim contains no references to dim1 and dim2 or their values. (deep copy)
	 * 
	 * @param dim1
	 * @param dim2
	 * @return
	 */
	public static Dim union(Dim dim1, Dim dim2)
	{
		if(!dim1.name().equals(dim2.name()))
		{
			return null;
		}
		
		List<String> newDimValues = new Vector<String>();
		newDimValues.addAll(dim1.dimValues);
		
		for (String s2 : dim2.dimValues)
		{
			if(!dim1.containsValue(s2))
			{
				newDimValues.add(s2);
			}
		}
		return new Dim(dim1.name(), newDimValues);
	}
	
	/**
	 * returned Dim contains no references to dim1 and dim2 or their values. (deep copy)
	 * 
	 * @param dim1
	 * @param dim2
	 * @return
	 */
	public static Dim intersect(Dim dim1, Dim dim2)
	{
		if(!dim1.name().equals(dim2.name()))
		{
			return null;
		}
		
		List<String> newDimValues = new Vector<String>();
		for (String s2 : dim2.dimValues)
		{
			if(dim1.containsValue(s2))
			{
				newDimValues.add(s2);
			}
		}
		return new Dim(dim1.name(), newDimValues);
	}
	
	@Override
	public String toString()
	{
		return this.toCSVString();
	}
	
	public String toCSVString()
	{
		if(this.dimName == null || this.dimValues == null)
		{
			return "";
		}
		
		String result = this.dimName;
		for (String value : this.dimValues)
		{
			result = result + "," + value;
		}
		return result;
	}
	
	@Override
	public Dim copy()
	{
		// Use constructor which copies each field over
		Dim ret = new Dim(this.name(), this.dimValues);
		return ret;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Dim))
		{
			return false;
		}
		Dim d = (Dim) o;
		if(!this.name().equals(d.name()))
		{
			return false;
		}
		return this.values().equals(d.values());
	}
	
	public boolean containsValue(String value)
	{
		return this.dimValueSet.contains(value);
	}
	
	public Attribute toArffAttribute()
	{
		return new Attribute(this.name(), this.values());
	}
}
