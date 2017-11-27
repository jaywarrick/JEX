package function.plugin.plugins.test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import function.ops.featuresets.wrappers.MirroredLabelRegionCursor;
import function.ops.geometry.DefaultSymmetryCoefficients;
import function.ops.geometry.DefaultSymmetryLineAngle;
import function.ops.geometry.GeomUtils;
import function.ops.geometry.TwinMirroredLabelRegionCursor;
import function.ops.histogram.BoundedHistogramCreate;
import function.ops.stats.DefaultSpearmansRankCorrelationCoefficient;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.plugins.featureExtraction.FeatureUtils;
import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import net.imagej.ops.Ops;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imagej.patcher.LegacyInjector;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;
import tables.DimensionMap;

public class OpTesters {

	public static String thing = "yourPassWord";
	static
	{
		LegacyInjector.preinit();
	}

	public static void main(String[] args) throws Exception
	{
		testSymmetryFeatures();
	}

	public static void testSymmetryFeatures()
	{
		FeatureUtils fu = new FeatureUtils();
		ImagePlus imMask = new ImagePlus("/Users/jaywarrick/Documents/Miyamoto/Grants/Submitted/2017 R01 - MM TME/Figures/SymmetryTests/Nuclei-01.tif");
		ImagePlus imImage = new ImagePlus("/Users/jaywarrick/Documents/Miyamoto/Grants/Submitted/2017 R01 - MM TME/Figures/SymmetryTests/Nuclei-02.tif");

		// Make the images
		Img<UnsignedByteType> imgA = ImageJFunctions.wrapByte(imMask);
		Img<FloatType> imgB = ImageJFunctions.convertFloat(imImage);
		//			fu.show(imgB, false);

		// get the label region
		ImgLabeling<Integer, IntType> labeling = fu.getLabeling(imgA, true);
		LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);

		NumberFormat formatter = new DecimalFormat("0.0000000"); 

		@SuppressWarnings("rawtypes")
		BinaryFunctionOp<RandomAccessibleInterval<FloatType>, LabelRegion<?>, TreeMap> symcoOp = null;
		
		TreeMap<DimensionMap,Double> results = new TreeMap<>();
		for(LabelRegion<?> region : regions)
		{
			if(symcoOp == null)
			{
				symcoOp = Functions.binary(IJ2PluginUtility.ij().op(), DefaultSymmetryCoefficients.class, TreeMap.class, (RandomAccessibleInterval<FloatType>) imgB, region, 4);
			}
			@SuppressWarnings("unchecked")
			TreeMap<String,Double> maskData = (TreeMap<String,Double>) symcoOp.calculate(null, region); // just get the info for the masks first
			@SuppressWarnings("unchecked")
			TreeMap<String,Double> imageData = (TreeMap<String,Double>) symcoOp.calculate(imgB, region); // just get the info for the masks first
			for(Entry<String,Double> e : maskData.entrySet())
			{
				results.put(new DimensionMap("Source=Mask,Id=" + region.getLabel() + ",Measurement=" + e.getKey()), e.getValue());
			}
			for(Entry<String,Double> e : imageData.entrySet())
			{
				results.put(new DimensionMap("Source=Image,Id=" + region.getLabel() + ",Measurement=" + e.getKey()), e.getValue());
			}
		}
		
		for(Entry<DimensionMap,Double> e : results.entrySet())
		{
			System.out.println(e.getKey().toString() + " = " + formatter.format(e.getValue()));
		}

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void testSymmetryOps()
	{
		FeatureUtils fu = new FeatureUtils();
		ImagePlus imMask = new ImagePlus("/Users/jaywarrick/Documents/Miyamoto/Grants/Submitted/2017 R01 - MM TME/Figures/SymmetryTests/Nuclei-01.tif");
		ImagePlus imImage = new ImagePlus("/Users/jaywarrick/Documents/Miyamoto/Grants/Submitted/2017 R01 - MM TME/Figures/SymmetryTests/Nuclei-02.tif");

		// Make the images
		Img<UnsignedByteType> imgA = ImageJFunctions.wrapByte(imMask);
		Img<FloatType> imgB = ImageJFunctions.convertFloat(imImage);

		// get the label region
		ImgLabeling<Integer, IntType> labeling = fu.getLabeling(imgA, true);
		LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);
		
		// Create a formatter for printing out results in a prettier way.
		NumberFormat formatter = new DecimalFormat("0.0000000"); 

