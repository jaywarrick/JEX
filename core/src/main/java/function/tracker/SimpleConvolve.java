package function.tracker;

import ij.ImagePlus;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

import jex.utilities.ImageUtility;
import logs.Logs;
import miscellaneous.StopWatch;

public class SimpleConvolve {
	
	// class variables
	private ImagePlus conv;
	private ImagePlus image;
	private ImagePlus result;
	// private float[][] imageSmall;
	
	// clas variables
	private int smallImageWidth;
	private int smallImageHeight;
	private Rectangle roi;
	
	// private int number;
	
	public SimpleConvolve()
	{}
	
	/**
	 * Start the convolver with an array of images
	 * 
	 * @param imageVector
	 * @param conv
	 * @param roi
	 * @param dim
	 * @param parent
	 */
	public ImagePlus start(ImagePlus image, ImagePlus conv, Rectangle roi)
	{
		this.conv = conv;
		this.roi = roi;
		this.image = image;
		
		// Do the work
		result = analyzeImage();
		Logs.log("Convolution finished... result saved ", 1, this);
		// result.show();
		
		return result;
	}
	
	// ------------------------------------
	// ------ CALCULATION UTILITIES -------
	// ------------------------------------
	
	// analyze and image, find the maxima, and save these points
	public ImagePlus analyzeImage()
	{// launch the calculation
	 // ImageProcessor imp = this.image.getProcessor();
	 // float[][] imageMatrix = imp.getFloatArray();
		
		// time the execution of convolution 1
		StopWatch stopwatch = new StopWatch();
		stopwatch.start();
		
		// do the convolution
		// smallImageWidth = conv.getWidth();
		// smallImageHeight = conv.getHeight();
		// imageSmall = conv.getProcessor().getFloatArray();
		// result = convolutionMatrix(imageMatrix, imageSmall);
		
		result = this.makeConvolution(image, conv);
		
		stopwatch.stop();
		Logs.log("First convolution took " + stopwatch.toString(), 1, this);
		
		// refresh();
		return result;
	}
	
	public ImageProcessor convolve(ImageProcessor arg, ImageProcessor kernel)
	{
		Logs.log("Averaging typical cell image ", 1, this);
		// int w = arg.getWidth() - smallImageWidth;
		// int h = arg.getHeight() - smallImageHeight;
		int w = arg.getWidth();
		int h = arg.getHeight();
		
		// normalize the kernel
		kernel = this.getAdjustedImage(kernel);
		
		// pass the small image on every location x and y inside the ROI
		// float maximum = 0;
		int startY = Math.max((int) roi.getY(), 0);
		int endY = Math.min(startY + (int) roi.getHeight(), h - smallImageHeight);
		int startX = Math.max((int) roi.getX(), 0);
		int endX = Math.min(startX + (int) roi.getWidth(), w - smallImageWidth);
		
		Logs.log("Convolving ... ", 1, this);
		ImageProcessor result = new FloatProcessor(w, h);
		for (int i = startY, len = endY; i < len; i++)
		{
			for (int j = startX, len2 = endX; j < len2; j++)
			{
				float value = scoreConvolution(arg, kernel, i, j);
				result.putPixelValue(j, i, value);
			}
		}
		
		ImagePlus finIm = new ImagePlus("", result);
		finIm.show();
		
		return result;
	}
	
	public ImageProcessor getAdjustedImage(ImageProcessor raw)
	{
		ImagePlus im = new ImagePlus("", raw);
		ImageStatistics stats = im.getStatistics();
		double mean = stats.mean;
		raw.add(-mean);
		return raw;
	}
	
	public float scoreConvolution(ImageProcessor arg, ImageProcessor kernel, int i, int j)
	{
		ImageProcessor result = arg.duplicate();
		
		// set the roi in the big image
		result.setRoi(new Rectangle(i, j, smallImageWidth, smallImageHeight));
		
		// crop the big image to an image the same size as the convolution image
		result = result.crop();
		
		// adjust the image to have a mean intensity level of 0
		// result = getAdjustedImage(result);
		
		// do the convolution
		result.copyBits(kernel, i, j, Blitter.MULTIPLY);
		
		// measure the mean of the resulting image
		ImagePlus im = new ImagePlus("", result);
		ImageStatistics stats = im.getStatistics();
		double mean = stats.mean;
		
		return (float) mean;
	}
	
