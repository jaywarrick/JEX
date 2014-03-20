package plugins.viewer;

import guiObject.SignalMenuButton;
import icons.IconRepository;

import java.awt.Dimension;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import signals.SSCenter;

public class EntryMenu {
	
	private JPanel panel;
	private int spacing = 4;
	private int iconSize = 25;
	private Image greyBox = JEXStatics.iconRepository.boxImage(iconSize, iconSize, DisplayStatics.dividerColor);
	
	public static final String SIG_Save_NULL = "SIG_Save_NULL";
	public static final String SIG_Revert_NULL = "SIG_Revert_NULL";
	public static final String SIG_Export_NULL = "SIG_Export_NULL";
	public static final String SIG_Distribute_NULL = "SIG_Distribute_NULL";
	public static final String SIG_ToggleValid_NULL = "SIG_ToggleValid_NULL";
	public static final String SIG_ToggleLiveScroll_NULL = "SIG_ToggleLiveScroll_NULL";
	
	private SignalMenuButton valid, liveScroll;
	private Icon valid_icon, invalid_icon;
	private Icon liveScrollOn_icon, liveScrollOff_icon;
	
	public EntryMenu()
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
		this.valid = new SignalMenuButton();
		this.liveScroll = new SignalMenuButton();
		
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_SAVE1, IconRepository.PLUGIN_ROI_SAVE2, IconRepository.PLUGIN_ROI_SAVE3, "saveClicked", "save");
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_REVERT1, IconRepository.PLUGIN_ROI_REVERT2, IconRepository.PLUGIN_ROI_REVERT3, "revertClicked", "revert");
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_DISTRIBUTE1, IconRepository.PLUGIN_ROI_DISTRIBUTE2, IconRepository.PLUGIN_ROI_DISTRIBUTE3, "distributeClicked", "distribute roi");
		// this.panel.add(Box.createHorizontalStrut(10));
		// this.panel.add(Box.createHorizontalGlue());
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_EXPORT1, IconRepository.PLUGIN_ROI_EXPORT2, IconRepository.PLUGIN_ROI_EXPORT3, "exportClicked", "export new image object");
		this.intializeButton(liveScroll, IconRepository.PLUGIN_ROI_LIVESCROLL1, IconRepository.PLUGIN_ROI_LIVESCROLL1, IconRepository.PLUGIN_ROI_LIVESCROLL3, "liveScrollClicked", "toggle live scrolling");
		this.intializeButton(valid, IconRepository.ENTRY_VALID, IconRepository.ENTRY_VALID, IconRepository.ENTRY_INVALID, "toggleValidClicked", "toggle entry valid");
		
		this.valid_icon = this.valid.icon();
		this.invalid_icon = this.valid.mousePressedIcon();
		this.liveScrollOn_icon = this.liveScroll.icon();
		this.liveScrollOff_icon = new ImageIcon(JEXStatics.iconRepository.getImageWithName(IconRepository.PLUGIN_ROI_LIVESCROLL2, iconSize, iconSize));
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
	
	public void saveClicked()
	{
		Logs.log("Save", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_Save_NULL, (Object[]) null);
	}
	
	public void revertClicked()
	{
		Logs.log("Revert", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_Revert_NULL, (Object[]) null);
	}
	
	public void exportClicked()
	{
		Logs.log("Export", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_Export_NULL, (Object[]) null);
	}
	
	public void distributeClicked()
	{
		Logs.log("Distribute", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_Distribute_NULL, (Object[]) null);
	}
	
	public void liveScrollClicked()
	{
		Logs.log("Toggle Live Scrolling", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_ToggleLiveScroll_NULL, (Object[]) null);
	}
	
	public void toggleValidClicked()
	{
		Logs.log("Toggle entry valid", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_ToggleValid_NULL, (Object[]) null);
	}
	
	public void setValid(boolean valid)
	{
		if(valid)
		{
			this.valid.setIcon(valid_icon);
			this.valid.setMouseOverIcon(valid_icon);
		}
		else
		{
			this.valid.setIcon(invalid_icon);
			this.valid.setMouseOverIcon(invalid_icon);
		}
		this.panel.repaint();
	}
	
	public void setLiveScroll(boolean liveScroll)
	{
		if(liveScroll)
		{
			this.liveScroll.setIcon(liveScrollOn_icon);
			this.liveScroll.setMouseOverIcon(liveScrollOn_icon);
		}
		else
		{
			this.liveScroll.setIcon(liveScrollOff_icon);
			this.liveScroll.setMouseOverIcon(liveScrollOff_icon);
		}
		this.panel.repaint();
	}
	
}
