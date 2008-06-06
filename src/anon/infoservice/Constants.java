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

import java.util.Locale;

final public class Constants
{
	public static final String DEFAULT_RESSOURCE_FILENAME = "InfoService.properties";

	public static final String CERTSPATH = "certificates/";
	public static final String CERT_JAPINFOSERVICEMESSAGES = "japinfoservicemessages.cer";
	;
	public static final int MAX_REQUEST_HEADER_SIZE = 10000;
	public static final int REQUEST_METHOD_UNKNOWN = -1;
	public static final int REQUEST_METHOD_GET = 1;
	public static final int REQUEST_METHOD_POST = 2;
	public static final int REQUEST_METHOD_HEAD = 3;

	public static final int MAX_NR_OF_CONCURRENT_CONNECTIONS = 50;

	/**
	 * The standard timeout for infoservice database entries in a JAP client. This should be an
	 * infinite timeout (1000 years should be infinite enough).
	 */
	public static final long TIMEOUT_INFOSERVICE_JAP = 1000 * 365 * 24 * 3600 * 1000L;

	/**
	 * The standard timeout for infoservice database entries in an infoservice.
	 */
	public static final long TIMEOUT_INFOSERVICE = 15 * 60 * 1000L; // 15 minutes
	public static final long TIMEOUT_MIX = 15 * 60 * 1000L; // 15 minutes
	public static final long TIMEOUT_MIXCASCADE = 15 * 60 * 1000L; // 15 minutes
	public static final long TIMEOUT_STATUS = 240 * 1000L; // 240 seconds
	public static final long TIMEOUT_PAYMENT_INSTANCE = 15 * 60 * 1000L; // 15 minutes
    public static final long TIMEOUT_TEMPORARY_CASCADE = 10 * 60 * 1000L; // 10 minutes

    /**
     * Minium and maximum length of dynamically build cascades
     */
    public static final int MAX_CASCADE_LENGTH =3;
    public static final int MIN_CASCADE_LENGTH = 2;

	/**
	 * The timeout for all entries in the database of JAP forwarders. If we don't get a new  update
	 * message from the forwarder within that time, it is removed from the database. The default is
	 * 15 minutes, so there is no problem, if the forwarder updates the entry every 10 minutes.
	 */
	public static final long TIMEOUT_JAP_FORWARDERS = 15 * 60 * (long) 1000;

	/**
	 * This is the timeout in seconds for verifying a JAP forwarding server (contacting the server
	 * and getting the acknowledgement, that it is a JAP forwarder). If we can't get the
	 * verification within that time, the JAP forwarding server is declared as invalid.
	 */
	public static final int FORWARDING_SERVER_VERIFY_TIMEOUT = 20;

	/**
	 * This is the general timeout for the Infoservice socket communication (milli seconds).
	 */
	public static final int COMMUNICATION_TIMEOUT = 30000; // 30 seconds

	public static final long ANNOUNCE_PERIOD = 5 * 60 * (long) (1000); // 5 minutes
	//public static final long ANNOUNCE_PERIOD = 30 * (long) (1000); // 30 seconds

	public static final long UPDATE_INFORMATION_ANNOUNCE_PERIOD = 10 * 60 * (long) (1000); // 10 minutes

	/**
	 * We use this for display some values in the local format.
	 */
	public static final Locale LOCAL_FORMAT = Locale.GERMAN;

	/**
	 * This is the version number of the infoservice software.
	 */
	public static final String INFOSERVICE_VERSION = "IS.08.006"; //never change the layout of this line!

}
