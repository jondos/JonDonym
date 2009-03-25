/*
Copyright (c) 2000, The JAP-Team
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
package gui.wizard;

import java.util.Vector;

// this class shall provide the basics for the wizard
//as flow control and so on ...
public class BasicWizard implements Wizard
	{
		private WizardHost wizardHost;
		protected Vector m_Pages;
		private String m_strTitle;
		protected int m_PageIndex;

public BasicWizard()
	{
		wizardHost=null;
		m_Pages=new Vector();
		m_PageIndex = 0;
	}

	public void setHost(WizardHost host)
			{
				wizardHost=host;
			}

				public WizardHost getHost()
						{
							return wizardHost;
						}


// todo -- what does appear if the user's clicked help
public void help(/*WizardPage wtp, WizardHost wh*/)
{
 // return helpDialog;

}

// determine number and order of the wizardpages
public WizardPage invokeWizard(/*WizardHost host*/)
 {
		//wizardHost=host;
		wizardHost.setBackEnabled(false);
		wizardHost.setFinishEnabled(false);
		wizardHost.showWizardPage(0);
		m_PageIndex=0;
		return null;
 }
 public WizardPage next(/*WizardPage currentPage, WizardHost host*/)
	 {
			//int pageIndex=m_Pages.indexOf(currentPage);
			m_PageIndex++;
			wizardHost.setBackEnabled(true);
			if(m_PageIndex==m_Pages.size()-1)
				{
					wizardHost.setFinishEnabled(true);
					wizardHost.setNextEnabled(false);
				}
			wizardHost.showWizardPage(m_PageIndex);
			return null;
	 }

 public WizardPage back(/*WizardPage currentPage, WizardHost host*/)
	 {
			//int pageIndex=m_Pages.indexOf(currentPage);
			m_PageIndex--;
			wizardHost.setNextEnabled(true);
			wizardHost.setFinishEnabled(false);
			if(m_PageIndex==0)
				wizardHost.setBackEnabled(false);
			wizardHost.showWizardPage(m_PageIndex);
			return null;
	 }

 public void addWizardPage(int index,WizardPage wizardPage)
	{
		m_Pages.insertElementAt(wizardPage,index);
		wizardHost.addWizardPage(index,wizardPage);
	}

	public int initTotalSteps()
		{
			return m_Pages.size();
		}

 public WizardPage finish(/*WizardPage currentPage, WizardHost host*/){return null;}

public void wizardCompleted()
		{

		}

	public void setWizardTitle(String title)
		{
			m_strTitle=title;
		}

	public String getWizardTitle()
		{
			return m_strTitle;
		}
}

