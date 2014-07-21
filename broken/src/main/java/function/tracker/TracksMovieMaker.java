package function.tracker;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import image.roi.Trajectory;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jex.statics.JEXStatics;
import logs.Logs;

import org.monte.media.quicktime.QuickTimeOutputStream;

public class TracksMovieMaker {
	
	// Variables
	private String path;
	private List<String> images;
	private Trajectory[] trajectories;
	
	// Options
	private boolean showTrail = false;
	private int imageBin = 1;
	private int trackBin = 1;
	private int radius = 3;
	private float compression = (float) 0.7;
	private int frameRate = 7;
	
	public TracksMovieMaker(String path, List<String> images, Trajectory[] trajectories)
	{
		this.path = path;
		this.images = images;
		this.trajectories = trajectories;
	}
	
	public TracksMovieMaker(String path, List<String> images, List<Trajectory> trajs)
	{
		this.path = path;
		this.images = images;
		this.trajectories = new Trajectory[trajs.size()];
		
		int index = 0;
		for (Trajectory t : trajs)
		{
			this.trajectories[index] = t;
			index++;
		}
	}
	
	public TracksMovieMaker(String path, String[] images, List<Trajectory> trajs)
	{
		this.path = path;
		this.images = new ArrayList<String>(0);
		this.trajectories = new Trajectory[trajs.size()];
		
		int index = 0;
		for (Trajectory t : trajs)
		{
			this.trajectories[index] = t;
			index++;
		}
		
		index = 0;
		for (String str : images)
		{
			this.images.add(str);
			index++;
		}
	}
	
	public void setShowTrail(boolean showTrail)
	{
		this.showTrail = showTrail;
	}
	
	public void setImageBinning(int imageBin)
	{
		this.imageBin = imageBin;
	}
	
	public void setTrackBinning(int trackBin)
	{
		this.trackBin = trackBin;
	}
	
	public void setCompression(float compression)
	{
		this.compression = compression;
	}
	
	public void setCellRadius(int radius)
	{
		this.radius = radius;
	}
	
	public void setFrameRate(int frameRate)
	{
		this.frameRate = frameRate;
	}
	
