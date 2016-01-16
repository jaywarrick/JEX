package function.plugin.plugins.test;

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

}