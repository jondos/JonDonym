package jap;


import java.awt.Component;
import javax.swing.JEditorPane;

import jap.pay.wizardnew.TermsAndConditionsPane;
import gui.dialog.JAPDialog;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.TermsAndConditionsFramework;

public class TermsAndConditionsDialog extends JAPDialog
{
	JEditorPane m_panel;
	
	public TermsAndConditionsDialog(Component a_parent)
	{
		super(a_parent, "T&C");
		
		setResizable(false);
		
		java.util.Hashtable tc = InfoServiceHolder.getInstance().getTCFrameworks();
		TermsAndConditionsFramework fr =
			(TermsAndConditionsFramework) tc.get("common_law_en_20081007");
		
		TermsAndConditionsPane contentPane =
			new TermsAndConditionsPane(this, false);
		
		contentPane.setText(fr.transform());
		
		contentPane.updateDialog();
		pack();
	}
}