	public String makeMovie()
	{
		File outMovieFile = new File(path);
		QuickTimeOutputStream newStream = null;
		try
		{
			QuickTimeOutputStream.VideoFormat format = QuickTimeOutputStream.VideoFormat.values()[0];
			newStream = new QuickTimeOutputStream(outMovieFile, format);
			newStream.setVideoCompressionQuality(compression);
			newStream.setTimeScale(frameRate);
		}
		catch (IOException e)
		{
			Logs.log("Not possible to create movie ... ", 1, this);
			return null;
		}
		
		// add each image one by one
		for (int k = 0, len = images.size(); (k < len); k++)
		{
			ImagePlus imk = new ImagePlus(images.get(k));
			
			// If the show trail flag is on show the trail, else not
			BufferedImage bimage = null;
			if(this.showTrail)
			{
				bimage = trackImageWithTrail(k, imk, trajectories);
			}
			else
			{
				bimage = trackImage(k, imk, trajectories);
			}
			
			try
			{
				newStream.writeFrame(bimage, 1);
				Logs.log("Writing frame " + k, 1, this);
			}
			catch (IOException e)
			{
				Logs.log("Not possible to write frame " + k, 1, this);
			}
			
			// Status bar
			int percentage = (int) (100 * ((double) k / (double) images.size()));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		try
		{
			newStream.finish();
			newStream.close();
		}
		catch (IOException e)
		{
			Logs.log("Not possible to finalize movie ", 1, this);
		}
		Logs.log("Tracks movie saved in " + outMovieFile.getPath(), 1, this);
		
		return path;
	}
	
	/**
	 * Return a buffered image with the tracks printed on the image IMPLUS in a pretty manner
	 * 
	 * @param i
	 * @param implus
	 * @return
	 */
	private BufferedImage trackImage(int i, ImagePlus implus, Trajectory[] trajectories)
	{
		ImageProcessor imp = implus.getProcessor();
		imp = imp.resize((implus.getWidth() / imageBin));
		
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(imp.getWidth(), imp.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(imp.getBufferedImage(), 0, 0, null);
		
		// plot current points
		for (int k = 0, len = trajectories.length; k < len; k++)
		{
			
			Trajectory trajK = trajectories[k];
			g.setColor(Color.YELLOW);
			
			// Get the first point of the trajectory
			int frame1 = trajK.initial();
			Point f2 = trajK.getPoint(frame1);
			Point s2 = f2;
			
			// If the first point is before the current index I or is null break
			if(frame1 > i)
				continue;
			
			while (s2 != null)
			{
				Point toDisplay1 = new Point((int) (f2.getX() / trackBin), (int) (f2.getY() / trackBin));
				Point toDisplay2 = new Point((int) (s2.getX() / trackBin), (int) (s2.getY() / trackBin));
				
				g.fillOval(toDisplay1.x - 1, toDisplay1.y - 1, 3, 3);
				g.drawLine(toDisplay1.x, toDisplay1.y, toDisplay2.x, toDisplay2.y);
				
				f2 = s2;
				frame1 = trajK.next(frame1);
				s2 = trajK.getPoint(frame1);
				if(frame1 > i)
					s2 = null;
			}
			
			Point currentP = trajK.getPoint(i);
			if(currentP != null)
			{
				g.setColor(Color.red);
				g.drawRect(currentP.x - radius, currentP.y - radius, radius * 2 + 1, radius * 2 + 1);
			}
		}
		
		g.dispose();
		
		return bimage;
	}
	
	/**
	 * Return a buffered image with the tracks printed on the image IMPLUS in a pretty manner
	 * 
	 * @param i
	 * @param implus
	 * @return
	 */
	private BufferedImage trackImageWithTrail(int i, ImagePlus implus, Trajectory[] trajectories)
	{
		ImageProcessor imp = implus.getProcessor();
		imp = imp.resize((implus.getWidth() / imageBin));
		
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(imp.getWidth(), imp.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(imp.getBufferedImage(), 0, 0, null);
		
		// plot current points
		for (int k = 0, len = trajectories.length; k < len; k++)
		{
			// Get the trajectory and all the points after index K
			Trajectory trajK = trajectories[k];
			List<Point> trajAllPoints = trajK.getPointsAfter(i);
			
			// Print the trail
			if(this.showTrail)
			{
				for (int kk = 0, length = trajAllPoints.size(); kk < length; kk++)
				{
					Point newP = trajAllPoints.get(kk);
					Point toDisplay = new Point((int) (newP.getX() / trackBin), (int) (newP.getY() / trackBin));
					
					int shade = 255 * (kk) / (length);
					Color c = new Color(shade, shade, 255);
					g.setColor(c);
					g.drawRect(toDisplay.x - 1, toDisplay.y - 1, 3, 3);
				}
			}
			
			g.setColor(Color.YELLOW);
			int frame1 = trajK.initial();
			Point f2 = trajK.getPoint(frame1);
			Point s2 = f2;
			
			while (s2 != null)
			{
				Point toDisplay1 = new Point((int) (f2.getX() / trackBin), (int) (f2.getY() / trackBin));
				Point toDisplay2 = new Point((int) (s2.getX() / trackBin), (int) (s2.getY() / trackBin));
				
				g.fillOval(toDisplay1.x - 1, toDisplay1.y - 1, 3, 3);
				g.drawLine(toDisplay1.x, toDisplay1.y, toDisplay2.x, toDisplay2.y);
				
				f2 = s2;
				frame1 = trajK.next(frame1);
				s2 = trajK.getPoint(frame1);
				if(frame1 > i)
					s2 = null;
			}
			
			Point currentP = trajK.getPoint(i);
			if(currentP != null)
			{
				g.setColor(Color.red);
				g.drawRect(currentP.x - radius, currentP.y - radius, radius * 2 + 1, radius * 2 + 1);
			}
		}
		
		g.dispose();
		
		return bimage;
	}
}
