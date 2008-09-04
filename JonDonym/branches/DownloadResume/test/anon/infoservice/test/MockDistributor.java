/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package anon.infoservice.test;

import java.util.Vector;
import junit.framework.Assert;
import junitx.framework.extension.IMock;

import anon.infoservice.IDistributor;
import anon.infoservice.IDistributable;

/**
 * This class is a mock implementation and for testing purposes only.
 * @author Rolf Wendolsky
 */
public class MockDistributor implements IDistributor, IMock
{
	private Vector m_actualJobs;
	private Vector m_expectedJobs;

	public MockDistributor()
	{
		reset();
	}

	public void reset()
	{
		m_actualJobs = new Vector();
		m_expectedJobs = new Vector();
	}

	public void addJob(IDistributable a_job)
	{
		m_actualJobs.addElement(a_job);
	}

	public void setExpectedJobs(Vector a_expectedJobs)
	{
		m_expectedJobs = a_expectedJobs;
	}

	public void addExpectedJob(IDistributable a_expectedJob)
	{
		m_expectedJobs.addElement(a_expectedJob);
	}

	public void verify()
	{
		Assert.assertEquals(m_expectedJobs, m_actualJobs);
	}


}
