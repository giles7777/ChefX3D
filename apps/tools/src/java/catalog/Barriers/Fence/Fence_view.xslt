<xsl:stylesheet version = '1.0'
     xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>

<xsl:output method="xml" omit-xml-declaration="yes" indent="yes"/>

<xsl:template match="Transform">
   
   <ProtoDeclare name='GeometryHolder'>
      <ProtoInterface>
         <field accessType='inputOutput' type='SFString' name='name' value=''></field>
         <field accessType='inputOutput' type='SFInt32' name='geometryType' value='0'></field>
         <field accessType='inputOutput' type='MFVec3f' name='coord'></field>
         <field accessType='inputOutput' type='MFVec2f' name='texCoord'></field>
         <field accessType='inputOutput' type='MFVec3f' name='normal'></field>
         <field accessType='inputOutput' type='MFInt32' name='index'></field>
         <field accessType='inputOutput' type='SFNode' name='appearance'></field>
      </ProtoInterface>
      <ProtoBody>
         <WorldInfo></WorldInfo>
      </ProtoBody>
   </ProtoDeclare>
   <ProtoDeclare name='Fence'>
      <ProtoInterface>
         <field accessType='initializeOnly' type='SFInt32' name='fencesPerShape' value='1'></field>
         <field accessType='initializeOnly' type='MFVec3f' name='start'></field>
         <field accessType='initializeOnly' type='MFVec3f' name='end'></field>
         <field accessType='initializeOnly' type='MFFloat' name='panelHeight'></field>
         <field accessType='initializeOnly' type='MFFloat' name='panelTopHeight'></field>
         <field accessType='initializeOnly' type='MFBool' name='panelSpan'></field>
         <field accessType='initializeOnly' type='MFInt32' name='postType'></field>
         <field accessType='initializeOnly' type='MFInt32' name='postTopType'></field>
         <field accessType='initializeOnly' type='MFVec3f' name='postSize'></field>
         <field accessType='initializeOnly' type='MFVec3f' name='postTopSize'></field>
         <field accessType='initializeOnly' type='MFBool' name='postBraced'></field>
         <field accessType='initializeOnly' type='MFNode' name='postGeometry'></field>
         <field accessType='initializeOnly' type='MFNode' name='postTopGeometry'></field>
         <field accessType='initializeOnly' type='SFInt32' name='panelAppearance' value='0'></field>
         <field accessType='initializeOnly' type='SFInt32' name='panelTopAppearance' value='0'></field>
         <field accessType='initializeOnly' type='SFInt32' name='panelRailAppearance' value='0'></field>
         <field accessType='initializeOnly' type='SFInt32' name='postAppearance' value='0'></field>
         <field accessType='initializeOnly' type='SFInt32' name='postTopAppearance' value='0'></field>
         <field accessType='initializeOnly' type='MFNode' name='appearance'></field>
         <field accessType='initializeOnly' type='MFInt32' name='panelRailType'></field>
         <field accessType='initializeOnly' type='MFFloat' name='panelRailHeight'></field>
         <field accessType='initializeOnly' type='MFInt32' name='panelRailCount'></field>
         <field accessType='initializeOnly' type='MFFloat' name='panelOverhang'></field>
         <field accessType='initializeOnly' type='MFFloat' name='panelRaisedHeight'></field>
         <field accessType='initializeOnly' type='SFBool' name='stepped' value='false'></field>
      </ProtoInterface>
      <ProtoBody>
         <Transform DEF='HOLDER'>
            <Shape>
               <IndexedTriangleSet solid='false' index='0 1 2 1 3 2'>
                  <Coordinate DEF='COORD' point='0.0 1.0 0.0 ,0.0 0.0 0.0 ,1.0 1.0 0.0 ,1.0 0.0 0.0'></Coordinate>
                  <Normal vector='0.0 0.0 1.0 ,0.0 0.0 1.0 ,0.0 0.0 1.0 ,0.0 0.0 1.0'></Normal>
                  <TextureCoordinate point='0.0 0.0 ,0.0 1.0 ,1.0 0.0 ,1.0 1.0'></TextureCoordinate>
               </IndexedTriangleSet>
            </Shape>
         </Transform>
         <Script DEF='SCRIPT'>
            <field accessType='initializeOnly' type='SFInt32' name='fencesPerShape'>
            </field>
            <field accessType='initializeOnly' type='MFFloat' name='panelHeight'>
            </field>
            <field accessType='initializeOnly' type='MFFloat' name='panelTopHeight'>
            </field>
            <field accessType='initializeOnly' type='MFVec3f' name='start'>
            </field>
            <field accessType='initializeOnly' type='MFVec3f' name='end'>
            </field>
            <field accessType='initializeOnly' type='MFInt32' name='postType'>
            </field>
            <field accessType='initializeOnly' type='MFInt32' name='postTopType'>
            </field>
            <field accessType='initializeOnly' type='MFVec3f' name='postSize'>
            </field>
            <field accessType='initializeOnly' type='MFVec3f' name='postTopSize'>
            </field>
            <field accessType='initializeOnly' type='SFInt32' name='panelAppearance'>
            </field>
            <field accessType='initializeOnly' type='SFInt32' name='panelTopAppearance'>
            </field>
            <field accessType='initializeOnly' type='SFInt32' name='panelRailAppearance'>
            </field>
            <field accessType='initializeOnly' type='SFInt32' name='postAppearance'>
            </field>
            <field accessType='initializeOnly' type='SFInt32' name='postTopAppearance'>
            </field>
         <field accessType='initializeOnly' type='MFNode' name='postGeometry'>         </field>
         <field accessType='initializeOnly' type='MFNode' name='postTopGeometry'>         </field>
         <field accessType='initializeOnly' type='MFNode' name='appearance'>         </field>
         <field accessType='outputOnly' type='MFNode' name='children'>         </field>
         <IS>
            <connect nodeField='fencesPerShape' protoField='fencesPerShape'></connect>
            <connect nodeField='panelHeight' protoField='panelHeight'></connect>
            <connect nodeField='panelTopHeight' protoField='panelTopHeight'></connect>
            <connect nodeField='start' protoField='start'></connect>
            <connect nodeField='end' protoField='end'></connect>
            <connect nodeField='postType' protoField='postType'></connect>
            <connect nodeField='postTopType' protoField='postTopType'></connect>
            <connect nodeField='postSize' protoField='postSize'></connect>
            <connect nodeField='postTopSize' protoField='postTopSize'></connect>
            <connect nodeField='postGeometry' protoField='postGeometry'></connect>
            <connect nodeField='postTopGeometry' protoField='postTopGeometry'></connect>
            <connect nodeField='panelAppearance' protoField='panelAppearance'></connect>
            <connect nodeField='panelTopAppearance' protoField='panelTopAppearance'></connect>
            <connect nodeField='panelRailAppearance' protoField='panelRailAppearance'></connect>
            <connect nodeField='postAppearance' protoField='postAppearance'></connect>
            <connect nodeField='postTopAppearance' protoField='postTopAppearance'></connect>
            <connect nodeField='appearance' protoField='appearance'></connect>
         </IS>
            <![CDATA[Barriers/Fence/classes/catalog.Barriers.Fence.Fence.class]]>
            </Script>
         <ROUTE fromNode='SCRIPT' fromField='children' toNode='HOLDER' toField='children'></ROUTE>
         </ProtoBody>
	</ProtoDeclare>

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
