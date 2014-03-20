package guiObject;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import logs.Logs;

public class PopupListener extends MouseAdapter {
	
	JPopupMenu popup;
	
	public PopupListener(JPopupMenu popupMenu)
	{
		popup = popupMenu;
	}
	
	@Override
	public void mousePressed(MouseEvent e)
	{
		Logs.log("Mouse pressed", 1, this);
		maybeShowPopup(e);
	}
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
		Logs.log("Mouse released", 1, this);
		maybeShowPopup(e);
	}
	
	private void maybeShowPopup(MouseEvent e)
	{
		popup.show(e.getComponent(), e.getX(), e.getY());
	}
}