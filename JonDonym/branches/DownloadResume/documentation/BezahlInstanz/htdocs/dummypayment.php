<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>

<?php
include_once("functions.php");

// international language support, load the appropriate language ini file
//$str = "../i18n/lang_" . $_POST['lang'] . "/lang.ini";
//$lang = parse_ini_file( $str );

//----------------------------------------------------------------------------
// Input Parsing / Validation
//----------------------------------------------------------------------------

// language code
if( (isset($_POST['lang'])) && (preg_match('/^[a-zA-Z]{2,3}$/', $_POST['lang'])) )
{
	$langCode = $_POST['lang'];
}
else 
{	// silently set default language german
	$langCode = "de";
}
// international language support, load the appropriate language ini file
$str = "i18n/lang_" . $langCode . "/lang.ini";
$lang = parse_ini_file( $str );


// transfer number
if( isset($_POST['transfernum']) && (preg_match('/^[0-9]{10,20}$/', $_POST['transfernum'])) )
{
	$transfernum = $_POST['transfernum'];
}
else
{	// fail...
	echo("<h1>".$lang['transnum-missing-head']."</h1>\n");
	echo("<p>".$lang['transnum-missing-msg']."</p>\n");
	exit;
}

// mbytes
if( isset($_POST['mbytes']) && (preg_match('/^[0-9]{1,10}$/', $_POST['mbytes'])))
{
	$mbytes = $_POST['mbytes'];
}
else
{
	echo("<h1>Error</h1><p>Wrong parameters</p>\n");
	exit;
}

// fixed_amount
if( isset($_POST['fixed_amount']) && (preg_match('/^(ja|nein)$/', $_POST['fixed_amount'])) )
{
}
else
{
	echo("<h1>Error</h1><p>Wrong parameters</p>");
	exit;
}

// amount
if($_POST['fixed_amount'] == 'nein') 
{
	if( !isset($_POST['amount']) || !preg_match('/^[0-9]{1,10}$/', $_POST['amount']))
	{
		echo("<h1>Error</h1><p>Wrong parameters</p>");
		exit;
	}
}

$validdays = $_POST['valid_days'];
settype($validdays, "integer");
$validmonths = $_POST['valid_months'];
settype($validmonths, "integer");



//----------------------------------------------------------------------------
echo("<title>".$lang['dummy-head']."</title>\n");
echo("<link rel=\"stylesheet\" type=\"text/css\" href=\"../style.css\">");
echo("</head><body>\n");


// connect to the database
$dbconn = connectToDatabase();
if( $dbconn == FALSE ) exit;


//----------------------------------------------------------------------------
// FInd out account number and old deposit.

$query1 = "SELECT A.DEPOSIT,A.ACCOUNTNUMBER FROM ACCOUNTS A, TRANSFERS T ".
	"WHERE A.ACCOUNTNUMBER=T.ACCOUNTNUMBER AND ".
	"T.TRANSFERNUMBER=".$transfernum;
//echo("Executing Query: ".$query1."<br>");

$result = pg_exec( $query1 );
$numrows = pg_numrows( $result );
if($numrows==0) {
	echo("Database error .. account number not found!");
}
$account = pg_fetch_object($result, 0);




//----------------------------------------------------------------------------
// calculate new deposit

if($_POST['fixed_amount'] == 'ja') {
	$deposit_plus = floor( $mbytes * 1024*1024); // db stores deposit in Bytes!
}
else {
	$deposit_plus = floor( $_POST['amount'] * 1024*1024 * $mbytes );
}
$new_deposit = $account->deposit + $deposit_plus;

// calculate new valid time
$now123 = localtime();
$unixtime = mktime( 
		$now123[2], // hour
		$now123[1], // minute
		0, // second
		$now123[4] + 1 + $validmonths, // month
		$now123[3] + 1 + $validdays, // day of month
		$now123[5] + 1900 // year
	);
$new_validtime = strftime("%Y-%m-%d %H:%M", $unixtime);
echo("<p>".$lang['deposit-valid-to']." ".$new_validtime."</p>\n");


//----------------------------------------------------------------------------
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

$result = pg_exec( $dbconn,   $query);
if($result == FALSE) 
{
	$error_message = "Account ".$account->accountnumber." not found in database\n";
	echo("$error_message\n");
	exit;
}

echo("<p>".$lang['dummy-msg']."</p>\n");
?>

