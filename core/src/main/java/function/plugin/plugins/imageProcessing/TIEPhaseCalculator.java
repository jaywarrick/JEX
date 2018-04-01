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
import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import jex.statics.JEXStatics;
import jex.utilities.ImageUtility;
import logs.Logs;
import miscellaneous.Pair;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="TIE Phase Calculator",
		menuPath="Image Processing",
		visible=true,
		description="Take a z-stack of brightfield images and calculate the phase shift of light through the sample."
		)
public class TIEPhaseCalculator extends JEXPlugin {

	public TIEPhaseCalculator()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Simple Algorithm?", description="This assumes a uniform illumination of the image, but is faster.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean simple;

	@ParameterMarker(uiOrder=2, name="Z Dim Name", description="Name of the 'Z' dimension for this image object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Z")
	String zDimName;

	//	@ParameterMarker(uiOrder=1, name="Z Values for Lo,Med,Hi", description="Which Z Values should be used as the low, medium, and hi z values. Medium should be the best focus plane of the z-stack.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1,2,3")
	//	String zVals;

	@ParameterMarker(uiOrder=3, name="z-step [µm]", description="What is the step distance between z planes.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	float dz;

	@ParameterMarker(uiOrder=4, name="Camera Pixel Size [µm]", description="The camera pixel size in microns. This is needed for 'absolute' quantitation and is not necessary if all you want is 'relative' quantitation (e.g., something is twice the other thing).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="6.45")
	float pixelSize;

	@ParameterMarker(uiOrder=5, name="Image Magnification", description="The magnification at which the images were taken. Used with the camera pixel size to determine the pixel width. (width = camera pixel size / magnification)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="20.0")
	float mag;

	@ParameterMarker(uiOrder=6, name="Wavelength of Light [nm]", description="This is the spectrally weighted average wavelength of the bright-field illumination. This is needed for 'absolute' quantitation and is not necessary if all you want is 'relative' quantitation (e.g., something is twice the other thing).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="600")
	float lambda;

	@ParameterMarker(uiOrder=7, name="Regularization Parameter (~0-2)", description="The regularization parameter for the equation is essentially the cuttoff a high-pass filter that takes values that typically range from 0.1-1 but needs to be >=0. This can generally be set low (0.1) and the resulting phase image can be filtered to 'remove background'.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.1")
	float r;

	@ParameterMarker(uiOrder=8, name="Threshold Parameter", description="The threshold parameter is expressed as a fraction of the max image intensity. All pixels below this min value are set to the min value to avoid dividing by 0. Typically 0.01 or 0.001.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.001")
	float thresh;
	
	@ParameterMarker(uiOrder=9, name="Filter and Scale Result?", description="Should the result be FFT filtered to remove noise and then scaled for saving at a particular bit depth?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean doFilteringAndScaling;
	
	@ParameterMarker(uiOrder=10, name="Filter: Feature Size Cutoff", description="The filter removes low frequency fluctuations (e.g., background) and keeps small features (higher frequency items). What is the feature size cutoff? [pixels].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="50.0")
	double filterLargeDia;

	//@ParameterMarker(uiOrder=11, name="FFT Post-Filter: Min Size", description="The smallest features to keep [pixels].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double filterSmallDia = 0;
	
	//@ParameterMarker(uiOrder=2, name="Filter: DC Component?", description="Should the 'DC component' or constant background be subtracted?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean filterDC = true;

	//@ParameterMarker(uiOrder=3, name="Suppress Stripes", description="Should stripes be suppressed and, if so, which direction should be suppressed.", ui=MarkerConstants.UI_DROPDOWN, choices={"None","Horizontal","Vertical"}, defaultChoice=0)
	String choiceDia = "None";

	//@ParameterMarker(uiOrder=4, name="Tolerane of Direction", description="Tolerance of direction for stripe suppression [%].", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5.0")
	double toleranceDia = 5.0;

	//@ParameterMarker(uiOrder=5, name="Save filter?", description="Should the filter in the frequency domain be saved as an image?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean saveFilter = false;

	//@ParameterMarker(uiOrder=6, name="Autoscale result?", description="Should the result be automatically scaled?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean doScalingDia = false;

	//@ParameterMarker(uiOrder=7, name="Saturate autoscaling?", description="If autoscaled, should the result be saturated (1% at tails) to better fill the dynamic range?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean saturateDia = false;

	@ParameterMarker(uiOrder=12, name="Scale: Output Bit Depth", description="Depth of the outputted image for all channels.", ui=MarkerConstants.UI_DROPDOWN, choices={ "8", "16", "32" }, defaultChoice=1)
	int bitDepth;
	
	@ParameterMarker(uiOrder=13, name="Scale: +/- Scale", description="The result will be scaled such that -Scale to +Scale in the initial phase result will be scaled to fill the range of the bit depth selected. Therefore 0 will be the middlest value of the final image range. (32-bit results in no scaling)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5.0")
	double scale;
	
	@ParameterMarker(uiOrder=14, name="Save Thresholded Filter Result?", description="Should a thresholded image be generated from the filtered and scaled phase image?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = false)
	boolean saveThresholdImage;
	
	@ParameterMarker(uiOrder=15, name="Threshold: Sigma", description="How many sigma above background should the threshold cutoff be? Use 0 to save the 'signal-to-noise' image which can be then thresholded or used to determine the best sigma.", ui=MarkerConstants.UI_TEXTFIELD, defaultText = "0.0")
	double sigma;

	//	@ParameterMarker(uiOrder=3, name="Double Precision (vs Float)", description="Should 'double' precision be used in the calculation?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	//	boolean dPrecision;

	@ParameterMarker(uiOrder=16, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;

	@ParameterMarker(uiOrder=17, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean keepExcluded;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Phase", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant phase image", enabled=true)
	JEXData output;
	
	@OutputMarker(uiOrder=2, name="Phase Mask", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Mask made from the the phase image, locally thresholded using the 'weighted mean filter' method.", enabled=true)
	JEXData mask;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}

		DimTable filterTable = new DimTable(this.filterDimTableString);

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,String> maskMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;

		// For each dimension map without a Z, iterator over Z
		Dim zDim = imageData.getDimTable().getDimWithName(this.zDimName);
		DimTable dt = imageData.getDimTable().getSubTable(this.zDimName);


		//		// Filter the zDim values if necessary.
		//		Dim zDim2 = filterTable.getDimWithName(this.zDimName);
		//		Vector<String> temp = new Vector<>();
		//		if(zDim2 != null)
		//		{
		//			for(int i = 0; i < zDim.size(); i++)
		//			{
		//				String item1 = zDim.valueAt(i);
		//				boolean found = false;
		//				for(String item2 : zDim2.dimValues)
		//				{
		//					if(item1.equals(item2))
		//					{
		//						found = true;
		//					}
		//				}
		//				if(!found)
		//				{
		//					temp.add(item1);
		//				}
		//			}
		//			zDim = new Dim(this.zDimName, temp);
		//		}

		// TIECalculator(int imWidth, int imHeight, float regularization, float threshold, float magnification, float pixelSizeInMicrons, float dzInMicrons, float wavelengthInNanometers)
		TIECalculator tie = null;
		
		FFT_Filter fft = new FFT_Filter();
		fft.choiceDia = this.choiceDia;
		fft.displayFilter = this.saveFilter;
		fft.doScalingDia = false;
		fft.filterLargeDia = 2*this.filterLargeDia;
		fft.filterSmallDia = this.filterSmallDia;
		fft.saturateDia = false;
		fft.toleranceDia = this.toleranceDia;
		fft.filterDC = this.filterDC;

		int total = dt.mapCount() * (zDim.size()-2);
		int originalBitDepth = 0;
		for (DimensionMap map : dt.getMapIterator())
		{
			// get the med and hi float processors
			FloatProcessor fpHi = null;
			FloatProcessor fpMed = null;
			FloatProcessor fpLo = null;
			DimensionMap toSave = null;
			for(int i = 0; i < zDim.size(); i++)
			{
				DimensionMap toGet = map.copyAndSet(this.zDimName + "=" + zDim.valueAt(i));
				if(filterTable.testMapAsExclusionFilter(toGet))
				{
					if(this.keepExcluded)
					{
						Logs.log("Skipping the processing of " + toGet.toString(), this);
						ImagePlus out = new ImagePlus(imageMap.get(toGet));
						tempPath = JEXWriter.saveImage(out);
						if(tempPath != null)
						{
							outputImageMap.put(toGet, tempPath);
						}
						count = count + 1;
						percentage = (int) (100 * ((double) (count) / ((double) total)));
						JEXStatics.statusBar.setProgressPercentage(percentage);
					}
					else
					{
						Logs.log("Skipping the processing and saving of " + toGet.toString(), this);
						count = count + 1;
						percentage = (int) (100 * ((double) (count) / ((double) total)));
						JEXStatics.statusBar.setProgressPercentage(percentage);
					}
					continue;
				}
				// Check if canceled
				if(this.isCanceled())
				{
					return false;
				}

				if(fpLo == null)
				{
					ImagePlus tempIm = new ImagePlus(imageMap.get(toGet));
					fpLo = tempIm.getProcessor().convertToFloatProcessor();
					if(originalBitDepth == 0)
					{
						originalBitDepth = tempIm.getBitDepth();
					}
					tempIm = null;
					if(tie == null)
					{
						tie = new TIECalculator(fpLo.getWidth(), fpLo.getHeight(), this.r, this.thresh, this.mag, this.pixelSize, this.dz, this.lambda, this.simple);
					}
					continue;
				}
				else if(fpMed == null)
				{
					fpMed = (new ImagePlus(imageMap.get(toGet)).getProcessor().convertToFloatProcessor());
					toSave = toGet.copy();
					continue;
				}
				else if(fpHi == null)
				{
					fpHi = (new ImagePlus(imageMap.get(toGet)).getProcessor().convertToFloatProcessor());
				}
				else
				{
					fpLo = fpMed;
					fpMed = fpHi;
					fpHi = (new ImagePlus(imageMap.get(toGet)).getProcessor().convertToFloatProcessor());
				}

				//				FileUtility.showImg(fpLo, true);
				//				FileUtility.showImg(fpMed, true);
				//				FileUtility.showImg(fpHi, true);
				FloatProcessor phi = tie.calculatePhase(fpMed, fpLo, fpHi);
				
				if(this.doFilteringAndScaling)
				{
					fft.filter(phi);
					ImagePlus filtered = new ImagePlus("Filtered", phi);
					
					Pair<FloatProcessor, String> images = ImageUtility.getWeightedMeanFilterImage(filtered, this.saveThresholdImage, true, true, false, this.filterLargeDia, 2d, 2d, 0.75, "Subtract Background", 0d, this.sigma, 100d);
					phi = images.p1;
					if(this.bitDepth < 32)
					{
						//						phi.resetMinAndMax();
						//						tie.viewImage(phi);
						double max = Math.pow(2, this.bitDepth)-1;
						double originalMax = Math.pow(2, originalBitDepth)-1;
						phi.multiply(max/(originalMax * this.scale)); // mimic preadjusting all images to range of 0-1 with '1/originalMax'
						phi.add(max/2);
						phi.resetMinAndMax();
					}
					maskMap.put(toSave.copy(), images.p2);
				}
				else
				{
					double originalMax = Math.pow(2, originalBitDepth)-1;
					phi.multiply(1/originalMax); // This mimics preadjusting all the images to a range of 0-1.
					phi.resetMinAndMax();
				}
				
				ImageProcessor out = JEXWriter.convertToBitDepthIfNecessary(phi, this.bitDepth);
				tempPath = JEXWriter.saveImage(out);
				if(tempPath != null)
				{
					outputImageMap.put(toSave.copy(), tempPath);
				}
				//				if(keepdIdz)
				//				{
				//					if(bitDepthOfOriginal < 32)
				//					{
				//						phi.p1.multiply(Math.pow(2, bitDepthOfOriginal)-1.0);
				//					}
				//					
				//					phi.p1.resetMinAndMax();
				//					tempPath = JEXWriter.saveImage(phi.p1);
				//					if(tempPath != null)
				//					{
				//						dIdzMap.put(toSave.copy(), tempPath);
				//					}
				//				}
				toSave = toGet.copy();
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) total)));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}

		if(outputImageMap.size() == 0)
		{
			return false;
		}

		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);
		
		if(maskMap.size() > 0)
		{
			this.mask = ImageWriter.makeImageStackFromPaths("temp", maskMap);
		}

		// Return status
		return true;
	}


	public  Vector<String[]> getZList(Dim zDim, int space)
	{
		Vector<String[]> ret = new Vector<>();
		int max = zDim.size();
		int lo = 0;
		int mid = lo + space;
		int hi = mid + space;
		while(hi <= max)
		{
			ret.add(new String[]{zDim.valueAt(lo), zDim.valueAt(mid), zDim.valueAt(hi)});
			lo++;
			mid++;
			hi++;
		}
		return ret;
	}

}
