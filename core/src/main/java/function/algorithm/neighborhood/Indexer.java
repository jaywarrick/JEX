package function.algorithm.neighborhood;

import java.util.Arrays;
import java.util.Iterator;

public class Indexer implements Iterator<long[]>
{

	long[] dims;
	long[] maxs;
	long[] pos;
	boolean[] flipped;
	long index;
	long maxIndex;
	boolean snaking;
	
	public Indexer(long[] dims)
	{
		this(dims, false);
	}

	public Indexer(long[] dims, boolean snaking)
	{
		this.snaking = snaking;
		this.dims = dims;
		this.maxs = new long[this.dims.length];
		this.maxIndex = 1L;
		for(int d=0; d < this.dims.length; d++)
		{
			this.maxIndex = this.maxIndex*this.dims[d];
			this.maxs[d] = this.dims[d] - 1;
		}
		this.reset();
	}

	public void reset()
	{
		this.pos = new long[this.dims.length];
		this.pos[0] = -1L;
		this.flipped = new boolean[this.dims.length];
		this.index = 0L;
	}

	public void indexPos()
	{
		this.pos[0] = this.pos[0] + 1;
		this.index = this.index + 1;
		for(int d=0; d < this.dims.length - 1; d++)
		{
			if(this.pos[d] > this.maxs[d])
			{
				this.pos[d] = 0;
				this.flipped[d] = !this.flipped[d];
				this.pos[d + 1] = this.pos[d + 1] + 1;
			}
		}
	}
	
	public long[] getCurrent()
	{
		if(this.snaking)
		{
			return this.getSnakedIndex();
		}
		return this.getLinearIndex();
	}
	
	private long[] getLinearIndex()
	{
		return Arrays.copyOf(this.pos, this.pos.length);
	}

	private long[] getSnakedIndex()
	{
		long[] ret = new long[this.dims.length];
		for(int d=0; d < this.dims.length; d++)
		{
			if(this.flipped[d])
			{
				ret[d] = this.maxs[d] - this.pos[d];
			}
			else
			{
				ret[d] = this.pos[d];
			}
		}
		return ret;
	}

	@Override
	public boolean hasNext() {
		return this.index < this.maxIndex;
	}

	@Override
	public long[] next() {
		this.indexPos();
		return this.getCurrent();
	}
}