package function.ops.intervals;

import net.imglib2.Localizable;
import net.imglib2.RandomAccess;
import net.imglib2.type.BooleanType;

/**
 * Note that this class assumes that the RA's are for intervals of the same size.
 * @author MMB
 *
 * @param <T>
 */
public class IntersectedBooleanRA<T extends BooleanType<T>> implements RandomAccess<T> {

	RandomAccess<T> a, b;
	
	public IntersectedBooleanRA(RandomAccess<T> a, RandomAccess<T> b)
	{
		this.a = a;
		this.b = b;
	}
	
	@Override
	public void localize(int[] position) {
		this.a.localize(position);
	}

	@Override
	public void localize(long[] position) {
		this.a.localize(position);
	}

	@Override
	public int getIntPosition(int d) {
		return this.a.getIntPosition(d);
	}

	@Override
	public long getLongPosition(int d) {
		return this.a.getLongPosition(d);
	}

	@Override
	public void localize(float[] position) {
		this.a.localize(position);
	}

	@Override
	public void localize(double[] position) {
		this.a.localize(position);
	}

	@Override
	public float getFloatPosition(int d) {
		return this.a.getFloatPosition(d);
	}

	@Override
	public double getDoublePosition(int d) {
		return this.a.getDoublePosition(d);
	}

	@Override
	public int numDimensions() {
		return Math.min(this.a.numDimensions(), this.b.numDimensions());
	}

	@Override
	public void fwd(int d) {
		this.a.fwd(d);
		this.b.fwd(d);
	}

	@Override
	public void bck(int d) {
		this.a.bck(d);
		this.b.bck(d);
	}

	@Override
	public void move(int distance, int d) {
		this.a.move(distance, d);
		this.b.move(distance, d);
	}

	@Override
	public void move(long distance, int d) {
		this.a.move(distance, d);
		this.b.move(distance, d);
	}

	@Override
	public void move(Localizable localizable) {
		this.a.move(localizable);
		this.b.move(localizable);
	}

	@Override
	public void move(int[] distance) {
		this.a.move(distance);
		this.b.move(distance);
	}

	@Override
	public void move(long[] distance) {
		this.a.move(distance);
		this.b.move(distance);
	}

	@Override
	public void setPosition(Localizable localizable) {
		this.a.setPosition(localizable);
		this.b.setPosition(localizable);
	}

	@Override
	public void setPosition(int[] position) {
		this.a.setPosition(position);
		this.b.setPosition(position);
	}

	@Override
	public void setPosition(long[] position) {
		this.a.setPosition(position);
		this.b.setPosition(position);
	}

	@Override
	public void setPosition(int position, int d) {
		this.a.setPosition(position, d);
		this.b.setPosition(position, d);
	}

	@Override
	public void setPosition(long position, int d) {
		this.a.setPosition(position, d);
		this.b.setPosition(position, d);
	}

	@Override
	public T get() {
		T ret = this.a.get().copy();
		ret.and(this.b.get());
		return ret;
	}

	@Override
	public IntersectedBooleanRA<T> copy() {
		return new IntersectedBooleanRA<T>(this.a.copyRandomAccess(), this.b.copyRandomAccess());
	}

	@Override
	public IntersectedBooleanRA<T> copyRandomAccess() {
		return this.copy();
	}

}
