package anon.client;

import anon.infoservice.ServiceOperator;

public interface ITermsAndConditionsContainer 
{
	public TermsAndConditonsDialogReturnValues showTermsAndConditionsDialog(ServiceOperator a_op);
	
	public boolean hasAcceptedTermsAndConditions(ServiceOperator a_op);
	
	public void revokeTermsAndConditions(ServiceOperator a_op);
	public void acceptTermsAndConditions(ServiceOperator a_op);
	
	public TermsAndConditionsResponseHandler getTermsAndConditionsRepsonseHandler();
	
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
