// Define package name as "plugins" as show here
package function.plugin.plugins.featureExtraction;

// Import needed classes here 
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXStatics;
import logs.Logs;
import net.imagej.ops.features.sets.FirstOrderStatFeatureSet;
import net.imagej.ops.features.sets.GeometricFeatureSet;
import net.imagej.ops.features.sets.Haralick2DFeatureSet;
import net.imagej.ops.features.sets.HistogramFeatureSet;
import net.imagej.ops.features.sets.ImageMomentsFeatureSet;
import net.imagej.ops.features.sets.ZernikeFeatureSet;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

// Specify plugin characteristics here
@Plugin(
		type = JEXPlugin.class,
		name="Test Feature Extraction",
		menuPath="Feature Extraction",
		visible=true,
		description="Function for testing feature extraction using the ImageJ Ops framework."
		)
public class TestFeatureExtraction extends JEXPlugin {

	public ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij().getContext());
	public FirstOrderStatFeatureSet<IterableInterval<UnsignedShortType>> opFirstOrder = null;
	public GeometricFeatureSet opGeometric = null;
	public Haralick2DFeatureSet<UnsignedShortType> opHaralick2DDiag = null;
	public Haralick2DFeatureSet<UnsignedShortType> opHaralick2DAntiDiag = null;
	public Haralick2DFeatureSet<UnsignedShortType> opHaralick2DHor = null;
	public Haralick2DFeatureSet<UnsignedShortType> opHaralick2DVer = null;
	public HistogramFeatureSet<UnsignedShortType> opHistogram = null;
	public ImageMomentsFeatureSet<IterableInterval<UnsignedShortType>> opMoments = null;
	public ZernikeFeatureSet<UnsignedByteType> opZernike = null;

	// Define a constructor that takes no arguments.
	public TestFeatureExtraction()
	{}

	/////////// Define Inputs here ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Intensity images", optional=false)
	JEXData imageData;

	@InputMarker(uiOrder=2, name="Whole Cell Mask", type=MarkerConstants.TYPE_IMAGE, description="Mask images defining the whole cell (should NOT have channel dimension)", optional=false)
	JEXData cellMaskData;
	
	@InputMarker(uiOrder=3, name="Masks to Measure", type=MarkerConstants.TYPE_IMAGE, description="Mask images (SHOULD have channel dimension)", optional=false)
	JEXData measureMaskData;

	@InputMarker(uiOrder=4, name="Maxima", type=MarkerConstants.TYPE_ROI, description="Maxima ROI", optional=false)
	JEXData roiData;

	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=1, name="** Channel Dimension Name", description="Name of the dimension that represents the different imaging channels", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String channelName;
	
	@ParameterMarker(uiOrder=2, name="Whole Cell Channel", description="Name of the channel that represents the whole cell inside which all other features of the cells should lie.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Blue + Green")
	String wholeCellChannel;

	@ParameterMarker(uiOrder=3, name="** Compute First Order Stats?", description="Whether to quantify first order statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean firstOrder;

	@ParameterMarker(uiOrder=4, name="** Compute Geometric Stats?", description="Whether to quantify geometric statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean geometric;

	@ParameterMarker(uiOrder=5, name="** Compute Haralick 2D Stats?", description="Whether to quantify Haralick texture statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean haralick2D;

	@ParameterMarker(uiOrder=6, name="Haralick Gray Levels", description="Number of gray levels for Haralick calculations", ui=MarkerConstants.UI_TEXTFIELD, defaultText="8")
	int haralickGrayLevels;

	@ParameterMarker(uiOrder=7, name="Haralick Co-Occurrence Matrix Distance", description="Distance at which to compute the co-occurrence matrix", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double haralickDistance;

	@ParameterMarker(uiOrder=8, name="Number of Haralick Directions", description="(Orthogonals and Diagonals etc) 2 performs horizontal and vertical. 4 adds the 2 diagonals as well.", ui=MarkerConstants.UI_DROPDOWN, choices={"2", "4"}, defaultChoice=1)
	String haralickNumDirections;

	@ParameterMarker(uiOrder=9, name="** Compute Histogram Stats?", description="Whether to quantify histogram statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean histogram;

	@ParameterMarker(uiOrder=10, name="Number of Histogram Bins", description="Number of bins for the histogram created for each cell region", ui=MarkerConstants.UI_TEXTFIELD, defaultText="256")
	int histogramBins;

	@ParameterMarker(uiOrder=11, name="** Compute Moments Stats?", description="Whether to quantify image moment statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean moments;

	@ParameterMarker(uiOrder=12, name="** Compute Zernike Stats?", description="Whether to quantify Zernike shape statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean zernike;

	@ParameterMarker(uiOrder=13, name="Include Zernike Magnitudes?", description="Whether to quantify magnitudes of Zernike features", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean zernikeMagnitude;

	@ParameterMarker(uiOrder=14, name="Include Zernike Phases?", description="Whether to quantify phase of Zernike features", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean zernikePhase;

	@ParameterMarker(uiOrder=15, name="Zernike Min Moment", description="Min Zernike moment calculate", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int zernikeMomentMin;

	@ParameterMarker(uiOrder=16, name="Zernike Min Moment", description="Min Zernike moment calculate", ui=MarkerConstants.UI_TEXTFIELD, defaultText="3")
	int zernikeMomentMax;

	@ParameterMarker(uiOrder=17, name="** Connectedness of Objects", description="The structuring element or number of neighbors to require to be part of the neighborhood.", ui=MarkerConstants.UI_DROPDOWN, choices={"4 Connected", "8 Connected"}, defaultChoice=0)
	String connectedness;

	// Add a primary mask and secondary mask

	/////////// Define Outputs here ///////////

	@OutputMarker(uiOrder=1, name="Output Table", type=MarkerConstants.TYPE_FILE, flavor="", description="Test table output.", enabled=true)
	JEXData outputTable;

	// Define threading capability here (set to 1 if using non-final static variables shared between function instances).
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	// Code the actions of the plugin here using comments for significant sections of code to enhance readability as shown here
	@Override
	public boolean run(JEXEntry optionalEntry)
	{

		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> cellMaskMap = ImageReader.readObjectToImagePathTable(cellMaskData);
		TreeMap<DimensionMap,String> measureMaskMap = cellMaskMap;
		if(measureMaskData != null)
		{
			measureMaskMap = ImageReader.readObjectToImagePathTable(measureMaskData);
		}
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,Double> outputStatMap = new TreeMap<DimensionMap,Double>();
		LabelRegion<Integer> reg;

		int count = 0, percentage = 0, total = measureMaskMap.size() + imageMap.size();

		if(!firstOrder && !geometric && !haralick2D && !histogram && !moments && !zernike)
		{
			Logs.log("Nothing selected to compute. Returning false.", this);
			return false;
		}
		try
		{						
			// Loop over whole cell masks
			for (DimensionMap mapCell : cellMaskData.getDimTable().getMapIterator())
			{

				if(this.isCanceled())
				{
					String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
					outputTable = FileWriter.makeFileObject("temp", null, tablePath);
					return true;
				}
				
				SCIFIOImgPlus<UnsignedByteType> cellMask = imgOpener.openImgs(measureMaskMap.get(mapCell), new UnsignedByteType()).get(0);
				ImgLabeling<Integer,IntType> cellLabeling = FeatureUtils.getConnectedComponents(cellMask, connectedness.equals("4 Connected"));
				LabelRegions<Integer> cellRegions = new LabelRegions<Integer>(cellLabeling);
				TreeMap<Integer,Integer> idToLabelMap = new TreeMap<Integer,Integer>();
				
				// Determine which LabelRegions are the ones we want to keep by testing if our maxima of interest are contained.
				ROIPlus maxima = roiMap.get(mapCell);
				for(LabelRegion<Integer> cellRegion : cellRegions)
				{
					for(IdPoint p : maxima.pointList)
					{
						if(this.isCanceled())
						{
							String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
							outputTable = FileWriter.makeFileObject("temp", null, tablePath);
							return true;
						}
						if(contains(cellRegion, p))
						{
							idToLabelMap.put(p.id, cellRegion.getLabel());
						}
					}
				}

				// For each measure mask, intersect the measure mask with the whole cell regions and perform feature extraction
				for(DimensionMap mapMeasure : measureMaskData.getDimTable().getMapIterator(mapCell))
				{
					if(this.isCanceled())
					{
						String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
						outputTable = FileWriter.makeFileObject("temp", null, tablePath);
						return true;
					}

					// Intersect measure mask with whole cell regions
					Img<UnsignedByteType> measureMask = imgOpener.openImgs(measureMaskMap.get(mapMeasure), new UnsignedByteType()).get(0);
					ImgLabeling<Integer, IntType> measureLabeling = LabelRegionUtils.intersect(cellRegions, measureMask);
					LabelRegions<Integer> measureRegions = new LabelRegions<Integer>(measureLabeling);

					// Perform shape feature measurements first if desired
					for(IdPoint p : maxima.pointList)
					{
						Integer labelId = idToLabelMap.get(p.id);
						if(labelId == null)
						{
							continue;
						}

						reg = measureRegions.getLabelRegion(labelId);
						if(reg == null)
						{
							continue;
						}

						if(!this.putGeometric(outputStatMap, mapMeasure, p.id, reg, measureMask))
						{
							return true;
						}

						if(!this.putZernike(outputStatMap, mapMeasure, p.id, reg, measureMask))
						{
							return true;
						}
					}

					// Then do texture and intensity measures
					// Loop over the images to quantify the mask region in all the original images
					for(DimensionMap mapImage : imageData.dimTable.getMapIterator())
					{
						if(!firstOrder && !haralick2D && !histogram && !moments)
						{
							continue;
						}

						Img<UnsignedShortType> image = imgOpener.openImgs(imageMap.get(mapImage), new UnsignedShortType()).get(0); // use get(0) because JEX saves all images individually

						for(IdPoint p : maxima.pointList)
						{
							if(this.isCanceled())
							{
								String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
								outputTable = FileWriter.makeFileObject("temp", null, tablePath);
								return true;
							}
							
							Integer labelId = idToLabelMap.get(p.id);
							if(labelId == null)
							{
								continue;
							}
							reg = measureRegions.getLabelRegion(labelId);
							if(reg == null)
							{
								continue;
							}

							if(!this.putFirstOrder(outputStatMap, mapMeasure, p.id, reg, image))
							{
								return true;
							}

							if(!this.putHaralick2D(outputStatMap, mapMeasure, p.id, reg, image))
							{
								return true;
							}

							if(!this.putHistogram(outputStatMap, mapMeasure, p.id, reg, image))
							{
								return true;
							}

							if(!this.putMoments(outputStatMap, mapMeasure, p.id, reg, image))
							{
								return true;
							}
						}
						
						// Update the user interface with progress
						count = count + 1;
						percentage = (int) (100 * ((double) (count) / ((double) total)));
						JEXStatics.statusBar.setProgressPercentage(percentage);
					}
				}
			}

			String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
			outputTable = FileWriter.makeFileObject("temp", null, tablePath);

			// Return status
			return true;
		}
		catch (ImgIOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	public boolean putZernike(TreeMap<DimensionMap,Double> outputStatMap, DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<UnsignedByteType>  mask)
	{
		if(this.isCanceled())
		{
			String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
			outputTable = FileWriter.makeFileObject("temp", null, tablePath);
			return false;
		}
		if(zernike)
		{
			if(opZernike == null)
			{
				opZernike = IJ2PluginUtility.ij().op().op(ZernikeFeatureSet.class, (IterableInterval<UnsignedByteType>) mask, zernikeMagnitude, zernikePhase, zernikeMomentMin, zernikeMomentMax);
			}
			List<Pair<String,DoubleType>> results = opZernike.getFeatureList(Regions.sample(reg, mask));
			for(Pair<String, DoubleType> result : results)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
				newMap.put("Id", ""+id);
				newMap.put("Label", ""+reg.getLabel());
				outputStatMap.put(newMap, result.getB().get());
			}
		}
		return true;
	}

	public boolean putGeometric(TreeMap<DimensionMap,Double> outputStatMap, DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<UnsignedByteType>  mask)
	{
		if(this.isCanceled())
		{
			String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
			outputTable = FileWriter.makeFileObject("temp", null, tablePath);
			return false;
		}
		if(geometric)
		{
			if(opGeometric == null)
			{
				opGeometric = IJ2PluginUtility.ij().op().op(GeometricFeatureSet.class, LabelRegion.class);
			}
			List<Pair<String,DoubleType>>results = opGeometric.getFeatureList(reg);
			for(Pair<String, DoubleType> result : results)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
				newMap.put("Id", ""+id);
				newMap.put("Label", ""+reg.getLabel());
				outputStatMap.put(newMap, result.getB().get());
			}			
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean putFirstOrder(TreeMap<DimensionMap,Double> outputStatMap, DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<UnsignedShortType>  image)
	{
		if(this.isCanceled())
		{
			String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
			outputTable = FileWriter.makeFileObject("temp", null, tablePath);
			return false;
		}
		if(firstOrder)
		{
			if(opFirstOrder == null)
			{
				opFirstOrder = IJ2PluginUtility.ij().op().op(FirstOrderStatFeatureSet.class, (IterableInterval<UnsignedShortType>) image);
			}
			List<Pair<String,DoubleType>>results = opFirstOrder.getFeatureList(Regions.sample(reg, image));
			for(Pair<String, DoubleType> result : results)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
				newMap.put("Id", ""+id);
				newMap.put("Label", ""+reg.getLabel());
				outputStatMap.put(newMap, result.getB().get());
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean putHaralick2D(TreeMap<DimensionMap,Double> outputStatMap, DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<UnsignedShortType>  image)
	{
		List<Pair<String,DoubleType>> results;

		if(haralick2D)
		{
			if(opHaralick2DHor == null || opHaralick2DVer == null)
			{
				opHaralick2DHor = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, (IterableInterval<UnsignedShortType>) image, haralickGrayLevels, haralickDistance, "HORIZONTAL");
				opHaralick2DVer = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, (IterableInterval<UnsignedShortType>) image, haralickGrayLevels, haralickDistance, "VERTICAL");
				if(haralickNumDirections.equals("4"))
				{
					if(opHaralick2DDiag == null || opHaralick2DAntiDiag == null)
					{
						opHaralick2DDiag = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, (IterableInterval<UnsignedShortType>) image, haralickGrayLevels, haralickDistance, "DIAGONAL");
						opHaralick2DAntiDiag = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, (IterableInterval<UnsignedShortType>) image, haralickGrayLevels, haralickDistance, "ANTIDIAGONAL");
					}
				}							
			}

			results = opHaralick2DHor.getFeatureList(Regions.sample(reg, image));

			///// Horizontal /////
			if(this.isCanceled())
			{
				String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
				outputTable = FileWriter.makeFileObject("temp", null, tablePath);
				return false;
			}
			for(Pair<String, DoubleType> result : results)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA() + "_Horizontal");
				newMap.put("Id", ""+id);
				newMap.put("Label", ""+reg.getLabel());
				outputStatMap.put(newMap, result.getB().get());
			}

			///// Vertical /////
			if(this.isCanceled())
			{
				String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
				outputTable = FileWriter.makeFileObject("temp", null, tablePath);
				return false;
			}
			results = opHaralick2DVer.getFeatureList(Regions.sample(reg, image));
			for(Pair<String, DoubleType> result : results)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA() + "_Vertical");
				newMap.put("Id", ""+id);
				newMap.put("Label", ""+reg.getLabel());
				outputStatMap.put(newMap, result.getB().get());
			}

			if(haralickNumDirections.equals("4"))
			{
				///// Diagonal /////
				if(this.isCanceled())
				{
					String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
					outputTable = FileWriter.makeFileObject("temp", null, tablePath);
					return false;
				}
				results = opHaralick2DDiag.getFeatureList(Regions.sample(reg, image));
				for(Pair<String, DoubleType> result : results)
				{
					DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA() + "_Diagonal");
					newMap.put("Id", ""+id);
					newMap.put("Label", ""+reg.getLabel());
					outputStatMap.put(newMap, result.getB().get());
				}

				///// Antidiagonal /////
				if(this.isCanceled())
				{
					String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
					outputTable = FileWriter.makeFileObject("temp", null, tablePath);
					return false;
				}
				results = opHaralick2DAntiDiag.getFeatureList(Regions.sample(reg, image));
				for(Pair<String, DoubleType> result : results)
				{
					DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA() + "_AntiDiagonal");
					newMap.put("Id", ""+id);
					newMap.put("Label", ""+reg.getLabel());
					outputStatMap.put(newMap, result.getB().get());
				}
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean putHistogram(TreeMap<DimensionMap,Double> outputStatMap, DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<UnsignedShortType>  image)
	{
		if(this.isCanceled())
		{
			String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
			outputTable = FileWriter.makeFileObject("temp", null, tablePath);
			return false;
		}
		if(histogram)
		{
			if(opHistogram == null)
			{
				opHistogram = IJ2PluginUtility.ij().op().op(HistogramFeatureSet.class, (IterableInterval<UnsignedShortType>) image, histogramBins);
			}
			List<Pair<String,LongType>> ret = opHistogram.getFeatureList(Regions.sample(reg, image));
			for(Pair<String, LongType> result : ret)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
				newMap.put("Id", ""+id);
				newMap.put("Label", ""+reg.getLabel());
				outputStatMap.put(newMap, (double) result.getB().get());
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean putMoments(TreeMap<DimensionMap,Double> outputStatMap, DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<UnsignedShortType>  image)
	{
		if(this.isCanceled())
		{
			String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
			outputTable = FileWriter.makeFileObject("temp", null, tablePath);
			return false;
		}
		if(moments)
		{
			if(opMoments == null)
			{
				opMoments = IJ2PluginUtility.ij().op().op(ImageMomentsFeatureSet.class, (IterableInterval<UnsignedShortType>) image);
			}
			List<Pair<String,DoubleType>>results = opMoments.getFeatureList(Regions.sample(reg, image));
			for(Pair<String, DoubleType> result : results)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
				newMap.put("Id", ""+id);
				newMap.put("Label", ""+reg.getLabel());
				outputStatMap.put(newMap, result.getB().get());
			}
		}	
		return true;
	}

	public static boolean contains(LabelRegion<?> region, Point p)
	{
		LabelRegionCursor c = region.localizingCursor();
		do
		{
			if(c.getIntPosition(0) == p.x && c.getIntPosition(1) == p.y)
			{
				return true;
			}
			c.next();
		} 
		while(c.hasNext());

		return false;
	}

	public TreeMap<String,Object> getPixelValues(Wand wand, IdPoint p, ByteProcessor impMask, FloatProcessor impImage1, FloatProcessor impImage2)
	{
		Vector<Double> m1 = null;
		PointList pts = null;
		if(impMask.getPixel(p.x, p.y) == 255) // if we land on a cell that made it through thresholding
		{
			wand.autoOutline(p.x, p.y); // outline it
			if(wand.npoints > 0)
			{
				Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON); // The roi helps for using getLength() (DON'T USE Roi.TRACED_ROI., IT SCREWS UP THE Polygon OBJECTS!!!! Bug emailed to ImageJ folks)
				java.awt.Polygon poly = new java.awt.Polygon(wand.xpoints, wand.ypoints, wand.npoints); // The polygon helps for using contains()
				Rectangle r = roi.getBounds();
				m1 = new Vector<Double>();
				pts = new PointList();
				for (int i = r.x; i < r.x + r.width; i++)
				{
					for (int j = r.y; j < r.y + r.height; j++)
					{
						// innerBoundary
						if(poly.contains(i, j) && impMask.getPixelValue(i, j) == 255)
						{
							m1.add((double) impImage1.getPixelValue(i, j));
							pts.add(i, j);
							// Logs.log("In - " + innerT, this);
						}
					}
				}
			}
		}
		TreeMap<String,Object> ret = new TreeMap<String,Object>();
		ret.put("m1", m1);
		ret.put("xy", pts);
		return ret;
	}
}
