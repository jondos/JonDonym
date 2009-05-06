package anon.terms;

import java.util.Vector;

/**
 * Container of TCComponents using the composite pattern
 *
 */
public class TCComposite extends TCComponent
{
	protected Vector tcComponents = new Vector();
	
	public TCComposite()
	{
		super();
	}
	
	public TCComposite(double id, Object content) 
	{
		super(id, content);
	}

	/**
	 * Adds the specified TCComponent to this container.
	 * The insertion will place the component according to its id
	 * in ascending order. If a component with the same id exists in this section
	 * it will be replaced by the specified paragraph
	 * @param tcComponent new component to be appended to this container.
	 */
	public void addTCComponent(TCComponent tcComponent)
	{
		int index = 0;
		
		TCComponent currentTCComponent = null;
		for(;index < tcComponents.size(); index++)
		{
			currentTCComponent = (TCComponent) tcComponents.elementAt(index);
			
			if(currentTCComponent.getId() == tcComponent.getId())
			{
				//replace
				tcComponents.removeElementAt(index);
				tcComponents.insertElementAt(tcComponent, index);
				return;
			}
			if(currentTCComponent.getId() > tcComponent.getId())
			{
				tcComponents.insertElementAt(tcComponent, index);
				return;
			}
		}
		tcComponents.addElement(tcComponent);
	}
	
	public void removeTCComponent(double id)
	{
		TCComponent currentTCComponents = null;
		for(int i = 0; i < tcComponents.size(); i++)
		{
			currentTCComponents = (TCComponent) tcComponents.elementAt(i);
			if(currentTCComponents.getId() == id)
			{
				tcComponents.removeElementAt(i);
			}
		}
	}
	
	public int getTCComponentCount()
	{
		return tcComponents.size();
	}
	
	public TCComponent[] getTCComponents()
	{
		TCComponent[] allComponents = new TCComponent[tcComponents.size()];
		for(int i = 0; i < tcComponents.size(); i++)
		{
			allComponents[i] = (TCComponent) tcComponents.elementAt(i);
		}
		return allComponents;
	}
	
	public TCComponent getTCComponent(double id)
	{
		TCComponent currentParagraph = null;
		for(int i = 0; i < tcComponents.size(); i++)
		{
			currentParagraph = (TCComponent) tcComponents.elementAt(i);
			if(currentParagraph.getId() == id)
			{
				return currentParagraph;
			}
		}
		return null;
	}
	
	public boolean hasContent()
	{
		return super.hasContent() || (getTCComponentCount() > 0);
	}
	
	public String toString()
	{
		return this.getClass()+"@"+this.id+": "+tcComponents.toString();
	}
	
	public Object clone()
	{
		TCComposite composite = null;
		try 
		{
			composite = (TCComposite) getClass().newInstance();
		} 
		catch (InstantiationException e) {}
		catch (IllegalAccessException e) {}
		if(composite != null)
		{
			composite.id = id;
			composite.content = content; 
			TCComponent[] allComponents = getTCComponents();
			for (int i = 0; i < allComponents.length; i++) 
			{
				composite.tcComponents.addElement(allComponents[i].clone());
			}
		}
		return composite;
	}
}
