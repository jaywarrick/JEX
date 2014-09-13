package function.plugin.plugins.io;

import function.plugin.IJ2.IJ2PluginUtility;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.Metadata;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.Writer;
import io.scif.config.SCIFIOConfig;
import io.scif.io.Location;

import java.io.IOException;
import java.util.List;
import java.util.TreeMap;

import logs.Logs;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;
import net.imglib2.meta.CalibratedAxis;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;


public class TestRead {
	
	public long xyRange=10;
	
	public static void main(String[] args) throws FormatException, IOException
	{
		//		final String filePath = "/Users/jaywarrick/Documents/JEX/Raw Data/TestImport/x0_y0_Time0.tif";
		//		final String filePath = "/Users/jaywarrick/Documents/JEX/Raw Data/TestImport/";
		//		final String filePath = "/Users/jaywarrick/Google Drive/example.nd2";
		final String hugeImage = "hugePlane&lengths=70000,80000.fake";
		readTiles(hugeImage, 8, 7);
	}
	
	public static TreeMap<DimensionMap,String> readTiles(String path, int rows, int cols) throws FormatException, IOException
	{
		// As always we'll need a SCIFIO for this tutorial
		final SCIFIO scifio = new SCIFIO(IJ2PluginUtility.ij.getContext());
		// This time we're going to set up an imageID with planes that won't fit
		// in a 2GB byte array.
		
//		final SCIFIO scifio = new SCIFIO();
		final String sampleImage =
		"8bit-signed&pixelType=int8&lengths=50,50,3,5,7&axes=X,Y,Z,Channel,Time.fake";
		// We'll need a path to write to. By making it a ".png" we are locking in
		// the final format of the file on disk.
		final String outPath = "SCIFIOTutorial.png";
		// Clear the file if it already exists.
		final Location l = new Location(scifio.getContext(), outPath);
		if (l.exists()) l.delete();
		// We'll need a reader for the input image
		final Reader reader = scifio.initializer().initializeReader(sampleImage);
		// .. and a writer for the output path
		final Writer writer =
		scifio.initializer().initializeWriter(sampleImage, outPath);
		// Note that these initialize methods are used for convenience.
		// Initializing a reader and a writer requires that you set the source
		// and metadata properly. Also note that the Metadata attached to a writer
		// describes how to interpret the incoming Planes, but may not reflect
		// the image on disk - e.g. if planes were saved in a different order
		// than on the input image. For accurate Metadata describing the saved
		// image, you need to re-parse it from disk.
		// Anyway, now that we have a reader and a writer, we can save all the
		// planes. We simply iterate over each image, and then each plane, writing
		// the planes out in order.
		for (int i = 0; i < reader.getImageCount(); i++) {
		for (int j = 0; j < reader.getPlaneCount(i); j++) {
		writer.savePlane(i, j, reader.openPlane(i, j));
		}
		}
		// Note that this code is for illustration purposes only.
		// A more general solution would need higher level API that could account
		// for larger planes, etc..
		// close our components now that we're done. This is a critical step, as
		// many formats have footer information that is written when the writer is
		// closed.
		reader.close();
		writer.close();

		
		
		// We initialize a reader as we did before
//		final Reader reader = scifio.initializer().initializeReader(path, new SCIFIOConfig().checkerSetOpen(true));
		// In T1b we saw that the naive use of the API to open planes doesn't
		// work when the individual planes are too large.
		// To open tiles of the appropriate size, we'll need some basic information
		// about this dataset, so let's get a reference to its metadata.
		final Metadata meta = reader.getMetadata();
		// Iterate over each image in the dataset (there's just one in this case)
		for (int i = 0; i < reader.getImageCount(); i++) {
			ImageMetadata iMeta = meta.get(i);
			// These methods will compute the optimal width to use with
			// reader#openPlane
			final long optimalTileWidth = iMeta.getAxisLength(Axes.X)/cols;
			if(cols*optimalTileWidth != iMeta.getAxisLength(Axes.X))
			{
				Logs.log("Number of columns provided doesn't divide image width evenly... clipping to make even.", TestRead.class);
			}
			final long optimalTileHeight = iMeta.getAxisLength(Axes.Y)/rows;
			if(rows*optimalTileHeight != iMeta.getAxisLength(Axes.Y))
			{
				Logs.log("Number of columns provided doesn't divide image width evenly... clipping to make even.", TestRead.class);
			}
			// Then we need to figure out how many tiles are actually present in a
			// plane, given the tile height and width
			final long tilesWide = cols;
			final long tilesHigh = rows;
			// now we can open each tile, one at a time, for each plane in this image
			
			// Create a plane to reuse over and over
			final long actualTileWidth = optimalTileWidth;
			final long actualTileHeight = optimalTileHeight;
			Plane p = null;
			long x, y = 0;
			Dim ImRow = new Dim("ImRow", 0, rows-1);
			Dim ImCol = new Dim("ImCol", 0, cols-1);
			DimTable table = new DimTable();
			table.add(ImCol);
			table.add(ImRow);
			for (int j = 0; j < iMeta.getPlaneCount(); j++)
			{
				for(DimensionMap map : table.getMapIterator())
				{
					x = Integer.parseInt(map.get("ImCol")) * optimalTileWidth;
					y = Integer.parseInt(map.get("ImRow")) * optimalTileHeight;
					
					if(p == null)
					{
						p =	reader.openPlane(i, j, new long[] { x, y }, new long[] {actualTileWidth, actualTileHeight});
						// Here we would do any necessary processing of each tile's bytes.
						// In this, we'll just print out the plane and position.
					}
					else
					{
						reader.openPlane(i, j, p, new long[] { x, y }, new long[] {actualTileWidth, actualTileHeight});
					}
					
					// Do something
					
					
					// Finish
					
				}
			}
		}
	}
	
	// This will be encoded in the AXES information and meta data for each image
	public DimTable getPlaneDimensions(int planes, Metadata iMeta)
	{
		final List<CalibratedAxis> dimOrderList =
				iMeta.get(0).getAxes();
		AxisType[] dimOrder = new AxisType[dimOrderList.size()];
		for(int i = 0; i < dimOrderList.size(); i++)
		{
			dimOrder[i] = dimOrderList.get(i).type();
		}
		final long sizeZ = iMeta.get(0).getAxisLength(Axes.Z);
		final long sizeT =
				iMeta.get(0).getAxisLength(Axes.TIME);
		final long sizeC =
				iMeta.get(0).getAxisLength(Axes.CHANNEL);
		final boolean certain = iMeta.get(0).isOrderCertain();
		
		Dim z = new Dim("Z", (int) sizeZ);
		Dim t = new Dim("Time", (int) sizeT);
		Dim c = new Dim("Color", (int) sizeC);
		
		DimTable ret = new DimTable();
		ret.add(z);
		ret.add(t);
		ret.add(c);
		return ret;
		
	}
	
	// This will be encoded in the 
	public DimTable getImageDimensions(int images, Metadata iMeta)
	{
		return null;
	}
}
