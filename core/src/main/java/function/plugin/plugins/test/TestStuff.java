package function.plugin.plugins.test;

import java.util.Random;

import function.plugin.IJ2.IJ2PluginUtility;
import image.roi.PointList;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import net.imagej.ops.Ops;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.roi.geometric.PointCollection;
import rtools.R;

public class TestStuff {
	
	public static void main (String[] args) throws Exception
	{
		tryIIPointList();
	}
	
	public static void tryIIPointList() throws Exception
	{
		int maxRadius = 100;
		int xOffset = 0;
		int yOffset = 0;
		int nPoints = 1000000;
		int rndSeed = 1234;
		
		DirectoryManager.setHostDirectory("C:/Users/David Niles/Desktop");
		
		PointList pl = getRandomPoints(maxRadius, xOffset, yOffset, nPoints, rndSeed);
		
		IterableInterval<Void> ii = getIterableInterval(pl);
		
		Circle result = (Circle) IJ2PluginUtility.ij().op().run(Ops.Geometric.SmallestEnclosingCircle.class, ii);
		
		TestStuff.plotAndShowResults(pl, result.getCenter().getDoublePosition(0), result.getCenter().getDoublePosition(1), result.getRadius());
	}
	
	public static PointList getRandomPoints(int maxRadius, int xOffset, int yOffset, int nPoints, int rndSeed)
	{
		PointList pl = new PointList();
		Random rand = new Random(rndSeed);
		
		for(int i = 0; i < nPoints; i++)
		{
			double r = rand.nextDouble()*maxRadius;
			double theta = rand.nextDouble()*2*Math.PI;
			pl.add((int) (r*Math.sin(theta)), (int) (r*Math.cos(theta))); 
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
	
	public static void plotAndShowResults(PointList pl, double xCenter, double yCenter, double radius) throws Exception
	{
//		R.eval("x <- 1"); // Just to get R going.
//		R.makeVector("x", pl.getXIntArray());
//		R.makeVector("y", pl.getYIntArray());
//		R.load("plotrix");
//		String filePath = R.startPlot("pdf", 7, 5, 0, 12, "Helvetica", null);
//		R.eval("plot(x,y, xlab='X [pixels]', ylab='Y [pixels]', asp=1)");
//		R.eval("draw.circle(x=" + xCenter + ",y=" + yCenter + ",radius=" + radius + ")");
//		R.endPlot();
//		FileUtility.openFileDefaultApplication(filePath);
		System.out.print("Center: (" + xCenter + ", " + yCenter + ")    ");
		System.out.print("Radius: " + radius);
	}
}