		TwinMirroredLabelRegionCursor<FloatType> tc = new TwinMirroredLabelRegionCursor<>(GeomUtils.INTERP_LINEAR, GeomUtils.OOB_BORDER, 0d,0d,0d);
		tc.setImage(imgB);
		Img toAnalyze = imgB;
		for(LabelRegion<Integer> region : regions)
		{

			UnaryFunctionOp<IterableInterval<?>, RealLocalizable> cogOp = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.CenterOfGravity.class, RealLocalizable.class, Regions.sample(region, toAnalyze));
			RealLocalizable cog = cogOp.calculate();
			//			System.out.println("X: " + region.getCenterOfMass().getDoublePosition(0) + ", Y: " + region.getCenterOfMass().getDoublePosition(1));
			//			System.out.println("X: " + cog.getDoublePosition(0) + ", Y: " + cog.getDoublePosition(1));

			BinaryFunctionOp<LabelRegion<Integer>, Img, ValuePair> thetaOp = Functions.binary(IJ2PluginUtility.ij().op(), DefaultSymmetryLineAngle.class, ValuePair.class, region, toAnalyze, cog, 1);
			ValuePair<Double,Double> results = thetaOp.calculate(region, toAnalyze);
			tc.setRegion(region, cog, results.getA());
			double rnew = DefaultSpearmansRankCorrelationCoefficient.calculateSpearmanRank(tc);
			tc.reset();
			double r2 = DefaultSpearmansRankCorrelationCoefficient.calculateSpearmanRank(tc);
			tc.reset();
			double rold = DefaultSpearmansRankCorrelationCoefficient.calculateSpearmanRankSynchronized(tc);

			// Show the original and mirrored
			// DirectoryManager.setHostDirectory("/Users/jaywarrick/Downloads/temp");
			// fu.show(imgA, false);
			MirroredLabelRegionCursor c = new MirroredLabelRegionCursor(region, region.getCenterOfMass(), results.getA());
			fu.showVoidCursor(c, region, false);
			System.out.println("Amplitude: " + formatter.format(results.getB()) + ", CorrelationNew: " + formatter.format(rnew) + ", CorrelationOld: " + formatter.format(rold) + ", CorrelationNew2: " + formatter.format(r2));
			System.out.println("");
		}

	}

	public static <I extends IntegerType<I>> void mirrorLabelRegion()
	{
		FeatureUtils fu = new FeatureUtils();
		ImagePlus im = new ImagePlus("/Users/jaywarrick/Downloads/BarBell.tif");

		Double[] angles = new Double[]{0d, 15d, 30d, 45d, 60d, 75d, 90d, 105d, 120d, 135d, 150d, 165d, 180d, 195d};
		for(Double angle : angles)
		{
			// Make the images
			ByteProcessor imp = new ByteProcessor(im.getWidth(), im.getHeight());
			imp.copyBits(im.getProcessor(), 0, 0, Blitter.COPY);

			ImagePlus im2 = new ImagePlus("" + angle, imp);
			im2.getProcessor().setBackgroundValue(0.0);
			im2.getProcessor().rotate(angle);
			Img<UnsignedByteType> imgA = ImageJFunctions.wrapByte(im2);
			Img<FloatType> imgB = ImageJFunctions.convertFloat(im2);
			//			fu.show(imgB, false);

			// get the label region
			ImgLabeling<Integer, IntType> labeling = fu.getLabeling(imgA, true);
			LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);
			LabelRegion<Integer> region = regions.iterator().next();

			// Find the symmetry line
			@SuppressWarnings("rawtypes")
			BinaryFunctionOp<LabelRegion<Integer>, Img<FloatType>, ValuePair> thetaOp = Functions.binary(IJ2PluginUtility.ij().op(), DefaultSymmetryLineAngle.class, ValuePair.class, region, imgB, region.getCenterOfMass(), 1);
			@SuppressWarnings("unchecked")
			ValuePair<Double,Double> results = thetaOp.calculate(region, imgB);

			// Show the original and mirrored
			// DirectoryManager.setHostDirectory("/Users/jaywarrick/Downloads/temp");
			// fu.show(imgA, false);
			MirroredLabelRegionCursor c = new MirroredLabelRegionCursor(region, region.getCenterOfMass(), results.getA());
			fu.showVoidCursor(c, region, false);	
			System.out.println("");

			// TODO: Check to see what happens if mirroring the object at the boundary of the image
			// goes over the boundary and causes a problem.
		}
	}

	@SuppressWarnings("rawtypes")
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

		/**
		 * > getPercentileLimits(duh, .25, .75)
		 * [1] -5  5
		 * > daMed - 2*daMad
		 * [1] -14.826
		 * > daMed + 2*daMad
		 * [1] 14.826
		 * > daMean - 2*daStdDev
		 * [1] -12.40967
		 * > daMean + 2*daStdDev
		 * [1] 12.40967
		 */

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
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
