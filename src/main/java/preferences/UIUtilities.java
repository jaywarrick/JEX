package preferences;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

/**
 * UIUtilities. <br>
 * 
 */
public class UIUtilities {
	
	public static void centerOnScreen(Window window)
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension size = window.getSize();
		window.setLocation((screenSize.width - size.width) / 2, (screenSize.height - size.height) / 2);
	}
	
}
