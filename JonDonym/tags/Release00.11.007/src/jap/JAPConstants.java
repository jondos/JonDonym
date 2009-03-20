/*
 Copyright (c) 2000-2006, The JAP-Team
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
package jap;

import java.awt.Font;

import anon.tor.Circuit;
import anon.tor.Tor;
import anon.mixminion.Mixminion;

public final class JAPConstants
{
	// Note: probably you like to change anon.AnonService.ANONLIB_VERSION as well
	public static final String aktVersion = "00.11.007"; //Never change the layout of this line!
	
	public final static boolean m_bUnstableVersion = false; //Set to true if this is an unstable (development) Version

	
	private static final String CVS_GENERATED_RELEASE_DATE = "$Date: 2009-02-26 21:16:39 $";

	//Warning: This is a little bit tricky,
	//because CVS will expand the $Date: 2009-02-26 21:16:39 $
	//to the date of the last commit of this file

	//Never change the layout of the next two lines as several automated tools use these lines to do fancy things...
	public final static boolean m_bReleasedVersion = true; //Set to true if this is a stable (release) Version
	private static final String RELEASE_DATE = "2008/08/06 12:11:16"; // Set only to a Value, if m_bReleaseVersion=true

	public static final String CURRENT_CONFIG_VERSION = aktVersion;

	public static final String strReleaseDate; //The Release date of this version
	
	public static final String PROGRAM_NAME_JAP = "JAP";
	public static final String PROGRAM_NAME_JAP_JONDO = "JAP/JonDo";
	public static final String PROGRAM_NAME_JONDO = "JonDo";

	//display in some information dialog and in
	//the update dialog
	static
	{ //This will set the strRealeaseDate to the correct Value
		//This is either the CVS_GENERATED_RELEASE_DATE or the RELEASE_DATE, if m_bReleasedVersion==true;
		if (m_bReleasedVersion)
		{
			strReleaseDate = RELEASE_DATE;
		}
		else
		{
			strReleaseDate = CVS_GENERATED_RELEASE_DATE.substring(7, 26);
		}
	}

	public static final boolean DEBUG = false;
	
	public static final String APPLICATION_NAME = "JonDo";

	public static final int DEFAULT_PORT_NUMBER = 4001;
	public static final boolean DEFAULT_LISTENER_IS_LOCAL = true;
	static final String DEFAULT_ANON_NAME = "noCascadesAvail";
	//static final String[] DEFAULT_ANON_MIX_IDs = new String[]{"BA6F90FB9120E0998ACFCC3A601F1B406A4655A1",
		//"75ACF4F101510607BA3E9E348821D8697BE8FC58"};
	static final String[] DEFAULT_ANON_MIX_IDs = new String[]{"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"};
	//static final String[] DEFAULT_ANON_HOSTS = {"mix.inf.tu-dresden.de", "141.76.45.33"};
	static final String[] DEFAULT_ANON_HOSTS = {"0.0.0.0"};

	//static final int DEFAULT_ANON_PORT_NUMBERS[] =
	//	{
	//	22, 80, 443, 6544};
	static final int DEFAULT_ANON_PORT_NUMBERS[] = {6544};

	/**
	 * The names of the default infoservices.
	 */
	public static final String DEFAULT_INFOSERVICE_NAMES[] =
		new String[]{"880D9306B90EC8309178376B43AC26652CE52B74",
		"8FF9236BD03A12391D939219310597C830F3943A",
		"1E47E65976C6F7868047B6E9A06654B8AFF36A38",
		"AE116ECB775FF127C02DF96F5466AECAF86B93A9"};
	//new String[]{"1AF4734DD3AA5BD1A8A4A2EDACAD825C711E1770"};
	public static final String DEFAULT_INFOSERVICE_HOSTNAMES[] =
		new String[]{"infoservice.inf.tu-dresden.de", "87.230.56.74", "78.129.146.44", "72.55.137.241"};
	//new String[]{"87.230.20.187"};

	public static final int DEFAULT_INFOSERVICE_PORT_NUMBERS[][] =
		{{80, 6543}, {80, 443}, {80, 443}, {80, 443}};
	//	{{80}};

	/**
	 * This defines, whether automatic infoservice request are disabled as default.
	 */
	public static final boolean DEFAULT_INFOSERVICE_DISABLED = false;

	/**
	 * This defines the timeout for infoservice communication (connections to the update server
	 * have also this timeout because of the same HTTPConnectionFactory).
	 */
	public static final int DEFAULT_INFOSERVICE_TIMEOUT = 30;

	public static final boolean REMIND_OPTIONAL_UPDATE = true;
	public static final boolean REMIND_JAVA_UPDATE = true;
	public static final boolean ANONYMIZED_HTTP_HEADERS = true;

	static final int SMALL_FONT_SIZE = 9;
	static final int SMALL_FONT_STYLE = Font.PLAIN;

	public static final int VIEW_NORMAL = 1;
	public static final int VIEW_SIMPLIFIED = 2;
	public static final int DEFAULT_VIEW = VIEW_SIMPLIFIED;

	static final boolean DEFAULT_SAVE_MAIN_WINDOW_POSITION = true;
	static final boolean DEFAULT_SAVE_MINI_WINDOW_POSITION = true;
	static final boolean DEFAULT_SAVE_CONFIG_WINDOW_POSITION = false;
	static final boolean DEFAULT_SAVE_HELP_WINDOW_POSITION = false;
	static final boolean DEFAULT_SAVE_HELP_WINDOW_SIZE = false;
	static final boolean DEFAULT_SAVE_CONFIG_WINDOW_SIZE = false;
	static final boolean DEFAULT_MOVE_TO_SYSTRAY_ON_STARTUP = false;
	static final boolean DEFAULT_MINIMIZE_ON_STARTUP = false;
	static final boolean DEFAULT_WARN_ON_CLOSE = true;

	static final String JAPLocalFilename = "JAP.jar";
	public static final String XMLCONFFN = "jap.conf";
	public static final String MESSAGESFN = "JAPMessages";
	public static final String BUSYFN = "busy.gif";
	static final String ABOUTFN = "info.gif";
	public static final String DOWNLOADFN = "install.gif";
	static final String IICON16FN = "icon16.gif";
	static final String ICON_JONDO = "JonDo.ico.gif";


	//static final String   CONFIGICONFN                 = "icoc.gif";
	static final String ENLARGEYICONFN = "enlarge.gif";
	static final String METERICONFN = "icom.gif";
	public static final String IMAGE_ARROW = "arrow46.gif";
	public static final String IMAGE_BLANK = "blank.gif";
	public static final String IMAGE_STEPFINISHED = "haken.gif";
	public static final String IMAGE_ARROW_DOWN = "arrowDown.gif";
	public static final String IMAGE_ARROW_UP = "arrowUp.gif";
	public static final String IMAGE_SERVER = "server.gif";
	public static final String IMAGE_SERVER_BLAU = "server_blau.gif";
	public static final String IMAGE_SERVER_ROT = "server_rot.gif";
	public static final String IMAGE_RELOAD = "reload.gif";
	public static final String IMAGE_RELOAD_DISABLED = "reloaddisabled_anim.gif";
	public static final String IMAGE_RELOAD_ROLLOVER = "reloadrollover.gif";
	public static final String IMAGE_WARNING = "warning.gif";
	public static final String IMAGE_INFORMATION = "information.gif";
	public static final String IMAGE_ERROR = "error.gif";
	public static final String IMAGE_CASCADE_MANUAL_NOT_TRUSTED = "cdisabled.gif";
	public static final String IMAGE_CASCADE_MANUELL = "servermanuell.gif";
	public static final String IMAGE_CASCADE_INTERNET_NOT_TRUSTED = "cdisabled.gif";
	public static final String IMAGE_CASCADE_PAYMENT = "serverwithpayment.gif";
	public static final String IMAGE_CASCADE_PAYMENT_NOT_TRUSTED = "cdisabled.gif";
	public static final String IMAGE_CASCADE_INTERNET = "serverfrominternet.gif";
	public static final String IMAGE_INFOSERVICE_MANUELL = "infoservicemanuell.gif";
	public static final String IMAGE_INFOSERVICE_INTERNET = "infoservicefrominternet.gif";
	public static final String IMAGE_INFOSERVICE_BIGLOGO = "infoservicebiglogo.gif";

	public static final String IMAGE_SAVE = "saveicon.gif";
	public static final String IMAGE_EXIT = "exiticon.gif";
	public static final String IMAGE_DELETE = "deleteicon.gif";
	public static final String IMAGE_COPY = "copyicon.gif";
	public static final String IMAGE_COPY_CONFIG = "copyintoicon.gif";

	public static final String CERTENABLEDICON = "cenabled.gif";
	public static final String CERTDISABLEDICON = "cdisabled.gif";

	public static final String IMAGE_COINS_FULL = "coins-full.gif";
	public static final String IMAGE_COINS_QUITEFULL = "coins-quitefull.gif";
	public static final String IMAGE_COINS_MEDIUM = "coins-medium.gif";
	public static final String IMAGE_COINS_LOW = "coins-low.gif";
	public static final String IMAGE_COINS_EMPTY = "coins-empty.gif";
	public static final String IMAGE_COIN_COINSTACK = "coinstack.gif";

	//images for payment methods - name has to be "IMAGE_" + XMLPaymentMethod.getName() to be found via reflection
	public static final String IMAGE_PAYPAL = "paypal_logo.png";
	public static final String IMAGE_PAYSAFECARD = "psc_logo.png";
	public static final String IMAGE_EGOLD = "e-gold_logo.png";

