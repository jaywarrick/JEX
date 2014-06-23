package function.roitracker;

import image.roi.Vect;

import java.awt.Point;

public class Axis {
	
	Point p1;
	Point p2;
	Vect vect;
	
	public Axis(Point p1, Point p2)
	{
		this.p1 = p1;
		this.p2 = p2;
		
		vect = new Vect(p1, p2);
		vect.multiply(1/vect.norm());
	}
	
	/**
	 * Returns an orthogonal axis to the current one
	 * 
	 * @return
	 */
	public Axis getPerpendicularAxis()
	{
		Vect axisVect = new Vect(p1, p2);
		axisVect.multiply(1 / (axisVect.norm()));
		
		Vect newAxisVect = axisVect.getOrthogonal();
		Point p2perp = newAxisVect.translatePoint(p1);
		
		Axis result = new Axis(p1, p2perp);
		return result;
	}
	
	/**
	 * Returns the value of the projection of the vector V on this axis
	 * 
	 * @param v
	 * @return
	 */
	public double projectVectorOnAxis(Vect v)
	{
		// Construct the main vector
		// Vect vect = new Vect(p1, p2);
		
		// Calculate the scalar product
		//double result = (vect.getDX() * v.getDX() + vect.getDY() * v.getDY()) / vect.norm();
		double result = (vect.getDX() * v.getDX() + vect.getDY() * v.getDY());
		
		// return the projection value
		return result;
	}
}
