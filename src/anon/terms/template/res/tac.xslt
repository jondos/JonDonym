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
					.operatorAddress { text-align: center }
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
	
	<xsl:template match="b">
		<xsl:choose>
			<xsl:when test="count(./child::*) = 0">
				<b><xsl:value-of select="."/></b>
			</xsl:when>
			<xsl:otherwise>
				<xsl:for-each select="./child::*">
					<b>
						<xsl:apply-templates select="."/>
					</b>
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="Preamble">
		
		<div id="preamble">
			<xsl:apply-templates /><br />
		</div>
	
	</xsl:template>
		
	<xsl:template match="//Operator">
		<p class="operatorAddress">
			<xsl:value-of select="Organisation" /><br />
			<xsl:apply-templates select="./AdditionalInfo"/>
			<xsl:value-of select="Street" /><br />
			<xsl:value-of select="PostalCode" /> <xsl:text> </xsl:text> <xsl:value-of select="City" /><br />
			<xsl:value-of select="OperatorCountry" /><br />
			<xsl:apply-templates select="./Vat"/>
			<xsl:apply-templates select="./Fax"/>
			<xsl:text>E-Mail: </xsl:text> <xsl:value-of select="EMail" /><br />
		</p>
	</xsl:template>
	
	<xsl:template match="Operator/Vat">
		<xsl:text>VAT: </xsl:text> <xsl:value-of select="." /><br />
	</xsl:template>
	
	<xsl:template match="Operator/AdditionalInfo">
		<xsl:value-of select="." /><br />
	</xsl:template>
	
	<xsl:template match="Operator/Fax">
			<xsl:text>Fax: </xsl:text><xsl:value-of select="." /><br />
	</xsl:template>
	
	<xsl:template match="Preamble/LeadingText">
		<p>
		<xsl:value-of select="."/>
		</p>
	</xsl:template>
	
	<xsl:template match="Preamble/TrailingText">
		<p>
		<xsl:value-of select="."/>
		</p>
	</xsl:template>
		
	<xsl:template match="Section">
		<h2>§ <xsl:value-of select="position()" /> - <xsl:value-of select="@name" /></h2>
		<xsl:apply-templates select="Paragraph" />
	</xsl:template>
	
	<xsl:template match="Paragraph">
			<p>
				<xsl:if test="count(../Paragraph) &gt;1">
					(<xsl:value-of select="position()" />) 
				</xsl:if>
				<xsl:apply-templates />
			</p>
	</xsl:template>
	
	<!-- <xsl:template match="//Operator">
		<xsl:value-of select="."/>
		<p class="operatorAddress">
			<xsl:value-of select="Name" /><br />
			<xsl:value-of select="Street" /><br />
			<xsl:value-of select="PostalCode" /><xsl:text> </xsl:text><xsl:value-of select="City" /><br />
			<xsl:value-of select="Country" /><br />
			<xsl:text>Fax: </xsl:text> <xsl:value-of select="Fax" /><br />
			<xsl:text> E-Mail: </xsl:text> <xsl:value-of select="EMail" /><br />
		</p> 
	</xsl:template> -->
	
	<!--  <xsl:template match="Paragraph/Url">
		<p class="url">
			<a>
				<xsl:attribute name="href">
					<xsl:value-of select="." />
				</xsl:attribute>
				<xsl:value-of select="." />
			</a>
		</p>
	</xsl:template> -->
	
	<xsl:template match="Url">
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
	