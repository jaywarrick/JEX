package function.plugin.plugins.roi;

import java.awt.Rectangle;
import java.awt.Shape;
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
import image.roi.IdPoint;
import image.roi.PointList;
import image.roi.ROIPlus;
import jex.statics.JEXDialog;
import jex.statics.JEXStatics;
import logs.Logs;
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
		name="Register Point ROI",
		menuPath="ROI",
		visible=true,
		description="Use a crop roi to exclude points and redefine their origin (register) for use with images cropped by the same roi."
		)
public class RegisterPointRoi extends JEXPlugin {

	public RegisterPointRoi()
	{}

	/////////// Define Inputs ///////////

	@InputMarker(uiOrder=1, name="Maxima", type=MarkerConstants.TYPE_ROI, description="ROI to be registered.", optional=false)
	JEXData maximaData;
	
	@InputMarker(uiOrder=2, name="Crop Roi", type=MarkerConstants.TYPE_ROI, description="ROI to be registered.", optional=false)
	JEXData cropData;

	/////////// Define Parameters ///////////

	//	@ParameterMarker(uiOrder=5, name="Reference point", description="Center point for scaling and rotation operations.", ui=MarkerConstants.UI_DROPDOWN, choices={"Origin", "ROI Center"}, defaultChoice=0)
	//	String referencePoint;
	//	
	//	@ParameterMarker(uiOrder=6, name="Move to origin first?", description="Move the ROI to the origin first before any scale/translate/rotate operation? (typically false, primarily use to move )", ui=MarkerConstants.UI_DROPDOWN, choices={"No","Yes - UL Corner", "Yes - Center"}, defaultChoice=0)
	//	String moveToOrigin;
	//	
	//	@ParameterMarker(uiOrder=1, name="Scale", description="Multiplier to scale locations/size of all ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="1.0")
	//	double scale;
	//	
	//	@ParameterMarker(uiOrder=2, name="Rotate (deg, CCW)", description="angle of rotation around the point indicated in ", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	//	double rotation;
	//	
	//	@ParameterMarker(uiOrder=3, name="Translate X", description="X displacement", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	//	double deltaX;
	//	
	//	@ParameterMarker(uiOrder=4, name="Translate Y", description="Y displacement", ui=MarkerConstants.UI_TEXTFIELD, defaultText="0.0")
	//	double deltaY;

	/////////// Define Outputs ///////////

	@OutputMarker(uiOrder=1, name="Registered Maxima", type=MarkerConstants.TYPE_ROI, flavor="", description="The roi containing the registered maxima.", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 10;
	}

	@Override
	public boolean run(JEXEntry optionalEntry)
	{
		maximaData.getDataMap();
		if(maximaData == null || !maximaData.getTypeName().getType().equals(JEXData.ROI))
		{
			JEXDialog.messageDialog("A maxima roi must be provided. Aborting.", this);
			return false;
		}
		
		cropData.getDataMap();
		if(cropData == null || !cropData.getTypeName().getType().equals(JEXData.ROI))
		{
			JEXDialog.messageDialog("A crop roi must be provided. Aborting.", this);
			return false;
		}

		// Run the function
		TreeMap<DimensionMap,ROIPlus> original = RoiReader.readObjectToRoiMap(maximaData);
		TreeMap<DimensionMap,ROIPlus> cropMap = RoiReader.readObjectToRoiMap(cropData);
		TreeMap<DimensionMap,ROIPlus> adjusted = new TreeMap<DimensionMap,ROIPlus>();
		int count = 0, percentage = 0;
		PointList toSave = null;
		for (DimensionMap map : cropMap.keySet())
		{
			toSave = new PointList();
			ROIPlus roi = original.get(map).copy();
			ROIPlus crop = cropMap.get(map);
			if(roi == null || crop == null)
			{
				Logs.log("Either no maxima roi or no crop roi at " + map + ". Skipping.", this);
				continue;
			}
			
			// Filter the points by cropping out any outside the region (or on the border)
			Shape cropS = crop.getShape();
			PointList pl = roi.getPointList();
			for(IdPoint p : pl)
			{
				if(cropS.contains(p))
				{
					toSave.add(p);
				}
			}
			
			// Translate the points to the new origin.
			Rectangle r = crop.pointList.getBounds();
			toSave.translate(-r.x, -r.y);
			
			// Save the new ROI
			adjusted.put(map, new ROIPlus(toSave, ROIPlus.ROI_POINT));
			
			// Update progress
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
