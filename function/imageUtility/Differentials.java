package function.imageUtility;

/*=====================================================================
 | Version: July 6, 2011
 \=====================================================================*/

/*=====================================================================
 | EPFL/STI/IMT/LIB
 | Philippe Thevenaz
 | BM-Ecublens 4.137
 | CH-1015 Lausanne VD
 | Switzerland
 |
 | phone (CET): +41(21)693.51.61
 | fax: +41(21)693.68.10
 | RFC-822: philippe.thevenaz@epfl.ch
 | X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
 | URL: http://bigwww.epfl.ch/
 \=====================================================================*/

/*===================================================================
 | This work is based on the following paper:
 |
 | Michael Unser
 | Splines: A Perfect Fit for Signal and Image Processing
 | IEEE Signal Processing Magazine
 | vol. 16, no. 6, pp. 22-38, November 1999
 |
 | This paper is available on-line at
 | http://bigwww.epfl.ch/publications/unser9902.html
 |
 | Other relevant on-line publications are available at
 | http://bigwww.epfl.ch/publications/
 \==================================================================*/

/*====================================================================
 | See the companion file "Differentials_.html" for additional help
 |
 | You'll be free to use this software for research purposes, but you should not
 | redistribute it without our consent. In addition, we expect you to include a
 | citation or acknowlegment whenever you present or publish results that are
 | based on it.
 \===================================================================*/

import ij.process.ImageProcessor;

/*====================================================================
 |	class Differentials_
 \===================================================================*/
/*------------------------------------------------------------------*/
public class Differentials

{ /* begin class Differentials_ */
	
	/*
	 * .................................................................... public variables ....................................................................
	 */
	public final static int GRADIENT_MAGNITUDE = 0;
	public final static int GRADIENT_DIRECTION = 1;
	public final static int LAPLACIAN = 2;
	public final static int LARGEST_HESSIAN = 3;
	public final static int SMALLEST_HESSIAN = 4;
	public final static int HESSIAN_ORIENTATION = 5;
	
	/*
	 * .................................................................... private variables ....................................................................
	 */
	private static final double FLT_EPSILON = Float.intBitsToFloat(0x33FFFFFF);
	
	/*
	 * .................................................................... PlugIn methods ....................................................................
	 */
	/*------------------------------------------------------------------*/
	// @Override
	// public void run(String arg)
	// {
	// ImagePlus imp = WindowManager.getCurrentImage();
	// this.imp = imp;
	// if(imp == null)
	// {
	// IJ.noImage();
	// return;
	// }
	// if((1 < imp.getStackSize()) && (imp.getType() == ImagePlus.COLOR_256))
	// {
	// IJ.error("Stack of color images not supported (use grayscale)");
	// return;
	// }
	// if(1 < imp.getStackSize())
	// {
	// if(imp.getStack().isRGB())
	// {
	// IJ.error("RGB color images not supported (use grayscale)");
	// return;
	// }
	// }
	// if(1 < imp.getStackSize())
	// {
	// if(imp.getStack().isHSB())
	// {
	// IJ.error("HSB color images not supported (use grayscale)");
	// return;
	// }
	// }
	// if(imp.getType() == ImagePlus.COLOR_256)
	// {
	// IJ.error("Indexed color images not supported (use grayscale)");
	// return;
	// }
	// if(imp.getType() == ImagePlus.COLOR_RGB)
	// {
	// IJ.error("Color images not supported (use grayscale)");
	// return;
	// }
	//
	// differentialsDialog dialog = new differentialsDialog(IJ.getInstance(), "Differentials", true, operation);
	// GUI.center(dialog);
	// dialog.setVisible(true);
	// this.cancel = dialog.getCancel();
	// operation = dialog.getOperation();
	// dialog.dispose();
	// if(this.cancel)
	// {
	// return;
	// }
	//
	// imp.startTiming();
	// if(1 < imp.getStackSize())
	// {
	// if(!(imp.getProcessor().getPixels() instanceof float[]))
	// {
	// new StackConverter(imp).convertToGray32();
	// }
	// }
	// else
	// {
	// if(!(imp.getProcessor().getPixels() instanceof float[]))
	// {
	// new ImageConverter(imp).convertToGray32();
	// }
	// }
	// ImageStack stack = imp.getStack();
	// this.stackSize = stack.getSize();
	// Undo.reset();
	//
	// this.setupProgressBar();
	// this.resetProgressBar();
	//
	// for (int i = 1; (i <= this.stackSize); i++)
	// {
	// ImageProcessor ip = stack.getProcessor(i);
	// this.calcDifferential(ip, operation);
	// imp.getProcessor().resetMinAndMax();
	// imp.setSlice(i);
	// imp.updateAndRepaintWindow();
	// }
	// imp.getProcessor().resetMinAndMax();
	// imp.setSlice(1);
	// imp.updateAndRepaintWindow();
	// IJ.showTime(imp, imp.getStartTime(), "Differentials: ");
	// ImageWindow win = imp.getWindow();
	// if(win != null)
	// {
	// win.running = false;
	// }
	// } /* end run */
	/*
	 * .................................................................... public methods ....................................................................
	 */
	/*------------------------------------------------------------------*/
	public static void getCrossHessian(ImageProcessor ip, double tolerance)
	{
		getHorizontalGradient(ip, tolerance);
		getVerticalGradient(ip, tolerance);
	} /* end getCrossHessian */
	
