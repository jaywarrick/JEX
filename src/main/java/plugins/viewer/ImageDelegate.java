package plugins.viewer;

import guiObject.PixelComponentDisplay;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.Vect;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

import logs.Logs;
import signals.SSCenter;

public class ImageDelegate {
	
	public static final String SIG_ImageUpdated_Null = "SIG_ImageUpdated_Null";
	
	// Source image variables
	private ImagePlus image = null;
	private String imagePath = null;
	private ImageProcessor sourceIJ = null; // for
	// looking
	// up
	// individual
	// pixels
	private Image adjustedSource = null;
	private int bitDepth = 0;
	private double minDisplayIntensity = -1, maxDisplayIntensity = -1;
	private double minImageIntensity = -1, maxImageIntensity = -1;
	private Rectangle imageRect = new Rectangle(0, 0, 0, 0);
	private Rectangle srcRect = new Rectangle(0, 0, 0, 0);
	private Point srcCenter = new Point(-1, -1);
	private double zoom = 1; // current
	// zoom
	// scale
	private boolean fitToDestination = true;
	private int maxZoom = 25;
	private double minZoom = 0.1;
	private double zoomFactor = 1.5;
	private boolean forceFit = true;
	
	public void setImage(String path)
	{
		if(path == null)
		{
			this.imagePath = null;
			this.setImage((ImagePlus) null);
			return;
		}
		if(path.equals(this.imagePath))
		{
			return;
		}
		this.imagePath = path;
		Logs.log("Opening image at path " + this.imagePath, 1, this);
		this.setImage(new ImagePlus(path));
	}
	
	/**
	 * Set the displayed image
	 * 
	 * @param image
	 */
	public void setImage(ImagePlus image)
	{
		if(image == null)
		{
			this.image = null;
			this.adjustedSource = null;
			this.sourceIJ = null;
			return;
		}
		
		this.image = image;
		ImageProcessor proc = image.getProcessor();
		ImageStatistics stats = (proc == null) ? null : ImageStatistics.getStatistics(proc, ImageStatistics.MIN_MAX, null);
		this.minImageIntensity = (stats == null) ? 0 : stats.min;
		this.maxImageIntensity = (stats == null) ? 255 : stats.max;
		if(image != null)
		{
			// Logs.log("Getting bitDepth", 0, this);
			this.bitDepth = image.getBitDepth();
			// Logs.log("Getting Processor", 0, this);
			this.sourceIJ = image.getProcessor();
			if(this.maxDisplayIntensity == -1 || this.minDisplayIntensity == -1)
			{
				Logs.log("Getting intensities", 0, this);
				this.minDisplayIntensity = (int) this.minImageIntensity;
				this.maxDisplayIntensity = (int) this.maxImageIntensity;
			}
			
			// setLimits sets the adjustedSource image.
			// Logs.log("Setting intensities", 0, this);
			if(this.image.getType() == ImagePlus.COLOR_RGB)
			{
				this.sourceIJ.snapshot();
			}
			this.setDisplayLimits(this.minDisplayIntensity, this.maxDisplayIntensity);
			if(this.sourceIJ != null && this.imageRect.width == this.sourceIJ.getWidth() && this.imageRect.height == this.sourceIJ.getHeight())
			{
				this.imageUpdated();
			}
			else
			{
				if(this.sourceIJ == null)
				{
					this.imageRect.width = 0;
					this.imageRect.height = 0;
					this.srcCenter.x = this.imageRect.width / 2;
					this.srcCenter.y = this.imageRect.height / 2;
					this.zoom = 1;
				}
				else
				{
					this.imageRect.width = this.sourceIJ.getWidth();
					this.imageRect.height = this.sourceIJ.getHeight();
					this.srcCenter.x = this.imageRect.width / 2;
					this.srcCenter.y = this.imageRect.height / 2;
					this.zoom = 1;
				}
				this.setToFitToDestination();
			}
		}
	}
	
	public Image getImage()
	{
		return this.adjustedSource;
	}
	
	public void setDisplayLimits(double min, double max)
	{
		this.minDisplayIntensity = min;
		this.maxDisplayIntensity = max;
		if(this.image == null)
		{
			return;
		}
		// Logs.log("Setting limits", 0, this);
		if(this.image.getType() == ImagePlus.COLOR_RGB)
		{
			this.sourceIJ.reset();
			this.sourceIJ.setMinAndMax(min, max);
			this.adjustedSource = this.sourceIJ.createImage();
		}
		else if(this.sourceIJ != null)
		{
			this.sourceIJ.setMinAndMax(min, max);
			this.adjustedSource = this.sourceIJ.createImage();
		}
		else
		{
			this.sourceIJ = null;
			this.adjustedSource = null;
		}
		// Logs.log("Creating image", 0, this);
		
		// Logs.log("Updating image after setting limits", 0,
		// this);
		this.imageUpdated();
	}
	
