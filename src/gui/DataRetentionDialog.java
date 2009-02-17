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
package gui;

import java.awt.Component;
import java.net.URL;
import java.util.Enumeration;

import anon.infoservice.DataRetentionInformation;
import anon.infoservice.MixCascade;
import anon.util.ClassUtil;
import anon.util.XMLDuration;

import gui.dialog.JAPDialog;

public class DataRetentionDialog 
{
	public static final String MSG_DATA_RETENTION_EXPLAIN_SHORT = 
		DataRetentionDialog.class.getName() + "_explainShort";
	public static final String MSG_DATA_RETENTION_MIX_EXPLAIN_SHORT = 
		DataRetentionDialog.class.getName() + "_explainShortMix";
	public static final String MSG_DATA_RETENTION_EXPLAIN = 
		DataRetentionDialog.class.getName() + "_explain";
	
	public static final String MSG_RETENTION_PERIOD = DataRetentionDialog.class.getName() + "_retentionPeriod";
	public static final String MSG_INFO_TITLE = DataRetentionDialog.class.getName() + "_info";
	public static final String MSG_NO_LOGS = DataRetentionDialog.class.getName() + "_noLogs";
	public static final String MSG_ENTRY_MIX_STORES = DataRetentionDialog.class.getName() + "_entryMixStores";
	public static final String MSG_CASCADE_STORES = DataRetentionDialog.class.getName() + "_cascadeStores";
	public static final String MSG_MIX_STORES = DataRetentionDialog.class.getName() + "_mixStores";
	public static final String MSG_NO_CHANCE = DataRetentionDialog.class.getName() + "_noChance";
	public static final String MSG_NO_TARGET_ADDRESSES = DataRetentionDialog.class.getName() + "_noTargetAdresses";
	public static final String MSG_IN_THE_SCOPE = DataRetentionDialog.class.getName() + "_inTheScope";
	public static final String MSG_WHETHER_CONNECTED = DataRetentionDialog.class.getName() + "_whetherConnected";
	public static final String MSG_WHICH_TARGETED = DataRetentionDialog.class.getName() + "_whichTargeted";
	public static final String MSG_WHETHER_TARGETED = DataRetentionDialog.class.getName() + "_whetherTargeted";
	public static final String MSG_WHETHER_USED = DataRetentionDialog.class.getName() + "_whetherUsed";
	
	private DataRetentionDialog() 
	{
	}
	
	public static void show(Component component, MixCascade a_cascade)
	{
		show(component, a_cascade, -1);
	}