	// convolve a matrix over a larger one
	public ImagePlus convolutionMatrix(float[][] fimage, float[][] simage)
	{
		Logs.log("Averaging typical cell image ", 1, this);
		int w = fimage.length - smallImageWidth;
		int h = fimage[0].length - smallImageHeight;
		// Logs.log("  ** Tracker: image size "+w+", "+h);
		float[][] fesult = new float[w][h];
		
		// find the average intensity of simage
		float average2 = 0;
		for (int i = 0, len = simage.length; i < len; i++)
		{
			for (int j = 0, lenj = simage[0].length; j < lenj; j++)
			{
				average2 = average2 + simage[j][i];
			}
		}
		average2 = average2 / (simage.length * simage.length);
		
		// pass the small image on every location x and y inside the ROI
		float maximum = 0;
		int startY = (int) roi.getY();
		int endY = Math.min(startY + (int) roi.getHeight(), h);
		int startX = (int) roi.getX();
		int endX = Math.min(startX + (int) roi.getWidth(), w);
		
		Logs.log("Convolving ... ", 1, this);
		for (int i = startY, len = endY; i < len; i++)
		{
			// imagePanel.displayLine(startX,endX+smallImageWidth,i);
			for (int j = startX, len2 = endX; j < len2; j++)
			{
				fesult[j][i] = scoreConvolution(j, i, fimage, simage, average2);
				maximum = (fesult[j][i] > maximum) ? fesult[j][i] : maximum;
				// imagePanel.displayPoint(new Point(j,i));
				// Logs.log("convolving point x="+i+", y="+j);
			}
		}
		
		ByteProcessor byp = new ByteProcessor(w, h);
		for (int i = 0, len = h; i < len; i++)
		{
			for (int j = 0, len2 = w; j < len2; j++)
			{
				byp.putPixel(j, i, (int) (127 * Math.max(fesult[j][i], 0) / maximum));
			}
		}
		result = new ImagePlus("result convolution", byp);
		// result.show();
		// Utility.saveFigure(result, imageSave);
		
		return result;
	}
	
	// convolve a matrix over a larger one
	public ImagePlus rapidConvolutionMatrix(float[][] fimage, float[][] simage)
	{
		// Creating kernel :
		int w = fimage.length - smallImageWidth;
		int h = fimage[0].length - smallImageHeight;
		float[][] fesult = new float[w][h];
		
		// find the average intensity of simage
		float average2 = 0;
		for (int i = 0, len = simage.length; i < len; i++)
		{
			for (int j = 0, lenj = simage[0].length; j < lenj; j++)
			{
				average2 = average2 + simage[j][i];
			}
		}
		average2 = average2 / (simage.length * simage.length);
		
		// pass the small image on every location x and y inside the ROI
		float maximum = 0;
		int startY = (int) roi.getY();
		int endY = Math.min(startY + (int) roi.getHeight(), h);
		int startX = (int) roi.getX();
		int endX = Math.min(startX + (int) roi.getWidth(), w);
		
		Logs.log("Convolving ... ", 1, this);
		for (int i = startY, len = endY; i < len; i++)
		{
			// imagePanel.displayLine(startX,endX+smallImageWidth,i);
			for (int j = startX, len2 = endX; j < len2; j++)
			{
				fesult[j][i] = scoreConvolution(j, i, fimage, simage, average2);
				maximum = (fesult[j][i] > maximum) ? fesult[j][i] : maximum;
				// imagePanel.displayPoint(new Point(j,i));
				// Logs.log("convolving point x="+i+", y="+j);
			}
		}
		
		ByteProcessor byp = new ByteProcessor(w, h);
		for (int i = 0, len = h; i < len; i++)
		{
			for (int j = 0, len2 = w; j < len2; j++)
			{
				byp.putPixel(j, i, (int) (127 * Math.max(fesult[j][i], 0) / maximum));
			}
		}
		result = new ImagePlus("result convolution", byp);
		// result.show();
		// Utility.saveFigure(result, imageSave);
		
		return result;
	}
	
