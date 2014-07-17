package icons;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import logs.Logs;
import Database.DBObjects.JEXData;
import Database.Definition.Type;
import Database.Definition.TypeName;

public class IconRepository {
	
	// Menu icons
	public static String ARROW_UP = "ArrowUp.png";
	public static String ARROW_DOWN = "ArrowDown.png";
	public static String ARROW_FORWARD = "ArrowForward.png";
	public static String ARROW_BACK = "ArrowBack.png";
	public static String ARROW_UP_WHITE = "ArrowUpWhite.png";
	public static String ARROW_DOWN_WHITE = "ArrowDownWhite.png";
	public static String ARROW_FORWARD_WHITE = "ArrowForwardWhite.png";
	public static String ARROW_BACK_WHITE = "ArrowBackWhite.png";
	public static String CROSS_WHITE = "CrossWhite.png";
	public static String INFO_MAGNIFY = "Info.png";
	public static String ADD = "Add.png";
	
	public static String BUTTON_FORWARD = "ButtonFWD.png";
	
	public static String MAIN_CREATE_SELECTED = "MenuCreate.png";
	public static String MAIN_CREATE_UNSELECTED = "MenuCreateUnSelected.png";
	public static String MAIN_ARRAYVIEW_SELECTED = "MenuArray.png";
	public static String MAIN_ARRAYVIEW_UNSELECTED = "MenuArrayUnSelected.png";
	public static String MAIN_DISTRIBUTE_SELECTED = "MenuDistribute.png";
	public static String MAIN_DISTRIBUTE_UNSELECTED = "MenuDistributeUnSelected.png";
	public static String MAIN_LABEL_SELECTED = "MenuLabel.png";
	public static String MAIN_LABEL_UNSELECTED = "MenuLabelUnSelected.png";
	public static String MAIN_NOTES_SELECTED = "MenuNotes.png";
	public static String MAIN_NOTES_UNSELECTED = "MenuNotesUnSelected.png";
	public static String MAIN_FUNCTION_SELECTED = "MenuFunction.png";
	public static String MAIN_FUNCTION_UNSELECTED = "MenuFunctionUnSelected.png";
	public static String MAIN_STATS_SELECTED = "MenuStats.png";
	public static String MAIN_STATS_UNSELECTED = "MenuStatsUnSelected.png";
	public static String MAIN_VIEW_SELECTED = "MenuView.png";
	public static String MAIN_VIEW_UNSELECTED = "MenuViewUnSelected.png";
	public static String MAIN_PLUGIN_SELECTED = "MenuPlugins.png";
	public static String MAIN_PLUGIN_UNSELECTED = "MenuPluginsUnSelected.png";
	
	public static String MAIN_LOGON = "mainmenu_logon.png";
	public static String MAIN_SAVE = "mainmenu_save.png";
	public static String MAIN_MYSESSION = "mainmenu_session.png";
	public static String MAIN_EXP = "mainmenu_experimental.png";
	public static String MAIN_VIEWER = "mainmenu_viewer.png";
	public static String MAIN_OCTAVE = "mainmenu_octave.png";
	public static String MAIN_STATS = "mainmenu_stats.png";
	public static String MAIN_REPORT = "mainmenu_tex.png";
	public static String MAIN_BOOKMARK = "mainmenu_bookmark.png";
	public static String MAIN_NOTES = "mainmenu_notes.png";
	public static String MAIN_OPEN_USER = "mainmenu_openUser.png";
	public static String MAIN_CREATE_USER = "mainmenu_createUser.png";
	public static String MAIN_PREFS = "Prefs.png";
	public static String MAIN_PREFS_OVER = "Prefs_Over.png";
	public static String MAIN_PREFS_CLICK = "Prefs_Pressed.png";
	public static String MAIN_CLEAR = "Clear.png";
	public static String MAIN_CLEAR_OVER = "Clear_Over.png";
	public static String MAIN_CLEAR_CLICK = "Clear_Pressed.png";
	public static String MAIN_UPDATE = "Menu_Update.png";
	public static String MAIN_UPDATE_OVER = "Menu_Update_Over.png";
	public static String MAIN_UPDATE_CLICK = "Menu_Update_Pressed.png";
	
