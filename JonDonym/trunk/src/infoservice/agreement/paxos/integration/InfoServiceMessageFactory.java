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
import infoservice.agreement.paxos.messages.PaxosMessage;

import java.util.Enumeration;
import java.util.Vector;

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
import anon.util.ZLibTools;

public class InfoServiceMessageFactory
{
    /**
     * Decodes the given POST data and creates a PaxosMessage off it
     * 
     * @param a_data
     *            The POST data to be decoded
     * @return A PaxosMessage or null if there has been an error
     */
    public static PaxosMessage decode(byte[] a_data)
    {
        byte[] data = null;
        try
        {
            data = ZLibTools.decompress(a_data);
            Document doc = XMLUtil.toXMLDocument(data);
            return parseDocument(doc);
        }
        catch (Exception e)
        {
            LogHolder.log(LogLevel.ERR, LogType.AGREEMENT, "Unable to decode given message");
        }
        return null;
    }

    /**
     * Parses the given document to a PaxosMessage
     * 
     * @param a_doc
     * @return
     */
    private static PaxosMessage parseDocument(Document a_doc)
    {

        Node rootNode = a_doc.getFirstChild();
        String nodeName = rootNode.getNodeName();
        PaxosMessage result = null;
        if (nodeName.equals(PaxosMessage.FREEZEPROOF))
            result = new FreezeProofMessage();
        else if (nodeName.equals(PaxosMessage.COLLECT))
            result = new CollectMessage();
        else
            result = new PaxosMessage(nodeName);
        Node tmp = XMLUtil.getFirstChildByName(rootNode, "RoundNr");
        result.setRound(XMLUtil.parseValue(tmp, -1));
        tmp = XMLUtil.getFirstChildByName(rootNode, "Initiator");
        result.setInitiator(XMLUtil.parseValue(tmp, ""));
        tmp = XMLUtil.getFirstChildByName(rootNode, "Proposal");
        result.setProposal(XMLUtil.parseValue(tmp, ""));
        tmp = XMLUtil.getFirstChildByName(rootNode, "Sender");
        result.setSender(XMLUtil.parseValue(tmp, ""));
        tmp = XMLUtil.getFirstChildByName(rootNode, "PaxosInstance");
        result.setPaxosInstanceIdentifier(XMLUtil.parseValue(tmp, ""));
        if (result instanceof FreezeProofMessage)
        {
            tmp = XMLUtil.getFirstChildByName(rootNode, "WeakValue");
            ((FreezeProofMessage) result).setWeakValue(XMLUtil.parseValue(tmp, ""));
            tmp = XMLUtil.getFirstChildByName(rootNode, "StrongValue");
            ((FreezeProofMessage) result).setStrongValue(XMLUtil.parseValue(tmp, ""));
            ((FreezeProofMessage) result).setSignedData(rootNode);
        }
        if (result instanceof CollectMessage)
        {
            parseProofs(rootNode, (CollectMessage) result);
        }
        if (isSane(a_doc, result))
            return result;
        return null;
    }

    /**
     * Parse the given node for FreezeProofMessages and add them to the given
     * CollectMessage
     * 
     * @param rootNode
     *            The node containing the proofs
     * @param message
     *            The CollectMessage to which to add the FreezeProofs
     */
    private static void parseProofs(Node rootNode, CollectMessage message)
    {
        Node proofs = XMLUtil.getFirstChildByName(rootNode, "FreezeProofs");
        Node proof = proofs.getFirstChild();
        Vector freezeProofs = new Vector();
        do
        {
            FreezeProofMessage msg = createFreezeProof(proof);
            freezeProofs.add(msg);
            proof = proof.getNextSibling();
        }
        while (proof != null);
        message.setProofs(freezeProofs);
    }

    /**
     * Creates a FreezeProofMessage off the given node
     * 
     * @param rootNode
     *            The node to be parsed
     * @return A FreezeProofMessage
     */
    private static FreezeProofMessage createFreezeProof(Node rootNode)
    {
        FreezeProofMessage result = new FreezeProofMessage();
        Node tmp = XMLUtil.getFirstChildByName(rootNode, "RoundNr");
        result.setRound(XMLUtil.parseValue(tmp, -1));
        tmp = XMLUtil.getFirstChildByName(rootNode, "Initiator");
        result.setInitiator(XMLUtil.parseValue(tmp, ""));
        tmp = XMLUtil.getFirstChildByName(rootNode, "Proposal");
        result.setProposal(XMLUtil.parseValue(tmp, ""));
        tmp = XMLUtil.getFirstChildByName(rootNode, "Sender");
        result.setSender(XMLUtil.parseValue(tmp, ""));
        tmp = XMLUtil.getFirstChildByName(rootNode, "PaxosInstance");
        result.setPaxosInstanceIdentifier(XMLUtil.parseValue(tmp, ""));
        tmp = XMLUtil.getFirstChildByName(rootNode, "WeakValue");
        result.setWeakValue(XMLUtil.parseValue(tmp, ""));
        tmp = XMLUtil.getFirstChildByName(rootNode, "StrongValue");
        result.setStrongValue(XMLUtil.parseValue(tmp, ""));
        result.setSignatureOk(SignatureVerifier.getInstance().verifyXml((Element) rootNode,
                SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE));
        result.setSignedData(rootNode);
        return result;
    }

    /**
     * Checks if the sender of the message also signed the message, i.e. the
     * senders id equals the subjectKeyIdentifier of the attached certificate
     * 
     * @param a_doc
     * @param a_msg
     * @return
     */
    private static boolean isSane(Document a_doc, PaxosMessage a_msg)
    {
        boolean result = true;
        result &= checkOriginator(a_doc, a_msg.getSender());
        result &= SignatureVerifier.getInstance().verifyXml(a_doc,
                SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
        if (a_msg instanceof FreezeProofMessage)
            ((FreezeProofMessage) a_msg).setSignatureOk(result);

        if (a_msg instanceof CollectMessage)
            result &= checkProofs((CollectMessage) a_msg);
        return result;
    }

    /**
     * Checks the signatures of the attached FreezeProofMessages
     * 
     * @param message
     * @return
     */
    private static boolean checkProofs(CollectMessage message)
    {
        boolean result = true;
        Enumeration en = message.getProofs().elements();
        while (en.hasMoreElements())
        {
            FreezeProofMessage msg = (FreezeProofMessage) en.nextElement();
            result &= msg.isSignatureOk();
        }
        return result;
    }

    /**
     * Checks the originator of the message, i.e. if the senders id equals the
     * subjectKeyIdentifier of the attached certificate
     * 
     * @param a_node
     * @param a_sender
     * @return
     */
    private static boolean checkOriginator(Node a_node, String a_sender)
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

}
