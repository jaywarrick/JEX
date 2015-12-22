package function.plugin.plugins.featureExtraction;

import image.roi.IdPoint;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;

public class LabelRegionUtils {

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
	public static <I extends RealType<I>> ImgLabeling<Integer,IntType> intersect(LabelRegions<Integer> regions, Img<I> innerMask)
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
	
	public static boolean contains(LabelRegion<?> region, IdPoint p)
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

}
