package miscellaneous;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import logs.Logs;
import net.imglib2.Interval;
import net.imglib2.IterableRealInterval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.type.numeric.RealType;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

/*************************************************************************
 *  Compilation:  javac StdRandom.java
 *  Execution:    java StdRandom
 *
 *  A library of static methods to generate random numbers from
 *  different distributions (bernoulli, uniform, gaussian,
 *  discrete, and exponential). Also includes a method for
 *  shuffling an array.
 *
 *  % java StdRandom 5
 *  90 26.36076 false 8.79269 0
 *  13 18.02210 false 9.03992 1
 *  58 56.41176 true  8.80501 0
 *  29 16.68454 false 8.90827 0
 *  85 86.24712 true  8.95228 0
 *
 *
 *  Remark
 *  ------
 *    - Uses Math.random() which generates a pseudorandom real number
 *      in [0, 1)
 *
 *    - This library does not allow you to set the pseudorandom number
 *      seed. See java.util.Random.
 *
 *    - See http://www.honeylocust.com/RngPack/ for an industrial
 *      strength random number generator in Java.
 *
 *************************************************************************/

/**
 * <i>Standard random</i>. This class provides methods for generating random number from various distributions.
 * <p>
 * For additional documentation, see <a href="http://www.cs.princeton.edu/introcs/22library">Section 2.2</a> of <i>Introduction to Programming in Java: An Interdisciplinary Approach</i> by Robert Sedgewick and Kevin Wayne.
 */
public class StatisticsUtility {

	/**
	 * Return real number uniformly in [0, 1).
	 */
	public static double uniform()
	{
		return Math.random();
	}

	/**
	 * Return real number uniformly in [a, b).
	 */
	public static double uniform(double a, double b)
	{
		return a + Math.random() * (b - a);
	}

	/**
	 * Return an integer uniformly between 0 and N-1.
	 */
	public static int uniform(int N)
	{
		return (int) (Math.random() * N);
	}

	/**
	 * Return a boolean, which is true with probability p, and false otherwise.
	 */
	public static boolean bernoulli(double p)
	{
		return Math.random() < p;
	}

	/**
	 * Return a boolean, which is true with probability .5, and false otherwise.
	 */
	public static boolean bernoulli()
	{
		return bernoulli(0.5);
	}

	/**
	 * Return a real number with a standard Gaussian distribution.
	 */
	public static double gaussian()
	{
		// use the polar form of the Box-Muller transform
		double r, x, y;
		do
		{
			x = uniform(-1.0, 1.0);
			y = uniform(-1.0, 1.0);
			r = x * x + y * y;
		}
		while (r >= 1 || r == 0);
		return x * Math.sqrt(-2 * Math.log(r) / r);

		// Remark: y * Math.sqrt(-2 * Math.log(r) / r)
		// is an independent random gaussian
	}

	/**
	 * Return a real number from a gaussian distribution with given mean and stddev
	 */
	public static double gaussian(double mean, double stddev)
	{
		return mean + stddev * gaussian();
	}

	/**
	 * Return an integer with a geometric distribution with mean 1/p.
	 */
	public static int geometric(double p)
	{
		// using algorithm given by Knuth
		return (int) Math.ceil(Math.log(uniform()) / Math.log(1.0 - p));
	}

	/**
	 * Return an integer with a Poisson distribution with mean lambda.
	 */
	public static int poisson(double lambda)
	{
		// using algorithm given by Knuth
		// see http://en.wikipedia.org/wiki/Poisson_distribution
		int k = 0;
		double p = 1.0;
		double L = Math.exp(-lambda);
		do
		{
			k++;
			p *= uniform();
		}
		while (p >= L);
		return k - 1;
	}

	/**
	 * Return a real number with a Pareto distribution with parameter alpha.
	 */
	public static double pareto(double alpha)
	{
		return Math.pow(1 - uniform(), -1.0 / alpha) - 1.0;
	}

	/**
	 * Return a real number with a Cauchy distribution.
	 */
	public static double cauchy()
	{
		return Math.tan(Math.PI * (uniform() - 0.5));
	}

	/**
	 * Return a number from a discrete distribution: i with probability a[i].
	 */
	public static int discrete(double[] a)
	{
		// precondition: sum of array entries equals 1
		double r = Math.random();
		double sum = 0.0;
		for (int i = 0; i < a.length; i++)
		{
			sum = sum + a[i];
			if(sum >= r)
			{
				return i;
			}
		}
		assert (false);
		return -1;
	}

	/**
	 * Return a real number from an exponential distribution with rate lambda.
	 */
	public static double exp(double lambda)
	{
		return -Math.log(1 - Math.random()) / lambda;
	}

	/**
	 * Return the mean value of the list
	 * 
	 * @param dList
	 * @return
	 */
	public static double mean(Double[] dList)
	{
		double result = 0;
		for (double d : dList)
		{
			result = result + d / dList.length;
		}
		return result;
	}

	/**
	 * Return the mean value of the list
	 * 
	 * @param dList
	 * @return
	 */
	public static double mean(Collection<Double> dList)
	{
		double result = 0;
		for (double d : dList)
		{
			result = result + d / dList.size();
		}
		return result;
	}

	/**
	 * Return the min of the list
	 * 
	 * @param dList
	 * @return
	 */
	public static double min(Collection<Double> dList)
	{
		double result = Double.MAX_VALUE;
		for (double d : dList)
		{
			if(result > d)
			{
				result = d;
			}
		}
		return result;
	}

	/**
	 * Return the min of the list
	 * 
	 * @param dList
	 * @return
	 */
	public static double min(double[] dList)
	{
		double result = Double.MAX_VALUE;
		for (double d : dList)
		{
			if(result > d)
			{
				result = d;
			}
		}
		return result;
	}

	/**
	 * Return the min of the list
	 * 
	 * @param dList
	 * @return
	 */
	public static double[] min(double[] dList1, double[] dList2)
	{
		if(dList1.length != dList2.length)
		{
			return null;
		}
		double[] result = new double[dList1.length];
		for (int i = 0; i < dList1.length; i++)
		{
			result[i] = Math.min(dList1[i], dList2[i]);
		}
		return result;
	}

	/**
	 * Return the min of the list
	 * 
	 * @param dList
	 * @return
	 */
	public static double max(Collection<Double> dList)
	{
		double result = Double.MIN_VALUE;
		for (double d : dList)
		{
			if(result < d)
			{
				result = d;
			}
		}
		return result;
	}

	/**
	 * Return the min of the list
	 * 
	 * @param dList
	 * @return
	 */
	public static double max(double[] dList)
	{
		double result = Double.MIN_VALUE;
		for (double d : dList)
		{
			if(result < d)
			{
				result = d;
			}
		}
		return result;
	}

	/**
	 * Return the list of numbers multiplied by a value
	 * 
	 */
	public static double[] multiply(double[] x, double a)
	{
		double[] ret = new double[x.length];
		for (int i = 0; i < x.length; i++)
		{
			ret[i] = x[i] * a;
		}
		return ret;
	}

	/**
	 * Return the list of numbers normalized to either the (min and max) or just (max) alone
	 * 
	 * 
	 */
	public static double[] normalize(double[] x, boolean maxOnly)
	{
		double[] ret = null;
		if(maxOnly)
		{
			double max = max(x);
			ret = multiply(x, 1 / max);
		}
		else
		{
			double min = min(x);
			double max = max(x);
			ret = add(-1 * min, x);
			ret = multiply(x, 1 / (max - min));
		}
		return ret;
	}

	/**
	 * Return the list of numbers offset by a value (the value is added)
	 * 
	 * 
	 */
	public static double[] add(double a, double[] x)
	{
		double[] ret = new double[x.length];
		for (int i = 0; i < x.length; i++)
		{
			ret[i] = x[i] + a;
		}
		return ret;
	}

	/**
	 * Return the element-wise difference between two lists of numbers (a-b)
	 * 
	 * 
	 */
	public static double[] diff(double[] a, double[] b)
	{
		if(a.length != b.length)
		{
			return null;
		}
		double[] ret = new double[a.length];
		for (int i = 0; i < a.length; i++)
		{
			ret[i] = a[i] - b[i];
		}
		return ret;
	}

	/**
	 * Return the element-wise absolute value of a list of numbers (|a|)
	 * 
	 * 
	 */
	public static double[] abs(double[] a)
	{
		double[] ret = new double[a.length];
		int i = 0;
		for (double d : a)
		{
			ret[i] = Math.abs(d);
			i++;
		}
		return ret;
	}

