package infoservice.performance;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import infoservice.HttpResponseStructure;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.util.XMLUtil;
import anon.util.XMLParseException;
import anon.infoservice.Database;
import anon.crypto.SignatureVerifier;

public class PerformanceRequestHandler 
{
	public HttpResponseStructure handlePerformanceTokenRequest(byte[] a_postData)
	{
		Document doc = null;
		PerformanceTokenRequest request = null;
		
		try
		{
			doc = XMLUtil.toXMLDocument(a_postData);
			request = new PerformanceTokenRequest(doc.getDocumentElement());
		}
		catch(XMLParseException ex)
		{
			LogHolder.log(LogLevel.WARNING, LogType.NET, "Error while processing PerformanceTokenRequest: " + ex.getMessage());
			
			// TODO: Return error?
			return new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		
		LogHolder.log(LogLevel.WARNING, LogType.NET, "InfoService " + request.getInfoServiceId() + " is requesting a performance token.");
	
		PerformanceToken token = new PerformanceToken();
		Database.getInstance(PerformanceToken.class).update(token);
		
		doc = XMLUtil.toSignedXMLDocument(token, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
		
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_PLAIN,
				HttpResponseStructure.HTTP_ENCODING_PLAIN, XMLUtil.toString(doc));
		
		LogHolder.log(LogLevel.WARNING, LogType.NET, "Token " + token.getId() + " issued.");
		
		return httpResponse;
	}
	
	public HttpResponseStructure handlePerformanceRequest(byte[] a_postData)
	{
		Document doc = null;
		PerformanceRequest request = null;
		
		try
		{
			doc = XMLUtil.toXMLDocument(a_postData);
			request = new PerformanceRequest(doc.getDocumentElement());
		}
		catch(XMLParseException ex)
		{
			LogHolder.log(LogLevel.WARNING, LogType.NET, "Error while processing PerformanceTokenRequest: " + ex.getMessage());
			
			// TODO: Return error?
			return new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		
		LogHolder.log(LogLevel.WARNING, LogType.NET, "Token " + request.getTokenId() + " is requesting " + request.getDataSize() + " bytes of random data.");
		
		byte[] data = new byte[request.getDataSize()];
		new java.util.Random().nextBytes(data);
		
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_PLAIN,
				HttpResponseStructure.HTTP_ENCODING_PLAIN, data);

		LogHolder.log(LogLevel.WARNING, LogType.NET, data.length + " bytes sent. Removed token.");
		
		Database.getInstance(PerformanceToken.class).remove(request.getTokenId());
		
		return httpResponse;
	}
}
