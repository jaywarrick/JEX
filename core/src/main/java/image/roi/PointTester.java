package image.roi;

import function.CrunchFactory;
import function.plugin.IJ2.IJ2PluginUtility;
import function.plugin.mechanism.JEXCrunchablePlugin;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.JEXPluginInfo;
import function.plugin.plugins.featureExtraction.FeatureUtils;
import function.singleCellAnalysis.SingleCellUtility;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.io.FileSaver;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.FloatStatistics;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import io.scif.Checker;
import io.scif.Format;
import io.scif.FormatException;
import io.scif.ImageMetadata;
import io.scif.MetaTable;
import io.scif.Metadata;
import io.scif.Plane;
import io.scif.Reader;
import io.scif.SCIFIO;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import io.scif.img.SCIFIOImgPlus;

import java.awt.Desktop;
import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import jex.JEXManager;
import jex.StatusBar;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import jex.utilities.ROIUtility;
import loci.common.DataTools;
import logs.Logs;
import miscellaneous.DirectoryManager;
import miscellaneous.FileUtility;
import miscellaneous.StatisticsUtility;
import miscellaneous.StringUtility;
import net.imagej.ChannelCollection;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.display.DataView;
import net.imagej.display.ImageDisplay;
import net.imagej.display.OverlayView;
import net.imagej.ops.OpRef;
import net.imagej.ops.features.sets.FirstOrderStatFeatureSet;
import net.imagej.options.OptionsChannels;
import net.imagej.overlay.Overlay;
import net.imagej.overlay.RectangleOverlay;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayCursor;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.ByteArray;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.CalibratedAxis;
import net.imglib2.meta.ImgPlus;
import net.imglib2.roi.EllipseRegionOfInterest;
import net.imglib2.roi.Regions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegionCursor;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

import org.apache.commons.math3.stat.StatUtils;
import org.scijava.command.CommandInfo;
import org.scijava.plugin.DefaultPluginFinder;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.util.ConversionUtils;

import rtools.R;
import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import updates.Updater;
import weka.core.converters.JEXTableWriter;
import Database.DBObjects.JEXData;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.FileWriter;
import Database.SingleUserDatabase.JEXDataIO;

public class PointTester {// extends URLClassLoader {

	//	static
	//	{
	//		LegacyInjector.preinit();
	//	}

	//	public PointTester() throws ImgIOException
	//	{
	//		ImageJ ij = new ImageJ();
	//		
	//		ij.ui().showUI();
	//		// open file as float with ImgOpener
	//		Img< FloatType > img =
	//				new ImgOpener(ij.getContext()).openImgs( "/Users/jaywarrick/Downloads/Dot_Blot.tif", new FloatType() ).get(0);
	//		
	//		// display image
	//		ImageJFunctions.show( img );
	//		
	//		// use a View to define an interval (min and max coordinate, inclusive) to display
	//		RandomAccessibleInterval< FloatType > view =
	//				Views.interval( img, new long[] { 200, 200 }, new long[]{ 500, 350 } );
	//		
	//		// display only the part of the Img
	//		ImageJFunctions.show( view );
	//		
	//		// or the same area rotated by 90 degrees (x-axis (0) and y-axis (1) switched)
	//		ImageJFunctions.show( Views.rotate( view, 0, 1 ) );
	//	}

	protected static final long SEED = 1234567890L;

	public static void main(String[] args) throws Exception
	{		
		tryOps3();
	}

