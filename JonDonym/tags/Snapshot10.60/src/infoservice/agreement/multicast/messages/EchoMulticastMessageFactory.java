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

import infoservice.agreement.multicast.AgreementMessageTypes;
import infoservice.agreement.multicast.interfaces.IAgreementMessage;

import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.crypto.XMLSignature;
import anon.util.XMLUtil;

public class EchoMulticastMessageFactory
{

    /* Singleton instance */
    private static final EchoMulticastMessageFactory m_instance = new EchoMulticastMessageFactory();

    /**
     * Singleton pattern
     * 
     * @return The only instance of <code>EchoMulticastMessageFactory</code>
     */
    public static EchoMulticastMessageFactory getInstance()
    {
        return m_instance;
    }

    /**
     * Parses the given XML Document and creates an instance of
     * <code>IAgreementMessage</code> depending on which type of message is
     * contained in the XML.
     * 
     * @param a_doc
     *            The document to be parsed
     * @return An <code>IAgreementMessage</code>
     */
    public synchronized IAgreementMessage parseMessage(Document a_doc)
    {
        Node rootNode = a_doc.getFirstChild();

        Node tmp = XMLUtil.getFirstChildByName(rootNode, "RoundNr");
        String lastCommonRandom = getNodeValue(tmp);
        tmp = XMLUtil.getFirstChildByName(rootNode, "Initiator");
        String initiatorsId = getNodeValue(tmp);
        tmp = XMLUtil.getFirstChildByName(rootNode, "Message");
        String proposal = getNodeValue(tmp);
        String nodeName = rootNode.getNodeName();
        String senderId = "";

        /*
         * Check if the senders id equals the subject key identifier of the
         * appended certificate. If not, discard the message, it is forged
         */
        boolean senderOk = false;
        if (!AgreementMessageTypes.getTypeAsString(AgreementMessageTypes.MESSAGE_TYPE_INIT)
                .equalsIgnoreCase(nodeName))
        {
            tmp = XMLUtil.getFirstChildByName(rootNode, "SenderId");
            senderId = getNodeValue(tmp);
            senderOk = checkOriginator(rootNode, senderId);
        }
        else
        {
            senderOk = checkOriginator(rootNode, initiatorsId);
        }
        if (!senderOk)
        {
            return null;
        }

        IAgreementMessage message = null;
        if (AgreementMessageTypes.getTypeAsString(AgreementMessageTypes.MESSAGE_TYPE_INIT)
                .equalsIgnoreCase(nodeName))
        {
            message = new InitMessage(initiatorsId, proposal, lastCommonRandom);
        }
        else if (AgreementMessageTypes.getTypeAsString(AgreementMessageTypes.MESSAGE_TYPE_ECHO)
                .equalsIgnoreCase(nodeName))
        {
            InitMessage tmpMsg = new InitMessage(initiatorsId, proposal, lastCommonRandom);
            message = new EchoMessage(tmpMsg, senderId);
            message.setXmlDocument(a_doc);
        }
        else if (AgreementMessageTypes.getTypeAsString(AgreementMessageTypes.MESSAGE_TYPE_COMMIT)
                .equalsIgnoreCase(nodeName))
        {
            tmp = XMLUtil.getFirstChildByName(rootNode, "EchoMessages");
            Node echo = tmp.getFirstChild();
            Hashtable echoMessages = new Hashtable();
            do
            {
                EchoMessage tmpMsg = createEchoMessage(echo);
                echoMessages.put(tmpMsg.getHashKey(), tmpMsg);
                echo = echo.getNextSibling();
            }
            while (echo != null);
            InitMessage tmpMsg = new InitMessage(initiatorsId, proposal, lastCommonRandom);
            message = new CommitMessage(tmpMsg, senderId, echoMessages);
        }
        else if (AgreementMessageTypes.getTypeAsString(AgreementMessageTypes.MESSAGE_TYPE_REJECT)
                .equalsIgnoreCase(nodeName))
        {
            InitMessage tmpMsg = new InitMessage(initiatorsId, proposal, lastCommonRandom);
            message = new RejectMessage(tmpMsg, senderId, lastCommonRandom);
        }
        else if (AgreementMessageTypes.getTypeAsString(
                AgreementMessageTypes.MESSAGE_TYPE_CONFIRMATION).equalsIgnoreCase(nodeName))
        {
            tmp = XMLUtil.getFirstChildByName(rootNode, "EchoMessages");
            Node echo = tmp.getFirstChild();
            Hashtable echoMessages = new Hashtable();
            do
            {
                EchoMessage tmpMsg = createEchoMessage(echo);
                echoMessages.put(tmpMsg.getHashKey(), tmpMsg);
                echo = echo.getNextSibling();
            }
            while (echo != null);

            InitMessage tmpMsg = new InitMessage(initiatorsId, proposal, lastCommonRandom);
            message = new ConfirmationMessage(new CommitMessage(tmpMsg, senderId, echoMessages),
                    senderId);
        }
        else
        {
            LogHolder.log(LogLevel.ERR, LogType.ALL,
                    "Couldn't determine type of agreement message: " + nodeName);
            return null;
        }
        boolean sigOk = false;
        sigOk = SignatureVerifier.getInstance().verifyXml((Element) rootNode,
                SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
        if (message != null)
            message.setSignatureOk(sigOk);
        return message;
    }

    private synchronized String getNodeValue(Node tmp)
    {
        String value = XMLUtil.parseValue(tmp, null);
        if (value == null)
        {
            LogHolder.log(LogLevel.ALERT, LogType.NET, "Unable to get value from node  "
                    + tmp.getNodeName());
            return "";
        }
        return value;

    }

    private boolean checkOriginator(Node a_node, String a_sender)
    {
        String ski = "";
        try
        {
            XMLSignature t = XMLSignature.getUnverified(a_node);
            if (t != null)
            {
                ski = t.getXORofSKIs();
            }
        }
        catch (Exception e)
        {
            // Ignored
        }
        boolean ok = a_sender.equalsIgnoreCase(ski);
        if (!ok)
        {
            LogHolder.log(LogLevel.ALERT, LogType.NET,
                    "Possibly forged message received! SenderID: " + a_sender
                            + " was signed with a certificate with SKI: " + ski + ")");
        }
        return ok;
    }

    /**
     * Creates an <code>EchoMessage</code> out of the given node
     * 
     * @param a_node
     *            The node containing the XML representation of the
     *            <code>EchoMessage</code>
     * @return The <code>EchoMessage</code>
     */
    private synchronized EchoMessage createEchoMessage(Node a_node)
    {
        Node tmp = XMLUtil.getFirstChildByName(a_node, "RoundNr");
        String lastCommonRandom = getNodeValue(tmp);
        tmp = XMLUtil.getFirstChildByName(a_node, "Initiator");
        String initiatorsId = getNodeValue(tmp);
        tmp = XMLUtil.getFirstChildByName(a_node, "Message");
        String proposal = getNodeValue(tmp);
        boolean sigOk = false;
        sigOk = SignatureVerifier.getInstance().verifyXml((Element) a_node,
                SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
        InitMessage tmpMsg = new InitMessage(initiatorsId, proposal, lastCommonRandom);
        tmp = XMLUtil.getFirstChildByName(a_node, "SenderId");
        String senderId = getNodeValue(tmp);
        if (!checkOriginator(a_node, senderId))
            return null;
        EchoMessage message = new EchoMessage(tmpMsg, senderId);
        message.setXmlDocument(a_node.getOwnerDocument());
        message.setSignatureOk(sigOk);
        return message;
    }
}
