package function.algorithm.neighborhood;

import java.util.Iterator;
import java.util.Vector;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.Sampler;

public class EdgeCursor<T> implements Cursor<T>
{
	private Vector<long[]> edge;
	private RandomAccess<T> ra;
	private Point kernelCenter;
	private Point kernelCenterOffset;
	private Point samplerLoc;
	private Iterator<long[]> itr;
	private int index;
	private long[] cur;

	public EdgeCursor(Vector<long[]> edge, RandomAccess<T> ra, Point kernelCenter, Point kernelCenterOffset)
	{
		this.edge = edge;
		this.ra = ra;
		this.kernelCenter = kernelCenter;
		this.kernelCenterOffset = kernelCenterOffset;
		this.samplerLoc = new Point(kernelCenterOffset.numDimensions());
		this.reset();
	}

	@Override
	public void localize(float[] position) {
		for(int d=0; d<2; d++)
		{
			position[d] = this.getFloatPosition(d);
		}
	}

	@Override
	public void localize(double[] position) {
		for(int d=0; d<2; d++)
		{
			position[d] = this.getDoublePosition(d);
		}
	}

	@Override
	public float getFloatPosition(int d) {
		return (float) this.cur[d];
	}

	@Override
	public double getDoublePosition(int d) {
		return (double) this.cur[d];
	}

	@Override
	public int numDimensions() {
		return this.ra.numDimensions();
	}

	public Point getSamplerLoc()
	{
		this.samplerLoc.setPosition(this.kernelCenter);
		this.samplerLoc.move(this.kernelCenterOffset);
		this.samplerLoc.move(this.cur);
		return new Point(this.samplerLoc);
	}

	@Override
	public T get() {
		this.ra.setPosition(this.getSamplerLoc());
		return this.ra.get();
	}

	@Override
	public Sampler<T> copy() {
		return this.copyCursor();
	}

	@Override
	public void jumpFwd(long steps) {
		for(long i=0; i < steps; i++)
		{
			this.fwd();
		}
	}

	@Override
	public void fwd() {
		this.cur = this.itr.next();
		this.index = this.index + 1;
	}

	@Override
	public void reset() {
		this.itr = edge.iterator();
		this.cur = new long[this.numDimensions()];
		this.index = -1;
	}

	@Override
	public boolean hasNext() {
		return this.itr.hasNext();
	}

	@Override
	public T next() {
		this.fwd();
		return this.get();
	}

	@Override
	public void localize(int[] position) {
		for(int d=0; d< this.numDimensions(); d++)
		{
			position[d] = this.getIntPosition(d);
		}
	}

	@Override
	public void localize(long[] position) {
		for(int d=0; d< this.numDimensions(); d++)
		{
			position[d] = this.getLongPosition(d);
		}
	}

	@Override
	public int getIntPosition(int d) {
		return (int) this.cur[d];
	}

	@Override
	public long getLongPosition(int d) {
		return this.cur[d];
	}

	@Override
	public Cursor<T> copyCursor() {
		Point newKernelCenter = new Point(kernelCenter.numDimensions());
		newKernelCenter.setPosition(kernelCenter);
		Point newKernelCenterOffset = new Point(kernelCenterOffset.numDimensions());
		newKernelCenterOffset.setPosition(kernelCenterOffset);
		EdgeCursor<T> ret = new EdgeCursor<>(this.edge, this.ra, newKernelCenter, newKernelCenterOffset);
		ret.reset();
		for(int i=-1; i < this.index; i++)
		{
			ret.fwd();
		}
		return ret;
	}
}