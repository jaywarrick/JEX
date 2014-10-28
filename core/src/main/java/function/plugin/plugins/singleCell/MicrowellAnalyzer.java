package function.plugin.plugins.singleCell;

import ij.process.ImageProcessor;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;

import java.awt.Rectangle;
import java.util.TreeMap;

import logs.Logs;
import miscellaneous.StatisticsUtility;
import rtools.R;


public class MicrowellAnalyzer {
	
	
	/**
	 * Quantify secretion intensity (1 color) and bead intensity (a different color) along with cell counts for microwells.
	 * 
	 * Most pixels in a microwell are background, thus we can use median to get the background. We can optionally refine this somewhat by 
	 * saying that background pixels are also dark in the "enhanced brightfield" image of the microwells. Pixels used to calculate the "background" means
	 * and thresholds are plotted as black dots. We get the median and mad of the "background" pixels to determine a 4 std dev's threshold for each color.
	 * These black dots are plotted over the red dots which is the entire population of pixels in the microwell. The brightfield is able to 
	 * provide a fluroescence independent method of refining what we call background. 
	 * 
	 * Next, we want to quantify anything that is a bead, so we only use the threshold of the bead color to determine which pixels in the microwell
	 * to quantify for secretion and bead signals, as we are typically interested in secretion signal per bead signal per microwell.
	 * 
	 * @param image1
	 * @param image2
	 * @param cells
	 * @param microwell
	 * @param template
	 * @param plot
	 * @param bfIm
	 * @param bfThresh
	 * @param bfRoiScale
	 * @return
	 */
	public static TreeMap<String,Object> getSecretionData(ImageProcessor image1, ImageProcessor image2, ROIPlus cells, IdPoint microwell, ROIPlus template, boolean plot, ImageProcessor bfIm, double bfThresh, double bfRoiScale) 
	{
		PointList pl = template.getPointList();
		pl.setCenter(microwell.x, microwell.y);
		Rectangle r = pl.getBounds();
		int n = r.width*r.height;
		int xMax = Math.max(image1.getWidth(), image2.getWidth());
		int yMax = Math.max(image1.getHeight(), image2.getHeight());
		boolean haveBF = (bfIm != null);
		
		// Get the data from the images
		double[] sig1 = new double[n];
		double[] sig2 = new double[n];
		double[] bf = new double[n];
		int i = 0;
		for(int x=r.x; x<(r.x+r.width); x++)
		{
			for(int y=r.y; y<(r.y+r.width); y++)
			{
				if(x >= 0 && x < xMax && y >= 0 && y < yMax)
				{
					sig1[i] = image1.getPixelValue(x, y);
					sig2[i] = image2.getPixelValue(x, y);
					if(haveBF)
					{
						bf[i] = bfIm.getPixelValue((int)Math.round(x*bfRoiScale), (int)Math.round(y*bfRoiScale));
					}
					i++;
				}
				else
				{
					Logs.log("Index out of bounds!!", Logs.ERROR, MicrowellAnalyzer.class);
				}
			}
		}
		
		boolean [] bfFilterHi = null;
		boolean [] bfFilterLo = null;
		double[] sig1BG = null;
		double[] sig2BG = null;
		if(haveBF)
		{
			bfFilterHi = StatisticsUtility.getThresholdFilter(bf, bfThresh, true, false);
			bfFilterLo = StatisticsUtility.getThresholdFilter(bf, bfThresh, false, true);
			sig1BG = StatisticsUtility.applyFilter(sig1, bfFilterLo); // Make sure we are working with BG pixels
			sig2BG = StatisticsUtility.applyFilter(sig2, bfFilterLo); // Make sure we are working with BG pixels
			sig1 = StatisticsUtility.applyFilter(sig1, bfFilterHi); // Make sure we are working with pixels where there is a bead or something
			sig2 = StatisticsUtility.applyFilter(sig2, bfFilterHi); // Make sure we are working with pixels where there is a bead or something
		}
		else
		{
			sig1BG = sig1;
			sig2BG = sig2;
		}
		
		// We want sig1/sig2 ratio so because we calculate dy/dx, we treat sig1 as y and sig2 as x
		double xMad = StatisticsUtility.mad(sig2BG);
		double yMad = StatisticsUtility.mad(sig1BG);
		double xMed = StatisticsUtility.median(sig2BG);
		double yMed = StatisticsUtility.median(sig1BG);
		double xThresh = xMed + 4*xMad;
		double yThresh = yMed + 4*yMad;
		boolean[] xFilter = StatisticsUtility.getThresholdFilter(sig2, xThresh, true, false); // Make sure we are working with pixels where there is a bead (this is a different measure of 'beadness')
		double[] xFiltered = StatisticsUtility.applyFilter(sig2, xFilter);
		double[] yFiltered = StatisticsUtility.applyFilter(sig1, xFilter);
			
		TreeMap<String,Object> results = new TreeMap<String,Object>();
		if(xFiltered.length > 10)
		{
			double ySig = StatisticsUtility.mean(yFiltered);
			double rise = ySig - yMed;
			double xSig = StatisticsUtility.mean(xFiltered);
			double run = xSig - xMed;
			double slope = rise/run;
			double yint = yMed - slope*xMed;
			double[] fit = new double[]{yint, slope};
			
			String path = null;
			if(plot)
			{
				path = plotPixels(microwell.id, sig2BG, sig1BG, sig2, sig1, xThresh, yThresh, fit);
			}
			
			results.put("plot", path);
			results.put("secretionMed", yMed);
			results.put("secretionMad", yMad);
			results.put("secretionThresh", yThresh);
			results.put("secretionSig", ySig);
			results.put("beadMed", xMed);
			results.put("beadMad", xMad);
			results.put("beadThresh", xThresh);
			results.put("beadSig", xSig);
			results.put("ratio", fit[1]);
			
			if(cells != null)
			{
				int numCells = 0;
				for(IdPoint p : cells.getPointList())
				{
					if(r.contains(p))
					{
						numCells++;
					}
				}
				results.put("cellCount", numCells);
			}
			return results;
		}
		else
		{
			return null;
		}
	}
	
