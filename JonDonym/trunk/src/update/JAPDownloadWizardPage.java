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

package update;

import gui.JAPMultilineLabel;
import gui.wizard.BasicWizardPage;
import jap.JAPConstants;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import anon.util.JAPMessages;
import gui.GUIUtils;

public class JAPDownloadWizardPage extends BasicWizardPage
{
	protected JLabel m_labelStatus;

	// Labels indicating the Steps of the current Update
	protected JLabel m_labelStep1_1, m_labelStep1_2;
	protected JLabel m_labelSaveFrom, m_labelSaveTo;

	protected JLabel m_labelStep2, m_labelStep3, m_labelStep4, m_labelStep5;

	//labels as placeholders for the icon indicating which step is the current job
	protected JLabel m_labelIconStep1;
	protected JLabel m_labelIconStep2, m_labelIconStep3, m_labelIconStep5;
	protected ImageIcon arrow, blank, stepfinished;
	protected JProgressBar progressBar;
	protected JPanel m_panelProgressBar;

	public JAPDownloadWizardPage()
	{
		GridBagLayout gridBagDownload;
		GridBagConstraints constraintsDownload;

		setIcon(GUIUtils.loadImageIcon(JAPConstants.DOWNLOADFN, false));
		setPageTitle("Download");

		arrow = GUIUtils.loadImageIcon(JAPConstants.IMAGE_ARROW, false);
		blank = GUIUtils.loadImageIcon(JAPConstants.IMAGE_BLANK, false);
		stepfinished = GUIUtils.loadImageIcon(JAPConstants.IMAGE_STEPFINISHED, false);

		gridBagDownload = new GridBagLayout();
		constraintsDownload = new GridBagConstraints();
		m_panelComponents.setLayout(gridBagDownload);

		constraintsDownload.gridx = 0;
		constraintsDownload.gridy = 0;
		constraintsDownload.gridheight = 1;
		constraintsDownload.gridwidth = 3;
		constraintsDownload.weightx = 1.0;
		constraintsDownload.fill = GridBagConstraints.HORIZONTAL;
		constraintsDownload.anchor = GridBagConstraints.NORTHWEST;
		constraintsDownload.insets = new Insets(0, 5, 10, 5);
		JAPMultilineLabel labelInformation = new JAPMultilineLabel(
			  JAPMessages.getString("updateDownloadIntroductionMessage"));

		gridBagDownload.setConstraints(labelInformation, constraintsDownload);
		m_panelComponents.add(labelInformation, constraintsDownload);

		m_labelIconStep1 = new JLabel();
		m_labelIconStep1.setIcon(arrow);
		//m_labelIconStep1.setText("   ");
		m_labelIconStep1.setPreferredSize(new Dimension(arrow.getIconWidth(), arrow.getIconHeight()));
		m_labelIconStep1.setMinimumSize(new Dimension(arrow.getIconWidth(), arrow.getIconHeight()));
		m_labelIconStep1.setVisible(true);
		constraintsDownload.gridx = 0;
		constraintsDownload.gridy = 1;
		constraintsDownload.gridheight = 2;
		constraintsDownload.gridwidth = 1;
		constraintsDownload.anchor = GridBagConstraints.WEST;
		constraintsDownload.weightx = 0;
		constraintsDownload.fill = GridBagConstraints.NONE;
		gridBagDownload.setConstraints(m_labelIconStep1, constraintsDownload);
		m_panelComponents.add(m_labelIconStep1);

		m_labelStep1_1 = new JLabel(JAPMessages.getString("updateM_labelStep1Part1"));
		constraintsDownload.gridx = 1;
		constraintsDownload.gridy = 1;
		constraintsDownload.weightx = 0;
		constraintsDownload.gridheight = 1;
		constraintsDownload.anchor = GridBagConstraints.NORTHWEST;
		constraintsDownload.fill = GridBagConstraints.HORIZONTAL;
		constraintsDownload.insets = new Insets(1, 5, 1, 5);
		gridBagDownload.setConstraints(m_labelStep1_1, constraintsDownload);
		m_panelComponents.add(m_labelStep1_1);

		m_labelSaveFrom = new JLabel();
		constraintsDownload.gridx = 2;
		constraintsDownload.gridy = 1;
		constraintsDownload.weightx = 1.0;
		constraintsDownload.gridheight = 1;
		constraintsDownload.anchor = GridBagConstraints.NORTHWEST;
		constraintsDownload.fill = GridBagConstraints.HORIZONTAL;
		gridBagDownload.setConstraints(m_labelSaveFrom, constraintsDownload);
		m_panelComponents.add(m_labelSaveFrom);

		m_labelStep1_2 = new JLabel(JAPMessages.getString("updateM_labelStep1Part2"));
		m_labelStep1_2.setHorizontalAlignment(SwingConstants.RIGHT);
		constraintsDownload.gridx = 1;
		constraintsDownload.gridy = 2;
		constraintsDownload.weightx = 0;
		constraintsDownload.gridheight = 1;
		constraintsDownload.anchor = GridBagConstraints.NORTHWEST;
		constraintsDownload.insets = new Insets(1, 5, 1, 5);
		gridBagDownload.setConstraints(m_labelStep1_2, constraintsDownload);
		m_panelComponents.add(m_labelStep1_2);

		m_labelSaveTo = new JLabel("");
		constraintsDownload.gridx = 2;
		constraintsDownload.gridy = 2;
		constraintsDownload.weightx = 1.0;
		constraintsDownload.gridheight = 1;
		constraintsDownload.anchor = GridBagConstraints.NORTHWEST;
		constraintsDownload.fill = GridBagConstraints.HORIZONTAL;
		gridBagDownload.setConstraints(m_labelSaveTo, constraintsDownload);
		m_panelComponents.add(m_labelSaveTo);

		m_labelIconStep2 = new JLabel();
		m_labelIconStep2.setIcon(blank);
		m_labelIconStep2.setVisible(true);
		constraintsDownload.insets = new Insets(5, 5, 5, 5);
		constraintsDownload.gridx = 0;
		constraintsDownload.gridy = 3;
		constraintsDownload.gridwidth = 1;
		constraintsDownload.weightx = 0.0;
		constraintsDownload.anchor = GridBagConstraints.WEST;
		constraintsDownload.fill = GridBagConstraints.NONE;
		gridBagDownload.setConstraints(m_labelIconStep2, constraintsDownload);
		m_panelComponents.add(m_labelIconStep2);

		m_labelStep2 = new JLabel(JAPMessages.getString("updateM_labelStep2"));
		constraintsDownload.gridx = 1;
		constraintsDownload.gridy = 3;
		constraintsDownload.gridwidth = 2;
		constraintsDownload.weightx = 1.0;
		constraintsDownload.fill = GridBagConstraints.HORIZONTAL;
		gridBagDownload.setConstraints(m_labelStep2, constraintsDownload);
		m_panelComponents.add(m_labelStep2);

		m_labelIconStep3 = new JLabel();
		m_labelIconStep3.setIcon(blank);
		m_labelIconStep3.setVisible(true);
		constraintsDownload.gridx = 0;
		constraintsDownload.gridy = 4;
		constraintsDownload.gridwidth = 1;
		constraintsDownload.weightx = 0.0;
		constraintsDownload.fill = GridBagConstraints.NONE;
		gridBagDownload.setConstraints(m_labelIconStep3, constraintsDownload);
		m_panelComponents.add(m_labelIconStep3);

		m_labelStep3 = new JLabel();
		constraintsDownload.gridx = 1;
		constraintsDownload.gridy = 4;
		constraintsDownload.gridwidth = 2;
		constraintsDownload.weightx = 1.0;
		constraintsDownload.fill = GridBagConstraints.HORIZONTAL;
		gridBagDownload.setConstraints(m_labelStep3, constraintsDownload);
		m_panelComponents.add(m_labelStep3);

		m_labelIconStep5 = new JLabel();
		m_labelIconStep5.setIcon(blank);
		m_labelIconStep5.setVisible(true);
		constraintsDownload.gridx = 0;
		constraintsDownload.gridy = 5;
		constraintsDownload.weightx = 0.0;
		constraintsDownload.gridwidth = 1;
		constraintsDownload.fill = GridBagConstraints.NONE;
		gridBagDownload.setConstraints(m_labelIconStep5, constraintsDownload);
		m_panelComponents.add(m_labelIconStep5);

		m_labelStep5 = new JLabel(JAPMessages.getString("updateM_labelStep5"));
		constraintsDownload.gridx = 1;
		constraintsDownload.gridy = 5;
		constraintsDownload.gridwidth = 2;
		constraintsDownload.weightx = 1.0;
		constraintsDownload.fill = GridBagConstraints.HORIZONTAL;
		gridBagDownload.setConstraints(m_labelStep5, constraintsDownload);
		m_panelComponents.add(m_labelStep5);
		// define an own panel for progressBar and its label

		m_panelProgressBar = new JPanel();
		GridBagLayout gridBagLayout = new GridBagLayout();
		GridBagConstraints constraintsPanelProgress = new GridBagConstraints();
		m_panelProgressBar.setLayout(gridBagLayout);
		m_labelStatus = new JLabel(JAPMessages.getString("updateM_labelStatus"));
		constraintsPanelProgress.gridx = 0;
		constraintsPanelProgress.gridy = 0;
		constraintsPanelProgress.anchor = GridBagConstraints.WEST;
		constraintsPanelProgress.gridwidth = 1;
		constraintsPanelProgress.insets = new Insets(10, 5, 5, 5);
		gridBagLayout.setConstraints(m_labelStatus, constraintsPanelProgress);
		m_panelProgressBar.add(m_labelStatus);

		progressBar = new JProgressBar(0, 500);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		progressBar.setPreferredSize(new Dimension(200, 20));
		progressBar.setMaximumSize(new Dimension(200, 20));
		progressBar.setMinimumSize(new Dimension(100, 20));
		constraintsPanelProgress.gridx = 1;
		constraintsPanelProgress.gridy = 0;
		constraintsPanelProgress.insets = new Insets(10, 5, 5, 5);
		gridBagLayout.setConstraints(progressBar, constraintsPanelProgress);
		m_panelProgressBar.add(progressBar);

		JLabel l = new JLabel();
		constraintsPanelProgress.gridx = 2;
		constraintsPanelProgress.weightx = 1.0;
		constraintsPanelProgress.fill = GridBagConstraints.HORIZONTAL;
		gridBagLayout.setConstraints(l, constraintsPanelProgress);
		m_panelProgressBar.add(l);

		constraintsDownload.gridx = 1;
		constraintsDownload.gridy = 6;
		constraintsDownload.gridwidth = 2;
		constraintsDownload.weightx = 1.0;
		constraintsDownload.anchor = GridBagConstraints.WEST;
		constraintsDownload.fill = GridBagConstraints.HORIZONTAL;
		gridBagDownload.setConstraints(m_panelProgressBar, constraintsDownload);
		m_panelComponents.add(m_panelProgressBar);

//				setVisible(true);

	}

	public boolean checkPage()
	{
		return true;
	}

	public static void main(String[] args)
	{
		JFrame parent = new JFrame("parent");
		//JAPDownloadWizardPage jdw = new JAPDownloadWizardPage("version",new JAPUpdateWizard("version"));
		//parent.getContentPane().add(jdw);
		//parent.pack();
		parent.setVisible(true);

	}
}
