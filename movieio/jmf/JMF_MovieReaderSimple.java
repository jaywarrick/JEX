package movieio.jmf;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Image;
import java.io.File;
import java.net.MalformedURLException;

import javax.media.Buffer;
import javax.media.Duration;
import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;
import javax.media.Time;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.FramePositioningControl;
import javax.media.format.VideoFormat;
import javax.media.protocol.DataSource;
import javax.media.util.BufferToImage;

public class JMF_MovieReaderSimple {
	
	Player p;
	FramePositioningControl fpc;
	FrameGrabbingControl fgc;
	Object waitSync = new Object();
	boolean stateTransitionOK = true;
	int totalFrames = FramePositioningControl.FRAME_UNKNOWN;
	BufferToImage frameConverter;
	String name;
	boolean grayscale = false;
	ImageStack stack;
	
	public JMF_MovieReaderSimple()
	{}
	
	public void run(String fname)
	{
		// Make the url for the media locator
		File file = new File(fname);
		java.net.URL url;
		String urlString = null;
		try
		{
			url = file.toURI().toURL();
			urlString = url.toString();
		}
		catch (MalformedURLException e1)
		{}
		
		// Create the media locator
		MediaLocator ml = new MediaLocator(urlString);
		
		// Create the data source
		DataSource ds = null;
		try
		{
			ds = Manager.createDataSource(ml);
			
		}
		catch (Exception e)
		{}
		
		// Open movie
		openMovie(ds);
	}
	
	@SuppressWarnings("unused")
	public boolean openMovie(DataSource ds)
	{
		
		try
		{
			p = Manager.createPlayer(ds);
		}
		catch (Exception e)
		{
			error("Failed to create a player from the given DataSource:\n \n" + e.getMessage());
			return false;
		}
		
		// p.realize();
		// if (!waitForState(p.Realized))
		// {
		// error("Failed to realize the JMF player.");
		// return false;
		// }
		
		// Try to retrieve a FramePositioningControl from the player.
		javax.media.Control control = p.getControl("FrameGrabbingControl");
		fpc = (FramePositioningControl) p.getControl("javax.media.control.FramePositioningControl");
		if(fpc == null)
		{
			error("The player does not support FramePositioningControl.");
			return false;
		}
		
		// Try to retrieve a FrameGrabbingControl from the player.
		fgc = (FrameGrabbingControl) p.getControl("javax.media.control.FrameGrabbingControl");
		if(fgc == null)
		{
			error("The player does not support FrameGrabbingControl.");
			return false;
		}
		
		Time duration = p.getDuration();
		if(duration != Duration.DURATION_UNKNOWN)
		{
			totalFrames = fpc.mapTimeToFrame(duration) + 1;
		}
		else
		{}
		
		// // Prefetch the player.
		// p.prefetch();
		// if (!waitForState(p.Prefetched))
		// {
		// error("Failed to prefetch the player.");
		// return false;
		// }
		
		Buffer frame = fgc.grabFrame();
		VideoFormat format = (VideoFormat) frame.getFormat();
		frameConverter = new BufferToImage(format);
		
		for (int i = 0; i < totalFrames; i++)
		{
			int currentFrame = fpc.mapTimeToFrame(p.getMediaTime());
			if(!saveImage(i + 1, fgc.grabFrame()))
				break;
			fpc.skip(1);
		}
		if(stack != null)
			new ImagePlus(name, stack).show();
		
		return true;
	}
	
	/** Block until the player has transitioned to the given state. */
	boolean waitForState(int state)
	{
		synchronized (waitSync)
		{
			try
			{
				while (p.getState() < state && stateTransitionOK)
					waitSync.wait();
			}
			catch (Exception e)
			{}
		}
		return stateTransitionOK;
	}
	
	private void error(String msg)
	{
		// IJ.showMessage("JMF Movie Reader", msg);
		// IJ.showStatus("");
		System.out.println(msg);
	}
	
	private boolean saveImage(int count, Buffer frame)
	{
		Image img = frameConverter.createImage(frame);
		ImageProcessor ip = new ColorProcessor(img);
		if(count == 1)
		{
			int width = ip.getWidth();
			int height = ip.getHeight();
			
			stack = allocateStack(width, height, totalFrames);
			if(stack == null)
			{
				return false;
			}
			grayscale = isGrayscale(ip);
		}
		if(grayscale)
			ip = ip.convertToByte(false);
		stack.setPixels(ip.getPixels(), count);
		return true;
	}
	
	@SuppressWarnings("unused")
	ImageStack allocateStack(int width, int height, int size)
	{
		ImageStack stack = null;
		byte[] temp;
		try
		{
			stack = new ImageStack(width, height);
			for (int i = 0; i < size; i++)
			{
				if(grayscale)
					stack.addSlice(null, new byte[width * height]);
				else
					stack.addSlice(null, new int[width * height]);
			}
			temp = new byte[width * height * 4 * 5 + 1000000];
		}
		catch (OutOfMemoryError e)
		{
			if(stack != null)
			{
				Object[] arrays = stack.getImageArray();
				if(arrays != null)
					for (int i = 0; i < arrays.length; i++)
						arrays[i] = null;
			}
			stack = null;
		}
		temp = null;
		System.gc();
		System.gc();
		return stack;
	}
	
	boolean isGrayscale(ImageProcessor ip)
	{
		int[] pixels = (int[]) ip.getPixels();
		boolean grayscale = true;
		int c, r, g, b;
		for (int i = 0; i < pixels.length; i++)
		{
			c = pixels[i];
			r = (c & 0xff0000) >> 16;
			g = (c & 0xff00) >> 8;
			b = c & 0xff;
			if(r != g || r != b || g != b)
			{
				grayscale = false;
				break;
			}
		}
		return grayscale;
	}
	
	public ImageStack getStack()
	{
		return stack;
	}
}
