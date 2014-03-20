package jex.arrayView;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.Trajectory;
import image.roi.Vect;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import logs.Logs;

public class ImageDisplay extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// statics
	public static double zoomFactor = 1.5;
	
	// Interacting classes
	private ImageDisplayController highLevelController;
	
	// Options of class
	private boolean canZoomAndPan = true;
	
	// Info to display with image
	private String title = "";
	
	// display variables
	private Color background = DisplayStatics.background;
	private JLabel titleLabel = new JLabel();
	private JPanel displayPanel;
	
	public ImageDisplay(ImageDisplayController controller, String title)
	{
		this.highLevelController = controller;
		this.title = title;
		
		this.initialize();
	}
	
	/**
	 * Initialize panel
	 */
	private void initialize()
	{
		this.setBackground(this.background);
		this.setLayout(new BorderLayout());
		
		this.displayPanel = new JPanel();
		this.displayPanel.setBackground(this.background);
		this.displayPanel.setLayout(new BorderLayout());
		this.displayPanel.addMouseListener(this);
		this.displayPanel.addMouseMotionListener(this);
		this.displayPanel.addMouseWheelListener(this);
		
		this.titleLabel.setText(this.title);
		this.titleLabel.setPreferredSize(new Dimension(200, 20));
		this.titleLabel.setMaximumSize(new Dimension(200, 20));
		this.titleLabel.setForeground(Color.WHITE);
		
		this.add(this.titleLabel, BorderLayout.PAGE_START);
		this.add(this.displayPanel, BorderLayout.CENTER);
	}
	
	/**
	 * Allows the image panel to zoom and pan
	 * 
	 * @param canZoomAndPan
	 */
	public void setCanZoomAndPan(boolean canZoomAndPan)
	{
		this.canZoomAndPan = canZoomAndPan;
	}
	
	/**
	 * Set a high level roi controller, responds to point and rectangle creations
	 * 
	 * @param highLevelController
	 */
	public void setHighLevelController(ImageDisplayController highLevelController)
	{
		this.highLevelController = highLevelController;
	}
	
	// -------------------------------------------------
	// -------------- Setters and actions --------------
	// -------------------------------------------------
	// Source image variables
	private ImageProcessor sourceIJ = null; // for
	// looking
	// up
	// individual
	// pixels
	private Image adjustedSource = null;
	private double minIntensity = -1, maxIntensity = -1;
	private Rectangle imageRect = new Rectangle(0, 0, 0, 0);
	private Rectangle srcRect = new Rectangle(0, 0, 0, 0);
	private Point srcCenter = new Point(-1, -1);
	private Rectangle dstRect = new Rectangle(0, 0, 0, 0);
	private double zoom = 1; // current
	
	// zoom
	// scale
	
	@Override
	public void setBackground(Color background)
	{
		this.background = background;
		this.repaint();
	}
	
	/**
	 * Set the displayed image
	 * 
	 * @param image
	 */
	public void setImage(ImagePlus image)
	{
		if(image != null)
		{
			this.setImage(image.getProcessor());
		}
	}
	
	/**
	 * Set the displayed image
	 * 
	 * @param image
	 */
	public void setImage(ImageProcessor image)
	{
		if(image == null)
		{
			return;
		}
		
		Logs.log("Setting image in panel", 2, this);
		this.sourceIJ = image;
		if(this.maxIntensity == -1 || this.minIntensity == -1)
		{
			this.minIntensity = (int) this.sourceIJ.getMin();
			this.maxIntensity = (int) this.sourceIJ.getMax();
		}
		this.setLimits(this.minIntensity, this.maxIntensity);
		if(this.sourceIJ != null && this.imageRect.width == this.sourceIJ.getWidth() && this.imageRect.height == this.sourceIJ.getHeight())
		{}
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
		}
		this.extractImage();
		this.repaint();
	}
	
	/**
	 * Set the displayed image
	 * 
	 * @param image
	 */
	public void setImage(ImagePlus image, boolean noRescaling)
	{
		if(!noRescaling)
		{
			this.setImage(image);
		}
		else
		{
			Logs.log("Setting image in panel", 1, this);
			this.sourceIJ = image.getProcessor();
			if(this.maxIntensity == -1 || this.minIntensity == -1)
			{
				this.minIntensity = (int) this.sourceIJ.getMin();
				this.maxIntensity = (int) this.sourceIJ.getMax();
			}
			this.setLimits(this.minIntensity, this.maxIntensity);
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
			}
			this.extractImage();
			this.repaint();
		}
	}
	
	public void setLimits(double min, double max)
	{
		this.minIntensity = min;
		this.maxIntensity = max;
		this.sourceIJ.setMinAndMax(min, max);
		this.adjustedSource = this.sourceIJ.createImage();
		this.repaint();
	}
	
	public double minIntensity()
	{
		return this.minIntensity;
	}
	
	public double maxIntensity()
	{
		return this.maxIntensity;
	}
	
	/**
	 * Set the displayed image
	 * 
	 * @param image
	 */
	public void setTitle(String title)
	{
		Logs.log("Setting title of image to " + title, 1, this);
		this.title = title;
		this.extractImage();
		this.repaint();
	}
	
	/**
	 * Zoom image around point (srcCenter.x,srcCenter.y)
	 * 
	 * @param units
	 *            number of increments
	 * @param centerP
	 *            center point around which zoom is done
	 */
	public void zoom(int units, Point centerP)
	{
		if(units >= 0)
		{
			this.zoom = this.zoom * units * ImageDisplay.zoomFactor;
		}
		else
		{
			this.zoom = Math.abs(this.zoom / (units * ImageDisplay.zoomFactor));
		}
		
		if(this.zoom < 1)
		{
			this.zoom = 1;
		}
		if(this.zoom > 401)
		{
			this.zoom = 400;
		}
		
		this.srcCenter.x = (int) centerP.getX();
		this.srcCenter.y = (int) centerP.getY();
		Logs.log("Zooming image, new zoom factor is " + this.zoom, 2, this);
		this.extractImage();
		this.repaint();
	}
	
	/**
	 * Translate image by a vector (dx,dy)
	 * 
	 * @param dx
	 *            x displacement
	 * @param dy
	 *            y displacement
	 */
	public void translate(Vect v)
	{
		this.srcCenter.x = (int) Math.round(this.srcCenter.x + v.dX);
		this.srcCenter.y = (int) Math.round(this.srcCenter.y + v.dY);
		Logs.log("Translating image by " + v.dX + "," + v.dY, 2, this);
		this.extractImage();
		this.repaint();
	}
	
	/**
	 * Translate image by a vector (dx,dy)
	 * 
	 * @param dx
	 *            x displacement
	 * @param dy
	 *            y displacement
	 */
	public void dragToPoint(Point mouseDragLocation)
	{
		Vect dp = new Vect(this.firstPointMouseDisplay.x - mouseDragLocation.x, this.firstPointMouseDisplay.y - mouseDragLocation.y);
		dp = this.displayToImage(dp);
		Point newLocation = new Point((int) (this.firstPointSrcCenter.x + dp.dX), (int) (this.firstPointSrcCenter.y + dp.dY));
		this.srcCenter.x = newLocation.x;
		this.srcCenter.y = newLocation.y;
		Logs.log("Dragging image to " + newLocation.x + "," + newLocation.y, 2, this);
		this.extractImage();
		this.repaint();
	}
	
	// -------------------------------------------------
	// -------------- ROI action -----------------------
	// -------------------------------------------------
	private ROIPlus activeROI;
	private HashSet<ROIPlus> rois;
	private HashMap<ROIPlus,Color> colors = new HashMap<ROIPlus,Color>();
	private Trajectory trajectory;
	private Point clickedPoint;
	private int radius;
	
	/**
	 * Set the active roi... default color is yellow
	 * 
	 * @param roi
	 */
	public void setActiveRoi(ROIPlus roi)
	{
		this.activeROI = roi;
		// extractImage();
		this.repaint();
	}
	
	/**
	 * Add a roi to the list of non active rois... default color is cyan
	 * 
	 * @param roi
	 */
	public void addRoi(ROIPlus roi)
	{
		if(this.rois == null)
		{
			this.rois = new HashSet<ROIPlus>();
		}
		if(roi == null)
		{
			return;
		}
		this.rois.add(roi);
		// extractImage();
		this.repaint();
	}
	
	/**
	 * Add a roi to the list of non active rois... default color is cyan
	 * 
	 * @param roi
	 */
	public void addRoi(ROIPlus roi, Color color)
	{
		if(this.rois == null)
		{
			this.rois = new HashSet<ROIPlus>();
		}
		if(roi == null)
		{
			return;
		}
		this.rois.add(roi);
		this.colors.put(roi, color);
		// extractImage();
		this.repaint();
	}
	
	/**
	 * Remove a roi from the list of non active rois
	 * 
	 * @param roi
	 */
	public void removeRoi(ROIPlus roi)
	{
		if(this.rois == null)
		{
			this.rois = new HashSet<ROIPlus>();
		}
		this.rois.remove(roi);
		// extractImage();
		this.repaint();
	}
	
	/**
	 * Set the whole list of rois... default color is cyan
	 * 
	 * @param rois
	 */
	public void setRois(HashSet<ROIPlus> rois)
	{
		this.rois = rois;
		// extractImage();
		this.repaint();
	}
	
	/**
	 * Returns the current active roi
	 * 
	 * @return ROIPlus
	 */
	public ROIPlus getActiveRoi()
	{
		return this.activeROI;
	}
	
	public void displayTrajectory(Trajectory trajectory)
	{
		this.trajectory = trajectory;
	}
	
	public void setRadius(int radius)
	{
		this.radius = radius;
	}
	
	public void setClickedPoint(Point p)
	{
		this.clickedPoint = p;
	}
	
	// -----------------------------------------------------------
	// -------------- Image preparation and display --------------
	// -----------------------------------------------------------
	// Variables for extracted image display
	private double scale = 1; // scale of displayed image
	// private int dstRect.x = 0 ; // x
	// location of image in frame
	// private int dstRect.y = 0 ; // y
	// location of image in frame
	private int wpane = 0; // width of image canvas
	private int hpane = 0; // height of image canvas
	private boolean scanLeft = false;
	private boolean scanRight = false;
	private boolean scanUp = false;
	private boolean scanDown = false;
	
	/**
	 * Extract image to display from source image
	 */
	private void extractImage()
	{
		
		if(this.adjustedSource == null)
		{
			this.srcRect = new Rectangle(0, 0, 0, 0);
			return;
		}
		
		this.srcRect.width = (int) (this.imageRect.width / this.zoom);
		if(this.srcRect.width >= this.imageRect.width)
		{
			this.srcCenter.x = this.srcRect.width / 2;
			this.srcRect.width = this.imageRect.width;
		}
		
		this.srcRect.height = (int) (this.imageRect.height / this.zoom);
		if(this.srcRect.height >= this.imageRect.height)
		{
			this.srcCenter.y = this.srcRect.height / 2;
			this.srcRect.height = this.imageRect.height;
		}
		
		// change here
		
		this.srcRect.x = this.srcCenter.x - this.srcRect.width / 2;
		if(this.srcRect.x + this.srcRect.width > this.imageRect.width)
		{
			this.srcRect.x = this.imageRect.width - this.srcRect.width - 1;
			this.srcCenter.x = this.imageRect.width - this.srcRect.width / 2;
		}
		if(this.srcRect.x < 0)
		{
			this.srcRect.x = 0;
			this.srcCenter.x = this.srcRect.width / 2;
		}
		
		this.srcRect.y = this.srcCenter.y - this.srcRect.height / 2;
		if(this.srcRect.y + this.srcRect.height > this.imageRect.height)
		{
			this.srcRect.y = this.imageRect.height - this.srcRect.height - 1;
			this.srcCenter.y = this.imageRect.height - this.srcRect.height / 2;
		}
		if(this.srcRect.y < 0)
		{
			this.srcRect.y = 0;
			this.srcCenter.y = this.srcRect.height / 2;
		}
		
		if(this.srcRect.x > 1)
		{
			this.scanLeft = true;
		}
		else
		{
			this.scanLeft = false;
		}
		if(this.srcRect.y > 1)
		{
			this.scanUp = true;
		}
		else
		{
			this.scanUp = false;
		}
		if(this.srcRect.x + this.srcRect.width < this.imageRect.width - 1)
		{
			this.scanRight = true;
		}
		else
		{
			this.scanRight = false;
		}
		if(this.srcRect.y + this.srcRect.height < this.imageRect.height - 1)
		{
			this.scanDown = true;
		}
		else
		{
			this.scanDown = false;
		}
		
		// Logs.log("Extracting image srcRect.x = "+srcRect.x+", srcRect.y = "+srcRect.y,
		// 1, this);
		// Logs.log("Extracting image srcRect.width = "+srcRect.width+", srcRect.height = "+srcRect.height,
		// 1, this);
		
		if(this.srcRect.width == 0 || this.srcRect.height == 0)
		{
			this.srcRect = new Rectangle(0, 0, 0, 0);
		}
		else
		{
			this.srcRect = new Rectangle(this.srcRect.x, this.srcRect.y, this.srcRect.width, this.srcRect.height);
		}
		
	}
	
	/**
	 * Paint this componement with cool colors
	 */
	@Override
	public void paint(Graphics g)
	{
		// Logs.log("Painting", 2, this);
		
		// get graphics
		Graphics2D g2 = (Graphics2D) g;
		super.paintComponent(g2);
		
		// paint a background rectangle to erase the modified image
		this.wpane = this.getWidth();
		this.hpane = this.getHeight();
		g2.setColor(this.background);
		g2.fillRect(0, 0, this.wpane, this.hpane);
		
		// // Scale image to take as much area of frame as possible
		// if (toDisplay == null) return;
		
		// Find the new scale of the image
		double scaleX = ((double) this.wpane) / ((double) this.srcRect.width);
		double scaleY = ((double) this.hpane) / ((double) this.srcRect.height);
		this.scale = Math.min(scaleX, scaleY);
		int newW = (int) (this.scale * this.srcRect.width);
		int newH = (int) (this.scale * this.srcRect.height);
		
		this.dstRect.x = this.wpane / 2 - newW / 2;
		this.dstRect.y = this.hpane / 2 - newH / 2;
		
		this.dstRect = new Rectangle(this.dstRect.x, this.dstRect.y, newW, newH);
		this.displayPanel.setBounds(this.dstRect);
		
		// // resize the image
		// if (image == null || imw!=newW || imh!=newH){
		// imw = newW;
		// imh = newH;
		// image = toDisplay.getScaledInstance(newW, newH, Image.SCALE_DEFAULT);
		// }
		
		// draw the image
		g2.setColor(this.background);
		g2.fillRect(0, 0, this.wpane, this.hpane);
		g2.drawImage(this.adjustedSource, this.dstRect.x, this.dstRect.y, this.dstRect.x + this.dstRect.width, this.dstRect.y + this.dstRect.height, this.srcRect.x, this.srcRect.y, this.srcRect.x + this.srcRect.width, this.srcRect.y + this.srcRect.height, null);
		// g2.drawImage(toDisplay, dstRect.x, dstRect.y,this);
		
		// draw the title
		g2.setColor(Color.CYAN);
		g2.drawString(this.title, this.dstRect.x + 10, this.dstRect.y + 20);
		
		// draw zoom level
		int zoomInt = (int) (this.zoom * 100.0);
		g2.setColor(Color.CYAN);
		g2.drawString("Zoom = " + zoomInt + "%", this.dstRect.x + newW - 100, this.dstRect.y + newH - 10);
		
		// draw triangles to signify panning possibility
		g2.setColor(Color.CYAN);
		int tHeight = 15;
		int tHalfWidth = 6;
		int tSpacing = 5;
		if(this.scanLeft)
		{
			int x1 = this.dstRect.x + tSpacing;
			int y1 = this.dstRect.y + newH / 2;
			int x2 = x1 + tHeight;
			int y2 = y1 - tHalfWidth;
			int x3 = x1 + tHeight;
			int y3 = y1 + tHalfWidth;
			int[] xPoints = new int[] { x1, x2, x3 };
			int[] yPoints = new int[] { y1, y2, y3 };
			g2.fillPolygon(xPoints, yPoints, 3);
		}
		if(this.scanRight)
		{
			int x1 = this.dstRect.x + newW - tSpacing;
			int y1 = this.dstRect.y + newH / 2;
			int x2 = x1 - tHeight;
			int y2 = y1 - tHalfWidth;
			int x3 = x1 - tHeight;
			int y3 = y1 + tHalfWidth;
			int[] xPoints = new int[] { x1, x2, x3 };
			int[] yPoints = new int[] { y1, y2, y3 };
			g2.fillPolygon(xPoints, yPoints, 3);
		}
		if(this.scanUp)
		{
			int x1 = this.dstRect.x + newW / 2;
			int y1 = this.dstRect.y + tSpacing;
			int x2 = x1 - tHalfWidth;
			int y2 = y1 + tHeight;
			int x3 = x1 + tHalfWidth;
			int y3 = y1 + tHeight;
			int[] xPoints = new int[] { x1, x2, x3 };
			int[] yPoints = new int[] { y1, y2, y3 };
			g2.fillPolygon(xPoints, yPoints, 3);
		}
		if(this.scanDown)
		{
			int x1 = this.dstRect.x + newW / 2;
			int y1 = this.dstRect.y + newH - tSpacing;
			int x2 = x1 - tHalfWidth;
			int y2 = y1 - tHeight;
			int x3 = x1 + tHalfWidth;
			int y3 = y1 - tHeight;
			int[] xPoints = new int[] { x1, x2, x3 };
			int[] yPoints = new int[] { y1, y2, y3 };
			g2.fillPolygon(xPoints, yPoints, 3);
		}
		
		// Paint the Rois
		this.paintRois(g2);
		this.paintActiveRoi(g2);
		this.paintTrajectory(g2);
		this.paintClickedPoint(g2);
	}
	
	/**
	 * Paint the active Roi a certain color
	 * 
	 * @param g2
	 */
	public void paintActiveRoi(Graphics2D g2)
	{
		if(this.activeROI != null)
		{
			this.paintRoi(g2, this.activeROI, Color.YELLOW);
		}
	}
	
	/**
	 * Paint the inactive Roi a certain color
	 * 
	 * @param g2
	 */
	public void paintRois(Graphics2D g2)
	{
		if(this.rois == null)
		{
			return;
		}
		for (ROIPlus roip : this.rois)
		{
			this.paintRoi(g2, roip, Color.CYAN);
		}
	}
	
	/**
	 * Paint single non-active roi
	 * 
	 * @param g2
	 *            graphics2D
	 * @param roip
	 *            RoiPlus
	 */
	private void paintRoi(Graphics2D g2, ROIPlus roip, Color color)
	{
		// String type = roip.type;
		Color colorToPaint = (this.colors.get(roip) == null) ? color : this.colors.get(roip);
		g2.setColor(colorToPaint);
		PointList points = roip.getPointList();
		for (Point p : points)
		{
			this.paintPoint(g2, colorToPaint, p);
		}
		HashSet<Point[]> lines = roip.getListOfLines();
		for (Point[] l : lines)
		{
			this.paintLine(g2, colorToPaint, l);
		}
	}
	
	/**
	 * Paint the trajectory on the image
	 * 
	 * @param g2
	 */
	private void paintTrajectory(Graphics2D g2)
	{
		if(this.trajectory == null)
		{
			return;
		}
		g2.setColor(Color.YELLOW);
		int frame1 = this.trajectory.initial();
		Point f2 = this.trajectory.getPoint(frame1);
		Point s2 = f2;
		
		while (s2 != null)
		{
			this.paintPoint(g2, Color.yellow, f2);
			this.paintLine(g2, Color.yellow, new Point[] { f2, s2 });
			// g2.fillOval(f2.x,f2.y,3,3);
			// g2.drawLine(f2.x, f2.y, s2.x, s2.y);
			f2 = s2;
			frame1 = this.trajectory.next(frame1);
			s2 = this.trajectory.getPoint(frame1);
		}
	}
	
	/**
	 * Paint a rectangle around the clicked point
	 */
	private void paintClickedPoint(Graphics2D g2)
	{
		if(this.clickedPoint == null)
		{
			return;
		}
		int dispRadius = (int) (this.radius * this.scale);
		this.paintRectangle(g2, Color.red, this.clickedPoint, dispRadius);
	}
	
	/**
	 * Paint point of roi in the display skip point if out of the field of view
	 * 
	 * @param p
	 */
	private void paintPoint(Graphics2D g2, Color color, Point p)
	{
		Point newP = this.imageToDisplay(p);
		if(this.isInDisplay(newP))
		{
			g2.setColor(color);
			g2.fillRect(newP.x - 1, newP.y - 1, 3, 3);
		}
	}
	
	/**
	 * Paint line in the display, crop the line if it goes out of the field of view
	 * 
	 * @param p1
	 * @param p2
	 */
	private void paintLine(Graphics2D g2, Color color, Point[] line)
	{
		Point p1 = this.imageToDisplay(line[0]);
		Point p2 = this.imageToDisplay(line[1]);
		
		boolean isP1 = this.isInDisplay(p1);
		boolean isP2 = this.isInDisplay(p2);
		
		if(isP1 && isP2)
		{
			g2.setColor(color);
			g2.drawLine(p1.x, p1.y, p2.x, p2.y);
			return;
		}
		
		double slope = ((double) (p2.y - p1.y)) / ((double) (p2.x - p1.x));
		
		if(!isP1 && isP2)
		{
			Point p11 = this.findFirstPointOfLineInDisplay(p1, slope);
			if(p11 == null)
			{
				return;
			}
			g2.setColor(color);
			g2.drawLine(p11.x, p11.y, p2.x, p2.y);
		}
		else if(isP1 && !isP2)
		{
			Point p21 = this.findFirstPointOfLineInDisplay(p2, slope);
			if(p21 == null)
			{
				return;
			}
			g2.setColor(color);
			g2.drawLine(p1.x, p1.y, p21.x, p21.y);
		}
		else
		{
			Point p11 = this.findFirstPointOfLineInDisplay(p1, slope);
			Point p21 = this.findFirstPointOfLineInDisplay(p2, slope);
			if(p11 == null || p21 == null)
			{
				return;
			}
			g2.setColor(color);
			g2.drawLine(p11.x, p11.y, p21.x, p21.y);
		}
	}
	
	/**
	 * Paint line in the display, crop the line if it goes out of the field of view
	 * 
	 * @param p1
	 * @param p2
	 */
	private void paintRectangle(Graphics2D g2, Color color, Point p, int radius)
	{
		Point newP = this.imageToDisplay(p);
		if(this.isInDisplay(newP))
		{
			g2.setColor(color);
			g2.drawRect(newP.x - radius, newP.y - radius, 2 * radius + 1, 2 * radius + 1);
		}
	}
	
	// ---------------------------------------------------------------------------------
	// -------------- Utilities to pass point from display to image world
	// --------------
	// ---------------------------------------------------------------------------------
	
	/**
	 * Return a point in the image space from a point on the screen
	 * 
	 * @param p
	 * @return point
	 */
	private Point displayToImage(Point p)
	{
		// Values are truncated to the upperleft corner on purpose
		// Find distance of point from top left corner of dstRect image
		int x1 = (int) (((p.x - this.dstRect.x)) / this.scale);
		int y1 = (int) (((p.y - this.dstRect.y)) / this.scale);
		
		// // Find distance of point from top left corner of real image
		// int x2 = (int) ((double) x1 / zoom);
		// int y2 = (int) ((double) y1 / zoom);
		
		// Find coordinates of point in image
		int x3 = x1 + this.srcRect.x;
		int y3 = y1 + this.srcRect.y;
		
		Point result = new Point(x3, y3);
		return result;
	}
	
	/**
	 * Return a point in the display space from a point on the image
	 * 
	 * @param p
	 * @return
	 */
	private Point imageToDisplay(Point p)
	{
		// Find coordinate of point relative to top left displayed corner
		int x3 = p.x - this.srcRect.x;
		int y3 = p.y - this.srcRect.y;
		
		// Find coordinate of point in displayed (extracted) image
		// int x2 = (int) ((double) x3 * zoom);
		// int y2 = (int) ((double) y3 * zoom);
		
		// Find coordinate of point in displayed (scaled) image
		int x1 = (int) ((x3) * this.scale + this.dstRect.x);
		int y1 = (int) ((y3) * this.scale + this.dstRect.y);
		
		Point result = new Point(x1, y1);
		return result;
	}
	
	/**
	 * Return a vector in the image space from the display space
	 * 
	 * @param v
	 * @return
	 */
	private Vect displayToImage(Vect v)
	{
		Vect result = v.duplicate();
		result.multiply(1 / this.scale);
		return result;
	}
	
	/**
	 * Return a vector in display space from the image space
	 * 
	 * @param v
	 * @return
	 */
	@SuppressWarnings("unused")
	private Vect imageToDisplay(Vect v)
	{
		Vect result = v.duplicate();
		result.multiply(this.scale);
		return result;
	}
	
	/**
	 * Returns true if point p should be displayed on the canvas
	 * 
	 * @param p
	 * @return boolean
	 */
	private boolean isInDisplay(Point p)
	{
		if(p.x > this.dstRect.x && p.y > this.dstRect.y && p.x < this.wpane - this.dstRect.x && p.y < this.hpane - this.dstRect.y)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Return the first point in the line passing by point P with slope SLOPE in the display range, return null if no overlap
	 * 
	 * @param p
	 * @param slope
	 * @return Point
	 */
	private Point findFirstPointOfLineInDisplay(Point p, double slope)
	{
		if(p.x < this.dstRect.x)
		{
			int y = p.y + (int) (slope * (this.dstRect.x - p.x));
			Point result = new Point(this.dstRect.x + 1, y);
			if(this.isInDisplay(result))
			{
				return result;
			}
		}
		else if(p.x > this.wpane - this.dstRect.x)
		{
			int y = p.y - (int) (slope * (p.x - this.wpane + this.dstRect.x));
			Point result = new Point(this.wpane - this.dstRect.x - 1, y);
			if(this.isInDisplay(result))
			{
				return result;
			}
		}
		else if(p.y < this.dstRect.y)
		{
			int x = p.x + (int) ((1 / slope) * (this.dstRect.y - p.y));
			Point result = new Point(x, this.dstRect.y + 1);
			if(this.isInDisplay(result))
			{
				return result;
			}
		}
		else if(p.y > this.hpane - this.dstRect.y)
		{
			int x = p.x - (int) ((1 / slope) * ((p.y - this.hpane + this.dstRect.y)));
			Point result = new Point(x, this.hpane - this.dstRect.y - 1);
			if(this.isInDisplay(result))
			{
				return result;
			}
		}
		
		return null;
	}
	
	// --------------------------------------------------
	// -------------- Mouse and key events --------------
	// --------------------------------------------------
	// private Point firstPoint = null; // To be replaced by second point to
	// find difference between updates
	private Point firstPointMouseDisplay = null; // Point saved for initiation
	// of dragging of image
	private Point firstPointSrcCenter = null; // Point saved to keep track of
	
	// original position;
	// private Point secondPoint =
	// null ; // Point saved for
	// dragging of image
	// private boolean dragging =
	// false; // Is in dragging
	// mode
	// private Rectangle rectTmp =
	// null ; // temporary
	// rectangle roi
	
	@Override
	public void mouseClicked(MouseEvent e)
	{
		if(e.getClickCount() > 1)
		{
			return;
		}
		if(e.getButton() == MouseEvent.BUTTON3)
		{
			// if (lowLevelController != null)
			// lowLevelController.clickedPoint(displayToImage(e.getPoint()));
			if(this.highLevelController != null)
			{
				this.highLevelController.rightClickedPoint(this.displayToImage(e.getPoint()));
				// Logs.log("Mouse right clicked", 0, this);
			}
		}
		// if (e.getButton() == MouseEvent.BUTTON1){
		// Point p = e.getPoint();
		// Point p2 = new Point(p.x + (int)dstRect.getX(), p.y +
		// (int)dstRect.getY());
		//
		// //if (lowLevelController != null)
		// lowLevelController.clickedPoint(displayToImage(scaledP));
		// if (highLevelController != null)
		// highLevelController.clickedPoint(displayToImage(p2));
		// //Logs.log("Mouse left clicked", 0, this);
		// }
	}
	
	@Override
	public void mouseEntered(MouseEvent e)
	{   
		
	}
	
	@Override
	public void mouseExited(MouseEvent e)
	{   
		
	}
	
	@Override
	public void mousePressed(MouseEvent e)
	{
		this.firstPointSrcCenter = new Point(this.srcCenter.x, this.srcCenter.y);
		this.firstPointMouseDisplay = e.getPoint();
	}
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
		this.firstPointMouseDisplay = null;
		this.firstPointSrcCenter = null;
		
		if(e.getButton() == MouseEvent.BUTTON1)
		{
			Point p = e.getPoint();
			Point p2 = new Point(p.x + (int) this.dstRect.getX(), p.y + (int) this.dstRect.getY());
			if(this.highLevelController != null)
			{
				this.highLevelController.clickedPoint(this.displayToImage(p2));
			}
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent e)
	{
		Point scaledP = e.getPoint();
		
		if(e.isAltDown() && this.canZoomAndPan)
		{
			this.dragToPoint(scaledP);
		}
		// if (!e.isAltDown() && lowLevelController != null)
		// lowLevelController.mouseMoved(displayToImage(scaledP));
		if(!e.isAltDown() && this.highLevelController != null)
		{
			// int x = Math.min(firstPointMouseDisplay.x, scaledP.x);
			// int y = Math.min(firstPointMouseDisplay.y, scaledP.y);
			// int w = Math.abs(scaledP.x-firstPointMouseDisplay.x);
			// int h = Math.abs(scaledP.y-firstPointMouseDisplay.y);
			int x = this.firstPointMouseDisplay.x;
			int y = this.firstPointMouseDisplay.y;
			int w = scaledP.x - this.firstPointMouseDisplay.x;
			int h = scaledP.y - this.firstPointMouseDisplay.y;
			Rectangle rectTmp = new Rectangle(x, y, w, h);
			this.highLevelController.extendedRectangle(rectTmp);
		}
		
		return;
	}
	
	@Override
	public void mouseMoved(MouseEvent e)
	{}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if(this.canZoomAndPan)
		{
			// Zoom around mouse pointer
			int units = e.getWheelRotation();
			Point scaledP = e.getPoint();
			this.zoom(-units, this.displayToImage(scaledP));
		}
	}
	
}
