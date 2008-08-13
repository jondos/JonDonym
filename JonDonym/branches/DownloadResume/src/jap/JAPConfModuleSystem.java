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
package jap;

import java.util.Enumeration;
import java.util.Hashtable;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.RootPaneContainer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import gui.GUIUtils;
import gui.JAPMessages;
import gui.JAPHelpContext;
import gui.dialog.JAPDialog;
import gui.help.JAPHelp;

/**
 * This is the implementation for the configuration module system. It manages the configuration
 * modules and displays the configuration tree and the configuration module content area.
 */
public class JAPConfModuleSystem implements JAPHelpContext.IHelpContext
{
	/**
	 * Stores the root panel for the whole configuration module system. The configuration tree and
	 * the module cards are created on this panel.
	 */
	private JPanel m_rootPanel;

	/**
	 * Stores the panel where the content of the configuration modules is displayed.
	 */
	private JPanel m_configurationCardsPanel;

	/**
	 * Stores the configuration tree.
	 */
	private JTree m_configurationTree;

	/**
	 * This table stores all registered instances of AbstractJAPConfModule. The key for each module
	 * in the table is the node of this module within the configuration tree.
	 */
	private Hashtable m_registratedModules;

	/**
	 * This table stores all node names of configuration components which are not included within an
	 * instance of AbstractJAPConfModule. The key for each node name in the table is the node of the
	 * associated node within the configuration tree. This table is only needed for compatibility
	 * with some old configuration structures in JAP. It will be removed as soon as possible.
	 */
	private Hashtable m_registratedPanelTitleIdentifiers;

	/**
	 * This table stores all associations between the tree nodes of the configuration modules (keys)
	 * and the symbolic names used to access the modules from outside.
	 */
	private Hashtable m_treeNodesToSymbolicNames;

	/**
	 * This table stores all associations between the symbolic names of the configuration modules
	 * used to access the modules from outside (keys) and the nodes of the modules within the
	 * configuration tree. It is the reverse-table of m_treeNodesToSymbolicNames.
	 */
	private Hashtable m_symbolicNamesToTreeNodes;

	private Hashtable m_symbolicNamesToHelpContext;
	private JAPHelpContext.IHelpContext m_currentHelpContext;

	/**
	 * Creates a new instance of JAPConfModuleSystem with an empty configuration tree. A lot of
	 * initialization is done here.
	 */
	public JAPConfModuleSystem()
	{
		m_registratedModules = new Hashtable();
		m_registratedPanelTitleIdentifiers = new Hashtable();
		m_treeNodesToSymbolicNames = new Hashtable();
		m_symbolicNamesToTreeNodes = new Hashtable();
		m_symbolicNamesToHelpContext = new Hashtable();
		m_configurationCardsPanel = new JPanel(new CardLayout());

		DefaultTreeModel configurationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("root"));

		DefaultTreeCellRenderer configurationTreeRenderer = new DefaultTreeCellRenderer();
		configurationTreeRenderer.setClosedIcon(GUIUtils.loadImageIcon("arrow.gif", true));
		configurationTreeRenderer.setOpenIcon(GUIUtils.loadImageIcon("arrow90.gif", true));
		configurationTreeRenderer.setLeafIcon(null);

		TreeSelectionModel configurationTreeSelectionModel = new DefaultTreeSelectionModel()
		{
			public void setSelectionPath(TreePath a_treePath)
			{
				String symbolicName = (String) (m_treeNodesToSymbolicNames.get(a_treePath.
					getLastPathComponent()));
				if (symbolicName != null)
				{
					/* there is a panel associated to this node -> selecting this node is possible */
					super.setSelectionPath(a_treePath);
				}
			}
		};
		configurationTreeSelectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		m_configurationTree = new JTree(configurationTreeModel);
		m_configurationTree.setSelectionModel(configurationTreeSelectionModel);
		m_configurationTree.setRootVisible(false);
		m_configurationTree.setEditable(false);
		m_configurationTree.setCellRenderer(configurationTreeRenderer);
		m_configurationTree.setBorder(new CompoundBorder(LineBorder.createBlackLineBorder(),
			new EmptyBorder(5, 5, 5, 5)));
		m_configurationTree.addTreeWillExpandListener(new TreeWillExpandListener()
		{
			public void treeWillCollapse(TreeExpansionEvent a_event) throws ExpandVetoException
			{
				throw new ExpandVetoException(a_event);
			}

			public void treeWillExpand(TreeExpansionEvent event)
			{
			}
		});
		m_configurationTree.addTreeSelectionListener(new TreeSelectionListener()
		{
			public void valueChanged(TreeSelectionEvent a_event)
			{
				if (a_event.isAddedPath())
				{
					String symbolicName =
						(String)(m_treeNodesToSymbolicNames.get(a_event.getPath().getLastPathComponent()));
					if (symbolicName != null)
					{
						m_currentHelpContext =
							(JAPHelpContext.IHelpContext) m_symbolicNamesToHelpContext.get(symbolicName);
						( (CardLayout) (m_configurationCardsPanel.getLayout())).show(
							m_configurationCardsPanel,
							symbolicName);
					}
				}
			}
		});

