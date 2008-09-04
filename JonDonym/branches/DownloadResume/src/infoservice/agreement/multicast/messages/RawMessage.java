/*
 Copyright (c) 2000 - 2005, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package infoservice.agreement.multicast.messages;

import infoservice.agreement.multicast.interfaces.IAgreementMessage;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;

import anon.util.XMLUtil;
import anon.util.ZLibTools;

/**
 * @author ss1
 * 
 */
public class RawMessage
{
    /* The POST data */
    private byte[] m_postData;

    /**
     * Creates a new <code>RawMessage</code> from the given POST data
     * 
     * @param a_postData
     *            The POST data containing an XML structure, containing an
     *            <code>IAgreementMessage</code>
     */
    public RawMessage(byte[] a_postData)
    {
        this.m_postData = a_postData;
    }

    /**
     * Returns the <code>IAgreementMessage</code> contained in the compressed
     * m_postData of this message
     * 
     * @return The <code>IAgreementMessage</code>
     */
    public IAgreementMessage getAgreementMessage()
    {
        IAgreementMessage message = null;
        byte[] data = null;
        try
        {
            data = ZLibTools.decompress(m_postData);
            Document doc = XMLUtil.toXMLDocument(data);
            message = EchoMulticastMessageFactory.getInstance().parseMessage(doc);
        }
        catch (Exception e)
        {
            LogHolder.log(LogLevel.ERR, LogType.ALL,
                    "Couldn't create IAgreementMessage from the data: " + e.toString());
        }
        return message;
    }
}
