package function.ops.intervals;

import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RealPositionable;

public class IntersectedInterval implements Interval {

	private Interval a, b;
	
	public IntersectedInterval(Interval a, Interval b)
	{
		this.a = a;
		this.b = b;
	}
	
	@Override
	public double realMin(int d) {
		return Math.max(this.a.min(d), this.b.min(d));
	}

	@Override
	public void realMin(double[] min) {
		for(int i = 0; i < min.length; i++)
		{
			min[i] = this.realMin(i);
		}
	}

	@Override
	public void realMin(RealPositionable min) {
		for(int i = 0; i < min.numDimensions(); i++)
		{
			min.setPosition(this.realMin(i), i);
		}
	}

	@Override
	public double realMax(int d) {
		return Math.min(this.a.realMax(d), this.b.realMax(d));
	}

	@Override
	public void realMax(double[] max) {
		for(int i = 0; i < max.length; i++)
		{
			max[i] = this.realMax(i);
		}
	}

	@Override
	public void realMax(RealPositionable max) {
		for(int i = 0; i < max.numDimensions(); i++)
		{
			max.setPosition(this.realMax(i), i);
		}
	}

	@Override
	public int numDimensions() {
		return Math.min(a.numDimensions(), b.numDimensions());
	}

	@Override
	public void dimensions(long[] dimensions) {
		for(int i = 0; i < this.numDimensions(); i++)
		{
			dimensions[i] = this.dimension(i);
		}
	}

	@Override
	public long dimension(int d) {
		return this.max(d) - this.min(d) + 1;
	}

	@Override
	public long min(int d) {
		return (long) this.realMin(d);
	}

	@Override
	public void min(long[] min) {
		for(int i = 0; i < min.length; i++)
		{
			min[i] = this.min(i);
		}
	}

	@Override
	public void min(Positionable min) {
		for(int i = 0; i < this.numDimensions(); i++)
		{
			min.setPosition(this.min(i), i);
		}
	}

	@Override
	public long max(int d) {
		return (long) this.realMax(d);
	}

	@Override
	public void max(long[] max) {
		for(int i = 0; i < max.length; i++)
		{
			max[i] = this.max(i);
		}
	}

	@Override
	public void max(Positionable max) {
		for(int i = 0; i < this.numDimensions(); i++)
		{
			max.setPosition(this.max(i), i);
		}
	}

}
