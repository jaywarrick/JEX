//
//  TrackAnalyser.java
//  MicroFluidicHT_Tools
//
//  Created by erwin berthier on 8/12/08.
//  Copyright 2008 __MyCompanyName__. All rights reserved.
//
package function.tracker;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import logs.Logs;
import miscellaneous.VectorUtility;

public class HistogramFactory {
	
	public static String CART_LINE = "Cartesian Line";
	public static String CART_SCATTER = "Cartesian Scatter";
	public static String CART_HISTOGRAM = "Cartesian Histogram";
	public static String POLAR_LINE = "Polar Line";
	public static String POLAR_SCATTER = "Polar Scatter";
	public static String POLAR_HISTOGRAM = "Polar Histogram";
	
	public double min = 0.0;
	public double maxX = -1;
	public double maxY = -1;
	public int numberOfBins = 8;
	public Color color = Color.red;
	public String coord = CART_HISTOGRAM;
	
	public String title = "";
	public String xLabel = "";
	public String yLabel = "";
	public List<Double> series;
	public List<Double> absc;
	public List<Double> hvalues;
	
	public HistogramFactory()
	{}
	
	public BufferedImage makeCartesianHistogram(List<Double> series)
	{
		this.series = series;
		
		if(this.maxX < 0)
		{
			this.maxX = 1.05 * VectorUtility.getMaximum(series);
		}
		
		this.coord = CART_HISTOGRAM;
		this.absc = makeAbscHistorgram(this.numberOfBins, this.min, this.maxX);
		this.hvalues = getLinearHistogram(series, this.absc);
		
		BufferedImage im = this.makeFigure();
		return im;
	}
	
	public BufferedImage makePolarHistogram(List<Double> series)
	{
		this.series = series;
		
		if(this.maxX < 0)
		{
			this.maxX = 1.05 * VectorUtility.getMaximum(series);
			;
		}
		
		this.coord = POLAR_HISTOGRAM;
		this.absc = makeAbscHistorgram(this.numberOfBins, 0, 360);
		this.hvalues = getIncreasingPolarHistogram(series, this.absc);
		
		BufferedImage im = this.makeFigure();
		return im;
	}
	
	public BufferedImage makeWeighedPolarHistogram(List<Double> series, List<Double> weights)
	{
		this.series = series;
		
		if(this.maxX < 0)
		{
			this.maxX = 1.05 * VectorUtility.getMaximum(series);
		}
		
		this.coord = POLAR_HISTOGRAM;
		this.absc = makeAbscHistorgram(this.numberOfBins, 0, 360);
		this.hvalues = getIncreasingPolarWeighedHistogram(series, weights, this.absc);
		
		BufferedImage im = this.makeFigure();
		return im;
	}
	
	/**
	 * Create the figure object from the graph
	 * 
	 * @return bfigure image
	 */
	public BufferedImage makeFigure()
	{
		FigureFactory figfact = new FigureFactory();
		figfact.color = this.color;
		figfact.min = this.min;
		figfact.maxX = this.maxX;
		figfact.maxY = this.maxY;
		figfact.type = this.coord;
		figfact.title = this.title;
		figfact.xLabel = this.xLabel;
		figfact.yLabel = this.yLabel;
		figfact.xseries = this.absc;
		figfact.yseries = this.hvalues;
		
		BufferedImage im = figfact.createBufferedImage();
		
		// ImageUtility.saveFigure(im, path);
		
		return im;
	}
	
	// --------------------------
	// -- Calculation functions
	// --------------------------
	
	public static List<Double> makeAbscHistorgram(int length, double min, double max)
	{
		Vector<Double> result = new Vector<Double>(0);
		
		// create an equal spacing vector
		for (int i = 0; i <= length; i++)
		{
			double Ai = min + i * (max - min) / (length);
			result.add(Ai);
		}
		
		return result;
	}
	
	public static List<Double> getLinearHistogram(List<Double> vect, List<Double> absc)
	{
		int length = absc.size();
		Double[] result = zerosArray(length);
		// Double[] lenArray = zerosArray(length);
		
		for (int i = 0, len = vect.size(); i < len; i++)
		{
			double Vi = vect.get(i);
			int index2place = findIndexHistogram(Vi, absc);
			if(index2place == -1)
			{
				continue;
			}
			result[index2place] = result[index2place] + 1;
		}
		
		List<Double> resultVect = array2Vect(result);
		return resultVect;
	}
	
	/**
	 * Distribute a vector of doubles VECT into bins determined by vector ABSC Each double can go at location i if it is higher of equal to element i of ABSC AND strictly lower to element i+1 All elements larger than the last element of ABSC are
	 * placed in bin ABSC.size()-1
	 * 
	 * @param vect
	 * @param absc
	 * @return
	 */
	public static List<Double> getIncreasingHistogram(List<Double> vect, List<Double> absc)
	{
		int length = absc.size();
		Double[] result = zerosArray(length);
		
		// Get the next value to consider
		for (int i = 0, len = vect.size(); i < len; i++)
		{
			double Vi = vect.get(i);
			
			// Determine the right bin to place it
			for (int j = 0, lenj = absc.size(); j < lenj - 1; j++)
			{
				double Aj1 = absc.get(j);
				double Aj2 = absc.get(j + 1);
				if(Vi >= Aj1 && Vi < Aj2)
				{
					result[j] = result[j] + 1;
					break;
				}
				else if(j == lenj - 2 && Vi >= Aj2)
				{
					result[j + 1] = result[j + 1] + 1;
				}
			}
			
		}
		
		List<Double> resultVect = array2Vect(result);
		return resultVect;
	}
	
