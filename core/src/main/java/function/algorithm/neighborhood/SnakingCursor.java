package function.algorithm.neighborhood;

import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;

public class SnakingCursor<T> implements Cursor<T> {
	
	RandomAccessibleInterval<T> rai;
	RandomAccess<T> ra;
	long[] min;
	Indexer snake;
	Point pos;
	
	public SnakingCursor(RandomAccessibleInterval<T> rai)
	{
		this.rai = rai;
		this.ra = this.rai.randomAccess();
		long[] dims = new long[rai.numDimensions()];
		this.min = new long[rai.numDimensions()];
		rai.dimensions(dims);
		rai.min(this.min);
		snake = new Indexer(dims, true);
		this.reset();
	}
	
	public void reset()
	{
		this.snake.reset();
		this.pos = new Point(this.numDimensions());
		this._updatePosWithSnakePosAdjustedRelativeToMin();
	}
	
	/** 
	 * Adjust pos relative to the min dim positions (could be non-zero)
	 */
	private void _updatePosWithSnakePosAdjustedRelativeToMin()
	{
		long[] temp = this.snake.getCurrent();
		for(int d=0; d < this.numDimensions(); d++)
		{
			this.pos.setPosition(temp[d] + this.min[d], d);
		}
	}

	@Override
	public void localize(float[] position) {
		this.pos.localize(position);
	}

	@Override
	public void localize(double[] position) {
		this.pos.localize(position);
	}

	@Override
	public float getFloatPosition(int d) {
		return this.pos.getFloatPosition(d);
	}

	@Override
	public double getDoublePosition(int d) {
		return this.pos.getDoublePosition(d);
	}

	@Override
	public int numDimensions() {
		return this.rai.numDimensions();
	}

	@Override
	public T get() {
		this.ra.setPosition(this.pos);
		return this.ra.get();
	}

	@Override
	public Sampler<T> copy() {
		return this.copyCursor();
	}

	@Override
	public void jumpFwd(long steps) {
		for(int i=0; i < steps; i++)
		{
			this.fwd();
		}
	}

	@Override
	public void fwd() {
		this.snake.indexPos();
		this._updatePosWithSnakePosAdjustedRelativeToMin();
	}

	@Override
	public boolean hasNext() {
		return this.snake.hasNext();
	}

	@Override
	public T next() {
		this.fwd();
		return this.get();
	}

	@Override
	public void localize(int[] position) {
		this.pos.localize(position);
	}

	@Override
	public void localize(long[] position) {
		this.pos.localize(position);
	}

	@Override
	public int getIntPosition(int d) {
		return this.pos.getIntPosition(d);
	}

	@Override
	public long getLongPosition(int d) {
		return this.pos.getLongPosition(d);
	}

	@Override
	public Cursor<T> copyCursor() {
		return null;
	}

}