// Bastian Voigt: Icons for the account meter
	public static final String[] ACCOUNTICONFNARRAY =
		{
		"accountDisabled.gif", "accountOk.gif", "accountBroken.gif"
	};

	// Bastian Voigt: Browser executable names for the browserstart hack
	public static final String[] BROWSERLIST =
		{
		"firefox", "iexplore", "explorer", "mozilla", "konqueror", "mozilla-firefox", "firebird", "opera"
	};

	public final static String PI_CERTS[] = new String[] {"bi.cer.dev", "Payment_Instance.cer"};
	public final static String CERTSPATH = "certificates/";
	public final static String INFOSERVICE_CERTSPATH = "acceptedInfoServiceCAs/";
	public final static String PAYMENT_ROOT_CERTSPATH = "acceptedPaymentCAs/";
	public final static String PAYMENT_ROOT_CERTS[] = new String[] {};
	public final static String PAYMENT_DEFAULT_CERTSPATH = "acceptedPIs/";
	public final static String MIX_CERTSPATH = "acceptedMixCAs/";
	public final static String OPERATOR_CERTSPATH = "acceptedMixOperators/";
	public final static String MIX_ROOT_CERTS[] =
		new String[] {"japmixroot.cer", "Operator_CA.cer", "Test_CA.cer.dev", "gpf_jondonym_ca.cer"};
	public final static String INFOSERVICE_ROOT_CERTS[] =
		new String[] {"japinfoserviceroot.cer", "InfoService_CA.cer"};
	public final static String CERT_JAPCODESIGNING = "japcodesigning.cer";
	public final static String CERT_JAPINFOSERVICEMESSAGES = "japupdatemessages.cer";
	public static final boolean DEFAULT_CERT_CHECK_ENABLED = true;

	public final static int TOR_MAX_CONNECTIONS_PER_ROUTE = Circuit.MAX_STREAMS_OVER_CIRCUIT;
	public final static int TOR_MAX_ROUTE_LEN = Tor.MAX_ROUTE_LEN;
	public final static int TOR_MIN_ROUTE_LEN = Tor.MIN_ROUTE_LEN;
	public final static int MIXMINION_MAX_ROUTE_LEN = Mixminion.MAX_ROUTE_LEN;
	public final static int MIXMINION_MIN_ROUTE_LEN = Mixminion.MIN_ROUTE_LEN;
	public final static boolean DEFAULT_TOR_PRECREATE_ROUTES = false;
	public final static int DEFAULT_TOR_MIN_ROUTE_LEN = Tor.MIN_ROUTE_LEN;
	public final static int DEFAULT_TOR_MAX_ROUTE_LEN = Tor.MIN_ROUTE_LEN + 1;
	public final static int DEFAULT_TOR_MAX_CONNECTIONS_PER_ROUTE = Circuit.MAX_STREAMS_OVER_CIRCUIT;
	public final static boolean DEFAULT_TOR_USE_NONE_DEFAULT_DIR_SERVER=false;
	public final static int DEFAULT_MIXMINION_ROUTE_LEN = Mixminion.MIN_ROUTE_LEN;
	public final static String DEFAULT_MIXMINION_EMAIL = "";
	/**
	 * The minimum bandwidth per user needed for forwarding. This affects the maximum number
	 * of users, which can be forwarded with a specified bandwidth. The default is 2 KByte/sec
	 * for each user.
	 */
	public static final int ROUTING_BANDWIDTH_PER_USER = 4000;

	/**
	 * This is the mailaddress of the InfoService mailgateway.
	 */
	public static final String MAIL_SYSTEM_ADDRESS = "japmailsystem@infoservice.inf.tu-dresden.de";

	//japconfig
	public final static String CONFIG_VERSION = "version";
	public final static String CONFIG_PORT_NUMBER = "portNumber";
	public final static String CONFIG_LISTENER_IS_LOCAL = "listenerIsLocal";
	public final static String CONFIG_NEVER_REMIND_ACTIVE_CONTENT = "neverRemindActiveContent";
	public final static String CONFIG_NEVER_EXPLAIN_FORWARD = "neverExplainForward";
	public final static String CONFIG_DO_NOT_ABUSE_REMINDER = "doNotAbuseReminder";
	public final static String CONFIG_NEVER_REMIND_GOODBYE = "neverRemindGoodBye";
	public final static String CONFIG_INFOSERVICE_DISABLED = "infoServiceDisabled";
	public final static String CONFIG_INFOSERVICE_TIMEOUT = "infoserviceTimeout";
	public final static String CONFIG_PROXY_HOST_NAME = "proxyHostName";
	public final static String CONFIG_PROXY_PORT_NUMBER = "proxyPortNumber";
	public final static String CONFIG_PROXY_TYPE = "proxyType";
	public final static String CONFIG_PROXY_AUTH_USER_ID = "proxyAuthUserID";
	public final static String CONFIG_PROXY_AUTHORIZATION = "proxyAuthorization";
	public final static String CONFIG_PROXY_MODE = "proxyMode";
	public final static String CONFIG_DUMMY_TRAFFIC_INTERVALL = "DummytrafficInterval";
	public final static String CONFIG_AUTO_CONNECT = "autoconnect";
	public final static String CONFIG_AUTO_RECONNECT = "autoReconnect";
	public final static String CONFIG_MINIMIZED_STARTUP = "minimizedStartup";
	public final static String CONFIG_LOCALE = "Locale";
	public final static String CONFIG_LOOK_AND_FEEL = "LookAndFeel";
	public final static String CONFIG_UNKNOWN = "unknown";
	public final static String CONFIG_GUI = "GUI";
	public final static String CONFIG_LOG_DETAIL = "Detail";
	public final static String CONFIG_MAIN_WINDOW = "MainWindow";
	public final static String CONFIG_LOCATION = "Location";
	public final static String CONFIG_X = "x";
	public final static String CONFIG_Y = "y";
	public final static String CONFIG_DX = "dx";
	public final static String CONFIG_DY = "dy";
	//public final static String CONFIG_SIZE = "Size";
	public final static String CONFIG_MOVE_TO_SYSTRAY = "MoveToSystray";
	public final static String CONFIG_DEFAULT_VIEW = "DefaultView";
	public final static String CONFIG_START_PORTABLE_FIREFOX = "StartPortableFirefox";
	public final static String CONFIG_NORMAL = "Normal";
	public final static String CONFIG_SIMPLIFIED = "Simplified";
	public final static String CONFIG_DEBUG = "Debug";
	public final static String CONFIG_LEVEL = "Level";
	public final static String CONFIG_TYPE = "Type";
	public final static String CONFIG_OUTPUT = "Output";
	public final static String CONFIG_CONSOLE = "Console";
	public final static String CONFIG_WINDOW = "showWindow";
	public final static String CONFIG_FILE = "File";
	public final static String CONFIG_TOR = "TOR";
	public static final String CONFIG_TOR_DIR_SERVER="DirectoryServer";
	public static final String CONFIG_XML_ATTR_TOR_NONE_DEFAULT_DIR_SERVER="useNoneDefault";

	public final static String CONFIG_Mixminion = "MixMinion";
	public final static String CONFIG_MAX_CONNECTIONS_PER_ROUTE = "MaxConnectionsPerRoute";
	public final static String CONFIG_TOR_PRECREATE_ANON_ROUTES = "PreCreateAnonRoutes";
	public final static String CONFIG_ROUTE_LEN = "RouteLen";
	public final static String CONFIG_MIXMINION_REPLY_MAIL = "MixminionREPLYMail";
	public final static String CONFIG_MIXMINION_PASSWORD_HASH = "MixminionPasswordHash";
	public final static String CONFIG_MIXMINION_KEYRING = "MixminionKeyring";
	public final static String CONFIG_MIN = "min";
	public final static String CONFIG_MAX = "max";
	public final static String CONFIG_PAYMENT = "Payment";
	public final static String CONFIG_ENCRYPTED_DATA = "EncryptedData";
	public final static String CONFIG_JAP_FORWARDING_SETTINGS = "JapForwardingSettings";
	public final static String CONFIG_ACCEPTED_TERMS_AND_CONDITIONS = "AcceptedTermsAndConditions";

	/** Supported non-generic payment names. Comma-separated list. e.g. "CreditCard,DirectDebit"*/
	public final static String PAYMENT_NONGENERIC="CreditCard";

	/**
	 * Restart after unrecoverable socket errors. Sets the time when such an error is seen as
	 * unrecoverable.
	 */
	public static final long TIME_RESTART_AFTER_SOCKET_ERROR = 1000 * 60;

	private static final String[] SUPPORTED_LANGUAGES = {"en", "de", "cs", "nl", "fr"}; // "zh", "ca", "es", "ru"}; //pt

	public static String[] getSupportedLanguages()
	{
		String[] languages = new String[SUPPORTED_LANGUAGES.length];
		for (int i = 0; i < languages.length; i++)
		{
			languages[i] = SUPPORTED_LANGUAGES[i];
		}

		return languages;
	}
	
	public static final String IN_ADDR_ANY_IPV4 = "0.0.0.0";
	public static final String IN_ADDR_LOOPBACK_IPV4 = "127.0.0.1";
}