	/*------------------------------------------------------------------*/
	public static void getHorizontalGradient(ImageProcessor ip, double tolerance)
	{
		if(!(ip.getPixels() instanceof float[]))
		{
			throw new IllegalArgumentException("Float image required");
		}
		
		int width = ip.getWidth();
		int height = ip.getHeight();
		double line[] = new double[width];
		
		for (int y = 0; (y < height); y++)
		{
			getRow(ip, y, line);
			getSplineInterpolationCoefficients(line, tolerance);
			getGradient(line);
			putRow(ip, y, line);
		}
	} /* end getHorizontalGradient */
	
	/*------------------------------------------------------------------*/
	public static void getHorizontalHessian(ImageProcessor ip, double tolerance)
	{
		if(!(ip.getPixels() instanceof float[]))
		{
			throw new IllegalArgumentException("Float image required");
		}
		
		int width = ip.getWidth();
		int height = ip.getHeight();
		double line[] = new double[width];
		
		for (int y = 0; (y < height); y++)
		{
			getRow(ip, y, line);
			getSplineInterpolationCoefficients(line, tolerance);
			getHessian(line);
			putRow(ip, y, line);
		}
	} /* end getHorizontalHessian */
	
	/*------------------------------------------------------------------*/
	public static void getVerticalGradient(ImageProcessor ip, double tolerance)
	{
		if(!(ip.getPixels() instanceof float[]))
		{
			throw new IllegalArgumentException("Float image required");
		}
		
		int width = ip.getWidth();
		int height = ip.getHeight();
		double line[] = new double[height];
		
		for (int x = 0; (x < width); x++)
		{
			getColumn(ip, x, line);
			getSplineInterpolationCoefficients(line, tolerance);
			getGradient(line);
			putColumn(ip, x, line);
		}
	} /* end getVerticalGradient */
	
	/*------------------------------------------------------------------*/
	public static void getVerticalHessian(ImageProcessor ip, double tolerance)
	{
		if(!(ip.getPixels() instanceof float[]))
		{
			throw new IllegalArgumentException("Float image required");
		}
		
		int width = ip.getWidth();
		int height = ip.getHeight();
		double line[] = new double[height];
		
		for (int x = 0; (x < width); x++)
		{
			getColumn(ip, x, line);
			getSplineInterpolationCoefficients(line, tolerance);
			getHessian(line);
			putColumn(ip, x, line);
		}
	} /* end getVerticalHessian */
	
	/*
	 * .................................................................... private methods ....................................................................
	 */
	/*------------------------------------------------------------------*/
	private static void antiSymmetricFirMirrorOnBounds(double[] h, double[] c, double[] s)
	{
		if(h.length != 2)
		{
			throw new IndexOutOfBoundsException("The half-length filter size should be 2");
		}
		if(h[0] != 0.0)
		{
			throw new IllegalArgumentException("Antisymmetry violation (should have h[0]=0.0)");
		}
		if(c.length != s.length)
		{
			throw new IndexOutOfBoundsException("Incompatible size");
		}
		if(2 <= c.length)
		{
			s[0] = 0.0;
			for (int i = 1; (i < (s.length - 1)); i++)
			{
				s[i] = h[1] * (c[i + 1] - c[i - 1]);
			}
			s[s.length - 1] = 0.0;
		}
		else
		{
			if(c.length == 1)
			{
				s[0] = 0.0;
			}
			else
			{
				throw new NegativeArraySizeException("Invalid length of data");
			}
		}
	} /* end antiSymmetricFirMirrorOnBounds */
	
