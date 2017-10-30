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
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.type.numeric.RealType;
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
public class DefaultPercentileLimits<I extends RealType<I>> extends
AbstractUnaryFunctionOp<Iterable<I>, ValuePair<Double,Double>> implements JEXOps.Limits
{

	@Parameter(min = "0", max = "100")
	private double lowerPercentile;

	@Parameter(min = "0", max = "100")
	private double upperPercentile;

	private UnaryFunctionOp<Iterable<I>, ValuePair<Double,Double>> limitsOp;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void initialize() {
		limitsOp = (UnaryFunctionOp) Functions.unary(ops(), DefaultQuantileLimits.class, ValuePair.class,
				in() == null ? Iterable.class : in(), lowerPercentile/100.0, upperPercentile/100.0);
	}

	@Override
	public ValuePair<Double,Double> calculate(final Iterable<I> input) {
		return(limitsOp.calculate(input));
	}
}
