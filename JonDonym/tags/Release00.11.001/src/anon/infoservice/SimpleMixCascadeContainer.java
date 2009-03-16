package anon.infoservice;

import anon.client.ITermsAndConditionsContainer;

/**
 * Takes and returns a single MixCascade.
 *
 * @author Rolf Wendolsky
 */
public class SimpleMixCascadeContainer extends AbstractMixCascadeContainer
{
	private MixCascade m_mixCascade;
	private boolean m_bAutoReConnect=false;
	public SimpleMixCascadeContainer(MixCascade a_mixCascade)
	{
		m_mixCascade = a_mixCascade;
	}
	public MixCascade getNextCascade()
	{
		return m_mixCascade;
	}
	public MixCascade getCurrentCascade()
	{
		return m_mixCascade;
	}

	public boolean isServiceAutoSwitched()
	{
		return false;
	}

	public void setAutoReConnect(boolean b)
	{
		m_bAutoReConnect=b;
	}
	public boolean isReconnectedAutomatically()
	{
		return m_bAutoReConnect;
	}

	public void keepCurrentService(boolean a_bKeepCurrentCascade)
	{
	}
	
	public ITermsAndConditionsContainer getTCContainer()
	{
		return null;
	}
}
