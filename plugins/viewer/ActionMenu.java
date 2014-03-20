package plugins.viewer;

import guiObject.SignalMenuButton;
import icons.IconRepository;

import java.awt.Dimension;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import signals.SSCenter;

public class ActionMenu {
	
	private JPanel panel;
	private int spacing = 4;
	private int iconSize = 25;
	private Image greyBox = JEXStatics.iconRepository.boxImage(iconSize, iconSize, DisplayStatics.dividerColor);
	private Icon view, viewPressed, move, movePressed, create, createPressed, smash, smashPressed;
	
	public static final String SIG_SetMode_intType = "SIG_SetMode_intType";
	public static final String SIG_DeleteRoi_NULL = "SIG_DeleteRoi_NULL";
	// public static final String SIG_Save_NULL = "SIG_Save_NULL";
	// public static final String SIG_Revert_NULL = "SIG_Revert_NULL";
	// public static final String SIG_Export_NULL = "SIG_Export_NULL";
	public static final String SIG_ToggleSmashMode_NULL = "SIG_ToggleSmashMode_NULL";
	// public static final String SIG_Distribute_NULL = "SIG_Distribute_NULL";
	
	private SignalMenuButton mode_view, mode_move, mode_create, mode_smash;
	
	public ActionMenu()
	{
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.menuBackground);
		this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.LINE_AXIS));
		this.initializeButtons();
		this.panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, iconSize));
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void initializeButtons()
	{
		this.mode_view = new SignalMenuButton();
		this.mode_move = new SignalMenuButton();
		this.mode_create = new SignalMenuButton();
		this.mode_smash = new SignalMenuButton();
		
		this.intializeButton(mode_view, IconRepository.MODE_VIEW, IconRepository.MODE_VIEW, IconRepository.MODE_VIEW_SELECTED, "viewModeClicked", "view");
		this.intializeButton(mode_move, IconRepository.MODE_MOVE, IconRepository.MODE_MOVE, IconRepository.MODE_MOVE_SELECTED, "moveModeClicked", "move");
		this.intializeButton(mode_create, IconRepository.MODE_CREATE, IconRepository.MODE_CREATE, IconRepository.MODE_CREATE_SELECTED, "createModeClicked", "create");
		this.intializeButton(mode_smash, IconRepository.MODE_SMASH, IconRepository.MODE_SMASH, IconRepository.MODE_SMASH_SELECTED, "smashModeToggled", "projection view");
		this.panel.add(Box.createHorizontalStrut(10));
		this.panel.add(Box.createHorizontalGlue());
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_DELETE1, IconRepository.PLUGIN_ROI_DELETE2, IconRepository.PLUGIN_ROI_DELETE3, "delete", "Delete");
		// this.intializeButton(new SignalMenuButton(),
		// IconRepository.PLUGIN_ROI_SAVE1, IconRepository.PLUGIN_ROI_SAVE2,
		// IconRepository.PLUGIN_ROI_SAVE3, "saveClicked", "save");
		// this.intializeButton(new SignalMenuButton(),
		// IconRepository.PLUGIN_ROI_REVERT1, IconRepository.PLUGIN_ROI_REVERT2,
		// IconRepository.PLUGIN_ROI_REVERT3, "revertClicked", "revert");
		// this.intializeButton(new SignalMenuButton(),
		// IconRepository.PLUGIN_ROI_EXPORT1, IconRepository.PLUGIN_ROI_EXPORT2,
		// IconRepository.PLUGIN_ROI_EXPORT3, "exportClicked",
		// "export new image object");
		// this.intializeButton(new SignalMenuButton(),
		// IconRepository.PLUGIN_ROI_DISTRIBUTE1,
		// IconRepository.PLUGIN_ROI_DISTRIBUTE2,
		// IconRepository.PLUGIN_ROI_DISTRIBUTE3, "distributeClicked",
		// "distribute roi");
		
		this.create = this.mode_create.icon();
		this.createPressed = this.mode_create.mousePressedIcon();
		this.view = this.mode_view.icon();
		this.viewPressed = this.mode_view.mousePressedIcon();
		this.move = this.mode_move.icon();
		this.movePressed = this.mode_move.mousePressedIcon();
		this.smash = this.mode_smash.icon();
		this.smashPressed = this.mode_smash.mousePressedIcon();
		
	}
	
	public void intializeButton(SignalMenuButton b, String icon1, String icon2, String icon3, String action, String name)
	{
		Image icon = JEXStatics.iconRepository.getImageWithName(icon1, iconSize, iconSize);
		Image iconOver = JEXStatics.iconRepository.getImageWithName(icon2, iconSize, iconSize);
		Image iconPressed = JEXStatics.iconRepository.getImageWithName(icon3, iconSize, iconSize);
		b.setBackgroundColor(DisplayStatics.menuBackground);
		b.setForegroundColor(DisplayStatics.menuBackground);
		b.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		b.setSize(new Dimension(iconSize, iconSize));
		b.setImage(icon);
		b.setMouseOverImage(iconOver);
		b.setMousePressedImage(iconPressed);
		b.setDisabledImage(greyBox);
		b.setToolTipText(name);
		// b.setText(name);
		b.setLabelFont(FontUtility.defaultFontl);
		SSCenter.defaultCenter().connect(b, SignalMenuButton.SIG_ButtonClicked_NULL, this, action, (Class[]) null);
		this.panel.add(Box.createHorizontalStrut(spacing));
		this.panel.add(b);
	}
	
	public void viewModeClicked()
	{
		Logs.log("Set View Mode", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_SetMode_intType, new Object[] { new Integer(ImageDisplayController.MODE_VIEW) });
	}
	
	public void moveModeClicked()
	{
		Logs.log("Set Move Mode", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_SetMode_intType, new Object[] { new Integer(ImageDisplayController.MODE_MOVE) });
	}
	
	public void createModeClicked()
	{
		Logs.log("Set Create Mode", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_SetMode_intType, new Object[] { new Integer(ImageDisplayController.MODE_CREATE) });
	}
	
	public void smashModeToggled()
	{
		Logs.log("Toggle Smash Mode", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_ToggleSmashMode_NULL, (Object[]) null);
	}
	
	public void delete()
	{
		Logs.log("delete", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_DeleteRoi_NULL, (Object[]) null);
	}
	
	// public void saveClicked()
	// {
	// Logs.log("Save", 0, this);
	// SSCenter.defaultCenter().emit(this, SIG_Save_NULL, (Object[])null);
	// }
	//
	// public void revertClicked()
	// {
	// Logs.log("Revert", 0, this);
	// SSCenter.defaultCenter().emit(this, SIG_Revert_NULL, (Object[])null);
	// }
	//
	// public void exportClicked()
	// {
	// Logs.log("Export", 0, this);
	// SSCenter.defaultCenter().emit(this, SIG_Export_NULL, (Object[])null);
	// }
	//
	// public void distributeClicked()
	// {
	// Logs.log("Distribute", 0, this);
	// SSCenter.defaultCenter().emit(this, SIG_Distribute_NULL, (Object[])null);
	// }
	
	public void setMode(Integer mode)
	{
		if(mode != ImageDisplayController.MODE_CREATE)
		{
			this.mode_create.setIcon(create);
			this.mode_create.setMouseOverIcon(create);
		}
		else
		{
			this.mode_create.setIcon(createPressed);
			this.mode_create.setMouseOverIcon(createPressed);
		}
		
		if(mode != ImageDisplayController.MODE_MOVE)
		{
			this.mode_move.setIcon(move);
			this.mode_move.setMouseOverIcon(move);
		}
		else
		{
			this.mode_move.setIcon(movePressed);
			this.mode_move.setMouseOverIcon(movePressed);
		}
		
		if(mode != ImageDisplayController.MODE_VIEW)
		{
			this.mode_view.setIcon(view);
			this.mode_view.setMouseOverIcon(view);
		}
		else
		{
			this.mode_view.setIcon(viewPressed);
			this.mode_view.setMouseOverIcon(viewPressed);
		}
	}
	
	public void setToggled(boolean on)
	{
		if(!on)
		{
			this.mode_smash.setIcon(smash);
			this.mode_smash.setMouseOverIcon(smash);
		}
		else
		{
			this.mode_smash.setIcon(smashPressed);
			this.mode_smash.setMouseOverIcon(smashPressed);
		}
	}
}
