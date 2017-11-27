package function.plugin.plugins.featureExtraction;

import java.io.File;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import Database.SingleUserDatabase.JEXReader;
import function.ops.intervals.CroppedRealRAI;
import function.ops.intervals.IntersectedBooleanRAI;
import function.ops.intervals.MapIIToSamplingRAI;
import function.plugin.IJ2.IJ2PluginUtility;
import ij.ImagePlus;
import ij.process.ByteProcessor;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.PointSamplerList;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
import miscellaneous.Canceler;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import net.imagej.ops.Op;
import net.imagej.ops.Ops;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.logic.RealLogic;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealCursor;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.converter.Converter;
import net.imglib2.converter.read.ConvertedCursor;
import net.imglib2.converter.read.ConvertedIterableInterval;
import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsFactory;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.BooleanType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.IterableRandomAccessibleInterval;
import net.imglib2.view.Views;

/**
 * Had to copy this class out temporarily while SNAPSHOTS conflict and this code is in flux.
 * Taken from imglib2-algorithm ConnectedComponents
 * 
 * @author Tobias Pietzsch
 *
 */
public class FeatureUtils {

	private UnaryFunctionOp<Object, Object> contourFunc;
	private UnaryFunctionOp<RealCursor<IntType>,Circle> cirOp;

	/////////////////////////////////////////
	//////// Connected Components ///////////
	/////////////////////////////////////////

	public <I extends IntegerType< I >> ImgLabeling<Integer, IntType> getLabeling(final RandomAccessibleInterval<I> inputImg, boolean fourConnected)
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public <T, I extends IntegerType<I>> ImgLabeling <T, I> applyLabeling(ImgLabeling<T, I> labeling, RandomAccessibleInterval<? extends RealType> mask)
	{
		// Create a labeling the same size as the mask
		long[] dims = new long[mask.numDimensions()];
		mask.dimensions(dims);
		ImgLabeling<T, I> ret = new ImgLabeling(new ArrayImgFactory< IntType >().create( dims, new IntType() ));

		// Turn the mask into a Boolean type RAI
		RandomAccess<BoolType> raMask = this.convertRealToBooleanTypeRAI(mask, new BoolType(false)).randomAccess();

		// Get the RandomAccess of the blank labeling
		RandomAccess<LabelingType<T>> raRet = ret.randomAccess();

		// Loop through the provided labeling
		Cursor<LabelingType<T>> cLabeling = labeling.cursor();
		while(cLabeling.hasNext())
		{
			cLabeling.fwd();
			raMask.setPosition(cLabeling);
			raRet.setPosition(cLabeling);
			// If the mask is true then transfer labels from labeling to ret
			if(raMask.get().get())
			{
				if(cLabeling.get().size() > 0)
				{
					raRet.get().addAll(cLabeling.get());
				}
			}
		}

		return ret;
	}

	public <T extends BooleanType<T>> ImgLabeling<Integer, IntType> getLabelingInRegion(RandomAccessibleInterval<T> reg, Img<UnsignedByteType> mask, boolean fourConnected)
	{
		CroppedRealRAI<T, UnsignedByteType> cropped = new CroppedRealRAI<>(reg, mask);
		IntervalView<UnsignedByteType> v = Views.offsetInterval(cropped, cropped);
		ImgLabeling<Integer, IntType> cellLabeling = this.getLabeling(v, fourConnected);
		return cellLabeling;
	}

	/////////////////////////////////////////
	///////////// Show Images ///////////////
	/////////////////////////////////////////

	public void showRegion(LabelRegion<?> region)
	{
		this.showRegion(region, "Label = " + region.getLabel() + " at pos: " + ((int) (region.getCenterOfMass().getDoublePosition(0) + 0.5)) + ", " + ((int) (region.getCenterOfMass().getDoublePosition(1) + 0.5)));
	}

	public <T extends BooleanType<T>> void showRegion(RandomAccessibleInterval<T> region, String title)
	{
		ImageJFunctions.showUnsignedByte(region, new BooleanTypeToUnsignedByteTypeConverter<T>(), title);
	}

