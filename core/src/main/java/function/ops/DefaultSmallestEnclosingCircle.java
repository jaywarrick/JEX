package function.ops;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Ops;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

@Plugin(type = Ops.Geometric.SmallestEnclosingCircle.class, priority = Priority.NORMAL_PRIORITY)
public class DefaultSmallestEnclosingCircle extends AbstractUnaryFunctionOp<List<? extends RealLocalizable>, Circle>
implements Ops.Geometric.SmallestEnclosingCircle {

	@Parameter(required = false)
	RealLocalizable center = null;

	@Parameter(required = false)
	boolean randomizePointRemoval = false;

	@Parameter(required = false)
	int rndSeed = 1234;

	@Override
	public Circle compute1(List<? extends RealLocalizable> input) throws IllegalArgumentException {

		List<RealLocalizable> points = new Vector<RealLocalizable>(input);
		if(randomizePointRemoval)
		{
			Collections.shuffle(points, new Random(rndSeed));
		}

		Circle D = getMinCircle(points);

		return D;
	}

	private Circle getMinCircle(List<? extends RealLocalizable> p) {

		if(p.size() == 0)
		{
			throw new IllegalArgumentException("List of points must be greater than 0.");
		}
		List<RealLocalizable> boundary = new Vector<RealLocalizable>(3);

		SS ss = new SS();
		ss.n = p.size();
		ss.b = boundary;
		
		Stack<SS> stack = new Stack<>();
		stack.push(ss.copy());
		Circle retVal = null;
		
		while(!stack.isEmpty())
		{
			SS cur = stack.pop();

			if(cur.stage == 0)
			{
				// Make a circle if you can and return
				Circle D =  makeNextCircle(cur, p);
				if(D != null)
				{
					retVal = D.copy();
					//System.out.println(cur.b);
					continue;
				}
				else
				{
					// Trim and call
					SS trimmed = cur.getTrimmed();

					// first call: D = miniDisk(ss.getTrimmed());
					cur.stage = 1;
					stack.push(cur); // Need to push cur here not trimmed.
					trimmed.stage = 0; // Don't need to copy trimmed because it is already a copy of cur.
					stack.push(trimmed);
				}
			}
			else if(cur.stage == 1)
			{
				if (!retVal.contains(cur.getTestPoint(p))) {
					cur.next(p);

					// second call: D = miniDisk(cur);
					SS next = cur.copy();
					next.stage = 0;
					stack.push(next);
				}
				// retVal = retVal;
				continue;
			}
		}
		return retVal;
	}

	public Circle makeNextCircle(SS ss, List<? extends RealLocalizable> p)
	{
		if (ss.b.size() == 3)
		{
			return makeCircle3(ss.b);
		} 
		else if (ss.n == 1 && ss.b.size() == 0)
		{
			return makeCircle1(p.get(0));
		} 
		else if (ss.n == 0 && ss.b.size() == 2)
		{
			return makeCircle2(ss.b);
		} 
		else if (ss.n == 1 && ss.b.size() == 1)
		{
			RealLocalizable p1 = p.get(0);
			RealLocalizable p2 = ss.b.get(0);
			List<RealLocalizable> pl = new Vector<>();
			pl.add(p1);
			pl.add(p2);
			return makeCircle2(pl);
		} 
		return null;
	}

	private Circle makeCircle1(RealLocalizable point)
	{
		return new Circle(point, 0);
	}

	private Circle makeCircle2(List<? extends RealLocalizable> points)
	{
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

	private Circle makeCircle3(List<? extends RealLocalizable> points)
	{
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

		// Check for vertical lines for which slopes cannot be calculated
		boolean mirrored = false;
		if((x2 == x1 || x3 == x2))
		{
			// Also check for an associated horizontal line (assuming here that no two points are on top of one another
			if(y2 == y3 || y2 == y1)
			{
				// Then lines are perpendicular. Just take average positions of 2 corners of the "L"
				x1 = Math.min(Math.min(x1, x2), x3);
				y1 = Math.min(Math.min(y1, y2), y3);
				x2 = Math.max(Math.max(x1, x2), x3);
				y2 = Math.max(Math.max(y1, y2), y3);
				return new Circle(new RealPoint((x1+x2)/2.0, (y1+y2)/2.0), calcDistance(x1,y1,x2,y2)/2.0);
			}
			else
			{
				// Then temporarily swap x for y, calculate, then swap back
				double temp = x1;
				x1 = y1;
				y1 = temp;
				temp = x2;
				x2 = y2;
				y2 = temp;
				temp = x3;
				x3 = y3;
				y3 = temp;
				mirrored = true;
			}	
		}
		double ma = (y2 - y1) / (x2 - x1);
		double mb = (y3 - y2) / (x3 - x2);

		// Calculate the center (intersection of lines perpendicular to
		// those separating the points)
		double x0 = (ma * mb * (y1 - y3) + mb * (x1 + x2) - ma * (x2 + x3)) / (2.0 * (mb - ma));
		double y0 = (-1.0 / ma) * (x0 - (x1 + x2) / 2.0) + (y1 + y2) / 2.0;
		
		if(mirrored)
		{
			// swap back
			double temp = x0;
			x0 = y0;
			y0 = temp;
			temp = x1;
			x1 = y1;
			y1 = temp;
			temp = x2;
			x2 = y2;
			y2 = temp;
			temp = x3;
			x3 = y3;
			y3 = temp;
		}

		// Calculate the radius
		double r = calcDistance(x0, y0, x1, y1);

		// Make circle
		RealPoint center = new RealPoint(x0, y0);
		Circle D = new Circle(center, r);
		
		if (!Double.isFinite(r)) {
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
		} 

		return D;
	}

	private double calcDistance(double xa, double ya, double xb, double yb)
	{
		return Math.sqrt(Math.pow(xb - xa, 2) + Math.pow(yb - ya, 2));
	}

	class SS
	{
		int n = 0;
		public List<RealLocalizable> b;
		int stage = 0;
		RealLocalizable test = null;
		Circle c = null;

		public SS getTrimmed()
		{
			SS ret = this.copy();
			ret.n = ret.n-1;
			return ret;
		}

		public RealLocalizable getTestPoint(List<? extends RealLocalizable> p)
		{
			return p.get(this.n-1);
		}

		public void next(List<? extends RealLocalizable> p)
		{
			b.add(p.get(this.n-1));
			this.n = this.n - 1;
		}

		public SS copy()
		{
			SS ret = new SS();
			ret.n = this.n;
			ret.b = new Vector<RealLocalizable>(this.b);
			ret.stage = this.stage;
			return ret;
		}

		public String toString()
		{
			return "---- SnapShot ----\n" + stage + "\npoints: " + n + "\nboundary: " + b;
		}
	}

}