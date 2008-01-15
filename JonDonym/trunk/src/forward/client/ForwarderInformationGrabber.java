/*
 Copyright (c) 2000 - 2004, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
  may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package forward.client;

import java.io.ByteArrayInputStream;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.infoservice.InfoServiceHolder;
import anon.util.captcha.IImageEncodedCaptcha;
import anon.util.captcha.ZipBinaryImageCaptchaClient;
import anon.util.XMLUtil;

/**
 * This class provides some tools for the forwarding client to fetch and handle the information
 * about a forwarder.
 */
public class ForwarderInformationGrabber
{

	/**
	 * This is the error code indicating that everything was fine.
	 */
	public static final int RETURN_SUCCESS = 0;

	/**
	 * This is the error code indicating that there was an error while fetching the data directly
	 * from the infoservice. This can happen, if we cannot reach any infoservice or if no
	 * infoservice knows any forwarder.
	 */
	public static final int RETURN_INFOSERVICE_ERROR = 1;

	/**
	 * This error code indicates, that there was an unexpected error, maybe the data fetched are
	 * in the wrong format.
	 */
	public static final int RETURN_UNKNOWN_ERROR = 2;

	/**
	 * This error occurs, if we don't know the captcha format which is used by the fetched
	 * forwarder information structure.
	 */
	public static final int RETURN_NO_CAPTCHA_IMPLEMENTATION = 3;

	/**
	 * This stores the error (if any) which occured, while the information was fetched or parsed.
	 */
	private int m_errorCode;

	/**
	 * This stores the captcha, if we have successfully parsed the forwarder information.
	 */
	private IImageEncodedCaptcha m_captcha;

	/**
	 * Creates a new ForwarderInformationGrabber and tries to fetch the information about a
	 * forwarder from the infoservices. Check the getErrorCode() method after the instance is
	 * constructed to see, if there occured any error. If no error occured, you can get the
	 * fetched information via the getCaptcha() method. The following error codes can occur:
	 * RETURN_SUCCESS, RETURN_INFOSERVICE_ERROR, RETURN_UNKNOWN_ERROR or
	 * RETURN_NO_CAPTCHA_IMPLEMENTATION.
	 */
	public ForwarderInformationGrabber()
	{
		m_captcha = null;
		Element japForwarderNode = InfoServiceHolder.getInstance().getForwarder();
		if (japForwarderNode != null)
		{
			/* get the CaptchaEncoded node */
			NodeList captchaEncodedNodes = japForwarderNode.getElementsByTagName("CaptchaEncoded");
			if (captchaEncodedNodes.getLength() > 0)
			{
				Element captchaEncodedNode = (Element) (captchaEncodedNodes.item(0));
				m_errorCode = findCaptchaImplementation(captchaEncodedNode);
			}
			else
			{
				/* no CaptchaEncoded node -> the infoservice only returns valid forwarder nodes ->
				 * return unknown error
				 */
				m_errorCode = RETURN_UNKNOWN_ERROR;
			}
		}
		else
		{
			/* we could not get a forwarder entry from the infoservice network -> we can't reach any
			 * infoservice or no infoservice knows a forwarder
			 */
			m_errorCode = RETURN_INFOSERVICE_ERROR;
		}
	}

