package image.roi;

import java.awt.Point;
import java.util.Comparator;

import miscellaneous.CSVList;
import miscellaneous.Copiable;
import net.imglib2.RealLocalizable;

public class IdPoint extends Point implements Comparator<IdPoint>, Copiable<IdPoint>, RealLocalizable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public int id = 0;
	
	public IdPoint()
	{
		super();
		this.id = 0;
	}
	
	public IdPoint(IdPoint p)
	{
		this.x = p.x;
		this.y = p.y;
		this.id = p.id;
	}
	
	public IdPoint(int x, int y, int id)
	{
		this.x = x;
		this.y = y;
		this.id = id;
	}
	
	public IdPoint(Point p, int id)
	{
		this(p.x, p.y, id);
	}
	
	public IdPoint(String csvString)
	{
		CSVList data = new CSVList(csvString);
		this.x = Integer.parseInt(data.get(0));
		this.y = Integer.parseInt(data.get(1));
		this.id = Integer.parseInt(data.get(2));
	}
	
	public String toString()
	{
		return this.x + "," + this.y + "," + this.id;
	}
	
	public boolean equals(Object o)
	{
		if(!(o instanceof IdPoint))
		{
			return false;
		}
		IdPoint p = (IdPoint) (o);
		if(p.x == this.x && p.y == this.y && p.id == this.id)
		{
			return true;
		}
		return false;
	}
	
	public int compare(IdPoint p1, IdPoint p2)
	{
		return p1.id - p2.id;
	}
	
	public IdPoint copy()
	{
		return new IdPoint(this.x, this.y, this.id);
	}
	
	public int hashCode()
	{
		return this.toString().hashCode();
	}

	@Override
	public int numDimensions()
	{
		return 2;
	}

	@Override
	public void localize(float[] position)
	{
		position[0] = this.x;
		position[1] = this.y;
	}

	@Override
	public void localize(double[] position)
	{
		position[0] = this.x;
		position[1] = this.y;
	}

	@Override
	public float getFloatPosition(int d)
	{
		if(d == 0)
		{
			return this.x;
		}
		else
		{
			return this.y;
		}
	}

	@Override
	public double getDoublePosition(int d)
	{
		if(d == 0)
		{
			return this.x;
		}
		else
		{
			return this.y;
		}
	}
	
}
