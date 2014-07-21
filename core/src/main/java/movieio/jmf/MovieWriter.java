/*
 * VIDEO LIBRARY FOR IMAGEJ
 * Author: Wilhelm Burger (wilbur@ieee.org)
 * Source: www.fh-hagenberg.at/staff/burger/imagej/
 */

package movieio.jmf;

/*
 * Abstract superclass of all movie writers (JMF, QT).
 * Introduces 3 methods for creating movies frame-by-frame:
 * 		createMovie()... creates a new movie
 * 		addFrame()...... adds a single frame to the movie
 * 		close()......... closes the movie
 */

import ij.IJ;
import ij.process.ImageProcessor;

public abstract class MovieWriter {
	
	static public final int UNKNOWN = -1;
	static public final int DEFAULT_FPS = 25;
	
	protected boolean isInitialized = false;
	protected int width = 0;
	protected int height = 0;
	protected String path = null;
	protected String pathAbsolute = null;
	protected double frameRate = DEFAULT_FPS;
	protected int totalFrames = UNKNOWN; // frame count unknown
	// (-1)
	protected int currentFrame = 0;
	
	public static MovieWriter createMovie(ImageProcessor ip, String path)
	{
		// create a new movie
		// this static method must be overridden in subclasses
		throw new Error("createMovie() not implemented");
	}
	
	public static MovieWriter createMovie(ImageProcessor ip, String path, int frameRate, VideoOutputFormat vof)
	{
		// create a new movie
		// this static method must be overridden in subclasses
		throw new Error("createMovie() not implemented");
	}
	
	public String getAbsolutePath()
	{
		return pathAbsolute;
	}
	
	public abstract void addFrame(ImageProcessor ip);
	
	// add one frame to the movie
	
	public abstract void close();
	
	// close the movie
	
	void error(String msg)
	{
		IJ.showMessage("MovieOpener: ", msg);
		IJ.showStatus("");
	}
}
