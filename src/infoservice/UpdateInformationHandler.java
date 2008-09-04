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
package infoservice;

import java.io.File;
import java.io.FileReader;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.infoservice.Constants;
import anon.infoservice.Database;
import anon.infoservice.JAPVersionInfo;
import anon.infoservice.JAPMinVersion;
import anon.infoservice.MessageDBEntry;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.JavaVersionDBEntry;
import anon.util.*;
import anon.infoservice.IDistributable;

/**
 * Manages the propaganda of the JAP update information.
 */
public class UpdateInformationHandler implements Runnable
{

	/**
	 * Stores the instance of UpdateInformationHandler (Singleton).
	 */
	private static UpdateInformationHandler ms_uihInstance;

	/**
	 * Returns the instance of UpdateInformationHandler (Singleton). If there is no instance,
	 * there is a new one created, also the included thread is started.
	 *
	 * @return The UpdateInformationHandler instance.
	 */
	public static UpdateInformationHandler getInstance()
	{
		synchronized (UpdateInformationHandler.class)
		{
			if (ms_uihInstance == null)
			{
				ms_uihInstance = new UpdateInformationHandler();
				Thread uihThread = new Thread(ms_uihInstance, "UpdateInformationHandler");
				uihThread.setDaemon(true);
				uihThread.start();
			}
		}
		return ms_uihInstance;
	}

	/**
	 * Creates a new instance of UpdateInformationHandler.
	 */
	private UpdateInformationHandler()
	{
	}

	/**
	 * This is the propaganda thread for all JAP update specific information. It posts the current
	 * JAP update information periodically (default: 10 minutes, see
	 * Constants.UPDATE_INFORMATION_ANNOUNCE_PERIOD) to all neighbour infoservices. Also it reads
	 * the information from the local filesystem, if this is the root-of-update information
	 * infoservice.
	 */
	public void run()
	{
		IDistributable distributable;
		Vector distributables;
		Element[] entries;

		while (true)
		{
			if (Configuration.getInstance().isRootOfUpdateInformation())
			{
				/* we are the root of update information -> try to read the update files from disk */
				try
				{
					byte[] releaseJnlpData = readLocalFile(Configuration.getInstance().getJapReleaseJnlpFile());
					Document releaseJnlpDocument = XMLUtil.toXMLDocument(releaseJnlpData);
					Element jnlpNode = (Element) (XMLUtil.getFirstChildByName(releaseJnlpDocument,
						JAPVersionInfo.getXmlElementName()));
					prepareEntryForPropaganda(jnlpNode);
					/* update the internal database and propagate the new version info */
					Database.getInstance(JAPVersionInfo.class).update(new JAPVersionInfo(jnlpNode,
						JAPVersionInfo.JAP_RELEASE_VERSION));
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "Error while processing JAP release information: " + e);
					/* try to propagate the JAP release version info from the database, if we have one
					 * stored there
					 */
					distributable = (JAPVersionInfo) (Database.getInstance(JAPVersionInfo.class).
						getEntryById(JAPVersionInfo.ID_RELEASE));
					if (distributable != null)
					{
						/* we have found a old entry in the database -> better than nothing */
						InfoServiceDistributor.getInstance().addJob(distributable);
					}
				}
				try
				{
					byte[] developmentJnlpData = readLocalFile(Configuration.getInstance().
						getJapDevelopmentJnlpFile());
					Document developmentJnlpDocument = XMLUtil.toXMLDocument(developmentJnlpData);
					Element jnlpNode = (Element) (XMLUtil.getFirstChildByName(developmentJnlpDocument,
						JAPVersionInfo.getXmlElementName()));
					prepareEntryForPropaganda(jnlpNode);
					/* update the internal database and propagate the new version info */
					Database.getInstance(JAPVersionInfo.class).update(new JAPVersionInfo(jnlpNode,
						JAPVersionInfo.JAP_DEVELOPMENT_VERSION));
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "Error while processing JAP development information: " + e);
					/* try to propagate the JAP development version info from the database, if we have one
					 * stored there
					 */
					distributable = (JAPVersionInfo) (Database.getInstance(JAPVersionInfo.class).
						getEntryById(JAPVersionInfo.ID_DEVELOPMENT));
					if (distributable != null)
					{
						/* we have found a old entry in the database -> better than nothing */
						InfoServiceDistributor.getInstance().addJob(distributable);
					}
				}
				try
				{
					byte[] minVersionData = readLocalFile(Configuration.getInstance().getJapMinVersionFile());
					Document minVersionDocument = XMLUtil.toXMLDocument(minVersionData);
					Element japNode = (Element) (XMLUtil.getFirstChildByName(minVersionDocument,
						JAPMinVersion.getXmlElementName()));
					prepareEntryForPropaganda(japNode);
					/* update the internal database and propagate the new minimum version info */
					Database.getInstance(JAPMinVersion.class).update(new JAPMinVersion(japNode));
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "Error while processing JAP minimum version information: " + e.toString());
					/* try to propagate the JAP minimum version info from the database, if we have one
					 * stored there
					 */
					distributable = (JAPMinVersion) (Database.getInstance(JAPMinVersion.class).
						getEntryById(JAPMinVersion.DEFAULT_ID));
					if (distributable != null)
					{
						/* we have found a old entry in the database -> better than nothing */
						InfoServiceDistributor.getInstance().addJob(distributable);
					}
				}

