/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2015 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
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


package function.ops.geometry;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.interpolation.InterpolatorFactory;
import net.imglib2.interpolation.randomaccess.LanczosInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.outofbounds.OutOfBoundsBorderFactory;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsRandomValueFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

public class GeomUtils {
	
	public static final String OOB_MIRROR_SINGLE = "Mirror Single", OOB_MIRROR_DOUBLE = "Mirror Double", OOB_ZERO = "Zero", OOB_VALUE = "Value", OOB_RANDOM = "Random", OOB_BORDER = "Border";
	
	public static final String INTERP_LANCZOS = "Lanczos", INTERP_LINEAR = "Linear", INTERP_NEAREST_NEIGHBOR = "Nearest Neighbor";

	/**
	 * 
	 * @param cursor
	 * @param polarOrigin
	 * @return valuePair (r,theta)
	 */
	public static ValuePair<Double,Double> getPolarCoordinatesOfCursor(RealLocalizable cursor, RealLocalizable polarOrigin)
	{
		// get 2d centered coordinates
		double centerX = polarOrigin.getDoublePosition(0);
		double centerY = polarOrigin.getDoublePosition(1);
		double x = cursor.getDoublePosition(0);
		double y = cursor.getDoublePosition(1);
		final double xm = (x - centerX);
		final double ym = (y - centerY);

		final double r = Math.sqrt(xm * xm + ym * ym);

		// calculate theta for this position
		double theta = regularizeTheta(Math.atan2(ym, xm));
		return new ValuePair<>(r, theta);
	}

	public static Double regularizeTheta(Double thetaRadians)
	{
		while(thetaRadians < 0)
		{
			thetaRadians = thetaRadians + 2*Math.PI;
		}
		while(thetaRadians > 2*Math.PI)
		{
			thetaRadians = thetaRadians - 2*Math.PI;
		}
		return thetaRadians;
	}

	public static <T extends RealType<T>> Img<T> rotateImage(Img<T> src, double degrees, String interpMethod)
	{
		Img<T> dest = (Img<T>) src.copy();
		
		double angleInRadians = degrees * (Math.PI/180.0);

		T zero = src.firstElement().createVariable();
		zero.setZero();

		InterpolatorFactory<T, RandomAccessible<T>> ifac = GeomUtils.getInterpolator(interpMethod);

		final RealRandomAccess<T> inter =
				ifac.create(Views.extend(src,
						new OutOfBoundsConstantValueFactory<T, RandomAccessibleInterval<T>>(
								zero)));

		final Cursor<T> c2 = Views.iterable(dest).localizingCursor();
		final double cos = Math.cos(angleInRadians);
		final double sin = Math.sin(angleInRadians);
		final double[] center = new double[dest.numDimensions()];
		center[0] = (dest.dimension(0) / 2.0) - 0.5;
		center[1] = (dest.dimension(1) / 2.0) - 0.5;
		final double[] delta = new double[dest.numDimensions()];
		final long[] d = new long[dest.numDimensions()];
		while (c2.hasNext()) {
			c2.fwd();
			c2.localize(d);
			findDeltas(center, d, cos, sin, delta);
			inter.setPosition(center[0] + delta[0], 0);
			inter.setPosition(center[1] + delta[1], 1);
			for (int i = 2; i < d.length; i++) {
				inter.setPosition(d[i], i);
			}
			c2.get().set(inter.get());
		}
		return dest;
	}
	
	public static <T extends RealType<T>> RealRandomAccessible<T> interpolate(RandomAccessible<T> img, String interpMethod)
	{
		return Views.interpolate(img, getInterpolator(interpMethod));
	}
	
	public static <T extends RealType<T>> ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval<T>> extend(RandomAccessibleInterval<T> img, String oobMethod, Double value, Double randomMin, Double randomMax)
	{
		T tValue = Views.iterable(img).firstElement().createVariable();
		tValue.setReal(value);
		OutOfBoundsFactory<T, RandomAccessibleInterval<T>> oobf = getOOBFactory(oobMethod, tValue, randomMin, randomMax);
		return Views.extend(img, oobf);
	}
	
