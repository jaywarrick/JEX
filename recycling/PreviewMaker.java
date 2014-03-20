package recycling;

import javax.swing.JPanel;

import jex.statics.DisplayStatics;
import Database.DBObjects.JEXData;

public class PreviewMaker {
	
	public static JPanel makePreviewPanel(JEXData data)
	{
		if(data == null)
		{
			return blankPanel();
		}
		else if(data.getDataObjectType().equals(JEXData.IMAGE))
		{
			ImagePreview imPreview = new ImagePreview(data, null);
			return imPreview.panel();
		}
		else
		{
			return blankPanel();
		}
	}
	
	public static JPanel blankPanel()
	{
		JPanel ret = new JPanel();
		ret.setBackground(DisplayStatics.background);
		return ret;
	}
}
