package Exporter;

import Database.DBObjects.JEXData;
import Database.DataReader.ValueReader;
import ij.ImagePlus;

import java.awt.Color;
import java.awt.Image;

import javax.swing.JFrame;

/**
 * <p>
 * This class is a very simple example of how to use the HeatMap class.
 * </p>
 * 
 * <hr />
 * <p>
 * <strong>Copyright:</strong> Copyright (c) 2007, 2008
 * </p>
 * 
 * <p>
 * HeatMap is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.
 * </p>
 * 
 * <p>
 * HeatMap is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * </p>
 * 
 * <p>
 * You should have received a copy of the GNU General Public License along with HeatMap; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 * </p>
 * 
 * @author Matthew Beckler (matthew@mbeckler.org)
 * @author Josh Hayes-Sheen (grey@grevian.org), Converted to use BufferedImage.
 * @author J. Keller (jpaulkeller@gmail.com), Added transparency (alpha) support, data ordering bug fix.
 * @version 1.6
 */

public class HeatMapFrame extends JFrame {
	
	private static final long serialVersionUID = 1L;
	HeatMap panel;
	
	// Range for a heatmap
	public Double minRange = Double.NaN;
	public Double meanRange = Double.NaN;
	public Double maxRange = Double.NaN;
	public boolean percent = false;
	
	public HeatMapFrame(JEXData[][] xos) throws Exception
	{
		super("Heat Map Frame");
		
		// Get the value field
		Double[][] data = new Double[xos[0].length][xos.length];
		for (int i = 0, leni = xos[0].length; i < leni; i++)
		{
			for (int j = 0, lenj = xos.length; j < lenj; j++)
			{
				JEXData jexdata = xos[j][i];
				if(jexdata == null)
				{
					data[i][j] = 0.0;
					continue;
				}
				Double value = ValueReader.readObjectToDouble(jexdata);
				if(value == null || value.isNaN() || value.isInfinite())
				{
					data[i][j] = 0.0;
					continue;
				}
				data[i][j] = value;
			}
		}
		
		boolean useGraphicsYAxis = true;
		// panel = new HeatMap(data, useGraphicsYAxis,
		// Gradient.GRADIENT_BLACK_TO_WHITE);
		// panel = new HeatMap(data, useGraphicsYAxis,
		// Gradient.GRADIENT_BLACK_TO_WHITE);
		panel = new HeatMap(data, useGraphicsYAxis, Gradient.createGradient(Color.BLACK, Color.GREEN, 100));
		
		// Color[] colors = new Color[] {Color.red, new Color(172,101,0), new
		// Color(43,164,0), Color.green};
		// Color[] colors = new Color[] {Color.black, Color.BLUE, Color.cyan,
		// Color.green, Color.yellow, Color.red, Color.white};
		// Color[] multiGrad = Gradient.createMultiGradient(colors, 250);
		// panel = new HeatMap(data, useGraphicsYAxis,
		// Gradient.createGradient(Color.BLACK, Color.GREEN, 100));
		// panel = new HeatMap(data, useGraphicsYAxis, multiGrad);
		
		// set miscelaneous settings
		panel.setDrawLegend(true);
		
		panel.setTitle("Height (m)");
		panel.setDrawTitle(true);
		
		panel.setXAxisTitle("X-Distance (m)");
		panel.setDrawXAxisTitle(true);
		
		panel.setYAxisTitle("Y-Distance (m)");
		panel.setDrawYAxisTitle(true);
		
		panel.setCoordinateBounds(0, 6.28, 0, 6.28);
		panel.setDrawXTicks(true);
		panel.setDrawYTicks(true);
		
		panel.setColorForeground(Color.black);
		panel.setColorBackground(Color.white);
		
		this.getContentPane().add(panel);
		
	}
	
	public HeatMapFrame() throws Exception
	{
		super("Heat Map Frame");
		this.setVisible(false);
	}
	
