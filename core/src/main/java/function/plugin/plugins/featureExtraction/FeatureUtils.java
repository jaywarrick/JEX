package function.plugin.plugins.featureExtraction;

import java.util.TreeMap;
import java.util.TreeSet;

import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.plugins.featureExtraction.ConnectedComponents.StructuringElement;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import miscellaneous.Canceler;
import miscellaneous.Pair;
import net.imagej.ops.Op;
import net.imagej.ops.Ops;
import net.imagej.ops.logic.RealLogic;
import net.imagej.ops.map.MapIterableIntervalToSamplingRAI;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 * Had to copy this class out temporarily while SNAPSHOTS conflict and this code is in flux.
 * Taken from imglib2-algorithm ConnectedComponents
 * 
 * @author Tobias Pietzsch
 *
 */
public class FeatureUtils {
	
	private UnaryFunctionOp<Object, Object> contourFunc;

	public <I extends IntegerType< I >> ImgLabeling<Integer, IntType> getConnectedComponents(final RandomAccessibleInterval<I> inputImg, boolean fourConnected)
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
		ConnectedComponents.labelAllConnectedComponents(inputImg, labeling, new LabelGenerator(), 
				se);		

		return labeling;
	}

	public <I extends IntegerType< I >> ImgLabeling<Integer, IntType> getConnectedComponents(final RandomAccessibleInterval<I> inputImg, IterableInterval<Void> region, boolean fourConnected)
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
		ImgLabeling<Integer, IntType> labeling = new ImgLabeling<Integer, IntType>(new SamplingIterableRegion<IntType>(region, indexImg));
		ConnectedComponents.labelAllConnectedComponents(inputImg, labeling, new LabelGenerator(), 
				se);		

		return labeling;
	}	

	//	public static <I extends IntegerType< I >> ImgLabeling<Integer, IntType> getConnectedComponents(RandomAccessibleInterval<UnsignedByteType> reg, Img< I > inputImg, boolean fourConnected)
	//	{
	//		StructuringElement se = null;
	//		if(fourConnected)
	//		{
	//			se = StructuringElement.FOUR_CONNECTED;
	//		}
	//		else
	//		{
	//			se = StructuringElement.EIGHT_CONNECTED;
	//		}
	//		
	//		long[] dimensions = new long[reg.numDimensions()];
	//		reg.dimensions(dimensions);
	//		final Img< IntType > indexImg = ArrayImgs.ints( dimensions );
	//		ImgLabeling<Integer, IntType> labeling = new ImgLabeling<Integer, IntType>(Regions.sample(reg, inputImg));
	//		ConnectedComponents.labelAllConnectedComponents(inputImg, labeling, new LabelGenerator(), se);
	//		
	//		return labeling;
	//	}
	
	public void showLabelRegion(LabelRegion< ? > region)
	{
		long[] dimensions = new long[region.numDimensions()];
		region.dimensions(dimensions);
		final Img< UnsignedByteType > indexImg = ArrayImgs.unsignedBytes( dimensions );
		Cursor<Void> c = region.cursor();
		Point min = new Point(0,0);
		Point max = new Point(0,0);
		Point cur = new Point(0,0);
		region.min(min);
		region.max(max);
		RandomAccess<UnsignedByteType> ra = indexImg.randomAccess();
		System.out.println(min + ", " + max);
		while(c.hasNext())
		{
			c.fwd();
			System.out.println("" + (c.getIntPosition(0)-min.getIntPosition(0)) + " , " + (c.getIntPosition(1)-min.getIntPosition(1)));
			cur.setPosition(c.getIntPosition(0)-min.getIntPosition(0), 0); 
			cur.setPosition(c.getIntPosition(1)-min.getIntPosition(1), 1);
			ra.setPosition(cur);
			ra.get().set(255);
		}
		ImageJFunctions.show(indexImg);
		//ImgLabeling<Integer, UnsignedShortType> labeling = new ImgLabeling<Integer, UnsignedShortType>(indexImg);
	}

	public <I extends IntegerType< I >> Img< UnsignedShortType > getConnectedComponentsImage(RandomAccessibleInterval< I > inputImg, boolean fourConnected)
	{
		StructuringElement se = null;
		if(fourConnected)
		{
			se = ConnectedComponents.StructuringElement.FOUR_CONNECTED;
		}
		else
		{
			se = ConnectedComponents.StructuringElement.EIGHT_CONNECTED;
		}

		long[] dimensions = new long[inputImg.numDimensions()];
		inputImg.dimensions(dimensions);
		final Img< UnsignedShortType > indexImg = ArrayImgs.unsignedShorts( dimensions );
		//ImgLabeling<Integer, UnsignedShortType> labeling = new ImgLabeling<Integer, UnsignedShortType>(indexImg);
		ConnectedComponents.labelAllConnectedComponents(inputImg, indexImg, se);

		return indexImg;
	}

	public Pair<Img<UnsignedByteType>,TreeMap<Integer,PointList>> keepRegionsWithMaxima(Img<UnsignedByteType> mask, boolean fourConnected, ROIPlus maxima, Canceler canceler)
	{
		// Create a blank image
		ArrayImgFactory<UnsignedByteType> factory = new ArrayImgFactory<UnsignedByteType>();
		long[] dims = new long[mask.numDimensions()];
		mask.dimensions(dims);
		Img<UnsignedByteType> blank = factory.create(dims, new UnsignedByteType(0));

		// Get the regions
		ImgLabeling<Integer, IntType> labeling = this.getConnectedComponents(mask, fourConnected);
		//		ImageJFunctions.show(mask);
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

		for(Integer label : labelsToCopy)
		{
			LabelRegion<Integer> region = regions.getLabelRegion(label);
			//			ImageJFunctions.show(new SamplingIterableRegion(region, mask));
			Op op = IJ2PluginUtility.ij().op().op(RealLogic.Or.class, RealType.class, RealType.class);
			IJ2PluginUtility.ij().op().run(MapIterableIntervalToSamplingRAI.class, blank, Regions.sample(region, mask), op);
		}

		Pair<Img<UnsignedByteType>,TreeMap<Integer,PointList>> ret = new Pair<Img<UnsignedByteType>,TreeMap<Integer,PointList>>();
		ret.p1 = blank;
		ret.p2 = labelToPointsMap;
		return ret;
	}
	
	public LabelRegions<Integer> getSubRegions(LabelRegion<Integer> reg, Img<UnsignedByteType> mask, boolean fourConnected)
	{
		ImgLabeling<Integer, IntType> cellLabeling = this.getConnectedComponents(mask, reg, fourConnected);
		LabelRegions<Integer> subRegions = new LabelRegions<Integer>(cellLabeling);
		return subRegions;
	}

	public boolean contains(LabelRegion<?> region, IdPoint p)
	{
		// Use the random access to directly determine if the label region contains the point.
		// Should be faster than iterating
		RandomAccess<BoolType> ra = Regions.iterable(region).randomAccess();
		ra.setPosition(p);
		return ra.get().get();
		//		LabelRegionCursor c = region.localizingCursor();
		//		region.randomAccess().
		//		do
		//		{
		//			if(c.getIntPosition(0) == p.x && c.getIntPosition(1) == p.y)
		//			{
		//				return true;
		//			}
		//			c.next();
		//		} 
		//		while(c.hasNext());
		//
		//		return false;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Polygon convert(final RandomAccessibleInterval<?> src) {
		if (contourFunc == null) {
			contourFunc = (UnaryFunctionOp) Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.Contour.class, Polygon.class, src, true, true);
		}
		final Polygon p = (Polygon) contourFunc.compute1(src);
		return p;
	}
	
	//	public <T> Polygon convert(final LabelRegion<?> src)
	//	{
	//		PositionableIterableRegion<BoolType> temp = src;
	//		IterableRegion<BoolType> temp2 = temp;
	//		IterableInterval<Void> temp3 = temp2;
	//		IterableInterval<BitType> convert = Converters.convert(temp3, new MyConverter(), new BitType());
	//		return convert(convert);
	//	}

	/**
	 * 1) Create a new ImgLabeling. 2) For each pixel location that intersects between the supplied regions and the supplied mask,
	 * label the new ImgLabeling with the region's label.
	 * 
	 * Thus, the new ImgLabeling should be within the boundaries of the supplied regions, but defined within those
	 * regions by the mask and labeled the same label as the region they intersect with.
	 * 
	 * @param regions
	 * @param innerMask
	 * @return ImgLabeling representing their intersection, copying labels from regions
	 */
	public <I extends RealType<I>> ImgLabeling<Integer,IntType> intersect(LabelRegions<Integer> regions, Img<I> innerMask)
	{
		long[] size = new long[innerMask.numDimensions()];
		innerMask.dimensions(size);
		final Img< IntType > indexImg = ArrayImgs.ints( size );
		ImgLabeling<Integer, IntType> ret = new ImgLabeling<Integer, IntType>(indexImg);
		RandomAccess<LabelingType<Integer>> raRet = ret.randomAccess();
		RandomAccess<I> raInner = innerMask.randomAccess();
	
		// For each region, use a cursor to iterate over the region
		for(LabelRegion<Integer> region : regions)
		{
			IterableRegion<BoolType> itrReg = Regions.iterable(region);
			Cursor<Void> c = itrReg.cursor();
			Integer toSet = region.getLabel();
			while(c.hasNext())
			{
				// Note: this is the proper way to use a cursor. Call c.fwd() first before using.
				c.fwd();
				// If the innerMask at the cursors location is > 0, then assign the label in region to the new ImgLabeling in that location
				raInner.setPosition(c);
				if(raInner.get().getRealDouble() > 0)
				{
					raRet.setPosition(c);
					raRet.get().add(toSet);
				}
			}
		}
		return ret;
	}
	
	class MyConverter implements Converter<Void, BitType> {

		@Override
		public void convert(Void input, BitType output) {
			output.set(true);
		}
	}
}


