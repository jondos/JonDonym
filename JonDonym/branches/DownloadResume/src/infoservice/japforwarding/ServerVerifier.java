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
package infoservice.japforwarding;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.infoservice.Constants;
import logging.LogHolder;
import logging.LogType;
import logging.LogLevel;
import anon.util.XMLUtil;

/**
 * This class is used for verifying a JAP forwarding server (whether it is accessable from
 * the outside world).
 */
final public class ServerVerifier
{

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
	 * This is the maximum net size of a protocol message in bytes. Bigger messages are not accepted
	 * and causes an exception.
	 */
	private static final int MAXIMUM_PROTOCOLMESSAGE_SIZE = 100000;

	/**
	 * Stores the internet address of a JAP forwarding server.
	 */
	private InetAddress m_serverAddress;

	/**
	 * Stores the port of a JAP forwarding server.
	 */
	private int m_serverPort;

	/** Stores the thread number of the ServerVerifiy-Thread...*
	 *
	 */
	private static long ms_lastServerVerifierThreadID=0;
	/**
	 * Creates a new ServerVerifier. It can verify the JAP forwarding server specified by the
	 * address and port parameters.
	 *
	 * @param a_serverAddress The internet address of the JAP forwarding server.
	 * @param a_serverPort The port of the JAP forwarding server.
	 */
	public ServerVerifier(InetAddress a_serverAddress, int a_serverPort)
	{
		m_serverAddress = a_serverAddress;
		m_serverPort = a_serverPort;
	}

