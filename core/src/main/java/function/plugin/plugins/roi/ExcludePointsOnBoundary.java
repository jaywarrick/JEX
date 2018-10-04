package function.plugin.plugins.roi;

import java.awt.Rectangle;
import java.util.TreeMap;
import java.util.Vector;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.ImageReader;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import ij.ImagePlus;
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import miscellaneous.CSVList;
import miscellaneous.StringUtility;
import tables.DimensionMap;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 * @author erwinberthier
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Exclude Points On Boundary",
		menuPath="ROI",
		visible=true,
		description="Keep points inside the boundary of the specified image/roi with user-specified widths (if no boundary is specified via image or roi, keep all points)."
		)
public class ExcludePointsOnBoundary extends JEXPlugin {

	public ExcludePointsOnBoundary()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Point Roi", type=MarkerConstants.TYPE_IMAGE, description="Image to be adjusted.", optional=false)
	JEXData roiData;

	@InputMarker(uiOrder=2, name="Image", type=MarkerConstants.TYPE_IMAGE, description="Image to be used to determine boundaries (takes precedence over Roi).", optional=false)
	JEXData imageData;

	@InputMarker(uiOrder=3, name="Region Roi (need Region Roi or Image)", type=MarkerConstants.TYPE_IMAGE, description="ROI to be used to determine boundaries.", optional=false)
	JEXData regionData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Boundary Width(s)", description="A single value or comma separated list of values that represent the boundary width in pixels along the Left, Top, Right, and Bottom boundaries (in that order).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1,1,1,1")
	String boundaryWidths;

	@ParameterMarker(uiOrder=1, name="Treat Region ROI as relative?", description="Should the Region ROI position be ignored and just position it at 0,0 (useful if you have an ROI that was used to crop and image in a previous step).", ui=MarkerConstants.UI_TEXTFIELD, defaultText="true")
	boolean relative;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Filtered ROI", type=MarkerConstants.TYPE_ROI, flavor="", description="The resultant roi.", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		if(roiData == null || !roiData.getTypeName().getType().matches(JEXData.ROI))
		{
			return false;
		}

		boolean haveRoi = false;
		if(imageData == null || !imageData.getTypeName().getType().matches(JEXData.IMAGE))
		{
			regionData.getDataMap();
			if(regionData == null || !regionData.getTypeName().getType().matches(JEXData.ROI))
			{
				JEXDialog.messageDialog("Both the image and the region roi were invalid for some reason.", this);
				return false;
			}
			else
			{
				haveRoi = true;
			}
		}

		// Get the boundary widths (L, T, R, B)
		Vector<Integer> widths = convertWidthsToNumeric(getCSVList(this.boundaryWidths));

		// Run the function
		TreeMap<DimensionMap,ROIPlus> original = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,ROIPlus> adjusted = new TreeMap<>();
		int count = 0, percentage = 0;
		if(haveRoi)
		{
			TreeMap<DimensionMap,ROIPlus> boundaries = RoiReader.readObjectToRoiMap(regionData);
			for (DimensionMap map : boundaries.keySet())
			{
				ROIPlus roi = original.get(map).copy();
				ROIPlus boundary = boundaries.get(map);
				if(roi == null)
				{
					continue;
				}
				if(boundary == null)
				{
					adjusted.put(map, new ROIPlus(roi.getPointList().copy(), ROIPlus.ROI_POINT));
				}
				PointList pl = roi.getPointList();
				Rectangle r = boundary.getPointList().getBounds();
				if(!relative)
				{
					r.x = r.x + widths.get(0);
					r.width = r.width - widths.get(0) - widths.get(2);
					r.y = r.y + widths.get(1);
					r.height = r.height - widths.get(1) - widths.get(3);
				}
				else
				{
					r.x = widths.get(0);
					r.width = r.width - widths.get(0) - widths.get(2);
					r.y = widths.get(1);
					r.height = r.height - widths.get(1) - widths.get(3);
				}
				PointList newPl = new PointList();
				for(IdPoint p : pl)
				{
					if(r.contains(p))
					{
						newPl.add(p.copy());
					}
				}
				adjusted.put(map, new ROIPlus(newPl, ROIPlus.ROI_POINT));

				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) original.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
			if(adjusted.size() == 0)
			{
				return false;
			}

			this.output = RoiWriter.makeRoiObject("temp", adjusted);

			// Return status
			return true;
		}
		else
		{
			TreeMap<DimensionMap,String> boundaries = ImageReader.readObjectToImagePathTable(imageData);
			for (DimensionMap map : boundaries.keySet())
			{
				ROIPlus roi = original.get(map).copy();
				String boundaryImagePath = boundaries.get(map);
				if(roi == null)
				{
					continue;
				}
				if(boundaryImagePath == null)
				{
					adjusted.put(map, new ROIPlus(roi.getPointList().copy(), ROIPlus.ROI_POINT));
				}
				PointList pl = roi.getPointList();
				ImagePlus im = new ImagePlus(boundaryImagePath);
				Rectangle r = new Rectangle(0, 0, im.getWidth(), im.getHeight());
				r.x = r.x + widths.get(0);
				r.width = r.width - widths.get(0) - widths.get(2);
				r.y = r.y + widths.get(1);
				r.height = r.height - widths.get(1) - widths.get(3);
				PointList newPl = new PointList();
				for(IdPoint p : pl)
				{
					if(r.contains(p))
					{
						newPl.add(p.copy());
					}
				}
				adjusted.put(map, new ROIPlus(newPl, ROIPlus.ROI_POINT));

				count = count + 1;
				percentage = (int) (100 * ((double) (count) / ((double) original.size())));
				JEXStatics.statusBar.setProgressPercentage(percentage);
			}
			if(adjusted.size() == 0)
			{
				return false;
			}

			this.output = RoiWriter.makeRoiObject("temp", adjusted);

			// Return status
			return true;
		}
	}

	private CSVList getCSVList(String param)
	{
		CSVList temp = new CSVList(param);
		CSVList ret = new CSVList();
		if(temp.size() == 1)
		{
			for(int i = 0; i < 4; i++)
			{
				// Repeat the value for all the borders
				ret.add(StringUtility.removeWhiteSpaceOnEnds(temp.get(0)));
			}
		}
		else
		{
			for(String p : temp)
			{
				// This list may not be the same length as the channel dim but we'll test for that elsewhere.
				ret.add(StringUtility.removeWhiteSpaceOnEnds(p));
			}
		}

		return ret;
	}

	private Vector<Integer> convertWidthsToNumeric(CSVList widths)
	{
		Vector<Integer> ret = new Vector<>();
		for(String width : widths)
		{
			ret.add(Integer.parseInt(width));
		}
		return ret;
	}
}
