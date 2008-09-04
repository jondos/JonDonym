package infoservice;

import anon.util.MyStringBuilder;
import java.text.NumberFormat;
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
		sb.append(ms_NumberFormat.format(ms_lNrOfGetMinJapVersion));
		sb.append("</td></tr><tr><td>GET Requests for /status: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetStatus));
		sb.append("</td></tr><tr><td>GET Requests for /performanceinfo: </td><td>");		
		sb.append(ms_NumberFormat.format(ms_lNrOfPerformanceInfoRequests));
		sb.append("</td></tr><tr><td>GET Requests for Payment: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfGetPaymentRequests));
		sb.append("</td></tr><tr><td>Unknown Requests: </td><td>");
		sb.append(ms_NumberFormat.format(ms_lNrOfUnknownRequests));
		sb.append("</td></tr><tr><td><br></td><td>");
		sb.append("</td></tr><tr><td>Active forwarders: </td><td>");
		sb.append(ms_NumberFormat.format(Database.getInstance(ForwarderDBEntry.class).getNumberOfEntries()));
		sb.append("</td></tr><tr><td><br></td><td>");
		sb.append("</td></tr><tr><td>Total Memory: </td><td>");
		sb.append(ms_NumberFormat.format(Runtime.getRuntime().totalMemory()));
		sb.append("</td></tr><tr><td>Free Memory: </td><td>");
		sb.append(ms_NumberFormat.format(Runtime.getRuntime().freeMemory()));
		sb.append("</td></tr></table>");
		return sb.toString();
	}
}
