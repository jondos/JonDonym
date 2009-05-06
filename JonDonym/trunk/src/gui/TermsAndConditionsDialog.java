package gui;

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
import anon.client.ITermsAndConditionsContainer.TermsAndConditonsDialogReturnValues;
import anon.terms.TermsAndConditions;
import anon.terms.TermsAndConditionsTranslation;
import anon.terms.template.TermsAndConditionsTemplate;
import anon.util.JAPMessages;

public class TermsAndConditionsDialog extends JAPDialog
{
	TermsAndConditionsPane m_panel;
	TermsAndConditonsDialogReturnValues m_ret;
	
	public final static String HTML_EXPORT_ENCODING = "ISO-8859-1";
	
	/*public TermsAndConditionsDialog(Component a_parent, ServiceOperator a_op, 
			boolean a_bUpdateFromInfoService)*/
	public TermsAndConditionsDialog(Component a_parent, TermsAndConditions tc)
	{
		this(a_parent, tc, JAPMessages.getLocale().getLanguage());
	}
	public TermsAndConditionsDialog(Component a_parent, TermsAndConditions tc, String langCode) 
	{
		super(a_parent, "T&C");
		
		m_ret = new TermsAndConditonsDialogReturnValues();
		m_ret.setError(true);
		
		setResizable(false);
		
		// try to find the TnC
		//TermsAndConditions tc = TermsAndConditions.getById(a_op.getId());
		if(tc == null)
		{
			return;
		}
	
		String htmlText = tc.getHTMLText(langCode);
		if(htmlText == null)
		{
			return;
		}
		//TermsAndConditions.getTranslationById(a_op.getId(), JAPMessages.getLocale());
		
		// try to find the TnC template
		/*TermsAndConditionsTemplate fr = 
			TermsAndConditionsTemplate.getById(tc.getReferenceId(), a_bUpdateFromInfoService);
		
		if(fr == null)
		{
			return;
		}
		
		fr.importData(tc);*/
		
		m_panel = new TermsAndConditionsPane(this, false, new TermsAndConditionsPane.TermsAndConditionsMessages());
		m_panel.setText(htmlText);
		
		m_panel.updateDialog();
		pack();
		
		m_ret.setError(false);
	}
	
	public static void previewTranslation(Component parent, TermsAndConditionsTranslation tcTranslation)
	{
		StringBuffer htmlTextBuffer = new StringBuffer();
		try 
		{
			TermsAndConditionsTemplate displayTemplate = 
				TermsAndConditionsTemplate.getById(tcTranslation.getTemplateReferenceId(), false);
			//displayTemplate.importData(tcTranslation);
			htmlTextBuffer.append(displayTemplate.transform(tcTranslation));
		}
		catch(Exception e)
		{
			htmlTextBuffer.append(
					"<html><head><title><Preview error></title></head><body><head><h1>Error creating tc preview</h1><h2>Reason:</h2><p>");
			htmlTextBuffer.append(e);
			htmlTextBuffer.append("</p>");
		}
		final String htmlText = htmlTextBuffer.toString();
		JapHtmlPane htmlPane = new JapHtmlPane(htmlText);
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
	
	public boolean hasError()
	{
		return m_ret.hasError();
	}
	
	public TermsAndConditonsDialogReturnValues getReturnValues()
	{
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
}
