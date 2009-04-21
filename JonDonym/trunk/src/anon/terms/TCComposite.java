package anon.terms;

import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.terms.template.Paragraph;

/**
 * Container of TCComponents using the composite pattern
 *
 */
public class TCComposite extends TCComponent
{
	protected Vector tcComponents = new Vector();
	
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
	
	public void removeTCComponent(String id)
	{
		TCComponent currentTCComponents = null;
		for(int i = 0; i < tcComponents.size(); i++)
		{
			currentTCComponents = (TCComponent) tcComponents.elementAt(i);
			if(currentTCComponents.equals(id))
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
	
	public TCComponent getTCComponent(String id)
	{
		TCComponent currentParagraph = null;
		for(int i = 0; i < tcComponents.size(); i++)
		{
			currentParagraph = (Paragraph) tcComponents.elementAt(i);
			if(currentParagraph.equals(id))
			{
				return currentParagraph;
			}
		}
		return null;
	}
	
	public String toString()
	{
		return this.getClass()+"@"+this.id+": "+tcComponents.toString();
	}
	
	/**
	 * returns an object with a reference to a clone of the internal vector.
	 */
	public Object clone()
	{
		TCComposite composite = new TCComposite();
		composite.id = id;
		//composite.content = new String(content);
		composite.tcComponents = (Vector) tcComponents.clone();
		return composite;
	}
}