	public void showRegion(LabelRegion<?> region, boolean defaultApp)
	{
		showVoidII(region, defaultApp);
	}

	public void showRegion(LabelRegion<?> region, Interval i)
	{
		this.showVoidII(region, i);
	}

	public void showRegion(LabelRegion<?> region, Interval i, boolean defaultApp)
	{
		this.showVoidII(region, i, defaultApp);
	}

	public <T extends BooleanType<T>> void showBooleanRAI(RandomAccessibleInterval< T > rai)
	{
		this.showBooleanRAI(rai, false);
	}

	public <T extends BooleanType<T>> void showBooleanRAI(RandomAccessibleInterval< T > rai, boolean defaultApp)
	{
		RandomAccessibleInterval<UnsignedByteType> converted = this.convertBooleanTypeToByteRAI(rai);
		FileUtility.showImg(converted, defaultApp);
	}

	public <T extends RealType<T>> void show(RandomAccessibleInterval< T > rai)
	{
		this.show(rai, false);
	}

	public <T extends RealType<T>> void show(RandomAccessibleInterval< T > rai, boolean defaultApp)
	{
		FileUtility.showImg(rai, defaultApp);
	}

	public <T extends BooleanType<T>> void showBooleanII(IterableInterval< T > region, Interval i, boolean defaultApp)
	{
		this.show(this.makeImgFromBooleanII(region, i), defaultApp);
	}

	public <T extends BooleanType<T>> void showBooleanII(IterableInterval< T > region, boolean defaultApp)
	{
		this.showBooleanII(region, null, defaultApp);
	}

	public <T extends BooleanType<T>> void showBooleanII(IterableInterval< T > region)
	{
		this.showBooleanII(region, null, false);
	}

	/**
	 * Mainly used with MirroredLabelRegionCursor where you are using a cursor
	 * other than that directly returned by the label region
	 * @param c
	 * @param i
	 * @param defaultApp
	 */
	public void showVoidCursor(Cursor< Void > c, Interval i, boolean defaultApp)
	{
		this.show(this.makeImgFromVoidCursor(c, i), defaultApp);
	}

	/**
	 * Mainly used with MirroredLabelRegionCursor where you are using a cursor
	 * other than that directly returned by the label region
	 * @param c
	 * @param i
	 * @param defaultApp
	 */
	public void showVoidCursor(Cursor< Void > c, Interval i)
	{
		this.show(this.makeImgFromVoidCursor(c, i), false);
	}

	public void showVoidII(IterableInterval< Void > region, Interval i, boolean defaultApp)
	{
		this.show(this.makeImgFromVoidII(region, i), defaultApp);
	}

	public void showVoidII(IterableInterval< Void > region, boolean defaultApp)
	{
		this.showVoidII(region, null, defaultApp);
	}

	public void showVoidII(IterableInterval< Void > region, Interval i)
	{
		this.showVoidII(region, i, false);
	}

	public void showVoidII(IterableInterval< Void > region)
	{
		this.showVoidII(region, null, false);
	}

	public <T extends RealType<T>>void showRealII(IterableInterval< T > region, boolean defaultApp)
	{
		this.show(this.makeImgFromRealII(region), defaultApp);
	}

	public <T extends RealType<T>>void showRealII(IterableInterval< T > region, Interval i, boolean defaultApp)
	{
		this.show(this.makeImgFromRealII(region, i), defaultApp);
	}

	public <T extends RealType<T>>void showRealII(IterableInterval< T > region)
	{
		this.showRealII(region, true);
	}

	public void show(ImgLabeling<Integer,IntType> labeling)
	{
		this.show(labeling, false);
	}

	public void show(ImgLabeling<Integer,IntType> labeling, boolean asMask)
	{
		if(asMask)
		{
			FileUtility.showImg(this.makeImgMaskFromLabeling(labeling), false);
		}
		else
		{
			FileUtility.showImg(this.makeImgFromLabeling(labeling), true);
		}
	}

	/////////////////////////////////////////
	///////////// Make Images ///////////////
	/////////////////////////////////////////

