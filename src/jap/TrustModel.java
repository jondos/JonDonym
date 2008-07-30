/*
 Copyright (c) 2000 - 2006, The JAP-Team
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

import java.security.SignatureException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import gui.JAPMessages;
import anon.client.BasicTrustModel;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.ServiceOperator;
import anon.infoservice.MixInfo;
import anon.infoservice.StatusInfo;
import anon.infoservice.PerformanceEntry;
import anon.infoservice.PerformanceInfo;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.BlacklistedCascadeIDEntry;
import anon.infoservice.NewCascadeIDEntry;
import anon.pay.PayAccountsFile;
import anon.client.TrustException;


/**
 * This is the general trust model for JAP.
 *
 * @author Rolf Wendolsky
 */
public class TrustModel extends BasicTrustModel implements IXMLEncodable
{
	public static final Integer NOTIFY_TRUST_MODEL_CHANGED = new Integer(0);
	public static final Integer NOTIFY_TRUST_MODEL_ADDED = new Integer(1);
	public static final Integer NOTIFY_TRUST_MODEL_REMOVED = new Integer(2);

	public static final String XML_ELEMENT_NAME = "TrustModel";
	public static final String XML_ELEMENT_CONTAINER_NAME = "TrustModels";
	private static final String XML_ATTR_CURRENT_TRUST_MODEL = "currentTrustModel";
	private static final String XML_ATTR_NAME = "name";

	public static final TrustModel TRUST_MODEL_USER_DEFINED;
	public static final TrustModel TRUST_MODEL_DEFAULT;

	public static TrustModel TRUST_MODEL_CUSTOM_FILTER;
	
	/**
	 * Always trust the cascade, regardless of the attribute
	 */
	public static final int TRUST_ALWAYS = 0;

	/**
	 * Only trust the cascade if the attribute is NOT true
	 */
	public static final int TRUST_IF_NOT_TRUE = 1;

	/**
	 * Only trust the cascade if the attribute is true
	 */
	public static final int TRUST_IF_TRUE = 2;

	/**
	 * Only trust the cascade if the attribute is greater than a specified value
	 */
	public static final int TRUST_IF_AT_LEAST = 3;

	/**
	 * Only trust the cascade if the attribute is lesser than a specified value
	 */
	public static final int TRUST_IF_AT_MOST = 5;

	/**
	 * Only trust the cascade if the attribute value is not within the specified list
	 */
	public static final int TRUST_IF_NOT_IN_LIST = 6;

	/**
	 * Reserved for future use
	 */
	public static final int TRUST_RESERVED = 7;
	
	public static final long TRUST_MODEL_ALL = 0;

	private static final String MSG_SERVICES_WITH_COSTS = TrustModel.class.getName() + "_servicesWithCosts";
	private static final String MSG_SERVICES_WITHOUT_COSTS = TrustModel.class.getName() + "_servicesWithoutCosts";
	private static final String MSG_SERVICES_USER_DEFINED = TrustModel.class.getName() + "_servicesUserDefined";
	private static final String MSG_CASCADES_FILTER = TrustModel.class.getName() + "_servicesFilter";
	private static final String MSG_ALL_SERVICES = TrustModel.class.getName() + "_allServices";
	
