package jex.jexTabPanel.jexDistributionPanel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jex.statics.DisplayStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;

public class DimensionSelector extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private Color foregroundColor = DisplayStatics.lightBackground;
	public String name;
	public int num;
	public String[] possibilities;
	public int sizeOfDimension = 1;
	JLabel numLabel;
	JLabel nameLabel;
	JComboBox values;
	JTextField sizeField;
	
	DimensionSelector(int num, String name, String[] possibilities)
	{
		this.num = num;
		this.name = "" + num + ". " + name;
		this.possibilities = possibilities;
		initialize();
	}
	
	private void initialize()
	{
		this.setLayout(new MigLayout("flowx, ins 0", "[0:0,15]3[grow,fill]3[]3[0:0,40]", "[]"));
		this.setBackground(foregroundColor);
		
		nameLabel = new JLabel(name);
		nameLabel.setBackground(foregroundColor);
		values = new JComboBox(possibilities);
		values.setEditable(true);
		values.setBackground(foregroundColor);
		values.addActionListener(this);
		values.setSelectedIndex(num - 1);
		sizeField = new JTextField(100);
		// sizeField.setPreferredSize(new Dimension(15,
		// sizeField.getPreferredSize().height));
		// sizeField.setMaximumSize(new Dimension(15,
		// sizeField.getPreferredSize().height));
		Logs.log("Size of sizeField " + sizeField.getMaximumSize(), 0, this);
		sizeField.setText("" + sizeOfDimension);
		sizeField.setBackground(foregroundColor);
		JLabel temp = new JLabel("of size");
		temp.setBackground(foregroundColor);
		
		// this.setMaximumSize(new Dimension(250,20));
		// this.add(Box.createHorizontalGlue());
		this.add(nameLabel);
		this.add(values, "growx");
		this.add(temp);
		this.add(sizeField);
		// this.add(Box.createHorizontalGlue());
	}
	
	public String getDimensionName()
	{
		String result = values.getSelectedItem().toString();
		return result;
	}
	
	public int getDimensionSize()
	{
		String s = sizeField.getText();
		Integer i = Integer.parseInt(s);
		return i;
	}
	
	public void setRestrictedPossibilityList(List<String> removeThesePossibilities)
	{   
		
	}
	
	public void actionPerformed(ActionEvent e)
	{}
}