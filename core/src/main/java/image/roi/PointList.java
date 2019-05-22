package image.roi;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import miscellaneous.CSVList;
import miscellaneous.Copiable;
import miscellaneous.LSVList;
import miscellaneous.SSVList;
import net.imglib2.AbstractCursor;
import net.imglib2.Cursor;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.Polygon2D;
import net.imglib2.type.numeric.integer.IntType;

@SuppressWarnings("unused")
public class PointList extends Vector<IdPoint> implements Copiable<PointList> {
	
	/**
	 * Need to rewrite getCenter and getBounds (currently circular)s
	 */
	private static final long serialVersionUID = 1L;
	
	public PointList()
	{
		super();
	}
	
	public PointList(PointList pl)
	{
		for (IdPoint p : pl)
		{
			this.add(p.x, p.y, p.id);
		}
	}
	
	public PointList(Polygon2D pg)
	{
		int id = 0;
		RealLocalizable p = null;
		for(int i = 0; i < pg.numVertices(); i++)
		{
			p = pg.vertex(i);
			this.add((int) (p.getDoublePosition(0) + 0.5), (int) (p.getDoublePosition(1) + 0.5), id);
			id = id + 1;
		}
	}
	
	public PointList(String polygonPts)
	{
		super();
		
		if(polygonPts != null)
		{
			SSVList polygon = new SSVList(polygonPts);
			Iterator<String> itr = polygon.iterator();
			while (itr.hasNext())
			{
				CSVList pt = new CSVList(itr.next());
				if(pt.get(0).equals("") || pt.get(1).equals(""))
					continue;
				int x = Integer.parseInt(pt.get(0));
				int y = Integer.parseInt(pt.get(1));
				int id = 0;
				if(pt.size() > 2)
				{
					id = Integer.parseInt(pt.get(2));
				}
				this.add(x, y, id);
			}
		}
	}
	
	public PointList(Point[] pa)
	{
		super();
		for (Point p : pa)
		{
			this.add(p);
		}
	}
	
	public PointList(Polygon pg)
	{
		this(polygonToPointArray(pg));
	}
	
	// public PointList(XPointList pl)
	// {
	// super();
	// this.setPoints(pl);
	// }
	//
	// public PointList(Iterable<IdPoint> pl)
	// {
	// super();
	// this.setPoints(pl);
	// }
	
	public boolean add(Point p)
	{
		return this.add(p.x, p.y, this.size());
	}
	
	public boolean add(int x, int y)
	{
		return this.add(x, y, this.size());
	}
	
	public boolean add(int x, int y, int id)
	{
		IdPoint temp = new IdPoint(x, y, id);
		return this.add(temp);
	}
	
	public Rectangle getBounds()
	{
		return this.toPolygon().getBounds();
	}
	
	/**
	 * The total length of all line segments unless isLine is true which
	 * then gives the length between the start and end point only
	 * @param isLine
	 * @return
	 */
	public double getLength(boolean isLine)
	{
		double length = 0;
		if(this.size() > 1)
		{
			Point start;
			Point end;
			
			double segment;
			for (int i = 1; i < this.size(); i++)
			{
				start = this.get(i - 1);
				end = this.get(i);
				length += start.distance(end);
			}
			if(!isLine)
			{
				length += this.get(0).distance(this.get(this.size() - 1));
			}
		}
		return length;
	}
	
	public IdPoint getCenter()
	{
		return getCenter(this.getBounds());
	}
	
	public void transform(double thetaDeg, double mag, int newLocationX, int newLocationY)
	{
		this.rotate(thetaDeg);
		this.scale(mag);
		this.setCenter(newLocationX, newLocationY);
	}
	
	public void transform(double thetaDeg, double mag, Point newLocation)
	{
		this.rotate(thetaDeg);
		this.scale(mag);
		this.setCenter(newLocation);
	}
	
	public void translate(double deltaX, double deltaY)
	{
		for (IdPoint p : this)
		{
			p.translate((int) Math.round(deltaX), (int) Math.round(deltaY));
		}
	}
	
	public void setCenter(int x, int y)
	{
		PointList l = this.getPointListRelativeToCenter();
		l.translate(x, y);
		this.setPoints(l);
	}
	
	public void setCenter(Point p)
	{
		this.setCenter(p.x, p.y);
	}
	
	/**
	 * Rotate the points about the center of their bounding box.
	 * @param thetaDeg
	 */
	public void rotate(double thetaDeg)
	{
		AffineTransform toApply = new AffineTransform();
		toApply.rotate(thetaDeg * Math.PI / 180);
		
		Point2D.Double[] srcPts = convert(this.getPointListRelativeToCenter().toArray());
		Point2D.Double[] dstPts = new Point2D.Double[srcPts.length];
		
		toApply.transform(srcPts, 0, dstPts, 0, srcPts.length);
		
		PointList newl = new PointList(convert(dstPts));
		newl.setCenter(this.getCenter());
		this.setPoints(newl);
	}
	