	public static String plotPixels(int id, double[] xBG, double[] yBG, double[] x, double[] y, double xThresh, double yThresh, double[] fit)
	{
		String path = R.startPlot("tif", 4, 3, 150, 12, R.FONT_HELVETICA, R.COMPRESSION_NONE);
		
		R.eval("'Plotting pixel data'");
		
		// Make the plot somewhat prettier
		R.eval("par(mar = c(3.7,3.7,0.8,0.8)) # beyond plot frame [b,l,t,r]");
		R.eval("par(mgp = c(2.5,0.8,0)) # placement of axis labels [1], numbers [2] and symbols[3]");
		R.eval("par(oma = c(0.1,0.1,0.1,0.1)) # cutoff beyond other measures");
		R.eval("par(cex = 1.25)");
		R.eval("par(cex.lab = 0.75) # axislabelsize");
		R.eval("par(cex.axis = 0.75)");
		
		// Pass R the values to plot
		R.makeVector("x", x);
		R.makeVector("y", y);
		R.makeVector("xBG", xBG);
		R.makeVector("yBG", yBG);
		
		// Plot stuff and adorn the plot
		R.eval("plot(x, y, xlab='Bead Signal [au]', ylab='Secretion Signal [au]', pch=20, cex=0.25, col='red')");
		R.eval("points(xBG, yBG, pch=20, cex=0.25, col='black')");
		R.eval("yint <- " + fit[0]);
		R.eval("slope <- " + fit[1]);
		R.eval("abline(v=" + xThresh + ", lty=2, col='gray')");
		R.eval("abline(h=" + yThresh + ", lty=2, col='gray')");
		R.eval("abline(yint, slope)");
		R.eval("mtext(paste('slope=',round(slope,2),', int=', round(yint,0), sep=''), adj=1)");
		R.eval("mtext(" + R.sQuote("ID=" + id) + ", adj=0)");
		
		// Call this to finish writing the plot file.
		R.endPlot();
		
		// Return the path to the plot.
		return path;
	}
	
}
