<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>

<?php
include_once("../functions.php");

/********************************************

Note this script is totally insecure and 
for testing only!!!

********************************************/

// international language support, load the appropriate language ini file
//$str = "../i18n/lang_" . $_POST['lang'] . "/lang.ini";
//$lang = parse_ini_file( $str );

echo("<title>Dummypayment</title>\n");
echo("<link rel=\"stylesheet\" type=\"text/css\" href=\"../style.css\">");
echo("</head><body>\n");


// Load Postgresql PHP extension - this is needed only if
// not already done in PHP.INI

//$result = dl("pgsql.so");
//if( $result == FALSE ) {
//  echo("<h1>".$lang['server-error']."</h1>\n");
//  echo("<p>".$lang['server-error-msg']."</p>\n");
//  echo("</body></html>\n");
//  exit;
//}
//

// connect to the database
$dbconn = connectToDatabase();
if( $dbconn == FALSE ) exit;

$validdays = $_POST['valid_days'];
$validmonths = $_POST['valid_months'];


// FInd out account number and old deposit.// SECURITY RISK!!! SQL INJECTION MAYBE POSSIBLE
	$query1 = "SELECT A.DEPOSIT,A.ACCOUNTNUMBER FROM ACCOUNTS A, TRANSFERS T ".
		"WHERE A.ACCOUNTNUMBER=T.ACCOUNTNUMBER AND ".
		"T.TRANSFERNUMBER=".$_POST['transfernum'];
	$result = pg_exec( $query1 );
	$numrows = pg_numrows( $result );
	if($numrows==0) {
		echo("Database error .. account number not found!");
	}
	$account = pg_fetch_object($result, 0);

	// calculate new deposit
	if($rate->fixed_amount == 't') {
		$deposit_plus = floor( $_POST['mbytes'] * 1024*1024); // db stores deposit in Bytes!
	}
	else {
		$deposit_plus = floor( $_POST['amount'] * 1024*1024 * $_POST['mbytes'] );
  	}
  	$new_deposit = $account->deposit + $deposit_plus;
//  	$new_maxbalance = $account->maxbalance + $balance_plus;


  	// calculate new valid time
	echo("Calculating new validtime<br>");
  	$now = localtime();
	echo("Months: ".$now[4]." + ".$validmonths." = ".($now[4]+$validmonths)."<br>");
	echo("Days: ".$now[3]." + ".$validdays." = ".($now[3]+$validdays)."<br>");

  	$unixtime = mktime( $now[2], $now[1], 0, $now[4] + $validmonths, 
		      $now[3] + $validdays, $now[5]);
  	$new_validtime = strftime("%Y-%m-%d %H:%M", $unixtime);


  // finally update db --- SQL INJECTION AGAIN
  $result = pg_exec( $dbconn, "UPDATE TRANSFERS SET USED='t' WHERE TRANSFERNUMBER=".$_POST['transfernum']);
  if($result == FALSE) {
    $error_message = "Transnum ".$_POST['transnum']." not found in database\n";
    echo("$error_message\n");
    exit;
  }

  $query = "UPDATE ACCOUNTS SET DEPOSIT=".$new_deposit.
					 ", DEPOSITVALIDTIME='".$new_validtime."' ".
					 " WHERE ACCOUNTNUMBER=".$account->accountnumber;

	echo("Executing Query: ".$query);
  $result = pg_exec( $dbconn,   $query);
  if($result == FALSE) {
    $error_message = "Account ".$account->accountnumber." not found in database\n";
    echo("$error_message\n");
    exit;
  }

?>	
