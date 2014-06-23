package plugins.selector;

import guiObject.FlatRoundedStaticButton;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class QuickSelectorHeaders implements ActionListener {
	
	public static final String SIG_ExptHeaderClicked_NULL = "SIG_ExptHeaderClicked_STRINGname";
	public static final String SIG_TrayHeaderClicked_NULL = "SIG_TrayHeaderClicked_NULL";
	
	private JPanel panel;
	private FlatRoundedStaticButton exptHeader;
	private FlatRoundedStaticButton trayHeader;
	private JLabel listHeader;
	
	public QuickSelectorHeaders()
	{
		this.panel = new JPanel(new MigLayout("left,flowy, ins 0", "[left,fill,grow]", "[]0[]5[]"));
		this.panel.setBackground(DisplayStatics.lightBackground);
		this.exptHeader = new FlatRoundedStaticButton("");
		this.exptHeader.setFont(FontUtility.boldFont);
		this.exptHeader.enableUnselection(false);
		this.trayHeader = new FlatRoundedStaticButton("");
		this.trayHeader.setFont(FontUtility.boldFont);
		this.trayHeader.enableUnselection(false);
		this.listHeader = new JLabel("");
		this.listHeader.setFont(FontUtility.defaultFonts);
		this.initializePanel();
		this.addListeners();
		this.panel().repaint();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void initializePanel()
	{
		this.panel.add(this.exptHeader.panel(), "gap 5 5 0 0, grow");
		this.panel.add(this.trayHeader.panel(), "gap 5 5 0 0, grow");
		this.panel.add(this.listHeader, "gap 1, grow");
	}
	
	public void addListeners()
	{
		this.exptHeader.addActionListener(this);
		this.trayHeader.addActionListener(this);
	}
	
	public void setExperimentHeader(String name)
	{
		this.exptHeader.setText(name);
	}
	
	public void setTrayHeader(String name)
	{
		this.trayHeader.setText(name);
	}
	
	public void setListHeader(String description)
	{
		this.listHeader.setText(description);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if(e.getSource() == this.exptHeader)
		{
			SSCenter.defaultCenter().emit(this, SIG_ExptHeaderClicked_NULL, (Object[]) null);
		}
		else if(e.getSource() == this.trayHeader)
		{
			SSCenter.defaultCenter().emit(this, SIG_TrayHeaderClicked_NULL, (Object[]) null);
		}
	}
	
	public void mouseClicked(MouseEvent e)
	{}
	
	public void mouseEntered(MouseEvent e)
	{}
	
	public void mouseExited(MouseEvent e)
	{}
	
	public void mouseReleased(MouseEvent e)
	{}
	
}