	/**
	 * Calculate the max fraction of the signal that could be explained by the window
	 * 
	 * Scale the window heights uniformly to be fully contained by the signal. Only use values that are significantly above noise to such calculations. Then sum the window to calculate the fraction of the signal that is overlapped by the window.
	 * 
	 * The position at which the scaling factor is chosen to fit the window within the sig, is chosen as the minimum scaling factor needed to make each value of the window the same as the sig. However, this is calculated first with an offest added to
	 * both signals to minimize the influence of noise on the choice of scaling location, and a second time without offset to scale all values properly. There is a chance of error in the calculation that can be extremely high with offset values below
	 * the noise level and limited error at high offsets, asymptotically approaching a constant reasonable error. However, choosing an infinite offest likely produces the slightly wrong answer more often than a mid range offset that is some multiple
	 * of the noise.
	 * 
	 * Return the sum of the window over the sum of the signal.
	 * 
	 * This calculation is useful for estimating percent nuclear localization.
	 * 
	 * return NULL if an index for scaling the window can't be found
	 */
	public static Pair<Double,Double> windowFraction(double[] sig, double[] window, double offset)
	{
		double[] truncSig = new double[sig.length];
		double[] truncWindow = new double[window.length];
		for (int i = 0; i < sig.length; i++)
		{
			if(sig[i] < 0)
			{
				truncSig[i] = 0.0;
			}
			else
			{
				truncSig[i] = sig[i];
			}
			if(window[i] < 0)
			{
				truncWindow[i] = 0.0;
			}
			else
			{
				truncWindow[i] = window[i];
			}
		}

		double[] sigOffset = add(offset, truncSig);

		double[] ratio = ratio(sigOffset, truncWindow);
		int minRatioI = -1;
		double minRatio = Double.MAX_VALUE;
		for (int i = 0; i < ratio.length; i++)
		{
			if(ratio[i] < minRatio && ratio[i] > 0)
			{
				minRatioI = i;
				minRatio = ratio[i];
			}
		}
		if(minRatioI == -1)
		{
			minRatio = 0;
		}

		double[] scaledWindow = multiply(truncWindow, minRatio);
		double[] normWindow = multiply(truncWindow, offset / max(truncWindow));

		// Return the total signal and the portion of the signal within the window
		Pair<Double,Double> ret = new Pair<Double,Double>(sum(sigOffset) - offset * sigOffset.length, sum(scaledWindow) - sum(normWindow));

		return ret;
	}

	/**
	 * Window fraction based on the max-normalizing the signal and window, taking the min of the two, summing the result and dividing by the sum of the max-normalized signal.
	 * 
	 * @param sig
	 * @param window
	 * @param offset
	 * @return
	 */
	public static Pair<Double,Double> windowFraction2(double[] sig, double[] window)
	{
		double[] truncSig = new double[sig.length];
		double[] truncWindow = new double[window.length];
		for (int i = 0; i < sig.length; i++)
		{
			if(sig[i] < 0)
			{
				truncSig[i] = 0.0;
			}
			else
			{
				truncSig[i] = sig[i];
			}
			if(window[i] < 0)
			{
				truncWindow[i] = 0.0;
			}
			else
			{
				truncWindow[i] = window[i];
			}
		}
		double[] sigNorm = multiply(truncSig, 1 / max(truncSig));
		double[] windowNorm = multiply(truncWindow, 1 / max(truncWindow));
		double[] min = min(sigNorm, windowNorm);
		double frac = sum(min) / sum(sigNorm);
		double sum = sum(sig);

		// Return the total signal and the portion of the signal within the window
		Pair<Double,Double> ret = new Pair<Double,Double>(sum, frac * sum);

		return ret;
	}

	/**
	 * Return the element-wise absolute value of a list of numbers (|a|)
	 * 
	 * 
	 */
	public static double[] abs(Collection<Double> a)
	{
		double[] ret = new double[a.size()];
		int i = 0;
		for (Double d : a)
		{
			ret[i] = Math.abs(d);
			i++;
		}
		return ret;
	}

	/**
	 * Return the element-wise difference between two lists of numbers (a-b)
	 * 
	 * 
	 */
	public static double[] diff(Collection<Double> a, Collection<Double> b)
	{
		if(a.size() != b.size())
		{
			return null;
		}
		double[] ret = new double[a.size()];
		Iterator<Double> aitr = a.iterator();
		Iterator<Double> bitr = b.iterator();
		int i = 0;
		while (aitr.hasNext())
		{
			ret[i] = aitr.next() - bitr.next();
		}
		return ret;
	}

	/**
	 * Return the element-wise difference between two lists of numbers (a-b)
	 * 
	 * 
	 */
	public static double[] ratio(Collection<Double> a, Collection<Double> b)
	{
		if(a.size() != b.size())
		{
			return null;
		}
		double[] ret = new double[a.size()];
		Iterator<Double> aitr = a.iterator();
		Iterator<Double> bitr = b.iterator();
		int i = 0;
		while (aitr.hasNext())
		{
			ret[i] = aitr.next() / bitr.next();
		}
		return ret;
	}

	/**
	 * Return the element-wise difference between two lists of numbers (a-b)
	 * 
	 * 
	 */
	public static double[] ratio(double[] a, double[] b)
	{
		if(a.length != b.length)
		{
			return null;
		}
		double[] ret = new double[a.length];
		for (int i = 0; i < a.length; i++)
		{
			ret[i] = a[i] / b[i];
		}
		return ret;
	}

	/**
	 * Return the element-wise multiplication of two lists of numbers (a*b)
	 * 
	 * 
	 */
	public static double[] multiply(double[] a, double[] b)
	{
		if(a.length != b.length)
		{
			return null;
		}
		double[] ret = new double[a.length];
		for (int i = 0; i < a.length; i++)
		{
			ret[i] = a[i] * b[i];
		}
		return ret;
	}

	/**
	 * Return the element-wise multiplication of two lists of numbers (a*b)
	 * 
	 * 
	 */
	public static double[] multiply(Collection<Double> a, Collection<Double> b)
	{
		if(a.size() != b.size())
		{
			return null;
		}
		double[] ret = new double[a.size()];
		Iterator<Double> aitr = a.iterator();
		Iterator<Double> bitr = b.iterator();
		int i = 0;
		while (aitr.hasNext())
		{
			ret[i] = aitr.next() * bitr.next();
		}
		return ret;
	}

	/**
	 * Return the list of numbers multiplied by a value
	 * 
	 * @param a
	 *            , the number to multiply by
	 * @param x
	 *            , the numbers to multiply
	 * 
	 */
	public static double[] multiply(Collection<Double> x, double a)
	{
		double[] ret = new double[x.size()];
		int i = 0;
		for (Double d : x)
		{
			ret[i] = d * a;
			i++;
		}
		return ret;
	}

	/**
	 * Return the list of numbers normalized to either the (min and max) or just (max) alone
	 * 
	 * @param a
	 *            , the number to multiply by
	 * @param x
	 *            , the numbers to multiply
	 * 
	 */
	public static double[] normalize(Collection<Double> x, boolean maxOnly)
	{
		double[] ret = null;
		if(maxOnly)
		{
			double max = max(x);
			ret = multiply(x, 1 / max);
		}
		else
		{
			double min = min(x);
			double max = max(x);
			ret = add(-1 * min, x);
			ret = multiply(x, 1 / (max - min));
		}
		return ret;
	}

	/**
	 * Return the list of numbers offset by a value (the value is added)
	 * 
	 * @param a
	 *            , the number to multiply by
	 * @param x
	 *            , the numbers to multiply
	 * 
	 */
	public static double[] add(double a, Collection<Double> x)
	{
		double[] ret = new double[x.size()];
		int i = 0;
		for (Double d : x)
		{
			ret[i] = d + a;
			i++;
		}
		return ret;
	}

	/**
	 * Return the index of the first value closest to the specified value in the list
	 * 
	 * @param dList
	 * @return -1 if list is empty
	 */
	public static int nearestIndex(List<Double> dList, double target)
	{
		int result = -1;
		double currentDifference = Double.MAX_VALUE;
		double thisDifference;
		for (int i = 0; i < dList.size(); i++)
		{
			thisDifference = Math.abs(target - dList.get(i));
			if(currentDifference > thisDifference)
			{
				currentDifference = thisDifference;
				result = i;
			}
		}
		return result;
	}

	/**
	 * Return the index of the first value closest to the specified value in the list
	 * 
	 * @param dList
	 * @return -1 if list is empty
	 */
	public static int farthestIndex(List<Double> dList, double target)
	{
		int result = -1;
		double currentDifference = -1;
		double thisDifference;
		for (int i = 0; i < dList.size(); i++)
		{
			thisDifference = Math.abs(target - dList.get(i));
			if(currentDifference < thisDifference)
			{
				currentDifference = thisDifference;
				result = i;
			}
		}
		return result;
	}

	/**
	 * Return the index of the first min value of the list
	 * 
	 * @param dList
	 * @return -1 if list is empty
	 */
	public static int minIndex(List<Double> dList)
	{
		int result = -1;
		double currentMin = Double.MAX_VALUE;
		for (int i = 0; i < dList.size(); i++)
		{
			if(currentMin > dList.get(i))
			{
				currentMin = dList.get(i);
				result = i;
			}
		}
		return result;
	}

	/**
	 * Return the index of the first max value of the list
	 * 
	 * @param dList
	 * @return -1 if list is empty
	 */
	public static int maxIndex(List<Double> dList)
	{
		int result = -1;
		double currentMax = Double.MIN_VALUE;
		for (int i = 0; i < dList.size(); i++)
		{
			if(currentMax < dList.get(i))
			{
				currentMax = dList.get(i);
				result = i;
			}
		}
		return result;
	}

