// Define package name as "plugins" as show here
package function.plugin.plugins.featureExtraction;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.SingleUserDatabase.JEXReader;
import function.ops.featuresets.DoubleNormalizedZernikeFeatureSet;
import function.ops.featuresets.Geometric2DFeatureSet;
import function.ops.featuresets.Haralick2DFeatureSet;
import function.ops.featuresets.HistogramFeatureSet;
import function.ops.featuresets.ImageMomentsFeatureSet;
import function.ops.featuresets.LBPHistogramFeatureSet;
import function.ops.featuresets.StatsFeatureSet;
import function.ops.featuresets.Tamura2DFeatureSet;
import function.ops.featuresets.ZernikeFeatureSet;
import function.ops.featuresets.wrappers.WriterWrapper;
import function.ops.geometry.DefaultSmallestEnclosingCircle;
import function.ops.geometry.DefaultSymmetryCoefficients;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
// Import needed classes here 
import image.roi.IdPoint;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.Pair;
import net.imagej.ops.featuresets.NamedFeature;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.image.cooccurrencematrix.MatrixOrientation2D;
import net.imagej.ops.special.function.BinaryFunctionOp;
import net.imagej.ops.special.function.Functions;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.roi.Regions;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

// Specify plugin characteristics here
@Plugin(
		type = JEXPlugin.class,
		name = "Feature Extraction",
		menuPath = "Feature Extraction",
		visible = true,
		description = "Function for testing feature extraction using the ImageJ Ops framework."
		)
public class FeatureExtraction<T extends RealType<T>> extends JEXPlugin {

	public final String CIRCLE_SMALLEST_ENCLOSING = "Smallest Enclosing", CIRCLE_SMALLEST_AT_CENTER_OF_MASS = "Smallest Enclosing Circle at Center of Mass";

	public WriterWrapper wrapWriter = new WriterWrapper();

	private Img<UnsignedByteType> wholeCellMaskImage;
	private Img<UnsignedByteType> mask;
	private Img<FloatType> image;
	private ImgLabeling<Integer,IntType> wholeCellLabeling;
	private ImgLabeling<Integer,IntType> maskParentLabeling;
	private LabelRegions<Integer> wholeCellRegions;
	private LabelRegions<Integer> maskParentRegions;
	private ROIPlus maxima;
	private LabelRegion<Integer> wholeCellRegion;
	private LabelRegion<Integer> combinedSubCellRegion;
	private Vector<LabelRegion<Integer>> individualSubCellRegions;
	private TreeMap<Integer, Pair<Double,RealLocalizable>> nuclearInfo;
	private boolean nucExists = false;
	private Vector<Polygon> polygons;
	private DimensionMap mapMask, mapImage, mapMask_NoChannel;
	private TreeMap<DimensionMap,Double> channelOffsetValues;
	private Integer pId;

	public Geometric2DFeatureSet<Polygon, DoubleType> opGeometric = null;
	public Haralick2DFeatureSet<FloatType, DoubleType> opHaralick2DHor = null;
	public Haralick2DFeatureSet<FloatType, DoubleType> opHaralick2DVer = null;
	public Haralick2DFeatureSet<FloatType, DoubleType> opHaralick2DDiag = null;
	public Haralick2DFeatureSet<FloatType, DoubleType> opHaralick2DAntiDiag = null;
	public HistogramFeatureSet<FloatType> opHistogram = null;
	public ImageMomentsFeatureSet<FloatType, DoubleType> opMoments = null;
	public ZernikeFeatureSet<FloatType> opZernike = null;
	public DoubleNormalizedZernikeFeatureSet<FloatType> opDNZernike = null;
	// What about IntensityFeatureSet???
	public StatsFeatureSet<FloatType, DoubleType> opStats = null;
	public DefaultSmallestEnclosingCircle opEncircle = null;
	public Tamura2DFeatureSet<FloatType,DoubleType> opTamura = null;
	public LBPHistogramFeatureSet<FloatType> opLBP = null;
	@SuppressWarnings("rawtypes")
	public BinaryFunctionOp<RandomAccessibleInterval<FloatType>, LabelRegion<?>, TreeMap> symcoOp = null;


	public int total = 0, count = 0;
	public int percentage = 0;

	public TreeMap<DimensionMap, String> imageMap = new TreeMap<DimensionMap, String>();
	public TreeMap<DimensionMap, String> maskMap = new TreeMap<DimensionMap, String>();
	public TreeMap<DimensionMap, ROIPlus> roiMap = new TreeMap<DimensionMap, ROIPlus>();
	TreeMap<Integer, Integer> idToLabelMap = new TreeMap<Integer, Integer>();
	DimTable filterTable = new DimTable();

	FeatureUtils utils = new FeatureUtils();

	// Define a constructor that takes no arguments.
	public FeatureExtraction() {
	}

	// ///////// Define Inputs here ///////////

	@InputMarker(uiOrder = 1, name = "Images to Measure", type = MarkerConstants.TYPE_IMAGE, description = "Intensity images", optional = false)
	JEXData imageData;

	@InputMarker(uiOrder = 3, name = "Masks", type = MarkerConstants.TYPE_IMAGE, description = "Mask images", optional = false)
	JEXData maskData;

	@InputMarker(uiOrder = 4, name = "Maxima", type = MarkerConstants.TYPE_ROI, description = "Maxima ROI", optional = false)
	JEXData roiData;

	// ///////// Define Parameters here ///////////

