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
package forward.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.util.XMLUtil;
import forward.ForwardUtils;

/**
 * This is the implementation for the first version of the JAP routing protocol.
 */
public class DefaultProtocolHandler implements IProtocolHandler {

  /**
   * This is the version of the current protocol implementation. Interaction is only possible with
   * clients, which use the same protocol version.
   */
  private static final int PROTOCOL_VERSION = 2;

  /**
   * This is the maximum net size of a protocol message in bytes. Bigger messages are not accepted
   * and causes an exception.
   */
  private static final int MAXIMUM_PROTOCOLMESSAGE_SIZE = 100000;

  /**
   * This is the start signature of every protocol message.
   */
  private static final byte[] MESSAGE_START_SIGNATURE = {(byte)0xFF, (byte)0x00, (byte)0xF0, (byte)0x0F};

  /**
   * This is the end signature of every protocol message.
   */
  private static final byte[] MESSAGE_END_SIGNATURE = {(byte)0xFF, (byte)0x00, (byte)0xE1, (byte)0x1E};

  /**
   * This is the state after establishing the connection. In this state the protocol waits for the
   * request message from the client (or the infoservice validation request).
   */
  private static final int STATE_WAIT_FOR_CLIENT_REQUEST = 0;

  /**
   * This is the state after we have gotten the connection request from a forwarding client (not
   * an infoservice) and sent the connection offer back to the client. In this state the protocol
   * waits for the answer (cascade selection) from the client.
   */
  private static final int STATE_WAIT_FOR_CASCADE_SELECTION = 1;

  /**
   * This is the state after we received the cascade selection from the client and built a
   * connection ton the selected cascade. We are forwarading all received packets between client
   * and cascade in this state.
   */
  private static final int STATE_CONNECTED_TO_MIX = 2;

  /**
   * This is the state after the connection was closed (because of connection termination by
   * client or an error on the connection). In this state we wait for the removing from the
   * forwarding scheduler.
   */
  private static final int STATE_CONNECTION_CLOSED = 3;

  /**
   * This is the state after sending the acknowledgement for the verify message of the
   * infoservice. We can't close the connection immediately because we have to wait until all
   * data is sent to the infoservice. The infoservice will close the connection after receiving
   * the acknowledgement message (no problem if the infoservice doesn't close the connection,
   * because we are not accepting any more messages, we will close the connection after the
   * timeout).
   */
  private static final int STATE_WAIT_FOR_INFOSERVICE_CLOSE = 4;


  /**
   * This stores the connection to the selected mixcascade. If this value is null, there is no
   * connection to a cascade and the protocol handles all incoming packets. If this value is
   * not null, all incoming packets are forwarded on this connection.
   */
  private Socket m_serverConnection;

  /**
   * This buffer stores the parts of a incoming protocol message until we received the full
   * message. If we have finished the protocol phase, all packets are directly forwarded, so we
   * only need this buffer while doing the protocol stuff before the forwarding process.
   */
  private ByteArrayOutputStream m_incomingMessageBuffer;

  /**
   * This stores the net length of a incoming message in the incoming message buffer. We get the
   * value from evaluating the message header.
   */
  private int m_incomingMessageLength;

  /**
   * This is the for outgoing protocol messages to the client. The connection handler will read
   * the data from this buffer and sends them to the client. If we have finished the protocol
   * phase, all packets are read directly from the buffer of the mixcascade connection, so we only
   * need this buffer while doing the protocol stuff before the forwarding process.
   */
  private ByteArrayInputStream m_outgoingMessageBuffer;

  /**
   * This stores the current protocol state. This state decides what to do with incoming messages.
   * Look at the constants in this class.
   */
  private int m_currentState;

  /**
   * This stores the client connection this protocol handler belongs to.
   */
  private ForwardConnection m_parentConnection;


  /**
   * Generates a new DefaultProtocolHandler.
   *
   * @param a_parentConnection This is the client connection for which we are doing the protocol
   *                           stuff.
   */
  public DefaultProtocolHandler(ForwardConnection a_parentConnection) throws Exception {
    m_incomingMessageBuffer = new ByteArrayOutputStream();
    m_incomingMessageLength = -1;
    m_outgoingMessageBuffer = new ByteArrayInputStream(new byte[0]);
    m_parentConnection = a_parentConnection;
    m_serverConnection = null;
    m_currentState = STATE_WAIT_FOR_CLIENT_REQUEST;
  }


