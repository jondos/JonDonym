<?php
////////////////////////////////////////////////////////////////////////////////
// Paypal IPN Notification Script for JAP Payment Instance
// (c) 2003 Bastian Voigt <bavoigt@inf.fu-berlin.de>
//
// Please check the configuration settings below before using this script:
//
////////////////////////////////////////////////////////////////////////////////

include_once("functions.php");


// Warnings, errors and possible debug info go to this address
// $site_email = "admin@accounting-instance.com"
$site_email = "bavoigt";

// Set this to 0 to disable debug emails
$notify_debug = 1;

// Subject line for error emails
$error_subject = "paypal-verify.php: Paypal Notification Error";



// If you are using PHP >= 4.3, you can enable ssl communication for
// better privacy. This script will then do all its communications
// with PayPal over an encrypted SSL link.
//
// To enable SSL, use the following settings:
// $use_ssl = 1;
// $verify_host = "www.paypal.com";
// $verify_port = 443;
//
$use_ssl = 0;
$verify_host = "www.paypal.com";
$verify_port = 80;



// main (!) address of the paypal business account
$paypal_receiver_email = "jap@inf.tu-dresden.de";

// accept payments with "unverified" status?
$accept_unverified = 'yes';

// accept payments with "unconfirmed" status?
$accept_unconfirmed = 'yes';


// Header lines for debug email
$date = date("D, j M Y H:i:s O");
$crlf = "\n";
$debug_headers = "From: $site_email" .$crlf;
$debug_headers .= "Reply-To: $site_email" .$crlf;
$debug_headers .= "Return-Path: $site_email" .$crlf;
$debug_headers .= "X-Mailer: Perl-Studio" .$crlf;
$debug_headers .= "Date: $date" .$crlf; 
$debug_headers .= "X-Sender-IP: $REMOTE_ADDR" .$crlf; 


// Paypal Addresses and URLs
$verify_url = "/cgi-bin/webscr";
$paypal_iprange_regexp = "/^65.206/";


// end of the configuration section
////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////


// this is only called if everything went smoothly
function doDatabaseUpdate($transnum, $ratename, $amount)
{
  global $site_email;
  global $error_subject;

  // connect to the database
  $dbconn = connectToDatabase();
  if( $dbconn == FALSE ) exit;


  // check transfer number
  $check = checkTransferNum( $dbconn, $transnum );
  if( $check != 0 ) {
	mail($site_email, "Transfer number invalid! Error no. ".$check, "hallo");
	exit;
  }

  // get account data from DB
  $result = pg_exec( $dbconn, "SELECT ACCOUNTNUMBER FROM TRANSFERS WHERE TRANSFERNUMBER=".$transnum);
  $transfer = pg_fetch_object( $result, 0);

  $result = pg_exec( $dbconn, "SELECT * FROM ACCOUNTS WHERE ACCOUNTNUMBER=".$tranfer->accountnumber);
  $account = pg_fetch_object( $result, 0);


  // get rate data from db
  $result = pg_exec( $dbconn, 
		     "SELECT AMOUNT,FIXED_AMOUNT,MBYTES,VALID_DAYS,VALID_MONTHS FROM RATES WHERE ID='".
		     $ratename."'");
  $numrows = pg_numrows( $result );
  // php>4.2.0: $numrows = pg_num_rows( $result );
  if( $numrows != 1 ) {
    $error_message = "Rate ".$ratename." not found in database\n";
    echo("$error_message\n");
    mail($site_email, $error_subject."4", $error_message, $debug_headers);
    exit;
  }
  $rate = pg_fetch_object( $result, 0 );


  // calculate new balance and maxbalance
  if($rate->fixed_amount == 't') {
	$balance_plus = floor( $rate->mbytes * 1024); // db stores balance in KBytes!
  }
  else {
	$balance_plus = floor( $amount * 1024 * $rate->mbytes );
  }
  $new_balance = $account->balance + $balance_plus;
  $new_maxbalance = $account->maxbalance + $balance_plus;


  // calculate new valid time
  $now = localtime();
  $unixtime = mktime( $now[2], $now[1], 0, $now[4] + $rate->valid_months, 
		      $now[3] + $rate->valid_days, $now[5]);
  $new_validtime = strftime("%Y-%m-%d %H:%M", $unixtime );


  // finally update db
  $result = pg_exec( $dbconn, "UPDATE TRANSFERS SET USED='t' WHERE TRANSFERNUMBER=$transnum");
  if($result == FALSE) {
    $error_message = "Transnum ".$transnum." not found in database\n";
    echo("$error_message\n");
    mail($site_email, $error_subject, $error_message, $debug_headers);
    exit;
  }
  mail($site_email, "now updating accounts table for account ".$account->accountnumber, "hallo");
  $result = pg_exec( $dbconn, "UPDATE ACCOUNTS SET BALANCE=".$new_balance.
					 ", MAXBALANCE=".$new_maxbalance.
					 ", BALANCE_VALIDTIME='".$new_validtime."' ".
					 " WHERE ACCOUNTNUMBER=".$account->accountnumber);

  if($result == FALSE) {
    $error_message = "Account ".$account->accountnumber." not found in database\n";
    echo("$error_message\n");
    mail($site_email, $error_subject, $error_message, $debug_headers);
    exit;
  }
}

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////


