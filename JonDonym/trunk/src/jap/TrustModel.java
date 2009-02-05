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
import java.lang.reflect.Method;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import gui.JAPMessages;
import anon.client.BasicTrustModel;
import anon.infoservice.Database;
import anon.infoservice.IServiceContextContainer;
import anon.infoservice.MixCascade;
import anon.infoservice.ServiceOperator;
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
	private static TrustModel TRUST_MODEL_DEFAULT;
	private static TrustModel TRUST_MODEL_PREMIUM;

	private static TrustModel TRUST_MODEL_CUSTOM_FILTER;
	
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
	public static final String MSG_SERVICES_WITHOUT_COSTS = TrustModel.class.getName() + "_servicesWithoutCosts";
	private static final String MSG_SERVICES_USER_DEFINED = TrustModel.class.getName() + "_servicesUserDefined";
	private static final String MSG_CASCADES_FILTER = TrustModel.class.getName() + "_servicesFilter";
	private static final String MSG_ALL_SERVICES = TrustModel.class.getName() + "_allServices";
	private static final String MSG_SERVICES_BUSINESS = TrustModel.class.getName() + "_servicesBusiness";
	private static final String MSG_SERVICES_PREMIUM_PRIVATE = TrustModel.class.getName() + "_servicesPremiumPrivate";
	
	
	private static final String MSG_EXCEPTION_PAY_CASCADE = TrustModel.class.getName() + "_exceptionPayCascade";
	private static final String MSG_EXCEPTION_FREE_CASCADE = TrustModel.class.getName() + "_exceptionFreeCascade";
	private static final String MSG_EXCEPTION_WRONG_SERVICE_CONTEXT = TrustModel.class.getName() + "_wrongServiceContext";
	private static final String MSG_EXCEPTION_NOT_ENOUGH_MIXES = TrustModel.class.getName() + "_exceptionNotEnoughMixes";
	private static final String MSG_EXCEPTION_EXPIRED_CERT = TrustModel.class.getName() + "_exceptionExpiredCert";
	private static final String MSG_EXCEPTION_NOT_USER_DEFINED = TrustModel.class.getName() + "_exceptionNotUserDefined";
	private static final String MSG_EXCEPTION_TOO_FEW_COUNTRIES = TrustModel.class.getName() + "_exceptionTooFewCountries";
	private static final String MSG_EXCEPTION_NOT_INTERNATIONAL = TrustModel.class.getName() + "_exceptionNotInternational";
	private static final String MSG_EXCEPTION_INTERNATIONAL = TrustModel.class.getName() + "_exceptionInternational";
	private static final String MSG_EXCEPTION_NOT_ENOUGH_ANON = TrustModel.class.getName() + "_exceptionNotEnoughAnon";
	private static final String MSG_EXCEPTION_BLACKLISTED = TrustModel.class.getName() + "_exceptionBlacklisted";
	private static final String MSG_EXCEPTION_NOT_ENOUGH_SPEED = TrustModel.class.getName() + "_exceptionNotEnoughSpeed";
	private static final String MSG_EXCEPTION_RESPONSE_TIME_TOO_HIGH = TrustModel.class.getName() + "_exceptionResponseTimeTooHigh";	
	
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
	
	private static final TrustModel CONTEXT_MODEL_PREMIUM;
	private static final TrustModel CONTEXT_MODEL_PREMIUM_PRIVATE;
	private static final TrustModel CONTEXT_MODEL_BUSINESS;
	private static final TrustModel CONTEXT_MODEL_FREE;
	private static final TrustModel CONTEXT_MODEL_ALL;
	
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
		public static final String XML_ATTR_IGNORE_NO_DATA = "ignoreNoData";
		
		private int m_category;
		private boolean m_bIgnoreNoDataAvailable;
		private int m_trustCondition;
		private Object m_conditionValue;

		private TrustAttribute(int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			m_trustCondition = a_trustCondition;
			m_conditionValue = a_conditionValue;
			m_category = CATEGORY_DEFAULT;
			m_bIgnoreNoDataAvailable = a_bIgnoreNoDataAvailable;
		}

		public static int getDefaultValue()
		{
			return 0;
		}
		
		public boolean isNoDataIgnored()
		{
			return m_bIgnoreNoDataAvailable;
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

		public boolean isTrusted(MixCascade a_cascade)
		{
			try
			{
				checkTrust(a_cascade);
				return true;
			}
			catch(TrustException a_e)
			{
				// ignore
			}
			catch (SignatureException a_e)
			{
				// ignore
			}
			return false;						
		}
		
		public abstract void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException;
		
		public Element toXmlElement(Document a_doc)
		{
			if(a_doc == null) return null;

			Element el = a_doc.createElement(XML_ELEMENT_NAME);
			XMLUtil.setAttribute(el, XML_ATTR_NAME, this.getClass().getName());
			XMLUtil.setAttribute(el, XML_ATTR_TRUST_CONDITION, m_trustCondition);
			XMLUtil.setAttribute(el, XML_ATTR_IGNORE_NO_DATA, m_bIgnoreNoDataAvailable);

			if (m_conditionValue instanceof Integer)
			{
				XMLUtil.setAttribute(el, XML_ATTR_CONDITION_VALUE, ((Integer) m_conditionValue).intValue());
			}
			else if (m_conditionValue instanceof Vector)
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
			boolean bIgnoreNoData = XMLUtil.parseAttribute(a_e, XML_ATTR_IGNORE_NO_DATA, false);			

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
				
				attr = getInstance(Class.forName(name), trustCondition, value, bIgnoreNoData);
				if (attr == null)
				{
					throw new XMLParseException(XML_ELEMENT_NAME, "Could not create TrustAttribute + " + name + "!");
				}
			}
			catch(Exception ex)
			{
				throw new XMLParseException(XML_ELEMENT_NAME, ex.getMessage());
			}
			
			return attr;
		}
		
		public static TrustAttribute getInstance(Class a_attr, int a_trustCondition, Object a_conditionValue, 
				boolean a_bIgnoreNoDataAvailable)
		{
			try
			{
				return (TrustAttribute) a_attr.getConstructor(new Class[] { int.class, Object.class, boolean.class })
			 	.newInstance(new Object[] { new Integer(a_trustCondition), a_conditionValue, new Boolean(a_bIgnoreNoDataAvailable)});
			}
			catch(Exception ex) 
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Could not create " + a_attr);
				return null;
			}			
		}
	}

	public static class ContextAttribute extends TrustAttribute
	{
		public ContextAttribute(int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			super(a_trustCondition, a_conditionValue, a_bIgnoreNoDataAvailable);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			String strContextCascade = a_cascade.getContext();
			String strContextModel = JAPModel.getInstance().getContext();
			
			if (getTrustCondition() == TRUST_IF_TRUE && !strContextCascade.equals(strContextModel))
			{
				throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_WRONG_SERVICE_CONTEXT));
			}
			else if (getTrustCondition() == TRUST_IF_NOT_TRUE && strContextCascade.equals(strContextModel))
			{
				throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_WRONG_SERVICE_CONTEXT));
			}
			else if (!(strContextCascade.equals(strContextModel) ||
					(strContextModel.startsWith(IServiceContextContainer.CONTEXT_JONDONYM) &&
					strContextCascade.equals(IServiceContextContainer.CONTEXT_JONDONYM_PREMIUM))))
			{
				throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_WRONG_SERVICE_CONTEXT));
			}
		}
	};
	
	
	public static class PaymentAttribute extends TrustAttribute
	{
		public PaymentAttribute(int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			super(a_trustCondition, a_conditionValue, a_bIgnoreNoDataAvailable);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if (a_cascade.isPayment())
			{
				if (getTrustCondition() == TRUST_IF_NOT_TRUE)
				{
					throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_PAY_CASCADE));
				}
			}
			else if (getTrustCondition() == TRUST_IF_TRUE)
			{
				throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_FREE_CASCADE));
			}
		}
	};

	public static class NumberOfMixesAttribute extends TrustAttribute
	{
		public NumberOfMixesAttribute(int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			// MUST always be TRUST_IF_AT_LEAST
			super(TRUST_IF_AT_LEAST, a_conditionValue, a_bIgnoreNoDataAvailable);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			int minMixes = ((Integer) getConditionValue()).intValue();
			
			if (getTrustCondition() == TRUST_IF_AT_LEAST && (a_cascade == null ||  
				a_cascade.getNumberOfOperators() < minMixes))
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_NOT_ENOUGH_MIXES)));
			}
		}
	};

	public static class UserDefinedAttribute extends TrustAttribute
	{
		public UserDefinedAttribute(int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			super(a_trustCondition, a_conditionValue, a_bIgnoreNoDataAvailable);
		}
		
		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if (getTrustCondition() == TRUST_IF_TRUE)
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
		public InternationalAttribute(int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			// MUST always be TRUST_IF_AT_LEAST
			super(TRUST_IF_AT_LEAST, a_conditionValue, a_bIgnoreNoDataAvailable);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if(getTrustCondition() == TRUST_IF_AT_LEAST && a_cascade.getNumberOfCountries() < ((Integer) getConditionValue()).intValue())
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_TOO_FEW_COUNTRIES)));
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
		public AnonLevelAttribute(int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			// MUST always be TRUST_IF_AT_LEAST
			super(TRUST_IF_AT_LEAST, a_conditionValue, a_bIgnoreNoDataAvailable);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			StatusInfo info = (StatusInfo) Database.getInstance(StatusInfo.class).getEntryById(a_cascade.getId());
			if(getTrustCondition() == TRUST_IF_AT_LEAST && (info == null || info.getAnonLevel() < ((Integer) getConditionValue()).intValue()))
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_NOT_ENOUGH_ANON)));
			}
		}
	}

	public static class OperatorBlacklistAttribute extends TrustAttribute
	{
		public OperatorBlacklistAttribute(int a_trustCondtion, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			// MUST always be TRUST_IF_NOT_IN_LIST and value must be a vector
			super (TRUST_IF_NOT_IN_LIST, (a_conditionValue == null || 
					!(a_conditionValue instanceof Vector)) ? new Vector() : a_conditionValue, a_bIgnoreNoDataAvailable);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			ServiceOperator mixOperator;
			ServiceOperator blacklistedOperator;
			
			if (getTrustCondition() == TRUST_IF_NOT_IN_LIST)
			{
				for(int i = 0; i < a_cascade.getNumberOfMixes(); i++)
				{
					Vector list = (Vector) getConditionValue();
					
					mixOperator = a_cascade.getMixInfo(i).getServiceOperator();
					if (list.contains(mixOperator))
					{
						throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_BLACKLISTED));
					}
					if (mixOperator.getOrganization() != null)
					{
						// additionally, test if this operator is known with another ID but the same name
						for (int j = 0; j < list.size(); j++)
						{
							blacklistedOperator = (ServiceOperator)list.elementAt(j);
							if (blacklistedOperator.getOrganization() == null)
							{
								continue;
							}
							if (blacklistedOperator.getOrganization().equals(mixOperator.getOrganization()))
							{
								throw new TrustException(JAPMessages.getString(MSG_EXCEPTION_BLACKLISTED));
							}
						}
					}
				}
			}
		}
	}

	public static class SpeedAttribute extends TrustAttribute
	{
		public SpeedAttribute(int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			// MUST always be TRUST_IF_AT_LEAST
			super(TRUST_IF_AT_LEAST, a_conditionValue, a_bIgnoreNoDataAvailable);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException
		{
			PerformanceEntry entry = PerformanceInfo.getLowestCommonBoundEntry(a_cascade.getId());
			int minSpeed = ((Integer) getConditionValue()).intValue();
			
			if (minSpeed <= 0)
			{
				// do not test speed, as all speed values are accepted
				return;
			}
			else if (entry == null || entry.getBound(PerformanceEntry.SPEED).getBound() == Integer.MAX_VALUE)  
			{
				if (isNoDataIgnored())
				{
					// no performance data available; do not test
					return;
				}
				else
				{
					throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_NOT_ENOUGH_SPEED)));
				}
			}	

			if (getTrustCondition() == TRUST_IF_AT_LEAST && (entry == null || 
				entry.getBound(PerformanceEntry.SPEED).getBound() < minSpeed))
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_NOT_ENOUGH_SPEED)));
			}
		}
	}
	
	public static class DelayAttribute extends TrustAttribute
	{
		public DelayAttribute(int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			// MUST always be TRUST_IF_AT_MOST
			super(TRUST_IF_AT_MOST, a_conditionValue, a_bIgnoreNoDataAvailable);
		}
		
		public void checkTrust(MixCascade a_cascade) throws TrustException
		{
			PerformanceEntry entry = PerformanceInfo.getLowestCommonBoundEntry(a_cascade.getId());
			int maxDelay = ((Integer) getConditionValue()).intValue();
			
			if (maxDelay == Integer.MAX_VALUE)
			{
				// do not test delay, as all delay values are accepted
				return;
			}
			
			if (entry == null || entry.getBound(PerformanceEntry.DELAY).getBound() == 0)
			{
				if (isNoDataIgnored())
				{
					// no performance data available; do not test
					return;
				}
				else
				{
					throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_RESPONSE_TIME_TOO_HIGH)));
				}
			}
			
			if (getTrustCondition() == TRUST_IF_AT_MOST && (entry == null || 
					entry.getBound(PerformanceEntry.DELAY).getBound() < 0 || 
					entry.getBound(PerformanceEntry.DELAY).getBound() > maxDelay))
			{
				throw (new TrustException(JAPMessages.getString(MSG_EXCEPTION_RESPONSE_TIME_TOO_HIGH)));
			}
		}
		
		public static int getDefaultValue()
		{
			return Integer.MAX_VALUE;
		}
	}	
	
	static
	{
		// Initialize basic trust models
		TrustModel model;

		model = new TrustModel(MSG_ALL_SERVICES, 0);
		model.setAttribute(ContextAttribute.class, TRUST_RESERVED);
		//model.setAttribute(DelayAttribute.class, TRUST_IF_AT_MOST, new Integer(8000), true);
		//model.setAttribute(SpeedAttribute.class, TRUST_IF_AT_LEAST, new Integer(50), true);
		TRUST_MODEL_DEFAULT = model;
		CONTEXT_MODEL_ALL = model;
		ms_trustModels.addElement(model);
		
		model = new TrustModel(MSG_SERVICES_BUSINESS, 0);
		model.setAttribute(PaymentAttribute.class, TRUST_IF_NOT_TRUE); // this might be altered when we have hybrid services...
		model.setAttribute(ContextAttribute.class, TRUST_IF_TRUE);
		//model.setAttribute(DelayAttribute.class, TRUST_IF_AT_MOST, new Integer(8000), true);
		//model.setAttribute(SpeedAttribute.class, TRUST_IF_AT_LEAST, new Integer(50), true);
		CONTEXT_MODEL_BUSINESS = model;

		model = new TrustModel(MSG_SERVICES_PREMIUM_PRIVATE, 2);
		model.setAttribute(PaymentAttribute.class, TRUST_IF_TRUE);
		model.setAttribute(ContextAttribute.class, TRUST_IF_NOT_TRUE);
		//model.setAttribute(DelayAttribute.class, TRUST_IF_AT_MOST, new Integer(4000), true);
		//model.setAttribute(SpeedAttribute.class, TRUST_IF_AT_LEAST, new Integer(100), true);
		model.setAttribute(NumberOfMixesAttribute.class, TRUST_IF_AT_LEAST, 3);
		CONTEXT_MODEL_PREMIUM_PRIVATE = model;

		model = new TrustModel(MSG_SERVICES_WITH_COSTS, 2);
		model.setAttribute(PaymentAttribute.class, TRUST_IF_TRUE);
		//model.setAttribute(DelayAttribute.class, TRUST_IF_AT_MOST, new Integer(4000), true);
		//model.setAttribute(SpeedAttribute.class, TRUST_IF_AT_LEAST, new Integer(100), true);
		model.setAttribute(NumberOfMixesAttribute.class, TRUST_IF_AT_LEAST, 3);
		TRUST_MODEL_PREMIUM = model;
		CONTEXT_MODEL_PREMIUM = model;
		ms_trustModels.addElement(model);

		model = new TrustModel(MSG_SERVICES_WITHOUT_COSTS, 3);
		model.setAttribute(PaymentAttribute.class, TRUST_IF_NOT_TRUE);
		//model.setAttribute(DelayAttribute.class, TRUST_IF_AT_MOST, new Integer(8000));
		//model.setAttribute(SpeedAttribute.class, TRUST_IF_AT_LEAST, new Integer(50));
		CONTEXT_MODEL_FREE = model;
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

	protected static void updateContext()
	{
		synchronized (ms_trustModels)
		{
			TrustModel temp = ms_currentTrustModel;
			
			ms_trustModels.removeElement(CONTEXT_MODEL_ALL);
			ms_trustModels.removeElement(CONTEXT_MODEL_PREMIUM);
			ms_trustModels.removeElement(CONTEXT_MODEL_FREE);
			ms_trustModels.removeElement(CONTEXT_MODEL_PREMIUM_PRIVATE);
			ms_trustModels.removeElement(CONTEXT_MODEL_BUSINESS);
			
			if (JAPModel.getInstance().getContext().equals(JAPModel.CONTEXT_JONDONYM))
			{
				ms_trustModels.insertElementAt(CONTEXT_MODEL_FREE, 0);
				ms_trustModels.insertElementAt(CONTEXT_MODEL_PREMIUM, 0);
				ms_trustModels.insertElementAt(CONTEXT_MODEL_ALL, 0);
				ms_currentTrustModel = CONTEXT_MODEL_ALL;
				
			}
			else if (JAPModel.getInstance().getContext().startsWith(JAPModel.CONTEXT_JONDONYM))
			{
				ms_trustModels.insertElementAt(CONTEXT_MODEL_PREMIUM_PRIVATE, 0);
				ms_trustModels.insertElementAt(CONTEXT_MODEL_BUSINESS, 0);
				ms_currentTrustModel = CONTEXT_MODEL_BUSINESS;
			}
			else
			{
				ms_trustModels.insertElementAt(CONTEXT_MODEL_BUSINESS, 0);
				ms_currentTrustModel = CONTEXT_MODEL_BUSINESS;
				
			}
			
			m_trustModelObservable.setChanged();
			setCurrentTrustModel(temp.getId());
		}
	}
	
	/**
	 * Creates a new TrustModel object with the specified name
	 *
	 * @param a_strName	The name of the TrustModel
	 */
	public TrustModel(String a_strName, long a_id)
	{
		super(JAPMessages.getInstance());
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
		super(JAPMessages.getInstance());
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
		super(JAPMessages.getInstance());
		XMLUtil.assertNodeName(a_trustModelElement, XML_ELEMENT_NAME);

		XMLUtil.assertNotNull(a_trustModelElement, XML_ATTR_ID);
		XMLUtil.assertNotNull(a_trustModelElement, XML_ATTR_NAME);

		m_id = XMLUtil.parseAttribute(a_trustModelElement, XML_ATTR_ID, -1l);
		m_strName = XMLUtil.parseAttribute(a_trustModelElement, XML_ATTR_NAME, null);
		m_bEditable = true;

		for (int i = 0; i < a_trustModelElement.getChildNodes().getLength(); i++)
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
			// non-editable models must not be removed
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
		return setAttribute(a_attr, a_trustCondition, false);
	}
	
	private TrustAttribute setAttribute(Class a_attr, int a_trustCondition, boolean a_bIgnoreNoDataAvailable)
	{
		return setAttribute(a_attr, a_trustCondition, (Object)null, a_bIgnoreNoDataAvailable);
	}

	public TrustAttribute setAttribute(Class a_attr, int a_trustCondition, int a_conditionValue)
	{
		return setAttribute(a_attr, a_trustCondition, a_conditionValue, false);
	}
	
	private TrustAttribute setAttribute(Class a_attr, int a_trustCondition, int a_conditionValue, boolean a_bIgnoreNoDataAvailable)
	{
		return setAttribute(a_attr, a_trustCondition, new Integer(a_conditionValue), a_bIgnoreNoDataAvailable);
	}
	
	public TrustAttribute setAttribute(Class a_attr, int a_trustCondition, Vector a_conditionValue)
	{
		return setAttribute(a_attr, a_trustCondition, a_conditionValue, false);
	}

	public void setEditable(boolean a_bEditable)
	{
		m_bEditable = a_bEditable;
	}

	public boolean isEditable()
	{
		return m_bEditable;
	}


	private TrustAttribute setAttribute(Class a_attr, int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
	{
		 return setAttribute(TrustAttribute.getInstance(a_attr, a_trustCondition, a_conditionValue, a_bIgnoreNoDataAvailable));
	}

	private TrustAttribute setAttribute(TrustAttribute a_attr)
	{
		if (a_attr != null)
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
			}
		}
		
		return a_attr;
	}

	public TrustAttribute getAttribute(Class a_attr)
	{
		if (a_attr == null)
		{
			return null;
		}
		
		synchronized(m_trustAttributes)
		{
			TrustAttribute attr = (TrustAttribute) m_trustAttributes.get(a_attr);
			if(attr == null)
			{
				Integer defaultValue = new Integer(0);
				try
				{
					Method getDefaultValue = a_attr.getMethod("getDefaultValue", (Class[]) null);
					defaultValue = (Integer) getDefaultValue.invoke(null, (Object[]) null);
				}
				catch(Exception ex)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Exception occured while trying to get the default value of a TrustAttribute: ", ex);
				}
				
				return setAttribute(a_attr, TRUST_ALWAYS, defaultValue.intValue());
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
	
	public static TrustModel getTrustModelPremium()
	{
		return TRUST_MODEL_PREMIUM;
	}
	
	public static TrustModel getTrustModelDefault()
	{
		return TRUST_MODEL_DEFAULT;
	}
	
	public static TrustModel getCurrentTrustModel()
	{
		synchronized (ms_trustModels)
		{
			return ms_currentTrustModel;
		}
	}

	public static TrustModel getCustomFilter()
	{
		return TRUST_MODEL_CUSTOM_FILTER;
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

		// allow to connect to unverified cascades in user defined filter
		synchronized(m_trustAttributes)
		{
			TrustAttribute attr = (TrustAttribute)m_trustAttributes.get(UserDefinedAttribute.class);
			if (attr != null && attr.getTrustCondition() == TRUST_IF_TRUE && a_cascade.isUserDefined())
			{
				return;
			}
		}

		
		super.checkTrust(a_cascade);
				

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
