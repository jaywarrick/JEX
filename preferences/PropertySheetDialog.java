package preferences;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;

/**
 * PropertySheetDialog. <br>
 * 
 */
public class PropertySheetDialog extends BaseDialog {
	
	private static final long serialVersionUID = 1L;
	
	public PropertySheetDialog() throws HeadlessException
	{
		super();
	}
	
	public PropertySheetDialog(Dialog owner) throws HeadlessException
	{
		super(owner);
	}
	
	public PropertySheetDialog(Dialog owner, boolean modal) throws HeadlessException
	{
		super(owner, modal);
	}
	
	public PropertySheetDialog(Frame owner) throws HeadlessException
	{
		super(owner);
	}
	
	public PropertySheetDialog(Frame owner, boolean modal) throws HeadlessException
	{
		super(owner, modal);
	}
	
	public PropertySheetDialog(Dialog owner, String title) throws HeadlessException
	{
		super(owner, title);
	}
	
	public PropertySheetDialog(Dialog owner, String title, boolean modal) throws HeadlessException
	{
		super(owner, title, modal);
	}
	
	public PropertySheetDialog(Frame owner, String title) throws HeadlessException
	{
		super(owner, title);
	}
	
	public PropertySheetDialog(Frame owner, String title, boolean modal) throws HeadlessException
	{
		super(owner, title, modal);
	}
	
	public PropertySheetDialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) throws HeadlessException
	{
		super(owner, title, modal, gc);
	}
	
	public PropertySheetDialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc)
	{
		super(owner, title, modal, gc);
	}
	
}
