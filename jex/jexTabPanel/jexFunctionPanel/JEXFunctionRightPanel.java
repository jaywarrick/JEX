package jex.jexTabPanel.jexFunctionPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;

public class JEXFunctionRightPanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	private JLabel title1, title2;
	private JPanel headerPane1, headerPane2;
	
	JEXFunctionRightPanel()
	{
		initialize();
	}
	
	/**
	 * Detach the signals
	 */
	public void deInitialize()
	{
		// SSCenter.defaultCenter().disconnect(this);
	}
	
	private void initialize()
	{
		// Build the selector header
		title1 = new JLabel("MY FUNCTIONS");
		title1.setFont(FontUtility.boldFont);
		headerPane1 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane1.setBackground(DisplayStatics.menuBackground);
		headerPane1.add(title1);
		
		// Build the selector header
		title2 = new JLabel("EXISTING FUNCTION");
		title2.setFont(FontUtility.boldFont);
		headerPane2 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane2.setBackground(DisplayStatics.menuBackground);
		headerPane2.add(title2);
		
		// Place the objects
		this.setBackground(DisplayStatics.lightBackground);
		this.setLayout(new MigLayout("center,flowy,ins 2", "[center,grow]", "[]1[0:0,fill,grow 50]1[]1[0:0,grow 50]1"));
		this.add(headerPane1, "growx");
		this.add(new JPanel(), "grow");
		this.add(headerPane2, "growx");
		this.add(new JPanel(), "grow");
		
		rebuild();
	}
	
	public void rebuild()
	{   
		
	}
	
	public void actionPerformed(ActionEvent e)
	{   
		
	}
}