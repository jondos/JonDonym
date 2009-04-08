/*
 Copyright (c) 2009, The JAP-Team, JonDos GmbH
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany, nor the name of
 the JonDos GmbH, nor the names of their contributors may be used to endorse or
 promote products derived from this software without specific prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package jondonym.console;

import java.util.Vector;

import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.PerformanceEntry;
import anon.infoservice.PerformanceInfo;
import anon.infoservice.StatusInfo;

public class MixCascadeInfo 
{
	private MixCascade m_cascade;
	private Vector m_cachedMixInfos;
	
	public MixCascadeInfo(MixCascade a_cascade)
	{
		m_cascade = a_cascade;
		m_cachedMixInfos = new Vector();
		for (int i = 0; i < a_cascade.getNumberOfMixes(); i++)
		{
			m_cachedMixInfos.addElement(new MixInfo(m_cascade.getMixInfo(i)));
		}
	}
	
	/**
	 * Get the minimum speed to expect on this cascade in kbit/s.
	 * @return the minimum speed to expect on this cascade in kbit/s
	 */
	public int getMinimumSpeed()
	{
		return PerformanceInfo.getLowestCommonBoundEntry(m_cascade.getId()).getBound(
				PerformanceEntry.SPEED).getBound();
	}
	
	/**
	 * Get the maximum speed to expect on this cascade in kbit/s.
	 * @return the maximum speed to expect on this cascade in kbit/s
	 */
	public int getMaximumSpeed()
	{
		return PerformanceInfo.getLowestCommonBoundEntry(m_cascade.getId()).getBestBound(
				PerformanceEntry.SPEED);
	}
	
	/**
	 * Get the maximum delay to expect on this cascade in ms.
	 * @return the maximum delay to expect on this cascade in ms
	 */
	public int getMaximumDelay()
	{
		return PerformanceInfo.getLowestCommonBoundEntry(m_cascade.getId()).getBound(
				PerformanceEntry.DELAY).getBound();
	}
	
	/**
	 * Get the minimum delay to expect on this cascade in ms.
	 * @return the minimum delay to expect on this cascade in ms
	 */
	public int getMinimumDelay()
	{
		return PerformanceInfo.getLowestCommonBoundEntry(m_cascade.getId()).getBestBound(
				PerformanceEntry.DELAY);
	}
	
	public int countMixes()
	{
		return m_cascade.getNumberOfMixes();
	}
	
	/**
	 * The number of countries counted for this cascade. The maximum is three. This does
	 * neither need to be the number of operator countries, nor the number of countries 
	 * where the mixes are located. It is an evaluation on how distributed this 
	 * cascade is.
	 * @return the number of evaluated countries; ranges from a minimum of 0 (worst value) 
	 * to a maximum of 3 (best value)
	 */
	public int countCountries()
	{
		return m_cascade.getNumberOfCountries();
	}
	
	public int countUsers()
	{
		StatusInfo status = (StatusInfo)Database.getInstance(StatusInfo.class).getEntryById(m_cascade.getId());
		if (status == null)
		{
			return -1;
		}
		return status.getNrOfActiveUsers();
	}
	
	/**
	 * Returns information about all mixes in this cascade from 0 to countMixes() - 1.
	 * @param a_position from 0 to countMixes() - 1
	 * @return
	 */
	public MixInfo getMixInfo(int a_position)
	{
		return (MixInfo)m_cachedMixInfos.elementAt(a_position);
	}
	
	public boolean isPremium()
	{
		return m_cascade.isPayment();
	}
	
	public boolean isSOCKS5Supported()
	{
		return m_cascade.isSocks5Supported();
	}
	
	/**
	 * How many independent operators do we have in this cascade?
	 * @return
	 */
	public int countOperators()
	{
		return m_cascade.getNumberOfOperatorsShown();
	}
	
	/**
	 * The maximum number of users allowed on this cascade.
	 * @return
	 */
	public int getUserLimit()
	{
		return m_cascade.getMaxUsers();
	}
	
	/**
	 * This cascade's name. Please not that the names of the Mixes may differ!
	 * @return
	 */
	public String getName()
	{
		return m_cascade.getName();
	}
}
