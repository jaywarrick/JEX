package function.ops.stats;

import org.scijava.plugin.Plugin;

import function.ops.JEXOps;
import net.imagej.ops.Ops.ImageMoments.CentralMoment00;
import net.imagej.ops.Ops.ImageMoments.CentralMoment02;
import net.imagej.ops.Ops.ImageMoments.CentralMoment20;
import net.imagej.ops.special.chain.RTs;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.IterableInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;

@Plugin(type = JEXOps.RadiusOfGyrationSquared.class, label = "Radius of Gyration Squared")
public class  DefaultRadiusOfGyrationSquared<I extends RealType<I>>
	extends AbstractUnaryFunctionOp<IterableInterval<I>, DoubleType> implements JEXOps.RadiusOfGyrationSquared
{

	private UnaryFunctionOp<IterableInterval<I>, ? extends RealType<?>> moment00Func;

	private UnaryFunctionOp<IterableInterval<I>, ? extends RealType<?>> moment20Func;
	
	private UnaryFunctionOp<IterableInterval<I>, ? extends RealType<?>> moment02Func;
	
	@Override
	public void initialize()
	{
		moment00Func = RTs.function(ops(), CentralMoment00.class, in());
		moment20Func = RTs.function(ops(), CentralMoment20.class, in());
		moment02Func = RTs.function(ops(), CentralMoment02.class, in());
	}

	@Override
	public DoubleType calculate(IterableInterval<I> input)
	{
		final double moment00 = moment00Func.calculate(input).getRealDouble();
		final double moment20 = moment20Func.calculate(input).getRealDouble();
		final double moment02 = moment02Func.calculate(input).getRealDouble();

		final double rog2 = (moment20 + moment02)/moment00;

		return(new DoubleType(rog2));
	}
}
