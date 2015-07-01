<?xml version="1.0" encoding="windows-1251"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>
    <xsl:template match="/Hello">
        <xsl:value-of select="."/>
    </xsl:template>
    <xsl:template match="text()"/>
</xsl:stylesheet>
