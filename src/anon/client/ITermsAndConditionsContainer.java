package anon.client;

import java.util.Locale;

import anon.infoservice.ServiceOperator;
import anon.terms.TermsAndConditions;
import anon.terms.TermsAndConditionsResponseHandler;

public interface ITermsAndConditionsContainer 
{
	//public TermsAndConditonsDialogReturnValues showTermsAndConditionsDialog(ServiceOperator a_op);
	public TermsAndConditonsDialogReturnValues showTermsAndConditionsDialog(TermsAndConditions tc);
	
	public boolean hasAcceptedTermsAndConditions(ServiceOperator a_op);
	
	public void revokeTermsAndConditions(ServiceOperator a_op);
	public void acceptTermsAndConditions(ServiceOperator a_op);
	
	public TermsAndConditionsResponseHandler getTermsAndConditionsResponseHandler();
	public Locale getDisplayLanguageLocale();
	
	public class TermsAndConditonsDialogReturnValues
	{
		private boolean m_bError = true;
		private boolean m_bAccepted = false;
		
		public boolean hasError()
		{
			return m_bError;
		}
		
		public boolean hasAccepted()
		{
			return m_bAccepted;
		}
		
		public void setError(boolean a_bError)
		{
			m_bError = a_bError;
		}
		
		public void setAccepted(boolean a_bAccepted)
		{
			m_bAccepted = a_bAccepted;
		}
	}
}
