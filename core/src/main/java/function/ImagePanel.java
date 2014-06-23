package function;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import image.roi.PointList;
import image.roi.Trajectory;

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
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;

public class ImagePanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
	
	private static final long serialVersionUID = 1L;
	
	// statics
	public static int zoomFactor = 2;
	
	// Class variables
	private ImagePanelInteractor controller;
	private ImagePlus source = null;
	private String title = "";
	
	private int orgX = 0; // x
	// location
	// of
	// image
	// in
	// frame
	private int orgY = 0; // y
	// location
	// of
	// image
	// in
	// frame
	private double scale = 1.0; // scale
	// of
	// displayed
	// image
	private int zoom = 1; // current
	// zoom
	// scale
	// private
	// int
	// transX
	// =
	// 0
	// ;
	// //
	// current
	// x
	// translation
	// of
	// image
	// private
	// int
	// transY
	// =
	// 0
	// ;
	// //
	// current
	// y
	// translation
	// of
	// image
	
	// ROI
	private Roi roi;
	
	// POINT LIST
	private PointList pList;
	private int cellRadius = 5;
	
	// POINT LIST ARRAY
	private PointList[] pLists;
	private Color[] colors;
	
	// TRACKS
	private Trajectory[] tracks;
	
	// Various ROIs
	private HashMap<Object,Color> roiMap;
	
	// display variables
	private Color background = DisplayStatics.background;
	private JLabel titleLabel = new JLabel();
	private Image image;
	private JPanel displayPanel;
	
	public ImagePanel(ImagePanelInteractor controller, String title)
	{
		this.controller = controller;
		this.title = title;
		
		this.roiMap = new HashMap<Object,Color>();
		this.initialize();
	}
	
	/**
	 * Initialize panel
	 */
	private void initialize()
	{
		this.setBackground(background);
		this.setLayout(new BorderLayout());
		
		displayPanel = new JPanel();
		displayPanel.setBackground(background);
		displayPanel.setLayout(new BorderLayout());
		displayPanel.addMouseListener(this);
		displayPanel.addMouseMotionListener(this);
		
		titleLabel.setText(title);
		titleLabel.setPreferredSize(new Dimension(200, 20));
		titleLabel.setMaximumSize(new Dimension(200, 20));
		titleLabel.setForeground(Color.WHITE);
		
		this.add(titleLabel, BorderLayout.PAGE_START);
		this.add(displayPanel, BorderLayout.CENTER);
	}
	
	/**
	 * Set the displayed image
	 * 
	 * @param image
	 */
	public void setImage(ImagePlus image)
	{
		this.source = image;
		this.repaint();
	}
	
	/**
	 * Set the displayed image
	 * 
	 * @param image
	 */
	public void setTitle(String title)
	{
		this.title = title;
		this.repaint();
	}
	
	public void zoom(int units, int centerX, int centerY)
	{
		zoom = ImagePanel.zoomFactor * zoom;
		
	}
	
	public void translate(int dx, int dy)
	{   
		
	}
	
	/**
	 * Add a roi to the roi list
	 * 
	 * @param roi
	 * @param color
	 */
	public void addRoiWithColor(Object roi, Color color)
	{
		roiMap.put(roi, color);
		this.repaint();
	}
	
	/**
	 * Remove roi from roi list
	 * 
	 * @param roi
	 */
	public void removeRoi(Object roi)
	{
		roiMap.remove(roi);
		this.repaint();
	}
	
	/**
	 * Reset the roi list
	 */
	public void resetRois()
	{
		this.roiMap = new HashMap<Object,Color>();
		this.repaint();
	}
	
	/**
	 * Set the roi in this image
	 * 
	 * @param roi
	 */
	public void setRoi(Roi roi)
	{
		this.roi = roi;
		this.repaint();
	}
	
	/**
	 * Set the point list for display
	 * 
	 * @param pList
	 */
	public void setPointList(PointList pList)
	{
		this.pList = pList;
		this.repaint();
	}
	
	/**
	 * Set the cell radius
	 * 
	 * @param cellRadius
	 */
	public void setCellRadius(int cellRadius)
	{
		this.cellRadius = cellRadius;
	}
	
	/**
	 * Display an array of pointlists with specific colors
	 * 
	 * @param pList
	 * @param colors
	 */
	public void setPointListArray(PointList[] pLists, Color[] colors)
	{
		this.pLists = pLists;
		this.colors = colors;
		this.repaint();
	}
	
	/**
	 * Display an array of pointlists with specific colors
	 * 
	 * @param pList
	 * @param colors
	 */
	public void setTracks(Trajectory[] tracks)
	{
		this.tracks = tracks;
		this.repaint();
	}
	
	/**
	 * Return the actual point location
	 * 
	 * @param p
	 * @return point
	 */
	public Point backScale(Point p)
	{
		int x = (int) (p.x / scale);
		int y = (int) (p.y / scale);
		Point result = new Point(x, y);
		return result;
	}
	
	/**
	 * Return the actual dimension
	 * 
	 * @param dim
	 * @return newDimension
	 */
	public Dimension backScale(Dimension dim)
	{
		int x = (int) (dim.getWidth() / scale);
		int y = (int) (dim.getHeight() / scale);
		Dimension result = new Dimension(x, y);
		return result;
	}
	
	/**
	 * Return the actual point location
	 * 
	 * @param p
	 * @return point
	 */
	public Point scale(Point p)
	{
		int x = (int) (p.x * scale);
		int y = (int) (p.y * scale);
		Point result = new Point(x, y);
		return result;
	}
	
	/**
	 * Return the actual dimension
	 * 
	 * @param dim
	 * @return newDimension
	 */
	public Dimension scale(Dimension dim)
	{
		int x = (int) (dim.getWidth() * scale);
		int y = (int) (dim.getHeight() * scale);
		Dimension result = new Dimension(x, y);
		return result;
	}
	
	/**
	 * Paint this componement with cool colors
	 */
	@Override
	public void paint(Graphics g)
	{
		// Logs.log("Repainting",1,this);
		Graphics2D g2 = (Graphics2D) g;
		super.paintComponent(g2);
		
		int wpane = this.getWidth();
		int hpane = this.getHeight();
		g2.setColor(background);
		g2.fillRect(0, 0, wpane, hpane);
		
		if(source == null || source.getProcessor() == null)
			return;
		
		// Find the new scale of the image
		int w = source.getWidth();
		int h = source.getHeight();
		double scaleX = ((double) wpane) / ((double) w);
		double scaleY = ((double) hpane) / ((double) h);
		this.scale = Math.min(scaleX, scaleY);
		int newW = (int) (scale * w);
		int newH = (int) (scale * h);
		
		orgX = wpane / 2 - newW / 2;
		orgY = hpane / 2 - newH / 2;
		
		Rectangle bou = new Rectangle(orgX, orgY, newW, newH);
		displayPanel.setBounds(bou);
		
		// resize the image
		ImageProcessor imp = source.getProcessor();
		imp = imp.resize(newW);
		image = imp.getBufferedImage();
		
		// draw the image
		g2.setColor(background);
		g2.fillRect(0, 0, wpane, hpane);
		g2.drawImage(image, orgX, orgY, this);
		
		// draw the title
		g2.setColor(Color.WHITE);
		g2.drawString(title, orgX + 10, orgY + 20);
		
		if(roi != null)
			displayRoi(roi, Color.yellow, g2);
		
		if(pList != null)
			displayPointList(pList, Color.RED, g2);
		
		if(pLists != null)
		{
			for (int i = 0, len = pLists.length; i < len; i++)
			{
				g2.setColor(colors[i]);
				PointList pointlist = pLists[i];
				displayPointList(pointlist, colors[i], g2);
			}
		}
		
		if(tracks != null)
		{
			g2.setColor(Color.YELLOW);
			for (int k = 0, len = tracks.length; k < len; k++)
			{
				Trajectory t = tracks[k];
				displayTrajectory(t, Color.yellow, g2);
			}
		}
		
		Set<Object> rois = roiMap.keySet();
		for (Object roi : rois)
		{
			Color c = roiMap.get(roi);
			
			if(roi instanceof Trajectory)
			{
				displayTrajectory((Trajectory) roi, c, g2);
			}
			else if(roi instanceof PointList)
			{
				displayPointList((PointList) roi, c, g2);
			}
			else if(roi instanceof Roi)
			{
				displayRoi((Roi) roi, c, g2);
			}
		}
	}
	
	private void displayRoi(Roi roi, Color c, Graphics2D g2)
	{
		// Rectangle r = roi.getBoundingRect();
		Rectangle r = roi.getBounds();
		Point rp = r.getLocation();
		Dimension dim = r.getSize();
		Point newRP = scale(rp);
		Dimension d = scale(dim);
		
		// draw the image
		g2.setColor(c);
		g2.drawRect(newRP.x + orgX, newRP.y + orgY, d.width, d.height);
	}
	
	private void displayPointList(PointList pointlist, Color c, Graphics2D g2)
	{
		if(pointlist == null)
			return;
		g2.setColor(c);
		int rad = (int) (scale * cellRadius);
		for (int k = 0, lenk = pointlist.size(); k < lenk; k++)
		{
			Point p = pointlist.get(k);
			Point newP = scale(p);
			g2.drawRect(newP.x - rad + orgX, newP.y - rad + orgY, 2 * rad, 2 * rad);
		}
	}
	
	private void displayTrajectory(Trajectory t, Color c, Graphics2D g2)
	{
		g2.setColor(c);
		int frame1 = t.initial();
		Point first = t.getPoint(frame1);
		Point second = first;
		
		while (second != null)
		{
			Point f2 = scale(first);
			Point s2 = scale(second);
			g2.fillOval(f2.x + orgX, f2.y + orgY, 3, 3);
			g2.drawLine(f2.x + orgX, f2.y + orgY, s2.x + orgX, s2.y + orgY);
			
			first = second;
			frame1 = t.next(frame1);
			second = t.getPoint(frame1);
		}
	}
	
	public void mouseClicked(MouseEvent e)
	{}
	
	public void mouseEntered(MouseEvent e)
	{}
	
	public void mouseExited(MouseEvent e)
	{}
	
	public void mousePressed(MouseEvent e)
	{
		
		if(e.isControlDown())
		{
			return;
		}
		
		if(this.controller == null)
			return;
		
		try
		{
			Point scaledP = e.getPoint();
			controller.pressedPoint(backScale(scaledP));
		}
		catch (Exception ex)
		{
			return;
		}
	}
	
	public void mouseReleased(MouseEvent e)
	{
		
		if(e.isControlDown())
		{
			
			return;
		}
		
		if(this.controller == null)
			return;
		Point scaledP = e.getPoint();
		controller.clickedPoint(backScale(scaledP));
	}
	
	public void mouseDragged(MouseEvent e)
	{
		
		if(e.isControlDown())
		{
			
			return;
		}
		
		Point scaledP = e.getPoint();
		controller.mouseMoved(backScale(scaledP));
	}
	
	public void mouseMoved(MouseEvent e)
	{}
	
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int units = e.getWheelRotation();
		int posX = e.getX();
		int posY = e.getY();
		zoom(units, posX, posY);
	}
	
}
