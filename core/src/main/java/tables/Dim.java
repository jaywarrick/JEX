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

/**
 * @author Jay Warrick, commented by Mengcheng
 *
 */
public class Dim implements Copiable<Dim> {
	
	// dimName, e.g. imRow, imCol, color
	public final String dimName;
	// dimValues, a string vector, e.g. {0, 1, 2, 3} if imRow = 4 means 4 rows.
	public final Vector<String> dimValues;
	// dimValueSet, a string TreeSet, e.g. {0, 1, 2, 3} if imRow = 4 means 4 rows.
	public TreeSet<String> dimValueSet;
	
	
	/**
	 * Class constructor specifying dimName and dimValues (array of string)
	 * 
	 * @param dimName the name of the dimension
	 * @param dimValues the value set of the dimension
	 */
	public Dim(String dimName, String[] dimValues)
	{
		// call class constructor below, convert dimValues into a List
		this(dimName, Arrays.asList(dimValues));
	}
	
	/**
	 * Class constructor specifying dimName and dimValues (collection of string)
	 * 
	 * @param dimName the name of the dimension
	 * @param values 
	 */
	public Dim(String dimName, Collection<String> values)
	{
		this.dimName = dimName;
		this.dimValues = new Vector<String>();
		this.dimValues.addAll(values);
		this.updateDimValueSet();
	}
	
	
	/**
	 * Add new dimValues to dimValueSet
	 */
	public void updateDimValueSet()
	{
		this.dimValueSet = new TreeSet<String>(new StringUtility());
		this.dimValueSet.addAll(this.dimValues);
	}
	
	/**
	 * Class constructor specifying attribute
	 * note - weka.core.Attribute
	 * 
	 * @param att an Attribute object
	 */
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
	
	/**
	 * Class constructor specifying csvString
	 * 
	 * Either of two formats is ok.
	 * Format1 <DimName>,<V1>,<V2>,...
	 * Format2 <DimName>=<V1>,<V2>,...
	 * 
	 * @param csvString
	 */
	public Dim(String csvString)
	{
		csvString = StringUtility.removeWhiteSpaceOnEnds(csvString);
		
		if(csvString.contains("="))
		{
			String[] split = csvString.split("=");
			this.dimName = StringUtility.removeWhiteSpaceOnEnds(split[0]);
			CSVList csv = new CSVList(StringUtility.removeWhiteSpaceOnEnds(split[1]));
			this.dimValues = new Vector<String>();
			for(String s : csv)
			{
				this.dimValues.add(StringUtility.removeWhiteSpaceOnEnds(s));
			}
		}
		else
		{
			CSVList csv = new CSVList(csvString);
			for(int i = 0; i < csv.size(); i++)
			{
				csv.set(i, StringUtility.removeWhiteSpaceOnEnds(csv.get(i)));
			}
			this.dimName = StringUtility.removeWhiteSpaceOnEnds(csv.get(0));
			this.dimValues = new Vector<String>();
			this.dimValues.addAll(csv.subList(1, csv.size()));
		}
		this.updateDimValueSet();
	}
	
	/**
	 * Class constructor specifying dimName, dimValue max
	 * create dimValues from 1 to max increment by 1
	 * 
	 * @param name
	 * @param max integer
	 */
	public Dim(String name, int max)
	{
		this(name, 1, max);
	}
	
	/**
	 * Class constructor specifying dimName, dimValue max and min
	 * create dimValues from min to max increment by 1
	 * 
	 * @param name
	 * @param min integer
	 * @param max integer
	 */
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
	
	/**
	 * Class constructor specifying dimName, dimValue max, min, and steps
	 * create dimValues from min to max by specified steps
	 * 
	 * @param name
	 * @param min double
	 * @param max double
	 * @param steps integer
	 */
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
	
	/**
	 * Class constructor specifying dimName, dimValues
	 * create dimValues as a CSVList
	 * 
	 * @param dimName
	 * @param csvValues a CSV string
	 */
	public Dim(String dimName, String csvValues)
	{
		this.dimName = dimName;
		this.dimValues = new CSVList(csvValues);
		this.updateDimValueSet();
	}
	
	/**
	 * returned dim name
	 * 
	 * @return dim name
	 */
	public String name()
	{
		return this.dimName;
	}
	
	/**
	 * returned dim values
	 * 
	 * @return dim values, a string vector
	 */
	public List<String> values()
	{
		return new Vector<String>(this.dimValues);
	}
	
	/**
	 * returned dim values
	 * 
	 * @return dim values, a double vector
	 */
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
	 * returned a sublist of dim values [0:index]
	 * index is inclusive
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
		// subList includes 0, excludes index+1
		return this.dimValues.subList(0, index + 1);
	}
	
	/**
	 * returned a sublist of dim values [index:end]
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
	
	/**
	 * returned dim values as an string array 
	 * 
	 * @return
	 */
	public String[] valueArray()
	{
		String[] result = this.dimValues.toArray(new String[0]);
		return result;
	}
	
	/**
	 * returned dimValue at specified index (vector index)
	 * 
	 * @param index integer
	 * @return dim value string
	 */
	public String valueAt(int index)
	{
		return this.dimValues.get(index);
	}
	
	/**
	 * returned index of given dim value
	 * 
	 * @param value string
	 * @return index integer
	 */
	public int index(String value)
	{
		return this.dimValues.indexOf(value);
	}
	
	/**
	 * returned minimum dim value
	 * 
	 * @return min dim value string
	 */
	public String min()
	{
		return this.dimValues.get(0);
	}
	
	/**
	 * returned maximum dim value
	 * 
	 * @return max dim value string
	 */
	public String max()
	{
		return this.dimValues.get(this.size() - 1);
	}
	
	/**
	 * returned the total number of dim values
	 * 
	 * @return size of dimValues integer
	 */
	public int size()
	{
		return this.dimValues.size();
	}
	
	/**
	 * returned Dim contains no references to dim1 and dim2 or their values. (deep copy)
	 * a union dimValues of dim1 and dim2
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
	 * a intersect dimValues of dim1 and dim2
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
	
	/**
	 * returned Dim contains no references to dim1 and dim2 or their values. (deep copy)
	 * a subtraction of dimValues (dim1 minus dim2)
	 * If names don't match... return null
	 * 
	 * @param dim1
	 * @param dim2
	 * @return
	 */
	public static Dim subtract(Dim dim1, Dim dim2)
	{
		if(!dim1.name().equals(dim2.name()))
		{
			return null;
		}
		
		List<String> newDimValues = new Vector<String>();
		for (String s1 : dim1.dimValues)
		{
			if(!dim2.containsValue(s1))
			{
				newDimValues.add(s1);
			}
		}
		return new Dim(dim1.name(), newDimValues);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return this.toCSVString();
	}
	
	/**
	 * returned a string contains dim name and dim values separated by comma
	 * 
	 * @return CSVList in string
	 */
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
	
	/* (non-Javadoc)
	 * @see miscellaneous.Copiable#copy()
	 */
	@Override
	public Dim copy()
	{
		// Use constructor which copies each field over
		Dim ret = new Dim(this.name(), this.dimValues);
		return ret;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
	
	/**
	 * returned true if dimValueSet contains the given value
	 * TreeSet searching
	 * 
	 * @param value string
	 * @return boolean
	 */
	public boolean containsValue(String value)
	{
		return this.dimValueSet.contains(value);
	}
	
	/**
	 * returned an Attribute of CSVList contains dimName and dimValues
	 * 
	 * @return Attribute
	 */
	public Attribute toArffAttribute()
	{
		return new Attribute(this.name(), this.values());
	}
}