	/**
	 * Rearrange the elements of an array in random order.
	 */
	public static void shuffle(Object[] a)
	{
		int N = a.length;
		for (int i = 0; i < N; i++)
		{
			int r = i + uniform(N - i); // between i and N-1
			Object temp = a[i];
			a[i] = a[r];
			a[r] = temp;
		}
	}

	/**
	 * Rearrange the elements of a double array in random order.
	 */
	public static void shuffle(double[] a)
	{
		int N = a.length;
		for (int i = 0; i < N; i++)
		{
			int r = i + uniform(N - i); // between i and N-1
			double temp = a[i];
			a[i] = a[r];
			a[r] = temp;
		}
	}

	/**
	 * Rearrange the elements of an int array in random order.
	 */
	public static void shuffle(int[] a)
	{
		int N = a.length;
		for (int i = 0; i < N; i++)
		{
			int r = i + uniform(N - i); // between i and N-1
			int temp = a[i];
			a[i] = a[r];
			a[r] = temp;
		}
	}

	/**
	 * For this distribution, X, this method returns P(X < x).
	 * 
	 * @return
	 */
	public static Double tDistCDF(double x, int dof)
	{
		TDistribution tdist = new TDistribution(dof);
		return tdist.cumulativeProbability(x);
	}

	/**
	 * For this distribution, X, this method returns the critical point x, such that P(X < x) = p. Returns Double.NEGATIVE_INFINITY for p=0 and Double.POSITIVE_INFINITY for p=1.
	 */
	public static Double tDistInverseCDF(double p_value, int dof)
	{
		TDistribution tdist = new TDistribution(dof);
		return tdist.inverseCumulativeProbability(p_value);
	}

	/**
	 * Get the critical value for determining outliers using Grubb's method http://www.itl.nist.gov/div898/handbook/eda/section3/eda35h1.htm
	 * 
	 * @param mean
	 * @param stdev
	 * @param n
	 * @param alpha
	 *            (0.05 for 95% confidence)
	 * @return g_critical, critical outlier threshold for furthest point.
	 */
	public static Double grubbsOutlierLimit(int n, double alpha)
	{
		double t = -1 * tDistInverseCDF(alpha / ((2 * n)), n - 2);
		double g_critical = ((n - 1) / (Math.sqrt(n))) * Math.sqrt((t * t) / (n - 2 + t * t));
		return g_critical;
	}

	/**
	 * Use Grubb's statistic for determining if there is an outlier and throw the point with the largest deviation from the mean if true.
	 * 
	 * @param values
	 * @return remaining values after culling if necessary
	 */
	public static int getOutlier(List<Double> values, double alpha)
	{
		if(values.size() < 3)
		{
			return -1;
		}
		double[] arrayValues = toArray(values);
		double stdev = stdDev(arrayValues);
		double mean = mean(arrayValues);
		double g_critical = grubbsOutlierLimit(values.size(), alpha);
		Double maxOutlier = null;
		int outlierIndex = -1;
		for (Double value : values)
		{
			if(maxOutlier == null)
			{
				maxOutlier = value;
			}
			if(Math.abs(mean - value) > Math.abs(mean - maxOutlier))
			{
				maxOutlier = value;
			}
		}
		if(Math.abs(mean - maxOutlier) / stdev > g_critical)
		{
			outlierIndex = values.indexOf(maxOutlier);
			Logs.log("Found outlier at index: " + outlierIndex, 1, "StatisticsUtility");
		}
		return outlierIndex;
	}

	/**
	 * Return the boolean array indicating whether it passes the threshold
	 * @param values
	 * @param thresh
	 * @param greaterThan
	 * @param inclusive
	 * @return
	 */
	public static boolean[] getThresholdFilter(double[] values, double thresh, boolean greaterThan, boolean inclusive)
	{
		boolean[] filtered = new boolean[values.length];
		if(greaterThan)
		{
			if(inclusive)
			{
				for(int i = 0; i < values.length; i++)
				{
					if(values[i] >= thresh)
					{
						filtered[i]=true;
					}
				}
			}
			else
			{
				for(int i = 0; i < values.length; i++)
				{
					if(values[i] > thresh)
					{
						filtered[i]=true;
					}
				}
			}
		}
		else
		{
			if(inclusive)
			{
				for(int i = 0; i < values.length; i++)
				{
					if(values[i] <= thresh)
					{
						filtered[i]=true;
					}
				}
			}
			else
			{
				for(int i = 0; i < values.length; i++)
				{
					if(values[i] < thresh)
					{
						filtered[i]=true;
					}
				}
			}
		}

		return filtered;
	}

	public static boolean[] union(boolean[] a, boolean[] b)
	{
		if(a.length != b.length)
		{
			Logs.log("Unequal length arguments not allowed", Logs.ERROR, StatisticsUtility.class);
			return null;
		}
		boolean[] ret = new boolean[a.length];
		for(int i = 0; i < a.length; i++)
		{
			ret[i] = a[i] || b[i];
		}
		return ret;
	}

	public static boolean[] intersection(boolean[] a, boolean[] b)
	{
		if(a.length != b.length)
		{
			Logs.log("Unequal length arguments not allowed", Logs.ERROR, StatisticsUtility.class);
			return null;
		}
		boolean[] ret = new boolean[a.length];
		for(int i = 0; i < a.length; i++)
		{
			ret[i] = a[i] && b[i];
		}
		return ret;
	}

	public static int countFilter(boolean[] filter, boolean countPositives)
	{
		// get the number of true elements for initialize the return object
		int count = 0;
		for(boolean b : filter)
		{
			if(b) count++;
		}

		if(countPositives)
		{
			return count;
		}
		else
		{
			return(filter.length-count);
		}
	}

	public static double[] applyFilter(double[] values, boolean[] filter)
	{
		if(values.length != filter.length)
		{
			Logs.log("Arguments not the same length. Returning null.", Logs.ERROR, StatisticsUtility.class);
		}

		// get the number of true elements for initialize the return object
		int count = 0;
		for(boolean b : filter)
		{
			if(b) count++;
		}

		// Put matching values into the return object
		double[] ret = new double[count];
		count = 0;
		for(int i = 0; i < filter.length; i++)
		{
			if(filter[i])
			{
				ret[count] = values[i];
				count++;
			}
		}
		return ret;
	}

	public static double[] toArray(List<Double> values)
	{
		int count = 0;
		double[] arrayValues = new double[values.size()];
		for (Double value : values)
		{
			arrayValues[count] = value;
			count = count + 1;
		}
		return arrayValues;
	}

	public static Double stdDev(double[] values)
	{
		StandardDeviation stdevCalculator = new StandardDeviation();
		return stdevCalculator.evaluate(values);
	}

	public static Double variance(double[] values)
	{
		Variance stdevCalculator = new Variance();
		return stdevCalculator.evaluate(values);
	}

	public static Double mean(double[] values)
	{
		Mean meanCalculator = new Mean();
		return meanCalculator.evaluate(values);
	}
	
	public static <T extends Number, K extends Number> Double weightedMad(Iterable<T> values, double[] weights, double median)
	{
		
		Iterator<T> itrV = values.iterator();
		TreeMap<Double, Double> temp = new TreeMap<>();
		double totWeight = 0.0d;
		int count = -1;
		while(itrV.hasNext())
		{
			//temp.put(itrV.next().doubleValue() - median, itrW.next());
			count = count + 1;
			double key = Math.abs(itrV.next().doubleValue() - median);
			double w = weights[count];
			totWeight = totWeight + w;
			Double oldW = temp.get(key);
			if(oldW == null)
			{
				oldW = 0.0d;
			}
			temp.put(key, w + oldW);
		}
		if(count + 1 != weights.length)
		{
			throw new IllegalArgumentException("There must be an equal number of values and weights. Aborting.");
		}
		return(1.4826 * weightedMedian(temp, totWeight));
	}
	
	public static Double weightedMad(double[] values, double[] weights, double median)
	{
		if(values.length != weights.length)
		{
			throw new IllegalArgumentException("There must be an equal number of values and weights. Aborting.");
		}
		TreeMap<Double, Double> temp = new TreeMap<>();
		double totWeight = 0.0d;
		for(int i = 0; i < values.length; i++)
		{
			//temp.put(itrV.next().doubleValue() - median, itrW.next());
			double key = Math.abs(values[i] - median);
			double w = weights[i];
			totWeight = totWeight + w;
			Double oldW = temp.get(key);
			if(oldW == null)
			{
				oldW = 0.0d;
			}
			temp.put(key, w + oldW);
		}
		return(1.4826 * weightedMedian(temp, totWeight));
	}
	
	public static <T extends Number, K extends Number> Double weightedMad(TreeMap<T,K> dataAndWeights, double median)
	{
		TreeMap<Double, Double> temp = new TreeMap<>();
		double totWeight = 0.0d;
		for(Entry<T,K> e : dataAndWeights.entrySet())
		{
			//temp.put(itrV.next().doubleValue() - median, itrW.next());
			double key = Math.abs(e.getKey().doubleValue() - median);
			double w = e.getValue().doubleValue();
			totWeight = totWeight + w;
			Double oldW = temp.get(key);
			if(oldW == null)
			{
				oldW = 0.0d;
			}
			temp.put(key, w + oldW);
		}
		return(1.4826 * weightedMedian(temp, totWeight));
	}
	