	public double minDisplayIntensity()
	{
		return this.minDisplayIntensity;
	}
	
	public double maxDisplayIntensity()
	{
		return this.maxDisplayIntensity;
	}
	
	public double minImageIntensity()
	{
		return this.minImageIntensity;
	}
	
	public double maxImageIntensity()
	{
		return this.maxImageIntensity;
	}
	
	public int bitDepth()
	{
		return this.bitDepth;
	}
	
	// ImageDelegate methods called by ModelDelegates
	
	/**
	 * Zoom image around point (srcCenter.x,srcCenter.y)
	 * 
	 * @param units
	 *            number of increments
	 * @param centerP
	 *            center point around which zoom is done in the actual image
	 */
	public void setZoom(int units, Point centerP)
	{
		this.fitToDestination = false;
		
		double newZoom = this.maxZoom;
		if(units > 0) // which means to zoom in.
		{
			newZoom = this.getNextHigherZoomLevel(this.zoom);
		}
		else
		{
			newZoom = this.getNextLowerZoomLevel(this.zoom);
		}
		if(this.zoom != newZoom)
		{
			this.zoom = newZoom;
			this.srcCenter.x = centerP.x;
			this.srcCenter.y = centerP.y;
			// Logs.log("Zooming to image point: "+
			// this.srcCenter.x + "," + this.srcCenter.y, 0, this);
			this.imageUpdated(); // sets fitToDestination back to true if necessary
			// via getSrcRect
		}
	}
	
	/**
	 * Zoom image around point (srcCenter.x,srcCenter.y)
	 * 
	 * @param units
	 *            number of increments
	 * @param centerP
	 *            center point around which zoom is done in the actual image
	 */
	public void setZoom(double zoom, Point centerP)
	{
		this.fitToDestination = false;
		
		double newZoom = zoom;
		if(newZoom > this.maxZoom) // which means to zoom in.
		{
			newZoom = this.maxZoom;
		}
		if(newZoom < this.minZoom)
		{
			newZoom = this.minZoom;
		}
		if(this.zoom != newZoom || this.srcCenter.x != centerP.x || this.srcCenter.y != centerP.y)
		{
			this.zoom = newZoom;
			this.srcCenter.x = centerP.x;
			this.srcCenter.y = centerP.y;
			// Logs.log("Zooming to image point: "+
			// this.srcCenter.x + "," + this.srcCenter.y, 0, this);
			this.imageUpdated(); // sets fitToDestination back to true if necessary
			// via getSrcRect
		}
	}
	
	private double getNextHigherZoomLevel(double currentZoom)
	{
		if(currentZoom >= this.maxZoom / this.zoomFactor)
		{
			return this.maxZoom;
		}
		double newZoom = this.maxZoom;
		while (Math.round(10000 * newZoom) > Math.round(10000 * currentZoom)) // Get rid of rounding errors
		{
			newZoom = newZoom / this.zoomFactor;
		}
		return newZoom * this.zoomFactor;
	}
	
	private double getNextLowerZoomLevel(double currentZoom)
	{
		double newZoom = this.maxZoom;
		while (newZoom >= currentZoom)
		{
			newZoom = newZoom / this.zoomFactor;
		}
		return newZoom;
	}
	
	public double getZoom()
	{
		return this.zoom;
	}
	
	public void setToFitToDestination()
	{
		this.fitToDestination = true;
		this.imageUpdated();
	}
	
	public void imageDragged(Rectangle r)
	{
		this.srcCenter.x = r.x - r.width;
		this.srcCenter.y = r.y - r.height;
		this.imageUpdated();
	}
	
	public void translateSrcRect(Vect v)
	{
		this.srcCenter.x = (int) (this.srcCenter.x + v.dX);
		this.srcCenter.y = (int) (this.srcCenter.y + v.dY);
		this.imageUpdated();
	}
	
	public void imageUpdated()
	{
		// Logs.log("paint(), srcCenter: " + srcCenter, 0,
		// this);
		// Logs.log("paint(), min: " + minIntensity, 0, this);
		// Logs.log("paint(), max: " + maxIntensity, 0, this);
		// Logs.log("paint(), zoom: " + zoom, 0, this);
		// Logs.log("Sending ImageUpdated signal", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_ImageUpdated_Null, (Object[]) null);
	}
	
