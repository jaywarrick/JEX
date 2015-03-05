package function.plugin.plugins.imageThresholding;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import fiji.threshold.Auto_Local_Threshold;
import function.plugin.mechanism.InputMarker;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.OutputMarker;
import function.plugin.mechanism.ParameterMarker;


@Plugin(
		type = JEXPlugin.class,
		name="Auto Local Threshold",
		menuPath="Image Thresholding > Auto Local Threshold",
		visible=true,
		description="Function that allows you to stitch an image ARRAY into a single image using two image alignment objects."
		)
public class AutoLocalThresholding extends JEXPlugin{
	
	
	
	public AutoLocalThresholding()
	{}

	/////////// Define Inputs ///////////
	
	@InputMarker(name="Images", type=MarkerConstants.TYPE_IMAGE, description="Image to be thresholded.", optional=false)
	JEXData imageData;

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=0, name="Method", description="select algorithm to be applied", ui=MarkerConstants.UI_DROPDOWN, choices={"Try All", "Bernsen", "Contrast", "Mean", "Median", "MidGrey", "Niblack", "Otsu", "Phansalkar", "Sauvola"}, defaultChoice=0)
	String method;
	
		
	@ParameterMarker(uiOrder=1, name="radius", description="sets the radius of the local domain over which the threshold will be computed", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String radius;

	@ParameterMarker(uiOrder=2, name="White Obj on Black Backgr", description="sets to white the pixels with values above the threshold value (otherwise, it sets to white the values less or equal to the threshold)", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String wObjbBack;
	
	@ParameterMarker(uiOrder=3, name="Parameter 1", description="See imagej.net/Auto_Local_Threshold for details", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String param1;
	
	@ParameterMarker(uiOrder=4, name="Parameter 2", description="See imagej.net/Auto_Local_Threshold for details", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String param2;
	
	@ParameterMarker(uiOrder=5, name="Stack", description="can be used to process all the slices of a stack", ui=MarkerConstants.UI_CHECKBOX, defaultBoolean=false)
	boolean stackProcessing;

	
	@Override
	public boolean run(JEXEntry optionalEntry) {
		// TODO Auto-generated method stub
		return false;
	}

	//////////Define Outputs ///////////
	
	@OutputMarker(name="Thresholded Image", type=MarkerConstants.TYPE_IMAGE, flavor="", description="The resultant thresholded image", enabled=true)
	JEXData output;
}