	public static void tryCCA() throws ImgIOException
	{
		ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij.getContext());
		List<SCIFIOImgPlus < UnsignedByteType >> images = imgOpener.openImgs("/Users/jaywarrick/Pictures/TIFFS/MM/0_0_x0_y0_Channel395 X 455M_ImCol0_ImRow1_Z1.tif", new UnsignedByteType());
		ImgLabeling<Integer, UnsignedShortType> labeling = FeatureUtils.getConnectedComponents(images.get(0), true);
		LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);
		double[] pos = new double[images.get(0).numDimensions()];

		for(LabelRegion<Integer> region : regions)
		{
			int id = region.getLabel();
			region.getCenterOfMass().localize(pos);

			Logs.log("" + id + " at " + pos[0] + "," + pos[1] + " - Size: " + region.size(), PointTester.class);
			Logs.log("Contains? " + contains(region, new Point(101,180)), PointTester.class);

		}
		Logs.log("Seems to have worked", PointTester.class);

		//		Img<UnsignedShortType> test = FeatureUtils.getConnectedComponentsImage(images.get(0), true);
		//		
		//		IJ2PluginUtility.ij.legacy().uiService().show(test);
		//		
		//		Logs.log("Seems to have worked again", PointTester.class);
		//		
	}

	public static boolean contains(LabelRegion<?> region, Point p)
	{
		LabelRegionCursor c = region.localizingCursor();
		do
		{
			if(c.getIntPosition(0) == p.x && c.getIntPosition(1) == p.y)
			{
				return true;
			}
			c.next();
		} 
		while(c.hasNext());

		return false;
	}

	public static void tryShinyApp()
	{
		R.eval("ddflkjsldkjf <- 0");
		R.serverEval("source('/Users/jaywarrick/Public/DropBox/GitHub/R-CTCApp/Images/ui.R')");
		R.serverEval("source('/Users/jaywarrick/Public/DropBox/GitHub/R-CTCApp/Images/server.R')");
		R.serverEval("runApp(shinyApp(ui=getUI(), server=getServer()), launch.browser=TRUE)");
		//ScriptRepository.runRScript(new String[]{"source('/Users/jaywarrick/Public/DropBox/GitHub/R-CTCApp/Images/ui.R');","source('/Users/jaywarrick/Public/DropBox/GitHub/R-CTCApp/Images/server.R');","runApp(shinyApp(ui=getUI(), server=getServer()), launch.browser=TRUE);"});
		Logs.log("Yo", PointTester.class);
	}

	//	public static <T extends RealType<T> & NativeType<T>> void Example1d() throws ImgIOException
	//	{
	//		ImageJ ij = new ImageJ();
	//		
	//		ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij.getContext());
	//		
	//		// open with ImgOpener. The type (e.g. ArrayImg, PlanarImg, CellImg) is
	//		// automatically determined. For a small image that fits in memory, this
	//		// should open as an ArrayImg.
	//		List<SCIFIOImgPlus < UnsignedShortType >> images = imgOpener.openImgs("/Users/jaywarrick/Pictures/TIFFS/PC3_001_8bit.tif", new UnsignedShortType());
	//		
	//		ImgFactory< UnsignedByteType > factory = new ArrayImgFactory< UnsignedByteType >();
	//		
	//		final Labeling< Long > LABELING = new NativeImgLabeling< Long, UnsignedShortType >( images.get(0)  );
	//		final Collection<Long> labels = LABELING.firstElement().getMapping().getLabels();
	//		final ArrayList<long[]> centroidsList = new ArrayList<long[]>();
	//		for (final Long i : labels)
	//		{
	////			final IterableInterval<BitType> ii =
	////					LABELING.getIterableRegionOfInterest(i).getIterableIntervalOverROI(src);
	////			
	////			@SuppressWarnings("unchecked")
	////			FirstOrderStatFeatureSet<Img<UnsignedShortType>> op = IJ2PluginUtility.ij.op().op(FirstOrderStatFeatureSet.class, view);
	////			
	////			for (final Entry<? extends OpRef,DoubleType> result : op.compute(images.get(0)).entrySet())
	////			{
	////				System.out.println(result.getKey().getType().getSimpleName() + " " + result.getValue().get());
	////			}
	////			Logs.log("\n\n", PointTester.class);
	//			
	//		}
	//		
	//	}

	public static void tryLabels() throws ImgIOException
	{
		ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij.getContext());
		// open with ImgOpener. The type (e.g. ArrayImg, PlanarImg, CellImg) is
		// automatically determined. For a small image that fits in memory, this
		// should open as an ArrayImg.

		// Get the black and white image
		List<SCIFIOImgPlus < UnsignedShortType >> images = imgOpener.openImgs("/Users/jaywarrick/Pictures/TIFFS/MM/x0_y0_Channel395 X 455M_ImCol0_ImRow1_Z1.tif", new UnsignedShortType());

		// Get the ROIs from the BW image.
	}

	public static < T extends RealType< T > & NativeType< T > > void tryOps2() throws ImgIOException
	{
		ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij.getContext());

		// open with ImgOpener. The type (e.g. ArrayImg, PlanarImg, CellImg) is
		// automatically determined. For a small image that fits in memory, this
		// should open as an ArrayImg.
		List<SCIFIOImgPlus < UnsignedShortType >> images = imgOpener.openImgs("/Users/jaywarrick/Pictures/TIFFS/MM/x0_y0_Channel395 X 455M_ImCol0_ImRow1_Z1.tif", new UnsignedShortType());

		ImagePlus im = new ImagePlus("/Users/jaywarrick/Pictures/TIFFS/MM/x0_y0_Channel395 X 455M_ImCol0_ImRow1_Z1.tif");
		ByteProcessor fp = im.getProcessor().convertToByteProcessor();
		fp.setRoi(20, 20, 20, 30);
		ByteProcessor ip = (ByteProcessor) fp.crop();
		ArrayImg<UnsignedByteType, ByteArray> temp = ArrayImgs.unsignedBytes((byte[]) ip.getPixels(), ip.getWidth(), ip.getHeight());
		ImageJFunctions.show(temp);

		// use a View to define an interval (min and max coordinate, inclusive) to display
		RandomAccessibleInterval< UnsignedShortType > view =
				Views.interval( images.get(0), new long[] { 200, 200 }, new long[]{ 500, 350 } );

		@SuppressWarnings("unchecked")
		FirstOrderStatFeatureSet<Img<UnsignedShortType>> op = IJ2PluginUtility.ij.op().op(FirstOrderStatFeatureSet.class, view);

		for (final Entry<? extends OpRef,DoubleType> result : op.compute(images.get(0)).entrySet())
		{
			System.out.println(result.getKey().getType().getSimpleName() + " " + result.getValue().get());
		}
		Logs.log("\n\n", PointTester.class);

	}
	
	public static TreeMap<String,JEXData> getJEXData()
	{
		JEXData maskData = new JEXData(JEXData.IMAGE, "Mask");
		JEXDataIO.loadJXD(maskData, "/Users/jaywarrick/Documents/JEX/Feature Extraction/Dataset Name/Cell_x0_y0/Image-Mask/x0_y0.jxd");
		JEXData imageData = new JEXData(JEXData.IMAGE, "Image");
		JEXDataIO.loadJXD(imageData, "/Users/jaywarrick/Documents/JEX/Feature Extraction/Dataset Name/Cell_x0_y0/Image-Image/x0_y0.jxd");
		JEXData roiData = new JEXData(JEXData.ROI, "Maxima");
		JEXDataIO.loadJXD(roiData, "/Users/jaywarrick/Documents/JEX/Feature Extraction/Dataset Name/Cell_x0_y0/Roi-Maxima/x0_y0.jxd");
		
		TreeMap<String,JEXData> ret = new TreeMap<String,JEXData>();
		ret.put("Mask", maskData);
		ret.put("Image", imageData);
		ret.put("Maxima", roiData);
		
		return ret;
	}

	public static void tryOps3()
	{
		JEXStatics.statusBar = new StatusBar();
		
		TreeMap<String,JEXData> data = getJEXData();
		
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(data.get("Image"));
		TreeMap<DimensionMap,String> maskMap = ImageReader.readObjectToImagePathTable(data.get("Mask"));
		TreeMap<DimensionMap,ROIPlus> roiMap = RoiReader.readObjectToRoiMap(data.get("Maxima"));
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		TreeMap<DimensionMap,Double> outputStatMap = new TreeMap<DimensionMap,Double>();
		TreeMap<Integer,Integer> idToLabelMap = new TreeMap<Integer,Integer>();
		int count = 0, percentage = 0;

		ImgOpener imgOpener = new ImgOpener(IJ2PluginUtility.ij.getContext());

		DirectoryManager.setHostDirectory("/Users/jaywarrick/Documents/JEX/Test");
		
		try
		{						
			// Loop through the items in the n-Dimensional object
			for (DimensionMap mapM : maskMap.keySet())
			{
				List<SCIFIOImgPlus<UnsignedByteType>> masks = imgOpener.openImgs(maskMap.get(mapM), new UnsignedByteType());
				
				for(Img<UnsignedByteType> mask : masks)
				{
					ImageJFunctions.show(mask);
					
					// Get the full labeling for the mask
					ImgLabeling<Integer, UnsignedShortType> labeling = FeatureUtils.getConnectedComponents(mask, true);
					LabelRegions<Integer> regions = new LabelRegions<Integer>(labeling);
					
					// Determine which labelings are the ones we want to keep by testing if our maxima of interest are contained.
					ROIPlus maxima = roiMap.get(mapM);
					for(LabelRegion<Integer> region : regions)
					{
						for(IdPoint p : maxima.pointList)
						{
							if(contains(region, p))
							{
								idToLabelMap.put(p.id, region.getLabel());
							}
						}
						
					}
					
					// Now we have the regions we want to quantify
					
					
					// For now just try to get the first order stats of the MASK image
					if(true)
					{
						@SuppressWarnings("unchecked")
						FirstOrderStatFeatureSet<IterableInterval<UnsignedByteType>> op = IJ2PluginUtility.ij.op().op(FirstOrderStatFeatureSet.class, (IterableInterval<UnsignedByteType>) mask);
						
						for(IdPoint p : maxima.pointList)
						{
							LabelRegion<Integer> region = regions.getLabelRegion(idToLabelMap.get(p.id));
							List<Pair<String, DoubleType>> results = op.getFeatureList(Regions.sample(region, mask));
							for(Pair<String, DoubleType> result : results)
							{
								DimensionMap newMap = mapM.copyAndSet("Measurement=" + result.getA());
								newMap.put("Id", ""+p.id);
								outputStatMap.put(newMap.copy(), result.getB().get());
								newMap.put("Measurement", "Area");
								outputStatMap.put(newMap, (double) 	region.size());
							}
						}
						
						
					}
					
				}
				
			}
			
			String tablePath = JEXTableWriter.writeTable("FirstOrderStats", outputStatMap);
			
			try {
				FileUtility.openFileDefaultApplication(tablePath);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		catch (ImgIOException e)
		{
			e.printStackTrace();
		}
	}

	//	public static void tryOps()
	//	{
	//		ImageJ ij = new ImageJ();
	//		/**
	//		 * Some random images
	//		 */
	//		Img<UnsignedByteType> empty;
	//		Img<UnsignedByteType> constant;
	//		Img<UnsignedByteType> random;
	//		Img<UnsignedByteType> ellipse;
	//		Img<UnsignedByteType> rotatedEllipse;
	//		
	//		OutputOpBuilderService fs;
	//		
	//		Context context = ij.getContext();
	//
	//		
	//		List<PluginInfo<Op>> opIs = ij.op().getPlugins();
	//		for(PluginInfo<Op> op : opIs)
	//		{
	//			if(op.getClassName().startsWith("net.imagej.ops.features.sets"))
	//			{
	//				Logs.log(op.toString(), PointTester.class);
	//			}
	//		}
	//		
	//		// SETUP //
	//		ImageGenerator dataGenerator = new ImageGenerator(SEED);
	//		long[] dim = new long[] { 100, 100 };
	//
	//		empty = dataGenerator.getEmptyUnsignedByteImg(dim);
	//		constant = dataGenerator.getConstantUnsignedByteImg(dim, 15);
	//		random = dataGenerator.getRandomUnsignedByteImg(dim);
	//
	//		double[] offset = new double[] { 0.0, 0.0 };
	//		double[] radii = new double[] { 20, 40 };
	//		ellipse = dataGenerator.getEllipsedBitImage(dim, radii, offset);
	//
	//		// translate and rotate ellipse
	//		offset = new double[] { 10.0, -10.0 };
	//		radii = new double[] { 40, 20 };
	//		rotatedEllipse = dataGenerator.getEllipsedBitImage(dim, radii, offset);
	//
	//		fs = context.getService(OutputOpBuilderService.class);
	//		
	//		// END SETUP // 
	//		
	//		// Test first order features
	//		
	//		@SuppressWarnings("unchecked")
	//		FirstOrderStatFeatureSet<Img<UnsignedByteType>> op = ij.op().op(FirstOrderStatFeatureSet.class, random);
	//		
	//		for (final Entry<? extends OpRef,DoubleType> result : op.compute(random).entrySet())
	//		{
	//			System.out.println(result.getKey().getType().getSimpleName() + " " + result.getValue().get());
	//		}
	//		Logs.log("\n\n", PointTester.class);
	//		for (final Entry<? extends OpRef,DoubleType> result : op.compute(constant).entrySet())
	//		{
	//			System.out.println(result.getKey().getType().getSimpleName() + " " + result.getValue().get());
	//		}
	//		Logs.log("\n\n", PointTester.class);
	//		for (final Entry<? extends OpRef,DoubleType> result : op.compute(empty).entrySet())
	//		{
	//			System.out.println(result.getKey().getType().getSimpleName() + " " + result.getValue().get());
	//		}
	//		
	//		// Test zernicke features
	//		// test on image with constant filling
	//        Map<? extends OpRef, DoubleType[]> res = ij.op().op(ZernikeFeatureSet.class, constant, true, true, 1, 3).compute(constant);
	//        for(Entry<? extends OpRef,DoubleType[]> x : res.entrySet())
	//        {
	//        	Logs.log(""+x.getKey().getType().getSimpleName(), PointTester.class);
	//        	System.out.println("");
	//        	for(DoubleType d : (DoubleType[]) x.getValue())
	//        	{
	//        		System.out.println(d);
	//        	}
	//        }
	//	}

	public static double[] calculateModes(final int[] n)
	{
		//		double[] ret = calculateModes(new int[]{12,13,14});
		//		for(double d : ret)
		//		{
		//			System.out.println(d);
		//		}

		double[] d = new double[n.length];
		for(int i=0; i < n.length; i++)
		{
			d[i] = n[i];
		}
		double[] modes = StatUtils.mode(d);
		return modes;
	}

	public static void playWithChoiceDialog()
	{
		JEXDialog.getChoice("Title", "Which Becaus this is a really long question that I can't figure out and I really need help or else I won't know what to do. choice do you want? Which Becaus this is a really long question that I can't figure out and I really need help or else I won't know what to do. choice do you want? Which Becaus this is a really long question that I can't figure out and I really need help or else I won't know what to do. choice do you want? Which Becaus this is a really long question that I can't figure out and I really need help or else I won't know what to do. choice do you want? Which Becaus this is a really long question that I can't figure out and I really need help or else I won't know what to do. choice do you want?", new String[]{"Choice 1", "Choice 2"}, 0);
		Logs.log("Hello there", PointTester.class);
	}

	public static void tryPluginFinder()
	{
		InputStream iStream = null;
		try
		{
			String pathToJar = "/Users/jaywarrick/Public/DropBox/GitHub/JEX-CTCPlugins/target/pom-ctc-0.0.1-SNAPSHOT.jar"; //JEXDialog.fileChooseDialog(true);
			URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
			URLClassLoader cl = URLClassLoader.newInstance(urls);

			//iStream = cl.getResourceAsStream("org.scijava.plugin.Plugin");


			DefaultPluginFinder pf = new DefaultPluginFinder();
			List<PluginInfo<?>> ret = new Vector<PluginInfo<?>>();
			pf.findPlugins(ret);
			for(PluginInfo<?> pi : ret)
			{
				if(pi.getClassName().startsWith("function.") || pi.getClassName().startsWith("plugins."))
				{
					System.out.println(pi);
				}

			}

			Logs.log("Yay!", PointTester.class);
		}
		catch (MalformedURLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			if(iStream != null)
			{
				try
				{
					iStream.close();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}


	}

	public static void checkImportPlugin() throws Exception
	{
		ImageJ ij = new ImageJ();

		String pathToJar = "/Users/jaywarrick/Public/DropBox/GitHub/JEX-CTCPlugins/target/pom-ctc-0.0.1-SNAPSHOT.jar"; //JEXDialog.fileChooseDialog(true);
		JarFile jarFile = new JarFile(pathToJar);
		Enumeration<JarEntry> e = jarFile.entries();

		URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
		URLClassLoader cl = URLClassLoader.newInstance(urls);

		while (e.hasMoreElements()) {
			JarEntry je = (JarEntry) e.nextElement();
			if(je.isDirectory() || !je.getName().endsWith(".class")){
				continue;
			}
			// -6 because of .class
			String className = je.getName().substring(0,je.getName().length()-6);
			className = className.replace('/', '.');
			Class<?> c = cl.loadClass(className);
			if(JEXPlugin.class.isAssignableFrom(c))
			{
				//		    	JEXPlugin jp = (JEXPlugin) c.newInstance();
				final Plugin p = c.getAnnotation(Plugin.class);
				@SuppressWarnings("unchecked")
				PluginInfo<JEXPlugin> pi = new PluginInfo(c, JEXPlugin.class, p);
				ij.plugin().addPlugin(pi);
				JEXPluginInfo jpi = new JEXPluginInfo(pi);
				JEXCrunchablePlugin crunchable = new JEXCrunchablePlugin(jpi);
				Logs.log(crunchable.toString(), PointTester.class);
				Logs.log("Yay!", PointTester.class);
			}

		}
	}

	public static void checkScifioStuff() throws Exception
	{
		ImageJ ij = new ImageJ();
		String path = "/Users/jaywarrick/Downloads/Test.nd2";
		Set<Format> formats = (Set<Format>)ij.scifio().format().getAllFormats();
		for(Format format : formats)
		{
			System.out.println(format.getClass().getSimpleName());
			System.out.println(format.isEnabled());
			Checker c = format.createChecker();
			System.out.println(c.isFormat(path));
			System.out.println(c.isFormat(path, new SCIFIOConfig().checkerSetOpen(true)));
			System.out.println(format.createChecker().isFormat(path, new SCIFIOConfig().checkerSetOpen(true)));
		}
		Reader reader = ij.scifio().initializer().initializeReader(path, new SCIFIOConfig().checkerSetOpen(true));
		getTiffs(path, ij);
		//System.out.println(OS.isMacOSX());
	}

	public static void playWithUpdater()
	{
		String s = "JEX-0.0.4-SNAPSHOT-all.zip";
		String[] s1 = s.split("\\-");
		for(String x : s1)
		{
			Logs.log(""+x, PointTester.class);
		}
		String[] s2 = s1[1].split("\\.");
		for(String x : s2)
		{
			Logs.log(""+x, PointTester.class);
		}
	}

	public static TreeMap<DimensionMap,String> getTiffs(String path, ImageJ ij) throws IOException, FormatException
	{
		final String outPath = "/Users/jaywarrick/Desktop/NewFolder";
		final SCIFIO scifio = new SCIFIO(ij.getContext());
		final Reader reader = scifio.initializer().initializeReader(path, new SCIFIOConfig().checkerSetOpen(true));
		Metadata meta = reader.getMetadata();

		DimTable table = getDataSetDimTable(meta);
		Logs.log(table.toString(), PointTester.class);

		String baseName = "Image_";
		String ext = ".tif";
		File outdir = new File(outPath);
		outdir.mkdirs();
		File[] files = outdir.listFiles();
		for(File f : files)
		{
			f.delete();
		}
		TreeMap<DimensionMap,String> ret = new TreeMap<DimensionMap,String>();
		Iterator<DimensionMap> itr = table.getMapIterator().iterator();
		for (int i = 0; i < reader.getImageCount(); i++) {
			for (int j = 0; j < reader.getPlaneCount(i); j++) {
				Plane plane = reader.openPlane(i, j);
				ImageMetadata d = plane.getImageMetadata();
				long[] dims = d.getAxesLengthsPlanar();
				short[] converted = (short[]) DataTools.makeDataArray(plane.getBytes(), 2, false, d.isLittleEndian());
				ShortProcessor p = new ShortProcessor((int)dims[0], (int)dims[1], converted, null);
				FileSaver fs = new FileSaver(new ImagePlus("temp", p));
				String filename = outPath + File.separator + baseName + i + "_" + j + ext;
				fs.saveAsTiff(filename);
				DimensionMap map = itr.next();
				ret.put(map,filename);
				Logs.log(map.toString(), PointTester.class);
			}
		}
		return ret;
	}

	public static String WAVELENGTH="Name #", X_POSITION="Z position", Y_POSITION="Z position", Z_POSITION="Z position", TIMESTAMP="timestamp", LAMBDA1="λ", LAMBDA2="� ";

	public static void testAutoImport() throws Exception {

		final String filePath = "/Users/jaywarrick/Google Drive/example.nd2";
		final String sampleImage = "/Users/jaywarrick/Google Drive/example.nd2";
		final String outPath = "/Users/jaywarrick/Desktop/NewFolder";

		ImageJ ij = new ImageJ();

		TreeMap<DimensionMap,String> filenames = getTiffs(filePath, ij);

		Logs.log(filenames.toString(), PointTester.class);

	}

	private static DimTable getDataSetDimTable(Metadata meta)
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

	/**
	 * Returns the plane number and the value of the information for that basename
	 */
	private static TreeMap<String,String> getOrderedInfo(ImageMetadata iMeta, String basename)
	{
		TreeMap<String,String> ret = new TreeMap<String,String>(new StringUtility());
		MetaTable info = iMeta.getTable();
		for(Entry<String,Object> e : info.entrySet())
		{
			String key = e.getKey();
			String val = (String) e.getValue().toString();
			if(key.contains(basename))
			{
				ret.put(key,val);
			}
		}
		return ret;
	}


	private static void dumpInfo(final ImgPlus<?> img) {
		for (int d = 0; d < img.numDimensions(); d++) {
			final CalibratedAxis axis = img.axis(d);
			System.out.println("dim #" + d + ": " + img.dimension(d) + " : " +
					axis.type());
		}
	}

	public static void testConversionUtils()
	{
		String s = "5.1";
		Logs.log("" + ConversionUtils.canConvert(s, int.class), PointTester.class);
		Object o = ConversionUtils.convert(s, int.class);
		Logs.log("" + o.getClass().getSimpleName(), PointTester.class);
	}

	//	public static void testSciJava()
	//	{
	//		ImageJ ij = new ImageJ();
	//		List<PluginInfo<JEXPlugin>> plugins = ij.plugin().getPluginsOfType(JEXPlugin.class);
	//		
	//		for(PluginInfo<JEXPlugin> info : plugins)
	//		{
	//			Class<? extends JEXPlugin> pluginClass;
	//			try
	//			{
	//				pluginClass = info.loadClass();
	//				JEXPluginInfo jexInfo = new JEXPluginInfo(info);
	//				Logs.log(jexInfo.parameters.get("Old Min").toString(), PointTester.class);
	//				Logs.log(info.toString(), PointTester.class);
	//			}
	//			catch (InstantiableException e)
	//			{
	//				e.printStackTrace();
	//			}
	//		}
	//	}

	public static void testTableWriter()
	{
		DirectoryManager.setHostDirectory("/Users/jaywarrick/Desktop/Temp2");
		TreeMap<DimensionMap,Object> stuff1 = new TreeMap<DimensionMap,Object>();
		TreeMap<DimensionMap,Object> stuff2 = new TreeMap<DimensionMap,Object>();
		String[] things = new String[] { "hI", "there", "what's", "up", "with" };
		Double[] bobs = new Double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
		Dim d = new Dim("Index", 5);
		DimTable table = new DimTable();
		table.add(d);
		int i = 0;
		for (DimensionMap map : table.getMapIterator())
		{
			stuff1.put(map, things[i]);
			stuff2.put(map, bobs[i]);
			i++;
		}
		String path1 = JEXTableWriter.writeTable("Test", stuff1);
		String path2 = JEXTableWriter.writeTable("Test", stuff2);
		try
		{
			FileUtility.openFileDefaultApplication(path1);
			FileUtility.openFileDefaultApplication(path2);
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void testGetMask()
	{
		String imagePath = "/Users/jaywarrick/Documents/My Pictures/TIFFS/x0_y0_Color0_Time2.tif";
		ImagePlus im = new ImagePlus(imagePath);
		im.show();
		PointList pl = new PointList();
		pl.add(100, 0);
		pl.add(100, 100);
		pl.add(150, 100);
		pl.add(150, 0);
		ROIPlus r = new ROIPlus(pl, ROIPlus.ROI_POLYGON);
		im.setRoi(r.getRoi());
		ImageProcessor ip = im.getMask();
		ImageProcessor ip3 = im.getProcessor().crop();
		ImagePlus im2 = new ImagePlus("Hello2", ip);
		ImagePlus im3 = new ImagePlus("Hello3", ip3);
		float[] pixels = ROIUtility.getPixelsInRoi(((FloatProcessor) im.getProcessor().convertToFloat()), r);
		Vector<Double> temp = new Vector<Double>();
		for (float f : pixels)
		{
			temp.add((double) f);
		}
		double mean = StatisticsUtility.mean(temp);

		im2.show();
		im3.show();
		FloatProcessor fp = new FloatProcessor(50, 100, pixels);
		fp.resetMinAndMax();
		ByteProcessor bp = fp.convertToByteProcessor();
		ImagePlus im4 = new ImagePlus("Duh", bp);
		im4.show();
		Logs.log("Size = " + pixels.length + ", Mean = " + mean, PointTester.class);
	}

	public static void checkOpenWebpage()
	{
		if(Desktop.isDesktopSupported())
		{
			URL page;
			try
			{
				page = new URL("http://www.oracle.com/technetwork/java/javase/downloads/index.html");
				Desktop.getDesktop().browse(page.toURI());
			}
			catch (MalformedURLException e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (URISyntaxException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void checkJavaVersion()
	{
		Logs.log(System.getProperty("java.version"), PointTester.class);
		Logs.log("" + Updater.javaVersionIsAtLeast("1.7.0_46"), PointTester.class);
	}

	public static void checkOverlay()
	{
		String imagePath = "/Users/jaywarrick/Documents/My Pictures/TIFFS/x0_y0_Color0_Time2.tif";

		ImageJ ij = new ImageJ();
		List<CommandInfo> commands = ij.command().getCommands();
		CommandInfo info = null;
		for (CommandInfo temp : commands)
		{
			if(temp.getTitle().equals("Fill"))
			{
				info = temp;
			}
		}
		// TreeMap<String,IJ2Plugin> plugins = IJ2PluginUtility.getIJ2Commands();
		try
		{
			if(ij.dataset().canOpen(imagePath))
			{
				Dataset d = ij.dataset().open(imagePath);
				ImageDisplay display = (ImageDisplay) ij.display().createDisplay(d);
				ij.display().setActiveDisplay(display);
				RectangleOverlay r = new RectangleOverlay(ij.getContext());
				r.setOrigin(0, 0);
				r.setOrigin(0, 1);
				r.setExtent(100, 0);
				r.setExtent(100, 1);

				List<Overlay> overlays = new Vector<Overlay>();
				overlays.add(r);
				ij.overlay().addOverlays(display, overlays);

				for (DataView view : display)
				{
					if(view instanceof OverlayView)
					{
						view.setSelected(true);
						break;
					}
				}

				OptionsChannels opts = ij.options().getOptions(OptionsChannels.class);
				ChannelCollection fg = opts.getFgValues();
				ij.ui().show(display);
				// CommandInfo info = plugins.get("Fill").command;
				ij.command().run(info, true, new Object[] { "display", display });
				// ij.overlay().fillOverlay(r, display, fg);
				// ij.ui().show(display);
				ij.ui().show(display);

			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void checkStatisticsUtility()
	{
		Vector<Double> test = new Vector<Double>();
	}

	public static void checkHungarianDistance()
	{
		Random r = new Random();
		int n = 100;
		PointList a = new PointList();
		PointList b = new PointList();
		for (int i = 1; i <= n; i++)
		{
			a.add(new Point(r.nextInt(n), r.nextInt(n)));
			b.add(new Point(r.nextInt(n), r.nextInt(n)));
		}
		PointList b2 = StatisticsUtility.getMinDistanceMappingOfB(a, b);
		Logs.log("A:\n\t" + a.toString(), PointTester.class);
		Logs.log("B:\n\t" + b.toString(), PointTester.class);
		Logs.log("B2:\n\t" + b2.toString(), PointTester.class);
		Logs.log("Before: " + StatisticsUtility.getMappingDistance(a, b), PointTester.class);
		Logs.log("After: " + StatisticsUtility.getMappingDistance(a, b2), PointTester.class);

	}

	public static void checkWandRoi()
	{
		ImagePlus im = new ImagePlus("/Users/jaywarrick/Documents/Yin_Lab/JEX Database/Test 1/CTC Test Set/Cell_x0_y0/Image-CELL CROPS/x0_y0_Color1_Id0.tif");
		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat();
		Wand wand = new Wand(imp);
		wand.autoOutline(38, 38);
		Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);
		// imp.draw(roi);
		imp.setRoi(roi);
		ImagePlus im2 = new ImagePlus("Hello", imp);
		im2.show();
		ImageStatistics stats = ImageStatistics.getStatistics(imp, ImageStatistics.AREA, null);
		Logs.log(stats.area + ", " + wand.npoints + ", " + roi.getLength(), PointTester.class);
		Logs.log("Yo", PointTester.class);

	}

	public static void checkCopyBits()
	{
		ImagePlus im = new ImagePlus("/Users/jaywarrick/Documents/Yin_Lab/JEX Database/Test 1/CTC Test Set/Cell_x0_y0/Image-GREEN/x0_y0_ImCol1_ImRow1.tif");
		FloatProcessor imp = (FloatProcessor) im.getProcessor().convertToFloat();
		int w = im.getWidth();
		int h = im.getHeight();
		FloatProcessor stitchIP = new FloatProcessor(2 * w, 2 * h);
		ImagePlus stitch = new ImagePlus("Stitch", stitchIP);
		stitch.show();
		im.show();
		stitchIP.copyBits(imp, 0, 0, Blitter.COPY);
		stitchIP.copyBits(imp, w, 0, Blitter.COPY);
		stitchIP.copyBits(imp, 0, h, Blitter.COPY);
		stitchIP.copyBits(imp, w, h, Blitter.COPY);
		stitch.setProcessor(stitchIP);
		stitch.resetDisplayRange();
		stitch.show();
		Logs.log("Yo", PointTester.class);

	}

	public static void checkStackRead()
	{
		String filePath = "/Users/jaywarrick/Desktop/CTC/20130806 MSK samples/10-3060 Cell save/tile_x001_y001.tif";
		ImagePlus im = new ImagePlus(filePath);
		im.show();
		Logs.log("Hello2", PointTester.class);
	}

	public static void checkOS()
	{
		String os = System.getProperty("os.name").toLowerCase();
		Logs.log(os, PointTester.class);
	}

	public static void testBackslashStuff()
	{
		String test = "\\C:\\My Documents";
		Logs.log(test, PointTester.class);
		Logs.log("" + test.length(), PointTester.class);
		Logs.log("" + test.startsWith("\\"), PointTester.class);
		for (int i = 0; i < test.length(); i++)
		{
			Logs.log(test.substring(i, i + 1), PointTester.class);
		}
		Logs.log(test.substring(1, test.length()), PointTester.class);
	}

	public static void testJarStuff()
	{
		try
		{
			URL jarFile = new URL("jar:file:/Users/jaywarrick/Desktop/Temp/JEX.jar!/");
			JarFile jar = ((JarURLConnection) jarFile.openConnection()).getJarFile();
			Enumeration<JarEntry> e = jar.entries();
			while (e.hasMoreElements())
			{
				JarEntry file = e.nextElement();
				// if(file.getName().endsWith("png") || file.getName().startsWith("JEX_"))
				// {
				Logs.log(file.getName(), CrunchFactory.class);
				// }
			}
		}
		catch (MalformedURLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void convertImages()
	{
		File dir = new File("/Volumes/NO NAME/Iyer scope test images/Originals");
		Vector<String> fileList = new Vector<String>();
		for (File f : dir.listFiles())
		{
			if(!f.isDirectory() && f.getAbsolutePath().endsWith("tif"))
			{
				fileList.add(f.getAbsolutePath());
			}
		}
		StringUtility.sortStringList(fileList);

		DecimalFormat format = new DecimalFormat("0000000000");
		String baseName = "Image_";
		int counter = 0;
		for (String path : fileList)
		{
			ImagePlus im = new ImagePlus(path);
			for (int s = 1; s < im.getStackSize() + 1; s++)
			{
				im.setSlice(s);
				ImagePlus toSave = new ImagePlus("temp", im.getProcessor());
				FileSaver fs = new FileSaver(toSave);
				fs.saveAsTiff("/Users/jaywarrick/Desktop/Converted/" + baseName + format.format(counter) + ".tif");
				counter = counter + 1;
			}
		}
	}

	public static void testIterator()
	{
		DimTable t = new DimTable();
		Dim a = new Dim("A", 0, 2);
		Dim b = new Dim("B", 0, 2);
		Dim c = new Dim("C", 0, 3);
		Dim d = new Dim("D", 0, 4);

		t.add(a);
		t.add(b);
		t.add(c);
		t.add(d);

		Logs.log("getMapIterator(filter)", PointTester.class);
		for (DimensionMap map : t.getMapIterator(new DimensionMap("A=0,B=2")))
		{
			Logs.log(map.toString(), PointTester.class);
		}

		Logs.log("getSubTable and getMapIterator", PointTester.class);
		for (DimensionMap map : t.getSubTable(new DimensionMap("A=0,B=2")).getMapIterator())
		{
			Logs.log(map.toString(), PointTester.class);
		}

		Logs.log("SubTableIterator", PointTester.class);
		for (DimTable map : t.getSubTableIterator("A"))
		{
			Logs.log(map.toString(), PointTester.class);
		}
	}

	public static void testR()
	{
		R.eval("x <- 0");
	}

	public static void testLinePlot()
	{
		double xAxisTransition = 5, xLinLogRatio = 10, yAxisTransition = 2, yLinLogRatio = 10;
		double xmin = -10, xmax = 100, ymin = xmin, ymax = xmax;
		double xmin2 = SingleCellUtility.calculateLogicleScaleValue(xmin, xAxisTransition, xLinLogRatio);
		double xmax2 = SingleCellUtility.calculateLogicleScaleValue(xmax, xAxisTransition, xLinLogRatio);
		double ymin2 = SingleCellUtility.calculateLogicleScaleValue(ymin, yAxisTransition, yLinLogRatio);
		double ymax2 = SingleCellUtility.calculateLogicleScaleValue(ymax, yAxisTransition, yLinLogRatio);
		DirectoryManager.setHostDirectory("C:/Users/Admin2.KVIELHUBERCOMP/Documents");
		R.eval("x <- 1:10");
		String path = R.startPlot("pdf", 4, 3, 300, 12, "Arial", null);
		SingleCellUtility.initializeFACSPlot("Old Method", "New Method", xmin2, xmax2, ymin2, ymax2);
		SingleCellUtility.plotLine(true, false, 0.5, 5, 1, 120, 100, "blue", 1.5, 5, 10, 5, 10);
		SingleCellUtility.drawLogicleAxis(true, 5, 10, "-5,0,5,10,100");
		SingleCellUtility.drawLogicleAxis(false, 2, 10, "-5,0,5,10,100");
		R.eval("box()");
		R.endPlot();
		try
		{
			FileUtility.openFileDefaultApplication(new File(path));
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static double showHistogram(FloatProcessor imp, double histMin, double histMax, int nBins, boolean showHist)
	{

		// Make the histogram
		if(nBins < 0)
		{
			nBins = (int) (histMax - histMin);
		}
		imp.setHistogramRange(histMin, histMax);
		imp.setHistogramSize(nBins);
		FloatStatistics stats = (FloatStatistics) imp.getStatistics();
		double[] bins = new double[nBins];
		for (int i = 0; i < nBins; i++)
		{
			bins[i] = histMin + i * stats.binSize;
		}

		if(showHist)
		{
			// Draw the histogram
			R.makeVector("binCenters", bins);
			R.makeVector("binCounts", stats.histogram);
			String path = "/Users/warrick/Desktop/myTest.pdf";
			R._startPlot(new File(path), 4, 3, 300, 10, null, null);
			R.eval("plot(binCenters,binCounts,cex=0.4)");
			R.endPlot();

			// Open the histogram plot
			try
			{
				FileUtility.openFileDefaultApplication(path);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		int nMax = 0;
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < stats.histogram.length - 1; i++)
		{
			if(stats.histogram[i] > max)
			{
				max = stats.histogram[i];
				nMax = i;
			}
		}
		return bins[nMax];
	}

	public static int getIndexOfMax(double[] values)
	{
		int ret = 0;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < values.length - 1; i++)
		{
			if(values[i] > max)
			{
				max = values[i];
				ret = i;
			}
		}
		return ret;
	}

	public static int getIndexOfMax(int[] values)
	{
		int ret = 0;
		int max = Integer.MIN_VALUE;
		for (int i = 0; i < values.length; i++)
		{
			if(values[i] > max)
			{
				max = values[i];
				ret = i;
			}
		}
		return ret;
	}

	static byte[] readStream(InputStream input) throws IOException
	{
		byte[] buffer = new byte[1024];
		int offset = 0, len = 0;
		for (;;)
		{
			if(offset == buffer.length)
			{
				buffer = realloc(buffer, 2 * buffer.length);
			}
			len = input.read(buffer, offset, buffer.length - offset);
			if(len < 0)
			{
				return realloc(buffer, offset);
			}
			offset += len;
		}
	}

	static byte[] realloc(byte[] buffer, int newLength)
	{
		if(newLength == buffer.length)
		{
			return buffer;
		}
		byte[] newBuffer = new byte[newLength];
		System.arraycopy(buffer, 0, newBuffer, 0, Math.min(newLength, buffer.length));
		return newBuffer;
	}

	// @Override
	// @SuppressWarnings("unchecked")
	// public Class<Object> loadClass(String name)
	// {
	// Class<Object> result = null;
	// try
	// {
	// InputStream input =
	// this.getResourceAsStream(FileUtility.getFileNameWithExtension(name));
	//
	// if (input != null)
	// {
	// byte[] buffer = readStream(input);
	// input.close();
	// result = (Class<Object>) this.defineClass("TestLoadable", buffer, 0,
	// buffer.length);
	// return result;
	// }
	// }
	// catch (Exception e)
	// {
	// e.printStackTrace();
	// }
	//
	// return result;
	// }

}

/**
 * 
 * Simple class to generate empty, randomly filled or constantly filled
 * images of various types.
 * 
 * @author Daniel Seebacher, University of Konstanz.
 * @author Andreas Graumann, University of Konstanz
 */
class ImageGenerator {

	private Random rand;

	/**
	 * Create the image generator with a predefined seed.
	 * 
	 * @param seed
	 *            a seed which is used by the random generator.
	 */
	public ImageGenerator(long seed) {
		this.rand = new Random(seed);
	}

	/**
	 * Default constructor, initialize with random seed.
	 */
	public ImageGenerator() {
		this.rand = new Random();
	}

	/**
	 * 
	 * @param dim
	 *            a long array with the desired dimensions of the image
	 * @return an empty {@link Img} of {@link UnsignedByteType}.
	 */
	public Img<UnsignedByteType> getEmptyUnsignedByteImg(long[] dim) {
		return ArrayImgs.unsignedBytes(dim);
	}

	/**
	 * 
	 * @param dim
	 *            a long array with the desired dimensions of the image
	 * @return an {@link Img} of {@link UnsignedByteType} filled with random
	 *         values.
	 */
	public Img<UnsignedByteType> getRandomUnsignedByteImg(long[] dim) {
		ArrayImg<UnsignedByteType, ByteArray> img = ArrayImgs
				.unsignedBytes(dim);

		UnsignedByteType type = img.firstElement();

		ArrayCursor<UnsignedByteType> cursor = img.cursor();
		while (cursor.hasNext()) {
			cursor.next().set(rand.nextInt((int) type.getMaxValue()));
		}

		return (Img<UnsignedByteType>) img;
	}

	/**
	 * 
	 * @param dim
	 *            a long array with the desired dimensions of the image
	 * @return an {@link Img} of {@link UnsignedByteType} filled with a
	 *         constant value.
	 */
	public Img<UnsignedByteType> getConstantUnsignedByteImg(long[] dim,
			int constant) {
		ArrayImg<UnsignedByteType, ByteArray> img = ArrayImgs
				.unsignedBytes(dim);

		UnsignedByteType type = img.firstElement();
		if (constant < type.getMinValue() || constant >= type.getMaxValue()) {
			throw new IllegalArgumentException(
					"Can't create image for constant [" + constant + "]");
		}

		ArrayCursor<UnsignedByteType> cursor = img.cursor();
		while (cursor.hasNext()) {
			cursor.next().set(constant);
		}

		return (Img<UnsignedByteType>) img;
	}

	/**
	 * 
	 * @param dim
	 * @param radii
	 * @return an {@link Img} of {@link BitType} filled with a ellipse
	 */
	public Img<UnsignedByteType> getEllipsedBitImage(long[] dim,
			double[] radii, double[] offset) {

		// create empty bittype image with desired dimensions
		ArrayImg<UnsignedByteType, ByteArray> img = ArrayImgs
				.unsignedBytes(dim);

		// create ellipse
		EllipseRegionOfInterest ellipse = new EllipseRegionOfInterest();
		ellipse.setRadii(radii);

		// set origin in the center of image
		double[] origin = new double[dim.length];
		for (int i = 0; i < dim.length; i++)
			origin[i] = dim[i] / 2;
		ellipse.setOrigin(origin);

		// get iterable intervall and cursor of ellipse
		IterableInterval<UnsignedByteType> ii = ellipse
				.getIterableIntervalOverROI(img);
		Cursor<UnsignedByteType> cursor = ii.cursor();

		// fill image with ellipse
		while (cursor.hasNext()) {
			cursor.next();
			cursor.get().set(255);
		}

		return (Img<UnsignedByteType>) img;
	}
}
