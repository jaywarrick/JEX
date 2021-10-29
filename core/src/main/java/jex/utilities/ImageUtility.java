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
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import function.ops.histogram.PolynomialRegression;
import function.plugin.plugins.featureExtraction.FeatureUtils;
import function.plugin.plugins.imageProcessing.GaussianBlurForcedRadius;
import function.plugin.plugins.imageProcessing.RankFilters2;
import function.singleCellAnalysis.SingleCellUtility;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatBlitter;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import image.roi.ROIPlus;
import logs.Logs;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import miscellaneous.StatisticsUtility;
import net.imglib2.IterableInterval;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import rtools.R;
import tables.DimensionMap;

public class ImageUtility {

	public static int HIST_MIN_BINS = 10, HIST_MAX_BINS = 250;

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
	
	public static Pair<Double, Double> percentile(ImageProcessor imp, double minPercentile, double maxPercentile, int sampleSize, double ignoreBelow, double ignoreAbove)
	{
		Pair<Double,Double> thresholds = null;
		
		double tempMin = minPercentile;
		double tempMax = maxPercentile;
		
		if(minPercentile < 0)
		{
			minPercentile = 0;
		}
		if(maxPercentile > 100)
		{
			maxPercentile = 100;
		}
		
		if(sampleSize < 1)
		{
			FeatureUtils fu = new FeatureUtils();
			Vector<Double> samples = fu.sampleFromShape(imp, new ROIPlus(new Roi(0,0,imp.getWidth(),imp.getHeight())), true);
			double[] temp = StatisticsUtility.percentile(samples, new double[] {minPercentile, maxPercentile}, ignoreBelow, ignoreAbove);
			return new Pair<>(temp[0],temp[1]);
		}
		
		// Get the thresholds
		if(imp instanceof ByteProcessor || imp.getBitDepth() == 8)
		{
			thresholds = StatisticsUtility.percentile((byte[]) imp.getPixelsCopy(), minPercentile, maxPercentile, ignoreBelow, ignoreAbove);
		}
		if(imp instanceof ShortProcessor || imp.getBitDepth() == 16)
		{
			thresholds = StatisticsUtility.percentile((short[]) imp.getPixelsCopy(), minPercentile, maxPercentile, ignoreBelow, ignoreAbove);
		}
		if(imp instanceof FloatProcessor || imp.getBitDepth() == 32)
		{
			thresholds = StatisticsUtility.percentile((float[]) imp.getPixelsCopy(), minPercentile, maxPercentile, ignoreBelow, ignoreAbove);
		}
		else
		{
			return null;
		}
		
		if(tempMin < 0)
		{
			thresholds.p1 = thresholds.p1 + (tempMin/100)*Math.abs(thresholds.p2 - thresholds.p1);
		}
		
		if(tempMax > 100)
		{
			thresholds.p2 = thresholds.p2 + (tempMax/100)*Math.abs(thresholds.p2 - thresholds.p1);
		}
		
		return thresholds;
	}
	
