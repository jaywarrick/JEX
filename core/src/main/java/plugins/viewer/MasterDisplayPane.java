package plugins.viewer;

import java.awt.Dimension;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import net.miginfocom.swing.MigLayout;

public class MasterDisplayPane {
	
	private JPanel panel;
	
	public MasterDisplayPane(ImageDisplayController display, LimitAdjuster limitAdjuster, StatusBar status)
	{
		this.panel = new JPanel();
		this.panel.setBackground(DisplayStatics.background);
		this.panel.setLayout(new MigLayout("flowy,ins 0", "[fill,grow]", "[]0[fill,grow]0[]"));
		this.panel.add(status.panel(), "gap 5, growx, height 15:15:15");
		this.panel.add(display.panel(), "grow");
		this.panel.add(limitAdjuster.panel(), "growx");
		this.panel.setMinimumSize(new Dimension(50, 50));
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
}
