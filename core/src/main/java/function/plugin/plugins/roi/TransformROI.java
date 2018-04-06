package function.plugin.plugins.roi;

import java.awt.Rectangle;
import java.util.TreeMap;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.DataReader.RoiReader;
import Database.DataWriter.RoiWriter;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;
import image.roi.PointList;
import image.roi.ROIPlus;
import jex.statics.JEXStatics;
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
		name="Transform ROI",
		menuPath="ROI",
		visible=true,
		description="Apply transformations to an ROI such as scale, rotate, and translate (done in that order)."
		)
public class TransformROI extends JEXPlugin {

	public TransformROI()
	{}
	
	/////////// Define Inputs ///////////
	
	@InputMarker(uiOrder=1, name="Roi", type=MarkerConstants.TYPE_ROI, description="ROI to be transformed.", optional=false)
	JEXData roiData;
	
	/////////// Define Parameters ///////////
	
	@ParameterMarker(uiOrder=5, name="Reference point", description="Center point for scaling and rotation operations.", ui=MarkerConstants.UI_DROPDOWN, choices={"Origin", "ROI Center"}, defaultChoice=0)
	String referencePoint;
	
	@ParameterMarker(uiOrder=6, name="Move to origin first?", description="Move the ROI to the origin first before any scale/translate/rotate operation? (typically false, primarily use to move )", ui=MarkerConstants.UI_DROPDOWN, choices={"No","Yes - UL Corner", "Yes - Center"}, defaultChoice=0)
	String moveToOrigin;
	
	@ParameterMarker(uiOrder=1, name="Scale", description="Multiplier to scale locations/size of all ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	double scale;
	
	@ParameterMarker(uiOrder=2, name="Rotate (deg, CCW)", description="angle of rotation around the point indicated in ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double rotation;
	
	@ParameterMarker(uiOrder=3, name="Translate X", description="X displacement", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double deltaX;
	
	@ParameterMarker(uiOrder=4, name="Translate Y", description="Y displacement", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	double deltaY;
	
	
	
	/////////// Define Outputs ///////////
	
	@OutputMarker(uiOrder=1, name="Adjusted ROI", type=MarkerConstants.TYPE_ROI, flavor="", description="The resultant adjusted roi.", enabled=true)
	JEXData output;
	
	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		roiData.getDataMap();
		if(roiData == null || !roiData.getTypeName().getType().equals(JEXData.ROI))
		{
			return false;
		}
		
		// Run the function
		TreeMap<DimensionMap,ROIPlus> original = RoiReader.readObjectToRoiMap(roiData);
		TreeMap<DimensionMap,ROIPlus> adjusted = new TreeMap<DimensionMap,ROIPlus>();
		int count = 0, percentage = 0;
		for (DimensionMap map : original.keySet())
		{
			ROIPlus roi = original.get(map).copy();
			PointList pl = roi.getPointList();
			String[] opts = new String[]{"Origin", "ROI Center"};
			
			// Move to the origin if necessary
			if(moveToOrigin.equals("Yes - Center"))
			{
				roi.pointList = pl.getPointListRelativeToCenter();
			}
			else if(moveToOrigin.equals("Yes - UL Corner"))
			{
				Rectangle r = pl.getBounds();
				pl.translate(-r.x, -r.y);
			}
			
			if(referencePoint.equals(opts[1]))
			{
				pl.scale(scale);
				pl.rotate(rotation);
			}
			else
			{
				pl.scaleRelativeToOrigin(scale);
				pl.rotateRelativeToOrigin(rotation);
			}
			
			pl.translate(deltaX, deltaY);
			
			adjusted.put(map, roi);
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
