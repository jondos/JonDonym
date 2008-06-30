package jap;

import gui.JAPMessages;
import gui.dialog.JAPDialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import anon.util.ZipArchiver.ZipEvent;

public class JAPHelpProgressDialog implements Observer
{
	
	private JProgressBar m_helpProgressBar;
	private JAPDialog m_displayProgress;
	private JLabel m_progressLabel;
	
	public final static String MSG_HELP_INSTALL = "helpInstall";
	
	long m_totalSizeExceedingInt = ZipEvent.UNDEFINED;
			
	public JAPHelpProgressDialog(JFrame parent)
	{
		m_helpProgressBar = new JProgressBar();
		m_displayProgress = new JAPDialog(parent, JAPMessages.getString(MSG_HELP_INSTALL), true);
		
		m_progressLabel = new JLabel("help progress");
		m_helpProgressBar.setBorderPainted(true);
		m_helpProgressBar.setStringPainted(true);
		//m_displayProgress.setSize(400, 300);
		m_displayProgress.getContentPane().setLayout(new BorderLayout());
		
		m_displayProgress.getContentPane().add(m_progressLabel, BorderLayout.NORTH);
		m_displayProgress.getContentPane().add(m_helpProgressBar, BorderLayout.CENTER);
		//m_displayProgress.setVisible(true);
		
		m_helpProgressBar.addChangeListener(new ProgressStateListener());
		m_totalSizeExceedingInt = ZipEvent.UNDEFINED;
	}
	
	public void setVisible()
	{
		m_displayProgress.setVisible(true);
	}
	
	public void update(Observable o, Object arg)
	{
		if( !(arg instanceof ZipEvent) )
		{
			//ignore
			return;
		}
		ZipEvent ze = (ZipEvent) arg;
		if(ze.isTotalSizeEvent())
		{
			m_helpProgressBar.setMinimum(0);
			long totalByteCount = ze.getTotalByteCount();
			if(totalByteCount > Integer.MAX_VALUE)
			{
				m_totalSizeExceedingInt = totalByteCount;
				m_helpProgressBar.setMaximum(Integer.MAX_VALUE);
			}
			else
			{
				m_totalSizeExceedingInt = ZipEvent.UNDEFINED;
				m_helpProgressBar.setMaximum((int) totalByteCount);
			}	
		}
		else
		{
			long byteCount = ze.getByteCount();
			String entryName = ze.getZipEntryName();
			if(byteCount != ZipEvent.UNDEFINED)
			{
				if(m_totalSizeExceedingInt != ZipEvent.UNDEFINED)
				{
					double byteCountRatio =
						((double) byteCount) / ((double) m_totalSizeExceedingInt);
					m_helpProgressBar.setValue((int) (byteCountRatio*Integer.MAX_VALUE));
					System.out.println("Case: exceed: "+byteCount+"/"+m_totalSizeExceedingInt+": "+byteCountRatio+", "+((int) (byteCountRatio*Integer.MAX_VALUE)));
				}
				else
				{
					System.out.println("Case: normal");
					m_helpProgressBar.setValue((int) byteCount);
				}
			}
			if(entryName != null)
			{
				m_progressLabel.setText("Extracting file: "+entryName);
			}
			m_helpProgressBar.repaint();
			m_progressLabel.repaint();
		}
	}
	
	public JProgressBar getProgressBar()
	{
		return m_helpProgressBar;
	}
	
	class ProgressStateListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent e) 
		{
			if(m_helpProgressBar.getPercentComplete() == 1.0)
			{
				m_displayProgress.dispose();
			}
		}
	}

	public void setMinimum(int i) 
	{
			
	}

	public void setMaximum(int totalSize) 
	{
		m_helpProgressBar.setMaximum(totalSize);	
	}
}
