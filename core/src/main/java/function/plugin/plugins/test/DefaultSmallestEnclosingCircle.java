package function.plugin.plugins.test;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;
import net.imagej.ops.Ops;
import net.imagej.ops.special.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.Point;
import net.imglib2.RealPoint;

@Plugin(type = Ops.Geometric.SmallestEnclosingCircle.class)
public class DefaultSmallestEnclosingCircle extends AbstractUnaryFunctionOp<IterableInterval<?>, Circle>
implements Ops.Geometric.SmallestEnclosingCircle {

	@Parameter(required = false)
	RealLocalizable center = null;

	@Parameter(required = false)
	double paddingRatio = 1.0;

	@Parameter(required = false)
	boolean randomizePointRemoval = true;

	@Parameter(required = false)
	int rndSeed = 1234;

	@ParameterMarker(name = "Algorithm", description = "Specifies algorithm used to find circle.", ui = MarkerConstants.UI_DROPDOWN, choices = {
			"Bouncing Bubble", "Welzl", "Welzl loop" }, defaultChoice = 0)
	String algorithm;

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

		SnapShotStruct currentSnapShot = new SnapShotStruct();
		currentSnapShot.points = points;
		currentSnapShot.boundary = boundary;
		currentSnapShot.D = null;
		currentSnapShot.stage = 0;

		SnapShotStruct result = miniDiskLoop(currentSnapShot);

		if (paddingRatio != 1.0) {
			result.D = new Circle(result.D.getCenter(), result.D.getRadius() * paddingRatio);
		}

		return result.D;
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


	private SnapShotStruct miniDiskLoop(SnapShotStruct currentSnapShot) {

		// Initialize the output
		SnapShotStruct returnVal = null;

		Stack<SnapShotStruct> snapshotStack = new Stack<SnapShotStruct>();
		snapshotStack.push(currentSnapShot);

		int counter = 0;

		while (!snapshotStack.empty()) {
			counter = counter + 1;
			// Get the top of the stack
			currentSnapShot = snapshotStack.pop();

			// Special cases
			if (currentSnapShot.boundary.size() == 3) {
				returnVal = makeCircle3(currentSnapShot);
				snapshotStack.push(returnVal);
				continue;
			} else if (currentSnapShot.points.size() >= 1 && currentSnapShot.boundary.size() == 0) {
				returnVal = makeCircle1(currentSnapShot);
				snapshotStack.push(returnVal);
				continue;
				//			SnapShotStruct newSnapshot = new SnapShotStruct();
				//			newSnapshot.boundary = points;
				//			newSnapshot.
			} else if (currentSnapShot.points.size() == 0 && currentSnapShot.boundary.size() == 2) {
				returnVal = makeCircle2(currentSnapShot);
				snapshotStack.push(returnVal);
				continue;
			} else if (currentSnapShot.points.size() == 1 && currentSnapShot.boundary.size() == 1) {
				RealLocalizable p1 = currentSnapShot.points.get(0);
				RealLocalizable p2 = currentSnapShot.boundary.get(0);
				List<RealLocalizable> pl = new Vector<>();
				pl.add(p1);
				pl.add(p2);
				returnVal = makeCircle2(currentSnapShot); // pointList and boundary
				snapshotStack.push(returnVal);
				continue;
			} else {
				if (currentSnapShot.D == null) {
					returnVal = makeCircle1(currentSnapShot);
					snapshotStack.push(returnVal);
					continue;
				}

				// Make a new snapshot

				// Remove a test point
				RealLocalizable testPoint = currentSnapShot.points.get(currentSnapShot.points.size()-1);
				returnVal = currentSnapShot.getTrimmedSnapShot();
				

				if (!currentSnapShot.D.contains(testPoint)) {

					// Return the new snapshot
					snapshotStack.push(returnVal);
					continue;

					// Otherwise simply return the circle
				} else {
					// Do nothing which results in shrinking
					returnVal = currentSnapShot;
					continue;
				}
			}
		}
		return returnVal;
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


	private SnapShotStruct makeCircle1(SnapShotStruct snapShot) {

		Circle D = new Circle(snapShot.points.get(snapShot.points.size()-1), 0);
		SnapShotStruct ret = snapShot.getTrimmedSnapShot();
		ret.D = D;
		return ret;

	}

	private SnapShotStruct makeCircle2(SnapShotStruct snapShot) {

		SnapShotStruct ret = snapShot.copy();

		RealLocalizable p = ret.points.get(0);
		RealLocalizable q = ret.points.get(1);

		double[] pos1 = new double[p.numDimensions()];
		double[] pos2 = new double[p.numDimensions()];

		p.localize(pos1);
		q.localize(pos2);

		double x0 = (pos1[0] + pos2[0]) / 2.0;
		double y0 = (pos1[1] + pos2[1]) / 2.0;
		double r = calcDistance(pos1[0], pos1[1], pos2[0], pos2[1]) / 2.0;

		RealPoint center = new RealPoint(x0, y0);

		ret.D = new Circle(center, r);

		return ret;
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

	private SnapShotStruct makeCircle3(SnapShotStruct snapShot) {

		SnapShotStruct ret = snapShot.copy();

		double[] pos1 = new double[ret.points.get(0).numDimensions()];
		double[] pos2 = new double[ret.points.get(0).numDimensions()];
		double[] pos3 = new double[ret.points.get(0).numDimensions()];

		ret.points.get(0).localize(pos1);
		ret.points.get(1).localize(pos2);
		ret.points.get(2).localize(pos3);

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
				ret.points.remove(2);
			else if (biggest == d13)
				ret.points.remove(1);
			else if (biggest == d23)
				ret.points.remove(0);

			return makeCircle2(ret);
		} else {
			// Calculate the center (intersection of lines perpendicular to
			// those separating the points)
			double x0 = (ma * mb * (y1 - y3) + mb * (x1 + x2) - ma * (x2 + x3)) / (2.0 * (mb - ma));
			double y0 = (-1.0 / ma) * (x0 - (x1 + x2) / 2.0) + (y1 + y2) / 2.0;

			// Calculate the radius
			double r = calcDistance(x0, y0, x1, y1);

			// Make circle
			RealPoint center = new RealPoint(x0, y0);
			ret.D = new Circle(center, r);
			return ret;
		}
	}

	private double calcDistance(double xa, double ya, double xb, double yb) {
		return Math.sqrt(Math.pow(xb - xa, 2) + Math.pow(yb - ya, 2));
	}

}

//Define snapshot to hold current state
class SnapShotStruct {

	List<RealLocalizable> points;
	List<RealLocalizable> boundary;
	Circle D;
	int stage;

	public SnapShotStruct(SnapShotStruct another) {
		this.points = another.points;
		this.boundary = another.boundary;
		this.D = another.D;
		this.stage = another.stage;
	}

	public SnapShotStruct() {
		this.points = null;
		this.boundary = null;
		this.D = null;
		this.stage = 0;
	}

	public SnapShotStruct copy()
	{
		SnapShotStruct ret = new SnapShotStruct();
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
	
	public SnapShotStruct getTrimmedSnapShot() {

		SnapShotStruct ret = this.copy();
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
