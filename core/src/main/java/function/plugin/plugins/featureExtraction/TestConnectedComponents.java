package function.plugin.plugins.featureExtraction;

import java.io.File;

import ij.ImagePlus;
import ij.io.FileSaver;
import miscellaneous.DirectoryManager;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;

public class TestConnectedComponents {
	
	public static void main(String[] args) throws Exception
	{
		/**
		 * I uploaded the sample image "C:/Users/MMB/Desktop/For ImageJ Forum/Objects.tif" using the FIJI upload sample image tool.
		 * 
		 * Here's the info from that process.
		 * 
		 * <!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
		 * <html><head>
		 * <title>201 Created</title>
		 * </head><body>
		 * <h1>Created</h1>
		 * <p>Resource /Objects.tif has been created.</p>
		 * <hr />
		 * <address>Apache/2.2.22 (Ubuntu) Server at upload.imagej.net Port 80</address>
		 * </body></html>
		 * 
		 * Here is the resulting file...
		 * <!DOCTYPE HTML PUBLIC "-//IETF//DTD HTML 2.0//EN">
		 * <html><head>
		 * <title>201 Created</title>
		 * </head><body>
		 * <h1>Created</h1>
		 * <p>Resource /LabeledObjects.tif has been created.</p>
		 * <hr />
		 * <address>Apache/2.2.22 (Ubuntu) Server at upload.imagej.net Port 80</address>
		 * </body></html>
		 */
		ImagePlus temp = new ImagePlus("/Users/jaywarrick/Desktop/Blob.tif");
		Img<UnsignedByteType> objects = ImageJFunctions.wrapByte(temp);
		StructuringElement se = ConnectedComponents.StructuringElement.FOUR_CONNECTED;

		long[] dimensions = new long[objects.numDimensions()];
		objects.dimensions(dimensions);
		final Img< UnsignedShortType > labeledObjects = ArrayImgs.unsignedShorts( dimensions );
		
		FeatureUtils utils = new FeatureUtils();
		ImgLabeling<Integer,IntType> cellLabeling = utils.getLabeling(objects, true);
		
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop");
		LabelRegions<Integer> cellRegions = new LabelRegions<Integer>(cellLabeling);
		utils.show(utils.getLabeling(objects, true), false);
		utils.show(objects, false);
		
		// Determine which LabelRegions are the ones we want to keep by
		// testing if our maxima of interest are contained.
		for (LabelRegion<Integer> cellRegion : cellRegions) {
			System.out.println(cellRegion.getLabel() + " = " + cellRegion.size() + " pixels big.");
		}
		
		ConnectedComponents.labelAllConnectedComponents(objects, labeledObjects, se);
		File f = File.createTempFile("LabeledObjects", ".tif");
		
		ImagePlus temp2 = ImageJFunctions.wrap(labeledObjects, "Labeled Objects");
		FileSaver fs = new FileSaver(temp2);
		fs.saveAsTiff(f.getAbsolutePath());
		
		System.out.println("Saved the file at: " + f.getAbsolutePath());
		
	}
}
