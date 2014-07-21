package image.roi;

import java.awt.Point;
import java.util.List;

public class Vect {
	
	public double dX = 0;
	public double dY = 0;
	
	public Vect()
	{}
	
	public Vect(double dx, double dy)
	{
		this.dX = dx;
		this.dY = dy;
	}
	
	public Vect(Point p1, Point p2)
	{
		this.dX = p2.x - p1.x;
		this.dY = p2.y - p1.y;
	}
	
	// Getters and setters
	/**
	 * Return the X displacement of the vector
	 */
	public double getDX()
	{
		return dX;
	}
	
	/**
	 * Return the Y displacement of the vector
	 * 
	 * @return
	 */
	public double getDY()
	{
		return dY;
	}
	
	public void add(Vect v)
	{
		this.dX = this.dX + v.dX;
		this.dY = this.dY + v.dY;
	}
	
	public void multiply(double m)
	{
		this.dX = this.dX * m;
		this.dY = this.dY * m;
	}
	
	/**
	 * Sum a list of vectors
	 * 
	 * @param vectors
	 * @return
	 */
	public static Vect sum(List<Vect> vectors)
	{
		Vect result = new Vect();
		for (Vect v : vectors)
		{
			result.add(v);
		}
		return result;
	}
	
	/**
	 * Sum the norms of a list of vectors
	 * 
	 * @param vectors
	 * @return
	 */
	public static double sumNorm(List<Vect> vectors)
	{
		double result = 0;
		for (Vect v : vectors)
		{
			result = result + v.norm();
		}
		return result;
	}
	
	/**
	 * Return the norm of the vector
	 * 
	 * @return
	 */
	public double norm()
	{
		double currentDistance = Math.sqrt((dX) * (dX) + (dY) * (dY));
		return currentDistance;
	}
	
	/**
	 * Return the angle of the vector
	 * 
	 * @return
	 */
	public double angle()
	{
		double resultRad = Math.atan2(dY, dX);
		double result = Math.toDegrees(resultRad);
		// System.out.println("Particule traveled dx="+x+" and dy="+y+" and resultRad="+resultRad+" and result="+result);
		return result;
	}
	
	/**
	 * Return a string representation of the vector
	 */
	@Override
	public String toString()
	{
		String result = "(" + dX + "," + dY + ")";
		return result;
	}
	
	/**
	 * Hard copy of the current vector
	 * 
	 * @return
	 */
	public Vect duplicate()
	{
		return new Vect(dX, dY);
	}
	
	/**
	 * Return a vector orthogonal to the current one, with the same norm
	 * 
	 * @return
	 */
	public Vect getOrthogonal()
	{
		Vect result = new Vect(-dY, dX);
		return result;
	}
	
	/**
	 * Return a new point based on the original point P translated by the value of the vect
	 * 
	 * @param p
	 * @return
	 */
	public Point translatePoint(Point p)
	{
		double x = p.getX() + dX;
		double y = p.getY() + dY;
		Point result = new Point((int) x, (int) y);
		return result;
	}
}
