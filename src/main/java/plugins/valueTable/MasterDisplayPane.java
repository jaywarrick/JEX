package plugins.valueTable;

import javax.swing.JScrollPane;

public class MasterDisplayPane {
	
	private JScrollPane scrollpane;
	
	public MasterDisplayPane(ValueTable display)
	{
		scrollpane = new JScrollPane(display);
		scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		scrollpane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		
		// this.panel = new JPanel();
		// this.panel.setBackground(DisplayStatics.background);
		// this.panel.setLayout(new BorderLayout());
		// this.panel.add(scrollpane,BorderLayout.CENTER);
	}
	
	public JScrollPane panel()
	{
		return this.scrollpane;
	}
	
}
