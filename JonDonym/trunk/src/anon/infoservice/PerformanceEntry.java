package anon.infoservice;

public class PerformanceEntry extends AbstractDatabaseEntry 
{
	private String m_strCascadeId;
	private String m_strInfoServiceId;
	
	private long m_lastUpdate;
	private long m_serial;
	
	private static final int ENTRY_TIMEOUT = 1000*60*5;
	
	private long m_lDelay;
	private double m_dSpeed;
	
	private long[] m_aDelays = new long[3];
	private double[] m_aSpeeds = new double[3];
	
	public PerformanceEntry(String a_strCascadeId, String a_strInfoServiceId)
	{
		super(System.currentTimeMillis() + ENTRY_TIMEOUT);
		
		m_strCascadeId = a_strCascadeId;
		m_strInfoServiceId = a_strInfoServiceId;
		
		m_lastUpdate = System.currentTimeMillis();
		m_serial = System.currentTimeMillis();
		m_lDelay = -1;
		m_dSpeed = -1;
	}
	
	/**
	 * Use IS and cascade IDs since this entry depends on a specific
	 * cascade and the info service we're retrieving it from.
	 */
	public String getId() 
	{
		return m_strCascadeId + "." + m_strInfoServiceId;
	}

	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	public long getVersionNumber() 
	{
		return m_serial;
	}
	
	public void updateDelay(long a_lDelay) 
	{
		if(m_aDelays == null)
			m_aDelays = new long[3]; // TODO: Make number of fixings configurable
		m_lDelay = 0;
		for(int i=1;i < m_aDelays.length;i++)
			m_lDelay += (m_aDelays[i-1] = m_aDelays[i]);
		m_aDelays[m_aDelays.length-1] = a_lDelay;
		m_lDelay = (m_lDelay + a_lDelay) / m_aDelays.length;
	}

	public void updateSpeed(double a_dSpeed) 
	{
		if(m_aSpeeds == null)
			m_aSpeeds = new double[3]; // TODO: Make number of fixings configurable
		m_dSpeed = 0.0;
		for(int i=1; i < m_aSpeeds.length; i++)
			m_dSpeed += (m_aSpeeds[i-1] = m_aSpeeds[i]);
		m_aSpeeds[m_aSpeeds.length-1] = a_dSpeed;
		m_dSpeed = (m_dSpeed + a_dSpeed) / m_aSpeeds.length;
	}

	public double getAverageThroughput()
	{
		return m_dSpeed;
	}
	
	public long getAverageDelay()
	{
		return m_lDelay;
	}
}
