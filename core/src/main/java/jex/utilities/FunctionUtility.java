package jex.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import ij.ImagePlus;
import ij.measure.Measurements;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import miscellaneous.LSVList;

public class FunctionUtility {
	
	public static ImagePlus makeImageToSave(FloatProcessor imp, boolean normalize, double multiplier, int bitDepth, boolean invert)
	{
		imp.resetMinAndMax();
		ImageStatistics ims;
		if(normalize)
		{
			ims = ImageStatistics.getStatistics(imp, Measurements.MIN_MAX, null);
			FunctionUtility.imAdjust(imp, ims.min, ims.max, 0, 1, 1);
//			FunctionUtility.normalizeScaleFloat(imp, Math.pow(2, bitDepth) - 1, ims.min, ims.max);
			imp.resetMinAndMax();
		}
		else if(multiplier != 1.0)
		{
			imp.multiply(multiplier);
			imp.resetMinAndMax();
		}
		
		ImageProcessor newImp;
		if(bitDepth == 8)
		{
			newImp = imp.convertToByte(false);
		}
		else if(bitDepth == 16)
		{
			newImp = imp.convertToShort(false);
		}
		else
		{
			newImp = imp;
		}
		if(invert)
		{
			newImp.invert();
		}
		ImagePlus result = new ImagePlus("", newImp);
		return result;
	}
	
	public static ImagePlus makeImageToSave(FloatProcessor imp, String normalize, int bitDepth)
	{
		return makeImageToSave(imp, normalize, bitDepth, 1);
	}
	
	public static ImagePlus makeImageToSave(FloatProcessor imp, String normalize, int bitDepth, double gamma)
	{
		imp.resetMinAndMax();
		ImageStatistics ims;
		String Norm_Scale = normalize;
		boolean NormScaleB = false;
		if(Norm_Scale != null)
		{
			if(Norm_Scale.equals("true"))
			{
				ims = ImageStatistics.getStatistics(imp, Measurements.MIN_MAX, null);
				FunctionUtility.imAdjust(imp, ims.min, ims.max, 0, 1, gamma);
				//FunctionUtility.normalizeScaleFloat(imp, 1, ims.min, ims.max);
				NormScaleB = true;
			}
			else if(Norm_Scale.equals("false"))
			{   
				
			}
			else
			{
				double d = (new Double(Norm_Scale)).doubleValue();
				imp.multiply(d);
			}
		}
		
		if(bitDepth == 0)
		{
			bitDepth = 8;
		}
		ImageProcessor newImp;
		if(bitDepth == 8)
		{
			newImp = imp.convertToByte(NormScaleB);
		}
		else if(bitDepth == 16)
		{
			newImp = imp.convertToShort(NormScaleB);
		}
		else
		{
			newImp = imp;
		}
		
//		newImp.gamma(gamma);
		ImagePlus result = new ImagePlus("", newImp);
		return result;
	}
	
