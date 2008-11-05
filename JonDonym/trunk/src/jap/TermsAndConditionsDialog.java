package jap;

import java.awt.Component;
import javax.swing.JEditorPane;

import jap.pay.wizardnew.TermsAndConditionsPane;
import gui.dialog.JAPDialog;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.TermsAndConditionsFramework;
import anon.infoservice.TermsAndConditions;

public class TermsAndConditionsDialog extends JAPDialog
{
	TermsAndConditionsPane m_panel;
	boolean m_foundTC;
	
	public TermsAndConditionsDialog(Component a_parent, String a_ski)
	{
		super(a_parent, "T&C");
		
		m_foundTC = false;
		
		setResizable(false);
		
		m_panel = new TermsAndConditionsPane(this, false);
		
		TermsAndConditions tc = TermsAndConditions.getById("de_" + a_ski);
		
		if(tc == null)
		{
			return;
		}
		TermsAndConditionsFramework fr = TermsAndConditionsFramework.getById(tc.getReferenceId());
		
		if(fr == null)
		{
			return;
		}
		
		fr.importData(tc);
		
		m_panel.setText(fr.transform());
		
		m_panel.updateDialog();
		pack();
		
		m_foundTC = true;
	}
	
	public boolean isTermsAccepted()
	{
		return m_panel.isTermsAccepted();
	}
	
	public boolean hasFoundTC()
	{
		return m_foundTC;
	}
}
