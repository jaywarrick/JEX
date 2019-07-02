package function.plugin.plugins.imageProcessing;

import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.old.JEX_ImageFilters;
import ij.ImagePlus;
import ij.plugin.filter.BackgroundSubtracter;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FHT;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.utilities.ImageUtility;
import logs.Logs;
import miscellaneous.Pair;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following
 * instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions
 * 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile
 * and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these
 * can be found in the main JEXperiment folder. These API provide methods to
 * retrieve data from these objects, create new objects and handle the data they
 * contain.
 * 
 */

@Plugin(type = JEXPlugin.class, name = "TIE Phase Calculator", menuPath = "Image Processing", visible = true, description = "Take a z-stack of brightfield images and calculate the phase shift of light through the sample.")
public class TIEPhaseCalculator extends JEXPlugin {

	public TIEPhaseCalculator() {
	}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder = 1, name = "Image", type = MarkerConstants.TYPE_IMAGE, description = "Image to be adjusted.", optional = false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder = 0, name = "Simple Algorithm?", description = "This assumes a uniform illumination of the image, but is faster.", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean simple;

	@ParameterMarker(uiOrder = 1, name = "dI/dz Calc", description = "Should dI/dz be calculated using successive frames, (Z(n)-Z(n-1))/dz, or skip a frame, (Z(n+1)-Z(n-1))/(2dz).", ui = MarkerConstants.UI_DROPDOWN, choices = {
			"2-Plane Method", "3-Plane Method" }, defaultChoice = 0)
	String dIdzAlg;

