package function.plugin.plugins.imageProcessing;

import org.jtransforms.dct.FloatDCT_2D;
import org.jtransforms.fft.FloatFFT_2D;

import edu.emory.mathcs.utils.ConcurrencyUtils;
import ij.process.Blitter;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import miscellaneous.FileUtility;
import miscellaneous.Pair;

/**
 * Transport of Intensity Equation Calculator
 * 
 * Takes three brightfield images acquired at different planes. The gradient of intensity
 * change in the z-direction is used to calculate the relative phase offset for each pixel.
 * 
 * This quantity can be related to physical paramters of the cell.
 * https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3872756/
 * 
 * The algorithm is derived from matlab code generously provided by 
 * Chao Zuo
 * Ph.D, Professor
 * Tel: +86-25-84315587
 * Email: zuochao@njust.edu.cn
 * Smart Computational Imaging Laboratory (SCILab) 
 * Nanjing University of Science and Technology 
 * No.200 Xiao Ling Wei, Nanjing, Jiangsu 210094, P. R. China
 * 
 * which is described in detail in
 * 
 * [1] C. Zuo, Q. Chen, and A. Asundi, “Boundary-artifact-free phase retrieval with the transport of intensity equation: fast solution with use of discrete cosine transform,” Opt Express, vol. 22, pp. 9220–44, Apr 2014.
 * [2] C. Zuo, Q. Chen, H. Li, W. Qu, and A. Asundi, “Boundary-artifact-free phase retrieval with the transport of intensity equation ii: applications to microlens characterization,” Opt Express, vol. 22, pp. 18310–24, Jul 2014.
 * 
 * @author jwarrick
 *
 */
public class TIECalculator {

	private int N, M;
	public float[][] negInvLambda, uu, vv;
	private float pixelSize, dz, wavelength, L0X, L0Y, thresh;
	private boolean simple;

	private FloatFFT_2D fft;
	private FloatDCT_2D dct = null;
	static {
		// This is to keep the FFT library from swallowing all the threads
		// and creating delay issues that mess up other programs running on the computer (e.g., the microscope software)
		ConcurrencyUtils.setNumberOfThreads(1);
	}

	/**
	 * Transport of Intensity Equation Calculator
	 * 
	 * Takes three brightfield images acquired at different planes. The gradient of intensity
	 * change in the z-direction is used to calculate the relative phase offset for each pixel.
	 * 
	 * @param imWidth [pixels] The width of the images to be treated 
	 * @param imHeight [pixels] The height of the images to be treated
	 * @param regularization A number typically between 0.1 and 1 for how much to "regularize" the computation (this basically implements a highpass filter, so this can be done after if desired)
	 * @param threshold [fraction] 0 < threshold < 1, This is to eliminate zeros in the image which mess up computation. It is expressed as a fraction of the max intensity (advised is 0.001).
	 * @param magnification The magnification used to take the image
	 * @param pixelSizeInMicrons The pixel size ON THE CAMERA (not the image). Magnification is used to adjust this accordingly.
	 * @param dzInMicrons [µm] The distance between each z-plane
	 * @param wavelengthInNanometers [nm] The spectrally-weighted average wavelength of light being used to image in BF
	 * @param simple [boolean] Should a flat intensity background be assumed so that a simple calculation can be performed that is much faster?
	 */
	public TIECalculator(int imWidth, int imHeight, float regularization, float threshold, float magnification, float pixelSizeInMicrons, float dzInMicrons, float wavelengthInNanometers, boolean simple)
	{
		/**
		 * Remember that M and N are a factor of 2 when doing the "complicated" calculation since we are using a doubled image. We'll just use M/2 and N/2 if doing simple calc
		 * This also means that fftshift and ifftshift are the same thing, but we do everything
		 * in unshifted space here.
		 */
		this.simple = simple;
		this.N = 2*imHeight;
		this.M = 2*imWidth;
		this.pixelSize = pixelSizeInMicrons / 1000000f;
		this.dz = dzInMicrons / 1000000f;
		this.wavelength = wavelengthInNanometers / 1000000000f;
		this.thresh = threshold;


		//		[N,M]=size(dIdz_double); % N is rows (y) and M is cols (x)
		//		m=1:M;
		//		n=1:N;
		//		L0X=pixelsize*M;
		//		L0Y=pixelsize*N;
		this.L0X = (pixelSize / magnification) * ((float) M);
		this.L0Y = (pixelSize / magnification) * ((float) N);


		//		u=(-M/L0X/2+1/L0X*((1:M)-1));
		//		u=u([(floor(M/2)+1):M,1:floor(M/2)])
		//		v=(-N/L0Y/2+1/L0Y*((1:N)-1));
		//		v=v([(floor(N/2)+1):N,1:floor(N/2)])
		//		[uu,vv] = meshgrid(u,v); 
		Pair<FloatProcessor,FloatProcessor> UUVV = getUUVV(simple);
		// viewImage(UUVV.p1); 
		// viewImage(UUVV.p2);


		// Do vv and uu first so we dont' have to duplicate UUVV Float processors to calculate negInvLambda
		//		vv = (2*1i*pi*(vv));
		this.uu = getFloatArray(UUVV.p1); // Re here
		uu = multRealByComplex(uu, 0f, (float) (2*Math.PI)); // Now complex
		//viewIm(uu);


		//		vv = (2*1i*pi*(vv));
		this.vv = getFloatArray(UUVV.p2); // Re here
		vv = multRealByComplex(vv, 0f, (float) (2*Math.PI)); // Now complex
		//viewIm(vv);


		//		negInvLambda = (-4*pi*pi*(uu.*uu+vv.*vv))./(r+(-4*pi*pi*(uu.*uu+vv.*vv)).^2)
		this.negInvLambda = getNegInvLambda((FloatProcessor) UUVV.p1, (FloatProcessor) UUVV.p2, regularization);
		//viewArray(negInvLambda);


		// create transforms for using later
		this.fft = new FloatFFT_2D(this.N, this.M);
		//this.dct = new FloatDCT_2D(this.N/2, this.M/2);

		// Now we are set for calculating a bunch of TIE's
	}


