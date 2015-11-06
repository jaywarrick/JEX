// Define package name as "plugins" as show here
package function.plugin.plugins.featureExtraction;

// Import needed classes here 
import image.roi.IdPoint;
import image.roi.ROIPlus;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;

import java.awt.Point;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.JEXCSVReader;
import miscellaneous.JEXCSVWriter;
import net.imagej.ops.features.sets.Geometric2DFeatureSet;
import net.imagej.ops.features.sets.Haralick2DFeatureSet;
import net.imagej.ops.features.sets.HistogramFeatureSet;
import net.imagej.ops.features.sets.ImageMomentsFeatureSet;
import net.imagej.ops.features.sets.StatsFeatureSet;
import net.imagej.ops.features.sets.ZernikeFeatureSet;
import net.imagej.ops.featuresets.NamedFeature;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.Positionable;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPositionable;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.Img;
import net.imglib2.roi.IterableRegion;
import net.imglib2.roi.PositionableIterableRegion;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.util.SamplingIterableInterval;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.view.IterableRandomAccessibleInterval;

import org.scijava.plugin.Plugin;

import tables.DimTable;
import tables.DimTableBuilder;
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
		name="Feature Extraction",
		menuPath="Feature Extraction",
		visible=true,
		description="Function for testing feature extraction using the ImageJ Ops framework."
		)
public class FeatureExtraction<T extends RealType<T>> extends JEXPlugin {

	public ImgOpener imgOpener;

	public Geometric2DFeatureSet<Polygon, DoubleType> opGeometric = null;
	public Haralick2DFeatureSet<T,DoubleType> opHaralick2DHor = null;
	public Haralick2DFeatureSet<T,DoubleType> opHaralick2DVer = null;
	public Haralick2DFeatureSet<T,DoubleType> opHaralick2DDiag = null;
	public Haralick2DFeatureSet<T,DoubleType> opHaralick2DAntiDiag = null;
	public HistogramFeatureSet<T> opHistogram = null;
	public ImageMomentsFeatureSet<T,DoubleType> opMoments = null;
	// What about IntensityFeatureSet???
	public StatsFeatureSet<T,DoubleType> opStats = null;
	// TODO: Figure out how to get a RandomAccessbleInterval<T> from a LabelRegion and an Img<T>
	//	public Tamura2DFeatureSet<T,DoubleType> opTamura = null;
	public ZernikeFeatureSet<BitType> opZernike = null;

	public JEXCSVWriter writer;
	public Set<String> header = null;

	// Define a constructor that takes no arguments.
	public FeatureExtraction()
	{}

	// ///////// Define Inputs here ///////////

	@InputMarker(uiOrder = 1, name = "Image", type = MarkerConstants.TYPE_IMAGE, description = "Intensity images", optional = false)
	JEXData imageData;

	@InputMarker(uiOrder = 2, name = "Whole Cell Mask", type = MarkerConstants.TYPE_IMAGE, description = "Mask images defining the whole cell (should NOT have channel dimension)", optional = false)
	JEXData cellMaskData;

	@InputMarker(uiOrder = 3, name = "Masks to Measure", type = MarkerConstants.TYPE_IMAGE, description = "Mask images (SHOULD have channel dimension)", optional = false)
	JEXData measureMaskData;

	@InputMarker(uiOrder = 4, name = "Maxima", type = MarkerConstants.TYPE_ROI, description = "Maxima ROI", optional = false)
	JEXData roiData;

	// ///////// Define Parameters here ///////////

