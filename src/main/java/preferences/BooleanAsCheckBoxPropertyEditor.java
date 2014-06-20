package preferences;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;

/**
 * BooleanAsCheckBoxPropertyEditor. <br>
 * 
 */
public class BooleanAsCheckBoxPropertyEditor extends AbstractPropertyEditor {
	
	public BooleanAsCheckBoxPropertyEditor()
	{
		editor = new JCheckBox();
		((JCheckBox) editor).setOpaque(false);
		((JCheckBox) editor).addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e)
			{
				firePropertyChange(((JCheckBox) editor).isSelected() ? Boolean.FALSE : Boolean.TRUE, ((JCheckBox) editor).isSelected() ? Boolean.TRUE : Boolean.FALSE);
				((JCheckBox) editor).transferFocus();
			}
		});
	}
	
	@Override
	public Object getValue()
	{
		return ((JCheckBox) editor).isSelected() ? Boolean.TRUE : Boolean.FALSE;
	}
	
	@Override
	public void setValue(Object value)
	{
		((JCheckBox) editor).setSelected(Boolean.TRUE.equals(value));
	}
	
}
