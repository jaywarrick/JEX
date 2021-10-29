package function.ops;

import org.scijava.plugin.Plugin;

import net.imagej.ops.AbstractNamespace;
import net.imagej.ops.Namespace;
import net.imagej.ops.OpMethod;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

@Plugin(type = Namespace.class)
public class StatsNamespace extends AbstractNamespace {

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
	
	@OpMethod(
			op = function.ops.stats.DefaultSpearmansRankCorrelationCoefficient.class)
	public <T extends RealType<T>> DoubleType spearmansrankcorrelationcoefficient(final Pair<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>> in1, IterableInterval<Void> in2) {
		final DoubleType result = (DoubleType) ops().run(
				function.ops.stats.DefaultSpearmansRankCorrelationCoefficient.class, in1, in2);
		return result;
	}
	
	@OpMethod(
			op = function.ops.stats.DefaultRadialLocalization.class)
	public <T extends RealType<T>> DoubleType radiallocalization(final IterableInterval<T> in1, final IterableInterval<T> in2) {
		final DoubleType result = (DoubleType) ops().run(
				function.ops.stats.DefaultRadialLocalization.class, in1, in2);
		return result;
	}
	
	@OpMethod(
			op = function.ops.stats.DefaultRadiusOfGyrationSquared.class)
	public <T extends RealType<T>> DoubleType defaultradiusofgyration(final IterableInterval<T> in1) {
		final DoubleType result = (DoubleType) ops().run(
				function.ops.stats.DefaultRadiusOfGyrationSquared.class, in1);
		return result;
	}
	
	@OpMethod(
			op = function.ops.stats.DefaultMAD.class)
	public <T extends RealType<T>> ValuePair<Double, Double> defaultmad(final IterableInterval<T> in1) {
		@SuppressWarnings("unchecked")
		final ValuePair<Double,Double> result = (ValuePair<Double,Double>) ops().run(
				function.ops.stats.DefaultMAD.class, in1);
		return result;
	}
	
	@OpMethod(
			op = function.ops.stats.DefaultQuantileLimits.class)
	public <T extends RealType<T>> ValuePair<Double, Double> defaultquantilelimits(final IterableInterval<T> in1) {
		@SuppressWarnings("unchecked")
		final ValuePair<Double,Double> result = (ValuePair<Double,Double>) ops().run(
				function.ops.stats.DefaultQuantileLimits.class, in1);
		return result;
	}
	
	@OpMethod(
			op = function.ops.stats.DefaultPercentileLimits.class)
	public <T extends RealType<T>> ValuePair<Double, Double> defaultpercentilelimits(final IterableInterval<T> in1) {
		@SuppressWarnings("unchecked")
		final ValuePair<Double,Double> result = (ValuePair<Double,Double>) ops().run(
				function.ops.stats.DefaultPercentileLimits.class, in1);
		return result;
	}
	
	@OpMethod(
			op = function.ops.stats.DefaultSigmaLimits.class)
	public <T extends RealType<T>> ValuePair<Double, Double> defaultsigmalimits(final IterableInterval<T> in1) {
		@SuppressWarnings("unchecked")
		final ValuePair<Double,Double> result = (ValuePair<Double,Double>) ops().run(
				function.ops.stats.DefaultSigmaLimits.class, in1);
		return result;
	}

}
