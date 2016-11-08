package function.plugin.plugins.adhesion;

import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import loci.common.DataTools;
import logs.Logs;
import miscellaneous.Canceler;
import miscellaneous.FileUtility;
import miscellaneous.SimpleFileFilter;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;

import org.scijava.plugin.Plugin;

import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

@Plugin(
		type = JEXPlugin.class,
		name="Import Adhesion Timelapse",
		menuPath="Adhesion",
		visible=true,
		description="Import a timelapse (i.e., no other dimensions such as color or Z). A log frequency sweep is assumed for subsampling."
		)
public class ImportAdhesionTimelapse extends JEXPlugin {

	public ImportAdhesionTimelapse() {}

	/////////// Define Inputs ///////////

	/*
	 * None necessary; Input Directory is classified as a parameter.
	 */

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Input Directory/File", description="Location of the multicolor TIFF images", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String inDir;

	@ParameterMarker(uiOrder=1, name="File Extension", description="The type of file that is being imported. Default is tif. Not necessary if importing a single file.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="tif")
	String fileExtension;

	@ParameterMarker(uiOrder=2, name="File Name Parse Separator", description="Charactor that separates dimension names in the image name (e.g., '_' in X002_Y003.tif). Use blank (i.e., no character) to avoid parsing.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="_")
	String separator;

	@ParameterMarker(uiOrder=3, name="Montage Rows", description="If this image is a montage and is to be split, how many rows are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imRows;

	@ParameterMarker(uiOrder=4, name="Montage Cols", description="If this image is a montage and is to be split, how many cols are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imCols;

	@ParameterMarker(uiOrder=5, name="Gather channel names?", description="Transfer the name of each channel (e.g. DAPI, FITC, etc) if available in the metadata of the image. Otherwise, channels are named by index in the order they were provided by the image.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean transferNames;

	@ParameterMarker(uiOrder=6, name="Initial Frequency [Hz]", description="Initial frequency of actuation.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2.0")
	double fi;

	@ParameterMarker(uiOrder=7, name="Final Frequency [Hz]", description="Final frequency of actuation.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.1")
	double ff;

	@ParameterMarker(uiOrder=9, name="Final Frame", description="Final frequency of actuation.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.1")
	double tf;

	@ParameterMarker(uiOrder=10, name="Time per Frame [s]", description="Time between frames", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.05")
	double frameTime;

	@ParameterMarker(uiOrder=11, name="Desired Frames Per Period", description="Number of frames desired for each period of actuation. Frames exceeding this frame rate will be omitted.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="30")
	double framesPerPeriod;

	@ParameterMarker(uiOrder=12, name="Binning", description="n X n binning to apply during import. (1 means do not bin)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int binning;
	
	@ParameterMarker(uiOrder=13, name="Open file to read metadata?", description="Setting this to true helps to read challenging formats such as ND2 but is slower per file. Can generally be false for basic file types like TIF.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean checker;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Imported Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The imported image object", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {

		tf = tf * frameTime;

		// GATHER DATA FROM PARAMETERS
		// create file object for input directory
		File filePath = new File(inDir);
		//boolean autoNameGathering = transferNames.equals("automatic");
		boolean autoNameGathering = transferNames;

		DimTable table = null;

		// FIGURE OUT IF ONE OR MULTIPLE FILES ARE BEING IMPORTED
		List<File> pendingImageFiles; // contains either one or multiple image files; depends on whether one file is selected or a whole directory

		if(!filePath.exists())
		{
			JEXDialog.messageDialog("Warning! The file or folder specified could not be found.");
			return false;
		}
		if (filePath.isDirectory()) { // multiple files
			// gather and sort the input files
			JEXStatics.statusBar.setStatusText("Sorting files to convert. May take minutes...");
			pendingImageFiles = FileUtility.getSortedFileList(filePath.listFiles(new SimpleFileFilter(new String[] { fileExtension })));
		}
		else { // one file
			// initialize pendingImageFiles and add the single file
			JEXStatics.statusBar.setStatusText("Adding file "+inDir+" to list.");
			pendingImageFiles = new Vector<File>();
			pendingImageFiles.add(filePath);
		}

		TreeMap<DimensionMap,String> multiMap = new TreeMap<DimensionMap,String>();
		boolean fileNotFound = false;
		final SCIFIO scifio = new SCIFIO(IJ2PluginUtility.ij().getContext());
		Reader reader = null;
		for (File f: pendingImageFiles)
		{
			if(!f.exists())
			{
				fileNotFound = true;
				continue;
			}

			if(this.isCanceled())
			{
				return false;
			}
			// usually x and y coordinate map
			DimensionMap baseMap = this.getMapFromPath(f.getAbsolutePath(), separator);

			// get reader for image file
			
			if(reader == null)
			{
				try
				{
					reader = scifio.initializer().initializeReader(f.getAbsolutePath(), new SCIFIOConfig().checkerSetOpen(checker));
				}
				catch (Exception e)
				{
					Logs.log("Couldn't initialize reader for file " + filePath, Logs.ERROR, this);
					e.printStackTrace();
					return false;
				}
			}
			
			// 	get table from reader		
			table = getDimTableFromReader(reader, autoNameGathering);
			if(table == null)
			{
				JEXDialog.messageDialog("Function canceled due to issues with determining dimensions of the image.");
				return false;
			}

			if(reader.getImageCount() > 1)
			{
				Dim loc = new Dim("Location",reader.getImageCount());
				table.add(0, loc);
			}
			Iterator<DimensionMap> itr = table.getMapIterator().iterator();
			double total = reader.getImageCount() * reader.getPlaneCount(0);
			double count = 0;
			LogFreqSweep lfs = new LogFreqSweep(fi, ff, tf);
			TreeSet<Integer> trueFrames = lfs.getTrueFrames(total, frameTime, framesPerPeriod);
			JEXStatics.statusBar.setProgressPercentage(0);
			for (int i = 0; i < reader.getImageCount(); i++)
			{
				for (int j = 0; j < reader.getPlaneCount(i); j++)
				{
					if(!trueFrames.contains((int) count))
					{
						// Skip over frames we don't need.
						@SuppressWarnings("unused")
						DimensionMap tempMap = itr.next();
						JEXStatics.statusBar.setProgressPercentage((int) (100.0 * count / total));
						count = count + 1;
						continue;
					}
					Plane plane;
					try
					{
						plane = reader.openPlane(i, j);
					}
					catch (Exception e)
					{
						Logs.log("Couldn't read image " + i + " plane " + j + " in " + f.getAbsolutePath() + ". Skipping to next plane.", Logs.ERROR, this);
						e.printStackTrace();
						continue;
					}
					ImageMetadata d = plane.getImageMetadata();
					long[] dims = d.getAxesLengthsPlanar();
					ImageProcessor ip = null;
					if(d.getBitsPerPixel() <= 8)
					{
						byte[] converted = (byte[]) DataTools.makeDataArray(plane.getBytes(), 1, false, d.isLittleEndian());
						ip = new ByteProcessor((int)dims[0], (int)dims[1], converted, null);
					}
					else if(d.getBitsPerPixel() >= 9 && d.getBitsPerPixel() <= 16)
					{
						short[] converted = (short[]) DataTools.makeDataArray(plane.getBytes(), 2, false, d.isLittleEndian());
						ip = new ShortProcessor((int)dims[0], (int)dims[1], converted, null);
					}
					else if(d.getBitsPerPixel() >= 17 && d.getBitsPerPixel() <= 32)
					{
						float[] converted = (float[]) DataTools.makeDataArray(plane.getBytes(), 4, true, d.isLittleEndian());
						ip = new FloatProcessor((int)dims[0], (int)dims[1], converted, null);
					}
					else
					{
						Logs.log("Couldn't handle writing of image with this particular bits-per-pixel: " + d.getBitsPerPixel(), Logs.ERROR, this);
						return false;
					}


					if(this.isCanceled())
					{
						return false;
					}

					// For each image split it if necessary
					if(imRows * imCols > 1)
					{
						TreeMap<DimensionMap,ImageProcessor> splitImages = splitRowsAndCols(ip, imRows, imCols, this);
						// The above might return null because of being canceled. Catch cancel condition and move on.
						if(this.isCanceled())
						{
							return false;
						}
						DimensionMap map = itr.next().copy();
						for(Entry<DimensionMap,ImageProcessor> e : splitImages.entrySet())
						{
							ImageProcessor temp = e.getValue();
							if(binning > 1)
							{
								temp = temp.bin(binning);
							}
							String filename = JEXWriter.saveImage(temp);
							map.putAll(e.getKey());
							multiMap.put(map.copy(),filename);
							Logs.log(map.toString() + " :: " + filename, this);
							temp = null;
						}
						splitImages.clear();
					}
					else
					{
						if(binning > 1)
						{
							ip = ip.bin(binning);
						}
						String filename = JEXWriter.saveImage(ip);
						DimensionMap map = itr.next().copy();
						map.putAll(baseMap.copy());
						multiMap.put(map,filename);
						Logs.log(map.toString() + " = " + filename, this);
						ip = null;
					}					

					JEXStatics.statusBar.setProgressPercentage((int) (100.0 * count / total));
					count = count + 1;
				}
			}
		}

		if(fileNotFound)
		{
			JEXDialog.messageDialog("Warning! At least one of the files specified for this function was not found.");
		}

		// OUTPUT PROCESSING
		output = ImageWriter.makeImageStackFromPaths(output.name, multiMap);
		//		if (table != null) {
		//			DimTable toSet = new DimTable(multiMap);
		//			for(Dim d : table)
		//			{
		//				toSet.removeDimWithName(d.dimName);
		//			}
		//			for(Dim d : table)
		//			{
		//				toSet.add(d.copy());
		//			}
		//			output.setDimTable(toSet);
		//		}

		return true;
	}

	/**
	 * Create DimensionMap of a given image 
	 * The image name should be in certain format, ex. Image_x001_y002_z004.tif
	 * 
	 * @param filePath image Path and Name
	 * @param separator separator of the image Name
	 * @return
	 */
	public DimensionMap getMapFromPath(String filePath, String separator) {
		String name = FileUtility.getFileNameWithoutExtension(filePath);
		String[] names = name.split(separator);

		DimensionMap dimMap = new DimensionMap();
		String dimValue, dimName, temp;
		int splitIndex = 0;

		for (int i = 0; i < names.length; i++){
			temp = names[i];

			// find the first Digit in the string in order to separate dimName and dimValue
			for (int j = 0; j < temp.length(); j++){
				if (Character.isDigit(temp.charAt(j))){
					splitIndex = j;
					break;
				}
				else
					splitIndex = 0;
			}

			// if the string is not a dimName followed by a dimValue then skip it.
			if (splitIndex != 0) {
				dimName = temp.substring(0, splitIndex);
				dimValue = temp.substring(splitIndex);

				dimMap.put(dimName, dimValue);
			}
		}

		return dimMap;

	}

	private static DimTable getDimTableFromReader(Reader r, boolean transferNames)
	{
		DimTable ret = new DimTable();
		try
		{
			ImageMetadata meta = r.openPlane(0, 0).getImageMetadata();
			long[] lengths = meta.getAxesLengthsNonPlanar();
			List<CalibratedAxis> axes = meta.getAxesNonPlanar();
			for(int i = lengths.length-1; i > -1; i--)
			{
				ret.add(new Dim(axes.get(i).type().getLabel(), (int) lengths[i]));
			}
		}
		catch (FormatException | IOException e)
		{
			e.printStackTrace();
			return null; // Cancels function
		}

		if(transferNames)
		{
			TreeMap<String,String> colors = new TreeMap<String,String>();
			for(Entry<String,Object> e : r.getMetadata().getTable().entrySet())
			{
				if(e.getKey().contains("Name #"))
				{
					colors.put(e.getKey().toString().trim(), e.getValue().toString().trim());
				}
			}
			try
			{
				String[] colorNames = ((String[]) colors.values().toArray(new String[]{}));
				Vector<String> colorNamesList = new Vector<String>();
				int i = ret.indexOfDimWithName(Axes.CHANNEL.getLabel());
				int size = ret.get(i).size();
				if(size == colorNames.length-1)
				{
					// This is because the first name found in the metadata is usually just the current setting of the scope or something rather than the name of the setting used to capture the first image.
					for(int j = 1; j < colorNames.length; j++)
					{
						colorNamesList.add(colorNames[j]);
					}
				}
				else
				{ // Just guessing now.
					for(int j = 0; j < colorNames.length; j++)
					{
						colorNamesList.add(colorNames[j]);
					}
				}

				// Check if there are any duplicate names and add something to the name to make it not a duplicate...
				Vector<String> newColorNamesList = new Vector<String>();
				for(int j = 0; j < colorNamesList.size(); j++)
				{
					String newName = colorNamesList.get(j);
					List<String> subList = colorNamesList.subList(j+1, colorNamesList.size());
					while(subList.contains(newName))
					{
						newName = newName + "_copy";
					}
					newColorNamesList.add(newName);
				}
				colorNamesList = newColorNamesList;

				Dim newColorDim = new Dim(Axes.CHANNEL.getLabel(), colorNamesList); // Using a TreeMap and the TreeMap.values() provides and ordered list based on the order of the "Name #x" key from the non-ordered HashMap of the MetaTable

				if(newColorDim.size() > size)
				{
					int choice = JEXDialog.getChoice("What should I do?", "The number of colors/channels does not match the number of possible channel names. Should we replace the indices of colors with the supposed names which might not be correct or just leave the indices?", new String[]{"Replace Names Anyway","Leave as Indices"}, 0);
					if(choice == 0)
					{
						newColorDim = new Dim("Color", newColorDim.valuesStartingAt(newColorDim.size() - size)); // Best guess appears to be last named color settings
						ret.set(i, newColorDim);
					}
					else if(choice == -1)
					{
						return null; // Cancels function
					}
				}
				else if(newColorDim.size() < size)
				{
					JEXDialog.messageDialog("Couldn't find enough color setting names for each color in the image set.\n\nLeaving indices instead of replacing indices with names.");
				}
				else
				{
					ret.set(i, newColorDim);
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				// Just means there is no color dimension
				// Don't worry about it.
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
}