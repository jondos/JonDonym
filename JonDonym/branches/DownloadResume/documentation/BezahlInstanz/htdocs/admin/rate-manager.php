<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
  <title>Tarifmodell-Verwaltung</title>
  <link rel="stylesheet" type="text/css" href="../style.css">
</head>
<body>
<h1>Tarifmodell-Verwaltung</h1>
<?php

include_once("../functions.php");

echo("WICHTIG!!! Dieses Script muss unbedingt htaccess-geschuetzt werden, sonst kann jeder direkt auf die Datenbank zugreifen<br>");

// not possible without selecting a language
if( !isset($_GET['lang']) ) {
  echo("<p>Bitte einen Language-Code als GET-Variable angeben!</p>");
?>
<form action="rate-manager.php" method="get">
<input type="text" name="lang" value="<?php echo($_GET['lang']);?>">
<input type="submit" value="Sprache &auml;ndern">
<?php
  exit;
}


// Load Postgresql PHP extension
/*
if( dl("pgsql.so") == FALSE ) {
  echo("<h1>".$lang['server-error']."</h1>\n");
  echo("<p>".$lang['server-error-msg']."</p>\n");
  echo("</body></html>\n");
  exit;
}
*/

// connect to the database
$dbconn = connectToDatabase();
if( $dbconn == FALSE ) exit;

// check if a new record is to be added
if( isset($_GET['newid'])) {
  echo("<hr>Creating new record ..<br>");
  $querystring = "INSERT INTO RATES VALUES (".$_GET['newid'].",'noname',0,false,0,0,0)";
  echo("Query: ".$querystring."<br>");
  $result = pg_exec( $dbconn, $querystring);
  $querystring = "INSERT INTO RATE_DESCR_".$_GET['lang']." VALUES (".$_GET['newid'].
    ", '<p>hier Beschreibung in Sprache ".$_GET['lang']." eintragen</p>')";
  echo("Query: ".$querystring."<br>");
  $result = pg_exec( $dbconn, $querystring);
  echo("\nOK<hr>\n\n\n");
}

// check if a record is to be deleted
else if( isset($_GET['delid'])) {
  echo("<hr>Deleting record ..<br>");
  $querystring = "DELETE FROM RATES WHERE ID=".$_GET['delid'];
  echo("Query: ".$querystring."<br>");
  $result = pg_exec( $dbconn, $querystring);
  echo("\nOK<hr>\n\n\n");
}

// check if a record must be submitted to the db
else if( isset($_GET['id']) ) {
  echo("<hr>Submitting record ..<br>");
  if( isset($_GET['fixed_amount'] )) $is_fixed="true"; else $is_fixed="false";
  $querystring = "UPDATE RATES SET NAME='".$_GET['name']."',".
    "AMOUNT=".$_GET['amount'].",FIXED_AMOUNT=".$is_fixed.",MBYTES=".$_GET['mbytes'].
    ",VALID_DAYS=".$_GET['valid_days'].",VALID_MONTHS=".$_GET['valid_months'].
    " WHERE ID=".$_GET['id'];
  echo("<pre>Query: ".$querystring."<br></pre>");
  $result = pg_exec( $dbconn, $querystring);
  $querystring = "UPDATE RATE_DESCR_".$_GET['lang']." SET HTML_DESCR='".$_GET['html_descr']."' WHERE ID=".$_GET['id'];
  echo("<pre>Query2: ".$querystring."<br></pre>");
  $result = pg_exec( $dbconn, $querystring);
  echo("\nOK<hr>\n\n\n");
}

// Form to change description language
?>
<form action="rate-manager.php" method="get">
<input type="text" name="lang" value="<?php echo($_GET['lang']);?>">
<input type="submit" value="Sprache &auml;ndern">
</form>


<?php
// fetch rate info from DB
$result = pg_exec( $dbconn, "SELECT * FROM RATES ORDER BY ID" );
//PHP >4.2.0: $result = pg_query( $dbconn, "SELECT * FROM RATES ORDER BY ID" );
$numrows = pg_numrows( $result );
//PHP >4.2.0: $numrows = pg_num_rows( $result );


