package function.roitracker;

import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import jex.statics.JEXStatics;
import jex.utilities.ROIUtility;
import logs.Logs;

import org.monte.media.quicktime.QuickTimeOutputStream;

public class RoiTrackMovieMaker {
	
	// Statics
	public static Color DEFAULT_ROI_COLOR = Color.YELLOW;
	public static Color DEFAULT_TRACK_COLOR = Color.RED;
	
	// Variables
	private String path;
	private List<String> images;
	private List<RoiTrack> tracks;
	
	// Options
	private boolean showTrail = false;
	private int binning = 1;
	private int radius = 3;
	private float compression = (float) 0.7;
	private int frameRate = 7;
	private Color trackColor = null;
	private Color roiColor = null;
	
	public RoiTrackMovieMaker(String path, List<String> images, List<RoiTrack> tracks)
	{
		this.path = path;
		this.images = images;
		this.tracks = tracks;
	}
	
	// ----------------------------------------------------
	// ------------- SETTINGS and I/O ---------------------
	// ----------------------------------------------------
	
	public void setShowTrail(boolean showTrail)
	{
		this.showTrail = showTrail;
	}
	
	public void setBinning(int binning)
	{
		this.binning = binning;
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
	
	public void setTrackColor(Color color)
	{
		this.trackColor = color;
	}
	
	public void setRoiColor(Color color)
	{
		this.roiColor = color;
	}
	
	// ----------------------------------------------------
	// ----------------- CALCULATION ----------------------
	// ----------------------------------------------------
	
	public String makeMovie()
	{
		File outMovieFile = new File(this.path);
		QuickTimeOutputStream newStream = null;
		try
		{
			QuickTimeOutputStream.VideoFormat format = QuickTimeOutputStream.VideoFormat.values()[0];
			newStream = new QuickTimeOutputStream(outMovieFile, format);
			newStream.setVideoCompressionQuality(this.compression);
			newStream.setTimeScale(this.frameRate);
		}
		catch (IOException e)
		{
			Logs.log("Not possible to create movie ... ", 1, this);
			return null;
		}
		
		// add each image one by one
		int len = this.images.size();
		for (int k = 0; (k < len); k++)
		{
			// get the imagen
			ImagePlus imk = new ImagePlus(this.images.get(k));
			
			// If the show trail flag is on show the trail, else not
			BufferedImage bimage = this.makeImage(k, imk, this.tracks);
			
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
			int percentage = (int) (100 * ((double) k / (double) this.images.size()));
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
		
		return this.path;
	}
	
	/**
	 * Return a buffered image with the tracks printed on the image IMPLUS in a pretty manner
	 * 
	 * @param i
	 * @param implus
	 * @return
	 */
	private BufferedImage makeImage(int i, ImagePlus implus, List<RoiTrack> tracks)
	{
		// Get the image processor
		ImageProcessor imp = implus.getProcessor();
		
		// If the binning is not 1 do binning
		if(this.binning != 1)
		{
			int newWidth = imp.getWidth() / this.binning;
			imp = imp.resize(newWidth);
		}
		
		// Create a buffered image using the default color model
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage bimage = new BufferedImage(imp.getWidth(), imp.getHeight(), type);
		
		// Copy image to buffered image
		Graphics g = bimage.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(imp.getBufferedImage(), 0, 0, null);
		
		// plot current points
		for (int k = 0, len = tracks.size(); k < len; k++)
		{
			// get the track
			RoiTrack track = tracks.get(k);
			
			// add the roi at frame i to the image
			if(!this.showTrail)
			{
				this.addRoiAtFrame(g, i, track);
			}
			else
			{
				this.addRoiAtFrameWithTrail(g, i, track);
			}
			
			System.out.print("..");
		}
		System.out.println("..");
		
		// release the graphics handle
		g.dispose();
		
		return bimage;
	}
	
	private void addRoiAtFrame(Graphics g, int i, RoiTrack track)
	{
		// Get the roi at frame i
		ROIPlus roi = track.getRoi(i);
		
		// If there is no ROI at this frame, return
		if(roi == null)
		{
			return;
		}
		
		// If there is then display it
		// Frist select the color
		Color c = track.getColor(i); // is there a color map
		if(this.roiColor != null)
		{
			g.setColor(this.roiColor);
		}
		else if(c == null)
		{
			g.setColor(DEFAULT_ROI_COLOR);
		}
		else
		{
			g.setColor(c);
		}
		
		// Second plot the roi
		this.plotRoi(g, roi, c);
	}
	
	private void addRoiAtFrameWithTrail(Graphics g, int i, RoiTrack track)
	{
		// Loop through the frames
		ROIPlus oldRoi = null;
		for (int j = 0; j <= i; j++)
		{
			// Get the roi at frame i
			ROIPlus roi = track.getRoi(j);
			
			// If there is no ROI at this frame, return
			if(roi == null)
			{
				continue;
			}
			
			// If there is then display it
			// Frist select the color
			Color c = track.getColor(j);
			if(this.trackColor != null)
			{
				g.setColor(this.trackColor);
			}
			else if(c == null && j < i)
			{
				g.setColor(DEFAULT_TRACK_COLOR);
			}
			else if(c == null && j == i)
			{
				g.setColor(DEFAULT_ROI_COLOR);
			}
			else if(j < i)
			{
				g.setColor(DEFAULT_TRACK_COLOR);
			}
			else
			{
				g.setColor(c);
			}
			
			// Second plot the roi
			// plotRoi(g,roi,c);
			
			// Third plot the line between the rois
			if(oldRoi == null)
			{
				oldRoi = roi;
			}
			
			// Get the points and draw the line
			this.plotLine(g, oldRoi, roi, DEFAULT_TRACK_COLOR);
			oldRoi = roi;
		}
		
		// Only plot the last ROI
		this.addRoiAtFrame(g, i, track);
	}
	
	public void plotRoi(Graphics g, ROIPlus roi, Color c)
	{
		// if it is a rectangle roi
		if(roi.getRoi().getType() == Roi.RECTANGLE)
		{
			// Get the rectangle
			Rectangle r = roi.getRoi().getBounds();
			int x = (int) r.getX() / this.binning;
			int y = (int) r.getY() / this.binning;
			int rw = (int) r.getWidth() / this.binning;
			int rh = (int) r.getHeight() / this.binning;
			
			// Draw the rectangle
			g.drawRect(x, y, rw, rh);
		}
		// If it is a point roi
		else if(roi.getRoi().getType() == Roi.POINT)
		{
			// Get the point
			PointList plist = roi.getPointList();
			
			if(plist == null || plist.size() == 0)
			{
				return;
			}
			
			// Plot the point
			Point p = plist.get(0);
			int x = (int) p.getX() / this.binning;
			int y = (int) p.getY() / this.binning;
			g.fillRect(x - 1, y - 1, 3, 3);
		}
		// if it is a polygon roi
		else if(roi.getRoi().getType() == Roi.OVAL)
		{
			// NOT IMPLEMENTED YET
		}
		// if it is a polygon roi
		else if(roi.getRoi().getType() == Roi.POLYGON)
		{
			PolygonRoi proi = (PolygonRoi) roi.getRoi();
			
			// Display the roi
			java.awt.Polygon poly = proi.getPolygon();
			
			// Get the points
			int[] xpos = poly.xpoints;
			int[] ypos = poly.ypoints;
			
			// Loop through the points and draw them
			int x1 = xpos[0] / this.binning;
			int x2;
			int y1 = ypos[0] / this.binning;
			int y2;
			for (int z = 1; z < xpos.length - 1; z++)
			{
				// Get point
				x2 = xpos[z] / this.binning;
				y2 = ypos[z] / this.binning;
				
				// Draw point and line
				g.fillRect(x2 - 1, y2 - 1, 2, 2);
				g.drawLine(x1, y1, x2, y2);
				
				// Update points
				x1 = x2;
				y1 = y2;
			}
		}
	}
	
	public void plotLine(Graphics g, ROIPlus roi1, ROIPlus roi2, Color c)
	{
		// Get the points
		Point p1 = ROIUtility.getRectangleCenter(roi1);
		Point p2 = ROIUtility.getRectangleCenter(roi2);
		
		// Scale the points
		int x1 = (int) p1.getX() / this.binning;
		int y1 = (int) p1.getY() / this.binning;
		int x2 = (int) p2.getX() / this.binning;
		int y2 = (int) p2.getY() / this.binning;
		
		// Draw the line
		g.setColor(c);
		g.fillRect(x1 - this.radius / 2, y1 - this.radius / 2, this.radius, this.radius);
		g.fillRect(x2 - this.radius / 2, y2 - this.radius / 2, this.radius, this.radius);
		g.drawLine(x1, y1, x2, y2);
	}
	
}
