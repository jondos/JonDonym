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
					return "Weitere Informationen";
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
			strMessage += "Die Mixe der Kaskade {0} erstellen keine Logdaten die Ihrer Anonymität schaden könnten.";
		}
		else
		{
			if (bFirstMixOnly)
			{
				strMessage = "Der Eingangs-Mix der Kaskade {0} speichert die folgenden Verbindungsdaten auf Vorrat:";
			}
			else if (a_mixID < 0)
			{
				strMessage = "Die Mixe der Kaskade {0} speichern die folgenden Verbindungsdaten auf Vorrat:";
			}
			else
			{
				strMessage = "Der Mix {0} des Betreibers {1} speichert die folgenden Verbindungsdaten auf Vorrat:";
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
			strMessage += "<p>Speicherdauer: ";
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
				strMessage += "Im Rahmen einer Strafverfolgung unter Richtervorbehalt könnte dadurch nachvollzogen werden, ";
				
				if (bFirstMixOnly || !drInfoCascade.isLogged(DataRetentionInformation.INPUT_CHANNEL_ID) ||
					!drInfoCascade.isLogged(DataRetentionInformation.OUTPUT_CHANNEL_ID))
				{
					if (drInfoFirstMix.isLogged(DataRetentionInformation.INPUT_TIME) ||
						drInfoFirstMix.isLogged(DataRetentionInformation.INPUT_CHANNEL_ID))
					{
						strMessage += "ob Sie die Kaskade {0} zu einer bestimmten Zeit genutzt haben.";
					}
					else
					{
						strMessage += "ob Sie zu einer bestimmten Zeit mit der Kaskade {0} verbunden waren.";
					}
				}
				else if (drInfoCascade.isLogged(DataRetentionInformation.INPUT_TIME))
				{
					if (drInfoLastMix.isLogged(DataRetentionInformation.OUTPUT_TARGET_DOMAIN) ||
						drInfoLastMix.isLogged(DataRetentionInformation.OUTPUT_TARGET_IP_ADDRESS))
					{
						bDataAboutRequestedPages = true;
						strMessage += "welche Internetadresse Sie zu einer bestimmten Zeit kontaktiert haben.";
					}
					else if (drInfoLastMix.isLogged(DataRetentionInformation.OUTPUT_SOURCE_IP_PORT))
					{
						strMessage += "ob Sie zu einer bestimmten Zeit eine bestimmten Internetserver kontaktiert haben. " +
								"Falls andere Nutzer gleichzeitig mit Ihnen Daten über die Kaskade ins Internet gesendet haben, wird diese Aufdeckung jedoch stark erschwert oder sogar unmöglich.";
					}
				}
				else
				{
					strMessage += "ob Sie zu einer bestimmten Zeit mit der Kaskade {0} verbunden waren.";
				}
				if (!bDataAboutRequestedPages)
				{
					strMessage += " <b>Es werden jedoch KEINE Daten über aufgerufene Internetadressen oder Webseiten gespeichert.</b>";
				}
			}
			else
			{
				strMessage += "<b>Mit diesen Daten kann keine Ihrer Verbindungen aufgedeckt werden</b>, da der erste Mix der Kaskade {0} Ihre IP-Adresse nicht speichert.";
			}
		}
		
		JAPDialog.showWarningDialog(component, strMessage, "Informationen zur Vorratsdatenspeicherung",
				adapter);
	}
	
}
