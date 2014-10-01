package function.plugin.plugins.Import;

import ij.process.Blitter;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import image.roi.PointTester;
import io.scif.ImageMetadata;
import io.scif.Metadata;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import jex.statics.JEXStatics;
import loci.common.DataTools;
import logs.Logs;
import miscellaneous.Canceler;

import org.scijava.plugin.Plugin;

import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ValueReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Import ND2 Files",
		menuPath="Import",
		visible=true,
		description="Import image sets saved as a single multi-dimensional ND2 file."
		)
public class ImportND2Files extends JEXPlugin {
	
	final public static String LAMBDA1="λ", LAMBDA2="� ";
	
	public ImportND2Files()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(name="Path", type=MarkerConstants.TYPE_VALUE, description="Value containing the file path to the ND2 file to be imported.", optional=false)
	JEXData path;
	
	///////// Define Parameters //////////
	
	@ParameterMarker(uiOrder=1, name="ImRows", description="Number of rows to split each image into.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imRows;
	
	@ParameterMarker(uiOrder=1, name="ImCols", description="Number of cols to split each image into.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imCols;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(name="Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The imported image", enabled=true)
	JEXData output;
	
	@Override
	public int getMaxThreads()
	{
		return 1;
	}
	
	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		// Get the path
		String nd2File = ValueReader.readValueObject(path);
		
		// Get the tiffs according to stuff in PointTester (need to modify to split ImRows and ImCols)
		final SCIFIO scifio = new SCIFIO(IJ2PluginUtility.ij.getContext());
		Reader reader;
		try
		{
			reader = scifio.initializer().initializeReader(nd2File, new SCIFIOConfig().checkerSetOpen(true));
		}
		catch (Exception e)
		{
			Logs.log("Couldn't initialize ND2 file reader for file " + path, Logs.ERROR, this);
			e.printStackTrace();
			return false;
		}
		
		Metadata meta = reader.getMetadata();
		
		DimTable table = getDimTable(meta);
		
		TreeMap<DimensionMap,String> ret = new TreeMap<DimensionMap,String>();
		Iterator<DimensionMap> itr = table.getMapIterator().iterator();
		double total = reader.getImageCount() * reader.getPlaneCount(0);
		double count = 0;
		JEXStatics.statusBar.setProgressPercentage(0);
		for (int i = 0; i < reader.getImageCount(); i++) {
			for (int j = 0; j < reader.getPlaneCount(i); j++) {
				Plane plane;
				try
				{
					plane = reader.openPlane(i, j);
				}
				catch (Exception e)
				{
					Logs.log("Couldn't read image " + i + " plane " + j + " in " + path + ". Skipping to next plane.", Logs.ERROR, this);
					e.printStackTrace();
					continue;
				}
				ImageMetadata d = plane.getImageMetadata();
				long[] dims = d.getAxesLengthsPlanar();
				short[] converted = (short[]) DataTools.makeDataArray(plane.getBytes(), 2, false, d.isLittleEndian());
				ShortProcessor p = new ShortProcessor((int)dims[0], (int)dims[1], converted, null);
				
				if(this.isCanceled())
				{
					return false;
				}
				
				// For each image split it if necessary
				if(imRows * imCols > 1)
				{
					TreeMap<DimensionMap,ImageProcessor> splitImages = splitRowsAndCols(p, imRows, imCols, this);
					// The above might return null because of being canceled. Catch cancel condition and move on.
					if(this.isCanceled())
					{
						return false;
					}
					DimensionMap map = itr.next().copy();
					for(Entry<DimensionMap,ImageProcessor> e : splitImages.entrySet())
					{
						String filename = JEXWriter.saveImage(e.getValue());
						map.putAll(e.getKey());
						ret.put(map.copy(),filename);
						Logs.log(map.toString() + " :: " + filename, PointTester.class);
					}
				}
				else
				{
					String filename = JEXWriter.saveImage(p);
					DimensionMap map = itr.next().copy();
					ret.put(map,filename);
					Logs.log(map.toString() + " = " + filename, PointTester.class);
				}
				JEXStatics.statusBar.setProgressPercentage((int) (100.0 * count / total));
				count = count + 1;
			}
		}
		
		// Set the output
		output = ImageWriter.makeImageStackFromPaths("temp", ret);

		return true;
	}
	
	public static TreeSet<DimensionMap> getSplitDims(int rows, int cols)
	{
		TreeSet<DimensionMap> ret = new TreeSet<DimensionMap>();
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				
				ret.add(new DimensionMap("ImRow=" + r + ",ImCol=" + c));
			}
		}
		return ret;
	}
	
	public static TreeMap<DimensionMap,ImageProcessor> splitRowsAndCols(ImageProcessor imp, int rows, int cols, Canceler canceler)
	{
		TreeMap<DimensionMap,ImageProcessor> ret = new TreeMap<DimensionMap,ImageProcessor>();
		
		int wAll = imp.getWidth();
		int hAll = imp.getHeight();
		int w = wAll / cols;
		int h = hAll / rows;
		
		for (int r = 0; r < rows; r++)
		{
			for (int c = 0; c < cols; c++)
			{
				if(canceler.isCanceled())
				{
					return null;
				}
				int x = c * w;
				int y = r * h;
				Rectangle rect = new Rectangle(x, y, w, h);
				imp.setRoi(rect);
				ImageProcessor toCopy = imp.crop();
				ImageProcessor toSave = imp.createProcessor(w, h);
				toSave.copyBits(toCopy, 0, 0, Blitter.COPY);
				ret.put(new DimensionMap("ImRow=" + r + ",ImCol=" + c), toSave);
			}
		}
		
		return ret;
	}
	
	private static DimTable getDimTable(Metadata meta)
	{
		String info = meta.getTable().get("Dimensions").toString();
		String[] infos = info.split("[ ][x][ ]");
		DimTable ret = new DimTable();
		for(String s : infos)
		{
			if(!s.equals("x"))
			{
				String[] bits = s.split("[(|)]");
				if(bits[0].equals(LAMBDA1) || bits[0].equals(LAMBDA2))
				{
					bits[0] = "Color";
				}
				Dim toAdd = new Dim(bits[0], Integer.parseInt(bits[1]));
				ret.add(toAdd);
			}
		}
		
		Vector<String> colors = new Vector<String>();
		for(Entry<String,Object> e : meta.getTable().entrySet())
		{
			if(e.getKey().contains("Name #"))
			{
				colors.add(e.getValue().toString().trim());
			}
		}
		Dim newColorDim = new Dim("Color", colors);
		ret.set(ret.indexOfDimWithName("Color"), newColorDim);
		return ret;
	}
}
