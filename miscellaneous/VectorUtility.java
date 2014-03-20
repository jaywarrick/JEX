package miscellaneous;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class VectorUtility {
	
	/**
	 * Find the index of the minimum of vector v
	 * 
	 * @param v
	 * @return
	 */
	public static <E extends Number> int findMinimum(List<E> v)
	{
		if((v == null) || (v.size() == 0))
		{
			return -1;
		}
		
		int result = 0;
		double value = v.get(0).doubleValue();
		for (int k = 0, len = v.size(); (k < len); k++)
		{
			if(v.get(k).doubleValue() < value)
			{
				result = k;
				value = v.get(k).doubleValue();
			}
		}
		
		return result;
	}
	
	/**
	 * Find the index of the minimum of vector v
	 * 
	 * @param v
	 * @return
	 */
	public static <E extends Number> int findMaximum(List<E> v)
	{
		if((v == null) || (v.size() == 0))
		{
			return -1;
		}
		
		int result = 0;
		double value = v.get(0).doubleValue();
		for (int k = 0, len = v.size(); (k < len); k++)
		{
			if(v.get(k).doubleValue() > value)
			{
				result = k;
				value = v.get(k).doubleValue();
			}
		}
		
		return result;
	}
	
	/**
	 * Return the maximum of Collection<Number> values
	 * 
	 * @param v
	 * @return
	 */
	public static <E extends Number> E getMaximum(Collection<E> values)
	{
		if((values == null) || (values.size() == 0))
		{
			return null;
		}
		
		E max = null;
		for (E value : values)
		{
			if(max == null || value.doubleValue() > max.doubleValue())
			{
				max = value;
			}
		}
		
		return max;
	}
	
	/**
	 * Return the maximum of Collection<Number> values
	 * 
	 * @param v
	 * @return
	 */
	public static <E extends Number> E getMinimum(Collection<E> values)
	{
		if((values == null) || (values.size() == 0))
		{
			return null;
		}
		
		E min = null;
		for (E value : values)
		{
			if(min == null || value.doubleValue() < min.doubleValue())
			{
				min = value;
			}
		}
		
		return min;
	}
	
	// return a vector of set length filled with the same object
	public static List<Double> ones(Double d, int length)
	{
		List<Double> result = new Vector<Double>(0);
		
		for (int k = 0; (k < length); k++)
		{
			result.add(d);
		}
		
		return result;
	}
	
	/**
	 * Return a vector filled with increasing integers from MIN of length LENGTH
	 * 
	 * @param min
	 * @param length
	 * @return
	 */
	public static List<Integer> lineSpace(int min, int length)
	{
		List<Integer> result = new Vector<Integer>(0);
		
		int k = min;
		while (result.size() <= length)
		{
			result.add(k);
			k++;
		}
		
		return result;
	}
	
	/**
	 * Return the mean of list L
	 */
	public static double mean(List<Double> l)
	{
		double result = 0;
		
		for (Double d : l)
		{
			// System.out.println("*** d="+d+"... d == NaN ? "+(d ==
			// Double.NaN)+" isNan?"+d.isNaN());
			if(d.isNaN())
			{
				continue;
			}
			result = result + d;
		}
		result = result / l.size();
		
		return result;
	}
	
	/**
	 * Return the sum of the list L
	 * 
	 * @param l
	 * @return
	 */
	public static double sum(List<Double> l)
	{
		double result = 0;
		
		for (Double d : l)
		{
			result = result + d;
		}
		
		return result;
	}
}
