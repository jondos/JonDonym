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
package infoservice.agreement.multicast;

import infoservice.Configuration;
import infoservice.HttpResponseStructure;
import infoservice.InfoServiceDistributor;
import infoservice.agreement.IInfoServiceAgreementAdapter;
import infoservice.agreement.common.FifoQueue;
import infoservice.agreement.logging.AgreementFileLog;
import infoservice.agreement.multicast.interfaces.IAgreementMessage;
import infoservice.agreement.multicast.messages.RawMessage;
import infoservice.dynamic.DynamicCascadeConfigurator;
import infoservice.dynamic.VirtualCascade;

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
import anon.infoservice.IDistributable;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.ListenerInterface;

/**
 * @author LERNGRUPPE An adapter for the infoservice to give them the ability to
 *         handle the agreement protocol.
 *
 */
public class InfoserviceEMCAdapter extends AbstractEMCAdapter implements
        IInfoServiceAgreementAdapter
{
    /**
     * A cascade configurator which holds logic for wireing mixes to cascades.
     * He uses the agreement result, the common random number, to do that.
     */
    private DynamicCascadeConfigurator m_dynamicMixConfigurator = new DynamicCascadeConfigurator();

    /**
     * Holds the number of all infoservices and freeze it as long as the
     * agreement runs.
     */
    protected int m_numberOfAllActiveInfoservices = 0;

    /**
     * Holds an freeze the infoservice object which are current existing as long
     * as the agreement runs.
     */
    Hashtable m_infoServiceSnapshot = new Hashtable();

    /**
     * A data stack which implements fifo logic used for holding messages.
     */
    private FifoQueue m_queue = new FifoQueue();

    /**
     * The database entry representation for this infoservice.
     */
    InfoServiceDBEntry m_self = null;

    /**
     * A thread which executes the agreement logic.
     */
    protected Thread m_agreementStarter = null;

    /**
     * A thread which handles messages for performance reasons.
     */
    private MessageHandlerThread m_messageHandler;

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
     * Creates a new <code>InfoserviceAgreementAdapter</code>. This adapter
     * is used to connect the real InfoService to the agreement extension.
     *
     * @param a_cmds
     *            The connection object to serve network and communication.
     */
    public InfoserviceEMCAdapter()
    {
        super(new AgreementFileLog());
        m_self = generateInfoServiceSelf();
        this.setIAgreementHandler(new EchoMulticastAgreementHandlerImpl(this));
        new Thread("InfoServiceEMCAdapter")
        {
            public void run()
            {
                try
                {
                    Thread.sleep(20000);
                }
                catch (InterruptedException e)
                {
                    error("Unable to sleep in thread");
                }
                evangelizeThisInfoservice();
                try
                {

                    Thread.sleep(20000);

                }
                catch (InterruptedException e)
                {
                    error("Unable to sleep in thread");
                }
                startAdapter();
            }
        }.start();
    }

    /**
     * Takes care that all infoservices know each other.
     *
     */
    protected void evangelizeThisInfoservice()
    {
        InfoServiceDBEntry generatedOwnEntry = generateInfoServiceSelf();
        Database.getInstance(InfoServiceDBEntry.class).update(generatedOwnEntry);
        InfoServiceDistributor.getInstance().addJobToInititalNeighboursQueue(generatedOwnEntry);
    }

    /**
     * Handles the messages coming in through the /agreement-command. The
     * a_postData is parsed to an IAgreementMessage and then put into the
     * message queue. Messages are only accepted if an agreement is currently
     * running or we are in the timeframe to start an new one
     *
     * @param a_postData
     *            The post data of the request containing an XML encoded
     *            IAgreementMessage
     * @return HTTP_RETURN_INTERNAL_SERVER_ERROR if the message could not be
     *         parsed or the time was not right, HTTP_RETURN_OK otherwise
     */
    public HttpResponseStructure handleMessage(byte[] a_postData)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_OK);

        RawMessage msg = new RawMessage(a_postData);

        synchronized (m_queue)
        {
            m_queue.push(msg);
            m_queue.notify();
        }
        return httpResponse;
    }

    /**
     * Starts the cascade building process.
     */
    protected void buildCascades(long l)
    {
        m_dynamicMixConfigurator.buildCascades(l);
    }

    /**
     * Make sure we have "real" information about our neighbours (and they about
     * us) once. Might not be needed in real world, but overcomes some problems
     * at concurrent startup of multiple InfoServices (InfoServicePropagandist
     * is too fast then and we don't want to wait 10 minutes)
     */
    protected void prepareStart()
    {
        /* Remove all InfoServiceDBEntries which have no valid IDs */
        Enumeration en = Database.getInstance(InfoServiceDBEntry.class)
                .getEntrySnapshotAsEnumeration();
        info("MY OWN ID is " + m_self.getId());
        while (en.hasMoreElements())
        {
            InfoServiceDBEntry entry = (InfoServiceDBEntry) en.nextElement();
            if (!entry.checkId() && !m_self.equals(entry))
            {
                info("Discarting " + entry.getId() + " because its a dummy");
                Database.getInstance(InfoServiceDBEntry.class).remove(entry);
            }
            else
            {
                info("Using InfoService " + entry.getId());
            }
        }

        /*
         * Build a snapshot of the currently known InfoServices to prevent
         * InfoServices coming online during the execution to get messages of
         * the current round
         */
        Enumeration enInfoServices = Database.getInstance(InfoServiceDBEntry.class)
                .getEntrySnapshotAsEnumeration();

        while (enInfoServices.hasMoreElements())
        {
            InfoServiceDBEntry current = (InfoServiceDBEntry) enInfoServices.nextElement();
            m_infoServiceSnapshot.put(current.getId(), current);
        }
        this.m_numberOfAllActiveInfoservices = m_infoServiceSnapshot.size();

        if (m_messageHandler == null)
        {
            throw new NullPointerException(this.getClass().getName()
                    + ": Message is null. Can't start agreement!");

        }
        m_messageHandler.start();
        /* Forget all TemporaryCascades if there are some */
        Database.getInstance(VirtualCascade.class).removeAll();
    }

    /**
     * Stops the running message handler if necessary and creates a new one.
     */
    public void startListening()
    {
        if (m_messageHandler != null)
        {
            synchronized (m_queue)
            {
                m_messageHandler.setRunning(false);
                m_queue.notify();
            }
        }
        m_messageHandler = new MessageHandlerThread(this, m_queue);
    }

    /**
     * Returns the freezed number of all infoservices which take part on the
     * agreement protocol.
     */
    public int getNumberOfAllInfoservices()
    {
        return this.m_numberOfAllActiveInfoservices;
    }

    /**
     * Returns a unique identifer for this infoservice.
     */
    public String getIdentifier()
    {
        return m_self.getId();
    }

    /**
     * Sends a message to a specified infoservice.
     *
     * @param a_id
     *            The id of the receiver.
     * @param a_message
     *            The message to send.
     */
    public void sendMessageTo(final String a_id, final IAgreementMessage a_message)
    {
        if (a_id.equals(m_self.getId()))
            return;
        /* Send the message asynchronously */
        new Thread("InfoServiceEMCAdapter - sendMessageTo()")
        {
            public void run()
            {
                sendToInfoService((InfoServiceDBEntry) m_infoServiceSnapshot.get(a_id), a_message);
            }
        }.start();
    }

    /**
     * Sends a message to all known infoservices.
     *
     * @param a_message
     *            The message to send.
     */
    public void multicastMessage(final IAgreementMessage a_message)
    {
        Enumeration infoServices = this.m_infoServiceSnapshot.elements();
        while (infoServices.hasMoreElements())
        {
            final InfoServiceDBEntry entry = (InfoServiceDBEntry) infoServices.nextElement();
            if (entry.getId().equals(m_self.getId()))
                continue;

            /* Send the message asynchronously */
            new Thread("InfoServiceEMCAdapter - multicastMessage")
            {
                public void run()
                {
                    sendToInfoService(entry, a_message);
                }
            }.start();
        }
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
    boolean sendToInfoService(InfoServiceDBEntry a_infoservice, IDistributable a_message)
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
}
