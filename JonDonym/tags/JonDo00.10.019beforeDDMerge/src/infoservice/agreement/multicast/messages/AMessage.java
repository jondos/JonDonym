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
import infoservice.agreement.multicast.interfaces.IAgreementMessage;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.infoservice.HTTPConnectionFactory;
import anon.util.Base64;
import anon.util.XMLUtil;
import anon.util.ZLibTools;

public abstract class AMessage implements IAgreementMessage
{

    protected String m_lastCommonRandom;

    protected String m_consensusId = "";

    protected String m_initiatorsId;

    protected int m_messageType = -1;

    protected String m_proposal = "";

    protected String m_senderId = "";

    protected boolean m_signatureOk = true;

    protected Document m_xmlNode = null;

    private byte[] m_compressedData = null;

    /**
     * Creates a new message. This constructor is used by the subclasses of
     * AMessage
     * 
     * @param a_consensusId
     *            The unique id of the consensus this message belongs to
     * @param a_messageType
     *            The type of the message (see
     *            <code>AgreementMessageTypes</code>)
     * @param a_initiatorsId
     *            The id of the initiator if the consensus
     * @param p_proposal
     *            The proposal (i.e. broadcasted message)
     * @param m_lastCommonRandom
     *            The last commonly known random number as a round identifier
     */
    protected AMessage(String a_consensusId, int a_messageType, String a_initiatorsId,
            String p_proposal, String lastCommonRandom)
    {
        this.m_messageType = a_messageType;
        this.m_initiatorsId = a_initiatorsId;
        this.m_proposal = p_proposal;
        this.m_lastCommonRandom = lastCommonRandom;
        if (a_consensusId == null && a_messageType == AgreementMessageTypes.MESSAGE_TYPE_INIT)
        {
            this.m_consensusId = createConsensusId();
        }
        else
        {
            this.m_consensusId = a_consensusId;
        }
    }

    /**
     * Subclasses can add some extra nodes into the xml representation of this
     * message by implementing this method
     * 
     * @param a_doc
     *            The owner document
     * @param a_rootElem
     *            The node to which the extra nodes should be added
     */
    protected abstract void appendCustomNodes(Document a_doc, Node a_rootElem);

    /**
     * Creates a unique id for this broadcast
     * 
     * @return The id for the broadcast
     */
    private String createConsensusId()
    {
        SHA1Digest digest = new SHA1Digest();
        byte[] proposalBytes = (m_initiatorsId + m_lastCommonRandom).getBytes();
        digest.update(proposalBytes, 0, proposalBytes.length);
        byte[] tmp = new byte[digest.getDigestSize()];
        digest.doFinal(tmp, 0);
        return Base64.encode(tmp, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.interfaces.IAgreementMessage#getConsensusId()
     */
    public String getConsensusId()
    {
        return this.m_consensusId;
    }

    /**
     * Returns a hash key for the message
     * 
     * @return The hash key
     */
    public String getHashKey()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("Initiator: " + getInitiatorsId());
        buf.append(" <" + AgreementMessageTypes.getTypeAsString(getMessageType()) + ">");
        buf.append(" Proposal: " + getProposal());
        buf.append(" LCR: " + this.m_lastCommonRandom.toString());
        buf.append(" Sender: ").append(m_senderId);
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see anon.infoservice.IDistributable#getId()
     */
    public String getId()
    {
        return this.m_consensusId;
    }

    /**
     * Returns the initiators id
     * 
     * @return The initiators id
     */
    public String getInitiatorsId()
    {
        return m_initiatorsId;
    }

    /**
     * Returns the last common random used as a round number
     * 
     * @return The last common random
     */
    public String getLastCommonRandom()
    {
        return m_lastCommonRandom;
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.interfaces.IAgreementMessage#getMessageType()
     */
    public int getMessageType()
    {
        return m_messageType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see anon.infoservice.IDistributable#getPostData()
     */
    public synchronized byte[] getPostData()
    {
        if (m_compressedData == null)
            m_compressedData = ZLibTools.compress(XMLUtil.toByteArray(this.toXML()));
        return m_compressedData;
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

    /**
     * Returns the proposal
     * 
     * @return The proposal
     */
    public String getProposal()
    {
        return m_proposal;
    }

    /**
     * Returns the senders id
     * 
     * @return The senders id
     */
    public String getSenderId()
    {
        return m_senderId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.interfaces.IAgreementMessage#isSignatureOK()
     */
    public boolean isSignatureOK()
    {
        return m_signatureOk;
    }

    /**
     * Sets the consensusId
     * 
     * @param a_id
     */
    public void setConsensusId(String a_id)
    {
        this.m_consensusId = a_id;
    }

    /**
     * Sets the proposal
     * 
     * @param a_proposal
     *            The proposal
     */
    public void setProposal(String a_proposal)
    {
        this.m_proposal = a_proposal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.interfaces.IAgreementMessage#setSignatureOk(boolean)
     */
    public void setSignatureOk(boolean a_ok)
    {
        this.m_signatureOk = a_ok;
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.interfaces.IAgreementMessage#setXmlNode(org.w3c.dom.Node)
     */
    public void setXmlDocument(Document a_node)
    {
        m_xmlNode = a_node;
    }

    /**
     * Returns a string representation of this message
     * 
     * @return The string represenation.
     */
    public String toString()
    {
        String name = GiveThingsAName.getNameForNumber(getInitiatorsId());
        StringBuffer buf = new StringBuffer();
        buf.append("ConsensusID: " + (m_consensusId.substring(0, 2)));
        // buf.append(" Initiator: " + getInitiatorsId());
        buf.append(" Initiator: " + name);
        buf.append(" <" + AgreementMessageTypes.getTypeAsString(getMessageType()) + ">");
        buf.append(" Propsal: " + getProposal());
        buf.append(" LCR: " + (this.m_lastCommonRandom.substring(0, 2)).toString());
        return buf.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.interfaces.IAgreementMessage#toXML()
     */
    public synchronized Document toXML()
    {
        if (m_xmlNode != null)
        {
            return m_xmlNode;
        }

        Document doc = XMLUtil.createDocument();
        Node root = doc.createElement(AgreementMessageTypes.getTypeAsString(this.m_messageType));
        doc.appendChild(root);

        Node tmp = doc.createElement("RoundNr");
        XMLUtil.setValue(tmp, this.m_lastCommonRandom);
        root.appendChild(tmp);

        tmp = doc.createElement("Initiator");
        XMLUtil.setValue(tmp, this.m_initiatorsId);
        root.appendChild(tmp);

        tmp = doc.createElement("Message");
        XMLUtil.setValue(tmp, this.m_proposal);
        root.appendChild(tmp);

        tmp = doc.createElement("ConsensusId");
        XMLUtil.setValue(tmp, this.m_consensusId);
        root.appendChild(tmp);

        appendCustomNodes(doc, root);
        SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE, doc);
        return doc;
    }

    public void setLastCommonRandom(String lcr)
    {
        this.m_lastCommonRandom = lcr;
    }

}