	public static Pair<Double, Double> percentile(ImageProcessor imp, double minPercentile, double maxPercentile, int sampleSize)
	{
		Pair<Double,Double> thresholds = null;
		
		double tempMin = minPercentile;
		double tempMax = maxPercentile;
		
		if(minPercentile < 0)
		{
			minPercentile = 0;
		}
		if(maxPercentile > 100)
		{
			maxPercentile = 100;
		}
		
		if(sampleSize < 1)
		{
			FeatureUtils fu = new FeatureUtils();
			Vector<Double> samples = fu.sampleFromShape(imp, new ROIPlus(new Roi(0,0,imp.getWidth(),imp.getHeight())), true);
			double[] temp = StatisticsUtility.percentile(samples, new double[] {minPercentile, maxPercentile});
			return new Pair<>(temp[0],temp[1]);
		}
		
		// Get the thresholds
		if(imp instanceof ByteProcessor || imp.getBitDepth() == 8)
		{
			thresholds = StatisticsUtility.percentile((byte[]) imp.getPixelsCopy(), minPercentile, maxPercentile);
		}
		if(imp instanceof ShortProcessor || imp.getBitDepth() == 16)
		{
			thresholds = StatisticsUtility.percentile((short[]) imp.getPixelsCopy(), minPercentile, maxPercentile);
		}
		if(imp instanceof FloatProcessor || imp.getBitDepth() == 32)
		{
			thresholds = StatisticsUtility.percentile((float[]) imp.getPixelsCopy(), minPercentile, maxPercentile);
		}
		else
		{
			return null;
		}
		
		if(tempMin < 0)
		{
			thresholds.p1 = thresholds.p1 + (tempMin/100)*Math.abs(thresholds.p2 - thresholds.p1);
		}
		
		if(tempMax > 100)
		{
			thresholds.p2 = thresholds.p2 + (tempMax/100)*Math.abs(thresholds.p2 - thresholds.p1);
		}
		
		return thresholds;
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

	/**
	 * Filter the image with a standard deviation filter. Then find the deviation from the mode.
	 * Divide the deviation by an estimate of the modal standard deviation, creating a sort of 
	 * z-score from background. Then calculate 1/(1+z^2) to get the weight. So with a z-score
	 * of 1, the weight is 0.5.
	 * 
	 * @param ip
	 * @param radius
	 * @return
	 */
	public static FloatProcessor[] getImageModeWeights(ImageProcessor ip, boolean showHist, double...scaling)
	{
		FloatProcessor ip1 = ip.convertToFloatProcessor();
		ip1.resetMinAndMax();
		//FileUtility.showImg(ip2, true);
		//double[] lim = StatisticsUtility.percentile(ip.getPixels(), 0.1);
		double min = ip1.getMin();
		double max = StatisticsUtility.median(ip1.getPixels());
		min = min + (max-min)/100.0;
		int nBins = getReasonableNumberOfBinsForHistogram(ip1.getWidth()*ip1.getHeight()/2, HIST_MIN_BINS, 250); // divide by 2 because we are only binning pixels below the median
		Pair<double[], int[]> hist = ImageUtility.getHistogram(ip1, min, max, nBins, false);
		double wMed = ImageUtility.getHistogramMode(hist.p1, hist.p2, true, false, true);
		double wMad = (wMed - hist.p1[0])/3.0;
		if(showHist)
		{
			ImageUtility.getHistogramPlot(hist.p1, hist.p2, showHist, wMed-wMad, wMed, wMed+wMad);
		}

		FloatProcessor[] weightImages = transformToWeights(ip1, wMed, wMad, scaling);
		
		return weightImages;
	}

	/**
	 * 
	 * @param weights image
	 * @param data
	 * @param nSigma (positive or negative is fine)
	 * @return
	 */
	public static ByteProcessor getThresholdedWeightsImage(FloatProcessor weights, float nSigma, double scaling)
	{
		// Note that weight pixels should be between 0 and 1;
		float[] weightPixels = (float[]) weights.getPixels();

		byte[] ret = new byte[weightPixels.length];
		for(int i = 0; i < weightPixels.length; i++)
		{
			if(weightPixels[i] <= 1/(1 + Math.pow(Math.abs(nSigma), scaling))) 
			{
				ret[i] = (byte) 255;
			}
		}
		return new ByteProcessor(weights.getWidth(), weights.getHeight(), ret);
	}

	/**
	 * 
	 * @param localSigma
	 * @param data
	 * @param nSigma
	 * @return
	 */
	public static ByteProcessor getLocalThresholdedImage(FloatProcessor localMean, FloatProcessor localSigma, FloatProcessor data, float nSigma)
	{
		float[] dataPixels = (float[]) data.getPixels();
		float[] muPixels = (float[]) localMean.getPixels();
		float[] sigmaPixels = (float[]) localSigma.getPixels();

		if(dataPixels.length != muPixels.length || dataPixels.length != sigmaPixels.length)
		{
			throw new IllegalArgumentException("FloatProcessors must be the same size.");
		}

		byte[] ret = new byte[dataPixels.length];
		for(int i = 0; i < dataPixels.length; i++)
		{
			if(dataPixels[i] >= muPixels[i] + nSigma * sigmaPixels[i])
			{
				ret[i] = (byte) 255;
			}
		}
		return new ByteProcessor(data.getWidth(), data.getHeight(), ret);
	}
	
	/**
	 * 
	 * @param localSigma
	 * @param data
	 * @param nSigma
	 * @return
	 */
	public static ByteProcessor getThresholdedImage(FloatProcessor data, float threshold, boolean above)
	{
		float[] dataPixels = (float[]) data.getPixels();

		byte[] ret = new byte[dataPixels.length];
		if(above)
		{
			for(int i = 0; i < dataPixels.length; i++)
			{
				if(dataPixels[i] >= threshold)
				{
					ret[i] = (byte) 255;
				}
			}
		}
		else
		{
			for(int i = 0; i < dataPixels.length; i++)
			{
				if(dataPixels[i] <= threshold)
				{
					ret[i] = (byte) 255;
				}
			}
		}
		
		return new ByteProcessor(data.getWidth(), data.getHeight(), ret);
	}

	/**
	 * Filter the image with a standard deviation filter. Then find the deviation from the mode.
	 * Divide the deviation by an estimate of the modal standard deviation, creating a sort of 
	 * z-score from background. Then calculate 1/(1+z^2) to get the weight. So with a z-score
	 * of 1, the weight is 0.5.
	 * 
	 * @param ip
	 * @param radius
	 * @return
	 */
	public static Pair<FloatProcessor[], FloatProcessor> getImageVarianceWeights(ImageProcessor ip, double radius, boolean returnStDevImage, boolean showHist, double...scaling)
	{
		// Duplicate the processor to perform calculations.
		FloatProcessor ip1 = ip.duplicate().convertToFloatProcessor(); // Weights Image
		FloatProcessor ip2 = null; // St. Dev. Image.

		// Perform variance filter.
		RankFilters2 rF = new RankFilters2();
		//FileUtility.showImg(ip2, true);
		rF.rank(ip1, radius, RankFilters2.STDEV);
		ip1.resetMinAndMax();

		//FileUtility.showImg(ip2, true);
		double min = ip1.getMin();
		double max = StatisticsUtility.median(ip1.getPixels());
		min = min + (max-min)/100.0;
		int nBins = getReasonableNumberOfBinsForHistogram(ip1.getWidth()*ip1.getHeight()/2, HIST_MIN_BINS, 250); // divide by 2 because we are only binning pixels below the median
		Pair<double[], int[]> hist = ImageUtility.getHistogram(ip1, min, max, nBins, false);
		double wMed = ImageUtility.getHistogramMode(hist.p1, hist.p2, true, true, showHist);
		double wMad = (wMed - hist.p1[0])/1.0;
		if(showHist)
		{
			ImageUtility.getHistogramPlot(hist.p1, hist.p2, showHist, wMed-wMad, wMed, wMed+wMad);
		}

		if(returnStDevImage)
		{
			ip2 = (FloatProcessor) ip1.duplicate();
		}
		//double totWeight = 0.0;
		// Convert the standard deviation image to an image of weights.
		FloatProcessor[] weightImages = transformToWeights(ip1, wMed, wMad, scaling);

		return new Pair<>(weightImages, ip2);
	}
	
	@SuppressWarnings("unused")
	private static FloatProcessor getImageMaskWeights(FloatProcessor ip)
	{
		float[] pixels = (float[]) ip.getPixels();
		for(int j = 0; j < pixels.length; j++)
		{
			// Invert the Mask for weighting as we assume that the objects are white.
			if(pixels[j] <= 0) pixels[j] = 1;
			else if(pixels[j] > 0) pixels[j] = 0.0000001f;
		}
		return(ip);
	}

	private static FloatProcessor[] transformToWeights(FloatProcessor ip, double wMed, double wMad, double...scaling)
	{
		// Convert the standard deviation image to an image of weights.
		FloatProcessor[] weightImages = new FloatProcessor[scaling.length];
		for(int i = 0; i < scaling.length; i++)
		{
			if(i == scaling.length-1)
			{
				// Then creating the last weights image and can use the original.
				weightImages[i] = ip;
			}
			else
			{
				// Then need to create a duplicate weights image to avoid altering the original.
				weightImages[i] = (FloatProcessor) ip.duplicate();
			}
			float[] pixels = (float[]) weightImages[i].getPixels();

			if(scaling[i] == 1)
			{
				for(int j = 0; j < pixels.length; j++)
				{
					// default weight is "zero" (e.g., for pixels with a standard deviation of zero, the pixels are usually saturated at extremes so they don't hold relevant information.
					double val = 0.0000001;
					if(pixels[j] > 0)
					{
						// else the pixel likely has relevant information, but catch conditions that result from rounding etc.
						val = (1 / (1 + Math.abs(pixels[j] - wMed)/wMad));
						if(Double.isFinite(val))
						{
							if(val < 0.0000001) val = 0.0000001;
							else if(val > 1) val = 1;
						}
						else
						{
							val = 0.0000001;
						}
						pixels[j] = (float) val;
						//totWeight = totWeight + pixels[j];
					}

				}
			}
			else
			{
				for(int j = 0; j < pixels.length; j++)
				{
					// default weight is "zero" (e.g., for pixels with a standard deviation of zero, the pixels are usually saturated at extremes so they don't hold relevant information.
					double val = 0.0000001;
					if(pixels[j] > 0)
					{
						// else the pixel likely has relevant information, but catch conditions that result from rounding etc.
						val = (1 / (1 + Math.pow(Math.abs(pixels[j] - wMed)/wMad, scaling[i])));
						if(Double.isFinite(val))
						{
							if(val < 0.0000001) val = 0.0000001;
							else if(val > 1) val = 1;
						}
						else
						{
							val = 0.0000001;
						}
						pixels[j] = (float) val;
						//totWeight = totWeight + pixels[j];
					}

				}
			}

		}
		return(weightImages);
	}
	
	public static Pair<FloatProcessor, ImageProcessor> getWeightedMeanFilterImage(FloatProcessor original, boolean doThreshold, boolean doSubtraction, boolean doBackgroundOnly, boolean doDivision, double meanRadius, double varRadius, double subScale, double threshScale, Double nominal, Double sigma, double darkfield, Double gaussianOuterRadius)
	{
		if(gaussianOuterRadius == null)
		{
			gaussianOuterRadius = 5*meanRadius;
		}
		Pair<FloatProcessor, ImageProcessor> ret = new Pair<>((FloatProcessor) null, (ImageProcessor) null);
		//	FloatProcessor original = im.getProcessor().convertToFloatProcessor();
		FloatProcessor copyOfOriginal = null, subLocalMean = null, threshLocalMean = null;
		if(doThreshold & sigma >= 0 || doSubtraction || doDivision)
		{
			copyOfOriginal = (FloatProcessor) original.duplicate(); // We need this 'original' image regardless of whether we are only doing subtraction, thresholding, or both.
		}

		// Get the Variance weighted image
		Pair<FloatProcessor[], FloatProcessor> w = ImageUtility.getImageVarianceWeights(original, varRadius, doThreshold, false, subScale, threshScale);
		FloatProcessor subWeights = w.p1[0];
		FloatProcessor threshWeights = w.p1[1];
		FloatProcessor localSD = w.p2; // only needed if doing threshold

		// Debug
		//FileUtility.showImg(w, true);

		// Save a thresholded version of the weights image if desired.
		if(doThreshold && sigma < 0)
		{
			// Then threshold the weights image instead of the actual image (e.g., BF images are better done this way)
			ByteProcessor bp = ImageUtility.getThresholdedWeightsImage(threshWeights, sigma.floatValue(), threshScale);
			//String myPath = JEXWriter.saveImage(bp);
			//FileUtility.showImg(bp, true);
			ret.p2 = bp;
		}

		//RankFilters2 rF = new RankFilters2();
		GaussianBlurForcedRadius gb = new GaussianBlurForcedRadius();


		// If necessary, continue calculating the localSD by multiplying by weights and summing
		if(doThreshold && sigma >= 0)
		{
			//FileUtility.showImg(localSD, true);
			localSD.copyBits(threshWeights, 0, 0, Blitter.MULTIPLY);		// localSD (Multiplied)
			//FileUtility.showImg(localSD, true);
			gb.blurGaussian(localSD, meanRadius, meanRadius, gaussianOuterRadius);
			//rF.rank(localSD, meanRadius, RankFilters2.SUM);     		// localSD (Multiplied, Summed)
			//FileUtility.showImg(localSD, true);
		}

		// Perform the final division with the summed weights to get weighted means for each pixel (i.e., sum(w*ret)/sum(w)).
		if(doBackgroundOnly || doSubtraction || doDivision)
		{
			subLocalMean = original;
			if(doThreshold)
			{
				// Then, we need to make a copy to leave one for doing threshold stuff.
				subLocalMean = (FloatProcessor) original.duplicate();
			}

			// Multiply the original image by the weights and sum
			subLocalMean.copyBits(subWeights, 0, 0, Blitter.MULTIPLY); 		// subLocalMean (Multiplied)
			gb.blurGaussian(subLocalMean, meanRadius, meanRadius, gaussianOuterRadius);
			//rF.rank(subLocalMean, meanRadius, RankFilters2.SUM);  		// subLocalMean (Multiplied, Summed)
			gb.blurGaussian(subWeights, meanRadius, meanRadius, gaussianOuterRadius);
			//rF.rank(subWeights, meanRadius, RankFilters2.SUM);    		// subWeights   (Summed)
			subLocalMean.copyBits(subWeights, 0, 0, Blitter.DIVIDE);   		// subLocalMean (Multiplied, Summed, Divided)
		}
		if(doThreshold)
		{
			// We can use localMean directly since we made a copy, if necessary, for subtraction calcs.
			threshLocalMean = original;										// threshLocalMean == original
			threshLocalMean.copyBits(threshWeights, 0, 0, Blitter.MULTIPLY);// threshLocalMean (Multiplied)
			gb.blurGaussian(threshLocalMean, meanRadius, meanRadius, gaussianOuterRadius);
			//rF.rank(threshLocalMean, meanRadius, RankFilters2.SUM);  	// threshLocalMean (Multiplied, Summed)
			//FileUtility.showImg(threshWeights, true);
			gb.blurGaussian(threshWeights, meanRadius, meanRadius, gaussianOuterRadius);
			//rF.rank(threshWeights, meanRadius, RankFilters2.SUM);    	// threshWeights   (Summed)
			//FileUtility.showImg(threshWeights, true);
			threshLocalMean.copyBits(threshWeights, 0, 0, Blitter.DIVIDE);  // threshLocalMean (Multiplied, Summed, Divided)
		}

		// Calculate the localSD if necessary 
		if(doThreshold && sigma >= 0)
		{
			localSD.copyBits(threshWeights, 0, 0, Blitter.DIVIDE); 			// localSD (Multiplied, Summed, Divided)
			//FileUtility.showImg(localSD, true);
		}

		// Calculate and save a locally thresholded image if desired
		if(doThreshold && sigma >= 0)
		{
			FloatBlitter tempfb = new FloatBlitter(threshLocalMean);
			tempfb.copyBits(copyOfOriginal, 0, 0, Blitter.SUBTRACT);
			tempfb.copyBits(localSD, 0, 0, Blitter.DIVIDE);
			ImageProcessor myPath = null;
			if(sigma == 0)
			{
				threshLocalMean.multiply(-1.0);
				myPath = threshLocalMean;
			}
			else
			{
				ByteProcessor mask = ImageUtility.getThresholdedImage(threshLocalMean, -1f*sigma.floatValue(), false);
				myPath = mask;
			}
			ret.p2 = myPath;
		}

		// Save the subtracted or filtered image
		if(doSubtraction)
		{
			//				FileUtility.showImg(copyOfOriginal, true);
			//				FileUtility.showImg(subLocalMean, true);
			copyOfOriginal.copyBits(subLocalMean, 0, 0, Blitter.SUBTRACT);	// copyOfOriginal (Subtracted)
			//				FileUtility.showImg(copyOfOriginal, true);
			if(nominal != 0)
			{
				copyOfOriginal.add(nominal);								// copyOfOriginal (Subtracted, Offset)
			}
		}
		else if(doDivision)
		{
			ImageStatistics stats = subLocalMean.getStats();
			double meanI = stats.mean - darkfield;
			copyOfOriginal.subtract(darkfield);
			subLocalMean.subtract(darkfield);
			copyOfOriginal.copyBits(subLocalMean, 0, 0, Blitter.DIVIDE);	// copyOfOriginal (Divided)
			copyOfOriginal.multiply(meanI);
			copyOfOriginal.subtract(meanI);									// copyOfOriginal (Divided, Scaled)
			if(nominal != 0)
			{
				copyOfOriginal.add(nominal);								// copyOfOriginal (Divided, Scaled Offset)
			}
		}
		else
		{
			copyOfOriginal = subLocalMean;
		}
		if(doBackgroundOnly || doSubtraction || doDivision)
		{
			ret.p1 = copyOfOriginal;
		}
		
		return ret;
	}
	
	public static FloatProcessor getWeightedMeanFilterImage(FloatProcessor original, FloatProcessor mask, boolean doSubtraction, boolean doBackgroundOnly, boolean doDivision, double meanRadius, double varRadius, double subScale, double threshScale, Double nominal, Double sigma, double darkfield, Double gaussianOuterRadius)
	{
		if(gaussianOuterRadius == null)
		{
			gaussianOuterRadius = 5*meanRadius;
		}
		
		//	FloatProcessor original = im.getProcessor().convertToFloatProcessor();
		FloatProcessor copyOfOriginal = null, subLocalMean = null;
		if(doSubtraction || doDivision)
		{
			copyOfOriginal = (FloatProcessor) original.duplicate(); // We need this 'original' image regardless of whether we are only doing subtraction, thresholding, or both.
		}

		// Get the Variance weighted image
		FloatProcessor subWeights = mask.convertToFloatProcessor();
//		FloatProcessor subWeights = ImageUtility.getImageMaskWeights(mask);

		// Debug
		//FileUtility.showImg(w, true);

		//RankFilters2 rF = new RankFilters2();
		GaussianBlurForcedRadius gb = new GaussianBlurForcedRadius();

		// Perform the final division with the summed weights to get weighted means for each pixel (i.e., sum(w*ret)/sum(w)).
		if(doBackgroundOnly || doSubtraction || doDivision)
		{
			subLocalMean = original;

			// Multiply the original image by the weights and sum
			subLocalMean.copyBits(subWeights, 0, 0, Blitter.MULTIPLY); 		// subLocalMean (Multiplied)
			gb.blurGaussian(subLocalMean, meanRadius, meanRadius, gaussianOuterRadius);
			//rF.rank(subLocalMean, meanRadius, RankFilters2.SUM);  		// subLocalMean (Multiplied, Summed)
			gb.blurGaussian(subWeights, meanRadius, meanRadius, gaussianOuterRadius);
			//rF.rank(subWeights, meanRadius, RankFilters2.SUM);    		// subWeights   (Summed)
			subLocalMean.copyBits(subWeights, 0, 0, Blitter.DIVIDE);   		// subLocalMean (Multiplied, Summed, Divided)
		}

		// Save the subtracted or filtered image
		if(doSubtraction)
		{
			//				FileUtility.showImg(copyOfOriginal, true);
			//				FileUtility.showImg(subLocalMean, true);
			copyOfOriginal.copyBits(subLocalMean, 0, 0, Blitter.SUBTRACT);	// copyOfOriginal (Subtracted)
			//				FileUtility.showImg(copyOfOriginal, true);
			if(nominal != 0)
			{
				copyOfOriginal.add(nominal);								// copyOfOriginal (Subtracted, Offset)
			}
		}
		else if(doDivision)
		{
			ImageStatistics stats = subLocalMean.getStats();
			double meanI = stats.mean - darkfield;
			copyOfOriginal.subtract(darkfield);
			subLocalMean.subtract(darkfield);
			copyOfOriginal.copyBits(subLocalMean, 0, 0, Blitter.DIVIDE);	// copyOfOriginal (Divided)
			copyOfOriginal.multiply(meanI);
			copyOfOriginal.subtract(meanI);									// copyOfOriginal (Divided, Scaled)
			if(nominal != 0)
			{
				copyOfOriginal.add(nominal);								// copyOfOriginal (Divided, Scaled Offset)
			}
		}
		else
		{
			copyOfOriginal = subLocalMean;
		}

		return copyOfOriginal;
	}

	/**
	 * Function to calculate the number of bins to use in a histogram for a given image size (width and height).
	 * The return value is bounded by the minBins and maxBins parameters. It will return (maxBins/2) * minBins
	 * when the average width of the image is equal to the maximum number of bins. The return value then
	 * asymptotically approaches minBins and maxBins on either side of that condition.
	 * 
	 * @param nPixels number of items to put in the histogram 
	 * @param minBins minimum number of bins to return
	 * @param maxBins maximum number of bins to return (e.g., to make computation time reasonable).
	 * @return a number of bins ranging gracefully between minBins and maxBins for various sized images.
	 */
	public static int getReasonableNumberOfBinsForHistogram(int nItems, int minBins, int maxBins)
	{
		double sqrt = Math.sqrt(nItems);
		double nBins = Math.max(minBins, sqrt * maxBins/(maxBins + sqrt));
		return (int) nBins;
	}

	public static <T extends Number> Pair<double[], int[]> getHistogram(Collection<T> input, double min, double max, int nBins, boolean includeTails)
	{
		// Make the histogram
		if(nBins <= 0)
		{
			nBins = getReasonableNumberOfBinsForHistogram((int) input.size(), HIST_MIN_BINS, HIST_MAX_BINS);
		}

		Vector<DoubleType> temp = new Vector<DoubleType>((int) input.size());
		for(T n : input)
		{
			temp.add(new DoubleType(n.doubleValue()));
		}

		final Histogram1d<DoubleType> hist = new Histogram1d<>(new Real1dBinMapper<DoubleType>(min, max, nBins, includeTails));
		hist.countData(temp);
		DoubleType val = temp.firstElement().createVariable();
		double[] values = new double[(int) hist.size()];
		int[] counts = new int[(int) hist.size()];
		for(int i=0; i < hist.size(); i++)
		{
			hist.getCenterValue(i, val);
			values[i] = val.getRealDouble();
			counts[i] = (int) hist.frequency(i);
		}
		return new Pair<>(values, counts);
	}

	public static < T extends RealType <T> > Pair<double[], int[]> getHistogram(IterableInterval<T> input, double min, double max, int nBins, boolean includeTails)
	{
		// Make the histogram
		if(nBins <= 0)
		{
			nBins = getReasonableNumberOfBinsForHistogram((int) input.size(), HIST_MIN_BINS, HIST_MAX_BINS);
		}

		final Histogram1d<T> hist = new Histogram1d<>(new Real1dBinMapper<T>(min, max, nBins, includeTails));
		hist.countData(input);
		T val = input.firstElement().createVariable();
		double[] values = new double[(int) hist.size()];
		int[] counts = new int[(int) hist.size()];
		for(int i=0; i < hist.size(); i++)
		{
			hist.getCenterValue(i, val);
			values[i] = val.getRealDouble();
			counts[i] = (int) hist.frequency(i);
		}
		return new Pair<>(values, counts);
	}

	public static < T extends RealType <T> > Pair<double[], int[]> getHistogram(ImageProcessor imp, double min, double max, int nBins, boolean includeTails)
	{
		// Make the histogram
		if(nBins <= 0)
		{
			nBins = getReasonableNumberOfBinsForHistogram(imp.getWidth()*imp.getHeight(), HIST_MIN_BINS, HIST_MAX_BINS); // divide by 2 because will only bin values below median.
		}

		Img<T> img = ImageJFunctions.wrapReal(new ImagePlus("temp", imp));
		final Histogram1d<T> hist = new Histogram1d<>(new Real1dBinMapper<T>(min, max, nBins, includeTails));
		hist.countData(img);
		T val = img.firstElement().createVariable();
		double[] values = new double[(int) hist.size()];
		int[] counts = new int[(int) hist.size()];
		for(int i=0; i < hist.size(); i++)
		{
			hist.getCenterValue(i, val);
			values[i] = val.getRealDouble();
			counts[i] = (int) hist.frequency(i);
		}
		return new Pair<>(values, counts);
	}

	//	/**
	//	 * If nBins is <=0, then nBins will be an appropriate number between HIST_MIN_BINS and HIST_MAX_BINS depending on the image size.
	//	 * 
	//	 * @param imp
	//	 * @param histMin
	//	 * @param histMax
	//	 * @param nBins
	//	 * @return Pair<double[], int[]> the bin centers and bin counts for the histogram.
	//	 */
	//	public static Pair<double[], int[]> getHistogram(ImageProcessor imp, double histMin, double histMax, int nBins, boolean omitEndBins)
	//	{
	//		// Make the histogram
	//		if(nBins <= 0)
	//		{
	//			nBins = getReasonableNumberOfBinsForHistogram(imp.getWidth()*imp.getHeight(), HIST_MIN_BINS, HIST_MAX_BINS);
	//		}
	//		imp.setHistogramRange(histMin, histMax);
	//		imp.setHistogramSize(nBins);
	//		ImageStatistics stats = imp.getStatistics();
	//
	//		double[] bins = new double[0];
	//		if(omitEndBins)
	//		{
	//			bins = new double[nBins-2];
	//			for (int i = 0; i < nBins-2; i++)
	//			{
	//				bins[i] = histMin + (i+1) * stats.binSize;
	//			}
	//		}
	//		else
	//		{
	//			bins = new double[nBins];
	//			for (int i = 0; i < nBins; i++)
	//			{
	//				bins[i] = histMin + i * stats.binSize;
	//			}
	//			return new Pair<>(bins, stats.histogram);
	//		}
	//		return new Pair<>(bins, stats.histogram);
	//	}

	public synchronized static String getHistogramPlot(double[] binCenters, int[] counts, boolean show, Double...vLines)
	{
		return getHistogramPlot(binCenters, counts, show, "pdf", vLines);
	}
	
	public synchronized static String getHistogramPlot(double[] binCenters, int[] counts, boolean show, String extension, Double...vLines)
	{
		if(extension == null)
		{
			extension = "tif";
		}
		// Draw the histogram
		R.eval("duh <- 1");
		R.makeVector("binCenters", binCenters);
		R.makeVector("binCounts", counts);
		String path = R.startPlot(extension, 4, 3, 300, 10, null, null);
		
		R.eval("plot(binCenters,binCounts,cex=0.4)");
		for(Double d : vLines)
		{
			R.eval("abline(v=" + d + ")");
		}
		R.endPlot();

		if(show)
		{
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

		return path;
	}

	private static Double getVertex(PolynomialRegression p)
	{
		if(p.degree() == 2)
		{
			return -1*p.beta(1)/(2.0*p.beta(2));
		}
		else
		{
			return null;
		}
	}

	public static Double getHistogramMode(double[] values, int[] counts, boolean performRegression, boolean findFirstPeak, boolean showHist)
	{
		int i = getHistogramModeIndex(counts);
		double mode = values[i];

		if(performRegression && findFirstPeak)
		{
			int window = (int) Math.max(5, Math.round(0.1*values.length));

			double minDiff = Double.MAX_VALUE;
			double diff = 0.0;
			Double vertex = Double.MAX_VALUE, lastVertex;
			for(int left = 0; left < values.length - window + 1; left++)
			{
				int right = left + window - 1;
				double[] valuesSubset = ImageUtility.getRange(values, left, right);
				int[] countsSubset = ImageUtility.getRange(counts, left, right);
				PolynomialRegression p = new PolynomialRegression(valuesSubset, countsSubset, 2);
				lastVertex = vertex;
				vertex = getVertex(p);
				if(vertex == null)
				{
					vertex = lastVertex;
				}
				int mid = (right + left)/2;
				diff = Math.abs(vertex - values[mid]);
				//Logs.log(values[(left + right)/2] + " - " + vertex + " - " + diff , TestStuff.class);
				if(p.degree() == 2 && p.beta(2) < 0 && vertex < values[right] && vertex > values[left] && p.predict(vertex) > 0.1 * mode && ((p.predict(vertex) - p.predict(values[left])) / p.predict(vertex)) > 0.1)
				{
					if(diff > minDiff)
					{
						// We just passed the peak and we need to return the previous vertex.
						// Logs.log("I think I found it at " + vertex, TestStuff.class);
						if(showHist)
						{
							ImageUtility.getHistogramPlot(values, counts, true, lastVertex);
						}
						return lastVertex;
					}
					// Then we are in a region of negative curvature.
					if(diff <= minDiff) 
					{
						// Then we are getting closer to the peak
						minDiff = diff;
					}
				}
			}
			if(showHist)
			{
				ImageUtility.getHistogramPlot(values, counts, true, mode);
			}
			return mode;
		}

		// If we fail to find the first peak, at least try regression around the mode index if regression is turned on
		if(performRegression)
		{
			int left = i;
			while(left > 0 && counts[left] > 0.9 * counts[i])
			{
				left = left - 1;
			}
			int right = i;
			while(right < counts.length-1 && counts[right] > 0.9 * counts[i])
			{
				right = right + 1;
			}

			double[] valuesSubset = getRange(values, left, right);
			int[] countsSubset = getRange(counts, left, right);
			PolynomialRegression p = new PolynomialRegression(valuesSubset, countsSubset, 2);
			if(p.degree() == 2)
			{
				mode = -1*p.beta(1)/(2.0*p.beta(2));
				if(showHist)
				{
					ImageUtility.getHistogramPlot(values, counts, true, mode);
				}
				return mode;
			}
			else
			{
				Logs.log("Had trouble fitting polynomial regression to histogram to best estimate the mode. Returning peak bin instead.", ImageUtility.class);
				if(showHist)
				{
					ImageUtility.getHistogramPlot(values, counts, true, mode);
				}
				return mode;
			}
		}

		// else return the indexed mode.
		if(showHist)
		{
			ImageUtility.getHistogramPlot(values, counts, true, mode);
		}
		return mode;
	}

	/**
	 * 
	 * @param original
	 * @param from (inclusive)
	 * @param to (inclusive)
	 * @return
	 */
	public static double[] getRange(double[] original, int from, int to)
	{
		int newLength = (to+1) - from;
		if (newLength < 0)
			throw new IllegalArgumentException(from + " > " + (to+1));
		double[] copy = new double[newLength];
		System.arraycopy(original, from, copy, 0,
				Math.min(original.length - from, newLength));
		return copy;
	}

	/**
	 * 
	 * @param original
	 * @param from (inclusive)
	 * @param to (inclusive)
	 * @return
	 */
	public static int[] getRange(int[] original, int from, int to)
	{
		int newLength = (to+1) - from;
		if (newLength < 0)
			throw new IllegalArgumentException(from + " > " + (to+1));
		int[] copy = new int[newLength];
		System.arraycopy(original, from, copy, 0,
				Math.min(original.length - from, newLength));
		return copy;
	}

	public static int getHistogramModeIndex(int[] counts)
	{
		int nMax = 0;
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < counts.length - 1; i++)
		{
			if(counts[i] > max)
			{
				max = counts[i];
				nMax = i;
			}
		}
		return nMax;
	}

	public synchronized static String getHistogramPlot(int[] nums, boolean show)
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

	public static String getHistogramPlot(Vector<Double> nums, double histMin, double histMax, double ymaxNorm, int nBins, boolean showHist, Double optionalMedian)
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
