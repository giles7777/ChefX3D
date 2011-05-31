<xsl:stylesheet version = '1.0'
     xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>

<xsl:template match="/" >
   <xsl:apply-templates select="ChefX3D/ToolParams" />
</xsl:template>

<xsl:template match="ToolParams" >
		<Inline url="{@url}" />
</xsl:template>

</xsl:stylesheet>