	/*------------------------------------------------------------------*/
	public static void calcDifferential(ImageProcessor ip, int operation)
	{
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		if(!(ip.getPixels() instanceof float[]))
		{
			throw new IllegalArgumentException("Float image required");
		}
		switch (operation)
		{
			case GRADIENT_MAGNITUDE:
			{
				ImageProcessor h = ip.duplicate();
				ImageProcessor v = ip.duplicate();
				float[] floatPixels = (float[]) ip.getPixels();
				float[] floatPixelsH = (float[]) h.getPixels();
				float[] floatPixelsV = (float[]) v.getPixels();
				
				getHorizontalGradient(h, FLT_EPSILON);
				getVerticalGradient(v, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++)
				{
					for (int x = 0; (x < width); x++, k++)
					{
						floatPixels[k] = (float) Math.sqrt(floatPixelsH[k] * floatPixelsH[k] + floatPixelsV[k] * floatPixelsV[k]);
					}
				}
			}
				break;
			case GRADIENT_DIRECTION:
			{
				ImageProcessor h = ip.duplicate();
				ImageProcessor v = ip.duplicate();
				float[] floatPixels = (float[]) ip.getPixels();
				float[] floatPixelsH = (float[]) h.getPixels();
				float[] floatPixelsV = (float[]) v.getPixels();
				
				getHorizontalGradient(h, FLT_EPSILON);
				getVerticalGradient(v, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++)
				{
					for (int x = 0; (x < width); x++, k++)
					{
						floatPixels[k] = (float) Math.atan2(floatPixelsH[k], floatPixelsV[k]);
					}
				}
			}
				break;
			case LAPLACIAN:
			{
				ImageProcessor hh = ip.duplicate();
				ImageProcessor vv = ip.duplicate();
				float[] floatPixels = (float[]) ip.getPixels();
				float[] floatPixelsHH = (float[]) hh.getPixels();
				float[] floatPixelsVV = (float[]) vv.getPixels();
				
				getHorizontalHessian(hh, FLT_EPSILON);
				getVerticalHessian(vv, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++)
				{
					for (int x = 0; (x < width); x++, k++)
					{
						floatPixels[k] = floatPixelsHH[k] + floatPixelsVV[k];
					}
				}
			}
				break;
			case LARGEST_HESSIAN:
			{
				ImageProcessor hh = ip.duplicate();
				ImageProcessor vv = ip.duplicate();
				ImageProcessor hv = ip.duplicate();
				float[] floatPixels = (float[]) ip.getPixels();
				float[] floatPixelsHH = (float[]) hh.getPixels();
				float[] floatPixelsVV = (float[]) vv.getPixels();
				float[] floatPixelsHV = (float[]) hv.getPixels();
				
				getHorizontalHessian(hh, FLT_EPSILON);
				getVerticalHessian(vv, FLT_EPSILON);
				getCrossHessian(hv, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++)
				{
					for (int x = 0; (x < width); x++, k++)
					{
						floatPixels[k] = (float) (0.5 * (floatPixelsHH[k] + floatPixelsVV[k] + Math.sqrt(4.0 * floatPixelsHV[k] * floatPixelsHV[k] + (floatPixelsHH[k] - floatPixelsVV[k]) * (floatPixelsHH[k] - floatPixelsVV[k]))));
					}
				}
			}
				break;
			case SMALLEST_HESSIAN:
			{
				ImageProcessor hh = ip.duplicate();
				ImageProcessor vv = ip.duplicate();
				ImageProcessor hv = ip.duplicate();
				float[] floatPixels = (float[]) ip.getPixels();
				float[] floatPixelsHH = (float[]) hh.getPixels();
				float[] floatPixelsVV = (float[]) vv.getPixels();
				float[] floatPixelsHV = (float[]) hv.getPixels();
				
				getHorizontalHessian(hh, FLT_EPSILON);
				getVerticalHessian(vv, FLT_EPSILON);
				getCrossHessian(hv, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++)
				{
					for (int x = 0; (x < width); x++, k++)
					{
						floatPixels[k] = (float) (0.5 * (floatPixelsHH[k] + floatPixelsVV[k] - Math.sqrt(4.0 * floatPixelsHV[k] * floatPixelsHV[k] + (floatPixelsHH[k] - floatPixelsVV[k]) * (floatPixelsHH[k] - floatPixelsVV[k]))));
					}
				}
			}
				break;
			case HESSIAN_ORIENTATION:
			{
				ImageProcessor hh = ip.duplicate();
				ImageProcessor vv = ip.duplicate();
				ImageProcessor hv = ip.duplicate();
				float[] floatPixels = (float[]) ip.getPixels();
				float[] floatPixelsHH = (float[]) hh.getPixels();
				float[] floatPixelsVV = (float[]) vv.getPixels();
				float[] floatPixelsHV = (float[]) hv.getPixels();
				
				getHorizontalHessian(hh, FLT_EPSILON);
				getVerticalHessian(vv, FLT_EPSILON);
				getCrossHessian(hv, FLT_EPSILON);
				for (int y = 0, k = 0; (y < height); y++)
				{
					for (int x = 0; (x < width); x++, k++)
					{
						if(floatPixelsHV[k] < 0.0)
						{
							floatPixels[k] = (float) (-0.5 * Math.acos((floatPixelsHH[k] - floatPixelsVV[k]) / Math.sqrt(4.0 * floatPixelsHV[k] * floatPixelsHV[k] + (floatPixelsHH[k] - floatPixelsVV[k]) * (floatPixelsHH[k] - floatPixelsVV[k]))));
						}
						else
						{
							floatPixels[k] = (float) (0.5 * Math.acos((floatPixelsHH[k] - floatPixelsVV[k]) / Math.sqrt(4.0 * floatPixelsHV[k] * floatPixelsHV[k] + (floatPixelsHH[k] - floatPixelsVV[k]) * (floatPixelsHH[k] - floatPixelsVV[k]))));
						}
					}
				}
			}
				break;
			default:
				throw new IllegalArgumentException("Invalid operation");
		}
		ip.resetMinAndMax();
		// this.imp.updateAndDraw();
	} /* end doIt */
	