	public static String SESSION_DATABASE = "DataBase.png";
	public static String SESSION_DATABASE_NEW = "DataBaseNew.png";
	public static String SESSION_DATABASE_EDIT = "Info.png";
	public static String SESSION_DATABASE_DELETE = "Delete.png";
	public static String SESSION_DATABASE_CLEAN = "DataBaseClean.png";
	public static String SESSION_DATABASE_LOAD = "DataBaseLoad.png";
	public static String SESSION_DATABASE_LOADALL = "DataBaseLoadFull.png";
	public static String SESSION_EXPERIMENT_SELECT = "ExperimentSelect.png";
	public static String SESSION_EXPERIMENT_UNSELECT = "ExperimentUnSelect.png";
	public static String SESSION_EXPERIMENT_EDIT = "Preferences.png";
	public static String SESSION_EXPERIMENT_CONSOLIDATE = "ExperimentConsolidate.png";
	public static String SESSION_EXPERIMENT_UNCONSOLID = "ExperimentNotConsolidated.png";
	public static String SESSION_EXPERIMENT_CONSOLID = "ExperimentConsolidated.png";
	public static String SESSION_EXPERIMENT_ARCHIVE = "ExperimentArchive.png";
	public static String SESSION_EXPERIMENT_NOTLOADED = "ExperimentNotLoaded.png";
	public static String SESSION_EXPERIMENT_DELETE = "Delete.png";
	public static String SESSION_EXPERIMENT_EXPORT = "ExperimentExport.png";
	public static String SESSION_REPOSITORY = "Database.png";
	public static String SESSION_REPOSITORY_DELETE = "Delete.png";
	public static String SESSION_REPOSITORY_EDIT = "Preferences.png";
	
	public static String DATABASE = "Database.png";
	public static String DATABASE_NEW = "DatabaseNew.png";
	
	public static String GROUP_DATABASE_ICON = "GROUP_DataBase.png";
	public static String GROUP_DATABASE_NEW = "GROUP_DataBase_NEW.png";
	public static String GROUP_EXPERIMENT_ICON = "GROUP_Experiment.png";
	public static String GROUP_EXPERIMENT_NEW = "GROUP_Experiment_NEW.png";
	public static String EXPERIMENT_ICON = "Experiment.png";
	public static String TRAY_ICON = "Tray.png";
	
	public static String TOOL_VIEW_SELECTED = "buttonEye.png";
	public static String TOOL_VIEW_UNSELECTED = "buttonEyeUnselected.png";
	public static String TOOL_ARRAY_SELECTED = "buttonNewExperiment.png";
	public static String TOOL_ARRAY_UNSELECTED = "buttonNewExperimentUnselected.png";
	public static String TOOL_FILE_SELECTED = "buttonDistribute.png";
	public static String TOOL_FILE_UNSELECTED = "buttonDistributeUnselected.png";
	public static String TOOL_LABELS_SELECTED = "buttonLabel.png";
	public static String TOOL_LABELS_UNSELECTED = "buttonLabelUnselected.png";
	public static String TOOL_BATCH_SELECTED = "buttonBatch.png";
	public static String TOOL_BATCH_UNSELECTED = "buttonBatchUnselected.png";
	public static String TOOL_PLUGIN_SELECTED = "buttonPlugin.png";
	public static String TOOL_PLUGIN_UNSELECTED = "buttonPluginUnselected.png";
	
	public static String TOOL_FILEDISTRIB_AUTO = "FileDistribute_ToArray.png";
	public static String TOOL_FILEDISTRIB_MANU = "FileDistribute_Manual.png";
	
