package anon.infoservice;


/**
 * Maps the position of a Mix in a cascade to a concrete Mix ID.
 */
public  class MixPosition
{
	private int m_position;
	private String m_MixId;

	public MixPosition(int a_position, String a_MixId)
	{
		m_position = a_position;
		m_MixId = a_MixId;
	}
	public int getPosition()
	{
		return m_position;
	}

	public String getId()
	{
		return m_MixId;
	}
	public String toString()
	{
		return m_MixId;
	}
	public boolean equals(Object a_mixPosition)
	{
		if (a_mixPosition == null || !(a_mixPosition instanceof MixPosition))
		{
			return false;
		}
		if (this == a_mixPosition || this.getId().equals(((MixPosition)a_mixPosition).getId()))
		{
			return true;
		}

		return false;
	}
	public int hashCode()
	{
		return m_MixId.hashCode();
	}
}
