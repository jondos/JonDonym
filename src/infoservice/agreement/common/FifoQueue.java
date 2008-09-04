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
package infoservice.agreement.common;

/**
 * 
 * @author LERNGRUPPE A first in first out queue used for holding zipped
 *         messages.
 * 
 */
public class FifoQueue
{
    /**
     * An array for holding the messages.
     */
    private Object[] m_messages = new Object[10];

    /**
     * An index pointer.
     */
    private int m_writeIndex = 0;

    /**
     * Push a message into the stack.
     * 
     * @param a_message
     *            A message to push.
     */
    public synchronized void push(Object a_message)
    {
        if (m_writeIndex >= m_messages.length)
        {
            Object[] tmp = new Object[m_messages.length + 10];
            System.arraycopy(m_messages, 0, tmp, 0, m_messages.length);
            m_messages = new Object[tmp.length];
            System.arraycopy(tmp, 0, m_messages, 0, tmp.length);
            push(a_message);
        }
        else
        {
            m_messages[m_writeIndex] = a_message;
            m_writeIndex++;
        }
    }

    /**
     * Get a message from the stack.
     * 
     * @return The message.
     */
    public synchronized Object pop()
    {
        Object result = m_messages[0];
        if (m_messages.length > 10)
        {
            Object[] tmp = new Object[m_messages.length - 1];
            System.arraycopy(m_messages, 1, tmp, 0, m_messages.length - 1);
            m_messages = new Object[tmp.length];
            System.arraycopy(tmp, 0, m_messages, 0, tmp.length);
        }
        else
        {
            System.arraycopy(m_messages, 1, m_messages, 0, m_messages.length - 1);
        }
        if (m_writeIndex > 0)
            m_writeIndex--;
        for (int i = m_writeIndex; i < m_messages.length; i++)
        {
            m_messages[i] = null;
        }
        return result;
    }

    /**
     * Clears the stack.
     * 
     */
    public void clear()
    {
        m_messages = new Object[10];
    }

    /**
     * Count the entries.
     * 
     * @return Number of entries hold in the queue.
     */
    public int getMessageCount()
    {
        int count = 0;
        for (int i = 0; i < m_messages.length; i++)
        {
            if (m_messages[i] != null)
                count++;
        }
        return count;
    }
}
