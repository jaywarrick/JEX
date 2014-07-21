package function.plugin.mechanism;



public final class MarkerConstants {
	
	//Match these to JEXData static Types i.e.,...
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
	
	public static final String TYPE_IMAGE = "Image";
	public static final String TYPE_FILE = "File";
	public static final String TYPE_MOVIE = "Movie";
	public static final String TYPE_SOUND = "Sound";
	public static final String TYPE_VALUE = "Value";
	public static final String TYPE_Label = "Label";
	public static final String TYPE_Workflow = "Workflow";
	public static final String TYPE_ROI = "Roi";
	
	// Match constants in Database.Definition.Parameter class
	public static final int UI_TEXTFIELD = 0;
	public static final int UI_DROPDOWN = 1;
	public static final int UI_FILECHOOSER = 2;
	public static final int UI_CHECKBOX = 3;
	public static final int UI_PASSWORD = 4;
	
	public static final String IO_INPUT = "input";
	public static final String IO_OUTPUT = "output";
	
}
