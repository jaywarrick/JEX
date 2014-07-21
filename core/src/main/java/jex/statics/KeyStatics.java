package jex.statics;

import java.awt.event.KeyEvent;

import logs.Logs;
import signals.SSCenter;

public class KeyStatics {
	
	private static boolean altDown, metaDown, shiftDown, ctrlDown, tabDown, escapeDown, ctrl_cmd_Down;
	
	private static final String SIG_EscapePressed_NULL = "SIG_EscapePressed_NULL";
	
	public static void captureModifiers(KeyEvent e)
	{
		if(e.getID() == KeyEvent.KEY_PRESSED)
		{
			KeyStatics.setModifierDown(e.getKeyCode());
		}
		else if(e.getID() == KeyEvent.KEY_RELEASED)
		{
			KeyStatics.setModifierUp(e.getKeyCode());
		}
	}
	
	public static void setModifierDown(int key)
	{
		switch (key)
		{
			case KeyEvent.VK_ALT:
			{
				altDown = true;
				break;
			}
			case KeyEvent.VK_META:
			{
				metaDown = true;
				ctrl_cmd_Down = true;
				break;
			}
			case KeyEvent.VK_SHIFT:
			{
				shiftDown = true;
				break;
			}
			case KeyEvent.VK_CONTROL:
			{
				ctrlDown = true;
				ctrl_cmd_Down = true;
				break;
			}
			case KeyEvent.VK_TAB:
			{
				tabDown = true;
				break;
			}
			case KeyEvent.VK_ESCAPE:
			{
				escapeDown = true;
				SSCenter.defaultCenter().emit(KeyStatics.class, SIG_EscapePressed_NULL, (Object[]) null);
				break;
			}
		}
		// printModifiers();
	}
	
	public static void setModifierUp(int key)
	{
		switch (key)
		{
			case KeyEvent.VK_ALT:
			{
				altDown = false;
				break;
			}
			case KeyEvent.VK_META:
			{
				metaDown = false;
				if(!ctrlDown)
					ctrl_cmd_Down = false;
				break;
			}
			case KeyEvent.VK_SHIFT:
			{
				shiftDown = false;
				break;
			}
			case KeyEvent.VK_CONTROL:
			{
				ctrlDown = false;
				if(!metaDown)
					ctrl_cmd_Down = false;
				break;
			}
			case KeyEvent.VK_TAB:
			{
				tabDown = false;
				break;
			}
			case KeyEvent.VK_ESCAPE:
			{
				escapeDown = false;
				break;
			}
		}
	}
	
	/** Returns true if the alt key is down. */
	public static boolean altIsDown()
	{
		return altDown;
	}
	
	/** Returns true if the meta/command key is down */
	public static boolean metaIsDown()
	{
		return metaDown;
	}
	
	/** Returns true if the shift key is down. */
	public static boolean shiftIsDown()
	{
		return shiftDown;
	}
	
	/** Returns true if the alt key is down. */
	public static boolean ctrlIsDown()
	{
		return ctrlDown;
	}
	
	/** Returns true if the alt key is down. */
	public static boolean tabIsDown()
	{
		return tabDown;
	}
	
	/** Returns true if the escape key is down */
	public static boolean escapeIsDown()
	{
		return escapeDown;
	}
	
	/** Returns true if control or command is down */
	public static boolean ctrlOrCmdIsDown()
	{
		return ctrl_cmd_Down;
	}
	
	public static void printModifiers()
	{
		Logs.log("AltDown:" + altIsDown() + ", MetaDown:" + metaIsDown() + ", ShiftDown:" + shiftIsDown() + ", CtrlDown:" + ctrlIsDown() + ", TabDown:" + tabIsDown() + ", EscapeDown:" + escapeIsDown() + ", CtrlOrCmdDown:" + ctrlOrCmdIsDown(), 0, KeyStatics.class);
	}
}