	// public static void imSave(FloatProcessor imp, boolean normalize, double
	// multiplier, int bitDepth, String fullPath)
	// {
	// imp.resetMinAndMax();
	// ImageStatistics ims;
	// if(normalize)
	// {
	// ims = ImageStatistics.getStatistics(imp, ImageStatistics.MIN_MAX, null);
	// FunctionUtility.normalizeScaleFloat(imp, Math.pow(2, bitDepth)-1,
	// ims.min, ims.max);
	// imp.resetMinAndMax();
	// }
	// else if(multiplier != 1.0)
	// {
	// imp.multiply(multiplier);
	// imp.resetMinAndMax();
	// }
	//
	// ImageProcessor newImp;
	// if(bitDepth == 8)
	// {
	// newImp = (ByteProcessor) imp.convertToByte(false);
	// }
	// else if(bitDepth == 16)
	// {
	// newImp = (ShortProcessor) imp.convertToShort(false);
	// }
	// else
	// {
	// newImp = imp;
	// }
	// ImagePlus im = new ImagePlus("Image To Save", newImp);
	// FileSaver imFS = new FileSaver(im);
	// imFS.saveAsTiff(fullPath);
	// //im.show();
	// }
	//
	// public static void imSave(FloatProcessor imp, String normalize, int
	// bitDepth, String fullPath)
	// {
	// imp.resetMinAndMax();
	// ImageStatistics ims;
	// String Norm_Scale = normalize ;
	// boolean NormScaleB = false;
	// if(Norm_Scale != null)
	// {
	// if(Norm_Scale.equals("true"))
	// {
	// ims = ImageStatistics.getStatistics(imp, ImageStatistics.MIN_MAX, null);
	// FunctionUtility.normalizeScaleFloat(imp, 1, ims.min, ims.max);
	// NormScaleB = true;
	// }
	// else if(Norm_Scale.equals("false"))
	// {
	//
	// }
	// else
	// {
	// double d = (new Double(Norm_Scale)).doubleValue();
	// imp.multiply(d);
	// }
	// }
	//
	// if(bitDepth == 0) bitDepth = 8;
	// ImageProcessor newImp;
	// if(bitDepth == 8)
	// {
	// newImp = (ByteProcessor) imp.convertToByte(NormScaleB);
	// }
	// else if(bitDepth == 16)
	// {
	// newImp = (ShortProcessor) imp.convertToShort(NormScaleB);
	// }
	// else
	// {
	// newImp = imp;
	// }
	// ImagePlus im = new ImagePlus("Image To Save", newImp);
	// FileSaver imFS = new FileSaver(im);
	// imFS.saveAsTiff(fullPath);
	// //im.show();
	// }
	//
	// public static void imSave(ByteProcessor imp, String fullPath)
	// {
	// imp.resetMinAndMax();
	// ImagePlus im = new ImagePlus("Image To Save", imp);
	// FileSaver imFS = new FileSaver(im);
	// imFS.saveAsTiff(fullPath);
	// }
	//
	// public static void imSave(ShortProcessor imp, String fullPath)
	// {
	// imp.resetMinAndMax();
	// ImagePlus im = new ImagePlus("Image To Save", imp);
	// FileSaver imFS = new FileSaver(im);
	// imFS.saveAsTiff(fullPath);
	// }
	//
	// public static void imSave(ImagePlus im, String fullPath){
	// FileSaver imFS = new FileSaver(im);
	// imFS.saveAsTiff(fullPath);
	// }
	
//	public static void normalizeScaleFloat(FloatProcessor ip, double newMax, double min, double max)
//	{
//		double ratio = newMax / (max - min);
//		int size = ip.getWidth() * ip.getHeight();
//		float[] pixels = (float[]) ip.getPixels();
//		double v;
//		for (int i = 0; i < size; i++)
//		{
//			v = pixels[i] - min;
//			if(v < 0.0)
//			{
//				v = 0.0;
//			}
//			v *= ratio;
//			if(v > newMax)
//			{
//				v = newMax;
//			}
//			pixels[i] = (float) v;
//		}
//		ip.setPixels(pixels);
//		ip.resetMinAndMax();
//	}
	
	/**
	 * Untested at moment
	 * @param ip
	 * @param min
	 * @param max
	 */
	public static void normalizeScaleShort(ShortProcessor ip, double min, double max)
	{
		double newMax = Math.pow(2, 16) - 1;
		double ratio = newMax / (max - min);
		int size = ip.getWidth() * ip.getHeight();
		short[] pixels = (short[]) ip.getPixels();
		double v;
		for (int i = 0; i < size; i++)
		{
			v = pixels[i] - min;
			if(v < 0.0)
			{
				v = 0.0;
			}
			v *= ratio;
			if(v > newMax)
			{
				v = newMax;
			}
			pixels[i] = (short) v;
		}
		ip.setPixels(pixels);
		ip.resetMinAndMax();
	}
	
	/**
	 * Untested at moment
	 * @param ip
	 * @param newMax
	 * @param min
	 * @param max
	 */
	public static void normalizeScaleByte(ByteProcessor ip, double newMax, double min, double max)
	{
		double ratio = newMax / (max - min);
		int size = ip.getWidth() * ip.getHeight();
		byte[] pixels = (byte[]) ip.getPixels();
		int v;
		for (int i = 0; i < size; i++)
		{
			v = (pixels[i]&0xff) - (int) min;
			if(v < 0.0)
			{
				v = 0;
			}
			v *= ratio;
			if(v > newMax)
			{
				v = (int) newMax;
			}
			pixels[i] = (byte) v;
		}
		ip.setPixels(pixels);
	}
	
	public static void scaleFloat(FloatProcessor ip, double scale)
	{
		int size = ip.getWidth() * ip.getHeight();
		float[] pixels = (float[]) ip.getPixels();
		for (int i = 0; i < size; i++)
		{
			pixels[i] = (float) (pixels[i] * scale);
		}
		ip.setPixels(pixels);
	}
	
