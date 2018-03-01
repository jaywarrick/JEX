package function.ops.stats;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import function.ops.JEXOps;
import function.plugin.plugins.featureExtraction.FeatureUtils;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.PairIterator;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
import sc.fiji.coloc.algorithms.SpearmanRankCorrelation;

@Plugin(type = JEXOps.SpearmansRankCorrelationCoefficient.class, priority = Priority.NORMAL)
public class DefaultSpearmansRankCorrelationCoefficient<I1 extends RealType<I1>> extends AbstractBinaryFunctionOp<Pair<RandomAccessibleInterval<I1>, RandomAccessibleInterval<I1>>, Cursor<Void>, DoubleType>
implements JEXOps.SpearmansRankCorrelationCoefficient {

	@Override
	public DoubleType calculate(Pair<RandomAccessibleInterval<I1>, RandomAccessibleInterval<I1>> input1,
			Cursor<Void> input2)
	{
		FeatureUtils utils = new FeatureUtils();
		Cursor<BitType> c = utils.convertVoidTypeToBitTypeCursor(input2);
		TwinCursor<I1> cursor = new TwinCursor<>(
				input1.getA().randomAccess(),
				input1.getB().randomAccess(),
				c);
		double r = DefaultSpearmansRankCorrelationCoefficient.calculateSpearmanRank(cursor);
		return new DoubleType(r);
	}

	// The Static method of the help class is not thread-safe so it causes errors when running multiple threads.
	// This will funnel calls to the helper class through a synchronized method call.
	public static <T extends RealType<T>, C extends Cursor<T> & PairIterator<T>> double calculateSpearmanRank(C c)
	{
		SpearmanCalculator<T> spc = new SpearmanCalculator<>();
		double r = spc.calculateSpearmanRank(c);
		return r;
	}

	public static <T extends RealType<T>, C extends Cursor<T> & PairIterator<T>> double[][] getPairedData(C c)
	{
		// Step 0: Count the pixels first.
		int n = 0;
		while (c.hasNext()) {
			n++;
			c.fwd();
		}
		c.reset();

		double[][] data = new double[n][2];

		for (int i = 0; i < n; i++) {
			c.fwd();
			data[i][0] = c.getFirst().getRealDouble();
			data[i][1] = c.getSecond().getRealDouble();
		}

		return data;
	}

	// The Static method of the help class is not thread-safe so it causes errors when running multiple threads.
	// This will funnel calls to the helper class through a synchronized method call.
	public static synchronized <T extends RealType<T>, C extends Cursor<T> & PairIterator<T>> double calculateSpearmanRankSynchronized(C c)
	{
		double[][] data = getPairedData(c);
		SpearmanRankCorrelation<DoubleType> sc = new SpearmanRankCorrelation<>();
		double r = sc.calculateSpearmanRank(data);
		return r;
	}

}