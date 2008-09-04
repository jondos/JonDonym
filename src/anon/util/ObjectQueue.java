/*
 Copyright (c) 2007, JonDos GmbH
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the JonDos GmbH nor the names of its contributors
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
package anon.util;

/**
 * This is a generic object queue.
 * 
 * @author Rolf Wendolsky
 */
public class ObjectQueue
	{
		private QueueItem m_head = null;
		private QueueItem m_foot = null;
		private int m_size = 0;

		private final class QueueItem
			{
				private Object m_object;
				private QueueItem m_previous = null;

				public QueueItem(Object a_object)
					{
						m_object = a_object;
					}
			}

		public int getSize()
			{
				return m_size;
			}

		/** Put an object into the queue */
		public synchronized void push(Object a_object)
			{
				QueueItem newItem = new QueueItem(a_object);
				m_size++;

				if (m_head == null)
					{
						// there is no item im the queue
						m_head = newItem;
						m_foot = newItem;
					}
				else
					{
						// there is more than one item in the queue
						m_head.m_previous = newItem;
						m_head = newItem;
					}
			}

		/** Get an object from the queue. If the queue is empty returns null */
		public synchronized Object pop()
			{
				Object object;
				if (m_head == null)
					{
						// there is not item in the queue
						return null;
					}
				else if (m_head == m_foot)
					{
						// there is only one object in the queue
						object = m_foot.m_object;
						m_head = null;
						m_foot = null;
					}
				else
					{
						// there is more than one object in the queue
						object = m_foot.m_object;
						m_foot = m_foot.m_previous;

					}
				m_size--;
				return object;
			}
		
		/** Takes the head of the queue. Waits until an element is in the queue if necessary*/
		public Object take() throws InterruptedException
			{
				while(true)
					{
						Object o=pop();
						if(o!=null)
							return o;
						Thread.sleep(100);
					}
			}
		
		/** Pools the queue for an object to become available until timeout is reached. returns null if timneout was reached*/
		public Object poll(int a_msTimeout) throws InterruptedException
			{
				Object o=pop();
				if(o!=null)
					return o;
				Thread.sleep(a_msTimeout);
				return pop();
			}
		
		public synchronized boolean isEmpty()
		{
				return (m_size==0);
		}

	}
