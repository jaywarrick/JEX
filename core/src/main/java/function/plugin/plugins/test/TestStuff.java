package function.plugin.plugins.test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;

import function.plugin.IJ2.IJ2PluginUtility;
import image.roi.PointList;
import image.roi.PointSamplerII;
import image.roi.PointSamplerList;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import net.imagej.ops.Ops;
import net.imagej.ops.features.sets.ZernikeFeatureSet;
import net.imagej.ops.features.zernike.helper.ZernikeComputer;
import net.imagej.ops.features.zernike.helper.ZernikeMoment;
import net.imagej.ops.featuresets.NamedFeature;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RealCursor;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geometric.PointCollection;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import rtools.R;

public class TestStuff {

	public static void main (String[] args) throws Exception
	{
		tryZFeatureSet();
	}

	public static void tryZFeatureSet()
	{
		int maxRadius = 100;
		int xOffset = 0;
		int yOffset = 0;
		int nPoints = 15;
		int rndSeed = 1234;
		double rotation = 0;
		double scale = 1.7;

		//DirectoryManager.setHostDirectory("C:/Users/David Niles/Desktop");
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop");

		PointSamplerList<IntType> pl = getRandomPoints(maxRadius, xOffset, yOffset, scale, rotation, nPoints, rndSeed);

		IterableInterval<IntType> ii = new PointSamplerII<IntType>(pl);

		UnaryFunctionOp<IterableInterval<IntType>,RealLocalizable> opCenter = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.CenterOfGravity.class, RealLocalizable.class, ii);
		RealLocalizable center = opCenter.compute1(ii);

		UnaryFunctionOp<RealCursor<IntType>,Circle> opCircle = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, ii.cursor(), center, false, 1234);
		Circle c = opCircle.compute1(ii.cursor());

		@SuppressWarnings("unchecked")
		ZernikeFeatureSet<IntType> opZ = IJ2PluginUtility.ij().op().op(ZernikeFeatureSet.class, ii, 1, 5);

		// Perform the calculations
		opZ.setEnclosingCircle(c);
		Map<NamedFeature, DoubleType> results1 = opZ.compute1(ii);
		// Perform the calculations
		Circle c2 = new Circle(c.getCenter(), c.getRadius()*1.2);
		opZ.setEnclosingCircle(c2);
		Map<NamedFeature, DoubleType> results2 = opZ.compute1(ii);

		System.out.println("\n --- SETTINGS ---");
		System.out.println("offsetXY = (" + xOffset + "," + yOffset + ")");
		System.out.println("rotation = " + rotation);
		System.out.println("scale = " + scale);
		System.out.println("\n --- POINTS ---");
		for(RealLocalizable p : pl)
		{
			System.out.println(p);
		}
		System.out.println("\n --- COM ---");
		System.out.println(center);
		System.out.println("\n --- CIRCLE ---");
		System.out.println(c);
		System.out.println("\n --- RESULTS1 (PaddingRatio=1) ---");
		for(Entry<NamedFeature, DoubleType> e : results1.entrySet())
		{
			System.out.println(e.getKey().getName() + " = " + e.getValue());
		}
		System.out.println("\n --- RESULTS1 (PaddingRatio=1.2) ---");
		for(Entry<NamedFeature, DoubleType> e : results2.entrySet())
		{
			System.out.println(e.getKey().getName() + " = " + e.getValue());
		}



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
		RealCursor<IntType> theCursor = pl.cursor();

		//		UnaryFunctionOp<RealCursor,Circle> opCursor = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, theCursor, (RealLocalizable) null, false, null);
		//		Circle retCursor = opCursor.compute1(theCursor);

		//		UnaryFunctionOp<List,Circle> opList = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, theList, (RealLocalizable) null, false, null);
		//		Circle retList = opList.compute1(theList);

		UnaryFunctionOp<Object,Circle> opCollection = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, theCollection, (RealLocalizable) null, false, null);
		Circle retCollection = (Circle) IJ2PluginUtility.ij().op().run(opCollection, theCollection, null, false, null);

		//Circle result1 = (Circle) IJ2PluginUtility.ij().op().run(Ops.Geometric.SmallestEnclosingCircle.class, col, (RealLocalizable) null, false, null);
		UnaryFunctionOp<RealCursor,Circle> op = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, pl.cursor(), (RealLocalizable) null, false, null);

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
		PointSamplerList<IntType> pl = new PointSamplerList<>(IntType.class);
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
