/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import anon.AnonChannel;
import anon.TooMuchDataForPacketException;


/**
 * @author Stefan Lieske
 */
public abstract class AbstractDataChain implements AnonChannel, Observer, Runnable {

  private DataChainInputStreamImplementation m_inputStream;

  private DataChainOutputStreamImplementation m_outputStream;

  private Vector m_messageQueuesNotifications;

  private IDataChannelCreator m_channelCreator;

  private DataChainErrorListener m_errorListener;

  private boolean m_chainClosed;

  private Thread m_downstreamThread;


  private class DataChainOutputStreamImplementation extends OutputStream {

    private boolean m_closed;

    private Object m_internalStreamSynchronization;


    public DataChainOutputStreamImplementation() {
      m_closed = false;
      m_internalStreamSynchronization = new Object();
    }


    public void write(int a_byte) throws IOException {
      byte[] byteAsArray = {(byte)a_byte};
      write(byteAsArray);
    }

    public void write(byte[] a_buffer, int a_offset, int a_length) throws IOException {
      synchronized (m_internalStreamSynchronization) {
        if (m_closed) {
          throw (new IOException("Stream is closed."));
        }
        byte[] dataToSend = new byte[a_length];
        System.arraycopy(a_buffer, a_offset, dataToSend, 0, a_length);
        DataChainSendOrderStructure order = new DataChainSendOrderStructure(dataToSend);
        synchronized (order.getSynchronizationObject()) {
          orderPacket(order);
          /* check whether we have a single-threaded implementation (processing of the
           * order is already done with the call of orderPacket()) or multi-threaded
           * implementation (we have to wait until the processing-thread has processed
           * the order)
           */
          if (!order.isProcessingDone()) {
            /* multi-threaded implementation */
            try {
              order.getSynchronizationObject().wait();
            }
            catch (InterruptedException e) {
              /* Attention: We will not set the number of transferred bytes in the thrown
               * Exception because we can't know it.
               */
              throw (new InterruptedIOException("InterruptedException: " + e.toString()));
            }
          }
          /* processing is done -> check for exceptions */
          if (order.getThrownException() != null) {
            /* throw that exception */
            throw (order.getThrownException());
          }
          /* check whether all bytes could been sent */
          if (order.getProcessedBytes() < a_length) {
            /* throw an exception with the number of processed bytes */
            throw (new TooMuchDataForPacketException(order.getProcessedBytes()));
          }
        }
      }
    }

    public void close() throws IOException {
      if (!m_closed) {
        synchronized (m_internalStreamSynchronization) {
          m_closed = true;
          outputStreamClosed();
        }
      }
    }
  }


  private class DataChainInputStreamImplementation extends InputStream {

    private boolean m_closed;

    private Vector m_queueEntries;


    private DataChainInputStreamImplementation() {
      m_queueEntries = new Vector();
      m_closed = false;
    }


    public int read() throws IOException {
      byte[] buffer = new byte[1];
      int bytesRead = 0;
      do {
        bytesRead = read(buffer);
      } while (bytesRead == 0);
      int returnedByte = -1;
      if (bytesRead == 1) {
        returnedByte = (new ByteArrayInputStream(buffer)).read();
      }
      return returnedByte;
    }

    public int read(byte[] a_buffer, int a_offset, int a_length) throws IOException {
      int bytesRead = 0;
      /* to prevent later exceptions, we check (and - if necessary - alter) offset and
       * length
       */
      if (a_buffer.length < a_offset) {
        a_offset = a_buffer.length;
      }
      if (a_buffer.length < a_offset + a_length) {
        a_length = a_buffer.length - a_offset;
      }
      if (a_length > 0) {
        synchronized (m_queueEntries) {
          if (m_closed) {
            throw (new IOException("Stream is closed."));
          }
          if (m_queueEntries.size() == 0) {
            /* nothing is currently available -> wait for available queue-entries */
            try {
              m_queueEntries.wait();
            }
            catch (InterruptedException e) {
              throw (new InterruptedIOException("InterruptedException: " + e.toString()));
            }
          }
          if (m_queueEntries.size() > 0) {
            /* we have an entry -> read data from there */
            DataChainInputStreamQueueEntry currentEntry = (DataChainInputStreamQueueEntry)(m_queueEntries.firstElement());
            switch (currentEntry.getType()) {
              case DataChainInputStreamQueueEntry.TYPE_STREAM_END: {
                /* end of stream reached -> do nothing */
                bytesRead = -1;
                break;
              }
              case DataChainInputStreamQueueEntry.TYPE_DATA_AVAILABLE: {
                /* data-available entry */
                while ((m_queueEntries.size() > 0) && (currentEntry.getType() == DataChainInputStreamQueueEntry.TYPE_DATA_AVAILABLE) && (bytesRead < a_length)) {
                  /* read some more bytes */
                  int bytesToRead = Math.min(a_length - bytesRead, currentEntry.getData().length - currentEntry.getAlreadyReadBytes());
                  System.arraycopy(currentEntry.getData(), currentEntry.getAlreadyReadBytes(), a_buffer, a_offset + bytesRead, bytesToRead);
                  bytesRead = bytesRead + bytesToRead;
                  currentEntry.setAlreadyReadBytes(currentEntry.getAlreadyReadBytes() + bytesToRead);
                  if (currentEntry.getAlreadyReadBytes() == currentEntry.getData().length) {
                    /* the data of the current entry was read completely -> remove the
                     * entry from the queue
                     */
                    m_queueEntries.removeElementAt(0);
                    if (m_queueEntries.size() > 0) {
                      /* get the next entry */
                      currentEntry = currentEntry = (DataChainInputStreamQueueEntry)(m_queueEntries.firstElement());
                    }
                  }
                }
                break;
              }
              case DataChainInputStreamQueueEntry.TYPE_IO_EXCEPTION: {
                /* we shall throw an IOException */
                IOException exceptionToThrow = currentEntry.getIOException();
                m_queueEntries.removeElementAt(0);
                if (exceptionToThrow != null) {
                  throw exceptionToThrow;
                }
                break;
              }
            }
          }
        }
      }
      return bytesRead;
    }

