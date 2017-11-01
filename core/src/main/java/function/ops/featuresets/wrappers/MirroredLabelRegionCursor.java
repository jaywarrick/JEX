package function.ops.featuresets.wrappers;

import function.ops.geometry.GeomUtils;
import net.imglib2.Cursor;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.RealRandomAccess;
import net.imglib2.Sampler;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.type.logic.BoolType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;

/**
 * The MirroredTwinCursor moves over one image with respect to a line of symmetry. The cursor
 * will visit all pixels on one side of the line and all locations (nearest neighbor) on the other
 * side of the line that don't have a mirrored pixel on the first side. Intended for use
 * with algorithms such as correlation coefficient calculators where data needs to be
 * paired (in this case, across a line of symmetry).
 *
 * @author Jay Warrick
 */
public class MirroredLabelRegionCursor implements Cursor<Void> {
	
	// Permanent references
	final protected LabelRegion<?> region;
	RealLocalizable center;
	Double symmetryAngle;
	
	// helper variables
	protected RealRandomAccess<BoolType> maskRA;
	protected Cursor<Void> mask1;
	protected long[] mask1Position;
	ValuePair<RealPoint, Double> mask1MirrorData;
	protected Cursor<Void> mask2;
	ValuePair<RealPoint, Double> mask2MirrorData;
	
	protected boolean hasNext = true;

	/**
	 * @param region
	 * @param center
	 * @param symmetryAngle (radians)
	 */
	public MirroredLabelRegionCursor(final LabelRegion<?> region, final RealLocalizable center, final Double symmetryAngle) {
		this.region = region;
		this.center = center;
		this.symmetryAngle = symmetryAngle;
		this.reset();
	}
	
	public void setSymmetryAngle(double symmetryAngle)
	{
		this.symmetryAngle = symmetryAngle;
	}
	
	public void setCenter(RealLocalizable center)
	{
		this.center = center;
	}

	@Override
	final public boolean hasNext() {
		return hasNext;
	}
	
	private boolean lookAhead() {
		if(this.mask2.hasNext())
		{
			this.mask2.fwd();
			mask2MirrorData = this.getMirroredXYPosition(this.mask2, this.center, this.symmetryAngle);
			if(this.mask2MirrorData.getB() >= 0.0 && this.mask2MirrorData.getB() <= Math.PI)
			{
				this.hasNext = true;
				return false;
			}
			else
			{
				// we are on the other side of the mirror line and need to look for mirrored pixels.
				this.maskRA.setPosition(this.mask2MirrorData.getA());
				if(this.maskRA.get().get())
				{
					// then we are in "covered" territory and need to try and move forward
					if(this.mask2.hasNext())
					{
						return true;
					}
					else
					{
						this.hasNext = false;
						return false;
					}
				}
				else
				{
					// we are in "un-covered" territory and can use this location
					this.hasNext = true;
					return false;
				}
			}
		}
		else
		{
			this.hasNext = false;
			return false;
		}
	}

	final public void fwd() {
		this.mask1 = this.mask2.copyCursor();
		this.mask1.localize(this.mask1Position);
		this.mask1MirrorData = this.mask2MirrorData;
		while(this.lookAhead())
		{
			this.lookAhead();
		}
	}

	@Override
	final public Void get() {
		return null;
	}

	@Override
	public void reset() {
		this.maskRA = (new NearestNeighborInterpolatorFactory<BoolType>()).create(Views.extendValue(region, new BoolType(false)));
		this.mask1 = region.cursor();
		this.mask1.reset();
		this.mask2 = this.mask1.copyCursor();
		this.mask1Position = new long[this.mask1.numDimensions()];
		this.hasNext = true;
		
		// Initialize positions and flags etc.
		this.mask1.localize(mask1Position);
		this.lookAhead();
	}

	@Override
	public void jumpFwd(long arg0) {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public Void next() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public double getDoublePosition(int arg0) {
		return this.mask1.getDoublePosition(arg0);
	}

	@Override
	public float getFloatPosition(int arg0) {
		return this.mask1.getFloatPosition(arg0);
	}

	@Override
	public void localize(float[] arg0) {
		this.mask1.localize(arg0);
	}

	@Override
	public void localize(double[] arg0) {
		this.mask1.localize(arg0);
	}
	
	public void localizeToMirrorPosition(double[] arg0) {
		this.mask1MirrorData.getA().localize(arg0);
	}

	@Override
	public int numDimensions() {
		return this.mask1.numDimensions();
	}

	@Override
	public int getIntPosition(int arg0) {
		return this.mask1.getIntPosition(arg0);
	}

	@Override
	public long getLongPosition(int arg0) {
		return this.mask1.getLongPosition(arg0);
	}

	@Override
	public void localize(int[] arg0) {
		this.mask1.localize(arg0);
	}

	@Override
	public void localize(long[] arg0) {
		this.mask1.localize(arg0);
	}

	@Override
	public Cursor<Void> copyCursor() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
	
	@Override
	public Sampler<Void> copy() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	/** Recall that in image coordinates, the y axis is flipped, so instead of positive
	 * radians resulting in counterclockwise rotation, things go clockwise. Thus if 
	 * thetaMirrorLine is pi/4, things will be rotated clockwise 45 deg
	 * 
	 * @param cur
	 * @param center
	 * @param thetaMirrorLine
	 * @return
	 */	
	public ValuePair<RealPoint,Double> getMirroredXYPosition(Cursor<?> cur, RealLocalizable center, final double thetaMirrorLine)
	{

		//		# R Test code:
		//		ax <- runif(5, 0.3, 1)
		//		ay <- runif(5, 1.1, 1.5)
		//
		//		plot(ax, ay, pch=20, col='red', xlim=c(-2,2), ylim=c(-2,2))
		//
		//		cx <- 0.5
		//		cy <- 0.5
		//
		//		axm <- ax-cx
		//		aym <- ay-cy
		//
		//		ar <- sqrt(axm^2 + aym^2)
		//
		//		theta <- atan2(aym, axm)
		//
		//		ax2 <- ar*cos(theta)
		//		ay2 <- ar*sin(theta)
		//
		//		points(ax2+cx, ay2+cy)
		//
		//		# Rotate to new mirror line
		//		thetaMirrorLine <- 7*pi/4
		//		theta <- theta - thetaMirrorLine
		//
		//		thetaReflected <- 2*pi - theta
		//
		//		bx <- ar*cos(thetaReflected+thetaMirrorLine) + cx
		//		by <- ar*sin(thetaReflected+thetaMirrorLine) + cy
		//
		//		points(cx, cy, pch=20, col='black')
		//		points(bx, by, pch=20, col='blue')


		// get relative polar coordinate r and theta
		ValuePair<Double,Double> coords = GeomUtils.getPolarCoordinatesOfCursor(cur, center);
		double r = coords.getA();
		double theta = coords.getB();

		// rotate the coordinate system to the mirror line at thetaMirror
		// Recall that in image coordinates, the y axis is flipped, so instead of positive
		// radians resulting in clockwise rotation, things go counterclockwise.
		theta = theta - thetaMirrorLine;

		// calculate the theta for the mirrored point in the rotated coordinate system
		// regularize to be between 0 and 2*pi
		double thetaReflected = GeomUtils.regularizeTheta(2*Math.PI - theta);
		
		// get mirrored x-y coordinate
		double x = r*Math.cos(thetaReflected + thetaMirrorLine) + center.getDoublePosition(0);
		double y = r*Math.sin(thetaReflected + thetaMirrorLine) + center.getDoublePosition(1);

		return new ValuePair<>(new RealPoint(x, y), thetaReflected);
	}
}
