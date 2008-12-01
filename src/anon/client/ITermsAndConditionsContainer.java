package anon.client;

import anon.infoservice.ServiceOperator;

public interface ITermsAndConditionsContainer 
{
	public void showTermsAndConditionsDialog(ServiceOperator op);
	
	public boolean hasAcceptedTermsAndConditions(ServiceOperator op);
}
