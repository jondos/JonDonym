/*
 Copyright (c) 2000 - 2003, The JAP-Team
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
package anon.infoservice;

import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.JAPCertificate;
import anon.util.XMLUtil;
import anon.crypto.X509DistinguishedName;
import anon.crypto.X509SubjectAlternativeName;
import anon.crypto.AbstractX509Extension;
import anon.util.Util;

/**
 * Holds the information of the location of a service.
 */
public class ServiceLocation
{

	/**
	 * This is the city where the service is located.
	 */
	private String city;

	/**
	 * This is the state where the service is located.
	 */
	private String state;

	/**
	 * This is the country where the service is located.
	 */
	private String country;

	/**
	 * This is the longitude of the service location. Should be between -180.0 (west of Greenwich)
	 * and 180.0 (east of Greenwich).
	 */
	private String longitude;

	/**
	 * This is the latitude of the service location. Should be between -90.0 (South Pole) and 90.0
	 * (North Pole).
	 */
	private String latitude;

	/**
	 * Creates a new ServiceLocation from XML description (Location node).
	 *
	 * @param locationNode The Location node from an XML document.
	 */
	public ServiceLocation(Node locationNode, JAPCertificate mixCertificate)
	{
		Node node;
		X509DistinguishedName subject;

		//try to get Service Location from the Certificate
		if (mixCertificate != null)
		{
			subject = mixCertificate.getSubject();

			/* get the city */
			city = subject.getLocalityName();

			/* get the state */
			state = subject.getStateOrProvince();

			/* get the country */
			country = subject.getCountryCode();

			// get the geolocation
			AbstractX509Extension extension =
				mixCertificate.getExtensions().getExtension(X509SubjectAlternativeName.IDENTIFIER);
			if (extension != null && extension instanceof X509SubjectAlternativeName)
			{
				X509SubjectAlternativeName alternativeName = (X509SubjectAlternativeName) extension;

				Vector coordinates;
				if (alternativeName.getTags().size() == 2 &&
					alternativeName.getValues().size() == 2)
				{
					coordinates = alternativeName.getTags();
					if (coordinates.elementAt(0).equals(
						X509SubjectAlternativeName.TAG_OTHER) &&
						coordinates.elementAt(1).equals(
							X509SubjectAlternativeName.TAG_OTHER))
					{
						coordinates = alternativeName.getValues();
						try
						{
							longitude = coordinates.elementAt(0).toString();
							Util.parseFloat(longitude);
							longitude = longitude.trim();
						}
						catch (NumberFormatException a_e)
						{
							longitude = "";
						}
						try
						{
							latitude = coordinates.elementAt(1).toString();
							Util.parseFloat(latitude);
							latitude = latitude.trim();
						}
						catch (NumberFormatException a_e)
						{
							latitude = "";
						}
					}
				}
			}
		}

	    /* check if the the information from the cert is valid (not null oder empty)
	     * and take the information from the XML-Structure if not
	     */
	    if (city == null || city.trim().length() == 0)
		{
			node = XMLUtil.getFirstChildByName(locationNode, "City");
			city = XMLUtil.parseValue(node, "");
		}

	    if(state == null || state.trim().length() == 0)
		{
			node = XMLUtil.getFirstChildByName(locationNode, "State");
			state = XMLUtil.parseValue(node, "");
		}
		if(country == null || country.trim().length() == 0)
		{
			node = XMLUtil.getFirstChildByName(locationNode, "Country");
			country = XMLUtil.parseValue(node, "");
		}

		/* get the longitude / latitude */
		Node positionNode = XMLUtil.getFirstChildByName(locationNode, "Position");
		positionNode = XMLUtil.getFirstChildByName(positionNode, "Geo");
		if(longitude == null || longitude.trim().length() == 0)
		{
			node = XMLUtil.getFirstChildByName(positionNode, "Longitude");
			longitude = XMLUtil.parseValue(node, "");
		}
		if(latitude == null || latitude.trim().length() == 0)
		{
			node = XMLUtil.getFirstChildByName(positionNode, "Latitude");
			latitude = XMLUtil.parseValue(node, "");
		}
	}

	/**
	 * Returns the city where the service is located.
	 *
	 * @return The city where the service is located.
	 */
	public String getCity()
	{
		return city;
	}

	/**
	 * Returns the state where the service is located.
	 *
	 * @return The state where the service is located.
	 */
	public String getState()
	{
		return state;
	}

	/**
	 * Returns the country where the service is located.
	 *
	 * @return The country where the service is located.
	 */
	public String getCountry()
	{
		return country;
	}

	/**
	 * Returns the longitude of the service location. Should be between -180.0 (west of Greenwich)
	 * and 180.0 (east of Greenwich).
	 *
	 * @return The longitude of the service location.
	 */
	public String getLongitude()
	{
		return longitude;
	}

	/**
	 * Returns the latitude of the service location. Should be between -90.0 (South Pole) and 90.0
	 * (North Pole).
	 *
	 * @return The latitude of the service location.
	 */
	public String getLatitude()
	{
		return latitude;
	}

}