	@ParameterMarker(uiOrder = 0, name = "Channel dim name", description = "Channel dimension name in mask data.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "Channel")
	String channelName;

	@ParameterMarker(uiOrder = 1, name = "Channel Offsets <Val1>,<Val2>,...<Valn>", description = "Set a single offset for all channels (e.g., positive 5.0 to subtract off 5.0 before doing calculations) or a different offset for each channel. Must have 1 value for each channel comma separated (e.g., '<Val1>,<Val2>,...<Valn>').", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "0.0")
	String channelOffsets;

	@ParameterMarker(uiOrder = 2, name = "'Whole Cell' mask channel value", description = "Which channel value of the mask image represents the whole cell that has a 1-to-1 mapping with the maxima points.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "WholeCell")
	String maskWholeCellChannelValue;

	@ParameterMarker(uiOrder = 3, name = "'Nuclear' mask channel value (optional)", description = "(Optional) If a nuclear mask exists and it is specified, additional nuanced calculations of Zernike features are provided (see comments in code).", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String maskNuclearChannelValue;
	
	@ParameterMarker(uiOrder = 4, name = "Exclusion Filter DimTable", description = "Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;
	
	//	@ParameterMarker(uiOrder = 3, name = "Image intensity offset", description = "Amount the images are offset from zero (will be subtracted before calculation)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "0.0")
	//	double offset;

	@ParameterMarker(uiOrder = 5, name = "** Compute Stats Features?", description = "Whether to quantify first order statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean stats;

	@ParameterMarker(uiOrder = 6, name = "** Compute 2D Geometric Features?", description = "Whether to quantify geometric statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean geometric;

	@ParameterMarker(uiOrder = 7, name = "** Compute 2D Haralick Features?", description = "Whether to quantify Haralick texture statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean haralick2D;

	@ParameterMarker(uiOrder = 8, name = "** Compute Histogram Features?", description = "Whether to quantify histogram statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean histogram;

	@ParameterMarker(uiOrder = 9, name = "** Compute Moments Features?", description = "Whether to quantify image moment statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean moments;

	@ParameterMarker(uiOrder = 10, name = "** Compute LBP Features?", description = "Whether to quantify linear binary pattern (LBP) statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean lbp;

	@ParameterMarker(uiOrder = 11, name = "** Compute Tamura Features?", description = "Whether to quantify Tamura statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean tamura;

	@ParameterMarker(uiOrder = 12, name = "** Compute Zernike Features?", description = "Whether to quantify Zernike shape statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean zernike;

	@ParameterMarker(uiOrder = 13, name = "** Connectedness Features", description = "The structuring element or number of neighbors to require to be part of the neighborhood.", ui = MarkerConstants.UI_DROPDOWN, choices = {"4 Connected", "8 Connected" }, defaultChoice = 0)
	String connectedness;

	// Feature set parameters

	@ParameterMarker(uiOrder = 18, name = "Haralick Gray Levels", description = "Number of gray levels for Haralick calculations", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "8")
	int haralickGrayLevels;

	@ParameterMarker(uiOrder = 19, name = "Haralick Co-Occurrence Matrix Distance", description = "Distance at which to compute the co-occurrence matrix", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "1")
	double haralickDistance;

	@ParameterMarker(uiOrder = 20, name = "Haralick Number of Directions", description = "(Orthogonals and Diagonals etc) 2 performs horizontal and vertical. 4 adds the 2 diagonals as well.", ui = MarkerConstants.UI_DROPDOWN, choices = {"2", "4" }, defaultChoice = 1)
	String haralickNumDirections;

	@ParameterMarker(uiOrder = 21, name = "Histogram Num. Bins", description = "Number of bins for the histogram created for each cell region", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "32")
	int histogramBins;

	@ParameterMarker(uiOrder = 22, name = "Tumura Num. Bins", description = "Number of bins for the Tamura calculations for each cell region", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "16")
	int tamuraBins;

	@ParameterMarker(uiOrder = 23, name = "Zernike Min Moment", description = "Min Zernike moment calculate", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "1")
	int zernikeMomentMin;

	@ParameterMarker(uiOrder = 24, name = "Zernike Max Moment", description = "Max Zernike moment calculate", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "3")
	int zernikeMomentMax;

	@ParameterMarker(uiOrder = 25, name = "Zernike Fixed Diameter [pixels]", description = "DIAMETER of circular region within which to normalize pixel locations for Zernike Calculations. Keep this consistent from dataset to dataset.", ui = MarkerConstants.UI_TEXTFIELD, defaultText="50")
	double zernikeFixedDiameter;

	@ParameterMarker(uiOrder = 26, name = "Zernike Avg Nuc Diameter [pixels]", description = "Typical DIAMETER of nucleus. This determines the nuclear padding ratio (fixed dia / nuc dia). Keep this consistent from dataset to dataset.", ui = MarkerConstants.UI_TEXTFIELD, defaultText="15")
	double zernikeNucDiameter;

	@ParameterMarker(uiOrder = 27, name = "Save ARFF version as well?", description = "Initially, the file is written as a CSV and can be also saved as a .arff file as well. Should the .arff file be saved (it takes longer)?", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean saveArff;

	// Add a primary mask and secondary mask

	/////////// Define Outputs here ///////////

	@OutputMarker(uiOrder = 1, name = "Feature CSV Table", type = MarkerConstants.TYPE_FILE, flavor = "", description = "Output in csv format (i.e., for Excel etc).", enabled = true)
	JEXData outputCSV;

	@OutputMarker(uiOrder = 2, name = "Feature ARFF Table", type = MarkerConstants.TYPE_FILE, flavor = "", description = "Output in arff format (e.g., for Weka etc).", enabled = true)
	JEXData outputARFF;

	// Define threading capability here (set to 1 if using non-final static
	// variables shared between function instances).
	@Override
	public int getMaxThreads() {
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {

		this.wrapWriter = new WriterWrapper();

		if(!checkInputs())
		{
			// Something is wrong and need to return false;
			return false;
		}

		// Initialize: imageMap, maskMap, roiMap, total, count, percentage, nucExists
		if(!this.initializeVariables())
		{
			return false;
		}

		DimTable imageDimTable = imageData.getDimTable();
		DimTable maskDimTable = maskData.getDimTable();
		DimTable maskDimTable_NoChannel = maskDimTable.getSubTable(channelName);
		Dim maskChannelDim = maskDimTable.getDimWithName(channelName);
		int subcount = imageDimTable.getSubTable(maskDimTable_NoChannel.getMapIterator().iterator().next()).mapCount();
		this.total = maskDimTable.mapCount() * subcount;
		
		this.filterTable = new DimTable(this.filterDimTableString);

		// For each whole cell mask
		for(DimensionMap mapMask_NoChannelTemp : maskDimTable_NoChannel.getMapIterator())
		{
			if (this.isCanceled()) { this.close(); return false; }

			this.mapMask_NoChannel = mapMask_NoChannelTemp;
			DimensionMap mapMask_WholeCell = this.mapMask_NoChannel.copyAndSet(channelName + "=" + maskWholeCellChannelValue);

			// Skip if we can
			if(this.filterTable.testMapAsExclusionFilter(mapMask_WholeCell))
			{
				continue;
			}
			
			this.setWholeCellMask(mapMask_WholeCell);
			if(this.wholeCellMaskImage == null) { continue; }
			if (this.isCanceled()) { this.close(); return false; }
			this.setMaximaRoi(mapMask_WholeCell);
			this.setIdToLabelMap(mapMask_WholeCell);
			if (this.isCanceled()) { this.close(); return false; }
			this.nuclearInfo = new TreeMap<Integer, Pair<Double, RealLocalizable>>();

			//	For each Image (IMAGE DimTable filtered on (WholeCellMask - ChannelDim))
			DimTable imageSubsetTable = imageDimTable.getSubTable(this.mapMask_NoChannel);
			boolean firstTimeThrough = true;
			for(DimensionMap mapMaskTemp: imageSubsetTable.getMapIterator())
			{
				// Skip if we can
				if(this.filterTable.testMapAsExclusionFilter(mapMaskTemp))
				{
					continue;
				}
				
				// Set the image
				this.setImageToMeasure(mapMaskTemp);
				if(this.image == null)
				{
					continue;
				}

				// Quantify nuclear region first if possible
				if(this.nucExists)
				{
					if(!this.quantifyFeatures(this.maskNuclearChannelValue, firstTimeThrough))
						return false;
					this.updateStatus();
				}

				// Then quantify whole cell mask
				if(!this.quantifyFeatures(this.maskWholeCellChannelValue, firstTimeThrough))
					return false;
				this.updateStatus();

				// Then quantify other masks
				for(String maskChannelValue : maskChannelDim.dimValues)
				{
					if(!maskChannelValue.equals(this.maskNuclearChannelValue) && !maskChannelValue.equals(this.maskWholeCellChannelValue))
					{
						if(!this.quantifyFeatures(maskChannelValue, firstTimeThrough))
							return false;
						this.updateStatus();
					}
				}

				firstTimeThrough = false;				
			}

		}
		this.close();
		return true;
	}

	public void updateStatus()
	{
		this.count = this.count + 1;
		this.percentage = (int) (100 * ((double) (count) / ((double) total)));
		JEXStatics.statusBar.setProgressPercentage(percentage);
	}

	public void setWholeCellMask(DimensionMap mapMask_WholeCell)
	{
		// Initialize structures for storing whole-cell mask information and maxima information.
		Logs.log("Getting whole cell mask: " + mapMask_WholeCell, this);
		String path = maskMap.get(mapMask_WholeCell);
		if(path == null || path.equals(""))
		{
			this.wholeCellMaskImage = null;
			this.wholeCellLabeling = null;
			this.wholeCellRegions = null;
		}
		else
		{
			this.wholeCellMaskImage = JEXReader.getSingleImage(path, null);
			this.wholeCellLabeling = utils.getLabeling(this.wholeCellMaskImage, connectedness.equals("4 Connected"));
			this.wholeCellRegions = new LabelRegions<Integer>(this.wholeCellLabeling);
		}
		//utils.show(this.cellLabeling, false);
		//utils.show(wholeCellMaskImage, false);
	}

	public void setImageToMeasure(DimensionMap mapImage)
	{
		Logs.log("Measuring image: " + this.mapImage, this);
		this.mapImage = mapImage;
		String pathToGet = imageMap.get(this.mapImage);
		if(pathToGet == null)
		{
			this.image = null;
		}
		else
		{
			this.image = JEXReader.getSingleFloatImage(imageMap.get(this.mapImage), this.channelOffsetValues.get(this.mapImage));
		}
	}

	public void setMaskToMeasure()
	{
		Logs.log("Measuring mask: " + this.mapMask, this);
		this.mask = JEXReader.getSingleImage(maskMap.get(this.mapMask), null);

		// Apply the labels of the whole cells to the mask (e.g., multiple subregions now get the same parent label)
		this.maskParentLabeling = utils.applyLabeling(this.wholeCellLabeling, this.mask);
		//		Img<UnsignedShortType> img = utils.makeImgFromLabeling(this.maskParentLabeling);
		//		String path = JEXWriter.saveImage(img);
		//		try {
		//			FileUtility.openFileDefaultApplication(path);
		//		} catch (Exception e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		this.maskParentRegions = new LabelRegions<Integer>(this.maskParentLabeling);
	}

	private void setMaximaRoi(DimensionMap mapMask_WholeCell)
	{
		this.maxima = roiMap.get(mapMask_WholeCell);
	}

	private void setIdToLabelMap(DimensionMap mapMask_WholeCell)
	{
		this.idToLabelMap = new TreeMap<Integer, Integer>();

		// Determine which LabelRegions are the ones we want to keep by
		// testing if our maxima of interest are contained.
		ROIPlus maxima = roiMap.get(mapMask_WholeCell);
		for (LabelRegion<Integer> cellRegion : this.wholeCellRegions) {
			RandomAccess<BoolType> ra = cellRegion.randomAccess();
			for (IdPoint p : maxima.pointList) {
				ra.setPosition(p);
				if (ra.get().get()) {
					idToLabelMap.put(p.id, cellRegion.getLabel().intValue());
					//utils.showRegion(cellRegion);
					//System.out.println("Getting Major Subregion for " + p.id + " of size " + cellRegion.size() + " at " + p.x + "," + p.y + " for label at " + cellRegion.getCenterOfMass());
				}
			}
		}
	}

	private void setRegionsAndPolygon(int pId)
	{
		this.pId = pId;
		Integer labelToGet = this.idToLabelMap.get(pId);
		if(labelToGet == null || !this.maskParentRegions.getExistingLabels().contains(labelToGet))
		{
			this.wholeCellRegion = null;
			this.combinedSubCellRegion = null;
			this.individualSubCellRegions = null;
			this.polygons = null;
		}
		else
		{
			this.wholeCellRegion = this.wholeCellRegions.getLabelRegion(labelToGet);
			this.combinedSubCellRegion = this.maskParentRegions.getLabelRegion(labelToGet);
			this.individualSubCellRegions = this.getSubRegions(this.wholeCellRegion, this.mask);
			this.polygons = new Vector<Polygon>();
			for(LabelRegion<Integer> region : this.individualSubCellRegions)
			{
				this.polygons.add(utils.getPolygonFromBoolean(region));
			}

		}
	}

	public boolean quantifyFeatures(String maskChannelName, boolean firstTimeThrough)
	{
		this.mapMask = this.mapMask_NoChannel.copyAndSet(this.channelName + "=" + maskChannelName);
		if(this.filterTable.testMapAsExclusionFilter(this.mapMask))
		{
			return true; // i.e., skip
		}
		this.setMaskToMeasure();
		for(IdPoint p : this.maxima.getPointList())
		{
			// Set geometries and quantify
			this.setRegionsAndPolygon(p.id);
			if(this.combinedSubCellRegion != null)
			{
				if(!this.putFeatures(firstTimeThrough))
					return false;
			}

		}
		return true;
	}

	private boolean putFeatures(boolean firstTimeThrough)
	{
		if(this.combinedSubCellRegion == null || this.combinedSubCellRegion.size() <= 1)
		{
			return true;
		}

		if(firstTimeThrough)
		{
			// Then quantify geometric features
			if(this.mapMask.get(this.channelName).equals(this.maskNuclearChannelValue))
			{
				// Store the nuclear Info
				this.nuclearInfo.put(this.pId, new Pair<Double,RealLocalizable>(this.getEquivalentRadius(this.combinedSubCellRegion), this.combinedSubCellRegion.getCenterOfMass()));
			}
			// Write the feature data
			if(!this.putGeometricFeatures())
				return false;
		}

		if(!this.putIntensityFeatures(firstTimeThrough))
			return false;

		if(!putBinaryTextureFeatures())
			return false;

		return true;

	}

	public void putSymmetryFeatures(DimensionMap mapM_Intensity, boolean firstTimeThrough)
	{
		if(symcoOp == null)
		{
			symcoOp = Functions.binary(IJ2PluginUtility.ij().op(), DefaultSymmetryCoefficients.class, TreeMap.class, (RandomAccessibleInterval<FloatType>) this.image, this.combinedSubCellRegion, 4);
		}

		if(firstTimeThrough)
		{
			// Then get info relate to the mask
			@SuppressWarnings("unchecked")
			TreeMap<String,Double> maskSymmetryData = (TreeMap<String,Double>) symcoOp.calculate(null, this.combinedSubCellRegion);
			for(Entry<String,Double> e : maskSymmetryData.entrySet())
			{
				DimensionMap newMap = mapM_Intensity.copyAndSet("Measurement=" + e.getKey());
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				newMap.put("ImageChannel", "None"); // Overwrite mask channel
				this.write(newMap, e.getValue());
			}
		}


		// Quantify this combination of mask and image
		@SuppressWarnings("unchecked")
		TreeMap<String,Double> imageSymmetryData = (TreeMap<String,Double>) symcoOp.calculate(this.image, this.combinedSubCellRegion);
		for(Entry<String,Double> e : imageSymmetryData.entrySet())
		{
			DimensionMap newMap = mapM_Intensity.copyAndSet("Measurement=" + e.getKey());
			newMap.put("Id", "" + this.pId);
			newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
			this.write(newMap, e.getValue());
		}
	}

	public boolean putIntensityFeatures(boolean firstTimeThrough)
	{
		DimensionMap mapM_Intensity = this.mapMask_NoChannel.copyAndSet("MaskChannel=" + mapMask.get(channelName));
		String imageChannelValue = this.mapImage.get(channelName);
		if(imageChannelValue == null)
		{
			imageChannelValue = "None";
		}
		mapM_Intensity.put("ImageChannel", imageChannelValue);

		IterableInterval<FloatType> ii = Regions.sample(this.combinedSubCellRegion, this.image); //Views.offsetInterval(this.image, this.wholeCellRegion));


		//      -- Symmetry -- 
		//
		//
		this.putSymmetryFeatures(mapM_Intensity, firstTimeThrough);
		if (this.isCanceled()) { this.close(); return false;}


		//      -- Standard Stats, Haralick, Histogram, and Moments --
		//
		//
		this.putStats(mapM_Intensity, ii);
		if (this.isCanceled()) { this.close(); return false;}
		this.putHaralick2D(mapM_Intensity, ii);
		if (this.isCanceled()) { this.close(); return false;}
		this.putHistogram(mapM_Intensity, ii);
		if (this.isCanceled()) { this.close(); return false;}
		this.putMoments(mapM_Intensity, ii);
		if (this.isCanceled()) { this.close(); return false;}

		//		-- In Zernike --
		//	
		//	QuantifyZernike (circle = fixed@thisCOM)
		//	QuantifyZernike (circle = thisSEC@thisCOM)

		this.putZernike(mapM_Intensity, ii, new Circle(this.combinedSubCellRegion.getCenterOfMass(), this.zernikeFixedDiameter/2.0), "_THISwFIXED", firstTimeThrough); 
		if (this.isCanceled()) { this.close(); return false;}
		this.putZernike(mapM_Intensity, ii, utils.getCircle(this.combinedSubCellRegion, this.combinedSubCellRegion.getCenterOfMass()), "_THISwSEC", firstTimeThrough);
		if (this.isCanceled()) { this.close(); return false;}

		//	if(NucMASK)
		//	
		//		QuantifyZernike (region = nuc, circle = paddedNucER@nucCOM)

		if(mapMask.get(channelName).equals(this.maskNuclearChannelValue) && this.nuclearInfo.get(this.pId) != null)
		{
			this.putZernike(mapM_Intensity, ii, new Circle(this.combinedSubCellRegion.getCenterOfMass(), this.nuclearInfo.get(this.pId).p1 * (this.zernikeFixedDiameter / this.zernikeNucDiameter)), "_NUCwPADDEDNUC", firstTimeThrough);
			if (this.isCanceled()) { this.close(); return false;}
		}

		//	else if(WholeCellMASK && circle != null)
		//			
		//		QuantifyZernike (region = WC, circle = fixed@nucCOM)
		//		QuantifyZernike (region = WC, circle = wcSEC@nucCOM)
		//		QauntifyZernike (region = WC, circle = paddedNucER@nucCOM)
		//		QuantifyZernike (region = WC, circle = wcER&nucER@nucCOM)

		else if(mapMask.get(channelName).equals(this.maskWholeCellChannelValue) && this.nucExists && this.nuclearInfo.get(this.pId) != null)
		{
			this.putZernike(mapM_Intensity, ii, new Circle(this.nuclearInfo.get(this.pId).p2, this.zernikeFixedDiameter/2.0), "_NUCwFIXED", firstTimeThrough);
			if (this.isCanceled()) { this.close(); return false;}
			this.putZernike(mapM_Intensity, ii, utils.getCircle(this.combinedSubCellRegion, this.nuclearInfo.get(this.pId).p2), "_NUCwSEC", firstTimeThrough);
			if (this.isCanceled()) { this.close(); return false;}
			this.putZernike(mapM_Intensity, ii, new Circle(this.nuclearInfo.get(this.pId).p2, this.nuclearInfo.get(this.pId).p1 * (this.zernikeFixedDiameter / this.zernikeNucDiameter)), "_NUCwPADDEDNUC", firstTimeThrough);
			if (this.isCanceled()) { this.close(); return false;}
			this.putDNZernike(mapM_Intensity, ii, new Circle(this.nuclearInfo.get(this.pId).p2, this.nuclearInfo.get(this.pId).p1), new Circle(this.nuclearInfo.get(pId).p2, this.getEquivalentRadius(this.combinedSubCellRegion)), firstTimeThrough);
			if (this.isCanceled()) { this.close(); return false;}
		}

		//	else if(!NucMASK && circle != null)
		//			
		//		QuantifyZernike (region = other, circle = fixed@nucCOM)
		//		QuantifyZernike (region = other, circle = thisSEC@nucCOM)
		//		QauntifyZernike (region = other, circle = paddedNucER@nucCOM)

		else if(!mapMask.get(channelName).equals(this.maskNuclearChannelValue) && this.nucExists && this.nuclearInfo.get(this.pId) != null)
		{
			this.putZernike(mapM_Intensity, ii, new Circle(this.nuclearInfo.get(this.pId).p2, this.zernikeFixedDiameter/2.0), "_NUCwFIXED", firstTimeThrough);
			if (this.isCanceled()) { this.close(); return false;}
			this.putZernike(mapM_Intensity, ii, utils.getCircle(this.combinedSubCellRegion, this.nuclearInfo.get(this.pId).p2), "_NUCwSEC", firstTimeThrough);
			if (this.isCanceled()) { this.close(); return false;}
			this.putZernike(mapM_Intensity, ii, new Circle(this.nuclearInfo.get(this.pId).p2, this.nuclearInfo.get(this.pId).p1 * (this.zernikeFixedDiameter / this.zernikeNucDiameter)), "_NUCwPADDEDNUC", firstTimeThrough);
			if (this.isCanceled()) { this.close(); return false;}
		}

		// Now do the binary texture patter stats


		return true;
	}

	public boolean putBinaryTextureFeatures()
	{
		DimensionMap mapM_Intensity = this.mapMask_NoChannel.copyAndSet("MaskChannel=" + mapMask.get(channelName));
		String imageChannelValue = this.mapImage.get(channelName);
		if(imageChannelValue == null)
		{
			imageChannelValue = "None";
		}
		mapM_Intensity.put("ImageChannel", imageChannelValue);

		RandomAccessibleInterval<FloatType> vals = utils.cropRealRAI(this.combinedSubCellRegion, this.image);

		// utils.show(vals);
		this.putTamura(mapM_Intensity, vals);
		if (this.isCanceled()) { this.close(); return false;}
		this.putLBPHistogram(mapM_Intensity, vals);
		if (this.isCanceled()) { this.close(); return false;}

		return true;
	}

	public boolean putGeometricFeatures()
	{
		if (this.isCanceled()) { this.close(); return false;}
		this.putGeometric();
		return true;
	}

	private double getEquivalentRadius(IterableInterval<?> region)
	{
		return Math.sqrt(((double) region.size())/Math.PI);
	}

	//	public String getMaskOnImageString(DimensionMap mapMask, DimensionMap mapImage) {
	//
	//		String imageChannelString = null;
	//		if (mapImage != null) {
	//			imageChannelString = mapImage.get(channelName);
	//		}
	//		String maskChannelString = null;
	//		if (mapImage != null) {
	//			maskChannelString = mapMask.get(channelName);
	//		}
	//
	//		if (maskChannelString == null && imageChannelString == null) {
	//			// Should never happen.
	//			return "NA_NA";
	//		}
	//		if (imageChannelString == null) {
	//			return maskChannelString + "_NA";
	//		}
	//		if (maskChannelString == null) {
	//			return "NA_" + imageChannelString;
	//		} else {
	//			return maskChannelString + "_" + imageChannelString;
	//		}
	//	}

	public LabelRegion<Integer> getMajorSubRegion(LabelRegion<Integer> region, Img<UnsignedByteType> mask)
	{
		List<LabelRegion<Integer>> subRegions = this.getSubRegions(region, mask);
		return subRegions.get(0);
	}

	/**
	 * Get a list of LabelRegions for a given mask within the pixels defined by the provided LabelRegion object
	 * @param region LabelRegion<Integer> within which to analyze the given mask to convert the mask to LabelRegions.
	 * @param mask Img<UnsignedByteType> mask to turn into LabelRegion objects within the provided region object.
	 * @return Vector<LabelRegion<Integer>> List of LabelRegions in order from largest to smallest.
	 */
	public Vector<LabelRegion<Integer>> getSubRegions(LabelRegion<Integer> region, Img<UnsignedByteType> mask)
	{
		ImgLabeling<Integer, IntType> labeling = utils.getLabelingInRegion(region, mask, connectedness.equals("4 Connected"));
		LabelRegions<Integer> subRegions = new LabelRegions<>(labeling);
		Vector<LabelRegion<Integer>> ret = new Vector<>();
		for (LabelRegion<Integer> subRegion : subRegions)
		{
			ret.add(subRegion);
		}
		Collections.sort(ret, new SizeComparator(true));
		return ret;
	}

	/**
	 * Returns region1.size() - region2.size() so > 0 if region1 is bigger than region2
	 * @author jaywarrick
	 *
	 */
	class SizeComparator implements Comparator<LabelRegion<?>>
	{
		boolean reverse = false;

		public SizeComparator(boolean reverse)
		{
			this.reverse = reverse;
		}

		@Override
		public int compare(LabelRegion<?> region1, LabelRegion<?> region2)
		{
			int ret = 0;
			long diff = (region1.size() - region2.size());
			if(diff < 0) ret = -1;
			if(diff > 0) ret = 1;
			if(reverse) return -1*ret;
			return ret;
		}

	}

	// @SuppressWarnings("unchecked")
	// public boolean putTamura(DimensionMap mapM, int id, LabelRegion<Integer>
	// reg, Img<T> image)
	// {
	// if(this.isCanceled())
	// {
	// this.close();
	// return false;
	// }
	// if(tamura)
	// {
	// if(opTamura == null)
	// {
	// opTamura = IJ2PluginUtility.ij().op().op(Tamura2DFeatureSet.class,
	// image);
	// }
	// Map<NamedFeature, DoubleType> results =
	// opTamura.compute(Views.Regions.sample(reg, image));
	// for(Entry<NamedFeature, DoubleType> result : results.entrySet())
	// {
	// DimensionMap newMap = mapM.copyAndSet("Measurement=" +
	// result.getKey().getName());
	// newMap.put("Id", "" + id);
	// newMap.put("Label", "" + idToLabelMap.get(id));
	// this.write(newMap, result.getValue().get());
	// }
	// }
	// return true;
	// }

	@SuppressWarnings("unchecked")
	public void putGeometric()
	{
		if (geometric) {

			if (polygons.size() == 0) {
				Logs.log("Encountered an empty list of polygons for geometric2d features. id:" + this.pId + ", label:" + this.combinedSubCellRegion.getLabel() + ", mask:" + this.mapMask,
						FeatureExtraction.class);
			}
			if (opGeometric == null) {
				opGeometric = IJ2PluginUtility.ij().op().op(Geometric2DFeatureSet.class, this.polygons.get(0));
			}

			int i = 1;
			for(Polygon p : polygons)
			{
				// TODO: Revisit whether to keep this in or not.
				if(p.getVertices().size() < 5)
				{
					i = i + 1;
					continue;
				}
				DimensionMap mapM_Geometry = this.mapMask_NoChannel.copyAndSet("MaskChannel=" + this.mapMask.get(channelName) + ".p" + i);
				mapM_Geometry.put("ImageChannel", "None");
				Map<NamedFeature, DoubleType> results = opGeometric.calculate(p);
				// Map<NamedFeature, DoubleType> results =
				// opGeometric.calculate(reg);
				for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
					DimensionMap newMap = mapM_Geometry.copyAndSet("Measurement=" + result.getKey().getName());
					newMap.put("Id", "" + this.pId);
					newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
					this.write(newMap, result.getValue().getRealDouble());
				}
				DimensionMap newMap = mapM_Geometry
						.copyAndSet("Measurement=" + net.imagej.ops.Ops.Geometric.Size.class.getName() + "Iterable");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + idToLabelMap.get(this.pId));
				this.write(newMap, (double) this.individualSubCellRegions.get(i-1).size());

				RealPoint wholeCellRelativeCenterOfMass = this.getWholeCellRelativeCenterOfMass();
				RealLocalizable subCellRegionCenterOfMass = this.individualSubCellRegions.get(i-1).getCenterOfMass();
				RealPoint subCellRelativeCenterOfMass = this.getOffsetPoint(subCellRegionCenterOfMass, wholeCellRelativeCenterOfMass);

				newMap = mapM_Geometry
						.copyAndSet("Measurement=" + "net.imagej.ops.Ops$Geometric$X");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + idToLabelMap.get(this.pId));
				this.write(newMap, (double) subCellRelativeCenterOfMass.getDoublePosition(0));

				newMap = mapM_Geometry
						.copyAndSet("Measurement=" + "net.imagej.ops.Ops$Geometric$Y");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + idToLabelMap.get(this.pId));
				this.write(newMap, (double) subCellRelativeCenterOfMass.getDoublePosition(1));

				i = i + 1;
			}
		}
	}

	private RealPoint getWholeCellRelativeCenterOfMass()
	{
		RealPoint wholeCellOffset = new RealPoint(new double[]{0.0, 0.0});
		this.wholeCellRegion.realMin(wholeCellOffset);
		return this.getOffsetPoint(this.wholeCellRegion.getCenterOfMass(), wholeCellOffset);
	}

	private RealPoint getOffsetPoint(RealLocalizable p1, RealPoint offset)
	{
		if(p1.numDimensions() != offset.numDimensions())
		{
			throw new IllegalArgumentException("Points must have the same number of dimensions");
		}
		for(int i = 0; i < offset.numDimensions(); i++)
		{
			offset.setPosition(-1.0*offset.getDoublePosition(i), i);
		}
		RealPoint ret = new RealPoint(p1);
		ret.move(offset);
		return ret;
	}

	@SuppressWarnings("unchecked")
	public void putStats(DimensionMap mapM, IterableInterval<FloatType> vals)
	{
		if (stats) {
			if (opStats == null) {
				opStats = IJ2PluginUtility.ij().op().op(StatsFeatureSet.class, vals);
			}
			//this.utils.showRealII(vals, (Interval) this.wholeCellRegion, true);
			//			this.utils.showRegion(this.wholeCellRegion);
			//			this.utils.showRegion(this.subCellRegion);
			Map<NamedFeature, DoubleType> results = opStats.calculate(vals);
			for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.write(newMap, result.getValue().get());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void putHaralick2D(DimensionMap mapM, IterableInterval<FloatType> vals)
	{
		Map<NamedFeature, DoubleType> results;

		if (haralick2D) {
			if (opHaralick2DHor == null || opHaralick2DVer == null) {
				opHaralick2DHor = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, vals, DoubleType.class,
						(int) haralickGrayLevels, (int) haralickDistance, MatrixOrientation2D.HORIZONTAL);
				opHaralick2DVer = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, vals,
						(int) haralickGrayLevels, (int) haralickDistance, MatrixOrientation2D.VERTICAL);
				if (haralickNumDirections.equals("4")) {
					if (opHaralick2DDiag == null || opHaralick2DAntiDiag == null) {
						opHaralick2DDiag = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, vals,
								(int) haralickGrayLevels, (int) haralickDistance, MatrixOrientation2D.DIAGONAL);
						opHaralick2DAntiDiag = IJ2PluginUtility.ij().op().op(Haralick2DFeatureSet.class, vals,
								(int) haralickGrayLevels, (int) haralickDistance, MatrixOrientation2D.ANTIDIAGONAL);
					}
				}
			}

			results = (Map<NamedFeature, DoubleType>) opHaralick2DHor.calculate(vals);

			///// Horizontal /////
			for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName() + "_Horizontal");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.write(newMap, result.getValue().getRealDouble());
			}

			///// Vertical /////
			results = opHaralick2DVer.calculate(vals);
			for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName() + "_Vertical");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.write(newMap, result.getValue().getRealDouble());
			}

			if (haralickNumDirections.equals("4")) {
				///// Diagonal /////
				results = opHaralick2DDiag.calculate(vals);
				for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
					DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName() + "_Diagonal");
					newMap.put("Id", "" + this.pId);
					newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
					this.write(newMap, result.getValue().getRealDouble());
				}

				///// Antidiagonal /////
				results = opHaralick2DAntiDiag.calculate(vals);
				for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
					DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName() + "_AntiDiagonal");
					newMap.put("Id", "" + this.pId);
					newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
					this.write(newMap, result.getValue().getRealDouble());
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void putTamura(DimensionMap mapM, RandomAccessibleInterval<FloatType> vals)
	{
		if (tamura) {
			if (opTamura == null) {
				opTamura = IJ2PluginUtility.ij().op().op(Tamura2DFeatureSet.class, vals, this.tamuraBins);
			}
			Map<NamedFeature, DoubleType> results = opTamura.calculate(vals);
			for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.write(newMap, result.getValue().get());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void putHistogram(DimensionMap mapM, IterableInterval<FloatType> vals)
	{
		if (histogram) {
			if (opHistogram == null) {
				opHistogram = IJ2PluginUtility.ij().op().op(HistogramFeatureSet.class, vals, histogramBins);
			}
			Map<NamedFeature, DoubleType> ret = opHistogram.calculate(vals);
			for (Entry<NamedFeature, DoubleType> result : ret.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.write(newMap, result.getValue().getRealDouble());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void putLBPHistogram(DimensionMap mapM, RandomAccessibleInterval<FloatType> vals)
	{
		if (lbp) {
			if (opLBP == null) {
				opLBP = IJ2PluginUtility.ij().op().op(LBPHistogramFeatureSet.class, vals);
			}
			//utils.show(vals, true);
			Map<NamedFeature, LongType> ret = opLBP.calculate(vals);
			for (Entry<NamedFeature, LongType> result : ret.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.write(newMap, result.getValue().getRealDouble());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void putMoments(DimensionMap mapM, IterableInterval<FloatType> vals)
	{
		if (moments) {
			if (opMoments == null) {
				opMoments = IJ2PluginUtility.ij().op().op(ImageMomentsFeatureSet.class, vals);
			}
			Map<NamedFeature, DoubleType> results = opMoments.calculate(vals);
			for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.write(newMap, result.getValue().get());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void putZernike(DimensionMap mapM, IterableInterval<FloatType> vals, Circle circle, String suffix, boolean firstTimeThrough)
	{
		if(zernike)
		{
			if (this.opZernike == null) {
				opZernike = IJ2PluginUtility.ij().op().op(ZernikeFeatureSet.class, vals, zernikeMomentMin, zernikeMomentMax);
			}

			// Set the enclosing circle for this cell
			opZernike.setEnclosingCircle(circle);

			Map<NamedFeature, DoubleType> results = opZernike.calculate(vals);
			for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName() + suffix);
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, result.getValue().get());
			}
			if(firstTimeThrough)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=ZernikeCircleX" + suffix);
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, circle.getCenter().getDoublePosition(0));
				newMap = mapM.copyAndSet("Measurement=ZernikeCircleY" + suffix);
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, circle.getCenter().getDoublePosition(1));
				newMap = mapM.copyAndSet("Measurement=ZernikeCircleR" + suffix);
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, circle.getRadius());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void putDNZernike(DimensionMap mapM, IterableInterval<FloatType> vals, Circle innerCircle, Circle outerCircleAtEquivalentRadius, boolean firstTimeThrough)
	{
		if(zernike && this.nucExists)
		{
			if (this.opDNZernike == null) {
				opDNZernike = IJ2PluginUtility.ij().op().op(DoubleNormalizedZernikeFeatureSet.class, vals, null, zernikeMomentMin, zernikeMomentMax, innerCircle, outerCircleAtEquivalentRadius, 1.0/3.0);
			}

			Circle outerCircle = new Circle(outerCircleAtEquivalentRadius.getCenter(), 1.5*outerCircleAtEquivalentRadius.getRadius());

			// Set the enclosing circle for this cell
			opDNZernike.setEnclosingCircles(innerCircle, outerCircle);

			Map<NamedFeature, DoubleType> results = opDNZernike.calculate(vals);
			for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, result.getValue().get());
			}
			if(firstTimeThrough)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=DNZernikeInnerCircleX");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, innerCircle.getCenter().getDoublePosition(0));
				newMap = mapM.copyAndSet("Measurement=DNZernikeInnerCircleY");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, innerCircle.getCenter().getDoublePosition(1));
				newMap = mapM.copyAndSet("Measurement=DNZernikeInnerCircleR");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, innerCircle.getRadius());
				newMap = mapM.copyAndSet("Measurement=DNZernikeOuterCircleX");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, outerCircle.getCenter().getDoublePosition(0));
				newMap = mapM.copyAndSet("Measurement=DNZernikeOuterCircleY");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, outerCircle.getCenter().getDoublePosition(1));
				newMap = mapM.copyAndSet("Measurement=DNZernikeOuterCircleR");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				WriterWrapper.write(this.wrapWriter, newMap, outerCircle.getRadius());
			}
		}
	}

	public void close()
	{
		Pair<JEXData, JEXData> results = WriterWrapper.close(this.wrapWriter, saveArff);
		outputCSV = results.p1;
		outputARFF = results.p2;
	}

	public void write(DimensionMap map, Double value)
	{
		WriterWrapper.write(this.wrapWriter, map, value);
	}

	public boolean checkInputs()
	{
		if (!stats && !geometric && !haralick2D && !histogram && !moments && !zernike && !tamura && !lbp) {
			JEXDialog.messageDialog("Feature Extraction: Nothing selected to compute. Returning false.");
			return false;
		}
		if (maskData == null) {
			JEXDialog.messageDialog("Feature Extraction: Returning false. NEED to have a mask.");
			return false;
		}
		if (roiData == null) {
			JEXDialog.messageDialog(
					"Feature Extraction: Returning false. NEED to have a roi that defines the id of each cell.");
			return false;
		}
		if (imageData == null && (stats || haralick2D || histogram || moments)) {
			JEXDialog.messageDialog(
					"Feature Extraction: Returning false. NEED to define an image to quantify if you want to use intensity based features such as first order, haralick2D, histogram, and moment statistics.");
			return false;
		}

		if ((imageData != null && !imageData.getTypeName().getType().equals(JEXData.IMAGE))
				|| (maskData != null && !maskData.getTypeName().getType().equals(JEXData.IMAGE))
				|| (roiData != null && !roiData.getTypeName().getType().equals(JEXData.ROI))) {
			JEXDialog.messageDialog(
					"All inputs to the function are not of the correct 'Type'. Please check that the image and mask object are 'Image' objects and the maxima is a 'Roi' object.",
					this);
			return false;
		}

		return true;
	}

	public boolean initializeVariables()
	{
		imageMap = new TreeMap<>();
		if (imageData != null) {
			imageMap = ImageReader.readObjectToImagePathTable(imageData);
			CSVList channelOffsetStrings = new CSVList(channelOffsets);
			this.channelOffsetValues = new TreeMap<>();
			Dim imageChannelDim = imageData.getDimTable().getDimWithName(this.channelName);
			if(channelOffsetStrings.size() > 1 && channelOffsetStrings.size() != imageChannelDim.size())
			{
				JEXDialog.messageDialog("The number of channel offsets must either be of length 1, or the same length as the number of Channels in the specified Channel Dimension. Aborting.", this);
				return false;
			}
			boolean repeat = channelOffsetStrings.size() == 1;
			try
			{
				for(int i = 0; i < imageChannelDim.size(); i++)
				{
					if(repeat)
					{
						double offsetValue = Double.parseDouble(channelOffsetStrings.get(0));
						this.channelOffsetValues.put(new DimensionMap(this.channelName + "=" + imageChannelDim.valueAt(i)), offsetValue);
					}
					else
					{
						double offsetValue = Double.parseDouble(channelOffsetStrings.get(i));
						this.channelOffsetValues.put(new DimensionMap(this.channelName + "=" + imageChannelDim.valueAt(i)), offsetValue);
					}
				}
			}
			catch(NumberFormatException nf)
			{
				JEXDialog.messageDialog("At least one of the channel offsets couldn't be parsed as a number. Aborting.", this);
				return false;
			}
		}
		maskMap = ImageReader.readObjectToImagePathTable(maskData);
		roiMap = RoiReader.readObjectToRoiMap(roiData);

		this.count = 0;
		this.percentage = 0;

		// Calculate expected number of iterations
		// Assume at least calculating mask features
		this.total = maskData.getDimTable().mapCount();

		if(!this.maskNuclearChannelValue.equals(""))
		{
			this.nucExists = true;
		}
		return true;
	}
}
