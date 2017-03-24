package guiObject;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class JTickedComponent extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	public static final String SIG_SelectionChanged_NULL = "SIG_SelectionChanged_NULL";
	
	// GUI variables
	JCheckBox tick = new JCheckBox("");
	JComponent component;
	
	// Interior variables
	String type = "Default";
	
	public JTickedComponent(String label, JButton button)
	{
		this.component = button;
		this.tick.setText(label);
		this.type = button.getClass().getName();
		
		initialize();
	}
	
	public JTickedComponent(String label, JComponent button)
	{
		this.component = button;
		this.tick.setText(label);
		this.type = button.getClass().getSimpleName();
		
		initialize();
	}
	
	public void initialize()
	{
		this.setBackground(Color.WHITE);
		this.setLayout(new MigLayout("flowx,ins 0", "[]5[fill,grow]", "[]"));
		// this.setLayout(new FlowLayout());
		// this.setMaximumSize(new Dimension(200,20));
		// this.setPreferredSize(new Dimension(200,20));
		
		// tick.setMaximumSize(new Dimension(100,20));
		// tick.setPreferredSize(new Dimension(100,20));
		// component.setMaximumSize(new Dimension(100,20));
		// component.setPreferredSize(new Dimension(100,20));
		
		this.add(tick, "width 20, height 20");
		this.add(component, "growx");
		this.tick.addActionListener(this);
	}
	
	public void setSelected(boolean checked)
	{
		this.tick.setSelected(checked);
	}
	
	/**
	 * Return the component
	 * 
	 * @return the component besides the tick
	 */
	public JComponent getComponent()
	{
		return component;
	}
	
	/**
	 * Return the result in the component
	 * 
	 * @return String value of the componement
	 */
	public String getValue()
	{
		String result = null;
		
		if(type.equals("JTextField"))
		{
			result = ((JTextField) component).getText();
		}
		if(type.equals("JComboBox"))
		{
			result = ((JComboBox<?>) component).getSelectedItem().toString();
		}
		if(type.equals("JButton"))
		{
			result = ((JButton) component).getText();
		}
		
		return result;
	}
	
	/**
	 * Return true if the tick is selected
	 * 
	 * @return true if selected
	 */
	public boolean isTicked()
	{
		boolean result = tick.isSelected();
		return result;
	}
	
	public void setLabelName(String labelName)
	{
		tick.setText(labelName);
		refresh();
	}
	
	public void setComponent(JComponent component)
	{
		this.component = component;
		refresh();
	}
	
	public void refresh()
	{
		tick.updateUI();
		component.updateUI();
		this.repaint();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.tick)
		{
			SSCenter.defaultCenter().emit(this, SIG_SelectionChanged_NULL, (Object[]) null);
		}
	}
}
