package guiObject;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class JLabeledTextField extends JPanel implements FocusListener {
	
	private static final long serialVersionUID = 1L;
	public JTextField field = new JTextField("", 8);
	public JLabel label = new JLabel("");
	public ActionListener listener;
	
	public JLabeledTextField(String labelName, String text)
	{
		this.setBackground(Color.WHITE);
		this.setLayout(new BorderLayout());
		setLabelName(labelName);
		setText(text);
		
		label.setMaximumSize(new Dimension(500, 20));
		label.setPreferredSize(new Dimension(100, 200));
		field.setMaximumSize(new Dimension(500, 20));
		field.setPreferredSize(new Dimension(60, 200));
		field.addFocusListener(this);
		
		this.setMaximumSize(new Dimension(500, 20));
		this.setPreferredSize(new Dimension(120, 20));
		
		this.add(label, BorderLayout.LINE_START);
		this.add(field, BorderLayout.CENTER);
	}
	
	public String getText()
	{
		return field.getText();
	}
	
	public String getLabel()
	{
		return label.getText();
	}
	
	public void setColor(Color color)
	{
		this.setBackground(color);
		label.setBackground(color);
		this.updateUI();
	}
	
	public void setLabelColor(Color color)
	{
		label.setBackground(color);
		this.repaint();
	}
	
	public void setLabelName(String labelName)
	{
		label.setText(labelName);
		refresh();
	}
	
	public void setText(String text)
	{
		field.setText(text);
		refresh();
	}
	
	public void setdimensions(int widthText, int totalWidth)
	{
		label.setPreferredSize(new Dimension(widthText, 20));
		field.setPreferredSize(new Dimension(totalWidth - widthText, 20));
		this.setPreferredSize(new Dimension(totalWidth, 20));
	}
	
	public void setLabelFont(Font font)
	{
		label.setFont(font);
	}
	
	public void refresh()
	{
		this.repaint();
	}
	
	public void setActionListener(ActionListener listener)
	{
		this.listener = listener;
	}
	
	public void focusGained(FocusEvent e)
	{}
	
	public void focusLost(FocusEvent e)
	{
		if(listener != null)
		{
			listener.actionPerformed(new ActionEvent(this, 0, null));
		}
	}
}
