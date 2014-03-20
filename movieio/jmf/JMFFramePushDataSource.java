/*
 * VIDEO LIBRARY FOR IMAGEJ
 * Author: Wilhelm Burger (wilbur@ieee.org)
 * Source: www.fh-hagenberg.at/staff/burger/imagej/
 */

package movieio.jmf;

import ij.IJ;
import ij.process.ImageProcessor;

import java.awt.Dimension;
import java.io.IOException;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.Time;
import javax.media.format.RGBFormat;
import javax.media.protocol.BufferTransferHandler;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;

/**
 * A DataSource to sequentially read images and turn them into a stream of JMF buffers. The DataSource is not seekable or positionable.
 */

public class JMFFramePushDataSource extends PushBufferDataSource {
	
	private ImagePushSourceStream[] streams;
	
	public JMFFramePushDataSource(int width, int height, int frameRate)
	{
		streams = new ImagePushSourceStream[1];
		streams[0] = new ImagePushSourceStream(width, height, frameRate);
	}
	
	public void setLocator(MediaLocator source)
	{}
	
	public MediaLocator getLocator()
	{
		return null;
	}
	
	/**
	 * Content type is RAW since we are sending buffers of movieio frames without a container format.
	 */
	public String getContentType()
	{
		return ContentDescriptor.RAW;
	}
	
	public void connect()
	{}
	
	public void disconnect()
	{}
	
	public void start()
	{}
	
	public void stop()
	{
		// IJ.log(" DataSource stop");
	}
	
	/**
	 * Return the ImageSourceStreams.
	 */
	public PushBufferStream[] getStreams()
	{
		return streams;
	}
	
	/**
	 * We could have derived the duration from the number of frames and frame rate. But for the purpose of this program, it's not necessary.
	 */
	public Time getDuration()
	{
		return DURATION_UNKNOWN;
	}
	
	public Object[] getControls()
	{
		return new Object[0];
	}
	
	public Object getControl(String type)
	{
		return null;
	}
	
	public void addFrame(ImageProcessor ip)
	{
		// IJ.log("FramePushDataSource.addFrame()");
		streams[0].addFrame(ip);
	}
	
	public void end()
	{
		// IJ.log("FramePushDataSource.end()");
		streams[0].end();
	}
	
}

// -----------------------------------------------------------------------------------------

/**
 * The source stream to go along with ImageDataSource.
 */
class ImagePushSourceStream implements PushBufferStream {
	
	int frameCount = 250;
	long frameNo = 0;
	int frameRate;
	int width, height;
	// VideoFormat format;
	RGBFormat format;
	int nextImage = 0; // index of the next
	// image to be read.
	boolean ended = false;
	int maxDataLength;
	int[] rgbPixels;
	ContentDescriptor contentDescr;
	protected BufferTransferHandler transferHandler;
	
	int framesAdded = 0;
	int framesRead = 0;
	
	public ImagePushSourceStream(int width, int height, int frameRate)
	{
		this.width = width;
		this.height = height;
		this.frameRate = frameRate;
		Dimension size = new Dimension(width, height);
		contentDescr = new ContentDescriptor(ContentDescriptor.RAW);
		
		format = new RGBFormat(size, maxDataLength, Format.intArray, frameRate, 32, 0xFF0000, 0xFF00, 0xFF);
		frameNo = 0;
		
		framesAdded = 0;
		framesRead = 0;
	}
	
	public void read(Buffer buffer) throws IOException
	{
		synchronized (this)
		{
			if(framesAdded > framesRead + 1)
				IJ.log("*********** buffer overflow ****************");
			if(ended)
			{
				buffer.setEOM(true);
				buffer.setOffset(0);
				buffer.setLength(0);
			}
			else if(framesAdded > framesRead)
			{ // check if content available
				buffer.setData(rgbPixels);
				buffer.setFormat(format);
				buffer.setTimeStamp((long) (frameNo * (1000 / frameRate) * 1000000));
				buffer.setSequenceNumber(frameNo);
				buffer.setLength(maxDataLength);
				buffer.setFlags(Buffer.FLAG_KEY_FRAME);
				buffer.setHeader(null);
				frameNo++;
				framesRead++;
				this.notifyAll();
			}
		}
	}
	
	public Format getFormat()
	{
		return format;
	}
	
	public ContentDescriptor getContentDescriptor()
	{
		return contentDescr;
	}
	
	public long getContentLength()
	{
		return 0;
	}
	
	public boolean endOfStream()
	{
		return ended;
	}
	
	public Object[] getControls()
	{
		return new Object[0];
	}
	
	public Object getControl(String type)
	{
		return null;
	}
	
	public void setTransferHandler(BufferTransferHandler transferHandler)
	{
		synchronized (this)
		{
			this.transferHandler = transferHandler;
			notifyAll();
		}
	}
	
	void addFrame(ImageProcessor ip)
	{
		// IJ.log("ImagePushSourceStream.addFrame() " + frameNo);
		if(transferHandler != null)
		{
			synchronized (this)
			{
				try
				{ // wait till added frame has been processed
					while (framesAdded > framesRead)
						this.wait();
				}
				catch (Exception e)
				{}
				rgbPixels = (int[]) ip.getPixels();
				framesAdded++;
				transferHandler.transferData(this);
			}
		}
	}
	
	public void end()
	{
		// IJ.log("ImagePushSourceStream.end()");
		synchronized (this)
		{
			ended = true;
		}
		transferHandler.transferData(this);
	}
}
