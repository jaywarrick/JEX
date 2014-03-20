package preferences;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * ButtonAreaLayout. <br>
 * 
 */
public final class ButtonAreaLayout implements LayoutManager {
	
	private int gap;
	
	public ButtonAreaLayout(int gap)
	{
		this.gap = gap;
	}
	
	public void addLayoutComponent(String string, Component comp)
	{}
	
	public void layoutContainer(Container container)
	{
		Insets insets = container.getInsets();
		Component[] children = container.getComponents();
		
		// calculate the max width
		int maxWidth = 0;
		int maxHeight = 0;
		int visibleCount = 0;
		Dimension componentPreferredSize;
		
		for (int i = 0, c = children.length; i < c; i++)
		{
			if(children[i].isVisible())
			{
				componentPreferredSize = children[i].getPreferredSize();
				maxWidth = Math.max(maxWidth, componentPreferredSize.width);
				maxHeight = Math.max(maxHeight, componentPreferredSize.height);
				visibleCount++;
			}
		}
		
		int usedWidth = maxWidth * visibleCount + gap * (visibleCount - 1);
		
		for (int i = 0, c = children.length; i < c; i++)
		{
			if(children[i].isVisible())
			{
				children[i].setBounds(container.getWidth() - insets.right - usedWidth + (maxWidth + gap) * i, insets.top, maxWidth, maxHeight);
			}
		}
	}
	
	public Dimension minimumLayoutSize(Container c)
	{
		return preferredLayoutSize(c);
	}
	
	public Dimension preferredLayoutSize(Container container)
	{
		Insets insets = container.getInsets();
		Component[] children = container.getComponents();
		
		// calculate the max width
		int maxWidth = 0;
		int maxHeight = 0;
		int visibleCount = 0;
		Dimension componentPreferredSize;
		
		for (int i = 0, c = children.length; i < c; i++)
		{
			if(children[i].isVisible())
			{
				componentPreferredSize = children[i].getPreferredSize();
				maxWidth = Math.max(maxWidth, componentPreferredSize.width);
				maxHeight = Math.max(maxHeight, componentPreferredSize.height);
				visibleCount++;
			}
		}
		
		int usedWidth = maxWidth * visibleCount + gap * (visibleCount - 1);
		
		return new Dimension(insets.left + usedWidth + insets.right, insets.top + maxHeight + insets.bottom);
	}
	
	public void removeLayoutComponent(Component c)
	{}
}
