package image.roi;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.HashSet;
import java.util.Iterator;

import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import miscellaneous.Copiable;
import net.imglib2.roi.geometric.Polygon;

public class ROIPlus implements Copiable<ROIPlus>, Iterable<ROIPlus>, Comparable<ROIPlus> {
	
	public static final int ROI_UNDEFINED = -1, ROI_LINE = 0, ROI_POLYLINE = 1, ROI_RECT = 2, ROI_ELLIPSE = 3, ROI_POLYGON = 4, ROI_POINT = 5;
	// public static String RECTANGLE = "JRectangle";
	// public static String ELIPSE = "JElipse";
	// public static String POINTSET = "JPolygon";
	// public static String POLYGON = "JPolygonLine";
	// public static String TRAJECTORY = "Trajectory";
	public static final String ORIGIN_CENTER = "Center", ORIGIN_UPPERLEFT = "Upper-Left", ORIGIN_UPPERRIGHT = "Upper-Right", ORIGIN_LOWERLEFT = "Lower-Left", ORIGIN_LOWERRIGHT = "Lower-Right";
	// Roi type
	public int type = ROI_RECT;
	
	// IJ ROI
	
	// Point list
	public PointList pointList;
	public PointList pattern;
	
	public ROIPlus(Roi roi)
	{
		if(roi.getType() == Roi.RECTANGLE)
		{
			type = ROI_RECT;
			Rectangle r = roi.getBounds();
			int x1 = r.x;
			int y1 = r.y;
			int x2 = r.width + r.x;
			int y2 = r.height + r.y;
			pointList = new PointList();
			pointList.add(x1, y1);
			pointList.add(x2, y2); // New
		}
		else if(roi.getType() == Roi.LINE)
		{
			type = ROI_LINE;
			Rectangle r = roi.getBounds();
			int x1 = r.x;
			int y1 = r.y;
			int x2 = r.width + r.x;
			int y2 = r.height + r.y;
			pointList = new PointList();
			pointList.add(x1, y1);
			pointList.add(x2, y2);
		}
		else if(roi.getType() == Roi.POINT)
		{
			type = ROI_POINT;
			PointRoi proi = (PointRoi) roi;
			pointList = new PointList(proi.getPolygon()); // New
		}
		else if(roi.getType() == Roi.POLYGON || roi.getType() == Roi.TRACED_ROI)
		{
			type = ROI_POLYGON;
			PolygonRoi proi = (PolygonRoi) roi;
			pointList = new PointList(proi.getPolygon()); // New
		}
		else if(roi.getType() == Roi.POLYLINE)
		{
			type = ROI_POLYLINE;
			PolygonRoi proi = (PolygonRoi) roi;
			pointList = new PointList(proi.getPolygon());
		}
		else if(roi.getType() == Roi.OVAL)
		{
			type = ROI_ELLIPSE;
			Rectangle r = roi.getBounds();
			int x1 = r.x;
			int y1 = r.y;
			int x2 = r.width + r.x;
			int y2 = r.height + r.y;
			pointList = new PointList();
			pointList.add(x1, y1);
			pointList.add(x2, y2);
		}
		
	}
	
	public ROIPlus(Roi roi, int type)
	{
		this(roi);
		this.type = type;
	}
	
	public ROIPlus(Polygon pg)
	{
		this(new PointList(pg), ROIPlus.ROI_POLYGON);
	}
	
	public ROIPlus(PointList pList, int type)
	{
		this.type = new Integer(type);
		
		if(pList == null)
		{
			this.pointList = new PointList();
		}
		else
		{
			this.pointList = new PointList(pList); // New
		}
	}
	
	public ROIPlus(Rectangle rectangle)
	{
		
		// New
		type = ROI_RECT;
		
		int x1 = rectangle.x;
		int y1 = rectangle.y;
		int x2 = x1 + rectangle.width;
		int y2 = y1 + rectangle.height;
		
		pointList = new PointList();
		pointList.add(new Point(x1,y1));
		pointList.add(new Point(x2,y2));
	}
	
	public ROIPlus(String pList, String type)
	{
		this(new PointList(pList), new Integer(type));
	}
	
	public Shape getShape()
	{
		if(this.isLine() || this.type == ROI_RECT || this.type == ROI_POINT)
		{
			if(this.pointList.size() == 0)
			{
				return null;
			}
			return this.pointList.getBounds();
		}
		if(this.type == ROI_ELLIPSE)
		{
			if(this.pointList.size() == 0)
			{
				return null;
			}
			return new Ellipse2D.Float(this.pointList.get(0).x, this.pointList.get(0).y, this.pointList.get(1).x - this.pointList.get(0).x, this.pointList.get(1).y - this.pointList.get(0).y);
		}
		if(this.type == ROI_POLYGON)
		{
			if(this.pointList.size() == 0)
			{
				return null;
			}
			return this.pointList.toPolygon();
		}
		return null;
	}
	
