package preferences;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.UIManager;

/**
 * A button with a fixed size to workaround bugs in OSX. Submitted by Hani Suleiman. Hani uses an icon for the ellipsis, I've decided to hardcode the dimension to 16x30 but only on Mac OS X.
 */
public final class FixedButton extends JButton {
	
	private static final long serialVersionUID = 1L;
	
	public FixedButton()
	{
		super("...");
		
		if(OS.isMacOSX() && UIManager.getLookAndFeel().isNativeLookAndFeel())
		{
			setPreferredSize(new Dimension(16, 30));
		}
		
		setMargin(new Insets(0, 0, 0, 0));
	}
	
}