    public int available() throws IOException {
      int availableBytes = 0;
      synchronized (m_queueEntries) {
        if (m_closed) {
          throw (new IOException("Stream is closed."));
        }
        if (m_queueEntries.size() > 0) {
          int i = 0;
          DataChainInputStreamQueueEntry currentEntry = (DataChainInputStreamQueueEntry)(m_queueEntries.elementAt(i));
          while (currentEntry != null) {
            i++;
            if (currentEntry.getType() == DataChainInputStreamQueueEntry.TYPE_DATA_AVAILABLE) {
              /* we have found a data entry */
              availableBytes = availableBytes + (currentEntry.getData().length - currentEntry.getAlreadyReadBytes());
              /* try to get the next entry */
              if (i < m_queueEntries.size()) {
                currentEntry = (DataChainInputStreamQueueEntry)(m_queueEntries.elementAt(i));
              }
              else {
                /* no more entries in the queue */
                currentEntry = null;
              }
            }
            else {
              /* stop processing of the queue because we have found an non-data entry */
              currentEntry = null;
            }
          }
        }
      }
      return availableBytes;
    }

    public void close() {
      if (!m_closed) {
        synchronized (m_queueEntries) {
          m_closed = true;
          /* clear all entries in the queue */
          m_queueEntries.removeAllElements();
          /* wake up waiting threads (normally there should be no one) */
          m_queueEntries.notifyAll();
        }
      }
    }

    public void addToQueue(DataChainInputStreamQueueEntry a_entry) {
      synchronized (m_queueEntries) {
        boolean addEntry = true;
        if (m_closed) {
          /* only add entries, if the stream isn't closed */
          addEntry = false;
        }
        else {
          if (m_queueEntries.size() > 0) {
            DataChainInputStreamQueueEntry lastEntry = (DataChainInputStreamQueueEntry)(m_queueEntries.lastElement());
            if (lastEntry.getType() == DataChainInputStreamQueueEntry.TYPE_STREAM_END) {
              /* don't add entries after a stream-end */
              addEntry = false;
            }
          }
        }
        if (addEntry) {
          m_queueEntries.addElement(a_entry);
          /* wake up a waiting thread */
          m_queueEntries.notify();
        }
      }
    }
  }


  public AbstractDataChain(IDataChannelCreator a_channelCreator, DataChainErrorListener a_errorListener) {
    m_channelCreator = a_channelCreator;
    m_errorListener = a_errorListener;
    m_inputStream = new DataChainInputStreamImplementation();
    m_outputStream = new DataChainOutputStreamImplementation();
    m_messageQueuesNotifications = new Vector();
    m_chainClosed = false;
    m_downstreamThread = new Thread(this, "AbstractDataChain: Downstream-Organizer Thread");
    m_downstreamThread.setDaemon(true);
    m_downstreamThread.start();
  }


  public InputStream getInputStream() {
    return m_inputStream;
  }

  public OutputStream getOutputStream() {
    return m_outputStream;
  }


  public void close() {
    if (!m_chainClosed) {
      /* First: Prevent loops which may occur when closing the output-stream (calls of
       *        outputStreamClosed() could re-call this method).
       */
      m_chainClosed = true;
      try {
        getOutputStream().close();
      }
      catch (IOException e) {
      }
      try {
        getInputStream().close();
      }
      catch (IOException e) {
      }
      closeDataChain();
      /* wait for stop of the downstream thread after all channels are closed */
      try {
        m_downstreamThread.join();
      }
      catch (InterruptedException e) {
      }
    }
  }

  public void update(Observable a_observedObject, Object a_message) {
    if (a_observedObject instanceof InternalChannelMessageQueue) {
      synchronized (m_messageQueuesNotifications) {
        m_messageQueuesNotifications.addElement(a_observedObject);
        m_messageQueuesNotifications.notify();
      }
    }
  }


  protected Vector getMessageQueuesNotificationsList() {
    return m_messageQueuesNotifications;
  }

  protected void addInputStreamQueueEntry(DataChainInputStreamQueueEntry a_entry) {
    m_inputStream.addToQueue(a_entry);
  }

  protected AbstractDataChannel createDataChannel() {
    return m_channelCreator.createDataChannel(this);
  }

  protected void interruptDownstreamThread() {
    m_downstreamThread.interrupt();
  }

  protected void propagateConnectionError() {
    m_errorListener.dataChainErrorSignaled();
  }


  public abstract int getOutputBlockSize();

  public abstract void createPacketPayload(DataChainSendOrderStructure a_order);

  public abstract void run();


  protected abstract void orderPacket(DataChainSendOrderStructure a_order);

  protected abstract void outputStreamClosed() throws IOException;

  protected abstract void closeDataChain();

}
