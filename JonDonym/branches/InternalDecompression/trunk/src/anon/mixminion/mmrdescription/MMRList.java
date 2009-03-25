/*
 Copyright (c) 2004, The JAP-Team
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
package anon.mixminion.mmrdescription;

import java.io.LineNumberReader;
import java.util.Hashtable;
import java.util.Vector;

import anon.crypto.MyRandom;
import anon.mixminion.mmrdescription.MMRDescription;
import anon.util.Base64;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;


public class MMRList
{

	private Vector m_mixminionrouters; // all Routers
	private Vector m_exitnodes; //nodes with smtp-availability
	private Vector m_fragexitnodes; //nodes with frag and exit availability
	private Hashtable m_mixminionroutersWithNames;
	private MyRandom m_rand;
	private MMRListFetcher m_mmrlistFetcher;

	/**
	 * constructor
	 *
	 */
	public MMRList(MMRListFetcher fetcher)
	{
		m_mixminionrouters = new Vector();
		m_fragexitnodes = new Vector();
		m_exitnodes = new Vector();
		m_mixminionroutersWithNames = new Hashtable();

		m_mmrlistFetcher = fetcher;
		m_rand = new MyRandom();

	}

	/**
	 * size of the MMRList
	 * @return
	 * number of routers in the list
	 */
	public synchronized int size()
	{
		return m_mixminionrouters.size();
	}

	public synchronized void setFetcher(MMRListFetcher fetcher)
	{
		m_mmrlistFetcher = fetcher;
	}

	/** Updates the list of available MMRouters.
	 * @return true if it was ok, false otherwise
	 */
	public synchronized boolean updateList()
	{
		try
		{
			byte[] doc = m_mmrlistFetcher.getMMRList();

			if (doc == null)
			{
				return false;
			}
			return parseDocument(doc);
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "There was a problem with fetching the available MMRouters: " + t.getMessage());
		}
		return false;
	}

	/**
	 * returns a List of all Mixminionrouters
	 * @return
	 * List of MMRDescriptions
	 */
	public Vector getList()
	{
		return (Vector) m_mixminionrouters;
	}

	/**
	 * gets an Mixminion router by it's name
	 * @param name
	 * name of the MMR
	 * @return
	 * MMRDescription of the onion router
	 */
	public synchronized MMRDescription getByName(String name)
	{
		return (MMRDescription) m_mixminionroutersWithNames.get(name);
	}

	/**
	 * removes an Mixminion router
	 * @param name
	 * name of the MMR
	 */
	public synchronized void remove(String name)
	{
		MMRDescription mmrd = getByName(name);
		m_mixminionrouters.removeElement(mmrd);
			m_exitnodes.removeElement(mmrd);
		m_mixminionroutersWithNames.remove(name);

	}

	/**
	 * selects a MMR randomly from a given list of allowed OR names
	 * @param mmrlist list of mixminionrouter names
	 * @return
	 */
	public synchronized MMRDescription getByRandom(Vector allowedNames)
	{
		return (MMRDescription) allowedNames.elementAt( (m_rand.nextInt(allowedNames.size())));
	}

	/**
	 * selects a MMR randomly
	 * @return
	 */
	public synchronized MMRDescription getByRandom()
	{
		return (MMRDescription) this.m_mixminionrouters.elementAt( (m_rand.nextInt(m_mixminionrouters.size())));
	}





	/**
	 * selects a Routing List randomly, last element is surely an exit-node
	 * tries to blanace the probability of exit and non-exit nodes
	 * @param hops int
	 * length of the circuit
	 * @return routers vector
	 */

	public synchronized Vector getByRandomWithExit(int hops)
	{
		Vector routers = new Vector();
		MMRDescription x = null;
		boolean contains = true;

		for (int i=0; i<hops-1; i++)
		{
			contains=true;
			int abbruch=0;
			while (contains && (abbruch != 10) )
			{
				abbruch++;
				x = getByRandom();
				contains = routers.contains(x);
			}
			routers.addElement(x);
		}

		contains = true;
		int abbruch = 0;
			while (contains && (abbruch != 10))
			{
				abbruch++;
				x = getByRandom(m_exitnodes);
				contains = routers.contains(x);
			}
		routers.addElement(x);
		return routers;
	}

	public synchronized Vector getByRandomWithFrag(int hops, int frags)
	{
		Vector routes = new Vector();
		Vector route = null;
		MMRDescription temp = null;
		MMRDescription exit = null;
		boolean contains = true;

		exit = getByRandom(m_fragexitnodes);

		for (int i = 0; i < frags; i++)
		{
			route = new Vector();
			for (int j = 0; j < hops-1; j++)
			{
				contains=true;
				while (contains)
				{
					temp = getByRandom();
					contains = route.contains(temp);
				}
				route.addElement(temp);
			}
			route.addElement(exit);
			//test
			//routes.addElement(mytesting());
			//end
			routes.addElement(route);
		}

		return routes;
	}

	/**
	 * returns a MMRDescription to the given MMRName
	 * @param name
	 * MMRName
	 * @return
	 * MMRDescription if the MMR exist, null else
	 */
	public synchronized MMRDescription getMMRDescription(String name)
	{
		if (this.m_mixminionroutersWithNames.containsKey(name))
		{
			return (MMRDescription)this.m_mixminionroutersWithNames.get(name);
		}
		return null;
	}


	/**
	 * parses the document and creates a list with all MMRDescriptions
	 * @param strDocument
	 * @throws Exception
	 * @return false if document is not a valid directory, true otherwise
	 */

	private boolean parseDocument(byte[] document) throws Exception
	{
		Vector mmrs = new Vector();
		Vector enodes = new Vector();
		Vector fnodes = new Vector();

		Hashtable mmrswn = new Hashtable();
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(new ByteArrayInputStream(document)));
		String aktLine = reader.readLine();

		//down servers
		ServerStats servSt = new ServerStats();
		Vector downServers = servSt.getWhoIsDown();

		if (aktLine == null)
			{
				return false;
			}
			
		
		for (; ; )
		{
			aktLine = reader.readLine();
			if (aktLine == null )
			{
				break;
			}


			if (aktLine.startsWith("[Server]"))
			{
				
				MMRDescription mmrd = MMRDescription.parse(reader);
				if ((mmrd != null)  && !downServers.contains(mmrd.getName()))
				{
					//check mmrswn.containsKey(mmrd.getName()) and we only use 0.0.7.1  0.0.8alpha2
					boolean addme = true;//(mmrd.getSoftwareVersion().intern() == "Mixminion 0.0.8alpha2");
					if (mmrswn.containsKey(mmrd.getName()))
						{
//						//sometimes there are entries with the same nicks, i suppose that the later the entries
//						//are in the list the newer they are
//						//so i remove the old one...
//						MMRDescription old = (MMRDescription)mmrswn.get(mmrd.getName());
//						fnodes.remove(old);
//						enodes.remove(old);
//						mmrs.remove(old);
//						mmrswn.remove(old.getName());
						addme=false;
						}

					//---
					if (addme)
					{
						if (mmrd.isExitNode())
						{
							if (mmrd.allowsFragmented())
				{
							fnodes.addElement(mmrd);
						}
							else
							{
							enodes.addElement(mmrd);
						}
					}


					mmrs.addElement(mmrd);
					mmrswn.put(mmrd.getName(), mmrd);
				}
				}
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Added: " + mmrd);
					}
		}


		m_exitnodes = enodes;
		m_fragexitnodes = fnodes;
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "ExitNodes : "+enodes.size() +"Frag-Exit-Nodes:" +fnodes.size());
		m_mixminionrouters = mmrs;
		m_mixminionroutersWithNames = mmrswn;
		//test
		//m_exitnodes=m_mixminionrouters=mytesting();
		if (mmrswn.isEmpty()) 
		{
			System.out.println("Infoservice geht nicht!");
			return false;
		}
		else
		return true;
	}



	public void vectortostring(Vector v) {
		String huhu="";
		for(int i=0; i<v.size(); i++) {
			huhu += ((MMRDescription)v.elementAt(i)).getName() +",";
		}
		System.out.println(huhu);
	}

	public Vector mytesting() {

//		[Server]
//		 Nickname: rinos
//		 Identity: MIIBCgKCAQEAs6lIEY4Vz2skNL8SHJKkO5hvfernaBkhO/RnowiyFD/TaHQ1kdxYryaIu3dQ3M03eh+k5VoPiU/sX9+OfmHu0hB4vIqm5c5UtOkigSZOhEBDnZ31OgmfrK0+TaQHqNoF9lgT95QC6KXUgdpbhz2Qklg6qNxPWAbKLlewr6g0RBO51pFM/KK4IF9DMu8jQ8dssmWddPWZcdmQuY77njVr83OcPkpP/T8K+heVdkw7/jmlPAJ+wC2iCgkOtM5NJhk6+8NqOA57P5xXkrcEJkA6qRG9pvYYKsN4lor3asETT+X8mMOEuAkkwBTkRkhovqhQ1WPR0MAHTXUKP1wYAjkB4QIDAQAB
//		 Digest: nLrOnRowaQV/U/1XCUlXicIAIKc=
//		 Signature: pg7tNp9vnxZw7AbtwhBO1UgIA/C0cpWdQPyVfFWIh0eIl7I9FveN2mS3+1K2Z3iyTZKDm4v0NRDuZQrlYWe7Nh5rU7y/OXV46IYEVFxcneFJ778wM0VmVWE1HtcNFAOsqfwq+lVJvZKsyY4hwtUtsv3wpSlRIBO+Qr4taqnrivoi/ilDjlNV2/M35+W3/+rkAGa/8aitilA48feV/s3BuoyQcYnAxeg5CDKUlF5YuKFFc2ge8sQX2ePcPl0E2Qp2i1mBYP6GoyHlv8CO87XL40S4kYxDAqj44hiAcZSdp8yVRvqXH6yMa4pXpnJfKyIlWR1xTkQ9Yv7bZNXwbR8Kkw==
//		 Packet-Key: MIIBCgKCAQEA0SiCjybZ/+YsuHG9pgAIFNN0j+xF5ZPu3YI1F9MtgGkYQ7xfSrUJksbXprfo+QjJS5izTLkXQfFlUzViy0DMC7JHufofCh1o3lqryGnmE0S0XVD5Cvvz2OLMyRhINLmytp+CXx3E355EVmDebJNtqVRoZaPdZRnvQ2wkB5I6dhiAmhhzIAQVho4DQFf7+2Riv++1VP097TxAww/2gzdq7Pmv3PDd+TI2djAOMDMZO9ZjeZrCX+B7WGZxIBX/hISi9ck1AYq9ss1F4mAOHStgUFoD/iwcONh9OiLyGUhWdmZDrH4HwTutm8thTgt7l3w6LEnvi3Fg8YqeyAp2ocCMOwIDAQAB
//		 Hostname: localhost
//		 Port: 48099
//		 Key-Digest: MK2+xQEe59Zfwd+7nQ17PCgVBlg=

		MMRDescription mym = new MMRDescription("localhost", "rinos", 48099, Base64.decode("nLrOnRowaQV/U/1XCUlXicIAIKc="),
						  Base64.decode("MK2+xQEe59Zfwd+7nQ17PCgVBlg="), true, true, "egal",null);
		mym.setIdentityKey(Base64.decode("MIIBCgKCAQEAs6lIEY4Vz2skNL8SHJKkO5hvfernaBkhO/RnowiyFD/TaHQ1kdxYryaIu3dQ3M03eh+k5VoPiU/sX9+OfmHu0hB4vIqm5c5UtOkigSZOhEBDnZ31OgmfrK0+TaQHqNoF9lgT95QC6KXUgdpbhz2Qklg6qNxPWAbKLlewr6g0RBO51pFM/KK4IF9DMu8jQ8dssmWddPWZcdmQuY77njVr83OcPkpP/T8K+heVdkw7/jmlPAJ+wC2iCgkOtM5NJhk6+8NqOA57P5xXkrcEJkA6qRG9pvYYKsN4lor3asETT+X8mMOEuAkkwBTkRkhovqhQ1WPR0MAHTXUKP1wYAjkB4QIDAQAB"));
		mym.setPacketKey(Base64.decode("MIIBCgKCAQEA0SiCjybZ/+YsuHG9pgAIFNN0j+xF5ZPu3YI1F9MtgGkYQ7xfSrUJksbXprfo+QjJS5izTLkXQfFlUzViy0DMC7JHufofCh1o3lqryGnmE0S0XVD5Cvvz2OLMyRhINLmytp+CXx3E355EVmDebJNtqVRoZaPdZRnvQ2wkB5I6dhiAmhhzIAQVho4DQFf7+2Riv++1VP097TxAww/2gzdq7Pmv3PDd+TI2djAOMDMZO9ZjeZrCX+B7WGZxIBX/hISi9ck1AYq9ss1F4mAOHStgUFoD/iwcONh9OiLyGUhWdmZDrH4HwTutm8thTgt7l3w6LEnvi3Fg8YqeyAp2ocCMOwIDAQAB"));

		Vector retn = new Vector();
		retn.addElement(mym);
		retn.addElement(mym);
		
		return retn;

//////		//end
	}
}