		m_rootPanel = new JPanel();

		GridBagLayout rootPanelLayout = new GridBagLayout();
		m_rootPanel.setLayout(rootPanelLayout);

		GridBagConstraints rootPanelConstraints = new GridBagConstraints();
		rootPanelConstraints.weightx = 0.0;
		rootPanelConstraints.weighty = 1.0;
		rootPanelConstraints.gridx = 0;
		rootPanelConstraints.gridy = 0;
		rootPanelConstraints.insets = new Insets(10, 10, 10, 10);
		rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		rootPanelConstraints.fill = GridBagConstraints.BOTH;
		rootPanelLayout.setConstraints(m_configurationTree, rootPanelConstraints);
		m_rootPanel.add(m_configurationTree);

		rootPanelConstraints.weightx = 1.0;
		rootPanelConstraints.weighty = 1.0;
		rootPanelConstraints.gridx = 1;
		rootPanelConstraints.gridy = 0;
		rootPanelConstraints.insets = new Insets(10, 10, 10, 10);
		rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		rootPanelConstraints.fill = GridBagConstraints.BOTH;
		rootPanelLayout.setConstraints(m_configurationCardsPanel, rootPanelConstraints);
		m_rootPanel.add(m_configurationCardsPanel);
	}

	/**
	 * Adds a configuration module to the module system and inserts it in the configuration tree.
	 *
	 * @param a_parentNode The parent node of the new module in the configuration tree.
	 * @param a_module The module to insert within the configuration tree.
	 * @param a_symbolicName A unique symbolic name for the new configuration module. This is used
	 *                       when the module shall be selected from the outside.
	 *
	 * @return The node of the inserted module within the configuration tree.
	 */
	public DefaultMutableTreeNode addConfigurationModule(DefaultMutableTreeNode a_parentNode,
		AbstractJAPConfModule a_module, String a_symbolicName)
	{
		DefaultMutableTreeNode moduleNode = new DefaultMutableTreeNode(a_module.getTabTitle());
		synchronized (this)
		{
			a_parentNode.add(moduleNode);
			m_configurationCardsPanel.add(a_module.getRootPanel(), a_symbolicName);
			m_registratedModules.put(moduleNode, a_module);
			m_treeNodesToSymbolicNames.put(moduleNode, a_symbolicName);
			m_symbolicNamesToTreeNodes.put(a_symbolicName, moduleNode);
			m_symbolicNamesToHelpContext.put(a_symbolicName, a_module);
		}
		return moduleNode;
	}

	/**
	 * Adds a configuration component to the module system and inserts it in the configuration tree.
	 * This method is only for compatibility with some old structures in JAP and will be removed
	 * soon. Only addConfigurationModule() should be used for the future.
	 *
	 * @param a_parentNode The parent node of the new component in the configuration tree.
	 * @param a_component The component to insert within the configuration tree. If this value is
	 *                    null, an unselectable node will be created.
	 * @param a_nodeNameIdentifier A name (resolvable via JAPMessages.getString()) used as the
	 *                             node name of the new component in the configuration tree.
	 * @param a_symbolicName A unique symbolic name for the new configuration component. This is used
	 *                       when the component shall be selected from the outside. This value is
	 *                       only evaluated, if a_component is not null.
	 *
	 * @return The node of the inserted module within the configuration tree.
	 */
	public DefaultMutableTreeNode addComponent(DefaultMutableTreeNode a_parentNode, Component a_component,
											   String a_nodeNameIdentifier, String a_symbolicName,
											   final String a_helpContext)
	{
		DefaultMutableTreeNode componentNode = new DefaultMutableTreeNode(JAPMessages.getString(
			a_nodeNameIdentifier));
		synchronized (this)
		{
			a_parentNode.add(componentNode);
			m_registratedPanelTitleIdentifiers.put(componentNode, a_nodeNameIdentifier);
			if (a_component != null)
			{
				/* this node has an associated component -> it will be selectable */
				m_configurationCardsPanel.add(a_component, a_symbolicName);
				m_treeNodesToSymbolicNames.put(componentNode, a_symbolicName);
				m_symbolicNamesToTreeNodes.put(a_symbolicName, componentNode);
				m_symbolicNamesToHelpContext.put(a_symbolicName, new  JAPHelpContext.IHelpContext()
				{
					public String getHelpContext()
					{
						return a_helpContext;
					}
					
					public Container getHelpExtractionDisplayContext()
					{
						return JAPConf.getInstance().getContentPane();
					}
				});
			}
		}
		return componentNode;
	}

	/**
	 * Returns the (invisible) root node of the configuration tree.
	 *
	 * @return The root node of the configuration tree.
	 */
	public DefaultMutableTreeNode getConfigurationTreeRootNode()
	{
		return (DefaultMutableTreeNode) (m_configurationTree.getModel().getRoot());
	}

	/**
	 * Returns the configuration tree. This can be used for doing some format-operations
	 * on the tree from the outside.
	 *
	 * @return The configuration tree.
	 */
	public JTree getConfigurationTree()
	{
		return m_configurationTree;
	}

	/**
	 * Returns the name of the module that is currently shown.
	 * @return the name of the module that is currently shown
	 */
	public String getHelpContext()
	{
		return m_currentHelpContext.getHelpContext();
	}

	public Container getHelpExtractionDisplayContext() 
	{
		return JAPConf.getInstance().getContentPane();
	}
	
	public AbstractJAPConfModule getCurrentModule()
	{
		return null;
	}

	/**
	 * Returns the root panel of the module system (where the configuration tree and the module
	 * content are displayed on).
	 *
	 * @return The root panel of the module system.
	 */
	public JPanel getRootPanel()
	{
		return m_rootPanel;
	}

	/**
	 * This method can be used to select a specific module from the outside and bring it to the
	 * front of the configuration dialog.
	 *
	 * @param a_symbolicName The symbolic name of the module (or component) to select. This name
	 *                       was specified when addConfigurationModule() or addComponent() was
	 *                       called.
	 */
	public void selectNode(String a_symbolicName)
	{
		synchronized (this)
		{
			DefaultMutableTreeNode treeNodeToSelect = (DefaultMutableTreeNode) (m_symbolicNamesToTreeNodes.
				get(a_symbolicName));
			if (treeNodeToSelect != null)
			{
				/* the symbolic name matches a node in the tree */
				m_configurationTree.setSelectionPath(new TreePath(treeNodeToSelect.getPath()));
			}
		}
	}

	/**
	 * Processes the configuration 'OK' button pressed event on all registered instances of
	 * AbstractJAPConfModule.
	 *
	 * @return True, if everything is ok or false, if one module returned a veto to this
	 *         event.
	 */
	public boolean processOkPressedEvent()
	{
		boolean returnValue = true;
		synchronized (this)
		{
			/* Call the event handler of all configuration modules. */
			Enumeration confModules = m_registratedModules.elements();
			while (confModules.hasMoreElements())
			{
				AbstractJAPConfModule confModule = (AbstractJAPConfModule) (confModules.nextElement());
				if (!confModule.okPressed())
				{
					returnValue = false;
				}
			}
		}

		return returnValue;
	}

	/**
	 * Processes the configuration 'Cancel' button pressed event on all registered instances of
	 * AbstractJAPConfModule.
	 */
	public void processCancelPressedEvent()
	{
		synchronized (this)
		{
			/* Call the event handler of all configuration modules. */
			Enumeration confModules = m_registratedModules.elements();
			while (confModules.hasMoreElements())
			{
				( (AbstractJAPConfModule) (confModules.nextElement())).cancelPressed();
			}
		}
	}

	/**
	 * Processes the configuration 'Reset to defaults' button pressed event on all registered
	 * instances of AbstractJAPConfModule.
	 */
	public void processResetToDefaultsPressedEvent()
	{
		synchronized (this)
		{
			/* Call the event handler of all configuration modules. */
			Enumeration confModules = m_registratedModules.elements();
			while (confModules.hasMoreElements())
			{
				( (AbstractJAPConfModule) (confModules.nextElement())).resetToDefaultsPressed();
			}
		}
	}

	/**
	 * Processes an update values event on all registered instances of AbstractJAPConfModule.
	 */
	public void processUpdateValuesEvent()
	{
		synchronized (this)
		{
			/* Call the event handler of all configuration modules. */
			Enumeration confModules = m_registratedModules.elements();
			while (confModules.hasMoreElements())
			{
				( (AbstractJAPConfModule) (confModules.nextElement())).updateValues(false);
			}
		}
	}

	/**
	 * Processes a create savepoints event on all registered instances of AbstractJAPConfModule.
	 * This method must be called everytime when the configuration dialog is displayed.
	 */
	public void createSavePoints()
	{
		synchronized (this)
		{
			/* Call the create savepoint handler of all configuration modules. */
			Enumeration confModules = m_registratedModules.elements();
			while (confModules.hasMoreElements())
			{
				( (AbstractJAPConfModule) (confModules.nextElement())).createSavePoint();
			}
		}
	}
}