	@ParameterMarker(uiOrder = 0, name = "Image intensity offset", description = "Amount the images are offset from zero (will be subtracted before calculation)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "0.0")
	double offset;

	@ParameterMarker(uiOrder = 1, name = "** Compute Stats Features?", description = "Whether to quantify first order statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean stats;

	@ParameterMarker(uiOrder = 2, name = "** Compute 2D Geometric Features?", description = "Whether to quantify geometric statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean geometric;

	@ParameterMarker(uiOrder = 3, name = "** Compute 2D Haralick Features?", description = "Whether to quantify Haralick texture statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean haralick2D;

	@ParameterMarker(uiOrder = 4, name = "** Compute Histogram Features?", description = "Whether to quantify histogram statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean histogram;

	@ParameterMarker(uiOrder = 5, name = "** Compute Moments Features?", description = "Whether to quantify image moment statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean moments;

	//	@ParameterMarker(uiOrder = 5, name = "** Compute 2D Tamura Features?", description = "Whether to quantify Tamura statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	//	boolean tamura;

	@ParameterMarker(uiOrder = 6, name = "** Compute Zernike Features?", description = "Whether to quantify Zernike shape statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean zernike;

	@ParameterMarker(uiOrder = 7, name = "** Connectedness Features", description = "The structuring element or number of neighbors to require to be part of the neighborhood.", ui = MarkerConstants.UI_DROPDOWN, choices = { "4 Connected", "8 Connected" }, defaultChoice = 0)
	String connectedness;

	@ParameterMarker(uiOrder = 8, name = "Haralick Gray Levels", description = "Number of gray levels for Haralick calculations", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "8")
	int haralickGrayLevels;

	@ParameterMarker(uiOrder = 9, name = "Haralick Co-Occurrence Matrix Distance", description = "Distance at which to compute the co-occurrence matrix", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "1")
	double haralickDistance;

	@ParameterMarker(uiOrder = 10, name = "Haralick Number of Directions", description = "(Orthogonals and Diagonals etc) 2 performs horizontal and vertical. 4 adds the 2 diagonals as well.", ui = MarkerConstants.UI_DROPDOWN, choices = { "2", "4" }, defaultChoice = 1)
	String haralickNumDirections;

	@ParameterMarker(uiOrder = 11, name = "Histogram Number of Bins", description = "Number of bins for the histogram created for each cell region", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "256")
	int histogramBins;

	@ParameterMarker(uiOrder = 11, name = "Tamura 2D Number of Bins", description = "Number of bins for the histogram created for each cell region for the directionality feature", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "256")
	int tamuraBins;

	@ParameterMarker(uiOrder = 14, name = "Zernike Min Moment", description = "Min Zernike moment calculate", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "1")
	int zernikeMomentMin;

	@ParameterMarker(uiOrder = 15, name = "Zernike Max Moment", description = "Max Zernike moment calculate", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "3")
	int zernikeMomentMax;

	// Add a primary mask and secondary mask

	/////////// Define Outputs here ///////////

	@OutputMarker(uiOrder = 1, name = "Output CSV Table", type = MarkerConstants.TYPE_FILE, flavor = "", description = "Output in csv format (i.e., for Excel etc).", enabled = true)
	JEXData outputCSV;

	@OutputMarker(uiOrder = 1, name = "Output ARFF Table", type = MarkerConstants.TYPE_FILE, flavor = "", description = "Test table output (i.e., for Weka etc).", enabled = true)
	JEXData outputARFF;

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
		this.imgOpener = new ImgOpener(IJ2PluginUtility.ij().getContext());

		LabelRegion<Integer> reg;
		if(!stats && !geometric && !haralick2D && !histogram && !moments && !zernike)
		{
			Logs.log("Nothing selected to compute. Returning false.", this);
			return false;
		}
		if(cellMaskData == null)
		{
			Logs.log("Returning false. NEED to have a whole cell mask.", this);
			return false;
		}
		if(measureMaskData == null)
		{
			measureMaskData = cellMaskData;
		}
		if(imageData == null && (stats || haralick2D || histogram || moments))
		{
			Logs.log("Returning false. NEED to define an image to quantify if you want to use intensity based features such as first order, haralick2D, histogram, and moment statistics.", this);
			return false;
		}

		TreeMap<DimensionMap, String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap, String> cellMaskMap = ImageReader.readObjectToImagePathTable(cellMaskData);
		TreeMap<DimensionMap, String> measureMaskMap = cellMaskMap;
		TreeMap<DimensionMap, ROIPlus> roiMap = RoiReader.readObjectToRoiMap(roiData);

		int count = 0, percentage = 0;

		// Calculate expected number of iterations
		DimTable maskIntersection = DimTable.intersect(measureMaskData.getDimTable(), cellMaskData.getDimTable());
		DimTable maskAdditional = cellMaskData.getDimTable().getSubTable(maskIntersection.getMapIterator().iterator().next());
		int total = maskIntersection.mapCount() * maskAdditional.mapCount();
		if(imageData != null && (stats || haralick2D || histogram || moments))
		{
			DimTable imageIntersection = DimTable.intersect(imageData.getDimTable(), cellMaskData.getDimTable());
			DimTable imageAdditional = imageData.getDimTable().getSubTable(imageIntersection.getMapIterator().iterator().next());
			total = total + imageIntersection.mapCount() * imageAdditional.mapCount();
		}

		try
		{
			// Loop over whole cell masks
			for(DimensionMap mapCell : cellMaskData.getDimTable().getMapIterator())
			{
				if((geometric || zernike) || (imageData != null && imageData.getDimTable().hasDimensionMap(mapCell) && (stats || haralick2D || histogram || moments)))
				{
					Logs.log("Utilizing cell mask: " + mapCell, this);
					if(this.isCanceled())
					{
						this.close();
						return true;
					}

					SCIFIOImgPlus<UnsignedByteType> cellMask = imgOpener.openImgs(cellMaskMap.get(mapCell), new UnsignedByteType()).get(0);
					ImgLabeling<Integer, IntType> cellLabeling = FeatureUtils.getConnectedComponents(cellMask, connectedness.equals("4 Connected"));
					LabelRegions<Integer> cellRegions = new LabelRegions<Integer>(cellLabeling);
					TreeMap<Integer, Integer> idToLabelMap = new TreeMap<Integer, Integer>();

					// Determine which LabelRegions are the ones we want to keep by testing if our maxima of interest are contained.
					ROIPlus maxima = roiMap.get(mapCell);
					for(LabelRegion<Integer> cellRegion : cellRegions)
					{
						for(IdPoint p : maxima.pointList)
						{
							if(this.isCanceled())
							{
								this.close();
								return true;
							}
							if(contains(cellRegion, p))
							{
								idToLabelMap.put(p.id, cellRegion.getLabel().intValue());
							}
						}
					}

					// For each measure mask (with appropriate dimensions per filter), intersect the measure mask with the whole cell regions and perform feature extraction
					if(measureMaskData.getDimTable().hasDimensionMap(mapCell))
					{
						for(DimensionMap mapMeasure : measureMaskData.getDimTable().getMapIterator(mapCell))
						{
							if((geometric || zernike) || (imageData != null && imageData.getDimTable().hasDimensionMap(mapCell) && (stats || haralick2D || histogram || moments)))
							{
								Logs.log("Intersecting cell mask: " + mapCell + " with measurement mask " + mapMeasure, this);
								if(this.isCanceled())
								{
									this.close();
									return true;
								}

								// Intersect measure mask with whole cell regions
								String measureMaskPath = measureMaskMap.get(mapMeasure);
								if(measureMaskPath == null)
								{
									continue;
								}

								Img<UnsignedByteType> measureMask = imgOpener.openImgs(measureMaskPath, new UnsignedByteType()).get(0);
								LabelRegions<Integer> measureRegions = cellRegions;
								if(cellMaskData != measureMaskData)
								{
									ImgLabeling<Integer, IntType> measureLabeling = cellLabeling;
									measureRegions = new LabelRegions<Integer>(measureLabeling);
								}

								// Measure the images first so that the CSV Table gets a FULL header of information given the images
								// might have a dimension that the masks don't
								if(imageData != null && imageData.getDimTable().hasDimensionMap(mapCell) && (stats || haralick2D || histogram || moments))
								{
									// Then do texture and intensity measures
									// Loop over the images to quantify the mask region in all the original images
									for(DimensionMap mapImage : imageData.dimTable.getMapIterator(mapCell))
									{
										Logs.log("Quantifying image: " + mapImage, this);

										String imagePath = imageMap.get(mapImage);
										if(imagePath == null)
										{
											continue;
										}

										@SuppressWarnings("unchecked")
										// T gets defined here; Use get(0) because JEX saves all images individually
										Img<T> image = (Img<T>) imgOpener.openImgs(imagePath).get(0);
										for(IdPoint p : maxima.pointList)
										{
											if(this.isCanceled())
											{
												this.close();
												return true;
											}

											Integer labelId = idToLabelMap.get(p.id);
											if(labelId == null)
											{
												continue;
											}
											reg = measureRegions.getLabelRegion(labelId);
											if(reg == null || reg.size() <= 1 )
											{
												continue;
											}

											if(!this.putStats(mapImage, p.id, reg, measureMask, image))
											{
												return true;
											}

											if(!this.putHaralick2D(mapImage, p.id, reg, image))
											{
												return true;
											}

											if(!this.putHistogram(mapImage, p.id, reg, image))
											{
												return true;
											}

											if(!this.putMoments(mapImage, p.id, reg, image))
											{
												return true;
											}

											//											if(!this.putTamura(mapImage, p.id, reg, image))
											//											{
											//												return true;
											//											}
										}
										// Count the fact we quantified an image
										count = count + 1;
										percentage = (int) (100 * ((double) (count) / ((double) total)));
										JEXStatics.statusBar.setProgressPercentage(percentage);
									}
								}

								if(geometric || zernike)
								{
									Logs.log("Quantifying measurement mask: " + mapMeasure, this);
									// For each cell, perform shape feature measurements first if desired
									for(IdPoint p : maxima.pointList)
									{
										Integer labelId = idToLabelMap.get(p.id);
										if(labelId == null)
										{
											continue;
										}

										reg = measureRegions.getLabelRegion(labelId);
										if(reg == null || reg.size() <=1 )
										{
											continue;
										}

										if(!this.putGeometric(mapMeasure, p.id, reg, measureMask))
										{
											return true;
										}

										if(!this.putZernike(mapMeasure, p.id, reg, measureMask))
										{
											return true;
										}
									}
								}

								// Count the fact that we quantified a mask
								count = count + 1;
								percentage = (int) (100 * ((double) (count) / ((double) total)));
								JEXStatics.statusBar.setProgressPercentage(percentage);													
							}
							else
							{
								// skip and index progress bar
								count = count + 1;
								percentage = (int) (100 * ((double) (count) / ((double) total)));
								JEXStatics.statusBar.setProgressPercentage(percentage);
							}
						}
					}
					else
					{
						Logs.log("Skipping cell mask: " + mapCell + " given measurement mask and or the images do not have images corresponding to this cell mask.", this);
					}
				}
				else
				{
					// skip and index progress bar
					count = count + 1;
					percentage = (int) (100 * ((double) (count) / ((double) total)));
					JEXStatics.statusBar.setProgressPercentage(percentage);
				}
			}

			this.close();

			// Return status
			return true;
		}
		catch (ImgIOException e)
		{
			e.printStackTrace();
			try
			{
				Logs.log("Trying to exit gracefully and close the file writer", this);
				this.close();
			}
			catch(Exception e2)
			{
				e.printStackTrace();
			}

			return false;
		}
	}

	public void write(DimensionMap map, Double value)
	{
		if(this.writer == null)
		{
			this.writer = new JEXCSVWriter();
			this.writer.writeHeader(map);
		}
		DimensionMap temp = map.copy();
		if(header == null)
		{
			header = map.copy().keySet();
		}
		for(String s : header)
		{
			if(!map.containsKey(s))
			{
				temp.put(s, "NA");
			}
		}
		writer.write(temp, value.toString());
	}

	public void close()
	{
		Logs.log("Closing the function (closing file writers and converting output file to arff as well).", this);
		this.writer.close();
		String csvPath = writer.getPath();
		JEXCSVReader reader = new JEXCSVReader(csvPath, true);

		// In order to write an Arff table we need to build a DimTable
		// We can't keep all the data in memory as it might be too large so just build DimTable for now.
		DimTableBuilder builder = new DimTableBuilder();
		while(!reader.isEOF())
		{
			builder.add(reader.readRowToDimensionMapString().p1);
		}
		reader.close();

		// Now that we have the DimTable we can transfer each row of the csv to the arff file after writing the header.
		reader = new JEXCSVReader(csvPath, true);
		JEXTableWriter arffWriter = new JEXTableWriter("FeatureTable", "arff");
		arffWriter.writeNumericTableHeader(builder.getDimTable());
		while(!reader.isEOF())
		{
			miscellaneous.Pair<DimensionMap, String> result = reader.readRowToDimensionMapString();
			arffWriter.writeData(result.p1, Double.parseDouble(result.p2));
		}

		// Close and save the data.
		reader.close();
		String arffPath = arffWriter.getPath();
		arffWriter.close();
		outputCSV = FileWriter.makeFileObject("temp", null, csvPath);
		outputARFF = FileWriter.makeFileObject("temp", null, arffPath);
	}

	@SuppressWarnings("unchecked")
	public boolean putZernike(DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<UnsignedByteType> mask)
	{
		if(this.isCanceled())
		{
			this.close();
			return false;
		}
		if(zernike)
		{
			if(this.opZernike == null)
			{
				opZernike = IJ2PluginUtility.ij().op().op(ZernikeFeatureSet.class, Regions.sample(reg, mask), zernikeMomentMin, zernikeMomentMax);
			}

			PositionableIterableRegion<BoolType> temp = reg;
			IterableRegion<BoolType> temp2 = temp;
			IterableInterval<Void> temp3 = temp2;
			IterableInterval<BitType> convert = Converters.convert(temp3, new MyConverter(), new BitType());

			Map<NamedFeature, DoubleType> results = opZernike.compute(convert);
			for(Entry<NamedFeature, DoubleType> result : results.entrySet())
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + id);
				newMap.put("Label", "" + reg.getLabel());
				this.write(newMap, result.getValue().get());
			}
		}
		return true;
	}

	//	@SuppressWarnings("unchecked")
	//	public boolean putTamura(DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<T> image)
	//	{
	//		if(this.isCanceled())
	//		{
	//			this.close();
	//			return false;
	//		}
	//		if(tamura)
	//		{
	//			if(opTamura == null)
	//			{
	//				opTamura = IJ2PluginUtility.ij().op().op(Tamura2DFeatureSet.class, image);
	//			}
	//			Map<NamedFeature, DoubleType> results = opTamura.compute(Views.Regions.sample(reg, image));
	//			for(Entry<NamedFeature, DoubleType> result : results.entrySet())
	//			{
	//				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
	//				newMap.put("Id", "" + id);
	//				newMap.put("Label", "" + reg.getLabel());
	//				this.write(newMap, result.getValue().get());
	//			}
	//		}
	//		return true;
	//	}

	@SuppressWarnings("unchecked")
	public boolean putGeometric(DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<UnsignedByteType> mask)
	{
		if(this.isCanceled())
		{
			this.close();
			return false;
		}
		if(geometric)
		{
			if(opGeometric == null)
			{
				opGeometric = IJ2PluginUtility.ij().op().op(Geometric2DFeatureSet.class, reg);
			}
			LabelRegions<Integer> regions = getSubRegions(reg, mask);
			
			Integer maxSizeLabel = null;
			int subRegionCount = 0;
			Polygon p = FeatureUtils.convert(reg);
			for(Integer label : regions.getExistingLabels())
			{
				LabelRegion<Integer> r = regions.getLabelRegion(label);
				// Check if all the sub regions of the parent region (because CCA is done in the bounding box, not the region)
				if(p.contains(r.cursor()))
				{
					if(maxSizeLabel == null || r.size() > maxSizeLabel)
					{
						maxSizeLabel = label;
					}
					subRegionCount = subRegionCount + 1;
				}
			}
			
			if(maxSizeLabel == null)
			{
				// Then there wasn't a subregion within the parent region
				return true;
			}
			
			Map<NamedFeature, DoubleType> results = opGeometric.compute(FeatureUtils.convert(regions.getLabelRegion(maxSizeLabel)));
			for(Entry<NamedFeature, DoubleType> result : results.entrySet())
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + id);
				newMap.put("Label", "" + reg.getLabel());
				this.write(newMap, result.getValue().getRealDouble());
			}
			DimensionMap newMap = mapM.copyAndSet("Measurement=subRegionCount");
			newMap.put("Id", "" + id);
			newMap.put("Label", "" + reg.getLabel());
			this.write(newMap, (double) subRegionCount);
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean putStats(DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<UnsignedByteType> mask, Img<T> image)
	{
		if(this.isCanceled())
		{
			this.close();
			return false;
		}
		if(stats)
		{
			if(opStats == null)
			{
				opStats = IJ2PluginUtility.ij().op().op(StatsFeatureSet.class, (IterableInterval<T>) image);
			}
			Map<NamedFeature, DoubleType> results = opStats.compute(Regions.sample(reg, image));
			for(Entry<NamedFeature, DoubleType> result : results.entrySet())
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + id);
				newMap.put("Label", "" + reg.getLabel());
				this.write(newMap, result.getValue().get());
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean putHaralick2D(DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<T> image)
	{
		Map<NamedFeature, DoubleType> results;

		if(haralick2D)
		{
			if(opHaralick2DHor == null || opHaralick2DVer == null)
			{
				opHaralick2DHor = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, image, DoubleType.class, (int) haralickGrayLevels, (int) haralickDistance, "HORIZONTAL");
				opHaralick2DVer = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, image, (int) haralickGrayLevels, (int) haralickDistance, "VERTICAL");
				if(haralickNumDirections.equals("4"))
				{
					if(opHaralick2DDiag == null || opHaralick2DAntiDiag == null)
					{
						opHaralick2DDiag = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, image, (int) haralickGrayLevels, (int) haralickDistance, "DIAGONAL");
						opHaralick2DAntiDiag = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, image, (int) haralickGrayLevels, (int) haralickDistance, "ANTIDIAGONAL");
					}
				}
			}

			results = opHaralick2DHor.compute(Regions.sample(reg, image));

			///// Horizontal /////
			if(this.isCanceled())
			{
				this.close();
				return false;
			}
			for(Entry<NamedFeature, DoubleType> result : results.entrySet())
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName() + "_Horizontal");
				newMap.put("Id", "" + id);
				newMap.put("Label", "" + reg.getLabel());
				this.write(newMap, result.getValue().getRealDouble());
			}

			///// Vertical /////
			if(this.isCanceled())
			{
				this.close();
				return false;
			}
			results = opHaralick2DVer.compute(Regions.sample(reg, image));
			for(Entry<NamedFeature, DoubleType> result : results.entrySet())
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName() + "_Vertical");
				newMap.put("Id", "" + id);
				newMap.put("Label", "" + reg.getLabel());
				this.write(newMap, result.getValue().getRealDouble());
			}

			if(haralickNumDirections.equals("4"))
			{
				///// Diagonal /////
				if(this.isCanceled())
				{
					this.close();
					return false;
				}
				results = opHaralick2DDiag.compute(Regions.sample(reg, image));
				for(Entry<NamedFeature, DoubleType> result : results.entrySet())
				{
					DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName() + "_Diagonal");
					newMap.put("Id", "" + id);
					newMap.put("Label", "" + reg.getLabel());
					this.write(newMap, result.getValue().getRealDouble());
				}

				///// Antidiagonal /////
				if(this.isCanceled())
				{
					this.close();
					return false;
				}
				results = opHaralick2DAntiDiag.compute(Regions.sample(reg, image));
				for(Entry<NamedFeature, DoubleType> result : results.entrySet())
				{
					DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName() + "_AntiDiagonal");
					newMap.put("Id", "" + id);
					newMap.put("Label", "" + reg.getLabel());
					this.write(newMap, result.getValue().getRealDouble());
				}
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean putHistogram(DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<T> image)
	{
		if(this.isCanceled())
		{
			this.close();
			return false;
		}
		if(histogram)
		{
			if(opHistogram == null)
			{
				opHistogram = IJ2PluginUtility.ij().op().op(HistogramFeatureSet.class, image, histogramBins);
			}
			IterableInterval<T> itr = Regions.sample(reg, image);
			Map<NamedFeature, LongType> ret = opHistogram.compute(itr);
			for(Entry<NamedFeature, LongType> result : ret.entrySet())
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + id);
				newMap.put("Label", "" + reg.getLabel());
				this.write(newMap, result.getValue().getRealDouble());
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean putMoments(DimensionMap mapM, int id, LabelRegion<Integer> reg, Img<T> image)
	{
		if(this.isCanceled())
		{
			this.close();
			return false;
		}
		if(moments)
		{
			if(opMoments == null)
			{
				opMoments = IJ2PluginUtility.ij().op().op(ImageMomentsFeatureSet.class, (IterableInterval<T>) image);
			}
			Map<NamedFeature, DoubleType> results = opMoments.compute(Regions.sample(reg, image));
			for(Entry<NamedFeature, DoubleType> result : results.entrySet())
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + id);
				newMap.put("Label", "" + reg.getLabel());
				this.write(newMap, result.getValue().get());
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

	//	public TreeMap<String, Object> getPixelValues(Wand wand, IdPoint p, ByteProcessor impMask, FloatProcessor impImage1, FloatProcessor impImage2)
	//	{
	//		Vector<Double> m1 = null;
	//		PointList pts = null;
	//		if(impMask.getPixel(p.x, p.y) == 255) // if we land on a cell that made
	//		// it through thresholding
	//		{
	//			wand.autoOutline(p.x, p.y); // outline it
	//			if(wand.npoints > 0)
	//			{
	//				// The roi helps for using getLength() (DON'T USE Roi.TRACED_ROI., IT SCREWS UP THE Polygon OBJECTS!!!! Bug emailed to ImageJ folks
	//				Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON);
	//				
	//				// The polygon helps for using contains()
	//				java.awt.Polygon poly = new java.awt.Polygon(wand.xpoints, wand.ypoints, wand.npoints); 
	//				Rectangle r = roi.getBounds();
	//				m1 = new Vector<Double>();
	//				pts = new PointList();
	//				for(int i = r.x; i < r.x + r.width; i++)
	//				{
	//					for(int j = r.y; j < r.y + r.height; j++)
	//					{
	//						// innerBoundary
	//						if(poly.contains(i, j) && impMask.getPixelValue(i, j) == 255)
	//						{
	//							m1.add((double) impImage1.getPixelValue(i, j));
	//							pts.add(i, j);
	//							// Logs.log("In - " + innerT, this);
	//						}
	//					}
	//				}
	//			}
	//		}
	//		TreeMap<String, Object> ret = new TreeMap<String, Object>();
	//		ret.put("m1", m1);
	//		ret.put("xy", pts);
	//		return ret;
	//	}

	class MyConverter implements Converter<Void, BitType> {

		@Override
		public void convert(Void input, BitType output)
		{
			output.set(true);
		}
	}
	
	private LabelRegions<Integer> getSubRegions(LabelRegion<Integer> reg, Img<UnsignedByteType> mask)
	{
		ImgLabeling<Integer, IntType> cellLabeling = FeatureUtils.getConnectedComponents(new IterableRandomAccessibleInterval<UnsignedByteType>(new RAII(reg, mask)), connectedness.equals("4 Connected"));
		LabelRegions<Integer> subRegions = new LabelRegions<Integer>(cellLabeling);
		return subRegions;
	}
	
	class RAII implements RandomAccessibleInterval<UnsignedByteType> {
		
		LabelRegion<Integer> reg;
		Img<UnsignedByteType> mask;
		SamplingIterableInterval<UnsignedByteType> sampler;
		
		public RAII(LabelRegion<Integer> reg, Img<UnsignedByteType> mask)
		{
			this.mask = mask;
			sampler = (SamplingIterableInterval<UnsignedByteType>) Regions.sample(reg, mask);
		}

		@Override
		public RandomAccess<UnsignedByteType> randomAccess() {
			return mask.randomAccess();
		}

		@Override
		public RandomAccess<UnsignedByteType> randomAccess(Interval interval) {
			return mask.randomAccess(interval);
		}

		@Override
		public int numDimensions() {
			// TODO Auto-generated method stub
			return sampler.numDimensions();
		}

		@Override
		public long min(int d) {
			// TODO Auto-generated method stub
			return sampler.min(d);
		}

		@Override
		public void min(long[] min) {
			// TODO Auto-generated method stub
			sampler.min(min);
		}

		@Override
		public void min(Positionable min) {
			// TODO Auto-generated method stub
			sampler.min(min);
		}

		@Override
		public long max(int d) {
			// TODO Auto-generated method stub
			return sampler.max(d);
		}

		@Override
		public void max(long[] max) {
			// TODO Auto-generated method stub
			sampler.max(max);
		}

		@Override
		public void max(Positionable max) {
			// TODO Auto-generated method stub
			sampler.max(max);
		}

		@Override
		public double realMin(int d) {
			// TODO Auto-generated method stub
			return sampler.realMin(d);
		}

		@Override
		public void realMin(double[] min) {
			// TODO Auto-generated method stub
			sampler.realMin(min);
		}

		@Override
		public void realMin(RealPositionable min) {
			// TODO Auto-generated method stub
			sampler.realMin(min);
		}

		@Override
		public double realMax(int d) {
			// TODO Auto-generated method stub
			return sampler.realMax(d);
		}

		@Override
		public void realMax(double[] max) {
			// TODO Auto-generated method stub
			sampler.realMax(max);
		}

		@Override
		public void realMax(RealPositionable max) {
			// TODO Auto-generated method stub
			sampler.realMax(max);
		}

		@Override
		public void dimensions(long[] dimensions) {
			// TODO Auto-generated method stub
			sampler.dimensions(dimensions);
		}

		@Override
		public long dimension(int d) {
			// TODO Auto-generated method stub
			return sampler.dimension(d);
		}
	}
}
