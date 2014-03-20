package plugins.viewer;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;

public class VerticalBoxLayoutPane {
	
	// private JSplitPane panel;
	// private JScrollPane top;
	// private JScrollPane bottom;
	
	private JPanel panel;
	
	// private RoiMenuBar roiMenu;
	// private RoiManager roiManager;
	// private RoiActionBar actionMenu;
	// private DataBrowser bars;
	
	public VerticalBoxLayoutPane(JPanel[] controlPanels)
	{
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBackground(DisplayStatics.background);
		for (JPanel panel : controlPanels)
		{
			this.panel.add(panel);
		}
	}
	
	public JPanel panel()
	{
		return panel;
	}
	
}
