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
package infoservice.agreement.paxos.integration;

import infoservice.Configuration;
import infoservice.InfoServiceDistributor;
import infoservice.agreement.IInfoServiceAgreementAdapter;
import infoservice.agreement.common.AgreementConstants;
import infoservice.agreement.logging.AgreementFileLog;
import infoservice.agreement.paxos.IPaxosTarget;
import infoservice.agreement.paxos.messages.CollectMessage;
import infoservice.agreement.paxos.messages.FreezeProofMessage;
import infoservice.agreement.paxos.messages.PaxosMessage;
import infoservice.dynamic.DynamicCascadeConfigurator;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import anon.infoservice.Database;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.HttpResponseStructure;
import anon.infoservice.IDistributable;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.ListenerInterface;

/**
 * @author ss1
 * 
 */
public class InfoServicePaxosAdapter extends PaxosAdapter implements IInfoServiceAgreementAdapter
{

    /**
     * A cascade configurator which holds logic for wireing mixes to cascades.
     * He uses the agreement result, the common random number, to do that.
     */
    private DynamicCascadeConfigurator m_dynamicMixConfigurator = new DynamicCascadeConfigurator();

    InfoServiceDBEntry m_self;

    /**
     * Creates a new InfoServicePaxosAdapter and prepares for the first
     * agreement
     */
    public InfoServicePaxosAdapter()
    {
        super();
        setLog(new AgreementFileLog());
        // getLog().setLogLevel(IAgreementLog.LOG_INFO);
        m_self = generateInfoServiceSelf();
        this.m_lastRandom = AgreementConstants.DEFAULT_COMMON_RANDOM;
        new Thread()
        {
            public void run()
            {
                /*
                 * Make sure we have "real" information about our neighbours
                 * (and they about us) once. Might not be needed in real world,
                 * but overcomes some problems at concurrent startup of multiple
                 * InfoServices (InfoServicePropagandist is too fast then and we
                 * don't want to wait 10 minutes)
                 */
                try
                {
                    Thread.sleep(20000);
                }
                catch (InterruptedException e)
                {
                    error("Unable to sleep in thread");
                }
                InfoServiceDBEntry generatedOwnEntry = generateInfoServiceSelf();
                Database.getInstance(InfoServiceDBEntry.class).update(generatedOwnEntry);
                InfoServiceDistributor.getInstance().addJobToInititalNeighboursQueue(
                        generatedOwnEntry);
                info("Now we are ready to start the agreement");
            }
        }.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.paxos.integration.PaxosAdapter#prepareTargets()
     */
    protected void prepareTargets()
    {
        m_targets = new Hashtable();
        info("OWN ID IS: " + m_self.getId());
        Enumeration en = Database.getInstance(InfoServiceDBEntry.class)
                .getEntrySnapshotAsEnumeration();
        while (en.hasMoreElements())
        {
            InfoServiceDBEntry entry = (InfoServiceDBEntry) en.nextElement();
            InfoServicePaxosTarget tmp = new InfoServicePaxosTarget(entry);
            if (!entry.checkId() && !m_self.equals(entry))
            {
                info("Discarting " + entry.getId() + " because its a dummy");
            }
            else
            {
                info("Using InfoService " + entry.getId());
                // debug("Using InfoService " + tmp.getId() + " "+ tmp.getId()
                // );
                m_targets.put(tmp.getId(), tmp);
            }
        }
    }

    /**
     * Generates an <code>InfoServiceDBEntry</code> for this InfoService
     * 
     * @return The <code>InfoServiceDBEntry</code>
     */
    InfoServiceDBEntry generateInfoServiceSelf()
    {
        Vector virtualListeners = Configuration.getInstance().getVirtualListeners();
        return new InfoServiceDBEntry(Configuration.getInstance().getOwnName(), Configuration
                .getInstance().getID(), virtualListeners, Configuration.getInstance()
                .holdForwarderList(), false, System.currentTimeMillis(), System.currentTimeMillis(), Configuration.getInstance().isPerfServerEnabled());
    }

    /**
     * Handles the given POST data
     * 
     * @param a_postData
     *            The POST data to be handled
     * @return OK every time
     */
    public HttpResponseStructure handleMessage(byte[] a_postData)
    {
        final PaxosMessage msg = InfoServiceMessageFactory.decode(a_postData);
        handleIncommingMessage(msg);
        return new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.paxos.PaxosCommunicator#getIdentifier()
     */
    protected String getIdentifier()
    {
        return m_self.getId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.paxos.PaxosCommunicator#sendMessage(infoservice.agreement.paxos.messages.PaxosMessage,
     *      infoservice.agreement.paxos.IPaxosTarget)
     */
    public void sendMessage(final PaxosMessage a_msg, final IPaxosTarget a_target)
    {
        new Thread()
        {
            public void run()
            {
                InfoServiceDBEntry entry = ((InfoServicePaxosTarget) a_target).getInfoService();
                IDistributable msg = null;
                if (a_msg instanceof CollectMessage)
                    msg = new InfoServiceCollectMessage((CollectMessage) a_msg);
                else if (a_msg instanceof FreezeProofMessage)
                    msg = new InfoServiceFreezeProofMessage((FreezeProofMessage) a_msg);
                else
                    msg = new InfoServicePaxosMessage(a_msg);

                sendToInfoService(entry, msg);
            }
        }.start();
    }

    /**
     * Encapsulates logic for sending a message to a infoservice.
     * 
     * @param a_infoservice
     *            The target infoservice.
     * @param postFile
     * @param postData
     * @return Success if <code>true</code>.
     */
    protected boolean sendToInfoService(InfoServiceDBEntry a_infoservice, IDistributable a_message)
    {
        /**
         * @todo Copied from InfoServiceDistributor, maybe make it public
         * there...
         */
        boolean connected = false;
        if (a_infoservice == null)
        {
            return false;
        }
        Enumeration enumer = a_infoservice.getListenerInterfaces().elements();
        while ((enumer.hasMoreElements()) && (connected == false))
        {
            ListenerInterface currentInterface = (ListenerInterface) (enumer.nextElement());
            if (currentInterface.isValid())
            {
                if (sendToInterface(currentInterface, a_message))
                {
                    connected = true;
                }
                else
                {
                    currentInterface.setUseInterface(false);
                }
            }
        }
        return connected;
    }

    /**
     * Encapsulates logic for sending a message to a specified listenere
     * interface.
     * 
     * @param a_listener
     *            The network interface.
     * @param postFile
     * @param postData
     * @return Success if <code>true</code>.
     */
    private boolean sendToInterface(ListenerInterface a_listener, IDistributable a_message)
    {
        /**
         * @todo Copied from InfoServiceDistributor, maybe make it public
         * there...
         */
        boolean connected = true;
        HTTPConnection connection = null;
        try
        {
            connection = HTTPConnectionFactory.getInstance().createHTTPConnection(a_listener,
                    a_message.getPostEncoding(), false);
            HTTPResponse response = connection.Post(a_message.getPostFile(), a_message
                    .getPostData());
            int statusCode = response.getStatusCode();
            connected = (statusCode >= 200 && statusCode <= 299);
        }
        catch (Exception e)
        {
            LogHolder.log(LogLevel.EMERG, LogType.NET, "ERROR WHILE SENDING TO "
                    + connection.getHost() + ":" + connection.getPort() + ": " + e.toString());
            connected = false;
        }
        if (connection != null)
        {
            connection.stop();
        }
        return connected;
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.paxos.integration.PaxosAdapter#useAgreement(long)
     */
    protected void useAgreement(long a_agreement)
    {
        m_dynamicMixConfigurator.buildCascades(a_agreement);
    }

    public void multicast(PaxosMessage a_msg)
    {

        IDistributable tmp = null;
        if (a_msg instanceof CollectMessage)
            tmp = new InfoServiceCollectMessage((CollectMessage) a_msg);
        else if (a_msg instanceof FreezeProofMessage)
            tmp = new InfoServiceFreezeProofMessage((FreezeProofMessage) a_msg);
        else
            tmp = new InfoServicePaxosMessage(a_msg);

        final IDistributable msg = tmp;
        msg.getPostData();
        Enumeration en = m_targets.keys();
        while (en.hasMoreElements())
        {
            final IPaxosTarget target = (IPaxosTarget) m_targets.get(en.nextElement());
            final InfoServiceDBEntry entry = ((InfoServicePaxosTarget) target).getInfoService();
            new Thread()
            {
                public void run()
                {
                    sendToInfoService(entry, msg);
                    // sendMessage(a_msg, target);
                }
            }.start();
        }
    }
}