	public static <T extends Number, K extends Number> Double weightedMad(Iterable<T> values, Iterable<K> weights, double median)
	{
		
		Iterator<T> itrV = values.iterator();
		Iterator<K> itrW = weights.iterator();
		TreeMap<Double, Double> temp = new TreeMap<>();
		double totWeight = 0.0d;
		while(itrV.hasNext() && itrW.hasNext())
		{
			//temp.put(itrV.next().doubleValue() - median, itrW.next());
			
			double key = Math.abs(itrV.next().doubleValue() - median);
			double w = itrW.next().doubleValue();
			totWeight = totWeight + w;
			Double oldW = temp.get(key);
			if(oldW == null)
			{
				oldW = 0.0d;
			}
			temp.put(key, w + oldW);
		}
		if(itrV.hasNext() || itrW.hasNext())
		{
			throw new IllegalArgumentException("There must be an equal number of values and weights. Aborting.");
		}
		return(1.4826 * weightedMedian(temp, totWeight));
	}
	
	public static <T extends Number, K extends Number> Double weightedMedian(TreeMap<T, K> values, double totWeight)
	{
		double test = 0.0d;
		Iterator<Entry<T, K>> itr = values.entrySet().iterator();
		T ret = null;
		while(test < 0.5 && itr.hasNext())
		{
			Entry<T, K> e = itr.next();
			ret = e.getKey();
			test = test + e.getValue().doubleValue()/totWeight;
		}
		
		// Capture special cases
		if(ret == null)
		{
			return null; // This should hopefully never happen.
		}
		if(test == 0.5d)
		{
			if(itr.hasNext())
			{
				return (ret.doubleValue() + itr.next().getKey().doubleValue())/2.0d;
			}
			else
			{
				return ret.doubleValue();
			}
		}
		else
		{
			return ret.doubleValue();
		}
	}
	
	public static <T extends Number, K extends Number> Double weightedMedian(TreeMap<T, K> values, boolean normalizeWeights)
	{
		double totWeight = 1.0d;
		if(normalizeWeights)
		{
			totWeight = 0.0d;
			for(Entry<T, K> e : values.entrySet())
			{
				totWeight = totWeight + e.getValue().doubleValue();
			}
		}
		return weightedMedian(values, totWeight);
	}
	
	public static <T extends Number, K extends Number> Double weightedMedian(Iterable<T> values, Iterable<K> weights)
	{
		Iterator<T> itrV = values.iterator();
		Iterator<K> itrW = weights.iterator();
		TreeMap<T, Double> temp = new TreeMap<>();
		double totWeight = 0.0d;
		while(itrV.hasNext() && itrW.hasNext())
		{
			T key = itrV.next();
			double w = itrW.next().doubleValue();
			totWeight = totWeight + w;
			Double oldW = temp.get(key);
			if(oldW == null)
			{
				oldW = 0.0d;
			}
			temp.put(key, w + oldW);
		}
		if(itrV.hasNext() || itrW.hasNext())
		{
			throw new IllegalArgumentException("There must be an equal number of values and weights. Aborting.");
		}
		return(weightedMedian(temp, totWeight));
	}
	
	/**
	 * The algorithm first determines a traditional median to seed the algorithm
	 * Each value is then weighted according to (abs(x-mean))^(scaling).
	 * The weighted median (i.e., the value at the index where the cumulative
	 * normalized weight is <= 0.5. If the cumulative weight of the median is 0.5,
	 * then the median of the upper and lower medians are returned.
	 * 
	 * Convergence typically occurs at iterations 4-6 with a scaling of 0.25-0.1
	 * 
	 * @param values
	 * @param iters
	 * @param scaling
	 * @return The adaptively determined weighted mean.
	 */
	public static Double adaptiveMedian(double[] values, int iters, double scaling)
	{
		AdaptiveMedian am = new AdaptiveMedian(values, scaling);
		return am.evaluate(iters);
	}
	
	public static Double adaptiveMedian(double[] values, int iters)
	{
		return adaptiveMedian(values, iters, AdaptiveMedian.SCALING);
	}
	
	public static Double adaptiveMedian(double[] values)
	{
		return adaptiveMedian(values, AdaptiveMedian.ITERS, AdaptiveMedian.SCALING);
	}
	
	/**
	 * The algorithm first determines a traditional median to seed the algorithm
	 * Each value is then weighted according to (abs(x-mean))^(scaling).
	 * The weighted median (i.e., the value at the index where the cumulative
	 * normalized weight is <= 0.5. If the cumulative weight of the median is 0.5,
	 * then the median of the upper and lower medians are returned.
	 * 
	 * Convergence typically occurs at iterations 4-6 with a scaling of 0.25-0.1
	 * 
	 * @param values
	 * @param iters
	 * @param scaling
	 * @return The adaptively determined weighted mean.
	 */
	public static Double adaptiveMedian(Collection<Double> values, int iters, double scaling)
	{
		double[] v = new double[values.size()];
		int count = 0;
		for (Double d : values)
		{
			v[count] = d;
			count++;
		}
		AdaptiveMedian am = new AdaptiveMedian(v, scaling);
		return am.evaluate(iters);
	}
	
	public static Double adaptiveMedian(Collection<Double> values, int iters)
	{
		return adaptiveMedian(values, iters, AdaptiveMedian.SCALING);
	}
	
	public static Double adaptiveMedian(Collection<Double> values)
	{
		return adaptiveMedian(values, AdaptiveMedian.ITERS, AdaptiveMedian.SCALING);
	}
	
	/**
	 * The algorithm first determines a traditional median to seed the algorithm
	 * Each value is then weighted according to (abs(x-mean))^(scaling).
	 * The weighted median (i.e., the value at the index where the cumulative
	 * normalized weight is <= 0.5. If the cumulative weight of the median is 0.5,
	 * then the median of the upper and lower medians are returned.
	 * 
	 * Convergence typically occurs at iterations 4-6 with a scaling of 0.25-0.1
	 * 
	 * @param values
	 * @param iters
	 * @param scaling
	 * @return The adaptively determined weighted mean.
	 */
	public static < T extends RealType< T > > Double adaptiveMedian(IterableRealInterval<T> values, int iters, double scaling)
	{
		double[] v = new double[(int) values.size()];
		int count = 0;
		for (T d : values)
		{
			v[count] = d.getRealDouble();
			count++;
		}
		AdaptiveMedian am = new AdaptiveMedian(v, scaling);
		return am.evaluate(iters);
	}
	
	public static < T extends RealType< T > > Double adaptiveMedian(IterableRealInterval<T> values, int iters)
	{
		return adaptiveMedian(values, iters, AdaptiveMedian.SCALING);
	}
	
	public static < T extends RealType< T > > Double adaptiveMedian(IterableRealInterval<T> values)
	{
		return adaptiveMedian(values, AdaptiveMedian.ITERS, AdaptiveMedian.SCALING);
	}
	
	public static Double median(Object values)
	{
		if(values instanceof byte[])
		{
			return median((byte[]) values);
		}
		else if(values instanceof int[])
		{
			return median((int[]) values);
		}
		else if(values instanceof short[])
		{
			return median((short[]) values);
		}
		else if(values instanceof float[])
		{
			return median((float[]) values);
		}
		else
		{
			return median((double[]) values);
		}
	}
	
	public static Double median(double[] values)
	{
		Median medianCalculator = new Median();
		return medianCalculator.evaluate(values);
	}
	
	public static Double median(float[] values)
	{
		Median medianCalculator = new Median();
		double[] temp = new double[values.length];
		for(int i=0; i < values.length; i++)
		{
			temp[i] = values[i];
		}
		return medianCalculator.evaluate(temp);
	}
	
	public static Double median(short[] values)
	{
		Median medianCalculator = new Median();
		double[] temp = new double[values.length];
		for(int i=0; i < values.length; i++)
		{
			temp[i] = values[i];
		}
		return medianCalculator.evaluate(temp);
	}
	
	public static Double median(byte[] values)
	{
		Median medianCalculator = new Median();
		double[] temp = new double[values.length];
		for(int i=0; i < values.length; i++)
		{
			temp[i] = values[i];
		}
		return medianCalculator.evaluate(temp);
	}
	
	public static Double median(TreeMap<DimensionMap,Double> values)
	{
		double[] v = new double[values.size()];
		int count = 0;
		for (Entry<DimensionMap,Double> d : values.entrySet())
		{
			v[count] = d.getValue();
			count++;
		}
		return median(v);
	}

	public static Double median(Collection<Double> values)
	{
		double[] v = new double[values.size()];
		int count = 0;
		for (Double d : values)
		{
			v[count] = d;
			count++;
		}
		return median(v);
	}

	public static < T extends RealType< T > > Double median(IterableRealInterval<T> values)
	{
		double[] v = new double[(int) values.size()];
		int count = 0;
		for (T d : values)
		{
			v[count] = d.getRealDouble();
			count++;
		}
		return median(v);
	}

