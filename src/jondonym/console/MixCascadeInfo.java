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

import java.util.Hashtable;
import java.util.Vector;

import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.PerformanceEntry;
import anon.infoservice.PerformanceInfo;
import anon.infoservice.StatusInfo;
import anon.util.CountryMapper;
import anon.util.JAPMessages;
import anon.util.Util;

public class MixCascadeInfo 
{
	public static int SECURITY_DISTRIBUTION_MAX = MixCascade.DISTRIBUTION_MAX;
	public static int SECURITY_DISTRIBUTION_MIN = MixCascade.DISTRIBUTION_MIN;
	public static int SECURITY_USERS_MAX = StatusInfo.ANON_LEVEL_MAX;
	public static int SECURITY_USERS_MIN = StatusInfo.ANON_LEVEL_MIN;
	
	public static final int SPEED_LOW = PerformanceEntry.BOUNDARIES[PerformanceEntry.SPEED][0];
	public static final int DELAY_HIGH = 
		PerformanceEntry.BOUNDARIES[PerformanceEntry.DELAY][PerformanceEntry.BOUNDARIES[PerformanceEntry.DELAY].length - 1];
	
	private static final String MSG_CONTACT_MESSAGE = MixCascadeInfo.class.getName() + "_contactMessage";
	private static final String MSG_CONTACT = MixCascadeInfo.class.getName() + "_contact";
	private static final String MSG_VISIT_MESSAGE = MixCascadeInfo.class.getName() + "_visitMessage";
	private static final String MSG_SECURITY = MixCascadeInfo.class.getName() + "_security";
	private static final String MSG_NUMBER_OF_USERS = MixCascadeInfo.class.getName() + "_numberOfUsers";
	private static final String MSG_COUNTRY_MIX = MixCascadeInfo.class.getName() + "_countryMix";
	private static final String MSG_COUNTRY_OPERATOR = MixCascadeInfo.class.getName() + "_countryOperator";
	
	private static boolean ms_bToStringAsHTML = false;
	private static Hashtable ms_countryISO2CodePathTable = new Hashtable();
	private static String ms_pathToUsersIcon;
	private static String ms_pathToSecurityIcon;
	private static boolean ms_bSupportForUsersAndSecurity = true;
	
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
	 * Should be called in a web environment for initializing the toString method.
	 * PLEASE NOTE: the interface of this method may change frequently, depending on what is currently needed 
	 * for displaying the toString method in HTML.
	 * @param a_countryISO2CodePathTable absolute or relative URL image paths to country flags, the the format: 
	 * code[ISO2, lower case], [path to flag]
	 * @param a_pathToUsersIcon absolute or relative URL image path to an icon representing the number of users on a cascade,
	 * e.g. an iconified person
	 * @param a_pathToSecurityIcon an absolute or relative path to an icon representing the security of the cascade
	 */
	public static void initToStringAsHTML(Hashtable a_countryISO2CodePathTable)  
			//String a_pathToUsersIcon, String a_pathToSecurityIcon)
	{
		ms_bToStringAsHTML = true;
		if (a_countryISO2CodePathTable != null)
		{
			ms_countryISO2CodePathTable = a_countryISO2CodePathTable;
		}
		ms_bSupportForUsersAndSecurity = false;  // maybe too much for short HTML space of 60 characters...
		//ms_pathToUsersIcon = a_pathToUsersIcon;
		//ms_pathToSecurityIcon = a_pathToSecurityIcon;
	}
	
	/**
	 * Get the minimum speed to expect on this cascade in kbit/s.
	 * @return the minimum speed to expect on this cascade in kbit/s; if < 0, then no information is available; 
	 * if >= SPEED_LOW, then this speed  is below measurement (very bad)
	 */
	public int getMinimumSpeed()
	{
		return PerformanceInfo.getLowestCommonBoundEntry(m_cascade.getId()).getBound(
				PerformanceEntry.SPEED).getBound();
	}
	
	/**
	 * Get the maximum speed to expect on this cascade in kbit/s.
	 * @return the maximum speed to expect on this cascade in kbit/s; if < 0, then no information is available; 
	 * if >= SPEED_LOW, then this speed  is below measurement (very bad)
	 */
	public int getMaximumSpeed()
	{
		return PerformanceInfo.getLowestCommonBoundEntry(m_cascade.getId()).getBestBound(
				PerformanceEntry.SPEED);
	}
	
