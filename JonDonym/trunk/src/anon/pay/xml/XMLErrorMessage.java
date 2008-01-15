package anon.pay.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import java.lang.reflect.Constructor;
import org.w3c.dom.Node;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class encapsulates an error or success message.
 * In order to be indipendent from the HTTP protocol on the higher layer,
 * this is now used instead of http errorcodes.
 *
 * @author Bastian Voigt
 */
public class XMLErrorMessage extends Exception implements IXMLEncodable
{
	public static final int ERR_OK = 0;
	public static final int ERR_INTERNAL_SERVER_ERROR = 1;
	public static final int ERR_WRONG_FORMAT = 2;
	public static final int ERR_WRONG_DATA = 3;
	public static final int ERR_KEY_NOT_FOUND = 4;
	public static final int ERR_BAD_SIGNATURE = 5;
	public static final int ERR_BAD_REQUEST = 6;
	public static final int ERR_NO_ACCOUNTCERT = 7;
	public static final int ERR_NO_BALANCE = 8;
	public static final int ERR_NO_CONFIRMATION = 9;
	public static final int ERR_ACCOUNT_EMPTY = 10;
	public static final int ERR_CASCADE_LENGTH = 11;
	public static final int ERR_DATABASE_ERROR = 12;
	public static final int ERR_INSUFFICIENT_BALANCE = 13;
	public static final int ERR_NO_FLATRATE_OFFERED = 14;
	public static final int ERR_INVALID_CODE = 15;
	public static final int ERR_OUTDATED_CC = 16;
	public static final int ERR_INVALID_PRICE_CERTS = 17;
	public static final int ERR_MULTIPLE_LOGIN = 18;
	public static final int ERR_NO_RECORD_FOUND = 19;
	public static final int ERR_SUCCESS_BUT_WITH_ERRORS = 20;
	public static final int ERR_BLOCKED = 21;


	private int m_iErrorCode;
	private String m_strErrMsg;
	private IXMLEncodable m_oMessageObject;

	/** default error descriptions */
	/* length of the array is checked, you need to have a number of strings here that
	   matches the number of constants defined for error codes !! */
	private static final String[] m_errStrings =
		{
		"Success",
		"Internal Server Error",
		"Wrong format",
		"Wrong Data",
		"Key not found",
		"Bad Signature",
		"Bad request",
		"No account certificate",
		"No balance",
		"No cost confirmation",
		"Account is empty",
		"Cascade too long",
		"Database error",
		"Insufficient balance",
		"No flatrate offered",
		"Invalid code",
		"outdated CC",
		"Invalid price certificates",
		"multiple login is not allowed",
		"no record found",
		"operation succeeded, but there were errors",
		"this account is blocked" //21
	};

	private static final String[] m_messageObjectTypes =
	{
		"none",
		"none",
		"none",
		"none",
		"none",
		"none",
		"none",
		"none",
		"none",
		"none",
		"XMLGenericText", //Account_empty
		"none",
		"none",
		"none",
		"none",
		"none",
		"XMLEasyCC", //outdated_cc
		"none",
		"none",
		"none",
		"none",
		"none"
	};

	public static final String XML_ELEMENT_NAME = "ErrorMessage";

	/**
	 * Parses an XMLErrorMessage object from DOM Document
	 *
	 * @param document Document
	 */
	public XMLErrorMessage(Document doc) throws Exception
	{
		Element elemRoot = doc.getDocumentElement();
		setValues(elemRoot);
	}

	/**
	 * XMLErrorMessage
	 *
	 * @param element Element
	 */
	public XMLErrorMessage(Element element) throws Exception
	{
		setValues(element);
	}


	/**
	 * Creates an errorMessage object. The errorcode should be one of the
	 * above ERR_* constants.
	 * @param errorCode int one of the above constants
	 * @param message String a human-readable description of the error
	 */
	public XMLErrorMessage(int errorCode, String message)
	{
		m_iErrorCode = errorCode;
		m_strErrMsg = message;
	}

	public XMLErrorMessage(int errorCode, String message, IXMLEncodable messageObject)
	{
		m_iErrorCode = errorCode;
		m_strErrMsg = message;
		m_oMessageObject = messageObject;
	}

	/**
	 * Uses a default description String
	 * @param errorCode int
	 */
	public XMLErrorMessage(int errorCode)
	{
		m_iErrorCode = errorCode;
		if (m_iErrorCode < 0 || m_iErrorCode > m_errStrings.length)
		{
			m_strErrMsg = "Unknown Message"; //says "message", not "error", since it doesn't have to be an error
		}
		else
		{
			m_strErrMsg = m_errStrings[errorCode];
		}
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(XML_ELEMENT_NAME);
		elemRoot.setAttribute("code", Integer.toString(m_iErrorCode));
		XMLUtil.setValue(elemRoot, m_strErrMsg);
		if (m_oMessageObject != null)
		{
			Element elemObjectRoot = a_doc.createElement("MessageObject");
			Element elemObject = m_oMessageObject.toXmlElement(a_doc);
			elemObjectRoot.appendChild(elemObject);
			elemRoot.appendChild(elemObjectRoot);
		}
		return elemRoot;
	}

	public String getErrorDescription()
	{
		return m_strErrMsg;
	}

	public int getErrorCode()
	{
		return m_iErrorCode;
	}

	public String getMessage()
	{
		return m_strErrMsg;
	}

	/**
	 * getMessageObject: object corresponding to the message, might be null
	 *
	 * @return IXMLEncodable: exact type will depend on the message code, to be looked up via getMessageObjectType
	 */
	public IXMLEncodable getMessageObject()
	{
		return m_oMessageObject;
	}

	public Class getMessageObjectType()
	{
		String objectType = m_messageObjectTypes[m_iErrorCode];
		try
		{
			return Class.forName(objectType);
		} catch (ClassNotFoundException e)
		{
			return null;
		}
	}

	public void setMessageObject(IXMLEncodable a_messageObject)
	{
		m_oMessageObject = a_messageObject;
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (! (elemRoot.getTagName().equals(XML_ELEMENT_NAME)))
		{
			throw new Exception("Format error: Root element wrong tagname");
		}
		m_iErrorCode = Integer.parseInt(elemRoot.getAttribute("code"));
		m_strErrMsg = XMLUtil.parseValue(elemRoot, "");

		//type and existence of a messageObject depend on error code
		String objectType = m_messageObjectTypes[m_iErrorCode];
		/*
		if (! objectType.equals("none") )
		{
			try
			{
				//get the class's element name
				Class messageObjectClass = Class.forName(getClass().getPackage().getName() + "." + objectType);

				String messageObjectElementName = XMLUtil.getXmlElementName(messageObjectClass);
				//extract xml string
				Node objectRoot = XMLUtil.getFirstChildByName(elemRoot, "MessageObject");
				String objectXml = XMLUtil.toString(XMLUtil.getFirstChildByName(objectRoot,
					messageObjectElementName));
				//build object
				Class[] constructorArgTypes =
					{
					String.class};
				Constructor xmlstringConstructor = messageObjectClass.getConstructor(constructorArgTypes);
				Object[] constructorArgs =
					{
					objectXml};
				Object messageObject = xmlstringConstructor.newInstance(constructorArgs);
				m_oMessageObject = (IXMLEncodable) messageObject;
			}
			catch (Exception a_e)
			{
				// ignore, not needed
			}
		}*/
	}
}
