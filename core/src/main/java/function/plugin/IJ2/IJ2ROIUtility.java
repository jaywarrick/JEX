package function.plugin.IJ2;

import image.roi.ROIPlus;

import java.awt.Rectangle;

import net.imagej.overlay.AbstractROIOverlay;
import net.imagej.overlay.RectangleOverlay;
import net.imglib2.roi.RegionOfInterest;

public class IJ2ROIUtility {
	
	public static AbstractROIOverlay<? extends RegionOfInterest> getROIOverlay(ROIPlus roi)
	{
		// AbstractROIOverlay<? extends RegionOfInterest> ret = null;
		
		return getRectangleOverlay(roi);
		
		// if(roi.type == ROIPlus.ROI_ELLIPSE)
		// {
		//
		// }
		// else if(roi.type == ROIPlus.ROI_LINE)
		// {
		//
		// }
		// else if(roi.type == ROIPlus.ROI_POINT)
		// {
		//
		// }
		// else if(roi.type == ROIPlus.ROI_POLYGON)
		// {
		//
		// }
		// else if(roi.type == ROIPlus.ROI_POLYLINE)
		// {
		//
		// }
		// else if(roi.type == ROIPlus.ROI_RECT)
		// {
		//
		// }
		//
		// return ret;
	}
	
	public static RectangleOverlay getRectangleOverlay(ROIPlus roi)
	{
		RectangleOverlay ret = new RectangleOverlay(IJ2PluginUtility.ij.getContext());
		
		Rectangle r = roi.getPointList().getBounds();
		
		// X
		ret.setOrigin(r.x, 0);
		ret.setExtent(r.width, 0);
		
		// Y
		ret.setOrigin(r.y, 1);
		ret.setExtent(r.height, 1);
		
		return ret;
	}
	
}
