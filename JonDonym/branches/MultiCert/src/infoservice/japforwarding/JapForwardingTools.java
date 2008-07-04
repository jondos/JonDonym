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

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.util.XMLUtil;
import infoservice.Configuration;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class provides some helper methods needed for the JAP forwarding system.
 */
public class JapForwardingTools
{

	/**
	 * This is the error code we send back to the forwarder, if we couldn't reach him.
	 */
	private static final int FORWARDER_VERIFY_ERROR_CODE = 1;

	/**
	 * This is the error description, we send back to the forwarder, if we couldn't reach him.
	 */
	private static final String FORWARDER_VERIFY_ERROR_MESSAGE =
		"Could not verify the specified forwarder, maybe because of a connection timeout.";

	/**
	 * This is the error code we send back to the forwarder, if we don't know the forwarder ID the
	 * forwarder has sent to us for renewing the entry.
	 */
	private static final int FORWARDER_RENEW_ERROR_CODE = 11;

	/**
	 * This is the error description, we send back to the forwarder, if we don't know the forwarder
	 * ID the forwarder has sent to us for renewing the entry.
	 */
	private static final String FORWARDER_RENEW_ERROR_MESSAGE =
		"No forwarder with the specified ID in the database, maybe the entry was removed because of inactivity.";

	/**
	 * This is the error code we send back to the blockee, if we could not find a forwarder entry.
	 */
	private static final int FORWARDER_FETCH_ERROR_CODE = 21;

	/**
	 * This is the error description, we send back to the blockee, if we could not find a
	 * forwarder entry.
	 */
	private static final String FORWARDER_FETCH_ERROR_MESSAGE = "There are no forwarders available.";

	/**
	 * Returns a snapshot of all infoservices with a primary forwarder list.
	 *
	 * @return all infoservices we know with a primary forwarder list.
	 */
	private static Vector getForwarderListInfoServices()
	{
		Enumeration enumer =
			Database.getInstance(InfoServiceDBEntry.class).getEntrySnapshotAsEnumeration();
		Vector resultList = new Vector();
		while (enumer.hasMoreElements())
		{
			InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (enumer.nextElement());
			if (currentInfoService.hasPrimaryForwarderList())
			{
				resultList.addElement(currentInfoService);
			}
		}
		return resultList;
	}

