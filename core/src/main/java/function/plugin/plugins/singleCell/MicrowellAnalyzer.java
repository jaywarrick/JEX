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
			double rise = StatisticsUtility.mean(yFiltered) - yMed;
			double run = StatisticsUtility.mean(xFiltered) - xMed;
			double slope = rise/run;
			double yint = yMed - slope*xMed;
			double[] fit = new double[]{yint, slope};
			
			String path = null;
			if(plot)
			{
				path = plotPixels(microwell.id, sig2BG, sig1BG, sig2, sig1, xThresh, yThresh, fit);
			}
			
			results.put("plot", path);
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