	/**
	 * Make the heat map in the frame
	 * 
	 * @param xos
	 */
	public void makeHeatMap(JEXData[][] xos)
	{
		// Prepare the min and max
		Double minValue = Double.MAX_VALUE;
		Double maxValue = Double.MIN_VALUE;
		Double meanValue = Double.NaN;
		double counter = 0;
		
		// Get the value field and make the scale
		Double[][] data = new Double[xos[0].length][xos.length];
		for (int i = 0, leni = xos[0].length; i < leni; i++)
		{
			for (int j = 0, lenj = xos.length; j < lenj; j++)
			{
				
				// Get the jexdata
				JEXData jexdata = xos[j][i];
				
				// If it is null, set it to NAN
				if(jexdata == null)
				{
					data[i][j] = Double.NaN;
					continue;
				}
				
				// Else read the value at that location
				Double value = ValueReader.readObjectToDouble(jexdata);
				
				// If the value is null set the value in the array to the mean
				// value
				if(value == null || value.isNaN() || value.isInfinite())
				{
					data[i][j] = Double.NaN;
					continue;
				}
				
				// Set the value in the array
				data[i][j] = value;
				
				// Update the min max mean
				minValue = Math.min(minValue, value);
				maxValue = Math.max(maxValue, value);
				meanValue = (meanValue.equals(Double.NaN)) ? value : meanValue + value;
				counter = counter + 1;
			}
		}
		// Finalize the value of the mean
		meanValue = meanValue / counter;
		
		// if percent variation is switched on, convert the data
		if(percent)
		{
			// Reset the min and max
			minValue = Double.MAX_VALUE;
			maxValue = Double.MIN_VALUE;
			
			// Loop through the data and convert it to a percent change
			for (int i = 0, leni = data[0].length; i < leni; i++)
			{
				for (int j = 0, lenj = data.length; j < lenj; j++)
				{
					
					// If the data is null, set to mean value
					if(data[i][j].equals(Double.NaN))
						data[i][j] = meanValue;
					
					// Convert data to percent change
					data[i][j] = 100 * (data[i][j] - meanValue) / meanValue;
					
					// Update the min and max
					minValue = Math.min(minValue, data[i][j]);
					maxValue = Math.max(maxValue, data[i][j]);
				}
			}
			meanValue = 0.0;
		}
		
		// Determine the min / max / mean for the scale
		minRange = (minRange.equals(Double.NaN)) ? minValue : Math.min(minValue, minRange);
		maxRange = (maxRange.equals(Double.NaN)) ? maxValue : Math.max(maxValue, maxRange);
		if(meanRange.equals(Double.NaN))
			meanRange = meanValue;
		else
			meanRange = (meanRange > maxRange) ? maxRange : (meanRange < minRange) ? minRange : meanRange;
		
		int numSteps = 100;
		int indexOfMean = (int) (numSteps * (meanRange - minRange) / (maxRange - minRange));
		
		// Make the color scale
		Color[] redToGreen = Gradient.createRedToGreenGradientWithSetMean(numSteps, indexOfMean);
		
		boolean useGraphicsYAxis = true;
		// panel = new HeatMap(data, useGraphicsYAxis, redToGreen);
		panel = new HeatMap();
		panel.setRange(minRange, maxRange);
		panel.makeHeatMap(data, useGraphicsYAxis, redToGreen);
		
		// set miscelaneous settings
		panel.setDrawLegend(true);
		
		panel.setTitle("Height (m)");
		panel.setDrawTitle(true);
		
		panel.setXAxisTitle("X-Distance (m)");
		panel.setDrawXAxisTitle(true);
		
		panel.setYAxisTitle("Y-Distance (m)");
		panel.setDrawYAxisTitle(true);
		
		panel.setCoordinateBounds(0, 6.28, 0, 6.28);
		panel.setDrawXTicks(true);
		panel.setDrawYTicks(true);
		
		panel.setColorForeground(Color.black);
		panel.setColorBackground(Color.white);
		
		this.getContentPane().add(panel);
	}
	
	/**
	 * Set the range for coloring when exporting a heat map
	 * 
	 * @param min
	 * @param mean
	 * @param max
	 */
	public void setRange(Double min, Double mean, Double max)
	{
		this.minRange = min;
		this.maxRange = max;
		this.meanRange = mean;
	}
	
	/**
	 * Convert values to percent variation around mean
	 * 
	 * @param percent
	 */
	public void setpercentVariation(boolean percent)
	{
		this.percent = percent;
	}
	
	/**
	 * Create an image of only the array
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public ImagePlus getHeatMapImage(int width, int height)
	{
		// solution 1
		int w = panel.getWidth();
		int h = panel.getHeight();
		java.awt.image.BufferedImage bim = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics g = bim.createGraphics();
		panel.paint(g);
		ImagePlus im = new ImagePlus("", bim);
		
		// solution 2
		// java.awt.image.BufferedImage image =
		// (java.awt.image.BufferedImage)panel.createImage(width , height);
		// ImagePlus im = new ImagePlus("",image);
		// im.show();
		
		return im;
	}
	
	/**
	 * Create an image of only the array
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public ImagePlus getImage(int width, int height)
	{
		Image image = this.createImage(width, height);
		ImagePlus im = new ImagePlus("", image);
		return im;
	}
	
	// // this function will be run from the EDT
	// private static void createAndShowGUI() throws Exception
	// {
	// HeatMapFrame hmf = new HeatMapFrame();
	// hmf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	// hmf.setSize(500,500);
	// hmf.setVisible(true);
	// }
	//
	// public static void main(String[] args)
	// {
	// SwingUtilities.invokeLater(new Runnable()
	// {
	// public void run()
	// {
	// try
	// {
	// createAndShowGUI();
	// }
	// catch (Exception e)
	// {
	// System.err.println(e);
	// e.printStackTrace();
	// }
	// }
	// });
	// }
}
