package function.plugin.plugins.multipleMyeloma;

import java.awt.Point;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ValueReader;
import Database.DataWriter.ImageWriter;
import Database.DataWriter.RoiWriter;
import Database.DataWriter.ValueWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.imageUtility.AutoThresholder;
import function.imageUtility.MaximumFinder;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.plugins.featureExtraction.FeatureUtils;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.RankFilters;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.utilities.FunctionUtility;
import jex.utilities.ROIUtility;
import miscellaneous.FileUtility;
import miscellaneous.Pair;
import miscellaneous.StatisticsUtility;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.logic.BoolType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
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
		name="Import Flow Images",
		menuPath="Multiple Myeloma",
		visible=true,
		description="Import flow images in format <prefix><Cell#>_Ch<ChannelNumber>.ome.tif"
		)
public class ImportFlowImages extends JEXPlugin {

	public ImportFlowImages()
	{}

	FeatureUtils utils = new FeatureUtils();

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Fold Path (optional)", type=MarkerConstants.TYPE_VALUE, description="A Value object containing the folder path to the images of interest. Files will be filtered to include only ome.tifs.", optional=false)
	JEXData folderData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Folder of Images", description="If no folder specified as an input object, select the folder that contains the .ome.tif images you would like to import.", ui=MarkerConstants.UI_FILECHOOSER, defaultText="")
	String folderString;

