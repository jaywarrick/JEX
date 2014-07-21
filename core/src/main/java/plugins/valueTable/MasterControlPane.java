package plugins.valueTable;

import java.awt.Dimension;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import net.miginfocom.swing.MigLayout;

public class MasterControlPane {
	
	private JPanel panel;
	
	public MasterControlPane(DataBrowser dataBrowser)
	{
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.background);
		this.panel.setLayout(new MigLayout("flowy, ins 0", "[fill,grow]", "[]0[fill,grow]0[]0[]"));
		this.panel.add(dataBrowser.panel(), "growx");
		this.panel.setMinimumSize(new Dimension(50, 50));
		this.panel.setPreferredSize(new Dimension(178, Integer.MAX_VALUE));
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
}
