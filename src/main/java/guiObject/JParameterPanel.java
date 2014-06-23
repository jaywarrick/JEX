package guiObject;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jex.statics.JEXStatics;
import logs.Logs;
import net.miginfocom.swing.MigLayout;
import jex.statics.DisplayStatics;
import Database.Definition.Parameter;

public class JParameterPanel implements DocumentListener, ChangeListener, ActionListener {
	
	// Model variable
	Parameter p;
	
	// GUI variables
	private JPanel panel;
	private JLabel titleField;
	private JComponent resultField;
	private JButton fileButton;
	
	public JParameterPanel(Parameter p)
	{
		this.p = p;
		this.initialize();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	/**
	 * initialize the entry
	 */
	private void initialize()
	{
		// This
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.panel.setLayout(new MigLayout("flowx, ins 0, hidemode 2", "[0:0,fill,140][0:0,fill,grow]", "[24,center]"));
		
		// GUI objects
		this.fileButton = new JButton("...");
		this.fileButton.addActionListener(this);
		this.titleField = new JLabel(this.p.title);
		this.titleField.setToolTipText(this.p.note);
		if(this.p.type == (Parameter.DROPDOWN) && this.p.options != null)
		{
			this.resultField = new JComboBox(this.p.options);
			
			for (int i = 0, len = this.p.options.length; i < len; i++)
			{
				if(this.p.result.equals(this.p.options[i]))
				{
					((JComboBox) this.resultField).setSelectedIndex(i);
				}
			}
			((JComboBox) this.resultField).addActionListener(this);
		}
		else if(this.p.type == (Parameter.CHECKBOX))
		{
			this.resultField = new JCheckBox();
			if(this.p.result.equals("true"))
			{
				((JCheckBox) this.resultField).setSelected(true);
			}
			((JCheckBox) this.resultField).addChangeListener(this);
		}
		else if(this.p.type == (Parameter.FILECHOOSER))
		{
			this.resultField = new JTextField("");
			((JTextField) this.resultField).setText(this.p.result);
			((JTextField) this.resultField).getDocument().addDocumentListener(this);
		}
		else if(this.p.type == (Parameter.PASSWORD))
		{
			this.resultField = new JPasswordField("");
			((JPasswordField) this.resultField).setText(this.p.result);
			((JPasswordField) this.resultField).getDocument().addDocumentListener(this);
		}
		else
		{
			this.resultField = new JTextField("");
			((JTextField) this.resultField).setText(this.p.result);
			((JTextField) this.resultField).getDocument().addDocumentListener(this);
		}
		if(this.p.result != null)
		{
			this.resultField.setToolTipText(this.p.getValue());
		}
		
		this.panel.add(this.titleField, "wmin 10");
		if(this.p.type == Parameter.FILECHOOSER)
		{
			JPanel temp = new JPanel();
			temp.setLayout(new MigLayout("flowx, ins 0", "[fill,grow][fill]", "[fill,grow]"));
			temp.add(this.resultField, "growx, wmin 10");
			temp.add(this.fileButton, "grow,width 40:40:40, height 10:20:");
			this.panel.add(temp, "growx");
		}
		else
		{
			this.panel.add(this.resultField, "growx, wmin 10, height 10:20:");
		}
		
	}
	
	/**
	 * Return the set value in the result Field
	 * 
	 * @return String value of the component
	 */
	public String getValue()
	{
		String result;
		if(this.p.type == (Parameter.DROPDOWN) && this.p.options != null)
		{
			result = ((JComboBox) this.resultField).getSelectedItem().toString();
		}
		else if(this.p.type == (Parameter.CHECKBOX))
		{
			result = "" + ((JCheckBox) this.resultField).isSelected();
		}
		else
		// works for both plain text field and file chooser text field
		{
			result = ((JTextField) this.resultField).getText();
		}
		
		return result;
	}
	
	/**
	 * Set the displayed value of the parameter
	 * 
	 * @param paramValue
	 */
	public void setValue(String paramValue)
	{
		this.p.setValue(paramValue);
		((JTextField) this.resultField).setText(paramValue);
	}
	
	/**
	 * Return the parameter displayed in this panel
	 * 
	 * @return
	 */
	public Parameter getParameter()
	{
		return this.p;
	}
	
	public void validate()
	{
		this.p.setValue(this.getValue());
	}
	
	@Override
	public void changedUpdate(DocumentEvent arg0)
	{
		String result = this.getValue();
		String old = this.p.getValue();
		this.p.setValue(result);
		if(this.p.type == Parameter.PASSWORD)
		{
			return;
		}
		this.resultField.setToolTipText(this.p.getValue());
		Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
	}
	
	@Override
	public void insertUpdate(DocumentEvent arg0)
	{
		String result = this.getValue();
		String old = this.p.getValue();
		this.p.setValue(result);
		if(this.p.type == Parameter.PASSWORD)
		{
			return;
		}
		this.resultField.setToolTipText(this.p.getValue());
		Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
	}
	
	@Override
	public void removeUpdate(DocumentEvent arg0)
	{
		String result = this.getValue();
		String old = this.p.getValue();
		this.p.setValue(result);
		if(this.p.type == Parameter.PASSWORD)
		{
			return;
		}
		this.resultField.setToolTipText(this.p.getValue());
		Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
	}
	
	@Override
	public void actionPerformed(ActionEvent action)
	{
		if(action.getSource() == this.fileButton)
		{
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int returnVal = fc.showOpenDialog(JEXStatics.main);
			String directory = null;
			if(returnVal == JFileChooser.APPROVE_OPTION)
			{
				try
				{
					directory = fc.getSelectedFile().getCanonicalPath();
				}
				catch (IOException e)
				{
					e.printStackTrace();
					return;
				}
				if(directory == null)
				{
					return;
				}
			}
			String old = this.p.getValue();
			((JTextField) this.resultField).setText(directory);
			this.p.setValue(this.getValue());
			if(this.p.type == Parameter.PASSWORD)
			{
				return;
			}
			this.resultField.setToolTipText(this.p.getValue());
			Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
		}
		else
		{
			String result = this.getValue();
			String old = this.p.getValue();
			this.p.setValue(result);
			if(this.p.type == Parameter.PASSWORD)
			{
				return;
			}
			this.resultField.setToolTipText(this.p.getValue());
			Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent arg0)
	{
		String result = this.getValue();
		String old = this.p.getValue();
		this.p.setValue(result);
		if(this.p.type == Parameter.PASSWORD)
		{
			return;
		}
		this.resultField.setToolTipText(this.p.getValue());
		Logs.log("Changed parameter from " + old + " to " + this.p.getValue(), 0, this);
	}
}
