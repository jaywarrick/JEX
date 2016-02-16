package image.roi;

import java.awt.Point;
import java.awt.geom.Rectangle2D;

import miscellaneous.CSVList;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.Sampler;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

public class PointSample<T extends RealType<T>> extends RealPoint implements PointSampler<T> {
	
	public T id;
	
	public PointSample(PointSampler<T> pos)
	{
		super(pos);
		this.id = pos.get().copy();
	}
	
	public PointSample(RealLocalizable pos, T id)
	{
		super(pos);
		this.id = id;
	}
	
	public PointSample(double[] pos, T id)
	{
		super(pos);
		this.id = id;
	}
	
	public PointSample(Point p, T id)
	{
		this(new double[]{p.x, p.y}, id);
	}
	
	public PointSample(double x, double y, T id)
	{
		this(new double[]{x, y}, id);
	}
	
	public PointSample(String csvString)
	{
		CSVList data = new CSVList(csvString);
		double[] pos = new double[data.size()-1];
		for(int i = 0; i < data.size()-1; i++)
		{
			pos[i] = Double.parseDouble(data.get(i));
		}
		this.get().setReal(Double.parseDouble(data.get(data.size()-1)));
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < this.numDimensions(); i++)
		{
			sb.append(this.position[i] + ",");
		}
		sb.append(this.get().getRealDouble());
		return sb.toString();
	}
	
	public boolean equals(Object o)
	{
		if(!(o instanceof PointSample))
		{
			return false;
		}
		@SuppressWarnings("rawtypes")
		PointSample p = (PointSample) (o);
		if(p.numDimensions() != this.numDimensions())
		{
			return false;
		}
		for(int i = 0; i < p.numDimensions(); i++)
		{
			if(p.getDoublePosition(i) != this.getDoublePosition(i))
			{
				return false;
			}
		}
		if(p.get() != this.get())
		{
			return false;
		}
		return true;
	}
	
	public int compareTo(Sampler<T> p2)
	{
		return this.compare(this, p2);
	}
	
	public int compare(Sampler<T> p1, Sampler<T> p2)
	{
		return p1.get().compareTo(p2.get());
	}
	
	public PointSample<T> copy()
	{
		return new PointSample<T>(this);
	}
	
	public int hashCode()
	{
		return this.toString().hashCode();
	}

	@Override
	public T get() {
		return this.id;
	}
	
	public static double distance(RealLocalizable here, RealLocalizable there)
	{
		if(here.numDimensions() != there.numDimensions())
		{
			throw new IllegalArgumentException();
		}
		double dist = 0.0;
		for(int i = 0; i < here.numDimensions(); i++)
		{
			dist = dist + (there.getDoublePosition(i) - here.getDoublePosition(i))*(there.getDoublePosition(i) - here.getDoublePosition(i));
		}
		return Math.sqrt(dist);
	}
	
	public void translate(double x, double y)
	{
		this.translate(new double[]{x, y});
	}
	
	public void translate(double[] distances)
	{
		this.move(distances);
	}
	
	public static PointSample<IntType> getCenter(Rectangle2D.Double r)
	{
		double x = (r.x + r.x + r.width) / 2.0;
		double y = (r.y + r.y + r.height) / 2.0;
		return new PointSample<IntType>(x, y, new IntType(0));
	}
}