	public static String TOOL_VIEWLABELS = "ViewLabel.png";
	public static String TOOL_VIEWLABELS_UNSELECTED = "ViewLabelNON.png";
	public static String TOOL_VIEWOBJECTS = "ViewObject.png";
	public static String TOOL_VIEWOBJECTS_UNSELECTED = "ViewObjectNON.png";
	
	public static String ARRAY_VIEW_MODE_2D = "ArrayViewMode2DSelected.png";
	public static String ARRAY_VIEW_MODE_2D_UNSELECTED = "ArrayViewMode2D.png";
	public static String ARRAY_VIEW_MODE_1D = "ArrayViewMode1DSelected.png";
	public static String ARRAY_VIEW_MODE_1D_UNSELECTED = "ArrayViewMode1D.png";
	public static String ARRAY_VIEW_MODE_0D = "ArrayViewMode0DSelected.png";
	public static String ARRAY_VIEW_MODE_0D_UNSELECTED = "ArrayViewMode0D.png";
	public static String ARRAY_VIEW_MODE_LINK = "ArrayViewModeLinkSelected.png";
	public static String ARRAY_VIEW_MODE_LINK_UNSELECTED = "ArrayViewModeLink.png";
	
	public static String STATS_VIEW_SELECTED = "buttonEye.png";
	public static String STATS_VIEW_UNSELECTED = "buttonEyeUnselected.png";
	public static String STATS_PLOT_SELECTED = "buttonPlot.png";
	public static String STATS_PLOT_UNSELECTED = "buttonPlotUnselected.png";
	public static String STATS_ANOVA_SELECTED = "buttonAnova.png";
	public static String STATS_ANOVA_UNSELECTED = "buttonAnovaUnselected.png";
	public static String STATS_OUTPUT_SELECTED = "buttonLabel.png";
	public static String STATS_OUTPUT_UNSELECTED = "buttonLabelUnselected.png";
	
	public static String ENTRY_VALID = "Valid.png";
	public static String ENTRY_INVALID = "ValidNON.png";
	public static String ENTRY_EYE = "Eye.png";
	public static String ENTRY_NOEYE = "EyeNON.png";
	public ImageIcon eyeImage;
	public ImageIcon noeyeImage;
	public ImageIcon validImage;
	public ImageIcon invalidImage;
	
	public static String MISC_YES = "valid_true.png";
	public static String MISC_NO = "valid_false.png";
	public static String MISC_EYE = "buttonNewEye.png";
	public static String MISC_BACK = "buttonBack.png";
	public static String MISC_FWD = "buttonForward.png";
	public static String MISC_UP = "buttonUp.png";
	public static String MISC_BACK_WHITE = "buttonBackWhite.png";
	public static String MISC_FWD_WHITE = "buttonForwardWhite.png";
	public static String MISC_MINUS = "ButtonMinus.png";
	public static String MISC_PLUS = "ButtonPlus.png";
	public static String MISC_MINUS_GREY = "minusButton.png";
	public static String MISC_PLUS_GREY = "plusButton.png";
	public static String MISC_PREFERENCES = "Preferences.png";
	public static String MISC_DELETE = "Delete.png";
	public static String MISC_STAR = "Bookmark.png";
	
	public static String ROI_MOUSE_SELECTED = "ROImouseselected.png";
	public static String ROI_MOUSE_UNSELECTED = "ROImouseUNselected.png";
	