	public Img<UnsignedShortType> makeImgFromShortII(IterableInterval<UnsignedShortType> ii)
	{
		long[] dims = new long[ii.numDimensions()];
		ii.dimensions(dims);
		Img<UnsignedShortType> img = ArrayImgs.unsignedShorts(dims);
		Cursor<UnsignedShortType> c = ii.cursor();
		RandomAccess<UnsignedShortType> ra = img.randomAccess();
		while(c.hasNext())
		{
			c.fwd();
			ra.setPosition(c);
			ra.get().set(c.get());
		}
		return img;
	}

	public Img<UnsignedByteType> makeImgFromByteII(IterableInterval<UnsignedByteType> ii)
	{
		long[] dims = new long[ii.numDimensions()];
		ii.dimensions(dims);
		Img<UnsignedByteType> img = ArrayImgs.unsignedBytes(dims);
		Cursor<UnsignedByteType> c = ii.cursor();
		RandomAccess<UnsignedByteType> ra = img.randomAccess();
		while(c.hasNext())
		{
			c.fwd();
			ra.setPosition(c);
			ra.get().set(c.get());
		}
		return img;
	}

	public Img<UnsignedByteType> makeImgFromByteRAI(RandomAccessibleInterval<UnsignedByteType> src)
	{
		RandomAccessibleInterval<UnsignedByteType> rai = Views.offsetInterval(src, src);
		long[] dims = new long[rai.numDimensions()];
		rai.dimensions(dims);
		Img<UnsignedByteType> img = ArrayImgs.unsignedBytes(dims);
		Cursor<UnsignedByteType> c = img.cursor();
		RandomAccess<UnsignedByteType> ra = rai.randomAccess();
		while(c.hasNext())
		{
			c.fwd();
			ra.setPosition(c);
			c.get().set(ra.get());
		}
		return img;
	}

	public <T extends RealType<T>> Img<UnsignedShortType> makeImgFromRealII(IterableInterval< T > region)
	{
		return makeImgFromRealII(region, null);
	}

	public <T extends RealType<T>> Img<UnsignedShortType> makeImgFromRealII(IterableInterval< T > region, Interval i)
	{
		final Img<UnsignedShortType> ret;
		if(i == null)
		{
			ret = makeBlackShortImageFromInterval(region);
		}
		else
		{
			ret = makeBlackShortImageFromInterval(i);
		}
		Cursor<T> c = region.cursor();
		Point min = new Point(0,0);
		Point max = new Point(0,0);
		Point cur = new Point(0,0);
		region.min(min);
		region.max(max);
		RandomAccess<UnsignedShortType> ra = ret.randomAccess();
		//System.out.println(min + ", " + max);
		while(c.hasNext())
		{
			c.fwd();
			//System.out.println("" + (c.getIntPosition(0)-min.getIntPosition(0)) + " , " + (c.getIntPosition(1)-min.getIntPosition(1)));
			if(i == null)
			{
				// Draw the image relative to itself
				cur.setPosition(c.getIntPosition(0)-min.getIntPosition(0), 0); 
				cur.setPosition(c.getIntPosition(1)-min.getIntPosition(1), 1);
			}
			else
			{
				// Draw the region on an image with upper left-hand corner at 0,0
				cur.setPosition(c.getIntPosition(0), 0); 
				cur.setPosition(c.getIntPosition(1), 1);
			}
			ra.setPosition(cur);
			ra.get().setReal(c.get().getRealDouble());
		}
		return ret;
	}

	public <T extends BooleanType<T>> Img<UnsignedByteType> makeImgFromBooleanII(IterableInterval< T > region)
	{
		return makeImgFromBooleanII(region, region);
	}