	/**
	 * Verifies the connection to the JAP forwarding server.
	 *
	 * @returns True, if we could reach a JAP forwarding server, false if we couldn't reach a JAP
	 *          forwarding server or there was a timeout.
	 */
	public boolean verifyServer()
	{
		final long verifyStartTime = System.currentTimeMillis();
		final VerificationLock verificationLock = new VerificationLock();
		Thread verifyThread = new Thread(new Runnable()
		{
			public void run()
			{
				Socket serverConnection = null;
				try
				{
					serverConnection = new Socket(m_serverAddress, m_serverPort);
					/* a timeout longer than the remaining time makes no sense */
					serverConnection.setSoTimeout(Constants.FORWARDING_SERVER_VERIFY_TIMEOUT * 1000 -
												  (int) (System.currentTimeMillis() - verifyStartTime));
					sendProtocolMessage(xmlToProtocolPacket(generateConnectionVerify()), serverConnection);
					byte[] message = readProtocolMessage(serverConnection);
					Document doc = XMLUtil.toXMLDocument(message);
					/* work through the XML tree and verify the received answer */
					NodeList japRoutingNodes = doc.getElementsByTagName("JAPRouting");
					if (japRoutingNodes.getLength() == 0)
					{
						throw (new Exception("Error in XML structure (JAPRouting node)."));
					}
					Element japRoutingNode = (Element) (japRoutingNodes.item(0));
					NodeList requestNodes = japRoutingNode.getElementsByTagName("Request");
					if (requestNodes.getLength() == 0)
					{
						throw (new Exception("Error in XML structure (Request node)."));
					}
					Element requestNode = (Element) (requestNodes.item(0));
					String subject = requestNode.getAttribute("subject");
					if (!subject.equals("connection"))
					{
						throw (new Exception("Error in XML structure (Request node, wrong subject)."));
					}
					String msg = requestNode.getAttribute("msg");
					if (!msg.equals("acknowledge"))
					{
						throw (new Exception("Error in XML structure (Request node, wrong msg)."));
					}
					/* if we are here, everything is ok -> close the connection, set the success flag and
					 * notify the main thread
					 */
					serverConnection.close();
					synchronized (verificationLock)
					{
						verificationLock.setSuccess(true);
						verificationLock.notify();
					}
				}
				catch (Exception e)
				{
					try
					{
						serverConnection.close();
					}
					catch (Exception e2)
					{
						/* it was already null */
					}
					LogHolder.log(LogLevel.INFO, LogType.NET,
						"Error while verifying the JAP forwarding server " +
								  m_serverAddress.toString() + ":" + Integer.toString(m_serverPort) + ": " +
								  e.toString());
					/* no success -> wake up the main thread */
					synchronized (verificationLock)
					{
						verificationLock.notify();
					}
				}
			}
		});
		verifyThread.setName("infoservice.japforwarding.ServerVerifier - "+ms_lastServerVerifierThreadID++);
		verifyThread.setDaemon(true);
		boolean verificationSuccess = false;
		synchronized (verificationLock)
		{
			/* start the verify thread */
			verifyThread.start();
			try
			{
				verificationLock.wait( ( (long) Constants.FORWARDING_SERVER_VERIFY_TIMEOUT) * (long) 1000);
				verificationSuccess = verificationLock.getSuccess();
				/* try to stop the verification thread */
				// I do not beleive, that this is sufficient, because it sets just set the interrupte state to ture
				// without really suspending the trhead!
				verifyThread.interrupt();
				// do not delete the following line until your really know what you are doing
				verifyThread.stop();
			}
			catch (Exception e)
			{
				/* should not happen */
			}
		}
		/* don't wait for the verify thread, it will come to an end automatically */
		if (verificationSuccess == true)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "ServerVerifier: verifyServer: Verification of the JAP forwarding server " +
						  m_serverAddress.toString() + ":" + Integer.toString(m_serverPort) +
						  " was successful.");
		}
		else
		{
			LogHolder.log(LogLevel.INFO, LogType.NET,
						  "Verification of the JAP forwarding server " +
						  m_serverAddress.toString() + ":" + Integer.toString(m_serverPort) + " failed.");
		}
		return verificationSuccess;
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
	 * Reads a message from the associated network connection. Header and trailer of the protocol
	 * message are removed. If there is a connection or protocol error, an exception is thrown.
	 *
	 * @param a_connection The connection to read from;
	 *
	 * @return The message read from the connection.
	 */
	private byte[] readProtocolMessage(Socket a_connection) throws Exception
	{
		byte[] messageHeader = new byte[MESSAGE_START_SIGNATURE.length + 4];
		byte[] currentMessage = null;
		int readBytes = 0;
		while (readBytes < messageHeader.length)
		{
			int lastRead = a_connection.getInputStream().read(messageHeader, readBytes,
				messageHeader.length - readBytes);
			if (lastRead == -1)
			{
				throw (new Exception(
					"ServerVerifier: readProtocolMessage: Read error - connection was closed."));
			}
			readBytes = lastRead + readBytes;
		}
		byte[] messageStart = new byte[MESSAGE_START_SIGNATURE.length];
		System.arraycopy(messageHeader, 0, messageStart, 0, MESSAGE_START_SIGNATURE.length);
		if (checkSignature(messageStart, MESSAGE_START_SIGNATURE) == false)
		{
			/* something is wrong, this is not a message for us -> throw an exception */
			throw (new Exception(
				"ServerVerifier: readProtocolMessage: Protocol error (invalid start signature)."));
		}
		/* we have a valid start signature -> get the net length of the message */
		byte[] netLength = new byte[4];
		System.arraycopy(messageHeader, MESSAGE_START_SIGNATURE.length, netLength, 0, 4);
		int incomingMessageLength = 0;
		incomingMessageLength = (new DataInputStream(new ByteArrayInputStream(netLength))).readInt();
		if ( (incomingMessageLength < 0) || (incomingMessageLength > MAXIMUM_PROTOCOLMESSAGE_SIZE))
		{
			/* something is wrong, this is not a message for us -> throw an exception */
			throw (new Exception("ServerVerifier: readProtocolMessage: Protocol error (invalid length)."));
		}
		/* we got the length, read the message + end signature */
		byte[] remainingMessage = new byte[incomingMessageLength + MESSAGE_END_SIGNATURE.length];
		readBytes = 0;
		while (readBytes < remainingMessage.length)
		{
			int lastRead = a_connection.getInputStream().read(remainingMessage, readBytes,
				remainingMessage.length - readBytes);
			if (lastRead == -1)
			{
				throw (new Exception(
					"ServerVerifier: readProtocolMessage: Read error - connection was closed."));
			}
			readBytes = lastRead + readBytes;
		}
		/* check the end signature */
		byte[] messageEnd = new byte[MESSAGE_END_SIGNATURE.length];
		System.arraycopy(remainingMessage, incomingMessageLength, messageEnd, 0, MESSAGE_END_SIGNATURE.length);
		if (checkSignature(messageEnd, MESSAGE_END_SIGNATURE) == false)
		{
			/* something is wrong, this is not a message for us -> throw an exception */
			throw (new Exception(
				"ServerVerifier: readProtocolMessage: Protocol error (invalid end signature)."));
		}
		/* extract the message */
		currentMessage = new byte[incomingMessageLength];
		System.arraycopy(remainingMessage, 0, currentMessage, 0, incomingMessageLength);
		return currentMessage;
	}

	/**
	 * Sends a message to the associated network connection. The message must be in the protocol
	 * message format (with header and trailer).
	 *
	 * @param a_message The message to send.
	 * @param a_connection The connection for writing the message.
	 */
	private void sendProtocolMessage(byte[] a_message, Socket a_connection) throws IOException
	{
		a_connection.getOutputStream().write(a_message);
	}

	/**
	 * Creates an verify request message for the verifying the JAP forwarding server. If we get the
	 * correct answer, we know that the JAP forwarding server works and is accessable from the
	 * outside.
	 *
	 * @return The connection verify XML structure.
	 */
	private Document generateConnectionVerify() throws Exception
	{
		Document doc = XMLUtil.createDocument();

		/* Create the JAPRouting element */
		Element japRoutingNode = doc.createElement("JAPRouting");
		/* Create the child of JAPRouting (Request) */
		Element requestNode = doc.createElement("Request");
		requestNode.setAttribute("subject", "connection");
		requestNode.setAttribute("msg", "verify");
		japRoutingNode.appendChild(requestNode);
		doc.appendChild(japRoutingNode);
		return doc;
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
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Transformer t = TransformerFactory.newInstance().newTransformer();
		Result r = new StreamResult(out);
		Source s = new DOMSource(a_doc);
		t.transform(s, r);
		return createProtocolPacket(out.toByteArray());
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
