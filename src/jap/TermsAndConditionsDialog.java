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
	
	public TermsAndConditionsDialog(Component a_parent, String a_ski)
	{
		super(a_parent, "T&C");
		
		setResizable(false);
		
		
		TermsAndConditionsPane contentPane =
			new TermsAndConditionsPane(this, false);
		
		/*java.util.Hashtable tcop = InfoServiceHolder.getInstance().getTermsAndConditions();
		TermsAndConditions op = 
			(TermsAndConditions) tcop.get("de_ED:1E:83:EF:A1:D5:84:58:AC:7D:AE:D8:E9:32:29:38:90:9F:15:81");*/
		
		TermsAndConditions tc = TermsAndConditions.getById("de_" + a_ski);
		
		java.util.Hashtable test = InfoServiceHolder.getInstance().getTermsAndConditionsSerials();
		
		if(tc == null)
		{
			return;
		}
		TermsAndConditionsFramework fr = InfoServiceHolder.getInstance().getTCFramework(tc.getReferenceId());
		
		if(fr == null)
		{
			return;
		}
		
		fr.importData(tc);
		
		contentPane.setText(fr.transform());
		
		contentPane.updateDialog();
		pack();
	}
}