	public static Vector<Point> generateRandomPointsInRectangularRegion(Interval interval, int numPoints)
	{
		// the number of dimensions
		int numDimensions = interval.numDimensions();

		// a random number generator
		Random rnd = new Random();

		// a list of Samples with coordinates
		Vector<Point> elements = new Vector<>();

		// Make a separate method that returns the list of random locations within the interval
		// Then just offset the locations by the min of each dimension.
		for ( int i = 0; i < numPoints; ++i )
		{
			Point point = new Point( numDimensions );

			for ( int d = 0; d < numDimensions; ++d )
				point.setPosition( Math.round(rnd.nextDouble() *
						( interval.realMax( d ) - interval.realMin( d ) ) + interval.realMin( d )), d );

			// add a new element with a random intensity in the range 0...1
			elements.add( point );
		}

		return elements;
	}

	/**
	 * Source: http://imagej.net/ImgLib2_Examples#Example_8b_-_Randomly_sample_an_existing_image_and_display_it
	 * 
	 * Sample a number of n-dimensional random points in a certain interval having a
	 * random intensity 0...1
	 *
	 * @param interval - the interval in which points are created
	 * @param numPoints - the amount of points
	 *
	 * @return a RealPointSampleList (which is an IterableRealInterval)
	 */
	public static < T extends RealType< T > > double[] samplePoints(
			RandomAccessible< T > input, Vector<Point> points, Localizable offset )
	{
		// a random accessible in the image data to grep the right value
		RandomAccess< T > randomAccess = input.randomAccess();

		// Move the set of points according to the cursor position
		double[] nums = new double[points.size()];
		for(int i = 0; i < points.size(); i++)
		{
			Point p = new Point(points.get(i));
			p.move(offset);
			randomAccess.setPosition(p);
			nums[i] = randomAccess.get().getRealDouble();
		}

		return nums;
	}

	/**
	 * Source: http://imagej.net/ImgLib2_Examples#Example_8b_-_Randomly_sample_an_existing_image_and_display_it
	 * 
	 * Sample a number of n-dimensional random points in a certain interval having a
	 * random intensity 0...1
	 *
	 * @param interval - the interval in which points are created
	 * @param numPoints - the amount of points
	 *
	 * @return a RealPointSampleList (which is an IterableRealInterval)
	 */
	public static < T extends RealType< T > > double[] sampleRandomPoints(
			RandomAccessible< T > input, Interval interval, int numPoints )
	{
		// the number of dimensions
		int numDimensions = interval.numDimensions();

		// a random number generator
		Random rnd = new Random();

		// a list of Samples with coordinates
		double[] ret = new double[numPoints];

		// a random accessible in the image data to grep the right value
		RandomAccess< T > randomAccess = input.randomAccess();

		// Make a separate method that returns the list of random locations within the interval
		// Then just offset the locations by the min of each dimension.
		for ( int i = 0; i < numPoints; ++i )
		{
			Point point = new Point( numDimensions );

			for ( int d = 0; d < numDimensions; ++d )
				point.setPosition( Math.round(rnd.nextDouble() *
						( interval.realMax( d ) - interval.realMin( d ) ) + interval.realMin( d )), d );

			randomAccess.setPosition( point );

			// add a new element with a random intensity in the range 0...1
			ret[i] = randomAccess.get().getRealDouble();
		}

		return ret;
	}

	/**
	 * @param values
	 * @param percentile value be > 0 and <= 100
	 * @return
	 */
	public static Double percentile(double[] values, Double percentile)
	{
		return StatUtils.percentile(values, percentile);
	}

	/**
	 * @param values
	 * @param percentile value be > 0 and <= 100
	 * @return
	 */
	public static Double percentile(Collection<Double> values, Double percentile)
	{
		double[] v = new double[values.size()];
		int count = 0;
		for (Double d : values)
		{
			v[count] = d;
			count++;
		}
		return StatUtils.percentile(v, percentile);
	}
	
	public static double[] percentile(Object values, double...percentiles)
	{
		if(values instanceof byte[])
		{
			return percentile((byte[]) values, percentiles);
		}
		else if(values instanceof int[])
		{
			return percentile((int[]) values, percentiles);
		}
		else if(values instanceof short[])
		{
			return percentile((short[]) values, percentiles);
		}
		else if(values instanceof float[])
		{
			return percentile((float[]) values, percentiles);
		}
		else
		{
			return percentile((double[]) values, percentiles);
		}
	}
	
	public static double[] percentile(byte[] values, double...percentiles)
	{
		double[] temp = new double[values.length];
		for(int i = 0; i < values.length; i++)
		{
			temp[i] = values[i];
		}
		return percentile(temp, percentiles);
	}
	
	public static double[] percentile(int[] values, double...percentiles)
	{
		double[] temp = new double[values.length];
		for(int i = 0; i < values.length; i++)
		{
			temp[i] = values[i];
		}
		return percentile(temp, percentiles);
	}
	
	public static double[] percentile(short[] values, double...percentiles)
	{
		double[] temp = new double[values.length];
		for(int i = 0; i < values.length; i++)
		{
			temp[i] = values[i];
		}
		return percentile(temp, percentiles);
	}
	
	public static double[] percentile(float[] values, double...percentiles)
	{
		double[] temp = new double[values.length];
		for(int i = 0; i < values.length; i++)
		{
			temp[i] = values[i];
		}
		return percentile(temp, percentiles);
	}

	/**
	 * @param values
	 * @param percentiles value be > 0 and <= 100
	 * @return
	 */
	public static double[] percentile(double[] values, double...percentiles)
	{
		Percentile p = new Percentile();
		p.setData(values);
		double[] ret = new double[percentiles.length];
		for(int i = 0; i < ret.length; i++)
		{
			ret[i] = p.evaluate(percentiles[i]);
		}
		return ret;
	}

	/**
	 * @param values
	 * @param percentiles value be > 0 and <= 100
	 * @return
	 */
	public static double[] percentile(List<Double> values, double[] percentiles)
	{
		double[] ret = new double[percentiles.length];
		Collections.sort(values, Comparator.naturalOrder());
		for(int i=0; i < ret.length; i++)
		{
			int j = (int) Math.round((values.size()-1)*(percentiles[i]/100.0));
			ret[i] = values.get(j);
		}
		return ret;
		//		double[] v = new double[values.size()];
		//		int count = 0;
		//		for (Double d : values)
		//		{
		//			v[count] = d;
		//			count++;
		//		}
		//		return StatisticsUtility.percentile(v, percentiles);
	}
	
	/**
	 * @param values
	 * @param percentiles value be > 0 and <= 100
	 * @return
	 */
	public static double[] percentile(List<Double> values, double[] percentiles, double ignoreBelow, double ignoreAbove)
	{
		double[] ret = new double[percentiles.length];
		Collections.sort(values, Comparator.naturalOrder());
		int first = 0, last = 0;
		for(int i=0; i < values.size(); i++)
		{
			if(values.get(i) < ignoreBelow)
			{
				first = first + 1;
			}
			if(values.get(i) <= ignoreAbove)
			{
				last = i;
			}
		}
		for(int i=0; i < ret.length; i++)
		{
			int j = first + (int) Math.round((last-first)*(percentiles[i]/100.0));
			ret[i] = values.get(j);
		}
		return ret;
		//		double[] v = new double[values.size()];
		//		int count = 0;
		//		for (Double d : values)
		//		{
		//			v[count] = d;
		//			count++;
		//		}
		//		return StatisticsUtility.percentile(v, percentiles);
	}
	
	public static Pair<Double,Double> percentile(float[] pixelsCopy, double minPercentile, double maxPercentile)
	{
		if(minPercentile < 0.0 || maxPercentile > 100.0)
		{
			return null;
		}
		Arrays.sort(pixelsCopy);
		int minI = (int) Math.round((pixelsCopy.length-1)*(minPercentile/100.0));
		int maxI = (int) Math.round((pixelsCopy.length-1)*(maxPercentile/100.0));
		return new Pair<>((double) pixelsCopy[minI], (double) pixelsCopy[maxI]);
	}
	
	public static Pair<Double,Double> percentile(float[] pixelsCopy, double minPercentile, double maxPercentile, double ignoreBelow, double ignoreAbove)
	{
		if(minPercentile < 0.0 || maxPercentile > 100.0)
		{
			return null;
		}
		Arrays.sort(pixelsCopy);
		int first = 0, last = 0;
		for(int i=0; i < pixelsCopy.length; i++)
		{
			if(pixelsCopy[i] < ignoreBelow)
			{
				first = first + 1;
			}
			if(pixelsCopy[i] <= ignoreAbove)
			{
				last = i;
			}
		}
		int minI = first + (int) Math.round((last-first)*(minPercentile/100.0));
		int maxI = first + (int) Math.round((last-first)*(maxPercentile/100.0));
		return new Pair<>((double) pixelsCopy[minI], (double) pixelsCopy[maxI]);
	}
	
