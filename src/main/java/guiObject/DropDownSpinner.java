package guiObject;

import icons.IconRepository;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import jex.statics.JEXStatics;
import logs.Logs;
import miscellaneous.FontUtility;
import signals.SSCenter;

public class DropDownSpinner extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	public static String NEXT = "next";
	public static String PREVIOUS = "previous";
	public static String VALUE = "value";
	public static String CLOSE = "close";
	
	public Color background = DisplayStatics.background;
	
	String[] values;
	String title;
	String currentValue;
	
	JComboBox field = new JComboBox();
	SignalMenuButton left;
	SignalMenuButton right;
	SignalMenuButton close;
	JPanel spinnerPane;
	JLabel titleLabel;
	
	public DropDownSpinner(String title, String[] values)
	{
		this.values = values;
		this.title = title;
		
		initialize();
	}
	
	public DropDownSpinner(String title, String currentValue, String[] values)
	{
		this.values = values;
		this.title = title;
		this.currentValue = currentValue;
		
		initialize();
	}
	
	private void initialize()
	{
		this.setBackground(background);
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		
		left = new SignalMenuButton();
		Image leftarrow = JEXStatics.iconRepository.getImageWithName(IconRepository.ARROW_BACK_WHITE, 20, 20);
		left.setBackgroundColor(background);
		left.setForegroundColor(background);
		left.setClickedColor(DisplayStatics.background, DisplayStatics.background);
		left.setSize(new Dimension(20, 20));
		left.setImage(leftarrow);
		left.setMouseOverImage(leftarrow);
		left.setMousePressedImage(leftarrow);
		left.setDisabledImage(leftarrow);
		left.setText(null);
		SSCenter.defaultCenter().connect(left, SignalMenuButton.SIG_ButtonClicked_NULL, this, "left", (Class[]) null);
		
		right = new SignalMenuButton();
		Image rightarrow = JEXStatics.iconRepository.getImageWithName(IconRepository.ARROW_FORWARD_WHITE, 20, 20);
		right.setBackgroundColor(background);
		right.setForegroundColor(background);
		right.setClickedColor(DisplayStatics.background, DisplayStatics.background);
		right.setSize(new Dimension(20, 20));
		right.setImage(rightarrow);
		right.setMouseOverImage(rightarrow);
		right.setMousePressedImage(rightarrow);
		right.setDisabledImage(rightarrow);
		right.setText(null);
		SSCenter.defaultCenter().connect(right, SignalMenuButton.SIG_ButtonClicked_NULL, this, "right", (Class[]) null);
		
		close = new SignalMenuButton();
		Image closeImage = JEXStatics.iconRepository.getImageWithName(IconRepository.ARROW_UP_WHITE, 20, 20);
		close.setBackgroundColor(background);
		close.setForegroundColor(background);
		close.setClickedColor(DisplayStatics.background, DisplayStatics.background);
		close.setSize(new Dimension(20, 20));
		close.setImage(closeImage);
		close.setMouseOverImage(closeImage);
		close.setMousePressedImage(closeImage);
		close.setDisabledImage(closeImage);
		close.setText(null);
		SSCenter.defaultCenter().connect(close, SignalMenuButton.SIG_ButtonClicked_NULL, this, "close", (Class[]) null);
		
		DefaultComboBoxModel model = new DefaultComboBoxModel(values);
		field.setModel(model);
		field.setMinimumSize(new Dimension(100, 25));
		field.addActionListener(this);
		
		spinnerPane = new JPanel();
		spinnerPane.setBackground(background);
		spinnerPane.setLayout(new BoxLayout(spinnerPane, BoxLayout.LINE_AXIS));
		spinnerPane.add(Box.createHorizontalGlue());
		spinnerPane.add(left);
		spinnerPane.add(field);
		spinnerPane.add(close);
		spinnerPane.add(Box.createHorizontalStrut(7));
		spinnerPane.add(right);
		spinnerPane.add(Box.createHorizontalGlue());
		spinnerPane.setMaximumSize(new Dimension(500, 25));
		
		titleLabel = new JLabel(this.title);
		titleLabel.setFont(FontUtility.boldFontl);
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		titleLabel.setForeground(Color.white);
		
		this.add(Box.createVerticalGlue());
		this.add(titleLabel);
		this.add(spinnerPane);
		this.add(Box.createVerticalGlue());
	}
	
	/**
	 * Set the value array and the selected value
	 * 
	 * @param selectedValue
	 * @param values
	 */
	public void setValues(String selectedValue, String[] values)
	{
		field.removeActionListener(this);
		
		// set the array model
		DefaultComboBoxModel model = new DefaultComboBoxModel(values);
		field.setModel(model);
		
		// set the selected value
		field.setSelectedItem(selectedValue);
		
		field.addActionListener(this);
		this.repaint();
	}
	
	/**
	 * Set the selected value in the dropdownbox
	 * 
	 * @param selectedValue
	 */
	public void setViewedValue(String selectedValue)
	{
		field.removeActionListener(this);
		field.setSelectedItem(selectedValue);
		field.addActionListener(this);
		this.repaint();
	}
	
	/**
	 * Left arrow clicked
	 */
	public void left()
	{
		Logs.log("Moving to previous " + this.title, 0, this);
		
		SSCenter.defaultCenter().emit(this, PREVIOUS, (Object[]) null);
	}
	
	/**
	 * Right arrow clicked
	 */
	public void right()
	{
		Logs.log("Moving to next " + this.title, 0, this);
		
		SSCenter.defaultCenter().emit(this, NEXT, (Object[]) null);
	}
	
	/**
	 * Close button clicked
	 */
	public void close()
	{
		Logs.log("Closing " + this.title, 0, this);
		
		SSCenter.defaultCenter().emit(this, CLOSE, (Object[]) null);
	}
	
	/**
	 * Action performed
	 */
	public void actionPerformed(ActionEvent e)
	{
		Logs.log("New value selected for " + this.title, 0, this);
		
		SSCenter.defaultCenter().emit(this, VALUE, (Object[]) null);
	}
	
	/**
	 * Return the currently selected value of the dropdown box
	 * 
	 * @return
	 */
	public String getValue()
	{
		Object o = field.getSelectedItem();
		if(o == null)
			return null;
		else
			return o.toString();
	}
}