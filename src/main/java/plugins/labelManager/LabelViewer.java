package plugins.labelManager;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.WindowConstants;

import jex.statics.DisplayStatics;
import preferences.XPreferences;
import Database.SingleUserDatabase.JEXDBInfo;

public class LabelViewer extends JDialog {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DatabaseLabelManager controller;
	private XPreferences labels;
	
	public LabelViewer()
	{
		this.controller = new DatabaseLabelManager();
		this.labels = new XPreferences();
		XPreferences spiked = this.labels.getChildNode("Spiked");
		XPreferences spikedV = spiked.getChildNode("True");
		spikedV.put(JEXDBInfo.DB_LABELCOLORCODE_COLOR, "0,0,0");
		spikedV = spiked.getChildNode("False");
		spikedV.put(JEXDBInfo.DB_LABELCOLORCODE_COLOR, "255,255,255");
		this.controller.setLabels(labels);
		this.getRootPane().setFocusable(true);
		this.getRootPane().requestFocusInWindow();
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setVisible(true);
		this.initizalizeDialog();
	}
	
	private void initizalizeDialog()
	{
		this.setBounds(100, 100, 800, 600);
		this.getContentPane().setBackground(DisplayStatics.background);
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(this.controller.panel(), BorderLayout.CENTER);
	}
}
