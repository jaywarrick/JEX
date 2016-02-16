package image.roi;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.jhotdraw.geom.Polygon2D;

import miscellaneous.CSVList;
import miscellaneous.LSVList;
import miscellaneous.SSVList;
import net.imglib2.RealCursor;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

@SuppressWarnings("unused")
public class PointSamplerList<T extends RealType<T>> extends Vector<PointSampler<T>> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private T type;

	public PointSamplerList(T type)
	{
		super();
		this.type = type;
	}
	
	public PointSamplerList(PointSamplerList<T> pl)
	{
		this.type = pl.type;
		for (PointSampler<T> p : pl)
		{
			PointSample<T> toAdd = new PointSample<>(p);
			super.add(toAdd);
		}
	}
	
	public PointSamplerList(List<? extends RealLocalizable> pl, T type)
	{
		this(type);
		for (RealLocalizable p : pl)
		{
			this.add(p);
		}
	}
	
	public PointSamplerList(String polygonPts, T type) throws InstantiationException, IllegalAccessException
	{
		this(type);
		
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
				int val = 0;
				if(pt.size() > 2)
				{
					val = Integer.parseInt(pt.get(2));
				}
				this.add(x, y, val);
			}
		}
	}
	
	public PointSamplerList(Point[] pa, T type)
	{
		this(type);
		for (Point p : pa)
		{
			this.add(p);
		}
	}
	
	public PointSamplerList(RealPoint[] pa, T type)
	{
		this(type);
		for (RealPoint p : pa)
		{
			this.add(p);
		}
	}
	
	//	public PointSampleList(Polygon pg)
	//	{
	//		this(polygonToPointArray(pg));
	//	}
	
	public PointSamplerList(Polygon2D pg, T type)
	{
		this(polygonToRealPointArray(pg), type);
	}
	
	public PointSamplerList(Polygon pg, T type)
	{
		this(pg.getVertices(), type);
	}
	
	// public RealPointList<T>(XRealPointList<T> pl)
	// {
	// super();
	// this.setPoints(pl);
	// }
	//
	// public RealPointList<T>(Iterable<RealPoint2<T>> pl)
	// {
	// super();
	// this.setPoints(pl);
	// }
	
	public boolean add(RealLocalizable p)
	{
		return this.add(new PointSample<T>(p, this.getNewSample(this.size())));
	}
	
	public boolean add(Point p)
	{
		return this.add(p.x, p.y, this.size());
	}
	
	public boolean add(double x, double y)
	{
		return this.add(x, y, this.size());
	}
	
	public boolean add(double x, double y, double val)
	{
		PointSample<T> temp = new PointSample<T>(x, y, this.getNewSample(val));
		return super.add(temp);
	}
	
	public Rectangle2D.Double getBounds2D()
	{
		Pair<double[], double[]> bounds = this.getMinMaxValues();
		double[] min = bounds.getA();
		double[] max = bounds.getB();
		return new Rectangle2D.Double(min[0], min[1], max[0]-min[0], max[1]-min[1]);
	}
	
	public Rectangle getBounds()
	{
		return this.getBounds2D().getBounds();
	}
	
	public Pair<double[],double[]> getMinMaxValues()
	{
		ValuePair<double[],double[]> ret = null;
		double[] min = null;
		double[] max = null;
		for(PointSampler<T> p : this)
		{
			if(min == null || max == null)
			{
				min = new double[p.numDimensions()];
				max = new double[p.numDimensions()];
				p.localize(min);
				p.localize(max);
				continue;
			}
			for(int i = 0; i < p.numDimensions(); i++)
			{
				if(p.getDoublePosition(i) < min[i])
				{
					min[i] = p.getDoublePosition(i);
				}
				if(p.getDoublePosition(i) > max[i])
				{
					max[i] = p.getDoublePosition(i);
				}
			}
		}
		if(min == null)
		{
			return ret;
		}
		else
		{
			ret = new ValuePair<>(min, max);
			return ret;
		}
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
			PointSampler<T> start;
			PointSampler<T> end;
			
			double segment;
			for (int i = 1; i < this.size(); i++)
			{
				start = this.get(i - 1);
				end = this.get(i);
				length += PointSample.distance(start,end);
			}
			if(!isLine)
			{
				length += PointSample.distance(this.get(0), this.get(this.size() - 1));
			}
		}
		return length;
	}
	
	public PointSample<IntType> getCenter()
	{
		return PointSample.getCenter(this.getBounds2D());
	}
	
	public void transform(double thetaDeg, double mag, double newLocationX, double newLocationY)
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
	
	public void transform(double thetaDeg, double mag, RealLocalizable newLocation)
	{
		this.rotate(thetaDeg);
		this.scale(mag);
		this.setCenter(newLocation);
	}
	
	public void translate(double[] distances)
	{
		for (PointSampler<T> p : this)
		{
			p.move(distances);
		}
	}
	
	public void translate(double deltaX, double deltaY)
	{
		this.translate(new double[]{deltaX, deltaY});
	}
	
	public void setCenter(double[] pos)
	{
		PointSamplerList<T> l = this.getRealPointListRelativeToCenter();
		l.translate(pos);
		this.setPoints(l);
	}
	
	public void setCenter(double x, double y)
	{
		PointSamplerList<T> l = this.getRealPointListRelativeToCenter();
		l.translate(x, y);
		this.setPoints(l);
	}
	
	public void setCenter(Point p)
	{
		this.setCenter(p.x, p.y);
	}
	
	public void setCenter(RealLocalizable p)
	{
		double[] newCenter = new double[p.numDimensions()];
		p.localize(newCenter);
		this.setCenter(newCenter);
	}
	
	public void rotate(double thetaDeg)
	{
		AffineTransform toApply = new AffineTransform();
		toApply.rotate(thetaDeg * Math.PI / 180);
		
		Point2D.Double[] srcPts = convert(this.getRealPointListRelativeToCenter().toArray());
		Point2D.Double[] dstPts = new Point2D.Double[srcPts.length];
		
		toApply.transform(srcPts, 0, dstPts, 0, srcPts.length);
		
		PointSamplerList<T> newl = new PointSamplerList<T>(convert(dstPts), this.type);
		newl.setCenter(this.getCenter());
		this.setPoints(newl);
	}
	
	public void rotateRelativeToOrigin(double thetaDeg)
	{
		AffineTransform toApply = new AffineTransform();
		toApply.rotate(thetaDeg * Math.PI / 180);
		
		Point2D.Double[] srcPts = convert(this.toArray());
		Point2D.Double[] dstPts = new Point2D.Double[srcPts.length];
		
		toApply.transform(srcPts, 0, dstPts, 0, srcPts.length);
		
		PointSamplerList<T> newl = new PointSamplerList<T>(convert(dstPts), this.type);
		//		newl.setCenter(this.getCenter());
		this.setPoints(newl);
	}
	
	public void scale(double mag)
	{
		AffineTransform toApply = new AffineTransform();
		toApply.scale(mag, mag);
		
		Point2D.Double[] srcPts = convert(this.getRealPointListRelativeToCenter().toArray());
		Point2D.Double[] dstPts = new Point2D.Double[srcPts.length];
		
		toApply.transform(srcPts, 0, dstPts, 0, srcPts.length);
		
		PointSamplerList<T> newl = new PointSamplerList<T>(convert(dstPts), this.type);
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
		
		PointSamplerList<T> newl = new PointSamplerList<T>(convert(dstPts), this.type);
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
	public PointSampler<T> nearestPointInRadius(PointSample<T> p, double radius)
	{
		TreeMap<Double,PointSampler<T>> distanceMap = new TreeMap<Double,PointSampler<T>>();
		double d = 0;
		for (PointSampler<T> thisP : this)
		{
			distanceMap.put(PointSample.distance(p, thisP), thisP);
		}
		Entry<Double,PointSampler<T>> ret = distanceMap.firstEntry();
		if(ret == null)
			return null;
		if(ret.getKey() <= radius)
			return ret.getValue();
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
	public PointSampler<T> nearestPointInCircularRange(PointSample<T> p, double radius)
	{
		TreeMap<Double,PointSampler<T>> distanceMap = new TreeMap<Double,PointSampler<T>>();
		double d = 0;
		for (PointSampler<T> thisP : this)
		{
			distanceMap.put(PointSample.distance(p,thisP), thisP);
		}
		Entry<Double,PointSampler<T>> ret = distanceMap.firstEntry();
		if(ret == null)
			return null;
		double r2 = ret.getKey();
		double r = Math.sqrt(r2);
		PointSampler<T> retP = ret.getValue();
		if(r <= radius)
			return retP;
		else
			return null;
	}
	
	public Polygon2D toPolygon()
	{
		double[] Xs = this.getDoubleArray(0);
		double[] Ys = this.getDoubleArray(1);
		return new Polygon2D.Double(Xs, Ys, this.size());
	}
	
	@Override
	public PointSampler<T>[] toArray()
	{
		@SuppressWarnings("unchecked")
		PointSampler<T>[] ret = new PointSample[this.size()];
		for (int i = 0; i < this.size(); i++)
		{
			ret[i] = this.get(i);
		}
		return ret;
	}
	
	public int[] getIntArray(int d)
	{
		int[] ret = new int[this.size()];
		for (int i = 0; i < this.size(); i++)
		{
			ret[i] = (int) Math.round(this.get(i).getDoublePosition(d));
		}
		return ret;
	}
	
	public double[] getDoubleArray(int d)
	{
		double[] ret = new double[this.size()];
		for (int i = 0; i < this.size(); i++)
		{
			ret[i] = this.get(i).getDoublePosition(d);
		}
		return ret;
	}
	
	public PointSamplerList<T> getRealPointListCenteredAt(double[] pos)
	{
		PointSamplerList<T> ret = new PointSamplerList<T>(this.type);
		for (PointSampler<T> p : this)
		{
			PointSample<T> toAdd = new PointSample<T>(p);
			RealPoint center = this.getCenter();
			double[] relPos = new double[p.numDimensions()];
			for(int i = 0; i < p.numDimensions(); i++)
			{
				relPos[i] = p.getDoublePosition(i) - center.getDoublePosition(i);
			}
			toAdd.setPosition(relPos);
			toAdd.translate(pos);
			ret.add(toAdd);
		}
		return ret;
	}
	
	public PointSamplerList<T> getRealPointListRelativeToCenter()
	{
		double[] zeros = new double[this.get(0).numDimensions()];
		for(int i = 0; i < zeros.length; i++)
		{
			zeros[i] = 0.0;
		}
		return this.getRealPointListCenteredAt(zeros);
	}
	
	public String RealPointListString()
	{
		SSVList polygonPts = new SSVList();
		String pt;
		for (PointSampler<T> temp : this)
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
		for (PointSampler<T> p : this)
		{
			ret.add(p.toString());
		}
		ret.add("bounds -> x: " + this.getBounds());
		return ret.toString();
	}
	
	@SuppressWarnings("rawtypes")
	private static Point2D.Double[] convert(PointSampler[] p)
	{
		Point2D.Double[] ret = new Point2D.Double[p.length];
		for (int i = 0; i < p.length; i++)
		{
			Point2D.Double temp = new Point2D.Double(p[i].getDoublePosition(0), p[i].getDoublePosition(1));
			ret[i] = temp;
		}
		return ret;
	}
	
	private static RealPoint[] convert(Point2D.Double[] p)
	{
		RealPoint[] ret = new RealPoint[p.length];
		RealPoint temp;
		for (int i = 0; i < p.length; i++)
		{
			temp = new RealPoint(p[i].x, p[i].y);
			ret[i] = temp;
		}
		return ret;
	}
	
	// Copy x,y information but leave val the way it is
	// This method is used after transforms to update x,y locations
	private void setPoints(PointSamplerList<T> pl)
	{
		if(this.size() == 0 || pl.size() == 0 || this.get(0).numDimensions() != pl.get(0).numDimensions())
		{
			throw new IllegalArgumentException();
		}
		for (int i = 0; i < this.size(); i++)
		{
			this.get(i).setPosition(pl.get(i));
		}
	}
	
	//	private static Point[] polygonToPointArray(Polygon p)
	//	{
	//		if(p == null)
	//			return new Point[0];
	//		Point[] ret = new Point[p.npoints + 1];
	//		PathIterator itr = p.getPathIterator(new AffineTransform());
	//		int i = 0;
	//		Point toAdd;
	//		while (!itr.isDone())
	//		{
	//			double[] temp = new double[2];
	//			itr.currentSegment(temp);
	//			toAdd = new Point((int) Math.round(temp[0]), (int) Math.round(temp[1]));
	//			ret[i] = toAdd;
	//			itr.next();
	//			i++;
	//		}
	//		return ret;
	//	}
	
	private static RealPoint[] polygonToRealPointArray(Polygon2D p)
	{
		if(p == null)
			return new RealPoint[0];
		RealPoint[] ret = new RealPoint[p.npoints + 1];
		PathIterator itr = p.getPathIterator(new AffineTransform());
		int i = 0;
		RealPoint toAdd;
		while (!itr.isDone())
		{
			double[] temp = new double[2];
			itr.currentSegment(temp);
			toAdd = new RealPoint(temp);
			ret[i] = toAdd;
			itr.next();
			i++;
		}
		return ret;
	}
	
	@Override
	public PointSamplerList<T> clone()
	{
		return this.copy();
	}
	
	public PointSamplerList<T> copy()
	{
		PointSamplerList<T> ret = new PointSamplerList<T>(this.type);
		for (PointSampler<T> p : this)
		{
			PointSample<T> toAdd = new PointSample<>(p);
			ret.add(toAdd);
		}
		return ret;
	}
	
	public boolean equals(PointSamplerList<T> l)
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
	
	public boolean matches(PointSamplerList<T> l)
	{
		return this.getRealPointListRelativeToCenter().equals(l.getRealPointListRelativeToCenter());
	}
	
	private T getNewSample(double val)
	{
		T ret = this.type.copy();
		ret.setReal(val);
		return ret;
	}
	
	public RealCursor<T> cursor()
	{
		return new PointSamplerCursor<T>(this);
	}
}
