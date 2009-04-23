package anon.terms;

import java.util.Vector;

public abstract class TCComponent
{
	/** the id of a TC component is represented by a String */
	protected double id = -1;
	/** a TCComponent is only supposed to have String content */
	protected Object content = null;
	
	public TCComponent() 
	{}
	
	public TCComponent(double id) 
	{
		this.id = id;
	}
	
	public TCComponent(double id, Object content) 
	{
		this.id = id;
		this.content = content;
	}

	public double getId() 
	{
		return id;
	}

	public void setId(double id) 
	{
		this.id = id;
	}

	public Object getContent() 
	{
		return content;
	}

	public void setContent(Object content) 
	{
		this.content = content;
	}
	
	public boolean hasContent()
	{
		return content != null;
	}
	
	public boolean equals(Object anotherObject)
	{
		if(!(anotherObject instanceof TCComponent)) return false;
		return (((TCComponent)anotherObject).getId() == id) && 
			getClass().equals(anotherObject.getClass());
	}
	
	public abstract Object clone();
	
	public String toString()
	{
		return this.content != null ? (this.getClass()+"@"+this.id+": "+this.content.toString()) : null;
	}
}
