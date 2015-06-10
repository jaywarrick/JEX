// Define package name as "plugins" as show here
package function.plugin.plugins.singleCell;

// Import needed classes here 
import image.roi.PointTester;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import jex.statics.JEXStatics;
import logs.Logs;
import net.imagej.ops.OpRef;
import net.imagej.ops.features.sets.FirstOrderStatFeatureSet;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;

import org.scijava.plugin.Plugin;

import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
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
	
	/////////// Define Parameters here ///////////
	
	@ParameterMarker(uiOrder=6, name="Checkbox", description="A simple checkbox for entering true/false variables.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean checkbox;
	
	/////////// Define Outputs here ///////////
	
	// See Database.Definition.OutputMarker for types of inputs that are supported (File, Image, Value, ROI...)
	@OutputMarker(uiOrder=1, name="Output Image 1", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Test output image #1", enabled=true)
	JEXData outputImage;
	
	@OutputMarker(uiOrder=2, name="Output Image 2", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Test output image #2", enabled=true)
	JEXData outputImage2;
	
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
		// Run the function
		// Read in the input with one of the many "reader" classes (see package "Database.DataReader")
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> maskMap = ImageReader.readObjectToImagePathTable(maskData);
		TreeMap<DimensionMap,String> outputMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		
		ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij.getContext());
		
		try
		{			
			//PointTester.tryOps2();
			SCIFIOImgPlus<UnsignedByteType> mask = imgOpener.openImgs(maskMap.firstEntry().getValue(), new UnsignedByteType()).get(0);
			
			ImageJFunctions.show(mask);
			
			
			// Loop through the items in the n-Dimensional object
			for (DimensionMap map : imageMap.keySet())
			{
				List<SCIFIOImgPlus<UnsignedShortType>> images = imgOpener.openImgs(imageMap.get(map), new UnsignedShortType());
				
				for(Img<UnsignedShortType> im : images)
				{
					ImageJFunctions.show(im);
					FirstOrderStatFeatureSet<Img<UnsignedShortType>> op = IJ2PluginUtility.ij.op().op(FirstOrderStatFeatureSet.class, im);
					for (final Entry<? extends OpRef,DoubleType> result : op.compute(im).entrySet())
					{
						System.out.println(result.getKey().getType().getSimpleName() + " " + result.getValue().get());
					}
					Logs.log("\n\n", PointTester.class);
				}
				
				// Update the user interface with progress
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
			
			// Return status
			return true;
		}
		catch (ImgIOException e)
		{
			e.printStackTrace();
			return false;
		}
	}
}
