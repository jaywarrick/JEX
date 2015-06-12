// Define package name as "plugins" as show here
package function.plugin.plugins.singleCell;

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

import net.imagej.ops.features.sets.FirstOrderStatFeatureSet;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
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
import function.plugin.plugins.featureExtraction.FeatureUtils;

// Specify plugin characteristics here
@Plugin(
		type = JEXPlugin.class,
		name="Test Feature Extraction",
		menuPath="Test Functions",
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

	@ParameterMarker(uiOrder=3, name="Haralick Stats?", description="Whether to quantify Haralick texture statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean haralick;

	@ParameterMarker(uiOrder=4, name="Histogram Stats?", description="Whether to quantify histogram statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean histogram;

	@ParameterMarker(uiOrder=5, name="Moments Stats?", description="Whether to quantify image moment statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean moments;

	@ParameterMarker(uiOrder=6, name="Zernike Stats?", description="Whether to quantify Zernike shape statistics", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean zernike;

	@ParameterMarker(uiOrder=7, name="Connectedness", description="The structuring element or number of neighbors to require to be part of the neighborhood.", ui=MarkerConstants.UI_DROPDOWN, choices={"4 Connected", "8 Connected"}, defaultChoice=0)
	String connectedness;

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
	@Override
	public boolean run(JEXEntry optionalEntry)
	{

		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> maskMap = ImageReader.readObjectToImagePathTable(maskData);
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,Double> outputStatMap = new TreeMap<DimensionMap,Double>();
		TreeMap<Integer,Integer> idToLabelMap = new TreeMap<Integer,Integer>();
		int count = 0, percentage = 0;

		ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij.getContext());

		try
		{						
			// Loop through the items in the n-Dimensional object
			for (DimensionMap mapM : maskMap.keySet())
			{
				List<SCIFIOImgPlus<UnsignedByteType>> masks = imgOpener.openImgs(maskMap.get(mapM), new UnsignedByteType());

				for(Img<UnsignedByteType> mask : masks)
				{
					//ImageJFunctions.show(mask);

					// Get the full labeling for the mask
					ImgLabeling<Integer, UnsignedShortType> labeling = FeatureUtils.getConnectedComponents(mask, true);
					LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);

					// Determine which labelings are the ones we want to keep by testing if our maxima of interest are contained.
					ROIPlus maxima = roiMap.get(mapM);
					for(LabelRegion<Integer> region : regions)
					{
						for(IdPoint p : maxima.pointList)
						{
							if(contains(region, p))
							{
								idToLabelMap.put(p.id, region.getLabel());
							}
						}

					}

					// Now we have the regions we want to quantify


					// For now just try to get the first order stats of the MASK image
					if(true)
					{
						@SuppressWarnings("unchecked")
						FirstOrderStatFeatureSet<IterableInterval<UnsignedByteType>> op = IJ2PluginUtility.ij.op().op(FirstOrderStatFeatureSet.class, (IterableInterval<UnsignedByteType>) mask);

						for(IdPoint p : maxima.pointList)
						{
							LabelRegion<Integer> region = regions.getLabelRegion(idToLabelMap.get(p.id));
							List<Pair<String, DoubleType>> results = op.getFeatureList(Regions.sample(region, mask));
							for(Pair<String, DoubleType> result : results)
							{
								DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
								newMap.put("Id", ""+p.id);
								outputStatMap.put(newMap.copy(), result.getB().get());
								newMap.put("Measurement", "Area");
								outputStatMap.put(newMap, (double) 	region.size());
							}
						}


					}

				}

			}

			String tablePath = JEXTableWriter.writeTable("FirstOrderStats", outputStatMap);
			outputTable = FileWriter.makeFileObject("temp", null, tablePath);

			//					// For each mask map, get corresponding image maps for sampling.
			//					for(DimensionMap mapI : imageData.dimTable.getDimensionMaps(mapM))
			//					{
			//						
			//						ImgLabeling<Integer, UnsignedShortType> labeling = FeatureUtils.getConnectedComponents(im, true);
			//						LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);
			//						double[] pos = new double[images.get(0).numDimensions()];
			//						
			//						for(LabelRegion<Integer> region : regions)
			//						{
			//							int id = region.getLabel();
			//							region.getCenterOfMass().localize(pos);
			//							
			//							Logs.log("" + id + " at " + pos[0] + "," + pos[1] + " - Size: " + region.size(), PointTester.class);
			//							Logs.log("Contains? " + contains(region, new Point(101,180)), PointTester.class);
			//							
			//						}
			//						Logs.log("Seems to have worked", PointTester.class);
			//						
			//						ImageJFunctions.show(im);
			//						FirstOrderStatFeatureSet<Img<UnsignedShortType>> op = IJ2PluginUtility.ij.op().op(FirstOrderStatFeatureSet.class, im);
			//						for (final Entry<? extends OpRef,DoubleType> result : op.compute(im).entrySet())
			//						{
			//							System.out.println(result.getKey().getType().getSimpleName() + " " + result.getValue().get());
			//						}
			//						Logs.log("\n\n", PointTester.class);
			//						
			//						List<SCIFIOImgPlus<UnsignedShortType>> images = imgOpener.openImgs(imageMap.get(map), new UnsignedShortType());
			//						
			//						for(Img<UnsignedShortType> im : images)
			//						{
			//							
			//						}
			//					}
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
