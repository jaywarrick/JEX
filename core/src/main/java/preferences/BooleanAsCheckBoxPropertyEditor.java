package preferences;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

/**
 * BooleanAsCheckBoxPropertyEditor. <br>
 * 
 */
public class BooleanAsCheckBoxPropertyEditor extends AbstractPropertyEditor<JCheckBox> {
	
	public BooleanAsCheckBoxPropertyEditor()
	{
		editor = new JCheckBox();
		editor.setOpaque(false);
		editor.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e)
			{
				firePropertyChange(editor.isSelected() ? Boolean.FALSE : Boolean.TRUE, editor.isSelected() ? Boolean.TRUE : Boolean.FALSE);
				editor.transferFocus();
			}
		});
	}
	
	@Override
	public Object getValue()
	{
		return editor.isSelected() ? Boolean.TRUE : Boolean.FALSE;
	}
	
	@Override
	public void setValue(Object value)
	{
		editor.setSelected(Boolean.TRUE.equals(value));
	}
	
}
