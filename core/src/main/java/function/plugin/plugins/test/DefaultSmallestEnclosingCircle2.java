package function.plugin.plugins.test;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import image.roi.PointSampleList;
import miscellaneous.FileUtility;
import net.imagej.ops.Ops;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.special.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import rtools.R;

@Plugin(type = Ops.Geometric.SmallestEnclosingCircle.class, priority=Priority.LAST_PRIORITY)
public class DefaultSmallestEnclosingCircle2 extends AbstractUnaryFunctionOp<IterableInterval<?>, Circle>
implements Ops.Geometric.SmallestEnclosingCircle {

	final static String BOUNCINGBUBBLE = "Bouncing Bubble", WELZL_RECURSIVE = "Welzl recursive", WELZL_LOOP = "Welzl loop";

	@Parameter(required = false)
	RealLocalizable center = null;

	@Parameter(required = false)
	double paddingRatio = 1.0;

	@Parameter(required = false)
	boolean randomizePointRemoval = true;

	@Parameter(required = false)
	int rndSeed = 1234;

	@Parameter(required = false)
	String algorithm = WELZL_LOOP;

	@Override
	public Circle compute1(IterableInterval<?> input) throws IllegalArgumentException {
		if (input.size() > Integer.MAX_VALUE || (center != null && input.size() > Integer.MAX_VALUE / 2)) {
			throw new IllegalArgumentException();
		}

		List<RealLocalizable> points = getInitialPointList(input);
		List<RealLocalizable> boundary = new Vector<RealLocalizable>(3);

		if (randomizePointRemoval) {
			Collections.shuffle(points, new Random(rndSeed));
		}

		// Make a stack and initialize

		SnapShot currentSnapShot = new SnapShot();
		currentSnapShot.points = points;
		currentSnapShot.boundary = boundary;
		currentSnapShot.D = null;
		currentSnapShot.stage = 0;

		Circle ret = miniDiskLoop(currentSnapShot);

		if (paddingRatio != 1.0) {
			ret = new Circle(ret.getCenter(), ret.getRadius() * paddingRatio);
		}

		return currentSnapShot.D;
	}

	private List<RealLocalizable> getInitialPointList(IterableInterval<?> input) {
		List<RealLocalizable> points;
		if (center == null)
			points = new Vector<RealLocalizable>((int) input.size());
		else
			points = new Vector<RealLocalizable>(2 * (int) input.size());

		Cursor<?> c = input.cursor();
		while (c.hasNext()) {
			c.fwd();
			Point p = new Point(c);
			points.add(p);
			if (center != null) {
				// Add a mirroring point
				double[] pos = new double[c.numDimensions()];
				for (int d = 0; d < c.numDimensions(); d++) {
					pos[d] = 2 * center.getDoublePosition(d) - p.getDoublePosition(d);
				}
				points.add(new RealPoint(pos));
			}
		}
		return points;
	}


	private Circle miniDiskLoop(SnapShot snapShot) {

		Stack<SnapShot> stack = new Stack<>();
		stack.push(snapShot);

		Circle ret = null;
		// Perform stack loop style recursion
		while (!stack.isEmpty())
		{
			SnapShot currentSnapShot = stack.pop();

			if(currentSnapShot.stage == 0)
			{
				if(currentSnapShot.points.size() == 0)
				{
					System.out.println(currentSnapShot);
					ret = currentSnapShot.D;
				}
				else
				{
					RealLocalizable testPoint = currentSnapShot.points.get(currentSnapShot.points.size()-1);
					currentSnapShot.points = currentSnapShot.points.subList(0, currentSnapShot.points.size() - 1);
					currentSnapShot.stage = 1;
					stack.push(currentSnapShot);
					
					if(currentSnapShot.points.size() > 0 && (currentSnapShot.D == null || !currentSnapShot.D.contains(testPoint)))
					{
						SnapShot newSnapShot = currentSnapShot.copy();
						newSnapShot.boundary.add(newSnapShot.points.remove(newSnapShot.points.size()-1));
						newSnapShot.stage = 0;
						stack.push(newSnapShot);
					}
				}
			}
			else
			{
				this.updateCircle(currentSnapShot);
				System.out.println(currentSnapShot);
				ret = currentSnapShot.D;
			}
		}
		
		return ret;
	}

	private void updateCircle(SnapShot snapShot)
	{
		
		if(snapShot.boundary.size() == 0)
		{
			snapShot.D = new Circle(snapShot.points.get(snapShot.points.size()-1), 0);
			snapShot.boundary.add(snapShot.points.remove(snapShot.points.size()-1));
		}
		else if(snapShot.boundary.size() == 1)
		{
			makeCircle1(snapShot);
		}
		else if(snapShot.boundary.size() == 2)
		{
			makeCircle2(snapShot);
		}
		else if(snapShot.boundary.size() == 3)
		{
			makeCircle3(snapShot);
		}
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
	}
	//	private Circle miniDisk2(List<RealLocalizable> points) {
	//		
	//		Circle left = null;
	//		Circle right = null;
	//		
	//		// Initialize with two points
	//		Point p = (Point) points.get(0);
	//		Point q = (Point) points.get(1);		
	//		Circle D = makeCircle2(p, q);
	//		
	//
	//		// For each additional point
	//		for (int i = 2; i < points.size(); i++) {
	//			Point s = (Point) points.get(i);
	//			
	//			if (!D.contains(s)) {
	//				// Form a circumcircle
	//				// TODO
	//				Circle test = makeCircumcircle(p, q, s);
	//				if (test == null) {
	//					continue;				
	//				} else if (cross > 0 && (left == null || pq.cross(c
	//				
	//				// 
	//			}
	//		}
	//	}


	//private Circle bouncingBubble(List<RealLocalizable> points) {
	//	
	//	int L = points.size();
	//	
	//	// Initialize with 2 points
	//	Circle D = makeCircle2(points.subList(0, 1));
	//	Point center = (Point) D.center;
	//	double r = D.radius;
	//	
	//	double xc, yc, xp, yp;
	//	
	//	for (int i = 0; i < 3; i++) {
	//		for (int j = 0; j < L; j++) {
	//			Point p = (Point) points.get(j);
	//						
	//			double[] pos1 = new double[p.numDimensions()];
	//			double[] pos2 = new double[p.numDimensions()];
	//
	//			center.localize(pos1);
	//			p.localize(pos2);
	//			
	//			xc = pos1(0);
	//			yc = pos1(1);
	//			
	//			xp = pos2(0);
	//			yp = pos2(1);
	//			
	//			double s = calcDistance(pos1[0], pos1[1], pos2[0], pos2[1]);
	//			
	//			if (s > r) {
	//				if (i < 2) {					
	//					r = (Math.pow(s, 2) + Math.pow(r, 2)/(2.0*s);
	//					double A = (Math.pow(s, 2) + Math.pow(r, 2))/(2.0*Math.pow(s, 2));
	//					double B = (Math.pow(s, 2) - Math.pow(r, 2))/(2.0*Math.pow(s, 2));
	//					xc = A*xc + B*xp;
	//					yc = A*yc + B*yp;
	//				} else {
	//					r = (r + s)/2.0;
	//					xc = xc + (s - r)*(xp - xc)/s;
	//					yc = yc + (s - r)*(yp - yc)/s;
	//				}
	//			}
	//		}
	//	}
	//	
	//	D.center = Point(xc, yc);
	//	D.radius = r;
	//	
	//	return D;
	//}
	//
	//
	//	private Circle welzl(List<RealLocalizable> points, List<RealLocalizable> boundary) {
	//
	//		Circle D;
	//
	//		// Special cases
	//		if (boundary.size() == 3) {
	//			D = makeCircle3(boundary);
	//		} else if (points.size() == 1 && boundary.size() == 0) {
	//			D = makeCircle1(points);
	//		} else if (points.size() == 0 && boundary.size() == 2) {
	//			D = makeCircle2(boundary);
	//		} else if (points.size() == 1 && boundary.size() == 1) {
	//			RealLocalizable p1 = points.get(0);
	//			RealLocalizable p2 = boundary.get(0);
	//			List<RealLocalizable> pl = new Vector<>();
	//			pl.add(p1);
	//			pl.add(p2);
	//			D = makeCircle2(pl); // pointList and boundary
	//		} else {
	//			// Recursively check points
	//			List<RealLocalizable> trimmed = getTrimmedList(points);
	//			D = miniDisk(trimmed, new Vector<RealLocalizable>(boundary));
	//			RealLocalizable testPoint = points.get(trimmed.size());
	//			if (!D.contains(testPoint)) {
	//				boundary.add(testPoint);
	//				D = miniDisk(trimmed, new Vector<RealLocalizable>(boundary));
	//			}
	//		}
	//
	//		return D;
	//	}



	//private SnapShot makeSnapshot1(SnapShot state) {
	//	List<RealLocalizable> points = state.points;
	//	List<RealLocalizable> boundary = state.boundary;
	//	Circle D = makeCircle1(points);
	//	
	//}

	private void makeCircle1(SnapShot snapShot) {

		snapShot.D = new Circle(new RealPoint(snapShot.boundary.get(0)), 0);

	}

	private void makeCircle2(SnapShot snapShot) {

		RealLocalizable p = snapShot.boundary.get(0);
		RealLocalizable q = snapShot.boundary.get(1);

		double[] pos1 = new double[p.numDimensions()];
		double[] pos2 = new double[p.numDimensions()];

		p.localize(pos1);
		q.localize(pos2);

		double x0 = (pos1[0] + pos2[0]) / 2.0;
		double y0 = (pos1[1] + pos2[1]) / 2.0;
		double r = calcDistance(pos1[0], pos1[1], pos2[0], pos2[1]) / 2.0;

		RealPoint center = new RealPoint(x0, y0);

		snapShot.D = new Circle(center, r);
	}

	//private Circle makeCircle2(List<RealLocalizable> points) {
	//	double[] pos1 = new double[points.get(0).numDimensions()];
	//	double[] pos2 = new double[points.get(0).numDimensions()];
	//
	//	points.get(0).localize(pos1);
	//	points.get(1).localize(pos2);
	//
	//	double x0 = (pos1[0] + pos2[0]) / 2.0;
	//	double y0 = (pos1[1] + pos2[1]) / 2.0;
	//	double r = calcDistance(pos1[0], pos1[1], pos2[0], pos2[1]) / 2.0;
	//
	//	RealPoint center = new RealPoint(x0, y0);
	//
	//	return new Circle(center, r);
	//
	//}

	private void makeCircle4(SnapShot snapShot, int keeper)
	{
		// We must eliminate 1 point. 
		SnapShot temp = snapShot.copy();
		for(int i = 0; i < 4; i++)
		{
			int i1 = i%4;
			int i2 = (i+1)%4;
			int i3 = (i+2)%4;
			int test = (i+3)%4;



		}
	}

	private void makeCircle3(SnapShot snapShot)
	{

		double[] pos1 = new double[snapShot.boundary.get(0).numDimensions()];
		double[] pos2 = new double[snapShot.boundary.get(0).numDimensions()];
		double[] pos3 = new double[snapShot.boundary.get(0).numDimensions()];

		snapShot.boundary.get(0).localize(pos1);
		snapShot.boundary.get(1).localize(pos2);
		snapShot.boundary.get(2).localize(pos3);

		double x1 = pos1[0];
		double x2 = pos2[0];
		double x3 = pos3[0];

		double y1 = pos1[1];
		double y2 = pos2[1];
		double y3 = pos3[1];

		double ma = (y2 - y1) / (x2 - x1);
		double mb = (y3 - y2) / (x3 - x2);

		// If the 3 points lie on a line, use only the 2 outermost points
		if (ma == mb) {
			double d12 = calcDistance(x1, y1, x2, y2);
			double d13 = calcDistance(x1, y1, x3, y3);
			double d23 = calcDistance(x2, y2, x3, y3);

			double biggest = Math.max(Math.max(d12, d13), d23);

			if (biggest == d12)
				snapShot.boundary.remove(2);
			else if (biggest == d13)
				snapShot.boundary.remove(1);
			else if (biggest == d23)
				snapShot.boundary.remove(0);

			makeCircle2(snapShot);
		} else {
			// Calculate the center (intersection of lines perpendicular to
			// those separating the points)
			double x0 = (ma * mb * (y1 - y3) + mb * (x1 + x2) - ma * (x2 + x3)) / (2.0 * (mb - ma));
			double y0 = (-1.0 / ma) * (x0 - (x1 + x2) / 2.0) + (y1 + y2) / 2.0;

			// Calculate the radius
			double r = calcDistance(x0, y0, x1, y1);

			// Make circle
			RealPoint center = new RealPoint(x0, y0);
			snapShot.D = new Circle(center, r);
		}
	}

	private double calcDistance(double xa, double ya, double xb, double yb) {
		return Math.sqrt(Math.pow(xb - xa, 2) + Math.pow(yb - ya, 2));
	}

}

//Define snapshot to hold current state
class SnapShot {

	List<RealLocalizable> points;
	List<RealLocalizable> boundary;
	Circle D;
	int stage;

	public SnapShot(SnapShot another) {
		this.points = another.points;
		this.boundary = another.boundary;
		this.D = another.D;
		this.stage = another.stage;
	}

	public SnapShot() {
		this.points = null;
		this.boundary = null;
		this.D = null;
		this.stage = 0;
	}

	public SnapShot copy()
	{
		SnapShot ret = new SnapShot();
		ret.points = new Vector<RealLocalizable>(this.points);
		ret.boundary = new Vector<RealLocalizable>(this.boundary);
		if(ret.D != null)
		{
			ret.D = this.D.copy();
		}
		ret.stage = this.stage;
		System.out.println(this);
		return ret;
	}

	public SnapShot getTrimmedSnapShot() {

		SnapShot ret = this.copy();
		ret.boundary.add(ret.points.get(ret.points.size()-1));
		ret.points = ret.points.subList(0, ret.points.size() - 1);

		return ret;

	}

	public String toString()
	{
		if(this.D == null)
		{
			return "null";
		}
		return this.D.toString();
	}
}
