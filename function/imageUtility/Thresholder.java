package function.imageUtility;

import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.measure.Measurements;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.StackProcessor;

import java.awt.Color;

import logs.Logs;

/**
 * This plugin implements the Proxess/Binary/Threshold and Convert to Mask commands.
 */
public class Thresholder implements Measurements {
	
	// Options
	private double minThreshold;
	private double maxThreshold;
	static boolean fill1 = true;
	static boolean fill2 = true;
	static boolean useBW = true;
	boolean convertToMask = true;
	
	// Argument
	public ImagePlus im2run;
	
	public Thresholder(ImagePlus im)
	{
		this.im2run = im;
	}
	
	public void run(String arg)
	{
		convertToMask = true;
		
		if(im2run == null)
		{
			Logs.log("No image set", 1, this);
			return;
		}
		applyThreshold(im2run);
	}
	
	void applyThreshold(ImagePlus imp)
	{
		if(!imp.lock())
			return;
		
		imp.killRoi();
		ImageProcessor ip = imp.getProcessor();
		ip.resetBinaryThreshold();
		
		fill1 = fill2 = true;
		
		if(!(imp.getType() == ImagePlus.GRAY8))
			convertToByte(imp);
		ip = imp.getProcessor();
		autoThreshold(imp);
		
		if(convertToMask && ip.isColorLut())
			ip.setColorModel(ip.getDefaultColorModel());
		int fcolor, bcolor;
		ip.resetThreshold();
		ip.setColor(Color.black);
		
		ip.drawPixel(0, 0);
		fcolor = ip.getPixel(0, 0);
		ip.setColor(Color.white);
		ip.drawPixel(0, 0);
		bcolor = ip.getPixel(0, 0);
		
		int[] lut = new int[256];
		for (int i = 0; i < 256; i++)
		{
			if(i >= minThreshold && i <= maxThreshold)
				lut[i] = fill1 ? fcolor : (byte) i;
			else
			{
				lut[i] = fill2 ? bcolor : (byte) i;
			}
		}
		if(imp.getStackSize() > 1)
			new StackProcessor(imp.getStack(), ip).applyTable(lut);
		else
			ip.applyTable(lut);
		if(convertToMask)
		{
			if(!imp.isInvertedLut())
			{
				setInvertedLut(imp);
				fcolor = 255 - fcolor;
				bcolor = 255 - bcolor;
			}
			if(Prefs.blackBackground)
				ip.invertLut();
		}
		if(fill1 = true && fill2 == true && ((fcolor == 0 && bcolor == 255) || (fcolor == 255 && bcolor == 0)))
			imp.getProcessor().setThreshold(fcolor, fcolor, ImageProcessor.NO_LUT_UPDATE);
		imp.unlock();
	}
	
	void convertToByte(ImagePlus imp)
	{
		ImageProcessor ip = imp.getProcessor();
		double min = ip.getMin();
		double max = ip.getMax();
		int currentSlice = imp.getCurrentSlice();
		ImageStack stack1 = imp.getStack();
		ImageStack stack2 = imp.createEmptyStack();
		int nSlices = imp.getStackSize();
		String label;
		for (int i = 1; i <= nSlices; i++)
		{
			label = stack1.getSliceLabel(i);
			ip = stack1.getProcessor(i);
			ip.setMinAndMax(min, max);
			stack2.addSlice(label, ip.convertToByte(true));
		}
		imp.setStack(null, stack2);
		imp.setSlice(currentSlice);
		imp.setCalibration(imp.getCalibration()); // update calibration
	}
	
	void setInvertedLut(ImagePlus imp)
	{
		ImageProcessor ip = imp.getProcessor();
		ip.invertLut();
		int nImages = imp.getStackSize();
		if(nImages == 1)
			ip.invert();
		else
		{
			ImageStack stack = imp.getStack();
			for (int slice = 1; slice <= nImages; slice++)
				stack.getProcessor(slice).invert();
			stack.setColorModel(ip.getColorModel());
		}
	}
	
	void autoThreshold(ImagePlus imp)
	{
		ImageStatistics stats = imp.getStatistics(MIN_MAX + MODE);
		ImageProcessor ip = imp.getProcessor();
		int threshold = ((ByteProcessor) ip).getAutoThreshold();
		if((stats.max - stats.mode) < (stats.mode - stats.min))
		{
			minThreshold = stats.min;
			maxThreshold = threshold;
		}
		else
		{
			minThreshold = threshold;
			maxThreshold = stats.max;
		}
	}
	
}
