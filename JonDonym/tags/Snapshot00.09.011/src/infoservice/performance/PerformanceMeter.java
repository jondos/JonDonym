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

import java.io.InputStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.Random;

import anon.infoservice.Database;
import anon.infoservice.MixCascade;

/**
 * A simple performance meter for Mix cascades.<br>
 * The meter runs as a thread inside the <code>InfoService</code> and periodically sends requests
 * to the ECHO port of each known cascade's exit point. The delay (the time between sending the first byte
 * and receiving the first byte of the response) and the throughput (the data rate of the response in bytes
 * per millisecond) are measured and set to the corresponding cascade.
 * @see anon.infoservice.MixCascade#setDelay(long)
 * @see anon.infoservice.MixCascade#setThroughput(double)
 *
 * @author oliver
 *
 */
public class PerformanceMeter implements Runnable {

	public static final int BYTES_PER_CHUNK = 512;

	private int interval;
	private int requestsPerInterval;

	private byte[] testdata;

	/**
	 * @param pTestDataSize
	 * @param pInterval
	 * @param pRequestsPerInterval
	 */
	public PerformanceMeter(int pTestDataSize, int pInterval, int pRequestsPerInterval) {
		interval = pInterval;
		requestsPerInterval = pRequestsPerInterval;
		testdata = new byte[pTestDataSize];
		new Random().nextBytes(testdata);
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {

		byte[] recvbuf = new byte[testdata.length];

		for(int i=0;i<requestsPerInterval;i++) {
	        Iterator knownMixCascades = Database.getInstance(MixCascade.class).getEntryList().iterator();
	        while(knownMixCascades.hasNext()) {
	        	try {
		        	MixCascade cascade = (MixCascade) knownMixCascades.next();
		        	long delay = -1;
		        	double throughput = 0.0;
		        	Socket s = getExitPointSocket(cascade);
		        	ProxyEchoRequester requester = new ProxyEchoRequester(s, testdata);
		        	Thread requesterThread = new Thread(requester);

		        	long transferInitiatedTime = System.currentTimeMillis();

		        	requesterThread.start();
		        	InputStream in = s.getInputStream();
		        	recvbuf[0] = (byte) in.read();

		        	long responseStartTime = System.currentTimeMillis();

		        	int recvAmount = in.read(recvbuf, 0, recvbuf.length - 1) + 1;

		        	long responseEndTime = System.currentTimeMillis();

		        	int j = 0;
		        	while(j<recvbuf.length && testdata[j] != recvbuf[j++]);
		        	if(i>=recvbuf.length) {
	        			delay = responseStartTime - transferInitiatedTime;
	        			throughput = testdata.length / (responseEndTime - responseStartTime);
		        	}
		        	requesterThread.join();
		        	cascade.setDelay(delay);
		        	cascade.setThroughput(throughput);
	        	}
	        	catch(Exception e) {
	        		// TODO: Log exception
	        	}
	        }
		}
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
			// TODO do proper logging
			e.printStackTrace();
		}
	}


	private Socket getExitPointSocket(MixCascade cascade) {
		// TODO Auto-generated method stub
		return null;
	}
}
