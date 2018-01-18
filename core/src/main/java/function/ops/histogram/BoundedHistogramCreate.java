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

package function.ops.histogram;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Ops;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

/**
 * @author Martin Horn (University of Konstanz)
 * @author Christian Dietz (University of Konstanz)
 */
@Plugin(type = Ops.Image.Histogram.class)
public class BoundedHistogramCreate<T extends RealType<T>> extends
AbstractUnaryFunctionOp<Iterable<T>, Histogram1d<T>> implements
Ops.Image.Histogram {

	public final static String MIN_MAX = "Min-Max", PERCENTILE = "Percentile", SIGMA = "Sigma", FIXED = "Fixed";

	@Parameter(required = false)
	private Integer numBins = 256;
	
	@Parameter(required = false)
	private String boundingStrategy = MIN_MAX;

	@Parameter(required = false)
	private double min=0.0d;

	@Parameter(required = false)
	private double max=255.0d;
	
	@Parameter(required = false)
	private boolean utilizeNonParametric = true;

	private UnaryFunctionOp<Iterable<T>, Pair<T, T>> minMaxFunc;
	private UnaryFunctionOp<Iterable<T>, Pair<Double,Double>> percentileFunc;
	private UnaryFunctionOp<Iterable<T>, Pair<Double,Double>> sigmaFunc;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void initialize() {
		if(boundingStrategy.equals(MIN_MAX))
		{
			minMaxFunc = (UnaryFunctionOp) Functions.unary(ops(), Ops.Stats.MinMax.class, Pair.class,
					in() != null ? in() : Iterable.class);
		}
		else if(boundingStrategy.equals(PERCENTILE))
		{
			percentileFunc = (UnaryFunctionOp) Functions.unary(ops(), function.ops.stats.DefaultPercentileLimits.class, ValuePair.class,
					in() != null ? in() : Iterable.class, min, max);
		}
		else if(boundingStrategy.equals(SIGMA))
		{
			sigmaFunc = (UnaryFunctionOp) Functions.unary(ops(), function.ops.stats.DefaultSigmaLimits.class, ValuePair.class,
					in() != null ? in() : Iterable.class, min, max, utilizeNonParametric);
		}
		else
		{
			// FIXED: so just use min and max.
		}
	}

	@Override
	public Histogram1d<T> calculate(final Iterable<T> input) {

		// Get the min and max bins.
		if(boundingStrategy.equals(MIN_MAX))
		{
			final Pair<T, T> res = minMaxFunc.calculate(input);
			min = res.getA().getRealDouble();
			max = res.getB().getRealDouble();
		}
		else if(boundingStrategy.equals(PERCENTILE))
		{
			final Pair<Double,Double> res = percentileFunc.calculate(input);
			min = res.getA();
			max = res.getB();
		}
		else if(boundingStrategy.equals(SIGMA))
		{
			final Pair<Double,Double> res = sigmaFunc.calculate(input);
			min = res.getA();
			max = res.getB();
		}
		else
		{
			// FIXED and just use the current values of min and max.
		}


		final Histogram1d<T> histogram1d = new Histogram1d<>(
				new Real1dBinMapper<T>(min, max, numBins, false));

		histogram1d.countData(input);

		return histogram1d;
	}
}
