package plugins.imageAligner2;

import guiObject.PaintComponentDelegate;
import guiObject.PixelComponentDisplay;
import ij.ImagePlus;
import image.roi.Vect;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import jex.statics.DisplayStatics;
import plugins.viewer.ImageDelegate;
import signals.SSCenter;

public class ImageAlignerInterface implements PaintComponentDelegate, MouseListener, MouseMotionListener {
	
	public static final String SIG_DoubleClick_ImagedDelegate_Point = "SIG_DoubleClick_ImagedDelegate_Point";
	public static final String SIG_Dragged_Rectangle = "SIG_Dragged_Rectangle";
	
	public PixelComponentDisplay display;
	public ImageDelegate delegate;
	
	private double alpha, zoom;
	private boolean draggable, clickable, paintsBackground = true;
	private Rectangle srcCenterDragRect;
	private Point firstPointSrcCenter, firstPointInDisplay, currentLocation;
	private Rectangle dstRect, srcRect, displaySrcRect;
	private BufferedImage bufferedImage;
	private Image image;
	
	public ImageAlignerInterface()
	{
		this.display = null;
		this.delegate = new ImageDelegate();
		this.delegate.setForceFit(false);
		this.alpha = 1;
		this.draggable = false;
	}
	
	public void setDisplay(PixelComponentDisplay display)
	{
		this.display = display;
		this.display.addMouseListener(this);
		this.display.addMouseMotionListener(this);
	}
	
	public void setImage(ImagePlus image)
	{
		this.delegate.setImage(image);
		this.image = this.delegate.getImage();
		if(alpha < 1)
		{
			bufferedImage = createTransparent(this.image, (float) alpha);
		}
	}
	
	private BufferedImage createTransparent(Image src, float alpha)
	{
		
		int type = BufferedImage.TYPE_INT_RGB;
		BufferedImage buffer1 = new BufferedImage(src.getWidth(null), src.getHeight(null), type);
		
		// Copy image to buffered image
		Graphics g = buffer1.createGraphics();
		
		// Paint the image onto the buffered image
		g.drawImage(image, 0, 0, null);
		
		// Create a buffered image using the default color model
		type = BufferedImage.TYPE_INT_ARGB;
		BufferedImage result = new BufferedImage(src.getWidth(null), src.getHeight(null), type);
		
		// Copy image to buffered image
		Graphics2D g2 = result.createGraphics();
		
		// Create the transparency
		int rule = AlphaComposite.SRC_OVER;
		AlphaComposite ac = AlphaComposite.getInstance(rule, alpha);
		g2.setComposite(ac);
		g2.drawImage(buffer1, null, 0, 0);
		g2.dispose();
		
		return result;
	}
	
	public void setZoom(double zoom, Point focusPoint)
	{
		if(focusPoint == null)
		{
			focusPoint = this.delegate.getSrcCenter();
		}
		this.zoom = zoom;
		this.delegate.setZoom(this.zoom, focusPoint);
	}
	
	public void setAlpha(double alpha)
	{
		this.alpha = alpha;
	}
	
	public void setDraggable(boolean draggable)
	{
		this.draggable = draggable;
	}
	
	public void setClickable(boolean clickable)
	{
		this.clickable = clickable;
	}
	
	public void setPaintsBackground(boolean paintsBackground)
	{
		this.paintsBackground = paintsBackground;
	}
	
	public void setDisplayLimits(int min, int max)
	{
		this.delegate.setDisplayLimits(min, max);
	}
	
	public void imageDragged(Rectangle dragSrcRect)
	{
		this.delegate.imageDragged(dragSrcRect); // causes repaint
	}
	
	public void paintComponent(Graphics2D g2)
	{
		// Gather the rectangles that represent important positions
		dstRect = this.display.getBoundsLocal(); // panel region
		srcRect = this.delegate.getSrcRect(dstRect); // region of original image
		// being drawn
		displaySrcRect = this.delegate.imageToDisplay(this.display, srcRect); // Actual
		// drawn
		// region
		// including
		// padding
		// but
		// not
		// panel
		// background
		// imageRect = this.delegate.getImageRect(); // Size of original image
		// displayImageRect = this.delegate.imageToDisplay(this.display,
		// imageRect); // Actual drawn region without padding but including
		// panel background
		// intersectionRect =
		// ImageDelegate.intersection(displaySrcRect,displayImageRect); //
		// Actual drawn region without padding and without panel background
		
		// Paint the image Background
		if(paintsBackground)
		{
			g2.setColor(DisplayStatics.background);
			g2.fill(this.display.getBoundsLocal());
		}
		
		if(this.delegate.getImage() == null)
			return;
		if(alpha < 1)
		{
			g2.drawImage(this.bufferedImage, displaySrcRect.x, displaySrcRect.y, displaySrcRect.x + displaySrcRect.width, displaySrcRect.y + displaySrcRect.height, srcRect.x, srcRect.y, srcRect.x + srcRect.width, srcRect.y + srcRect.height, null);
		}
		else
		{
			g2.drawImage(this.image, displaySrcRect.x, displaySrcRect.y, displaySrcRect.x + displaySrcRect.width, displaySrcRect.y + displaySrcRect.height, srcRect.x, srcRect.y, srcRect.x + srcRect.width, srcRect.y + srcRect.height, null);
		}
	}
	
	public void mouseDragged(MouseEvent e)
	{
		if(draggable)
		{
			if(!(e.getModifiersEx() == MouseEvent.BUTTON1_DOWN_MASK))
				return;
			
			// Set the currentLocation;
			currentLocation = e.getPoint();
			
			// Determine what is being dragged and send current drag rectangle
			Vect shift = new Vect(currentLocation.x - firstPointInDisplay.x, currentLocation.y - firstPointInDisplay.y);
			shift = this.delegate.displayToImage(this.display, shift);
			
			// dragRect.x and y already hold the firstPoints, just update the
			// width and height
			srcCenterDragRect.width = (int) shift.dX;
			srcCenterDragRect.height = (int) shift.dY;
			
			SSCenter.defaultCenter().emit(this, SIG_Dragged_Rectangle, new Object[] { srcCenterDragRect });
		}
	}
	
	public void mousePressed(MouseEvent e)
	{
		if(!draggable || !(e.getButton() == MouseEvent.BUTTON1))
			return;
		
		firstPointInDisplay = e.getPoint();
		firstPointSrcCenter = this.delegate.getSrcCenter();
		srcCenterDragRect = new Rectangle(firstPointSrcCenter.x, firstPointSrcCenter.y, 0, 0);
		
	}
	
	public void mouseClicked(MouseEvent e)
	{
		if(clickable && e.getClickCount() == 2)
		{
			SSCenter.defaultCenter().emit(this, SIG_DoubleClick_ImagedDelegate_Point, new Object[] { this.delegate, e.getPoint() });
		}
	}
	
	public void mouseMoved(MouseEvent e)
	{}
	
	public void mouseEntered(MouseEvent e)
	{}
	
	public void mouseExited(MouseEvent e)
	{}
	
	public void mouseReleased(MouseEvent e)
	{}
	
}
