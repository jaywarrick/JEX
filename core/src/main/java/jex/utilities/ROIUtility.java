package jex.utilities;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Vector;

import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import image.roi.ROIPlus.PatternRoiIterator;

public class ROIUtility {
	
	public static float[] getPixelsInRoi(FloatProcessor ip, ROIPlus roi)
	{
		if(roi == null)
		{
			return null;
		}
		
		// Use roi bounds because typically this will be faster as the ROI will generally be a small region within the image.
		Rectangle r = roi.pointList.getBounds();
		Roi roi2 = roi.getRoi();
		PointList within = new PointList();
		for (int y = r.y; y < r.y + r.height; y++)
		{
			for (int x = r.x; x < r.x + r.width; x++)
			{
				// ROI might be outside image so check if within ROI AND if within bounds of image.
				if(x >= 0 && y >= 0 && x < ip.getWidth() && y < ip.getHeight() && roi2.contains(x, y))
				{
					within.add(x, y);
				}
				
			}
		}
		float[] ret = new float[within.size()];
		for (int i = 0; i < within.size(); i++)
		{
			ret[i] = ip.getPixelValue(within.get(i).x, within.get(i).y);
		}
		return ret;
	}
	
	public static float[] getPixelsOutsideRoi(FloatProcessor ip, ROIPlus roi)
	{
		if(roi == null)
		{
			return null;
		}
		Rectangle r = new Rectangle(ip.getWidth(), ip.getHeight());
		Shape s = roi.getShape();
		PointList outside = new PointList();
		for (int y = r.y; y < r.y + r.height; y++)
		{
			for (int x = r.x; x < r.x + r.width; x++)
			{
				if(!s.contains(x, y))
				{
					outside.add(x, y);
				}
				
			}
		}
		float[] ret = new float[outside.size()];
		for (int i = 0; i < outside.size(); i++)
		{
			ret[i] = ip.getPixelValue(outside.get(i).x, outside.get(i).y);
		}
		return ret;
	}
	
	public static ROIPlus excludePoints(ROIPlus pointRoi, ROIPlus regionRoi, boolean keepPointsInsideRegion)
	{
		if(pointRoi == null || regionRoi == null || regionRoi.getPointList().size() == 0)
		{
			return null;
		}
		PointList ret = new PointList();
		PatternRoiIterator itr = regionRoi.patternRoiIterator();
		Vector<Roi> patternRois = new Vector<>();
		while(itr.hasNext())
		{
			ROIPlus r = itr.next();
			patternRois.add(r.getRoi());
		}
		for(IdPoint p : pointRoi.getPointList())
		{
			for(Roi roi : patternRois)
			{
				boolean pointInRegion = roi.contains(p.x, p.y);
				if(pointInRegion && keepPointsInsideRegion)
				{
					ret.add(p);
				}
				else if(!pointInRegion && !keepPointsInsideRegion)
				{
					ret.add(p);
				}
			}
		}
		ROIPlus toRet = new ROIPlus(ret, ROIPlus.ROI_POINT);
		return toRet;
	}
	
	/**
	 * Get the center of mass of the ROI based on weights provided by a binary imageprocessor IP
	 * 
	 * @param ip
	 * @return
	 */
	public static Point getCenterOfMass(ImageProcessor ip, ROIPlus myRoi)
	{
		// setup
		int width = ip.getWidth();
		int height = ip.getHeight();
		Rectangle roi = myRoi.getPointList().getBounds();
		int rx, ry, rw, rh;
		if(roi != null)
		{
			rx = roi.x;
			ry = roi.y;
			rw = roi.width;
			rh = roi.height;
		}
		else
		{
			rx = 0;
			ry = 0;
			rw = width;
			rh = height;
		}
		
		byte[] pixels = (byte[]) ip.getPixels();
		byte[] mask = ip.getMaskArray();
		int v, i, mi;
		double dv, sum1 = 0.0, xsum = 0.0, ysum = 0.0;
		for (int y = ry, my = 0; y < (ry + rh); y++, my++)
		{
			i = y * width + rx;
			mi = my * rw;
			for (int x = rx; x < (rx + rw); x++)
			{
				if(mask == null || mask[mi++] != 0)
				{
					v = pixels[i] & 255;
					dv = (v) / 255.0;
					sum1 += dv;
					xsum += x * dv;
					ysum += y * dv;
				}
				i++;
			}
		}
		double xCenterOfMass = xsum / sum1 + 0.5;
		double yCenterOfMass = ysum / sum1 + 0.5;
		
		Point result = new Point((int) xCenterOfMass, (int) yCenterOfMass);
		return result;
	}
	