if( $numrows == 0 ) {
  echo("<p>Noch keine Tarifmodelle eingetragen</p>");
  ?>
  <form action="rate-manager.php" method="get">
  <input type="hidden" name="newid" value="1">
  <input type="hidden" name="lang" value="<?php echo($_GET['lang']);?>">
  <input type="submit" value="Eintrag erzeugen">
  </form>
  <?php
}
else {
  ?>
  <table border="1">
  <tr><td>ID<td>Name (sprachunabh&auml;ngig)<td>Beschreibung<td>Betrag<td>Festbetrag?<td>MBytes<td>Tage<td>Monate</tr>
  <?php
  for($i=0; $i<$numrows; $i++) {
    $row = pg_fetch_object( $result, $i);
    
    // try fetch rate descriptions in the current language from DB
    // first check if the language table exists, otherwise create it
    $querystring = "SELECT relname FROM PG_CLASS WHERE RELNAME='rate_descr_".$_GET['lang']."'";
    $result2 = pg_exec( $dbconn, $querystring);
    $numrows2 = pg_numrows($result2);
    if($numrows2 == 0) {
      $result2 = pg_exec( $dbconn, "CREATE TABLE RATE_DESCR_".$_GET['lang'].
			  " (id serial references rates on delete cascade,html_descr varchar(4000))");
    }

    // now check if the entry in our table exists, otherwise insert it
    $result2 = pg_exec( $dbconn, "SELECT * FROM RATE_DESCR_".$_GET['lang']." WHERE ID=".$row->id );
    $numrows2 = pg_numrows($result2);
    if($numrows2 == 0) {
      $result2 = pg_exec( $dbconn, "INSERT INTO RATE_DESCR_".$_GET['lang']." VALUES (".$row->id.
			  ", '<p>hier beschreibung in Sprache ".$_GET['lang']." eintragen</p>')" );
      $result2 = pg_exec( $dbconn, "SELECT * FROM RATE_DESCR_".$_GET['lang']." WHERE ID=".$row->id );
    }
    $row2 = pg_fetch_object( $result2, 0);

    ?>
    <tr>
      <form method="get" action="rate-manager.php">
      <input type="hidden" name="lang" value="<?php echo($_GET['lang']);?>">
      <td><?php echo($row->id);?></td><input type="hidden" name="id" value="<?php echo($row->id);?>">
      <td><input type="text" name="name" value="<?php echo($row->name);?>"></td>
      <td><textarea cols="20" rows="5" name="html_descr"><?php echo($row2->html_descr);?></textarea></td>
      <td><input type="text" size="10" name="amount" value="<?php echo($row->amount);?>"></td>
      <td><input type="checkbox" name="fixed_amount" value="1" <?php if($row->fixed_amount=='t') echo("checked=\"checked\" ");?>></td>
      <td><input type="text" size="10" name="mbytes" value="<?php echo($row->mbytes);?>"></td>
      <td><input type="text" size="4" name="valid_days" value="<?php echo($row->valid_days);?>"></td>
      <td><input type="text" size="4" name="valid_months" value="<?php echo($row->valid_months);?>"></td>
      <td><input type="submit" value="speichern"></td>
      </form>
      <form action="rate-manager.php" method="get">
      <input type="hidden" name="delid" value="<?php echo($row->id);?>">
      <input type="hidden" name="lang" value="<?php echo($_GET['lang']);?>">
      <td><input type="submit" value="l&ouml;schen"></td>
      </form>
    </tr>
    <?php
  }
  echo("</table>\n");
  ?>

  <form action="rate-manager.php" method="get">
  <input type="text" name="newid" value="<?php echo($i+1);?>">
  <input type="hidden" name="lang" value="<?php echo($_GET['lang']);?>">
  <input type="submit" value="Eintrag mit dieser ID erzeugen">
  </form>
  <?php
}

?>