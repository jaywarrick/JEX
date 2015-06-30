package function.plugin.plugins.featureExtraction;

import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
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
			LabelRegionCursor c = region.iterator();
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

}
