<?php

// connect to the database
// and output error message if not successful
function connectToDatabase() {
  global $lang;
  $dbconn = pg_pconnect( "host=localhost port=5432 dbname=BIdb user=bi" ); //"requiressl"
  if($dbconn == FALSE) {
      echo("<h1>".$lang['dbconn-error-head']."</h1>\n");
      echo("<p>".$lang['dbconn-error-msg']."</p>\n");
      echo("</body></html>\n");
      return FALSE;
  }
  return $dbconn;
}


// check transfer number
// return value >0 means error (number not usable for transactions)
function checkTransferNum( $dbconn, $transfernumber )
{
  global $lang;

  // check if transfernumber is contained in DB
  $result = pg_exec( $dbconn,
  		"SELECT used,deposit,accountnumber," .
		"to_char(validtime,'YYYY-MM-DD HH24:MI:SS') as validtime " .
		"FROM TRANSFERS WHERE TRANSFERNUMBER=".$transfernumber
		);
  // php>4.2.0: $result = pg_query( $dbconn, "SELECT * FROM TRANSFERS WHERE TRANSFERNUMBER=".$transfernumber);
  $numrows = pg_numrows( $result );
  // php>4.2.0: $numrows = pg_num_rows( $result );
  if( $numrows != 1 )
	{
    return 2;
  }

  // check if transfernumber was already used or is too old
  $transfer = pg_fetch_object( $result, 0 );
  if($transfer->used == 't')
	{
    return 3;
  }

  // check if transfernumber's deposit is the same as account's deposit
  $result = pg_exec( $dbconn, "SELECT DEPOSIT FROM ACCOUNTS WHERE ACCOUNTNUMBER=".
					 $transfer->accountnumber );
  $account = pg_fetch_object( $result, 0 );
  if($transfer->deposit != $account->deposit)
	{
    return 4;
  }

  // check validTime
//  $pos = strrpos( $account->validtime, '.' );
// obsolete  if($pos>0) $str = substr( $account->validtime, 0, $pos );
 $str = $transfer->validtime;
  if(strtotime( $str ) < time() )
	{
    return 5;
  }

  return 0;
}


function insert_separators($transfernum) {
  $output = $transfernum / 100000000 % 10000;
  $output .= "-" . ($transfernum / 10000) % 10000;
  $output .= "-" . $transfernum % 10000;
  return $output;
}

?>