	public Rectangle getImageRect()
	{
		return (Rectangle) this.imageRect.clone();
	}
	
	public Point getSrcCenter()
	{
		return (Point) this.srcCenter.clone();
	}
	
	/**
	 * srcCenter and zoom determine everything. srcRect is what is represented in the PixelDisplay and is calculated from srcCenter and zoom. This method is called by those who will paint this image onto a display (dstRect).
	 */
	public Rectangle getSrcRect(Rectangle dstRect)
	{
		
		// Logs.log("getSrcRect start: srcCenter = " +
		// this.srcCenter.x + "," + this.srcCenter.y, 0, this);
		
		if(this.fitToDestination)
		{
			double imageAspect = ((double) this.imageRect.height) / ((double) this.imageRect.width);
			double dstAspect = ((double) dstRect.height) / ((double) dstRect.width);
			
			boolean heightLimited = false;
			if(dstAspect <= imageAspect)
			{
				heightLimited = true;
			}
			
			if(heightLimited)
			{
				this.srcRect.height = (this.imageRect.height);
				this.srcRect.width = (int) ((this.srcRect.height) / dstAspect);
				this.zoom = ((double) dstRect.width) / ((double) this.srcRect.width);
			}
			else
			{
				this.srcRect.width = (this.imageRect.width);
				this.srcRect.height = (int) (dstAspect * (this.srcRect.width));
				this.zoom = ((double) dstRect.height) / ((double) this.srcRect.height);
			}
		}
		else
		{
			this.srcRect.width = (int) (dstRect.width / this.zoom);
			this.srcRect.height = (int) (dstRect.height / this.zoom);
		}
		
		boolean srcIsWiderThanImage = false;
		if(this.srcRect.width > this.imageRect.width)
		{
			srcIsWiderThanImage = true;
		}
		boolean srcIsTallerThanImage = false;
		if(this.srcRect.height > this.imageRect.height)
		{
			srcIsTallerThanImage = true;
		}
		
		if(this.forceFit && srcIsWiderThanImage && srcIsTallerThanImage)
		{
			this.fitToDestination = true;
			return this.getSrcRect(dstRect);
		}
		
		if(this.forceFit && srcIsWiderThanImage)
		{
			this.srcCenter.x = this.imageRect.width / 2;
			this.srcRect.x = this.srcCenter.x - this.srcRect.width / 2;
		}
		else
		{
			this.srcRect.x = this.srcCenter.x - this.srcRect.width / 2;
			if(this.forceFit)
			{
				// Correct if off to the right
				if(this.srcRect.x + this.srcRect.width > this.imageRect.width)
				{
					this.srcCenter.x = this.imageRect.width - this.srcRect.width / 2;
					this.srcRect.x = this.imageRect.width - this.srcRect.width;
					
				}
				// Correct if off to the left
				if(this.srcRect.x < 0)
				{
					this.srcRect.x = 0;
					this.srcCenter.x = this.srcRect.width / 2;
				}
			}
		}
		
		if(this.forceFit && srcIsTallerThanImage)
		{
			this.srcCenter.y = this.imageRect.height / 2;
			this.srcRect.y = this.srcCenter.y - this.srcRect.height / 2;
		}
		else
		{
			this.srcRect.y = this.srcCenter.y - this.srcRect.height / 2;
			if(this.forceFit)
			{
				// Correct if off to the bottom
				if(this.srcRect.y + this.srcRect.height > this.imageRect.height)
				{
					this.srcCenter.y = this.imageRect.height - this.srcRect.height / 2;
					this.srcRect.y = this.imageRect.height - this.srcRect.height;
					
				}
				// Correct if off to the left
				if(this.srcRect.y < 0)
				{
					this.srcRect.y = 0;
					this.srcCenter.y = this.srcRect.height / 2;
				}
			}
		}
		// Logs.log("getSrcRect end: srcCenter = " +
		// this.srcCenter.x + "," + this.srcCenter.y, 0, this);
		
		// Logs.log("getSrcRect(), dstRect: " + dstRect, 0,
		// this);
		// Logs.log("getSrcRect(), srcRect: " + srcRect, 0,
		// this);
		return (Rectangle) this.srcRect.clone();
	}
	
	public boolean getFitsInDestination()
	{
		return this.fitToDestination;
	}
	
	public void setForceFit(boolean forceFit)
	{
		this.forceFit = forceFit;
	}
	