	/**
	 * Distribute a vector of doubles VECT into bins determined by vector ABSC Each double can go at location i if it is higher of equal to element i of ABSC AND strictly lower to element i+1 All elements larger than the last element of ABSC are
	 * placed in bin ABSC.size()-1
	 * 
	 * @param vect
	 * @param absc
	 * @return
	 */
	public static List<Double> getIncreasingPolarHistogram(List<Double> vect, List<Double> absc)
	{
		int length = absc.size();
		Double[] result = zerosArray(length);
		
		// Get the next value to consider
		for (int i = 0, len = vect.size(); i < len; i++)
		{
			double Vi = vect.get(i);
			
			// Determine the right bin to place it
			for (int j = 0, lenj = absc.size(); j < lenj - 1; j++)
			{
				double Aj1 = absc.get(j);
				double Aj2 = absc.get(j + 1);
				if(Vi >= Aj1 && Vi < Aj2)
				{
					result[j] = result[j] + 1;
					break;
				}
			}
		}
		result[absc.size() - 1] = result[0];
		
		List<Double> resultVect = array2Vect(result);
		return resultVect;
	}
	
	/**
	 * Distribute a vector of doubles VECT into bins determined by vector ABSC with the weight WEIGHTS Each double can go at location i if it is higher of equal to element i of ABSC AND strictly lower to element i+1 All elements larger than the last
	 * element of ABSC are placed in bin ABSC.size()-1
	 * 
	 * @param vect
	 * @param weighs
	 * @param bins
	 * @return
	 */
	public static List<Double> getIncreasingPolarWeighedHistogram(List<Double> vect, List<Double> weights, List<Double> bins)
	{
		int length = bins.size();
		Double[] result = zerosArray(length);
		
		// Get the next value to consider
		for (int i = 0, len = vect.size(); i < len; i++)
		{
			double Vi = vect.get(i);
			double Wi = weights.get(i);
			
			// Determine the right bin to place it
			for (int j = 0, lenj = bins.size(); j < lenj - 1; j++)
			{
				double Aj1 = bins.get(j);
				double Aj2 = bins.get(j + 1);
				if(Vi >= Aj1 && Vi < Aj2)
				{
					result[j] = result[j] + Wi;
					break;
				}
			}
		}
		result[bins.size() - 1] = result[0];
		
		List<Double> resultVect = array2Vect(result);
		return resultVect;
	}
	
	public static int findIndexHistogram(Double d, List<Double> absc)
	{
		for (int i = 0, len = absc.size(); i < len - 1; i++)
		{
			Double v1 = absc.get(i);
			Double v2 = absc.get(i + 1);
			if((d >= v1) && (d < v2))
			{
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Return an array of zeros of length LENGTH
	 * 
	 * @param length
	 * @return
	 */
	public static Double[] zerosArray(int length)
	{
		Double[] result = new Double[length];
		for (int i = 0; i < length; i++)
		{
			result[i] = new Double(0);
		}
		return result;
	}
	
	/**
	 * Transform an array of doubles to a Vector
	 * 
	 * @param array
	 * @return
	 */
	public static List<Double> array2Vect(Double[] array)
	{
		Vector<Double> result = new Vector<Double>(0);
		for (int i = 0, len = array.length; i < len; i++)
		{
			result.add(array[i]);
		}
		return result;
	}
	
	/**
	 * Normalize a degrees angle between 0 (included) and 360 (excluded)
	 * 
	 * @param a
	 * @return
	 */
	public static double normalizeAngle(double a)
	{
		double result = a;
		while (result < 0 || result >= 360)
		{
			if(result < 0)
			{
				result = result + 360;
			}
			if(result >= 360)
			{
				result = result - 360;
			}
		}
		return result;
	}
	
	// --------------------------
	// -- DB operations : create figures / values
	// --------------------------
	/**
	 * Output a text file to load 2 X and Y vectors in a different plotting program
	 */
	public void printToFile(Vector<Double> velocityHistAbsc, Vector<Double> velocityHist, String path)
	{
		try
		{
			if(velocityHistAbsc.size() != velocityHist.size())
			{
				System.out.println("Two vectors are uneven");
				return;
			}
			FileWriter fileWriter = new FileWriter(path);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			for (int i = 0, len = velocityHistAbsc.size(); i < len; i++)
			{
				bufferedWriter.write(velocityHistAbsc.get(i) + "\t" + velocityHist.get(i));
				bufferedWriter.newLine();
			}
			
			Logs.log("File written to " + path, HistogramFactory.class);
			bufferedWriter.close();
		}
		catch (IOException e)
		{
			System.out.println("Cannot write file");
			return;
		}
	}
	
	// /** Find the top limit
	// * @param v
	// * @param percentile
	// * @return
	// */
	// public static double topLimit(List<Double> v){
	// List<Double> sorted = VectorUtility.sort(v);
	// double result = VectorUtility.mean(sorted, sorted.size()-3,
	// sorted.size());
	// return result;
	// }
}
