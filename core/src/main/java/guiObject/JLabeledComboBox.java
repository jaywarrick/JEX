package guiObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class JLabeledComboBox extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	JComboBox<String> field = new JComboBox<>();
	JLabel label = new JLabel("");
	ActionListener parent;
	
	public JLabeledComboBox(String labelName, String[] options)
	{
		this.setBackground(Color.WHITE);
		this.setLayout(new BorderLayout());
		setLabelName(labelName);
		setOptions(options);
		
		label.setMaximumSize(new Dimension(500, 20));
		label.setPreferredSize(new Dimension(100, 200));
		field.setMaximumSize(new Dimension(500, 20));
		field.setPreferredSize(new Dimension(60, 200));
		
		this.setMaximumSize(new Dimension(500, 20));
		this.setPreferredSize(new Dimension(120, 20));
		
		this.add(label, BorderLayout.LINE_START);
		this.add(field, BorderLayout.CENTER);
	}
	
	public JLabeledComboBox(String labelName, List<String> optionList)
	{
		String[] options = optionList.toArray(new String[0]);
		
		this.setBackground(Color.WHITE);
		this.setLayout(new BorderLayout());
		setLabelName(labelName);
		setOptions(options);
		
		label.setMaximumSize(new Dimension(500, 20));
		label.setPreferredSize(new Dimension(100, 200));
		field.setMaximumSize(new Dimension(500, 20));
		field.setPreferredSize(new Dimension(60, 200));
		
		this.setMaximumSize(new Dimension(500, 20));
		this.setPreferredSize(new Dimension(120, 20));
		
		this.add(label, BorderLayout.LINE_START);
		this.add(field, BorderLayout.CENTER);
	}
	
	public JComboBox<?> getElement()
	{
		return field;
	}
	
	public Object getSelectedOption()
	{
		return field.getSelectedItem();
	}
	
	public String getValue()
	{
		return field.getSelectedItem().toString();
	}
	
	public void setColor(Color color)
	{
		this.setBackground(color);
		label.setBackground(color);
		this.invalidate();
		this.validate();
	}
	
	public void setLabelName(String labelName)
	{
		label.setText(labelName);
		refresh();
	}
	
	public void setOptions(List<String> optionList)
	{
		String[] options = optionList.toArray(new String[0]);
		
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(options);
		field.setModel(model);
		refresh();
	}
	
	public void setOptions(String[] options)
	{
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(options);
		field.setModel(model);
		refresh();
	}
	
	public void setdimensions(int widthText, int totalWidth)
	{
		label.setPreferredSize(new Dimension(widthText, 20));
		field.setPreferredSize(new Dimension(totalWidth - widthText, 20));
		this.setPreferredSize(new Dimension(totalWidth, 20));
	}
	
	public void setEditable(boolean editable)
	{
		field.setEditable(editable);
	}
	
	public void refresh()
	{
		label.updateUI();
		field.updateUI();
		this.repaint();
	}
	
	public void setChangeActor(ActionListener parent)
	{
		System.out.println("   JLabeledComboBox ---> ActionListener set");
		this.parent = parent;
		field.addActionListener(this);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		parent.actionPerformed(new ActionEvent(this, 0, null));
	}
}
