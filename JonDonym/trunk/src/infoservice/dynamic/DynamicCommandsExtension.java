/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package infoservice.dynamic;

import infoservice.Configuration;
import infoservice.HttpResponseStructure;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.infoservice.Constants;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * This class provides the functionality needed by the "dynamic cascade
 * extension". It is essentially an extension to
 * <code>InfoServiceCommands</code>
 * 
 * @author LERNGRUPPE
 */

public class DynamicCommandsExtension
{
    /**
     * This method is called, when we receive data from a mixcascade (first mix)
     * or when we receive data from a remote infoservice, which posts data about
     * a mixcascade.
     * 
     * @param a_postData
     *            The data we have received.
     * 
     * @return The HTTP response for the client.
     */
    public HttpResponseStructure cascadePostHelo(byte[] a_postData, int a_encoding)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_OK);
        try
        {
            LogHolder.log(LogLevel.DEBUG, LogType.NET, "MixCascade HELO received: XML: "
                    + (new String(a_postData)));

            /* verify the signature */
            MixCascade mixCascadeEntry;
            if (a_encoding == HttpResponseStructure.HTTP_ENCODING_ZLIB)
            {
                mixCascadeEntry = new MixCascade(a_postData);
            }
            else if (a_encoding == HttpResponseStructure.HTTP_ENCODING_PLAIN)
            {
                Element mixCascadeNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil
                        .toXMLDocument(a_postData), MixCascade.XML_ELEMENT_NAME));
                mixCascadeEntry = new MixCascade(mixCascadeNode);
            }
            else
            {
                throw new Exception("Unsupported post encoding:" + a_encoding);
            }
            // remove temporary cascades if existig
            VirtualCascade temporaryCascade = (VirtualCascade) Database.getInstance(
                    VirtualCascade.class).getEntryById(mixCascadeEntry.getId());
            if (temporaryCascade != null)
            {
                if (temporaryCascade.getRealCascade().getMixIDsAsString().equals(
                        mixCascadeEntry.getMixIDsAsString()))
                {
                    Database.getInstance(VirtualCascade.class).remove(temporaryCascade);

                }
            }

            Database.getInstance(MixCascade.class).update(mixCascadeEntry);
        }
        catch (Exception e)
        {
            LogHolder.log(LogLevel.ERR, LogType.NET, e);
            httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
        }
        return httpResponse;
    }

    /**
     * Checks if there exists new cascade information for the mix with the given
     * ID.
     * 
     * @param a_strMixId
     * @return <code>HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR</code>
     *         if no new information is available,
     *         <code>HttpResponseStructure.HTTP_RETURN_OK</code> otherwise
     */
    public HttpResponseStructure isNewCascadeAvailable(String a_strMixId)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_NOT_FOUND);

        if (!haveNewCascadeInformation(a_strMixId))
        {
            return httpResponse;
        }
        httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
        return httpResponse;
    }

    public HttpResponseStructure reconfigureMix(String a_strMixId)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
        MixCascade cascade = getTemporaryCascade(a_strMixId);
        if (cascade != null)
        {
            Element doc = cascade.getXmlStructure();
            SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE,
                    doc);
            httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
                    HttpResponseStructure.HTTP_ENCODING_PLAIN, XMLUtil.toString(doc));
        }
        return httpResponse;
    }

    /**
     * Tests if there exists new cascade information for the mix with the given
     * ID. New information should be available if 1) No current cascade is found
     * and a temporary cascade is available 2) Current cascade is not equal to a
     * temporary cascade
     * 
     * @param a_strMixId
     *            The ID of the mix in question
     * @return <code>true</code> if a new cascade is available,
     *         <code>false</code> otherwise
     */
    private boolean haveNewCascadeInformation(String a_strMixId)
    {
        MixCascade assignedCascade = getCurrentCascade(a_strMixId);
        MixCascade assignedTemporaryCascade = getTemporaryCascade(a_strMixId);
        // No new information
        if (assignedTemporaryCascade == null)
            return false;

        // No new information
        if (assignedTemporaryCascade.compareMixIDs(assignedCascade))
            return false;

        // In all other cases the assignedTemporaryCascade should be newer ->
        // new information
        return true;
    }

    /**
     * Returns a temporary cascade for the mix with the given ID. Not the
     * VirtualCascade-object, but the real MixCascade is returned
     * 
     * @param a_mixId
     *            The ID of the mix in question
     * @return A temporary <code>MixCascade</code> for the mix or
     *         <code>null</code>
     */
    private MixCascade getTemporaryCascade(String a_mixId)
    {
        Enumeration knownTemporaryMixCascades = Database.getInstance(VirtualCascade.class)
                .getEntryList().elements();
        MixCascade assignedTemporaryCascade = null;
        while (knownTemporaryMixCascades.hasMoreElements() && (assignedTemporaryCascade == null))
        {
            MixCascade currentCascade = ((VirtualCascade) (knownTemporaryMixCascades.nextElement()))
                    .getRealCascade();
            if (currentCascade.getMixIds().contains(a_mixId))
            {
                /* the mix is assigned to that cascade */
                assignedTemporaryCascade = currentCascade;
                break;
            }
        }
        return assignedTemporaryCascade;
    }

    /**
     * Returns the current cascade for the mix with the given ID.
     * 
     * @param a_mixId
     *            The ID of the mix in question
     * @return The current <code>MixCascade</code> for the mix or
     *         <code>null</code>
     */
    private MixCascade getCurrentCascade(String a_mixId)
    {
        /* check whether the mix is already assigned to a mixcascade */
        Enumeration knownMixCascades = Database.getInstance(MixCascade.class).getEntryList()
                .elements();
        MixCascade assignedCascade = null;
        while (knownMixCascades.hasMoreElements() && (assignedCascade == null))
        {
            MixCascade currentCascade = (MixCascade) (knownMixCascades.nextElement());
            if (currentCascade.getMixIds().contains(a_mixId))
            {
                /* the mix is assigned to that cascade */
                assignedCascade = currentCascade;
                break;
            }
        }
        return assignedCascade;
    }

    /**
     * This method gets called when a last mix posts its cascade information to
     * the InfoService Such a cascade is not yet established, so it is a
     * temporary cascade an will be treated as such
     * 
     * @param a_postData
     *            The data of the POST request
     * @return <code>HttpResponseStructure</code> HTTP_RETURN_OK (no payload)
     *         or HTTP_RETURN_INTERNAL_SERVER_ERROR
     */
    public HttpResponseStructure lastMixPostDynaCascade(byte[] a_postData)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_OK);
        try
        {
            LogHolder.log(LogLevel.DEBUG, LogType.NET, "MixCascade HELO received: XML: "
                    + (new String(a_postData)));
            Element mixCascadeNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil
                    .toXMLDocument(a_postData), MixCascade.XML_ELEMENT_NAME));
            /* verify the signature */
            if (SignatureVerifier.getInstance().verifyXml(mixCascadeNode,
                    SignatureVerifier.DOCUMENT_CLASS_MIX) == true)
            {
                MixCascade mixCascadeEntry = new MixCascade(mixCascadeNode);
                VirtualCascade tmp = new VirtualCascade(mixCascadeEntry);
                Database.getInstance(VirtualCascade.class).update(tmp);
            }
            else
            {
                LogHolder.log(LogLevel.WARNING, LogType.NET,
                        "Signature check failed for MixCascade entry! XML: "
                                + (new String(a_postData)));
                httpResponse = new HttpResponseStructure(
                        HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
            }
        }
        catch (Exception e)
        {
            LogHolder.log(LogLevel.ERR, LogType.NET, e);
            httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
        }
        return httpResponse;

    }

    /**
     * This method gets called when a mix asks the InfoService to verify its
     * connectivity. Connectivity verification works as follows
     * <ul>
     * <li>Mix sends /connectivity-request as HTTP-Post containing a XML
     * structure with the port to be probed</li>
     * <li>The InfoService opens a socket to the source address of the request
     * to the requested port</li>
     * <li>It then sends a random number as echo request (cp. ICMP echo
     * request) to the mix</li>
     * <li>The mix sends the payload (i.e. random number back over the socket</li>
     * <li>InfoService responds with "OK" if it got the random number back,
     * "Failed" otherwise</li>
     * </ul>
     * 
     * @param a_sourceAddress
     *            The source address of the request
     * @param a_postData
     *            The POST data containing a XML structure with the port
     * @return <code>HttpResponseStructure</code> HTTP_RETURN_OK (containing
     *         the answer XML structure) or HTTP_RETURN_INTERNAL_SERVER_ERROR
     */
    public HttpResponseStructure mixPostConnectivityTest(InetAddress a_sourceAddress,
            byte[] a_postData)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
        int port = extractPort(a_postData);
        LogHolder.log(LogLevel.EMERG, LogType.MISC, "Port: " + port);
        if (port == -1)
        {
            LogHolder.log(LogLevel.DEBUG, LogType.MISC, "connectivityTest: No Port given");
            return httpResponse;
        }

        Document docConnectivity = null;
        if (isReachable(a_sourceAddress, port))
        {
            docConnectivity = constructAnswer("OK");
        }
        else
        {
            docConnectivity = constructAnswer("Failed");
        }
        httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
                HttpResponseStructure.HTTP_ENCODING_PLAIN, XMLUtil.toString(docConnectivity));
        return httpResponse;
    }

    /**
     * Constructs the answer for a connectivity request.
     * 
     * @param response
     *            Either "True" or "False"
     * @return The XML Document containing the answer
     */
    private Document constructAnswer(String response)
    {
        Document result = XMLUtil.createDocument();
        Node nodeConnectivity = result.createElement("Connectivity");
        Node nodeResult = result.createElement("Result");
        result.appendChild(nodeConnectivity);
        nodeConnectivity.appendChild(nodeResult);
        XMLUtil.setValue(nodeResult, response);
        return result;
    }

    /**
     * Checks if the given address is reachable on the given port
     * 
     * @param a_Address
     *            The address to be tested
     * @param port
     *            The port to be tested
     * @return <code>true</code> if the address is reachable on the given
     *         port, <code>false</code> otherwise
     */
    private boolean isReachable(InetAddress a_Address, int port)
    {
        // Construct the challenge to send
        Random rand = new Random();
        long echoRequest = Math.abs(rand.nextLong());

        LogHolder.log(LogLevel.EMERG, LogType.ALL, "Echo request is: " + echoRequest);
        String result = doPing(a_Address, port, echoRequest);
        if (result == null)
            return false;

        long echoResponse = Long.parseLong(result);
        return (echoResponse == echoRequest);
    }

    /**
     * Actually executes the ping-like connectivity-test. Sends a echoRequest to
     * the querying mix and waits for the same token to come back from the mix
     * 
     * @param a_Address
     *            The target address
     * @param port
     *            The target port
     * @param echoRequest
     *            The echoRequest to send
     * @return The echoResponse from the mix
     */
    private String doPing(InetAddress a_Address, int port, long echoRequest)
    {
        StringBuffer result = new StringBuffer();
        try
        {
            Socket socket = new Socket(a_Address, port);
            socket.setSoTimeout(5000);
            BufferedOutputStream str = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

            byte[] content = Long.toString(echoRequest).getBytes();

            // Send the echo request
            str.write(content);
            str.flush();

            // Read the response from the mix
            byte[] buff = new byte[content.length];
            int bytes = -1;
            while ((bytes = in.read(buff)) != -1)
            {
                byte[] real = new byte[bytes];
                System.arraycopy(buff, 0, real, 0, bytes);
                result.append(new String(real));
            }

            in.close();
            socket.close();
        }
        catch (Exception e)
        {
            return null;
        }
        LogHolder.log(LogLevel.DEBUG, LogType.NET, "Answer from Mix was:  " + result);
        return result.toString();
    }

    /**
     * Extracts the port from the POST data. Creates a XML-Document and returns
     * the value of the <code>Port</code> element.
     * 
     * @param a_postData
     *            The POST data to parse
     * @return The port or -1 if there was an error
     */
    private int extractPort(byte[] a_postData)
    {
        Document doc;
        int port = -1;
        try
        {
            doc = XMLUtil.toXMLDocument(a_postData);
            Node rootNode = doc.getFirstChild();
            Element portNode = (Element) XMLUtil.getFirstChildByName(rootNode, "Port");
            port = XMLUtil.parseValue(portNode, -1);
        }
        catch (XMLParseException e)
        {
            // we can ignore this
        }
        return port;
    }

    /**
     * Sends a generated HTML file with all status entrys to the client. This
     * function is not used by the JAP client. It's intended to use with a
     * webbrowser to see the status of all cascades.
     * 
     * @return The HTTP response for the client.
     */
    public HttpResponseStructure virtualCascadeStatus()
    {
        /* this is only the default, if something is going wrong */
        HttpResponseStructure httpResponse;
        try
        {
            String virtualCascades = getCascadeHtmlTable(VirtualCascade.class);
            String realCascades = getCascadeHtmlTable(MixCascade.class);

            String htmlData = "<HTML>\n"
                    + "  <HEAD>\n"
                    + "    <TITLE>INFOSERVICE - Virtual-Cascades Status</TITLE>\n"
                    + "    <STYLE TYPE=\"text/css\">\n"
                    + "      <!--\n"
                    + "        h1 {color:blue; text-align:center;}\n"
                    + "        b,h3,h4,h5 {font-weight:bold; color:maroon;}\n"
                    + "        body {margin-top:0px; margin-left:0px; margin-width:0px; margin-height:0px; background-color:white; color:black;}\n"
                    + "        h1,h2,h3,h4,h5,p,address,ol,ul,tr,td,th,blockquote,body,.smalltext,.leftcol {font-family:geneva,arial,helvetica,sans-serif;}\n"
                    + "        p,address,ol,ul,tr,td,th,blockquote {font-size:11pt;}\n"
                    + "        .leftcol,.smalltext {font-size: 10px;}\n"
                    + "        h1 {font-size:17px;}\n"
                    + "        h2 {font-size:16px;}\n"
                    + "        h3 {font-size:15px;}\n"
                    + "        h4 {font-size:14px;}\n"
                    + "        h5 {font-size:13px;}\n"
                    + "        address {font-style:normal;}\n"
                    + "        hr {color:#cccccc;}\n"
                    + "        h2,.leftcol {font-weight:bold; color:#006699;}\n"
                    + "        a:link {color:#006699; font-weight:bold; text-decoration:none;}\n"
                    + "        a:visited {color:#666666; font-weight:bold; text-decoration:none;}\n"
                    + "        a:active {color:#006699; font-weight:bold; text-decoration:none;}\n"
                    + "        a:hover {color:#006699; font-weight:bold; text-decoration:underline;}\n"
                    + "        th {color:white; background:#006699; font-weight:bold; text-align:left;}\n"
                    + "        td.name {border-bottom-style:solid; border-bottom-width:1pt; border-color:#006699; background:#eeeeff;}\n"
                    + "        td.status {border-bottom-style:solid; border-bottom-width:1pt; border-color:#006699;}\n"
                    + "      -->\n" + "    </STYLE>\n"
                    + "    <META HTTP-EQUIV=\"refresh\" CONTENT=\"10\">\n" + "  </HEAD>\n"
                    + "  <BODY BGCOLOR=\"#FFFFFF\">\n" + "    <P ALIGN=\"right\">"
                    + (new Date()).toString() + "</P>\n"
                    + "    <H2>INFOSERVICE - Virtual-Cascades Status</H2><BR><BR>\n"
                    + "<h3>Real cascades</h3>";

            htmlData += realCascades;
            htmlData += "<br/><h3>Virtual cascades</h3>";
            htmlData += virtualCascades;

            htmlData += "<h3>Unused mixes</h3>";
            htmlData += "    <TABLE ALIGN=\"center\" BORDER=\"0\" width=\"100%\">\n";
            htmlData += "      <COLGROUP>\n";
            htmlData += "        <COL>\n";
            htmlData += "        <COL>\n";
            htmlData += "        <COL>\n";
            htmlData += "        <COL>\n";
            htmlData += "        </COLGROUP>\n";
            htmlData += "      <TR>\n";
            htmlData += "        <TH>Mix Id</TH>\n";
            htmlData += "        <TH>Mix Host</TH>\n";
            htmlData += "        <TH>Mix Port</TH>\n";
            htmlData += "        <TH>Mix Type</TH>\n";
            htmlData += "      </TR>\n";
            Vector unusedMixes = getUnusedMixex();
            Enumeration mixes = unusedMixes.elements();
            while (mixes.hasMoreElements())
            {
                MixInfo mixInfo2 = (MixInfo) mixes.nextElement();
                htmlData += "<TR>\n";
                htmlData += "<TD>" + mixInfo2.getId() + "</TD>\n";
                htmlData += "<TD>" + mixInfo2.getFirstHostName() + "</TD>\n";
                htmlData += "<TD>" + mixInfo2.getFirstPort() + "</TD>\n";
                htmlData += "<TD>" + mixInfo2.getTypeAsString() + "</TD>\n";
                htmlData += "</TR>\n";
            }
            htmlData = htmlData + "    </TABLE><BR><BR><BR><BR>\n";
            htmlData = htmlData + "    <P>Infoservice [" + Constants.INFOSERVICE_VERSION
                    + "] Startup Time: " + Configuration.getInstance().getStartupTime() + "</P>\n"
                    + "    <HR noShade SIZE=\"1\">\n"
                    + "    <ADDRESS>&copy; 2000 - 2006 The JAP Team</ADDRESS>\n" + "  </BODY>\n"
                    + "</HTML>\n";
            /* send content */
            httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_HTML,

            HttpResponseStructure.HTTP_ENCODING_PLAIN, htmlData);
        }
        catch (Exception e)
        {
            String htmlData = "<HTML>\n"
                    + "  <HEAD>\n"
                    + "    <TITLE>INFOSERVICE - Virtual-Cascades Status</TITLE>\n"
                    + "    <STYLE TYPE=\"text/css\">\n"
                    + "      <!--\n"
                    + "        h1 {color:blue; text-align:center;}\n"
                    + "        b,h3,h4,h5 {font-weight:bold; color:maroon;}\n"
                    + "        body {margin-top:0px; margin-left:0px; margin-width:0px; margin-height:0px; background-color:white; color:black;}\n"
                    + "        h1,h2,h3,h4,h5,p,address,ol,ul,tr,td,th,blockquote,body,.smalltext,.leftcol {font-family:geneva,arial,helvetica,sans-serif;}\n"
                    + "        p,address,ol,ul,tr,td,th,blockquote {font-size:11pt;}\n"
                    + "        .leftcol,.smalltext {font-size: 10px;}\n"
                    + "        h1 {font-size:17px;}\n"
                    + "        h2 {font-size:16px;}\n"
                    + "        h3 {font-size:15px;}\n"
                    + "        h4 {font-size:14px;}\n"
                    + "        h5 {font-size:13px;}\n"
                    + "        address {font-style:normal;}\n"
                    + "        hr {color:#cccccc;}\n"
                    + "        h2,.leftcol {font-weight:bold; color:#006699;}\n"
                    + "        a:link {color:#006699; font-weight:bold; text-decoration:none;}\n"
                    + "        a:visited {color:#666666; font-weight:bold; text-decoration:none;}\n"
                    + "        a:active {color:#006699; font-weight:bold; text-decoration:none;}\n"
                    + "        a:hover {color:#006699; font-weight:bold; text-decoration:underline;}\n"
                    + "        th {color:white; background:#006699; font-weight:bold; text-align:left;}\n"
                    + "        td.name {border-bottom-style:solid; border-bottom-width:1pt; border-color:#006699; background:#eeeeff;}\n"
                    + "        td.status {border-bottom-style:solid; border-bottom-width:1pt; border-color:#006699;}\n"
                    + "      -->\n" + "    </STYLE>\n"
                    + "    <META HTTP-EQUIV=\"refresh\" CONTENT=\"10\">\n" + "  </HEAD>\n"
                    + "  <BODY BGCOLOR=\"#FFFFFF\">\n"
                    + "    <P ALIGN=\"right\"><h3>Updating status, please wait...</h3></p><br/>"
                    + " <P>Infoservice [" + Constants.INFOSERVICE_VERSION + "] Startup Time: "
                    + Configuration.getInstance().getStartupTime() + "</P>\n"
                    + "    <HR noShade SIZE=\"1\">\n"
                    + "    <ADDRESS>&copy; 2000 - 2006 The JAP Team</ADDRESS>\n" + "  </BODY>\n"
                    + "</HTML>\n";
            httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_HTML,
                    HttpResponseStructure.HTTP_ENCODING_PLAIN, htmlData);
            LogHolder.log(LogLevel.ERR, LogType.MISC, e);
        }
        return httpResponse;
    }

    /**
     * Returns a vector containing all unused dynamic mixes. A dynamic mix is
     * considered unused if it is in no cascade at all or if it is used in a
     * cascade only containing itself
     * 
     * @return
     */
    private Vector getUnusedMixex()
    {
        Vector result = new Vector();
        // First the mixes in their own cascade
        Enumeration enumer2 = Database.getInstance(VirtualCascade.class).getEntrySnapshotAsEnumeration();
        MixCascade mixCascade2 = null;
        while (enumer2.hasMoreElements())
        {
            mixCascade2 = ((VirtualCascade) (enumer2.nextElement())).getRealCascade();
            if (mixCascade2.getNumberOfMixes() != 1)
                continue;
            Enumeration enumMixID2 = mixCascade2.getMixIds().elements();
            String mixId = "";
            if (enumMixID2.hasMoreElements())
            {
                mixId = (String) enumMixID2.nextElement();
            }
            else
            {
                // How can that be?
                continue;
            }
            result.add( Database.getInstance(MixInfo.class).getEntryById(mixId));
        }
        
        // Now the rest
        Enumeration en = Database.getInstance(MixInfo.class).getEntrySnapshotAsEnumeration();
        while(en.hasMoreElements())
        {
            MixInfo mix = (MixInfo) en.nextElement();
            if(!mix.isDynamic())
                continue;
            if( getTemporaryCascade(mix.getId()) == null && getCurrentCascade(mix.getId()) == null)
                result.add(mix);
        }
        return result;
    }

    private String getCascadeHtmlTable(Class a_clazz) throws Exception
    {
        String htmlData = ""
                + "    <TABLE ALIGN=\"center\" BORDER=\"0\" width=\"100%\">\n"
                + "      <COLGROUP>\n"
                // + " <COL>\n"
                + "        <COL>\n"
                + "        <COL>\n"
                + "        <COL>\n"
                + "        <COL>\n"
                + "        <COL>\n"
                + "        </COLGROUP>\n"
                + "      <TR>\n"
                // + " <TH>Cascade Name</TH>\n"
                + "        <TH>Cascade ID&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</TH>\n"
                + "        <TH>Mix Host</TH>\n" + "        <TH>Mix Port</TH>\n"
                + "        <TH>Mix Type</TH>\n" + "        <TH>Mix Id</TH>\n" + "      </TR>\n";
        /* get all status entrys from database */
        Enumeration enumer = Database.getInstance(a_clazz).getEntrySnapshotAsEnumeration();
        MixCascade mixCascade = null;
        while (enumer.hasMoreElements())
        {
            if (a_clazz.equals(VirtualCascade.class))
                mixCascade = ((VirtualCascade) (enumer.nextElement())).getRealCascade();
            else
                mixCascade = (MixCascade) enumer.nextElement();

            if (mixCascade.getNumberOfMixes() == 1)
                continue;
            Enumeration enumMixID = mixCascade.getMixIds().elements();
            String mixId = "";
            if (enumMixID.hasMoreElements())
            {
                mixId = (String) enumMixID.nextElement();
            }
            MixInfo mixInfo = (MixInfo) Database.getInstance(MixInfo.class).getEntryById(mixId);
            htmlData += "<TR>\n";
            htmlData += "<TD>" + mixCascade.getId() + "</TD>\n";
            htmlData += "<TD>" + mixInfo.getFirstHostName() + "</TD>\n";
            htmlData += "<TD>" + mixInfo.getFirstPort() + "</TD>\n";
            htmlData += "<TD>" + mixInfo.getTypeAsString() + "</TD>\n";
            htmlData += "<TD>" + mixInfo.getId() + "</TD>\n";
            htmlData += "</TR>\n";
            for (int i = 1; i < mixCascade.getNumberOfMixes() && enumMixID.hasMoreElements(); i++)
            {
                mixId = (String) enumMixID.nextElement();
                mixInfo = (MixInfo) Database.getInstance(MixInfo.class).getEntryById(mixId);
                htmlData += "<TR>\n";
                htmlData += "<TD>" + "</TD>\n";
                htmlData += "<TD>" + mixInfo.getFirstHostName() + "</TD>\n";
                htmlData += "<TD>" + mixInfo.getFirstPort() + "</TD>\n";
                htmlData += "<TD>" + mixInfo.getTypeAsString() + "</TD>\n";
                htmlData += "<TD>" + mixInfo.getId() + "</TD>\n";
                htmlData += "</TR>\n";
            }
            htmlData += "<TR>\n";
            htmlData += "<TD colspann=\"6\">&nbsp;</TD>\n";
            htmlData += "</TR>\n";
        }
        htmlData = htmlData + "    </TABLE><BR><BR>\n";
        return htmlData;
    }
}
