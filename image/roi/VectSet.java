package image.roi;

import java.util.ArrayList;
import java.util.List;

public class VectSet extends ArrayList<Vect> {
	
	private static final long serialVersionUID = 1L;
	public static int XAXIS = 1;
	public static int YAXIS = 2;
	
	public VectSet()
	{
		super(0);
	}
	
	@Override
	public boolean add(Vect v)
	{
		return super.add(v);
	}
	
	@Override
	public Vect get(int index)
	{
		Vect result = super.get(index);
		return result;
	}
	
	public void add(List<Vect> vectors)
	{
		for (Vect v : vectors)
		{
			add(v);
		}
	}
	
	/**
	 * Sum a list of vectors
	 * 
	 * @param vectors
	 * @return
	 */
	public Vect sum()
	{
		Vect result = new Vect();
		for (Vect v : this)
		{
			result.add(v);
		}
		return result;
	}
	
	/**
	 * Return the mean vector
	 * 
	 * @return
	 */
	public Vect mean()
	{
		Vect result = sum();
		result.multiply(1 / (double) this.size());
		return result;
	}
	
	/**
	 * Returns the average value of the norms of the vectors in the list
	 * 
	 * @param vectors
	 * @return
	 */
	public double meanNorm()
	{
		double result = 0;
		for (Vect v : this)
		{
			Double norm = v.norm();
			if(norm.isNaN())
				continue;
			result = result + v.norm();
		}
		result = result / this.size();
		return result;
	}
	
	/**
	 * Returs the norm of the mean displacement vector
	 * 
	 * @return
	 */
	public double normMean()
	{
		Vect mean = sum();
		double norm = mean.norm();
		norm = norm / this.size();
		return norm;
	}
	
	/**
	 * Returns the mean norm of the vector set along the chosen axis
	 * 
	 * @param axis
	 * @return norm along chosen axis
	 */
	public double meanNormOnAxis(int axis)
	{
		double result = 0;
		for (Vect v : this)
		{
			if(axis == XAXIS)
				result = result + v.dX;
			if(axis == YAXIS)
				result = result + v.dY;
		}
		result = result / this.size();
		return result;
	}
	
	/**
	 * Returns the average angle of the vectors in the list
	 * 
	 * @return
	 */
	public double meanAngle()
	{
		Vect resultVect = new Vect();
		int length = 0;
		for (Vect v : this)
		{
			Vect mean = v.duplicate();
			double norm = mean.norm();
			if(norm > 0)
			{
				mean.multiply(1 / norm);
				resultVect.add(mean);
				length++;
			}
		}
		resultVect.multiply(1 / (double) length);
		double resultRad = Math.atan2(resultVect.dY, resultVect.dX);
		double result = Math.toDegrees(resultRad);
		return result;
	}
	
	/**
	 * Returs the angle of the mean displacement vector
	 * 
	 * @return
	 */
	public double angleMean()
	{
		Vect mean = sum();
		double resultRad = Math.atan2(mean.dY, mean.dX);
		double result = Math.toDegrees(resultRad);
		return result;
	}
	
	/**
	 * Return the chemotaxis index of this list of vectors
	 * 
	 * @return
	 */
	public double chemtotaxisIndex()
	{
		double lengthTotal = 0;
		for (Vect v : this)
		{
			Double norm = v.norm();
			if(norm.isNaN())
				continue;
			lengthTotal = lengthTotal + v.norm();
		}
		
		Vect sum = sum();
		double lengthEffective = sum.norm();
		
		double result = lengthEffective / lengthTotal;
		return result;
	}
	
	/**
	 * Print the current vector set
	 */
	public void print()
	{
		String result = "";
		String result2 = "";
		String result3 = "";
		for (Vect v : this)
		{
			result = result + "(" + v.dX + "," + v.dY + ")";
			result2 = result2 + " - " + v.norm();
			result3 = result3 + " - " + v.angle();
		}
		System.out.println("   VectSet ---> Vectors are " + result);
		System.out.println("   VectSet ---> Distance travelled are " + result2);
		System.out.println("   VectSet ---> Angles are " + result3);
	}
}