	/**
	 * Rotate the points about the origin of their coordinate system.
	 * @param thetaDeg
	 */
	public void rotateRelativeToOrigin(double thetaDeg)
	{
		AffineTransform toApply = new AffineTransform();
		toApply.rotate(thetaDeg * Math.PI / 180);
		
		Point2D.Double[] srcPts = convert(this.toArray());
		Point2D.Double[] dstPts = new Point2D.Double[srcPts.length];
		
		toApply.transform(srcPts, 0, dstPts, 0, srcPts.length);
		
		PointList newl = new PointList(convert(dstPts));
		//		newl.setCenter(this.getCenter());
		this.setPoints(newl);
	}
	
	public void scale(double mag)
	{
		AffineTransform toApply = new AffineTransform();
		toApply.scale(mag, mag);
		
		Point2D.Double[] srcPts = convert(this.getPointListRelativeToCenter().toArray());
		Point2D.Double[] dstPts = new Point2D.Double[srcPts.length];
		
		toApply.transform(srcPts, 0, dstPts, 0, srcPts.length);
		
		PointList newl = new PointList(convert(dstPts));
		newl.setCenter(this.getCenter());
		this.setPoints(newl);
	}
	
	public void scaleRelativeToOrigin(double mag)
	{
		AffineTransform toApply = new AffineTransform();
		toApply.scale(mag, mag);
		
		Point2D.Double[] srcPts = convert(this.toArray());
		Point2D.Double[] dstPts = new Point2D.Double[srcPts.length];
		
		toApply.transform(srcPts, 0, dstPts, 0, srcPts.length);
		
		PointList newl = new PointList(convert(dstPts));
		// newl.setCenter(this.getCenter());
		this.setPoints(newl);
	}
	
	/**
	 * radius is actually a square region extending radius pixels around the specified point (i.e. radius up, radius down, radius left, and radius right). Returns the nearest point within that range.
	 * 
	 * @param p
	 * @param radius
	 * @return
	 */
	public IdPoint nearestPointInRectRange(Point p, double radius)
	{
		TreeMap<Double,IdPoint> distanceMap = new TreeMap<Double,IdPoint>();
		double d = 0;
		for (IdPoint thisP : this)
		{
			distanceMap.put(distanceSquared(p, thisP), thisP);
		}
		Entry<Double,IdPoint> ret = distanceMap.firstEntry();
		if(ret == null)
			return null;
		IdPoint retP = ret.getValue();
		if(Math.abs(p.x - retP.x) <= radius && Math.abs(p.y - retP.y) <= radius)
			return retP;
		else
			return null;
	}
	
	/**
	 * radius is actually a circular region extending radius pixels around the specified point (i.e. radius up, radius down, radius left, and radius right). Returns the nearest point within that range.
	 * 
	 * @param p
	 * @param radius
	 * @return
	 */
	public IdPoint nearestPointInCircularRange(Point p, double radius)
	{
		TreeMap<Double,IdPoint> distanceMap = new TreeMap<Double,IdPoint>();
		double d = 0;
		for (IdPoint thisP : this)
		{
			distanceMap.put(distanceSquared(p, thisP), thisP);
		}
		Entry<Double,IdPoint> ret = distanceMap.firstEntry();
		if(ret == null)
			return null;
		double r2 = ret.getKey();
		double r = Math.sqrt(r2);
		IdPoint retP = ret.getValue();
		if(r <= radius)
			return retP;
		else
			return null;
	}
	
	private static Double distanceSquared(Point p1, Point p2)
	{
		return new Double((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y));
	}
	
	public Polygon toPolygon()
	{
		int[] Xs = this.getXIntArray();
		int[] Ys = this.getYIntArray();
		return new Polygon(Xs, Ys, this.size());
	}
	
	@Override
	public IdPoint[] toArray()
	{
		IdPoint[] ret = new IdPoint[this.size()];
		for (int i = 0; i < this.size(); i++)
		{
			ret[i] = this.get(i);
		}
		return ret;
	}
	
	public int[] getXIntArray()
	{
		int[] ret = new int[this.size()];
		for (int i = 0; i < this.size(); i++)
		{
			ret[i] = this.get(i).x;
		}
		return ret;
	}
	
	public int[] getYIntArray()
	{
		int[] ret = new int[this.size()];
		for (int i = 0; i < this.size(); i++)
		{
			ret[i] = this.get(i).y;
		}
		return ret;
	}
	
	public PointList getPointListCenteredAt(int x, int y)
	{
		PointList ret = new PointList();
		for (IdPoint p : this)
		{
			IdPoint toAdd = new IdPoint(p);
			IdPoint center = this.getCenter();
			toAdd.setLocation(p.x - center.x, p.y - center.y);
			toAdd.translate(x, y);
			ret.add(toAdd);
		}
		return ret;
	}
	
