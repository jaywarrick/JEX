package function.plugin.plugins.test;

import org.scijava.plugin.Plugin;

import net.imagej.ops.Ops;
import net.imagej.ops.special.AbstractUnaryFunctionOp;
import net.imglib2.IterableInterval;
import net.imglib2.Point;

@Plugin(type=Ops.Geometric.SmallestEnclosingCircle.class)
	public class DefaultSmallestEnclosingCircle<EI> extends AbstractUnaryFunctionOp<IterableInterval<EI>, Circle>
		implements Ops.Geometric.SmallestEnclosingCircle
	{
		@Override
		public Circle compute1(IterableInterval<EI> input)
		{
			return new Circle(new Point(0,0), 10.0);
		}
	}