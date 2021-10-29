package function.ops.geometry;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import function.ops.JEXOps;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.RealCursor;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;

@Plugin(type = JEXOps.SmallestEnclosingCircle.class, priority = Priority.NORMAL+4)
public class DefaultSmallestEnclosingCircleOfRealCursor extends AbstractUnaryFunctionOp<RealCursor<?>, Circle>
implements JEXOps.SmallestEnclosingCircle {

	@Parameter(required = false)
	RealLocalizable center = null;

	@Parameter(required = false)
	boolean randomizePointRemoval = false;

	@Parameter(required = false)
	int rndSeed = 1234;
	
	UnaryFunctionOp<List<? extends RealLocalizable>, Circle> op;
	
	@Override
	public void initialize()
	{
		op = Functions.unary(ops(), function.ops.geometry.DefaultSmallestEnclosingCircle.class, Circle.class, new Vector<RealLocalizable>(), center, randomizePointRemoval, rndSeed);
	}

	@Override
	public Circle calculate(RealCursor<?> input) throws IllegalArgumentException {

		List<RealLocalizable> points = getInitialPointList(input);
		if(randomizePointRemoval)
		{
			Collections.shuffle(points, new Random(rndSeed));
		}

		Circle D = op.calculate(points);

		return D;
	}

	private List<RealLocalizable> getInitialPointList(RealCursor<?> cursor)
	{
		List<RealLocalizable> points;
		points = new Vector<RealLocalizable>();
		while (cursor.hasNext()) {
			cursor.fwd();
			// Add this location to the list
			points.add(new RealPoint(cursor));
			if (center != null) {
				// Add a mirroring point
				double[] pos = new double[cursor.numDimensions()];
				for (int d = 0; d < cursor.numDimensions(); d++) {
					pos[d] = 2 * center.getDoublePosition(d) - cursor.getDoublePosition(d);
				}
				points.add(new RealPoint(pos));
			}
		}
		return points;
	}
}