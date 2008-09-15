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
		
		for(int i=0; i<infos.length; i++)
		{
			root = infos[i].getRootCertificate();
			op = infos[i].getSecondCertificate();
			end = infos[i].getFirstCertificate();
			
			if(root != null)
			{
				m_rootNodes.put(root, new Node(root, infos[i].isVerified()));
			}
			if(op != null)
			{
				m_opNodes.put(op, new Node(op));
			}
			m_endNodes.put(end, new Node(end));
		}
		
		for(int i=0; i<infos.length; i++)
		{
			root = infos[i].getRootCertificate();
			op = infos[i].getSecondCertificate();
			end = infos[i].getFirstCertificate();
			
			if(op != null)
			{
				child = (Node) m_opNodes.get(op);
				if(root != null)
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

	/*private Vector createRootLevel(Vector a_infos)
	{
		CertPathInfo pathInfo, otherPathInfo;
		Node rootNode, operatorNode, endNode;
		Vector sameRoot, sameOp, rootNodes;
		
		rootNodes = new Vector();
		
		for(int i=0; i<a_infos.size();i++)
		{
			pathInfo = (CertPathInfo) a_infos.elementAt(i);
			if(pathInfo.getRootCertificate() != null)
			{
				sameRoot = new Vector();
				sameRoot.addElement(pathInfo);
								
				for(int j=i+1; j<a_infos.size(); j++)
				{
					otherPathInfo = (CertPathInfo) a_infos.elementAt(j);
					if(pathInfo.getRootCertificate().equals(otherPathInfo.getRootCertificate()))
					{
						sameRoot.addElement(otherPathInfo);
					}
				}
				
				rootNode = new Node(pathInfo.getRootCertificate());
				rootNodes.addElement(rootNode);
				a_infos.removeElementAt(i);
				
				for(int j=0; j<sameRoot.size(); j++)
				{
					pathInfo = (CertPathInfo) sameRoot.get(j);
					if(pathInfo.getSecondCertificate() != null)
					{
						sameOp = new Vector();
						sameOp.addElement(pathInfo);
						
						for(int k=j+1; k<sameRoot.size(); k++)
						{
							otherPathInfo = (CertPathInfo) sameRoot.elementAt(k);
							if(pathInfo.getSecondCertificate().equals(otherPathInfo.getSecondCertificate()))
							{
								sameOp.addElement(otherPathInfo);
							}
						}
						
						operatorNode = rootNode.createChildNode(pathInfo.getSecondCertificate());
						sameRoot.removeElementAt(j);
						
						for(int k=0; k<sameOp.size(); k++)
						{
							pathInfo = (CertPathInfo) sameOp.get(j);
							endNode = operatorNode.createChildNode(pathInfo.getFirstCertificate());
						}
					}					
 				}
				
				
				if(pathInfo.getSecondCertificate() != null)
				{
					operatorNode = rootNode.createChildNode(pathInfo.getSecondCertificate());
					endNode = operatorNode.createChildNode(pathInfo.getFirstCertificate());
					
				}
				
				a_infos.removeElementAt(i);
			}
		}
		
		//handle paths without root
		for(int i=0; i<a_infos.size(); i++)
		{
			//rootNodes.addElement(new TrustGraphNode(null, Util.toVector(a_infos.elementAt(i)), null));
		}
		
		return rootNodes;
	}*/
	
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
	
		/*public Node createChildNode(JAPCertificate a_cert)
		{
			Node child;
			
			if(m_childNodes == null)
			{
				m_childNodes = new Vector();
			}
			child = new Node(a_cert);
			m_childNodes.addElement(child);
		    
			return child;
		}*/
		
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