	/**
	 * Transport of Intensity Equation Calculator
	 * 
	 * Takes three brightfield images acquired at different planes. The gradient of intensity
	 * change in the z-direction is used to calculate the relative phase offset for each pixel.


	 * 
	 * 
	 * @param I The in-focus image
	 * @param lo The image below the in-focus image
	 * @param hi The image above the in-focus image
	 * @return
	 */
	public FloatProcessor calculatePhase(FloatProcessor I, FloatProcessor lo, FloatProcessor hi)
	{
		if(!this.simple && I == null)
		{
			return null;
		}
		//		if(this.simple)
		//		{
		//			FloatProcessor kdIdz = getkdIdz(lo, hi, this.dz, this.wavelength);
		//			float[][] ret = dct(kdIdz, false);
		//			this.viewArray(ret);
		//			this.viewArray(this.negInvLambda);
		//			this.multRealByReal(ret, this.negInvLambda);
		//			this.viewArray(ret);
		//			this.idct(ret, false);
		//			this.viewArray(ret);
		//			setFloatArray(I, ret);
		//			I.resetMinAndMax();
		//			return I;
		//			
		//		}

		//		dIdz_double=[dIdz,fliplr(dIdz)];
		//		dIdz_double=[dIdz_double',(flipud(dIdz_double))']';
		//		kdIdz_double=-k*dIdz_double;
		FloatProcessor kdIdz = getkdIdz(lo, hi);
		//viewImage(kdIdz);
		
		kdIdz = replicate(kdIdz);
		
		//		Fleft=fft2(kdIdz_double);
		float[][] Fleft_c = fft(kdIdz);
		// viewImage(kdIdz);	
		// viewImage(getFFTImage(Fleft_c, true));


		//		Fphi=(Fleft).*negInvLambda;
		float[][] Fphi_c = duplicate(Fleft_c);
		multComplexByReal(Fphi_c, this.negInvLambda);
		//viewMag(Fphi_c);


		//		bigphi=real(ifft2((Fphi)));
		float[][] bigphi_r = ifft_array(Fphi_c, true);
		//viewArray(bigphi_r);
		
		if(simple)
		{
			//	phi=phi(1:end/2,1:end/2);
			FloatProcessor ret = getULAsFloatProcessor(bigphi_r);
			ret.resetMinAndMax();
			return ret;
		} // else do long calculation

		//		Fbigphi=fft2(bigphi);
		float[][] Fbigphi_c = fft_real(bigphi_r);

		
		//		Fphi=(Fbigphi).*(2*1i*pi*(uu));
		Fphi_c = multComplexByComplex(Fbigphi_c, this.uu);
		//viewMag(Fphi_c);


		//		dxbigphi=real(ifft2((Fphi)));
		float[][] dxbigphi_r = ifft_array(Fphi_c, true);
		//viewArray(dxbigphi_r);


		//		Fphi=(Fbigphi).*(2*1i*pi*(vv));
		Fphi_c = multComplexByComplex(Fbigphi_c, this.vv);
		//		dybigphi=real(ifft2((Fphi)));


		float[][] dybigphi_r = ifft_array(Fphi_c, true);
		//viewArray(dybigphi_r);


		// Do this before replicating to save time
		//		I0_double(find(I0_double<threshold*max(max(I0_double))))=threshold*max(max(I0_double));
		threshold(I, this.thresh);


		//		I0_double=[I0,fliplr(I0)];
		//		I0_double=[I0_double',(flipud(I0_double))']';
		I = replicate(I);


		//		dxbigphi=dxbigphi./I0_double;
		//		dybigphi=dybigphi./I0_double;
		divideRealByFloatProcessor(dxbigphi_r, I);
		divideRealByFloatProcessor(dybigphi_r, I);


		//		Fbigphi=fft2(dxbigphi);
		//		Fphi=(Fbigphi).*(2*1i*pi*(uu));
		//		dxdxbigphi=real(ifft2((Fphi)));
		Fbigphi_c = fft_real(dxbigphi_r);
		Fphi_c = multComplexByComplex(Fbigphi_c, uu);
		float[][] dxdxbigphi_r = ifft_array(Fphi_c, true);


		//		Fbigphi=fft2(dybigphi);
		//		Fphi=(Fbigphi).*(2*1i*pi*(vv));
		//		dydybigphi=real(ifft2((Fphi)));
		Fbigphi_c = fft_real(dybigphi_r);
		Fphi_c = multComplexByComplex(Fbigphi_c, vv);
		float[][] dydybigphi_r = ifft_array(Fphi_c, true);


		//		ddphi=dxdxbigphi+dydybigphi;
		addRealWithReal(dxdxbigphi_r, dydybigphi_r); // void operation alters first argument
		//viewArray(dxdxbigphi_r);


		//		Fleft=fft2(ddphi);
		Fleft_c = fft_real(dxdxbigphi_r); // so use again here for ddphi


		//		Fphi=(Fleft).*(-4*pi*pi*(uu.*uu+vv.*vv))./(r+(-4*pi*pi*(uu.*uu+vv.*vv)).^2);
		multComplexByReal(Fleft_c, negInvLambda); // void operation alters Fleft_c instead of assigning Fphi_c


		//		phi=real(ifft2((Fphi)));
		float[][] phi = ifft_array(Fleft_c, true);


		//		phi=phi(1:end/2,1:end/2);
		FloatProcessor ret = getULAsFloatProcessor(phi);
		ret.resetMinAndMax();
		return ret;
	}

