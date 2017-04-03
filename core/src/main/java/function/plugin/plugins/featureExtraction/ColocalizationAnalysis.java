// Define package name as "plugins" as show here
package function.plugin.plugins.featureExtraction;

import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.SingleUserDatabase.JEXReader;
import function.ops.featuresets.wrappers.WriterWrapper;
import function.ops.stats.DefaultPearsonsCorrelationCoefficient;
import function.ops.stats.DefaultSpearmansRankCorrelationCoefficient;
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
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.DoubleType;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

// Specify plugin characteristics here
@Plugin(
		type = JEXPlugin.class,
		name = "Colocalization Analysis",
		menuPath = "Feature Extraction",
		visible = true,
		description = "Function for calculating colocalization of different channels for each cell."
		)
public class ColocalizationAnalysis<T extends RealType<T>> extends JEXPlugin {

	public final String CIRCLE_SMALLEST_ENCLOSING = "Smallest Enclosing", CIRCLE_SMALLEST_AT_CENTER_OF_MASS = "Smallest Enclosing Circle at Center of Mass";

	public WriterWrapper wrapWriter = new WriterWrapper();

	private Img<UnsignedByteType> wholeCellMaskImage;
	private Img<UnsignedByteType> mask;
	private Img<T> image1;
	private Img<T> image2;
	private ImgLabeling<Integer,IntType> wholeCellLabeling;
	private ImgLabeling<Integer,IntType> maskParentLabeling;
	private LabelRegions<Integer> wholeCellRegions;
	private LabelRegions<Integer> maskParentRegions;
	private ROIPlus maxima;
//	private LabelRegion<Integer> wholeCellRegion;
	private LabelRegion<Integer> combinedSubCellRegion;
	// private LabelRegion<Integer> majorSubCellRegion; UNUSED IN COLOC
	private DimensionMap mapMask, mapImage1, mapImage2, mapMask_NoChannel;
	private Integer pId;

	public DefaultPearsonsCorrelationCoefficient<T> opR = null;
	public DefaultSpearmansRankCorrelationCoefficient<T> opRho = null;

	public int total = 0, count = 0;
	public int percentage = 0;

	public TreeMap<DimensionMap, String> imageMap = new TreeMap<DimensionMap, String>();
	public TreeMap<DimensionMap, String> maskMap = new TreeMap<DimensionMap, String>();
	public TreeMap<DimensionMap, ROIPlus> roiMap = new TreeMap<DimensionMap, ROIPlus>();
	TreeMap<Integer, Integer> idToLabelMap = new TreeMap<Integer, Integer>();

	FeatureUtils utils = new FeatureUtils();

	// Define a constructor that takes no arguments.
	public ColocalizationAnalysis() {
	}

	// ///////// Define Inputs here ///////////

	@InputMarker(uiOrder = 1, name = "Images to Measure", type = MarkerConstants.TYPE_IMAGE, description = "Intensity images", optional = false)
	JEXData imageData;

	@InputMarker(uiOrder = 3, name = "Masks", type = MarkerConstants.TYPE_IMAGE, description = "Mask images", optional = false)
	JEXData maskData;

	@InputMarker(uiOrder = 4, name = "Maxima", type = MarkerConstants.TYPE_ROI, description = "Maxima ROI", optional = false)
	JEXData roiData;

	// ///////// Define Parameters here ///////////

