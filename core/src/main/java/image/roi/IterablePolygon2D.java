package image.roi;

import java.util.Iterator;

import net.imglib2.RealLocalizable;
import net.imglib2.roi.geom.real.Polygon2D;

public class IterablePolygon2D implements Iterable<RealLocalizable> {

	private Polygon2D pg;
	
	public IterablePolygon2D(Polygon2D pg)
	{
		this.pg = pg;
	}
	
	@Override
	public Iterator<RealLocalizable> iterator() {
		return new Polygon2DIterator(this.pg);
	}
	
}

class Polygon2DIterator implements Iterator<RealLocalizable>
{
	private Polygon2D pg = null;
	private int cur = -1;
	private int max = -1;

	public Polygon2DIterator(Polygon2D pg)
	{
		this.pg = pg;
		this.max = pg.numVertices();
	}
	@Override
	public boolean hasNext() {
		return(this.cur < this.max);
	}

	@Override
	public RealLocalizable next() {
		this.cur = this.cur + 1;
		return(this.pg.vertex(this.cur));
	}
	
}
