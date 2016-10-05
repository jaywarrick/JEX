package function.ops.namespace;

import org.scijava.plugin.Plugin;

import net.imagej.ops.AbstractNamespace;
import net.imagej.ops.Namespace;
import net.imagej.ops.OpMethod;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;

@Plugin(type = Namespace.class)
public class Stats extends AbstractNamespace {

	@Override
	public String getName() {
		return "stats";
	}

	@OpMethod(
			op = function.ops.stats.DefaultPearsonsCorrelationCoefficient.class)
	public <T extends RealType<T>> DoubleType pearsonscorrelationcoefficient(final Pair<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> in1, IterableInterval<Void> in2) {
		final DoubleType result = (DoubleType) ops().run(
				function.ops.stats.DefaultPearsonsCorrelationCoefficient.class, in1, in2);
		return result;
	}

}
