package function.algorithm.neighborhood;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Arrays;
import java.util.Vector;

import function.plugin.plugins.featureExtraction.LabelGenerator;
import net.imglib2.Cursor;
import net.imglib2.Localizable;
import net.imglib2.Point;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Sampler;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;

public class PositionableRunningNeighborhood<T extends RealType<T>> implements Cursor<T>, Positionable 
{
	public static String BORDER="border", MIRROR="mirror", PERIODIC="periodic";
	
	private ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval<T>> extImage;//
	private RandomAccess<T> raImage;// The location for the neighborhood in the image
	private RandomAccess<T> raSampler;// The location in the image to actually be sampled
	private Point samplerLoc;// Helper variable to determine location in the image to actually be sampled
	private Point lastPos;// Helper variable to determine how much this neighborhood has been moved
	private Point curPos;// Helper variable to determine how much this neighborhood has been moved
	private Cursor<Void> cKernel;// relative position within the kernel being iterated over
	private Img<UnsignedByteType> kernelImage;// backing for the kernel cursor
	private long kSize;
	private ExtendedRandomAccessibleInterval<UnsignedByteType, Img<UnsignedByteType>> kernel;// RandomAccess of an extended version of the image
	private Point kernelCenterOffset;// Distance from the center of the kernel to the upper left of the kernel
	private Vector<Vector<long[]>> posNewEdges; // Each 'edge' is a Vector<long[]> where long[] holds the pixel locations
	private Vector<Vector<long[]>> negNewEdges; // Each 'edge' is a Vector<long[]> where long[] holds the pixel locations
	private Vector<Vector<long[]>> posOldEdges; // Each 'edge' is a Vector<long[]> where long[] holds the pixel locations
	private Vector<Vector<long[]>> negOldEdges; // Each 'edge' is a Vector<long[]> where long[] holds the pixel locations
	
	public PositionableRunningNeighborhood(Shape s, RandomAccessibleInterval<T> img, String borderType)
	{
		this(getKernelWithShape(s), img, borderType);
	}
	
	public PositionableRunningNeighborhood(Img<UnsignedByteType> kernel, RandomAccessibleInterval<T> img, String borderType)
	{
		if(kernel.numDimensions() != img.numDimensions())
		{
			throw new IllegalArgumentException("The kernel provided is " + kernel.numDimensions() + "D while the image is " + img.numDimensions() + "D. Currently, they must be the same.");
		}
		
		// Set the kernel and associated edge pixels to aid running calculations
		this.setKernel(kernel);
		this.setEdges();
		
		// Set the image and the OutOfBounds behavior for the image.
		if(borderType == null)
		{
			borderType = MIRROR;
		}
		if(borderType.equals(BORDER))
		{
			this.extImage = Views.extendBorder(img);
		}
		else if(borderType.equals(MIRROR))
		{
			this.extImage = Views.extendMirrorSingle(img);
		}
		else // if(borderType.equals(PERIODIC))
		{
			this.extImage = Views.extendPeriodic(img);
		}
		this.raImage = img.randomAccess().copyRandomAccess();
		
		// Create helper variables
		this.raSampler = this.extImage.randomAccess();
		this.samplerLoc = new Point(2);
		this.lastPos = new Point(2);
		this.curPos = new Point(2);
	}
	
	public Point getLastPosition()
	{
		return this.lastPos;
	}
	
	public Point getCurPosition()
	{
		return this.curPos;
	}
	
	/**
	 * Return the dimension in which the neighborhood was last moved.
	 * The value is actually the dimension + 1 and the sign is the
	 * direction in which it was changed by 1 unit. If the neighborhood
	 * was moved by more than one unit, it returns null;
	 * @return
	 */
	public long[] getMovement() {
		long[] diffs = new long[2];
		long totalAbsDiff = 0;
		for(int i=0; i < 2; i++)
		{
			diffs[i] = curPos.getLongPosition(i) - lastPos.getLongPosition(i);
			totalAbsDiff = totalAbsDiff + Math.abs(diffs[i]);
		}
		if(totalAbsDiff != 1)
		{
			return null;
		}
		return diffs;
	}

	@Override
	public int numDimensions() {
		return 2;
	}