	@ParameterMarker(uiOrder = 2, name = "Z Dim Name", description = "Name of the 'Z' dimension for this image object.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "Z")
	String zDimName;
	
	@ParameterMarker(uiOrder = 3, name = "z-GAP", description = "Should the z-planes being used be separated by a gap (e.g., 0-GAP = planes 1,2,3 then 2,3,4 vs 1-GAP = planes 1,3,5 then 2,4,6)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "0")
	int zGAP;

	@ParameterMarker(uiOrder = 4, name = "z-step per plane [µm]", description = "What is the step distance between z planes (total distance between analyzed planes will be adjusted for different gaps, so just enter distance between captured planes).", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "1.0")
	float dz;
	
	@ParameterMarker(uiOrder = 5, name = "Camera Pixel Size [µm]", description = "The camera pixel size in microns. This is needed for 'absolute' quantitation and is not necessary if all you want is 'relative' quantitation (e.g., something is twice the other thing).", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "6.45")
	float pixelSize;

	@ParameterMarker(uiOrder = 6, name = "Image Magnification", description = "The magnification at which the images were taken. Used with the camera pixel size to determine the pixel width. (width = camera pixel size / magnification)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "20.0")
	float mag;

	@ParameterMarker(uiOrder = 7, name = "Wavelength of Light [nm]", description = "This is the spectrally weighted average wavelength of the bright-field illumination. This is needed for 'absolute' quantitation and is not necessary if all you want is 'relative' quantitation (e.g., something is twice the other thing).", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "600")
	float lambda;

//	@ParameterMarker(uiOrder = 8, name = "Regularization Parameter (~0-2)", description = "The regularization parameter for the equation is essentially the cuttoff a high-pass filter that takes values that typically range from 0.01-1 but needs to be >=0. This can generally be set low (0.01) and the resulting phase image can be filtered to 'remove background'.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "0.01")
//	float r;

//	@ParameterMarker(uiOrder = 9, name = "Threshold Parameter", description = "The threshold parameter is expressed as a fraction of the max image intensity. All pixels below this min value are set to the min value to avoid dividing by 0. Typically 0.01 or 0.001.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "0.001")
	float thresh = 0.001f;

//	@ParameterMarker(uiOrder = 10, name = "Filter and Scale Result?", description = "Should the result be FFT filtered to remove noise and then scaled for saving at a particular bit depth?", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean doFilteringAndScaling = true;

	@ParameterMarker(uiOrder = 11, name = "FFT: Feature Size Cutoff [pixels]", description = "The filter removes low frequency fluctuations (e.g., background) and keeps small features (higher frequency items). What is the feature size cutoff? [pixels].", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "250")
	double filterLargeDia;
	
//	@ParameterMarker(uiOrder = 12, name = "Filter (WM): Kernal Outer Weighting Factor", description="How much weight should the outer portion of the kernel be given relative to the inner portion (Kernel = Gaussian * (1-Gaussian)^factor). Typically 0 (standard Gaussian) to 5 (weighted to outer portion), but can go higher with diminishing impact.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0")
//	double outerWeighting=0;
	
//	@ParameterMarker(uiOrder = 12, name = "Filter (FFT): Do FFT-base filtering step?", description="Should an FFT filter be applied with the specified size cutoff.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
//	boolean doFFT;
	
//	@ParameterMarker(uiOrder = 12, name = "Filter (WMF): Do weighted mean filtering step?", description="Should the weighted mean filtering step be performed at all?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
//	boolean doWMF;
//	
//	@ParameterMarker(uiOrder = 13, name = "Filter (WMF): Gaussian Filter Radius", description="Sigma of the gaussian filter used with WMF. Optionally supply a second argument (CSV list, e.g., '50,300') to specify how far out the gaussian kernel should extend in pixels. The second radius must be larger than the first.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="20,350")
//	String radiiWMF;
//	
//	@ParameterMarker(uiOrder = 14, name = "Filter (WMF): Variance Filter Radius", description="Radius of the variance filter used with WMF to detect background pixels. Typically 2 to 4", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4.0")
//	double radiusVarWMF;
//	
//	@ParameterMarker(uiOrder = 15, name = "Filter (WMF): Subtraction Sharpness Parameter", description="How sharp a difference should there be between the weights of background and foreground pixels. A higher number causes a sharper transition in weighting. A sharper transition means less dark shadow around bright features (typically 0.5-5).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4.0")
//	double subtractionPower;
//	
//	@ParameterMarker(uiOrder = 16, name = "Filter (WMF): Zero the Background", description="Typically the mean background is half the bit depth range of the final image. This subtracts that value to set the mean background to zero.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean zero = true;
//	
//	@ParameterMarker(uiOrder = 17, name = "Filter (WMF): Save Thresholded Filter Result?", description = "Should a thresholded image be generated from the filtered and scaled phase image?", ui = MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean saveThresholdImage = false;
//	
//	@ParameterMarker(uiOrder = 18, name = "Filter (WMF): Threshold Sharpness Parameter", description="How sharp a difference should there be between the weights of background and foreground pixels. A higher number causes a sharper transition in weighting. A sharper transition means less dark shadow around bright features (typically lower than used for subtraction to create better defined edges, 0.5-5).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double thresholdPower = 2;
//
//	@ParameterMarker(uiOrder = 19, name = "Filter (WMF): Threshold: Sigma", description = "How many sigma above background should the threshold cutoff be? Use 0 to save the 'signal-to-noise' image which can be then thresholded or used to determine the best sigma.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "0.0")
	double sigma;
	
	@ParameterMarker(uiOrder = 12, name = "Filter (RBF): Do rolling ball filtering (RBF) step?", description="Should the rolling ball filtering step be performed at all?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean doRBF;
	
	@ParameterMarker(uiOrder = 13, name = "Filter (RBF): Rolling Ball Radius", description="Radius of the rolling ball filter in pixels. Recommend to use parabaloid with very small radius (~0.1-0.25). Rolling ball version should use normal radius similar to feature size cutoff specified above.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5")
	double radiusRBF;
	
//	@ParameterMarker(uiOrder=2, name="Filter (RBF): Light background?", description="Generally false for fluorescent images and true for bright-field etc.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean lightBackground = false;

//	@ParameterMarker(uiOrder=3, name="Filter (RBF): Create background (don't subtract)?", description="Output an 'image' of the background instead of subtracting from the original and outputing the result?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean createBackground = false;

	@ParameterMarker(uiOrder = 14, name = "Filter (RBF): Sliding parabaloid?", description="Parabaloid is generally a little faster and has less artifacts than rolling ball.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean paraboloid;

	@ParameterMarker(uiOrder = 15, name = "Filter (RBF): Do presmoothing?", description="Should a 3x3 mean filter be applied prior to rolling ball subtraction (good for speckly/noisy images)", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean presmooth;
	
	@ParameterMarker(uiOrder = 16, name = "Filter (Hybrid Mask): Do hybrid masked BG subtraction?", description="Overides selections for RBF. A mask is made from the variance weights as well as from the RBF image (lowest pixels in RBF get high weight) and unioned. Those weights are used to inform a weighted mean filter.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean doHMF;
	
	@ParameterMarker(uiOrder = 17, name = "Filter (Hybrid Mask): RBF Percentile?", description="Select the lowest X percent pixels from an RBF filtered result to help inform WMF background subtraction. Typical = 0.5%. Set to < 0 to ignore RBF points.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.5")
	double HMFPercentile;
	
	@ParameterMarker(uiOrder = 18, name = "Filter (Hybrid Mask): Variance Weighting Filter Radius", description="Radius of the variance weighting filter used to detect background pixels.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4.0")
	double radiusVariance;
	
	@ParameterMarker(uiOrder = 19, name = "Filter (Hybrid Mask): Decay Filter Radius", description="Radius of the power decay filter (1/(1+(x/radius)^power)). The radius at which weights drop to 0.5 of max. Typically 2-50", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5")
	double radiusHMF;
	
	@ParameterMarker(uiOrder = 20, name = "Filter (Hybrid Mask): Decay Filter Power", description="Radius of the power decay filter (1/(1+(x/radius)^power)). Typically 2-5", ui=MarkerConstants.UI_TEXTFIELD, defaultText="3.5")
	double powerHMF;	

//	// @ParameterMarker(uiOrder=11, name="FFT Post-Filter: Min Size", description="The smallest features to keep [pixels].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
//	double filterSmallDia = 0;

//	// @ParameterMarker(uiOrder=2, name="Filter: DC Component?", description="Should the 'DC component' or constant background be subtracted?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
//	boolean filterDC = true;

//	// @ParameterMarker(uiOrder=3, name="Suppress Stripes", description="Should stripes be suppressed and, if so, which direction should be suppressed.", ui=MarkerConstants.UI_DROPDOWN, choices={"None","Horizontal","Vertical"}, defaultChoice=0)
//	String choiceDia = "None";

//	// @ParameterMarker(uiOrder=4, name="Tolerane of Direction", description="Tolerance of direction for stripe suppression [%].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5.0")
//	double toleranceDia = 5.0;

//	// @ParameterMarker(uiOrder=5, name="Save filter?", description="Should the filter in the frequency domain be saved as an image?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
//	boolean saveFilter = false;

//	// @ParameterMarker(uiOrder=6, name="Autoscale result?", description="Should the result be automatically scaled?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
//	boolean doScalingDia = false;

	// @ParameterMarker(uiOrder=7, name="Saturate autoscaling?", description="If autoscaled, should the result be saturated (1% at tails) to better fill the dynamic range?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean saturateDia = false;

	@ParameterMarker(uiOrder = 26, name = "Scale: Output Bit Depth", description = "Depth of the outputted image for all channels.", ui = MarkerConstants.UI_DROPDOWN, choices = {
			"8", "16", "32" }, defaultChoice = 1)
	int bitDepth;

	@ParameterMarker(uiOrder = 27, name = "Scale: +/- Scale", description = "The result will be scaled such that -Scale to +Scale in the initial phase result will be scaled to fill the range of the bit depth selected. Therefore 0 will be the middlest value of the final image range. (32-bit results in no scaling)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "25.0")
	double scale;

	@ParameterMarker(uiOrder = 28, name = "Tiles: Rows", description = "If desired, the images can be split into tiles before processing by setting the number of tile rows here to > 1.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "1")
	int rows;

	@ParameterMarker(uiOrder = 29, name = "Tiles: Cols", description = "If desired, the images can be split into tiles before processing by setting the number of tile cols here to > 1.", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "1")
	int cols;

	@ParameterMarker(uiOrder = 30, name = "Tiles: Overlap", description = "Set the percent overlap of the tiles", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "1.0")
	double overlap;

	@ParameterMarker(uiOrder = 31, name = "Exclusion Filter DimTable", description = "Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder = 1, name = "Phase", type = MarkerConstants.TYPE_IMAGE, flavor = "", description = "The resultant phase image", enabled = true)
	JEXData output;

	@OutputMarker(uiOrder = 2, name = "SNR Phase Mask", type = MarkerConstants.TYPE_IMAGE, flavor = "", description = "Mask made from the the phase image, locally thresholded using the 'weighted mean filter' method.", enabled = true)
	JEXData mask;

	@Override
	public int getMaxThreads() {
		return 10;
	}

//	public double sigmaWMF = 0;
//	public Double radiusWMF = null;
	@Override
	public boolean run(JEXEntry optionalEntry) {
		// Validate the input data
		if (imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE)) {
			return false;
		}

		if (this.rows < 1 || this.cols < 1) {
			JEXDialog.messageDialog("The number of tile rows and cols must both be >= 1. Aborting.", this);
			return false;
		}
		
//		CSVList radiiWMFList = new CSVList(this);
//		this.sigmaWMF = Double.parseDouble(radiiWMFList.get(0));
//		if(radiiWMFList.size() > 1)
//		{
//			this.radiusWMF = Double.parseDouble(radiiWMFList.get(1));
//		}
//		if(this.radiusWMF == null)
//		{
//			this.radiusWMF = 2.0;
//		}
		

		boolean successive = this.dIdzAlg.equals("2-Plane Method");

//		if (successive && !this.simple) {
//			JEXDialog.messageDialog(
//					"The box for the 'simple' method must be checked if using successive frames to calculate dI/dz. Aborting",
//					this);
//			return false;
//		}

		DimTable filterTable = new DimTable(this.filterDimTableString);

		// Run the function
		TreeMap<DimensionMap, String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap, String> outputImageMap = new TreeMap<DimensionMap, String>();
		TreeMap<DimensionMap, String> maskMap = new TreeMap<DimensionMap, String>();
		int count = 0, percentage = 0;
		String tempPath;

		// For each dimension map without a Z, iterate over Z
		Dim zDim = imageData.getDimTable().getDimWithName(this.zDimName);
		if (zDim == null) {
			JEXDialog.messageDialog(
					"No Z dimension with the provided name could be found in the provided image object. Aborting",
					this);
			return false;
		}
		Dim zFilter = filterTable.removeDimWithName(this.zDimName);
		Vector<String> zVals = new Vector<String>();
		if(zFilter != null)
		{
			for(String zVal : zDim.dimValues)
			{
				if(!zFilter.containsValue(zVal))
				{
					zVals.add(zVal);
				}
			}
			zDim = new Dim(this.zDimName, zVals);
		}
		
		DimTable dt = imageData.getDimTable().getSubTable(this.zDimName);

		// // Filter the zDim values if necessary.
		// Dim zDim2 = filterTable.getDimWithName(this.zDimName);
		// Vector<String> temp = new Vector<>();
		// if(zDim2 != null)
		// {
		// for(int i = 0; i < zDim.size(); i++)
		// {
		// String item1 = zDim.valueAt(i);
		// boolean found = false;
		// for(String item2 : zDim2.dimValues)
		// {
		// if(item1.equals(item2))
		// {
		// found = true;
		// }
		// }
		// if(!found)
		// {
		// temp.add(item1);
		// }
		// }
		// zDim = new Dim(this.zDimName, temp);
		// }

		// TIECalculator(int imWidth, int imHeight, float regularization, float
		// threshold, float magnification, float pixelSizeInMicrons, float dzInMicrons,
		// float wavelengthInNanometers)
		TIECalculator tie = null;

		//		FFT_Filter fft = new FFT_Filter();
		//		fft.choiceDia = this.choiceDia;
		//		fft.displayFilter = this.saveFilter;
		//		fft.doScalingDia = false;
		//		fft.filterLargeDia = 2*this.filterLargeDia; // lightly prefilter image with large feature size cutoff
		//		fft.filterSmallDia = this.filterSmallDia;
		//		fft.saturateDia = false;
		//		fft.toleranceDia = this.toleranceDia;
		//		fft.filterDC = this.filterDC;
		
		BackgroundSubtracter bS = new BackgroundSubtracter();

		int total = dt.mapCount() * (zDim.size() - 2 - 2*zGAP);
		if (successive)
		{
			total = dt.mapCount() * (zDim.size() - 1 - 1*zGAP);
		}
		if(total < 1)
		{
			Logs.log("\n\nNot enough images for specified GAP parameter.\n\n", Logs.ERROR, this);
		}
		
		int originalBitDepth = 0;
		RankFilters2 rF = new RankFilters2();
		for (DimensionMap map : dt.getMapIterator())
		{
			if (filterTable.testMapAsExclusionFilter(map))
			{
				Logs.log("Skipping the processing and saving of " + map.toString(), this);
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) total * this.rows * this.cols)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				continue;
			}
			// get the med and hi float processors
			// FloatProcessor fpHi = null;
			// FloatProcessor fpMed = null;
			// FloatProcessor fpLo = null;
			TreeMap<DimensionMap, ImageProcessor> tiles = new TreeMap<>();
			DimensionMap toSave = null;

			int maxZ = zDim.size()-2-2*this.zGAP;
			if(successive)
			{
				maxZ = zDim.size()-1-1*this.zGAP;
			}
			
			for (int i = 0; i < maxZ; i++)
			{
				DimensionMap loToGet = map.copyAndSet(this.zDimName + "=" + zDim.valueAt(i));
				DimensionMap medToGet = map.copyAndSet(this.zDimName + "=" + zDim.valueAt(i+this.zGAP+1));
				DimensionMap hiToGet = map.copyAndSet(this.zDimName + "=" + zDim.valueAt(i+this.zGAP+1));
				if(!successive)
				{
					hiToGet = map.copyAndSet(this.zDimName + "=" + zDim.valueAt(i+this.zGAP+1+this.zGAP+1));
				}
				
				// Check if canceled
				if (this.isCanceled()) {
					return false;
				}

				if (tiles.get(new DimensionMap("TIEZ=Hi,ImRow=0,ImCol=0")) == null)
				{
					// Then need to initialize things.
					ImagePlus tempIm = new ImagePlus(imageMap.get(loToGet));
					FloatProcessor fpLo = tempIm.getProcessor().convertToFloatProcessor();
					if (originalBitDepth == 0)
					{
						originalBitDepth = tempIm.getBitDepth();
					}
					TreeMap<DimensionMap, ImageProcessor> temp = new TreeMap<>();
					if (this.rows > 1 || this.cols > 1)
					{
						Logs.log("Splitting image into tiles", this);
						temp.put(new DimensionMap("TIEZ=Lo"), fpLo);
						tiles.putAll(ImageWriter.separateTilesToProcessors(temp, overlap, rows, cols, this.getCanceler()));
					}
					else
					{
						tiles.put(new DimensionMap("TIEZ=Lo,ImRow=0,ImCol=0"), fpLo);
					}
					tempIm = null;
					
					if(!successive)
					{
						FloatProcessor fpMed = (new ImagePlus(imageMap.get(medToGet)).getProcessor().convertToFloatProcessor());
						temp = new TreeMap<>();
						if (this.rows > 1 || this.cols > 1)
						{
							Logs.log("Splitting image into tiles", this);
							temp.put(new DimensionMap("TIEZ=Med"), fpMed);
							tiles.putAll(ImageWriter.separateTilesToProcessors(temp, overlap, rows, cols, this.getCanceler()));
						}
						else
						{
							tiles.put(new DimensionMap("TIEZ=Med,ImRow=0,ImCol=0"), fpMed);
						}
						toSave = medToGet.copy();
					}
					
					
					FloatProcessor fpHi = (new ImagePlus(imageMap.get(hiToGet)).getProcessor().convertToFloatProcessor());
					temp = new TreeMap<>();
					if (this.rows > 1 || this.cols > 1)
					{
						Logs.log("Splitting image into tiles", this);
						temp.put(new DimensionMap("TIEZ=Hi"), fpHi);
						tiles.putAll(ImageWriter.separateTilesToProcessors(temp, overlap, rows, cols, this.getCanceler()));
					}
					else
					{
						tiles.put(new DimensionMap("TIEZ=Hi,ImRow=0,ImCol=0"), fpHi);
					}
					
					
					if (successive)
					{
						toSave = hiToGet.copy();
					}
					
					//FileUtility.showImg(fpLo, true);
					//FileUtility.showImg(fpHi, true);
				}
				else
				{
					// Shift images and load newest
					DimTable tilesDT = new DimTable(tiles);
					if (successive)
					{
						FloatProcessor fpLo = null, fpHi = null;
						
						if(this.zGAP==0)
						{
							// Transfer Hi images to Lo
							for (DimensionMap tempMap : tilesDT.getSubTable(new DimensionMap("TIEZ=Hi")).getMapIterator())
							{
								fpLo = (FloatProcessor) tiles.get(tempMap);
								tiles.put(tempMap.copyAndSet("TIEZ=Lo"), tiles.get(tempMap)); // fpLo = fpHi;
							}
						}
						else
						{
							// Read in new Lo images
							fpLo = (new ImagePlus(imageMap.get(loToGet)).getProcessor().convertToFloatProcessor());
							TreeMap<DimensionMap, ImageProcessor> temp = new TreeMap<>();
							if (this.rows > 1 || this.cols > 1)
							{
								Logs.log("Splitting image into tiles", this);
								temp.put(new DimensionMap("TIEZ=Lo"), fpLo);
								tiles.putAll(ImageWriter.separateTilesToProcessors(temp, overlap, rows, cols,
										this.getCanceler()));
							}
							else
							{
								tiles.put(new DimensionMap("TIEZ=Lo,ImRow=0,ImCol=0"), fpLo);
							}
						}
						
						// Get new Hi images
						fpHi = (new ImagePlus(imageMap.get(hiToGet)).getProcessor().convertToFloatProcessor());
						TreeMap<DimensionMap, ImageProcessor> temp = new TreeMap<>();
						if (this.rows > 1 || this.cols > 1)
						{
							Logs.log("Splitting image into tiles", this);
							temp.put(new DimensionMap("TIEZ=Hi"), fpHi);
							tiles.putAll(ImageWriter.separateTilesToProcessors(temp, overlap, rows, cols,
									this.getCanceler()));
						}
						else
						{
							tiles.put(new DimensionMap("TIEZ=Hi,ImRow=0,ImCol=0"), fpHi);
						}
						toSave = hiToGet.copy();
						
						//FileUtility.showImg(fpLo, true);
						//FileUtility.showImg(fpHi, true);
					}
					else
					{
						FloatProcessor fpLo = null, fpMed=null, fpHi = null;
						
						if(this.zGAP == 0)
						{
							// Transfer Med to Lo
							for (DimensionMap tempMap : tilesDT.getSubTable(new DimensionMap("TIEZ=Med")).getMapIterator())
							{
								fpLo = (FloatProcessor) tiles.get(tempMap);
								tiles.put(tempMap.copyAndSet("TIEZ=Lo"), tiles.get(tempMap)); // fpLo = fpMed;
							}
							// Transfer Hi to Med
							for (DimensionMap tempMap : tilesDT.getSubTable(new DimensionMap("TIEZ=Hi")).getMapIterator())
							{
								tiles.put(tempMap.copyAndSet("TIEZ=Med"), tiles.get(tempMap)); // fpMed = fpHi;
							}
						}
						else
						{
							// Read in new Lo images
							fpLo = (new ImagePlus(imageMap.get(loToGet)).getProcessor().convertToFloatProcessor());
							TreeMap<DimensionMap, ImageProcessor> temp = new TreeMap<>();
							if (this.rows > 1 || this.cols > 1)
							{
								Logs.log("Splitting image into tiles", this);
								temp.put(new DimensionMap("TIEZ=Lo"), fpLo);
								tiles.putAll(ImageWriter.separateTilesToProcessors(temp, overlap, rows, cols, this.getCanceler()));
							}
							else
							{
								tiles.put(new DimensionMap("TIEZ=Lo,ImRow=0,ImCol=0"), fpLo);
							}
							// Read in new Med images
							fpMed = (new ImagePlus(imageMap.get(medToGet)).getProcessor().convertToFloatProcessor());
							TreeMap<DimensionMap, ImageProcessor> temp2 = new TreeMap<>();
							if (this.rows > 1 || this.cols > 1)
							{
								Logs.log("Splitting image into tiles", this);
								temp2.put(new DimensionMap("TIEZ=Med"), fpMed);
								tiles.putAll(ImageWriter.separateTilesToProcessors(temp2, overlap, rows, cols, this.getCanceler()));
							}
							else
							{
								tiles.put(new DimensionMap("TIEZ=Med,ImRow=0,ImCol=0"), fpMed);
							}
						}
						
						// Get new Hi images
						fpHi = (new ImagePlus(imageMap.get(hiToGet)).getProcessor().convertToFloatProcessor());
						TreeMap<DimensionMap, ImageProcessor> temp = new TreeMap<>();
						if (this.rows > 1 || this.cols > 1)
						{
							Logs.log("Splitting image into tiles", this);
							temp.put(new DimensionMap("TIEZ=Hi"), fpHi);
							tiles.putAll(ImageWriter.separateTilesToProcessors(temp, overlap, rows, cols, this.getCanceler()));
						}
						else
						{
							tiles.put(new DimensionMap("TIEZ=Hi,ImRow=0,ImCol=0"), fpHi);
						}
						toSave = medToGet.copy();
						
						//FileUtility.showImg(fpLo, true);
						//FileUtility.showImg(fpHi, true);
					}
				}

				
				FHT kernel = null;
				DimTable subt = new DimTable(tiles).getSubTable("TIEZ");
				for (DimTable subsubt : subt.getSubTableIterator("ImRow")) {
					for (DimensionMap tileMap : subsubt.getMapIterator()) {
						// Check if canceled
						if (this.isCanceled()) {
							return false;
						}

						if (tie == null)
						{
							// Calculate parameters
							// Calculate the inverse of the 1/e frequencies for large and small structures.
							int width = tiles.get(tileMap.copyAndSet("TIEZ=Lo")).getWidth();
							int height = tiles.get(tileMap.copyAndSet("TIEZ=Lo")).getHeight();
							// int maxN = Math.max(width, height);
					        float reg = (float) (32d / this.filterLargeDia );
					        Logs.log("Regularization Parameter calculated to be " + reg, this);
							if(successive)
							{
								// Initialize TIE Calculator first time through
								tie = new TIECalculator(width, height, reg, this.thresh, this.mag,
										this.pixelSize, this.dz*(1+this.zGAP), this.lambda, this.simple);
							}
							else
							{
								// Initialize TIE Calculator first time through
								tie = new TIECalculator(width, height, reg, this.thresh, this.mag,
										this.pixelSize, 2*this.dz*(1+this.zGAP), this.lambda, this.simple);
							}
							
						}
						// Logs.log("" + tiles.get(tileMap.copyAndSet("Z=Med")).getWidth() + " : "
						// +tiles.get(tileMap.copyAndSet("Z=Med")).getHeight(), this);
						Logs.log("Processing Tile: " + toSave.toString() + "," + tileMap.toString(), this);
						FloatProcessor phi = null;
						if (successive) {
							phi = tie.calculatePhase(null, (FloatProcessor) tiles.get(tileMap.copyAndSet("TIEZ=Lo")),
									(FloatProcessor) tiles.get(tileMap.copyAndSet("TIEZ=Hi")));
						} else {
							phi = tie.calculatePhase((FloatProcessor) tiles.get(tileMap.copyAndSet("TIEZ=Med")),
									(FloatProcessor) tiles.get(tileMap.copyAndSet("TIEZ=Lo")),
									(FloatProcessor) tiles.get(tileMap.copyAndSet("TIEZ=Hi")));
						}

						ImageProcessor out = null;
						if (this.doFilteringAndScaling) {
							
							//							if(this.doFFT)
							//							{
							//								fft.filter(phi);
							//							}
							
							Pair<FloatProcessor, ?> images = null;
//							if(this.doWMF & !this.hybrid)
//							{
//								images = ImageUtility.getWeightedMeanFilterImage(phi,
//										this.saveThresholdImage, true, true, false, 0.4*this.filterLargeDia, 2d, this.subtractionPower, this.thresholdPower, 0d, this.sigma, 0d, this.radiusWMF);
//								phi = images.p1;
//							}
							
							double max = 1;
//							double originalMax = 1;
							if (this.bitDepth < 32) {
								max = Math.pow(2, this.bitDepth) - 1;
							}
//							if (originalBitDepth < 32) {
//								originalMax = Math.pow(2, originalBitDepth) - 1;
//							}
							
							phi.multiply(max / this.scale); // Original data is essentially adjusted to range of
							// 0-1 in TIECalculator by dividing by middle plane (or equivalent). This then maps
							// the range of +/- this.scale to span the full range of the output bitdepth 
							
//							if(this.simple)
//							{
////								double median1 = ImageStatistics.getStatistics(tiles.get(tileMap.copyAndSet("TIEZ=Hi")), ImageStatistics.MEDIAN, null).median;
////								double median2 = ImageStatistics.getStatistics(tiles.get(tileMap.copyAndSet("TIEZ=Lo")), ImageStatistics.MEDIAN, null).median;
//								double mean = (tiles.get(tileMap.copyAndSet("TIEZ=Hi")).getStats().median + tiles.get(tileMap.copyAndSet("TIEZ=Lo")).getStats().median)/2;
//								phi.multiply(max / (mean * this.scale)); // mimic preadjusting all images to range of
//								// 0-1 with '1/originalMax'
//							}
//							else
//							{
//								 // mimic preadjusting all images to range of
//								// 0-1 with '1/originalMax'
//							}
							
//							if (this.bitDepth < 32 && (this.doWMF & !this.zero) || (this.doRBF) || (!this.doWMF & !this.doRBF))
//							{
//								phi.add(max / 2);
//							}
							phi.resetMinAndMax();							
							
							if(this.doRBF & !this.doHMF)
							{
								out = JEXWriter.convertToBitDepthIfNecessary(phi, this.bitDepth);
								// apply the function to imp
								bS.rollingBallBackground(out, this.radiusRBF, this.createBackground, this.lightBackground, this.paraboloid, this.presmooth, true);
							}
							
							if(this.doHMF)
							{
//								FileUtility.showImg(phi, true);
								
								Pair<FloatProcessor[], FloatProcessor> ret = null;
								if(this.HMFPercentile >= 0)
								{
									// Get variance weights, threshold, and filter
									ret = ImageUtility.getImageVarianceWeights((FloatProcessor) tiles.get(tileMap.copyAndSet("TIEZ=Hi")), this.radiusVariance, false, false, this.scale);
									this.threshold(ret.p1[0], 0.5);
									rF.rank(ret.p1[0], 2*this.radiusVariance, RankFilters2.MIN);
									rF.rank(ret.p1[0], 2*this.radiusVariance, RankFilters2.MAX);
//									rF.rank(ret.p1[0], 2*this.radiusVarWMF, RankFilters2.MAX);
//									rF.rank(ret.p1[0], 2*this.radiusVarWMF, RankFilters2.MIN);
//									ret.p1[0].invert();
									
//									FileUtility.showImg(ret.p1[0], true);
//									FileUtility.showImg(temp, true);
								}
								
								
								// Do RBF
								
								if(this.HMFPercentile != 0)
								{
									ImageProcessor temp = JEXWriter.convertToBitDepthIfNecessary(phi, this.bitDepth);
									byte[] overPixels = this.getOverPixels(temp);
//									FileUtility.showImg(temp, true);
									bS.rollingBallBackground(temp, this.radiusRBF, this.createBackground, this.lightBackground, this.paraboloid, this.presmooth, true);
									Pair<Double, Double>thresholds = ImageUtility.percentile(temp, Math.abs(this.HMFPercentile), Math.abs(this.HMFPercentile), -1, -1, Double.MAX_VALUE);
									
									this.thresholdLoHi(temp, thresholds.p1, overPixels);
//									FileUtility.showImg(temp, true);
//									FileUtility.showImg(temp, true);
									temp.invert();
//									FileUtility.showImg(temp, true);
									
									if(this.HMFPercentile > 0)
									{
										ret.p1[0].copyBits(temp, 0, 0, Blitter.OR); // Final Weights for WMF
										images = new Pair<>(null, new ImageProcessor[] {ret.p1[0], temp});
									}
									else
									{
										images = new Pair<>(null, new ImageProcessor[] {temp});
									}
									
								}
								else
								{
									images = new Pair<>(null, new ImageProcessor[] {ret.p1[0]});
								}
								
//								FileUtility.showImg(ret.p1[0], true);
								
								// Phi X Weights
								out = phi.duplicate();
								
								ImageProcessor floatWeights = ((ImageProcessor[]) images.p2)[0].convertToFloatProcessor();
								phi.copyBits(floatWeights, 0, 0, Blitter.MULTIPLY);
								
//								FileUtility.showImg(phi, true);
								
								// Blur ( Phi X Weights )
								if(kernel == null)
								{
									kernel = JEX_ImageFilters.getDecayKernel((int) Math.pow(2,TIECalculator.nearestSuperiorPowerOf2(phi.getWidth())), this.radiusHMF, this.powerHMF);
								}
								JEX_ImageFilters.convolve(phi, kernel);
								
//								FileUtility.showImg(phi, true);
								
								// Blur ( Weights )
								JEX_ImageFilters.convolve(floatWeights, kernel);
								
//								FileUtility.showImg(floatWeights, true);
								
								// BG = Blur ( Phi X Wieghts ) / Blur ( Weights )
								phi.copyBits(floatWeights, 0, 0, Blitter.DIVIDE);
								
//								FileUtility.showImg(phi, true);
								
								// Result = Phi - BG
								out.copyBits(phi, 0, 0, Blitter.SUBTRACT);
								
//								FileUtility.showImg(out, true);
								
//								out = ImageUtility.getWeightedMeanFilterImage(, ((ImageProcessor[]) images.p2)[0].convertToFloatProcessor(), true, false, false, this.sigmaWMF, this.radiusVarWMF, this.scale, this.scale, 0.0, 0.0, 0.0, this.radiusWMF);
								out = JEXWriter.convertToBitDepthIfNecessary(out, this.bitDepth);
								
							}
							
							DimensionMap mapToSave = toSave.copy();
							if (this.rows > 1) {
								mapToSave.put("ImRow", tileMap.get("ImRow"));
							}
							if (this.cols > 1) {
								mapToSave.put("ImCol", tileMap.get("ImCol"));
							}
							if (images != null && images.p2 != null)
							{
								if(images.p2 instanceof ImageProcessor[])
								{
									String maskPath = JEXWriter.saveImage(((ImageProcessor[])images.p2)[0], 8);
									maskMap.put(mapToSave.copyAndSet("Mask=Final"), maskPath);
									if(((ImageProcessor[]) images.p2).length > 1)
									{
										maskPath = JEXWriter.saveImage(((ImageProcessor[])images.p2)[1], 8);
										maskMap.put(mapToSave.copyAndSet("Mask=RBF"), maskPath);
									}
								}
								else
								{
									String maskPath = JEXWriter.saveImage((ImageProcessor) images.p2, 8);
									maskMap.put(mapToSave.copyAndSet("Mask=RBF"), maskPath);
								}
							}
							
						} else {
							double originalMax = Math.pow(2, originalBitDepth) - 1;
							phi.multiply(1 / originalMax); // This mimics preadjusting all the images to a range of 0-1.
							phi.resetMinAndMax();
						}

						if(out == null)
						{
							out = JEXWriter.convertToBitDepthIfNecessary(phi, this.bitDepth);
						}
						
						
						
						tempPath = JEXWriter.saveImage(out);
						if (tempPath != null) {
							DimensionMap mapToSave = toSave.copy();
							if (this.rows > 1) {
								mapToSave.put("ImRow", tileMap.get("ImRow"));
							}
							if (this.cols > 1) {
								mapToSave.put("ImCol", tileMap.get("ImCol"));
							}
							outputImageMap.put(mapToSave, tempPath);
						}
						// if(keepdIdz)
						// {
						// if(bitDepthOfOriginal < 32)
						// {
						// phi.p1.multiply(Math.pow(2, bitDepthOfOriginal)-1.0);
						// }
						//
						// phi.p1.resetMinAndMax();
						// tempPath = JEXWriter.saveImage(phi.p1);
						// if(tempPath != null)
						// {
						// dIdzMap.put(toSave.copy(), tempPath);
						// }
						// }
						count = count + 1;
						percentage = (int) (100 * ((double) (count) / ((double) total * this.rows * this.cols)));
						JEXStatics.statusBar.setProgressPercentage(percentage);
					}
				}
			}
		}

		if (outputImageMap.size() == 0) {
			return false;
		}

		this.output = ImageWriter.makeImageStackFromPaths("temp", outputImageMap);

		if (maskMap.size() > 0) {
			this.mask = ImageWriter.makeImageStackFromPaths("temp", maskMap);
		}

		// Return status
		return true;
	}

	public Vector<String[]> getZList(Dim zDim, int space) {
		Vector<String[]> ret = new Vector<>();
		int max = zDim.size();
		int lo = 0;
		int mid = lo + space;
		int hi = mid + space;
		while (hi <= max) {
			ret.add(new String[] { zDim.valueAt(lo), zDim.valueAt(mid), zDim.valueAt(hi) });
			lo++;
			mid++;
			hi++;
		}
		return ret;
	}
	
	private void threshold(ImageProcessor imp, Double thresh)
	{
		if(imp instanceof FloatProcessor)
		{
			float[] pixels = (float[]) imp.getPixels();
			for(int j = 0; j < pixels.length; j++)
			{
				if(pixels[j] <= thresh)
				{
					pixels[j] = 0f;
				}
				else
				{
					pixels[j] = 1f;
				}
			}
			imp.resetMinAndMax();
		}
		if(imp instanceof ShortProcessor)
		{
			short[] pixels = (short[]) imp.getPixels();
			for(int j = 0; j < pixels.length; j++)
			{
				if(pixels[j] <= thresh)
				{
					pixels[j] = (short) 0;
				}
				else
				{
					pixels[j] = (short) 1;
				}
			}
		}
		if(imp instanceof ByteProcessor)
		{
			byte[] pixels = (byte[]) imp.getPixels();
			for(int j = 0; j < pixels.length; j++)
			{
				if(pixels[j] <= thresh)
				{
					pixels[j] = (byte) 0;
				}
				else
				{
					pixels[j] = (byte) 1;
				}
			}
		}
	}
	
	private byte[] getOverPixels(ImageProcessor imp)
	{
		byte[] ret = null;
		if(imp instanceof FloatProcessor)
		{
			float[] pixels = (float[]) imp.getPixels();
			ret = new byte[pixels.length];
			for(int j = 0; j < pixels.length; j++)
			{
				if(pixels[j] == Float.POSITIVE_INFINITY)
				{
					ret[j] = (byte) 1;
				}
			}
		}
		if(imp instanceof ShortProcessor)
		{
			short[] pixels = (short[]) imp.getPixels();
			ret = new byte[pixels.length];
			for(int j = 0; j < pixels.length; j++)
			{
				if((pixels[j] & 0xffff) == 65535)
				{
					ret[j] = (byte) 1;
				}
			}
		}
		if(imp instanceof ByteProcessor)
		{
			byte[] pixels = (byte[]) imp.getPixels();
			ret = new byte[pixels.length];
			for(int j = 0; j < pixels.length; j++)
			{
				if(pixels[j] == 255)
				{
					ret[j] = (byte) 1;
				}
			}
		}
		return ret;
	}
	
	private void thresholdLoHi(ImageProcessor imp, Double lo, byte[] overPixels)
	{
		if(imp instanceof FloatProcessor)
		{
			float[] pixels = (float[]) imp.getPixels();
			for(int j = 0; j < pixels.length; j++)
			{
				if(pixels[j] > lo || overPixels[j] == 1)
				{
					pixels[j] = 1f;
				}
				else
				{
					pixels[j] = 0f;
				}
			}
			imp.resetMinAndMax();
		}
		if(imp instanceof ShortProcessor)
		{
			short[] pixels = (short[]) imp.getPixels();
			for(int j = 0; j < pixels.length; j++)
			{
				if((pixels[j] & 0xffff) > lo || (overPixels[j] & 0xffff) == 1)
				{
					pixels[j] = (short) 1;
				}
				else
				{
					pixels[j] = (short) 0;
				}
			}
		}
		if(imp instanceof ByteProcessor)
		{
			byte[] pixels = (byte[]) imp.getPixels();
			for(int j = 0; j < pixels.length; j++)
			{
				if((pixels[j] & 0xFF) > lo || (overPixels[j] & 0xFF) == 1)
				{
					pixels[j] = (byte) 1;
				}
				else
				{
					pixels[j] = (byte) 0;
				}
			}
		}
	}

}
