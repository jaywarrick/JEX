package function.plugin.plugins.dataEditing;

import org.scijava.plugin.Plugin;

import Database.DBObjects.JEXData;
import Database.DBObjects.JEXEntry;
import Database.Definition.Type;
import Database.Definition.TypeName;
import cruncher.Ticket;
import function.plugin.mechanism.JEXPlugin;
import function.plugin.mechanism.MarkerConstants;
import function.plugin.mechanism.ParameterMarker;
import jex.statics.JEXStatics;

/**
 * This is a JEXperiment function template To use it follow the following instructions
 * 
 * 1. Fill in all the required methods according to their specific instructions 2. Place the file in the Functions/SingleDataPointFunctions folder 3. Compile and run JEX!
 * 
 * JEX enables the use of several data object types The specific API for these can be found in the main JEXperiment folder. These API provide methods to retrieve data from these objects, create new objects and handle the data they contain.
 * 
 */

@Plugin(
		type = JEXPlugin.class,
		name="Delete Object",
		menuPath="Data Editing",
		visible=true,
		description="Delete an object with a particular type, flavor, and name in the selected entries."
		)
public class DeleteObject extends JEXPlugin {

//	public static final String ANY = JEXData.ANY.getType();
//	public static final String IMAGE = JEXData.IMAGE.getType();
//	public static final String FILE = JEXData.FILE.getType();
//	public static final String ROI = JEXData.ROI.getType();
//	public static final String VALUE = JEXData.VALUE.getType();
//	public static final String LABEL =JEXData.LABEL.getType();
//	public static final String SOUND = JEXData.SOUND.getType();
//	public static final String MOVIE = JEXData.MOVIE.getType();
//	public static final String TRACK = JEXData.TRACK.getType();
//	public static final String WORKFLOW = JEXData.WORKFLOW.getType();
//	public static final String ROI_TRACK = JEXData.ROI_TRACK.getType();
	
//	public static final Type ANY = new Type("Any"); // Used when an plugin input can be generic (not a type intended for saving in the database).
//	public static final Type IMAGE = new Type("Image");
//	public static final Type FILE  = new Type("File");
//	public static final Type MOVIE = new Type("Movie");
//	public static final Type SOUND = new Type("Sound");
//	public static final Type VALUE = new Type("Value");
//	public static final Type LABEL = new Type("Label");
//	public static final Type FUNCTION_OLD = new Type("Function");
//	public static final Type WORKFLOW     = new Type("Workflow");
//	public static final Type ROI          = new Type("Roi");
//	public static final Type HIERARCHY    = new Type("Hierarchy");
//	public static final Type TRACK        = new Type("Track");
//	public static final Type ROI_TRACK    = new Type("ROI Track");

	public DeleteObject() 
	{}

	/////////// Define Parameters ///////////

	@ParameterMarker(uiOrder=1, name="Type", description="Type of the object to delete.", ui=MarkerConstants.UI_DROPDOWN, choices = {"Any", "Image", "File", "Roi", "Value", "Label", "Track", "ROI Track", "Movie",  "Workflow", "Sound" }, defaultChoice = 1)
	String type;

	@ParameterMarker(uiOrder=2, name="Flavor", description="Flavor of the object to delete.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="Virtual Chunk")
	String flavor;

	@ParameterMarker(uiOrder=3, name="Name", description="Name of the object to delete.", ui=MarkerConstants.UI_TEXTFIELD, defaultText="")
	String name;


	/////////// Define Outputs ///////////

	// Don't allow the gui to show the output because when the output is of type "ANY" then, when it is dragged to the input of another function
	// then it doesn't match the type of the object in the database (e.g., Image).
	//@OutputMarker(uiOrder=0, name="New Object", type=MarkerConstants.TYPE_ANY, flavor="", description="The resultant image with added dimension", enabled=true)
	JEXData output;

	@Override
	public int getMaxThreads()
	{
		return 1;
	}

	@Override
	public boolean run(JEXEntry optionalEntry) {

		TypeName toGet = new TypeName(new Type(type, flavor), name); 
		JEXData toRemove = JEXStatics.jexManager.getDataOfTypeNameInEntry(toGet, optionalEntry);

		// update the database manually (since we are working at such a low level)
		JEXStatics.jexDBManager.removeDataFromEntry(optionalEntry, toRemove);
		// Return status
		return true;
	}
	
	@Override
	public void finalizeTicket(Ticket ticket)
	{
		JEXStatics.jexDBManager.updateObjectsView();
	}


}