	/**
	 * Get the maximum delay to expect on this cascade in ms.
	 * @return the maximum delay to expect on this cascade in ms; if < 0, then no information is available; 
	 * if >= DELAY_HIGH, then this delay is above measurement (very bad)
	 */
	public int getMaximumDelay()
	{
		return PerformanceInfo.getLowestCommonBoundEntry(m_cascade.getId()).getBound(
				PerformanceEntry.DELAY).getBound();
	}
	
	/**
	 * Get the minimum delay to expect on this cascade in ms.
	 * @return the minimum delay to expect on this cascade in ms; if < 0, then no information is available; 
	 * if >= DELAY_HIGH, then this delay is above measurement (very bad)
	 */
	public int getMinimumDelay()
	{
		return PerformanceInfo.getLowestCommonBoundEntry(m_cascade.getId()).getBestBound(
				PerformanceEntry.DELAY);
	}
	
	/**
	 * The number of Mix server processes in this cascade. This does not mean that these processes
	 * are independent, distributed or otherwise secure. Use countIndependentOperators(), getSecurityDistribution() 
	 * and getSecurityUsers() to get its security attributes.
	 * @return the number of Mix server processes in this cascade
	 */
	public int countMixes()
	{
		return m_cascade.getNumberOfMixes();
	}
	
	/**
	 * The number of users currently on this cascade.
	 * @return the number of users currently on this cascade; if < 0, no information is available
	 */
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
	 * The distribution points of a Cascade. This is a security attribute. The higher 
	 * the distribution, the more secure this cascade is.
	 * @return the distribution of a cascade; ranges from SECURITY_DISTRIBUTION_MIN (worst value) 
	 * to SECURITY_DISTRIBUTION_MAX (best value)
	 */
	public int getSecurityDistribution()
	{
		return m_cascade.getDistribution();
	}
	
	/**
	 * The user level of a cascade. This is a security attribute. The higher 
	 * the user level, the more secure this cascade is.
	 * @return the distribution of a cascade; ranges from SECURITY_USERS_MIN (worst value) 
	 * to SECURITY_USERS_MAX (best value); if it is < SECURITY_USERS_MIN, than no information is available
	 */
	public int getSecurityUsers()
	{
		StatusInfo status = (StatusInfo)Database.getInstance(StatusInfo.class).getEntryById(m_cascade.getId());
		if (status == null)
		{
			return SECURITY_USERS_MIN - 1;
		}
		return status.getAnonLevel();
	}
	
	/**
	 * Returns information about all mixes in this cascade from 0 to countMixes() - 1. If this
	 * should be presented to the user, however, you should count from 0 to countIndependentOperators(),
	 * just in order not to count more Mixes than there are independent operators.
	 * @param a_position from 0 to countMixes() - 1; if presented to users: from 0 to countIndependentOperators()
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
	public int countIndependentOperators()
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
	 * This cascade's name. Please note that the names of the Mixes may differ!
	 * @return
	 */
	public String getName()
	{
		return m_cascade.getName();
	}
	
