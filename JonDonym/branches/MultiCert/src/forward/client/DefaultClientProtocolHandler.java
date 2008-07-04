/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package forward.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.crypto.SignatureVerifier;
import anon.infoservice.MixCascade;
import anon.shared.ProxyConnection;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * This is the implementation of the client side for the first version of the JAP routing protocol.
 */
public class DefaultClientProtocolHandler
{

	/**
	 * This is the version of the current protocol implementation. Interaction is only possible with
	 * forwarders, which use the same protocol version.
	 */
	private static final int PROTOCOL_VERSION = 2;

	/**
	 * This is the maximum net size of a protocol message in bytes. Bigger messages are not accepted
	 * and causes an exception.
	 */
	private static final int MAXIMUM_PROTOCOLMESSAGE_SIZE = 1000000;

	/**
	 * This is the state after the physical connection to the forwarder is established and the
	 * protocol is initialized.
	 */
	private static final int STATE_INITIALIZE = 0;

	/**
	 * This is the state after receiving the forward connection offer from the forwarder.
	 */
	private static final int STATE_OFFER_RECEIVED = 1;

	/**
	 * This is the state after we have sent our selected MixCascade to the forwarder.
	 */
	private static final int STATE_CASCADE_SELECTED = 2;

	/**
	 * This is the state while forwarding is active.
	 */
	private static final int STATE_FORWARDING = 3;

	/**
	 * This is the state after we have got an error and closed the connection.
	 */
	private static final int STATE_CLOSED_AFTER_ERROR = 4;

	/**
	 * This is the start signature of every protocol message.
	 */
	private static final byte[] MESSAGE_START_SIGNATURE =
		{
		(byte) 0xFF, (byte) 0x00, (byte) 0xF0, (byte) 0x0F};

	/**
	 * This is the end signature of every protocol message.
	 */
	private static final byte[] MESSAGE_END_SIGNATURE =
		{
		(byte) 0xFF, (byte) 0x00, (byte) 0xE1, (byte) 0x1E};

	/**
	 * This stores the forwarded connection.
	 */
	private ProxyConnection m_connection;

	/**
	 * This stores the internal protocol state. See the constants in this class.
	 */
	private int m_state;

	/**
	 * This stores the minimal dummy traffic interval (in ms) for the forwarder. We have to create
	 * dummy traffic with at least that rate. If this value is -1, the forwarder doesn't need dummy
	 * traffic.
	 */
	private int m_minDummyTrafficInterval;

	/**
	 * This stores the selected MixCascade.
	 */
	private MixCascade m_selectedMixCascade;

	/**
	 * Creates a new instance of DefaultClientProtocol handler.
	 *
	 * @param a_connection A active ProxyConnection to a forwarder.
	 */
	public DefaultClientProtocolHandler(ProxyConnection a_connection)
	{
		m_connection = a_connection;
		m_state = STATE_INITIALIZE;
	}

