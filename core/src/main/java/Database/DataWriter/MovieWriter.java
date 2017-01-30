package Database.DataWriter;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXDataSingle;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.SingleUserDatabase.JEXWriter;
import function.JEXCrunchable;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import image.roi.ROIPlus;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.TreeMap;

import logs.Logs;
import miscellaneous.FileUtility;

import org.monte.media.Format;
import org.monte.media.VideoFormatKeys;
import org.monte.media.avi.AVIWriter;
import org.monte.media.math.Rational;
import org.monte.media.quicktime.QuickTimeWriter;

import tables.DimensionMap;

public class MovieWriter implements ImageObserver {
	
	public static final String AVI_JPEG = VideoFormatKeys.ENCODING_AVI_MJPG, AVI_PNG = VideoFormatKeys.ENCODING_AVI_PNG;
	public static final Format QT_JPEG = QuickTimeWriter.VIDEO_JPEG, QT_PNG = QuickTimeWriter.VIDEO_PNG, QT_RAW = QuickTimeWriter.VIDEO_RAW, QT_ANIMATION = QuickTimeWriter.VIDEO_ANIMATION;
	
	// Movie writing variables
	private boolean imageLoaded = false;
	private boolean imageLoadingError = false;
	
	/**
	 * Make a movie data object with a single movie inside
	 * 
	 * @param objectName
	 * @param filePath
	 * @return data
	 */
	public static JEXData makeMovieObject(String objectName, String filePath)
	{
		JEXData data = new JEXData(JEXData.MOVIE, objectName);
		JEXDataSingle ds = FileWriter.saveFileDataSingle(filePath);
		if(ds == null)
		{
			return null;
		}
		data.addData(new DimensionMap(), ds);
		return data;
	}
	
	/**
	 * Returns the path where this movie object is saved. Compression method is either MovieWriter.QT_JPEG, QT_PNG, QT_RAW, or QT_ANIMATION. The 'compression' parameter is only used for JPEG format.
	 * 
	 * @param images
	 * @return
	 */
	public String makeQuickTimeMovie(JEXData images, JEXData cropROI, int imageBinning, Format format, int imagesPerSecond, Color textColor, JEXCrunchable optionalCruncherForCanceling)
	{
		return this.makeQuickTimeMovie(images, cropROI, imageBinning, format, imagesPerSecond, 0, 0, null, 0, 0, 0, textColor, optionalCruncherForCanceling);
	}
	