// check receiver email address
if( $paypal_receiver_email != $_POST['receiver_email'] ) {
  $error_message .= "Error code 501. Possible fraud. Error with receiver_email. ";
  $error_message .= "receiver_email = ".$_POST['receiver_email']."\n";
  echo("$error_message\n");
  mail($site_email, $error_subject, $error_message, $debug_headers);
  exit;
}  


// check whether the request comes from the correct paypal IP range
if(!preg_match($paypal_iprange_regexp, $_SERVER['REMOTE_ADDR'])) {
  $error_message .= "Error code 502. Possible fraud. Error with REMOTE IP ADDRESS";
  $error_message .= "= ".$_SERVER['REMOTE_ADDR'] ." . The remote address of the script posting ";
  $error_message .= "to this notify script does not match a valid PayPal ip address\n";
  echo("$error_message\n");
  mail($site_email, $error_subject, $error_message, $debug_headers);
  exit;
}


// put all posted vars + the validate-command in one string
$workString = 'cmd=_notify-validate';
reset($_POST);
while(list($key, $val) = each($_POST)) { 
  $debug_string .= $key.'='.$val.'\n'; 
  $val = stripslashes($val);
  $val = urlencode($val); 
  $workString .= '&' .$key .'=' .$val; 
}


// send debugging info email no. 1
if($notify_debug){
  $debugmess = "Variables sent from PayPal\n==========================\n$debug_string\n\n";
  $str = $workString;
  $str = str_replace('&', "\n", $str );
  $debugmess .= "workString posted back to PayPal\n==========================\n$str\n\n";
  mail($site_email, "JPI: Paypal Notify Debug Results 1", $debugmess, $debug_headers);
}


// post back to PayPal using fsockopen with https url 
// and receive response
$header = "POST $verify_url HTTP/1.0\r\n";
$header .= "Content-Type: application/x-www-form-urlencoded\r\n";
$header .= "Content-Length: " . strlen ($workString) . "\r\n\r\n";

if( $use_ssl ) {
  // in PHP>4.3.0 you can use a ssl (https) connection: 
  $fp = fsockopen ("ssl://$verify_host", $verify_port, $errno, $errstr, 60);
}
else {
  // This is for php < 4.3:
  $fp = fsockopen ("http://$verify_host", $verify_port, $errno, $errstr, 60);
}

if ( $fp == FALSE ) {
  // HTTPS ERROR
  $error_message = "Error connecting to $verify_host on port $verify_port. \n".
    "Error message: $errstr, error code: $errno\n";
  echo($error_message);
  mail( $site_email, $error_subject, $error_message, $debug_headers);
  exit;
}
else {
  fputs ($fp, $header . $workString);
  $verify_response = fread($fp,1024);
  $status = socket_get_status($fp);
  $bytes_left = $status['unread_bytes'];
  if ($bytes_left > 0) { 
    $verify_response .= read($fp, $bytes_left); 
  }
  fclose ($fp);
}


// Handle response and decide whether to accept or refuse the payment
if ( ereg('VERIFIED', $verify_response) ) { 
  if ( (eregi('Completed',$_POST['payment_status'])) && ($error == 0) ) {
    if (eregi('unverified',$_POST['payer_status'])) { 
      if($accept_unverified == 'yes'){       
		doDatabaseUpdate($_POST['item_number'],$_POST['item_name'],$_POST['mc_gross']);
		$debug_status = "Accepted: VERIFIED-completed-unverified payer status";
      }
      else {
		$debug_status = "Refused: VERIFIED-unverified response";
      }  
    }
    else if (eregi('unconfirmed',$_POST['address_status'])) {  
      mail($site_email, "unconfirmed", "completed");
      if($accept_unconfirmed == 'yes'){
		doDatabaseUpdate($_POST['item_number'],$_POST['item_name'],$_POST['mc_gross']);
		$debug_status = "Accepted: VERIFIED-completed response with unconfirmed address status";
      }
      else {
		$debug_status = "Refused: VERIFIED-completed-unconfirmed address status";
      }  
    }
    else {
      doDatabaseUpdate($_POST['item_number'],$_POST['item_name'],$_POST['mc_gross']);
      $debug_status = "Accepted: VERIFIED-completed response, Order Complete";
    }
  }   // end payment status complete
  else {
    $debug_status = "Refused: VERIFIED, but paymentstatus is ".$_POST['payment_status'].", should be 'completed'";
  }
}   // end VERIFIED response from paypal
else if (ereg('INVALID',$verify_response)) { 
  $valid_post = 'INVALID POST'; 
  $debug_status = "Refused: INVALID! PayPal returned an INVALID response";
} 
else {
  $debug_status = "Undefined status";
}

// send debug message no. 2
if($notify_debug) {
  mail( $site_email, "paypal-verify.php: Skript durchgelaufen", $debug_status, $debug_headers);
}

exit;

?>
