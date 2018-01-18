package function.ops.namespace;

import org.scijava.plugin.Plugin;

import net.imagej.ops.AbstractNamespace;
import net.imagej.ops.Namespace;
import net.imagej.ops.OpMethod;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

@Plugin(type = Namespace.class)
public class Histogram extends AbstractNamespace {

	@Override
	public String getName() {
		return "histogram";
	}

	@OpMethod(
			op = function.ops.histogram.BoundedHistogramCreate.class)
	public <T extends RealType<T>> ValuePair<Double, Double> boundedhistogramcreate(final IterableInterval<T> in1, int numBins, double min, double max, String boundingStrategy) {
		@SuppressWarnings("unchecked")
		final ValuePair<Double,Double> result = (ValuePair<Double,Double>) ops().run(
				function.ops.histogram.BoundedHistogramCreate.class, in1, numBins, min, max, boundingStrategy);
		return result;
	}
	

}