	/*------------------------------------------------------------------*/
	private static void getColumn(ImageProcessor ip, int x, double[] column)
	{
		int width = ip.getWidth();
		
		if(ip.getHeight() != column.length)
		{
			throw new IndexOutOfBoundsException("Incoherent array sizes");
		}
		if(ip.getPixels() instanceof float[])
		{
			float[] floatPixels = (float[]) ip.getPixels();
			for (int i = 0; (i < column.length); i++)
			{
				column[i] = floatPixels[x];
				x += width;
			}
		}
		else
		{
			throw new IllegalArgumentException("Float image required");
		}
	} /* end getColumn */
	
	/*------------------------------------------------------------------*/
	private static void getGradient(double[] c)
	{
		double h[] = { 0.0, -1.0 / 2.0 };
		double s[] = new double[c.length];
		
		antiSymmetricFirMirrorOnBounds(h, c, s);
		System.arraycopy(s, 0, c, 0, s.length);
	} /* end getGradient */
	
	/*------------------------------------------------------------------*/
	private static void getHessian(double[] c)
	{
		double h[] = { -2.0, 1.0 };
		double s[] = new double[c.length];
		
		symmetricFirMirrorOnBounds(h, c, s);
		System.arraycopy(s, 0, c, 0, s.length);
	} /* end getHessian */
	
	/*------------------------------------------------------------------*/
	private static double getInitialAntiCausalCoefficientMirrorOnBounds(double[] c, double z, double tolerance)
	{
		return ((z * c[c.length - 2] + c[c.length - 1]) * z / (z * z - 1.0));
	} /* end getInitialAntiCausalCoefficientMirrorOnBounds */
	
	/*------------------------------------------------------------------*/
	protected static double getInitialCausalCoefficientMirrorOnBounds(double[] c, double z, double tolerance)
	{
		double z1 = z, zn = Math.pow(z, c.length - 1);
		double sum = c[0] + zn * c[c.length - 1];
		int horizon = c.length;
		
		if(0.0 < tolerance)
		{
			horizon = 2 + (int) (Math.log(tolerance) / Math.log(Math.abs(z)));
			horizon = (horizon < c.length) ? (horizon) : (c.length);
		}
		zn = zn * zn;
		for (int n = 1; (n < (horizon - 1)); n++)
		{
			zn = zn / z;
			sum = sum + (z1 + zn) * c[n];
			z1 = z1 * z;
		}
		return (sum / (1.0 - Math.pow(z, 2 * c.length - 2)));
	} /* end getInitialCausalCoefficientMirrorOnBounds */
	
