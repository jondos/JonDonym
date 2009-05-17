package gui;

import gui.dialog.DialogContentPane;
import gui.dialog.JAPDialog;
import gui.dialog.TermsAndConditionsPane;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import logging.LogType;
import anon.terms.TermsAndConditions;
import anon.terms.TermsAndConditionsTranslation;
import anon.util.JAPMessages;

public class TermsAndConditionsDialog extends JAPDialog
{
	TermsAndConditionsPane m_panel;
	TermsAndConditonsDialogReturnValues m_ret;
	
	boolean acceptInitialValue = false;
	
	public final static String HTML_EXPORT_ENCODING = "ISO-8859-1";
	public final static String MSG_DIALOG_TITLE = TermsAndConditionsDialog.class.getName()+"_dialogTitle";
	
	public TermsAndConditionsDialog(Component a_parent, boolean accepted, TermsAndConditions tc)
	{
		this(a_parent, accepted, tc, JAPMessages.getLocale().getLanguage());
	}
	public TermsAndConditionsDialog(Component a_parent, boolean accepted, TermsAndConditions tc, String langCode) 
	{
		super(a_parent, JAPMessages.getString(MSG_DIALOG_TITLE, tc.getOperator().getOrganization()));
		
		m_ret = new TermsAndConditonsDialogReturnValues();
		setResizable(false);

		m_panel = new TermsAndConditionsPane(this, accepted, new TermsAndConditionsPane.TermsAndConditionsMessages());
		m_panel.setText(tc.getHTMLText(langCode));
		m_panel.updateDialog();
		pack();
	}
	
	public static void previewTranslation(Component parent, TermsAndConditionsTranslation tcTranslation)
	{
		final String htmlText = TermsAndConditions.getHTMLText(tcTranslation);
		JapHtmlPane htmlPane = new JapHtmlPane(htmlText, new UpperLeftStartViewport());
		htmlPane.setPreferredSize(new Dimension(800,600));
		final JAPDialog displayDialog = new JAPDialog(parent, "Translation preview ["+tcTranslation+"]");
		Container contentPane = displayDialog.getContentPane();
		contentPane.setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		
		final JButton exportButton = new JButton(JAPMessages.getString("bttnSaveAs"));
		final JButton closeButton = new JButton(JAPMessages.getString("bttnClose"));
		String organizationName = tcTranslation.getOperator() != null ? 
				(tcTranslation.getOperator().getOrganization() != null ? 
						tcTranslation.getOperator().getOrganization() : "???") : "???";
		final String suggestedFileName = "Terms_"+organizationName+"_"+tcTranslation.getLocale()+".html";
		ActionListener actionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e) 
			{
				if(e.getSource() == exportButton)
				{
					actionExportHTMLToFile(displayDialog.getContentPane(), 
							htmlText, 
							suggestedFileName);
				}
				else if(e.getSource() == closeButton)
				{
					displayDialog.dispose();
				}
			}
		};
		exportButton.addActionListener(actionListener);
		closeButton.addActionListener(actionListener);
		
		buttonPanel.add(exportButton);		
		buttonPanel.add(closeButton);
		
		contentPane.add(buttonPanel, BorderLayout.NORTH);
		contentPane.add(htmlPane, BorderLayout.SOUTH);
		contentPane.add(htmlPane);
		displayDialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		displayDialog.pack();
		displayDialog.setVisible(true);
	}
	
	public TermsAndConditonsDialogReturnValues getReturnValues()
	{
		m_ret.setCancelled(m_panel.getButtonValue() != DialogContentPane.RETURN_VALUE_OK);
		m_ret.setAccepted(m_panel.isTermsAccepted());
		return m_ret;
	}
	
	private static void actionExportHTMLToFile(Component parent, String htmlOutput, String suggestedFileName)
	{
		//JFileChooser fc = (lastOpened != null) ? new JFileChooser(lastOpened) : new JFileChooser();
		JFileChooser fc = new JFileChooser();
		File suggestedFile = new File(fc.getCurrentDirectory()+File.separator+suggestedFileName);
		fc.setSelectedFile(suggestedFile);
		int clicked = fc.showSaveDialog(parent);
		switch ( clicked )
		{
			case JFileChooser.APPROVE_OPTION:
			{
				File selectedFile = fc.getSelectedFile();
				boolean confirmed = true;
				if(selectedFile.exists())
				{
					confirmed = 
						JAPDialog.showConfirmDialog(parent, 
							//JAPMessages.getString(MSG_FILE_EXISTS, selectedFile.getName()),
							"File already "+selectedFile.getName()+" already exists. Do you want to replace it?",
							JAPDialog.OPTION_TYPE_YES_NO, 
							JAPDialog.MESSAGE_TYPE_QUESTION) == JAPDialog.RETURN_VALUE_YES;
				}
				if(confirmed)
				{
					try 
					{
						//Make sure the exported HTML is ISO-Latin-1 encoded
						OutputStreamWriter exportWriter = 
							new OutputStreamWriter(new FileOutputStream(selectedFile), HTML_EXPORT_ENCODING);
						
						exportWriter.write(htmlOutput);
						exportWriter.flush();
						exportWriter.close();
					} 
					catch (IOException e) 
					{
						JAPDialog.showErrorDialog(parent, 
								//JAPMessages.getString(MSG_SAVE_FILE_ERROR, selectedFile.getName()), 
								"Could not export to "+selectedFile.getName(),
								LogType.MISC, e);
					} 
				}
				break;
			}
			case JFileChooser.CANCEL_OPTION:
			{
				break;
			}
			case JFileChooser.ERROR_OPTION:
			{
				break;
			}
		}
	}
	
	public class TermsAndConditonsDialogReturnValues
	{
		private boolean cancelled = false;
		private boolean accepted = false;
		
		public boolean isCancelled() 
		{
			return cancelled;
		}

		public void setCancelled(boolean cancelled) 
		{
			this.cancelled = cancelled;
		}

		public boolean isAccepted()
		{
			return accepted;
		}
		
		public void setAccepted(boolean accepted)
		{
			this.accepted = accepted;
		}
	}
}
