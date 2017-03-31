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
import miscellaneous.Pair;
import net.imagej.ops.featuresets.NamedFeature;
import net.imagej.ops.geom.geom2d.Circle;
import net.imagej.ops.image.cooccurrencematrix.MatrixOrientation2D;
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
import net.imglib2.view.Views;
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
	private Img<T> image;
	private ImgLabeling<Integer,IntType> wholeCellLabeling;
	private ImgLabeling<Integer,IntType> maskLabeling;
	private LabelRegions<Integer> wholeCellRegions;
	private LabelRegions<Integer> maskRegions;
	private ROIPlus maxima;
	private LabelRegion<Integer> wholeCellRegion;
	private LabelRegion<Integer> subCellRegion;
	private Vector<LabelRegion<Integer>> subCellRegions;
	private TreeMap<Integer, Pair<Double,RealLocalizable>> nuclearInfo;
	private boolean nucExists = false;
	private Vector<Polygon> polygons;
	private DimensionMap mapMask, mapImage, mapMask_NoChannel;
	private Integer pId;

	public Geometric2DFeatureSet<Polygon, DoubleType> opGeometric = null;
	public Haralick2DFeatureSet<T, DoubleType> opHaralick2DHor = null;
	public Haralick2DFeatureSet<T, DoubleType> opHaralick2DVer = null;
	public Haralick2DFeatureSet<T, DoubleType> opHaralick2DDiag = null;
	public Haralick2DFeatureSet<T, DoubleType> opHaralick2DAntiDiag = null;
	public HistogramFeatureSet<T> opHistogram = null;
	public ImageMomentsFeatureSet<T, DoubleType> opMoments = null;
	public ZernikeFeatureSet<T> opZernike = null;
	public DoubleNormalizedZernikeFeatureSet<T> opDNZernike = null;
	// What about IntensityFeatureSet???
	public StatsFeatureSet<T, DoubleType> opStats = null;
	public DefaultSmallestEnclosingCircle opEncircle = null;
	public Tamura2DFeatureSet<T,DoubleType> opTamura = null;
	public LBPHistogramFeatureSet<T> opLBP = null;

	public int total = 0, count = 0;
	public int percentage = 0;

	public TreeMap<DimensionMap, String> imageMap = new TreeMap<DimensionMap, String>();
	public TreeMap<DimensionMap, String> maskMap = new TreeMap<DimensionMap, String>();
	public TreeMap<DimensionMap, ROIPlus> roiMap = new TreeMap<DimensionMap, ROIPlus>();
	TreeMap<Integer, Integer> idToLabelMap = new TreeMap<Integer, Integer>();

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

	@ParameterMarker(uiOrder = 1, name = "'Whole Cell' mask channel value", description = "Which channel value of the mask image represents the whole cell that has a 1-to-1 mapping with the maxima points.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "WholeCell")
	String maskWholeCellChannelValue;

	@ParameterMarker(uiOrder = 2, name = "'Nuclear' mask channel value (optional)", description = "(Optional) If a nuclear mask exists and it is specified, additional nuanced calculations of Zernike features are provided (see comments in code).", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String maskNuclearChannelValue;

	//	@ParameterMarker(uiOrder = 3, name = "Image intensity offset", description = "Amount the images are offset from zero (will be subtracted before calculation)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "0.0")
	//	double offset;

	@ParameterMarker(uiOrder = 4, name = "** Compute Stats Features?", description = "Whether to quantify first order statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean stats;

	@ParameterMarker(uiOrder = 5, name = "** Compute 2D Geometric Features?", description = "Whether to quantify geometric statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean geometric;

	@ParameterMarker(uiOrder = 6, name = "** Compute 2D Haralick Features?", description = "Whether to quantify Haralick texture statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean haralick2D;

	@ParameterMarker(uiOrder = 7, name = "** Compute Histogram Features?", description = "Whether to quantify histogram statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean histogram;

	@ParameterMarker(uiOrder = 8, name = "** Compute Moments Features?", description = "Whether to quantify image moment statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean moments;

	@ParameterMarker(uiOrder = 9, name = "** Compute LBP Features?", description = "Whether to quantify linear binary pattern (LBP) statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean lbp;

	@ParameterMarker(uiOrder = 10, name = "** Compute Tamura Features?", description = "Whether to quantify Tamura statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean tamura;

	@ParameterMarker(uiOrder = 11, name = "** Compute Zernike Features?", description = "Whether to quantify Zernike shape statistics", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean zernike;

	@ParameterMarker(uiOrder = 12, name = "** Connectedness Features", description = "The structuring element or number of neighbors to require to be part of the neighborhood.", ui = MarkerConstants.UI_DROPDOWN, choices = {"4 Connected", "8 Connected" }, defaultChoice = 0)
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

	// Add a primary mask and secondary mask

	/////////// Define Outputs here ///////////

	@OutputMarker(uiOrder = 1, name = "Output CSV Table", type = MarkerConstants.TYPE_FILE, flavor = "", description = "Output in csv format (i.e., for Excel etc).", enabled = true)
	JEXData outputCSV;

	@OutputMarker(uiOrder = 1, name = "Output ARFF Table", type = MarkerConstants.TYPE_FILE, flavor = "", description = "Test table output (i.e., for Weka etc).", enabled = true)
	JEXData outputARFF;

	// Define threading capability here (set to 1 if using non-final static
	// variables shared between function instances).
	@Override
	public int getMaxThreads() {
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {

		if(!checkInputs())
		{
			// Something is wrong and need to return false;
			return false;
		}

		// Initialize: imageMap, maskMap, roiMap, total, count, percentage, nucExists
		this.initializeVariables();

		DimTable imageDimTable = imageData.getDimTable();
		DimTable maskDimTable = maskData.getDimTable();
		DimTable maskDimTable_NoChannel = maskDimTable.getSubTable(channelName);
		Dim maskChannelDim = maskDimTable.getDimWithName(channelName);
		int subcount = imageDimTable.getSubTable(maskDimTable_NoChannel.getMapIterator().iterator().next()).mapCount();
		this.total = maskDimTable.mapCount() * subcount;

		// For each whole cell mask
		for(DimensionMap mapMask_NoChannelTemp : maskDimTable_NoChannel.getMapIterator())
		{
			if (this.isCanceled()) { this.close(); return false; }

			this.mapMask_NoChannel = mapMask_NoChannelTemp;
			DimensionMap mapMask_WholeCell = this.mapMask_NoChannel.copyAndSet(channelName + "=" + maskWholeCellChannelValue);

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
			this.wholeCellMaskImage = JEXReader.getSingleImage(path);
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
			this.image = JEXReader.getSingleImage(imageMap.get(this.mapImage));
		}
	}

	public void setMaskToMeasure(DimensionMap mapMask)
	{
		Logs.log("Measuring mask: " + this.mapMask, this);
		this.mapMask = mapMask;
		this.mask = JEXReader.getSingleImage(maskMap.get(this.mapMask));
		this.maskLabeling = utils.getSubLabeling(this.wholeCellLabeling, this.mask);
		this.maskRegions = new LabelRegions<Integer>(this.maskLabeling);
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
		if(labelToGet == null)
		{
			this.wholeCellRegion = null;
			this.subCellRegion = null;
			this.subCellRegions = null;
			this.polygons = null;
		}
		else
		{
			this.wholeCellRegion = this.wholeCellRegions.getLabelRegion(labelToGet);
			this.subCellRegion = this.maskRegions.getLabelRegion(labelToGet);
			this.subCellRegions = this.getSubRegions(this.wholeCellRegion, this.mask);
			this.polygons = new Vector<Polygon>();
			for(LabelRegion<Integer> region : this.subCellRegions)
			{
				this.polygons.add(utils.getPolygonFromBoolean(region));
			}
		}
	}

	public boolean quantifyFeatures(String maskChannelName, boolean firstTimeThrough)
	{
		this.setMaskToMeasure(this.mapMask_NoChannel.copyAndSet(this.channelName + "=" + maskChannelName));
		for(IdPoint p : this.maxima.getPointList())
		{
			// Set geometries and quantify
			this.setRegionsAndPolygon(p.id);
			if(this.subCellRegion != null)
			{
				if(!this.putFeatures(firstTimeThrough))
					return false;
			}

		}
		return true;
	}

	private boolean putFeatures(boolean firstTimeThrough)
	{
		if(this.subCellRegion == null || this.subCellRegion.size() <= 1)
		{
			return true;
		}

		if(firstTimeThrough)
		{
			// Then quantify geometric features
			if(this.mapMask.get(this.channelName).equals(this.maskNuclearChannelValue))
			{
				// Store the nuclear Info
				this.nuclearInfo.put(this.pId, new Pair<Double,RealLocalizable>(this.getEquivalentRadius(this.subCellRegion), this.subCellRegion.getCenterOfMass()));
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

	public boolean putIntensityFeatures(boolean firstTimeThrough)
	{
		DimensionMap mapM_Intensity = this.mapMask_NoChannel.copyAndSet("MaskChannel=" + mapMask.get(channelName));
		String imageChannelValue = this.mapImage.get(channelName);
		if(imageChannelValue == null)
		{
			imageChannelValue = "None";
		}
		mapM_Intensity.put("ImageChannel", imageChannelValue);

		IterableInterval<T> ii = Regions.sample(this.subCellRegion, this.image); //Views.offsetInterval(this.image, this.wholeCellRegion));

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

		this.putZernike(mapM_Intensity, ii, new Circle(this.subCellRegion.getCenterOfMass(), this.zernikeFixedDiameter/2.0), "_THISwFIXED", firstTimeThrough); 
		if (this.isCanceled()) { this.close(); return false;}
		this.putZernike(mapM_Intensity, ii, utils.getCircle(this.subCellRegion, this.subCellRegion.getCenterOfMass()), "_THISwSEC", firstTimeThrough);
		if (this.isCanceled()) { this.close(); return false;}

		//	if(NucMASK)
		//	
		//		QuantifyZernike (region = nuc, circle = paddedNucER@nucCOM)

		if(mapMask.get(channelName).equals(this.maskNuclearChannelValue) && this.nuclearInfo.get(this.pId) != null)
		{
			this.putZernike(mapM_Intensity, ii, new Circle(this.subCellRegion.getCenterOfMass(), this.nuclearInfo.get(this.pId).p1 * (this.zernikeFixedDiameter / this.zernikeNucDiameter)), "_NUCwPADDEDNUC", firstTimeThrough);
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
			this.putZernike(mapM_Intensity, ii, utils.getCircle(this.subCellRegion, this.nuclearInfo.get(this.pId).p2), "_NUCwSEC", firstTimeThrough);
			if (this.isCanceled()) { this.close(); return false;}
			this.putZernike(mapM_Intensity, ii, new Circle(this.nuclearInfo.get(this.pId).p2, this.nuclearInfo.get(this.pId).p1 * (this.zernikeFixedDiameter / this.zernikeNucDiameter)), "_NUCwPADDEDNUC", firstTimeThrough);
			if (this.isCanceled()) { this.close(); return false;}
			this.putDNZernike(mapM_Intensity, ii, new Circle(this.nuclearInfo.get(this.pId).p2, this.getEquivalentRadius(this.subCellRegion)), new Circle(this.nuclearInfo.get(pId).p2, this.getEquivalentRadius(this.subCellRegion)), firstTimeThrough);
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
			this.putZernike(mapM_Intensity, ii, utils.getCircle(this.subCellRegion, this.nuclearInfo.get(this.pId).p2), "_NUCwSEC", firstTimeThrough);
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

		RandomAccessibleInterval<T> vals = Views.offsetInterval(utils.cropRealRAI(this.subCellRegion, Views.offsetInterval(this.image, this.wholeCellRegion)), this.subCellRegion);

		//utils.showRealII(Views.flatIterable(vals));
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
				Logs.log("Encountered an empty list of polygons for geometric2d features. id:" + this.pId + ", label:" + this.subCellRegion.getLabel() + ", mask:" + this.mapMask,
						FeatureExtraction.class);
			}
			if (opGeometric == null) {
				opGeometric = IJ2PluginUtility.ij().op().op(Geometric2DFeatureSet.class, this.polygons.get(0));
			}
			
			int i = 1;
			for(Polygon p : polygons)
			{
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
				this.write(newMap, (double) this.subCellRegions.get(i-1).size());
				
				RealPoint wholeCellRelativeCenterOfMass = this.getWholeCellRelativeCenterOfMass();
				RealLocalizable subCellRegionCenterOfMass = this.subCellRegions.get(i-1).getCenterOfMass();
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
	public void putStats(DimensionMap mapM, IterableInterval<T> vals)
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
	public void putHaralick2D(DimensionMap mapM, IterableInterval<T> vals)
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
	public void putTamura(DimensionMap mapM, RandomAccessibleInterval<T> vals)
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
	public void putHistogram(DimensionMap mapM, IterableInterval<T> vals)
	{
		if (histogram) {
			if (opHistogram == null) {
				opHistogram = IJ2PluginUtility.ij().op().op(HistogramFeatureSet.class, vals, histogramBins);
			}
			Map<NamedFeature, LongType> ret = opHistogram.calculate(vals);
			for (Entry<NamedFeature, LongType> result : ret.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.write(newMap, result.getValue().getRealDouble());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void putLBPHistogram(DimensionMap mapM, RandomAccessibleInterval<T> vals)
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
	public void putMoments(DimensionMap mapM, IterableInterval<T> vals)
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
	public void putZernike(DimensionMap mapM, IterableInterval<T> vals, Circle circle, String suffix, boolean firstTimeThrough)
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
				this.wrapWriter.write(newMap, result.getValue().get());
			}
			if(firstTimeThrough)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=ZernikeCircleX" + suffix);
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.wrapWriter.write(newMap, circle.getCenter().getDoublePosition(0));
				newMap = mapM.copyAndSet("Measurement=ZernikeCircleY" + suffix);
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.wrapWriter.write(newMap, circle.getCenter().getDoublePosition(1));
				newMap = mapM.copyAndSet("Measurement=ZernikeCircleR" + suffix);
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.wrapWriter.write(newMap, circle.getRadius());
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void putDNZernike(DimensionMap mapM, IterableInterval<T> vals, Circle innerCircle, Circle outerCircleAtEquivalentRadius, boolean firstTimeThrough)
	{
		if(zernike && this.nucExists)
		{
			if (this.opDNZernike == null) {
				opDNZernike = IJ2PluginUtility.ij().op().op(DoubleNormalizedZernikeFeatureSet.class, vals, null, zernikeMomentMin, zernikeMomentMax, innerCircle, outerCircleAtEquivalentRadius, 0.3);
			}

			Circle outerCircle = new Circle(outerCircleAtEquivalentRadius.getCenter(), 1.5*outerCircleAtEquivalentRadius.getRadius());

			// Set the enclosing circle for this cell
			opDNZernike.setEnclosingCircles(innerCircle, outerCircle);

			Map<NamedFeature, DoubleType> results = opDNZernike.calculate(vals);
			for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
				DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.wrapWriter.write(newMap, result.getValue().get());
			}
			if(firstTimeThrough)
			{
				DimensionMap newMap = mapM.copyAndSet("Measurement=DNZernikeInnerCircleX");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.wrapWriter.write(newMap, innerCircle.getCenter().getDoublePosition(0));
				newMap = mapM.copyAndSet("Measurement=DNZernikeInnerCircleY");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.wrapWriter.write(newMap, innerCircle.getCenter().getDoublePosition(1));
				newMap = mapM.copyAndSet("Measurement=DNZernikeInnerCircleR");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.wrapWriter.write(newMap, innerCircle.getRadius());
				newMap = mapM.copyAndSet("Measurement=DNZernikeOuterCircleX");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.wrapWriter.write(newMap, outerCircle.getCenter().getDoublePosition(0));
				newMap = mapM.copyAndSet("Measurement=DNZernikeOuterCircleY");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.wrapWriter.write(newMap, outerCircle.getCenter().getDoublePosition(1));
				newMap = mapM.copyAndSet("Measurement=DNZernikeOuterCircleR");
				newMap.put("Id", "" + this.pId);
				newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
				this.wrapWriter.write(newMap, outerCircle.getRadius());
			}
		}
	}

	public void close()
	{
		Pair<JEXData, JEXData> results = this.wrapWriter.close();
		outputCSV = results.p1;
		outputARFF = results.p2;
	}

	public void write(DimensionMap map, Double value)
	{
		this.wrapWriter.write(map, value);
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

	public void initializeVariables()
	{
		imageMap = new TreeMap<>();
		if (imageData != null) {
			imageMap = ImageReader.readObjectToImagePathTable(imageData);
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
	}
}
