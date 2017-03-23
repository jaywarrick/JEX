package function.plugin.plugins.Import;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.io.IOPlugin;
import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataWriter.FileWriter;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.ValueWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.plugins.featureExtraction.FeatureUtils;
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
import io.scif.img.converters.PlaneConverter;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import loci.common.DataTools;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.Canceler;
import miscellaneous.FileUtility;
import miscellaneous.LSVList;
import miscellaneous.Pair;
import miscellaneous.SimpleFileFilter;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.CalibratedAxis;
import net.imglib2.FinalDimensions;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;

@Plugin(
		type = JEXPlugin.class,
		name="Import Images (SCIFIO)",
		menuPath="Import",
		visible=true,
		description="Import images of (nearly) any format and (nearly) any dimensionality"
				+ "(e.g. one (or many) ND2 files or tif stacks)"
		)
public class ImportImages_SCIFIO extends JEXPlugin {

	public ImportImages_SCIFIO() {}

	/////////// Define Inputs ///////////

	/*
	 * None necessary; Input Directory is classified as a parameter.
	 */

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Input Directory/File", description="Location of the multicolor TIFF images", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String inDir;

	@ParameterMarker(uiOrder=1, name="File Extension", description="The type of file that is being imported. Default is tif. Not necessary if importing a single file.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="tif")
	String fileExtension;

	@ParameterMarker(uiOrder=2, name="Name-Value Pair Separator", description="Charactor that separates dimension name-value pairs in the image name (e.g., '_' in X.002_Y.003.tif). Use blank (i.e., no character) to avoid parsing anything whatsoever.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String dimSeparator;

	@ParameterMarker(uiOrder=3, name="Name-Value Separator", description="Charactor that separates the name and value of the name-value pair in the image name (e.g., '.' in X.002_Y.003.tif). Use blank (i.e., no character) to split on first numeric character.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String valueSeparator;

	@ParameterMarker(uiOrder=4, name="Montage Rows", description="If this image is a montage and is to be split, how many rows are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imRows;

	@ParameterMarker(uiOrder=5, name="Montage Cols", description="If this image is a montage and is to be split, how many cols are in the image.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	int imCols;

	@ParameterMarker(uiOrder=6, name="Binning", description="Amount to bin the pixels to reduce image size. Value of 1 skips binning. Partial values converted to scale operation (e.g., bin=3.5 is converted to scale=1/3.5)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1")
	double binning;

	@ParameterMarker(uiOrder=7, name="Binning Method", description="Method for binning the image.", ui=MarkerConstants.UI_DROPDOWN, choices={"NONE", "NEAREST NEIGHBOR", "BILINEAR", "BICUBIC"}, defaultChoice = 2)
	String binMethod;

	@ParameterMarker(uiOrder=8, name="Gather channel names (and other metadata)?", description="Transfer the name of each channel (e.g. DAPI, FITC, etc) if available in the metadata of the image. Otherwise, channels are named by index in the order they were provided by the image. Text from the entire metadata is saved as a file.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean transferNames;

	@ParameterMarker(uiOrder=9, name="Dimension to separate (optional)", description="Optionally name a dimension of the image set to separate into different image objects.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String dimensionToSplit;


	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Imported Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The imported image object", enabled=true)
	JEXData output;

	@OutputMarker(uiOrder=2, name="List of Imported Files", type=MarkerConstants.TYPE_VALUE, flavor="", description="The list of imported files.", enabled=true)
	JEXData inputFileList;

	@OutputMarker(uiOrder=3, name="Metadata", type=MarkerConstants.TYPE_FILE, flavor="", description="The imported image object metadata", enabled=true)
	JEXData meta;

	@OutputMarker(uiOrder=4, name="Split Imported Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The imported image objects", enabled=true)
	Vector<JEXData> output2 = new Vector<JEXData>();

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

		// DO something
		Pair<Vector<JEXData>, JEXData> imagesAndMetaData = importFiles(this.output.getDataObjectName(), pendingImageFiles, this.dimSeparator, this.valueSeparator, this.fileExtension, this.imRows, this.imCols, binning, binMethod, "ImRow", "ImCol", this.transferNames, this, this.dimensionToSplit);
		if(imagesAndMetaData.p1.get(0) == null || (imagesAndMetaData.p1.size() < 2 && imagesAndMetaData.p1.get(0).getDataObjectName().equals(this.output.getDataObjectName())))
		{
			// We have a single output to return.
			this.output = imagesAndMetaData.p1.get(0);
		}
		else
		{
			// We have multiple outputs to return due to splitting the 
			this.output2 = imagesAndMetaData.p1;
		}

		this.inputFileList = ValueWriter.makeValueObject("temp", this.getFileList(pendingImageFiles).toString());
		this.meta = imagesAndMetaData.p2;

		return true;
	}

	/**
	 * Create DimensionMap of a given image 
	 * The image name should be in certain format, ex. Image_x001_y002_z004.tif
	 * If the value separator is null or "", then it is assumed that the value begins with a number.
	 * If no number exists that case, that chuck of string is ignored. For example "Well_Channel1"
	 * would produce Channel=1 while "Well_ChannelBF" would produce an empty dimension map.
	 * 
	 * @param filePath image Path and Name
	 * @param dimSeparator separator between the name-value pairs
	 * @param valueSeparator separator between the dimension name and the dimension value
	 * @return
	 */
	public DimensionMap getMapFromPath(String filePath, String dimSeparator, String valueSeparator) {
		String name = FileUtility.getFileNameWithoutExtension(filePath);
		String[] names = name.split(dimSeparator);

		DimensionMap dimMap = new DimensionMap();
		String dimValue, dimName, temp;
		int splitIndex = 0;

		if(valueSeparator == null || valueSeparator.equals(""))
		{
			// Do the old method.
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
		}
		else
		{
			// Do the new method with two separator characters
			for (int i = 0; i < names.length; i++){
				temp = names[i];
				String[] nameValue = temp.split(valueSeparator);
				// if the string is not a name value pair then skip it.
				if(nameValue.length == 2)
				{
					dimMap.put(nameValue[0], nameValue[1]);
				}
			}
		}


		return dimMap;

	}

	public Pair<DimTable,Pair<DimensionMap,String>> getDimTableFromReader(Reader r, boolean transferNames)
	{
		Pair<DimensionMap,String> metaDataFile = null;
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
			metaDataFile = new Pair<DimensionMap,String>();
			TreeMap<String,String> colors = new TreeMap<String,String>();
			LSVList info = new LSVList();
			for(Entry<String,Object> e : r.getMetadata().getTable().entrySet())
			{
				info.add(e.getKey() + " = " + e.getValue().toString().trim());

				if(e.getKey().contains("Name #"))
				{
					colors.put(e.getKey().toString().trim(), e.getValue().toString().trim());
				}
			}
			String path = JEXWriter.saveText(info.toString(), "txt");
			metaDataFile.p1 = new DimensionMap("File=" + FileUtility.getFileNameWithoutExtension(r.getCurrentFile()));
			metaDataFile.p2 = path;
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
					while(newColorNamesList.contains(newName))
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
		return new Pair<DimTable, Pair<DimensionMap,String>>(ret, metaDataFile);
	}

	public LSVList getFileList(List<File> filesToImport)
	{
		LSVList fileList = new LSVList();
		for(File f : filesToImport)
		{
			fileList.add(f.getAbsolutePath());
		}
		return fileList;
	}

	/**
	 * 
	 * @param imp ImageProcessor to split
	 * @param binning double amount to bin the image (scale = 1/bin)
	 * @param binMethod "NONE" or "NEAREST NEIGHBOR" or "BILINEAR" or "BICUBIC"
	 * @param rows int number of rows to split into
	 * @param cols int number of cols to split into
	 * @param rowName String name of the row dimension to create
	 * @param colName String name of the col dimension to create
	 * @param canceler Canceler object for interrupting if necessary (checked in each loop)
	 * @return
	 */
	public TreeMap<DimensionMap,ImageProcessor> splitRowsAndCols(ImageProcessor imp, double binning, String binMethod, int rows, int cols, String rowName, String colName, Canceler canceler)
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
				ImageProcessor toSave = imp.crop();
				if(binning != 1)
				{
					double scale = 1/binning;
					double width = toSave.getWidth();
					int newWidth = (int) (scale * width);
					if(binMethod.equals("NONE"))
					{
						toSave.setInterpolationMethod(ImageProcessor.NONE);
					}
					else if(binMethod.equals("BILINEAR"))
					{
						toSave.setInterpolationMethod(ImageProcessor.BILINEAR);
					}
					else if(binMethod.equals("NEAREST NEIGHBOR"))
					{
						toSave.setInterpolationMethod(ImageProcessor.NEAREST_NEIGHBOR);
					}
					else // do BICUBIC
					{
						toSave.setInterpolationMethod(ImageProcessor.BICUBIC);
					}
					toSave = toSave.resize(newWidth);
				}

				ret.put(new DimensionMap(rowName + "=" + r + "," + colName + "=" + c), toSave);
			}
		}
		return ret;
	}

	public <T extends RealType<T>> Pair<Vector<JEXData>,JEXData> importFiles(String objectName, List<File> pendingImageFiles, String dimSeparator, String valueSeparator, String fileExtension, int imRows, int imCols, double binning, String binMethod, String rowName, String colName, boolean autoNameGathering, Canceler canceler, String dimensionToSplit)
	{
		if(valueSeparator.equals("."))
		{
			valueSeparator = "\\.";
		}
		if(dimSeparator.equals("."))
		{
			dimSeparator = "\\.";
		}		

		DimTable table = null;

		TreeMap<DimensionMap,String> multiMap = new TreeMap<>();
		TreeMap<DimensionMap,String> metaDataMap = new TreeMap<>();
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
			DimensionMap baseMap = getMapFromPath(f.getAbsolutePath(), dimSeparator, valueSeparator);

			// get reader for image file
			final SCIFIO scifio = new SCIFIO(IJ2PluginUtility.ij().getContext());
			Reader reader = null;
			try
			{
				reader = scifio.initializer().initializeReader(f.getAbsolutePath(), new SCIFIOConfig().checkerSetOpen(true));
			}
			catch (Exception e)
			{
				JEXDialog.messageDialog("Couldn't initialize reader for file " + f.getAbsolutePath(), ImportImages_SCIFIO.class);
				e.printStackTrace();
				return null;
			}
			// 	get table from reader		
			Pair<DimTable, Pair<DimensionMap,String>> tableAndMetaData = getDimTableFromReader(reader, autoNameGathering);
			table = tableAndMetaData.p1;
			if(tableAndMetaData.p2 != null)
			{
				metaDataMap.put(tableAndMetaData.p2.p1, tableAndMetaData.p2.p2);
			}
			if(tableAndMetaData.p1 == null)
			{
				JEXDialog.messageDialog("Function canceled manually OR due to issues with determining dimensions of the image OR with getting file metadata.", ImportImages_SCIFIO.class);
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
						JEXDialog.messageDialog("Couldn't read image " + i + " plane " + j + " in " + f.getAbsolutePath() + ". Skipping to next plane.", ImportImages_SCIFIO.class);
						e.printStackTrace();
						continue;
					}
					ImageMetadata d = plane.getImageMetadata();
					long[] dims = d.getAxesLengthsPlanar();
					Vector<ImageProcessor> ips = new Vector<>();
					if(d.getBitsPerPixel() <= 8)
					{
						long[] axesLengths = d.getAxesLengthsPlanar();
						if(axesLengths.length > 2)
						{
							
							// Treat as multichannel image
							// Get image width and height
							int channelAxis = 0;
							long[] whd = new long[]{0,0,0}; 
							for(CalibratedAxis a : d.getAxesPlanar())
							{
								if(a.type().getLabel().equals("X"))
								{
									whd[0] = d.getAxesLengthsPlanar()[channelAxis];
								}
								else if(a.type().getLabel().equals("Y"))
								{
									whd[1] = d.getAxesLengthsPlanar()[channelAxis];
								}
								else
								{
									whd[2] = d.getAxesLengthsPlanar()[channelAxis];
								}
								channelAxis = channelAxis + 1;
							}
							
							//							Logs.log("here", this);
							//							PlaneConverter pc = IJ2PluginUtility.ij().scifio().planeConverter().getDefaultConverter();
							//							ImgFactory<UnsignedByteType> factory = new ArrayImgFactory<>();
							//							Img<UnsignedByteType> out = factory.create(new FinalDimensions(new long[]{whd[0], whd[1] * whd[2]}), new UnsignedByteType(0));
							//							ImgPlus<UnsignedByteType> out2 = IJ2PluginUtility.ij().scifio().imgUtil().makeSCIFIOImgPlus(out);
							//							pc.populatePlane(reader, i, j, plane.getBytes(), out2, new SCIFIOConfig().checkerSetOpen(true));
							//							
							//							FeatureUtils utils = new FeatureUtils();
							//							utils.show(out2);
							//							String thePath = JEXWriter.saveImage(out2);
							//							Logs.log(thePath, this);

							int skip = 0;
							int length = (int) (whd[0]*whd[1]);
							for(int k = 0; k < axesLengths[2]; k++)
							{
								byte[] toSave = new byte[length];
								System.arraycopy(plane.getBytes(), skip, toSave, 0, length);
								byte[] converted = (byte[]) DataTools.makeDataArray(toSave, 1, false, d.isLittleEndian());
								ips.add(new ByteProcessor((int)dims[0], (int)dims[1], converted, null));
								skip = skip + length;
							}
						}
						else
						{
							JEXDialog.messageDialog("Can't handle images with this level of dimensionality at yet. Sorry. Aborting.", this);
						}

					}
					else if(d.getBitsPerPixel() >= 9 && d.getBitsPerPixel() <= 16)
					{
						short[] converted = (short[]) DataTools.makeDataArray(plane.getBytes(), 2, false, d.isLittleEndian());
						ips.add(new ShortProcessor((int)dims[0], (int)dims[1], converted, null));
					}
					else if(d.getBitsPerPixel() >= 17 && d.getBitsPerPixel() <= 32)
					{
						float[] converted = (float[]) DataTools.makeDataArray(plane.getBytes(), 4, true, d.isLittleEndian());
						ips.add(new FloatProcessor((int)dims[0], (int)dims[1], converted, null));
					}
					else
					{
						JEXDialog.messageDialog("Couldn't handle writing of image with this particular bits-per-pixel: " + d.getBitsPerPixel(), ImportImages_SCIFIO.class);
						return null;
					}

					if(canceler.isCanceled())
					{
						return null;
					}

					DimensionMap map = itr.next().copy();
					int imageCounter = 1;

					// Channel Counter
					int channelIndex = 0;
					for(ImageProcessor ip : ips)
					{
						TreeMap<DimensionMap,ImageProcessor> splitImages = splitRowsAndCols(ip, binning, binMethod, imRows, imCols, rowName, colName, canceler);

						// The above might return null because of being canceled. Catch cancel condition and move on.
						if(canceler.isCanceled())
						{
							return null;
						}

						// For each image split it if necessary
						if(imRows * imCols > 1)
						{
							for(Entry<DimensionMap,ImageProcessor> e : splitImages.entrySet())
							{
								String filename = JEXWriter.saveImage(e.getValue());
								map.putAll(baseMap.copy());
								map.putAll(e.getKey());
								if(pendingImageFiles.size() > 1)
								{
									// Then save a dimension for Loc
									if(dimSeparator.equals(""))
									{
										// Then call FileIndex dimension "FileIndex"
										map.put("SplitImageIndex", "" + imageCounter);
										map.put("FileIndex", "" + (fi + 1)); // fi starts at 0
										imageCounter = imageCounter + 1;
									}
									else
									{
										// Else the FileIndex dimension is already taken care of / named by parsing file names.
										map.put("SplitImageIndex", "" + imageCounter);
										imageCounter = imageCounter + 1;
									}
								}
								if(ips.size() > 1)
								{
									map.put("ChannelIndex", "" + channelIndex);
								}
								multiMap.put(map.copy(),filename);
								Logs.log(map.toString() + " :: " + filename, ImportImages_SCIFIO.class);
							}
							splitImages.clear();
						}
						else
						{
							// Otherwise we don't split each image or save a SplitImageIndex dimension
							String filename = JEXWriter.saveImage(ip);
							map.putAll(baseMap.copy());
							if(pendingImageFiles.size() > 1)
							{
								// Then save a dimension for FileIndex
								if(dimSeparator.equals(""))
								{
									// Then call FileIndex dimension "FileIndex"
									map.put("FileIndex", "" + (fi + 1)); // fi starts at 0
									imageCounter = imageCounter + 1;
								}
								else
								{
									// Else the FileIndex dimension is already taken care of / named by parsing file names.
									imageCounter = imageCounter + 1;
								}
							}
							if(ips.size() > 1)
							{
								map.put("ChannelIndex", "" + channelIndex);
							}
							multiMap.put(map.copy(),filename);
							Logs.log(map.toString() + " = " + filename, ImportImages_SCIFIO.class);
						}	
						channelIndex = channelIndex + 1;
					}
					ips.clear();

					JEXStatics.statusBar.setProgressPercentage((int) (100.0 * count / total));
					count = count + 1;
				}
			}
		}
		if(fileNotFound)
		{
			JEXDialog.messageDialog("Warning! At least one of the files specified for this function was not found. Will attempt to continue", ImportImages_SCIFIO.class);
		}

		// OUTPUT PROCESSING

		if (table != null)
		{
			DimTable toSet = new DimTable(multiMap);
			for(Dim d : table)
			{
				toSet.removeDimWithName(d.dimName);
			}
			for(Dim d : table)
			{
				toSet.add(d.copy());
			}
			table = toSet.copy();
		}


		Vector<JEXData> ret = new Vector<JEXData>();
		if(!dimensionToSplit.equals(""))
		{
			Dim toSplit = table.getDimWithName(dimensionToSplit);
			if(toSplit != null)
			{
				Vector<DimTable> separateDimTables = new Vector<>();
				for(String val : toSplit.dimValues)
				{
					separateDimTables.add(table.getSubTable(new DimensionMap(dimensionToSplit + "=" + val)));
				}
				Vector<TreeMap<DimensionMap,String>> subImageSets = new Vector<>();
				for(DimTable dt : separateDimTables)
				{
					TreeMap<DimensionMap,String> toSave = new TreeMap<>();
					for(DimensionMap map : dt.getMapIterator())
					{
						toSave.put(map, multiMap.get(map));
					}
					subImageSets.add(toSave);
				}
				// Create a JEXData for each subImageSet appending the Dim Value to the object name.
				for(int i = 0; i < subImageSets.size(); i++)
				{
					JEXData temp = ImageWriter.makeImageStackFromPaths(objectName + " " + dimensionToSplit + " " + toSplit.valueAt(i), subImageSets.get(i));
					temp.setDimTable(separateDimTables.get(i));
					ret.add(temp);
				}
			}
			else
			{
				// We didn't find the dimension to split in the image set and don't know what to do so skip splitting.
				CSVList csvl = new CSVList();
				for(Dim d : table)
				{
					csvl.add(d.dimName);
				}
				JEXDialog.messageDialog("I Couldn't split the image objects on the specified dimension. Specified = " + dimensionToSplit + "; Dims Present = " + csvl.toString(), this);
				ret.add(ImageWriter.makeImageStackFromPaths(objectName, multiMap));
			}
		}
		else
		{
			// No need to split the images into separate objects. Just return the image object.
			ret.add(ImageWriter.makeImageStackFromPaths(objectName, multiMap));
		}

		JEXData metaDataOutput = FileWriter.makeFileObject("temp", null, metaDataMap);

		return new Pair<Vector<JEXData>, JEXData>(ret, metaDataOutput);
	}
}
