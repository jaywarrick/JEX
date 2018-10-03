package function.plugin.plugins.imageProcessing;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import function.algorithm.neighborhood.EdgeCursor;
import function.algorithm.neighborhood.PositionableRunningNeighborhood;
import function.algorithm.neighborhood.SnakingCursor;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import function.plugin.plugins.featureExtraction.FeatureUtils;
import function.plugin.plugins.medianFilterHelpers.RedBlackTreeFloat;
import ij.ImagePlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.CSVList;
import miscellaneous.StatisticsUtility;
import net.imglib2.Cursor;
import net.imglib2.Point;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessible;
import net.imglib2.algorithm.neighborhood.Neighborhood;
import net.imglib2.algorithm.neighborhood.RectangleShape;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;
import net.imglib2.view.Views;
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
		name="Super Fast Median Filter",
		menuPath="Image Processing",
		visible=true,
		description="Fast median filter that uses a square shaped kernel and random sampling of the region to reduce calc time."
		)
public class SuperFastMedianFilter extends JEXPlugin {

	public SuperFastMedianFilter()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=0, name="Operation", description="Which running math operation should be performed", ui=MarkerConstants.UI_DROPDOWN, choices={"Median", "Mean"}, defaultChoice=0)
	String operation;

	@ParameterMarker(uiOrder=1, name="Kernal Shape", description="What shape should the kernel be?", ui=MarkerConstants.UI_DROPDOWN, choices={"Rectangle", "Ellipse"}, defaultChoice=1)
	String shape;
	
	@ParameterMarker(uiOrder=2, name="Kernal Width", description="Pixel width of the kernel. If < 100, then probably should use 'Fast Median Background Subtraction/Filter' instead.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100.0")
	double kernelWidth;
	
	@ParameterMarker(uiOrder=3, name="Kernal Height", description="Pixel height of the kernel. If < 100, then probably should use 'Fast Median Background Subtraction/Filter' instead.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100.0")
	double kernelHeight;

	@ParameterMarker(uiOrder=4, name="Kernel Sample Size", description="Size of the sample within the kernel for calculating the median.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	int n;
	
	@ParameterMarker(uiOrder=5, name="Kernel Sampling Method", description="How should the spacing of the samples be chosen?", ui=MarkerConstants.UI_DROPDOWN, choices={"Random", "Hexagonal Spacing"}, defaultChoice=1)
	String samplingMethod;

	@ParameterMarker(uiOrder=6, name="Regenerate Sampling Locations for Each Image?", description="If checked, this will regenerate random locations for each image, incurring time lost to generate random numbers and the backing image associated with it.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean resample;

	@ParameterMarker(uiOrder=7, name="Perform Subtraction?", description="If checked, this will return the original image minus the median filtered result instead of just the median filtered result.", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=true)
	boolean performSubtraction;

	@ParameterMarker(uiOrder=8, name="Nominal Value to Add Back", description="Nominal value to add to all pixels after background subtraction because some image formats don't allow negative numbers. (Use following notation to specify different parameters for differen dimension values, '<Dim Name>'=<val1>,<val2>,<val3>' e.g., 'Channel=0,100,100'. The values will be applied in that order for the ordered dim values.) ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="100")
	String nominal;

	double nominalVal;

	@ParameterMarker(uiOrder=9, name="Output Bit Depth", description="What bit depth should the output be saved as.", ui=MarkerConstants.UI_DROPDOWN, choices={"8","16","32"}, defaultChoice=1)
	int outputBitDepth;
	
	@ParameterMarker(uiOrder=10, name="Exclusion Filter DimTable", description="Exclude combinatoins of Dimension Names and values. (Use following notation '<DimName1>=<a1,a2,...>;<DimName2>=<b1,b2,...>' e.g., 'Channel=0,100,100; Time=1,2,3,4,5' (spaces are ok).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String exclusionFilterString;
	
	@ParameterMarker(uiOrder=11, name="Keep Excluded Images?", description="Should images excluded by the filter be copied to the new object?", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean = true)
	boolean keepExcluded;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Adjusted Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant adjusted image", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	FeatureUtils utils = new FeatureUtils();
	Img<UnsignedByteType> samplingImg = null;
	PositionableRunningNeighborhood<UnsignedByteType> p = null;
	SnakingCursor<UnsignedByteType> sc = null;
	int op = 0;

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		this.samplingImg = null;
		
		if(this.operation.equals("Mean"))
		{
			this.op = 1;
		}
		
		// Validate the input data
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			return false;
		}
		
		DimTable filterTable = new DimTable(this.exclusionFilterString);

		// Get the nominal add back values (typically different for fluorescence and brightfield
		TreeMap<DimensionMap,Double> nominals = getNominals();
		if(nominals == null)
		{
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		TreeMap<DimensionMap,String> outputImageMap = new TreeMap<DimensionMap,String>();
		int count = 0, percentage = 0;
		String tempPath;

		for (DimensionMap map : imageMap.keySet())
		{
			if(this.isCanceled())
			{
				Logs.log("Function canceled.", this);
				return false;
			}
			
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
				}
				else
				{
					Logs.log("Skipping the processing and saving of " + map.toString(), this);
				}
				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
				continue;
			}
			
			// Call helper method
			this.nominalVal = nominals.get(map);
			
			Shape s = null;
			if(this.shape.equals("Rectangle"))
			{
				s = new Rectangle2D.Double(0,0,this.kernelWidth, this.kernelHeight);
			}
			else
			{
				s = new Ellipse2D.Double(0,  0, this.kernelWidth, this.kernelHeight);
			}
			ImagePlus out = this.getMedianBackground2(imageMap.get(map), s, this.performSubtraction, outputBitDepth);
			if(out == null)
			{
				return false;
			}
			tempPath = JEXWriter.saveImage(out);
			if(tempPath != null)
			{
				outputImageMap.put(map, tempPath);
			}
			count = count + 1;
			percentage = (int) (100 * ((double) (count) / ((double) imageMap.size())));
			JEXStatics.statusBar.setProgressPercentage(percentage);
		}
		if(outputImageMap.size() == 0)
		{
			return false;
		}

		this.output = ImageWriter.makeImageStackFromPaths("temp",outputImageMap);

		// Return status
		return true;
	}

	public TreeMap<DimensionMap,Double> getNominals()
	{
		TreeMap<DimensionMap, Double> ret = new TreeMap<>();
		Dim nominalDim = null;
		Double nominalD = 0.0;
		try
		{
			nominalD = Double.parseDouble(nominal);
			ret.put(new DimensionMap(), nominalD);
			return ret;
		}
		catch(Exception e)
		{
			String[] temp = nominal.split("=");
			if(temp.length != 2)
			{
				JEXDialog.messageDialog("Couldn't parse the parameter '" + nominal + "' into a number or into format '<Dim Name>=<val1>,<val2>...'. Aborting.", this);
				return null;
			}
			nominalDim = imageData.getDimTable().getDimWithName(temp[0]);
			CSVList temp2 = new CSVList(temp[1]);
			if(nominalDim.size() != 1 && nominalDim.size() != temp2.size())
			{
				JEXDialog.messageDialog("The number of nominal addback values did not match the number of values in the dimension '" + nominalDim.dimName + "'. Aborting.", this);
				return null;
			}
			for(int i=0; i < temp2.size(); i++)
			{
				try
				{
					Double toGet = Double.parseDouble(temp2.get(i));
					ret.put(new DimensionMap(nominalDim.dimName + "=" + nominalDim.dimValues.get(i)), toGet);
				}
				catch(Exception e2)
				{
					JEXDialog.messageDialog("Couldn't parse the value '" + temp2.get(i) + "' to type double. Aborting.", this);
					return null;
				}

			}
			return ret;
		}
	}

	/**
	 * Based upon: http://imagej.net/ImgLib2_Examples#Example_3a_-_Min.2FMax_search example 4b
	 * @param source
	 * @return
	 */
	public < T extends RealType< T >> ImagePlus getMedianBackground2(String imagePath, Shape s, boolean performSubtraction, int outputBitDepth)
	{

		Img<T> source = ImageJFunctions.wrapReal(new ImagePlus(imagePath));
		if(this.samplingImg == null || this.resample)
		{
			this.samplingImg = this.utils.makeImageFromInterval(source, new UnsignedByteType(0));
			this.p = new PositionableRunningNeighborhood<>(s, this.samplingImg, PositionableRunningNeighborhood.MIRROR);
			
			// Add speckles to aid random sampling of the source image.
			double imgN = source.size();
			double kN = p.getKernelSize();
			double alpha = imgN/kN;
			if(this.samplingMethod.equals("Random"))
			{
				this.utils.addRandomSpeckles(this.samplingImg, Math.round(this.n*alpha), new UnsignedByteType(255));
			}
			else
			{
				this.utils.addHexPackedSpeckles(this.samplingImg, Math.round(this.n*alpha), new UnsignedByteType(255));
			}
			this.sc = new SnakingCursor<>(this.samplingImg);
			//this.utils.show(this.samplingImg, true);
		}
		Img< FloatType > ret = this.utils.makeImageFromInterval(source, new FloatType(0.0f));
		RandomAccess<FloatType> toSet = ret.randomAccess();
		RandomAccess<T> toGet = Views.extendMirrorSingle(source).randomAccess();


		long count = 0;
		int percentage = 0;
		int oldPercentage = -1;
		this.sc.reset();
		this.p.reset();
		RedBlackTreeFloat rbt = new RedBlackTreeFloat();
		float tot = 0;
		float size = 0;
		while(sc.hasNext())
		{
			sc.fwd();
			p.setPosition(sc); // this causes a reset of the neighborhood cursor.
			toSet.setPosition(sc);
			ValuePair<EdgeCursor<UnsignedByteType>, EdgeCursor<UnsignedByteType>> edges = p.getEdgeCursors();
			if(edges == null)
			{
				tot = 0;
				size = 0;
				rbt = new RedBlackTreeFloat();
				while(p.hasNext())
				{
					p.fwd();
					toGet.setPosition(p.getSamplerLoc());
					if(p.get().get() != 0)
					{
						if(op == 0)
						{
							//System.out.println("Inserting: " + toGet.get().getRealFloat() + " X: " + toGet.getFloatPosition(0) + "," + p.getSamplerLoc().getFloatPosition(0) + " Y: " + toGet.getFloatPosition(1) + "," + p.getSamplerLoc().getFloatPosition(1));
							rbt.insert(toGet.get().getRealFloat());
						}
						else
						{
							tot = tot + toGet.get().getRealFloat();
							size = size + 1;
						}
					}
				}
			}
			else
			{
				// Add new values
				while(edges.a.hasNext())
				{
					edges.a.fwd();
					if(edges.a.get().get() != 0)
					{
						toGet.setPosition(edges.a.getSamplerLoc());
						if(op == 0)
						{
							//System.out.println("Inserting: " + toGet.get().getRealFloat() + " X: " + toGet.getFloatPosition(0) + "," + edges.a.getSamplerLoc().getFloatPosition(0) + " Y: " + toGet.getFloatPosition(1) + "," + edges.a.getSamplerLoc().getFloatPosition(1));
							rbt.insert(toGet.get().getRealFloat());
						}
						else
						{
							tot = tot + toGet.get().getRealFloat();
							size = size + 1;
						}
					}
				}
				// Subtract old values
				while(edges.b.hasNext())
				{
					edges.b.fwd();
					if(edges.b.get().get() != 0)
					{
						toGet.setPosition(edges.b.getSamplerLoc());
						if(op == 0)
						{
							//System.out.println("Removing: " + toGet.get().getRealFloat() + " X: " + toGet.getFloatPosition(0) + "," + edges.b.getSamplerLoc().getFloatPosition(0) + " Y: " + toGet.getFloatPosition(1) + "," + edges.b.getSamplerLoc().getFloatPosition(1));
							rbt.remove(toGet.get().getRealFloat());
						}
						else
						{
							tot = tot - toGet.get().getRealFloat();
							size = size - 1;
						}
					}
				}
			}
			if(this.op == 0)
			{
				//System.out.println("==== Finding Median ====");
				//rbt.printTree();
				final int medianRank = rbt.size() / 2 + 1;
				toSet.get().set(rbt.select(medianRank));
				//System.out.println("Found Median: " + rbt.select(medianRank));
				//System.out.println("========================\n\n\n");
				//toSet.get().set(rbt.size());
			}
			else
			{
				toSet.get().set(tot/size);
			}

			count = count + 1;
			percentage = (int) (100 * ((double) count) / ((double) source.size()));
			if(oldPercentage != percentage)
			{
				JEXStatics.statusBar.setProgressPercentage(percentage);
				if(this.isCanceled())
				{
					Logs.log("Function canceled.", this);
					return null;
				}
				oldPercentage = percentage;
			}
		}

		if(performSubtraction)
		{
			// create a Cursor that iterates over the source along with the neighborhoods.
			// (the center cursor runs over the image in the same iteration order as neighborhood)
			final Cursor< T > srcC = source.cursor();
			RandomAccess< T > srcRA = source.randomAccess();
			srcRA.setPosition(new int[]{0,0});
//			T testVal = srcRA.get();
//			boolean truncate = !(testVal instanceof FloatType);
			while(srcC.hasNext())
			{
				srcC.fwd();
				toSet.setPosition(srcC);
				double val = srcC.get().getRealDouble() - toSet.get().getRealDouble() + this.nominalVal;
				toSet.get().setReal(val);
//				if(truncate && val < 0)
//				{
//					toSet.get().setReal(0.0d);
//				}
//				else
//				{
//					toSet.get().setReal(val);
//				}
			}
		}

		ImagePlus ret2 = null;
		if(outputBitDepth == 8)
		{
			ret2 = ImageJFunctions.wrapUnsignedByte(ret, "Background Subtracted");
		}
		else if(outputBitDepth == 16)
		{
			ret2 = ImageJFunctions.wrapUnsignedShort(ret, "Background Subtracted");
		}
		else
		{
			ret2 = ImageJFunctions.wrapFloat(ret, "Background Subtracted");
		}

		return ret2;
	}

	/**
	 * Based upon: http://imagej.net/ImgLib2_Examples#Example_3a_-_Min.2FMax_search example 4b
	 * @param source
	 * @return
	 */
	public < T extends RealType< T >> ImagePlus getMedianBackground(String imagePath, int width, int sampleSize, boolean performSubtraction, int outputBitDepth, boolean resample)
	{
		Img<T> source = ImageJFunctions.wrapReal(new ImagePlus(imagePath));
		ArrayImgFactory<FloatType> factory = new ArrayImgFactory<>();
		Img< FloatType > ret = factory.create(source, new FloatType());

		RandomAccess<FloatType> toSet = ret.randomAccess();

		// create an infinite view where all values outside of the Interval are
		// the mirrored content, the mirror is the last pixel
		RandomAccessible< T > sourceExt = Views.extendMirrorSingle( source );

		// instantiate a RectangleShape to access rectangular local neighborhoods
		// of radius 1 (that is 3x3x...x3 neighborhoods), skipping the center pixel
		// (this corresponds to an 8-neighborhood in 2d or 26-neighborhood in 3d, ...)
		final RectangleShape shape = new RectangleShape( (int) (width-1)/2, false );
		// Look into using HyperSphereShape and neighborhoods

		// iterate over the set of neighborhoods in the image
		Vector<Point> pointsToSample = null;
		long count = 0;
		int percentage = 0;
		int oldPercentage = -1;
		for ( final Neighborhood< T > localNeighborhood : shape.neighborhoods( source ) )
		{
			toSet.setPosition(localNeighborhood);
			double median = 0;
			if(!resample)
			{
				if(pointsToSample == null)
				{
					pointsToSample = StatisticsUtility.generateRandomPointsInRectangularRegion(localNeighborhood, sampleSize);
				}
				double[] vals = StatisticsUtility.samplePoints(sourceExt, pointsToSample, localNeighborhood);
				median = StatisticsUtility.median(vals);
			}
			else
			{
				double[] vals = StatisticsUtility.sampleRandomPoints(sourceExt, localNeighborhood, sampleSize);
				median = StatisticsUtility.median(vals);
			}
			toSet.get().setReal(median);

			count = count + 1;
			percentage = (int) (100 * ((double) count) / ((double) source.size()));
			if(oldPercentage != percentage)
			{
				JEXStatics.statusBar.setProgressPercentage(percentage);
				if(this.isCanceled())
				{
					Logs.log("Function canceled.", this);
					return null;
				}
				oldPercentage = percentage;
			}
		}

		if(performSubtraction)
		{
			// create a Cursor that iterates over the source along with the neighborhoods.
			// (the center cursor runs over the image in the same iteration order as neighborhood)
			final Cursor< T > srcC = Views.iterable( source ).cursor();
			RandomAccess< T > srcRA = source.randomAccess();
			while(srcC.hasNext())
			{
				srcC.fwd();
				toSet.setPosition(srcC);
				srcRA.setPosition(srcC);
				toSet.get().setReal(srcRA.get().getRealDouble() - toSet.get().getRealDouble() + this.nominalVal);
			}
		}

		ImagePlus ret2 = null;
		if(outputBitDepth == 8)
		{
			ret2 = ImageJFunctions.wrapUnsignedByte(ret, "Background Subtracted");
		}
		else if(outputBitDepth == 16)
		{
			ret2 = ImageJFunctions.wrapUnsignedShort(ret, "Background Subtracted");
		}
		else
		{
			ret2 = ImageJFunctions.wrapFloat(ret, "Background Subtracted");
		}

		return ret2;
	}
}
