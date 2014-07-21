package preferences;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import logs.Logs;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class XPreferencePanelController implements ActionListener {
	
	public static final String SIG_Save_NULL = "SIG_Save_NULL";
	public static final String SIG_Cancel_NULL = "SIG_Cancel_NULL";
	
	private XPreferences prefs;
	private PropertySheetTableModel model;
	private JPanel prefPane;
	private JPanel panel;
	private JButton save, cancel;
	private boolean editable;
	
	public XPreferencePanelController(XPreferences prefs, boolean editable)
	{
		this.panel = new JPanel(new MigLayout("flowy, ins 3", "[fill,grow]", "[fill,grow][]"));
		this.prefs = prefs;
		this.editable = editable;
		this.rebuild();
	}
	
	public void rebuild()
	{
		this.panel.removeAll();
		Property[] newProperties = this.prefs.getSubProperties();
		this.model = new PropertySheetTableModel();
		this.model.setProperties(newProperties);
		
		PropertySheetTable table = new PropertySheetTable(this.model);
		this.prefPane = new PropertySheetPanel(table);
		this.panel.add(this.prefPane, "grow");
		
		if(this.editable)
		{
			this.save = new JButton("Save");
			this.cancel = new JButton("Cancel");
			JPanel resultPanel = new JPanel(new MigLayout("flowx, ins 0, right", "[100]0[100]", "[]"));
			resultPanel.add(this.save, "growx");
			resultPanel.add(this.cancel, "growx");
			this.save.addActionListener(this);
			this.cancel.addActionListener(this);
			this.panel.add(resultPanel, "growx");
		}
	}
	
	public JPanel getPanel()
	{
		return this.panel;
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.save)
		{
			Logs.log("Saving the new preferences", 0, this);
			SSCenter.defaultCenter().emit(this, SIG_Save_NULL, (Object[]) null);
		}
		else
		{
			Logs.log("Canceled changes to preferences", 0, this);
			SSCenter.defaultCenter().emit(this, SIG_Cancel_NULL, (Object[]) null);
		}
	}
	
}
