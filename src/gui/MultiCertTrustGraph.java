package gui;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import anon.crypto.CertPathInfo;
import anon.crypto.JAPCertificate;

public class MultiCertTrustGraph
{
	Hashtable m_rootNodes;
	Hashtable m_opNodes;
	Hashtable m_endNodes;

	public MultiCertTrustGraph(CertPathInfo[] a_infos)
	{
		createGraph(a_infos);
	}
	
	private void createGraph(CertPathInfo[] infos)
	{
		m_rootNodes = new Hashtable();
		m_opNodes = new Hashtable();
		m_endNodes = new Hashtable();
		JAPCertificate root, op, end;
		Node parent, child;
		boolean verified;
		
		for (int i=0; i<infos.length; i++)
		{
			root = infos[i].getRootCertificate();
			op = infos[i].getSecondCertificate();
			end = infos[i].getFirstCertificate();
			verified = infos[i].isVerified();
			
			if(root != null)
			{
				m_rootNodes.put(root, new Node(root, verified));
			}
			if(op != null)
			{
				m_opNodes.put(op, new Node(op, verified));
			}
			m_endNodes.put(end, new Node(end, verified));
		}
		
		for (int i=0; i<infos.length; i++)
		{
			root = infos[i].getRootCertificate();
			op = infos[i].getSecondCertificate();
			end = infos[i].getFirstCertificate();
			
			if (op != null)
			{
				child = (Node) m_opNodes.get(op);
				if (root != null)
				{	//create root-op connection
					parent = (Node) m_rootNodes.get(root);
					parent.addChild(child);
					m_opNodes.remove(op);
				}
				//create op-end connection
				parent = child;
				child = new Node(infos[i].getFirstCertificate());
				parent.addChild(child);
				m_endNodes.remove(end);
			}
			else if (root != null) //no op
			{	//create root-end connection
				child = new Node(infos[i].getFirstCertificate());
				parent = (Node) m_rootNodes.get(root);
				parent.addChild(child);
				m_endNodes.remove(end);
			}
		}	
	}
	
	public Enumeration getRootNodes()
	{
		return m_rootNodes.elements();
	}
	
	public Enumeration getOperatorNodes()
	{
		return m_opNodes.elements();
	}
	
	public Enumeration getEndNodes()
	{
		return m_endNodes.elements();
	}
	
	public int countTrustedRootNodes()
	{
		int count = 0;
		Enumeration rootNodes = getRootNodes();
		Node current;
		
		while (rootNodes.hasMoreElements())
		{
			current = (Node) rootNodes.nextElement();
			if(current.isTrusted())
			{
				count++;
			}
		}
		return count;
	}
	
	public final class Node
	{
		private JAPCertificate m_cert;
		private Vector m_childNodes;
		private boolean m_trusted;
		
		public Node(JAPCertificate a_cert, boolean b_trusted)
		{	
			m_cert = a_cert;
			m_trusted = b_trusted;
			m_childNodes = new Vector();
		}
		
		public Node(JAPCertificate a_cert)
		{
			this(a_cert, false);
		}
		
		public void addChild(Node a_child)
		{
			if(!m_childNodes.contains(a_child))
			{
				m_childNodes.addElement(a_child);
				a_child.m_trusted = this.m_trusted;		
			}
		}
		
		public JAPCertificate getCertificate()
		{
			return m_cert;
		}
		
		public boolean isTrusted()
		{
			return m_trusted;
		}
		
		public Vector getChildNodes()
		{
			return m_childNodes;
		}
		
		public boolean hasChildNodes()
		{
			return m_childNodes.size() > 0;
		}
		
		public int getWidth()
		{
			if(m_childNodes.size() == 0)
			{
				return 1;
			}
			return m_childNodes.size();
		}
	}
}
