package anon.terms;

import java.util.Vector;

public interface TermsAndConditionConfirmation 
{
	/**
	 * performs an action to confirm the terms and conditions
	 * @return if the terms and conditions were confirmed by this action, false
	 * otherwise
	 */
	public boolean confirmTermsAndConditions(Vector operators, Vector terms);
	
	public static final class AlwaysAccept implements TermsAndConditionConfirmation
	{
		public boolean confirmTermsAndConditions(Vector operators, Vector terms) 
		{
			return true;
		}
	}
}