	/**
	 * Creates a new ForwarderInformationGrabber instance and parses the supplied information
	 * structure. Check the getErrorCode() method after the instance is constructed to see, if
	 * there occured any error. If no error occured, you can get the parsed information via the
	 * getCaptcha() method. The following error codes can occur: RETURN_SUCCESS,
	 * RETURN_UNKNOWN_ERROR or RETURN_NO_CAPTCHA_IMPLEMENTATION.
	 *
	 * @param a_xmlData The XML structure with the JapForwarder node, like it is distribute from
	 *                  the infoservices with the getforwarder command.
	 */
	public ForwarderInformationGrabber(String a_xmlData)
	{
		m_captcha = null;
		try
		{
			/* parse the user input */
            Document doc = XMLUtil.toXMLDocument(a_xmlData);
			NodeList japForwarderNodes = doc.getElementsByTagName("JapForwarder");
			if (japForwarderNodes.getLength() > 0)
			{
				Element japForwarderNode = (Element) (japForwarderNodes.item(0));
				NodeList captchaEncodedNodes = japForwarderNode.getElementsByTagName("CaptchaEncoded");
				if (captchaEncodedNodes.getLength() > 0)
				{
					Element captchaEncodedNode = (Element) (captchaEncodedNodes.item(0));
					m_errorCode = findCaptchaImplementation(captchaEncodedNode);
				}
				else
				{
					/* invalid input, the XML information pasted in by the user should always be valid */
					m_errorCode = RETURN_UNKNOWN_ERROR;
				}
			}
			else
			{
				/* invalid input, the XML information pasted in by the user should always be valid */
				m_errorCode = RETURN_UNKNOWN_ERROR;
			}
		}
		catch (Exception e)
		{
			/* invalid input, the XML information pasted in by the user should always be valid */
			m_errorCode = RETURN_UNKNOWN_ERROR;
		}
	}

	/**
	 * Returns the error code which may occured while creating this instance of
	 * ForwarderInformationGrabber. See the RETURN constants in this class.
	 *
	 * @return The error code of the constructor of this instance.
	 */
	public int getErrorCode()
	{
		return m_errorCode;
	}

	/**
	 * Returns the structure which includes the captcha with the forwarder information. The value
	 * may be null, if there occured an error while constucting this instance of
	 * ForwarderInformationGrabber. So check getErrorCode() always first.
	 *
	 * @return The structure with the information about a forwarder, secured by a captcha.
	 */
	public IImageEncodedCaptcha getCaptcha()
	{
		return m_captcha;
	}

	/**
	 * Finds the correct captcha implementation for the supplied captcha. We use the
	 * CaptchaDataFormat node of the supplied structure to search matching local implementations.
	 *
	 * @param a_captchaEncodedNode The CaptchaEncodedNode which holds all needed information.
	 *
	 * @return RETURN_SUCCESS, if we have found a matching implementation. Also the internal
	 *                         storage of the captcha is updated.
	 *         RETURN_NO_CAPTCHA_IMPLEMENTATION, if we could not find a matching captcha
	 *                                           implementation.
	 *         RETURN_UNKNOWN_ERROR, if there occured an unexpected error, maybe because of invalid
	 *                               data in the supplied structure.
	 */
	private int findCaptchaImplementation(Element a_captchaEncodedNode)
	{
		int returnCode = RETURN_UNKNOWN_ERROR;
		/* read the captcha format */
		NodeList captchaDataFormatNodes = a_captchaEncodedNode.getElementsByTagName("CaptchaDataFormat");
		if (captchaDataFormatNodes.getLength() > 0)
		{
			Element captchaDataFormatNode = (Element) (captchaDataFormatNodes.item(0));
			if (ZipBinaryImageCaptchaClient.CAPTCHA_DATA_FORMAT.equals(captchaDataFormatNode.getFirstChild().
				getNodeValue()))
			{
				/* the captcha has the ZIP_BINARY_IMAGE format */
				try
				{
					m_captcha = new ZipBinaryImageCaptchaClient(a_captchaEncodedNode);
					returnCode = RETURN_SUCCESS;
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC,
						"Error while creating the captcha implementation!", e);
					returnCode = RETURN_UNKNOWN_ERROR;
				}
			}
			else
			{
				/* we don't know other implementations yet */
				returnCode = RETURN_NO_CAPTCHA_IMPLEMENTATION;
			}
		}
		else
		{
			/* no CaptchaDataFormat node */
			returnCode = RETURN_UNKNOWN_ERROR;
		}
		return returnCode;
	}

}
