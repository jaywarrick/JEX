package function.ops.geometry;

import function.ops.featuresets.wrappers.MirroredLabelRegionCursor;
import net.imglib2.Cursor;
import net.imglib2.PairIterator;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealRandomAccess;
import net.imglib2.Sampler;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.type.numeric.RealType;

/**
 * The TwinCursor moves over two images with respect to a mask. The mask
 * has to be of the same dimensionality as the images. Position information
 * obtained from this class comes from the mask.
 *
 * @author Jay Warrick
 */
public class TwinMirroredLabelRegionCursor<T extends RealType<T>> implements Cursor<T>, PairIterator<T> {

	// Variables that can be set
	private RandomAccessibleInterval<T> image;

	// Intermediate Variables
	protected MirroredLabelRegionCursor mirror;
	private RealRandomAccess<T> ra;
	private double[] position;

	// Final variables
	final private String interpMethod;
	final private String oobMethod;
	final private Double oobValue;
	final private Double oobRandomMin;
	final private Double oobRandomMax;

	/**
	 * 
	 * @param image
	 * @param region
	 * @param center
	 * @param symmetryAngle (radians)
	 * @param interpMethod
	 * @param oobMethod
	 * @param oobValue (only used for Value and Random OOB strategies)
	 * @param oobRandomMin (only used for Random OOB strategies)
	 * @param oobRandomMax (only used for Random OOB strategies)
	 */
	public TwinMirroredLabelRegionCursor(String interpMethod, String oobMethod, double oobValue, double oobRandomMin, double oobRandomMax) {
		this.interpMethod = interpMethod;
		this.oobMethod = oobMethod;
		this.oobValue = oobValue;
		this.oobRandomMin = oobRandomMin;
		this.oobRandomMax = oobRandomMax;
	}

	/**
	 * 
	 * @param image
	 * @param region
	 * @param center
	 * @param symmetryAngle (radians)
	 * @param interpMethod
	 * @param oobMethod
	 * @param oobValue (only used for Value and Random OOB strategies)
	 * @param oobRandomMin (only used for Random OOB strategies)
	 * @param oobRandomMax (only used for Random OOB strategies)
	 */
	public TwinMirroredLabelRegionCursor(RandomAccessibleInterval<T> image, LabelRegion<?> region, RealLocalizable center, Double symmetryAngle, String interpMethod, String oobMethod, double oobValue, double oobRandomMin, double oobRandomMax) {
		this(interpMethod, oobMethod, oobValue, oobRandomMin, oobRandomMax);
		this.setImage(image);
		this.setRegion(region, center, symmetryAngle);
	}

	/**
	 * This is intended to be called before setting the region.
	 * @param image
	 */
	public void setImage(RandomAccessibleInterval<T> image)
	{
		this.image = image;
		this.ra = GeomUtils.extendAndInterpolate(this.image, this.interpMethod, this.oobMethod, this.oobValue, this.oobRandomMin, this.oobRandomMax).realRandomAccess();
	}

	/**
	 * This is intended to be called after setting the image.
	 * @param region
	 * @param center
	 * @param symmetryAngle (radians)
	 */
	public void setRegion(LabelRegion<?> region, RealLocalizable center, Double symmetryAngle)
	{
		if(this.image == null)
		{
			throw new NullPointerException("Need to set the image before setting the region.");
		}
		if(region == null || center == null || symmetryAngle == null)
		{
			throw new NullPointerException("The region, center, and symmetry angle need to be non-null.");
		}
		
		// Set up the mirror cursor
		this.mirror = new MirroredLabelRegionCursor(region, center, symmetryAngle);
		this.position = new double[this.mirror.numDimensions()];
		this.mirror.localize(this.position);
	}
	
	public void setCenter(RealLocalizable center)
	{
		this.mirror.setCenter(center);
	}
	
	public void setSymmetryAngle(Double symmetryAngle)
	{
		this.mirror.setSymmetryAngle(symmetryAngle);
	}
	
	@Override
	final public boolean hasNext() {
		return this.mirror.hasNext();
	}

	@Override
	final public T getFirst() {
		this.mirror.localize(this.position);
		this.ra.setPosition(this.position);
		return this.ra.get();
	}

	@Override
	final public T getSecond() {
		this.mirror.localizeToMirrorPosition(this.position);
		this.ra.setPosition(this.position);
		return this.ra.get();
	}

	@Override
	public void reset() {
		this.mirror.reset();
		this.mirror.localize(this.position);
	}

	@Override
	public void fwd() {
		this.mirror.fwd();
	}

	@Override
	public void jumpFwd(long arg0) {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public T next() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public double getDoublePosition(int arg0) {
		return this.mirror.getDoublePosition(arg0);
	}

	@Override
	public float getFloatPosition(int arg0) {
		return this.mirror.getFloatPosition(arg0);
	}

	@Override
	public void localize(float[] arg0) {
		this.mirror.localize(arg0);
	}

	@Override
	public void localize(double[] arg0) {
		this.mirror.localize(arg0);
	}

	public void localizeToMirrorPosition(double[] arg0) {
		this.mirror.localizeToMirrorPosition(arg0);
	}

	@Override
	public int numDimensions() {
		return this.mirror.numDimensions();
	}

	@Override
	public Sampler<T> copy() {
		throw new UnsupportedOperationException("This method has not been implemented, yet."); 
	}

	@Override
	public T get() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}

	@Override
	public int getIntPosition(int arg0) {
		return this.mirror.getIntPosition(arg0);
	}

	@Override
	public long getLongPosition(int arg0) {
		return this.mirror.getLongPosition(arg0);
	}

	@Override
	public void localize(int[] arg0) {
		this.mirror.localize(arg0);
	}

	@Override
	public void localize(long[] arg0) {
		this.mirror.localize(arg0);
	}

	@Override
	public Cursor<T> copyCursor() {
		throw new UnsupportedOperationException("This method has not been implemented, yet.");
	}
}
