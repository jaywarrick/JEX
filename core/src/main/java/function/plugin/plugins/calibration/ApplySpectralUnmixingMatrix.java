package function.plugin.plugins.calibration;

import ij.ImagePlus;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.TreeMap;
import java.util.Vector;

import jex.statics.JEXDialog;

import org.scijava.plugin.Plugin;

import tables.Dim;
import tables.DimTable;
import tables.DimensionMap;
import tables.Table;
import weka.core.converters.JEXTableReader;
import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.FileReader;
import Database.DataReader.ImageReader;
import Database.DataWriter.ImageWriter;
import Database.SingleUserDatabase.JEXWriter;
import Jama.Matrix;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;

@Plugin(
		type = JEXPlugin.class,
		name="Apply Spectral Unmixing Matrix",
		menuPath="Calibration",
		visible=true,
		description="Treat an image object as a stack, calculate the median/mean of subgroups of images, take the mean of the subgroups, and smooth if desired. Result of the first successful entry is copied to all selected entries if desired."
		)
public class ApplySpectralUnmixingMatrix extends JEXPlugin {
	
	public static ImagePlus sharedCalibrationImage = null;
	
	public ApplySpectralUnmixingMatrix()
	{}
	
	/**
	 * Returns true if the user wants to allow multithreding
	 * 
	 * @return
	 */
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to use to generate calibration image.", optional=true)
	JEXData imageData;
	
	@InputMarker(uiOrder=2, name="Matrix", type=MarkerConstants.TYPE_FILE, description="Spectral unmixing matrix,", optional=true)
	JEXData matrixData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=1, name="Channel Dim Name", description="After unmixing, how much to scale the result", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Channel")
	String channelDimName;
	
