package image.roi;

import java.util.Iterator;

import net.imglib2.Cursor;
import net.imglib2.RealCursor;
import net.imglib2.Sampler;
import net.imglib2.type.numeric.RealType;

public class PointSamplerCursor<T extends RealType<T>> implements Cursor<T>, RealCursor<T>
{
	private PointSamplerList<T> pl;
	private Iterator<PointSampler<T>> iterator;
	private PointSampler<T> cur;

	public PointSamplerCursor(PointSamplerList<T> pl)
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
	public PointSamplerCursor<T> copyCursor() {
		// Make Cursor on same object
		PointSamplerCursor<T> ret = new PointSamplerCursor<T>(this.pl);

		// Put iterator of new object into same state as the current object.
		while(ret.hasNext() && ret.cur != this.cur)
		{
			ret.next();
		}
		return ret;
	}

	@Override
	public void localize(int[] position) {
		for(int i = 0; i < position.length; i++)
		{
			position[i] = this.getIntPosition(i);
		}
	}

	@Override
	public void localize(long[] position) {
		for(int i = 0; i < position.length; i++)
		{
			position[i] = this.getLongPosition(i);
		}
	}

	@Override
	public int getIntPosition(int d) {
		return (int) Math.round(this.getDoublePosition(d)) ;
	}

	@Override
	public long getLongPosition(int d) {
		return Math.round(this.getDoublePosition(d));
	}

}