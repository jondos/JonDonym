/*
 Copyright (c) 2000, The JAP-Team
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

package update;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.awt.Container;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.IProxyInterfaceGetter;
import anon.infoservice.JAPVersionInfo;
import anon.infoservice.ListenerInterface;
import gui.JAPMessages;
import gui.wizard.BasicWizard;
import gui.wizard.BasicWizardHost;
import gui.wizard.WizardPage;
import gui.dialog.JAPDialog;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPModel;
import jap.JAPUtil;
import jarify.JarVerifier;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.util.ClassUtil;
import anon.util.Util;
import jap.AbstractJAPMainView;
import gui.GUIUtils;
import anon.infoservice.IMutableProxyInterface;
public final class JAPUpdateWizard extends BasicWizard implements Runnable
{
	public JAPWelcomeWizardPage welcomePage;
	public JAPDownloadWizardPage downloadPage;
	public JAPFinishWizardPage finishPage;
	private BasicWizardHost host;

	//private Vector m_Pages;


	//private JAPUpdateWizard updateWizard;

	//private String pathToJapJar;
	//fileName->> Name of the chosen File without extension and path i.e. 'Jap'
	//extension->> the extension of the chosen File ie. '.jar'
	// path ->> path to the chosen File without extension i.e. 'C:\Programme\'
	private String m_strAktJapJarFileName;
	private String m_strAktJapJarExtension;
	private String m_strAktJapJarPath;

	private final static String EXTENSION_BACKUP = ".backup";
	private final static String EXTENSION_NEW = ".new";

	private static final File CLASSFILE = ClassUtil.getClassDirectory(ClassUtil.class);

	private boolean updateAborted = false;

	//which version chose the user
	private String m_strNewJapVersion;

	//which type dev or rel?
	private JAPVersionInfo japVersionInfo;

	//aktJapJar --> the original JAP.jar; cp_aktJapJar --> the copy of the original File extended by the current version-number
	//i.e. JAPaktVersion.jar; cp_updJapJar --> the downloaded Upgrade extended by the version-number
	//cp_updJapJar --> copy of the newJarFile without version-number named JAP.jar

	private File m_fileAktJapJar;
	private File m_fileJapJarCopy;
	private File m_fileNewJapJar;
	private File updJapJar;

	//private int countPackages = 0;

	//private int countBytes = 0;
	//private int value = 0;
	//private int totalLength = 0;
	private byte[] m_arBufferNewJapJar = null;

	private Thread updateThread;

	//private UpdateListener updateListener;

	private int m_Status;
	public final static int UPDATESTATUS_SUCCESS = 0;
	public final static int UPDATESTATUS_ABORTED = 1;
	public final static int UPDATESTATUS_ERROR = -1;

	public JAPUpdateWizard(JAPVersionInfo info, JAPDialog a_dialog)
	{
		this(info, (Object)a_dialog);
	}

	public JAPUpdateWizard(JAPVersionInfo info, Container a_container)
	{
		this(info, (Object)a_container);
	}


	private JAPUpdateWizard(JAPVersionInfo info, Object a_parent)
	{
		setWizardTitle("JAP Update Wizard");
		if (a_parent instanceof JAPDialog)
		{
			host = new BasicWizardHost((JAPDialog)a_parent, this);
		}
		else
		{
			host = new BasicWizardHost((Container)a_parent, this);
		}
		setHost(host);
		m_Status = UPDATESTATUS_ABORTED;
		japVersionInfo = info;
		m_strNewJapVersion = info.getJapVersion();
		//updateWizard = this;
		welcomePage = new JAPWelcomeWizardPage(info);
		downloadPage = new JAPDownloadWizardPage();
		finishPage = new JAPFinishWizardPage();

		addWizardPage(0, welcomePage);
		addWizardPage(1, downloadPage);
		addWizardPage(2, finishPage);
		//m_Pages = getPageVector();
		invokeWizard( /*host*/);
	}

	public int getStatus()
	{
		return m_Status;
	}

	private void startUpdateThread()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Start update...");
		updateThread = new Thread(this,"JAPUpdateWizard");
		updateThread.setDaemon(true);
		updateThread.start();
	}

	public void run()
	{
		m_Status = UPDATESTATUS_SUCCESS;
		// Start with Step 1 copy
		if (renameJapJar() != 0)
		{
			downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep1"));
			resetChanges();
			return;
		}
		// Step 2 - download (either full or incremental)
		if (downloadUpdate() != 0)
		{
			if (!updateAborted)
			{
				if (JAPModel.getInstance().getUpdateAnonymousConnectionSetting() ==
					JAPModel.CONNECTION_FORCE_ANONYMOUS &&
					!JAPController.getInstance().isAnonConnected())
				{
					int answer =
						JAPDialog.showConfirmDialog(downloadPage,
						JAPMessages.getString("updateInformationMsgStep2")
						+ JAPMessages.getString("updateInformationMsgStep2_noDirectConn"),
						JAPDialog.OPTION_TYPE_YES_NO, JAPDialog.MESSAGE_TYPE_ERROR);
					if (answer == JAPDialog.RETURN_VALUE_YES)
					{
						JAPModel.getInstance().setUpdateAnonymousConnectionSetting(JAPModel.CONNECTION_ALLOW_ANONYMOUS);
					}
				}
				else if (JAPModel.getInstance().getUpdateAnonymousConnectionSetting() ==
					JAPModel.CONNECTION_BLOCK_ANONYMOUS &&
					JAPController.getInstance().isAnonConnected())
				{
					int answer =
						JAPDialog.showConfirmDialog(downloadPage,
						JAPMessages.getString("updateInformationMsgStep2")
						+ JAPMessages.getString("updateInformationMsgStep2_noAnonConn"),
						JAPDialog.OPTION_TYPE_YES_NO, JAPDialog.MESSAGE_TYPE_ERROR);
					if (answer == JAPDialog.RETURN_VALUE_YES)
					{
						JAPModel.getInstance().setUpdateAnonymousConnectionSetting(JAPModel.CONNECTION_ALLOW_ANONYMOUS);
					}
				}
				else
				{
					downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep2"));
				}
			}
			resetChanges();
			return;
		}
		//Step 3 or 3' save new JAP.jar (probably with incremental changes)
		if (welcomePage.isIncrementalUpdate())
		{
			if (applyJARDiffJAPJar() != 0)
			{
				downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep3"));
				resetChanges();
				return;
			}
		}
		else
		{
			if (createNewJAPJar() != 0)
			{
				downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep3"));
				resetChanges();
				return;
			}
		}
		//Step 4 - check signature
		if(!checkSignature())
		{
			downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep4"));
			resetChanges();
			return;
		}
		// Step 5
		if (overwriteJapJar() != 0)
		{
			downloadPage.showInformationDialog(JAPMessages.getString("updateInformationMsgStep5"));
			return;
		}
		try
		{
			if (!m_fileNewJapJar.delete())
			{
				downloadPage.showInformationDialog(
								JAPMessages.getString("updateM_DeletingofJAP_new.jarfailed"));
				return;
			}
			host.setNextEnabled(true);
			host.setFinishEnabled(false);
			host.setCancelEnabled(false);
		}
		catch (Exception e)
		{
			downloadPage.showInformationDialog(e.toString());
			return;
		}
	}

	private void setJapJarFile(File japjarfile)
	{
		m_fileAktJapJar = japjarfile;
		parsePathToJapJar();
		//Setting the Texts according to the Jap.jar File choosen...
		String strFileNameJapJarBackup = m_strAktJapJarPath + m_strAktJapJarFileName +
			JAPConstants.aktVersion + EXTENSION_BACKUP + m_strAktJapJarExtension;
		downloadPage.m_labelSaveFrom.setText(m_fileAktJapJar.getAbsolutePath());
		downloadPage.m_labelSaveTo.setText(strFileNameJapJarBackup);
		downloadPage.m_labelStep3.setText(JAPMessages.getString("updateM_labelStep3Part1") + " " +
										  m_strAktJapJarFileName + m_strNewJapVersion + EXTENSION_NEW +
										  m_strAktJapJarExtension);
		finishPage.m_labelBackupOfJapJar.setText(strFileNameJapJarBackup);

	}

	public WizardPage next( /*WizardPage currentPage, WizardHost host*/)
	{

		if (! ( (WizardPage) m_Pages.elementAt(m_PageIndex)).checkPage())
		{
			return null;
		}
		//int pageIndex=m_Pages.indexOf(currentPage);
		m_PageIndex++;
		host.setBackEnabled(true);
		//already the last page --> tell the user that Jap exits itself

		//next page is FinishWizardPage
		if (m_PageIndex == m_Pages.size() - 1)
		{
			host.setFinishEnabled(true);
			host.setNextEnabled(false);
			try
			{
				updateThread.join();
			}
			catch (InterruptedException ie)
			{
				LogHolder.log(LogLevel.ERR, LogType.THREAD, ie);
			}
		}
		//if it is the DownloadWizardPage
		if (m_PageIndex == 1)
		{
			host.setBackEnabled(false);
			host.setFinishEnabled(false);
			host.setNextEnabled(false);
			setJapJarFile(welcomePage.getJapJarFile());
			host.showWizardPage(m_PageIndex);
			startUpdateThread();
		}
		else
		{
			host.showWizardPage(m_PageIndex);
		}

		return null;
	}

	public WizardPage finish( /*WizardPage currentPage, WizardHost host*/)
	{
		Container parent = host.getDialogParent().getOwner();
		host.getDialogParent().dispose();
		if (m_fileAktJapJar != null && m_fileAktJapJar.equals(CLASSFILE))
		{
			if (! (parent instanceof AbstractJAPMainView))
			{
				parent.setVisible(false);
			}

			JAPController.goodBye(false);
		}
		return null;
	}

	public WizardPage back( /*WizardPage currentPage, WizardHost host*/)
	{
		//		int pageIndex=m_Pages.indexOf(currentPage);
		// we are on the Finishpage --> don't go back to the first page
		if (m_PageIndex == (m_Pages.size() - 1))
		{
			host.setBackEnabled(false);
		}

		super.back( /*currentPage,host*/);
		return null;
	}

	public void wizardCompleted()
	{
		updateAborted = true;
		// updateThread.join();
		//updateListener.progress(0,0,20);
	}

