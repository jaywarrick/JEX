/*
 * VIDEO LIBRARY FOR IMAGEJ
 * Author: Wilhelm Burger (wilbur@ieee.org)
 * Source: www.fh-hagenberg.at/staff/burger/imagej/
 */

package movieio.jmf;

import ij.IJ;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.io.File;

import javax.media.Buffer;
import javax.media.ConfigureCompleteEvent;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Duration;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.PrefetchCompleteEvent;
import javax.media.RealizeCompleteEvent;
import javax.media.ResourceUnavailableEvent;
import javax.media.Time;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.util.BufferToImage;

public class JMFMovieReader extends MovieReader implements ControllerListener {
	
	Player player;
	FramePositioningControl fpc;
	FrameGrabbingControl fgc;
	Object waitSync = new Object();
	boolean stateTransitionOK = true;
	BufferToImage frameConverter;
	
	// ---- methods defined by MovieOpener interface: ----
	
	public static MovieReader openMovie(String path)
	{
		JMFMovieReader m = new JMFMovieReader();
		if(m.open(path))
			return m;
		else
			return null;
	}
	
	@SuppressWarnings("unused")
	public boolean open(String path)
	{
		String url = "file://" + path;
		MediaLocator ml = new MediaLocator(url);
		if(ml == null)
		{
			error("Cannot build media locator from: " + url);
			return false;
		}
		
		// remember file path and name vars
		File file = new File(path);
		this.path = file.getAbsolutePath();
		this.name = file.getName();
		
		// Create a DataSource given the media locator.
		DataSource ds;
		IJ.showStatus("creating JMF data source");
		try
		{
			ds = Manager.createDataSource(ml);
		}
		catch (Exception e)
		{
			error("Cannot create DataSource from: " + ml);
			return false;
		}
		
		IJ.showStatus("opening: " + ds.getContentType());
		try
		{
			player = Manager.createPlayer(ds);
		}
		catch (Exception e)
		{
			error("Failed to create a player from the given DataSource:\n \n" + e.getMessage());
			return false;
		}
		
		IJ.showStatus("adding listener");
		// attach this object as an event listener to the player
		player.addControllerListener(this);
		player.realize();
		if(!waitForState(Controller.Realized))
		{
			error("Failed to realize the JMF player.");
			return false;
		}
		
		// Try to retrieve a FramePositioningControl from the player.
		fpc = (FramePositioningControl) player.getControl("javax.media.control.FramePositioningControl");
		if(fpc == null)
		{
			error("The player does not support FramePositioningControl.");
			return false;
		}
		
		// Try to retrieve a FrameGrabbingControl from the player.
		fgc = (FrameGrabbingControl) player.getControl("javax.media.control.FrameGrabbingControl");
		if(fgc == null)
		{
			error("The player does not support FrameGrabbingControl.");
			return false;
		}
		
		// ------------------------------
		totalFrames = UNKNOWN;
		Time duration = player.getDuration();
		if(duration != Duration.DURATION_UNKNOWN)
		{
			// IJ.log("Duration: (sec) " + duration.getSeconds());
			int lastFrame = fpc.mapTimeToFrame(duration);
			if(lastFrame == FramePositioningControl.FRAME_UNKNOWN)
			{
				if(IJ.debugMode)
					IJ.log("The FramePositioningControl does not support mapTimeToFrame.");
			}
			else
				totalFrames = lastFrame + 1;
		}
		else
		{
			IJ.log("Movie duration: unknown");
		}
		
		rewind();
		
		// -- determine dimensions ---
		Buffer frame = fgc.grabFrame();
		VideoFormat format = (VideoFormat) frame.getFormat();
		Dimension dim = format.getSize();
		width = dim.width;
		height = dim.height;
		frameConverter = new BufferToImage(format);
		
		return true;
	}
	
	public void close()
	{
		player.close(); // to stop JMF threads!
		player.deallocate();
		player = null;
		fpc = null;
		fgc = null;
		frameConverter = null;
		System.gc();
	}
	
	public boolean rewind()
	{
		currentFrame = fpc.seek(0);
		// int startframe = fpc.mapTimeToFrame(new Time(0));
		// IJ.log("rewind: current frame = " + currentFrame);
		// Prefetch the player.
		player.prefetch();
		if(!waitForState(Controller.Prefetched))
		{
			error("Failed to prefetch the player.");
			return false;
		}
		return true;
	}
	
	public ImageProcessor createImageProcessor()
	{
		return new ColorProcessor(width, height);
	}
	
	public int getNextFrame(ImageProcessor ip)
	{
		// IJ.log("getNext: currentFrame = " + currentFrame);
		if(currentFrame >= totalFrames)
		{
			// IJ.log("finished " + totalFrames);
			return -1;
		}
		Buffer frame = fgc.grabFrame();
		if(frame != null)
		{
			Image img = frameConverter.createImage(frame);
			int[] ijPixels = (int[]) ip.getPixels();
			PixelGrabber pg = new PixelGrabber(img, 0, 0, width, height, ijPixels, 0, width);
			try
			{
				pg.grabPixels();
			}
			catch (Exception e)
			{
				// IJ.log("could not grab image for frame " + currentFrame);
				return -1;
			}
			int thisFrameNo = currentFrame;
			currentFrame = currentFrame + fpc.skip(1);
			// currentFrame = fpc.seek(currentFrame + 1); // Wilbur: performance
			// problem?
			// IJ.log("thisFrameNo: " + thisFrameNo);
			return thisFrameNo;
		}
		else
		{
			// IJ.log("no Buffer for frame " + currentFrame);
			return -1;
		}
	}
	
	// --- internal methods ------------------------------------------
	
	void showMovieInfo()
	{
		Buffer frame = fgc.grabFrame();
		VideoFormat format = (VideoFormat) frame.getFormat();
		IJ.log("" + format.toString());
	}
	
	public void controllerUpdate(ControllerEvent evt)
	{
		// IJ.log("controllerUpdate - " + evt);
		if(evt instanceof ConfigureCompleteEvent || evt instanceof RealizeCompleteEvent || evt instanceof PrefetchCompleteEvent)
		{
			synchronized (waitSync)
			{
				stateTransitionOK = true;
				waitSync.notifyAll();
			}
		}
		else if(evt instanceof ResourceUnavailableEvent)
		{
			synchronized (waitSync)
			{
				stateTransitionOK = false;
				waitSync.notifyAll();
			}
		}
		else if(evt instanceof EndOfMediaEvent)
		{
			player.setMediaTime(new Time(0));
		}
	}
	
	/** Block until the player has transitioned to the given state. */
	boolean waitForState(int state)
	{
		synchronized (waitSync)
		{
			try
			{
				while (player.getState() < state && stateTransitionOK)
					waitSync.wait();
			}
			catch (Exception e)
			{}
		}
		return stateTransitionOK;
	}
	
}