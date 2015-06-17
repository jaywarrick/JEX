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
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

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

	// Define a constructor that takes no arguments.
	public TestFeatureExtraction()
	{}

	/////////// Define Inputs here ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Intensity images", optional=false)
	JEXData imageData;

	@InputMarker(uiOrder=1, name="Mask", type=MarkerConstants.TYPE_IMAGE, description="Mask images", optional=false)
	JEXData maskData;

	@InputMarker(uiOrder=1, name="Maxima", type=MarkerConstants.TYPE_ROI, description="Maxima ROI", optional=false)
	JEXData roiData;

	/////////// Define Parameters here ///////////

	@ParameterMarker(uiOrder=1, name="First Order Stats?", description="Whether to quantify first order statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean firstOrder;

	@ParameterMarker(uiOrder=2, name="Geometric Stats?", description="Whether to quantify geometric statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean geometric;

	@ParameterMarker(uiOrder=3, name="Haralick 2D Stats?", description="Whether to quantify Haralick texture statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean haralick2D;

	// Add number of gray levels for Haralick currently 8
	double haralickGrayLevels = 8;
	// Add distance at which to compute the co-occurrence matrix
	double haralickDistance = 1;
	// Add number of directions choices = {	"DIAGONAL", "ANTIDIAGONAL", "HORIZONTAL", "VERTICAL" })
	String haralickNumDirections = "2";

	@ParameterMarker(uiOrder=4, name="Histogram Stats?", description="Whether to quantify histogram statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean histogram;

	// Add number of histogram bins
	int histogramBins = 256;

	@ParameterMarker(uiOrder=5, name="Moments Stats?", description="Whether to quantify image moment statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean moments;

	@ParameterMarker(uiOrder=6, name="Zernike Stats?", description="Whether to quantify Zernike shape statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean zernike;

	// Add compute magnitude default true
	boolean zernikeMagnitude = true;
	// Add compute phase default true
	boolean zernikePhase = true;
	// Add min moment
	int zernikeMomentMin = 1;
	// Add max moment
	int zernikeMomentMax = 3;

	@ParameterMarker(uiOrder=7, name="Connectedness", description="The structuring element or number of neighbors to require to be part of the neighborhood.", ui=MarkerConstants.UI_DROPDOWN, choices={"4 Connected", "8 Connected"}, defaultChoice=0)
	String connectedness;

	// Add a primary mask and secondary mask

	/////////// Define Outputs here ///////////

	// See Database.Definition.OutputMarker for types of inputs that are supported (File, Image, Value, ROI...)
	@OutputMarker(uiOrder=1, name="Label Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Image with intensities that match the cell label number", enabled=true)
	JEXData outputImage;

	@OutputMarker(uiOrder=3, name="Output Table", type=MarkerConstants.TYPE_FILE, flavor="", description="Test table output.", enabled=true)
	JEXData outputTable;

	// Define threading capability here (set to 1 if using non-final static variables shared between function instances).
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	// Code the actions of the plugin here using comments for significant sections of code to enhance readability as shown here
	@SuppressWarnings("unchecked")
	@Override
	public boolean run(JEXEntry optionalEntry)
	{

		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> maskMap = ImageReader.readObjectToImagePathTable(maskData);
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,Double> outputStatMap = new TreeMap<DimensionMap,Double>();
		TreeMap<Integer,Integer> idToLabelMap = new TreeMap<Integer,Integer>();
		LabelRegion<Integer> reg;
		List<Pair<String, DoubleType>> results;
		int count = 0, percentage = 0;

		ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij.getContext());

		FirstOrderStatFeatureSet<IterableInterval<UnsignedShortType>> opFirstOrder = null;
		GeometricFeatureSet opGeometric = null;
		Haralick2DFeatureSet<UnsignedShortType> opHaralick2DDiag = null;
		Haralick2DFeatureSet<UnsignedShortType> opHaralick2DAntiDiag = null;
		Haralick2DFeatureSet<UnsignedShortType> opHaralick2DHor = null;
		Haralick2DFeatureSet<UnsignedShortType> opHaralick2DVer = null;
		HistogramFeatureSet<UnsignedShortType> opHistogram = null;
		ImageMomentsFeatureSet<IterableInterval<UnsignedShortType>> opMoments = null;
		ZernikeFeatureSet<UnsignedByteType> opZernike = null;
		boolean firstMask = true;
		boolean firstImage = true;

		if(!firstOrder && !geometric && !haralick2D && !histogram && !moments && !zernike)
		{
			return false;
		}
		try
		{						
			// Loop over masks to then apply them to images
			for (DimensionMap mapM : maskMap.keySet())
			{
				if(this.isCanceled())
				{
					String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
					outputTable = FileWriter.makeFileObject("temp", null, tablePath);
					return true;
				}
				List<SCIFIOImgPlus<UnsignedByteType>> masks = imgOpener.openImgs(maskMap.get(mapM), new UnsignedByteType());

				for(Img<UnsignedByteType> mask : masks)
				{
					if(this.isCanceled())
					{
						String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
						outputTable = FileWriter.makeFileObject("temp", null, tablePath);
						return true;
					}

					if(firstMask)
					{
						if(zernike)
						{
							opZernike = IJ2PluginUtility.ij.op().op(ZernikeFeatureSet.class, (IterableInterval<UnsignedByteType>) mask, zernikeMagnitude, zernikePhase, zernikeMomentMin, zernikeMomentMax);
						}
						if(geometric)
						{
							opGeometric = IJ2PluginUtility.ij.op().op(GeometricFeatureSet.class, LabelRegion.class);
						}
						firstMask = false;
					}

					// Get the full labeling for the mask
					ImgLabeling<Integer, IntType> labeling = FeatureUtils.getConnectedComponents(mask, true);
					LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);

					// Determine which labelings are the ones we want to keep by testing if our maxima of interest are contained.
					ROIPlus maxima = roiMap.get(mapM);
					for(LabelRegion<Integer> region : regions)
					{
						for(IdPoint p : maxima.pointList)
						{
							if(this.isCanceled())
							{
								String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
								outputTable = FileWriter.makeFileObject("temp", null, tablePath);
								return true;
							}
							if(contains(region, p))
							{
								idToLabelMap.put(p.id, region.getLabel());
							}
						}
					}

					for(IdPoint p : maxima.pointList)
					{
						if(this.isCanceled())
						{
							String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
							outputTable = FileWriter.makeFileObject("temp", null, tablePath);
							return true;
						}

						reg = regions.getLabelRegion(idToLabelMap.get(p.id));
						if(reg == null)
						{
							continue;
						}

						if(geometric)
						{
							results = opGeometric.getFeatureList(Regions.iterable(reg));
							for(Pair<String, DoubleType> result : results)
							{
								DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
								newMap.put("Id", ""+p.id);
								newMap.put("Label", ""+reg.getLabel());
								outputStatMap.put(newMap, result.getB().get());
							}
							//							DimensionMap newMap = mapM.copyAndSet("Measurement=Area");
							//							newMap.put("Label",""+reg.getLabel());
							//							outputStatMap.put(newMap.copy(), (double)reg.size());
							//							newMap.put("Measurement", "CenterOfMass.X");
							//							outputStatMap.put(newMap.copy(), reg.getCenterOfMass().getDoublePosition(0));
							//							newMap.put("Measurement", "CenterOfMass.Y");
							//							outputStatMap.put(newMap, reg.getCenterOfMass().getDoublePosition(1));

						}
						if(zernike)
						{
							results = opZernike.getFeatureList(Regions.sample(reg, mask));
							for(Pair<String, DoubleType> result : results)
							{
								DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
								newMap.put("Id", ""+p.id);
								newMap.put("Label", ""+reg.getLabel());
								outputStatMap.put(newMap, result.getB().get());
							}
						}
					}

					// Loop over the images to quantify the mask region in all the original images
					for(DimensionMap mapI : imageData.dimTable.getMapIterator())
					{
						if(this.isCanceled())
						{
							String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
							outputTable = FileWriter.makeFileObject("temp", null, tablePath);
							return true;
						}

						if(!firstOrder && !haralick2D && !histogram && !moments)
						{
							continue;
						}

						Img<UnsignedShortType> image = imgOpener.openImgs(imageMap.get(mapI), new UnsignedShortType()).get(0); // Because JEX saves all images individually

						if(firstImage)
						{
							if(firstOrder)
							{
								opFirstOrder = IJ2PluginUtility.ij.op().op(FirstOrderStatFeatureSet.class, (IterableInterval<UnsignedShortType>) image);
							}
							if(haralick2D)
							{
								opHaralick2DHor = IJ2PluginUtility.ij.op().op(Haralick2DFeatureSet.class, (IterableInterval<UnsignedShortType>) image, haralickGrayLevels, haralickDistance, "HORIZONTAL");
								opHaralick2DVer = IJ2PluginUtility.ij.op().op(Haralick2DFeatureSet.class, (IterableInterval<UnsignedShortType>) image, haralickGrayLevels, haralickDistance, "VERTICAL");
								if(haralickNumDirections.equals("4"))
								{
									Logs.log("Running Haralick features in four directions...", this);
									opHaralick2DDiag = IJ2PluginUtility.ij.op().op(Haralick2DFeatureSet.class, (IterableInterval<UnsignedShortType>) image, haralickGrayLevels, haralickDistance, "DIAGONAL");
									opHaralick2DAntiDiag = IJ2PluginUtility.ij.op().op(Haralick2DFeatureSet.class, (IterableInterval<UnsignedShortType>) image, haralickGrayLevels, haralickDistance, "ANTIDIAGONAL");
								}							
							}
							if(histogram)
							{
								opHistogram = IJ2PluginUtility.ij.op().op(HistogramFeatureSet.class, (IterableInterval<UnsignedShortType>) image, histogramBins);
							}
							if(moments)
							{
								opMoments = IJ2PluginUtility.ij.op().op(ImageMomentsFeatureSet.class, (IterableInterval<UnsignedShortType>) image);
							}
						}

						for(IdPoint p : maxima.pointList)
						{
							if(this.isCanceled())
							{
								String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
								outputTable = FileWriter.makeFileObject("temp", null, tablePath);
								return true;
							}

							reg = regions.getLabelRegion(idToLabelMap.get(p.id));
							if(reg == null)
							{
								continue;
							}

							if(firstOrder)
							{
								results = opFirstOrder.getFeatureList(Regions.sample(reg, image));
								for(Pair<String, DoubleType> result : results)
								{
									DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
									newMap.put("Id", ""+p.id);
									newMap.put("Label", ""+reg.getLabel());
									outputStatMap.put(newMap, result.getB().get());
								}
							}
							if(haralick2D)
							{
								results = opHaralick2DHor.getFeatureList(Regions.sample(reg, image));
								for(Pair<String, DoubleType> result : results)
								{
									DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA() + "_Horizontal");
									newMap.put("Id", ""+p.id);
									newMap.put("Label", ""+reg.getLabel());
									outputStatMap.put(newMap, result.getB().get());
								}
								if(this.isCanceled())
								{
									String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
									outputTable = FileWriter.makeFileObject("temp", null, tablePath);
									return true;
								}
								results = opHaralick2DVer.getFeatureList(Regions.sample(reg, image));
								for(Pair<String, DoubleType> result : results)
								{
									DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA() + "_Vertical");
									newMap.put("Id", ""+p.id);
									newMap.put("Label", ""+reg.getLabel());
									outputStatMap.put(newMap, result.getB().get());
								}
								if(haralickNumDirections.equals("4"))
								{
									if(this.isCanceled())
									{
										String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
										outputTable = FileWriter.makeFileObject("temp", null, tablePath);
										return true;
									}
									results = opHaralick2DDiag.getFeatureList(Regions.sample(reg, image));
									for(Pair<String, DoubleType> result : results)
									{
										DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA() + "_Diagonal");
										newMap.put("Id", ""+p.id);
										newMap.put("Label", ""+reg.getLabel());
										outputStatMap.put(newMap, result.getB().get());
									}
									if(this.isCanceled())
									{
										String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
										outputTable = FileWriter.makeFileObject("temp", null, tablePath);
										return true;
									}
									results = opHaralick2DAntiDiag.getFeatureList(Regions.sample(reg, image));
									for(Pair<String, DoubleType> result : results)
									{
										DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA() + "_AntiDiagonal");
										newMap.put("Id", ""+p.id);
										newMap.put("Label", ""+reg.getLabel());
										outputStatMap.put(newMap, result.getB().get());
									}
								}
							}
							if(histogram)
							{
								if(this.isCanceled())
								{
									String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
									outputTable = FileWriter.makeFileObject("temp", null, tablePath);
									return true;
								}
								List<Pair<String,LongType>> ret = opHistogram.getFeatureList(Regions.sample(reg, image));
								for(Pair<String, LongType> result : ret)
								{
									DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
									newMap.put("Id", ""+p.id);
									newMap.put("Label", ""+reg.getLabel());
									outputStatMap.put(newMap, (double) result.getB().get());
								}
							}
							if(moments)
							{
								if(this.isCanceled())
								{
									String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
									outputTable = FileWriter.makeFileObject("temp", null, tablePath);
									return true;
								}
								results = opMoments.getFeatureList(Regions.sample(reg, image));
								for(Pair<String, DoubleType> result : results)
								{
									DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
									newMap.put("Id", ""+p.id);
									newMap.put("Label", ""+reg.getLabel());
									outputStatMap.put(newMap, result.getB().get());
								}
							}	
						}				
					}
				}
			}

			String tablePath = JEXTableWriter.writeTable("FeatureTable", outputStatMap);
			outputTable = FileWriter.makeFileObject("temp", null, tablePath);

			// Update the user interface with progress

			// Return status
			return true;
		}
		catch (ImgIOException e)
		{
			e.printStackTrace();
			return false;
		}
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
				Polygon poly = new Polygon(wand.xpoints, wand.ypoints, wand.npoints); // The polygon helps for using contains()
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
