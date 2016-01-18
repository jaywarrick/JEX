package function.plugin.plugins.test;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Ops;
import net.imagej.ops.special.AbstractUnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.RealPoint;

@Plugin(type = Ops.Geometric.SmallestEnclosingCircle.class)
public class DefaultSmallestEnclosingCircle extends AbstractUnaryFunctionOp<IterableInterval<?>, Circle>
		implements Ops.Geometric.SmallestEnclosingCircle {

	@Parameter(required = false)
	Localizable center = null;

	@Parameter(required = false)
	double paddingRatio = 1.0;
	
	@Parameter(required = false)
	boolean randomizePointRemoval = true;
	
	@Parameter(required = false)
	int rndSeed = 1234;

	@Override
	public Circle compute1(IterableInterval<?> input) throws IllegalArgumentException {
		if (input.size() > Integer.MAX_VALUE || (center != null && input.size() > Integer.MAX_VALUE / 2)) {
			throw new IllegalArgumentException();
		}

		List<Localizable> points = getInitialPointList(input);
		List<Localizable> boundary = new Vector<Localizable>(3);
		
		if(randomizePointRemoval)
		{
			Collections.shuffle(points, new Random(rndSeed));
		}

		Circle D = miniDisk(points, boundary);
		
		if(paddingRatio != 1.0)
		{
			D = new Circle(D.getCenter(), D.getRadius()*paddingRatio);
		}

		return D;
	}

	private List<Localizable> getInitialPointList(IterableInterval<?> input) {
		List<Localizable> points;
		if (center == null)
			points = new Vector<Localizable>((int) input.size());
		else
			points = new Vector<Localizable>(2 * (int) input.size());

		Cursor<?> c = input.cursor();
		while (c.hasNext()) {
			c.fwd();
			Point p = new Point(c);
			points.add(p);
			if (center != null) {
				// Add a mirroring point
				long[] pos = new long[c.numDimensions()];
				for (int d = 0; d < c.numDimensions(); d++) {
					pos[d] = 2 * center.getLongPosition(d) - p.getLongPosition(d);
				}
				points.add(new Point(pos));
			}
		}
		return points;
	}

	private Circle miniDisk(List<Localizable> points, List<Localizable> boundary) {

		Circle D;

		// Special cases
		if (boundary.size() == 3) {
			D = makeCircle3(boundary);
		} else if (points.size() == 1 && boundary.size() == 0) {
			D = makeCircle1(points);
		} else if (points.size() == 0 && boundary.size() == 2) {
			D = makeCircle2(boundary);
		} else if (points.size() == 1 && boundary.size() == 1) {
			Localizable p1 = points.get(0);
			Localizable p2 = boundary.get(0);
			List<Localizable> pl = new Vector<>();
			pl.add(p1);
			pl.add(p2);
			D = makeCircle2(pl); // pointList and boundary
		} else {
			// Recursively check points
			List<Localizable> trimmed = getTrimmedList(points);
			D = miniDisk(trimmed, new Vector<Localizable>(boundary));
			Localizable testPoint = points.get(trimmed.size());
			if (!D.contains(testPoint)) {
				boundary.add(testPoint);
				D = miniDisk(trimmed, new Vector<Localizable>(boundary));
			}
		}

		return D;
	}

	private List<Localizable> getTrimmedList(List<Localizable> list) {
		return list.subList(0, list.size()-1);
	}

	private Circle makeCircle1(List<Localizable> points) {
		return new Circle(points.get(0), 0);
	}

	private Circle makeCircle2(List<Localizable> points) {
		double[] pos1 = new double[points.get(0).numDimensions()];
		double[] pos2 = new double[points.get(0).numDimensions()];

		points.get(0).localize(pos1);
		points.get(1).localize(pos2);

		double x0 = (pos1[0] + pos2[0]) / 2.0;
		double y0 = (pos1[1] + pos2[1]) / 2.0;
		double r = calcDistance(pos1[0], pos1[1], pos2[0], pos2[1]) / 2.0;

		RealPoint center = new RealPoint(x0, y0);

		return new Circle(center, r);

	}

	private Circle makeCircle3(List<Localizable> points) {

		Circle D;

		double[] pos1 = new double[points.get(0).numDimensions()];
		double[] pos2 = new double[points.get(0).numDimensions()];
		double[] pos3 = new double[points.get(0).numDimensions()];

		points.get(0).localize(pos1);
		points.get(1).localize(pos2);
		points.get(2).localize(pos3);

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
				points.remove(2);
			else if (biggest == d13)
				points.remove(1);
			else if (biggest == d23)
				points.remove(0);

			D = makeCircle2(points);
		} else {
			// Calculate the center (intersection of lines perpendicular to
			// those separating the points)
			double x0 = (ma * mb * (y1 - y3) + mb * (x1 + x2) - ma * (x2 + x3)) / (2.0 * (mb - ma));
			double y0 = (-1.0 / ma) * (x0 - (x1 + x2) / 2.0) + (y1 + y2) / 2.0;

			// Calculate the radius
			double r = calcDistance(x0, y0, x1, y1);

			// Make circle
			RealPoint center = new RealPoint(x0, y0);
			D = new Circle(center, r);
		}

		return D;
	}

	private double calcDistance(double xa, double ya, double xb, double yb) {
		return Math.sqrt(Math.pow(xb - xa, 2) + Math.pow(yb - ya, 2));
	}

}