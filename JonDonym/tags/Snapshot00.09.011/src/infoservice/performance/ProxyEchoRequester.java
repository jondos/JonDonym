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
package infoservice.performance;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This class sends data to the output stream of a certain socket.
 * It implements runnable to allow asynchronous sending, e.g. already listening for the response
 * while the data are still being sent.
 * If any error occurs while sending, the thread will die. The exception that was thrown
 * can be accessed through the getException() method.
 *
 * @author oliver
 *
 */
class ProxyEchoRequester implements Runnable {

	private byte[] dataToSend;
	private Socket socket;
	private Throwable exception;

	/**
	 * Constructs a new <code>ProxyEchoRequester</code> object.
	 * @param pSocket the socket to send to
	 * @param pDataToSend the data to be sent
	 */
	public ProxyEchoRequester(Socket pSocket, byte[] pDataToSend) {
		dataToSend = pDataToSend;
		socket = pSocket;
	}

	/**
	 * Opens the output stream of the socket given to the constructor and sends
	 * the data through it. If any exception occurs, the method will terminate and
	 * save the exception where it can be retrieved by {@link #getException()}.
	 *
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		OutputStream stream = null;
		try {
			stream = socket.getOutputStream();
			stream.write(dataToSend);
			stream.close();
		} catch (IOException e) {
			exception = e;
			try {
				if(stream != null)
					stream.close();
			} catch (IOException e1) {
				exception = e1;
			}
		}
	}

	/**
	 * @return The exception that was last thrown while the {@link ProxyEchoRequester#run()} method was executed, or <code>null</code> if no error occurred.
	 */
	public Throwable getException() {
		return exception;
	}

}
