package anon.infoservice;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * TODO Maybe this should be turned into an IXMLEncodable...
 *
 */
public class OperatorAddress 
{
	
	public final static String NODE_NAME_STREET = "Street";
	public final static String NODE_NAME_POSTALCODE = "PostalCode";
	public final static String NODE_NAME_CITY = "City";
	public final static String NODE_NAME_VAT = "Vat";
	public final static String NODE_NAME_FAX = "Fax";
	public final static String NODE_NAME_VENUE = "Venue";
	
	public final static String PROPERTY_NAME_STREET = "street";
	public final static String PROPERTY_NAME_POSTALCODE = "postalCode";
	public final static String PROPERTY_NAME_CITY = "city";
	public final static String PROPERTY_NAME_VAT = "vat";
	public final static String PROPERTY_NAME_FAX = "fax";
	public final static String PROPERTY_NAME_VENUE = "venue";
	
	private String street;
	private String postalCode;
	private String city;
	private String vat;
	private String fax;
	private String venue;
	
	private static Hashtable propertyDescriptors = new Hashtable();
	
	static 
	{
		try 
		{
			BeanInfo info = Introspector.getBeanInfo(OperatorAddress.class);
			PropertyDescriptor[] pds = info.getPropertyDescriptors();
			for (int i = 0; i < pds.length; i++)
			{
				propertyDescriptors.put(pds[i].getName(), pds[i]);
			}
		} 
		catch (IntrospectionException e) 
		{
		} 
	}
	
	public OperatorAddress()
	{
		
	}
	
	public OperatorAddress(Element xmlRoot) throws XMLParseException
	{
		//the name of the root element does not matter.
		NodeList nl = xmlRoot.getChildNodes();
		Element currElement = null;
		Field currField = null;
		for (int i = 0; i < nl.getLength(); i++) 
		{
			if(nl.item(i).getNodeType() == Node.ELEMENT_NODE)
			{
				currElement = (Element) nl.item(i);
				try 
				{
					String name = currElement.getTagName();
					name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
					currField = this.getClass().getDeclaredField(name);
					currField.set(this, XMLUtil.parseValue(currElement, (String)null));
				} catch (SecurityException e) {
				} catch (NoSuchFieldException e) {
				} catch (IllegalArgumentException e) {
				} catch (DOMException e) {
					throw new XMLParseException(e.getMessage());
				} catch (IllegalAccessException e) {
				}
			}
		}
	}
	
	public String getStreet() 
	{
		return street;
	}
	
	public void setStreet(String street) 
	{
		this.street = street;
	}
	
	public String getPostalCode() 
	{
		return postalCode;
	}
	
	public void setPostalCode(String postalCode) 
	{
		this.postalCode = postalCode;
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
			if(!Modifier.isFinal(allFields[i].getModifiers()) && 
				!Modifier.isStatic(allFields[i].getModifiers()))
			{
				Field f;
				Object value;
				try 
				{
					value = allFields[i].get(this);
					
					if( (value != null) && !value.toString().equals("") )
					{
						f = this.getClass().getDeclaredField("NODE_NAME_"+allFields[i].getName().toUpperCase());
						Element element = owner.createElement(f.get(this).toString());
						XMLUtil.setValue(element, value.toString());
						v.addElement(element);
					}
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
	
	public static PropertyDescriptor getDescriptor(String propertyName)
	{
		return (PropertyDescriptor) propertyDescriptors.get(propertyName);
	}
	
	/*Element getPostElement(Document owner)
	{
		Element e = owner.createElement(NODE_NAME_POST);
		XMLUtil.setValue(e, post);
		return e;
	}*/
	
	
	
	public static void main(String[] args)
	{
		try 
		{
			BeanInfo info = Introspector.getBeanInfo(TermsAndConditionsTranslation.class);
			PropertyDescriptor[] pds = info.getPropertyDescriptors();
		
			for (int i = 0; i < pds.length; i++)
			{
				System.out.println("Property "+pds[i].getName()+" has writeMethod: "+pds[i].getWriteMethod());
			}
		} 
		catch (IntrospectionException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*try 
		{
			BeanInfo info = Introspector.getBeanInfo(OperatorAddress.class);
			PropertyDescriptor[] pds = info.getPropertyDescriptors();
			EventSetDescriptor[] esds = info.getEventSetDescriptors();
			for (int i = 0; i < pds.length; i++)
			{
				System.out.println("Property "+pds[i].getName()+" has writeMethod: "+pds[i].getWriteMethod());
				if(pds[i].createPropertyEditor(null) == null)
				{
					System.out.println("no property editor.");
				}
			}
			if(esds != null)
			{
				for (int i = 0; i < esds.length; i++) {
					System.out.println("EventSetDescriptor: "+esds[i].getName());
				}
			}
			//PropertyEditorSupport pes = new PropertyEditorSupport();
		} 
		catch (IntrospectionException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
}
