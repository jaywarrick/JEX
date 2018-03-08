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
import jex.statics.JEXStatics;
import logs.Logs;
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

	@ParameterMarker(uiOrder=1, name="Simple Algorithm?", description="This assumes a uniform illumination of the image, but is faster.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean simple;

	@ParameterMarker(uiOrder=2, name="Z Dim Name", description="Name of the 'Z' dimension for this image object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Z")
	String zDimName;

	//	@ParameterMarker(uiOrder=1, name="Z Values for Lo,Med,Hi", description="Which Z Values should be used as the low, medium, and hi z values. Medium should be the best focus plane of the z-stack.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1,2,3")
	//	String zVals;

	@ParameterMarker(uiOrder=3, name="z-step [µm]", description="What is the step distance between z planes.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	float dz;

	@ParameterMarker(uiOrder=4, name="Camera Pixel Size [µm]", description="The camera pixel size in microns. This is needed for 'absolute' quantitation and is not necessary if all you want is 'relative' quantitation (e.g., something is twice the other thing).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="6.45")
	float pixelSize;

	@ParameterMarker(uiOrder=4, name="Image Magnification", description="The magnification at which the images were taken. Used with the camera pixel size to determine the pixel width. (width = camera pixel size / magnification)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="20.0")
	float mag;

	@ParameterMarker(uiOrder=5, name="Wavelength of Light [nm]", description="This is the spectrally weighted average wavelength of the bright-field illumination. This is needed for 'absolute' quantitation and is not necessary if all you want is 'relative' quantitation (e.g., something is twice the other thing).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="600")
	float lambda;

	@ParameterMarker(uiOrder=5, name="Regularization Parameter (~0-2)", description="The regularization parameter for the equation is essentially the cuttoff a high-pass filter that takes values that typically range from 0.1-1 but needs to be >=0. This can generally be set low (0.1) and the resulting phase image can be filtered to 'remove background'.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.1")
	float r;

	@ParameterMarker(uiOrder=5, name="Threshold Parameter", description="The threshold parameter is expressed as a fraction of the max image intensity. All pixels below this min value are set to the min value to avoid dividing by 0. Typically 0.01 or 0.001.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.001")
	float thresh;

	//	@ParameterMarker(uiOrder=3, name="Double Precision (vs Float)", description="Should 'double' precision be used in the calculation?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	//	boolean dPrecision;

	@ParameterMarker(uiOrder=6, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;

	@ParameterMarker(uiOrder=7, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean keepExcluded;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Phase", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData output;

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
				if(filterTable.testMapAsExclusionFilter(map))
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
						percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
						JEXStatics.statusBar.setProgressPercentage(percentage);
					}
					else
					{
						Logs.log("Skipping the processing and saving of " + map.toString(), this);
						count = count + 1;
						percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
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
					fpLo = (new ImagePlus(imageMap.get(toGet)).getProcessor().convertToFloatProcessor());
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
				tempPath = JEXWriter.saveImage(phi);
				if(tempPath != null)
				{
					outputImageMap.put(toSave.copy(), tempPath);
				}
				toSave = toGet.copy();
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
		}

		if(outputImageMap.size() == 0)
		{
			return false;
		}

		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);

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
