/*
 Copyright (c) 2000 - 2006, The JAP-Team
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

import infoservice.HttpResponseStructure;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.SignatureVerifier;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class DynamicNetworkingHelper
{

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
     * Constructs the answer for a connectivity request. It is an XML structure
     * of the following type: <code>
     * @param a_response
     * @return
     */
    private Document constructAnswer(String a_response)
    {
        Document result = XMLUtil.createDocument();
        Node nodeConnectivity = result.createElement("Connectivity");
        Node nodeResult = result.createElement("Result");
        result.appendChild(nodeConnectivity);
        nodeConnectivity.appendChild(nodeResult);
        XMLUtil.setValue(nodeResult, a_response);
        return result;
    }

    private boolean isReachable(InetAddress a_Address, int a_port)
    {
        // Construct the echoRequest to send
        Random rand = new Random();
        long echoRequest = Math.abs(rand.nextLong());

        String result = doPing(a_Address, a_port, echoRequest);
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
     * @param a_port
     *            The target port
     * @param a_echoRequest
     *            The echoRequest to send
     * @return The echoResponse from the mix
     */
    private String doPing(InetAddress a_Address, int a_port, long a_echoRequest)
    {
        StringBuffer result = new StringBuffer();
        try
        {
            Socket socket = new Socket(a_Address, a_port);
            BufferedOutputStream str = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());

            byte[] content = Long.toString(a_echoRequest).getBytes();

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

}