	public FloatProcessor getkdIdz(FloatProcessor lo, FloatProcessor hi)
	{
		FloatProcessor ret = (FloatProcessor) hi.duplicate();
		FloatBlitter b = new FloatBlitter(ret);
		b.copyBits(lo, 0, 0, Blitter.SUBTRACT);
		double factor = (2*Math.PI/(wavelength))*(1/(2*this.dz)); 
		ret.multiply(factor);
		return ret;
	}
	
	public void viewArray(float[][] a)
	{
		int R = a.length;
		int C = a[1].length;
		FloatProcessor fp = new FloatProcessor(C, R);
		setFloatArray(fp, a);
		FileUtility.showImg(fp, true);
	}

	public void viewRe(float[][] a)
	{
		int R = a.length;
		int C = a[1].length;
		float[][] ret = new float[R][C/2];
		// Copy the fpa into the real float array for fft'ing
		for(int r = 0; r < R; r++)
		{
			for(int c = 0; c < C; c = c + 2)
			{
				ret[r][c/2] = a[r][c];
			}
		}
		viewArray(ret);
	}

	public void viewIm(float[][] a)
	{
		int R = a.length;
		int C = a[1].length;
		float[][] ret = new float[R][C/2];
		// Copy the fpa into the real float array for fft'ing
		for(int r = 0; r < R; r++)
		{
			for(int c = 0; c < C; c = c + 2)
			{
				ret[r][c/2] = a[r][c+1];
			}
		}
		viewArray(ret);
	}

