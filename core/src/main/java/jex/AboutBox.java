package jex;

//
//	File:	AboutBox.java
//

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class AboutBox extends JFrame implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	private static String TITLE = "Je'Xperiment by Jay and Erwin";
	private static String SUBTITLE = "... and Ben & Edmond";
	private static String VERSION = "Version: Academic MC (v3.0)";
	private static String COPYRIGHT = "Copyright Jay 'n' Erwin";
	
	protected JLabel titleLabel, aboutLabel[];
	protected static int labelCount = 9;
	protected static int aboutWidth = 280;
	protected static int aboutHeight = 230;
	protected static int aboutTop = 200;
	protected static int aboutLeft = 350;
	protected Font titleFont, bodyFont;
	
	public AboutBox()
	{
		super("");
		this.setResizable(false);
		
		SymWindow aSymWindow = new SymWindow();
		this.addWindowListener(aSymWindow);
		
		// Initialize useful fonts
		titleFont = new Font("Lucida Grande", Font.BOLD, 14);
		if(titleFont == null)
		{
			titleFont = new Font("SansSerif", Font.BOLD, 14);
		}
		bodyFont = new Font("Lucida Grande", Font.PLAIN, 10);
		if(bodyFont == null)
		{
			bodyFont = new Font("SansSerif", Font.PLAIN, 10);
		}
		
		this.getContentPane().setLayout(new BorderLayout(15, 15));
		
		aboutLabel = new JLabel[labelCount];
		aboutLabel[0] = new JLabel("");
		aboutLabel[1] = new JLabel(TITLE);
		aboutLabel[1].setFont(titleFont);
		aboutLabel[2] = new JLabel(SUBTITLE);
		aboutLabel[2].setFont(titleFont);
		aboutLabel[3] = new JLabel(VERSION);
		aboutLabel[3].setFont(bodyFont);
		aboutLabel[4] = new JLabel("");
		aboutLabel[5] = new JLabel("");
		aboutLabel[6] = new JLabel("JDK " + System.getProperty("java.version"));
		aboutLabel[6].setFont(bodyFont);
		aboutLabel[7] = new JLabel(COPYRIGHT);
		aboutLabel[7].setFont(bodyFont);
		aboutLabel[8] = new JLabel("");
		
		Panel textPanel2 = new Panel(new GridLayout(labelCount, 1));
		for (int i = 0; i < labelCount; i++)
		{
			aboutLabel[i].setHorizontalAlignment(SwingConstants.CENTER);
			textPanel2.add(aboutLabel[i]);
		}
		this.getContentPane().add(textPanel2, BorderLayout.CENTER);
		this.pack();
		this.setLocation(aboutLeft, aboutTop);
		this.setSize(aboutWidth, aboutHeight);
	}
	
	class SymWindow extends java.awt.event.WindowAdapter {
		
		@Override
		public void windowClosing(java.awt.event.WindowEvent event)
		{
			setVisible(false);
		}
	}
	
	public void actionPerformed(ActionEvent newEvent)
	{
		setVisible(false);
	}
}