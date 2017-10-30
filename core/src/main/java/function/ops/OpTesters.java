package function.ops;

import java.util.Vector;

import function.ops.histogram.BoundedHistogramCreate;
import function.plugin.IJ2.IJ2PluginUtility;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;

public class OpTesters {
	
	public static String thing = "yourPassWord";
	static
	{
		LegacyInjector.preinit();
	}

	public static void main(String[] args) throws Exception
	{
		tryBoundedHistogram();
	}

	@SuppressWarnings("unchecked")
	public static void tryBoundedHistogram()
	{
		Integer[] data = new Integer[]{-10,-9,-8,-7,-6,-5,-4,-3,-2,-1,0,1,2,3,4,5,6,7,8,9,10};
		
		Vector<DoubleType> dataList = new Vector<>();
		for(Integer i : data)
		{
			dataList.add(new DoubleType(i));
		}
		
		UnaryFunctionOp<Iterable, Histogram1d> limitFunc =  Functions.unary(IJ2PluginUtility.ij().op(), BoundedHistogramCreate.class, Histogram1d.class, Iterable.class, 8, BoundedHistogramCreate.FIXED, -4d, 4d, true);
		Histogram1d ret = limitFunc.calculate(dataList);
		printLimitsOfHistogram(ret);
		limitFunc =  Functions.unary(IJ2PluginUtility.ij().op(), BoundedHistogramCreate.class, Histogram1d.class, dataList, 8, BoundedHistogramCreate.PERCENTILE, 25d, 75d, true);
		ret = limitFunc.calculate();
		printLimitsOfHistogram(ret);
		limitFunc =  Functions.unary(IJ2PluginUtility.ij().op(), BoundedHistogramCreate.class, Histogram1d.class, dataList, 8, BoundedHistogramCreate.SIGMA, -2.0d, 2.0d, true);
		ret = limitFunc.calculate();
		printLimitsOfHistogram(ret);
		limitFunc =  Functions.unary(IJ2PluginUtility.ij().op(), BoundedHistogramCreate.class, Histogram1d.class, dataList, 8, BoundedHistogramCreate.SIGMA, -2.0d, 2.0d, false);
		ret = limitFunc.calculate();
		printLimitsOfHistogram(ret);
		limitFunc =  Functions.unary(IJ2PluginUtility.ij().op(), BoundedHistogramCreate.class, Histogram1d.class, dataList, 8, BoundedHistogramCreate.MIN_MAX, -4d, 4d, true);
		ret = limitFunc.calculate();
		printLimitsOfHistogram(ret);
		
	}
	
	private static void printLimitsOfHistogram(Histogram1d hist)
	{
		long n = hist.dimension(0);
		DoubleType ll = new DoubleType();
		DoubleType ul = new DoubleType();
		hist.getLowerBound(0, ll);
		hist.getUpperBound(n-1, ul);
		System.out.println(ll.getRealDouble() + " - " + ul.getRealDouble());
	}
	

}
