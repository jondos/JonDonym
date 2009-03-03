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
package jap.forward;

import java.io.ByteArrayInputStream;
import java.util.Vector;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.transport.address.Endpoint;
import anon.transport.address.IAddress;
import anon.util.JAPMessages;
import anon.util.captcha.IImageEncodedCaptcha;
import forward.client.ClientForwardException;
import forward.client.ForwardConnectionDescriptor;
import forward.client.ForwarderInformationGrabber;
import gui.JAPHtmlMultiLineLabel;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPModel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import jap.TrustModel;

/**
 * This is implementation of the dialog shown when starting a forwarded
 * connection. The dialog is asking the user some parameters and establishes the
 * forwarded connection. Also the anonymity mode is initialized by this dialog.
 */
public class JAPRoutingEstablishForwardedConnectionDialog
	{
		private boolean m_bForwardingSuccessful = false;

		/**
		 * Stores the parent component over which all windows of this dialog are
		 * centered.
		 */
		private Component m_parentComponent;

		/**
		 * Stores the default font to use within the whole dialog.
		 */
		private Font m_fontSetting;

		/**
		 * Creates a new JAPRoutingEstablishForwardedConnectionDialog. This will
		 * create a forwarded connection and starting the anonymity mode (if
		 * possible). Only the Java-AWT event-dispatch thread should call this
		 * constructor. Any other caller will produce a freeze here.
		 * 
		 * @param a_parentComponent
		 *          The parent component over which the dialog is centered.
		 * @param a_defaultFont
		 *          The default font to use within the dialog.
		 */
		public JAPRoutingEstablishForwardedConnectionDialog(
				Component a_parentComponent)
			{
				m_parentComponent = a_parentComponent;

				/** @todo forwarder does not yet support trust models */
				TrustModel.setCurrentTrustModel(TrustModel.TRUST_MODEL_ALL);

				/* show the whole dialog including all steps */
				boolean endOfDialogReached = false;
				while (endOfDialogReached == false)
					{
						/* show all steps of the dialog, until it is done */
						IImageEncodedCaptcha fetchedCaptcha = null;

						if (JAPModel.getInstance().getRoutingSettings()
								.getForwardInfoService() == false)
							{
								fetchedCaptcha = showConfigClientDialogGetForwarderInfo();
							}
						if (fetchedCaptcha == null)
							{
								fetchedCaptcha = showConfigClientDialogViaMail();
							}

						if (fetchedCaptcha == null)
							{
								/* no captcha could be fetched -> stop the dialog */
								endOfDialogReached = true;
							}
						else
							{
								ListenerInterface forwarder = showConfigClientDialogCaptcha(fetchedCaptcha);
								if (forwarder == null)
									{
										/* user pressed cancel -> stop the dialog */
										endOfDialogReached = true;
									}
								else
									{
										/* solving the captcha was successful */
										JAPModel.getInstance().getRoutingSettings().setForwarder(
												forwarder.getHost(), forwarder.getPort());
										if (showConfigClientDialogConnectToForwarder() == true)
											{
												/*
												 * contacting the forwarder was successful -> in the
												 * other case, we show the fetch-captcha step again in
												 * the next while loop
												 */
												ForwardConnectionDescriptor connectionOffer = showConfigClientDialogGetOffer();
												if (connectionOffer != null)
													{
														/*
														 * obtaining the connection offer from the forwarder
														 * was successful -> in the other case, we show the
														 * fetch-captcha step again in the next while loop
														 */
														MixCascade selectedMixCascade = showConfigClientDialogStep2(connectionOffer);
														if (selectedMixCascade != null)
															{
																/*
																 * the user has selected a mixcascade ->
																 * announce that cascade to the forwarder; if
																 * ther user pressed cancel, we show the
																 * fetch-captcha step again in the next while
																 * loop
																 */
																/*
																 * if the final step is successful, we can close
																 * the dialog
																 */
																m_bForwardingSuccessful = showConfigClientDialogAnnounceCascade(selectedMixCascade);
																endOfDialogReached = m_bForwardingSuccessful;
															}
													}
											}
									}
							}
					}
				/* forwarding successful enabled or the dialog was canceled by the user */
			}

		/**
		 * Creates a new JAPRoutingEstablishForwardedConnectionDialog. This will
		 * create a forwarded connection and starting the anonymity mode (if
		 * possible). No CAPATCHE Dialog will be shown, as the Address of the
		 * Forwarding Server is directly provided.
		 * 
		 * @param a_parentComponent
		 *          The parent component over which the dialog is centered.
		 * @param a_address
		 *          The address of the forwarding Server
		 */
		public JAPRoutingEstablishForwardedConnectionDialog(
				Component a_parentComponent, IAddress a_address)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.NET,"Start establishing forward connection with a given address");
				m_parentComponent = a_parentComponent;

				/** @todo forwarder does not yet support trust models */
				TrustModel.setCurrentTrustModel(TrustModel.TRUST_MODEL_ALL);

				// bypass captcha and directly set the address
				JAPModel.getInstance().getRoutingSettings().setForwarderAddress(
						a_address);
				if (!showConfigClientDialogConnectToForwarder()) return;
				/*
				 * contacting the forwarder was successful -> in the other case, we show
				 * the fetch-captcha step again in the next while loop
				 */
				ForwardConnectionDescriptor connectionOffer = showConfigClientDialogGetOffer();
				if (connectionOffer == null) return;
				/*
				 * obtaining the connection offer from the forwarder was successful ->
				 * in the other case, we show the fetch-captcha step again in the next
				 * while loop
				 */
				// MixCascade
				MixCascade selectedMixCascade = JAPController.getInstance()
						.getCurrentMixCascade(); // check if we can use a self defined
																			// cascade hopefully given on the command
																			// line...
				if (selectedMixCascade == null || !selectedMixCascade.isUserDefined()) // use
																																								// a
																																								// self
																																								// defined
																																								// cascade
																																								// hopefully
																																								// given
																																								// on
																																								// the
																																								// command
																																								// line
																																								// ...
					{
						selectedMixCascade = showConfigClientDialogStep2(connectionOffer);
					}
				if (selectedMixCascade == null) return;
				/*
				 * the user has selected a mixcascade -> announce that cascade to the
				 * forwarder; if there user pressed cancel, we show the fetch-captcha
				 * step again in the next while loop
				 */
				/*
				 * if the final step is successful, we can close the dialog
				 */
				m_bForwardingSuccessful = showConfigClientDialogAnnounceCascade(selectedMixCascade);

			}

		public boolean isForwardingSuccessful()
			{
				return m_bForwardingSuccessful;
			}

		/**
		 * Returns the parent component over which all windows of this dialog are
		 * centered.
		 * 
		 * @return The parent component of this dialog.
		 */
		private Component getRootComponent()
			{
				return m_parentComponent;
			}

		/**
		 * Returns the default font to use within the whole dialog.
		 * 
		 * @return The default font for this dialog.
		 */
		private Font getFontSetting()
			{
				return m_fontSetting;
			}

		/**
		 * Shows the get forwarder information from infoservice box in the client
		 * configuration dialog.
		 * 
		 * @return The captcha fetched from the infoservice or null, if fetching the
		 *         captcha failed (e.g. if the user pressed 'cancel').
		 */
		private IImageEncodedCaptcha showConfigClientDialogGetForwarderInfo()
			{
				final JAPDialog infoserviceDialog = new JAPDialog(getRootComponent(),
						JAPMessages
								.getString("settingsRoutingClientConfigDialogInfoServiceTitle"));
				infoserviceDialog.setResizable(false);
				infoserviceDialog
						.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

				/* this Vector contains a message, if an error occured. */
				final Vector occuredError = new Vector();
				/*
				 * this Vector contains the fetched captcha, if no error occured while
				 * fetching
				 */
				final Vector fetchedCaptcha = new Vector();

				final Runnable infoserviceThread = new Runnable()
				{
					public void run()
						{
							/* this is the infoservice get forwarder thread */
							/*
							 * create a new ForwarderInformationGrabber, which fetches a
							 * captcha from the infoservices
							 */
							ForwarderInformationGrabber grabber = new ForwarderInformationGrabber();
							/* clear the interrupted flag, if it is set */
							Thread.interrupted();
							if (grabber.getErrorCode() == ForwarderInformationGrabber.RETURN_SUCCESS)
								{
									/*
									 * we are successful -> store the captcha -> next is the
									 * solve-captcha dialog
									 */
									fetchedCaptcha.addElement(grabber.getCaptcha());
								}
							else if (grabber.getErrorCode() == ForwarderInformationGrabber.RETURN_INFOSERVICE_ERROR)
								{
									occuredError
											.addElement(JAPMessages
													.getString("settingsRoutingClientGrabCapchtaInfoServiceError"));
								}
							else if (grabber.getErrorCode() == ForwarderInformationGrabber.RETURN_NO_CAPTCHA_IMPLEMENTATION)
								{
									occuredError
											.addElement(JAPMessages
													.getString("settingsRoutingClientGrabCapchtaImplementationError"));
								}
							else
								{
									/* an unknown error occured */
									occuredError
											.addElement(JAPMessages
													.getString("settingsRoutingClientGrabCaptchaUnknownError"));
								}
						}
				};

				WorkerContentPane worker = new WorkerContentPane(
						infoserviceDialog,
						JAPMessages
								.getString("settingsRoutingClientConfigDialogInfoServiceLabel"),
						infoserviceThread);
				worker.setInterruptThreadSafe(false);
				worker.updateDialog();
				infoserviceDialog.pack();
				infoserviceDialog.setVisible(true);

				IImageEncodedCaptcha returnValue = null;

				if (fetchedCaptcha.size() > 0)
					{
						returnValue = (IImageEncodedCaptcha) (fetchedCaptcha.firstElement());
					}
				else if (occuredError.size() > 0)
					{
						/**
						 * there occured an error while fetching the information about a
						 * forwarder from the InfoServices
						 * 
						 * @todo show this error in the next dialog (mail)
						 */
						LogHolder.log(LogLevel.ERR, LogType.NET, (String) (occuredError
								.firstElement()));
					}

				return returnValue;
			}

		/**
		 * Shows the first step of the client configuration dialog. This is the way
		 * to connect via the information from the mail system.
		 * 
		 * @return The captcha from the parsed user input or null, if no captcha
		 *         could be parsed (e.g. if the user pressed 'cancel').
		 */
		private IImageEncodedCaptcha showConfigClientDialogViaMail()
			{
				final JAPDialog client1MailDialog = new JAPDialog(getRootComponent(),
						JAPMessages
								.getString("settingsRoutingClientConfigDialog1MailTitle"));
				final JPanel client1MailPanel = new JPanel();
				client1MailDialog.getContentPane().add(client1MailPanel);

				JAPHtmlMultiLineLabel settingsRoutingClientConfigDialog1MailInstructionsLabel = new JAPHtmlMultiLineLabel(
						JAPMessages
								.getString("settingsRoutingClientConfigDialog1MailInstructions1")
								+ JAPConstants.MAIL_SYSTEM_ADDRESS
								+ JAPMessages
										.getString("settingsRoutingClientConfigDialog1MailInstructions2"),
						getFontSetting());

				JLabel settingsRoutingClientConfigDialog1MailAnswerLabel = new JLabel(
						JAPMessages
								.getString("settingsRoutingClientConfigDialog1MailAnswerLabel"));
				settingsRoutingClientConfigDialog1MailAnswerLabel
						.setFont(getFontSetting());
				final JTextArea settingsRoutingAnswerArea = new JTextArea();
				settingsRoutingAnswerArea.setFont(getFontSetting());
				settingsRoutingAnswerArea.setRows(7);
				JScrollPane settingsRoutingAnswerPane = new JScrollPane(
						settingsRoutingAnswerArea);
				settingsRoutingAnswerArea.addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent event)
						{
							handlePopupEvent(event);
						}

					public void mouseReleased(MouseEvent event)
						{
							handlePopupEvent(event);
						}

					private void handlePopupEvent(MouseEvent event)
						{
							if (event.isPopupTrigger())
								{
									JPopupMenu rightButtonMenu = new JPopupMenu();
									JMenuItem pasteItem = new JMenuItem(
											JAPMessages
													.getString("settingsRoutingClientConfigDialog1MailAnswerPopupPaste"));
									pasteItem.addActionListener(new ActionListener()
									{
										public void actionPerformed(ActionEvent event)
											{
												settingsRoutingAnswerArea.paste();
											}
									});
									rightButtonMenu.add(pasteItem);
									rightButtonMenu.show(event.getComponent(), event.getX(),
											event.getY());
								}
						}
				});

				JButton settingsRoutingClientConfigDialog1MailInsertButton = new JButton(
						JAPMessages
								.getString("settingsRoutingClientConfigDialog1MailInsertButton"));
				settingsRoutingClientConfigDialog1MailInsertButton
						.setFont(getFontSetting());
				settingsRoutingClientConfigDialog1MailInsertButton
						.addActionListener(new ActionListener()
						{
							public void actionPerformed(ActionEvent event)
								{
									/*
									 * removes all data in the answer area and inserts the text
									 * from the clipboard
									 */
									settingsRoutingAnswerArea.setText("");
									settingsRoutingAnswerArea.paste();
								}
						});

				/*
				 * this Vector contains the parsed captcha, if the inserted data are a
				 * valid captcha
				 */
				final Vector parsedCaptcha = new Vector();

				final JButton settingsRoutingClientConfigDialog1MailNextButton = new JButton(
						JAPMessages
								.getString("settingsRoutingClientConfigDialog1MailNextButton"));
				settingsRoutingClientConfigDialog1MailNextButton
						.setFont(getFontSetting());
				settingsRoutingClientConfigDialog1MailNextButton.setEnabled(false);
				settingsRoutingClientConfigDialog1MailNextButton
						.addActionListener(new ActionListener()
						{
							public void actionPerformed(ActionEvent event)
								{
									/*
									 * if the Next button is pressed, parse the replied data ->
									 * show the captcha dialog
									 */
									ForwarderInformationGrabber dataParser = new ForwarderInformationGrabber(
											settingsRoutingAnswerArea.getText());
									if (dataParser.getErrorCode() == ForwarderInformationGrabber.RETURN_SUCCESS)
										{
											parsedCaptcha.addElement(dataParser.getCaptcha());
											client1MailDialog.dispose();
										}
									if (dataParser.getErrorCode() == ForwarderInformationGrabber.RETURN_NO_CAPTCHA_IMPLEMENTATION)
										{
											JAPDialog
													.showErrorDialog(
															client1MailPanel,
															JAPMessages
																	.getString("settingsRoutingClientGrabCapchtaImplementationError"),
															LogType.MISC);
											client1MailDialog.dispose();
										}
									if (dataParser.getErrorCode() == ForwarderInformationGrabber.RETURN_UNKNOWN_ERROR)
										{
											JAPDialog
													.showErrorDialog(
															client1MailPanel,
															JAPMessages
																	.getString("settingsRoutingClientConfigDialog1MailParseError"),
															LogType.MISC);
											settingsRoutingAnswerArea.setText("");
										}
								}
						});

				settingsRoutingAnswerArea.addCaretListener(new CaretListener()
				{
					public void caretUpdate(CaretEvent event)
						{
							/*
							 * something was changed in the answer area -> if there is at
							 * least one character in there now, enable the next-button
							 */
							if (settingsRoutingAnswerArea.getText().equals("") == false)
								{
									settingsRoutingClientConfigDialog1MailNextButton
											.setEnabled(true);
								}
							else
								{
									/* no text in the answer area -> disable the next button */
									settingsRoutingClientConfigDialog1MailNextButton
											.setEnabled(false);
								}
						}
				});

				JButton settingsRoutingClientConfigDialog1MailCancelButton = new JButton(
						JAPMessages.getString("cancelButton"));
				settingsRoutingClientConfigDialog1MailCancelButton
						.setFont(getFontSetting());
				settingsRoutingClientConfigDialog1MailCancelButton
						.addActionListener(new ActionListener()
						{
							public void actionPerformed(ActionEvent event)
								{
									/* if the Cancel button is pressed, close the dialog */
									client1MailDialog.dispose();
								}
						});

				TitledBorder settingsRoutingClientConfigDialog1MailBorder = new TitledBorder(
						JAPMessages
								.getString("settingsRoutingClientConfigDialog1MailBorder"));
				settingsRoutingClientConfigDialog1MailBorder
						.setTitleFont(getFontSetting());
				client1MailPanel
						.setBorder(settingsRoutingClientConfigDialog1MailBorder);

				GridBagLayout client1MailPanelLayout = new GridBagLayout();
				client1MailPanel.setLayout(client1MailPanelLayout);

				GridBagConstraints client1MailPanelConstraints = new GridBagConstraints();
				client1MailPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
				client1MailPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				client1MailPanelConstraints.weightx = 1.0;
				client1MailPanelConstraints.gridwidth = 2;

				client1MailPanelConstraints.gridx = 0;
				client1MailPanelConstraints.gridy = 0;
				client1MailPanelConstraints.insets = new Insets(5, 5, 0, 5);
				client1MailPanelLayout.setConstraints(
						settingsRoutingClientConfigDialog1MailInstructionsLabel,
						client1MailPanelConstraints);
				client1MailPanel
						.add(settingsRoutingClientConfigDialog1MailInstructionsLabel);

				client1MailPanelConstraints.gridx = 0;
				client1MailPanelConstraints.gridy = 1;
				client1MailPanelConstraints.insets = new Insets(15, 5, 0, 5);
				client1MailPanelLayout.setConstraints(
						settingsRoutingClientConfigDialog1MailAnswerLabel,
						client1MailPanelConstraints);
				client1MailPanel.add(settingsRoutingClientConfigDialog1MailAnswerLabel);

				client1MailPanelConstraints.gridx = 0;
				client1MailPanelConstraints.gridy = 2;
				client1MailPanelConstraints.insets = new Insets(0, 5, 2, 5);
				client1MailPanelConstraints.weighty = 1.0;
				client1MailPanelLayout.setConstraints(settingsRoutingAnswerPane,
						client1MailPanelConstraints);
				client1MailPanel.add(settingsRoutingAnswerPane);

				client1MailPanelConstraints.gridx = 0;
				client1MailPanelConstraints.gridy = 3;
				client1MailPanelConstraints.insets = new Insets(0, 5, 20, 5);
				client1MailPanelConstraints.weighty = 0.0;
				client1MailPanelLayout.setConstraints(
						settingsRoutingClientConfigDialog1MailInsertButton,
						client1MailPanelConstraints);
				client1MailPanel
						.add(settingsRoutingClientConfigDialog1MailInsertButton);

				client1MailPanelConstraints.gridx = 0;
				client1MailPanelConstraints.gridy = 4;
				client1MailPanelConstraints.gridwidth = 1;
				client1MailPanelConstraints.insets = new Insets(0, 5, 10, 5);
				client1MailPanelLayout.setConstraints(
						settingsRoutingClientConfigDialog1MailCancelButton,
						client1MailPanelConstraints);
				client1MailPanel
						.add(settingsRoutingClientConfigDialog1MailCancelButton);

				client1MailPanelConstraints.gridx = 1;
				client1MailPanelConstraints.gridy = 4;
				client1MailPanelConstraints.insets = new Insets(0, 5, 10, 5);
				client1MailPanelLayout.setConstraints(
						settingsRoutingClientConfigDialog1MailNextButton,
						client1MailPanelConstraints);
				client1MailPanel.add(settingsRoutingClientConfigDialog1MailNextButton);

				client1MailDialog.pack();
				client1MailDialog.setVisible(true);

				IImageEncodedCaptcha returnValue = null;

				if (parsedCaptcha.size() > 0)
					{
						/* parsing the captcha-data was successful */
						returnValue = (IImageEncodedCaptcha) (parsedCaptcha.firstElement());
					}

				return returnValue;
			}

		/**
		 * Shows the "solve captcha" box in the client configuration dialog.
		 * 
		 * @param a_captcha
		 *          The captcha to solve.
		 * 
		 * @return The ListenerInterface of the forwarder (the soulution of the
		 *         captcha) or null, if the user pressed 'Cancel'.
		 */
		private ListenerInterface showConfigClientDialogCaptcha(
				final IImageEncodedCaptcha a_captcha)
			{
				final JAPDialog captchaDialog = new JAPDialog(getRootComponent(),
						JAPMessages
								.getString("settingsRoutingClientConfigDialogCaptchaTitle"));
				final JPanel captchaPanel = new JPanel();
				captchaDialog.getContentPane().add(captchaPanel);

				JLabel captchaImageLabel = new JLabel(new ImageIcon(a_captcha
						.getImage()));

				JLabel settingsRoutingClientConfigDialogCaptchaCharacterSetLabel = new JLabel(
						JAPMessages
								.getString("settingsRoutingClientConfigDialogCaptchaCharacterSetLabel")
								+ " " + a_captcha.getCharacterSet());
				settingsRoutingClientConfigDialogCaptchaCharacterSetLabel
						.setFont(getFontSetting());
				JLabel settingsRoutingClientConfigDialogCaptchaCharacterNumberLabel = new JLabel(
						JAPMessages
								.getString("settingsRoutingClientConfigDialogCaptchaCharacterNumberLabel")
								+ " " + Integer.toString(a_captcha.getCharacterNumber()));
				settingsRoutingClientConfigDialogCaptchaCharacterNumberLabel
						.setFont(getFontSetting());

				JLabel settingsRoutingClientConfigDialogCaptchaInsertCaptchaLabel = new JLabel(
						JAPMessages
								.getString("settingsRoutingClientConfigDialogCaptchaInsertCaptchaLabel"));
				settingsRoutingClientConfigDialogCaptchaInsertCaptchaLabel
						.setFont(getFontSetting());

				final JButton settingsRoutingClientConfigDialogCaptchaNextButton = new JButton(
						JAPMessages
								.getString("settingsRoutingClientConfigDialogCaptchaNextButton"));
				settingsRoutingClientConfigDialogCaptchaNextButton
						.setFont(getFontSetting());

				final JTextField captchaField = new JTextField()
				{
					/**
					 * serial version UID
					 */
					private static final long serialVersionUID = 1L;

					protected Document createDefaultModel()
						{
							return (new PlainDocument()
							{
								/**
								 * serial version UID
								 */
								private static final long serialVersionUID = 1L;

								public void insertString(int a_position,
										String a_stringToInsert, AttributeSet a_attributes)
										throws BadLocationException
									{
										if (getLength() + a_stringToInsert.length() <= a_captcha
												.getCharacterNumber())
											{
												/* the new text fits in the box */
												boolean invalidCharacters = false;
												int i = 0;
												while ((i < a_stringToInsert.length())
														&& (invalidCharacters == false))
													{
														if (a_captcha.getCharacterSet().indexOf(
																a_stringToInsert.toUpperCase().substring(i,
																		i + 1)) < 0)
															{
																/* we have found an invalid character */
																invalidCharacters = true;
															}
														i++;
													}
												if (invalidCharacters == false)
													{
														/*
														 * only insert strings, which fit in the box and
														 * have no invalid characters
														 */
														super.insertString(a_position, a_stringToInsert
																.toUpperCase(), a_attributes);
													}
											}
									}
							});
						}
				};
				captchaField.getDocument().addDocumentListener(new DocumentListener()
				{
					public void changedUpdate(DocumentEvent a_event)
						{
						}

					public void insertUpdate(DocumentEvent a_event)
						{
							if (a_event.getDocument().getLength() == a_captcha
									.getCharacterNumber())
								{
									settingsRoutingClientConfigDialogCaptchaNextButton
											.setEnabled(true);
								}
							else
								{
									settingsRoutingClientConfigDialogCaptchaNextButton
											.setEnabled(false);
								}
						}

					public void removeUpdate(DocumentEvent a_event)
						{
							if (a_event.getDocument().getLength() == a_captcha
									.getCharacterNumber())
								{
									settingsRoutingClientConfigDialogCaptchaNextButton
											.setEnabled(true);
								}
							else
								{
									settingsRoutingClientConfigDialogCaptchaNextButton
											.setEnabled(false);
								}
						}
				});
				captchaField.setFont(getFontSetting());

				/*
				 * this Vector contains the solved captcha (the interface data of a
				 * forwarder)
				 */
				final Vector forwarderInterface = new Vector();

				if (captchaField.getText().length() != a_captcha.getCharacterNumber())
					{
						/*
						 * maybe there will be an empty captcha, so disable only, if the
						 * text is not equal to the captcha length
						 */
						settingsRoutingClientConfigDialogCaptchaNextButton
								.setEnabled(false);
					}
				settingsRoutingClientConfigDialogCaptchaNextButton
						.addActionListener(new ActionListener()
						{
							public void actionPerformed(ActionEvent event)
								{
									/* if the Next button is pressed, we try to solve the captcha */
									try
										{
											byte[] test = new byte[10];
											byte[] plainForwarderData = a_captcha.solveCaptcha(
													captchaField.getText().trim(), test);
											/*
											 * with a extremely high chance, we have decrypted the
											 * correct forwarder information
											 */
											ByteArrayInputStream ipAddressStream = new ByteArrayInputStream(
													plainForwarderData, 10, 4);
											/* read the IP address */
											String ipAddress = Integer.toString(ipAddressStream
													.read());
											for (int i = 0; i < 3; i++)
												{
													ipAddress = ipAddress + "."
															+ Integer.toString(ipAddressStream.read());
												}
											ByteArrayInputStream portStream = new ByteArrayInputStream(
													plainForwarderData, 14, 2);
											int port = portStream.read();
											port = (port * 256) + portStream.read();

											ListenerInterface forwarder = new ListenerInterface(
													ipAddress, port);
											/*
											 * no error occured -> the solution is valid -> try to
											 * connect to that forwarder
											 */
											forwarderInterface.addElement(forwarder);
											captchaDialog.dispose();
										}
									catch (Exception e)
										{
											/* the inserted key is not valid */
											JAPDialog
													.showErrorDialog(
															captchaPanel,
															JAPMessages
																	.getString("settingsRoutingClientConfigDialogCaptchaError"),
															LogType.MISC);
											captchaField.setText("");
										}
								}
						});

				JButton settingsRoutingClientConfigDialogCaptchaCancelButton = new JButton(
						JAPMessages.getString("cancelButton"));
				settingsRoutingClientConfigDialogCaptchaCancelButton
						.setFont(getFontSetting());
				settingsRoutingClientConfigDialogCaptchaCancelButton
						.addActionListener(new ActionListener()
						{
							public void actionPerformed(ActionEvent event)
								{
									/* if the Cancel button is pressed, close the dialog */
									captchaDialog.dispose();
								}
						});

				TitledBorder settingsRoutingClientConfigDialogCaptchaBorder = new TitledBorder(
						JAPMessages
								.getString("settingsRoutingClientConfigDialogCaptchaBorder"));
				settingsRoutingClientConfigDialogCaptchaBorder
						.setTitleFont(getFontSetting());
				captchaPanel.setBorder(settingsRoutingClientConfigDialogCaptchaBorder);

				GridBagLayout captchaPanelLayout = new GridBagLayout();
				captchaPanel.setLayout(captchaPanelLayout);

				GridBagConstraints captchaPanelConstraints = new GridBagConstraints();
				captchaPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
				captchaPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
				captchaPanelConstraints.weightx = 1.0;
				captchaPanelConstraints.gridwidth = 2;

				captchaPanelConstraints.gridx = 0;
				captchaPanelConstraints.gridy = 0;
				captchaPanelConstraints.insets = new Insets(5, 5, 0, 5);
				captchaPanelLayout.setConstraints(captchaImageLabel,
						captchaPanelConstraints);
				captchaPanel.add(captchaImageLabel);

				captchaPanelConstraints.gridx = 0;
				captchaPanelConstraints.gridy = 1;
				captchaPanelConstraints.insets = new Insets(10, 5, 0, 5);
				captchaPanelLayout.setConstraints(
						settingsRoutingClientConfigDialogCaptchaCharacterSetLabel,
						captchaPanelConstraints);
				captchaPanel
						.add(settingsRoutingClientConfigDialogCaptchaCharacterSetLabel);

				captchaPanelConstraints.gridx = 0;
				captchaPanelConstraints.gridy = 2;
				captchaPanelConstraints.insets = new Insets(5, 5, 0, 5);
				captchaPanelLayout.setConstraints(
						settingsRoutingClientConfigDialogCaptchaCharacterNumberLabel,
						captchaPanelConstraints);
				captchaPanel
						.add(settingsRoutingClientConfigDialogCaptchaCharacterNumberLabel);

				captchaPanelConstraints.gridx = 0;
				captchaPanelConstraints.gridy = 3;
				captchaPanelConstraints.insets = new Insets(10, 5, 0, 5);
				captchaPanelLayout.setConstraints(
						settingsRoutingClientConfigDialogCaptchaInsertCaptchaLabel,
						captchaPanelConstraints);
				captchaPanel
						.add(settingsRoutingClientConfigDialogCaptchaInsertCaptchaLabel);

				captchaPanelConstraints.gridx = 0;
				captchaPanelConstraints.gridy = 4;
				captchaPanelConstraints.insets = new Insets(0, 5, 0, 5);
				captchaPanelLayout
						.setConstraints(captchaField, captchaPanelConstraints);
				captchaPanel.add(captchaField);

				captchaPanelConstraints.gridx = 0;
				captchaPanelConstraints.gridy = 5;
				captchaPanelConstraints.gridwidth = 1;
				captchaPanelConstraints.weighty = 1.0;
				captchaPanelConstraints.insets = new Insets(20, 5, 5, 5);
				captchaPanelLayout.setConstraints(
						settingsRoutingClientConfigDialogCaptchaCancelButton,
						captchaPanelConstraints);
				captchaPanel.add(settingsRoutingClientConfigDialogCaptchaCancelButton);

				captchaPanelConstraints.gridx = 1;
				captchaPanelConstraints.gridy = 5;
				captchaPanelConstraints.insets = new Insets(20, 5, 5, 5);
				captchaPanelLayout.setConstraints(
						settingsRoutingClientConfigDialogCaptchaNextButton,
						captchaPanelConstraints);
				captchaPanel.add(settingsRoutingClientConfigDialogCaptchaNextButton);

				captchaDialog.pack();
				captchaDialog.setVisible(true);

				ListenerInterface returnValue = null;

				if (forwarderInterface.size() > 0)
					{
						/* solving the captcha was successful */
						returnValue = (ListenerInterface) (forwarderInterface
								.firstElement());
					}

				return returnValue;
			}

		/**
		 * Shows the connect to forwarder box in the client configuration dialog.
		 * 
		 * @return True, if the contacting the forwarder was successful or false, if
		 *         the connection to the forwarder failed.
		 */
		private boolean showConfigClientDialogConnectToForwarder()
			{
				final JAPDialog connectDialog = new JAPDialog(
						getRootComponent(),
						JAPMessages
								.getString("settingsRoutingClientConfigConnectToForwarderTitle"));
				connectDialog.setResizable(false);
				connectDialog
						.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

				/* this Vector contains a message, if an error occured. */
				final Vector occuredError = new Vector();

				final Runnable connectThread = new Runnable()
				{
					public void run()
						{
							/* this is the connect to forwarder thread */
							/* the forwarder is already set, we only need to connect to */
							if (JAPModel.getInstance().getRoutingSettings().setRoutingMode(
									JAPRoutingSettings.ROUTING_MODE_CLIENT) == false)
								{
									/*
									 * error while connecting -> show a message and go back to
									 * step 1
									 */
									occuredError
											.addElement(JAPMessages
													.getString("settingsRoutingClientConfigConnectToForwarderError"));
								}
						}
				};

				IAddress currentForwarderAddress = JAPModel.getInstance()
						.getRoutingSettings().getForwarderAddress();
				String currentForwarderString = "";
				if (currentForwarderAddress != null)
					{
						currentForwarderString = Endpoint.toURN(currentForwarderAddress);
					}

				WorkerContentPane worker = new WorkerContentPane(
						connectDialog,
						JAPMessages
								.getString("settingsRoutingClientConfigDialogConnectToForwarderLabel"),
						JAPMessages
								.getString("settingsRoutingClientConfigDialogConnectToForwarderInfoLabel")
								+ " " + currentForwarderString, connectThread);
				worker.setInterruptThreadSafe(false);
				worker.updateDialog();
				connectDialog.pack();
				connectDialog.setVisible(true);

				boolean returnValue = false;

				if (worker.hasValidValue() && occuredError.size() == 0)
					{
						/* no error occured -> contacting the forwarder was successful */
						returnValue = true;
					}
				else if (occuredError.size() > 0)
					{
						/*
						 * there occured an error while connecting to the forwarder -> show
						 * a message and go back to step 1
						 */
						JAPDialog.showErrorDialog(getRootComponent(),
								(String) (occuredError.firstElement()), LogType.NET);
					}

				return returnValue;
			}

		/**
		 * Shows the get connection offer box in the client configuration dialog.
		 * 
		 * @return The descriptor with the connection offer from the forwarder or
		 *         null, if the descriptor could not be obtained (e.g. if the user
		 *         pressed 'cancel').
		 */
		private ForwardConnectionDescriptor showConfigClientDialogGetOffer()
			{
				final JAPDialog offerDialog = new JAPDialog(getRootComponent(),
						JAPMessages.getString("settingsRoutingClientConfigGetOfferTitle"));
				offerDialog.setResizable(false);
				offerDialog
						.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

				/* this Vector contains a message, if an error occured */
				final Vector occuredError = new Vector();
				/* this Vector contains the fetched connection descriptor */
				final Vector fetchedDescriptor = new Vector();

				final Runnable offerThread = new Runnable()
				{
					public void run()
						{
							/* this is the get connection offer thread */
							try
								{
									ForwardConnectionDescriptor connectionDescriptor = JAPModel
											.getInstance().getRoutingSettings()
											.getConnectionDescriptor();
									/*
									 * we are successful -> store the descriptor from the
									 * forwarder and show the next step
									 */
									fetchedDescriptor.addElement(connectionDescriptor);
								}
							catch (ClientForwardException e)
								{
									/* there was an error while receiving the connection offer */
									LogHolder.log(LogLevel.ERR, LogType.NET, e);
									if (e.getErrorCode() == ClientForwardException.ERROR_CONNECTION_ERROR)
										{
											occuredError
													.addElement(JAPMessages
															.getString("settingsRoutingClientGetOfferConnectError"));
										}
									else if (e.getErrorCode() == ClientForwardException.ERROR_VERSION_ERROR)
										{
											occuredError
													.addElement(JAPMessages
															.getString("settingsRoutingClientGetOfferVersionError"));
										}
									else
										{
											occuredError
													.addElement(JAPMessages
															.getString("settingsRoutingClientGetOfferUnknownError"));
										}
								}
						}
				};

				WorkerContentPane worker = new WorkerContentPane(offerDialog,
						JAPMessages
								.getString("settingsRoutingClientConfigDialogGetOfferLabel"),
						offerThread);

				worker.getButtonCancel().addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent event)
						{
							/*
							 * if the Cancel button is pressed, stop the connection -> the
							 * getConnectionDescriptor() method ends with an exception
							 */
							JAPModel.getInstance().getRoutingSettings().setRoutingMode(
									JAPRoutingSettings.ROUTING_MODE_DISABLED);
						}
				});
				worker.setInterruptThreadSafe(false);
				worker.updateDialog();
				offerDialog.pack();
				offerDialog.setVisible(true);
				offerDialog.dispose();

				ForwardConnectionDescriptor returnValue = null;

				if (fetchedDescriptor.size() > 0)
					{
						/*
						 * no error occured -> fetching the connection offer from the
						 * forwarder was successful
						 */
						returnValue = (ForwardConnectionDescriptor) (fetchedDescriptor
								.firstElement());
					}
				else if (occuredError.size() > 0)
					{
						/*
						 * there occured an error while fetching the connection offer from
						 * the forwarder -> show a message and go back to step 1
						 */
						JAPDialog.showErrorDialog(getRootComponent(),
								(String) (occuredError.firstElement()), LogType.NET);
					}

				return returnValue;
			}

		/**
		 * Shows the second step of the client configuration dialog.
		 * 
		 * @param a_connectionDescriptor
		 *          The connection offer from the forwarder, which is visualized.
		 * 
		 * @return The mixcascade selected by the user in this step or null, if he
		 *         pressed 'Cancel'.
		 */
		private MixCascade showConfigClientDialogStep2(
				ForwardConnectionDescriptor a_connectionDescriptor)
			{
				final JAPDialog client2Dialog = new JAPDialog(getRootComponent(),
						JAPMessages.getString("settingsRoutingClientConfigDialog2Title"));
				client2Dialog
						.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				JPanel client2Panel = new JPanel();
				client2Dialog.getContentPane().add(client2Panel);

				JLabel settingsRoutingClientConfigDialog2GuaranteedBandwidthLabel = new JLabel(
						JAPMessages
								.getString("settingsRoutingClientConfigDialog2GuaranteedBandwidthLabel")
								+ " "
								+ Integer.toString(a_connectionDescriptor
										.getGuaranteedBandwidth()));
				settingsRoutingClientConfigDialog2GuaranteedBandwidthLabel
						.setFont(getFontSetting());

				JLabel settingsRoutingClientConfigDialog2MaxBandwidthLabel = new JLabel(
						JAPMessages
								.getString("settingsRoutingClientConfigDialog2MaxBandwidthLabel")
								+ " "
								+ Integer
										.toString(a_connectionDescriptor.getMaximumBandwidth()));
				settingsRoutingClientConfigDialog2MaxBandwidthLabel
						.setFont(getFontSetting());

				JLabel settingsRoutingClientConfigDialog2DummyTrafficLabel = new JLabel();
				settingsRoutingClientConfigDialog2DummyTrafficLabel
						.setFont(getFontSetting());
				if (a_connectionDescriptor.getMinDummyTrafficInterval() != -1)
					{
						settingsRoutingClientConfigDialog2DummyTrafficLabel
								.setText(JAPMessages
										.getString("settingsRoutingClientConfigDialog2DummyTrafficLabel")
										+ " "
										+ Integer.toString(a_connectionDescriptor
												.getMinDummyTrafficInterval() / 1000));
					}
				else
					{
						settingsRoutingClientConfigDialog2DummyTrafficLabel
								.setText(JAPMessages
										.getString("settingsRoutingClientConfigDialog2DummyTrafficLabel")
										+ " "
										+ JAPMessages
												.getString("settingsRoutingClientConfigDialog2DummyTrafficLabelNoNeed"));
					}

				final JButton settingsRoutingClientConfigDialog2FinishButton = new JButton(
						JAPMessages
								.getString("settingsRoutingClientConfigDialog2FinishButton"));

				JLabel settingsRoutingClientConfigDialog2MixCascadesLabel = new JLabel(
						JAPMessages
								.getString("settingsRoutingClientConfigDialog2MixCascadesLabel"));
				settingsRoutingClientConfigDialog2MixCascadesLabel
						.setFont(getFontSetting());
				final JList supportedCascadesList = new JList(a_connectionDescriptor
						.getMixCascadeList());
				supportedCascadesList
						.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				supportedCascadesList
						.addListSelectionListener(new ListSelectionListener()
						{
							public void valueChanged(ListSelectionEvent event)
								{
									/*
									 * if there is something selected, we can enable the Finish
									 * button
									 */
									if (supportedCascadesList.getSelectedIndex() != -1)
										{
											settingsRoutingClientConfigDialog2FinishButton
													.setEnabled(true);
										}
									else
										{
											settingsRoutingClientConfigDialog2FinishButton
													.setEnabled(false);
										}
								}
						});
				JScrollPane supportedCascadesScrollPane = new JScrollPane(
						supportedCascadesList);
				supportedCascadesScrollPane.setFont(getFontSetting());

				/* this Vector contains the selected mixcascade */
				final Vector selectedMixCascade = new Vector();

				settingsRoutingClientConfigDialog2FinishButton
						.setFont(getFontSetting());
				if (supportedCascadesList.getSelectedIndex() != -1)
					{
						settingsRoutingClientConfigDialog2FinishButton.setEnabled(true);
					}
				else
					{
						settingsRoutingClientConfigDialog2FinishButton.setEnabled(false);
					}
				settingsRoutingClientConfigDialog2FinishButton
						.addActionListener(new ActionListener()
						{
							public void actionPerformed(ActionEvent event)
								{
									/*
									 * if the Finish button is pressed, we start the submit
									 * mixcascade step
									 */
									selectedMixCascade
											.addElement((MixCascade) (supportedCascadesList
													.getSelectedValue()));
									client2Dialog.dispose();
								}
						});

				JButton settingsRoutingClientConfigDialog2CancelButton = new JButton(
						JAPMessages.getString("cancelButton"));
				settingsRoutingClientConfigDialog2CancelButton
						.setFont(getFontSetting());
				settingsRoutingClientConfigDialog2CancelButton
						.addActionListener(new ActionListener()
						{
							public void actionPerformed(ActionEvent event)
								{
									/*
									 * if the Cancel button is pressed, stop routing and close the
									 * dialog
									 */
									JAPModel.getInstance().getRoutingSettings().setRoutingMode(
											JAPRoutingSettings.ROUTING_MODE_DISABLED);
									client2Dialog.dispose();
									/*
									 * let the user fetch another forwarder, maybe he gets a
									 * better one
									 */
								}
						});

				TitledBorder settingsRoutingClientConfigDialog2Border = new TitledBorder(
						JAPMessages.getString("settingsRoutingClientConfigDialog2Border"));
				settingsRoutingClientConfigDialog2Border.setTitleFont(getFontSetting());
				client2Panel.setBorder(settingsRoutingClientConfigDialog2Border);

				GridBagLayout client2PanelLayout = new GridBagLayout();
				client2Panel.setLayout(client2PanelLayout);

				GridBagConstraints client2PanelConstraints = new GridBagConstraints();
				client2PanelConstraints.anchor = GridBagConstraints.NORTHWEST;
				client2PanelConstraints.fill = GridBagConstraints.BOTH;
				client2PanelConstraints.weightx = 1.0;
				client2PanelConstraints.weighty = 0.0;
				client2PanelConstraints.gridwidth = 2;

				client2PanelConstraints.gridx = 0;
				client2PanelConstraints.gridy = 0;
				client2PanelConstraints.insets = new Insets(0, 5, 10, 5);
				client2PanelLayout.setConstraints(
						settingsRoutingClientConfigDialog2GuaranteedBandwidthLabel,
						client2PanelConstraints);
				client2Panel
						.add(settingsRoutingClientConfigDialog2GuaranteedBandwidthLabel);

				client2PanelConstraints.gridx = 0;
				client2PanelConstraints.gridy = 1;
				client2PanelLayout.setConstraints(
						settingsRoutingClientConfigDialog2MaxBandwidthLabel,
						client2PanelConstraints);
				client2Panel.add(settingsRoutingClientConfigDialog2MaxBandwidthLabel);

				client2PanelConstraints.gridx = 0;
				client2PanelConstraints.gridy = 2;
				client2PanelLayout.setConstraints(
						settingsRoutingClientConfigDialog2DummyTrafficLabel,
						client2PanelConstraints);
				client2Panel.add(settingsRoutingClientConfigDialog2DummyTrafficLabel);

				client2PanelConstraints.gridx = 0;
				client2PanelConstraints.gridy = 3;
				client2PanelConstraints.insets = new Insets(0, 5, 0, 5);
				client2PanelLayout.setConstraints(
						settingsRoutingClientConfigDialog2MixCascadesLabel,
						client2PanelConstraints);
				client2Panel.add(settingsRoutingClientConfigDialog2MixCascadesLabel);

				client2PanelConstraints.gridx = 0;
				client2PanelConstraints.gridy = 4;
				client2PanelConstraints.weighty = 1.0;
				client2PanelConstraints.insets = new Insets(0, 5, 20, 5);
				client2PanelLayout.setConstraints(supportedCascadesScrollPane,
						client2PanelConstraints);
				client2Panel.add(supportedCascadesScrollPane);

				client2PanelConstraints.gridx = 0;
				client2PanelConstraints.gridy = 5;
				client2PanelConstraints.gridwidth = 1;
				client2PanelConstraints.weighty = 0.0;
				client2PanelConstraints.insets = new Insets(0, 5, 5, 5);
				client2PanelLayout.setConstraints(
						settingsRoutingClientConfigDialog2CancelButton,
						client2PanelConstraints);
				client2Panel.add(settingsRoutingClientConfigDialog2CancelButton);

				client2PanelConstraints.gridx = 1;
				client2PanelConstraints.gridy = 5;
				client2PanelConstraints.insets = new Insets(0, 5, 5, 5);
				client2PanelLayout.setConstraints(
						settingsRoutingClientConfigDialog2FinishButton,
						client2PanelConstraints);
				client2Panel.add(settingsRoutingClientConfigDialog2FinishButton);

				client2Dialog.pack();
				client2Dialog.setVisible(true);

				MixCascade returnValue = null;

				if (selectedMixCascade.size() > 0)
					{
						/* the user has selected a MixCascade */
						returnValue = (MixCascade) (selectedMixCascade.firstElement());
					}

				return returnValue;
			}

		/**
		 * Shows the announce selected mixcascade box in the client configuration
		 * dialog.
		 * 
		 * @param a_selectedMixCascade
		 *          The mixcascade which was selected in step 2.
		 * 
		 * @return True, if announcing the cascade at the forwarder was successful
		 *         or false, if it was not.
		 */
		private boolean showConfigClientDialogAnnounceCascade(
				final MixCascade a_selectedMixCascade)
			{
				final JAPDialog announceDialog = new JAPDialog(
						getRootComponent(),
						JAPMessages
								.getString("settingsRoutingClientConfigDialogAnnounceCascadeTitle"));
				announceDialog.setResizable(false);
				announceDialog
						.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

				/* this Vector contains a message, if an error occured. */
				final Vector occuredError = new Vector();

				final Runnable announceThread = new Runnable()
				{
					public void run()
						{
							/* this is the announce mixcascade thread */
							try
								{
									JAPModel.getInstance().getRoutingSettings().selectMixCascade(
											a_selectedMixCascade);
								}
							catch (ClientForwardException e)
								{
									/* there was an error while receiving the connection offer */
									LogHolder.log(LogLevel.ERR, LogType.NET,
											"JAPConfRouting: showConfigClientDialogAnnounceCascade: "
													+ e.toString());
									if (e.getErrorCode() == ClientForwardException.ERROR_CONNECTION_ERROR)
										{
											occuredError
													.addElement(JAPMessages
															.getString("settingsRoutingClientAnnounceCascadeConnectError"));
										}
									else
										{
											occuredError
													.addElement(JAPMessages
															.getString("settingsRoutingClientAnnounceCascadeUnknownError"));
										}
								}
						}
				};

				WorkerContentPane worker = new WorkerContentPane(
						announceDialog,
						JAPMessages
								.getString("settingsRoutingClientConfigDialogAnnounceCascadeLabel"),
						announceThread);

				worker.getButtonCancel().addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent event)
						{
							/*
							 * if the Cancel button is pressed, stop the connection -> the
							 * selectMixCascade() method ends with an exception
							 */
							JAPModel.getInstance().getRoutingSettings().setRoutingMode(
									JAPRoutingSettings.ROUTING_MODE_DISABLED);
						}
				});
				worker.setInterruptThreadSafe(false);
				worker.updateDialog();
				announceDialog.pack();
				announceDialog.setVisible(true);

				boolean returnValue = false;

				if (occuredError.size() == 0)
					{
						/*
						 * no error occured -> sending the mixcascade was successful ->
						 * start the anonymous mode
						 */
						JAPController.getInstance().setCurrentMixCascade(
								a_selectedMixCascade);
						JAPController.getInstance().setAnonMode(true);
						returnValue = true;
					}
				else if (occuredError.size() > 0)
					{
						/*
						 * there occured an error while announcing the selected mixcascade
						 * at the forwarder
						 */
						JAPDialog.showErrorDialog(m_parentComponent, (String) (occuredError
								.firstElement()), LogType.NET);
					}

				return returnValue;
			}
	}