	public <T extends BooleanType<T>> Img<UnsignedByteType> makeImgFromBooleanII(IterableInterval< T > region, Interval i)
	{
		final Img<UnsignedByteType> ret;
		if(i == null)
		{
			ret = makeBlackByteImageFromInterval(region);
		}
		else
		{
			ret = makeBlackByteImageFromInterval(i);
		}
		Cursor<T> c = region.cursor();
		Point min = new Point(0,0);
		Point max = new Point(0,0);
		Point cur = new Point(0,0);
		region.min(min);
		region.max(max);
		RandomAccess<UnsignedByteType> ra = ret.randomAccess();
		//System.out.println(min + ", " + max);
		while(c.hasNext())
		{
			c.fwd();
			//System.out.println("" + (c.getIntPosition(0)-min.getIntPosition(0)) + " , " + (c.getIntPosition(1)-min.getIntPosition(1)));
			if(i == null)
			{
				// Draw the image relative to itself
				cur.setPosition(c.getIntPosition(0)-min.getIntPosition(0), 0); 
				cur.setPosition(c.getIntPosition(1)-min.getIntPosition(1), 1);
			}
			else
			{
				// Draw the region on an image with upper left-hand corner at 0,0
				cur.setPosition(c.getIntPosition(0), 0); 
				cur.setPosition(c.getIntPosition(1), 1);
			}
			ra.setPosition(cur);
			if(c.get().get())
			{
				ra.get().set(255);
			}
		}
		return ret;
	}

	public Img<UnsignedByteType> makeImgFromVoidII(IterableInterval< Void > region)
	{
		return makeImgFromVoidII(region, region);
	}

	/**
	 * This is mainly used for MirroredLabelRegionCursors as you will have a cursor
	 * that you want to visualize that isn't the cursor directly returned by the
	 * label region object.
	 * @param c
	 * @param region
	 * @return
	 */
	public Img<UnsignedByteType> makeImgFromVoidCursor(Cursor<Void> c, Interval region)
	{
		final Img<UnsignedByteType> ret;
		ret = makeBlackByteImageFromInterval(region);

		Point min = new Point(0,0);
		Point max = new Point(0,0);
		Point cur = new Point(0,0);
		region.min(min);
		region.max(max);
		RandomAccess<UnsignedByteType> ra = ret.randomAccess();
		//System.out.println(min + ", " + max);
		while(c.hasNext())
		{
			c.fwd();
			//System.out.println("" + (c.getIntPosition(0)-min.getIntPosition(0)) + " , " + (c.getIntPosition(1)-min.getIntPosition(1)));
			// Draw the image relative to itself
			cur.setPosition(c.getIntPosition(0)-min.getIntPosition(0), 0); 
			cur.setPosition(c.getIntPosition(1)-min.getIntPosition(1), 1);
			ra.setPosition(cur);
			UnsignedByteType toSet = ra.get();
			if(toSet != null)
			{
				toSet.set(255);
			}
		}
		return ret;
	}

	public Img<UnsignedByteType> makeImgFromVoidII(IterableInterval< Void > region, Interval i)
	{
		final Img<UnsignedByteType> ret;
		if(i == null)
		{
			ret = makeBlackByteImageFromInterval(region);
		}
		else
		{
			ret = makeBlackByteImageFromInterval(i);
		}

		Cursor<Void> c = region.cursor();
		Point min = new Point(0,0);
		Point max = new Point(0,0);
		Point cur = new Point(0,0);
		region.min(min);
		region.max(max);
		RandomAccess<UnsignedByteType> ra = ret.randomAccess();
		//System.out.println(min + ", " + max);
		while(c.hasNext())
		{
			c.fwd();
			//System.out.println("" + (c.getIntPosition(0)-min.getIntPosition(0)) + " , " + (c.getIntPosition(1)-min.getIntPosition(1)));
			if(i == null)
			{
				// Draw the image relative to itself
				cur.setPosition(c.getIntPosition(0)-min.getIntPosition(0), 0); 
				cur.setPosition(c.getIntPosition(1)-min.getIntPosition(1), 1);
			}
			else
			{
				// Draw the region on an image with upper left-hand corner at 0,0
				cur.setPosition(c.getIntPosition(0), 0); 
				cur.setPosition(c.getIntPosition(1), 1);
			}
			ra.setPosition(cur);
			UnsignedByteType toSet = ra.get();
			if(toSet != null)
			{
				toSet.set(255);
			}
		}
		return ret;
	}

	public Img<UnsignedByteType> makeBlackByteImageFromInterval(Interval i)
	{
		long[] dimensions = new long[i.numDimensions()];
		i.dimensions(dimensions);
		final Img<UnsignedByteType> ret = ArrayImgs.unsignedBytes( dimensions );
		return ret;
	}

