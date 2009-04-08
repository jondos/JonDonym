package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import gui.dialog.JAPDialog;
import gui.dialog.TermsAndConditionsPane;
import anon.client.ITermsAndConditionsContainer.TermsAndConditonsDialogReturnValues;
import anon.infoservice.TermsAndConditionsFramework;
import anon.infoservice.TermsAndConditions;
import anon.infoservice.TermsAndConditionsTranslation;
import anon.util.JAPMessages;

public class TermsAndConditionsDialog extends JAPDialog
{
	TermsAndConditionsPane m_panel;
	TermsAndConditonsDialogReturnValues m_ret;
	
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
		
		// try to find the TnC framework
		/*TermsAndConditionsFramework fr = 
			TermsAndConditionsFramework.getById(tc.getReferenceId(), a_bUpdateFromInfoService);
		
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
	
	public static void previewTranslation(Frame owner,
										TermsAndConditionsTranslation tcTranslation, JButton exportButton)
	{
		String htmlText = null;
		try 
		{
			TermsAndConditionsFramework displayTemplate = 
				TermsAndConditionsFramework.getById(tcTranslation.getTemplateReferenceId(), false);
			displayTemplate.importData(tcTranslation);
			htmlText = displayTemplate.transform();
		}
		catch(Exception e)
		{
			htmlText = "<html><head><title><Preview error></title></head><body><head><h1>Error creating tc preview</h1>"+
				"<h2>Reason:</h2><p>"+e+"</p>";
			e.printStackTrace();
		}
		JapHtmlPane htmlPane = new JapHtmlPane(htmlText);
		htmlPane.setPreferredSize(new Dimension(800,600));
		final JDialog displayDialog = new JDialog(owner, "Translation preview ["+tcTranslation+"]");
		
		displayDialog.setLayout(new BorderLayout());
		JPanel buttonPanel = new JPanel();
		
		if(exportButton != null)
		{
			buttonPanel.add(exportButton);
		}
		JButton closeButton = new JButton(JAPMessages.getString("bttnClose"));
		closeButton.addActionListener(
				new ActionListener()
				{
					public void actionPerformed(ActionEvent e) 
					{
						displayDialog.dispose();
					}
				});
		buttonPanel.add(closeButton);
		displayDialog.add(buttonPanel, BorderLayout.NORTH);
		displayDialog.add(htmlPane, BorderLayout.SOUTH);
		displayDialog.add(htmlPane);
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
}
