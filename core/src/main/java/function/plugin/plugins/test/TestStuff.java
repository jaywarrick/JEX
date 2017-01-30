package function.plugin.plugins.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import function.ops.geometry.DefaultSmallestEnclosingCircle;
import function.ops.zernike.ZernikeComputer;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.plugins.featureExtraction.FeatureUtils;
import ij.ImagePlus;
import image.roi.PointList;
import image.roi.PointSample;
import image.roi.PointSampler;
import image.roi.PointSamplerII;
import image.roi.PointSamplerList;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import net.imagej.ops.Ops;
import net.imagej.ops.features.zernike.helper.ZernikeMoment;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealCursor;
import net.imglib2.RealLocalizable;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.geometric.PointCollection;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import rtools.R;

public class TestStuff {

	public static void main (String[] args) throws Exception
	{
		tryLabelSubLabelRegion();
	}
	
	public static void tryLabelSubLabelRegion()
	{
		IJ2PluginUtility.ij().op();
		ImagePlus im = new ImagePlus("/Users/jaywarrick/Desktop/For ImageJ Forum/Objects.tif");
		ImagePlus im2 = new ImagePlus("/Users/jaywarrick/Desktop/For ImageJ Forum/ErodedObjects.tif");
		
		FeatureUtils utils = new FeatureUtils();
		// Make a two images, parent and child
		RandomAccessibleInterval<UnsignedByteType> parentImg = ImageJFunctions.wrapByte(im);
		RandomAccessibleInterval<UnsignedByteType> childImg = ImageJFunctions.wrapByte(im2);
		
		
		// Create a LabelRegion from the parent.
		ImgLabeling<Integer, IntType > labeling = utils.getLabeling(parentImg, true);
		ImgLabeling<Integer, IntType > subLabeling = utils.getSubLabeling(labeling, childImg);
		
		utils.show(labeling);
		utils.show(subLabeling);
		
		LabelRegions<Integer> regions = new LabelRegions<>(subLabeling);
		int i = 1;
		for(LabelRegion<Integer> r : regions)
		{
			utils.showRegion(r, true);
			RandomAccessibleInterval<UnsignedByteType> crop = utils.cropRealRAI(r, childImg);
			ImgLabeling<Integer, IntType> temp = utils.getLabeling(crop, true);
			LabelRegions<Integer> temp2 = new LabelRegions<Integer>(temp);
			System.out.println("Region" + i + " size = " + temp2.getExistingLabels().size());
			Polygon p = utils.getPolygonFromBoolean(r);
			System.out.println(p.getVertices());
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends RealType<T>> void tryMomentInvariance() throws Exception
	{
		
		//### Results
		/*
		 * For Hu3, using getAbsNormDiff and getAbsSum made the diffHu the same as the HuDiff 
		 * 
		 * For Hu2, 
		 */
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop");
		PointSamplerList<DoubleType> pl1a = getRCGrid(4,3,1);
		PointSamplerList<DoubleType> pl1b = getRCGrid(4,3,1);
		pl1b.rotate(180);
		double[] origin = new double[]{0,0};
		pl1b = pl1b.getRealPointListCenteredAt(origin);
		PointSamplerList<DoubleType> pl1c = getDiff(pl1a, pl1b);
		System.out.println(pl1a);
		System.out.println(pl1b);
		System.out.println(pl1c);
		
		PointSamplerList<DoubleType> pl2a = pl1a.copy();
		pl2a.rotate(37);
		pl2a = pl2a.getRealPointListCenteredAt(origin);
		PointSamplerList<DoubleType> pl2b = pl1b.copy();
		pl2b.rotate(37);
		pl2b = pl2b.getRealPointListCenteredAt(origin);
		PointSamplerList<DoubleType> pl2c = pl1c.copy();
		pl2c.rotate(37);
		pl2c = pl2c.getRealPointListCenteredAt(origin);
		//System.out.println(pl2a);
		//System.out.println(pl2b);
		System.out.println(pl2c);
		
		PointSamplerII<DoubleType> ii = new PointSamplerII<DoubleType>(pl1a);
		UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType> op11 = (UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType>) IJ2PluginUtility.ij().op().op(Ops.ImageMoments.Moment11.class, ii);
		UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType> op20 = (UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType>) IJ2PluginUtility.ij().op().op(Ops.ImageMoments.CentralMoment20.class, ii);
		UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType> op02 = (UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType>) IJ2PluginUtility.ij().op().op(Ops.ImageMoments.CentralMoment02.class, ii);
		UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType> op30 = (UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType>) IJ2PluginUtility.ij().op().op(Ops.ImageMoments.CentralMoment30.class, ii);
		UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType> op03 = (UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType>) IJ2PluginUtility.ij().op().op(Ops.ImageMoments.CentralMoment03.class, ii);
		UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType> op21 = (UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType>) IJ2PluginUtility.ij().op().op(Ops.ImageMoments.CentralMoment21.class, ii);
		UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType> op12 = (UnaryFunctionOp<IterableInterval<DoubleType>, DoubleType>) IJ2PluginUtility.ij().op().op(Ops.ImageMoments.CentralMoment12.class, ii);
		
		double val20, val02, bval20, bval02, cval20, cval02, val11, bval11, cval11, val30, val03, val21, val12, bval30, bval03, bval21, bval12, cval30, cval03, cval21, cval12, suma, sumb, sumc;
		
		ii = new PointSamplerII<DoubleType>(pl1a);
		val11 = op11.compute1(ii).getRealDouble();
		val02 = op02.compute1(ii).getRealDouble();
		val20 = op20.compute1(ii).getRealDouble();
		val03 = op03.compute1(ii).getRealDouble();
		val30 = op30.compute1(ii).getRealDouble();
		val21 = op21.compute1(ii).getRealDouble();
		val12 = op12.compute1(ii).getRealDouble();
		suma = getSum(ii);
		printInfo2(val20, val02, val11, suma);
		//printInfo3(val30, val03, val21, val12, suma);
		ii = new PointSamplerII<DoubleType>(pl1b);
		bval11 = op11.compute1(ii).getRealDouble();
		bval02 = op02.compute1(ii).getRealDouble();
		bval20 = op20.compute1(ii).getRealDouble();
		bval03 = op03.compute1(ii).getRealDouble();
		bval30 = op30.compute1(ii).getRealDouble();
		bval21 = op21.compute1(ii).getRealDouble();
		bval12 = op12.compute1(ii).getRealDouble();
		sumb = getSum(ii);
		printInfo2(bval20, bval02, bval11, sumb);
		//printInfo3(bval30, bval03, bval21, bval12, sumb);
		ii = new PointSamplerII<DoubleType>(pl1c);
		cval11 = op11.compute1(ii).getRealDouble();
		cval02 = op02.compute1(ii).getRealDouble();
		cval20 = op20.compute1(ii).getRealDouble();
		cval03 = op03.compute1(ii).getRealDouble();
		cval30 = op30.compute1(ii).getRealDouble();
		cval21 = op21.compute1(ii).getRealDouble();
		cval12 = op12.compute1(ii).getRealDouble();
		sumc = getAbsSum(ii);
		printInfo2(cval20, cval02, cval11, sumc);
		//printInfo3(cval30, cval03, cval21, cval12, sumc);
		//System.out.println("Hu3(Diff)=" + getHu3(val30-bval30, val03-bval03, val21-bval21, val12-bval12));
		//System.out.println("Hu3(Diff)=" + getHu3(val30/suma-bval30/sumb, val03/suma-bval03/sumb, val21/suma-bval21/sumb, val12/suma-bval12/sumb));
		
		ii = new PointSamplerII<DoubleType>(pl2a);
		val11 = op11.compute1(ii).getRealDouble();
		val02 = op02.compute1(ii).getRealDouble();
		val20 = op20.compute1(ii).getRealDouble();
		val03 = op03.compute1(ii).getRealDouble();
		val30 = op30.compute1(ii).getRealDouble();
		val21 = op21.compute1(ii).getRealDouble();
		val12 = op12.compute1(ii).getRealDouble();
		suma = getSum(ii);
		printInfo2(val20, val02, val11, suma);
		//printInfo3(val30, val03, val21, val12, suma);
		ii = new PointSamplerII<DoubleType>(pl2b);
		bval11 = op11.compute1(ii).getRealDouble();
		bval02 = op02.compute1(ii).getRealDouble();
		bval20 = op20.compute1(ii).getRealDouble();
		bval03 = op03.compute1(ii).getRealDouble();
		bval30 = op30.compute1(ii).getRealDouble();
		bval21 = op21.compute1(ii).getRealDouble();
		bval12 = op12.compute1(ii).getRealDouble();
		sumb = getSum(ii);
		printInfo2(bval20, bval02, bval11, sumb);
		//printInfo3(bval30, bval03, bval21, bval12, sumb);
		ii = new PointSamplerII<DoubleType>(pl2c);
		cval11 = op11.compute1(ii).getRealDouble();
		cval02 = op02.compute1(ii).getRealDouble();
		cval20 = op20.compute1(ii).getRealDouble();
		cval03 = op03.compute1(ii).getRealDouble();
		cval30 = op30.compute1(ii).getRealDouble();
		cval21 = op21.compute1(ii).getRealDouble();
		cval12 = op12.compute1(ii).getRealDouble();
		sumc = getAbsSum(ii);
		printInfo2(cval20, cval02, cval11, sumc);
		//System.out.println("Hu3(Diff)=" + getHu3(val30-bval30, val03-bval03, val21-bval21, val12-bval12));
		//System.out.println("Hu3(Diff)=" + getHu3(val30/suma-bval30/sumb, val03/suma-bval03/sumb, val21/suma-bval21/sumb, val12/suma-bval12/sumb));
		
	}
	
	public static PointSamplerList<DoubleType> getDiff(PointSamplerList<DoubleType> pl1, PointSamplerList<DoubleType> pl2)
	{
		PointSamplerList<DoubleType> ret = new PointSamplerList<DoubleType>(new DoubleType(0));
		for(PointSampler<DoubleType> p1 : pl1)
		{
			for(PointSampler<DoubleType> p2 : pl2)
			{
				if(p2.getDoublePosition(0) == p1.getDoublePosition(0) && p2.getDoublePosition(1) == p1.getDoublePosition(1))
				{
					PointSampler<DoubleType> toAdd = new PointSample<DoubleType>(p1);
					toAdd.get().set(p1.get().getRealDouble() - p2.get().getRealDouble());
					ret.add(toAdd);
				}
			}
		}
		return ret;
	}
	
	public static PointSamplerList<DoubleType> getAbsDiff(PointSamplerList<DoubleType> pl1, PointSamplerList<DoubleType> pl2)
	{
		PointSamplerList<DoubleType> ret = new PointSamplerList<DoubleType>(new DoubleType(0));
		for(PointSampler<DoubleType> p1 : pl1)
		{
			for(PointSampler<DoubleType> p2 : pl2)
			{
				if(p2.getDoublePosition(0) == p1.getDoublePosition(0) && p2.getDoublePosition(1) == p1.getDoublePosition(1))
				{
					PointSampler<DoubleType> toAdd = new PointSample<DoubleType>(p1);
					toAdd.get().set(Math.abs(p1.get().getRealDouble() - p2.get().getRealDouble()));
					ret.add(toAdd);
				}
			}
		}
		return ret;
	}
	
	public static PointSamplerList<DoubleType> getAbsNormDiff(PointSamplerList<DoubleType> pl1, PointSamplerList<DoubleType> pl2)
	{
		double sum1 = getSum(pl1);
		double sum2 = getSum(pl2);
		PointSamplerList<DoubleType> ret = new PointSamplerList<DoubleType>(new DoubleType(0));
		for(PointSampler<DoubleType> p1 : pl1)
		{
			for(PointSampler<DoubleType> p2 : pl2)
			{
				if(p2.getDoublePosition(0) == p1.getDoublePosition(0) && p2.getDoublePosition(1) == p1.getDoublePosition(1))
				{
					PointSampler<DoubleType> toAdd = new PointSample<DoubleType>(p1);
					toAdd.get().set(Math.abs(p1.get().getRealDouble()/sum1 - p2.get().getRealDouble()/sum2));
					ret.add(toAdd);
				}
			}
		}
		return ret;
	}
	
	public static PointSamplerList<DoubleType> getNormDiff(PointSamplerList<DoubleType> pl1, PointSamplerList<DoubleType> pl2)
	{
		double sum1 = getSum(pl1);
		double sum2 = getSum(pl2);
		PointSamplerList<DoubleType> ret = new PointSamplerList<DoubleType>(new DoubleType(0));
		for(PointSampler<DoubleType> p1 : pl1)
		{
			for(PointSampler<DoubleType> p2 : pl2)
			{
				if(p2.getDoublePosition(0) == p1.getDoublePosition(0) && p2.getDoublePosition(1) == p1.getDoublePosition(1))
				{
					PointSampler<DoubleType> toAdd = new PointSample<DoubleType>(p1);
					toAdd.get().set(p1.get().getRealDouble()/sum1 - p2.get().getRealDouble()/sum2);
					ret.add(toAdd);
				}
			}
		}
		return ret;
	}
	
	public static double getSum(PointSamplerII<DoubleType> ii)
	{
		double tot = 0;
		for(PointSampler<DoubleType> p : ii.pl)
		{
			tot = tot + p.get().getRealDouble();
		}
		return tot;
	}
	public static double getAbsSum(PointSamplerII<DoubleType> ii)
	{
		double tot = 0;
		for(PointSampler<DoubleType> p : ii.pl)
		{
			tot = tot + Math.abs(p.get().getRealDouble());
		}
		return tot;
	}
	public static double getSum(PointSamplerList<DoubleType> pl)
	{
		double tot = 0;
		for(PointSampler<DoubleType> p : pl)
		{
			tot = tot + p.get().getRealDouble();
		}
		return tot;
	}
	
	public static double getAbsSum(PointSamplerList<DoubleType> pl)
	{
		double tot = 0;
		for(PointSampler<DoubleType> p : pl)
		{
			tot = tot + Math.abs(p.get().getRealDouble());
		}
		return tot;
	}
	
	public static void printInfo3(double n30, double n03, double n21, double n12, double sum)
	{
		System.out.println("val30=" + n30 + ", val03=" + n03 + ", val21=" + n21 + ", val12=" + n12 + ", Hu3=" + getHu3(n30/sum, n03/sum, n21/sum, n12/sum));
	}
	
	public static void printInfo2(double n20, double n02, double n11, double sum)
	{
		System.out.println("val20=" + n20 + ", val02=" + n02 + ", val11=" + n11 + ", Hu2=" + getHu2(n20/sum, n02/sum, n11/sum));
	}
	
	public static double getHu3(double n30, double n03, double n21, double n12)
	{
		double ret = (n30 - 3*n12) * (n30 - 3*n12) + (3*n21 - n03) * (3*n21 - n03);
		return ret;
	}
	
	public static double getHu2(double n20, double n02, double n11)
	{
		double ret = (n20 - n02) * (n20 - n02) + 4 * n11 * n11;
		return ret;
	}
	
	public static PointSamplerList<DoubleType> getRCGrid(int r, int c, double intensity)
	{
		PointSamplerList<DoubleType> pl = new PointSamplerList<DoubleType>(new DoubleType(0.0));
		double sum = 0;
		for(int i = -r; i <= r; i++)
		{
			for(int j = -c; j <= c; j++)
			{
				if(i > 0)
				{
					pl.add(i, j, i*intensity);
					sum = sum + i*intensity;
				}
				else
				{
					pl.add(i, j, intensity);
					sum = sum + intensity;
				}
			}
		}
		System.out.println(sum);
		return pl;
	}
	
	public static void trySamplingIterableRegion() throws Exception
	{
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop");
		PointSamplerList<IntType> pl = new PointSamplerList<IntType>(new IntType(0));
		pl.add(0, 0);
		IJ2PluginUtility.ij().op().op(DefaultSmallestEnclosingCircle.class, pl);
		ImagePlus im = new ImagePlus("/Users/jaywarrick/Desktop/For ImageJ Forum/Objects.tif");
		ImagePlus im2 = new ImagePlus("/Users/jaywarrick/Desktop/For ImageJ Forum/ErodedObjects.tif");
		Img<UnsignedByteType> img = ImageJFunctions.wrapByte(im);
		Img<UnsignedByteType> img2 = ImageJFunctions.wrapByte(im2);
		FeatureUtils utils = new FeatureUtils();
		//utils.show(img2);
		ImgLabeling<Integer, IntType> labeling = utils.getLabeling(img, true);
		//utils.showLabelingImg(labeling, true);
		ImgLabeling<Integer, IntType> labeling2 = utils.getLabeling(img2, true);
		LabelRegions<Integer> cellRegions = new LabelRegions<Integer>(labeling);
		LabelRegions<Integer> cellRegions2 = new LabelRegions<Integer>(labeling2);
		//FileUtility.showImg(img, false);
		for(LabelRegion<Integer> region : cellRegions)
		{
			for(LabelRegion<Integer> region2 : cellRegions2)
			{
				Cursor<Void> reg2 = region2.cursor();
				reg2.fwd();
				RandomAccess<BoolType> reg = region.randomAccess();
				reg.setPosition(reg2);
				if(reg.get().get())
				{
					// Then show the region and intersected region
					ImgLabeling<Integer,IntType> tempLabeling = utils.getLabelingInRegion(region, img2, true);
					utils.show(tempLabeling);
					System.out.println(region.getLabel());
					System.out.println(region.size());
				}
			}
			//			System.out.println(region.getLabel());
			//			System.out.println(region.size());
			//			ImageJFunctions.show(utils.convertLabelRegionToByteImage(region));
			//			SamplingIterableRegion<UnsignedByteType> inter = new SamplingIterableRegion<UnsignedByteType>(region, img2);
			//			ImageJFunctions.show(inter);
			//			System.out.println(region.getLabel());
		}
	}

	public static void tryZFeatureSet()
	{
		int maxRadius = 100;
		int xOffset = 0;
		int yOffset = 10;
		int nPoints = 100;
		int rndSeed = 1234;
		double rotation = 0;
		double scale = 1.7;

		//DirectoryManager.setHostDirectory("C:/Users/David Niles/Desktop");
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop");

		PointSamplerList<IntType> pl = getRandomPoints(maxRadius, xOffset, yOffset, scale, rotation, nPoints, rndSeed);
		
		
		PointSamplerList<IntType> pl3 = new PointSamplerList<>(new IntType(0));
		
		pl3.add(new PointSample<IntType>(0,0,new IntType(1)));
		pl3.add(new PointSample<IntType>(0,1,new IntType(1)));
		pl3.add(new PointSample<IntType>(0,2,new IntType(1)));
		pl3.add(new PointSample<IntType>(1,0,new IntType(1)));
		pl3.add(new PointSample<IntType>(1,1,new IntType(1)));
		pl3.add(new PointSample<IntType>(1,2,new IntType(1)));
		pl3.add(new PointSample<IntType>(2,0,new IntType(1)));
		pl3.add(new PointSample<IntType>(2,1,new IntType(1)));
		pl3.add(new PointSample<IntType>(2,2,new IntType(1)));
		
		PointSamplerList<IntType> pl2 = new PointSamplerList<>(new IntType(0));
		for(PointSampler<IntType> p : pl)
		{
			pl2.add(p);
			pl2.add(new PointSample<IntType>(-1*p.getDoublePosition(0), -1*p.getDoublePosition(1), new IntType(1)));
		}

		IterableInterval<IntType> ii = new PointSamplerII<IntType>(pl2);

		UnaryFunctionOp<IterableInterval<IntType>,RealLocalizable> opCenter = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.CenterOfGravity.class, RealLocalizable.class, ii);
		RealLocalizable center = opCenter.compute1(ii);

		UnaryFunctionOp<RealCursor<IntType>,Circle> opCircle = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, ii.cursor(), center, false, 1234);
		Circle c = opCircle.compute1(ii.cursor());

		ZernikeComputer<IntType> comp = new ZernikeComputer<>();
		comp.setOrder(0);
		comp.setRepetition(0);
		comp.setEnclosingCircle(c);
		
		ZernikeMoment zm = comp.compute1(ii);
		System.out.println(zm.getMagnitude());
		
		
		//@SuppressWarnings("unchecked")
		//ZernikeFeatureSet<IntType> opZ = IJ2PluginUtility.ij().op().op(ZernikeFeatureSet.class, ii, 1, 5);

		//		// Perform the calculations
		//		//opZ.setEnclosingCircle(c);
		//		Map<NamedFeature, DoubleType> results1 = opZ.compute1(ii);
		//		// Perform the calculations
		//		Circle c2 = new Circle(c.getCenter(), c.getRadius()*1.2);
		//		opZ.setEnclosingCircle(c2);
		//		Map<NamedFeature, DoubleType> results2 = opZ.compute1(ii);
		//
		//		System.out.println("\n --- SETTINGS ---");
		//		System.out.println("offsetXY = (" + xOffset + "," + yOffset + ")");
		//		System.out.println("rotation = " + rotation);
		//		System.out.println("scale = " + scale);
		//		System.out.println("\n --- POINTS ---");
		//		for(RealLocalizable p : pl)
		//		{
		//			System.out.println(p);
		//		}
		//		System.out.println("\n --- COM ---");
		//		System.out.println(center);
		//		System.out.println("\n --- CIRCLE ---");
		//		System.out.println(c);
		//		System.out.println("\n --- RESULTS1 (PaddingRatio=1) ---");
		//		for(Entry<NamedFeature, DoubleType> e : results1.entrySet())
		//		{
		//			System.out.println(e.getKey().getName() + " = " + e.getValue());
		//		}
		//		System.out.println("\n --- RESULTS1 (PaddingRatio=1.2) ---");
		//		for(Entry<NamedFeature, DoubleType> e : results2.entrySet())
		//		{
		//			System.out.println(e.getKey().getName() + " = " + e.getValue());
		//		}



	}