	@ParameterMarker(uiOrder = 0, name = "Mask and Image channel dim name", description = "Channel dimension name in mask data.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "Channel")
	String channelName;

	@ParameterMarker(uiOrder = 1, name = "'Whole Cell' mask channel value", description = "Which channel value of the mask image represents the whole cell that has a 1-to-1 mapping with the maxima points.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "WholeCell")
	String maskWholeCellChannelValue;

	@ParameterMarker(uiOrder = 2, name = "** Connectedness Features", description = "The structuring element or number of neighbors to require to be part of the neighborhood.", ui = MarkerConstants.UI_DROPDOWN, choices = {"4 Connected", "8 Connected" }, defaultChoice = 0)
	String connectedness;
	
	@ParameterMarker(uiOrder = 3, name = "Transform Data to 'Similarity'", description = "This takes the correlation metrics and transoforms them using Similarity=ln((1+R)/(1-R)). Recommended.", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean transform;

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
		if(imageDimTable.getDimWithName(channelName) == null || maskDimTable.getDimWithName(channelName) == null)
		{
			JEXDialog.messageDialog("The channel dimension name provided is not found in both the image and mask image objects. Aborting.", this);
			return false;
		}
		DimTable maskDimTable_NoChannel = maskDimTable.getSubTable(channelName);
		Dim maskChannelDim = maskDimTable.getDimWithName(channelName);
		int subcount = imageDimTable.getSubTable(maskDimTable_NoChannel.getMapIterator().iterator().next()).mapCount();
		this.total = maskDimTable.mapCount() * subcount;

		Dim imageChannelDim = imageDimTable.getDimWithName(channelName);
		Vector<Pair<String,String>> channelCombos = this.getChannelPermutations(imageChannelDim);
		if(channelCombos == null)
		{
			return false;
		}

		// For each whole cell mask
		for(DimensionMap mapMask_NoChannelTemp : maskDimTable_NoChannel.getMapIterator())
		{
			if (this.isCanceled()) { this.close(); return false; }

			this.mapMask_NoChannel = mapMask_NoChannelTemp;
			DimensionMap mapMask_WholeCell = this.mapMask_NoChannel.copyAndSet(channelName + "=" + maskWholeCellChannelValue);

			this.setWholeCellMask(mapMask_WholeCell);
			if(this.wholeCellMaskImage == null)
			{
				continue;
			}
			if (this.isCanceled()) { this.close(); return false; }

			this.setMaximaRoi(mapMask_WholeCell);

			this.setIdToLabelMap(mapMask_WholeCell);
			if (this.isCanceled()) { this.close(); return false; }

			// Get the subDimTable corresponding to the images for this whole cell mask (i.e., corresponding image but all channels)
			DimTable imageSubsetTable = imageDimTable.getSubTable(this.mapMask_NoChannel);

			// Loop over the image channel combinations to measure
			for(Pair<String,String> channelCombo : channelCombos)
			{
				// Set the images to measure
				Pair<DimensionMap,DimensionMap> combo = this.getMapsForCombo(imageSubsetTable, channelCombo);
				this.setImagesToMeasure(combo.p1, combo.p2);

				// Then quantify other masks
				for(String maskChannelValue : maskChannelDim.dimValues)
				{
					if(this.isCanceled()) return false;
					if(!this.quantifyColocalizations(maskChannelValue))
						return false;
					this.updateStatus();
				}
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
		if(path == null)
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

	public void setImagesToMeasure(DimensionMap map1, DimensionMap map2)
	{
		Logs.log("Measuring image: " + this.mapImage1 + " & " + this.mapImage2, this);
		this.mapImage1 = map1;
		this.mapImage2 = map2;
		this.image1 = JEXReader.getSingleImage(imageMap.get(this.mapImage1));
		this.image2 = JEXReader.getSingleImage(imageMap.get(this.mapImage2));
	}

	//	public void setMaskToMeasure(DimensionMap mapMask)
	//	{
	//		Logs.log("Measuring mask: " + this.mapMask, this);
	//		this.mapMask = mapMask;
	//		this.mask = JEXReader.getSingleImage(maskMap.get(this.mapMask));
	//	}
	public void setMaskToMeasure(DimensionMap mapMask)
	{
		Logs.log("Measuring mask: " + this.mapMask, this);
		this.mapMask = mapMask;
		this.mask = JEXReader.getSingleImage(maskMap.get(this.mapMask));
		this.maskParentLabeling = utils.applyLabeling(this.wholeCellLabeling, this.mask);
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
		if(this.wholeCellRegions == null)
		{
			System.out.println("lah");
		}
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

	private void setRegions(int pId)
	{
		this.pId = pId;
		Integer labelToGet = this.idToLabelMap.get(pId);
		if(labelToGet == null)
		{
			// this.wholeCellRegion = null;  // UNUSED IN COLOC
			this.combinedSubCellRegion = null;
			// this.majorSubCellRegion = null; // UNUSED IN COLOC
		}
		else
		{
			// this.wholeCellRegion = this.wholeCellRegions.getLabelRegion(labelToGet); // UNUSED IN COLOC
			this.combinedSubCellRegion = this.maskParentRegions.getLabelRegion(labelToGet);
			// this.majorSubCellRegion = this.getMajorSubRegion(this.wholeCellRegion, this.mask); // UNUSED IN COLOC
		}
	}

	public boolean quantifyColocalizations(String maskChannelName)
	{
		this.setMaskToMeasure(this.mapMask_NoChannel.copyAndSet(this.channelName + "=" + maskChannelName));
		for(IdPoint p : this.maxima.getPointList())
		{
			// Set geometries and quantify
			this.setRegions(p.id);
			if(this.combinedSubCellRegion != null)
			{
				if(!this.putResults())
					return false;
			}
		}
		return true;
	}

	private boolean putResults()
	{
		if(this.combinedSubCellRegion == null || this.combinedSubCellRegion.size() <= 1)
		{
			return true;
		}

		if(!this.putColocalizatoinValues())
			return false;

		return true;

	}

	public boolean putColocalizatoinValues()
	{
		DimensionMap mapM_Intensity = this.mapMask_NoChannel.copyAndSet("MaskChannel=" + mapMask.get(channelName));
		String image1ChannelValue = this.mapImage1.get(channelName);
		String image2ChannelValue = this.mapImage2.get(channelName);

		mapM_Intensity.put("ImageChannel", image1ChannelValue + "_" + image2ChannelValue);

		// TODO: Check whether I need to offset any more because we are now getting subregions differently.
		//		RandomAccessibleInterval<T> subImage1 = Views.offsetInterval(this.image1, this.wholeCellRegion);
		//		RandomAccessibleInterval<T> subImage2 = Views.offsetInterval(this.image2, this.wholeCellRegion);
		//		RandomAccessibleInterval<T> subRegionImage = utils.cropRealRAI(this.subCellRegion, subImage2);
		Cursor<Void> c = this.combinedSubCellRegion.cursor();
		//IterableRegion<BoolType> ir = utils.intersectRegions(this.wholeCellRegion, this.subCellRegion);

		//		utils.show(subImage1);
		//		utils.show(subImage2);
		//		utils.showVoidII(this.subCellRegion, this.wholeCellRegion);

		this.putPearson(mapM_Intensity, this.image1, this.image2, c);

		if(this.isCanceled()) return false;

		return true;
	}

	public LabelRegion<Integer> getMajorSubRegion(LabelRegion<Integer> region, Img<UnsignedByteType> mask)
	{
		ImgLabeling<Integer, IntType> labeling = utils.getLabelingInRegion(region, mask, connectedness.equals("4 Connected"));
		LabelRegions<Integer> subRegions = new LabelRegions<>(labeling);
		long maxSize = 1;
		LabelRegion<Integer> majorSubRegion = null;
		for (LabelRegion<Integer> subRegion : subRegions)
		{
			if(subRegion.size() > maxSize)
			{
				majorSubRegion = subRegion;
				maxSize = subRegion.size();
			}
		}
		return majorSubRegion;
	}

	@SuppressWarnings("unchecked")
	public void putPearson(DimensionMap mapM, RandomAccessibleInterval<T> subImage1, RandomAccessibleInterval<T> subImage2, Cursor<Void> maskCursor)
	{
		if (opR == null) {
			opR = IJ2PluginUtility.ij().op().op(DefaultPearsonsCorrelationCoefficient.class, new Pair<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>>(), maskCursor);
			
		}
		if (opRho == null) {
			opRho = IJ2PluginUtility.ij().op().op(DefaultSpearmansRankCorrelationCoefficient.class, new Pair<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>>(), maskCursor);
		}
		

		DoubleType result_R = opR.calculate(new Pair<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>>(subImage1, subImage2), maskCursor);
		DoubleType result_Rho = opRho.calculate(new Pair<RandomAccessibleInterval<T>, RandomAccessibleInterval<T>>(subImage1, subImage2), maskCursor);

		String measurement = "Measurement=net.imagej.ops.Ops$Stats$PearsonsCorrelationCoefficient";
		if(transform)
		{
			measurement = "Measurement=net.imagej.ops.Ops$Stats$PearsonsCorrelationCoefficient.Similarity";
		}
		DimensionMap newMap = mapM.copyAndSet(measurement);
		newMap.put("Id", "" + this.pId);
		newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
		if(transform)
		{
			this.write(newMap, Math.log((1.0+result_R.get())/(1.0-result_R.get())));
		}
		else
		{
			this.write(newMap, result_R.get());
		}
		
		measurement = "Measurement=net.imagej.ops.Ops$Stats$SpearmansRankCorrelationCoefficient";
		if(transform)
		{
			measurement = "Measurement=net.imagej.ops.Ops$Stats$SpearmansRankCorrelationCoefficient.Similarity";
		}
		newMap = mapM.copyAndSet(measurement);
		newMap.put("Id", "" + this.pId);
		newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
		if(transform)
		{
			this.write(newMap, Math.log((1.0+result_Rho.get())/(1.0-result_Rho.get())));
		}
		else
		{
			this.write(newMap, result_Rho.get());
		}

		//		for (Entry<NamedFeature, DoubleType> result : results.entrySet()) {
		//			DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getKey().getName());
		//			newMap.put("Id", "" + this.pId);
		//			newMap.put("Label", "" + this.idToLabelMap.get(this.pId));
		//			this.write(newMap, result.getValue().get());
		//		}
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
		if (maskData == null) {
			JEXDialog.messageDialog("Colocalization: Returning false. NEED to have a mask.");
			return false;
		}
		if (roiData == null) {
			JEXDialog.messageDialog(
					"Colocalization: Returning false. NEED to have an roi that defines the id of each cell.");
			return false;
		}
		if (imageData == null) {
			JEXDialog.messageDialog(
					"Colocalization: Returning false. NEED to define an image to quantify if you want to use intensity based features such as first order, haralick2D, histogram, and moment statistics.");
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
	}

	private Vector<Pair<String,String>> getChannelPermutations(Dim channelDim)
	{
		Vector<Pair<String,String>> ret = new Vector<>();
		if(channelDim.size() < 2)
		{
			JEXDialog.messageDialog("There must be at least to image channels to perform colocalization. Aborting.");
			return null;
		}
		for(int i = 0; i < channelDim.size()-1; i++)
		{
			for(int j = i + 1; j < channelDim.size(); j++)
			{
				ret.add(new Pair<String,String>(channelDim.values().get(i), channelDim.values().get(j)));
			}
		}
		return ret;
	}

	private Pair<DimensionMap,DimensionMap> getMapsForCombo(DimTable subTable, Pair<String,String> combo)
	{
		DimensionMap map1 = null;
		DimensionMap map2 = null;
		for(DimensionMap map : subTable.getMapIterator())
		{
			if(map.get(this.channelName).equals(combo.p1))
			{
				map1 = map;
			}
			if(map.get(this.channelName).equals(combo.p2))
			{
				map2 = map;
			}
		}
		return new Pair<DimensionMap,DimensionMap>(map1, map2);
	}
}
