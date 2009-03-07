package anon.infoservice;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.XMLUtil;

/**
 * TODO Maybe this should be turned into an IXMLEncodable...
 *
 */
public class OperatorAddress 
{
	
	public final static String STREET_NODE = "Street";
	public final static String POST_NODE = "PostalCode";
	public final static String CITY_NODE = "City";
	public final static String VAT_NODE = "VAT";
	public final static String FAX_NODE = "Fax";
	public final static String VENUE_NODE = "Venue";
	
	private String street;
	private String post;
	private String city;
	private String vat;
	private String fax;
	private String venue;
	
	public String getStreet() 
	{
		return street;
	}
	
	public void setStreet(String street) 
	{
		this.street = street;
	}
	
	public String getPost() 
	{
		return post;
	}
	
	public void setPost(String post) 
	{
		this.post = post;
	}
	
	public String getCity() 
	{
		return city;
	}
	
	public void setCity(String city) 
	{
		this.city = city;
	}
	
	public String getVat() 
	{
		return vat;
	}
	
	public void setVat(String vat) 
	{
		this.vat = vat;
	}
	
	public String getFax() 
	{
		return fax;
	}
	
	public void setFax(String fax) 
	{
		this.fax = fax;
	}
	
	public String getVenue() 
	{
		return venue;
	}
	
	public void setVenue(String venue) 
	{
		this.venue = venue;
	}
	
	public Enumeration getAddressAsNodeList(Document owner)
	{
		Vector v = new Vector();
		
		Field[] allFields = this.getClass().getDeclaredFields();
		for (int i = 0; i < allFields.length; i++) 
		{
			if(!Modifier.isFinal(allFields[i].getModifiers()))
			{
				Field f;
				try 
				{
					f = this.getClass().getDeclaredField(allFields[i].getName().toUpperCase()+"_NODE");
					Element element = owner.createElement(f.get(this).toString());
					XMLUtil.setValue(element, allFields[i].get(this).toString());
					v.addElement(element);
				} 
				catch (SecurityException e) {
					e.printStackTrace();
				} catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (DOMException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				
			}
		}
		return v.elements();
	}
	
	
	Element getPostElement(Document owner)
	{
		Element e = owner.createElement(POST_NODE);
		XMLUtil.setValue(e, post);
		return e;
	}
}
