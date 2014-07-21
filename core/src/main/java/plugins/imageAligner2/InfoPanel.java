package plugins.imageAligner2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import net.miginfocom.swing.MigLayout;
import signals.SSCenter;

public class InfoPanel implements ActionListener {
	
	public static final String SIG_OkClicked_NULL = "SIG_OkClicked_NULL";
	
	public static final String GREEK_Delta = "\u0394";
	public JPanel panel;
	public JButton OK;
	public JLabel dxLabel, dyLabel, dx, dy;
	
	public InfoPanel()
	{
		this.panel = new JPanel();
		dxLabel = new JLabel(GREEK_Delta + "X");
		dyLabel = new JLabel(GREEK_Delta + "Y");
		dx = new JLabel("0");
		dy = new JLabel("0");
		OK = new JButton("OK");
		this.initialize();
	}
	
	public JPanel panel()
	{
		return this.panel;
	}
	
	public void initialize()
	{
		this.panel.setBackground(DisplayStatics.menuBackground);
		this.panel.setLayout(new MigLayout("ins 3", "[right]5[left,grow]5[]", "[]3[]"));
		OK.addActionListener(this);
		this.panel.add(dxLabel, "cell 0 0");
		this.panel.add(dx, "cell 1 0");
		this.panel.add(OK, "cell 2 0 1 2");
		this.panel.add(dyLabel, "cell 0 1");
		this.panel.add(dy, "cell 1 1");
	}
	
	public void setInfo(int dx, int dy)
	{
		this.dx.setText("" + dx);
		this.dy.setText("" + dy);
		// this.panel.repaint();
	}
	
	public void setInfo(String dx, String dy)
	{
		this.dx.setText("" + dx);
		this.dy.setText("" + dy);
		// this.panel.repaint();
	}
	
	public void actionPerformed(ActionEvent arg0)
	{
		SSCenter.defaultCenter().emit(this, SIG_OkClicked_NULL, (Object[]) null);
	}
	
}