	private static final String MSG_EXCEPTION_PAY_CASCADE = TrustModel.class.getName() + "_exceptionPayCascade";
	private static final String MSG_EXCEPTION_FREE_CASCADE = TrustModel.class.getName() + "_exceptionFreeCascade";
	private static final String MSG_EXCEPTION_MORE_THAN_1_OPERATOR = TrustModel.class.getName() + "_exceptionMoreThan1Op";
	private static final String MSG_EXCEPTION_SINGLE_MIX = TrustModel.class.getName() + "_exceptionSingleMix";
	private static final String MSG_EXCEPTION_EXPIRED_CERT = TrustModel.class.getName() + "_exceptionExpiredCert";
	private static final String MSG_EXCEPTION_NOT_USER_DEFINED = TrustModel.class.getName() + "_exceptionNotUserDefined";
	private static final String MSG_EXCEPTION_TOO_FEW_COUNTRIES = TrustModel.class.getName() + "_exceptionTooFewCountries";
	private static final String MSG_EXCEPTION_NOT_INTERNATIONAL = TrustModel.class.getName() + "_exceptionNotInternational";
	private static final String MSG_EXCEPTION_INTERNATIONAL = TrustModel.class.getName() + "_exceptionInternational";
	private static final String MSG_EXCEPTION_NOT_ENOUGH_ANON = TrustModel.class.getName() + "_exceptionNotEnoughAnon";
	private static final String MSG_EXCEPTION_BLACKLISTED = TrustModel.class.getName() + "_exceptionBlacklisted";
	private static final String MSG_EXCEPTION_NOT_ENOUGH_SPEED = TrustModel.class.getName() + "_exceptionNotEnoughSpeed";
	private static final String MSG_EXCEPTION_RESPONSE_TIME_TOO_HIGH = TrustModel.class.getName() + "_exceptionResponseTimeTooHigh";
	private static final String MSG_EXCEPTION_NO_VALID_SIGNATURE = TrustModel.class.getName() + "_exceptionNoValidSignature";
	
	private static Vector ms_trustModels = new Vector();
	private static TrustModel ms_currentTrustModel;

	protected static class InnerObservable extends Observable
	{
		public void setChanged()
		{
			super.setChanged();
		}
	}
	private static InnerObservable m_trustModelObservable = new InnerObservable();

	private Hashtable m_trustAttributes = new Hashtable();

	private String m_strName;
	private long m_id;

	private boolean m_bEditable;

	public static abstract class TrustAttribute implements IXMLEncodable
	{
		public static final int CATEGORY_DEFAULT = 0;

		public static final String XML_ELEMENT_NAME = "TrustAttribute";
		public static final String XML_VALUE_ELEMENT_NAME = "ConditionValue";
		public static final String XML_VALUE_CONTAINER_ELEMENT_NAME = "ConditionValueList";
		public static final String XML_ATTR_NAME = "name";
		public static final String XML_ATTR_TRUST_CONDITION = "trustCondition";
		public static final String XML_ATTR_CONDITION_VALUE = "conditonValue";
		
		private int m_category;
		protected int m_trustCondition;
		protected Object m_conditionValue;

		private TrustAttribute(int a_trustCondition, Object a_conditionValue)
		{
			m_trustCondition = a_trustCondition;
			m_conditionValue = a_conditionValue;
			m_category = CATEGORY_DEFAULT;
		}

		public final int getCategory()
		{
			return m_category;
		}

		public int getTrustCondition()
		{
			return m_trustCondition;
		}

		public Object getConditionValue()
		{
			return m_conditionValue;
		}

		public abstract void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException;

		public Element toXmlElement(Document a_doc)
		{
			if(a_doc == null) return null;

			Element el = a_doc.createElement(XML_ELEMENT_NAME);
			XMLUtil.setAttribute(el, XML_ATTR_NAME, this.getClass().getName());
			XMLUtil.setAttribute(el, XML_ATTR_TRUST_CONDITION, m_trustCondition);

			if(m_conditionValue instanceof Integer)
				XMLUtil.setAttribute(el, XML_ATTR_CONDITION_VALUE, ((Integer) m_conditionValue).intValue());
			else if(m_conditionValue instanceof Vector)
			{
				Vector vec = (Vector) m_conditionValue;
				Element list = a_doc.createElement(XML_VALUE_CONTAINER_ELEMENT_NAME);
				for(int i = 0; i < vec.size(); i++)
				{
					Element e = a_doc.createElement(XML_VALUE_ELEMENT_NAME);
					XMLUtil.setValue(e, ((ServiceOperator) vec.elementAt(i)).getId());
					list.appendChild(e);
				}
				el.appendChild(list);
			}

			return el;
		}

