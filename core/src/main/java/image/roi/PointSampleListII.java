package image.roi;

import java.util.Iterator;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.IterableRealInterval;
import net.imglib2.Positionable;
import net.imglib2.RealCursor;
import net.imglib2.RealPositionable;
import net.imglib2.Sampler;
import net.imglib2.type.numeric.RealType;

public class PointSampleListII<T extends RealType<T>> implements IterableRealInterval<T>, IterableInterval<T>
{
	public PointSampleList<T> pl;

	public PointSampleListII(PointSampleList<T> pl)
	{
		this.pl = pl;
	}

	@Override
	public long size() {
		return pl.size();
	}

	@Override
	public T firstElement() {
		return pl.get(0).id;
	}

	@Override
	public Object iterationOrder() {
		return new PointListIterationOrder();
	}

	@Override
	public double realMin(int d) {
		double val = Integer.MAX_VALUE;
		for(PointSample<T> p : this.pl)
		{
			if(val > p.getDoublePosition(d))
			{
				val = p.getDoublePosition(d);
			}
		}
		return val;
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
		double[] theMin = new double[min.numDimensions()];
		this.realMin(theMin);
		min.setPosition(theMin);
	}

	@Override
	public double realMax(int d) {
		double val = Integer.MIN_VALUE;
		for(PointSample<T> p : this.pl)
		{
			if(val < p.getDoublePosition(d))
			{
				val = p.getDoublePosition(d);
			}
		}
		return val;
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
		double[] theMax = new double[2];
		this.realMax(theMax);
		max.setPosition(theMax);
	}

	@Override
	public int numDimensions() {
		return 2;
	}

	@Override
	public Iterator<T> iterator() {
		return new SampleIterator<T>(this.pl);
	}

	@Override
	public long min(int d) {
		return (long) this.realMin(d);
	}

	@Override
	public void min(long[] min) {
		min[0] = this.min(0);
		min[1] = this.min(1);
	}

	@Override
	public void min(Positionable min) {
		long[] mins = new long[2];
		this.min(mins);
		min.setPosition(mins);
	}

	@Override
	public long max(int d) {
		return (long) this.realMax(d);
	}

	@Override
	public void max(long[] max) {
		max[0] = this.max(0);
		max[1] = this.max(1);
	}

	@Override
	public void max(Positionable max) {
		long[] maxs = new long[2];
		this.max(maxs);
		max.setPosition(maxs);
	}

	@Override
	public void dimensions(long[] dimensions) {
		long[] mins = new long[2];
		this.min(mins);

		long[] maxs = new long[2];
		this.max(maxs);

		long[] ret = new long[2];
		ret[0] = maxs[0]-mins[0];
		ret[1] = maxs[1]-mins[1];
	}

	@Override
	public long dimension(int d) {
		long[] dims = new long[2];
		this.dimensions(dims);

		return dims[d];
	}

	@Override
	public PointSampleListCursor<T> cursor() {
		return new PointSampleListCursor<T>(this.pl);
	}

	@Override
	public PointSampleListCursor<T> localizingCursor() {
		return new PointSampleListCursor<T>(this.pl);
	}
}

class SampleIterator<T extends RealType<T>> implements Iterator<T>
{
	private Iterator<PointSample<T>> iterator;

	public SampleIterator(PointSampleList<T> pl)
	{
		this.iterator = pl.iterator();
	}

	@Override
	public boolean hasNext() {
		return this.iterator.hasNext();
	}

	@Override
	public T next() {
		return this.iterator.next().get();
	}
}

class PointSampleListCursor<T extends RealType<T>> implements Cursor<T>, RealCursor<T>
{
	private PointSampleList<T> pl;
	private Iterator<PointSample<T>> iterator;
	private PointSample<T> cur;

	public PointSampleListCursor(PointSampleList<T> pl)
	{
		this.pl = pl;
		this.iterator = pl.iterator();
	}

	@Override
	public void localize(float[] position) {
		this.cur.localize(position);
	}

	@Override
	public void localize(double[] position) {
		this.cur.localize(position);
	}

	@Override
	public float getFloatPosition(int d) {
		return this.cur.getFloatPosition(d);
	}

	@Override
	public double getDoublePosition(int d) {
		return this.cur.getDoublePosition(d);
	}

	@Override
	public int numDimensions() {
		return 2;
	}

	@Override
	public T get() {
		return this.cur.get();
	}

	@Override
	public Sampler<T> copy() {
		return this.cur.copy();
	}

	@Override
	public void jumpFwd(long steps) {
		for(long i = 0; i < steps-1; i++)
		{
			this.iterator.next();
		}
		this.cur = this.iterator.next();
	}

	@Override
	public void fwd() {
		this.cur = this.iterator.next();
	}

	@Override
	public void reset() {
		this.iterator = this.pl.iterator();
	}

	@Override
	public boolean hasNext() {
		return this.iterator.hasNext();
	}

	@Override
	public T next() {
		return this.iterator.next().get();
	}

	@Override
	public PointSampleListCursor<T> copyCursor() {
		// Make Cursor on same object
		PointSampleListCursor<T> ret = new PointSampleListCursor<T>(this.pl);

		// Put iterator of new object into same state as the current object.
		while(ret.hasNext() && ret.cur != this.cur)
		{
			ret.next();
		}
		return ret;
	}

	@Override
	public void localize(int[] position) {
		this.cur.localize(position);
	}

	@Override
	public void localize(long[] position) {
		this.cur.localize(position);
	}

	@Override
	public int getIntPosition(int d) {
		return this.cur.getIntPosition(d);
	}

	@Override
	public long getLongPosition(int d) {
		return this.cur.getLongPosition(d);
	}

}