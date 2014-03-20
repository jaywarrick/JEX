package miscellaneous;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class ListUtility {
	
	// ------------------------------------
	// --------- ARRAYLIST UTILITIES ------
	// ------------------------------------
	/**
	 * Concatenate two vectors
	 */
	public static List<Object> concat(List<Object> l1, List<Object> l2)
	{
		for (int k = 0, len = l2.size(); (k < len); k++)
		{
			l1.add(l2.get(k));
		}
		return l1;
	}
	
	/**
	 * Make the intersection of two vectors
	 */
	public static List<Object> intersection(List<Object> l1, List<Object> l2)
	{
		ArrayList<Object> result = new ArrayList<Object>(0);
		for (int k = 0, len = l1.size(); (k < len); k++)
		{
			if(isMember(l1.get(k), l2))
			{
				result.add(l1.get(k));
			}
		}
		return result;
		
	}
	
	/**
	 * Make the union of two vectors
	 */
	public static List<Object> union(List<Object> l1, List<Object> l2)
	{
		for (int k = 0, len = l2.size(); (k < len); k++)
		{
			if(!isMember(l2.get(k), l1))
			{
				l1.add(l2.get(k));
			}
		}
		return l1;
	}
	
	/**
	 * Concatenate two vectors
	 */
	public static List<String> concatStringList(List<String> l1, List<String> l2)
	{
		for (int k = 0, len = l2.size(); (k < len); k++)
		{
			l1.add(l2.get(k));
		}
		return l1;
	}
	
	/**
	 * Make the intersection of two vectors
	 */
	public static List<String> intersectionStringList(List<String> l1, List<String> l2)
	{
		ArrayList<String> result = new ArrayList<String>(0);
		for (int k = 0, len = l1.size(); (k < len); k++)
		{
			if(isStringMember(l1.get(k), l2))
			{
				result.add(l1.get(k));
			}
		}
		return result;
		
	}
	
	/**
	 * Make the union of two vectors
	 */
	public static List<String> unionStringList(List<String> l1, List<String> l2)
	{
		// TreeSet<String> unionedAndAlphaNumericOrderedStrings = new
		// TreeSet<String>(new StringUtility());
		for (int k = 0, len = l2.size(); (k < len); k++)
		{
			if(!isStringMember(l2.get(k), l1))
			{
				l1.add(l2.get(k));
			}
		}
		return l1;
	}
	
	/**
	 * Subtract all elements of l1 that are part of l2
	 */
	public static List<Object> substract(List<Object> l1, List<Object> l2)
	{
		ArrayList<Object> result = new ArrayList<Object>(0);
		for (int k = 0, len = l1.size(); (k < len); k++)
		{
			if(!isMember(l1.get(k), l2))
			{
				result.add(l1.get(k));
			}
		}
		return result;
	}
	
	/**
	 * Output the arraylist to an array
	 * 
	 * @param l
	 * @return
	 */
	public static Object[] vector2Array(List<Object> l)
	{
		Object[] array = new Object[l.size()];
		for (int j = 0, len = l.size(); j < len; j++)
		{
			Object o = l.get(j);
			array[j] = o;
		}
		return array;
	}
	
	/**
	 * Is an object part of a vector?
	 */
	public static boolean isMember(Object o, List<Object> l)
	{
		if(o == null)
			return false;
		for (int k = 0, len = l.size(); (k < len); k++)
		{
			if(o.equals(l.get(k)))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Is an object part of a vector?
	 */
	public static boolean isStringMember(String o, List<String> l)
	{
		if(o == null)
			return false;
		for (int k = 0, len = l.size(); (k < len); k++)
		{
			if(o.equals(l.get(k)))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return the first index of vector v that matches with object o, -1 if none match
	 */
	public static int isMemberIndex(Object o, List<Object> v)
	{
		for (int k = 0, len = v.size(); (k < len); k++)
		{
			if(o.equals(v.get(k)))
			{
				return k;
			}
		}
		return -1;
	}
	
	/**
	 * print the list l into a string
	 * 
	 * @param l
	 * @return
	 */
	public static String printString(List<Object> l)
	{
		String ret = "";
		for (Object o : l)
		{
			ret = ret + " - " + o.toString();
		}
		return ret;
	}
	
	/**
	 * Return true if vector v1 and v2 contain the equal objects in the same order
	 * 
	 * @param v1
	 * @param v2
	 * @return
	 */
	public static boolean areEqual(Vector<Object> v1, Vector<Object> v2)
	{
		if(v1.size() != v2.size())
		{
			return false;
		}
		for (int i = 0, len = v1.size(); i < len; i++)
		{
			if(!v1.get(i).equals(v2.get(i)))
			{
				return false;
			}
		}
		return true;
	}
	
}
