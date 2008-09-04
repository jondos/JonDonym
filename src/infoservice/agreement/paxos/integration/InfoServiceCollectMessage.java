/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package infoservice.agreement.paxos.integration;

import infoservice.agreement.paxos.messages.CollectMessage;
import infoservice.agreement.paxos.messages.FreezeProofMessage;

import java.util.Enumeration;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.IDistributable;
import anon.util.XMLUtil;
import anon.util.ZLibTools;

public class InfoServiceCollectMessage implements IDistributable
{
    private CollectMessage m_message;

    private byte[] m_compressedData = null;

    /**
     * Creates a decorating InfoServiceCollectMessage for the given message
     * 
     * @param a_msg
     */
    public InfoServiceCollectMessage(CollectMessage a_msg)
    {
        m_message = a_msg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see anon.infoservice.IDistributable#getId()
     */
    public String getId()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see anon.infoservice.IDistributable#getPostFile()
     */
    public String getPostFile()
    {
        return "/agreement";
    }

    /*
     * (non-Javadoc)
     * 
     * @see anon.infoservice.IDistributable#getPostEncoding()
     */
    public int getPostEncoding()
    {
        return HTTPConnectionFactory.HTTP_ENCODING_ZLIB;
    }

    /*
     * (non-Javadoc)
     * 
     * @see anon.infoservice.IDistributable#getPostData()
     */
    public byte[] getPostData()
    {
        synchronized (this)
        {
            // Lazy compression
            if (m_compressedData == null)
            {
                Document doc = XMLUtil.createDocument();
                Node root = doc.createElement(m_message.getMessageType());
                doc.appendChild(root);

                Node tmp = doc.createElement("RoundNr");
                XMLUtil.setValue(tmp, m_message.getRound());
                root.appendChild(tmp);

                tmp = doc.createElement("Initiator");
                XMLUtil.setValue(tmp, m_message.getInitiator());
                root.appendChild(tmp);

                tmp = doc.createElement("Proposal");
                XMLUtil.setValue(tmp, m_message.getProposal());
                root.appendChild(tmp);

                tmp = doc.createElement("Sender");
                XMLUtil.setValue(tmp, m_message.getSender());
                root.appendChild(tmp);

                tmp = doc.createElement("PaxosInstance");
                XMLUtil.setValue(tmp, m_message.getPaxosInstanceIdentifier());
                root.appendChild(tmp);

                Node proofs = doc.createElement("FreezeProofs");
                root.appendChild(proofs);

                Enumeration en = m_message.getProofs().elements();
                while (en.hasMoreElements())
                {
                    FreezeProofMessage fpm = (FreezeProofMessage) en.nextElement();
                    Node fpmn = (Node) fpm.getSignedData();
                    Node g = doc.importNode(fpmn, true);
                    proofs.appendChild(g);
                }

                SignatureCreator.getInstance().signXml(
                        SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE, doc);
                m_compressedData = ZLibTools.compress(XMLUtil.toByteArray(doc));
            }
        }
        return m_compressedData;
    }

}
