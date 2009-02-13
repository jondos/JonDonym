/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client.replay;

import java.util.Observable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.ErrorCodes;
import anon.client.ChannelTable;
import anon.client.Multiplexer;
import anon.client.XmlControlChannel;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.IServiceContainer;


/**
 * @author Stefan Lieske
 */
public class ReplayControlChannel extends XmlControlChannel {

  private MessageDistributor m_messageDistributor;

  private Object m_internalSynchronization;


  private class MessageDistributor extends Observable {

    public void publishTimestamps(Vector a_timestamps) {
      publishObject(a_timestamps);
    }

    public void publishException(Exception a_exception) {
      publishObject(a_exception);
    }


    private void publishObject(Object a_object) {
      synchronized (this) {
        setChanged();
        notifyObservers(a_object);
      }
    }

  }


  public ReplayControlChannel(Multiplexer a_multiplexer, IServiceContainer a_serviceContainer) {
    super(ChannelTable.CONTROL_CHANNEL_ID_REPLAY, a_multiplexer, a_serviceContainer);
    m_messageDistributor = new MessageDistributor();
    m_internalSynchronization = new Object();
  }


  public Observable getMessageDistributor() {
    return m_messageDistributor;
  }


  protected void processXmlMessage(Document a_document) {
    try {
      LogHolder.log(LogLevel.DEBUG, LogType.NET, "Received a message: " + XMLUtil.toString(a_document));
      Element mixesNode = a_document.getDocumentElement();
      if (mixesNode == null) {
        throw (new XMLParseException(XMLParseException.ROOT_TAG, "No document element in received XML structure."));
      }
      if (!mixesNode.getNodeName().equals("Mixes")) {
        throw (new XMLParseException(XMLParseException.ROOT_TAG, "Mixes node expected in received XML structure."));
      }
      Vector timestamps = new Vector();
      NodeList mixNodes = mixesNode.getElementsByTagName("Mix");
      for (int i = 0; i < mixNodes.getLength(); i++) {
        Element currentMixNode = (Element)(mixNodes.item(i));
        /* get the mix parameters */
        String currentMixId = XMLUtil.parseAttribute(currentMixNode, "id", null);
        if (currentMixId == null) {
          throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "XML structure of Mix " + Integer.toString(i) + " does not contain a Mix-ID."));
        }
        /* get the timestamps */
        NodeList currentReplayNodes = currentMixNode.getElementsByTagName("Replay");
        if (currentReplayNodes.getLength() == 0) {
          throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "XML structure of Mix " + Integer.toString(i) + " does not contain a Replay node."));
        }
        /* there should be only one replay node */
        NodeList currentReplayTimeStampNodes = ((Element)(currentReplayNodes.item(0))).getElementsByTagName("ReplayTimestamp");
        if (currentReplayTimeStampNodes.getLength() == 0) {
          throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "XML structure of Mix " + Integer.toString(i) + " does not contain a ReplayTimestamp node."));
        }
        /* there should be only one replaytimestamp node */
        int currentMixOffset = XMLUtil.parseAttribute(currentReplayTimeStampNodes.item(0), "offset", -1);
        if (currentMixOffset == -1) {
          throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "XML structure of Mix " + Integer.toString(i) + " does not contain a valid ReplayTimestamp offset."));
        }
        int currentMixInterval = XMLUtil.parseAttribute(currentReplayTimeStampNodes.item(0), "interval", -1);
        if (currentMixInterval == -1) {
          throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "XML structure of Mix " + Integer.toString(i) + " does not contain a valid ReplayTimestamp interval."));
        }
        timestamps.addElement(new ReplayTimestamp(currentMixId, currentMixInterval, currentMixOffset));
      }
      m_messageDistributor.publishTimestamps(timestamps);
    }
    catch (Exception e) {
		getServiceContainer().keepCurrentService(false); // do not reconnect to this cascade if possible
      LogHolder.log(LogLevel.ERR, LogType.NET, e);
      m_messageDistributor.publishException(e);
    }
  }


  public void requestTimestamps() {
    try {
      Document doc = XMLUtil.createDocument();
      if (doc == null) {
        throw (new Exception("ReplayControlChannel: requestTimestamps(): Cannot create XML document for request."));
      }
      Element getTimestampsNode = doc.createElement("GetTimestamps");
      doc.appendChild(getTimestampsNode);
      int errorCode = 0;
      synchronized (m_internalSynchronization) {
        errorCode = sendXmlMessage(doc);
      }
      if (errorCode != ErrorCodes.E_SUCCESS) {
        throw (new Exception("ReplayControlChannel: requestTimestamps(): Errorcode '" + Integer.toString(errorCode) + "' while sending request."));
      }
    }
    catch (Exception e) {
      LogHolder.log(LogLevel.ERR, LogType.NET, e);
      m_messageDistributor.publishException(e);
    }
  }

}
