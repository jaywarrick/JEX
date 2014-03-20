package plugins.viewer;

import guiObject.SignalMenuButton;
import icons.IconRepository;
import image.roi.ROIPlus;

import java.awt.Dimension;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import signals.SSCenter;

public class RoiMenu {
	
	private JPanel panel;
	private int spacing = 4;
	private int iconSize = 25;
	private Image greyBox = JEXStatics.iconRepository.boxImage(iconSize, iconSize, DisplayStatics.dividerColor);
	
	public static final String SIG_AddRoi_intType = "SIG_AddRoi_intType";
	
	public RoiMenu()
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
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_RECT1, IconRepository.PLUGIN_ROI_RECT2, IconRepository.PLUGIN_ROI_RECT3, "newRect", "Rect");
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_ELLIPSE1, IconRepository.PLUGIN_ROI_ELLIPSE2, IconRepository.PLUGIN_ROI_ELLIPSE3, "newEllipse", "Ellipse");
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_PGON1, IconRepository.PLUGIN_ROI_PGON2, IconRepository.PLUGIN_ROI_PGON3, "newPGon", "P-Gon");
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_LINE1, IconRepository.PLUGIN_ROI_LINE2, IconRepository.PLUGIN_ROI_LINE3, "newLine", "Line");
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_PLINE1, IconRepository.PLUGIN_ROI_PLINE2, IconRepository.PLUGIN_ROI_PLINE3, "newPLine", "P-Line");
		this.intializeButton(new SignalMenuButton(), IconRepository.PLUGIN_ROI_POINT1, IconRepository.PLUGIN_ROI_POINT2, IconRepository.PLUGIN_ROI_POINT3, "newPoint", "Point");
		
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
	
	public void newRect()
	{
		Logs.log("newRect", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_AddRoi_intType, new Object[] { new Integer(ROIPlus.ROI_RECT) });
	}
	
	public void newEllipse()
	{
		Logs.log("newEllipse", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_AddRoi_intType, new Object[] { new Integer(ROIPlus.ROI_ELLIPSE) });
	}
	
	public void newLine()
	{
		Logs.log("newLine", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_AddRoi_intType, new Object[] { new Integer(ROIPlus.ROI_LINE) });
	}
	
	public void newPLine()
	{
		Logs.log("newPLine", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_AddRoi_intType, new Object[] { new Integer(ROIPlus.ROI_POLYLINE) });
	}
	
	public void newPGon()
	{
		Logs.log("newPGon", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_AddRoi_intType, new Object[] { new Integer(ROIPlus.ROI_POLYGON) });
	}
	
	public void newPoint()
	{
		Logs.log("newPoint", 0, this);
		SSCenter.defaultCenter().emit(this, SIG_AddRoi_intType, new Object[] { new Integer(ROIPlus.ROI_POINT) });
	}
}