	@ParameterMarker(uiOrder=2, name="Number of Cells to Import", description="How many cells should be imported? (use number <= 0 to indicate all images)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="-1")
	int numCells;

	@ParameterMarker(uiOrder=3, name="File Prefix", description="Prefix of the *.ome.tif files to grab.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Image_")
	String prefix;

	@ParameterMarker(uiOrder=4, name="Thumb Size", description="About how wide/tall (square) of an area (pixels) is needed to encompass an image of a cell (estimate large to ensure large enough for all).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	int thumbSize;

	@ParameterMarker(uiOrder=5, name="# of Rows", description="How many rows of images should be put into each page of thumbs?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="20")
	int nRows;

	@ParameterMarker(uiOrder=6, name="# of Cols", description="How many cols of images should be put into each page of thumbs?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="20")
	int nCols;

	@ParameterMarker(uiOrder=7, name="BG Ellipse Definition (pixels)", description="How many pixels in from the corner of the images should a circle be drawn to define 'background' pixels for background subtraction and thresholding?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10")
	int nBG;
	
	@ParameterMarker(uiOrder=8, name="BF # of Sigmas", description="How many standard deviations (sigmas) above background noise should be used to define masked regions of the brightfield image?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="4")
	double BFSigma;

	@ParameterMarker(uiOrder=9, name="Fill Holes Smaller Than...", description="What size holes (in total number of pixels) in the BF mask should be filled?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	int fillSize;

	@ParameterMarker(uiOrder=9, name="BF Mask Erosion Radius", description="How much should the BF mask be eroded?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.7")
	double bfRadius;
	
	@ParameterMarker(uiOrder=10, name="Fluor. # of Sigmas", description="How many standard deviations (sigmas) above BG median should be used to define masked regions of the fluorescent images?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="3")
	double FluorSigma;

	@ParameterMarker(uiOrder=11, name="Fluor. Auto-threshold?", description="Should the fluorescent images also be auto-thresholded to create additional masks?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean autoThresh;

	@ParameterMarker(uiOrder=12, name="Auto-thresh. Method", description="Which method of auto-thresholding should be used?", ui=MarkerConstants.UI_DROPDOWN, choices={ "HUANG", "INTERMODES", "ISODATA", "LI", "MAXENTROPY", "MEAN", "MINERROR", "MINIMUM", "MOMENTS", "OTSU", "PERCENTILE", "RENYIENTROPY", "SHANBHAG", "TRIANGLE", "YEN" }, defaultChoice=9)
	String autoThreshMethod;

	@ParameterMarker(uiOrder=13, name="Auto-thresh. Multiplier", description="A value by which to scale the threshold provided by the autothreshold routine.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.55")
	double autoThreshMultiplier;

	@ParameterMarker(uiOrder=14, name="Merge Sigma/Auto-Thresh Masks?", description="If both the sigma method and the auto-method were used to create masks, should the masks be merged using an AND operation?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean merge;

	@ParameterMarker(uiOrder=15, name="Cell Id Channel", description="Channel to use for producing a point inside the cell marked with the cell ID.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="5")
	String nucChannel;

	@ParameterMarker(uiOrder=16, name="Offset Intensity (integer)", description="How much itnensity should be added back after background subtraction to preserve background noise characteristics?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="500")
	int offset;


	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Images", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The background corrected image flow images.", enabled=true)
	JEXData imageData;

	@OutputMarker(uiOrder=2, name="Masks", type=MarkerConstants.TYPE_IMAGE, flavor="", description="Masks of the image flow images.", enabled=true)
	JEXData maskData;

	@OutputMarker(uiOrder=3, name="Cell Number Roi", type=MarkerConstants.TYPE_ROI, flavor="", description="A Roi with the id numbers of the cells from the image flow software.", enabled=true)
	JEXData roiData;

	@OutputMarker(uiOrder=4, name="Imported Folder", type=MarkerConstants.TYPE_VALUE, flavor="", description="Folder where images were imported from.", enabled=true)
	JEXData folderValueData;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		FeatureUtils utils = new FeatureUtils();
		File folder = new File(folderString);
		if(folderData != null)
		{
			String inputFolderString = ValueReader.readValueObject(folderData);
			folder = new File(inputFolderString);
		}

		int methodInt = getMethodInt();

		if(!folder.exists())
		{
			JEXDialog.messageDialog("The specified folder was not found: " + folderString, this);
			return false;
		}

		Collection<File> files = Arrays.asList(folder.listFiles());

		// Presort the files
		TreeMap<DimensionMap,String> sortedFiles = getSortedFiles(files);

		DimTable fileDimTable = new DimTable(sortedFiles);
		Dim channelDim = fileDimTable.getDimWithName("Channel");
		boolean haveNucChannel = !nucChannel.equals("") && channelDim.containsValue(nucChannel);
		TreeMap<DimensionMap,Pair<Integer,FloatProcessor>> imageMap = new TreeMap<>();
		TreeMap<DimensionMap,Pair<Integer,ByteProcessor>> maskMap = new TreeMap<>();
		int cellCounter = 0;
		int cellsPerPage = nRows * nCols;
		TreeMap<DimensionMap,String> imagePages = new TreeMap<>();
		TreeMap<DimensionMap,String> maskPages = new TreeMap<>();
		TreeMap<DimensionMap,ROIPlus> cellNumberRoiMap = new TreeMap<>();
		int pageCounter = 1;
		TreeMap<String, Vector<Double>> channelBgMedians = new TreeMap<>();
		for(String val : channelDim.dimValues)
		{
			channelBgMedians.put(val, new Vector<Double>());
		}
		TreeMap<String, Vector<Double>> channelBgMads = new TreeMap<>();
		for(String val : channelDim.dimValues)
		{
			channelBgMads.put(val, new Vector<Double>());
		}
		Vector<Double> edgeMedians = new Vector<>();
		Vector<Double> edgeMads = new Vector<>();
		Iterator<DimTable> itr = fileDimTable.getSubTableIterator("Cell").iterator();
		int count = 0, percentage = 0;
		int total = fileDimTable.getDimWithName("Cell").size();
		if(numCells <= 0)
		{
			numCells = total;
		}
		else
		{
			total = numCells;
		}
		// For each cell
		while(itr.hasNext() && count <= numCells-1)
		{
			DimTable cellTable = itr.next();
			if(this.isCanceled())
			{
				return false;
			}
			
			// For all channels
			for(DimensionMap map : cellTable.getMapIterator())
			{
				// Check if canceled
				if(this.isCanceled())
				{
					return false;
				}

				// Get the image for manipulation
				ImagePlus im = new ImagePlus(sortedFiles.get(map));
				FloatProcessor imp = im.getProcessor().convertToFloatProcessor();

				// Get center
				IdPoint p = new IdPoint(im.getWidth()/2, im.getHeight()/2, 0);

				// Get BG circle
				ROIPlus circle = getCircle(p);

				// Get the background median
				Pair<Double,Double> med_mad = getMedAndMad(circle, imp);
				
				channelBgMedians.get(map.get(channelDim.dimName)).add(med_mad.p1);
				channelBgMads.get(map.get(channelDim.dimName)).add(med_mad.p2);

				// Get background subtracted image
				if(map.get("Channel").equals("1"))
				{					
					// Don't correct the BF channel
					FloatProcessor bf = (new ImagePlus(sortedFiles.get(map))).getProcessor().convertToFloatProcessor();
					Pair<Integer,FloatProcessor> pairBF = new Pair<>(cellCounter, bf);
					imageMap.put(map.copy(), pairBF);

					// Do the edge detection
					FloatProcessor edge = copyProcessor(imp);
					edge.findEdges();
					Pair<Double,Double> edge_med_mad = getMedAndMad(circle, edge);
					edgeMedians.add(edge_med_mad.p1);
					edgeMads.add(edge_med_mad.p2);
					//					double thresh = edge_med_mad.p1 + BFSigma * edge_med_mad.p2;
					//					//					// FileUtility.showImg(maskUpper, true);
					//					FunctionUtility.imThresh(maskUpper, thresh, false);
					//					//					// FileUtility.showImg(maskUpper, true);
					//					ByteProcessor bpUpper = getByteProcessor(maskUpper, false);
					//					//					FileUtility.showImg(bpUpper, true);
					//					utils.filterMaskRegions(bpUpper, 0, fillSize, true, false, false);
					//					//					FileUtility.showImg(bpUpper, true);
					//					DimensionMap upper = map.copyAndSet("Channel=1_Upper");
					//					Pair<Integer,ByteProcessor> pairUpper = new Pair<>(cellCounter, bpUpper);
					//					maskMap.put(upper, pairUpper);
				}
				else
				{
					// Get the background corrected image
					FloatProcessor bg = copyProcessor(imp);
					bg.add(offset);
					bg.subtract(med_mad.p1);
					Pair<Integer,FloatProcessor> pairBG = new Pair<>(cellCounter, bg);
					imageMap.put(map.copy(), pairBG);

					if(autoThresh)
					{
						// Get the auto mask
						FloatProcessor maskAuto = copyProcessor(imp);
						double thresh = getThreshold(maskAuto, methodInt);
						FunctionUtility.imThresh(maskAuto, thresh, false);
						ByteProcessor bpAuto = getByteProcessor(maskAuto, false); // Creating black and white image
						DimensionMap mapAuto = map.copy();
						Pair<Integer,ByteProcessor> pairAuto = new Pair<>(cellCounter, bpAuto);
						maskMap.put(mapAuto, pairAuto);
						//
						//					// Merge masks?
						//					if(merge)
						//					{
						//						bpSigma.copyBits(bpAuto, 0, 0, Blitter.AND);
						//						maskMap.put(map.copy(), new Pair<Integer,ByteProcessor>(cellCounter, bpSigma));
						//					}
						//					else
						//					{
						//						DimensionMap mapSigma = map.copyAndSet("Channel=" + map.get("Channel") + "_Sigma");
						//						DimensionMap mapAuto = map.copyAndSet("Channel=" + map.get("Channel") + "_Auto");
						//						Pair<Integer,ByteProcessor> pairSigma = new Pair<>(cellCounter, bpSigma);
						//						Pair<Integer,ByteProcessor> pairAuto = new Pair<>(cellCounter, bpAuto);
						//						maskMap.put(mapSigma, pairSigma);
						//						maskMap.put(mapAuto, pairAuto);
						//					}
					}
					
				}
			}

			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) total)));
			JEXStatics.statusBar.setProgressPercentage(percentage);

			if(cellCounter < cellsPerPage - 1 && itr.hasNext() && count <= numCells-1)
			{
				cellCounter = cellCounter + 1;
			}
			else
			{
				DimTable imageTable = new DimTable(imageMap);
				FloatProcessor imageMapResults;
				ByteProcessor maskMapResults;
				FloatProcessor sigmaFloatMask;
				ByteProcessor sigmaByteMask;
				ByteProcessor autoMask = null;
				for(DimTable pageTable : imageTable.getSubTableIterator("Channel"))
				{
					// Check if canceled
					if(this.isCanceled())
					{
						return false;
					}
					
					String channel = pageTable.getDimWithName("Channel").valueAt(0);
					double med = StatisticsUtility.median(channelBgMedians.get(channel));
					double mad = StatisticsUtility.median(channelBgMads.get(channel));
					double edgeMed = StatisticsUtility.median(edgeMedians);
					double edgeMad = StatisticsUtility.median(edgeMads);
					DimTable toSave_dt = pageTable.copy();
					toSave_dt.removeDimWithName("CellCount");
					toSave_dt.removeDimWithName("Cell");
					DimensionMap pageMap = toSave_dt.getDimensionMaps().get(0);
					pageMap.put("Page", ""+pageCounter);
					if(pageTable.getDimWithName("Channel").valueAt(0).equals("1"))
					{
						// Get the float page with a background equal to the background of the cells
						imageMapResults = getPage(pageTable, imageMap, (int) Math.round(med));
						
						// Create the sigmaMask
						sigmaFloatMask = copyProcessor(imageMapResults); // copy over the data from the float page
						sigmaFloatMask.findEdges(); // Perform edge detection
						FunctionUtility.imThresh(sigmaFloatMask, edgeMed + this.BFSigma * edgeMad, false); // Threshold the float page to the desired sigma threshold
						sigmaByteMask = getByteProcessor(sigmaFloatMask, false); // Creating black and white image
						
						utils.filterMaskRegions(sigmaByteMask, 0, fillSize, true, false, false); // Fill holes in the BF mask
						
						// Erode back from the sobel filter.
						RankFilters rF = new RankFilters();
						rF.rank(sigmaByteMask, this.bfRadius, RankFilters.MIN);
						
						// Adjust the float page background to the desired offset level
						imageMapResults.add(this.offset - med);
					}
					else
					{
						// Get the page setting the page background to the desired fluorescence offset
						imageMapResults = getPage(pageTable, imageMap, this.offset);
						
						// Create the sigmaMask
						sigmaFloatMask = copyProcessor(imageMapResults); // copy over the data from the float page
						FunctionUtility.imThresh(sigmaFloatMask, this.offset + this.FluorSigma * mad, false); // Threshold the float page to the desired sigma threshold
						sigmaByteMask = getByteProcessor(sigmaFloatMask, false); // Creating black and white image
						
						// If necessary, get the auto mask and possibly merge with the sigma mask
						if(autoThresh)
						{
							maskMapResults = getPage(pageTable, maskMap, 0);
							autoMask = maskMapResults;
							if(merge)
							{
								sigmaByteMask.copyBits(autoMask, 0, 0, Blitter.AND);
								autoMask = null;
							}
						}
					}
					
					// Save roi information if necessary
					if(haveNucChannel && pageMap.get("Channel").equals(nucChannel))
					{
						DimensionMap roiMap = pageMap.copy();
						roiMap.remove("Channel");
						ROIPlus roi = this.getPageBestPoints(sigmaByteMask, pageTable, imageMap);
						cellNumberRoiMap.put(roiMap, roi);
					}
					else if(!haveNucChannel)
					{
						// It's ok if it overwrites a few times, the results are the same for each non-nuChannel channel
						DimensionMap roiMap = pageMap.copy();
						roiMap.remove("Channel");
						ROIPlus roi = this.getPageBestPoints(null, pageTable, imageMap);
						cellNumberRoiMap.put(roiMap, roi);
					}
					
					// Save the grayscale image
					ImageProcessor results = JEXWriter.convertToBitDepthIfNecessary(imageMapResults, 16);
					String path = JEXWriter.saveImage(results);
					imagePages.put(pageMap, path);
					
					// Save the alt mask if necessary
					if(autoMask != null)
					{
						path = JEXWriter.saveImage(autoMask);
						maskPages.put(pageMap.copyAndSet("Channel=" + pageMap.get("Channel") + "_Auto"), path);
					}
					
					// Save the sigma mask
					path = JEXWriter.saveImage(sigmaByteMask);
					maskPages.put(pageMap, path);
				}

				// Clean up
				channelBgMedians.clear();
				channelBgMads.clear();
				edgeMedians.clear();
				edgeMads.clear();
				imageMap.clear();
				maskMap.clear();
				imageMapResults = null;
				maskMapResults = null;
				cellCounter = 0;
				pageCounter = pageCounter + 1;
			}
		}

		// Save the pages.
		if(imagePages.size() > 0)
		{
			this.imageData = ImageWriter.makeImageStackFromPaths("Duh", imagePages);
			this.maskData = ImageWriter.makeImageStackFromPaths("Duh", maskPages);
			this.roiData = RoiWriter.makeRoiObject("duh", cellNumberRoiMap);
			this.folderValueData = ValueWriter.makeValueObject("Duh", folder.getAbsolutePath());
		}

		// Return status
		return true;
	}

	/**t how wide/tall (square) of an area (pixels) is needed to encompass an image of a cell (estimate large to ensure large enough for all).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	@ParameterMarker(uiOrder=4, name="Thumb Size", description="About how wide/tall (square) of an area (pixels) is needed to encompass an image of a cell (estimate large to ensure large enough for all).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	int thumbSize;

	@ParameterMarker(uiOrder=4, name="# of Rows", description="How many rows of images should be put into each page of thumbs?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="20")
	@ParameterMarker(uiOrder=5, name="# of Rows", description="How many rows of images should be put into each page of thumbs?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="20")
	int nRows;

	@ParameterMarker(uiOrder=5, name="# of Cols", description="How many cols of images should be put into each page of thumbs?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="20")
	@ParameterMarker(uiOrder=6, name="# of Cols", description="How many cols of images should be put into each page of thumbs?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="20")
	int nCols;

	@ParameterMarker(uiOrder=6, name="BG Ellipse Definition (pixels)", description="How many pixels in from the corner of the images should a circle be drawn to define 'background' pixels for background subtraction and thresholding?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10")
	@ParameterMarker(uiOrder=7, name="BG Ellipse Definition (pixels)", description="How many pixels in from the corner of the images should a circle be drawn to define 'background' pixels for background subtraction and thresholding?", ui=MarkerConstants.UI_TEXTFIELD, defaultText="10")
	int nBG;
	 * The point is assumed to be the center of the image.
	 * @param p
	 * @return
	 */
	private ROIPlus getCircle(Point p)
	{
		int r = (int) Math.round(Math.sqrt(p.x*p.x + p.y*p.y));
		r = r - nBG;
		PointList pl = new PointList();
		pl.add(p.x-r, p.y-r);
		pl.add(p.x+r, p.y+r);
		ROIPlus circle = new ROIPlus(pl, ROIPlus.ROI_ELLIPSE);
		return circle;
	}

	private Pair<Double,Double> getMedAndMad(ROIPlus circle, FloatProcessor ip)
	{
		float[] tempPixels = null;
		tempPixels = ROIUtility.getPixelsOutsideRoi(ip, circle);
		//		ImagePlus temp = new ImagePlus("Duh", copyProcessor(ip));
		//		temp.setRoi(circle.getRoi());
		//		temp.getProcessor().set(0);
		//		temp.show();
		if(tempPixels == null)
		{
			return null;
		}

		// Convert to double use median calculation code.
		double[] pixels = new double[tempPixels.length];
		int i = 0;
		for (float f : tempPixels)
		{
			pixels[i] = f;
			i++;
		}
		double med = StatisticsUtility.median(pixels);
		double mad = StatisticsUtility.mad(med, pixels);
		return new Pair<Double,Double>(med, mad);
	}

	private TreeMap<DimensionMap,String> getSortedFiles(Collection<File> files)
	{
		TreeMap<DimensionMap,String> sortedFiles = new TreeMap<>();
		for(File f : files)
		{
			if(f.getAbsolutePath().endsWith(".ome.tif") && FileUtility.getFileNameWithExtension(f.getAbsolutePath()).startsWith(prefix))
			{
				String fileName = FileUtility.getFileNameWithoutExtension(f.getAbsolutePath());
				fileName = FileUtility.getFileNameWithoutExtension(fileName); // Remove the .ome subextension
				String[] pieces = fileName.split("_");
				int n = pieces.length;
				String cellNum = pieces[n-2];
				String chString = pieces[n-1];
				chString = chString.substring(2, chString.length());
				sortedFiles.put(new DimensionMap("Cell=" + cellNum + ",Channel=" + chString), f.getAbsolutePath());
			}
		}
		return sortedFiles;
	}

	private <T extends ImageProcessor> T copyProcessor(T imp)
	{
		@SuppressWarnings("unchecked")
		T toSave = (T) imp.createProcessor(imp.getWidth(), imp.getHeight());
		toSave.copyBits(imp, 0, 0, Blitter.COPY);
		return toSave;
	}

	private int getMethodInt()
	{
		String method = autoThreshMethod;
		int methodInt = AutoThresholder.OTSU;
		if(method.equals("HUANG"))
		{
			methodInt = AutoThresholder.HUANG;
		}
		else if(method.equals("INTERMODES"))
		{
			methodInt = AutoThresholder.INTERMODES;
		}
		else if(method.equals("ISODATA"))
		{
			methodInt = AutoThresholder.ISODATA;
		}
		else if(method.equals("LI"))
		{
			methodInt = AutoThresholder.LI;
		}
		else if(method.equals("MAXENTROPY"))
		{
			methodInt = AutoThresholder.MAXENTROPY;
		}
		else if(method.equals("MEAN"))
		{
			methodInt = AutoThresholder.MEAN;
		}
		else if(method.equals("MINERROR"))
		{
			methodInt = AutoThresholder.MINERROR;
		}
		else if(method.equals("MINIMUM"))
		{
			methodInt = AutoThresholder.MINIMUM;
		}
		else if(method.equals("MOMENTS"))
		{
			methodInt = AutoThresholder.MOMENTS;
		}
		else if(method.equals("OTSU"))
		{
			methodInt = AutoThresholder.OTSU;
		}
		else if(method.equals("PERCENTILE"))
		{
			methodInt = AutoThresholder.PERCENTILE;
		}
		else if(method.equals("RENYIENTROPY"))
		{
			methodInt = AutoThresholder.RENYIENTROPY;
		}
		else if(method.equals("SHANBHAG"))
		{
			methodInt = AutoThresholder.SHANBHAG;
		}
		else if(method.equals("TRIANGLE"))
		{
			methodInt = AutoThresholder.TRIANGLE;
		}
		else if(method.equals("YEN"))
		{
			methodInt = AutoThresholder.YEN;
		}
		return methodInt;
	}

	private double getThreshold(ImageProcessor ip, int methodInt)
	{
		AutoThresholder at = new AutoThresholder();

		// Do threshold
		FloatProcessor temp = (FloatProcessor) ip.convertToFloat();
		FunctionUtility.imAdjust(temp, ip.getMin(), ip.getMax(), 0d, 255d, 1d);
		ByteProcessor bp = (ByteProcessor) temp.convertToByte(false);
		int[] hist = bp.getHistogram();
		double threshold = at.getThreshold(methodInt, hist);
		threshold = threshold * this.autoThreshMultiplier;
		if(threshold > 255)
		{
			threshold = 255;
		}
		else if(threshold < 0)
		{
			threshold = 0;
		}
		return threshold;
	}

	private ByteProcessor getByteProcessor(FloatProcessor imp, boolean invert)
	{
		ImagePlus im = FunctionUtility.makeImageToSave(imp, false, 1, 8, invert); // Creating black and white image
		return (ByteProcessor) im.getProcessor();
	}

	private <T extends ImageProcessor> T getPage(DimTable pageTable, TreeMap<DimensionMap,Pair<Integer,T>> imageMap, int offsetLevel)
	{
		int pageWidth = nCols*thumbSize;
		int pageHeight = nRows*thumbSize;

		@SuppressWarnings("unchecked")
		T page = (T) imageMap.firstEntry().getValue().p2.createProcessor(pageWidth, pageHeight);
		page.set(offsetLevel);

		for(DimensionMap map : pageTable.getMapIterator())
		{
			// Get the image
			T imp = imageMap.get(map).p2;

			// Get center
			IdPoint p = new IdPoint(imp.getWidth()/2, imp.getHeight()/2, 0);

			// Get rc position
			int i = imageMap.get(map).p1;
			int c = i % nCols; // values for c start at 0
			int r = i / nCols; // values for r start at 0
			int ULx = (int) Math.round((thumbSize*(c + 0.5) - p.x));
			int ULy = (int) Math.round((thumbSize*(r + 0.5) - p.y));
			page.copyBits(imp, ULx, ULy, Blitter.COPY);
		}

		return page;
	}
	
	private <T extends ImageProcessor> ROIPlus getPageBestPoints(ByteProcessor bp, DimTable pageTable, TreeMap<DimensionMap,Pair<Integer,T>> imageMap)
	{
		PointList pl = new PointList();
		for(DimensionMap map : pageTable.getMapIterator())
		{
			// Get the image
			T imp = imageMap.get(map).p2;

			// Get center
			IdPoint p = new IdPoint(imp.getWidth()/2, imp.getHeight()/2, 0);

			// Get rc position
			int i = imageMap.get(map).p1;
			int c = i % nCols; // values for c start at 0
			int r = i / nCols; // values for r start at 0
			int ULx = (int) Math.round((thumbSize*(c + 0.5) - p.x));
			int ULy = (int) Math.round((thumbSize*(r + 0.5) - p.y));
			Roi roi = new Roi(ULx, ULy, imp.getWidth(), imp.getHeight());

			// Save cell number and image position
			if(bp != null)
			{
				Point bestP = getBestPoint((ByteProcessor) bp, roi);
				if(bestP != null)
				{
					pl.add(new IdPoint(ULx + bestP.x, ULy + bestP.y, new Integer(map.get("Cell"))));
				}
			}
			else
			{
				// Just approximate where the cell is.
				pl.add(new IdPoint((int) Math.round(thumbSize*(c + 0.5)), (int) Math.round(thumbSize*(r + 0.5)), new Integer(map.get("Cell"))));
			}
		}

		ROIPlus roi = new ROIPlus(pl, ROIPlus.ROI_POINT);
		return roi;
	}

	private Point getBestPoint(ByteProcessor bp, Roi roi)
	{
		// Set the roi
		bp.setRoi(roi);
		bp = (ByteProcessor) bp.crop();
		
		// Get Maxima
		MaximumFinder mf = new MaximumFinder();
		ROIPlus maxima = (ROIPlus) mf.findMaxima(bp, 25, 125, MaximumFinder.ROI, true, false, roi, false);

		// Get Regions
		Img<UnsignedByteType> img = ImageJFunctions.wrapByte(new ImagePlus("Duh", bp));
		ImgLabeling<Integer, IntType> labeling = utils.getLabeling(img, true);
		LabelRegions<Integer> regions = new LabelRegions<>(labeling);

		LabelRegion<Integer> largest = null;
		for (LabelRegion<Integer> region : regions)
		{
			if(largest == null)
			{
				largest = region;
			}
			else
			{
				if(region.size() > largest.size())
				{
					largest = region;
				}
			}
		}

		RandomAccess<BoolType> ra = largest.randomAccess();
		for (IdPoint p : maxima.pointList)
		{
			ra.setPosition(p);
			if (ra.get().get())
			{
				//				ImagePlus im = new ImagePlus("duh", bp);
				//				PointList pl = new PointList();
				//				pl.add(p);
				//				im.setRoi((new ROIPlus(pl, ROIPlus.ROI_POINT)).getRoi());
				//				im.show();
				return p;
			}
		}
		return null;
	}
}
