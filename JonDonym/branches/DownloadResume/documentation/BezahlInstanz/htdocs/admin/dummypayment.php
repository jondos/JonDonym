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

//----------------------------------------------------------------------------
// Input Parsing / Validation
//----------------------------------------------------------------------------

// transfer number
if( isset($_POST['transnum']) && (preg_match('/^[0-9]{10,20}$/', $_POST['transnum'])) )
{
	$transfernum = $_POST['transnum'];
}
else
{	// fail...
	echo("<h1>".$lang['transnum-missing-head']."</h1>\n");
	echo("<p>".$lang['transnum-missing-msg']."</p>\n");
	exit;
}

$validdays = $_POST['valid_days'];
settype($validdays, "integer");
$validmonths = $_POST['valid_months'];
settype($validmonths, "integer");



//----------------------------------------------------------------------------
echo("<title>Dummypayment</title>\n");
echo("<link rel=\"stylesheet\" type=\"text/css\" href=\"../style.css\">");
echo("</head><body>\n");



// connect to the database
$dbconn = connectToDatabase();
if( $dbconn == FALSE ) exit;



// FInd out account number and old deposit.
$query1 = "SELECT A.DEPOSIT,A.ACCOUNTNUMBER FROM ACCOUNTS A, TRANSFERS T ".
	"WHERE A.ACCOUNTNUMBER=T.ACCOUNTNUMBER AND ".
	"T.TRANSFERNUMBER=".$transfernum;
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


// calculate new valid time
echo("Calculating new validtime<br>");
$now = localtime();
echo("Months: ".$now["tm_mon"]." + ".$validmonths." = ".($now["tm_mon"]+$validmonths)."<br>");
echo("Days: ".$now["tm_mday"]." + ".$validdays." = ".($now["tm_mday"]+$validdays)."<br>");
$unixtime = mktime( 
		$now["tm_hour"], 
		$now["tm_min"], 
		0, 
		$now["tm_mon"] + 1 + $validmonths, 
		$now["tm_mday"] + 1 + $validdays, 
		$now["tm_year"] + 1900
	);
$new_validtime = strftime("%Y-%m-%d %H:%M", $unixtime);


// finally update db 
$result = pg_exec( $dbconn, "UPDATE TRANSFERS SET USED='t' WHERE TRANSFERNUMBER=".$transfernum);
if($result == FALSE) 
{
	$error_message = "Transnum ".$transfernum." not found in database\n";
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