	@Override
	public void fwd(int d) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.fwd(d);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void bck(int d) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.bck(d);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void move(int distance, int d) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.move(distance, d);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void move(long distance, int d) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.move(distance, d);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void move(Localizable localizable) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.move(localizable);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void move(int[] distance) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.move(distance);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void move(long[] distance) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.move(distance);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void setPosition(Localizable localizable) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.setPosition(localizable);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void setPosition(int[] position) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.setPosition(position);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void setPosition(long[] position) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.setPosition(position);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void setPosition(int position, int d) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.setPosition(position, d);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void setPosition(long position, int d) {
		this.lastPos = this.curPos;
		this.curPos = new Point(2);
		this.raImage.setPosition(position, d);
		this.curPos.setPosition(this.raImage);
		this.reset();
	}

	@Override
	public void localize(float[] position) {
		this.raImage.localize(position);
	}

	@Override
	public void localize(double[] position) {
		this.raImage.localize(position);
	}

	@Override
	public float getFloatPosition(int d) {
		return this.raImage.getFloatPosition(d);
	}

	@Override
	public double getDoublePosition(int d) {
		return this.raImage.getDoublePosition(d);
	}

	@Override
	public T get() {
		this.samplerLoc.setPosition(this.raImage);
		this.samplerLoc.move(this.kernelCenterOffset);
		this.samplerLoc.move(this.cKernel);
		this.raSampler.setPosition(this.samplerLoc);
		return this.raSampler.get();
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
		this.cKernel.fwd();
	}

	@Override
	public void reset() {
		this.cKernel.reset();
	}

	@Override
	public boolean hasNext() {
		return this.cKernel.hasNext();
	}

	@Override
	public T next() {
		this.fwd();
		return this.get();
	}

	@Override
	public void localize(int[] position) {
		this.cKernel.localize(position);
	}

	@Override
	public void localize(long[] position) {
		this.cKernel.localize(position);
	}

	@Override
	public int getIntPosition(int d) {
		return this.cKernel.getIntPosition(d);
	}

	@Override
	public long getLongPosition(int d) {
		return this.cKernel.getLongPosition(d);
	}

	@Override
	public Cursor<T> copyCursor() {
		// TODO Implement;
		return null;
	}
	
	public EdgeCursor<T> getEdgeCursor(int d, boolean pos, boolean newEdge)
	{
		Point kernelCenter = new Point(this.numDimensions());
		kernelCenter.setPosition(this.raImage);
		if(pos)
		{
			if(newEdge)
			{
				return new EdgeCursor<>(this.posNewEdges.get(d), this.raSampler.copyRandomAccess(), kernelCenter, this.kernelCenterOffset);
			}
			return new EdgeCursor<>(this.posOldEdges.get(d), this.raSampler.copyRandomAccess(), kernelCenter, this.kernelCenterOffset);
		}
		else
		{
			if(newEdge)
			{
				return new EdgeCursor<>(this.negNewEdges.get(d), this.raSampler.copyRandomAccess(), kernelCenter, this.kernelCenterOffset);
			}
			return new EdgeCursor<>(this.negOldEdges.get(d), this.raSampler.copyRandomAccess(), kernelCenter, this.kernelCenterOffset);
		}
	}
	
	public ValuePair<EdgeCursor<T>, EdgeCursor<T>> getEdgeCursors(long[] movement)
	{
		if(movement == null)
		{
			return null;
		}
		int d=0;
		
		for( ; d < 2; d++)
		{
			if(movement[d] != 0)
			{
				break;
			}
		}
		if(d >= 2)
		{
			return null;
		}
		
		EdgeCursor<T> newEdgeC = this.getEdgeCursor(d, movement[d] > 0, true);
		EdgeCursor<T> oldEdgeC = this.getEdgeCursor(d, movement[d] < 0, false);
		return new ValuePair<>(newEdgeC, oldEdgeC);
	}
	
	/**
	 * Return a pair of cursors to the edge pixels of the neighborhood that represent the new and old pixels
	 * of the neighborhood that was moved by one pixel in a dimension. If the most recent movement of the
	 * neighborhood was not by a single pixel, then null is returned.
	 * 
	 * null is returned if there are no valid edges.
	 * @return ValuePair<Cursor<T>,Cursor<T>> First item refers to new values. Second item refers to old values.
	 */
	public ValuePair<EdgeCursor<T>, EdgeCursor<T>> getEdgeCursors()
	{
		long[] movement = this.getMovement();
		return this.getEdgeCursors(movement);
	}
	
