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

import java.util.Vector;

import org.scijava.plugin.Plugin;

import function.ops.JEXOps;
import net.imagej.ops.Op;
import net.imagej.ops.Ops;
import net.imagej.ops.special.chain.RTs;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.ValuePair;

/**
 * {@link Op} to calculate the {@code stats.stdDev} using
 * {@code stats.variance}.
 * 
 * @author Daniel Seebacher (University of Konstanz)
 * @author Christian Dietz (University of Konstanz)
 * @param <I> input type
 * @param <O> output type
 */
@Plugin(type = JEXOps.MAD.class, label = "Statistics: MAD")
public class DefaultMAD<I extends RealType<I>>
	extends AbstractUnaryFunctionOp<Iterable<I>, ValuePair<Double, Double>> implements JEXOps.MAD
{
	
	private UnaryFunctionOp<Iterable<I>, DoubleType> median1;
	private UnaryFunctionOp<Iterable<DoubleType>, DoubleType> median2;

	@Override
	public void initialize() {
		median1 = RTs.function(ops(), Ops.Stats.Median.class, in());
		median2 = RTs.function(ops(), Ops.Stats.Median.class, new Vector<DoubleType>());
	}

	@Override
	public ValuePair<Double,Double> calculate(final Iterable<I> input) {
		
		DoubleType med = this.median1.calculate(input);
		Double mad = this.mad(input, med.getRealDouble());
		return new ValuePair<Double,Double>(med.getRealDouble(), mad);
	}
	
	private Double mad(Iterable<I> values, double med)
	{
		Vector<DoubleType> diffs = new Vector<DoubleType>();
		for (I d : values)
		{
			diffs.add(new DoubleType(Math.abs(d.getRealDouble() - med)));
		}
		return 1.4826 * median2.calculate(diffs).getRealDouble();
	}

}