		public static TrustAttribute fromXmlElement(Element a_e) throws XMLParseException
		{
			if(a_e == null) return null;

			XMLUtil.assertNodeName(a_e, XML_ELEMENT_NAME);
			XMLUtil.assertNotNull(a_e, XML_ATTR_NAME);

			String name = XMLUtil.parseAttribute(a_e, XML_ATTR_NAME, null);
			int trustCondition = XMLUtil.parseAttribute(a_e, XML_ATTR_TRUST_CONDITION, TRUST_ALWAYS);
			int conditionValue = XMLUtil.parseAttribute(a_e, XML_ATTR_CONDITION_VALUE, 0);

			TrustAttribute attr;

			try
			{
				Object value = null;

				if(trustCondition == TRUST_IF_NOT_IN_LIST)
				{
					Node n = XMLUtil.getFirstChildByName(a_e, XML_VALUE_CONTAINER_ELEMENT_NAME);
					XMLUtil.assertNotNull(n);

					NodeList list = n.getChildNodes();
					value = new Vector();

					for(int i = 0; i < list.getLength(); i++)
					{
						// look for a matching ServiceOperator database entry
						ServiceOperator op = (ServiceOperator) Database.getInstance(ServiceOperator.class).getEntryById(XMLUtil.parseValue(list.item(i),null));
						if(op != null) ((Vector) value).addElement(op);
					}
				}
				else
				{
					value = new Integer(conditionValue);
				}

				attr = (TrustAttribute) Class.forName(name).getConstructor(new Class[] { int.class, Object.class })
					.newInstance(new Object[] { new Integer(trustCondition), value });
			}
			catch(Exception ex)
			{
				throw new XMLParseException(XML_ELEMENT_NAME);
			}

			return attr;
		}
	}

