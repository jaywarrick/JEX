package function.plugin.plugins.test;

import net.imglib2.Localizable;
import net.imglib2.RealLocalizable;

public class Circle
{
	RealLocalizable center;

	double radius;

	public Circle(RealLocalizable center, double radius)
	{
		this.center = center;
		this.radius = radius;
	}

	public RealLocalizable getCenter()
	{
		return this.center;
	}

	public double getRadius()
	{
		return this.radius;
	}

	public boolean contains(Localizable testPoint)
	{		
				
		double[] p  = new double[testPoint.numDimensions()];
		double[] p0 = new double[testPoint.numDimensions()];
		
		testPoint.localize(p);			
		double x = p[0];
		double y = p[1];

		center.localize(p0);
		double x0 = p0[0];
		double y0 = p0[1];
		
				
		// Calculate distance from center and compare with radius
		double distance = Math.sqrt(Math.pow(x-x0, 2) + Math.pow(y-y0, 2));
		return distance <= this.radius;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for(int d = 0; d < this.center.numDimensions(); d++)
		{
			sb.append(this.center.getDoublePosition(d));
			sb.append(",");
		}
		sb.append(radius);
		return sb.toString();
	}
	
}