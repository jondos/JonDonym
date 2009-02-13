/*
Copyright (c) 2008 The JAP-Team, JonDos GmbH

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation and/or
       other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
       the JonDos GmbH, nor the names of their contributors may be used to endorse or
       promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package anon.infoservice;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;

import org.w3c.dom.Node;

import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class TermsAndConditionsMixInfo 
{
	public final static String TNC_MIX_INFO_ROOT = "TermsAndConditionsInfos";
	public final static String TNC_MIX_INFO = "TermsAndConditionsInfo";
	public final static String TNC_MIX_INFO_ID = "id";
	public final static String TNC_MIX_INFO_DATE = "date";
	public final static String TNC_MIX_INFO_LOCALE = "locale";
	public final static String TNC_MIX_INFO_DEFAULT_LANG = "defaultLang";
	public final static String TNC_MIX_INFO_TEMPLATE_REFID = "referenceId";
	
	private String id = "";
	private String date = "";
	private String defaultLang = "";
	private Hashtable templates = new Hashtable();
	
	public TermsAndConditionsMixInfo(Node infoRoot) throws XMLParseException		
	{
		if(infoRoot == null)
		{
			throw new XMLParseException("T&C Info Node is null");
		}
		id = XMLUtil.parseAttribute(infoRoot, TNC_MIX_INFO_ID, "");
		if(id.equals(""))
		{
			throw new XMLParseException("T&C Info Node does not contain an ID");
		}
		date = XMLUtil.parseAttribute(infoRoot, TNC_MIX_INFO_DATE, "");
		if(date.equals(""))
		{
			throw new XMLParseException("T&C Info Node "+id+" does not contain a valid date");
		}
		defaultLang = XMLUtil.parseAttribute(infoRoot, TNC_MIX_INFO_DEFAULT_LANG, "").trim().toLowerCase();
		if(defaultLang.equals(""))
		{
			throw new XMLParseException("T&C Info Node "+id+" does not define a default language");
		}	
		Node it = XMLUtil.getFirstChildByName(infoRoot, TNC_MIX_INFO);
		String currentInfoLang = "";
		String currentInfoTemplateRefId = "";
		while(it != null)
		{
			currentInfoLang = XMLUtil.parseAttribute(it, TNC_MIX_INFO_LOCALE, "");
			currentInfoTemplateRefId = XMLUtil.parseAttribute(it, TNC_MIX_INFO_TEMPLATE_REFID, "");
			if( !(currentInfoLang.equals("") || currentInfoTemplateRefId.equals("")) )
			{
				//System.out.println(id+"/"+date+" putting: "+currentInfoLang+" -> "+currentInfoTemplateRefId);
				templates.put(currentInfoLang.trim().toLowerCase(), currentInfoTemplateRefId);
			}
			it = XMLUtil.getNextSiblingByName(it, TNC_MIX_INFO);
		}
		Enumeration e = getLanguages();
	}
	
	public String getId()
	{
		return id;
	}
	
	public String getDate() 
	{
		return date;
	}
	
	public String getTemplateRefId(Locale locale)
	{
		return getTemplateRefId(locale.getLanguage());
	}
	
	public String getTemplateRefId(String langCode)
	{
		return (String) templates.get(langCode.trim().toLowerCase());
	}
	
	public String getDefaultTemplateRefId()
	{
		return (String) templates.get(getDefaultLanguage());
	}
	
	public boolean hasTranslation(String langCode)
	{
		return templates.get(langCode.trim().toLowerCase()) != null;
	}
	
	public boolean hasTranslation(Locale locale)
	{
		return hasTranslation(locale.getLanguage());
	}
	
	public String getDefaultLanguage()
	{
		return defaultLang;
	}
	
	public Enumeration getLanguages()
	{
		return templates.keys();
	}
}