	public float getPixelIntensity(Point p)
	{
		return this.sourceIJ.getPixelValue(p.x, p.y);
	}
	
	public void show()
	{
		ImagePlus im = new ImagePlus("whatever", this.getImage());
		im.show();
	}
	
	// ///////////////////////////////////////////////////////////////////////
	// //// Utilities to pass point from display to image world ///////
	// ///////////////////////////////////////////////////////////////////////
	
	/**
	 * Return a point in the image space from a point on the screen
	 * 
	 * @param p
	 * @return point
	 */
	public IdPoint displayToImage(PixelComponentDisplay display, IdPoint p)
	{
		Rectangle srcRect = this.getSrcRect(display.getBounds()); // 0,0 in
		// srcRect is
		// the upper
		// left corner
		// of the
		// image
		IdPoint ret = new IdPoint((int) (((p.x) / this.getZoom()) + srcRect.x), (int) (((p.y) / this.getZoom()) + srcRect.y), p.id);
		return ret;
	}
	
	public IdPoint displayToImage(PixelComponentDisplay display, Point p)
	{
		return this.displayToImage(display, new IdPoint(p, 0));
	}
	
	public Rectangle displayToImage(PixelComponentDisplay display, Rectangle r)
	{
		IdPoint loc = new IdPoint(r.x, r.y, 0);
		Vect w_h = new Vect(r.width, r.height);
		loc = this.displayToImage(display, loc);
		w_h = this.displayToImage(display, w_h);
		Rectangle ret = new Rectangle(loc.x, loc.y, (int) w_h.dX, (int) w_h.dY);
		return ret;
	}
	
	/**
	 * Return a point in the display space from a point on the image
	 * 
	 * @param p
	 * @return
	 */
	public PointList displayToImage(PixelComponentDisplay display, PointList pl)
	{
		PointList ret = new PointList();
		for (IdPoint p : pl)
		{
			ret.add(this.displayToImage(display, p));
		}
		return ret;
	}
	
	/**
	 * Return a vector in the image space from the display space
	 * 
	 * @param v
	 * @return
	 */
	public Vect displayToImage(PixelComponentDisplay display, Vect v)
	{
		Vect result = v.duplicate();
		result.multiply(1 / this.getZoom());
		return result;
	}
	
	/**
	 * Return a point in the display space from a point on the image
	 * 
	 * @param p
	 * @return
	 */
	public IdPoint imageToDisplay(PixelComponentDisplay display, IdPoint p)
	{
		Rectangle srcRect = this.getSrcRect(display.getBounds()); // 0,0 in
		// srcRect is
		// the upper
		// left corner
		// of the
		// image
		return new IdPoint((int) (((double) p.x - srcRect.x) * this.getZoom()), (int) (((double) p.y - srcRect.y) * this.getZoom()), p.id);
	}
	
	public IdPoint imageToDisplay(PixelComponentDisplay display, Point p)
	{
		return this.imageToDisplay(display, new IdPoint(p, 0));
	}
	
	public Rectangle imageToDisplay(PixelComponentDisplay display, Rectangle r)
	{
		IdPoint loc = new IdPoint(r.x, r.y, 0);
		Vect w_h = new Vect(r.width, r.height);
		loc = this.imageToDisplay(display, loc);
		w_h = this.imageToDisplay(display, w_h);
		return new Rectangle(loc.x, loc.y, (int) w_h.dX, (int) w_h.dY);
	}
	
	/**
	 * Return a vector in display space from the image space
	 * 
	 * @param v
	 * @return
	 */
	public Vect imageToDisplay(PixelComponentDisplay display, Vect v)
	{
		Vect result = v.duplicate();
		result.multiply(this.getZoom());
		return result;
	}
	
	/**
	 * Return a point in the display space from a point on the image
	 * 
	 * @param p
	 * @return
	 */
	public PointList imageToDisplay(PixelComponentDisplay display, PointList pl)
	{
		PointList ret = new PointList();
		for (IdPoint p : pl)
		{
			ret.add(this.imageToDisplay(display, p));
		}
		return ret;
	}
	
	public static Rectangle intersection(Rectangle r1, Rectangle r2)
	{
		Rectangle ret = new Rectangle();
		
		int x2 = Math.min(r1.x + r1.width, r2.x + r2.width);
		int y2 = Math.min(r1.y + r1.height, r2.y + r2.height);
		ret.x = Math.max(r1.x, r2.x);
		ret.y = Math.max(r1.y, r2.y);
		ret.width = x2 - ret.x;
		ret.height = y2 - ret.y;
		
		return ret;
		
	}
	
}
