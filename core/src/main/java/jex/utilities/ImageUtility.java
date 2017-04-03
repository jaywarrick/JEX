package jex.utilities;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import function.singleCellAnalysis.SingleCellUtility;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import ij.process.ImageProcessor;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import rtools.R;
import tables.DimensionMap;

public class ImageUtility {
	
	// ------------------------------------
	// --------- IMAGE UTILITIES ----------
	// ------------------------------------
	
	public static BufferedImage imagePlus2BufferedImage(ImagePlus i)
	{
		BufferedImage bimage = null;
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		bimage = new BufferedImage(i.getWidth(), i.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(i.getImage(), 0, 0, null);
		g.dispose();
		
		return bimage;
	}
	
	public static ImagePlus makeImagePlusStackFromJEXStack(List<DimensionMap> maps, TreeMap<DimensionMap,String> files)
	{
		ImageStack im = null;
		int i = 1;
		boolean first = true;
		ImageProcessor imp = null;
		for(DimensionMap map : maps)
		{
			String imPath = files.get(map);
			if(imPath == null)
			{
				continue;
			}
			imp = (new ImagePlus(imPath)).getProcessor();
			if(first)
			{
				im = new ImageStack(imp.getWidth(), imp.getHeight());
				first = false;
			}
			im.addSlice(imp);
			im.setSliceLabel(map.toString(), i);
			i = i + 1;
		}
		
		if(i == 1)
		{
			return null;
		}
		
		ImagePlus temp = new ImagePlus("Montage");
		temp.setStack(im);
		
		return temp;
	}
	
	public static Pair<Double,Double> getHistogramQuantileThresholds(FloatProcessor imp, double histMin, double histMax, int nBins, double minPercentile, double maxPercentile, boolean showHist, String title)
	{
		
		// Make the histogram
		if(nBins < 2)
		{
			nBins = (int) (histMax - histMin);
		}
		imp.setHistogramRange(histMin, histMax);
		imp.setHistogramSize(nBins);
		FloatStatistics stats = (FloatStatistics) imp.getStatistics();
		double[] bins = new double[nBins];
		for (int i = 0; i < nBins; i++)
		{
			bins[i] = histMin + i * stats.binSize;
		}
		
		long tot = imp.getPixelCount();
		double minCount = minPercentile * (double) tot / 100.0;
		double maxCount = maxPercentile * (double) tot / 100.0;
		long curCount = stats.getHistogram()[0];
		Double minThresh = null;
		Double maxThresh = null;
		for (int i = 1; i < stats.histogram.length; i++)
		{
			curCount = curCount + stats.getHistogram()[i];
			if(curCount > minCount && minThresh == null)
			{
				double leftCount = curCount - stats.getHistogram()[i];
				double rightCount = curCount;
				double fraction = (minCount-leftCount)/(rightCount-leftCount);
				minThresh = bins[i-1] + fraction*stats.binSize;
			}
			if(curCount >= maxCount && maxThresh == null)
			{
				double leftCount = curCount - stats.getHistogram()[i];
				double rightCount = curCount;
				double fraction = (maxCount-leftCount)/(rightCount-leftCount);
				maxThresh = bins[i-1] + fraction*stats.binSize;
			}
		}
		
		if(showHist)
		{
			// Draw the histogram
			R.eval("duh <- 1");
			R.makeVector("binCenters", bins);
			R.makeVector("binCounts", stats.histogram);
			String path = R.startPlot("pdf", 4, 3, 300, 10, null, null);
			if(title == null)
			{
				title = "Histogram";
			}
			title.replace('\'', '_');
			R.eval("plot(binCenters,binCounts,cex=0.4, main=" + R.sQuote(title) + ")");
			R.eval("abline(v=" + minThresh + ")");
			R.eval("abline(v=" + maxThresh + ")");
			R.endPlot();
			
			// Open the histogram plot
			try
			{
				FileUtility.openFileDefaultApplication(path);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return(new Pair<Double,Double>(minThresh, maxThresh));
	}
	
	public static double getHistogramPeakBin(FloatProcessor imp, double histMin, double histMax, int nBins, boolean showHist)
	{
		
		// Make the histogram
		if(nBins < 0)
		{
			nBins = (int) (histMax - histMin);
		}
		imp.setHistogramRange(histMin, histMax);
		imp.setHistogramSize(nBins);
		FloatStatistics stats = new FloatStatistics(imp);
		double[] bins = new double[nBins];
		for (int i = 0; i < nBins; i++)
		{
			bins[i] = histMin + i * stats.binSize;
		}
		
		if(showHist)
		{
			// Draw the histogram
			R.eval("duh <- 1");
			R.makeVector("binCenters", bins);
			R.makeVector("binCounts", stats.histogram);
			String path = R.startPlot("pdf", 4, 3, 300, 10, null, null);
			R.eval("plot(binCenters,binCounts,cex=0.4)");
			R.endPlot();
			
			// Open the histogram plot
			try
			{
				FileUtility.openFileDefaultApplication(path);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		int nMax = 0;
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < stats.histogram.length - 1; i++)
		{
			if(stats.histogram[i] > max)
			{
				max = stats.histogram[i];
				nMax = i;
			}
		}
		return bins[nMax];
	}
	
	public synchronized static String makeHistogramPlot(int[] nums, boolean show)
	{
		R.eval("temp <- 1");
		R.makeVector("counts", nums);
		String path = R.startPlot("pdf", 10, 8, 300, 12, "Arial", null);
		R.eval("plot(counts)");
		R.endPlot();
		if(show)
		{
			try
			{
				FileUtility.openFileDefaultApplication(path);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
		return path;
	}
	
	public static String makeHistogramPlot(Vector<Double> nums, double histMin, double histMax, double ymaxNorm, int nBins, boolean showHist, Double optionalMedian)
	{
		// Draw the histogram
		String path = R.startPlot("png", 4, 3, 300, 10, null, null);
		SingleCellUtility.calculateHistogram("x", "temp", nums, histMin, histMax, ymaxNorm, nBins);
		SingleCellUtility.plotLinearHistogram("temp", "Values", "Counts", histMin, histMax, ymaxNorm, nBins, optionalMedian);
		R.endPlot();
		
		// Open the histogram plot
		if(showHist)
		{
			try
			{
				FileUtility.openFileDefaultApplication(path);
				return path;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
		}
		else
		{
			return path;
		}
	}
	
	public static BufferedImage image2BufferedImage(Image i)
	{
		BufferedImage bimage = null;
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		bimage = new BufferedImage(i.getWidth(null), i.getHeight(null), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(i, 0, 0, null);
		g.dispose();
		
		return bimage;
	}
	
	public static BufferedImage convertToGrayscale(BufferedImage source)
	{
		BufferedImageOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
		return op.filter(source, null);
	}
	
	public static ImagePlus adjustImageContrast(ImagePlus i, double contrast)
	{
		ImageProcessor imp = i.getProcessor();
		imp.multiply(contrast);
		i = new ImagePlus("adjusted image", imp);
		return i;
	}
	
	/**
	 * Open a bufferedimage from string str
	 * 
	 * @param path
	 * @return
	 */
	public static BufferedImage openImage(String path)
	{
		ImagePlus im = new ImagePlus(path);
		if(im.getProcessor() == null)
			return null;
		Image image = im.getImage();
		BufferedImage result = image2BufferedImage(image);
		return result;
	}
	
	/**
	 * Open a bufferedimage from string str
	 * 
	 * @param path
	 * @return
	 */
	public static BufferedImage openImage(String path, int width, int height)
	{
		ImagePlus im = new ImagePlus(path);
		if(im.getProcessor() == null)
			return null;
		
		ImageProcessor imp = im.getProcessor();
		imp = imp.resize(width, height);
		im = new ImagePlus("icon", imp);
		
		Image image = im.getImage();
		BufferedImage result = image2BufferedImage(image);
		return result;
	}
	
	/**
	 * Return a bufferedimage from an image i
	 * 
	 * @param i
	 * @return
	 */
	private static BufferedImage toBufferedImage(Image i)
	{
		if(i instanceof BufferedImage)
		{
			return (BufferedImage) i;
		}
		
		BufferedImage bimage = null;
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		bimage = new BufferedImage(i.getWidth(null), i.getHeight(null), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(i, 0, 0, null);
		g.dispose();
		
		return bimage;
	}
	
	/**
	 * Create a transparent image of color COLOR, with transparency level ALPHA, width WIDTH and height HEIGHT
	 * 
	 * @param color
	 * @param alpha
	 * @param width
	 * @param height
	 */
	private static BufferedImage createMask(Color color, float alpha, int width, int height)
	{
		BufferedImage mask = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gbi = (Graphics2D) mask.getGraphics();
		gbi.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		gbi.setColor(color);
		gbi.fillRect(0, 0, mask.getWidth(), mask.getHeight());
		return mask;
	}
	
	/**
	 * Return an image with a transparency level of ALPHA
	 * 
	 * @param src
	 * @param alpha
	 * @return
	 */
	public static BufferedImage createTransparent(BufferedImage src, float alpha)
	{
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_ARGB;
		BufferedImage result = new BufferedImage(src.getWidth(null), src.getHeight(null), type);
		
		// Copy image to buffered image
		Graphics2D g2 = result.createGraphics();
		
		// Create the transparency
		int rule = AlphaComposite.SRC_OVER;
		AlphaComposite ac = AlphaComposite.getInstance(rule, alpha);
		g2.setComposite(ac);
		g2.drawImage(src, null, 0, 0);
		g2.dispose();
		
		return result;
	}
	
	/**
	 * Return an image shaded by the desired color COLOR
	 * 
	 * @param image
	 * @param color
	 * @return
	 */
	public static ImagePlus filterImageWithColor(ImagePlus image, Color color)
	{
		Image im = image.getImage();
		BufferedImage src = toBufferedImage(im);
		
		// create mask
		BufferedImage mask = createMask(color, (float) 0.5, image.getWidth(), image.getHeight());
		
		// Copy image to buffered image
		Graphics2D g2 = src.createGraphics();
		g2.drawImage(src, null, 0, 0);
		g2.drawImage(mask, null, 0, 0);
		g2.dispose();
		
		ImagePlus result = new ImagePlus("filtered icon", src);
		return result;
	}
	
	/**
	 * Return an image shaded by the desired color COLOR
	 * 
	 * @param image
	 * @param color
	 * @return
	 */
	public static Image filterImageWithColor(Image image, Color color)
	{
		BufferedImage src = toBufferedImage(image);
		
		// create mask
		BufferedImage mask = createMask(color, (float) 0.5, src.getWidth(), src.getHeight());
		
		// Copy image to buffered image
		Graphics2D g2 = src.createGraphics();
		g2.drawImage(src, null, 0, 0);
		g2.drawImage(mask, null, 0, 0);
		g2.dispose();
		
		ImagePlus resultPlus = new ImagePlus("filtered icon", src);
		Image result = resultPlus.getImage();
		return result;
	}
	
	/**
	 * Return an image with a border of color COLOR
	 * 
	 * @param image
	 * @param color
	 * @return
	 */
	public static Image createImageBorderColor(Image image, Color color)
	{
		BufferedImage src = toBufferedImage(image);
		
		// Copy image to buffered image
		Graphics2D g2 = src.createGraphics();
		g2.drawImage(src, null, 0, 0);
		g2.setColor(color);
		g2.drawRect(0, 0, src.getWidth() - 1, src.getHeight() - 1);
		g2.drawRect(1, 1, src.getWidth() - 3, src.getHeight() - 3);
		g2.drawRect(2, 2, src.getWidth() - 5, src.getHeight() - 5);
		g2.drawRect(3, 3, src.getWidth() - 7, src.getHeight() - 7);
		g2.dispose();
		
		ImagePlus resultPlus = new ImagePlus("filtered icon", src);
		Image result = resultPlus.getImage();
		return result;
	}
	
	/**
	 * Add two imagepluses using blitter mode MODE
	 * 
	 * @param im1
	 * @param im2
	 * @return
	 */
	public static ImagePlus add(ImagePlus im1, ImagePlus im2, int mode)
	{
		ImageProcessor imp1 = im1.getProcessor();
		ImageProcessor imp2 = im2.getProcessor();
		
		imp1.copyBits(imp2, 0, 0, mode);
		ImagePlus result = new ImagePlus("Added", imp1);
		return result;
	}
	
	/**
	 * Add two imagepluses using blitter mode MODE
	 * 
	 * @param im1
	 * @param im2
	 * @return
	 */
	public static ImageProcessor add(ImageProcessor imp1, ImageProcessor imp2, int mode)
	{
		imp1.copyBits(imp2, 0, 0, mode);
		return imp1;
	}
	
	public static ImagePlus mergeStacks(int w, int h, ImagePlus red, ImagePlus green, ImagePlus blue)
	{
		ImagePlus result = null;
		ColorProcessor cp;
		byte[] redPixels, greenPixels, bluePixels;
		boolean invertedRed = red != null ? red.getProcessor().isInvertedLut() : false;
		boolean invertedGreen = green != null ? green.getProcessor().isInvertedLut() : false;
		boolean invertedBlue = blue != null ? blue.getProcessor().isInvertedLut() : false;
		
		cp = new ColorProcessor(w, h);
		redPixels = getPixels(red, 0, w, h);
		greenPixels = getPixels(green, 1, w, h);
		bluePixels = getPixels(blue, 2, w, h);
		if(invertedRed)
			redPixels = invert(redPixels);
		if(invertedGreen)
			greenPixels = invert(greenPixels);
		if(invertedBlue)
			bluePixels = invert(bluePixels);
		cp.setRGB(redPixels, greenPixels, bluePixels);
		
		result = new ImagePlus("Merge", cp);
		
		return result;
	}
	
	private static byte[] getPixels(ImagePlus stack, int color, int w, int h)
	{
		if(stack == null)
			return new byte[w * h];
		Object pixels = stack.getProcessor().getPixels();
		if(!(pixels instanceof int[]))
		{
			if(pixels instanceof byte[])
				return (byte[]) pixels;
			else
			{
				ImageProcessor ip = stack.getProcessor();
				ip = ip.convertToByte(true);
				return (byte[]) ip.getPixels();
			}
		}
		else
		{ // RGB
			byte[] r, g, b;
			int size = stack.getWidth() * stack.getHeight();
			r = new byte[size];
			g = new byte[size];
			b = new byte[size];
			ColorProcessor cp = (ColorProcessor) stack.getProcessor();
			cp.getRGB(r, g, b);
			switch (color)
			{
				case 0:
					return r;
				case 1:
					return g;
				case 2:
					return b;
			}
		}
		return null;
	}
	
	private static byte[] invert(byte[] pixels)
	{
		byte[] pixels2 = new byte[pixels.length];
		System.arraycopy(pixels, 0, pixels2, 0, pixels.length);
		for (int i = 0; i < pixels2.length; i++)
			pixels2[i] = (byte) (255 - pixels2[i] & 255);
		return pixels2;
	}
	
	public static void invert(ImageProcessor ip)
	{
		float[] cTable = ip.getCalibrationTable();
		//ip = ip.duplicate();
		if(cTable == null)
		{
			// invert image for finding minima of uncalibrated images
			ip.invert();
		}
		else
		{
			// we are using getPixelValue, so the CalibrationTable must be
			// inverted
			float[] invertedCTable = new float[cTable.length];
			for (int i = cTable.length - 1; i >= 0; i--)
				invertedCTable[i] = -cTable[i];
			ip.setCalibrationTable(invertedCTable);
		}
	}
}