	public void viewMag(float[][] a)
	{
		int R = a.length;
		int C = a[1].length;
		float[][] ret = new float[R][C/2];
		// Copy the fpa into the real float array for fft'ing
		for(int r = 0; r < R; r++)
		{
			for(int c = 0; c < C; c = c + 2)
			{
				ret[r][c/2] = (float) Math.sqrt(a[r][c]*a[r][c] + a[r][c+1]*a[r][c+1]);
			}
		}
		viewArray(ret);
	}

	public void viewImage(FloatProcessor fp)
	{
		FileUtility.showImg(fp, true);
	}

	private void threshold(FloatProcessor ip, float thresh)
	{
		ip.resetMinAndMax();
		float min = (float) (thresh*ip.getMax());
		int size = ip.getWidth() * ip.getHeight();
		float[] pixels = (float[]) ip.getPixels();
		float v;
		for (int i = 0; i < size; i++)
		{
			v = pixels[i];
			if(v < min)
			{
				pixels[i] = min;
			}
		}
		ip.setPixels(pixels);
	}

	public FloatProcessor replicate(FloatProcessor fp)
	{
		int W = fp.getWidth();
		int H = fp.getHeight();
		FloatProcessor ret = new FloatProcessor(2*W, 2*H);
		FloatBlitter b = new FloatBlitter(ret);
		b.copyBits(fp, 0, 0, Blitter.COPY);
		fp.flipHorizontal();
		b.copyBits(fp, W, 0, Blitter.COPY);
		fp.flipVertical();
		b.copyBits(fp, W, H, Blitter.COPY);
		fp.flipHorizontal();
		b.copyBits(fp, 0, H, Blitter.COPY);
		fp.flipVertical(); // Bring image back to original state to reuse again
		return ret;
	}

	private float[][] getNegInvLambda(FloatProcessor UU, FloatProcessor VV, float r)
	{
		// negInvLambda = (-4*pi*pi*(uu.*uu+vv.*vv))./(r+(-4*pi*pi*(uu.*uu+vv.*vv)).^2)
		FloatBlitter bUU = new FloatBlitter(UU);

		// UU*UU
		UU.sqr();

		// UU0
		float UU0 = UU.getPixel(1, 0);

		// VV*VV
		VV.sqr();

		// UU*UU + VV*VV
		bUU.copyBits(VV, 0, 0, Blitter.ADD);

		// -4*pi*pi*(UU*UU + VV*VV)
		UU.multiply(-4d*Math.PI*Math.PI);

		// copy
		FloatProcessor ret = (FloatProcessor) UU.duplicate();
		FloatBlitter bRet = new FloatBlitter(ret);

		// (-4*pi*pi*(uu.*uu+vv.*vv)).^2
		UU.sqr();

		// r + (-4*pi*pi*(uu.*uu+vv.*vv)).^2
		UU.add(Math.pow(-4*Math.PI*Math.PI*(2*r*r*UU0), 2));

		// negInvLambda = (-4*pi*pi*(uu.*uu+vv.*vv))./(r+(-4*pi*pi*(uu.*uu+vv.*vv)).^2)
		bRet.copyBits(UU, 0, 0, Blitter.DIVIDE);

		float[][] daRet = ret.getFloatArray();

		return transpose(daRet);
	}

	private FloatProcessor getULAsFloatProcessor(float[][] a)
	{
		int R = a.length/2;
		int C = a[1].length/2;
		float[][] ret = new float[R][C];
		for(int r = 0; r < R; r++)
		{
			System.arraycopy(a[r], 0, ret[r], 0, C);
		}
		FloatProcessor fp = new FloatProcessor(C, R);
		setFloatArray(fp, ret);
		return fp;
	}

	private float[] getXoL(int M, float L)
	{
		float[] temp = new float[M];
		float[] ret = new float[M];
		for(int m = 1; m <= M; m++)
		{
			temp[m-1] = (-M/L/2+1/L*(m-1));
		}
		int i = 0;
		for(int m = (int) Math.floor(M/2); m < M; m++)
		{
			ret[i] = temp[m];
			i++;
		}
		i = (int) Math.floor(M/2);
		for(int m = 0; m < (int) Math.floor(M/2); m++)
		{
			ret[i] = temp[m];
			i++;
		}
		return ret;
	}

