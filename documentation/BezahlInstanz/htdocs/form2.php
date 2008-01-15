<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>

<?php
include_once("functions.php");

//----------------------------------------------------------------------------
// Input Parsing / validation
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

// rate
if( isset($_POST['rate']) && (preg_match('/^[a-zA-Z0-9]{1,64}$/', $_POST['rate'])) )
{
	$rateName = $_POST['rate'];
}
else
{
	echo("<h1>".$lang['rate-missing-head']."</h1>\n");
	echo("<p>".$lang['rate-missing-msg']."</p>\n");
	echo("<p>DEBUG Rate: ".$_POST['rate']."</p>");
	exit;
}



//----------------------------------------------------------------------------
echo("<title>" . $lang['form2-title'] . "</title>\n");
echo("<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">");
echo("</head><body>\n");


// connect to the database
$dbconn = connectToDatabase();
if( $dbconn == FALSE ) exit;


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


// fetch amount from DB
$result = pg_exec( $dbconn, "SELECT * FROM RATES WHERE ID='".$rateName."'" );
//PHP >4.2.0: $result = pg_query( $dbconn, "SELECT * FROM RATES WHERE NAME=".$rate );
$numrows = pg_numrows( $result );
//PHP >4.2.0: $numrows = pg_num_rows( $result );
if( $numrows != 1 ) {
  echo("<h1>".$lang['error']."</h1>\n");
  echo("<p>".$lang['no-rate-selected']."</p>\n");
  echo("<p><a href=\"index.php?transfernum=".$transfernum . "&lang=" . $langCode . 
       "\">" . $lang['click-here-to-go-back'] . "</a></p>\n" );
  echo("</body></html>\n");
  exit;
}
$rate = pg_fetch_object( $result, 0);
//$amount = $rate->amount;
//$fixed_amount = $rate->fixed_amount;
//$mbytes = $rate->mbytes;


// Handling for PayPal payment method:
// build PayPal Form
if( $_POST['method'] == "paypal" ) {
  echo("<h1>" . $lang['form2-paypal-head'] . "</h1>\n");
  echo("<p>" . $lang['form2-paypal-msg'] . "</p>\n");
  ?>
  <form action="https://www.paypal.com/cgi-bin/webscr" method="post">
  <input type="hidden" name="cmd" value="_xclick">
  <input type="hidden" name="business" value="paymentjap@cookiecooker.de">
  <input type="hidden" name="no_note" value="1">
  <input type="hidden" name="currency_code" value="EUR">
  <input type="hidden" name="item_number" value="<?php echo($transfernum);?>">
  <input type="hidden" name="item_name" value="<?php echo($rateName)?>">
  <table border="0" cellspacing="10" cellpadding="3">
    <tr><td colspan="2"><hr></tr>
    <tr>
      <td valign="top"><p><?php echo($lang['transfernum']);?>:</p>
       <td><p><strong><?php echo($transfernum);?></strong></p>
    </tr>
    <tr><td colspan="2"><hr></tr>
    <tr>
      <td valign="top"><p><?php echo($lang['rate']);?>:</p>
      <td><p><strong><?php echo($rate->name);?></strong></p>
    </tr>
    <tr><td colspan="2"><hr></tr>
    <tr>
      <td valign="top"><p><?php echo($lang['amount']);?>:</p>
<?php
  if($rate->fixed_amount == 't') {
    ?>
      <input type="hidden" name="amount" value="<?php echo($rate->amount);?>">
      <td valign="top"><p><strong><?php echo($rate->amount);?> Euro (fest)</strong></p>
    <?php
  }
  else {
    ?>
      <td><p><?php echo($lang['please-enter-amount'] . " (" . $rate->mbytes . " MBytes " . $lang['cost'] . " " . $rate->amount . " Euro).");?></p>
          <p><input type="text" name="amount" value="<?php echo($rate->amount);?>" size="10" class="i20"> Euro</p>
    <?php
  }
?>
    </tr>
    <tr><td colspan="2"><hr></tr>
    <tr>
      <td>
      <td><input type="submit" value="<?php echo($lang['on-to-paypal']);?>" border="0" name="submit">
    </tr>
  </table></form>
  </body></html>
  <?php
  exit;
}

// Handling of Bank Transfer
// show data for bank transfer in a spiffy table
else if( $_POST['method'] == "bank" ) {
  echo("<h1>". $lang['form2-bank-transfer-head'] . "</h1>\n");
  echo($lang['form2-bank-transfer-msg']);
  ?>
  <table border="0" cellspacing="3" cellpadding="3">
  <tr><td colspan="2"><hr></tr>
  <tr>
    <td><p><?php echo($lang['receiver']);?>:</p>
    <td><p>TragMichEin!!</p>
  </tr>
  <tr><td colspan="2"><hr></tr>
  <tr>
    <td><p><?php echo($lang['accountnumber']);?>:</p>
    <td><p>TragMichEin!!</p>
  </tr>
  <tr><td colspan="2"><hr></tr>
  <tr>
    <td><p><?php echo($lang['bank-code']);?>:</p>
    <td><p>TragMichEin!!</p>
  </tr>
  <tr><td colspan="2"><hr></tr>
  <tr>
    <td><p><?php echo($lang['name-of-bank'])?>:</p>
    <td><p>TragMichEin!!</p>
  </tr>
  <tr><td colspan="2"><hr></tr>
  <tr>
    <td><p><?php echo($lang['amount']);?>:</p>
<?php 
if( $rate->fixed_amount == 't' ) echo("    <td><p>$rate->amount Euro\n");
else echo("    <td><p>" . $lang['depending-on-volume'] . " (" . $rate->amount . " Euro " . $lang['per'] . " MByte)\n");
?>
  </tr>
  <tr><td colspan="2"><hr></tr>
  <tr>
    <td><p><?php echo($lang['reason-for-transfer']);?>:</p>
    <td><p><?php echo( insert_separators($transfernum) . "-" . $rateName );?></p>
  </tr>
  <tr><td colspan="2"><hr></tr>
  </table></body></html>
  <?php
  exit;
}


// Handling of Dummy payment method ... simply insert new balance into DB :)
//
else if($_POST['method']== "dummy") {
	echo("<h1>".$lang['form2-dummy-head']."</h1>");
	
	// is this a rate with fixed amount??
	if($rate->fixed_amount == 't' ) {
		echo($lang['fixed-amount']."<br/>");
		echo($lang['amount'].":".$rate->mbytes."<br/>");
		?>
		<form action="dummypayment.php" method="post">
		<input type="hidden" name="transfernum" value="<?php echo($transfernum);?>">
		<input type="hidden" name="mbytes" value="<?php echo($rate->mbytes);?>">
		<input type="hidden" name="valid_months" value="<?php echo($rate->valid_months);?>">
		<input type="hidden" name="valid_days" value="<?php echo($rate->valid_days);?>">
		<input type="hidden" name="fixed_amount" value="ja">
		<input type="submit" value="Weiter">
		</form>
		<?php
	}
	else {
		echo("<p>".$lang['please-choose-amount']."</p>");
		?>
		<form action="dummypayment.php" method="post">
		<input type="hidden" name="transfernum" value="<?php echo($transfernum);?>">
		Betrag: <input type="text" name="amount" value="<?php echo($rate->amount);?>">
		<input type="hidden" name="mbytes" value ="<?php echo($rate->mbytes);?>">
		<input type="hidden" name="valid_months" value="<?php echo($rate->valid_months);?>">
		<input type="hidden" name="valid_days" value="<?php echo($rate->valid_days);?>">
		<input type="hidden" name="fixed_amount" value="nein">
		<input type="submit" value="Weiter">
		</form>
		<?php
	}
	echo("<p>".$lang['form2-dummy-msg']."</p>");
}


// No payment method selected: Error
else {
  echo("<h1>" . $lang['error'] . "</h1>\n");
  echo("<p>" . $lang['no-rate-selected'] . "</p>\n");
  echo("<p><a href=\"index.php?transfernum=" . $transfernum . "&lang=" . $langCode . 
       "\">" . $lang['click-here-to-go-back'] . "</a></p>\n");
  echo("</body></html>\n");
  exit;
}
