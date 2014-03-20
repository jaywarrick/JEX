package jex.arrayView;

import java.awt.Color;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;

public abstract class ArrayCellPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	
	protected Color background = DisplayStatics.lightBackground;
	protected Color temporaryGround = DisplayStatics.lightBackground;
	
	public ArrayCellController controller;
	
	public ArrayCellPanel(ArrayCellController controller)
	{
		this.controller = controller;
	}
	
	public ArrayCellController controller()
	{
		return controller;
	}
	
	public void setController(ArrayCellController controller)
	{
		this.controller = controller;
	}
	
	public abstract void rebuild();
}
