package miscellaneous;

//
//  Calculator.java
//  MicroFluidicHT_Tools
//
//  Created by erwin berthier on 5/22/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//

import java.util.List;
import java.util.Vector;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

public class Calculator {
	
	public Calculator()
	{   
		
	}
	
	// --------------------------------------------
	// -- GENERAL UTILITY Functions
	// --------------------------------------------
	
	/**
	 * convert a string vector to a Double array
	 */
	public Double[] stringVectToFloat(Vector<String> v)
	{
		// Vector result = new Vector(0);
		Double[] result = new Double[v.size()];
		for (int i = 0, len = v.size(); (i < len); i++)
		{
			// result.add(Double.parseDouble((String)v.get(i)));
			result[i] = Double.parseDouble(v.get(i));
		}
		return result;
	}
	
	/**
	 * convert a string vector to a double array
	 */
	public double[] stringVectToDouble(Vector<String> v)
	{
		// Vector result = new Vector(0);
		double[] result = new double[v.size()];
		for (int i = 0, len = v.size(); (i < len); i++)
		{
			// result.add(Double.parseDouble((String)v.get(i)));
			result[i] = Double.parseDouble(v.get(i));
		}
		return result;
	}
	
	/**
	 * Converts a Double array into a flaot vector
	 */
	public Vector<String> floatVectToString(Double[] v)
	{
		Vector<String> result = new Vector<String>(0);
		for (int i = 0, len = v.length; (i < len); i++)
		{
			result.add("" + v[i]);
		}
		return result;
	}
	
	/**
	 * Concatenates two list of obects
	 */
	public List<Object> concat(List<Object> l1, List<Object> l2)
	{
		for (int i = 0, len = l2.size(); (i < len); i++)
		{
			l1.add(l2.get(i));
		}
		return l1;
	}
	
	// --------------------------------------------
	// -- Statistics Functions
	// --------------------------------------------
	
	/**
	 * Return the statistics on a vector of strings v, in order min, max, mean and stdDev
	 */
	public static Vector<String> calculateStatistics(Vector<String> v)
	{
		System.out.println("  Calculator: calculating statistics");
		DescriptiveStatistics statVect = new DescriptiveStatistics(v.size());
		String nullString = "-";
		for (int i = 0, len = v.size(); (i < len); i++)
		{
			String str = v.get(i);
			if(str == null)
			{
				v.set(i, nullString);
				continue;
			}
			try
			{
				double d = Double.parseDouble(v.get(i));
				statVect.addValue(d);
			}
			catch (NumberFormatException e)
			{
				continue;
			}
		}
		Double min = statVect.getMin();
		Double max = statVect.getMax();
		Double mean = statVect.getMean();
		Double variance = statVect.getVariance();
		Double stdDev = Math.sqrt(variance);
		v.add(0, "-----");
		v.add(0, "" + stdDev.toString());
		v.add(0, "" + mean.toString());
		v.add(0, "" + max.toString());
		v.add(0, "" + min.toString());
		return v;
	}
	
	/**
	 * Return the statistics on a vector of strings v, in order min, max, mean and stdDev
	 */
	public static Vector<Double> calculateDoubleStatistics(Vector<Double> v)
	{
		// System.out.println("  Calculator: calculating statistics");
		DescriptiveStatistics statVect = new DescriptiveStatistics(4);
		for (int i = 0, len = v.size(); (i < len); i++)
		{
			double d = v.get(i);
			try
			{
				statVect.addValue(d);
			}
			catch (NumberFormatException e)
			{
				continue;
			}
		}
		
		Double min = statVect.getMin();
		Double max = statVect.getMax();
		Double mean = statVect.getMean();
		Double variance = statVect.getVariance();
		Double stdDev = Math.sqrt(variance);
		
		Vector<Double> result = new Vector<Double>(0);
		result.add(0, min);
		result.add(0, max);
		result.add(0, mean);
		result.add(0, variance);
		result.add(0, stdDev);
		return result;
	}
	
	/**
	 * Calculate the Pvalue between two data vectors - parameters not implements yet
	 */
	public Double calculatePValue(Vector<String> data1, Vector<String> data2, List<String> parameters)
	{
		// TTestImpl tTestCalculator = new TTestImpl() ;
		// TestUtils tTestCalculator = new TestUtils() ;
		double[] sample1 = stringVectToDouble(data1);
		double[] sample2 = stringVectToDouble(data2);
		double pValue = 0d;
		pValue = TestUtils.tTest(sample1, sample2);
		System.out.println("  Calculator: P-value found: " + pValue);
		return pValue;
	}
	
}
