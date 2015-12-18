package function.plugin.plugins.Import;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
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
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import loci.common.DataTools;
import logs.Logs;
import miscellaneous.Canceler;
import miscellaneous.FileUtility;
import miscellaneous.SimpleFileFilter;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

@Plugin(
		type = JEXPlugin.class,
		name="Import <Prefix><LetterRow><NumberCol>.<ext> (SCIFIO)",
		menuPath="Import",
		visible=true,
		description="Import a single file that matches the filenaming format provided from (nearly) any format and (nearly) any dimensionality"
				+ "(e.g. an ND2 file or tif stack)"
		)
public class ImportPrefixLetterNumberImages_SCIFIO extends JEXPlugin {

	public ImportPrefixLetterNumberImages_SCIFIO() {}

	/////////// Define Inputs ///////////

	/*
	 * None necessary; Input Directory is classified as a parameter.
	 */

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Input Directory/File", description="Location of the multicolor TIFF images", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String inDir;
	
	@ParameterMarker(uiOrder=1, name="File Prefix", description="Prefix in the file name preceeding the RowLetterColumnNumber.fileExtension name format.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String prefix;
	
	@ParameterMarker(uiOrder=1, name="Column Num Digits", description="Number of digits used to define the column number (e.g., 02 is represented with 2 digits)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="2")
	int digits;

	@ParameterMarker(uiOrder=2, name="File Extension", description="The type of file that is being imported. Default is nd2.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="nd2")
	String fileExtension;

	@ParameterMarker(uiOrder=3, name="File Name Parse Separator", description="Charactor that separates dimension names in the image name (e.g., '_' in X002_Y003.tif). Use blank (i.e., no character) to avoid parsing.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String separator;

	@ParameterMarker(uiOrder=4, name="Montage Rows", description="If this image is a montage and is to be split, how many rows are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imRows;

	@ParameterMarker(uiOrder=5, name="Montage Cols", description="If this image is a montage and is to be split, how many cols are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imCols;

	@ParameterMarker(uiOrder=6, name="Gather channel names?", description="Transfer the name of each channel (e.g. DAPI, FITC, etc) if available in the metadata of the image. Otherwise, channels are named by index in the order they were provided by the image.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean transferNames;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Imported Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The imported image object", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {
		// GATHER DATA FROM PARAMETERS
		// create file object for input directory
		File filePath = new File(inDir);

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
		
		// Filter files for this particular entry
		String expectedFileName = prefix + getEntryString(optionalEntry.getTrayY(), optionalEntry.getTrayX());
		List<File> newFileList = new Vector<>();
		for(File file : pendingImageFiles)
		{
			if(FileUtility.getFileNameWithoutExtension(file.getAbsolutePath()).equals(expectedFileName))
			{
				newFileList.add(file);
				break;
			}
		}
		if(newFileList.size() == 0)
		{
			Logs.log("No file named '" + expectedFileName + "." + this.fileExtension + "' found for X:" + optionalEntry.getTrayX() + " Y:" + optionalEntry.getTrayY(), this);
			return false;
		}

		// DO something
		output = importFiles(newFileList, this.separator, this.fileExtension, this.imRows, this.imCols, "ImRow", "ImCol", this.transferNames, this);

		return true;
	}
	
	public String getColString(int trayX)
	{
		String formatted = String.format("%0" + digits + "d", trayX);
		return formatted;
	}
	
	public String getEntryString(int trayY, int trayX)
	{
		StringBuilder sb = new StringBuilder();
		int rowMultiple = (trayY / 26);
		for(int i = 0; i < rowMultiple; i++)
		{
			sb.append(getRowString(rowMultiple));
		}
		sb.append(getRowString(trayY));
		sb.append(getColString(trayX + 1));
		return sb.toString();
	}
	
	public String getRowString(int trayY)
	{
		return String.valueOf((char)((trayY % 26) + 65));
	}

	/**
	 * Create DimensionMap of a given image 
	 * The image name should be in certain format, ex. Image_x001_y002_z004.tif
	 * 
	 * @param filePath image Path and Name
	 * @param separator separator of the image Name
	 * @return
	 */
	public static DimensionMap getMapFromPath(String filePath, String separator) {
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

	public static TreeMap<DimensionMap,ImageProcessor> splitRowsAndCols(ImageProcessor imp, int rows, int cols, String rowName, String colName, Canceler canceler)
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
				ret.put(new DimensionMap(rowName + "=" + r + "," + colName + "=" + c), toSave);
			}
		}
		return ret;
	}

	public static JEXData importFiles(List<File> pendingImageFiles, String parseFileNameSeparator, String fileExtension, int imRows, int imCols, String rowName, String colName, boolean autoNameGathering, Canceler canceler)
	{
		DimTable table = null;
		
		TreeMap<DimensionMap,String> multiMap = new TreeMap<DimensionMap,String>();
		boolean fileNotFound = false;
		for (int fi = 0; fi < pendingImageFiles.size(); fi++)
		{
			File f = pendingImageFiles.get(fi);
			if(!f.exists())
			{
				fileNotFound = true;
				continue;
			}

			if(canceler.isCanceled())
			{
				return null;
			}
			// usually x and y coordinate map
			DimensionMap baseMap = getMapFromPath(f.getAbsolutePath(), parseFileNameSeparator);

			// get reader for image file
			final SCIFIO scifio = new SCIFIO(IJ2PluginUtility.ij().getContext());
			Reader reader = null;
			try
			{
				reader = scifio.initializer().initializeReader(f.getAbsolutePath(), new SCIFIOConfig().checkerSetOpen(true));
			}
			catch (Exception e)
			{
				JEXDialog.messageDialog("Couldn't initialize reader for file " + f.getAbsolutePath(), ImportPrefixLetterNumberImages_SCIFIO.class);
				e.printStackTrace();
				return null;
			}
			// 	get table from reader		
			table = getDimTableFromReader(reader, autoNameGathering);
			if(table == null)
			{
				JEXDialog.messageDialog("Function canceled manually OR due to issues with determining dimensions of the image.", ImportPrefixLetterNumberImages_SCIFIO.class);
				return null;
			}

			if(reader.getImageCount() > 1)
			{
				Dim loc = new Dim("Loc",reader.getImageCount());
				table.add(0, loc);
			}
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
						JEXDialog.messageDialog("Couldn't read image " + i + " plane " + j + " in " + f.getAbsolutePath() + ". Skipping to next plane.", ImportPrefixLetterNumberImages_SCIFIO.class);
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
						JEXDialog.messageDialog("Couldn't handle writing of image with this particular bits-per-pixel: " + d.getBitsPerPixel(), ImportPrefixLetterNumberImages_SCIFIO.class);
						return null;
					}


					if(canceler.isCanceled())
					{
						return null;
					}

					// For each image split it if necessary
					if(imRows * imCols > 1)
					{
						TreeMap<DimensionMap,ImageProcessor> splitImages = splitRowsAndCols(ip, imRows, imCols, rowName, colName, canceler);
						// The above might return null because of being canceled. Catch cancel condition and move on.
						if(canceler.isCanceled())
						{
							return null;
						}
						DimensionMap map = itr.next().copy();
						int imageCounter = 0;
						for(Entry<DimensionMap,ImageProcessor> e : splitImages.entrySet())
						{
							String filename = JEXWriter.saveImage(e.getValue());
							map.putAll(baseMap.copy());
							map.putAll(e.getKey());
							if(pendingImageFiles.size() > 1)
							{
								if(reader.getImageCount() > 1)
								{
									if(parseFileNameSeparator.equals(""))
									{
										map.put("Loc", ""+fi);
										map.put("Loc 2", ""+imageCounter);
										imageCounter = imageCounter + 1;
									}
									else
									{
										map.put("Loc", ""+imageCounter);
										imageCounter = imageCounter + 1;
									}
								}
								else
								{
									if(parseFileNameSeparator.equals(""))
									{
										map.put("Loc", ""+fi);
									}
									else
									{
										// Do nothing, the multi-file "Loc" dimension(s) is/are created through parsing of the file names
									}
								}
							}
							multiMap.put(map.copy(),filename);
							Logs.log(map.toString() + " :: " + filename, ImportPrefixLetterNumberImages_SCIFIO.class);
						}
						splitImages.clear();
					}
					else
					{
						String filename = JEXWriter.saveImage(ip);
						DimensionMap map = itr.next().copy();
						map.putAll(baseMap.copy());
						int imageCounter = 0;
						if(pendingImageFiles.size() > 1)
						{
							if(reader.getImageCount() > 1)
							{
								if(parseFileNameSeparator.equals(""))
								{
									map.put("Loc", ""+fi);
									map.put("Loc 2", ""+imageCounter);
									imageCounter = imageCounter + 1;
								}
								else
								{
									map.put("Loc", ""+imageCounter);
									imageCounter = imageCounter + 1;
								}
							}
							else
							{
								if(parseFileNameSeparator.equals(""))
								{
									map.put("Loc", ""+fi);
								}
								else
								{
									// Do nothing, the multi-file "Loc" dimension(s) is/are created through parsing of the file names
								}
							}
						}
						multiMap.put(map,filename);
						Logs.log(map.toString() + " = " + filename, ImportPrefixLetterNumberImages_SCIFIO.class);
						ip = null;
					}					

					JEXStatics.statusBar.setProgressPercentage((int) (100.0 * count / total));
					count = count + 1;
				}
			}
		}
		if(fileNotFound)
		{
			JEXDialog.messageDialog("Warning! At least one of the files specified for this function was not found. Will attempt to continue", ImportPrefixLetterNumberImages_SCIFIO.class);
		}

		// OUTPUT PROCESSING
		JEXData output = ImageWriter.makeImageStackFromPaths("temp", multiMap);
		if (table != null) {
			DimTable toSet = new DimTable(multiMap);
			for(Dim d : table)
			{
				toSet.removeDimWithName(d.dimName);
			}
			for(Dim d : table)
			{
				toSet.add(d.copy());
			}
			output.setDimTable(toSet);
		}
		
		return output;
	}
}
