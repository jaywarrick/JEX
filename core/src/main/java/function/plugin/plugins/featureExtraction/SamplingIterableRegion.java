package function.plugin.plugins.featureExtraction;

import java.util.Iterator;

import net.imglib2.AbstractWrappedInterval;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.roi.util.SamplingCursor;

public class SamplingIterableRegion< F >
extends AbstractWrappedInterval< IterableInterval< Void > > implements IterableInterval< F >, RandomAccessibleInterval< F >
{
	final RandomAccessibleInterval< F > target;
	final IterableInterval<Void> region;
	
	long size;

	public SamplingIterableRegion( final IterableInterval< Void > region, final RandomAccessibleInterval< F > target )
	{
		super( region );
		final int n = numDimensions();
		long s = region.dimension( 0 );
		for ( int d = 1; d < n; ++d )
			s *= region.dimension( d );
		size = s;
		
		this.region = region;
		this.target = target;
	}

	@Override
	public Cursor< F > cursor()
	{
		return new SamplingCursor< F >( sourceInterval.cursor(), target.randomAccess( sourceInterval ) );
	}

	@Override
	public Cursor< F > localizingCursor()
	{
		return new SamplingCursor< F >( sourceInterval.localizingCursor(), target.randomAccess( sourceInterval ) );
	}

	@Override
	public long size()
	{
		return sourceInterval.size();
	}

	@Override
	public F firstElement()
	{
		return cursor().next();
	}

	@Override
	public Object iterationOrder()
	{
		return sourceInterval.iterationOrder();
	}

	@Override
	public Iterator< F > iterator()
	{
		return cursor();
	}

	@Override
	public RandomAccess<F> randomAccess()
	{
		return target.randomAccess();
	}

	@Override
	public RandomAccess<F> randomAccess(Interval interval)
	{
		return target.randomAccess(interval);
	}
}