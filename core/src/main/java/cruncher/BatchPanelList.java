package cruncher;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.PrintStream;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import jex.jexTabPanel.jexFunctionPanel.JEXConsole;
import jex.statics.DisplayStatics;
import miscellaneous.FontUtility;
import net.miginfocom.swing.MigLayout;

public class BatchPanelList implements MouseListener {
	
	public JPanel outerPanel;
	public JPanel innerPanel;
	public JScrollPane scroll;
	public Vector<BatchPanel> batchList = new Vector<BatchPanel>();
	boolean consoleOn = true;
	private JLabel consoleTitle = new JLabel("CONSOLE (ON)");
	private JPanel consoleHeader = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
	JEXConsole console = new JEXConsole(1000);
	PrintStream o1 = System.out;
	PrintStream e1 = System.err;
	PrintStream consolePS = new PrintStream(this.console);
	
	public BatchPanelList()
	{
		this.initialize();
	}
	
	public void initialize()
	{
		// Build the selector header
		JLabel title1 = new JLabel("BATCH QUEUE");
		JPanel headerPane1 = new JPanel(new MigLayout("flowy,center,ins 1", "[center]", "[center]"));
		headerPane1.setBackground(DisplayStatics.menuBackground);
		title1.setFont(FontUtility.boldFont);
		headerPane1.add(title1);
		
		this.consoleTitle.setText("CONSOLE (ON)");
		System.setOut(this.consolePS);
		System.setErr(this.consolePS);
		consoleOn = true;
		
		this.console.panel().addMouseListener(this);
		this.console.scroll().addMouseListener(this);
		this.consoleHeader.addMouseListener(this);
		this.consoleTitle.addMouseListener(this);
		consoleHeader.setBackground(DisplayStatics.menuBackground);
		consoleTitle.setFont(FontUtility.boldFont);
		consoleHeader.add(consoleTitle);
		
		this.innerPanel = new JPanel();
		this.innerPanel.setLayout(new MigLayout("flowy, ins 3", "[fill,grow]", "[]2[]"));
		this.innerPanel.setBackground(DisplayStatics.lightBackground);
		this.scroll = new JScrollPane(this.innerPanel);
		this.scroll.setBackground(DisplayStatics.lightBackground);
		this.scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		this.scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.scroll.setBorder(BorderFactory.createEmptyBorder());
		this.scroll.getVerticalScrollBar().setUnitIncrement(16);
		this.outerPanel = new JPanel();
		this.outerPanel.setLayout(new MigLayout("flowy, ins 2", "[fill,grow]", "[]1[fill,grow]1[]1[fill,grow]"));
		this.outerPanel.setBackground(DisplayStatics.lightBackground);
		this.outerPanel.add(headerPane1, "growx");
		this.outerPanel.add(this.scroll, "growx, growy 50, h 50:100:");
		this.outerPanel.add(consoleHeader, "growx");
		this.outerPanel.add(this.console.scroll(), "growx, growy 50, h 50:100:");
	}
	
	public void add(Batch batch)
	{
		BatchPanel bpanel = new BatchPanel(batch, this);
		this.batchList.add(bpanel);
		this.innerPanel.add(bpanel.panel(), "growx");
		this.innerPanel.revalidate();
		this.innerPanel.repaint();
	}
	
	public void remove(BatchPanel batchPanel)
	{
		this.innerPanel.remove(batchPanel.panel());
		this.batchList.remove(batchPanel);
		this.innerPanel.revalidate();
		this.innerPanel.repaint();
	}
	
	public JPanel panel()
	{
		return this.outerPanel;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if((e.getSource() == this.consoleHeader || e.getSource() == this.consoleTitle) && e.getClickCount() == 2)
		{
			// Toggle output of the console on/off
			if(consoleOn)
			{
				System.setOut(this.o1);
				System.setErr(this.e1);
				this.consoleTitle.setText("CONSOLE (OFF)");
				consoleOn = false;
			}
			else
			{
				this.consoleTitle.setText("CONSOLE (ON)");
				System.setOut(this.consolePS);
				System.setErr(this.consolePS);
				consoleOn = true;
			}
		}
	}
}