	public static final String PLUGIN_ROI_RECT1 = "Roi_Rect.png";
	public static final String PLUGIN_ROI_RECT2 = "Roi_Rect.png";
	public static final String PLUGIN_ROI_RECT3 = "Roi_Rect.png";
	public static final String PLUGIN_ROI_ELLIPSE1 = "Roi_Ellipse.png";
	public static final String PLUGIN_ROI_ELLIPSE2 = "Roi_Ellipse.png";
	public static final String PLUGIN_ROI_ELLIPSE3 = "Roi_Ellipse.png";
	public static final String PLUGIN_ROI_LINE1 = "Roi_Line.png";
	public static final String PLUGIN_ROI_LINE2 = "Roi_Line.png";
	public static final String PLUGIN_ROI_LINE3 = "Roi_Line.png";
	public static final String PLUGIN_ROI_PLINE1 = "Roi_PLine.png";
	public static final String PLUGIN_ROI_PLINE2 = "Roi_PLine.png";
	public static final String PLUGIN_ROI_PLINE3 = "Roi_PLine.png";
	public static final String PLUGIN_ROI_PGON1 = "Roi_PGon.png";
	public static final String PLUGIN_ROI_PGON2 = "Roi_PGon.png";
	public static final String PLUGIN_ROI_PGON3 = "Roi_PGon.png";
	public static final String PLUGIN_ROI_POINT1 = "Roi_Point.png";
	public static final String PLUGIN_ROI_POINT2 = "Roi_Point.png";
	public static final String PLUGIN_ROI_POINT3 = "Roi_Point.png";
	public static final String PLUGIN_ROI_DELETE1 = "Roi_Delete.png";
	public static final String PLUGIN_ROI_DELETE2 = "Roi_Delete.png";
	public static final String PLUGIN_ROI_DELETE3 = "Roi_Delete.png";
	public static final String PLUGIN_ROI_DISTRIBUTE1 = "Roi_Distribute.png";
	public static final String PLUGIN_ROI_DISTRIBUTE2 = "Roi_Distribute.png";
	public static final String PLUGIN_ROI_DISTRIBUTE3 = "Roi_Distribute_Pressed.png";
	public static final String PLUGIN_ROI_REVERT1 = "Revert.png";
	public static final String PLUGIN_ROI_REVERT2 = "Revert.png";
	public static final String PLUGIN_ROI_REVERT3 = "Revert_Pressed.png";
	public static final String PLUGIN_ROI_SAVE1 = "Save.png";
	public static final String PLUGIN_ROI_SAVE2 = "Save.png";
	public static final String PLUGIN_ROI_SAVE3 = "Save_Pressed.png";
	public static final String PLUGIN_ROI_EXPORT1 = "Export.png";
	public static final String PLUGIN_ROI_EXPORT2 = "Export.png";
	public static final String PLUGIN_ROI_EXPORT3 = "Export_Pressed.png";
	public static final String PLUGIN_ROI_VALID1 = "Valid.png";
	public static final String PLUGIN_ROI_VALID2 = "Valid.png";
	public static final String PLUGIN_ROI_VALID3 = "Valid_Pressed.png";
	public static final String PLUGIN_ROI_LIVESCROLL1 = "LiveScroll_On.png";
	public static final String PLUGIN_ROI_LIVESCROLL2 = "LiveScroll_Off.png";
	public static final String PLUGIN_ROI_LIVESCROLL3 = "LiveScroll_Pressed.png";
	
	public static final String TOGGLE_HERE = "Toggle_Here.png";
	public static final String TOGGLE_LEFT = "Toggle_Left.png";
	public static final String TOGGLE_RIGHT = "Toggle_Right.png";
	public static final String TOGGLE_ALL = "Toggle_All.png";
	
	public static final String MODE_VIEW = "Mode_View.png";
	public static final String MODE_VIEW_SELECTED = "Mode_View_Selected.png";
	public static final String MODE_MOVE = "Mode_Move.png";
	public static final String MODE_MOVE_SELECTED = "Mode_Move_Selected.png";
	public static final String MODE_CREATE = "Mode_Create.png";
	public static final String MODE_CREATE_SELECTED = "Mode_Create_Selected.png";
	public static final String MODE_SMASH = "Mode_Smash.png";
	public static final String MODE_SMASH_SELECTED = "Mode_Smash_Selected.png";
	