	public boolean isLine()
	{
		if(this.type == ROI_LINE || this.type == ROI_POLYLINE)
		{
			return true;
		}
		return false;
	}
	
	public Roi getRoi()
	{
		return this.createIJroi();
	}
	
	public double getLength()
	{
		return this.getPointList().getLength(this.isLine());
	}
	
	private Roi createIJroi()
	{
		Roi ret = null;
		if(this.pointList != null && this.pointList.size() > 0)
		{
			switch (this.type)
			{
				case ROI_RECT:
					ret = this.toRectangleRoi();
					break;
				case ROI_ELLIPSE:
					ret = this.toEllipseRoi();
					break;
				case ROI_LINE:
					ret = this.toLineRoi();
					break;
				case ROI_POLYLINE:
					ret = this.toPolylineRoi();
					break;
				case ROI_POLYGON:
					ret = this.toPolygonRoi();
					break;
				case ROI_POINT:
					ret = this.toPointRoi();
					break;
			}
		}
		return ret;
	}
	
	private Roi toRectangleRoi()
	{
		Rectangle r = this.pointList.getBounds();
		return new Roi(r.x, r.y, r.width, r.height);
	}
	
	private OvalRoi toEllipseRoi()
	{
		Rectangle r = this.pointList.getBounds();
		return new OvalRoi(r.x, r.y, r.width, r.height);
	}
	
	private Roi toLineRoi()
	{
		int x1 = this.pointList.get(0).x;
		int y1 = this.pointList.get(0).y;
		int x2 = this.pointList.get(1).x;
		int y2 = this.pointList.get(1).y;
		return new Line(x1, y1, x2, y2);
	}
	
	private PolygonRoi toPolygonRoi()
	{
		return new PolygonRoi(this.pointList.getXIntArray(), this.pointList.getYIntArray(), this.pointList.size(), Roi.POLYGON);
	}
	
	private PolygonRoi toPolylineRoi()
	{
		return new PolygonRoi(this.pointList.getXIntArray(), this.pointList.getYIntArray(), this.pointList.size(), Roi.POLYLINE);
	}
	
	private PolygonRoi toPointRoi()
	{
		return new PointRoi(this.pointList.getXIntArray(), this.pointList.getYIntArray(), this.pointList.size());
	}
	
	/**
	 * Return the list of points in the Roi. This is a live reference of the internal variable.
	 * 
	 * @return
	 */
	public PointList getPointList()
	{
		return pointList;
	}
	
	/**
	 * Set the points that define the pattern that this roi is a part of. The pattern is defined in relative terms from the first point in the list which is 0,0. This way if someone moves the roi, the pattern points don't need to be redefined. This
	 * method returns a live reference to the internal pattern variable.
	 */
	public PointList getPattern()
	{
		if(this.pattern == null)
		{
			this.pattern = new PointList();
			this.pattern.add(new Point(0, 0));
		}
		return this.pattern;
	}
	
	/**
	 * Sets the points that represent the pattern of this roi
	 * 
	 * @param pattern
	 */
	public void setPattern(PointList pattern)
	{
		if(pattern == null || pattern.size() == 0)
		{
			this.pattern = null;
			return;
		}
		// If the first point isn't 0,0, then translate all the points to make
		// it that way.
		if(pattern.get(0).x != 0 || pattern.get(0).y != 0)
		{
			pattern.translate(-1 * pattern.get(0).x, -1 * pattern.get(0).y);
		}
		this.pattern = pattern;
	}
	
	/**
	 * Sets the points that represent the pattern of this roi
	 * 
	 * @param pattern
	 */
	public void setPattern(String patternString)
	{
		if(patternString == null || patternString.equals(""))
		{
			this.pattern = null;
			return;
		}
		PointList patternToSet = new PointList(patternString);
		this.setPattern(patternToSet);
	}
	
	public Iterator<ROIPlus> iterator()
	{
		return this.patternRoiIterator();
	}
	