	public static void tryCircleOp() throws Exception
	{
		int maxRadius = 100;
		int xOffset = 0;
		int yOffset = 0;
		int nPoints = 100;
		int rndSeed = 1234;
		double scale = 1;
		double rotation = 0;

		//DirectoryManager.setHostDirectory("C:/Users/David Niles/Desktop");
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop");

		PointSamplerList<IntType> pl = getRandomPoints(maxRadius, xOffset, yOffset, scale, rotation, nPoints, rndSeed);

		List<RealLocalizable> theList = new Vector<>();
		theList.addAll(pl);
		Collection<RealLocalizable> theCollection = new HashSet<RealLocalizable>();
		theCollection.addAll(pl);
		//RealCursor<IntType> theCursor = pl.cursor();

		//		UnaryFunctionOp<RealCursor,Circle> opCursor = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, theCursor, (RealLocalizable) null, false, null);
		//		Circle retCursor = opCursor.compute1(theCursor);

		//		UnaryFunctionOp<List,Circle> opList = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, theList, (RealLocalizable) null, false, null);
		//		Circle retList = opList.compute1(theList);

		//UnaryFunctionOp<Object,Circle> opCollection = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, theCollection, (RealLocalizable) null, false, null);
		//Circle retCollection = (Circle) IJ2PluginUtility.ij().op().run(opCollection, theCollection, null, false, null);

		//Circle result1 = (Circle) IJ2PluginUtility.ij().op().run(Ops.Geometric.SmallestEnclosingCircle.class, col, (RealLocalizable) null, false, null);
		UnaryFunctionOp<RealCursor<IntType>,Circle> op = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, pl.cursor(), (RealLocalizable) null, false, null);

