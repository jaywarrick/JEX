package function.plugin.plugins.imageProcessing;

import java.util.TreeMap;

import org.jtransforms.dct.FloatDCT_2D;
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
import function.plugin.plugins.featureExtraction.FeatureUtils;
import ij.ImagePlus;
import ij.process.FloatProcessor;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FileUtility;
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
		name="Phase Calculator",
		menuPath="Image Processing",
		visible=true,
		description="Take a z-stack of brightfield images and calculate the phase shift of light through the sample."
		)
public class PhaseCalculator extends JEXPlugin {

	public PhaseCalculator()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Z Dim Name", description="Name of the 'Z' dimension for this image object.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Z")
	String zDimName;
	
	@ParameterMarker(uiOrder=1, name="z-step [µm]", description="What is the step distance between z planes.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double dz;
	
	@ParameterMarker(uiOrder=1, name="Camera Pixel Size [µm]", description="The camera pixel size in microns. This is needed for 'absolute' quantitation and is not necessary if all you want is 'relative' quantitation (e.g., something is twice the other thing).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="6.45")
	double pixelSize;
	
	@ParameterMarker(uiOrder=2, name="Wavelength of Light [nm]", description="This is the spectrally weighted average wavelength of the bright-field illumination. This is needed for 'absolute' quantitation and is not necessary if all you want is 'relative' quantitation (e.g., something is twice the other thing).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="580")
	double lambda;
	
	@ParameterMarker(uiOrder=3, name="Double Precision (vs Float)", description="Should 'double' precision be used in the calculation?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean dPrecision;
	
	@ParameterMarker(uiOrder=4, name="Exclusion Filter DimTable", description="Filter specific dimension combinations from analysis. (Format: <DimName1>=<a1,a2,...>;<DimName2>=<b1,b2...>)", ui = MarkerConstants.UI_TEXTFIELD, defaultText = "")
	String filterDimTableString;
	
	@ParameterMarker(uiOrder=5, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
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
		FeatureUtils fu = new FeatureUtils();
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
		DimTable dt = imageData.getDimTable();
//		DimTable dt = imageData.getDimTable().getSubTable(this.zDimName);
//		Dim zDim = dt.getDimWithName(this.zDimName);
//		if(zDim.size() < 3)
//		{
//			JEXDialog.messageDialog("The image does not contain at least 3 Z-planes. Aborting.", this);
//			return false;
//		}
		for (DimensionMap map : dt.getMapIterator())
		{
			// get the med and hi float processors
//			for(int i = 0; i < zDim.size()-2; i++)
//			{
				// Copy med and hi FPs to lo and med FPs
				// Get new hi FP
				// getdIdz
				
				if(filterTable.testMapAsExclusionFilter(map))
				{
					if(this.keepExcluded)
					{
						Logs.log("Skipping the processing of " + map.toString(), this);
						ImagePlus out = new ImagePlus(imageMap.get(map));
						tempPath = JEXWriter.saveImage(out);
						if(tempPath != null)
						{
							outputImageMap.put(map, tempPath);
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
				
				/**
				 * Algorithm from:
				 * Boundary-artifact-free phase retrieval with the transport of intensity equation: fast solution with use of discrete cosine transform
				 * Chao Zuo,1,2,3 Qian Chen,1,* and Anand Asundi2
				 * 
				 * k = 2pi/lambda (wave number)
				 * 
				 * λ(m,n)=-π^2(m^2/a^2 + n^2/b^2)
				 * 
				 * (1) Calculate the intensity derivative term −k ∂I ∂z from the measured intensity images.
				 * (2) Transform − k∂I/∂z into cosine frequency domain by DCT (Eq. (43)).(Eq. (44)).
				 * (3) Reweight the DCT coefficients by λ^(−1)_(m,n) (Eq. (27))
				 * (4) Transform the modified DCT coefficient back to spatial domain to get ψ (5) Calculate ∇ψ based on DCT (Eqs. (53), (55), and (58)).
				 * (6) Calculate I −1∇ψ .
				 * (7) Calculate∇⋅(I−1∇ψ )(Eqs. (53), (55), and (59)).
				 * (8) Transform ∇ ⋅ (I −1∇ψ ) into cosine frequency domain by DCT. (9) Reweight the DCT coefficients by λ^(−1)_(m,n)
				 * (10) The phase can be determined up to an additive constant by transforming the modifiedDCT coefficient back to spatial domain. Steps (8)-(10) are analogous to steps (2)-(4).
				 * 
				 * Helpful description of gradient, divergence, and laplacian
				 * https://math.stackexchange.com/questions/690493/gradient-and-divergence
				 */
				
				// (1)
				
				// test DCT
				FloatProcessor fp = (new ImagePlus(imageMap.get(map))).getProcessor().convertToFloatProcessor();
				FloatDCT_2D dct = new FloatDCT_2D(fp.getHeight(), fp.getWidth());
				dct.forward((float[]) fp.getPixels(), false);
				FileUtility.showImg(fp, true);
				dct.inverse((float[]) fp.getPixels(), false);
				FileUtility.showImg(fp, true);
				
//				tempPath = JEXWriter.saveImage(imp);
//				if(tempPath != null)
//				{
//					outputImageMap.put(map, tempPath);
//				}
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
//			}
		}
		if(outputImageMap.size() == 0)
		{
			return false;
		}
		
		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);
		
		// Return status
		return true;
	}
	
	private FloatProcessor getdIdz(FloatProcessor lo, FloatProcessor med, FloatProcessor hi)
	{
		// Calc gradient
		// Multiply by -k
		return null;
	}
	
}