	/**
	 * Get the center of the centroid for this roi based on a byte imageprocessor IP
	 * 
	 * @param ip
	 * @return
	 */
	public static Point getCentroid(ImageProcessor ip, ROIPlus myRoi)
	{
		// setup
		int width = ip.getWidth();
		int height = ip.getHeight();
		Rectangle roi = ip.getRoi();
		int rx, ry, rw, rh;
		if(roi != null)
		{
			rx = roi.x;
			ry = roi.y;
			rw = roi.width;
			rh = roi.height;
		}
		else
		{
			rx = 0;
			ry = 0;
			rw = width;
			rh = height;
		}
		ip.setRoi(myRoi.getRoi());
		
		// Calculate
		byte[] mask = ip.getMaskArray();
		int count = 0, mi;
		double xsum = 0.0, ysum = 0.0;
		for (int y = ry, my = 0; y < (ry + rh); y++, my++)
		{
			mi = my * rw;
			for (int x = rx; x < (rx + rw); x++)
			{
				if(mask == null || mask[mi++] != 0)
				{
					count++;
					xsum += x;
					ysum += y;
				}
			}
		}
		double xCentroid = xsum / count + 0.5;
		double yCentroid = ysum / count + 0.5;
		
		// Prepare result
		Point result = new Point((int) xCentroid, (int) yCentroid);
		return result;
	}
	
	/**
	 * Get the center of the centroid for this roi The function will need to build a default binary imageprocessor
	 * 
	 * @param ip
	 * @return
	 */
	public static Point getCentroidOLD(ROIPlus myRoi)
	{
		// Get size
		Rectangle rect = myRoi.getRoi().getBounds();
		int width = (int) (rect.getX() + rect.getWidth());
		int height = (int) (rect.getY() + rect.getHeight());
		
		// Create fake image
		ByteProcessor ip = new ByteProcessor(width, height);
		ip.setRoi(myRoi.getRoi());
		
		// setup
		Rectangle roi = ip.getRoi();
		int rx, ry, rw, rh;
		if(roi != null)
		{
			rx = roi.x;
			ry = roi.y;
			rw = roi.width;
			rh = roi.height;
		}
		else
		{
			rx = 0;
			ry = 0;
			rw = width;
			rh = height;
		}
		
		byte[] mask = ip.getMaskArray();
		int count = 0, mi;
		double xsum = 0.0, ysum = 0.0;
		for (int y = ry, my = 0; y < (ry + rh); y++, my++)
		{
			mi = my * rw;
			for (int x = rx; x < (rx + rw); x++)
			{
				if(mask == null || mask[mi++] != 0)
				{
					count++;
					xsum += x;
					ysum += y;
				}
			}
		}
		double xCentroid = xsum / count + 0.5;
		double yCentroid = ysum / count + 0.5;
		
		Point result = new Point((int) xCentroid, (int) yCentroid);
		return result;
	}
	
	/**
	 * Get the center of the centroid for this roi The function will need to build a default binary imageprocessor
	 * 
	 * @param ip
	 * @return
	 */
	public static Point getCentroid(ROIPlus myRoi)
	{
		
		// Get the mask
		Roi r = myRoi.getRoi();
		ImageProcessor ip = r.getMask();
		
		// Get size
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		// setup
		Rectangle roi = ip.getRoi();
		int rx, ry, rw, rh;
		if(roi != null)
		{
			rx = roi.x;
			ry = roi.y;
			rw = roi.width;
			rh = roi.height;
		}
		else
		{
			rx = 0;
			ry = 0;
			rw = width;
			rh = height;
		}
		
		byte[] mask = ip.getMaskArray();
		int count = 0, mi;
		double xsum = 0.0, ysum = 0.0;
		for (int y = ry, my = 0; y < (ry + rh); y++, my++)
		{
			mi = my * rw;
			for (int x = rx; x < (rx + rw); x++)
			{
				if(mask == null || mask[mi++] != 0)
				{
					count++;
					xsum += x;
					ysum += y;
				}
			}
		}
		double xCentroid = xsum / count + 0.5;
		double yCentroid = ysum / count + 0.5;
		
		Point result = new Point((int) xCentroid, (int) yCentroid);
		return result;
	}
	
	/**
	 * Get the center of a ROIPlus
	 * 
	 * @param ip
	 * @return
	 */
	public static Point getRectangleCenter(ROIPlus myRoi)
	{
		return myRoi.getPointList().getCenter();
	}
	
	/**
	 * Get the center of a Rectangle
	 * 
	 * @param ip
	 * @return
	 */
	public static Point getRectangleCenter(Rectangle myRoi)
	{
		return PointList.getCenter(myRoi);
	}
	
}