	public static void show(Component component, MixCascade a_cascade, int a_mixID)
	{
		String strMessage = "";
		JAPDialog.LinkedInformationAdapter adapter = null;
		DataRetentionInformation drInfo = null;
		DataRetentionInformation drInfoFirstMix = null;
		DataRetentionInformation drInfoLastMix = null;
		DataRetentionInformation drInfoCascade = null;
		URL url = null;
		boolean bFirstMixOnly = false;
		boolean bDataAboutRequestedPages = false;
		XMLDuration duration;
		
		if (a_mixID >= 0)
		{
			if (a_cascade != null && a_cascade.getMixInfo(a_mixID) != null)
			{
				drInfo = a_cascade.getMixInfo(a_mixID).getDataRetentionInformation();
			}
		}
		else if (a_cascade != null)
		{
			drInfo = a_cascade.getDataRetentionInformation();
		}
		
		if (drInfo == null || a_cascade == null)
		{
			// no information available
			return;
		}
			
		url = drInfo.getURL(JAPMessages.getLocale().getLanguage());
		final URL finalURL = url;
		if (finalURL != null)
		{
			adapter = new JAPDialog.AbstractLinkedURLAdapter()
			{
				public URL getUrl()
				{
					return finalURL;
				}
				
				public String getMessage()
				{
					return JAPMessages.getString(JAPDialog.ILinkedInformation.MSG_MORE_INFO);
				}
			};
		}
		
		if (a_cascade.getMixInfo(0) != null)
		{
			// store first mix info for later usage
			drInfoFirstMix = a_cascade.getMixInfo(0).getDataRetentionInformation();
			if (a_mixID < 0 && drInfo.isLogged(DataRetentionInformation.NOTHING))
			{
				// it seems that there are no common log entries; investigate  the first Mix only!
				if (drInfoFirstMix != null)
				{
					bFirstMixOnly = true;
				}
			}	
		}
		drInfoCascade = a_cascade.getDataRetentionInformation();
		
		if (a_cascade.getMixInfo(a_cascade.getNumberOfMixes() - 1) != null)
		{
			drInfoLastMix = a_cascade.getMixInfo(a_cascade.getNumberOfMixes() - 1).getDataRetentionInformation();
		}
		
		if (a_mixID < 0 && (drInfoFirstMix == null || drInfoFirstMix.isLogged(DataRetentionInformation.NOTHING)))
		{
			// nothing is logged!!
			strMessage += JAPMessages.getString(MSG_NO_LOGS, "<i>" + a_cascade.getName() + "</i>");
		}
		else
		{
			if (bFirstMixOnly)
			{
				strMessage = JAPMessages.getString(MSG_ENTRY_MIX_STORES, "<i>" + a_cascade.getName() + "</i>");
			}
			else if (a_mixID < 0)
			{
				strMessage = JAPMessages.getString(MSG_CASCADE_STORES, "<i>" + a_cascade.getName() + "</i>");
			}
			else
			{	
				String strOperator = "unknown";
				if (a_cascade.getMixInfo(a_mixID).getServiceOperator() != null)
				{
					strOperator = a_cascade.getMixInfo(a_mixID).getServiceOperator().getOrganization();
				}
				
				strMessage = JAPMessages.getString(MSG_MIX_STORES, new String[]{
						"<i>" + a_cascade.getMixInfo(a_mixID).getName() + "</i>", "<i>" + strOperator + "</i>"});
			}
			
			strMessage += "<ul>";
			for (int i = 0; i < DataRetentionInformation.getLoggedElementsLength(); i++)
			{
				if (!drInfo.isLogged(DataRetentionInformation.getLoggedElementID(i)))
				{
					if (a_mixID < 0) // cascade
					{
						// test the logs that are relevant for the first mix
						if (drInfoFirstMix != null && (
							i == DataRetentionInformation.INPUT_SOURCE_IP_ADDRESS ||
							i == DataRetentionInformation.INPUT_SOURCE_IP_PORT ||
							i == DataRetentionInformation.INPUT_TIME ||
							i == DataRetentionInformation.OUTPUT_TIME) &&
							drInfoFirstMix.isLogged(i)) 
						{
							// show this element as logged
						}
						else if (drInfoLastMix != null && (
								i == DataRetentionInformation.OUTPUT_SOURCE_IP_ADDRESS ||
								i == DataRetentionInformation.OUTPUT_SOURCE_IP_PORT ||
								i == DataRetentionInformation.OUTPUT_TARGET_IP_ADDRESS ||
								i == DataRetentionInformation.OUTPUT_TARGET_DOMAIN) &&
								drInfoLastMix.isLogged(i))
						{
							// show this element as logged
						}
						else
						{
							continue;
						}
					}
					else
					{
						continue;
					}
				}
			
				strMessage += "<li>" + JAPMessages.getString(ClassUtil.getClassNameStatic() + "_" +
						(DataRetentionInformation.getLoggedElementName(i))) + "</li>";
			}
			
			strMessage += "</ul>";
			strMessage += "<p>" + JAPMessages.getString(MSG_RETENTION_PERIOD)  + ": ";
			if (bFirstMixOnly)
			{
				duration = drInfoFirstMix.getDuration();
			}
			else
			{
				duration = drInfo.getDuration();
			}
			Enumeration enumFields = duration.getFields();
			Object currentField;
			while (enumFields.hasMoreElements())
			{
				currentField = enumFields.nextElement();
				strMessage += duration.getField(currentField).intValue() + " " + 
					JAPMessages.getString(XMLDuration.getFieldName(currentField));
				if (enumFields.hasMoreElements())
				{
					strMessage += ", ";
				}
			}
			strMessage += "</p>";
			strMessage += "<br>";
			
			
			if (drInfoCascade != null && drInfoFirstMix != null && 
				drInfoFirstMix.isLogged(DataRetentionInformation.INPUT_SOURCE_IP_ADDRESS))
			{
				strMessage += JAPMessages.getString(MSG_IN_THE_SCOPE) + " ";
				
				if (bFirstMixOnly || !drInfoCascade.isLogged(DataRetentionInformation.INPUT_CHANNEL_ID) ||
					!drInfoCascade.isLogged(DataRetentionInformation.OUTPUT_CHANNEL_ID))
				{
					if (drInfoFirstMix.isLogged(DataRetentionInformation.INPUT_TIME) ||
						drInfoFirstMix.isLogged(DataRetentionInformation.INPUT_CHANNEL_ID))
					{
						strMessage += JAPMessages.getString(MSG_WHETHER_USED, "<i>" + a_cascade.getName() + "</i>");
					}
					else
					{
						strMessage += JAPMessages.getString(MSG_WHETHER_CONNECTED, "<i>" + a_cascade.getName() + "</i>");
					}
				}
				else if (drInfoCascade.isLogged(DataRetentionInformation.INPUT_TIME))
				{
					if (drInfoLastMix.isLogged(DataRetentionInformation.OUTPUT_TARGET_DOMAIN) ||
						drInfoLastMix.isLogged(DataRetentionInformation.OUTPUT_TARGET_IP_ADDRESS))
					{
						bDataAboutRequestedPages = true;
						strMessage += JAPMessages.getString(MSG_WHICH_TARGETED);
					}
					else if (drInfoLastMix.isLogged(DataRetentionInformation.OUTPUT_SOURCE_IP_PORT))
					{
						strMessage += JAPMessages.getString(MSG_WHETHER_TARGETED, "<i>" + a_cascade.getName() + "</i>");
					}
				}
				else
				{
					strMessage += JAPMessages.getString(MSG_WHETHER_CONNECTED, "<i>" + a_cascade.getName() + "</i>");
				}
				if (!bDataAboutRequestedPages)
				{
					strMessage += " " + "<b>" + JAPMessages.getString(MSG_NO_TARGET_ADDRESSES) + "</b>";
				}
			}
			else
			{
				strMessage += JAPMessages.getString(MSG_NO_CHANCE, "<i>" + a_cascade.getName() + "</i>");
			}
		}
		
		JAPDialog.showWarningDialog(component, strMessage, JAPMessages.getString(MSG_INFO_TITLE), adapter);
	}
}
