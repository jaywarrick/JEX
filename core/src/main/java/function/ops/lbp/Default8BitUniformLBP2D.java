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

import org.scijava.Priority;
import org.scijava.plugin.Plugin;

import net.imagej.ops.Ops;
import net.imagej.ops.features.lbp2d.AbstractLBP2DFeature;
import net.imagej.ops.features.lbp2d.DefaultLBP2D;
import net.imagej.ops.features.lbp2d.LBP2DFeature;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.RandomAccessibleInterval;
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
	private UnaryFunctionOp<RandomAccessibleInterval, ArrayList> lbp2dOp;

	@Override
	public void initialize() {
		lbp2dOp = Functions.unary(ops(), DefaultLBP2D.class, ArrayList.class, in(), 1, 256);
	}

	@Override
	public ArrayList<LongType> createOutput(RandomAccessibleInterval<I> input) {
		return new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void compute(RandomAccessibleInterval<I> input,
		ArrayList<LongType> output)
	{
		ArrayList<LongType> numberList = lbp2dOp.calculate(input);
		for(int i = 0; i < numberList.size(); i++)
		{
			long num = numberList.get(i).getIntegerLong();
			for(long l = 0; l < num; l++)
			output.add(new LongType(uniformLookupTable[i]));
		}
	}
}