	@ParameterMarker(uiOrder=2, name="Fluor Dim Name", description="After unmixing, how much to scale the result", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Fluor")
	String fluorDimName;
	
	@ParameterMarker(uiOrder=3, name="Intensity Scaling Factor", description="After unmixing, how much to scale the result", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double scalingFactor;
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Unmixed Image", type=MarkerConstants.TYPE_FILE, flavor="", description="The result of unmixing the channels. The resulting intensities (with a scaling factor of 1) represent intensities relative to the calibration images used to create the unmixing matrix and do not account for changes in exposure times etc.", enabled=true)
	JEXData outputImage;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}
	
	@Override
	public boolean run(JEXEntry entry)
	{
		
		// Check the inputs
		if(imageData == null || !imageData.getTypeName().getType().equals(JEXData.IMAGE))
		{
			return false;
		}
		
		if(matrixData == null || !matrixData.getTypeName().getType().equals(JEXData.FILE))
		{
			return false;
		}
		
		// Get the data
		TreeMap<DimensionMap,String> imageMap = ImageReader.readObjectToImagePathTable(imageData);
		
		// Prepare a data structure to store the results
		TreeMap<DimensionMap,String> outputImages = new TreeMap<DimensionMap,String>();
		
		// Get the inverse of the mixing matrix (i.e., the unmixing matrix)
		Table<Double> matrix = JEXTableReader.getNumericTable(FileReader.readFileObject(matrixData));
		double[][] imMatrix = getInverseMatrix(matrix, channelDimName, fluorDimName);
		
		// Prepare a table that doesn't include channel for looping
		DimTable table = imageData.getDimTable();
		DimTable channellessTable = table.getSubTable(channelDimName);
		
		// For each combination of the other dimensions in this image object
		for(DimensionMap filter : channellessTable.getMapIterator())
		{
			// Gather the image processors for each of the channels
			Vector<ImageProcessor> data = new Vector<ImageProcessor>();
			for(DimensionMap map : table.getMapIterator(filter))
			{
				data.add((new ImagePlus(imageMap.get(map)).getProcessor()));				
			}
			
			// Convert the channels signals into fluor signals.
			Vector<FloatProcessor> ret = getFluorSignals(data, imMatrix);
			
			// Save the results
			int i = 0;
			for(DimensionMap map : table.getMapIterator(filter))
			{
				String toSave = JEXWriter.saveImage(ret.get(i));
				outputImages.put(map, toSave);	
				i = i + 1;
			}
		}
	
		// Write the files and save them
		outputImage = ImageWriter.makeImageStackFromPaths("temp", outputImages);
		
		return true;
	}
	
	public double[][] getInverseMatrix(Table<Double> matrixData, String channelDimName, String fluorDimName)
	{
		Dim channelDim = matrixData.dimTable.getDimWithName(channelDimName);
		Dim fluorDim = matrixData.dimTable.getDimWithName(fluorDimName);
		double[][] mMatrix = new double[channelDim.size()][fluorDim.size()];
		double[][] imMatrix = new double[fluorDim.size()][channelDim.size()];
		for(DimensionMap map : matrixData.dimTable.getMapIterator())
		{
			int c = channelDim.index(map.get(channelDimName));
			int f = channelDim.index(map.get(fluorDimName));
			mMatrix[c][f] = matrixData.getData(map);
		}
		Matrix mixingMatrix = new Matrix(mMatrix);
		imMatrix = mixingMatrix.inverse().getArray();
		
		if (mixingMatrix.rank() < fluorDim.size())
		{
			JEXDialog.messageDialog("Warning!!! - The rank of the matrix appears insufficient to use to unmix the channels");
		}
		
		return imMatrix;
	}
	
	public Vector<FloatProcessor> getFluorSignals(Vector<ImageProcessor> channelSignals, double[][] imMatrix)
	{
		// Dimensions of imMatrix should be double[fluors][channels] given the mMatrix should have been double[channels][fluors].
		
		int width = channelSignals.get(0).getWidth();
		int height = channelSignals.get(0).getHeight();
		int channels = imMatrix.length;
		int fluors = imMatrix[0].length;
		
		// Initialize the return object
		Vector<FloatProcessor> ret = new Vector<FloatProcessor>();
		for(int i = 0; i < fluors; i++)
		{
			ret.add(new FloatProcessor(width, height));
		}
		
		// Loop through pixel locations and convert channel signals to fluor signals.
		for(int x=0; x<width; x++)
		{
			for(int y=0; y<height; y++)
			{
				// Assume the image is already background subtracted
				// Apply the unmixing matrix
				for(int f=0; f<fluors; f++)
				{
					double value = 0;
					for(int c=0; c<channels; c++)
					{
						value = value + imMatrix[f][c] * channelSignals.get(c).getPixelValue(x, y);
					}
					ret.get(f).putPixelValue(x, y, value);
				}
			}
		}
		
		return ret;
	}
	
//	public FloatProcessor getThreshMean(FloatProcessor fp, ROIPlus roi)
//	{
//		AutoThresholder at = new AutoThresholder();
//		
//		int width = fp.getWidth();
//		int height = fp.getHeight();
//		for(int x=0; x<width; x++)
//		{
//			for(int y=0; y<height; y++)
//			{
//				// Assume the image is already background subtracted	
//				// Apply the unmixing matrix
//				for(f=0; f<nFluors; f++)
//				{
//					value  = fInverseMatrix[f][0] * chPixels[0];
//					for(c=1; c<nChannels; ++c)
//					{
//						value += fInverseMatrix[f][c] * chPixels[c];
//					}
//					fluoIPVector[f].putPixelValue(x, y, value);
//				}
//			}
//		}
//		// Do threshold
//		FloatProcessor temp = (FloatProcessor) fp.convertToFloat();
//		FunctionUtility.imAdjust(temp, fp.getMin(), fp.getMax(), 0d, 255d, 1d);
//		ByteProcessor bp = (ByteProcessor) temp.convertToByte(false);
//		int[] hist = bp.getHistogram();
//		double threshold = at.getThreshold(AutoThresholder.OTSU, hist);
//		if(threshold > 255)
//		{
//			threshold = 255;
//		}
//		else if(threshold < 0)
//		{
//			threshold = 0;
//		}
//		bp.threshold((int) threshold);
//		Rectangle r = null;
//		Roi ijRoi = null;
//		if(roi != null)
//		{
//			ijRoi = roi.getRoi();
//			r = roi.pointList.getBounds();
//		}
//		else
//		{
//			r = new Rectangle(bp.getWidth(), bp.getHeight());
//		}
//		
//		Vector<Double> measurements = new Vector<Double>();
//		for (int i = r.x; i < r.x + r.width; i++)
//		{
//			for (int j = r.y; j < r.y + r.height; j++)
//			{
//				if((bp.getPixelValue(i, j) > threshold) && (roi != null && ijRoi.contains(i,j)))
//				{
//					measurements.add((double) bp.getPixelValue(i, j));
//				}
//			}	
//		}
//		
//		if(measurements.size() > 0)
//		{
//			return StatisticsUtility.mean(measurements);
//		}
//		else
//		{
//			return -1.0;
//		}
//	}
}
