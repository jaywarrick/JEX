package function.ops.stats;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import function.ops.JEXOps;
import function.ops.JEXOps.RadiusOfGyrationSquared;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

@Plugin(type = JEXOps.RadialLocalization.class, priority = Priority.NORMAL)
public class DefaultRadialLocalization<I extends RealType<I>> extends AbstractBinaryFunctionOp<IterableInterval<I>, IterableInterval<I>, DoubleType>
implements JEXOps.RadialLocalization {

	private UnaryFunctionOp<IterableInterval<I>, DoubleType> rogAFunc;
	
	private UnaryFunctionOp<IterableInterval<I>, DoubleType> rogBFunc;

	@Override
	public void initialize() {
		rogAFunc = Functions.unary(ops(), RadiusOfGyrationSquared.class, DoubleType.class, in1());
		rogBFunc = Functions.unary(ops(), RadiusOfGyrationSquared.class, DoubleType.class, in2());
	}
	
	@Override
	public DoubleType calculate(IterableInterval<I> inputA, IterableInterval<I> inputB)
	{
		DoubleType rogA = rogAFunc.calculate(inputA);
		DoubleType rogB = rogBFunc.calculate(inputB);
		rogA.div(rogB);
		return rogA;
	}
}