	private Pair<FloatProcessor,FloatProcessor> getUUVV(boolean simple)
	{
		// N and M are the size of the doubled image in pixels
		// N is rows (i.e., y)
		// M is cols (i.e., x)
		// L0X and L0Y are the actual width and height of the doubled image in length units
		//		if(simple)
		//		{
		//			int X = this.M;
		//			int Y = this.N;
		//			FloatProcessor retU = new FloatProcessor(X/2, Y/2);
		//			FloatProcessor retV = new FloatProcessor(X/2, Y/2);
		//			float[] u = getXoL(X, L0X);
		//			float[] v = getXoL(Y, L0Y);
		//			for(int x = 0; x < X/2; x++)
		//			{
		//				for(int y = 0; y < Y/2; y++)
		//				{
		//					// MAYBE WE NEED TO FLIP y?
		//					retU.setf(x, y, u[x]);
		//					retV.setf(x, y, v[y]);
		//				}
		//			}
		//			return new Pair<FloatProcessor,FloatProcessor>(retU, retV);
		//		}
		//		else
		//		{
		int X = this.M;
		int Y = this.N;
		FloatProcessor retU = new FloatProcessor(X, Y);
		FloatProcessor retV = new FloatProcessor(X, Y);
		float[] u = getXoL(X, L0X);
		float[] v = getXoL(Y, L0Y);
		for(int x = 0; x < X; x++)
		{
			for(int y = 0; y < Y; y++)
			{
				// MAYBE WE NEED TO FLIP y?
				retU.setf(x, y, u[x]);
				retV.setf(x, y, v[y]);
			}
		}
		return new Pair<FloatProcessor,FloatProcessor>(retU, retV);
		//		}
	}

	public float[][] transpose(float[][] a)
	{
		float[][] ret = new float[a[1].length][a.length];
		for(int r = 0; r < a.length; r++)
		{
			for(int c = 0; c < a[1].length; c++)
			{
				ret[c][r] = a[r][c];
			}
		}
		return ret;
	}

	public float[][] duplicate(float[][] a)
	{
		int R = a.length;
		int C = a[1].length;

		float[][] ret = new float[R][C];
		for(int r = 0; r < R; r++)
		{
			System.arraycopy(a[r], 0, ret[r], 0, C);
		}
		return ret;
	}

	public float[][] getFloatArray(FloatProcessor fp)
	{
		float[][] fpa = fp.getFloatArray();
		return transpose(fpa);
	}

	public void setFloatArray(FloatProcessor fp, float[][] fpa)
	{
		fp.setFloatArray(transpose(fpa));
	}

	/**
	 * These arrays are always in [row][col] format
	 * @param fp
	 * @return
	 */
	public float[][] fft(FloatProcessor fp)
	{
		float[][] real = new float[fp.getHeight()][2*fp.getWidth()];
		float[][] fpa = getFloatArray(fp);

		// Copy the fpa into the real float array for fft'ing
		for(int r = 0; r < fp.getHeight(); r++)
		{
			System.arraycopy(fpa[r], 0, real[r], 0, fp.getWidth());
		}

		this.fft.realForwardFull(real);
		return real;
	}

	/**
	 * These arrays are always in [row][col] format
	 * @param fp
	 * @return
	 */
	public float[][] dct(FloatProcessor fp, boolean scale)
	{
		if(this.dct == null)
		{
			this.dct = new FloatDCT_2D(this.N/2, this.M/2);
		}
		float[][] fpa = getFloatArray(fp);
		dct.forward(fpa, scale);
		return fpa;
	}

	/**
	 * These arrays are always in [row][col] format
	 * @param fp
	 * @return
	 */
	public void idct(float[][] fpa, boolean scale)
	{
		if(this.dct == null)
		{
			this.dct = new FloatDCT_2D(this.N/2, this.M/2);
		}
		dct.inverse(fpa, scale);
	}

	/**
	 * These arrays are always in [row][col] format
	 * @param fp
	 * @return
	 */
	public float[][] fft_real(float[][] real)
	{
		int R = real.length;
		int C = 2*real[1].length;
		FloatFFT_2D fft = new FloatFFT_2D(R, C/2);
		float[][] complex = new float[R][C];

		// Copy the fpa into the real float array for fft'ing
		for(int r = 0; r < R; r++)
		{
			System.arraycopy(real[r], 0, complex[r], 0, C/2);
		}

		fft.realForwardFull(complex);
		return complex;
	}

	/**
	 * These arrays are always in [row][col] format
	 * @param fp
	 * @return
	 */
	public void fft_complex(float[][] complex)
	{
		int R = complex.length;
		int C = complex[1].length;
		FloatFFT_2D fft = new FloatFFT_2D(R, C/2);
		fft.complexForward(complex);
	}

