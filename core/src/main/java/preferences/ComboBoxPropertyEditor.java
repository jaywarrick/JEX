package preferences;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * ComboBoxPropertyEditor. <br>
 * 
 */
public class ComboBoxPropertyEditor extends AbstractPropertyEditor<JComboBox<Object>> {
	
	private Object oldValue;
	private Icon[] icons;
	
	public ComboBoxPropertyEditor()
	{
		editor = new JComboBox<Object>() {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			public void setSelectedItem(Object anObject)
			{
				oldValue = getSelectedItem();
				super.setSelectedItem(anObject);
			}
		};

		final JComboBox<Object> combo = (JComboBox<Object>) editor;
		
		combo.setRenderer(new Renderer());
		combo.addPopupMenuListener(new PopupMenuListener() {
			
			public void popupMenuCanceled(PopupMenuEvent e)
			{}
			
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
				ComboBoxPropertyEditor.this.firePropertyChange(oldValue, combo.getSelectedItem());
			}
			
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{}
		});
		combo.addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e)
			{
				if(e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					ComboBoxPropertyEditor.this.firePropertyChange(oldValue, combo.getSelectedItem());
				}
			}
		});
	}
	
	@Override
	public Object getValue()
	{
		Object selected = editor.getSelectedItem();
		if(selected instanceof Value)
		{
			return ((Value) selected).value;
		}
		else
		{
			return selected;
		}
	}
	
	@Override
	public void setValue(Object value)
	{
		JComboBox<Object> combo = editor;
		Object current = null;
		int index = -1;
		for (int i = 0, c = combo.getModel().getSize(); i < c; i++)
		{
			current = combo.getModel().getElementAt(i);
			if(value == current || (current != null && current.equals(value)))
			{
				index = i;
				break;
			}
		}
		editor.setSelectedIndex(index);
	}
	
	public void setAvailableValues(Object[] values)
	{
		editor.setModel(new DefaultComboBoxModel<Object>(values));
	}
	
	public void setAvailableIcons(Icon[] icons)
	{
		this.icons = icons;
	}
	
	public class Renderer extends DefaultListCellRenderer {
		
		private static final long serialVersionUID = 1L;
		
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			Component component = super.getListCellRendererComponent(list, (value instanceof Value) ? ((Value) value).visualValue : value, index, isSelected, cellHasFocus);
			if(icons != null && index >= 0 && component instanceof JLabel)
				((JLabel) component).setIcon(icons[index]);
			return component;
		}
	}
	
	public static final class Value {
		
		private Object value;
		private Object visualValue;
		
		public Value(Object value, Object visualValue)
		{
			this.value = value;
			this.visualValue = visualValue;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if(o == this)
				return true;
			if(value == o || (value != null && value.equals(o)))
				return true;
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return value == null ? 0 : value.hashCode();
		}
	}
}
