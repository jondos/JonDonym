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

import infoservice.agreement.logging.GiveThingsAName;
import infoservice.agreement.multicast.AgreementMessageTypes;

import java.util.Enumeration;
import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import anon.util.XMLUtil;

public class CommitMessage extends AMessage
{

    private Hashtable m_EchoMessages = null;

    private InitMessage m_initMessage = null;

    /**
     * Creates a new <code>CommitMessage</code>. It contains a set of at
     * least 2/3n+1 (n=count of InfoServices) <code>EchoMessages</code> signed
     * by different InfoServices.
     * 
     * @param a_msg
     *            The <code>InitMessage</code> to be committed
     * @param a_senderId
     *            The sender of this <code>CommitMessage</code> (should be the
     *            same as the initiatorsId in the <code>InitMessage</code>
     * @param a_echoMessages
     *            The <code>EchoMessage</code>s
     */
    public CommitMessage(InitMessage a_msg, String a_senderId, Hashtable a_echoMessages)
    {
        super(a_msg.getConsensusId(), AgreementMessageTypes.MESSAGE_TYPE_COMMIT, a_msg
                .getInitiatorsId(), a_msg.getProposal(), a_msg.getLastCommonRandom());
        this.m_initMessage = a_msg;
        this.m_senderId = a_senderId;
        this.m_EchoMessages = a_echoMessages;
    }

    /**
     * Returns the set of <code>EchoMessage</code>s contained in this
     * <code>CommitMessage</code>
     * 
     * @return A <code>Hashtable</code> containing the
     *         <code>EchoMessage</code>s
     */
    public Hashtable getEchoMessages()
    {
        return m_EchoMessages;
    }

    /**
     * Returns a hash key for the message
     * 
     * @return The hashkey
     */
    public String getHashKey()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("Initiator: " + getInitiatorsId());
        buf.append(" <" + AgreementMessageTypes.getTypeAsString(getMessageType()) + ">");
        buf.append(" Proposal: " + getProposal());
        buf.append(" Senders Id: " + this.getSenderId());
        buf.append(" LCR: " + this.m_lastCommonRandom.toString());

        EchoMessage echo = null;
        Enumeration en = this.m_EchoMessages.elements();
        while (en.hasMoreElements())
        {
            echo = (EchoMessage) en.nextElement();
            buf.append(echo.getHashKey());
        }
        return buf.toString();
    }

    /**
     * Returns the <code>InitMessage</code> contained in this message
     * 
     * @return The <code>InitMessage</code>
     */
    public InitMessage getInitMessage()
    {
        return this.m_initMessage;
    }

    /**
     * Sets the <code>Hashtable</code> of <code>EchoMessage</code> contained
     * in this <code>CommitMessage</code>
     * 
     * @param a_echoMessages
     *            The <code>Hashtable</code> of <code>EchoMessage</code>
     */
    public void setEchoMessages(Hashtable a_echoMessages)
    {
        m_EchoMessages = a_echoMessages;
    }

    /**
     * Returns a string representation of this message
     * 
     * @return The string represenation
     */
    public String toString()
    {
        String sendersName = GiveThingsAName.getNameForNumber(m_senderId);
        String initiatorsName = GiveThingsAName.getNameForNumber(getInitiatorsId());
        StringBuffer buf = new StringBuffer();
        buf.append("ConsensusID: " + m_consensusId.substring(0, 2));
        // buf.append(" SenderId: " + m_senderId);
        buf.append(" SenderId: " + sendersName);
        // buf.append(" Initiator: " + getInitiatorsId());
        buf.append(" Initiator: " + initiatorsName);
        buf.append(" <" + AgreementMessageTypes.getTypeAsString(getMessageType()) + ">");
        buf.append(" Proposal: " + getProposal());
        buf.append(" LCR: " + this.m_lastCommonRandom.toString().substring(0, 2));

        EchoMessage echo = null;
        Enumeration en = this.m_EchoMessages.elements();
        while (en.hasMoreElements())
        {
            echo = (EchoMessage) en.nextElement();
            buf.append("\n     EchoMessage" + echo);
        }
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.multicast.messages.AMessage#appendCustomNodes(org.w3c.dom.Document,
     *      org.w3c.dom.Node)
     */
    protected void appendCustomNodes(Document a_doc, Node a_rootElem)
    {
        Node tmp = a_doc.createElement("SenderId");
        XMLUtil.setValue(tmp, this.m_senderId);
        a_rootElem.appendChild(tmp);

        Node echoes = a_doc.createElement("EchoMessages");
        a_rootElem.appendChild(echoes);

        Enumeration en = m_EchoMessages.keys();
        while (en.hasMoreElements())
        {
            EchoMessage msg = (EchoMessage) m_EchoMessages.get(en.nextElement());
            Node node = msg.toXML().getFirstChild();
            /**
             * @todo This happend sometimes but I think the problem is solved,
             * leave it just to be sure. As a note: It might be that the problem
             * came from AMessage.toXML not beeing synchronized
             */
            if (node == null)
            {
                LogHolder.log(LogLevel.ALERT, LogType.NET, "Node was null again, thats bad!");
                continue;
            }
            Node msgNode = a_doc.importNode(node, true);
            echoes.appendChild(msgNode);
        }
    }
}
