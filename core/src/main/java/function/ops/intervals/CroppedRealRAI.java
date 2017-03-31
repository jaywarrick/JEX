package function.ops.intervals;

import net.imglib2.Interval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.type.BooleanType;
import net.imglib2.type.numeric.RealType;

public class CroppedRealRAI<T extends BooleanType<T>, R extends RealType<R>> implements RandomAccessibleInterval<R>
	{
		IntersectedInterval ii;
		RandomAccess<T> a;
		RandomAccess<R> b;
		
		public CroppedRealRAI(RandomAccessibleInterval<T> a, RandomAccessibleInterval<R> b)
		{
			this.a = a.randomAccess();
			this.b = b.randomAccess();
			this.ii = new IntersectedInterval(a, b);
		}

		@Override
		public RandomAccess<R> randomAccess() {
			return new CroppedRealRA<T,R>(this.a.copyRandomAccess(), this.b.copyRandomAccess());
		}

		@Override
		public RandomAccess<R> randomAccess(Interval interval) {
			return new CroppedRealRA<T,R>(this.a.copyRandomAccess(), this.b.copyRandomAccess());
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