	private void setKernel(Img<UnsignedByteType> kernelImage)
	{
		this.kernelImage = kernelImage;
		long[] kernelCenterOffsetArray = new long[kernelImage.numDimensions()];
		this.kernelImage.dimensions(kernelCenterOffsetArray);
		for(int d=0; d < kernelCenterOffsetArray.length; d++)
		{
			kernelCenterOffsetArray[d] = -kernelCenterOffsetArray[d]/2L;
		}
		this.kernelCenterOffset = Point.wrap(kernelCenterOffsetArray);
		ImgLabeling<Integer, IntType> labeling = getLabeling(this.kernelImage, true);
		LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);
		LabelRegion<Integer> region = regions.iterator().next();
		this.cKernel = region.cursor();
		this.kSize = region.size();
		this.kernel = Views.extendValue(this.kernelImage, new UnsignedByteType(0));
	}
	
	public long getKernelSize()
	{
		return this.kSize;
	}
	
	public static Img<UnsignedByteType> getKernelWithShape(Shape s)
	{
		Rectangle r = s.getBounds();
		Point kernelCenterOffset = new Point(2);
		kernelCenterOffset.setPosition(new int[]{(int) (-r.getWidth()/2.0), (int) (-r.getHeight()/2.0)});
		
		// Create a byte image backing for the kernel
		ArrayImgFactory<UnsignedByteType> factory = new ArrayImgFactory<>();
		Img<UnsignedByteType> kernelImage = factory.create(new int[]{(int) r.getWidth(), (int) r.getHeight()}, new UnsignedByteType(0));
		Cursor<UnsignedByteType> c = kernelImage.cursor();
		while(c.hasNext())
		{
			c.fwd();
			double x = c.getDoublePosition(0);
			double y = c.getDoublePosition(1);
			if(s.contains(x + 0.5, y + 0.5))
			{
				c.get().set(255);
			}
		}
		return kernelImage;
	}
	
	private ImgLabeling<Integer, IntType> getLabeling(Img<UnsignedByteType> inputImg, boolean fourConnected)
	{
		StructuringElement se = null;
		if(fourConnected)
		{
			se = StructuringElement.FOUR_CONNECTED;
		}
		else
		{
			se = StructuringElement.EIGHT_CONNECTED;
		}

		long[] dimensions = new long[inputImg.numDimensions()];
		inputImg.dimensions(dimensions);
		final Img< IntType > indexImg = ArrayImgs.ints( dimensions );
		ImgLabeling<Integer, IntType> labeling = new ImgLabeling<Integer, IntType>(indexImg);
		ConnectedComponents.labelAllConnectedComponents(Views.offsetInterval(inputImg, inputImg), labeling, new LabelGenerator(), se);		
		return labeling;
	}
	
	public Img<UnsignedByteType> getKernel()
	{
		return this.kernelImage;
	}

	private void setEdges()
	{
		this.posNewEdges = new Vector<>();
		this.negNewEdges = new Vector<>();
		this.posOldEdges = new Vector<>();
		this.negOldEdges = new Vector<>();
		for(int d=0; d < 2; d++)
		{
			// Initialize edge vectors
			this.posNewEdges.add(new Vector<long[]>());
			this.negNewEdges.add(new Vector<long[]>());
			this.posOldEdges.add(new Vector<long[]>());
			this.negOldEdges.add(new Vector<long[]>());
		}

		this.cKernel.reset();
		RandomAccess<UnsignedByteType> ra = this.kernel.randomAccess();
		long[] cur = new long[2];
		long[] pos = new long[2];
		long[] neg = new long[2];
		long[] old;
		while(this.cKernel.hasNext())
		{
			this.cKernel.fwd();
			this.cKernel.localize(cur);
			this.cKernel.localize(pos);
			this.cKernel.localize(neg);

			for(int d=0; d < 2; d++)
			{
				// Offset the neg and pos position
				neg[d] = cur[d] - 1;
				pos[d] = cur[d] + 1;

				// Check if pos edge position
				ra.setPosition(pos);
				if(ra.get().getInteger() == 0)
				{
					old = Arrays.copyOf(pos, pos.length);
					posOldEdges.get(d).add(old);
					posNewEdges.get(d).add(Arrays.copyOf(cur, cur.length));
				}

				// Check if neg edge position
				ra.setPosition(neg);
				if(ra.get().getInteger() == 0)
				{
					old = Arrays.copyOf(neg, pos.length);
					negOldEdges.get(d).add(old);
					negNewEdges.get(d).add(Arrays.copyOf(cur, cur.length));
				}
				
				// Undo the offset of the neg and pos position
				neg[d] = cur[d] + 1;
				pos[d] = cur[d] - 1;
			}
		}
	}
}
