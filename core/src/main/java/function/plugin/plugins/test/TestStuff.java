package function.plugin.plugins.test;

import java.util.List;
import java.util.Random;

import function.plugin.IJ2.IJ2PluginUtility;
import image.roi.PointList;
import image.roi.PointSampleList;
import image.roi.PointSampleListII;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import net.imagej.ops.Ops;
import net.imagej.ops.features.zernike.helper.ZernikeComputer;
import net.imagej.ops.features.zernike.helper.ZernikeMoment;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.special.Functions;
import net.imagej.ops.special.UnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geometric.PointCollection;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import rtools.R;

public class TestStuff {

	public static void main (String[] args) throws Exception
	{
		tryCircleOp();
	}

	public static void tryCircleOp() throws Exception
	{
		int maxRadius = 100;
		int xOffset = 0;
		int yOffset = 0;
		int nPoints = 100;
		int rndSeed = 1234;

		//DirectoryManager.setHostDirectory("C:/Users/David Niles/Desktop");
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop");

		PointSampleList<IntType> pl = getRandomPoints(maxRadius, xOffset, yOffset, nPoints, rndSeed);
		PointSampleListII<IntType> plII = new PointSampleListII<IntType>(pl);

		UnaryFunctionOp<IterableInterval<IntType>,Circle> op = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, plII, (RealLocalizable) null, null, false);

		//		List<Circle> result = (List<Circle>) op.compute1(plII);
		Circle result = (Circle) op.compute1(plII);

		//		UnaryFunctionOp<IterableInterval<IntType>,ZernikeMoment> opZ = Functions.unary(IJ2PluginUtility.ij().op(), ZernikeComputer.class, ZernikeMoment.class, plII, 2, 2, null, null);
		//		ZernikeMoment m = opZ.compute1(plII);
		//		double mag = m.getMagnitude();
		//		System.out.println(plII);
		//		System.out.println(center);
		//		System.out.println(mag);
		//		System.out.println(result);
		System.out.println(result);
		//TestStuff.plotAndShowResults(pl, result.getCenter().getDoublePosition(0), result.getCenter().getDoublePosition(1), result.getRadius());
		//		TestStuff.plotAndShowResults2(pl, result);
	}

	public static void tryIIPointList() throws Exception
	{
		int maxRadius = 100;
		int xOffset = 0;
		int yOffset = 0;
		int nPoints = 5;
		int rndSeed = 1234;

		//DirectoryManager.setHostDirectory("C:/Users/David Niles/Desktop");
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop");

		PointSampleList<IntType> pl = getRandomPoints(maxRadius, xOffset, yOffset, nPoints, rndSeed);

		//pl.rotate(90);

		pl = getPointListWithCoMAtOrigin(pl);
		PointSampleList<IntType> pl2 = pl.copy();
		pl2.rotate(30);
		pl2 = getPointListWithCoMAtOrigin(pl2);
		PointSampleList<IntType> pl3 = pl2.copy();
		pl3.translate(10, 10);
		PointSampleList<IntType> pl4 = pl3.copy();
		pl4.rotate(-30);
		pl4 = getPointListWithCoMAtOrigin(pl4);
		PointSampleList<IntType> pl5 = pl4.copy();
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

	public static <T extends RealType<T>> PointSampleList<T> getPointListWithCoMAtOrigin(PointSampleList<T> pl)
	{
		pl = pl.getRealPointListRelativeToCenter();
		RealLocalizable com = getCenterOfMass(pl);
		pl.translate(-1*com.getDoublePosition(0), -1*com.getDoublePosition(1));
		System.out.println("Center of Mass now at: " + getCenterOfMass(pl));
		return pl;
	}

	public static <T extends RealType<T>> void getZernikeInfo(PointSampleList<T> pl) throws Exception
	{
		PointSampleListII<T> plII = new PointSampleListII<>(pl);
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

	public static <T extends RealType<T>> RealLocalizable getCenterOfMass(PointSampleList<T> pl)
	{
		return (RealLocalizable) IJ2PluginUtility.ij().op().run(Ops.Geometric.CenterOfGravity.class, new PointSampleListII<T>(pl));
	}

	public static PointSampleList<IntType> getRandomPoints(int maxRadius, int xOffset, int yOffset, int nPoints, int rndSeed)
	{
		PointSampleList<IntType> pl = new PointSampleList<>(IntType.class);
		Random rand = new Random(rndSeed);

		for(int i = 0; i < nPoints; i++)
		{
			double r = rand.nextDouble()*maxRadius;
			double theta = rand.nextDouble()*2*Math.PI;
			pl.add((int) (r*Math.sin(theta)), (int) (r*Math.cos(theta)), 1); 
		}

		pl.translate(xOffset, yOffset);
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

	public static void plotAndShowResults(PointSampleList<?> pl, double xCenter, double yCenter, double radius) throws Exception
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

	public static void plotAndShowResults2(PointSampleList<?> pl, List<Circle> circles) throws Exception
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
