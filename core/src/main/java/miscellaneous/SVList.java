package miscellaneous;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * This class Separate Variable List (SVList) 
 * separates a collection of variables and save each variable into a String Vector
 * by specifying separators
 * 
 * @author Jay Warrick, commented by Mengcheng
 *
 */
public class SVList extends Vector<String> {
	
	private static final long serialVersionUID = 1L;
	// separator for variable splitting up
	protected String separatorRegex;
	// separator for variable building back 
	protected String separator;
	
	/**
	 * Class constructor specifying separators 
	 * for variable splitting up and building back
	 * 
	 * @param sepReg a separator string
	 * @param sep a separator string
	 */
	public SVList(String sepReg, String sep)
	{
		super();
		this.separatorRegex = sepReg;
		this.separator = sep;
	}
	
	/**
	 * Class constructor 
	 * Split String svl by sepReg, add each segment of svl to the String Vector 
	 * 	 
	 * @param svl a input string need to be split up
	 * @param sepReg a separator string
	 * @param sep a separator string
	 */
	public SVList(String svl, String sepReg, String sep)
	{
		this(sepReg, sep);
		this.add(svl.split(this.separatorRegex));
	}
	
	
	/**
	 * Class constructor 
	 * Add all strings inside the given List to the String Vector
	 * 
	 * @param values a String List
	 * @param sepReg a separator string
	 * @param sep a separator string
	 */
	public SVList(List<String> values, String sepReg, String sep)
	{
		this(sepReg, sep);
		this.addAll(values);
	}
	
	/**
	 * Class constructor 
	 * Add certain element in a List of String array given array index into the String Vector 
	 * 
	 * @param values a List of String array
	 * @param arrayIndex
	 * @param sepReg a separator string
	 * @param sep a separator string
	 */
	public SVList(List<String[]> values, int arrayIndex, String sepReg, String sep)
	{
		this(sepReg, sep);
		
		// check parameters
		if(values == null)
		{
			return;
		}
		if(arrayIndex < 0 || arrayIndex >= values.size())
		{
			System.out.println("Warning in SVList(List<String[], int arrayIndex) - invalid arrayIndex");
			return;
		}
		
		// add each element of String array inside the List values into the String Vector 
		Iterator<String[]> itr = values.iterator();
		while (itr.hasNext())
		{
			// add the string specified by the given index
			this.add(itr.next()[arrayIndex]);
		}
	}
	
	/**
	 * Class constructor 
	 * Add all Objects inside the given Object array to the String Vector
	 * 
	 * @param s an Object array
	 * @param sepReg a separator string
	 * @param sep a separator string
	 */
	public SVList(Object[] s, String sepReg, String sep)
	{
		this(sepReg, sep);
		this.add(s);
	}
	
	/**
	 * Insert all variables in the string to the front of the String Vector
	 * 
	 * @param svl a string contains variables
	 */
	public void prepend(String svl)
	{
		// create a new SVList with the same separators 
		SVList toAdd = new SVList(svl, this.separatorRegex, this.separator);
		for (int i = toAdd.size() - 1; i >= 0; i--)
		{
			// insert the string to the front
			this.add(0, toAdd.get(i));
		}
	}
	
	/**
	 * Add all variables in the string to the end of the String Vector
	 * 
	 * @param svl a string contains variables
	 */
	public void append(String svl)
	{
		SVList toAdd = new SVList(svl, this.separatorRegex, this.separator);
		Iterator<String> itr = toAdd.iterator();
		while (itr.hasNext())
		{
			this.add(itr.next());
		}
	}
	
	/**
	 * @param values
	 */
	@SuppressWarnings({ "unused", "rawtypes" })
	private void add(List values)
	{
		// if(values == null) return;
		// if the list hold Object[]'s big enough, add the second item
		if(values.size() > 0 && values.get(0).getClass().isArray() && Array.getLength(values.get(0)) > 1)
		{
			Iterator itr = values.iterator();
			while (itr.hasNext())
			{
				this.add(((Object[]) itr.next())[1].toString());
			}
		}
		// if the list hold some other type of non-array object
		else if(values.size() > 0 && !(values.get(0).getClass().isArray()))
		{
			Iterator itr = values.iterator();
			while (itr.hasNext())
			{
				this.add(itr.next().toString());
			}
		}
	}
	
	/**
	 * Add each element in given object array s to the String Vector
	 * 
	 * @param s an object array
	 */
	private void add(Object[] s)
	{
		// if(s == null) return;
		for (int i = 0; i < s.length; i++)
		{
			this.add(s[i].toString());
		}
	}
	
	
	/* (non-Javadoc)
	 * @see java.util.Vector#toString()
	 */
	@Override
	public String toString()
	{
		return this.toSVString();
	}
	
	/**
	 * ToString
	 * 
	 * @return A String contains all variables, separated by specified separator
	 */
	public String toSVString()
	{
		// Using a string builder is REDICULOUSLY faster. Writing the points for a big roi went from 5 mins to <1 sec !!!!
		if(this.size() > 0)
		{
			StringBuilder ret = new StringBuilder();
			ret.append(this.get(0));
			for (int i = 1; i < this.size(); i++)
			{
				ret.append(this.separator);
				ret.append(this.get(i));
			}
			return ret.toString();
		}
		return "";
	}
	
	/**
	 * ToStringArray
	 * 
	 * @return A String array contains all the variables
	 */
	public String[] toStringArray()
	{
		String[] ret = this.toSVString().split(this.separatorRegex);
		return ret;
	}
	
	/**
	 * A set of variables as a field
	 * Given a prefix, return a Vector of String array
	 * Each String array contains a prefix with an index and a variable
	 * 
	 * @param prefix
	 * @return A Vector of String array
	 */
	public List<String[]> toFields(String prefix)
	{
		List<String[]> ret = new Vector<String[]>();
		for (int i = 0; i < this.size(); i++)
		{
			ret.add(new String[] { prefix + i, this.get(i) });
		}
		if(ret.size() == 0)
		{
			return null;
		}
		return ret;
	}
	
	/**
	 * Merge two Lists of String array with the same size
	 * return a List of String array 
	 * each String array contains two variables from given Lists at the same position
	 * 
	 * @param l another SVList be merged with this one
	 * @return a List of String array
	 */
	public List<String[]> mergePairs(SVList l)
	{
		if(this.size() != l.size())
		{
			System.out.println("Warning in SVList:mergePairs - arguments should be same size");
			return null;
		}
		
		List<String[]> ret = new Vector<String[]>();
		for (int i = 0; i < this.size(); i++)
		{
			ret.add(new String[] { this.get(i), l.get(i) });
		}
		return ret;
	}
	
	
	/**
	 * Compare two SVLists whether they are equal or not
	 * 
	 * @param l another SVList be compared with this one
	 * @return equals or not
	 */
	public boolean equals(SVList l)
	{
		// compare their toString
		return this.toSVString().equals(l.toSVString());
	}
}
