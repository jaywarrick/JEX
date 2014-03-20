package guiObject;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.plaf.metal.MetalToolTipUI;

public class MultiLineToolTip extends JToolTip {
	
	private static final long serialVersionUID = 1L;
	
	public MultiLineToolTip()
	{
		setUI(new MultiLineToolTipUI());
	}
}

class MultiLineToolTipUI extends MetalToolTipUI {
	
	private String[] strs;
	
	// private int maxWidth = 0;
	
	@Override
	public void paint(Graphics g, JComponent c)
	{
		Graphics2D g2 = (Graphics2D) g;
		FontRenderContext frc = g2.getFontRenderContext();
		LineMetrics lm = g.getFont().getLineMetrics("", frc);
		
		Dimension size = c.getSize();
		g.setColor(c.getBackground());
		g.fillRect(0, 0, size.width, size.height);
		g.setColor(c.getForeground());
		if(strs != null)
		{
			for (int i = 0; i < strs.length; i++)
			{
				g.drawString(strs[i], 3, ((int) lm.getHeight()) * (i + 1));
			}
		}
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public Dimension getPreferredSize(JComponent c)
	{
		FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(c.getFont());
		String tipText = ((JToolTip) c).getTipText();
		if(tipText == null)
		{
			tipText = "";
		}
		BufferedReader br = new BufferedReader(new StringReader(tipText));
		String line;
		int maxWidth = 0;
		Vector<String> v = new Vector<String>();
		try
		{
			while ((line = br.readLine()) != null)
			{
				int width = SwingUtilities.computeStringWidth(metrics, line);
				maxWidth = (maxWidth < width) ? width : maxWidth;
				v.addElement(line);
			}
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
		int lines = v.size();
		if(lines < 1)
		{
			strs = null;
			lines = 1;
		}
		else
		{
			strs = new String[lines];
			int i = 0;
			for (Enumeration<String> e = v.elements(); e.hasMoreElements(); i++)
			{
				strs[i] = e.nextElement();
			}
		}
		int height = metrics.getHeight() * lines;
		// this.maxWidth = maxWidth;
		return new Dimension(maxWidth + 6, height + 4);
	}
}
