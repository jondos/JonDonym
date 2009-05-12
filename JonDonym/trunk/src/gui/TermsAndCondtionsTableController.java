package gui;

import anon.infoservice.ServiceOperator;

public interface TermsAndCondtionsTableController 
{
	public boolean handleOperatorAction(ServiceOperator operator, boolean accepted);
	public void handleSelectLineAction(ServiceOperator operator);
	//public void handleAcceptAction(TermsAndConditions terms, boolean accept);
}
