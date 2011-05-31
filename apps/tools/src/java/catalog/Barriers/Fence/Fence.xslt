<xsl:stylesheet version = '1.1'
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>

<xsl:template match="Transform">

	<ProtoInstance name="Fence">	
	
		<!-- get the global values -->
		<fieldValue name="stepped" value="{/ChefX3D/EntityParams/Sheet[@name='SMAL']/EntityDefinition/Fence/@stepped}" />	
		<fieldValue name="panelAppearance" value="{/ChefX3D/EntityParams/Sheet[@name='SMAL']/EntityDefinition/Fence/@panelAppearance}" />	
		<fieldValue name="panelTopAppearance" value="{/ChefX3D/EntityParams/Sheet[@name='SMAL']/EntityDefinition/Fence/@panelTopAppearance}" />
		<fieldValue name="panelRailAppearance" value="{/ChefX3D/EntityParams/Sheet[@name='SMAL']/EntityDefinition/Fence/@panelRailAppearance}" />
		<fieldValue name="postAppearance" value="{/ChefX3D/EntityParams/Sheet[@name='SMAL']/EntityDefinition/Fence/@postAppearance}" />	
		<fieldValue name="postTopAppearance" value="{/ChefX3D/EntityParams/Sheet[@name='SMAL']/EntityDefinition/Fence/@postTopAppearance}" />
		
		<!-- build the start array -->
		<fieldValue name="start">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/@start" />
			</xsl:attribute>		
		</fieldValue>
						
		<!-- build the end array -->
		<fieldValue name="end">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/@end" />
			</xsl:attribute>		
		</fieldValue>				

		<!-- build panelHeight -->	
		<fieldValue name="panelHeight">
			<xsl:attribute name="value">			
				<xsl:for-each select="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams">
					<xsl:value-of select="/ChefX3D/EntityParams/Sheet[@name='SMAL']/EntityDefinition/Panel/@panelHeight"/>
					<xsl:text> </xsl:text>
				</xsl:for-each>				
			</xsl:attribute>		
		</fieldValue>				
	
		<!-- build panelTopHeight -->				
		<fieldValue name="panelTopHeight">
			<xsl:attribute name="value">	
				<xsl:for-each select="/ChefX3D/ToolParams/Segments/Segment/EndVertexParams">
					<xsl:value-of select="/ChefX3D/EntityParams/Sheet[@name='SMAL']/EntityDefinition/Panel/@panelTopHeight"/>
					<xsl:text> </xsl:text>
				</xsl:for-each>		
			</xsl:attribute>		
		</fieldValue>								
	
		<!-- build the panelRailType array -->	
		<fieldValue name="panelRailType">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelRailType" />
			</xsl:attribute>		
		</fieldValue>	
					
		<!-- build the panelRailHeight array -->
		<fieldValue name="panelRailHeight">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelRailHeight" />
			</xsl:attribute>		
		</fieldValue>
						
		<!-- build the panelRailCount array -->
		<fieldValue name="panelRailCount">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelRailCount" />
			</xsl:attribute>		
		</fieldValue>
						
		<!-- build the panelOverhang array -->
		<fieldValue name="panelOverhang">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelOverhang" />
			</xsl:attribute>		
		</fieldValue>
						
		<!-- build the panelRaisedHeight array -->	
		<fieldValue name="panelRaisedHeight">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelRaisedHeight" />
			</xsl:attribute>		
		</fieldValue>
						
		<!-- build the panelSpan array -->
		<fieldValue name="panelSpan">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelSpan" />
			</xsl:attribute>		
		</fieldValue>				
						
		<!-- build the postSize array -->
		<fieldValue name="postSize">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postSize" />
			</xsl:attribute>		
		</fieldValue>
						
		<!-- build the postTopSize array -->
		<fieldValue name="postTopSize">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postTopSize" />
			</xsl:attribute>		
		</fieldValue>				
						
		<!-- build the postType array -->
		<fieldValue name="postType">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postType" />
			</xsl:attribute>		
		</fieldValue>				
						
		<!-- build the postTopType array -->
		<fieldValue name="postTopType">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postTopType" />
			</xsl:attribute>		
		</fieldValue>				
						
		<!-- build the postBraced array -->
		<fieldValue name="postBraced">
			<xsl:attribute name="value">
				<xsl:apply-templates select="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postBraced" />
			</xsl:attribute>		
		</fieldValue>
		
		<!-- add the post geometry -->				
		<fieldValue name='postGeometry'>
   			<ProtoInstance name='GeometryHolder' DEF='BOX_GEOM_{/ChefX3D/ToolParams/@entityID}'>
				<fieldValue name='name' value='Box'></fieldValue>
				<fieldValue name='coord' value='0.5 -0.5 0.5 ,0.5 0.5 0.5 ,-0.5 0.5 0.5 ,-0.5 -0.5 0.5 ,-0.5 -0.5 0.5 ,-0.5 0.5 0.5 ,-0.5 0.5 -0.5 ,-0.5 -0.5 -0.5 ,-0.5 -0.5 -0.5 ,-0.5 0.5 -0.5 ,0.5 0.5 -0.5 ,0.5 -0.5 -0.5 ,0.5 -0.5 -0.5 ,0.5 0.5 -0.5 ,0.5 0.5 0.5 ,0.5 -0.5 0.5 ,0.5 0.5 0.5 ,0.5 0.5 -0.5 ,-0.5 0.5 -0.5 ,-0.5 0.5 0.5 ,-0.5 -0.5 -0.5 ,0.5 -0.5 -0.5 ,0.5 -0.5 0.5 ,-0.5 -0.5 0.5'></fieldValue>
				<fieldValue name='texCoord' value='1.0 0.0 ,1.0 1.0 ,0.0 1.0 ,0.0 0.0 ,1.0 0.0 ,1.0 1.0 ,0.0 1.0 ,0.0 0.0 ,1.0 0.0 ,1.0 1.0 ,0.0 1.0 ,0.0 0.0 ,1.0 0.0 ,1.0 1.0 ,0.0 1.0 ,0.0 0.0 ,1.0 0.0 ,1.0 1.0 ,0.0 1.0 ,0.0 0.0 ,0.0 0.0 ,1.0 0.0 ,1.0 1.0 ,0.0 1.0'></fieldValue>
				<fieldValue name='normal' value='0.0 0.0 1.0 ,0.0 0.0 1.0 ,0.0 0.0 1.0 ,0.0 0.0 1.0 ,-1.0 0.0 0.0 ,-1.0 0.0 0.0 ,-1.0 0.0 0.0 ,-1.0 0.0 0.0 ,0.0 0.0 -1.0 ,0.0 0.0 -1.0 ,0.0 0.0 -1.0 ,0.0 0.0 -1.0 ,1.0 0.0 0.0 ,1.0 0.0 0.0 ,1.0 0.0 0.0 ,1.0 0.0 0.0 ,0.0 1.0 0.0 ,0.0 1.0 0.0 ,0.0 1.0 0.0 ,0.0 1.0 0.0 ,0.0 -1.0 0.0 ,0.0 -1.0 0.0 ,0.0 -1.0 0.0 ,0.0 -1.0 0.0'></fieldValue>
				<fieldValue name='index' value='0 1 2 2 3 0 4 5 6 6 7 4 8 9 10 10 11 8 12 13 14 14 15 12 16 17 18 18 19 16 20 21 22 22 23 20'></fieldValue>
   			</ProtoInstance>
		</fieldValue>

		<!-- add the geometry node -->
		<fieldValue name='postTopGeometry'>
   			<GeometryHolder USE='BOX_GEOM_{/ChefX3D/ToolParams/@entityID}'></GeometryHolder>
		</fieldValue>
		
		<!-- add the appearance nodes -->
		<fieldValue name='appearance'>
			<Appearance>
				<ImageTexture url='"Barriers/Fence/Textures/fence01.png"'></ImageTexture>
			</Appearance>
			<Appearance>
				<Material diffuseColor='0.5 0.5 0.5' shininess='0.7'></Material>
			</Appearance>
			<Appearance>
				<Material diffuseColor='0.0 0.0 0.0' shininess='0.7'></Material>
			</Appearance>
			<Appearance>
				<Material diffuseColor='1.0 1.0 1.0'></Material>
			</Appearance>
			<Appearance>
				<ImageTexture url='"Barriers/Fence/Textures/barbwire1.png"'></ImageTexture>
			</Appearance>
		</fieldValue>

	</ProtoInstance>
	
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/@start">
	<xsl:value-of select="."/>
	<xsl:if test="position() != last()">
		<xsl:text>,</xsl:text>
	</xsl:if>
	
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/@end">
	<xsl:value-of select='.'/>
	<xsl:if test="position() != last()">
		<xsl:text>,</xsl:text>
	</xsl:if>
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelRailType">
	<xsl:value-of select='.'/>
	<xsl:if test="position() != last()">
		<xsl:text> </xsl:text>
	</xsl:if>
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelRailHeight">
	<xsl:value-of select='.'/>
	<xsl:if test="position() != last()">
		<xsl:text> </xsl:text>
	</xsl:if>
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelRailCount">
	<xsl:value-of select='.'/>
	<xsl:if test="position() != last()">
		<xsl:text> </xsl:text>
	</xsl:if>
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelOverhang">
	<xsl:value-of select='.'/>
	<xsl:if test="position() != last()">
		<xsl:text> </xsl:text>
	</xsl:if>
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelRaisedHeight">
	<xsl:value-of select='.'/>
	<xsl:if test="position() != last()">
		<xsl:text> </xsl:text>
	</xsl:if>
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/SegmentParams/Sheet[@name='Segment']/SegmentDefinition/Panel/@panelSpan">
	<xsl:value-of select='.'/>
	<xsl:if test="position() != last()">
		<xsl:text> </xsl:text>
	</xsl:if>
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postSize">
	<xsl:value-of select='.'/>
	<xsl:text>,</xsl:text>
	<xsl:if test="position() = last()">
		<xsl:for-each select="/ChefX3D/ToolParams/Segments/Segment/EndVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postSize" >
			<xsl:if test="position() = last()">
				<xsl:value-of select='.'/>
			</xsl:if>
		</xsl:for-each>
	</xsl:if>	
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postTopSize">
	<xsl:value-of select='.'/>
	<xsl:text>,</xsl:text>
	<xsl:if test="position() = last()">
		<xsl:for-each select="/ChefX3D/ToolParams/Segments/Segment/EndVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postTopSize" >
			<xsl:if test="position() = last()">
				<xsl:value-of select='.'/>
			</xsl:if>
		</xsl:for-each>
	</xsl:if>	
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postType" >
	<xsl:value-of select='.'/>
	<xsl:text> </xsl:text>
	<xsl:if test="position() = last()">
		<xsl:for-each select="/ChefX3D/ToolParams/Segments/Segment/EndVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postType" >
			<xsl:if test="position() = last()">
				<xsl:value-of select='.'/>
			</xsl:if>
		</xsl:for-each>
	</xsl:if>	
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postTopType">
	<xsl:value-of select='.'/>
	<xsl:text> </xsl:text>
	<xsl:if test="position() = last()">
		<xsl:for-each select="/ChefX3D/ToolParams/Segments/Segment/EndVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postTopType" >
			<xsl:if test="position() = last()">
				<xsl:value-of select='.'/>
			</xsl:if>
		</xsl:for-each>
	</xsl:if>	
</xsl:template>

<xsl:template match="/ChefX3D/ToolParams/Segments/Segment/StartVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postBraced">
	<xsl:value-of select='.'/>
	<xsl:text> </xsl:text>
	<xsl:if test="position() = last()">
		<xsl:for-each select="/ChefX3D/ToolParams/Segments/Segment/EndVertexParams/Sheet[@name='Vertex']/VertexDefinition/Vertex/@postBraced" >
			<xsl:if test="position() = last()">
				<xsl:value-of select='.'/>
			</xsl:if>
		</xsl:for-each>
	</xsl:if>	
</xsl:template>

</xsl:stylesheet>