	public Img<UnsignedByteType> makeWhiteByteImageFromInterval(Interval i)
	{
		final Img<UnsignedByteType> ret = makeBlackByteImageFromInterval(i);

		Cursor<UnsignedByteType> c = ret.cursor();
		while(c.hasNext())
		{
			c.fwd();
			c.get().set(255);
		}
		return ret;
	}

	public Img<UnsignedShortType> makeBlackShortImageFromInterval(Interval i)
	{
		long[] dimensions = new long[i.numDimensions()];
		i.dimensions(dimensions);
		final Img<UnsignedShortType> ret = ArrayImgs.unsignedShorts( dimensions );
		return ret;
	}

	public Img<UnsignedShortType> makeWhiteShortImageFromInterval(Interval i)
	{
		final Img<UnsignedShortType> ret = makeBlackShortImageFromInterval(i);

		Cursor<UnsignedShortType> c = ret.cursor();
		while(c.hasNext())
		{
			c.fwd();
			c.get().set(65535);
		}
		return ret;
	}

	public Img<UnsignedByteType> makeImgMaskFromLabeling(ImgLabeling<Integer,IntType> labeling)
	{
		LabelRegions<Integer> regions = new LabelRegions<>(labeling);
		final Img< UnsignedByteType > ret = makeBlackByteImageFromInterval(labeling);
		RandomAccess<UnsignedByteType> ra = ret.randomAccess();
		for(LabelRegion<Integer> region : regions)
		{
			Cursor<Void> c = region.cursor();
			while(c.hasNext())
			{
				c.fwd();
				ra.setPosition(c);
				ra.get().set(255);
			}
		}
		return ret;
	}

	public Img<UnsignedShortType> makeImgFromLabeling(ImgLabeling<Integer,IntType> labeling)
	{
		LabelRegions<Integer> regions = new LabelRegions<>(labeling);

		final Img< UnsignedShortType > ret = makeBlackShortImageFromInterval(labeling);
		RandomAccess<UnsignedShortType> ra = ret.randomAccess();
		for(LabelRegion<Integer> region : regions)
		{
			Cursor<Void> c = region.cursor();
			while(c.hasNext())
			{
				c.fwd();
				ra.setPosition(c);
				ra.get().setInteger(region.getLabel());
			}
		}
		return ret;
	}

