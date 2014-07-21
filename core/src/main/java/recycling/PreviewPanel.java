package recycling;

import Database.DBObjects.JEXData;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;

public abstract class PreviewPanel {
	
	/**
	 * Return a panel showing a preview of the JEXData. Otherwise return a blank panel.
	 * 
	 * @param data
	 * @param supportingData
	 *            - like an image for a roi
	 * @return
	 */
	public JPanel panel(JEXData data, JEXData supportingData)
	{
		return blankPanel();
	}
	
	protected JPanel blankPanel()
	{
		JPanel ret = new JPanel();
		ret.setBackground(DisplayStatics.background);
		return ret;
	}
	
}
