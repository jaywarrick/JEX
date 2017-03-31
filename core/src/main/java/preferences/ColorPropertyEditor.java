package preferences;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

/**
 * ColorPropertyEditor. <br>
 * 
 */
public class ColorPropertyEditor extends AbstractPropertyEditor<JPanel> {
	
	private ColorCellRenderer label;
	private JButton button;
	private Color color;
	
	public ColorPropertyEditor()
	{
		editor = new JPanel(new BorderLayout(0, 0));
		editor.add("Center", label = new ColorCellRenderer());
		label.setOpaque(false);
		editor.add("East", button = new FixedButton());
		button.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e)
			{
				selectColor();
			}
		});
		editor.setOpaque(false);
	}
	
	@Override
	public Object getValue()
	{
		return color;
	}
	
	@Override
	public void setValue(Object value)
	{
		color = (Color) value;
		label.setValue(color);
	}
	
	protected void selectColor()
	{
		ResourceManager rm = ResourceManager.all(FilePropertyEditor.class);
		String title = rm.getString("ColorPropertyEditor.title");
		Color selectedColor = JColorChooser.showDialog(editor, title, color);
		
		if(selectedColor != null)
		{
			Color oldColor = color;
			Color newColor = selectedColor;
			label.setValue(newColor);
			color = newColor;
			firePropertyChange(oldColor, newColor);
		}
	}
	
}