	public IconRepository()
	{
		
		// initialize
		this.eyeImage = this.getIconWithName(IconRepository.ENTRY_EYE, 22, 18);
		this.noeyeImage = this.getIconWithName(IconRepository.ENTRY_NOEYE, 22, 18);
		this.validImage = this.getIconWithName(IconRepository.ENTRY_VALID, 20, 20);
		this.invalidImage = this.getIconWithName(IconRepository.ENTRY_INVALID, 20, 20);
	}
	
	/**
	 * Returns the icon for a groupbutton
	 */
	public ImageIcon getIconWithName(String iconName, int width, int height)
	{
		Image scaledimage = this.getImageWithName(iconName, width, height);
		ImageIcon icon = new ImageIcon(scaledimage);
		return icon;
	}
	
	/**
	 * Returns an image of size WIDTH by HEIGHT from icon with name ICONNAME
	 * 
	 * @param iconName
	 * @param width
	 * @param height
	 * @return
	 */
	public Image getImageWithName(String iconName, int width, int height)
	{
		// URL url = this.getClass().getClassLoader().getResource("icons/" + iconName);
		URL url = IconRepository.class.getResource("icons/" + iconName);
		if(url == null)
		{
			url = IconRepository.class.getResource("/icons/" + iconName);
		}
		if(url == null)
		{
			Logs.log("Couldn't find the image: " + iconName, this);
			return null;
		}
		// Logs.log("Image: " + iconName + "  -->  " + url.toString(), this);
		Image image = Toolkit.getDefaultToolkit().getImage(url);
		Image scaledimage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
		
		return scaledimage;
	}
	
	/**
	 * Return the icon for a jexdata of typename typename
	 * 
	 * @param tn
	 * @return
	 */
	public ImageIcon getJEXDataIcon(TypeName tn, int width, int height)
	{
		String result = "";
		Type type = tn.getType();
		if(type.matches(JEXData.IMAGE) && tn.getDimension() == 1)
		{
			result = "XImageStack.png";
		}
		else if(type.matches(JEXData.IMAGE))
		{
			result = "XImage.png";
		}
		else if(type.matches(JEXData.FILE))
		{
			result = "XFile.png";
		}
		else if(type.matches(JEXData.MOVIE))
		{
			result = "XMovie.png";
		}
		else if(type.matches(JEXData.VALUE))
		{
			result = "XValue.png";
		}
		else if(type.matches(JEXData.LABEL))
		{
			result = "XLabel.png";
		}
		else if(type.matches(JEXData.ROI))
		{
			result = "XRoi.png";
		}
		else if(type.matches(JEXData.TRACK))
		{
			result = "XTrack.png";
		}
		else if(type.matches(JEXData.WORKFLOW))
		{
			result = "XFunction.png";
		}
		else
		{
			result = "XDefault.png";
		}
		
		return this.getIconWithName(result, width, height);
	}
	
	/**
	 * Returns a uniformly colored box of size WIDTH by HEIGHT and color COLOR
	 * 
	 * @param width
	 * @param height
	 * @param color
	 * @return
	 */
	public Image boxImage(int width, int height, Color color)
	{
		BufferedImage bim = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bim.createGraphics();
		
		g.setColor(color);
		g.fillRect(0, 0, bim.getWidth(), bim.getHeight());
		
		g.setColor(Color.black);
		g.drawRect(0, 0, bim.getWidth() - 1, bim.getHeight() - 1);
		
		g.dispose();
		
		return bim;
	}
	
	/**
	 * Returns a uniformly colored box of size WIDTH by HEIGHT and color COLOR
	 * 
	 * @param width
	 * @param height
	 * @param color
	 * @return
	 */
	public Icon boxIcon(int width, int height, Color color)
	{
		BufferedImage bim = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = bim.createGraphics();
		
		g.setColor(color);
		g.fillRect(0, 0, bim.getWidth(), bim.getHeight());
		
		g.setColor(Color.black);
		g.drawRect(0, 0, bim.getWidth() - 1, bim.getHeight() - 1);
		
		g.dispose();
		
		ImageIcon icon = new ImageIcon(bim);
		return icon;
	}
	
}
