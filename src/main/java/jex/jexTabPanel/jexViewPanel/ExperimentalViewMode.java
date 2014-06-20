package jex.jexTabPanel.jexViewPanel;

import guiObject.SignalMenuButton;
import icons.IconRepository;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import jex.JEXManager;
import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import signals.SSCenter;

public class ExperimentalViewMode extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	int spacing = 3;
	public SignalMenuButton view2D = new SignalMenuButton();
	public SignalMenuButton view1D = new SignalMenuButton();
	public SignalMenuButton view0D = new SignalMenuButton();
	public SignalMenuButton viewLink = new SignalMenuButton();
	
	public ExperimentalViewMode()
	{
		SSCenter.defaultCenter().connect(JEXStatics.jexManager, JEXManager.ARRAYVIEWMODE, this, "viewModeChanged", (Class[]) null);
		
		initialize();
	}
	
	private void initialize()
	{
		this.setBackground(DisplayStatics.background);
		this.setPreferredSize(new Dimension(100, 30));
		this.setMaximumSize(new Dimension(300, 30));
		this.setMinimumSize(new Dimension(100, 30));
		this.setLayout(new BorderLayout());
		
		JPanel centerPane = new JPanel();
		centerPane.setBackground(DisplayStatics.background);
		centerPane.setLayout(new BoxLayout(centerPane, BoxLayout.LINE_AXIS));
		
		Image iconLink = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_LINK_UNSELECTED, 20, 20);
		viewLink.setBackgroundColor(DisplayStatics.menuBackground);
		viewLink.setForegroundColor(Color.black);
		viewLink.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		viewLink.setImage(iconLink);
		viewLink.setMouseOverImage(iconLink);
		viewLink.setMousePressedImage(iconLink);
		viewLink.setDisabledImage(iconLink);
		viewLink.setText(null);
		viewLink.setToolTipText("Switch to tree viewing mode");
		SSCenter.defaultCenter().connect(viewLink, SignalMenuButton.SIG_ButtonClicked_NULL, this, "selectLink", (Class[]) null);
		centerPane.add(Box.createHorizontalGlue());
		centerPane.add(viewLink);
		
		Image icon0D = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_0D_UNSELECTED, 20, 20);
		view0D.setBackgroundColor(DisplayStatics.menuBackground);
		view0D.setForegroundColor(Color.black);
		view0D.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		view0D.setImage(icon0D);
		view0D.setMouseOverImage(icon0D);
		view0D.setMousePressedImage(icon0D);
		view0D.setDisabledImage(icon0D);
		view0D.setText(null);
		view0D.setToolTipText("Switch to 0D viewer viewing mode");
		SSCenter.defaultCenter().connect(view0D, SignalMenuButton.SIG_ButtonClicked_NULL, this, "select0D", (Class[]) null);
		centerPane.add(Box.createHorizontalStrut(spacing));
		centerPane.add(view0D);
		
		Image icon1D = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_1D_UNSELECTED, 20, 20);
		view1D.setBackgroundColor(DisplayStatics.menuBackground);
		view1D.setForegroundColor(Color.black);
		view1D.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		view1D.setImage(icon1D);
		view1D.setMouseOverImage(icon1D);
		view1D.setMousePressedImage(icon1D);
		view1D.setDisabledImage(icon1D);
		view1D.setText(null);
		view1D.setToolTipText("Switch to 1D list viewing mode");
		SSCenter.defaultCenter().connect(view1D, SignalMenuButton.SIG_ButtonClicked_NULL, this, "select1D", (Class[]) null);
		centerPane.add(Box.createHorizontalStrut(spacing));
		centerPane.add(view1D);
		
		Image icon2D = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_2D_UNSELECTED, 20, 20);
		view2D.setBackgroundColor(DisplayStatics.menuBackground);
		view2D.setForegroundColor(Color.black);
		view2D.setClickedColor(DisplayStatics.menuBackground, DisplayStatics.lightBackground);
		view2D.setImage(icon2D);
		view2D.setMouseOverImage(icon2D);
		view2D.setMousePressedImage(icon2D);
		view2D.setDisabledImage(icon2D);
		view2D.setText(null);
		view2D.setToolTipText("Switch to 2D array viewing mode");
		SSCenter.defaultCenter().connect(view2D, SignalMenuButton.SIG_ButtonClicked_NULL, this, "select2D", (Class[]) null);
		centerPane.add(Box.createHorizontalStrut(spacing));
		centerPane.add(view2D);
		centerPane.add(Box.createHorizontalGlue());
		
		this.add(Box.createRigidArea(new Dimension(5, 5)), BorderLayout.PAGE_START);
		this.add(centerPane, BorderLayout.CENTER);
		this.add(Box.createRigidArea(new Dimension(5, 5)), BorderLayout.PAGE_END);
	}
	
	public void select2D()
	{
		Logs.log("Switching to 2D viewing mode", 1, this);
		String viewMode = JEXStatics.jexManager.getArrayViewingMode();
		if(viewMode.equals(JEXManager.ARRAY_2D))
			return;
		JEXStatics.jexManager.setArrayViewingMode(JEXManager.ARRAY_2D);
	}
	
	public void select1D()
	{
		Logs.log("Switching to 1D viewing mode", 1, this);
		String viewMode = JEXStatics.jexManager.getArrayViewingMode();
		if(viewMode.equals(JEXManager.ARRAY_1D))
			return;
		JEXStatics.jexManager.setArrayViewingMode(JEXManager.ARRAY_1D);
	}
	
	public void select0D()
	{
		Logs.log("Switching to 0D viewing mode", 1, this);
		String viewMode = JEXStatics.jexManager.getArrayViewingMode();
		if(viewMode.equals(JEXManager.ARRAY_0D))
			return;
		JEXStatics.jexManager.setArrayViewingMode(JEXManager.ARRAY_0D);
	}
	
	public void selectLink()
	{
		Logs.log("Switching to tree viewing mode", 1, this);
		String viewMode = JEXStatics.jexManager.getArrayViewingMode();
		if(viewMode.equals(JEXManager.ARRAY_LINK))
			return;
		JEXStatics.jexManager.setArrayViewingMode(JEXManager.ARRAY_LINK);
	}
	
	public void viewModeChanged()
	{
		Image icon2D = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_2D_UNSELECTED, 20, 20);
		Image icon1D = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_1D_UNSELECTED, 20, 20);
		Image icon0D = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_0D_UNSELECTED, 20, 20);
		Image iconLink = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_LINK_UNSELECTED, 20, 20);
		view0D.setImage(icon0D);
		view1D.setImage(icon1D);
		view2D.setImage(icon2D);
		viewLink.setImage(iconLink);
		
		String newMode = JEXStatics.jexManager.getArrayViewingMode();
		if(newMode.equals(JEXManager.ARRAY_0D))
		{
			Image icon = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_0D, 20, 20);
			view0D.setImage(icon);
		}
		if(newMode.equals(JEXManager.ARRAY_1D))
		{
			Image icon = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_1D, 20, 20);
			view1D.setImage(icon);
		}
		if(newMode.equals(JEXManager.ARRAY_2D))
		{
			Image icon = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_2D, 20, 20);
			view2D.setImage(icon);
		}
		if(newMode.equals(JEXManager.ARRAY_LINK))
		{
			Image icon = JEXStatics.iconRepository.getImageWithName(IconRepository.ARRAY_VIEW_MODE_LINK, 20, 20);
			viewLink.setImage(icon);
		}
	}
}