	public static Pair<Double,Double> percentile(short[] pixelsCopy, double minPercentile, double maxPercentile)
	{
		if(minPercentile < 0.0 || maxPercentile > 100.0)
		{
			return null;
		}
		Arrays.sort(pixelsCopy);
		int minI = (int) Math.round((pixelsCopy.length-1)*(minPercentile/100.0));
		int maxI = (int) Math.round((pixelsCopy.length-1)*(maxPercentile/100.0));
		return new Pair<>((double) pixelsCopy[minI], (double) pixelsCopy[maxI]);
	}
	
	public static Pair<Double,Double> percentile(short[] pixelsCopy, double minPercentile, double maxPercentile, double ignoreBelow, double ignoreAbove)
	{
		if(minPercentile < 0.0 || maxPercentile > 100.0)
		{
			return null;
		}
		Arrays.sort(pixelsCopy);
		int first = 0, last = 0;
		for(int i=0; i < pixelsCopy.length; i++)
		{
			if(pixelsCopy[i] < ignoreBelow)
			{
				first = first + 1;
			}
			if(pixelsCopy[i] <= ignoreAbove)
			{
				last = i;
			}
		}
		int minI = first + (int) Math.round((last-first)*(minPercentile/100.0));
		int maxI = first + (int) Math.round((last-first)*(maxPercentile/100.0));
		return new Pair<>((double) pixelsCopy[minI], (double) pixelsCopy[maxI]);
	}
	
	public static Pair<Double,Double> percentile(byte[] pixelsCopy, double minPercentile, double maxPercentile)
	{
		if(minPercentile < 0.0 || maxPercentile > 100.0)
		{
			return null;
		}
		Arrays.sort(pixelsCopy);
		int minI = (int) Math.round((pixelsCopy.length-1)*(minPercentile/100.0));
		int maxI = (int) Math.round((pixelsCopy.length-1)*(maxPercentile/100.0));
		return new Pair<>((double) pixelsCopy[minI], (double) pixelsCopy[maxI]);
	}

	public static Pair<Double,Double> percentile(byte[] pixelsCopy, double minPercentile, double maxPercentile, double ignoreBelow, double ignoreAbove)
	{
		if(minPercentile < 0.0 || maxPercentile > 100.0)
		{
			return null;
		}
		Arrays.sort(pixelsCopy);
		int first = 0, last = 0;
		for(int i=0; i < pixelsCopy.length; i++)
		{
			if(pixelsCopy[i] < ignoreBelow)
			{
				first = first + 1;
			}
			if(pixelsCopy[i] <= ignoreAbove)
			{
				last = i;
			}
		}
		int minI = first + (int) Math.round((last-first)*(minPercentile/100.0));
		int maxI = first + (int) Math.round((last-first)*(maxPercentile/100.0));
		return new Pair<>((double) pixelsCopy[minI], (double) pixelsCopy[maxI]);
	}
	
	public static int mode(int[] values)
	{
		double[] d = new double[values.length];
		for(int i=0; i < values.length; i++)
		{
			d[i] = values[i];
		}
		double[] modes = StatUtils.mode(d);
		return (int) Math.round(modes[0]);
	}

	public static double mode(double[] values)
	{
		return StatUtils.mode(values)[0];
	}

	public static double mode(Collection<Double> values)
	{
		double[] v = new double[values.size()];
		int count = 0;
		for (Double d : values)
		{
			v[count] = d;
			count++;
		}
		return StatUtils.mode(v)[0];
	}

	/**
	 * MEDIAN absolute deviation
	 * @param median
	 * @param values
	 * @return
	 */
	public static Double mad(double median, double[] values)
	{
		double med = median;
		double[] diffs = new double[values.length];
		int count = 0;
		for (Double d : values)
		{
			diffs[count] = Math.abs(d - med);
			count++;
		}
		return 1.4826 * median(diffs);
	}

	public static Double mad(double[] values)
	{
		return mad(median(values), values);
	}
	
	public static Double mad(double median, Collection<Double> values)
	{
		double med = median;
		double[] diffs = new double[values.size()];
		int count = 0;
		for (Double d : values)
		{
			diffs[count] = Math.abs(d - med);
			count++;
		}
		return 1.4826 * median(diffs);
	}
	
	public static Double mad(Collection<Double> values)
	{
		return mad(median(values), values);
	}

	public static Double sum(double[] values)
	{
		double sum = 0;
		for (double d : values)
		{
			sum = sum + d;
		}
		return new Double(sum);
	}

	public static Double sum(Collection<Double> values)
	{
		double sum = 0;
		for (Double d : values)
		{
			sum = sum + d;
		}
		return sum;
	}

	public static Double stdDev(Collection<Double> values)
	{
		double[] v = new double[values.size()];
		int count = 0;
		for (Double d : values)
		{
			v[count] = d;
			count++;
		}
		return stdDev(v);
	}

	public static Double variance(Collection<Double> values)
	{
		double[] v = new double[values.size()];
		int count = 0;
		for (Double d : values)
		{
			v[count] = d;
			count++;
		}
		return variance(v);
	}

	public static double normalCDF(double x, double mean, double sigma)
	{
		NormalDistribution normDist = new NormalDistribution(mean, sigma);
		double ret = normDist.cumulativeProbability(x);
		return ret;
	}

	public static double normalInverseCDF(double pValue, double mean, double sigma)
	{
		NormalDistribution normDist = new NormalDistribution(mean, sigma);
		double ret = normDist.inverseCumulativeProbability(pValue);
		return ret;
	}

	public static double normalPDF(double x, double mean, double sigma)
	{
		double A = 1 / (sigma * Math.sqrt(2 * Math.PI));
		double B = -1 * Math.pow((x - mean), 2) / (2 * Math.pow(sigma, 2));
		double ret = A * Math.exp(B);
		return ret;
	}

	public static PointList getMinDistanceMappingOfB(PointList a, PointList b)
	{
		double[][] d = new double[a.size()][b.size()];

		for (int r = 0; r < a.size(); r++)
		{
			for (int c = 0; c < b.size(); c++)
			{
				d[r][c] = IdPoint.distance(a.elementAt(r).x, a.elementAt(r).y, b.elementAt(c).x, b.elementAt(c).y);
			}
		}
		HungarianAlgorithm h = new HungarianAlgorithm(d);
		int[] map = h.execute();

		PointList ret = new PointList();
		for (int i : map)
		{
			ret.add(b.elementAt(i).copy());
		}

		return ret;
	}

	public static double getMappingDistance(PointList a, PointList b)
	{
		double ret = 0;
		for (int r = 0; r < a.size(); r++)
		{
			ret = ret + IdPoint.distance(a.elementAt(r).x, a.elementAt(r).y, b.elementAt(r).x, b.elementAt(r).y);
		}

		return ret;
	}
	
	public static TreeMap<DimensionMap,Vector<Pair<Double,Double>>> getDiffVectors(TreeMap<DimensionMap,ROIPlus> maximaRois, String timeDimName)
	{
		DimTable dt = new DimTable(maximaRois);
		Dim d = dt.getDimWithName(timeDimName);
		Vector<String> vals = d.dimValues;
		TreeMap<Double, String> vals2 = new TreeMap<>();
		for(String val : vals)
		{
			vals2.put(Double.parseDouble(val), val);
		}
		
		TreeMap<Double,DimensionMap> timeOrderedMaps = new TreeMap<>();
		TreeMap<DimensionMap,Vector<Pair<Double,Double>>> ret = new TreeMap<>();
		for(DimensionMap filter : dt.getSubTable(timeDimName).getMapIterator())
		{
			// Use this map to grab all times for a particular dimension location
			DimTable timeTable = dt.getSubTable(filter); // Time is the only non-singleton dim here
			for(DimensionMap map : timeTable.getMapIterator())
			{
				timeOrderedMaps.put(Double.parseDouble(map.get(timeDimName)), map);
			}
			
			// Loop through time-ordered maps to calculate deltas for each point at each time (i.e., points in roi2 vs points in roi1)
			ROIPlus roi1 = null, roi2 = null;
			for(Entry<Double,DimensionMap> e : timeOrderedMaps.entrySet())
			{
				if(maximaRois.get(e.getValue()) == null)
				{
					continue;
				}
				if(roi2 == null)
				{
					roi2 = maximaRois.get(e.getValue());
					Vector<Pair<Double,Double>> toSave = new Vector<>();
					for (int i = 0; i < roi2.pointList.size(); i++) {
						toSave.add(new Pair<Double,Double>(0d,0d));
					}
					ret.put(e.getValue().copy(), toSave);
					continue;
				}
				
				roi1 = roi2;
				roi2 = maximaRois.get(e.getValue());
				
				// 1st iterate over points in roi2 to load them into a TreeMap for searching
				TreeMap<Integer, IdPoint> orderedPointList = new TreeMap<>();
				for(IdPoint p : roi2.pointList)
				{
					orderedPointList.put(p.id, p);
				}
				
				// For each point in roi1, get the corresponding point in roi2
				Vector<Pair<Double,Double>> toSave = new Vector<>();
				for(IdPoint p1 : roi1.pointList)
				{
					IdPoint p2 = orderedPointList.get(p1.id);
					if(p2!= null)
					{
						double dx = p2.x - p1.x;
						double dy = p2.y - p1.y;
						// then save the deltas
						toSave.add(new Pair<Double,Double>(dx, dy));
					}
				}
				// put the deltas into the output object
				ret.put(e.getValue().copy(), toSave);
			}
		}
		return ret;
	}
	