  /**
   * Returns the number of bytes which are ready for sending to the client without blocking by the
   * next call of read(). This call throws an exception, if there is something wrong with the
   * connection to the server. If no connection to the server is established, we look in the buffer
   * for protocol messages.
   *
   * @return The number of bytes which can be read.
   */
  public int available() throws Exception {
    int availableData = 0;
    if (m_serverConnection != null) {
      availableData = m_serverConnection.getInputStream().available();
    }
    else {
      /* only look in the protocol message buffer, if there is no server connection */
      synchronized (m_outgoingMessageBuffer) {
        availableData = m_outgoingMessageBuffer.available();
      }
    }
    return availableData;
  }

  /**
   * Read a_buffer.length bytes from the server in the buffer a_buffer. This call blocks until
   * a_buffer.length bytes could be read. This call throws an exception, if there is something
   * wrong with the connection to the server. If no connection to the server is established, we
   * look in the buffer for protocol messages.
   *
   * @param a_buffer A buffer for the read bytes.
   *
   * @return The bytes read into the buffer or -1, if the end of the stream is reached.
   */
  public int read(byte[] a_buffer) throws Exception {
    int bytesRead = 0;
    if (m_serverConnection != null) {
      bytesRead = m_serverConnection.getInputStream().read(a_buffer);
    }
    else {
      /* only look in the protocol message buffer, if there is no server connection */
      synchronized (m_outgoingMessageBuffer) {
        bytesRead = m_outgoingMessageBuffer.read(a_buffer);
      }
      if (bytesRead == -1) {
        /* end of the protocol stream is no problem */
        bytesRead = 0;
      }
    }
    return bytesRead;
  }

  /**
   * Writes the bytes in a_buffer to the server or the protocol handler. This call blocks until
   * the bytes could be written in the send queue. This call throws an exception, if there is
   * something wrong with the connection to the server.
   *
   * @param a_buffer A buffer with the bytes to write.
   */
  public void write(byte[] a_buffer) throws Exception {
    if (m_serverConnection != null) {
      /* we are connected to a server -> send all the bytes to the server */
      m_serverConnection.getOutputStream().write(a_buffer);
      m_serverConnection.getOutputStream().flush();
    }
    else {
      /* we are not connected with a server, this must be protocol data */
      messageHandler(a_buffer);
    }
  }