		//		List<Circle> result = (List<Circle>) op.compute1(plII);
		Circle result = (Circle) op.compute1(pl.cursor());

		//		UnaryFunctionOp<IterableInterval<IntType>,ZernikeMoment> opZ = Functions.unary(IJ2PluginUtility.ij().op(), ZernikeComputer.class, ZernikeMoment.class, plII, 2, 2, null, null);
		//		ZernikeMoment m = opZ.compute1(plII);
		//		double mag = m.getMagnitude();
		//		System.out.println(plII);
		//		System.out.println(center);
		//		System.out.println(mag);
		//		System.out.println(result);
		System.out.println(result);
		TestStuff.plotAndShowResults(pl, result.getCenter().getDoublePosition(0), result.getCenter().getDoublePosition(1), result.getRadius());
		//		TestStuff.plotAndShowResults2(pl, result);

		//		System.out.println(pl);
		//		FeatureUtils utils = new FeatureUtils();
		//		Polygon p = utils.convert(plII);
		//		List<RealLocalizable> other = new Vector<>();
	}

	public static void printIterable(Iterable<? extends RealLocalizable> pts)
	{
		for(RealLocalizable p : pts)
		{
			System.out.println(p);
		}
	}

	public static void tryIIPointList() throws Exception
	{
		int maxRadius = 100;
		int xOffset = 0;
		int yOffset = 0;
		int nPoints = 5;
		int rndSeed = 1234;
		double scale = 1;
		double rotation = 0;

		//DirectoryManager.setHostDirectory("C:/Users/David Niles/Desktop");
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop");

		PointSamplerList<IntType> pl = getRandomPoints(maxRadius, xOffset, yOffset, scale, rotation, nPoints, rndSeed);

		//pl.rotate(90);

		pl = getPointListWithCoMAtOrigin(pl);
		PointSamplerList<IntType> pl2 = pl.copy();
		pl2.rotate(30);
		pl2 = getPointListWithCoMAtOrigin(pl2);
		PointSamplerList<IntType> pl3 = pl2.copy();
		pl3.translate(10, 10);
		PointSamplerList<IntType> pl4 = pl3.copy();
		pl4.rotate(-30);
		pl4 = getPointListWithCoMAtOrigin(pl4);
		PointSamplerList<IntType> pl5 = pl4.copy();
		pl5.translate(100, 100);
		pl5.rotate(40.7);
		pl5.scale(1.722);
		//		pl5 = getPointListWithCoMAtOrigin(pl5);


		getZernikeInfo(pl);
		getZernikeInfo(pl2);
		getZernikeInfo(pl3);
		getZernikeInfo(pl4);
		getZernikeInfo(pl5);


	}

	public static <T extends RealType<T>> PointSamplerList<T> getPointListWithCoMAtOrigin(PointSamplerList<T> pl)
	{
		pl = pl.getRealPointListRelativeToCenter();
		RealLocalizable com = getCenterOfMass(pl);
		pl.translate(-1*com.getDoublePosition(0), -1*com.getDoublePosition(1));
		System.out.println("Center of Mass now at: " + getCenterOfMass(pl));
		return pl;
	}

	public static <T extends RealType<T>> void getZernikeInfo(PointSamplerList<T> pl) throws Exception
	{
		PointSamplerII<T> plII = new PointSamplerII<>(pl);
		RealLocalizable center = getCenterOfMass(plII.pl);
		UnaryFunctionOp<IterableInterval<T>,Circle> op = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, plII, (RealLocalizable) null);
		Circle result = op.compute1(plII);

		UnaryFunctionOp<IterableInterval<T>,ZernikeMoment> opZ = Functions.unary(IJ2PluginUtility.ij().op(), ZernikeComputer.class, ZernikeMoment.class, plII, 2, 2, null, null);
		ZernikeMoment m = opZ.compute1(plII);
		double mag = m.getMagnitude();
		System.out.println(plII);
		System.out.println(center);
		System.out.println(mag);
		System.out.println(result);
		TestStuff.plotAndShowResults(plII.pl, result.getCenter().getDoublePosition(0), result.getCenter().getDoublePosition(1), result.getRadius());
	}

	public static <T extends RealType<T>> RealLocalizable getCenterOfMass(PointSamplerList<T> pl)
	{
		return (RealLocalizable) IJ2PluginUtility.ij().op().run(Ops.Geometric.CenterOfGravity.class, new PointSamplerII<T>(pl));
	}

	public static PointSamplerList<IntType> getRandomPoints(int maxRadius, int xOffset, int yOffset, double scale, double rotation, int nPoints, int rndSeed)
	{
		PointSamplerList<IntType> pl = new PointSamplerList<>(new IntType(0));
		Random rand = new Random(rndSeed);

		for(int i = 0; i < nPoints; i++)
		{
			double r = rand.nextDouble()*maxRadius;
			double theta = rand.nextDouble()*2*Math.PI;
			pl.add((int) (r*Math.sin(theta)), (int) (r*Math.cos(theta)), 1); 
		}

		pl.translate(xOffset, yOffset);
		pl.rotate(rotation);
		pl.scale(scale);
		return pl;
	}

	public static IterableInterval<Void> getIterableInterval(PointList pl)
	{
		IterableInterval<Void> p = new PointCollection(pl);
		Cursor<Void> c = p.cursor();
		long[] pos = new long[p.numDimensions()];
		while(c.hasNext())
		{
			c.fwd();
			c.localize(pos);
			System.out.println(pos[0] + " " + pos[1]);
		}

		return p;
	}

	public static void plotAndShowResults(PointSamplerList<?> pl, double xCenter, double yCenter, double radius) throws Exception
	{
		R.eval("x <- 1"); // Just to get R going.
		R.makeVector("x", pl.getDoubleArray(0));
		R.makeVector("y", pl.getDoubleArray(1));
		R.load("plotrix");
		String filePath = R.startPlot("pdf", 7, 5, 0, 12, "Helvetica", null);
		R.eval("plot(x,y, xlab='X [pixels]', ylab='Y [pixels]', asp=1)");
		R.eval("draw.circle(x=" + xCenter + ",y=" + yCenter + ",radius=" + radius + ")");
		R.endPlot();
		FileUtility.openFileDefaultApplication(filePath);
		//		System.out.print("Center: (" + xCenter + ", " + yCenter + ")    ");
		//		System.out.print("Radius: " + radius);

	}

	public static void plotAndShowResults2(PointSamplerList<?> pl, List<Circle> circles) throws Exception
	{
		R.eval("x <- 1"); // Just to get R going.
		R.eval("graphics.off()");
		R.makeVector("x", pl.getDoubleArray(0));
		R.makeVector("y", pl.getDoubleArray(1));
		R.load("plotrix");
		String filePath = R.startPlot("pdf", 7, 5, 0, 12, "Helvetica", null);
		R.eval("plot(x,y, xlab='X [pixels]', ylab='Y [pixels]', asp=1)");

		double total = circles.size()*2;
		double count = 0;
		for(Circle c : circles)
		{
			if(c != null)
			{
				R.eval("draw.circle(x=" + c.getCenter().getDoublePosition(0) + ",y=" + c.getCenter().getDoublePosition(1) + ",radius=" + c.getRadius() + ", border=gray(" + (0.5-count/total) + "))");
			}
			count = count + 1;
		}
		R.eval("draw.circle(x=" + circles.get(circles.size()-1).getCenter().getDoublePosition(0) + ",y=" + circles.get(circles.size()-1).getCenter().getDoublePosition(1) + ",radius=" + circles.get(circles.size()-1).getRadius() + ", border='blue')");
		R.endPlot();
		FileUtility.openFileDefaultApplication(filePath);
		//		System.out.print("Center: (" + xCenter + ", " + yCenter + ")    ");
		//		System.out.print("Radius: " + radius);

	}


}
