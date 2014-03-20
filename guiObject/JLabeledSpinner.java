package guiObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;

public class JLabeledSpinner extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private JSpinner spinner = new JSpinner();
	JLabel label = new JLabel("");
	
	public JLabeledSpinner(String labelStr, SpinnerModel model)
	{
		this.setBackground(Color.WHITE);
		this.setLayout(new BorderLayout());
		setLabelName(labelStr);
		setSpinnerModel(model);
		
		label.setMaximumSize(new Dimension(500, 20));
		label.setPreferredSize(new Dimension(70, 200));
		spinner.setMaximumSize(new Dimension(500, 20));
		spinner.setPreferredSize(new Dimension(50, 200));
		
		this.setMaximumSize(new Dimension(500, 20));
		this.setPreferredSize(new Dimension(100, 20));
		
		this.add(label, BorderLayout.LINE_START);
		this.add(spinner, BorderLayout.CENTER);
	}
	
	public SpinnerModel getSpinnerModel()
	{
		return spinner.getModel();
	}
	
	public void setLabelName(String labelName)
	{
		label.setText(labelName);
		refresh();
	}
	
	public void setSpinnerModel(SpinnerModel model)
	{
		spinner.setModel(model);
		refresh();
	}
	
	public String getText()
	{
		return spinner.getModel().getValue().toString();
	}
	
	public void refresh()
	{
		label.updateUI();
		spinner.updateUI();
		this.repaint();
	}
}