	/**
	 * These arrays are always in [row][col] format
	 * @param fp
	 * @return
	 */
	public float[][] ifft_array(float[][] complex, boolean scale)
	{
		int R = complex.length;
		int C = complex[1].length;
		FloatFFT_2D fft = new FloatFFT_2D(R, C/2);
		fft.complexInverse(complex, scale);
		float[][] toSet = new float[R][C/2];

		// Copy the fpa into the real float array for fft'ing
		for(int r = 0; r < R; r++)
		{
			for(int c = 0; c < C; c = c + 2)
			{
				toSet[r][c/2] = complex[r][c];
			}
		}
		return toSet;
	}

	/**
	 * These arrays are always in [row][col] format
	 * @param fp
	 * @return
	 */
	public FloatProcessor ifft_fp(float[][] complex, boolean scale)
	{
		int R = complex.length;
		int C = complex[1].length;
		float[][] toSet = ifft_array(complex, scale);
		FloatProcessor ret = new FloatProcessor(C/2, R);
		setFloatArray(ret, toSet);
		return ret;
	}

	public FloatProcessor getFFTImage(float[][] complex, boolean log)
	{
		int R = complex.length;
		int C = complex[1].length;
		float[][] temp  = new float[R][C/2];
		if(log)
		{
			for(int r = 0; r < R; r++)
			{
				for(int c = 0; c < C; c=c+2)
				{
					temp[r][c/2] = (float) Math.log(Math.sqrt(complex[r][c]*complex[r][c] + complex[r][c+1]*complex[r][c+1]));
				}
			}
		}
		else
		{
			for(int r = 0; r < R; r++)
			{
				for(int c = 0; c < C; c=c+2)
				{
					temp[r][c/2] = (float) Math.sqrt(complex[r][c]*complex[r][c] + complex[r][c+1]*complex[r][c+1]);
				}
			}
		}
		FloatProcessor ret = new FloatProcessor(C/2, R);
		setFloatArray(ret, temp);
		return ret;
	}

	public void addRealWithReal(float[][] a1, float[][] a2)
	{
		int R = a1.length;
		int C = a1[1].length;
		for(int r = 0; r < R; r++)
		{
			for(int c = 0; c < C; c=c+2)
			{
				a1[r][c] = a1[r][c] + a2[r][c];
			}
		}
	}

	public void multRealByReal(float[][] a1, float[][] a2)
	{
		int R = a1.length;
		int C = a1[1].length;
		for(int r = 0; r < R; r++)
		{
			for(int c = 0; c < C; c++)
			{
				a1[r][c] = a1[r][c] * a2[r][c];
			}
		}
	}

	public void multComplexByReal(float[][] complex, float[][] real)
	{
		int R = complex.length;
		int C = complex[1].length;
		for(int r = 0; r < R; r++)
		{
			for(int c = 0; c < C; c=c+2)
			{
				complex[r][c] = complex[r][c] * real[r][c/2];
				complex[r][c+1] = complex[r][c+1] * real[r][c/2];
			}
		}
	}

	public void divideRealByFloatProcessor(float[][] real, FloatProcessor fp)
	{
		int R = real.length;
		int C = real[1].length;
		float[][] fpa = getFloatArray(fp);
		for(int r = 0; r < R; r++)
		{
			for(int c = 0; c < C; c++)
			{
				real[r][c] = real[r][c] / fpa[r][c];
			}
		}
	}

	public float[][] multRealByComplex(float[][] real, float re, float im)
	{
		int R = real.length;
		int C = real[1].length*2;
		float[][] ret = new float[R][C];
		for(int r = 0; r < R; r++)
		{
			for(int c = 0; c < C; c=c+2)
			{
				ret[r][c] = real[r][c/2] * re; // The Re part
				ret[r][c+1] = real[r][c/2] * im; // The Im part
			}
		}
		return ret;
	}

	public float[][] multComplexByComplex(float[][] complex1, float[][] complex2)
	{
		int R = complex1.length;
		int C = complex1[1].length;
		float[][] ret = new float[R][C];
		for(int r = 0; r < R; r++)
		{
			for(int c = 0; c < C; c=c+2)
			{
				ret[r][c] = complex1[r][c] * complex2[r][c] - complex1[r][c+1] * complex2[r][c+1]; // The Re*Re - Im*Im
				ret[r][c+1] = complex1[r][c] * complex2[r][c+1] + complex1[r][c+1] * complex2[r][c]; // The Re*Im part for both
			}
		}
		return ret;
	}

}
