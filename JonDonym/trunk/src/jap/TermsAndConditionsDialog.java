package jap;

import java.awt.Component;
import java.text.DateFormat;
import java.util.Locale;

import javax.swing.JEditorPane;

import jap.pay.wizardnew.TermsAndConditionsPane;
import gui.dialog.JAPDialog;
import gui.JAPMessages;
import anon.client.ITermsAndConditionsContainer.TermsAndConditonsDialogReturnValues;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ServiceOperator;
import anon.infoservice.TermsAndConditionsFramework;
import anon.infoservice.TermsAndConditions;

public class TermsAndConditionsDialog extends JAPDialog
{
	TermsAndConditionsPane m_panel;
	TermsAndConditonsDialogReturnValues m_ret;
	
	/*public TermsAndConditionsDialog(Component a_parent, ServiceOperator a_op, 
			boolean a_bUpdateFromInfoService)*/
	public TermsAndConditionsDialog(Component a_parent, TermsAndConditions tc) 
	{
		super(a_parent, "T&C");
		
		m_ret = new TermsAndConditonsDialogReturnValues();
		m_ret.setError(true);
		
		setResizable(false);
		
		m_panel = new TermsAndConditionsPane(this, false);
		
		// try to find the TnC
		//TermsAndConditions tc = TermsAndConditions.getById(a_op.getId());
		if(tc == null)
		{
			return;
		}
	
		String htmlText = tc.getHTMLText(JAPMessages.getLocale());
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
		
		m_panel.setText(htmlText);
		
		m_panel.updateDialog();
		pack();
		
		m_ret.setError(false);
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
