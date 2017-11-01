package function.ops.geometry;

import org.scijava.Priority;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import function.ops.JEXOps;
import net.imagej.ops.special.function.AbstractBinaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealCursor;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.ValuePair;

@Plugin(type = JEXOps.SymmetryLineAngle.class, priority = Priority.NORMAL_PRIORITY)
public class DefaultSymmetryLineAngle<T extends RealType<T>> extends AbstractBinaryFunctionOp<LabelRegion<?>, RandomAccessibleInterval<T>, ValuePair<Double,Double>>
implements JEXOps.SymmetryLineAngle {
	
	@Parameter(required = false)
	private RealLocalizable center = null;
	
	@Parameter(required=false, min = "1", max = "10")
	private int foldSymmetry = 1;

	@Override
	/**
	 * Returns the ValuePair A: symmetry angle, B: amplitude of symmetry
	 */
	public ValuePair<Double,Double> calculate(LabelRegion<?> region, RandomAccessibleInterval<T> image) {
		
		if(this.foldSymmetry < 1)
		{
			throw new IllegalArgumentException("The argument for fold symmetry must be an integer of 1 or more.");
		}
		
		double totalSin = 0;
		double totalCos = 0;
		double n = 0;
		double total = 0;
		
		if(image != null)
		{
			IterableInterval<T> ii = Regions.sample(region, image);
			// get the cursor of the iterable interval
			RealCursor<T> cur = ii.localizingCursor();
			while(cur.hasNext())
			{
				cur.fwd();
				
				// get the relative polar coordinates of the cursor position
				ValuePair<Double,Double> coords = GeomUtils.getPolarCoordinatesOfCursor(cur, center);		
				double val = cur.get().getRealDouble();
				totalSin = totalSin + Math.sin(((double) foldSymmetry) * coords.getB()) * val;
				totalCos = totalCos + Math.cos(((double) foldSymmetry) * coords.getB()) * val;
				total = total + val;
				n++;
			}
		}
		else
		{
			Cursor<Void> cur = region.cursor();
			while(cur.hasNext())
			{
				cur.fwd();
				
				// get the relative polar coordinates of the cursor position
				ValuePair<Double,Double> coords = GeomUtils.getPolarCoordinatesOfCursor(cur, center);		
				// implied is that the intensity at each location is 1 
				totalSin = totalSin + Math.sin(((double) foldSymmetry) * coords.getB());
				totalCos = totalCos + Math.cos(((double) foldSymmetry) * coords.getB());
				total++;
				n++;
			}
		}
		
		totalSin = totalSin/n;
		totalCos = totalCos/n;
		total = total/n;
		double r = Math.sqrt(totalSin*totalSin + totalCos*totalCos);
		Double theta = GeomUtils.regularizeTheta(Math.atan2(totalSin, totalCos));
		Double amplitude = r/total;
		
		// We divide by the fold symmetry because for every 90 degrees change in the sin / cos
		// total space, there is a 90/foldSymmetry degree change in actual space.
		double symmetryAngle = GeomUtils.regularizeTheta(theta/((double)foldSymmetry));
		
		return new ValuePair<>(symmetryAngle, amplitude);
	}
	
	public void setCenter(RealLocalizable newCenter)
	{
		this.center = newCenter;
	}
	
	public void setFoldSymmetry(int foldSymmetry)
	{
		this.foldSymmetry = foldSymmetry;
	}
}
