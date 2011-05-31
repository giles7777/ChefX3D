<xsl:stylesheet version = '1.0'
     xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>

<xsl:template match="Transform">
   
	<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment" />
						
</xsl:template>


<xsl:template match="/ChefX3D/ToolParams/Segments/Segment">
    <Transform>
	   <xsl:attribute name="translation">
		  <xsl:value-of select="@start" />
	   </xsl:attribute>                        
    
	    <Shape>
	       <Appearance>
		      <Material diffuseColor="1 0 0" />
		   </Appearance>
	       <Box size="1 1 1"/>
	    </Shape>
	</Transform>
	<xsl:if test="position() != last()">
		<Transform>
		   <xsl:attribute name="translation">
			  <xsl:value-of select="@end" />
		   </xsl:attribute>                        
		
			<Shape>
			   <Appearance>
			      <Material diffuseColor="1 0 0" />
			   </Appearance>
			   <Box size="1 1 1"/>
			</Shape>
		</Transform>
	</xsl:if>
</xsl:template>
 
</xsl:stylesheet>
