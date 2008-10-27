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
		
		
		TermsAndConditionsPane contentPane =
			new TermsAndConditionsPane(this, false);
		
		java.util.Hashtable tcop = InfoServiceHolder.getInstance().getTermsAndConditions();
		TermsAndConditions op = 
			(TermsAndConditions) tcop.get("de_ED:97:C9:1B:EC:5D:ED:6A:9B:67:32:C8:3E:51:9A:38:A4:89:12:B6");
		
		TermsAndConditionsFramework fr = InfoServiceHolder.getInstance().getTCFramework(op.getReferenceId());
		
		fr.importData(op);
		
		contentPane.setText(fr.transform());
		
		contentPane.updateDialog();
		pack();
	}
}
