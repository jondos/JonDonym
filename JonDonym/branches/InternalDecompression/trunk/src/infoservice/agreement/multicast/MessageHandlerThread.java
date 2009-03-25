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

import infoservice.agreement.common.FifoQueue;
import infoservice.agreement.multicast.messages.RawMessage;

public class MessageHandlerThread extends Thread
{

    private boolean m_bRunning = true;

    private AbstractEMCAdapter m_agreementHandler = null;

    private FifoQueue m_queue = null;

    /**
     * LERNGRUPPE Creates a new <code>MessageHandler</code>. When notified it
     * will pop a message off the given queue and call the given handler to
     * handle it.
     *
     * @param a_handler
     *            The ArgreementHandler to handle the messages
     * @param a_queue
     *            A queue of messages
     */
    public MessageHandlerThread(AbstractEMCAdapter a_handler, FifoQueue a_queue)
    {
		super("infoservice.agreement.multicast.MessageHandlerThread");
        this.m_agreementHandler = a_handler;
        this.m_queue = a_queue;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        while (m_bRunning)
        {
            RawMessage msg = null;
            synchronized (m_queue)
            {
                msg = (RawMessage) m_queue.pop();
            }
            while (msg != null)
            {
                m_agreementHandler.handleMessage(msg.getAgreementMessage());
                synchronized (m_queue)
                {
                    msg = (RawMessage) m_queue.pop();
                }
            }
            synchronized (m_queue)
            {
                try
                {
                    m_queue.wait();
                }
                catch (InterruptedException e)
                {
                    // can be ignored
                }
            }
        }
    }

    /**
     * LERNGRUPPE Sets the m_bRunning member
     *
     * @param a_bRunning
     *            the new value of m_bRunning
     */
    public void setRunning(boolean a_bRunning)
    {
        this.m_bRunning = a_bRunning;
    }

}