	/**
	 * Returns the path where this movie object is saved. Compression method is either MovieWriter.QT_JPEG, QT_PNG, QT_RAW, or QT_ANIMATION. The 'compression' parameter is only used for JPEG format.
	 * 
	 * @param images
	 * @return
	 */
	public String makeQuickTimeMovie(JEXData images, JEXData cropROI, int imageBinning, Format format, int imagesPerSecond, double startTime, double interval, String units, int digits, int fontSize, int inset, Color textColor, JEXCrunchable optionalCruncherForCanceling)
	{
		if(images == null || !images.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return null;
		}
		TreeMap<DimensionMap,String> imset = ImageReader.readObjectToImagePathTable(images);
		
		boolean roiSupplied = true;
		boolean singletonRoi = false;
		Roi crop = null;
		if(cropROI == null || !cropROI.getTypeName().getType().equals(JEXData.ROI))
		{
			roiSupplied = false;
		}
		TreeMap<DimensionMap,ROIPlus> roiset = new TreeMap<DimensionMap,ROIPlus>();
		if(roiSupplied)
		{
			roiset = RoiReader.readObjectToRoiMap(cropROI);
			if(roiset.size() == 1)
			{
				singletonRoi = true;
				crop = roiset.firstEntry().getValue().getRoi();
			}
		}
		
		String newMoviePath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("mov");
		
		try
		{
			QuickTimeWriter writer = new QuickTimeWriter(new File(newMoviePath));
			
			// main loop
			// add each image one by one
			int len = imset.size();
			
			double time = 0;
			DecimalFormat formatter = null;
			Font font = null;
			if(interval > 0)
			{
				time = startTime;
				String formatString = "0";
				for (int i = 0; i < digits; i++)
				{
					if(i == 0 && digits > 0)
					{
						formatString = formatString + ".";
					}
					formatString = formatString + "0";
				}
				formatter = new DecimalFormat(formatString);
				font = new Font("Times", Font.PLAIN, fontSize);
			}
			
			int k = 0;
			for (DimensionMap map : imset.keySet())
			{
				if(optionalCruncherForCanceling != null && optionalCruncherForCanceling.isCanceled())
				{
					return null;
				}
				ImagePlus imk = new ImagePlus(imset.get(map));
				ColorProcessor imp = (ColorProcessor) imk.getProcessor().convertToRGB();
				
				if(roiSupplied)
				{
					if(singletonRoi)
					{
						imp.setRoi(crop);
					}
					else
					{
						ROIPlus temp = roiset.get(map);
						crop = temp.getRoi();
						imp.setRoi(crop);
					}
				}
				
				if(imageBinning != 1)
				{
					imp = (ColorProcessor) imp.resize((imp.getWidth() / imageBinning));
				}
				
				if(interval > 0)
				{
					String timeStamp = "" + formatter.format(time) + " " + units;
					imp.setFont(font);
					imp.setColor(textColor);
					imp.drawString(timeStamp, inset, imp.getHeight() - inset);
				}
				
				if(k == 0)
				{
					ImagePlus sample = new ImagePlus("Sample Frame", imp);
					try
					{
						FileUtility.openFileDefaultApplication(JEXWriter.saveImage(sample));
						sample.flush();
						sample = null;
					}
					catch (Exception e)
					{
						e.printStackTrace();
						if(sample != null)
						{
							sample.flush();
							sample = null;
						}
					}
				}
				
				BufferedImage bimage = this.getAndWaitForBufferedImage(imp);
				if(k == 0)
				{
					format = format.append(new Format(//
					VideoFormatKeys.DepthKey, 24, //
					VideoFormatKeys.QualityKey, 1f, //
					VideoFormatKeys.FrameRateKey, new Rational(imagesPerSecond, 1), //
					VideoFormatKeys.WidthKey, imp.getWidth(), //
					VideoFormatKeys.HeightKey, imp.getHeight()));
					writer.addTrack(format);// .addVideoTrack(format,
					// imagesPerSecond,
					// bimage.getWidth(),
					// bimage.getHeight());
				}
				
				Logs.log("Writing movie frame " + k + " of " + len, 0, MovieWriter.class.getSimpleName());
				writer.write(0, bimage, 1);
				imp = null;
				imk.flush();
				imk = null;
				time = time + interval;
				k = k + 1;
			}
			
			writer.close();
			return newMoviePath;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	private BufferedImage getAndWaitForBufferedImage(ColorProcessor imp)
	{
		this.imageLoadingError = false;
		this.imageLoaded = false;
		BufferedImage bi = new BufferedImage(imp.getWidth(), imp.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) bi.getGraphics();
		if(!g.drawImage(imp.createImage(), 0, 0, this))
		{
			this.waitForImage(bi);
		}
		return bi;
	}
	
	/** Used by ImagePlus to monitor loading of images. */
	@Override
	public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h)
	{
		if((flags & ERROR) != 0)
		{
			this.imageLoadingError = true;
			return false;
		}
		this.imageLoaded = (flags & (ALLBITS | FRAMEBITS | ABORT)) != 0;
		return !this.imageLoaded;
	}
	
	private void waitForImage(Image img)
	{
		while (!this.imageLoaded && !this.imageLoadingError)
		{
			// IJ.showStatus(imageUpdateY+" "+imageUpdateW);
			Logs.log("Waiting for image", 0, this);
			try
			{
				Thread.sleep(30);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns the path where this movie object is saved. The format key is either MovieWriter.AVI_JPEG or AVI_PNG
	 * 
	 * @param images
	 * @return
	 */
	public String makeAVIMovie(JEXData images, JEXData cropROI, int imageBinning, String encoding, int imagesPerSecond, Color textColor, JEXCrunchable optionalCruncherForCanceling)
	{
		return this.makeAVIMovie(images, cropROI, imageBinning, encoding, imagesPerSecond, 0, 0, null, 0, 0, 0, textColor, optionalCruncherForCanceling);
	}
	
	/**
	 * Returns the path where this movie object is saved. The format key is either MovieWriter.AVI_JPEG or AVI_PNG
	 * 
	 * @param images
	 * @return
	 */
	public String makeAVIMovie(JEXData images, JEXData cropROI, int imageBinning, String encoding, int imagesPerSecond, double startTime, double interval, String units, int digits, int fontSize, int inset, Color textColor, JEXCrunchable optionalCruncherForCanceling)
	{
		if(images == null || !images.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return null;
		}
		TreeMap<DimensionMap,String> imset = ImageReader.readObjectToImagePathTable(images);
		
		boolean roiSupplied = true;
		boolean singletonRoi = false;
		Roi crop = null;
		if(cropROI == null || !cropROI.getTypeName().getType().equals(JEXData.ROI))
		{
			roiSupplied = false;
		}
		TreeMap<DimensionMap,ROIPlus> roiset = new TreeMap<DimensionMap,ROIPlus>();
		if(roiSupplied)
		{
			roiset = RoiReader.readObjectToRoiMap(cropROI);
			if(roiset.size() == 1)
			{
				singletonRoi = true;
				crop = roiset.firstEntry().getValue().getRoi();
			}
		}
		
		String newMoviePath = JEXWriter.getDatabaseFolder() + File.separator + JEXWriter.getUniqueRelativeTempPath("avi");
		
		try
		{
			int len = imset.size();
			AVIWriter writer = new AVIWriter(new File(newMoviePath));
			
			double time = 0;
			DecimalFormat formatter = null;
			Font font = null;
			if(interval > 0)
			{
				time = startTime;
				String formatString = "0";
				for (int i = 0; i < digits; i++)
				{
					if(i == 0 && digits > 0)
					{
						formatString = formatString + ".";
					}
					formatString = formatString + "0";
				}
				formatter = new DecimalFormat(formatString);
				font = new Font("Times", Font.PLAIN, fontSize);
			}
			
			// main loop
			// add each image one by one
			int k = 0;
			for (DimensionMap map : imset.keySet())
			{
				if(optionalCruncherForCanceling != null && optionalCruncherForCanceling.isCanceled())
				{
					return null;
				}
				ImagePlus imk = new ImagePlus(imset.get(map));
				ImageProcessor imp = imk.getProcessor().convertToRGB();
				
				if(roiSupplied)
				{
					if(singletonRoi)
					{
						imp.setRoi(crop);
					}
					else
					{
						ROIPlus temp = roiset.get(map);
						crop = temp.getRoi();
						imp.setRoi(crop);
					}
				}
				
				if(imageBinning != 1)
				{
					imp = imp.resize((imk.getWidth() / imageBinning));
				}
				
				if(interval > 0)
				{
					String timeStamp = "" + formatter.format(time) + " " + units;
					imp.setFont(font);
					imp.setColor(textColor);
					imp.drawString(timeStamp, inset, imp.getHeight() - inset);
				}
				
				if(k == 0)
				{
					ImagePlus sample = new ImagePlus("Sample Frame", imp);
					try
					{
						FileUtility.openFileDefaultApplication(JEXWriter.saveImage(sample));
						sample.flush();
						sample = null;
					}
					catch (Exception e)
					{
						e.printStackTrace();
						if(sample != null)
						{
							sample.flush();
							sample = null;
						}
					}
				}
				
				BufferedImage bimage = imp.getBufferedImage();
				if(k == 0)
				{
					Format format = new Format(VideoFormatKeys.EncodingKey, encoding, VideoFormatKeys.DepthKey, 24, //
					VideoFormatKeys.QualityKey, 0.1f, VideoFormatKeys.MediaTypeKey, VideoFormatKeys.MediaType.VIDEO, //
					VideoFormatKeys.FrameRateKey, new Rational(imagesPerSecond, 1), VideoFormatKeys.WidthKey, bimage.getWidth(), VideoFormatKeys.HeightKey, bimage.getHeight());
					writer.addTrack(format);
				}
				
				Logs.log("Writing movie frame " + k + " of " + len, 0, MovieWriter.class.getSimpleName());
				writer.write(0, bimage, 1);
				imp = null;
				imk.flush();
				imk = null;
				time = time + interval;
				k = k + 1;
			}
			
			writer.close();
			return newMoviePath;
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
}
