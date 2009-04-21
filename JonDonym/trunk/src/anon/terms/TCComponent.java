package anon.terms;

public class TCComponent
{
	/** the id of a TC component is represented by a String */
	protected int id = -1;
	/** a TCComponent is only supposed to have String content */
	protected Object content = null;
	
	public TCComponent() 
	{}
	
	public TCComponent(int id) 
	{
		this.id = id;
	}
	
	public TCComponent(int id, String content) 
	{
		this.id = id;
		this.content = content;
	}
	
	public int getId() 
	{
		return id;
	}

	public void setId(int id) 
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
	
	public boolean equals(Object anotherObject)
	{
		return ((TCComponent)anotherObject).getId() == id;
	}
	
	public String toString()
	{
		return this.content != null ? (this.getClass()+"@"+this.id+": "+this.content.toString()) : null;
	}
}