				try
				{
					entries =
						XMLUtil.readElementsByTagName(Configuration.getInstance().getMessageFile(),
													  MessageDBEntry.XML_ELEMENT_NAME);
					for (int i = 0; i < entries.length; i++)
					{
						prepareEntryForPropaganda(entries[i]);

						try
						{
							Database.getInstance(MessageDBEntry.class).update(new MessageDBEntry(entries[i]));
						}
						catch (Exception ex)
						{
							LogHolder.log(LogLevel.ERR, LogType.NET,
										  "Error while processing message information.", ex);
						}
						entries[i] = null;
					}
					entries = null;
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "Error while processing message information.", a_e);
				}


				try
				{
					entries =
						XMLUtil.readElementsByTagName(Configuration.getInstance().getJavaLatestVersionFile(),
						JavaVersionDBEntry.XML_ELEMENT_NAME);
					for (int i = 0; i < entries.length; i++)
					{
						prepareEntryForPropaganda(entries[i]);

						try
						{
							Database.getInstance(
							 JavaVersionDBEntry.class).update(new JavaVersionDBEntry(entries[i]));
						}
						catch (Exception ex)
						{
							LogHolder.log(LogLevel.ERR, LogType.NET,
										  "Error while processing Java version information.", ex);
						}
						entries[i] = null;
					}
					entries = null;
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "Error while processing Java version information.", a_e);
				}
			}
			else
			{
				/* we are not the root of update information, nevertheless we propagate the update
				 * information from the local database (this is necessary, if the root of update
				 * information infoservice is down)
				 */
				distributables = Database.getInstance(JavaVersionDBEntry.class).getEntryList();
				distributables.addElement(Database.getInstance(JAPVersionInfo.class).
										  getEntryById(JAPVersionInfo.ID_RELEASE));
				distributables.addElement(Database.getInstance(JAPVersionInfo.class).
										  getEntryById(JAPVersionInfo.ID_DEVELOPMENT));
				distributables.addElement(Database.getInstance(JAPMinVersion.class).
										  getEntryById(JAPMinVersion.DEFAULT_ID));

				for (int i = 0; i < distributables.size(); i++)
				{
					if (distributables.elementAt(i) != null)
					{
						InfoServiceDistributor.getInstance().addJob(
											  (IDistributable)distributables.elementAt(i));
					}
				}
				distributables.clear();
				distributables = null;
			}
			try
			{
				Thread.sleep(Constants.UPDATE_INFORMATION_ANNOUNCE_PERIOD);
			}
			catch (InterruptedException e)
			{
			}
		}
	}

	/**
	 * Reads a file from local filesystem and returns the data.
	 *
	 * @param a_fileName The file to read (path + filename).
	 *
	 * @return The data read from the file or null, if there was an error.
	 */
	private byte[] readLocalFile(String a_fileName)
	{
		try
		{
			FileReader fr = new FileReader(a_fileName);
			File file = new File(a_fileName);
			int fileSize = (int) (file.length());
			char[] readData = new char[fileSize];
			int count = fr.read(readData);
			if (count != fileSize)
			{
				throw (new Exception("Error reading file."));
			}
			fr.close();
			fr = null;
			file = null;
			return new String(readData).getBytes();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "readLocalFile(" + a_fileName + "): " + e);
			return null;
		}
	}

	/**
	 * This prepares a JAP update information entry read from the local filesystem for propaganda
	 * within the infoservice network. The LastUpdate timestamp will be updated and the whole
	 * structure is signed with the update messages certificate. This method is only necessary
	 * within the root-of-update-information infoservice.
	 *
	 * @param a_entryRootElement The root node of the update entry to prepare.
	 */
	private void prepareEntryForPropaganda(Element a_entryRootElement)
	{
		/* remove all LastUpdate and Signature nodes (if there are any) from the root element */
		NodeList lastUpdateNodes = a_entryRootElement.getElementsByTagName("LastUpdate");
		for (int i = 0; i < lastUpdateNodes.getLength(); i++)
		{
			a_entryRootElement.removeChild(lastUpdateNodes.item(i));
		}
		NodeList signatureNodes = a_entryRootElement.getElementsByTagName("Signature");
		for (int i = 0; i < signatureNodes.getLength(); i++)
		{
			a_entryRootElement.removeChild(signatureNodes.item(i));
		}
		/* create a new LastUpdate node and sign the whole JNLP structure */
		Element lastUpdateNode = a_entryRootElement.getOwnerDocument().createElement("LastUpdate");
		XMLUtil.setValue(lastUpdateNode, Long.toString(System.currentTimeMillis()));
		a_entryRootElement.appendChild(lastUpdateNode);
		/* try to sign the XML root node */
		if (SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_UPDATE,
			a_entryRootElement) == false)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC,
						  "The update information cannot be signed. Propagate unsigned information.");
		}
	}

}
