package anon.infoservice;

/**
 * This interface specifies all objects that contain information about the service 
 * context. A Service context describes the context in which a certain service object is provided.
 * Example: A mixcascade is provided by the JonDonym service so the MixCascade-Object implementing 
 * this interface returns the context CONTEXT_JONDONYM.
 * 
 * This interface allows Anon-Services to be offered to different groups of customers.
 * The default context for service objects is CONTEXT_JONDONYM context
 * @author Simon Pecher
 *
 */
public interface IServiceContextContainer 
{
	
	/* default service contexts */
	
	/** context for all JonDonym services. this is the default context for all services*/ 
	public static final String CONTEXT_JONDONYM = "de.jondos.jondonym";
	
	public final String XML_ATTR_CONTEXT = "context";
	
	public String getContext();
	
}