	public static Pair<Double,Double> getMedianDiff(Vector<Pair<Double,Double>> diffs)
	{
		Vector<Double> dxs = new Vector<>(diffs.size());
		Vector<Double> dys = new Vector<>(diffs.size());
		for(Pair<Double,Double> diff : diffs)
		{
			dxs.add(diff.p1);
			dys.add(diff.p2);
		}
		double dx = StatisticsUtility.median(dxs);
		double dy = StatisticsUtility.median(dys);
		return new Pair<Double,Double>(dx, dy);
	}
	
	public static boolean isEven(int n)
	{
		return (n & 1) == 0;
	}
	
	public static boolean isOdd(int n)
	{
		return (n & 1) != 0;
	}

}

/*
 * Copyright (c) 2012 Kevin L. Stern
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * An implementation of the Hungarian algorithm for solving the assignment problem. An instance of the assignment problem consists of a number of workers along with a number of jobs and a cost matrix which gives the cost of assigning the i'th worker
 * to the j'th job at position (i, j). The goal is to find an assignment of workers to jobs so that no job is assigned more than one worker and so that no worker is assigned to more than one job in such a manner so as to minimize the total cost of
 * completing the jobs.
 * <p>
 * 
 * An assignment for a cost matrix that has more workers than jobs will necessarily include unassigned workers, indicated by an assignment value of -1; in no other circumstance will there be unassigned workers. Similarly, an assignment for a cost
 * matrix that has more jobs than workers will necessarily include unassigned jobs; in no other circumstance will there be unassigned jobs. For completeness, an assignment for a square cost matrix will give exactly one unique worker to each job.
 * <p>
 * 
 * This version of the Hungarian algorithm runs in time O(n^3), where n is the maximum among the number of workers and the number of jobs.
 * 
 * @author Kevin L. Stern
 */
class HungarianAlgorithm {

	private final double[][] costMatrix;
	private final int rows, cols, dim;
	private final double[] labelByWorker, labelByJob;
	private final int[] minSlackWorkerByJob;
	private final double[] minSlackValueByJob;
	private final int[] matchJobByWorker, matchWorkerByJob;
	private final int[] parentWorkerByCommittedJob;
	private final boolean[] committedWorkers;

	/**
	 * Construct an instance of the algorithm.
	 * 
	 * @param costMatrix
	 *            the cost matrix, where matrix[i][j] holds the cost of assigning worker i to job j, for all i, j. The cost matrix must not be irregular in the sense that all rows must be the same length.
	 */
	public HungarianAlgorithm(double[][] costMatrix)
	{
		this.dim = Math.max(costMatrix.length, costMatrix[0].length);
		this.rows = costMatrix.length;
		this.cols = costMatrix[0].length;
		this.costMatrix = new double[this.dim][this.dim];
		for (int w = 0; w < this.dim; w++)
		{
			if(w < costMatrix.length)
			{
				if(costMatrix[w].length != this.cols)
				{
					throw new IllegalArgumentException("Irregular cost matrix");
				}
				this.costMatrix[w] = Arrays.copyOf(costMatrix[w], this.dim);
			}
			else
			{
				this.costMatrix[w] = new double[this.dim];
			}
		}
		this.labelByWorker = new double[this.dim];
		this.labelByJob = new double[this.dim];
		this.minSlackWorkerByJob = new int[this.dim];
		this.minSlackValueByJob = new double[this.dim];
		this.committedWorkers = new boolean[this.dim];
		this.parentWorkerByCommittedJob = new int[this.dim];
		this.matchJobByWorker = new int[this.dim];
		Arrays.fill(this.matchJobByWorker, -1);
		this.matchWorkerByJob = new int[this.dim];
		Arrays.fill(this.matchWorkerByJob, -1);
	}

	/**
	 * Compute an initial feasible solution by assigning zero labels to the workers and by assigning to each job a label equal to the minimum cost among its incident edges.
	 */
	protected void computeInitialFeasibleSolution()
	{
		for (int j = 0; j < this.dim; j++)
		{
			this.labelByJob[j] = Double.POSITIVE_INFINITY;
		}
		for (int w = 0; w < this.dim; w++)
		{
			for (int j = 0; j < this.dim; j++)
			{
				if(this.costMatrix[w][j] < this.labelByJob[j])
				{
					this.labelByJob[j] = this.costMatrix[w][j];
				}
			}
		}
	}

	/**
	 * Execute the algorithm.
	 * 
	 * @return the minimum cost matching of workers to jobs based upon the provided cost matrix. A matching value of -1 indicates that the corresponding worker is unassigned.
	 */
	public int[] execute()
	{
		/*
		 * Heuristics to improve performance: Reduce rows and columns by their smallest element, compute an initial non-zero dual feasible solution and create a greedy matching from workers to jobs of the cost matrix.
		 */
		this.reduce();
		this.computeInitialFeasibleSolution();
		this.greedyMatch();

		int w = this.fetchUnmatchedWorker();
		while (w < this.dim)
		{
			this.initializePhase(w);
			this.executePhase();
			w = this.fetchUnmatchedWorker();
		}
		int[] result = Arrays.copyOf(this.matchJobByWorker, this.rows);
		for (w = 0; w < result.length; w++)
		{
			if(result[w] >= this.cols)
			{
				result[w] = -1;
			}
		}
		return result;
	}

	/**
	 * Execute a single phase of the algorithm. A phase of the Hungarian algorithm consists of building a set of committed workers and a set of committed jobs from a root unmatched worker by following alternating unmatched/matched zero-slack edges.
	 * If an unmatched job is encountered, then an augmenting path has been found and the matching is grown. If the connected zero-slack edges have been exhausted, the labels of committed workers are increased by the minimum slack among committed
	 * workers and non-committed jobs to create more zero-slack edges (the labels of committed jobs are simultaneously decreased by the same amount in order to maintain a feasible labeling).
	 * <p>
	 * 
	 * The runtime of a single phase of the algorithm is O(n^2), where n is the dimension of the internal square cost matrix, since each edge is visited at most once and since increasing the labeling is accomplished in time O(n) by maintaining the
	 * minimum slack values among non-committed jobs. When a phase completes, the matching will have increased in size.
	 */
	protected void executePhase()
	{
		while (true)
		{
			int minSlackWorker = -1, minSlackJob = -1;
			double minSlackValue = Double.POSITIVE_INFINITY;
			for (int j = 0; j < this.dim; j++)
			{
				if(this.parentWorkerByCommittedJob[j] == -1)
				{
					if(this.minSlackValueByJob[j] < minSlackValue)
					{
						minSlackValue = this.minSlackValueByJob[j];
						minSlackWorker = this.minSlackWorkerByJob[j];
						minSlackJob = j;
					}
				}
			}
			if(minSlackValue > 0)
			{
				this.updateLabeling(minSlackValue);
			}
			this.parentWorkerByCommittedJob[minSlackJob] = minSlackWorker;
			if(this.matchWorkerByJob[minSlackJob] == -1)
			{
				/*
				 * An augmenting path has been found.
				 */
				int committedJob = minSlackJob;
				int parentWorker = this.parentWorkerByCommittedJob[committedJob];
				while (true)
				{
					int temp = this.matchJobByWorker[parentWorker];
					this.match(parentWorker, committedJob);
					committedJob = temp;
					if(committedJob == -1)
					{
						break;
					}
					parentWorker = this.parentWorkerByCommittedJob[committedJob];
				}
				return;
			}
			else
			{
				/*
				 * Update slack values since we increased the size of the committed workers set.
				 */
				int worker = this.matchWorkerByJob[minSlackJob];
				this.committedWorkers[worker] = true;
				for (int j = 0; j < this.dim; j++)
				{
					if(this.parentWorkerByCommittedJob[j] == -1)
					{
						double slack = this.costMatrix[worker][j] - this.labelByWorker[worker] - this.labelByJob[j];
						if(this.minSlackValueByJob[j] > slack)
						{
							this.minSlackValueByJob[j] = slack;
							this.minSlackWorkerByJob[j] = worker;
						}
					}
				}
			}
		}
	}

	/**
	 * 
	 * @return the first unmatched worker or {@link #dim} if none.
	 */
	protected int fetchUnmatchedWorker()
	{
		int w;
		for (w = 0; w < this.dim; w++)
		{
			if(this.matchJobByWorker[w] == -1)
			{
				break;
			}
		}
		return w;
	}

	/**
	 * Find a valid matching by greedily selecting among zero-cost matchings. This is a heuristic to jump-start the augmentation algorithm.
	 */
	protected void greedyMatch()
	{
		for (int w = 0; w < this.dim; w++)
		{
			for (int j = 0; j < this.dim; j++)
			{
				if(this.matchJobByWorker[w] == -1 && this.matchWorkerByJob[j] == -1 && this.costMatrix[w][j] - this.labelByWorker[w] - this.labelByJob[j] == 0)
				{
					this.match(w, j);
				}
			}
		}
	}