	public PointList getPointListRelativeToCenter()
	{
		return this.getPointListCenteredAt(0, 0);
	}
	
	public String pointListString()
	{
		SSVList polygonPts = new SSVList();
		String pt;
		for (IdPoint temp : this)
		{
			pt = temp.toString();
			polygonPts.add(pt);
		}
		String result = polygonPts.toString();
		return result;
	}
	
	@Override
	public String toString()
	{
		if(this.size() == 0)
			return "empty";
		LSVList ret = new LSVList();
		for (IdPoint p : this)
		{
			ret.add("x: " + p.x + ",y: " + p.y + ",id:" + p.id);
			;
		}
		ret.add("bounds -> x: " + this.getBounds());
		return ret.toString();
	}
	
	private static Point2D.Double[] convert(Point[] p)
	{
		Point2D.Double[] ret = new Point2D.Double[p.length];
		for (int i = 0; i < p.length; i++)
		{
			Point2D.Double temp = new Point2D.Double();
			temp.setLocation(p[i]);
			ret[i] = temp;
		}
		return ret;
	}
	
	private static Point[] convert(Point2D.Double[] p)
	{
		Point[] ret = new Point[p.length];
		Point temp;
		for (int i = 0; i < p.length; i++)
		{
			temp = new Point();
			temp.setLocation(p[i]);
			ret[i] = temp;
		}
		return ret;
	}
	
	// Copy x,y information but leave id the way it is
	// This method is used after transforms to update x,y locations
	private void setPoints(PointList pl)
	{
		for (int i = 0; i < this.size(); i++)
		{
			this.get(i).x = pl.get(i).x;
			this.get(i).y = pl.get(i).y;
		}
	}
	
	public static IdPoint getCenter(Rectangle r)
	{
		IdPoint ret = new IdPoint();
		ret.x = r.x + r.width / 2;
		ret.y = r.y + r.height / 2;
		ret.id = 0;
		return ret;
	}
	
	private static Point[] polygonToPointArray(Polygon p)
	{
		if(p == null)
			return new Point[0];
		Point[] ret = new Point[p.npoints + 1];
		PathIterator itr = p.getPathIterator(new AffineTransform());
		int i = 0;
		Point toAdd;
		while (!itr.isDone())
		{
			double[] temp = new double[2];
			itr.currentSegment(temp);
			toAdd = new Point((int) Math.round(temp[0]), (int) Math.round(temp[1]));
			ret[i] = toAdd;
			itr.next();
			i++;
		}
		return ret;
	}
	
	@Override
	public PointList clone()
	{
		return this.copy();
	}
	
	public PointList copy()
	{
		PointList ret = new PointList();
		for (IdPoint p : this)
		{
			ret.add(p.copy());
		}
		return ret;
	}
	
	public boolean equals(PointList l)
	{
		if(this.size() != l.size())
			return false;
		if(this.getBounds().equals(l.getBounds()))
		{
			for (int i = 0; i < this.size(); i++)
			{
				if(!this.get(i).equals(l.get(i)))
					return false;
			}
		}
		return true;
	}
	
	public boolean matches(PointList l)
	{
		return this.getPointListRelativeToCenter().equals(l.getPointListRelativeToCenter());
	}
	
	public Cursor<IntType> cursor()
	{
		return new PointListCursor(this);
	}
	
	
	// %%%%%%%%%%%%%%%%%%%%%
	
	class PointListCursor extends AbstractCursor<IntType> 
	{
		PointList pl;
		Iterator<IdPoint> itr;
		IdPoint cur;

		public PointListCursor(PointList pl)
		{
			super(2);
			this.pl = pl;
			this.itr = pl.iterator();
			this.cur = null;
		}
		
		@Override
		public IntType get() {
			return new IntType(cur.id);
		}

		@Override
		public void fwd() {
			this.cur = itr.next();
		}

		@Override
		public void reset() {
			this.itr = this.pl.iterator();
		}

		@Override
		public boolean hasNext() {
			return this.itr.hasNext();
		}

		@Override
		public void localize(long[] position) {
			this.cur.localize(position);
		}

		@Override
		public long getLongPosition(int d) {
			return this.cur.getLongPosition(d);
		}

		@Override
		public AbstractCursor<IntType> copy() {
			return this.copyCursor();
		}

		@Override
		public AbstractCursor<IntType> copyCursor() {
			PointListCursor ret = new PointListCursor(this.pl);
			ret.itr = this.pl.iterator();
			ret.cur = null;
			while(itr.hasNext() && ret.cur != this.cur)
			{
				itr.next();
			}
			return ret;
		}
		
	}
	
	public IdPoint getPointWithId(int id)
	{
		for(IdPoint p : this)
		{
			if(p.id == id)
			{
				return(p);
			}
		}
		return null;
	}
}

