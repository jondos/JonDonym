package infoservice;

import anon.util.MyStringBuilder;
import anon.util.Util;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import anon.infoservice.Database;
import infoservice.japforwarding.ForwarderDBEntry;

/**
 * This class collects some statistic information about the IS runtime.*/
final class ISRuntimeStatistics
{	
	//How many TCP/IP Connections did we receive?
	static volatile long ms_lTCPIPConnections=0;

	///How many get /mixcascadestatus command did we process?
	static volatile long ms_lNrOfGetMixCascadeStatusRequests=0;

	///How many post commands did we process?
	static volatile long ms_lNrOfPosts=0;

	///How many post commands did we process?
	static volatile long ms_lNrOfGets=0;

	///How many unknown commands did we process?
	static volatile long ms_lNrOfUnknownRequests=0;

	///How many commands that were not get/post did we process?
	static volatile long ms_lNrOfOtherMethod=0;

	///How many get /mixinfo/ commands did we process?
	static volatile long ms_lNrOfGetMixinfoRequests=0;

	///How many get /cascadeinfo/ commands did we process?
	static volatile long ms_lNrOfGetCascadeinfoRequests=0;

	///How many get /infoservices commands did we process?
	static volatile long ms_lNrOfGetInfoservicesRequests=0;

	///How many get /infoserviceserials commands did we process?
	static volatile long ms_lNrOfGetInfoserviceserialsRequests=0;

	///How many get /cascadeserials commands did we process?
	static volatile long ms_lNrOfGetCascadeserialsRequests=0;

	///How many get /cascades commands did we process?
	static volatile long ms_lNrOfGetCascadesRequests=0;

	///How many get commands for forwarding did we process?
	static volatile long ms_lNrOfGetForwarding=0;

	///How many get commands for /status did we process?
	static volatile long ms_lNrOfGetStatus=0;

	///How many get /paymentinstances or /paymentinstance commands did we process?
	static volatile long ms_lNrOfGetPaymentRequests=0;
	
	///How many get /performanceentries commands did we process?
	static volatile long ms_lNrOfPerformanceInfoRequests=0;

	///How many get /tornodes command did we process?
	static volatile long ms_lNrOfGetTorNodesRequests=0;

	///How many get min jap version command did we process?
	static volatile long ms_lNrOfGetMinJapVersion=0;

	static NumberFormat ms_NumberFormat=NumberFormat.getInstance();
	
	private static Hashtable[] ms_hashClientVersions;
	private static int ms_currentHour;
	
	static
	{
		// create a hashtable for 24 hours so we get statistics for one day;
		ms_hashClientVersions = new Hashtable[24];
		for (int i = 0; i < ms_hashClientVersions.length; i++)
		{
			ms_hashClientVersions[i] = new Hashtable();
		}
		ms_currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
	}
	
	static void putClientVersion(String a_property, String a_value)
	{
		if (a_property == null || a_value == null)
		{
			return;
		}
		int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		
		synchronized (ms_hashClientVersions[hour])
		{
			if (ms_currentHour != hour)
			{
				ms_hashClientVersions[hour].clear();
				ms_currentHour = hour;
			}
			putClientVersion(a_property, a_value, ms_hashClientVersions[hour], BigInteger.valueOf(1));
		}
	}
	
	private static void putClientVersion(String a_property, String a_value, Hashtable a_hashtable, BigInteger a_addedValue)
	{
		Hashtable hashProperty;
		BigInteger count;
		
		// this is just a flooding protection
		if (a_property.length() > 100 || a_value.length() > 100)
		{
			// do nothing
			return;
		}
		a_property = a_property.toLowerCase();
		if (a_value.endsWith("Sun Microsystems Inc."))
		{
			a_value = a_value.substring(0, a_value.lastIndexOf("Sun Microsystems Inc.")).trim();
		}
		if (a_value.endsWith("/"))
		{
			a_value = a_value.substring(0, a_value.length() - 1).trim();
		}
		
		synchronized(a_hashtable)
		{	
			hashProperty = (Hashtable)a_hashtable.get(a_property);
			if (hashProperty == null)
			{
				// this is just a flooding protection
				if (a_hashtable.size() > 20)
				{
					// do nothing
					return;
				}
				
				hashProperty = new Hashtable();
			}
			
			count = (BigInteger)hashProperty.get(a_value);
			if (count == null)
			{
				// this is just a flooding protection
				if (hashProperty.size() > 100)
				{
					// do nothing
					return;
				}
				
				count = a_addedValue;
			}
			else
			{
				count = count.add(a_addedValue);				
			}
			hashProperty.put(a_value, count);
			a_hashtable.put(a_property, hashProperty);
		}
	}

