<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
<?php

// Include utility functions
include_once("functions.php");

//----------------------------------------------------------------------------
// input parsing / validation
//----------------------------------------------------------------------------

// language code
if( (isset($_GET['lang'])) && (preg_match('/^[a-zA-Z]{2,3}$/', $_GET['lang'])) )
{
	$langCode = $_GET['lang'];
	$showLangBar = 0;
}
else 
{	// silently set default language german
	$langCode = "de";
	$showLangBar = 1;
}

// international language support, load the appropriate language ini file
$lang = parse_ini_file( "i18n/lang_".$langCode."/lang.ini");
if( $showLangBar == 1 ) // show the "select your language" bar if appropriate
{
	include_once("i18n/i18n.php");
}


// transfer number
if( isset($_GET['transfernum']) && (preg_match('/^[0-9]{10,20}$/', $_GET['transfernum'])) )
{
	$transfernum = $_GET['transfernum'];
}
else
{	// fail...
	echo("<h1>".$lang['transnum-missing-head']."</h1>\n");
	echo("<p>".$lang['transnum-missing-msg']."</p>\n");
	exit;
}



//----------------------------------------------------------------------------
echo("  <title>".$lang['form1-head']."</title>\n");
echo("  <link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">\n");
echo("</head>\n");
echo("<body>\n");


$dbconn = connectToDatabase();
if( $dbconn == FALSE ) exit;

// do on-line check
$check = checkTransferNum( $dbconn, $transfernum );
if($check!=0) 
{
	echo("<h1>".$lang['transnum-invalid-head']."</h1>\n");
	echo("<p>".$lang['transnum-invalid-msg']."</p>\n");
	echo("<p>Transfer number: ".$transfernum."</p>");
	echo("<p>Error number: ".$check."</p>");
	echo("</body></html>\n");
	exit;
}


//----------------------------------------------------------------------------
// Transfernumber is OK. Print the HTML form
echo("<h1>".$lang['form1-head']."</h1>\n");
echo("<p>".$lang['form1-msg']."</p>\n");
?>
<form action="form2.php" method="post">
<input type="hidden" name="lang" value="<?php echo($langCode);?>">
<table cellspacing="3" cellpadding="3">
  <tr><td colspan="3"><hr></tr>
  <tr>
    <td valign="top"><p><?php echo($lang['transfernum']);?>:</p>
    <td>
    <td><input type="text" name="transfernum" size="30" class="i20" value="<?php echo($transfernum);?>">
  </tr>
  <tr><td colspan="3"><hr></tr>
<?php


//----------------------------------------------------------------------------
// get available rates from DB // TODO: limit this to only one price
$result = pg_exec( $dbconn, "SELECT * FROM RATES ORDER BY ID" );
$numrows = pg_numrows( $result ); // PHP > 4.2.0: $numrows = pg_num_rows( $result );

// get rate descriptions in the user's language
$result2 = pg_exec( $dbconn, "SELECT * FROM RATE_DESCR_".$langCode." ORDER BY ID" );

for ($i=0; $i<$numrows; $i++) {
  $row = pg_fetch_object($result, $i);
  $row2 = pg_fetch_object($result2, $i);

  if($i==0) echo("  <tr>\n    <td valign=\"top\"><p>".$lang['rate'].":</p>\n");
  else echo("  <tr>\n    <td>\n");
  echo( "    <td valign=\"top\"><input type=\"radio\" name=\"rate\" value=\"" . $row->id . "\">\n" );
  echo( "    <td><p><strong>".$row->name. ":</strong></p>".$row2->html_descr . "<p>&nbsp;</p>\n" );
  echo( "  </tr>\n");
}

?>
  <tr><td colspan="3"><hr></tr>
  <tr>
    <td valign="top"><p><?php echo($lang['payment-method']);?>:</p>
    <td><input type="radio" name="method" value="paypal">
    <td><p>
		<?php 
			echo($lang['payment-paypal1'].
					" <a target=\"_blank\" href=\"http://www.paypal.com\">PayPal</a>. ".
					$lang['payment-paypal2']
				);
		?>
		</p>
  </tr>
  <tr>
    <td>
    <td><input type="radio" name="method" value="bank">
    <td><p><?php echo($lang['payment-bank-transfer']);?></p>
  </tr>
  <tr>
    <td>
    <td><input type="radio" name="method" value="dummy">
    <td><p><?php echo($lang["payment-dummy"]);?></p>
  </tr>
  <tr><td colspan="3"><hr></tr>
  <tr>
    <td valign="top"><p><?php echo($lang['to-next-form']);?>:</p>
    <td>
    <td><input type="submit" name="submit" value="<?php echo($lang['next']);?>">
  </tr>
</table>
</form>
</body></html>
