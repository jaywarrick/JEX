/*
 * VIDEO LIBRARY FOR IMAGEJ
 * Author: Wilhelm Burger (wilbur@ieee.org)
 * Source: www.fh-hagenberg.at/staff/burger/imagej/
 */

package movieio.jmf;

import ij.IJ;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public abstract class MovieReader {
	
	public static final int UNKNOWN = -1;
	protected int width = 0;
	protected int height = 0;
	protected int totalFrames = UNKNOWN; // frame count unknown (-1)
	protected int currentFrame = 0;
	protected String path = "";
	protected String name = "";
	
	public static MovieReader openMovie(String path)
	{
		// this static method must be overwritten by subclasses
		throw new Error("openMovie() not implemented");
		// return null;
	}
	
	public ImageProcessor createImageProcessor()
	{
		return new ColorProcessor(width, height);
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public int getFrameCount()
	{
		return totalFrames;
	}
	
	public String getPath()
	{
		return path;
	}
	
	public String getName()
	{
		return name;
	}
	
	public abstract boolean open(String url);
	
	public abstract void close();
	
	public abstract boolean rewind();
	
	public abstract int getNextFrame(ImageProcessor ip);
	
	protected void error(String msg)
	{
		IJ.showMessage("MovieOpener: ", msg);
		IJ.showStatus("");
	}
	
}