	static String getAsHTML()
	{
		MyStringBuilder sb=new MyStringBuilder(512);
		sb.append("<table>");
		sb.append("<tr><td>TCP/IP Connections received: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lTCPIPConnections));
		sb.append("</td></tr><tr><td>Total GET Requests: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGets));
		sb.append("</td></tr><tr><td>Total POST Requests: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfPosts));
		sb.append("</td></tr><tr><td>Total other than GET/POST Requests: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfOtherMethod));
		sb.append("</td></tr><tr><td><br></td><td>");
		
		/*
		sb.append("</td></tr><tr><td>GET Requests for /mixcascadestatus/: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetMixCascadeStatusRequests));
		sb.append("</td></tr><tr><td>GET Requests for /cascadeserials: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetCascadeserialsRequests));
		sb.append("</td></tr><tr><td>GET Requests for /cascades: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetCascadesRequests));
		sb.append("</td></tr><tr><td>GET Requests for /infoserviceserials: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetInfoserviceserialsRequests));
		sb.append("</td></tr><tr><td>GET Requests for /infoservices: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetInfoservicesRequests));
		sb.append("</td></tr><tr><td>GET Requests for /mixinfo/: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetMixinfoRequests));
		sb.append("</td></tr><tr><td>GET Requests for /cascadeinfo/: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetCascadeinfoRequests));
		sb.append("</td></tr><tr><td>GET Requests for Forwarding: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetForwarding));
		sb.append("</td></tr><tr><td>GET Requests for /tornodes: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetTorNodesRequests));
		sb.append("</td></tr><tr><td>GET Requests for /currentjapversion: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetMinJapVersion));*/
		sb.append("</td></tr><tr><td>GET Requests for /status: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetStatus));
		/*
		sb.append("</td></tr><tr><td>GET Requests for /performanceinfo: </td><td>");		
		sb.append(ms_NumberFormat.format(ms_lNrOfPerformanceInfoRequests));
		sb.append("</td></tr><tr><td>GET Requests for Payment: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetPaymentRequests));*/
		sb.append("</td></tr><tr><td>Unknown Requests: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfUnknownRequests));
		sb.append("</td></tr><tr><td><br></td><td>");
		sb.append("</td></tr><tr><td>Active forwarders: </td><td>");
		sb.append(ms_NumberFormat.format(Database.getInstance(ForwarderDBEntry.class).getNumberOfEntries()));
		sb.append("</td></tr><tr><td><br></td><td>");
		
		Hashtable hashVersionStrings = new Hashtable();
		Enumeration enumProperties, enumValues;
		String strProperty, strValue;
		Hashtable hashValue;
		for (int i = 0; i < ms_hashClientVersions.length; i++)
		{
			synchronized (ms_hashClientVersions[i])
			{
				enumProperties = ms_hashClientVersions[i].keys();
				while (enumProperties.hasMoreElements())
				{
					strProperty = (String)enumProperties.nextElement();
					hashValue = (Hashtable)ms_hashClientVersions[i].get(strProperty);
					enumValues = hashValue.keys();
					while (enumValues.hasMoreElements())
					{
						strValue = (String)enumValues.nextElement();
						putClientVersion(strProperty, strValue, hashVersionStrings, (BigInteger)hashValue.get(strValue));
					}
				}
			}
		}
		
		// now print all the properties
		BigInteger totalCounts;
		BigInteger currentCount;
		Vector vecValues;
		BigInteger allStatisticsCount = BigInteger.valueOf(0);
		
		enumProperties = hashVersionStrings.keys();
		while (enumProperties.hasMoreElements())
		{
			strProperty = (String)enumProperties.nextElement();
			hashValue = (Hashtable)hashVersionStrings.get(strProperty);
			sb.append("</td></tr><tr><td>" + strProperty + ": </td><td>");
			
			totalCounts = BigInteger.valueOf(0);
			vecValues = new Vector();
			enumValues = hashValue.keys();
			while (enumValues.hasMoreElements())
			{
				strValue = (String)enumValues.nextElement();
				currentCount = (BigInteger)hashValue.get(strValue);
				int i = 0;
				for (; i < vecValues.size(); i++)
				{
					if (currentCount.compareTo((BigInteger)hashValue.get(vecValues.elementAt(i))) >= 0)
					{
						break;
					}
				}
				vecValues.insertElementAt(strValue, i);
				totalCounts = totalCounts.add(currentCount);
			}
			allStatisticsCount = allStatisticsCount.add(totalCounts);
			
			enumValues = vecValues.elements();
			while (enumValues.hasMoreElements())
			{
				strValue = (String)enumValues.nextElement();
				currentCount = (BigInteger)hashValue.get(strValue);
				currentCount = currentCount.multiply(BigInteger.valueOf(1000));
				currentCount = currentCount.divide(totalCounts);
				//if (currentCount.intValue() > 0)
				{
					sb.append(strValue + " (" + currentCount.doubleValue() / 10.0 + "%)");
					
					if (enumValues.hasMoreElements())
					{
						sb.append(", ");
					}
				}
			}
		}
		sb.append("</td></tr><tr><td>statistics total: </td><td>" + allStatisticsCount.toString());
		
		sb.append("</td></tr><tr><td><br></td><td>");
		sb.append("</td></tr><tr><td>Total Memory: </td><td>");
		sb.append(ms_NumberFormat.format(Runtime.getRuntime().totalMemory()));
		sb.append("</td></tr><tr><td>Free Memory: </td><td>");
		sb.append(ms_NumberFormat.format(Runtime.getRuntime().freeMemory()));
		
		sb.append("</td></tr></table>");
		return sb.toString();
	}
}