	/////////////////////////////////////////
	///////// Geometry Functions ////////////
	/////////////////////////////////////////

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T extends BooleanType<T>> Polygon getPolygonFromBoolean(final RandomAccessibleInterval<T> src) {
		if (contourFunc == null) {
			contourFunc = (UnaryFunctionOp) Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.Contour.class, Polygon.class, src, true);
		}
		final Polygon p = (Polygon) contourFunc.calculate(src);
		return p;
	}

	public <T extends RealType<T>> Polygon getPolygonFromReal(final RandomAccessibleInterval<T> src)
	{
		return this.getPolygonFromBoolean(this.convertRealToBooleanTypeRAI(src, new BoolType(false)));
	}

	/**
	 * If center is null the circle is the smallest enclosing circle.
	 * If not, the circle is the smallest enclosing circle with a center at 'center'.
	 * @param pg
	 * @param center
	 * @return
	 */
	public Circle getCircle(Polygon pg, RealLocalizable center)
	{
		PointSamplerList<IntType> psl = new PointSamplerList<>(pg.getVertices(), new IntType(0));
		if(this.cirOp == null)
		{
			cirOp = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, psl.cursor(), center);
		}
		UnaryFunctionOp<RealCursor<IntType>,Circle> cirOp = Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.SmallestEnclosingCircle.class, Circle.class, psl.cursor(), center);
		return cirOp.calculate(psl.cursor());
	}

	public <T extends BooleanType<T>> Circle getCircle(RandomAccessibleInterval<T> region, RealLocalizable center)
	{
		Polygon p = this.getPolygonFromBoolean(region);
		return this.getCircle(p, center);
	}

	/////////////////////////////////////////
	//////////// Sub-routines ///////////////
	/////////////////////////////////////////

	public void filterMaskRegions(ImagePlus im, int minSize, int maxSize, boolean fourConnected, boolean filterWhite, boolean keep)
	{
		this.filterMaskRegions((ByteProcessor) im.getProcessor(), minSize, maxSize, fourConnected, filterWhite, keep);
	}

	public Img<UnsignedByteType> filterMaskRegions(String imagePath, int minSize, int maxSize, boolean fourConnected, boolean filterWhite, boolean keep)
	{
		// Get image data
		File f = new File(imagePath);
		if(!f.exists())
		{
			return null;
		}

		Img<UnsignedByteType> img = JEXReader.getSingleImage(imagePath, 0.0);
		filterMaskRegions(img, minSize, maxSize, fourConnected, filterWhite, keep);
		return img;
	}

	public void invert(Img<UnsignedByteType> img)
	{
		for(UnsignedByteType t : img)
		{
			if(t.getInteger() == 0)
			{
				t.setInteger(255);
			}
			else
			{
				t.setInteger(0);
			}
		}
	}

	public void filterMaskRegions(Img<UnsignedByteType> img, int minSize, int maxSize, boolean fourConnected, boolean filterWhite, boolean keep)
	{
		if(!filterWhite)
		{
			this.invert(img);
		}
		ImgLabeling<Integer, IntType> labeling = this.getLabeling(img, fourConnected);
		LabelRegions<Integer> regions = new LabelRegions<>(labeling);
		for(LabelRegion<Integer> r : regions)
		{
			if(keep)
			{
				if(r.size() <= minSize || r.size() >= maxSize)
				{
					this.setPixelsInRegion(img, r, 0);
				}
			}
			else
			{
				if(r.size() >= minSize && r.size() <= maxSize)
				{
					this.setPixelsInRegion(img, r, 0);
				}
			}
		}

		if(!filterWhite)
		{
			this.invert(img);
		}
	}

	public void filterMaskRegions(ByteProcessor bp, int minSize, int maxSize, boolean fourConnected, boolean filterWhite, boolean keep)
	{
		if(!filterWhite)
		{
			bp.invert();
		}
		Img<UnsignedByteType> img = ImageJFunctions.wrapByte(new ImagePlus("bp",bp));
		ImgLabeling<Integer, IntType> labeling = this.getLabeling(img, fourConnected);
		LabelRegions<Integer> regions = new LabelRegions<>(labeling);
		for(LabelRegion<Integer> r : regions)
		{
			if(keep)
			{
				if(r.size() <= minSize || r.size() >= maxSize)
				{
					this.setPixelsInRegion(bp, r, 0);
				}
			}
			else
			{
				if(r.size() >= minSize && r.size() <= maxSize)
				{
					this.setPixelsInRegion(bp, r, 0);
				}
			}
		}

		if(!filterWhite)
		{
			bp.invert();
		}
	}

	public void setPixelsInRegion(Img<UnsignedByteType> mask, LabelRegion<Integer> region, int val)
	{
		Cursor<Void> c = region.cursor();
		RandomAccess<UnsignedByteType> ra = mask.randomAccess();
		while(c.hasNext())
		{
			c.fwd();
			ra.setPosition(c);
			ra.get().setInteger(val);
		}
	}

	public void setPixelsInRegion(ByteProcessor mask, LabelRegion<Integer> region, int val)
	{
		Cursor<Void> c = region.cursor();
		int[] pos = new int[2];
		while(c.hasNext())
		{
			c.fwd();
			c.localize(pos);
			mask.set(pos[0], pos[1], val);
		}
	}

	public Pair<Img<UnsignedByteType>,TreeMap<Integer,PointList>> keepRegionsWithMaxima(Img<UnsignedByteType> mask, boolean fourConnected, ROIPlus maxima, boolean removeClumps, Canceler canceler)
	{
		// Create a blank image
		ArrayImgFactory<UnsignedByteType> factory = new ArrayImgFactory<UnsignedByteType>();
		long[] dims = new long[mask.numDimensions()];
		mask.dimensions(dims);
		Img<UnsignedByteType> blank = factory.create(dims, new UnsignedByteType(0));

		// Get the regions
		ImgLabeling<Integer, IntType> labeling = this.getLabeling(mask, fourConnected);
		//		ImageJFunctions.show(mask);
		//		this.show(mask);
		//		this.show(labeling);
		LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);

		TreeMap<Integer, PointList> labelToPointsMap = new TreeMap<Integer,PointList>();
		TreeSet<Integer> labelsToCopy = new TreeSet<Integer>();

		// For each region, if contains a maxima, "copy" the region to the blank image 
		for(Integer label : regions.getExistingLabels())
		{
			if(canceler != null && canceler.isCanceled())
			{
				return null;
			}
			LabelRegion<Integer> region = regions.getLabelRegion(label);
			//Polygon poly = convert(region);
			if(maxima == null)
			{
				JEXDialog.messageDialog("Check to see if the ROI object has an extra dimension compared to the Masks. Can't find a maxima for this mask. Aborting.");
				return null; // i.e., cancel the run.
			}
			for(IdPoint p : maxima.getPointList())
			{
				if(canceler != null && canceler.isCanceled())
				{
					return null;
				}
				if(this.contains(region, p)) //poly.contains(p))
				{
					PointList pl = labelToPointsMap.get(label);
					if(pl == null)
					{
						pl = new PointList();
					}
					pl.add(p.copy());
					labelToPointsMap.put(label, pl);
					labelsToCopy.add(label);
				}
			}
		}

		// Create a mask with only the regions of containing maxima and, depending on 'keepClumps', exclude regions with multiple maxima as well.
		Vector<Integer> labelsToRemove = new Vector<>();
		for(Integer label : labelsToCopy)
		{
			if(!removeClumps || labelToPointsMap.get(label).size() == 1)
			{
				LabelRegion<Integer> region = regions.getLabelRegion(label);
				//			ImageJFunctions.show(new SamplingIterableRegion(region, mask));
				Op op = IJ2PluginUtility.ij().op().op(RealLogic.Or.class, RealType.class, RealType.class);
				IJ2PluginUtility.ij().op().run(MapIIToSamplingRAI.class, blank, Regions.sample(region, mask), op);
			}
			else
			{
				labelsToRemove.add(label);
			}
		}
		for(Integer label : labelsToRemove)
		{
			labelToPointsMap.remove(label);
		}

		Pair<Img<UnsignedByteType>,TreeMap<Integer,PointList>> ret = new Pair<Img<UnsignedByteType>,TreeMap<Integer,PointList>>();
		ret.p1 = blank;
		ret.p2 = labelToPointsMap;
		return ret;
	}

	/////////////////////////////////////////
	///////// Region Manipulation ///////////
	/////////////////////////////////////////

	public <T extends BooleanType<T>, R extends RealType<R>> RandomAccessibleInterval<R> cropRealRAI(RandomAccessibleInterval<T> region, RandomAccessibleInterval<R> img)
	{
		return new CroppedRealRAI<T,R>(region, img);
	}

	public RandomAccessibleInterval<BoolType> extendSubRegionToParentRegionBounds(LabelRegion<Integer> subRegion, LabelRegion<Integer> parentRegion)
	{
		final OutOfBoundsFactory< BoolType, RandomAccessibleInterval< BoolType >> oobImageFactory = new OutOfBoundsConstantValueFactory<>( new BoolType(false) );
		return Views.interval(Views.extend(subRegion, oobImageFactory), parentRegion);
	}

	public boolean contains(LabelRegion<?> region, IdPoint p)
	{
		// Use the random access to directly determine if the label region contains the point.
		// Should be faster than iterating
		RandomAccess<BoolType> ra = Regions.iterable(region).randomAccess();
		ra.setPosition(p);
		return ra.get().get();
	}

	//	Commented out as it appears that the regions need to be of identical size, which is typically unlikely
	//  public <T extends BooleanType<T>> IterableRegion<T> intersectRegions(IterableRegion<T> a, IterableRegion<T> b)
	//	{
	//		RandomAccessibleInterval<T> temp = intersectRAIs(a, b);
	//		return Regions.iterable(temp);
	//	}

	/**
	 * Assumes the RAI's are the same size.
	 * @param a
	 * @param b
	 * @return
	 */
	public <T extends BooleanType<T>> RandomAccessibleInterval<T> intersectRAIs(RandomAccessibleInterval<T> a, RandomAccessibleInterval<T> b)
	{
		if(a.numDimensions() != b.numDimensions())
		{
			throw new IllegalArgumentException("The RAIs must have the same number of dimensions");
		}
		for(int i = 0; i < a.numDimensions(); i++)
		{
			if(a.realMin(i) != b.realMin(i))
			{
				throw new IllegalArgumentException("The RAIs must be specified over the same interval");
			}
			if(a.realMax(i) != b.realMax(i))
			{
				throw new IllegalArgumentException("The RAIs must be specified over the same interval");
			}
		}
		return new IntersectedBooleanRAI<T>(a, b);
	}

	/////////////////////////////////////////
	////////////// Conversion ///////////////
	/////////////////////////////////////////

	public <T extends BooleanType<T>> RandomAccessibleInterval<UnsignedByteType> convertBooleanTypeToByteRAI(RandomAccessibleInterval<T> rai)
	{
		return new ConvertedRandomAccessibleInterval<T, UnsignedByteType>(rai, new BooleanTypeToUnsignedByteTypeConverter<T>(), new UnsignedByteType(0));
	}

	public <R extends RealType<R>, I extends BooleanType<I>> RandomAccessibleInterval<I> convertRealToBooleanTypeRAI(RandomAccessibleInterval<R> rai, I exemplar)
	{
		return new ConvertedRandomAccessibleInterval<R, I>(rai, new RealTypeToBoolTypeConverter<R>(), exemplar);
	}

	public IterableInterval<BitType> convertVoidToBitTypeII(IterableInterval<Void> ii)
	{
		return new ConvertedIterableInterval<>(ii, new VoidToBitTypeConverter(), new BitType(false));
	}

	public IterableInterval<BitType> convertBoolTypeToBitTypeII(IterableInterval<BoolType> ii)
	{
		return new ConvertedIterableInterval<>(ii, new BoolTypeToBitTypeConverter(), new BitType(false));
	}

	public Cursor<BitType> convertVoidTypeToBitTypeCursor(Cursor<Void> c)
	{
		ConvertedCursor< Void, BitType > ret = new ConvertedCursor<>( c, new VoidToBitTypeConverter(), new BitType(false) );
		return ret;
	}

	public <T> IterableRandomAccessibleInterval<T> convertRAItoIterableRAI(RandomAccessibleInterval<T> rai)
	{
		return IterableRandomAccessibleInterval.create(rai);
	}

	////////// Converters ///////////

	public class BoolTypeToBitTypeConverter implements Converter<BoolType, BitType> {

		@Override
		public void convert(BoolType input, BitType output) {
			output.set(new BitType(input.get()));
		}
	}

	public class VoidToBitTypeConverter implements Converter<Void, BitType> {

		@Override
		public void convert(Void input, BitType output) {
			output.set(true);
		}
	}

	public class BooleanTypeToUnsignedByteTypeConverter<T extends BooleanType<T>> implements Converter<T, UnsignedByteType> {

		@Override
		public void convert(T input, UnsignedByteType output) {
			if(input.get())
			{
				output.set(255);
			}
			else
			{
				output.set(0);
			}

		}
	}

	/**
	 * Note that this converter works to convert UnsignedByteType (which is a RealType) to a BoolType (which is a BooleanType)
	 * @author jaywarrick
	 *
	 * @param <R>
	 */
	@SuppressWarnings("rawtypes")
	public class RealTypeToBoolTypeConverter<R extends RealType<R>> implements Converter<R, BooleanType> {

		@Override
		public void convert(R input, BooleanType output) {
			if(input.getRealDouble() > 0)
			{
				output.set(true);
			}
			else
			{
				output.set(false);
			}

		}
	}

	public class RealTypeToBitTypeConverter<R extends RealType<R>> implements Converter<R, BitType> {

		@Override
		public void convert(R input, BitType output) {
			if(input.getRealDouble() > 0)
			{
				output.set(true);
			}
			else
			{
				output.set(false);
			}
		}
	}
}