	/**
	 * Return the list of lines in the Roi
	 * 
	 * @return
	 */
	public HashSet<Point[]> getListOfLines()
	{
		HashSet<Point[]> result = new HashSet<Point[]>();
		if(pointList == null || pointList.size() == 0)
		{   
			
		}
		else if(type == ROI_RECT)
		{
			Point p1 = pointList.get(0);
			Point p2 = pointList.get(1);
			Point p3 = pointList.get(2);
			Point p4 = pointList.get(3);
			Point[] line1 = new Point[] { p1, p2 };
			Point[] line2 = new Point[] { p2, p3 };
			Point[] line3 = new Point[] { p3, p4 };
			Point[] line4 = new Point[] { p4, p1 };
			result.add(line1);
			result.add(line2);
			result.add(line3);
			result.add(line4);
		}
		else if(type == ROI_POINT)
		{}
		else if(type == ROI_POLYGON)
		{
			for (int i = 0, len = pointList.size() - 1; i < len; i++)
			{
				Point p1 = pointList.get(i);
				Point p2 = pointList.get(i + 1);
				Point[] line = new Point[] { p1, p2 };
				result.add(line);
			}
			Point p1 = pointList.get(0);
			Point p2 = pointList.get(pointList.size() - 1);
			Point[] line = new Point[] { p1, p2 };
			result.add(line);
		}
		else
		// Back compatibility for Trajectory
		{
			for (int i = 0, len = pointList.size() - 1; i < len; i++)
			{
				Point p1 = pointList.get(i);
				Point p2 = pointList.get(i + 1);
				Point[] line = new Point[] { p1, p2 };
				result.add(line);
			}
		}
		return result;
	}
	
	/**
	 * Return a low level copy of the ROIPLus
	 * 
	 * @return
	 */
	public ROIPlus copy()
	{
		PointList pList = this.pointList.copy();
		ROIPlus result = new ROIPlus(pList, this.type);
		if(this.pattern != null)
		{
			PointList patternCopy = this.pattern.copy();
			result.setPattern(patternCopy);
		}
		return result;
	}
	
	public PatternRoiIterator patternRoiIterator()
	{
		return new PatternRoiIterator(this);
	}
	
	public Iterator<PointList> patternPointListIterator()
	{
		return new PatternPointListIterator(this);
	}
	
	public class PatternRoiIterator implements Iterator<ROIPlus> {
		
		public ROIPlus roip;
		public Iterator<IdPoint> itr;
		private IdPoint currentPoint;
		
		public PatternRoiIterator(ROIPlus roi)
		{
			this.roip = roi;
			this.itr = this.roip.getPattern().iterator();
		}
		
		public boolean hasNext()
		{
			return itr.hasNext();
		}
		
		public ROIPlus next()
		{
			currentPoint = itr.next();
			ROIPlus copy = roip.copy();
			PointList points = copy.getPointList();
			points.translate(currentPoint.x, currentPoint.y);
			return copy;
		}
		
		public IdPoint currentPatternPoint()
		{
			return currentPoint.copy();
		}
		
		public void remove()
		{
			// TODO Auto-generated method stub
			
		}
		
	}
	
	class PatternPointListIterator implements Iterator<PointList> {
		
		public PointList points;
		public Iterator<IdPoint> itr;
		
		public PatternPointListIterator(ROIPlus roi)
		{
			this.points = roi.getPointList();
			this.itr = roi.getPattern().iterator();
		}
		
		public boolean hasNext()
		{
			return itr.hasNext();
		}
		
		public PointList next()
		{
			Point p = itr.next();
			PointList pointsCopy = points.copy();
			pointsCopy.translate(p.x, p.y);
			return pointsCopy;
		}
		
		public void remove()
		{
			// TODO Auto-generated method stub
			
		}
		
	}
	
	public static ROIPlus makeRoi(int x, int y, String roiOrigin, int roiType, int roiWidth, int roiHeight)
	{
		PointList pl = new PointList();
		Point p1 = new Point(0, 0);
		Point p2 = new Point(roiWidth, roiHeight);
		
		ROIPlus ret = null;
		
		if(roiType == ROIPlus.ROI_POINT)
		{
			pl.add(p1);
			ret = new ROIPlus(pl, roiType);
			return ret;
		}
		else
		{
			pl.add(p1);
			pl.add(p2);
		}
		
		double width = (double) roiWidth;
		double height = (double) roiHeight;
		
		if(roiOrigin.equals(ROIPlus.ORIGIN_CENTER))
		{
			pl.translate(-width / 2, -height / 2);
		}
		else if(roiOrigin.equals(ROIPlus.ORIGIN_UPPERRIGHT))
		{
			pl.translate(-width, 0);
		}
		else if(roiOrigin.equals(ROIPlus.ORIGIN_LOWERLEFT))
		{
			pl.translate(0, -height);
		}
		else if(roiOrigin.equals(ROIPlus.ORIGIN_LOWERRIGHT))
		{
			pl.translate(-width, -height);
		}
		
		pl.translate(x, y);
		
		ret = new ROIPlus(pl, roiType);
		return ret;
	}
	
	public int compareTo(ROIPlus roip)
	{
		return this.toString().compareTo(roip.toString());
	}
}
