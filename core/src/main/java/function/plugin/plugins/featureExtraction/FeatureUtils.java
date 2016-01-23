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
import net.imagej.ops.special.Functions;
import net.imagej.ops.special.UnaryFunctionOp;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <T> Polygon convert(final IterableInterval<T> src) {
		if (contourFunc == null) {
			contourFunc = (UnaryFunctionOp) Functions.unary(IJ2PluginUtility.ij().op(), Ops.Geometric.Contour.class, Polygon.class, src, true, true);
		}
		final Polygon p = (Polygon) contourFunc.compute1(src);
		return p;
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

	public <I extends IntegerType< I >> Img< UnsignedShortType > getConnectedComponentsImage(Img< I > inputImg, boolean fourConnected)
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
				if(LabelRegionUtils.contains(region, p)) //poly.contains(p))
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
}