	public static class PaymentAttribute extends TrustAttribute
	{
		public PaymentAttribute(int a_trustCondition, Object a_conditionValue)
		{
			super(a_trustCondition, a_conditionValue);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if (a_cascade.isPayment())
			{
				if (m_trustCondition == TRUST_IF_NOT_TRUE)
				{
					throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_PAY_CASCADE));
				}
			}
			else if (m_trustCondition == TRUST_IF_TRUE)
			{
				throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_FREE_CASCADE));
			}
		}
	};

	public static class SingleMixAttribute extends TrustAttribute
	{
		public SingleMixAttribute(int a_trustCondition, Object a_conditionValue)
		{
			super(a_trustCondition, a_conditionValue);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if (m_trustCondition == TRUST_IF_TRUE && a_cascade.getNumberOfOperators() > 1)
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_MORE_THAN_1_OPERATOR)));
			}
			else if (m_trustCondition == TRUST_IF_NOT_TRUE && a_cascade.getNumberOfOperators() <= 1)
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_SINGLE_MIX)));
			}
		}
	};

	public static class ExpiredCertsAttribute extends TrustAttribute
	{
		public ExpiredCertsAttribute(int a_trustCondition, Object a_conditionValue)
		{
			super(a_trustCondition, a_conditionValue);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if (a_cascade.getCertPath() != null && !a_cascade.getCertPath().checkValidity(new Date()))
			{
				if (m_trustCondition == TRUST_IF_NOT_TRUE)
				{
					throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_EXPIRED_CERT));
				}
			}
		}
	};

	public static class UserDefinedAttribute extends TrustAttribute
	{
		public UserDefinedAttribute(int a_trustCondition, Object a_conditionValue)
		{
			super(a_trustCondition, a_conditionValue);
		}
		
		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if (m_trustCondition == TRUST_IF_TRUE)
			{
				if (a_cascade.isUserDefined())
				{
					//if (a_cascade.getCertificate() == null && (a_cascade.getNumberOfMixes() == 0 ||
					//	a_cascade.getNumberOfMixes() == 1 &&
					//	(a_cascade.getMixInfo(0) == null || a_cascade.getMixInfo(0).getCertificate() == null)))
					{
						// not yet connected; do not make further tests
						return;
					}
				}
				else
				{
					throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_NOT_USER_DEFINED));
				}
			}
		}
	};

	public static class InternationalAttribute extends TrustAttribute
	{
		public InternationalAttribute(int a_trustCondition, Object a_conditionValue)
		{
			super(a_trustCondition, a_conditionValue);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if(m_trustCondition == TRUST_IF_AT_LEAST && a_cascade.getNumberOfCountries() < ((Integer) m_conditionValue).intValue())
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_TOO_FEW_COUNTRIES)));
			}
			else if (m_trustCondition == TRUST_IF_TRUE && a_cascade.getNumberOfCountries() <= 1)
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_NOT_INTERNATIONAL)));
			}
			else if (m_trustCondition == TRUST_IF_NOT_TRUE && a_cascade.getNumberOfCountries() > 1)
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_INTERNATIONAL)));
			}
		}
	};

	/*public static class NewAttribute extends TrustAttribute
	{
		public NewAttribute(int a_trustCondition, Object a_conditionValue)
		{
			super(a_trustCondition, a_conditionValue);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if (m_trustCondition == TRUST_IF_TRUE)
			{
				if (Database.getInstance(NewCascadeIDEntry.class).getEntryById(
					 a_cascade.getMixIDsAsString()) == null)
				{
					throw (new TrustException("only new cascades are accepted!"));
				}
			}
			else if(m_trustCondition == TRUST_IF_NOT_TRUE)
			{
				if (Database.getInstance(NewCascadeIDEntry.class).getEntryById(a_cascade.getMixIDsAsString()) != null)
				{
					throw (new TrustException("No new cascades are allowed"));
				}
			}
		}
	};*/

	public static class AnonLevelAttribute extends TrustAttribute
	{
		public AnonLevelAttribute(int a_trustCondition, Object a_conditionValue)
		{
			// MUST always be TRUST_IF_AT_LEAST
			super(TRUST_IF_AT_LEAST, a_conditionValue);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			StatusInfo info = (StatusInfo) Database.getInstance(StatusInfo.class).getEntryById(a_cascade.getId());
			if(m_trustCondition == TRUST_IF_AT_LEAST && (info == null || info.getAnonLevel() < ((Integer) m_conditionValue).intValue()))
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_NOT_ENOUGH_ANON)));
			}
		}
	}

	public static class OperatorBlacklistAttribute extends TrustAttribute
	{
		public OperatorBlacklistAttribute(int a_trustCondtion, Object a_conditionValue)
		{
			// MUST always be TRUST_IF_NOT_IN_LIST and value must be a vector
			super(TRUST_IF_NOT_IN_LIST, (a_conditionValue == null || !(a_conditionValue instanceof Vector)) ? new Vector() : a_conditionValue);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if(m_trustCondition == TRUST_IF_NOT_IN_LIST)
			{
				for(int i = 0; i < a_cascade.getNumberOfMixes(); i++)
				{
					Vector list = (Vector) m_conditionValue;

					if(list.contains(a_cascade.getMixInfo(i).getServiceOperator()))
					{
						throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_BLACKLISTED));
					}
					/*for(int j = 0; j < ((Vector)m_conditionValue).size(); j++)
					{
						if(((Vector)m_conditionValue).elementAt(j).equals(a_cascade.getMixInfo(i).getServiceOperator()))
						{
							throw new TrustException("This cascade has a blacklisted mix!");
						}
					}*/
				}
			}
		}
	}

	public static class SpeedAttribute extends TrustAttribute
	{
		public SpeedAttribute(int a_trustCondition, Object a_conditionValue)
		{
			// MUST always be TRUST_IF_AT_LEAST
			super(TRUST_IF_AT_LEAST, a_conditionValue);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			PerformanceEntry entry = PerformanceInfo.getAverageEntry(a_cascade.getId());
			int minSpeed = ((Integer) m_conditionValue).intValue();
			
			if(minSpeed == 0)
			{
				return;
			}
			
			// TODO: make it configurable
			if(entry == null || entry.isXMLInvalid()) 
			{
				return;
			}
			
			if(m_trustCondition == TRUST_IF_AT_LEAST && (entry == null || entry.isXMLInvalid() || entry.getXMLAverage(PerformanceEntry.SPEED) < minSpeed))
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_NOT_ENOUGH_SPEED)));
			}
		}
	}
	
	public static class DelayAttribute extends TrustAttribute
	{
		public DelayAttribute(int a_trustCondition, Object a_conditionValue)
		{
			// MUST always be TRUST_IF_AT_MOST
			super(TRUST_IF_AT_MOST, a_conditionValue);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			PerformanceEntry entry = PerformanceInfo.getAverageEntry(a_cascade.getId());
			int maxDelay = ((Integer) m_conditionValue).intValue();
			
			if(maxDelay == TRUST_ALWAYS)
			{
				return;
			}
			
			// TODO: make it configurable
			if(entry == null || entry.isXMLInvalid())
			{
				return;
			}
			
			if(m_trustCondition == TRUST_IF_AT_MOST && (entry == null || entry.isXMLInvalid() || entry.getXMLAverage(PerformanceEntry.DELAY) > maxDelay))
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_RESPONSE_TIME_TOO_HIGH)));
			}
		}
	}	
	
	static
	{
		// Initialize basic trust models
		TrustModel model;

		model = new TrustModel(MSG_ALL_SERVICES, 0);
		model.setAttribute(ExpiredCertsAttribute.class, TRUST_RESERVED);
		TRUST_MODEL_DEFAULT = model;
		ms_trustModels.addElement(model);

		model = new TrustModel(MSG_SERVICES_WITH_COSTS, 2);
		model.setAttribute(PaymentAttribute.class, TRUST_IF_TRUE);
		model.setAttribute(ExpiredCertsAttribute.class, TRUST_RESERVED);
		ms_trustModels.addElement(model);

		model = new TrustModel(MSG_SERVICES_WITHOUT_COSTS, 3);
		model.setAttribute(PaymentAttribute.class, TRUST_IF_NOT_TRUE);
		model.setAttribute(ExpiredCertsAttribute.class, TRUST_RESERVED);
		ms_trustModels.addElement(model);

		model = new TrustModel(MSG_SERVICES_USER_DEFINED, 4)
		{
			public boolean isAdded()
			{
				Enumeration enties = Database.getInstance(MixCascade.class).getEntrySnapshotAsEnumeration();
				while (enties.hasMoreElements())
				{
					if (((MixCascade)enties.nextElement()).isUserDefined())
					{
						return true;
					}
				}
				return false;
			}
		};
		model.setAttribute(UserDefinedAttribute.class, TRUST_IF_TRUE);
		TRUST_MODEL_USER_DEFINED = model;
		ms_trustModels.addElement(model);

		setCurrentTrustModel((TrustModel)ms_trustModels.elementAt(0));
	}

	/**
	 * Creates a new TrustModel object with the specified name
	 *
	 * @param a_strName	The name of the TrustModel
	 */
	public TrustModel(String a_strName, long a_id)
	{
		//m_id = ms_trustModels.size();
		m_id = a_id;
		m_strName = a_strName == null ? "Default trust model" : a_strName;
	}

	/**
	 * Creates a TrustModel object from another TrustModel object
	 *
	 * @param a_trustModel	The TrustModel object to copy
	 */
	public TrustModel(TrustModel a_trustModel)
	{
		copyFrom(a_trustModel);
	}

	/**
	 * Creates a TrustModel object from an XML element
	 *
	 * @param a_trustModelElement	The XML element which holds the TrustModel data
	 *
	 * @throws XMLParseException
	 */
	public TrustModel(Element a_trustModelElement) throws XMLParseException
	{
		XMLUtil.assertNodeName(a_trustModelElement, XML_ELEMENT_NAME);

		XMLUtil.assertNotNull(a_trustModelElement, XML_ATTR_ID);
		XMLUtil.assertNotNull(a_trustModelElement, XML_ATTR_NAME);

		m_id = XMLUtil.parseAttribute(a_trustModelElement, XML_ATTR_ID, -1l);
		m_strName = XMLUtil.parseAttribute(a_trustModelElement, XML_ATTR_NAME, null);
		m_bEditable = true;

		for(int i = 0; i < a_trustModelElement.getChildNodes().getLength(); i++)
		{
			Element el = (Element) a_trustModelElement.getChildNodes().item(i);
			setAttribute(TrustAttribute.fromXmlElement(el));
		}
	}

	public void copyFrom(TrustModel a_trustModel)
	{
		if (a_trustModel == null)
		{
			throw new IllegalArgumentException("No argument given!");
		}

		m_id = a_trustModel.m_id;
		m_strName = a_trustModel.m_strName;
		m_bEditable = a_trustModel.m_bEditable;

		synchronized(m_trustAttributes)
		{
			m_trustAttributes = (Hashtable) a_trustModel.m_trustAttributes.clone();
		}
	}

	public static Observable getObservable()
	{
		return m_trustModelObservable;
	}

	public static void addModelObserver(Observer a_observer)
	{
		m_trustModelObservable.addObserver(a_observer);
	}

	public static void deleteModelObserver(Observer a_observer)
	{
		m_trustModelObservable.deleteObserver(a_observer);
	}

	/**
	 * Compares two TrustModel objects
	 *
	 * @return true if the id's of the two objects match, false otherwise
	 */
	public boolean equals(Object a_trustModel)
	{
		if (a_trustModel == null || !(a_trustModel instanceof TrustModel))
		{
			return false;
		}
		return getId() == ((TrustModel)a_trustModel).getId();
	}

	public int hashCode()
	{
		return (int)getId();
	}

	public static boolean addTrustModel(TrustModel a_trustModel)
	{
		synchronized (ms_trustModels)
		{
			if (a_trustModel != null && !ms_trustModels.contains(a_trustModel))
			{
				ms_trustModels.addElement(a_trustModel);
				m_trustModelObservable.setChanged();
				m_trustModelObservable.notifyObservers(NOTIFY_TRUST_MODEL_ADDED);
				return true;
			}
		}
		return false;
	}

	public static TrustModel removeTrustModel(TrustModel a_trustModel)
	{
		if (!a_trustModel.isEditable())
		{
			// editable models must not be removed
			return null;
		}
		synchronized (ms_trustModels)
		{
			if (a_trustModel != null && ms_trustModels.removeElement(a_trustModel))
			{
				m_trustModelObservable.setChanged();
				m_trustModelObservable.notifyObservers(NOTIFY_TRUST_MODEL_REMOVED);
				return a_trustModel;
			}
		}
		return null;
	}

	public TrustAttribute setAttribute(Class a_attr, int a_trustCondition)
	{
		return setAttribute(a_attr, a_trustCondition, null);
	}

	public TrustAttribute setAttribute(Class a_attr, int a_trustCondition, int a_conditionValue)
	{
		return setAttribute(a_attr, a_trustCondition, new Integer(a_conditionValue));
	}

	public void setEditable(boolean a_bEditable)
	{
		m_bEditable = a_bEditable;
	}

	public boolean isEditable()
	{
		return m_bEditable;
	}


	public TrustAttribute setAttribute(Class a_attr, int a_trustCondition, Object a_conditionValue)
	{
		try
		{
			 return setAttribute((TrustAttribute) a_attr.getConstructor(new Class[] { int.class, Object.class })
			 	.newInstance(new Object[] { new Integer(a_trustCondition), a_conditionValue}));
		}
		catch(Exception ex) { LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Could not create " + a_attr); return null; }}

	public TrustAttribute setAttribute(TrustAttribute a_attr)
	{
		synchronized(this)
		{
			TrustAttribute value;

			if((value = (TrustAttribute) m_trustAttributes.get(a_attr)) != null)
			{
				if(value.getTrustCondition() != a_attr.getTrustCondition() || value.getConditionValue() != a_attr.getConditionValue())
				{
					m_trustAttributes.put(a_attr, value);
					setChanged();
				}
			}

			m_trustAttributes.put(a_attr.getClass(), a_attr);
			notifyObservers();

			return a_attr;
		}
	}

	public TrustAttribute getAttribute(Class a_attr)
	{
		synchronized(m_trustAttributes)
		{
			TrustAttribute attr = (TrustAttribute) m_trustAttributes.get(a_attr);
			if(attr == null)
			{
				return setAttribute(a_attr, TRUST_ALWAYS, new Integer(0));
			}

			return attr;
		}
	}

	public static Vector getTrustModels()
	{
		return (Vector)ms_trustModels.clone();
	}

	public static void setCurrentTrustModel(long a_id)
	{
		if (a_id < 0)
		{
			return;
		}

		synchronized (ms_trustModels)
		{
			for (int i = 0; i < ms_trustModels.size(); i++)
			{
				if (((TrustModel)ms_trustModels.elementAt(i)).getId() == a_id)
				{
					ms_currentTrustModel = (TrustModel)ms_trustModels.elementAt(i);
					m_trustModelObservable.setChanged();
					break;
				}
			}
			m_trustModelObservable.notifyObservers(NOTIFY_TRUST_MODEL_CHANGED);
		}
	}

	public static void setCurrentTrustModel(TrustModel a_trustModel)
	{
		if (a_trustModel == null)
		{
			return;
		}
		synchronized (ms_trustModels)
		{
			if (!ms_trustModels.contains(a_trustModel))
			{
				ms_trustModels.addElement(a_trustModel);
			}
			if (ms_currentTrustModel != a_trustModel)
			{
				ms_currentTrustModel = a_trustModel;
				m_trustModelObservable.setChanged();
			}
			m_trustModelObservable.notifyObservers(NOTIFY_TRUST_MODEL_CHANGED);
		}
	}

	public static TrustModel getTrustModelUserDefined()
	{
		return TRUST_MODEL_USER_DEFINED;
	}
	
	public static TrustModel getTrustModelDefault()
	{
		return TRUST_MODEL_DEFAULT;
	}
	
	public static TrustModel getCurrentTrustModel()
	{
		return ms_currentTrustModel;
	}

	public static void fromXmlElement(Element a_container)
	{
		int trustModelsAdded = 0;

		if (a_container != null && a_container.getNodeName().equals(XML_ELEMENT_CONTAINER_NAME))
		{

			NodeList elements = a_container.getElementsByTagName(XML_ELEMENT_NAME);
			for (int i = 0; i < elements.getLength(); i++)
			{
				try
				{
					TrustModel model = new TrustModel( (Element) elements.item(i));
					TRUST_MODEL_CUSTOM_FILTER = model;
					addTrustModel(model);					
					trustModelsAdded++;
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Could not load trust model from XML!", a_e);
				}
			}
			
			setCurrentTrustModel(XMLUtil.parseAttribute(a_container, XML_ATTR_CURRENT_TRUST_MODEL, 0l));
		}
		
		if(trustModelsAdded == 0)
		{
			TrustModel model = new TrustModel(MSG_CASCADES_FILTER, 5);
			model.setEditable(true);
			TRUST_MODEL_CUSTOM_FILTER = model;
			addTrustModel(model);
		}
	}
	
	public static void restoreDefault()
	{
		removeTrustModel(TRUST_MODEL_CUSTOM_FILTER);
	
		TrustModel model = new TrustModel(MSG_CASCADES_FILTER, 5);
		model.setEditable(true);
		TRUST_MODEL_CUSTOM_FILTER = model;
		addTrustModel(model);
		
		setCurrentTrustModel(TRUST_MODEL_DEFAULT);
	}

	public static Element toXmlElement(Document a_doc, String a_xmlContainerName)
	{
		if (a_doc == null || a_xmlContainerName == null)
		{
			return null;
		}

		Element container = a_doc.createElement(a_xmlContainerName);
		XMLUtil.setAttribute(container, XML_ATTR_CURRENT_TRUST_MODEL, getCurrentTrustModel().getId());
		synchronized (ms_trustModels)
		{
			for (int i = 0; i < ms_trustModels.size(); i++)
			{
				// Only write custom trust models to the configuration file
				if(((TrustModel)ms_trustModels.elementAt(i)).isEditable())
				{
					container.appendChild(((TrustModel)ms_trustModels.elementAt(i)).toXmlElement(a_doc));
				}
			}
		}
		return container;
	}

	public void setName(String a_strName) throws IllegalArgumentException
	{
		if (a_strName == null || a_strName.trim().length() == 0)
		{
			throw new IllegalArgumentException("Invalid name for trust model!");
		}
		m_strName = a_strName;
	}

	/**
	 * Returns the localized name of the TrustModel
	 *
	 * @return The localized name of the TrustModel
	 */
	public String getName()
	{
		return JAPMessages.getString(m_strName);
	}

	public String toString()
	{
		return getName();
	}

	public long getId()
	{
		return m_id;
	}


	public Element toXmlElement(Document a_doc)
	{
		if (a_doc == null)
		{
			return null;
		}

		Element elemTrustModel = a_doc.createElement(XML_ELEMENT_NAME);

		//XMLUtil.setAttribute(elemTrustModel, XML_ATTR_SHOW_WARNING, m_bSh owWarning);
		XMLUtil.setAttribute(elemTrustModel, XML_ATTR_ID, m_id);
		XMLUtil.setAttribute(elemTrustModel, XML_ATTR_NAME, getName());

		synchronized(m_trustAttributes)
		{
			synchronized(m_trustAttributes)
			{
				Enumeration e = m_trustAttributes.elements();
				while (e.hasMoreElements()) {
					TrustAttribute attr = (TrustAttribute) e.nextElement();
					elemTrustModel.appendChild(attr.toXmlElement(a_doc));
				}
			}
		}

		return elemTrustModel;
	}

	public boolean isPaymentForced()
	{
		TrustAttribute attr = getAttribute(PaymentAttribute.class);

		return (attr == null) ? false : (attr.getTrustCondition() == TRUST_IF_TRUE);
	}

	public boolean isAdded()
	{
		return true;
	}

	/**
	 * Checks if the current trust model has any trusted cascades at all
	 * @return true if the trust model has trusted cascades, false otherwise
	 */
	public boolean hasTrustedCascades()
	{
		Vector list = Database.getInstance(MixCascade.class).getEntryList();

		for(int i = 0; i < list.size(); i++)
		{
			if(isTrusted((MixCascade)list.elementAt(i))) return true;
		}

		return false;
	}

	public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
	{
		if (a_cascade == null)
		{
			throw (new TrustException("Cascade is null!"));
		}

		if (Database.getInstance(BlacklistedCascadeIDEntry.class).getEntryById(
			  a_cascade.getMixIDsAsString()) != null)
		{
			throw (new TrustException("Cascade is in blacklist!"));
		}

		if (a_cascade.isPayment() && PayAccountsFile.getInstance().getBI(a_cascade.getPIID()) == null)
		{
			throw (new TrustException("Payment instance for this cascade is unknown!"));
		}

		// allow to conenct to unverified cascades in user defined filter
		synchronized(m_trustAttributes)
		{
			TrustAttribute attr = (TrustAttribute)m_trustAttributes.get(UserDefinedAttribute.class);
			if (attr != null && attr.getTrustCondition() == TRUST_IF_TRUE && a_cascade.isUserDefined())
			{
				return;
			}
		}

		if (!a_cascade.isUserDefined())
		{
			super.checkTrust(a_cascade);
		}


		// test if all mixes have valid certificates.
		MixInfo info;
		for (int i = 0; i < a_cascade.getNumberOfMixes(); i++)
		{
			info = a_cascade.getMixInfo(i);
			if (info == null || !info.isVerified())
			{
				throw new SignatureException(JAPMessages.getString(MSG_EXCEPTION_NO_VALID_SIGNATURE, new Integer(i+1)));
			}
		}

		synchronized(m_trustAttributes)
		{
			Enumeration e = m_trustAttributes.elements();
			while (e.hasMoreElements()) {
				TrustAttribute attr = (TrustAttribute) e.nextElement();
				attr.checkTrust(a_cascade);
			}
		}
	}
}
