package image.roi;

import java.awt.Point;
import java.awt.geom.Point2D;

import signals.Observable;
import signals.SSCenter;

public class XPoint extends Point implements Observable {
	
	private final Object hashCodeObject = new Object();
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final String OBSERVE_X = "x";
	public static final String OBSERVE_Y = "y";
	
	public XPoint()
	{
		this.x = 0;
		this.y = 0;
	}
	
	public XPoint(Point p)
	{
		this.x = p.x;
		this.y = p.y;
	}
	
	public XPoint(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	public Integer x()
	{
		return new Integer((int) Math.round(this.getX()));
	}
	
	public Integer y()
	{
		return new Integer((int) Math.round(this.getY()));
	}
	
	@Override
	public void move(int x, int y)
	{
		this.setLocation(x, y);
	}
	
	@Override
	public void translate(int dx, int dy)
	{
		this.setLocation(this.x + dx, this.y + dy);
	}
	
	public void translate(double dx, double dy)
	{
		this.setLocation((int) Math.round(this.x + dx), (int) Math.round(this.y + dy));
	}
	
	@Override
	public void setLocation(double x, double y)
	{
		this.setLocation((int) Math.round(x), (int) Math.round(y));
	}
	
	@Override
	public void setLocation(Point p)
	{
		this.setLocation(p.x, p.y);
	}
	
	@Override
	public void setLocation(Point2D p)
	{
		this.setLocation((int) Math.round(p.getX()), (int) Math.round(p.getY()));
	}
	
	@Override
	public void setLocation(int x, int y)
	{
		SSCenter.changing(this, OBSERVE_X);
		SSCenter.changing(this, OBSERVE_Y);
		this.x = x;
		this.y = y;
		SSCenter.changed(this, OBSERVE_X);
		SSCenter.changed(this, OBSERVE_Y);
	}
	
	// public void silentSetLocation(int x, int y)
	// {
	// this.x = x;
	// this.y = y;
	// }
	
	@Override
	public XPoint getLocation()
	{
		return new XPoint(this.x, this.y);
	}
	
	public boolean equals(Point p)
	{
		return this == p;
	}
	
	@Override
	public int hashCode()
	{
		return hashCodeObject.hashCode();
	}
	
}
