package function.plugin.plugins.calibration;

import java.util.List;
import java.util.Vector;

import Jama.Matrix;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import jex.statics.JEXDialog;
import tables.Dim;
import tables.DimensionMap;
import tables.Table;

public class SpectralUnmixingUtility {
	
	public static double[][] getInverseMatrix(Table<Double> matrixData, String channelDimName, String fluorDimName)
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
	
	public static Vector<FloatProcessor> getFluorSignals(List<ImageProcessor> channelSignals, double[][] imMatrix)
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

}