	/**
	 * Decodes the received data, check the connection to the forwarder and adds him to the
	 * JAP forwarder list.
	 *
	 * @param a_receivedData The data the JAP client has sent to us.
	 * @param a_sourceAddress The internet address where the request was coming from. We use this
	 *                        for checking the connection to the forwarder.
	 *
	 * @return XML encoded data with the answer to the addforwarder request, or null if the received
	 *         data was malformed or if this infoservice doesn't have a primary forwarder list.
	 */
	public static String addForwarder(byte[] a_receivedData, InetAddress a_sourceAddress)
	{
		String answer = null;
		if (Configuration.getInstance().holdForwarderList() == true)
		{
			/* we have a primary forwarding list -> decode xml data */
			try
			{
				Document doc = XMLUtil.toXMLDocument(a_receivedData);
				/* walk through the japforwarder XML tree */
				NodeList japForwarderNodes = doc.getElementsByTagName("JapForwarder");
				if (japForwarderNodes.getLength() == 0)
				{
					throw new Exception("Error in XML structure (JapForwarder node).");
				}
				Element japForwarderNode = (Element) (japForwarderNodes.item(0));
				NodeList plainInformationNodes = japForwarderNode.getElementsByTagName("PlainInformation");
				if (plainInformationNodes.getLength() == 0)
				{
					throw new Exception("Error in XML structure (PlainInformation node).");
				}
				Element plainInformationNode = (Element) (plainInformationNodes.item(0));
				NodeList portNodes = plainInformationNode.getElementsByTagName("Port");
				if (portNodes.getLength() == 0)
				{
					throw new Exception("Error in XML structure (Port node).");
				}
				Element portNode = (Element) (portNodes.item(0));
				int portNumber = Integer.parseInt(portNode.getFirstChild().getNodeValue());

				/* prepare the answer document */
				Document answerDoc = XMLUtil.createDocument();
				/* create the JapForwarder element */
				Element answerJapForwarderNode = answerDoc.createElement("JapForwarder");

				/* now check, whether we can reach the forwarder */
				ServerVerifier verifyInstance = new ServerVerifier(a_sourceAddress, portNumber);
				if (verifyInstance.verifyServer())
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC,
						"JapForwardingTools: addForwarder: Added a new JAP forwarder to the forwarder database.");
					/* we could reach the forwarder -> add the forwarder to the database, there should be
					 * no collision with the 128 bit random id
					 */
					ForwarderDBEntry newForwarder = new ForwarderDBEntry(a_sourceAddress, portNumber);
					Database.getInstance(ForwarderDBEntry.class).update(newForwarder);
					/* now create the answer with the id, so the forwarder can periodically renew the
					 * entry
					 */
					Element answerPlainInformationNode = answerDoc.createElement("PlainInformation");
					Element answerForwarderNode = answerDoc.createElement("Forwarder");
					answerForwarderNode.setAttribute("id", newForwarder.getId());
					answerPlainInformationNode.appendChild(answerForwarderNode);
					answerJapForwarderNode.appendChild(answerPlainInformationNode);
				}
				else
				{
					/* we could not reach the forwarder -> create an answer with the error information */
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						"JapForwardingTools: addForwarder: Could not reach the forwarder. Entry not added.");
					Element answerErrorInformationNode = answerDoc.createElement("ErrorInformation");
					Element answerErrorNode = answerDoc.createElement("Error");
					answerErrorNode.setAttribute("code", Integer.toString(FORWARDER_VERIFY_ERROR_CODE));
					answerErrorNode.appendChild(answerDoc.createTextNode(FORWARDER_VERIFY_ERROR_MESSAGE));
					answerErrorInformationNode.appendChild(answerErrorNode);
					answerJapForwarderNode.appendChild(answerErrorInformationNode);
				}
				answerDoc.appendChild(answerJapForwarderNode);
				answer = XMLUtil.toString(answerDoc);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "JapForwardingTools: addForwarder: Error while adding a forwarder: " +
							  e.toString());
				/* something was wrong -> method returns null */
				answer = null;
			}
		}
		return answer;
	}

	/**
	 * Renews a forwarder in the JAP forwarder database.
	 *
	 * @param a_receivedData The data the JAP client has sent to us.
	 *
	 * @return XML encoded data with the answer to the renewforwarder request, or null if the
	 *         received data was malformed or if this infoservice doesn't have a primary forwarder
	 *         list.
	 */
	public static String renewForwarder(byte[] a_receivedData)
	{
		String answer = null;
		if (Configuration.getInstance().holdForwarderList() == true)
		{
			/* we have a primary forwarding list -> decode xml data */
			try
			{
				Document doc = XMLUtil.toXMLDocument(a_receivedData);
				/* walk through the japforwarder XML tree */
				NodeList japForwarderNodes = doc.getElementsByTagName("JapForwarder");
				if (japForwarderNodes.getLength() == 0)
				{
					throw (new Exception(
						"JapForwardingTools: renewForwarder: Error in XML structure (JapForwarder node)."));
				}
				Element japForwarderNode = (Element) (japForwarderNodes.item(0));
				NodeList plainInformationNodes = japForwarderNode.getElementsByTagName("PlainInformation");
				if (plainInformationNodes.getLength() == 0)
				{
					throw (new Exception(
						"JapForwardingTools: renewForwarder: Error in XML structure (PlainInformation node)."));
				}
				Element plainInformationNode = (Element) (plainInformationNodes.item(0));
				NodeList forwarderNodes = plainInformationNode.getElementsByTagName("Forwarder");
				if (forwarderNodes.getLength() == 0)
				{
					throw (new Exception(
						"JapForwardingTools: renewForwarder: Error in XML structure (Forwarder node)."));
				}
				Element forwarderNode = (Element) (forwarderNodes.item(0));
				String forwarderId = forwarderNode.getAttribute("id");

				/* prepare the answer document */
				Document answerDoc = XMLUtil.createDocument();
				/* create the JapForwarder element */
				Element answerJapForwarderNode = answerDoc.createElement("JapForwarder");

				/* we have the id, get the entry from the database */
				ForwarderDBEntry forwarderEntry =
					(ForwarderDBEntry) Database.getInstance(ForwarderDBEntry.class).getEntryById(forwarderId);
				if (forwarderEntry != null)
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC,
						"JapForwardingTools: renewForwarder: Renewed a JAP forwarder in the forwarder database.");
					/* we have found an entry with that id -> update it */
					Database.getInstance(ForwarderDBEntry.class).update(forwarderEntry.getUpdateClone());
					/* send an empty JapForwarder node as answer */
				}
				else
				{
					/* send an answer with the error description */
					LogHolder.log(LogLevel.INFO, LogType.MISC,
								  "JapForwardingTools: renewForwarder: Renew faild because of an unknown ID.");
					Element answerErrorInformationNode = answerDoc.createElement("ErrorInformation");
					Element answerErrorNode = answerDoc.createElement("Error");
					answerErrorNode.setAttribute("code", Integer.toString(FORWARDER_RENEW_ERROR_CODE));
					answerErrorNode.appendChild(answerDoc.createTextNode(FORWARDER_RENEW_ERROR_MESSAGE));
					answerErrorInformationNode.appendChild(answerErrorNode);
					answerJapForwarderNode.appendChild(answerErrorInformationNode);
				}
				answerDoc.appendChild(answerJapForwarderNode);
				answer = XMLUtil.toString(answerDoc);
			}
			catch (Exception e)
			{
				/* something was wrong -> method returns null */
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "JapForwardingTools: renewForwarder: Error while renew: " + e.toString());
			}
		}
		return answer;
	}

	/**
	 * Gets a forwarder entry (encoded with a captcha) from the JAP forwarder database. If we have
	 * such a database, but there is no data in it, we send back an answer with the error
	 * description. If this infoservice doesn't have a primary forwarder list, we aks all known
	 * infoservices with such a list for an entry, until we get an entry from one of them or we
	 * asked all and no one has an JAP forwarder entry. In this case we return also an answer with
	 * the error description.
	 *
	 * @return XML encoded data with the answer to the getforwarder request. This value can be null,
	 *         but under normal circumstances, this can't happen.
	 */
	public static String getForwarder()
	{
		String answer = null;
		try
		{
			/* prepare the answer document */
			Document answerDoc = XMLUtil.createDocument();
			/* create the JapForwarder element */
			Node answerJapForwarderNode = answerDoc.createElement("JapForwarder");
			if (Configuration.getInstance().holdForwarderList() == true)
			{
				/* we have a primary forwarding list -> try to get a random entry */
				ForwarderDBEntry forwarderEntry =
					(ForwarderDBEntry) Database.getInstance(ForwarderDBEntry.class).getRandomEntry();
				if (forwarderEntry != null)
				{
					/* we have a real entry -> overwrite the answerJapForwarderNode */
					answerJapForwarderNode = XMLUtil.importNode(answerDoc, forwarderEntry.createCaptchaNode(), true);
					LogHolder.log(LogLevel.INFO, LogType.MISC,
						"JapForwardingTools: getForwarder: Returned one JAP forwarder entry from the own database.");
				}
				else
				{
					/* no entries in the database -> send answer with error information */
					LogHolder.log(LogLevel.INFO, LogType.MISC,
						"JapForwardingTools: getForwarder: Could not return a forwarder because database is empty.");
					Element answerErrorInformationNode = answerDoc.createElement("ErrorInformation");
					Element answerErrorNode = answerDoc.createElement("Error");
					answerErrorNode.setAttribute("code", Integer.toString(FORWARDER_FETCH_ERROR_CODE));
					answerErrorNode.appendChild(answerDoc.createTextNode(FORWARDER_FETCH_ERROR_MESSAGE));
					answerErrorInformationNode.appendChild(answerErrorNode);
					answerJapForwarderNode.appendChild(answerErrorInformationNode);
				}
			}
			else
			{
				/* we don't have a primary forwarding list -> ask infoservices with such a list for a
				 * forwarder entry
				 */
				Enumeration enumer = getForwarderListInfoServices().elements();
				boolean gotForwarder = false;
				while ( (enumer.hasMoreElements()) && (gotForwarder == false))
				{
					InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (enumer.nextElement());
					try
					{
						Element japForwarderNode = currentInfoService.getForwarder();
						/* there occured no exception -> we have a real entry -> overwrite the
						 * answerJapForwarderNode
						 */
						answerJapForwarderNode = XMLUtil.importNode(answerDoc, japForwarderNode, true);
						gotForwarder = true;
						LogHolder.log(LogLevel.INFO, LogType.MISC,
							"JapForwardingTools: getForwarder: Returned one JAP forwarder fetched from a remote infoservice.");
					}
					catch (Exception e)
					{
						/* current infoservice was not successful -> try the next one */
					}
				}
				if (gotForwarder == false)
				{
					/* we could not find any forwarder -> create a answer message with the error
					 * information
					 */
					LogHolder.log(LogLevel.INFO, LogType.MISC, "JapForwardingTools: getForwarder: Could not return a forwarder because no known primary infoservice knows one.");
					Element answerErrorInformationNode = answerDoc.createElement("ErrorInformation");
					Element answerErrorNode = answerDoc.createElement("Error");
					answerErrorNode.setAttribute("code", Integer.toString(FORWARDER_FETCH_ERROR_CODE));
					answerErrorNode.appendChild(answerDoc.createTextNode(FORWARDER_FETCH_ERROR_MESSAGE));
					answerErrorInformationNode.appendChild(answerErrorNode);
					answerJapForwarderNode.appendChild(answerErrorInformationNode);
				}
			}
			answerDoc.appendChild(answerJapForwarderNode);
			answer = XMLUtil.toString(answerDoc);
		}
		catch (Exception e)
		{
			/* should never happen -> method returns null */
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "JapForwardingTools: getForwarder: Unexpected exception while getting entry: " +
						  e.toString());
		}
		return answer;
	}

}
