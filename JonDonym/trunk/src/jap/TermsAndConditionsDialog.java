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
	JEditorPane m_panel;
	
	public TermsAndConditionsDialog(Component a_parent)
	{
		super(a_parent, "T&C");
		
		setResizable(false);
		
		java.util.Hashtable tc = InfoServiceHolder.getInstance().getTCFrameworks();
		TermsAndConditionsFramework fr =
			(TermsAndConditionsFramework) tc.get("CommonLaw_en_20081007");
		
		TermsAndConditionsPane contentPane =
			new TermsAndConditionsPane(this, false);
		
		java.util.Hashtable tcop = InfoServiceHolder.getInstance().getTCOperatorData();
		TermsAndConditions op = 
			(TermsAndConditions) tcop.get("en_15A1D8B31B5360225EB26B98D10F37C269E3DEED");
		
		fr.importData(op);
		
		contentPane.setText(fr.transform());
		
		contentPane.updateDialog();
		pack();
	}
}