	/*------------------------------------------------------------------*/
	private static void getRow(ImageProcessor ip, int y, double[] row)
	{
		int rowLength = ip.getWidth();
		
		if(rowLength != row.length)
		{
			throw new IndexOutOfBoundsException("Incoherent array sizes");
		}
		y *= rowLength;
		if(ip.getPixels() instanceof float[])
		{
			float[] floatPixels = (float[]) ip.getPixels();
			for (int i = 0; (i < rowLength); i++)
			{
				row[i] = floatPixels[y++];
			}
		}
		else
		{
			throw new IllegalArgumentException("Float image required");
		}
	} /* end getRow */
	
	/*------------------------------------------------------------------*/
	private static void getSplineInterpolationCoefficients(double[] c, double tolerance)
	{
		double z[] = { Math.sqrt(3.0) - 2.0 };
		double lambda = 1.0;
		
		if(c.length == 1)
		{
			return;
		}
		for (int k = 0; (k < z.length); k++)
		{
			lambda = lambda * (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
		}
		for (int n = 0; (n < c.length); n++)
		{
			c[n] = c[n] * lambda;
		}
		for (int k = 0; (k < z.length); k++)
		{
			c[0] = getInitialCausalCoefficientMirrorOnBounds(c, z[k], tolerance);
			for (int n = 1; (n < c.length); n++)
			{
				c[n] = c[n] + z[k] * c[n - 1];
			}
			c[c.length - 1] = getInitialAntiCausalCoefficientMirrorOnBounds(c, z[k], tolerance);
			for (int n = c.length - 2; (0 <= n); n--)
			{
				c[n] = z[k] * (c[n + 1] - c[n]);
			}
		}
	} /* end getSplineInterpolationCoefficients */
	
	/*------------------------------------------------------------------*/
	private static void putColumn(ImageProcessor ip, int x, double[] column)
	{
		int width = ip.getWidth();
		
		if(ip.getHeight() != column.length)
		{
			throw new IndexOutOfBoundsException("Incoherent array sizes");
		}
		if(ip.getPixels() instanceof float[])
		{
			float[] floatPixels = (float[]) ip.getPixels();
			for (int i = 0; (i < column.length); i++)
			{
				floatPixels[x] = (float) column[i];
				x += width;
			}
		}
		else
		{
			throw new IllegalArgumentException("Float image required");
		}
	} /* end putColumn */
	
	/*------------------------------------------------------------------*/
	private static void putRow(ImageProcessor ip, int y, double[] row)
	{
		int rowLength = ip.getWidth();
		
		if(rowLength != row.length)
		{
			throw new IndexOutOfBoundsException("Incoherent array sizes");
		}
		y *= rowLength;
		if(ip.getPixels() instanceof float[])
		{
			float[] floatPixels = (float[]) ip.getPixels();
			for (int i = 0; (i < rowLength); i++)
			{
				floatPixels[y++] = (float) row[i];
			}
		}
		else
		{
			throw new IllegalArgumentException("Float image required");
		}
	} /* end putRow */
	
	/*------------------------------------------------------------------*/
	private static void symmetricFirMirrorOnBounds(double[] h, double[] c, double[] s)
	{
		if(h.length != 2)
		{
			throw new IndexOutOfBoundsException("The half-length filter size should be 2");
		}
		if(c.length != s.length)
		{
			throw new IndexOutOfBoundsException("Incompatible size");
		}
		if(2 <= c.length)
		{
			s[0] = h[0] * c[0] + 2.0 * h[1] * c[1];
			for (int i = 1; (i < (s.length - 1)); i++)
			{
				s[i] = h[0] * c[i] + h[1] * (c[i - 1] + c[i + 1]);
			}
			s[s.length - 1] = h[0] * c[c.length - 1] + 2.0 * h[1] * c[c.length - 2];
		}
		else
		{
			switch (c.length)
			{
				case 1:
					s[0] = (h[0] + 2.0 * h[1]) * c[0];
					break;
				default:
					throw new NegativeArraySizeException("Invalid length of data");
			}
		}
	} /* end symmetricFirMirrorOnBounds */
	
} /* end class Differentials_ */