//User's clicked next and the path to the chosen Jar-File is being set
	/* public void setPath(String pathToJapJar)
	 {

	  this.pathToJapJar = pathToJapJar;

	 }
	 */
	private void parsePathToJapJar()
	{
		try
		{
			m_strAktJapJarFileName = m_fileAktJapJar.getName();
			m_strAktJapJarPath = m_fileAktJapJar.getCanonicalPath();
			m_strAktJapJarPath = m_strAktJapJarPath.substring(0, m_strAktJapJarPath.length() -
				m_strAktJapJarFileName.length());
			m_strAktJapJarExtension = m_fileAktJapJar.getName();
			int i = m_strAktJapJarExtension.lastIndexOf('.');
			m_strAktJapJarExtension = m_strAktJapJarExtension.substring(i);
			m_strAktJapJarFileName = m_strAktJapJarFileName.substring(0, i);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
		}
	}

	//Step 1
	private int renameJapJar()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Start to make a copy of old JAP.jar!");
		
		//just copy the File and then rename the copy
		downloadPage.m_labelIconStep1.setIcon(downloadPage.arrow);
		try
		{
			//newFile = new File(prefix+JAPConstants.aktVersion2+suffix);
			m_fileJapJarCopy = new File(m_strAktJapJarPath + m_strAktJapJarFileName +
										JAPConstants.aktVersion + EXTENSION_BACKUP +
										m_strAktJapJarExtension);
			Util.copyStream(new FileInputStream(m_fileAktJapJar), 
					new FileOutputStream(m_fileJapJarCopy));
			//TODO
			//if totalLength!=0 ...
			// the first step has the Zone from 0 to 5 in the ProgressBar
			downloadPage.progressBar.setValue(5);
			downloadPage.progressBar.repaint();
			downloadPage.m_labelIconStep1.setIcon(downloadPage.stepfinished);
			return 0;
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "Could not make a copy of old JAP.jar: " + t.getMessage());
			return -1;
		}
	}

	/**
	 * This class manages the download process of a new Jap version jar file.
	 */
	final class JapDownloadManager implements Runnable
	{

		/**
		 * Stores the ListenerInterface of the system, where the new jar file is located.
		 */
		private ListenerInterface targetInterface;

		/**
		 * Stores the path, filename and the query String of the new jar file at the remote system.
		 */
		private String fileName;

		/**
		 * Stores the download result (0 if it was successful, -1 if there was an error).
		 */
		private int downloadResult;

		/**
		 * Stores the new jar file.
		 */
		private byte[] newJarBuff;

		/**
		 * Constructs a new JapDownloadManager.
		 *
		 * @param jarUrl The URL of the wanted jar file.
		 */
		public JapDownloadManager(URL jarUrl) throws Exception
		{
			downloadResult = -1;
			newJarBuff = null;
			String hostName = jarUrl.getHost();
			int port = jarUrl.getPort();
			if (port == -1)
			{
				port = 80;
			}
			targetInterface = new ListenerInterface(hostName, port);
			fileName = jarUrl.getFile();
		}

		/**
		 * This method is executed by the internal Thread. The jar file download is done here.
		 * Don't call this method directly.
		 */
		public void run()
		{
			try
			{
				/* HTTPConnectionFactory has the right proxy settings, it is updated for the infoservice.
				 * This connection is like the infoservice connection not anonymized by the JAP.
				 */
				HTTPConnection connection;
				HTTPResponse response = null;
				boolean bAnonProxy = false;
				IMutableProxyInterface proxyInterface =
					JAPModel.getInstance().getUpdateProxyInterface();
				IProxyInterfaceGetter proxyInterfaceGetter;


				for (int i = 0; (i < 2) && !Thread.currentThread().isInterrupted(); i++)
				{
					if (i == 1)
					{
						bAnonProxy = true;
					}

					proxyInterfaceGetter = proxyInterface.getProxyInterface(bAnonProxy);
					if (proxyInterfaceGetter == null)
					{
						continue;
					}

					try
					{
						connection = HTTPConnectionFactory.getInstance().createHTTPConnection(
							targetInterface, proxyInterfaceGetter.getProxyInterface());
						response = connection.Get(fileName);
					}
					catch (Exception a_e)
					{
						LogHolder.log(LogLevel.WARNING, LogType.NET, a_e);
						continue;
					}
					if (response.getStatusCode() == 200)
					{
						break;
					}
				}
				if (response == null || response.getStatusCode() != 200)
				{
					/* if someone waiting for the end of the download, notify him */
					synchronized (this)
					{
						notifyAll();
					}
					return;
				}
				int lenTotal = response.getHeaderAsInt("Content-Length");
				InputStream in = response.getInputStream();
				byte[] buff = new byte[2048];
				newJarBuff = new byte[lenTotal];
				int currentPos = 0;
				int len = in.read(buff);
				while (len > 0)
				{
					System.arraycopy(buff, 0, newJarBuff, currentPos, len);
					currentPos +=  len;
					// the Download has the Zone from 5 to 455 in the ProgressBar
					int value = (int)(((450 * (long)currentPos) / (long)lenTotal));
					downloadPage.progressBar.setValue(value + 5);
					downloadPage.progressBar.repaint();
					if (updateAborted)
					{
						in.close();
						/* if someone waiting for the end of the download, notify him */
						synchronized (this)
						{
							notifyAll();
						}
						return;
					}
					len = in.read(buff);
				}

				/* download ready */
				downloadResult = 0;
				/* if someone waiting for the end of the download, notify him */
				synchronized (this)
				{
					notifyAll();
				}
			}
			catch (Exception e)
			{
				/* if someone waiting for the end of the download, notify him */
				synchronized (this)
				{
					notifyAll();
				}
			}
		}

		/**
		 * This method starts the download of the new jar file by creating the internal thread.
		 */
		public void startDownload()
		{
			Thread downloadThread = new Thread(this);
			downloadThread.start();
		}

		/**
		 * Returns the error code of the download (0 - download successful, -1 - download aborted).
		 *
		 * @return The result code of the download.
		 */
		public int getDownloadResult()
		{
			return downloadResult;
		}

		/**
		 * Returns the new jar file as byte array. Null is returned, if getDownloadResult() is not 0.
		 *
		 * @return The byte array of the new jar file.
		 */
		public byte[] getNewJar()
		{
			if (getDownloadResult() == 0)
			{
				return newJarBuff;
			}
			return null;
		}

	}

	/**
	 * Downloads a new JAP jar file.
	 *
	 * @return The error code of the download (0 - successful, -1 - there was an error / abort).
	 */
	private int downloadUpdate()
	{
		URL codeBase = japVersionInfo.getCodeBase();
		URL jarUrl=null;
		try
		{
			if (welcomePage.isIncrementalUpdate())
			{
				jarUrl = new URL(codeBase,
								 japVersionInfo.getJAPJarFileName() + "?version-id=" +
								 japVersionInfo.getJapVersion()
								 + "&current-version-id=" +
								 JAPConstants.aktVersion);
			}
			else
			{
				jarUrl = new URL(codeBase,
								 japVersionInfo.getJAPJarFileName() + "?version-id=" +
								 japVersionInfo.getJapVersion());
			}
		}
		catch (Exception e)
		{
			return -1;
		}
		downloadPage.m_labelIconStep2.setIcon(downloadPage.arrow);
		try
		{
			JapDownloadManager downloadManager = new JapDownloadManager(jarUrl);
			synchronized (downloadManager)
			{
				downloadManager.startDownload();
				/* wait for the end of the download */
				downloadManager.wait();
			}
			if (downloadManager.getDownloadResult() == -1)
			{
				return -1;
			}
			m_arBufferNewJapJar = downloadManager.getNewJar();
			downloadPage.m_labelIconStep2.setIcon(downloadPage.stepfinished);
			return 0;
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	//Step 3 needed by a full Update
	private int createNewJAPJar()
	{
		try
		{
			m_fileNewJapJar = new File(m_strAktJapJarPath + m_strAktJapJarFileName + m_strNewJapVersion +
									   EXTENSION_NEW + m_strAktJapJarExtension);
			FileOutputStream fos = new FileOutputStream(m_fileNewJapJar);
			if (m_arBufferNewJapJar == null)
			{
				fos.close();
				return -1;
			}
			downloadPage.m_labelIconStep3.setIcon(downloadPage.arrow);
			fos.write(m_arBufferNewJapJar);
			fos.flush();
			fos.close();
			// the creatNewJapJar step has the Zone from 455 to 490 in the ProgressBar
			downloadPage.progressBar.setValue(490);
			downloadPage.progressBar.repaint();
			downloadPage.m_labelIconStep3.setIcon(downloadPage.stepfinished);
			return 0;
		}
		catch (Exception e)
		{
			return -1;
		}
	}

/////////////////////////////////////////////////////////////////////////////////
	//Step 3' needed by a differential Update
	private synchronized int applyJARDiffJAPJar()
	{
		//get the buffer (bufferJapJar) where the data is stored
		//apply the JarDiff
		//create a new File "Jap_"+newversion+".jar"

		try
		{
			m_fileNewJapJar = new File(m_strAktJapJarPath + m_strAktJapJarFileName + m_strNewJapVersion +
									   EXTENSION_NEW + m_strAktJapJarExtension);
			if (JAPUtil.applyJarDiff(m_fileAktJapJar,
									 m_fileNewJapJar, m_arBufferNewJapJar) != 0)
			{
				return -1;
			}
			downloadPage.m_labelIconStep3.setIcon(downloadPage.arrow);
			// the creatNewJapJar step has the Zone from 455 to 490 in the ProgressBar
			downloadPage.progressBar.setValue(490);
			downloadPage.progressBar.repaint();
			downloadPage.m_labelIconStep3.setIcon(downloadPage.stepfinished);
			return 0;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return -1; //listener.progress(0,0,UpdateListener.STATE_ABORTED_STEP3);
		}

	}

/**
	 * Step 4 check the signature of the downloaded file
	 * @return true, if Signature of downloaded JAP.jar is ok
	 * @return false otherwise
**/
private boolean checkSignature()
{
	return JarVerifier.verify(m_fileNewJapJar,JAPModel.getJAPCodeSigningCert());
}

/////////////////////////////////////////////////////////////////////////////////
	//Step 5 create the new JAP.jar-File by overwriting the oldFile by the new downloaded file
	private int overwriteJapJar()
	{
		try
		{
			if (m_fileAktJapJar != null && m_fileAktJapJar.equals(CLASSFILE))
			{
				// If the parent jar is overwritten, no images should be loaded any more!
				GUIUtils.setLoadImages(false);
			}
			downloadPage.m_labelIconStep5.setIcon(downloadPage.arrow);
	
			Util.copyStream(new FileInputStream(m_fileNewJapJar),
					new FileOutputStream(m_fileAktJapJar));
			
			// the 5th step has the Zone from 490 to 500 in the ProgressBar
			downloadPage.progressBar.setValue(500);
			downloadPage.progressBar.repaint();
			downloadPage.m_labelIconStep5.setIcon(downloadPage.stepfinished);
			return 0;
		}
		catch (Exception e)
		{
			GUIUtils.setLoadImages(true);
			return -1;
		}
	}

	// method deletes all Files created new while the updating if there is an abort
	// by the system
	private void resetChanges()
	{
		if (updateAborted)
		{
			m_Status = UPDATESTATUS_ABORTED;
		}
		else
		{
			m_Status = UPDATESTATUS_ERROR;
		}
		if (m_fileJapJarCopy != null)
		{
			m_fileJapJarCopy.delete();
		}
		if (m_fileNewJapJar != null)
		{
			m_fileNewJapJar.delete();
		}
		if (updJapJar != null)
		{
			updJapJar.delete();
		}
		host.getDialogParent().dispose();
	}
}
