/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2017 Board of Regents of the University of
 * Wisconsin-Madison, University of Konstanz and Brian Northan.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package function.ops.stats;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import function.ops.JEXOps;
import net.imagej.ops.Op;
import net.imagej.ops.Ops;
import net.imagej.ops.special.computer.Computers;
import net.imagej.ops.special.computer.UnaryComputerOp;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ValuePair;

/**
 * {@link Op} to calculate the n-th {@code stats.percentile}.
 * 
 * @author Daniel Seebacher (University of Konstanz)
 * @author Christian Dietz (University of Konstanz)
 * @author Jan Eglinger
 * @param <I>
 *            input type
 * @param <O>
 *            output type
 */
@Plugin(type = JEXOps.Limits.class, label = "Statistics: Percentile Limits")
public class DefaultSigmaLimits<I extends RealType<I>> extends
AbstractUnaryFunctionOp<Iterable<I>, ValuePair<Double,Double>> implements JEXOps.Limits
{

	@Parameter
	private double lowerSigma;

	@Parameter
	private double upperSigma;
	
	@Parameter(required=false)
	private boolean utilizeNonParametric = true;

	private UnaryComputerOp<Iterable<I>, DoubleType> meanOp;
	private UnaryComputerOp<Iterable<I>, DoubleType> stdDevOp;
	private UnaryFunctionOp<Iterable<I>, ValuePair<Double,Double>> madOp;
	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void initialize() {
		
		if(utilizeNonParametric)
		{
			madOp = (UnaryFunctionOp) Functions.unary(ops(), JEXOps.MAD.class, ValuePair.class,
					in() == null ? Iterable.class : in());
		}
		else
		{
			meanOp = (UnaryComputerOp) Computers.unary(ops(), Ops.Stats.Mean.class, DoubleType.class,
					in() == null ? Iterable.class : in());
			stdDevOp = (UnaryComputerOp) Computers.unary(ops(), Ops.Stats.StdDev.class, DoubleType.class,
					in() == null ? Iterable.class : in());
		}
	}

	@Override
	public ValuePair<Double,Double> calculate(final Iterable<I> input) {
		if(this.utilizeNonParametric)
		{
			ValuePair<Double,Double> ret = madOp.calculate(input);
			return new ValuePair<Double,Double>(ret.getA() + lowerSigma * ret.getB(), ret.getA() + upperSigma * ret.getB());
		}
		else
		{
			DoubleType mean = new DoubleType(0.0d);
			meanOp.compute(input, mean);
			DoubleType stdDev = new DoubleType(0.0d);
			stdDevOp.compute(input, stdDev);
			return new ValuePair<Double,Double>(mean.getRealDouble() + lowerSigma * stdDev.getRealDouble(), mean.getRealDouble() + upperSigma * stdDev.getRealDouble());
		}
		
	}
}
