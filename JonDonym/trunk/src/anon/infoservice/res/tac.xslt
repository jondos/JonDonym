<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" 
    media-type="text/html" 
    doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
    doctype-system="http://www.w3.org/TR/html4/loose.dtd"
    indent="yes"
    encoding="utf-8"
	omit-xml-declaration="yes" />

	<xsl:template match="TermsAndConditionsTemplate">
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
			<head>
				<title><xsl:value-of select="@name" /></title>
				<style style="text/css">
					body { font-family: Lucida Grande, Arial; font-size: 90% }
					h1 { text-align: center }
					h2 { text-align: center }
					body { font-size: 12pt }
					#preamble { text-align: center }
					.operatorAddress { margin-left: 150px; }
					.url { text-align: center }
				</style>
			</head>
			<body>
				<h1><xsl:value-of select="@name" /></h1>
				<xsl:apply-templates />
			</body>
		</html>
	</xsl:template>
	
	<xsl:template match="br">
		<br />
	</xsl:template>
	
	<xsl:template match="PreAmble">
		<div id="preamble">
			<xsl:apply-templates /><br />
		</div>
	</xsl:template>
		
	<xsl:template match="PreAmble/Operator">
		<xsl:value-of select="Organisation" /><br />
		<xsl:value-of select="Street" /><br />
		<xsl:value-of select="PostalCode" /> <xsl:text> </xsl:text> <xsl:value-of select="City" /><br />
		<xsl:value-of select="OperatorCountry" /><br />
		<br />
		<xsl:text>VAT: </xsl:text> <xsl:value-of select="Vat" /><br />
		<xsl:text>Fax: </xsl:text><xsl:value-of select="Fax" /><br />
		<xsl:text>E-Mail: </xsl:text> <xsl:value-of select="EMail" /><br />
	</xsl:template>
		
	<xsl:template match="Section">
		<h2>§ <xsl:value-of select="@id" /> - <xsl:value-of select="@name" /></h2>
		<xsl:choose>
			<xsl:when test="@id=9"><b><xsl:apply-templates select="Paragraph" /></b></xsl:when>
			<xsl:otherwise><xsl:apply-templates select="Paragraph" /></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="SeverabilityClause">
		<h2>§ <xsl:value-of select="@id" /> - <xsl:value-of select="@name" /></h2>
		<p>
			<xsl:apply-templates />
		</p>
	</xsl:template>
	
	<xsl:template match="Paragraph">
		<p>(<xsl:value-of select="position()" />) <xsl:apply-templates /></p>
	</xsl:template>
	
	<xsl:template match="Paragraph/Operator">
		<p class="operatorAddress">
			<xsl:value-of select="Name" /><br />
			<xsl:value-of select="Street" /><br />
			<xsl:value-of select="PostalCode" /><xsl:text> </xsl:text><xsl:value-of select="City" /><br />
			<xsl:value-of select="Country" /><br />
			<xsl:text>Fax: </xsl:text> <xsl:value-of select="Fax" /><br />
			<xsl:text> E-Mail: </xsl:text> <xsl:value-of select="EMail" /><br />
		</p>
	</xsl:template>
	
	<xsl:template match="Paragraph/Url">
		<p class="url">
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="." />
				</xsl:attribute>
				<xsl:value-of select="." />
			</a>
		</p>
	</xsl:template>
	
	<xsl:template match="Sig">
		<br />
		<xsl:value-of select="City" />, <xsl:value-of select="Date" />
	</xsl:template>
	
	<xsl:template match="Signature"></xsl:template>
</xsl:stylesheet>
	