  /**
   * Closes the connection to the server and stops handling of protocol messages. All later calls
   * of available(), read(), write() will throw an exception.
   */
  public void close() {
    if (m_serverConnection != null) {
      try {
        m_serverConnection.close();
      }
      catch (Exception e) {
      }
      m_serverConnection = null;
    }
    m_incomingMessageBuffer = null;
    m_outgoingMessageBuffer = null;
    m_currentState = STATE_CONNECTION_CLOSED;
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
  private boolean checkSignature(byte[] a_signature1, byte[] a_signature2) {
    boolean identical = false;
    try {
      if (a_signature1.length == a_signature2.length) {
        int i = 0;
        identical = true;
        while ((i < a_signature1.length) && (identical == true)) {
          if (a_signature1[i] != a_signature2[i]) {
            identical = false;
          }
          i++;
        }
      }
    }
    catch (Exception e) {
      /* if one or both signatures are null -> identical is false */
    }
    return identical;
  }

  /**
   * This method handles all data packets received from the client. We evaluate the message
   * header, put the data in the incoming message buffer and call messageReceived() method, if
   * we received the full message. If we find a protocol error (wrong header/trailer, message
   * to long, wrong message content, ...) this method throws an exception.
   *
   * @param a_newData The data packet received from the client.
   */
  private void messageHandler(byte[] a_newData) throws Exception {
    if (m_incomingMessageBuffer.size() < MESSAGE_START_SIGNATURE.length + 4) {
      if (m_incomingMessageBuffer.size() < MESSAGE_START_SIGNATURE.length) {
        /* we must check for the message start signature */
        m_incomingMessageBuffer.write(a_newData);
        if (m_incomingMessageBuffer.size() >= MESSAGE_START_SIGNATURE.length) {
          /* now there are enough bytes to check for the start signature */
          byte[] messageStart = new byte[MESSAGE_START_SIGNATURE.length];
          System.arraycopy(m_incomingMessageBuffer.toByteArray(), 0, messageStart, 0, MESSAGE_START_SIGNATURE.length);
          if (checkSignature(messageStart, MESSAGE_START_SIGNATURE) == false) {
            /* something is wrong, this is not a message for us -> throw an exception */
            throw(new Exception("DefaultProtocolHandler: messageHandler: Protocol error (invalid start signature)."));
          }
        }
      }
      else {
        /* we have already a valid start signature in the m_incomingMessageBuffer */
        m_incomingMessageBuffer.write(a_newData);
      }
      if (m_incomingMessageBuffer.size() >= MESSAGE_START_SIGNATURE.length + 4) {
        /* read the net length of the message */
        byte[] netLength = new byte[4];
        System.arraycopy(m_incomingMessageBuffer.toByteArray(), MESSAGE_START_SIGNATURE.length, netLength, 0, 4);
        try {
          m_incomingMessageLength = (new DataInputStream(new ByteArrayInputStream(netLength))).readInt();
        }
        catch (Exception e) {
          throw (new Exception("DefaultProtocolHandler: messageHandler: Error while reading message length."));
        }
        if ((m_incomingMessageLength < 0) || (m_incomingMessageLength > MAXIMUM_PROTOCOLMESSAGE_SIZE)) {
          m_incomingMessageLength = -1;
          /* something is wrong, this is not a message for us -> throw an exception */
          throw(new Exception("DefaultProtocolHandler: messageHandler: Protocol error (invalid length)."));
        }
      }
    }
    else {
      /* we have already a valid start signature and the message length */
      m_incomingMessageBuffer.write(a_newData);
    }
    if (m_incomingMessageLength != -1) {
     /* check, whether we received the whole packet */
      if (m_incomingMessageBuffer.size() >= MESSAGE_START_SIGNATURE.length + 4 + m_incomingMessageLength + MESSAGE_END_SIGNATURE.length) {
        /* check the end signatue */
        byte[] messageEnd = new byte[MESSAGE_END_SIGNATURE.length];
        System.arraycopy(m_incomingMessageBuffer.toByteArray(), MESSAGE_START_SIGNATURE.length + 4 + m_incomingMessageLength, messageEnd, 0, MESSAGE_END_SIGNATURE.length);
        if (checkSignature(messageEnd, MESSAGE_END_SIGNATURE) == false) {
          /* something is wrong, this is not a message for us -> throw an exception */
          throw(new Exception("DefaultProtocolHandler: messageHandler: Protocol error (invalid end signature)."));
        }
        /* extract the message */
        byte[] currentMessage = new byte[m_incomingMessageLength];
        System.arraycopy(m_incomingMessageBuffer.toByteArray(), MESSAGE_START_SIGNATURE.length + 4, currentMessage, 0, m_incomingMessageLength);
        /* clear the m_incomingMessageBuffer and push all other bytes back to it (maybe the
         * protocol handler needs the information there)
         */
        byte[] otherMessages = new byte[m_incomingMessageBuffer.size() - MESSAGE_START_SIGNATURE.length - 4 - m_incomingMessageLength - MESSAGE_END_SIGNATURE.length];
        System.arraycopy(m_incomingMessageBuffer.toByteArray(), MESSAGE_START_SIGNATURE.length + 4 + m_incomingMessageLength + MESSAGE_END_SIGNATURE.length, otherMessages, 0, otherMessages.length);
        m_incomingMessageBuffer.reset();
        m_incomingMessageLength = -1;
        m_incomingMessageBuffer.write(otherMessages);
        /* handle the message */
        messageReceived(currentMessage);
        if (m_incomingMessageBuffer.size() > 0) {
          /* if there are still data in the m_incomingMessageBuffer after the call of
           * messageReceived(), this must be protocol messages too, because messageReceived() have to
           * clear this buffer, if the end of the protocol phase is reached and the transfer phase
           * starts -> try to handle those messages (stored in otherMessages)
           */
          m_incomingMessageBuffer.reset();
          messageHandler(otherMessages);
        }
      }
    }
  }

  /**
   * This method handles and evaluates a whole message. The handleProtocol() method is called
   * after parsing the message. This method throws an exception, if there is an error while
   * parsing the message.
   *
   * @param a_newMessage A whole message received from the client.
   */
  private void messageReceived(byte[] a_newMessage) throws Exception {
    /* all messages are XML documents */
    Document doc = XMLUtil.toXMLDocument(a_newMessage);
    /* the root node is always JAPRouting */
    NodeList japRoutingNodes = doc.getElementsByTagName("JAPRouting");
    if (japRoutingNodes.getLength() == 0) {
      throw (new Exception("DefaultProtocolHandler: messageReceived: Error in XML structure (JAPRouting node)."));
    }
    Element japRoutingNode = (Element)(japRoutingNodes.item(0));
    handleProtocol(japRoutingNode);
  }

  /**
   * This method handles the incoming XML messages. Dependent of the current prototcol state, it
   * calls the method needed for handling the expected message. This method throws an exception,
   * if there is something wrong with the received message or if there is an error while calling
   * the method for handling the associated event.
   *
   * @param a_japRoutingNode The JAPRouting node, which is the root node of every protocol
   *                         message.
   */
  private void handleProtocol(Element a_japRoutingNode) throws Exception {
    switch (m_currentState) {
      case STATE_WAIT_FOR_CLIENT_REQUEST: {
        /* we wait for an initial request message */
        handleInitialRequestMessage(a_japRoutingNode);
        break;
      }
      case STATE_WAIT_FOR_CASCADE_SELECTION: {
        /* we wait for a cascade select message */
        handleClientCascadeSelectMessage(a_japRoutingNode);
        break;
      }
      default: {
        /* something is going wrong, we got an unexpected message */
        throw (new Exception("DefaultProtocolHandler: handleProtocol: Protocol error."));
      }
    }
  }

  /**
   * This method handles the initial request messages from a client or an infoservice. The request
   * is parsed and if the request was from a client, we send the connection offer back. I the
   * request was the verify message from the infoservice, we send an acknowledgement. This method
   * throws an exception, if there is something wrong with the received message or if there is an
   * error while creating the answer.
   *
   * @param a_japRoutingNode The JAPRouting node of the connection request message.
   */
  private void handleInitialRequestMessage(Element a_japRoutingNode) throws Exception {
    /* we got an initial request message -> work through the XML tree and decide whether it was
     * from a forwarding client or an infoservice
     */
    NodeList requestNodes = a_japRoutingNode.getElementsByTagName("Request");
    if (requestNodes.getLength() == 0) {
      throw (new Exception("DefaultProtocolHandler: handleInitialRequestMessage: Error in XML structure (Request node)."));
    }
    Element requestNode = (Element)(requestNodes.item(0));
    String subject = requestNode.getAttribute("subject");
    if (!subject.equals("connection")) {
      throw (new Exception("DefaultProtocolHandler: handleInitialRequestMessage: Error in XML structure (Request node, wrong subject)."));
    }
    String msg = requestNode.getAttribute("msg");
    if (msg.equals("request")) {
      /* we ignore the client protocol version at the moment -> maybe it is useful in the future
       * for backwards compatibility
       */
      /* change the protocol state and send back the connection offer to the client */
      m_currentState = STATE_WAIT_FOR_CASCADE_SELECTION;
      sendProtocolDataToClient(xmlToProtocolPacket(generateConnectionOfferXml()));
    }
    else {
      if (msg.equals("verify")) {
        /* it's the verify message from the infoservice -> send an acknowledgement */
        m_currentState = STATE_WAIT_FOR_INFOSERVICE_CLOSE;
        sendProtocolDataToClient(xmlToProtocolPacket(generateConnectionAcknowledgement()));
      }
      else {
        /* invalid request message */
        throw (new Exception("DefaultProtocolHandler: handleInitialRequestMessage: Error in XML structure (Request node, wrong msg)."));
      }
    }
  }

  /**
   * This method handles the cascade select message from a forwarding client. If everything is ok,
   * it connects to the selected cascade, so forwarding can start. This method throws an exception,
   * if there is something wrong with the received message or if there is an error while
   * connecting to the specified mixcascade.
   *
   * @param a_japRoutingNode The JAPRouting node of the cascade select message.
   */
  private void handleClientCascadeSelectMessage(Element a_japRoutingNode) throws Exception {
    /* we got the cascade select message from a forwarding client -> work through the XML tree */
    NodeList requestNodes = a_japRoutingNode.getElementsByTagName("Request");
    if (requestNodes.getLength() == 0) {
      throw (new Exception("DefaultProtocolHandler: handleClientCascadeSelectMessage: Error in XML structure (Request node)."));
    }
    Element requestNode = (Element)(requestNodes.item(0));
    String subject = requestNode.getAttribute("subject");
    if (!subject.equals("cascade")) {
      throw (new Exception("DefaultProtocolHandler: handleClientCascadeSelectMessage: Error in XML structure (Request node, wrong subject)."));
    }
    String msg = requestNode.getAttribute("msg");
    if (!msg.equals("select")) {
      throw (new Exception("DefaultProtocolHandler: handleClientCascadeSelectMessage: Error in XML structure (Request node, wrong msg)."));
    }
    NodeList mixCascadeNodes = requestNode.getElementsByTagName("MixCascade");
    if (mixCascadeNodes.getLength() == 0) {
      throw (new Exception("DefaultProtocolHandler: handleClientCascadeSelectMessage: Error in XML structure (MixCascade node)."));
    }
    Element mixCascadeNode = (Element) (mixCascadeNodes.item(0));
    String selectedCascadeId = mixCascadeNode.getAttribute("id");
    /* check, whether this is an allowed cascade */
    MixCascade selectedCascade = ForwardServerManager.getInstance().getAllowedCascadesDatabase().getMixCascadeById(selectedCascadeId);
    if (selectedCascade == null) {
      throw (new Exception("DefaultProtocolHandler: handleClientCascadeSelectMessage: Selected cascade not available."));
    }
    /* connect to the cascade */
    if (connectTo(selectedCascade) == true) {
      /* maybe there are some bytes already in the buffer */
      emptyBuffers();
      m_currentState = STATE_CONNECTED_TO_MIX;
    }
    else {
      /* shut everything down */
      close();
      throw (new Exception("DefaultProtocolHandler: handleClientCascadeSelectMessage: Error connecting the selected cascade."));
    }
    /* that's it */
  }

  /**
   * This method tries to get a connection to a mixcascade (by probing all ListenerInterfaces).
   * The connection is made by calling the createConnection() method of ForwardUtils,
   * which knows the current proxy settings.
   *
   * @param a_selectedCascade The mixcascade to build a connection to.
   *
   * @return True, if we got the connection (stored in m_serverConnection). False, if we couldn't
   *         make the connection.
   */
  private boolean connectTo(MixCascade a_selectedCascade) {
    int i = 0;
    while ((i < a_selectedCascade.getNumberOfListenerInterfaces()) && (m_serverConnection == null)) {
      ListenerInterface currentListenerInterface = a_selectedCascade.getListenerInterface(i);
      try {
        m_serverConnection = ForwardUtils.getInstance().createConnection(currentListenerInterface.getHost(), currentListenerInterface.getPort());
        m_serverConnection.setSoTimeout(0);
      }
      catch (Exception e) {
        m_serverConnection = null;
      }
      i++;
    }
    return (m_serverConnection != null);
  }

  /**
   * This method sends all data in our incoming message buffer to the server. This method is
   * called after we made a connection to a mixcascade to send the already received data to the
   * first mix.
   */
  private void emptyBuffers() throws Exception {
    m_serverConnection.getOutputStream().write(m_incomingMessageBuffer.toByteArray());
  }


  /**
   * Creates the connection offer XML structure. This structure is sent directly after the
   * connection request from the client. The client will find information about the used routing
   * protocol verison, all available MixCascades, our quality of service and whether we need
   * dummy traffic for holding connections.
   *
   * @return The connection offer XML structure.
   */
  private Document generateConnectionOfferXml() throws Exception {
    Document doc = XMLUtil.createDocument();
    /* Create the JAPRouting element */
    Element japRoutingNode = doc.createElement("JAPRouting");
    /* Create the children of JAPRouting (Protocol, Request) */
    Element protocolNode = doc.createElement("Protocol");
    protocolNode.setAttribute("version", Integer.toString(PROTOCOL_VERSION));
    japRoutingNode.appendChild(protocolNode);
    Element requestNode = doc.createElement("Request");
    requestNode.setAttribute("subject", "connection");
    requestNode.setAttribute("msg", "offer");
    /* append the allowed MixCascades */
    requestNode.appendChild(ForwardServerManager.getInstance().getAllowedCascadesDatabase().toXmlNode(doc));
    /* append the quality of service info */
    Element qualityOfServiceNode = doc.createElement("QualityOfService");
    Element maximumBandwidthNode = doc.createElement("MaximumBandwidth");
    maximumBandwidthNode.appendChild(doc.createTextNode(Integer.toString(m_parentConnection.getParentScheduler().getMaximumBandwidth())));
    qualityOfServiceNode.appendChild(maximumBandwidthNode);
    Element guaranteedBandwidthNode = doc.createElement("GuaranteedBandwidth");
    guaranteedBandwidthNode.appendChild(doc.createTextNode(Integer.toString(m_parentConnection.getParentScheduler().getGuaranteedBandwidth())));
    qualityOfServiceNode.appendChild(guaranteedBandwidthNode);
    requestNode.appendChild(qualityOfServiceNode);
    /* append the info, whether we need dummy traffic */
    Element dummyTrafficNode = doc.createElement("DummyTraffic");
    dummyTrafficNode.setAttribute("interval", Integer.toString(ForwardServerManager.getInstance().getDummyTrafficInterval()));
    requestNode.appendChild(dummyTrafficNode);
    japRoutingNode.appendChild(requestNode);
    doc.appendChild(japRoutingNode);
    return doc;
  }

  /**
   * Creates an acknowledge message for the verify request of the infoservice. So the infoservice
   * knows, that verifying the forwarder was successful. The infoservice will close the connection
   * immediately after receiving this acknowledgement (no problem if the infoservice doesn't close
   * the connection, because we are not accepting any more messages, we will close the connection
   * after the timeout).
   *
   * @return The verify acknowledge XML structure.
   */
  private Document generateConnectionAcknowledgement() throws Exception {
    Document doc = XMLUtil.createDocument();
    /* Create the JAPRouting element */
    Element japRoutingNode = doc.createElement("JAPRouting");
    /* Create the child of JAPRouting (Request) */
    Element requestNode = doc.createElement("Request");
    requestNode.setAttribute("subject", "connection");
    requestNode.setAttribute("msg", "acknowledge");
    japRoutingNode.appendChild(requestNode);
    doc.appendChild(japRoutingNode);
    return doc;
  }

  /**
   * Put the specified data in the buffer for outgoing protocol messages to the client.
   *
   * @param a_data The data to put into the buffer.
   */
  private void sendProtocolDataToClient(byte[] a_data) {
    synchronized (m_outgoingMessageBuffer) {
      /* we need exclusive access */
      byte[] tempBuffer = new byte[m_outgoingMessageBuffer.available() + a_data.length];
      /* save previous data */
      m_outgoingMessageBuffer.read(tempBuffer, 0, m_outgoingMessageBuffer.available());
      /* append the new data */
      System.arraycopy(a_data, 0, tempBuffer, tempBuffer.length - a_data.length, a_data.length);
      m_outgoingMessageBuffer = new ByteArrayInputStream(tempBuffer);
    }
  }

  /**
   * Creates a protocol packet from an XML structure.
   *
   * @param doc The XML structure which shall be transformed in a protocol packet.
   *
   * @return The protocol packet with the XML structure inside.
   */
  private byte[] xmlToProtocolPacket(Document a_doc) throws Exception {
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
  private byte[] createProtocolPacket(byte[] a_data) {
    byte[] protocolPacket = new byte[MESSAGE_START_SIGNATURE.length + 4 + a_data.length + MESSAGE_END_SIGNATURE.length];
    /* add the start signature */
    System.arraycopy(MESSAGE_START_SIGNATURE, 0, protocolPacket, 0, MESSAGE_START_SIGNATURE.length);
    /* add the length of the data */
    ByteArrayOutputStream dataLength = new ByteArrayOutputStream(4);
    try {
      (new DataOutputStream(dataLength)).writeInt(a_data.length);
      System.arraycopy(dataLength.toByteArray(), 0, protocolPacket, MESSAGE_START_SIGNATURE.length, 4);
    }
    catch (Exception e) {
      /* this exception should never occur -> write invalid message length */
      byte[] dummyLength = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
      System.arraycopy(dummyLength, 0, protocolPacket, MESSAGE_START_SIGNATURE.length, 4);
    }
    /* add the data */
    System.arraycopy(a_data, 0, protocolPacket, MESSAGE_START_SIGNATURE.length + 4, a_data.length);
    /* add the end signature */
    System.arraycopy(MESSAGE_END_SIGNATURE, 0, protocolPacket, MESSAGE_START_SIGNATURE.length + 4 + a_data.length, MESSAGE_END_SIGNATURE.length);
    return protocolPacket;
  }

}