	public static <T extends RealType<T>> ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval<T>> extend(RandomAccessibleInterval<T> img, String oobMethod, Double value)
	{
		return extend(img, oobMethod, value, 0d, 0d);
	}
	
	public static <T extends RealType<T>> ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval<T>> extend(RandomAccessibleInterval<T> img, String oobMethod)
	{
		return extend(img, oobMethod, 0d, 0d, 0d);
	}
	
	public static <T extends RealType<T>> RealRandomAccessible<T> extendAndInterpolate(RandomAccessibleInterval<T> img, String interpMethod, String oobMethod, Double value, Double randomMin, Double randomMax)
	{
		return interpolate(extend(img, oobMethod, value, randomMin, randomMax), interpMethod);
	}
	
	public static <T extends RealType<T>> RealRandomAccessible<T> extendAndInterpolate(RandomAccessibleInterval<T> img, String interpMethod, String oobMethod, Double value)
	{
		return extendAndInterpolate(img, interpMethod, oobMethod, value, 0d, 0d);
	}
	
	public static <T extends RealType<T>> RealRandomAccessible<T> extendAndInterpolate(RandomAccessibleInterval<T> img, String interpMethod, String oobMethod)
	{
		return extendAndInterpolate(img, interpMethod, oobMethod, 0d, 0d, 0d);
	}

	public static <T extends RealType<T>> InterpolatorFactory<T, RandomAccessible<T>>getInterpolator(String method)
	{
		if (method.equals(INTERP_LINEAR)) {
			return new NLinearInterpolatorFactory<T>();
		}
		else if (method.equals(INTERP_NEAREST_NEIGHBOR)) {
			return new NearestNeighborInterpolatorFactory<T>();
		}
		else if (method.equals(INTERP_LANCZOS)) {
			return new LanczosInterpolatorFactory<T>();
		}
		else throw new IllegalArgumentException("unknown interpolation method: " +
				method);
	}
	
	public static <T extends RealType<T>> OutOfBoundsFactory<T, RandomAccessibleInterval<T>> getOOBFactory(String oobStrategy, final T valueIfNeeded, double randomMin, double randomMax)
	{
		if(oobStrategy.equals(OOB_BORDER))
		{
			return new OutOfBoundsBorderFactory<T, RandomAccessibleInterval<T>>();
		}
		else if(oobStrategy.equals(OOB_MIRROR_SINGLE))
		{
			return new OutOfBoundsMirrorFactory<T, RandomAccessibleInterval<T>>(OutOfBoundsMirrorFactory.Boundary.SINGLE);
		}
		else if(oobStrategy.equals(OOB_MIRROR_DOUBLE))
		{
			return new OutOfBoundsMirrorFactory<T, RandomAccessibleInterval<T>>(OutOfBoundsMirrorFactory.Boundary.DOUBLE);
		}
		else if(oobStrategy.equals(OOB_ZERO))
		{
			//input.firstElement().createVariable()
			final T zeroValue = valueIfNeeded.createVariable();
			zeroValue.setReal(0d);
			return new OutOfBoundsConstantValueFactory<T, RandomAccessibleInterval<T>>(zeroValue);
		}
		else if(oobStrategy.equals(OOB_VALUE))
		{
			return new OutOfBoundsConstantValueFactory<T, RandomAccessibleInterval<T>>(valueIfNeeded);
		}
		else if(oobStrategy.equals(OOB_RANDOM))
		{
			return new OutOfBoundsRandomValueFactory<T, RandomAccessibleInterval<T>>( valueIfNeeded, randomMin, randomMax );
		}
		else
		{
			throw new IllegalArgumentException("Not a valid choice for out-of-bou nds strategy");
		}
	}
	
	private static void findDeltas(double[] ctr, long[] pt, double cos, double sin, double[] delta) {
		double dx = pt[0] - ctr[0];
		double dy = pt[1] - ctr[1];
	    double xPrime = dx * cos - dy * sin;
	    double yPrime = dx * sin + dy * cos;
		delta[0] = xPrime;
		delta[1] = yPrime;
	}
	
	
}