	public static void powerFloat(FloatProcessor ip, double power)
	{
		int size = ip.getWidth() * ip.getHeight();
		float[] pixels = (float[]) ip.getPixels();
		for (int i = 0; i < size; i++)
		{
			pixels[i] = (float) (Math.pow(pixels[i], power));
		}
		ip.setPixels(pixels);
	}
	
	public static double[] getBins(FloatProcessor imp)
	{
		int n;
		double min = imp.getMin();
		double max = imp.getMax();
		n = imp.getHistogramSize();
		double[] bins = new double[n];
		for (int i = 0; i < n; i++)
		{
			bins[i] = min + (i - 1) * (max / (n - 1));
		}
		return bins;
	}
	
	public static void imAdjust(ImageProcessor imp, Double oldMin, Double oldMax, double newMin, double newMax, double gamma)
	{
		ImageProcessor ip;
		if(imp instanceof FloatProcessor)
		{
			ip = (FloatProcessor) imp;
		}
		else
		{
			ip = imp.convertToFloatProcessor();
		}
		
		double ratio = (newMax - newMin) / (oldMax - oldMin);
		double offset = newMin - ratio*oldMin;
		
		float[] pixels = (float[]) ip.getPixels();
		for(int i = 0; i < pixels.length; i++)
		{
			pixels[i] = (float) (pixels[i] * ratio + offset);
		}
		//		ip.add(offset);
		//		ip.multiply(ratio);
		//		int size = ip.getWidth() * ip.getHeight();
		//		float[] pixels = (float[]) ip.getPixels();
		//		
		//		float v;
		//		if(newMin != oldMin && newMax != oldMax)
		//		{
		//			for (int i = 0; i < size; i++)
		//			{
		//				v = pixels[i];
		//				if(v < newMinf)
		//				{
		//					v = newMinf;
		//				}
		//				if(v > newMaxf)
		//				{
		//					v = newMaxf;
		//				}
		//				pixels[i] = v;
		//			}
		//		}
		if(imp instanceof FloatProcessor)
		{
			ip.resetMinAndMax();
			ip.gamma(gamma);
		}
		else
		{
			if(imp instanceof ByteProcessor)
			{
				ip = ip.convertToByteProcessor(false);
				imp.setPixels(ip.getPixels());
			}
			if(imp instanceof ShortProcessor)
			{
				ip = ip.convertToShortProcessor(false);
				imp.setPixels(ip.getPixels());
			}
		}
		imp.gamma(gamma);
	}
	
	public static void imThresh(FloatProcessor ip, double thresh, boolean invert)
	{
		int size = ip.getWidth() * ip.getHeight();
		float[] pixels = (float[]) ip.getPixels();
		float v;
		for (int i = 0; i < size; i++)
		{
			v = pixels[i];
			if(v <= (float) thresh)
			{
				v = 0f;
			}
			if(v > (float) thresh)
			{
				v = 255f;
			}
			pixels[i] = v;
		}
		ip.setPixels(pixels);
		if(invert)
		{
			ip.invert();
		}
		ip.resetMinAndMax();
	}
	
	public static void imSubstract(FloatProcessor ip, double thresh)
	{
		int size = ip.getWidth() * ip.getHeight();
		float[] pixels = (float[]) ip.getPixels();
		float v;
		for (int i = 0; i < size; i++)
		{
			v = pixels[i];
			// if (v <= (float) thresh) v = 0f;
			// if (v > (float) thresh) v = 255f;
			pixels[i] = (v <= (float) thresh) ? 0f : v;
		}
		ip.setPixels(pixels);
		ip.resetMinAndMax();
	}
	
	public static String write(String string, String path, boolean append)
	{
		try
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(path, append));
			out.write(string);
			out.close();
		}
		catch (IOException e)
		{
			System.out.println("Error in writing file.");
			return "" + e;
		}
		return null;
	}
	
	public static LSVList read(File f) throws IOException
	{
		LSVList ret = new LSVList();
		BufferedReader bufRdr = new BufferedReader(new FileReader(f));
		String line = null;
		
		// read each line of text file
		while ((line = bufRdr.readLine()) != null)
		{
			ret.add(line);
		}
		
		// close the file
		bufRdr.close();
		return ret;
	}
	
}
