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

import anon.crypto.MultiCertPath;

public class MixInfo
{
	private String m_strName;
	private String m_strOrganisation;
	private String m_strOrganisationalUnit;
	private String m_strEmail;
	private String m_strWWW;
	private String m_operatorCountry;
	private String m_mixCountry;
	private String m_mixCity;
	private String m_mixState;
	private String m_mixLatitude;
	private String m_mixLongitude;
	private MultiCertPath m_certPaths;
	
	protected MixInfo(anon.infoservice.MixInfo a_info)
	{
		if (a_info != null)
		{
			m_strName = a_info.getName();
			m_certPaths = a_info.getCertPath();
			
			if (a_info.getServiceOperator() != null)
			{
				m_strOrganisation = a_info.getServiceOperator().getOrganization();
				m_strOrganisationalUnit = a_info.getServiceOperator().getOrganizationUnit();
				m_strEmail = a_info.getServiceOperator().getEMail();
				m_strWWW = a_info.getServiceOperator().getUrl();
				m_operatorCountry = a_info.getServiceOperator().getCountryCode();
			}
			if (a_info.getServiceLocation() != null)
			{
				m_mixCountry = a_info.getServiceLocation().getCountryCode();
				m_mixCity = a_info.getServiceLocation().getCity();
				m_mixState = a_info.getServiceLocation().getState();
				m_mixLatitude = a_info.getServiceLocation().getLatitude();
				m_mixLongitude = a_info.getServiceLocation().getLongitude();
			}
		}
	}
	
	public String getName()
	{
		return m_strName;
	}
	
	public int countVerifiedCertificationPaths()
	{
		if (m_certPaths != null)
		{
			return m_certPaths.countVerifiedPaths();
		}
		return 0;
	}
	
	public String getLocationCity()
	{
		return m_mixCity;
	}
	
	public String getLocationState()
	{
		return m_mixState;
	}
	
	public String getLocationCountry()
	{
		return m_mixCountry;
	}
	
	public String getLocationLatitude()
	{
		return m_mixLatitude;
	}
	
	public String getLocationLongitude()
	{
		return m_mixLongitude;
	}
	
	public String getOperatorCountry()
	{
		return m_operatorCountry;
	}
	
	public String getOperatorOrganization()
	{
		return m_strOrganisation;
	}
	
	public String getOperatorOrganizationalUnit()
	{
		return m_strOrganisationalUnit;
	}
	
	public String getOperatorEMailContact()
	{
		return m_strEmail;
	}
	
	public String getOperatorHomepage()
	{
		return m_strWWW;
	}
	
}