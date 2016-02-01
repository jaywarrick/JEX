package function.ops.intervals;

import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.type.BooleanType;

public class IntersectedBooleanRAI<T extends BooleanType<T>> implements RandomAccessibleInterval<T>
	{
		IntersectedInterval ii;
		RandomAccessible<T> a, b;
		
		public IntersectedBooleanRAI(RandomAccessibleInterval<T> a, RandomAccessibleInterval<T> b)
		{
			this.ii = new IntersectedInterval(a, b);
			//this.ra = new IntersectedBooleanRA<T>(a.randomAccess(), b.randomAccess());
		}

		@Override
		public RandomAccess<T> randomAccess() {
			return new IntersectedBooleanRA<T>(this.a.randomAccess().copyRandomAccess(), this.b.randomAccess().copyRandomAccess());
		}

		@Override
		public RandomAccess<T> randomAccess(Interval interval) {
			return new IntersectedBooleanRA<T>(this.a.randomAccess().copyRandomAccess(), this.b.randomAccess().copyRandomAccess());
		}

		@Override
		public int numDimensions() {
			return this.ii.numDimensions();
		}

		@Override
		public long min(int d) {
			return this.ii.min(d);
		}

		@Override
		public void min(long[] min) {
			this.ii.min(min);
		}

		@Override
		public void min(Positionable min) {
			this.ii.min(min);
		}

		@Override
		public long max(int d) {
			return this.ii.max(d);
		}

		@Override
		public void max(long[] max) {
			this.ii.max(max);
		}

		@Override
		public void max(Positionable max) {
			this.ii.max(max);
		}

		@Override
		public double realMin(int d) {
			return this.ii.realMin(d);
		}

		@Override
		public void realMin(double[] min) {
			this.ii.realMin(min);
		}

		@Override
		public void realMin(RealPositionable min) {
			this.ii.realMin(min);
		}

		@Override
		public double realMax(int d) {
			return this.ii.realMax(d);
		}

		@Override
		public void realMax(double[] max) {
			this.ii.realMax(max);
		}

		@Override
		public void realMax(RealPositionable max) {
			this.ii.realMax(max);
		}

		@Override
		public void dimensions(long[] dimensions) {
			this.ii.dimensions(dimensions);
		}

		@Override
		public long dimension(int d) {
			return this.ii.dimension(d);
		}
	}