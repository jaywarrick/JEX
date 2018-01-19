package function.ops.geometry;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Ops;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.RealLocalizable;

@Plugin(type = Ops.Geometric.SmallestEnclosingCircle.class, priority = Priority.NORMAL)
public class DefaultSmallestEnclosingCircleOfCollection extends AbstractUnaryFunctionOp<Collection<? extends RealLocalizable>, Circle>
implements Ops.Geometric.SmallestEnclosingCircle {

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
		op = Functions.unary(ops(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, (List<? extends RealLocalizable>) null, center, randomizePointRemoval, rndSeed);
	}

	@Override
	public Circle calculate(Collection<? extends RealLocalizable> input) throws IllegalArgumentException {

		// Need and list of points instead of a collection
		List<RealLocalizable> points = getInitialPointList(input);
		
		return op.calculate(points);
	}

	private List<RealLocalizable> getInitialPointList(Collection<? extends RealLocalizable> pts)
	{
		return new Vector<RealLocalizable>(pts);
	}

}