	/**
	 * This string returns some general info about the cascade, excluding its name.
	 */
	public String toString()
	{
		String strOutput = "";
		boolean bShowBracket = false;
				
		int nrUsers = countUsers();
		if (nrUsers >= 0 && ms_bSupportForUsersAndSecurity)
		{
			bShowBracket = true;
			
			if (ms_bToStringAsHTML && ms_pathToUsersIcon != null)
			{
				strOutput += "<a title=\"" + JAPMessages.getString(MSG_NUMBER_OF_USERS) + 
				"\"><img alt=\""+ JAPMessages.getString(MSG_NUMBER_OF_USERS) + "\" src=\"" + ms_pathToUsersIcon + "\"/></a> ";
			}
			else
			{
				strOutput += JAPMessages.getString(MSG_NUMBER_OF_USERS) + ": ";
			}
			strOutput += "" + nrUsers;
			if (getUserLimit() > 0)
			{
				strOutput += "/" + getUserLimit() + " ";
			}
			else
			{
				strOutput += " ";
			}
		}		
		
		int securityUsers = getSecurityUsers();
		
		if (getSecurityDistribution() > SECURITY_DISTRIBUTION_MIN && ms_bSupportForUsersAndSecurity)
		{
			bShowBracket = true;
			
			if (ms_bToStringAsHTML && ms_pathToSecurityIcon != null)
			{
				strOutput += "<a title=\"" + JAPMessages.getString(MSG_SECURITY) + 
				"\"><img alt=\"" + JAPMessages.getString(MSG_SECURITY) + "\" src=\"" + ms_pathToSecurityIcon + "\"/></a> ";
			}
			else
			{
				strOutput += JAPMessages.getString(MSG_SECURITY) + ": ";
			}
		
			if (securityUsers >= SECURITY_USERS_MIN)
			{
				strOutput += getSecurityDistribution() + "," + securityUsers;
			}
			else
			{
				strOutput += getSecurityDistribution() + ",?";
			}
			strOutput += "/" + SECURITY_DISTRIBUTION_MAX + "," + SECURITY_USERS_MAX;
		}
		
		if (bShowBracket && countIndependentOperators() > 0)
		{
			strOutput += " [";
		}
		
		for (int i = 0; i < countIndependentOperators(); i++)
		{
			if (ms_bToStringAsHTML && getMixInfo(i).getOperatorHomepage() != null)
			{
				strOutput += "<a title=\"" + JAPMessages.getString(MSG_VISIT_MESSAGE, "" + (i+1)) + "\" target=\"_blank\" href=\"" + 
					getMixInfo(i).getOperatorHomepage() + "\">";
			}
	
			strOutput += "Mix" + (i + 1);
			
			if (ms_bToStringAsHTML && getMixInfo(i).getOperatorHomepage() != null)
			{
				strOutput += "</a>";
			}
			
			if (getMixInfo(i).getLocationCountry() != null)
			{
				strOutput += " " + getFormattedCountry(getMixInfo(i).getLocationCountry(), 
						JAPMessages.getString(MSG_COUNTRY_MIX, "" + (i+1)));
			}
			
			if (getMixInfo(i).getOperatorCountry() != null || 
				(ms_bToStringAsHTML && getMixInfo(i).getOperatorEMailContact() != null))
			{
				strOutput += "(";
				
				if (ms_bToStringAsHTML && getMixInfo(i).getOperatorEMailContact() != null)
				{
					strOutput += "<a title=\"" + JAPMessages.getString(MSG_CONTACT_MESSAGE, "" + (i+1)) + "\" href=\"mailto:" + 
						getMixInfo(i).getOperatorEMailContact() + "\">" + "" + JAPMessages.getString(MSG_CONTACT) + "</a> ";
				}
				
				if (getMixInfo(i).getOperatorCountry() != null)
				{
					strOutput += getFormattedCountry(getMixInfo(i).getOperatorCountry(), 
							JAPMessages.getString(MSG_COUNTRY_OPERATOR, "" + (i+1)));
				}
				
				strOutput = strOutput.trim() + ")";
			}
			if (i + 1  < countIndependentOperators())
			{
				strOutput += ", ";
			}
		}
		
		if (bShowBracket && countIndependentOperators() > 0)
		{
			strOutput = strOutput.trim() + "]";
		}
		
		return Util.toHTMLEntities(strOutput.trim());
	}
	
	private static String getFormattedCountry(String a_countryCode, String a_titleMessage)
	{
		String strImgPath = (String)ms_countryISO2CodePathTable.get(a_countryCode);
		String strAlt = a_titleMessage + " ";
		String strOutput = "";
		try
		{
			strAlt += new CountryMapper(a_countryCode).toString();
		}
		catch (IllegalArgumentException a_e)
		{
			strAlt += a_countryCode;
		}

		if (ms_bToStringAsHTML && strImgPath != null)
		{	
			strOutput += "<a title=\"" + strAlt + "\">";
			strOutput += "<img alt=\"" + a_countryCode + "\" src=\"" + strImgPath + "\"/>";
			strOutput += "</a>";
		}
		else
		{
			strOutput += a_countryCode;
		}
		
		return strOutput;
	}
}
