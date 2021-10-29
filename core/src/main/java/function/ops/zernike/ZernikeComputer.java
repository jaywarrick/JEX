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
package function.ops.zernike;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import function.ops.JEXOps;
import net.imagej.ops.Op;
import net.imagej.ops.features.zernike.helper.Polynom;
import net.imagej.ops.features.zernike.helper.ZernikeMoment;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.special.function.AbstractUnaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imagej.types.BigComplex;
import net.imglib2.IterableInterval;
import net.imglib2.RealCursor;
import net.imglib2.RealLocalizable;
import net.imglib2.type.numeric.RealType;

/**
 * 
 * Computes a specific zernike moment
 * 
 * @author Andreas Graumann, University of Konstanz
 * @author jaywarrick (added ability to define and calculate enclosing circle)
 */
@Plugin(type = Op.class)
public class ZernikeComputer<T extends RealType<T>> extends
	AbstractUnaryFunctionOp<IterableInterval<T>, ZernikeMoment>
{	
	@Parameter
	private int order;

	@Parameter
	private int repetition;
	
	@Parameter(required = false)
	private Circle enclosingCircle = null;
	
	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	public ZernikeMoment calculate(IterableInterval<T> ii) {
		
		Circle circle = enclosingCircle;
		if(circle == null)
		{
			UnaryFunctionOp<RealCursor<T>,Circle> cirOp = Functions.unary(ops(), JEXOps.SmallestEnclosingCircle.class, Circle.class, in().cursor(), (RealLocalizable) null);
			circle = cirOp.calculate(ii.cursor());
		}
		
		final double centerX = circle.getCenter().getDoublePosition(0);
		final double centerY = circle.getCenter().getDoublePosition(1);

		// Compute pascal's triangle for binomal coefficients: d[x][y] equals (x
		// over y)
		double[][] d = computePascalsTriangle(order);

		// initialize zernike moment
		ZernikeMoment moment = initZernikeMoment(order, repetition, d);

		// get the cursor of the iterable interval
		final RealCursor<? extends RealType<?>> cur = ii.localizingCursor();

		// count number of pixel inside the unit circle
		int count = 0;

		// run over itarble interval
		while (cur.hasNext()) {
			cur.fwd();

			// get 2d centered coordinates
			final double x = cur.getDoublePosition(0);
			final double y = cur.getDoublePosition(1);

			final double xm = (x - centerX) / circle.getRadius();
			final double ym = (y - centerY) / circle.getRadius();

			final double r = Math.sqrt(xm * xm + ym * ym);

			// calculate theta for this position
			final double theta = Math.atan2(ym, xm);

			// get current pixel value
			double pixel = cur.get().getRealDouble();

//			if (pixel >= 0.0) {
				// increment number of pixel inside the unit circle
				count++;

				// calculate the desired moment
				// evaluate radial polynom at position r
				final double rad = moment.getP().evaluate(r);

				// p * rad * exp(-1i * m * theta);
				final BigComplex product = multiplyExp(pixel, rad, theta, moment.getM());

				// add together
				moment.getZm().add(product);
//			}

		}

		// normalization
		// I checked that this normalization approach produces
		// size scale invariance.
		// If one divides by the mean intensity after all this, 
		// then one achieves intensity/contrast invariance.
		// i.e., the measure provided here will double if all intensities are doubled.
		try
		{
			
			normalize(moment.getZm(), moment.getN(), count);
			
		}
		catch(NumberFormatException e)
		{
			System.out.println(count);
			System.out.println(moment);
			System.out.println(moment.getN());
			System.out.println(moment.getZm());
			System.out.println(moment.getZm().getRealDouble());
			System.out.println(moment.getZm().getImaginaryDouble());
			e.printStackTrace();
			System.out.println("WTF Number format exception");
		}

		return moment;
	}

	/**
	 * 
	 * Multiplication of pixel * rad * exp(-m*theta) using eulers formula
	 * (pixel*rad) * (cos(m*theta) - i*sin(m*theta))
	 * 
	 * @param _pixel
	 *            Current pixel
	 * @param _rad
	 *            Computed value of radial polynom,
	 * @param _theta
	 *            Angle of current position
	 * @param _m
	 *            Repitition m
	 * @return Computed term
	 */
	private BigComplex multiplyExp(final double _pixel, final double _rad, final double _theta, final int _m) {
		BigComplex c = new BigComplex();
		c.setReal(_pixel * _rad * Math.cos(_m * _theta));
		c.setImag(-(_pixel * _rad * Math.sin(_m * _theta)));
		return c;
	}

	/**
	 * 
	 * Normalization of all calculated zernike moments in complex representation
	 * 
	 * @param _complex
	 *            Complex representation of zernike moment
	 * @param _n
	 *            Order n
	 * @param _count
	 *            Number of pixel within unit circle
	 */
	private void normalize(BigComplex _complex, int _n, int _count) {
		_complex.setReal(_complex.getRealDouble() * (_n + 1) / _count);
		_complex.setImag(_complex.getImaginaryDouble() * (_n + 1) / _count);
	}

	/**
	 * 
	 * Initialize a zernike moment with a given order and repition
	 * 
	 * @param _order
	 *            Order n
	 * @param _repitition
	 *            Repitition m
	 * @param _d
	 *            Pascal matrix
	 * @return Empty Zernike moment of order n and repitition m
	 */
	private ZernikeMoment initZernikeMoment(final int _order, final int _repitition, final double[][] _d) {

		if (_order - Math.abs(_repitition) % 2 != 0) {
			// throw new IllegalArgumentException("This combination of order an
			// repitition is not valid!");
		}

		return createZernikeMoment(_d, _order, _repitition);
	}

	/**
	 * 
	 * Create one zernike moment of order n and repitition m with suitable
	 * radial polynom
	 * 
	 * @param _d
	 *            Pascal matrix
	 * @param _n
	 *            Order n
	 * @param _m
	 *            Repition m
	 * @return Empty Zernike moment of order n and repition m
	 */
	private ZernikeMoment createZernikeMoment(double[][] _d, int _n, int _m) {
		ZernikeMoment p = new ZernikeMoment();
		p.setM(_m);
		p.setN(_n);
		p.setP(createRadialPolynom(_n, _m, _d));
		BigComplex complexNumber = new BigComplex();
		p.setZm(complexNumber);
		return p;
	}

	/**
	 * Efficient calculation of pascal's triangle up to order max
	 * 
	 * @param _max
	 *            maximal order of pascal's triangle
	 * @return pascal's triangle
	 */
	private double[][] computePascalsTriangle(int _max) {
		double[][] d = new double[_max + 1][_max + 1];
		for (int n = 0; n <= _max; n++) {
			for (int k = 0; k <= n; k++) {
				if ((n == 0 && k == 0) || (n == k) || (k == 0)) {
					d[n][k] = 1.0;
					continue;
				}
				d[n][k] = (((double) n / (n - k))) * d[n - 1][k];
			}
		}
		return d;
	}

	/**
	 * 
	 * @param _n
	 *            Order n
	 * @param _m
	 *            Repitition m
	 * @param _k
	 *            Radius k
	 * @param _d
	 *            Pascal matrix
	 * @return computed term
	 */
	public static int computeBinomialFactorial(final int _n, final int _m, final int _k, double[][] _d) {
		int fac1 = (int) _d[_n - _k][_k];
		int fac2 = (int) _d[_n - 2 * _k][((_n - _m) / 2) - _k];
		int sign = (int) Math.pow(-1, _k);

		return sign * fac1 * fac2;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public void setRepetition(int repetition) {
		this.repetition = repetition;
	}
	
	public void setEnclosingCircle(Circle circle) {
		this.enclosingCircle = circle;
	}

	/**
	 * 
	 * Creates a radial polynom for zernike moment with order n and repitition m
	 * 
	 * @param _n
	 *            Order n
	 * @param _m
	 *            Repitition m
	 * @param _d
	 *            Pascal matrix
	 * @return Radial polnom for moment of order n and repition m
	 */
	public static Polynom createRadialPolynom(final int _n, final int _m, final double[][] _d) {
		final Polynom result = new Polynom(_n);
		for (int s = 0; s <= ((_n - Math.abs(_m)) / 2); ++s) {
			final int pos = _n - (2 * s);
			result.setCoefficient(pos, computeBinomialFactorial(_n, _m, s, _d));
		}
		return result;
	}

}
