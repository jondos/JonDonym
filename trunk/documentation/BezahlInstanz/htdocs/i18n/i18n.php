<?php

// This file provides an internationalization (i18n) bar that can be shown on
// each page just by include_once'ing this file. The user can then select other languages

$location = "i18n";
$all = opendir( $location );
echo("<table cellspacing=\"3\" cellpadding=\"3\"><tr>\n");
echo("<td>Select your language:</td>\n");
while ($file = readdir($all)) {
  if (is_dir($location.'/'.$file) and substr($file, 0, 5) == "lang_" ) {
    echo("<td><a href=\"index.php?lang=" . substr($file, 5, 2) . "\"><img src=\"".$location."/".$file."/flag.gif\"></a></td>\n");
  }
}
echo("</tr></table><hr>\n");
closedir($all);
unset($all);


?>