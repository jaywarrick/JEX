package guiObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class JLabeledButton extends JPanel {
	
	private static final long serialVersionUID = 1L;
	JLabel label = new JLabel("");
	JButton button = new JButton();
	
	public JLabeledButton(String labelName, JButton button)
	{
		this.button = button;
		button.setText(null);
		this.setBackground(Color.WHITE);
		this.setLayout(new FlowLayout(FlowLayout.LEFT));
		setLabelName(labelName);
		button.setAlignmentY(TOP_ALIGNMENT);
		// this.setMaximumSize(new Dimension(500,20));
		
		this.add(button, BorderLayout.LINE_START);
		this.add(label, BorderLayout.CENTER);
	}
	
	public JButton getButton()
	{
		return button;
	}
	
	public void setColors(Color bckColor, Color frgColor)
	{
		label.setForeground(frgColor);
		label.setBackground(bckColor);
		this.setBackground(bckColor);
		this.updateUI();
	}
	
	public void setLabelName(String labelName)
	{
		label.setText(labelName);
		refresh();
	}
	
	public void setButton(JButton button)
	{
		this.button = button;
		refresh();
	}
	
	public void setdimensions(int width)
	{
		label.setPreferredSize(new Dimension(width, 20));
		this.setPreferredSize(new Dimension(width, 20));
	}
	
	public void setdimensions(int width, int height)
	{
		label.setPreferredSize(new Dimension(width, height));
		this.setPreferredSize(new Dimension(width, height));
	}
	
	public void refresh()
	{
		label.updateUI();
		button.updateUI();
		this.repaint();
	}
}