	// find the score of a convolution
	public float scoreConvolution(int x, int y, float[][] fimage, float[][] simage, float average2)
	{
		// Logs.log("  ** Tracker: Convolving position "+x+", "+y);
		// find the average intensity of the zone covering the image fimage
		float average = 0;
		for (int i = 0, len = simage.length; i < len; i++)
		{
			for (int j = 0, lenj = simage[0].length; j < lenj; j++)
			{
				average = average + fimage[x + j][y + i];
			}
		}
		average = average / (simage.length * simage.length);
		
		// convolve the images while correcting for average intesity differences
		float score = 0;
		for (int i = 0, len = simage.length; i < len; i++)
		{
			for (int j = 0; j < len; j++)
			{
				score = score + (fimage[x + j][y + i] - average) * (simage[j][i] - average2);
			}
		}
		return score;
	}
	
	/**
	 * Create the kernel for the convolution
	 * 
	 * @param convImage
	 * @return
	 */
	public static Kernel makeConvolutionKernel(float[][] convImage, ImagePlus convIm, ImagePlus im)
	{
		int w = convImage.length;
		int h = convImage[0].length;
		float[] linImage = new float[w * h];
		
		// normalize the input image
		ImageStatistics stats = convIm.getStatistics();
		double mean = stats.mean;
		double maxConv = 0;
		
		// Normalize the convolution image to get a mean of 0
		for (int i = 0, len = w; i < len; i++)
		{
			for (int j = 0, len2 = h; j < len2; j++)
			{
				linImage[j * w + i] = (convImage[i][j] - (float) mean);
				maxConv = maxConv + linImage[j * w + i] * linImage[j * w + i];
			}
		}
		
		// Scale the convolution image to get a maximum of 255 intesity in final
		for (int i = 0, len = linImage.length; i < len; i++)
		{
			linImage[i] = 255 * linImage[i] / ((float) maxConv);
		}
		
		// Make kernel
		Kernel result = new Kernel(w, h, linImage);
		return result;
	}
	
	/**
	 * Faster convolution operation using implemented functions of awt.image
	 * 
	 * @param im
	 * @param convIm
	 * @return
	 */
	public ImagePlus makeConvolution(ImagePlus im, ImagePlus convIm)
	{
		// Make bufferedimage source
		BufferedImage bim = ImageUtility.imagePlus2BufferedImage(im);
		
		// make the kernel
		FloatProcessor convFloat = (FloatProcessor) convIm.getProcessor().convertToFloat();
		float[][] convMat = convFloat.getFloatArray();
		Kernel kernel = makeConvolutionKernel(convMat, convIm, im);
		
		// Make the convolution operator
		ConvolveOp convolver = new ConvolveOp(kernel);
		
		// Make the destination image
		ColorModel model = im.getProcessor().getColorModel();
		BufferedImage out = convolver.createCompatibleDestImage(bim, model);
		
		// Do the convolution
		out = convolver.filter(bim, out);
		
		// Transform the buffered image into imageplus and return
		ImagePlus result = new ImagePlus("", out);
		return result;
	}
	
	/**
	 * Return the score of the convolution of image CELLIMAGE in image IMAGE at location P
	 * 
	 * @param image
	 * @param cellImage
	 * @param p
	 * @return
	 */
	public static float scoreConvolution(ImagePlus image, ImagePlus cellImage, int topx, int topy)
	{
		ImageProcessor result = image.getProcessor().duplicate();
		
		// set the roi in the big image
		result.setRoi(new Rectangle(topx, topy, cellImage.getWidth(), cellImage.getHeight()));
		
		// crop the big image to an image the same size as the convolution image
		result = result.crop();
		
		// do the convolution
		result.copyBits(cellImage.getProcessor(), 0, 0, Blitter.MULTIPLY);
		
		return 0;
	}
}