package anon.client;

public class IllegalTCRequestPostConditionException extends Exception
{

	private StringBuffer errorMessages= new StringBuffer();
	private int errorMessageNrs = 0;
	
	public IllegalTCRequestPostConditionException() 
	{
		super();
	}
	
	public void addErrorMessage(String errorMessage)
	{
		errorMessages.append("\n");
		errorMessages.append(++errorMessageNrs);
		errorMessages.append(". ");
		errorMessages.append(errorMessage);
	}
	
	public boolean hasErrorMessages()
	{
		return errorMessageNrs > 0;
	}
	
	public String getMessage()
	{
		return hasErrorMessages() ? errorMessages.toString() : null;
	}
}
