package function.ops.stats;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import algorithms.SpearmanRankCorrelation;
import function.ops.JEXOps;
import function.plugin.plugins.featureExtraction.FeatureUtils;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.TwinCursor;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;

@Plugin(type = JEXOps.SpearmansRankCorrelationCoefficient.class, priority = Priority.NORMAL_PRIORITY)
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
		double r = SpearmanRankCorrelation.calculateSpearmanRank(cursor);
		return new DoubleType(r);
	}
}