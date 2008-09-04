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


public interface Wizard
{
	//Set the Host to use by this Wizard
	public void setHost(WizardHost host);

	//Getst the Host to use by this Wizard
	public WizardHost getHost();

	//determine Number and Order of the WizardPages
	public WizardPage invokeWizard(/*WizardHost host*/);

	//user's clicked finish --> do the work
	public WizardPage finish(/*WizardPage currentPage, WizardHost host*/);

	//user's clicked next --> return next WizardPage
	public WizardPage next(/*WizardPage currentPage, WizardHost host*/);

 //user's clicked back --> return prev WizardPage
	public WizardPage back(/*WizardPage currentPage, WizardHost host*/);

	//user's clicked help --> make Help Dialog
	public void help(/*WizardPage currentPage, WizardHost host*/);

	//get the number of total steps
	public int initTotalSteps();

	//user's clicked finish or cancel, wizard has completed
	public void wizardCompleted();

	//Title of Wizard
	public String getWizardTitle();

	//determine the concrete action depending on the individual Pages
	//public Vector getPageVector();
}