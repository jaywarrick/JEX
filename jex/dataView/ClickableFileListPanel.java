package jex.dataView;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JList;
import javax.swing.JPanel;

import miscellaneous.FileUtility;

public class ClickableFileListPanel extends JPanel implements MouseListener {
	
	private static final long serialVersionUID = 1L;
	
	@Override
	public void mouseClicked(MouseEvent evt)
	{
		JList list = (JList) evt.getSource();
		if(evt.getClickCount() == 2)
		{
			int index = list.locationToIndex(evt.getPoint());
			Object file = list.getModel().getElementAt(index);
			if(file != null)
			{
				if(file instanceof File)
				{
					try
					{
						FileUtility.openFileDefaultApplication((File) file);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				
			}
		}
	}
	
	@Override
	public void mouseEntered(MouseEvent arg0)
	{}
	
	@Override
	public void mouseExited(MouseEvent arg0)
	{}
	
	@Override
	public void mousePressed(MouseEvent arg0)
	{}
	
	@Override
	public void mouseReleased(MouseEvent arg0)
	{}
	
}
