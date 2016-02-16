package function.ops.lbp;


/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2014 - 2016 Board of Regents of the University of
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

import java.util.ArrayList;
import java.util.Iterator;

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Ops;
import net.imagej.ops.features.lbp2d.AbstractLBP2DFeature;
import net.imagej.ops.features.lbp2d.DefaultLBP2D;
import net.imagej.ops.features.lbp2d.LBP2DFeature;
import net.imagej.ops.image.histogram.HistogramCreate;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.LongType;

/**
 * Default implementation of 2d local binary patterns
 * 
 * @author Andreas Graumann, University of Konstanz
 * @param <I>
 * @param <O>
 */
@Plugin(type = Ops.LBP.LBP2D.class, label = "2d Local Binary Pattern", priority=Priority.FIRST_PRIORITY)
public class Default8BitUniformLBP2D<I extends RealType<I>> extends AbstractLBP2DFeature<I> implements LBP2DFeature<I>
{
	
	// Citing source: http://answers.opencv.org/question/15493/uniform-lbp-mapping-using-lookup-table/
	public final static long[] uniformLookupTable = {
			0, 1, 2, 3, 4, 58, 5, 6, 7, 58, 58, 58, 8, 58, 9, 10,
			11, 58, 58, 58, 58, 58, 58, 58, 12, 58, 58, 58, 13, 58, 14, 15,
			16, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
			17, 58, 58, 58, 58, 58, 58, 58, 18, 58, 58, 58, 19, 58, 20, 21,
			22, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
			58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
			23, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
			24, 58, 58, 58, 58, 58, 58, 58, 25, 58, 58, 58, 26, 58, 27, 28,
			29, 30, 58, 31, 58, 58, 58, 32, 58, 58, 58, 58, 58, 58, 58, 33,
			58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 34,
			58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58,
			58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 35,
			36, 37, 58, 38, 58, 58, 58, 39, 58, 58, 58, 58, 58, 58, 58, 40,
			58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 58, 41,
			42, 43, 58, 44, 58, 58, 58, 45, 58, 58, 58, 58, 58, 58, 58, 46,
			47, 48, 58, 49, 58, 58, 58, 50, 51, 52, 58, 53, 54, 55, 56, 57 };


	@SuppressWarnings("rawtypes")
	private UnaryFunctionOp<ArrayList, Histogram1d> histOp;
	@SuppressWarnings("rawtypes")
	private UnaryFunctionOp<RandomAccessibleInterval, ArrayList> lbp2dOp;

	@Override
	public void initialize() {
		histOp = Functions.unary(ops(), HistogramCreate.class, Histogram1d.class,
				ArrayList.class, 59);
		lbp2dOp = Functions.unary(ops(), DefaultLBP2D.class, ArrayList.class, RandomAccessibleInterval.class, 1, 256);
	}

	@Override
	public ArrayList<LongType> createOutput(RandomAccessibleInterval<I> input) {
		return new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void compute1(RandomAccessibleInterval<I> input,
		ArrayList<LongType> output)
	{
		ArrayList<LongType> numberList = lbp2dOp.compute1(input);
		ArrayList<LongType> numberList2 = new ArrayList<>();
		for(LongType l : numberList)
		{
			numberList2.add(new LongType(uniformLookupTable[(int)l.get()]));
		}
		Histogram1d<Integer> hist = histOp.compute1(numberList2);
		Iterator<LongType> c = hist.iterator();
		while (c.hasNext()) {
			output.add(new LongType(c.next().get()));
		}
	}
}