	/**
	 * Returns the connection descriptor with the offer from the forwarder. This method must be
	 * called once after creating this protocol handler. If there is an error on the connection
	 * while receiving the connection offer, an ClientForwardException is thrown with detailed
	 * information.
	 *
	 * @return The connection descriptor with the connection offer from the forwarder.
	 */
	public ForwardConnectionDescriptor getConnectionDescriptor() throws ClientForwardException
	{
		ForwardConnectionDescriptor connectionDescriptor = new ForwardConnectionDescriptor();
		if (m_state == STATE_INITIALIZE)
		{
			/* we have the correct state -> send the connection request message */
			byte[] protocolPacket = null;
			try
			{
				protocolPacket = xmlToProtocolPacket(generateConnectionRequest());
			}
			catch (Exception e)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "XML transforming error (" +
												  e.toString() + ")."));
			}
			sendProtocolMessage(protocolPacket);
			byte[] message = readProtocolMessage();
			Document doc = null;
			try
			{
				/* all messages are XML documents */
				doc = XMLUtil.toXMLDocument(message);
			}
			catch (Exception e)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error while parsing XML message (" +
												  e.toString() + ")."));
			}
			NodeList japRoutingNodes = doc.getElementsByTagName("JAPRouting");
			if (japRoutingNodes.getLength() == 0)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (JAPRouting node)."));
			}
			Element japRoutingNode = (Element) (japRoutingNodes.item(0));
			/* check the routing protocol version */
			NodeList protocolNodes = japRoutingNode.getElementsByTagName("Protocol");
			if (protocolNodes.getLength() == 0)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (Protocol node)."));
			}
			Element protocolNode = (Element) (protocolNodes.item(0));
			int protocolVersion = -1;
			try
			{
				protocolVersion = Integer.parseInt(protocolNode.getAttribute("version"));
			}
			catch (Exception e)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (Protocol node -> version info)."));
			}
			if (protocolVersion != PROTOCOL_VERSION)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_VERSION_ERROR,
												  "Forwarder is using protocol version " +
												  Integer.toString(protocolVersion) + ", but we use version " +
												  Integer.toString(PROTOCOL_VERSION) + "."));
			}
			/* get the request node and check the attributes */
			NodeList requestNodes = japRoutingNode.getElementsByTagName("Request");
			if (requestNodes.getLength() == 0)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (Request node)."));
			}
			Element requestNode = (Element) (requestNodes.item(0));
			String subject = requestNode.getAttribute("subject");
			if (!subject.equals("connection"))
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (Request node -> subject)."));
			}
			String msg = requestNode.getAttribute("msg");
			if (!msg.equals("offer"))
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (Request node -> msg)."));
			}
			/* get all cascades supported by the forwarder */
			NodeList allowedCascadesNodes = requestNode.getElementsByTagName("AllowedCascades");
			if (allowedCascadesNodes.getLength() == 0)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (AllowedCascades node)."));
			}
			Element allowedCascadesNode = (Element) (allowedCascadesNodes.item(0));
			NodeList mixCascadeNodes = allowedCascadesNode.getElementsByTagName("MixCascade");
			for (int i = 0; i < mixCascadeNodes.getLength(); i++)
			{
				Element currentMixCascadeNode = (Element) (mixCascadeNodes.item(i));
				/* check the signature of the mixcascade structures */
				try
				{
					MixCascade cascade = new MixCascade(currentMixCascadeNode);
					if (cascade.isVerified())
					{
						/* signature is valid, try to add that mixcascade to the descriptor mixcascade list */

						connectionDescriptor.addMixCascade(cascade);
					}
					else
					{
						/* certificate check failed */
						LogHolder.log(LogLevel.ERR, LogType.MISC,
									  "Signature check for a MixCascade failed.");
					}
				}
				catch (XMLParseException e)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC,
								  "Error while parsing MixCascade", e);
				}

			}
			/* get the quality of service information */
			NodeList qualityOfServiceNodes = requestNode.getElementsByTagName("QualityOfService");
			if (qualityOfServiceNodes.getLength() == 0)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (QualityOfService node)."));
			}
			Element qualityOfServiceNode = (Element) (qualityOfServiceNodes.item(0));
			NodeList maximumBandwidthNodes = qualityOfServiceNode.getElementsByTagName("MaximumBandwidth");
			if (maximumBandwidthNodes.getLength() == 0)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (MaximumBandwidth node)."));
			}
			Element maximumBandwidthNode = (Element) (maximumBandwidthNodes.item(0));
			int maximumBandwidth = -1;
			try
			{
				maximumBandwidth = Integer.parseInt(maximumBandwidthNode.getFirstChild().getNodeValue());
			}
			catch (Exception e)
			{
			}
			if (maximumBandwidth < 0)
			{
				/* error while parsing or illegal value */
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (MaximumBandwidth has illegal value)."));
			}
			connectionDescriptor.setMaximumBandwidth(maximumBandwidth);
			NodeList guaranteedBandwidthNodes = qualityOfServiceNode.getElementsByTagName(
				"GuaranteedBandwidth");
			if (guaranteedBandwidthNodes.getLength() == 0)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (GuaranteedBandwidth node)."));
			}
			Element guaranteedBandwidthNode = (Element) (guaranteedBandwidthNodes.item(0));
			int guaranteedBandwidth = -1;
			try
			{
				guaranteedBandwidth = Integer.parseInt(guaranteedBandwidthNode.getFirstChild().getNodeValue());
			}
			catch (Exception e)
			{
			}
			if (guaranteedBandwidth < 0)
			{
				/* error while parsing or illegal value */
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (GuaranteedBandwidth has illegal value)."));
			}
			connectionDescriptor.setGuaranteedBandwidth(guaranteedBandwidth);
			/* get the dummy traffic information */
			NodeList dummyTrafficNodes = japRoutingNode.getElementsByTagName("DummyTraffic");
			if (dummyTrafficNodes.getLength() == 0)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (DummyTraffic node)."));
			}
			Element dummyTrafficNode = (Element) (dummyTrafficNodes.item(0));
			try
			{
				m_minDummyTrafficInterval = Integer.parseInt(dummyTrafficNode.getAttribute("interval"));
				if (m_minDummyTrafficInterval < -1)
				{
					throw (new Exception("Illegal value."));
				}
			}
			catch (Exception e)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "Error in XML structure (DummyTraffic node -> interval info)."));
			}
			connectionDescriptor.setMinDummyTrafficInterval(m_minDummyTrafficInterval);
			m_state = STATE_OFFER_RECEIVED;
		}
		else
		{
			/* wrong protocol state */
			throw (new ClientForwardException(ClientForwardException.ERROR_PROTOCOL_ERROR,
											  "Wrong protocol state to call this method (current state: " +
											  Integer.toString(m_state) + ")."));
		}
		return connectionDescriptor;
	}

	/**
	 * This method must be called exactly once, after we have received the the connection offer
	 * from the forwarder. If the call of this method doesn't throw an exception, everything is
	 * ready for starting the anonymous connection. This method throws an exception, if there is
	 * something wrong while sending our decision to the forwarder. At the moment this method
	 * must be called within the forwarder dummy traffic interval, because dummy traffic is not
	 * implemented within this protocol -> dummy traffic is available after starting the JAP
	 * AnonProxy on the forwarded connection.
	 *
	 * @param a_mixCascade The mixcascade from the connection offer we want to use.
	 */
	public void selectMixCascade(MixCascade a_mixCascade) throws ClientForwardException
	{
		if (m_state == STATE_OFFER_RECEIVED)
		{
			/* we have the correct state */
			Document doc = null;
			try
			{
				doc = XMLUtil.createDocument();
			}
			catch (Exception e)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "XML DocumentBuilder error (" +
												  e.toString() + ")."));
			}
			Element japRoutingNode = doc.createElement("JAPRouting");
			/* create the child of JAPRouting (Request) */
			Element requestNode = doc.createElement("Request");
			requestNode.setAttribute("subject", "cascade");
			requestNode.setAttribute("msg", "select");
			/* append the selected MixCascades */
			Element mixCascadeNode = doc.createElement("MixCascade");
			mixCascadeNode.setAttribute("id", a_mixCascade.getId());
			requestNode.appendChild(mixCascadeNode);
			japRoutingNode.appendChild(requestNode);
			doc.appendChild(japRoutingNode);
			byte[] protocolPacket = null;
			try
			{
				protocolPacket = xmlToProtocolPacket(doc);
			}
			catch (Exception e)
			{
				throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
												  "XML transforming error (" +
												  e.toString() +
												  ")."));
			}
			sendProtocolMessage(protocolPacket);
			m_state = STATE_CASCADE_SELECTED;
		}
		else
		{
			/* wrong protocol state */
			throw (new ClientForwardException(ClientForwardException.ERROR_PROTOCOL_ERROR,
											  "Wrong protocol state to call this method (current state: " +
											  Integer.toString(m_state) + ")."));
		}
	}

	/**
	 * Creates the connection request message. It is sent directly after connecting the forwarding
	 * server to request the connection offer structure.
	 *
	 * @return The connection request XML structure.
	 */
	private Document generateConnectionRequest() throws ClientForwardException
	{
		Document doc = null;
		try
		{
			doc = XMLUtil.createDocument();
		}
		catch (Exception e)
		{
			throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
											  "XML DocumentBuilder error (" +
											  e.toString() + ")."));
		}
		/* Create the JAPRouting element */
		Element japRoutingNode = doc.createElement("JAPRouting");
		/* Create the children of JAPRouting (Protocol, Request) */
		Element protocolNode = doc.createElement("Protocol");
		protocolNode.setAttribute("version", Integer.toString(PROTOCOL_VERSION));
		japRoutingNode.appendChild(protocolNode);
		Element requestNode = doc.createElement("Request");
		requestNode.setAttribute("subject", "connection");
		requestNode.setAttribute("msg", "request");
		japRoutingNode.appendChild(requestNode);
		doc.appendChild(japRoutingNode);
		return doc;
	}

	/**
	 * Reads a message from the associated network connection. Header and trailer of the protocol
	 * message are removed. If there is a connection or protocol error, an exception is thrown.
	 *
	 * @return The message read from the connection.
	 */
	private byte[] readProtocolMessage() throws ClientForwardException
	{
		byte[] messageHeader = new byte[MESSAGE_START_SIGNATURE.length + 4];
		byte[] currentMessage = null;
		try
		{
			int readBytes = 0;
			while (readBytes < messageHeader.length)
			{
				int lastRead = m_connection.getSocket().getInputStream().read(messageHeader, readBytes,
					messageHeader.length - readBytes);
				if (lastRead == -1)
				{
					throw (new IOException("Read error: connection was closed."));
				}
				readBytes = lastRead + readBytes;
			}
			byte[] messageStart = new byte[MESSAGE_START_SIGNATURE.length];
			System.arraycopy(messageHeader, 0, messageStart, 0, MESSAGE_START_SIGNATURE.length);
			if (checkSignature(messageStart, MESSAGE_START_SIGNATURE) == false)
			{
				/* something is wrong, this is not a message for us -> throw an exception */
				throw (new ClientForwardException(ClientForwardException.ERROR_PROTOCOL_ERROR,
												  "Protocol error (invalid start signature)."));
			}
			/* we have a valid start signature -> get the net length of the message */
			byte[] netLength = new byte[4];
			System.arraycopy(messageHeader, MESSAGE_START_SIGNATURE.length, netLength, 0, 4);
			int incomingMessageLength = 0;
			try
			{
				incomingMessageLength = (new DataInputStream(new ByteArrayInputStream(netLength))).readInt();
			}
			catch (Exception e)
			{
				throw (new IOException("Error while reading message length."));
			}
			if ( (incomingMessageLength < 0)) // || (incomingMessageLength > MAXIMUM_PROTOCOLMESSAGE_SIZE))
			{
				/* something is wrong, this is not a message for us -> throw an exception */
				throw (new ClientForwardException(ClientForwardException.ERROR_PROTOCOL_ERROR,
												  "Protocol error (invalid length)."));
			}
			/* we got the length, read the message + end signature */
			byte[] remainingMessage = new byte[incomingMessageLength + MESSAGE_END_SIGNATURE.length];
			readBytes = 0;
			while (readBytes < remainingMessage.length)
			{
				int lastRead = m_connection.getSocket().getInputStream().read(remainingMessage, readBytes,
					remainingMessage.length - readBytes);
				if (lastRead == -1)
				{
					throw (new IOException("Read error: connection was closed."));
				}
				readBytes = lastRead + readBytes;
			}
			/* check the end signature */
			byte[] messageEnd = new byte[MESSAGE_END_SIGNATURE.length];
			System.arraycopy(remainingMessage, incomingMessageLength, messageEnd, 0,
							 MESSAGE_END_SIGNATURE.length);
			if (checkSignature(messageEnd, MESSAGE_END_SIGNATURE) == false)
			{
				/* something is wrong, this is not a message for us -> throw an exception */
				throw (new ClientForwardException(ClientForwardException.ERROR_PROTOCOL_ERROR,
												  "Protocol error (invalid end signature)."));
			}
			/* extract the message */
			currentMessage = new byte[incomingMessageLength];
			System.arraycopy(remainingMessage, 0, currentMessage, 0, incomingMessageLength);
		}
		catch (IOException e)
		{
			throw (new ClientForwardException(ClientForwardException.ERROR_CONNECTION_ERROR,
											  "Connection error (" +
											  e.toString() + ")."));
		}
		return currentMessage;
	}

	/**
	 * Sends a message to the associated network connection. The message must be in the protocol
	 * message format (with header and trailer).
	 *
	 * @param a_message The message to send.
	 */
	private void sendProtocolMessage(byte[] a_message) throws ClientForwardException
	{
		try
		{
			m_connection.getSocket().getOutputStream().write(a_message);
		}
		catch (IOException e)
		{
			throw (new ClientForwardException(ClientForwardException.ERROR_CONNECTION_ERROR,
											  "Connection error (" +
											  e.toString() + ")."));
		}
	}

	/**
	 * This method checks, whether to byte arrays have identical content or not.
	 *
	 * @param a_signature1 The first byte array.
	 * @param a_signature2 The second byte array.
	 *
	 * @return True, if both byte arrays are not null and have the same content, else the result is
	 *         false.
	 */
	private boolean checkSignature(byte[] a_signature1, byte[] a_signature2)
	{
		boolean identical = false;
		try
		{
			if (a_signature1.length == a_signature2.length)
			{
				int i = 0;
				identical = true;
				while ( (i < a_signature1.length) && (identical == true))
				{
					if (a_signature1[i] != a_signature2[i])
					{
						identical = false;
					}
					i++;
				}
			}
		}
		catch (Exception e)
		{
			/* if one or both signatures are null -> identical is false */
		}
		return identical;
	}


	/**
	 * Creates a protocol packet from an XML structure.
	 *
	 * @param doc The XML structure which shall be transformed in a protocol packet.
	 *
	 * @return The protocol packet with the XML structure inside.
	 */
	private byte[] xmlToProtocolPacket(Document a_doc) throws Exception
	{
		//ByteArrayOutputStream out = new ByteArrayOutputStream();
		//Transformer t = TransformerFactory.newInstance().newTransformer();
		//Result r = new StreamResult(out);
		//Source s = new DOMSource(a_doc);
		//t.transform(s, r);
		return createProtocolPacket(XMLUtil.toByteArray(a_doc));
	}

	/**
	 * Creates a protocol packet from byte array with data. This method adds header and trailer to
	 * the data and returns the whole packet.
	 *
	 * @param a_data The bytes to put in the protocol packet.
	 *
	 * @return The protocol packet with data, header and trailer.
	 */
	private byte[] createProtocolPacket(byte[] a_data)
	{
		byte[] protocolPacket = new byte[MESSAGE_START_SIGNATURE.length + 4 + a_data.length +
			MESSAGE_END_SIGNATURE.length];
		/* add the start signature */
		System.arraycopy(MESSAGE_START_SIGNATURE, 0, protocolPacket, 0, MESSAGE_START_SIGNATURE.length);
		/* add the length of the data */
		ByteArrayOutputStream dataLength = new ByteArrayOutputStream(4);
		try
		{
			(new DataOutputStream(dataLength)).writeInt(a_data.length);
			System.arraycopy(dataLength.toByteArray(), 0, protocolPacket, MESSAGE_START_SIGNATURE.length, 4);
		}
		catch (Exception e)
		{
			/* this exception should never occur -> write invalid message length */
			byte[] dummyLength =
				{
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
			System.arraycopy(dummyLength, 0, protocolPacket, MESSAGE_START_SIGNATURE.length, 4);
		}
		/* add the data */
		System.arraycopy(a_data, 0, protocolPacket, MESSAGE_START_SIGNATURE.length + 4, a_data.length);
		/* add the end signature */
		System.arraycopy(MESSAGE_END_SIGNATURE, 0, protocolPacket,
						 MESSAGE_START_SIGNATURE.length + 4 + a_data.length, MESSAGE_END_SIGNATURE.length);
		return protocolPacket;
	}

}