	/**
	 * Initialize the next phase of the algorithm by clearing the committed workers and jobs sets and by initializing the slack arrays to the values corresponding to the specified root worker.
	 * 
	 * @param w
	 *            the worker at which to root the next phase.
	 */
	protected void initializePhase(int w)
	{
		Arrays.fill(this.committedWorkers, false);
		Arrays.fill(this.parentWorkerByCommittedJob, -1);
		this.committedWorkers[w] = true;
		for (int j = 0; j < this.dim; j++)
		{
			this.minSlackValueByJob[j] = this.costMatrix[w][j] - this.labelByWorker[w] - this.labelByJob[j];
			this.minSlackWorkerByJob[j] = w;
		}
	}

	/**
	 * Helper method to record a matching between worker w and job j.
	 */
	protected void match(int w, int j)
	{
		this.matchJobByWorker[w] = j;
		this.matchWorkerByJob[j] = w;
	}

	/**
	 * Reduce the cost matrix by subtracting the smallest element of each row from all elements of the row as well as the smallest element of each column from all elements of the column. Note that an optimal assignment for a reduced cost matrix is
	 * optimal for the original cost matrix.
	 */
	protected void reduce()
	{
		for (int w = 0; w < this.dim; w++)
		{
			double min = Double.POSITIVE_INFINITY;
			for (int j = 0; j < this.dim; j++)
			{
				if(this.costMatrix[w][j] < min)
				{
					min = this.costMatrix[w][j];
				}
			}
			for (int j = 0; j < this.dim; j++)
			{
				this.costMatrix[w][j] -= min;
			}
		}
		double[] min = new double[this.dim];
		for (int j = 0; j < this.dim; j++)
		{
			min[j] = Double.POSITIVE_INFINITY;
		}
		for (int w = 0; w < this.dim; w++)
		{
			for (int j = 0; j < this.dim; j++)
			{
				if(this.costMatrix[w][j] < min[j])
				{
					min[j] = this.costMatrix[w][j];
				}
			}
		}
		for (int w = 0; w < this.dim; w++)
		{
			for (int j = 0; j < this.dim; j++)
			{
				this.costMatrix[w][j] -= min[j];
			}
		}
	}

	/**
	 * Update labels with the specified slack by adding the slack value for committed workers and by subtracting the slack value for committed jobs. In addition, update the minimum slack values appropriately.
	 */
	protected void updateLabeling(double slack)
	{
		for (int w = 0; w < this.dim; w++)
		{
			if(this.committedWorkers[w])
			{
				this.labelByWorker[w] += slack;
			}
		}
		for (int j = 0; j < this.dim; j++)
		{
			if(this.parentWorkerByCommittedJob[j] != -1)
			{
				this.labelByJob[j] -= slack;
			}
			else
			{
				this.minSlackValueByJob[j] -= slack;
			}
		}
	}
}

@SuppressWarnings("rawtypes")
class ComparablePair<T1 extends Comparable, T2> extends Pair<T1, T2> implements Comparable<Pair<T1, T2>>
{

	public ComparablePair(T1 p1, T2 p2)
	{
		super(p1, p2);
	}

	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(Pair<T1, T2> o) {
		return this.p1.compareTo(o.p1);
	}
}

//class AdaptiveWeightedMedian
//{
//	public static final double SCALING = 5.0;
//	public static final int ITERS = 4;
//	TreeMap<Double,Double> data;
//	double[] weights;
//	double totalWeight = 0;
//	double scaling;
//
//	public AdaptiveWeightedMedian(double[] data, double[] weights)
//	{
//		this(data, weights, 5.0);
//	}
//	
//	public AdaptiveWeightedMedian(double[] data, double[] weights, double scaling)
//	{
//		if(data.length != weights.length)
//		{
//			throw new IllegalArgumentException("The data and weights must be the same length.");
//		}
//		this.data = new TreeMap<>();
//		for(int i = 0; i < data.length; i++)
//		{
//			this.data.put(data[i], weights[i]);
//		}
//		weights = new double[data.length];
//		this.scaling = scaling;
//	}
//	
//	private Entry<Double,Double> get(int i)
//	{
//		int count = 0;
//		if(i < 0 || i >= this.data.size())
//		{
//			return null;
//		}
//		for(Entry<Double,Double> e : this.data.entrySet())
//		{
//			if(count == i)
//			{
//				return e;
//			}
//		}
//		return null;
//	}
//
//	public Double evaluate(int iters)
//	{
//		if(data.size() == 0)
//		{
//			return null;
//		}
//		if(data.size() == 1)
//		{
//			this.get(0);
//		}
//
//		// get an initial median
//		int n = data.size();
//		int medianIndex = ((n+1)/2)-1;
//		double median = this.get(medianIndex).getKey();
//		if(StatisticsUtility.isEven(data.size()))
//		{
//			median = (median + this.get(medianIndex + 1).getKey())/2.0;
//		}
//		
//		// iteratively approach weighted median
//		int index = 0;
//		double oldMed = Double.MIN_VALUE;
//		double oldOldMed = Double.MIN_VALUE;
//		while(iters > 0 && median != oldMed && median != oldOldMed)
//		{
//			oldOldMed = oldMed;
//			oldMed = median;
//			this.calculateWeights(median, scaling);
//			double cumWeight = 0;
//			index = 0;
//			for(double p : weights)
//			{
//				if(cumWeight > 0.5)
//				{
//					break;
//				}
//				cumWeight = cumWeight + p/totalWeight;
//				index = index + 1;
//			}
//			if(index >= data.size())
//			{
//				index = medianIndex;
//			}
//			median = this.get(index).getKey();
//			iters = iters - 1;
//		}
//		
//		if(weights[index] == 0.5)
//		{
//			median = (median + this.get(index + 1).getKey())/2.0;
//		}
//		return median;
//	}
//
//	private void calculateWeights(double median, double scaling)
//	{
//		/*
//		 * From R test code
//		 * 
//		 * a=1 near optimal;
//		 * 
//		 * b=5 performs well
//		 * 
//		 * getWeights <- function(x, med, a, b)
//		 * {
//		 * 		w <- abs(x-med)
//		 *      w <- w/(mad(x, center=med))
//		 *  	w <- 1/(1+w^b)
//		 *		w <- w/sum(w)
//		 *		return(w)
//		 * }
//		 */
//		totalWeight = 0;
//		double w = 0.0;
//		double mad = StatisticsUtility.weightedMad(this.data.keySet(), weights, median);
//		for(Entry<>)
//		{
//			w = Math.abs(data[i]-median);
//			w = w / mad;
//			w = 1/(1 + Math.pow(w, scaling));
//			weights[i] = w;
//			totalWeight = totalWeight + w;
//		}
//	}	
//}

class AdaptiveMedian
{
	public static final double SCALING = 5.0;
	public static final int ITERS = 4;
	double[] data, weights;
	double totalWeight = 0;
	double scaling;

	public AdaptiveMedian(double[] data)
	{
		this(data, 5.0);
	}
	
	public AdaptiveMedian(double[] data, double scaling)
	{
		this.data = data;
		weights = new double[data.length];
		this.scaling = scaling;
	}

	public Double evaluate(int iters)
	{
		if(data.length == 0)
		{
			return null;
		}
		if(data.length == 1)
		{
			return data[0];
		}
		Arrays.sort(data);

		// get an initial median
		int n = data.length;
		int medianIndex = ((n+1)/2)-1;
		double median = data[medianIndex];
		if(StatisticsUtility.isEven(data.length))
		{
			median = (median + data[medianIndex + 1])/2.0;
		}
		
		// iteratively approach weighted median
		int index = 0;
		double oldMed = Double.MIN_VALUE;
		double oldOldMed = Double.MIN_VALUE;
		while(iters > 0 && median != oldMed && median != oldOldMed)
		{
			oldOldMed = oldMed;
			oldMed = median;
			this.calculateWeights(median, scaling);
			double cumWeight = 0;
			index = 0;
			for(double p : weights)
			{
				if(cumWeight > 0.5)
				{
					break;
				}
				cumWeight = cumWeight + p/totalWeight;
				index = index + 1;
			}
			if(index >= data.length)
			{
				index = medianIndex;
			}
			median = data[index];
			iters = iters - 1;
		}
		
		if(weights[index] == 0.5)
		{
			median = (median + data[index + 1])/2.0;
		}
		return median;
	}

	private void calculateWeights(double median, double scaling)
	{
		/*
		 * From R test code
		 * 
		 * a=1 near optimal;
		 * 
		 * b=5 performs well
		 * 
		 * getWeights <- function(x, med, a, b)
		 * {
		 * 		w <- abs(x-med)
		 *      w <- w/(mad(x, center=med))
		 *  	w <- 1/(1+w^b)
		 *		w <- w/sum(w)
		 *		return(w)
		 * }
		 */
		totalWeight = 0;
		double w = 0.0;
		double mad = StatisticsUtility.weightedMad(data, weights, median);
		for(int i=0; i < data.length; i++)
		{
			w = Math.abs(data[i]-median);
			w = w / mad;
			w = 1/(1 + Math.pow(w, scaling));
			weights[i] = w;
			totalWeight = totalWeight + w;
		}
	}	
}
