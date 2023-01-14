<#function toJavaType attribute>
    <#local javaType='String'>

    <#if attribute.type == 'INTEGER'>
        <#local javaType="Long" />
    </#if>
    <#if attribute.type == 'DECIMAL'>
        <#local javaType="Double" />
    </#if>
    <#if attribute.type == 'DATE_TIME'>
        <#local javaType="Instant" />
    </#if>
    <#if attribute.type == 'BOOLEAN'>
        <#local javaType="Boolean" />
    </#if>
    <#if attribute.type == 'BINARY'>
        <#local javaType="byte[]" />
    </#if>
    <#if attribute.type == 'COMPLEX'>
        <#local javaType="${attribute.name?cap_first}" />
    </#if>
    <#return attribute.multiValued?then("List<", "") + javaType + attribute.multiValued?then(">", "") />
</#function>

<#function doNotIgnoreAttribute attribute>
    <#assign name=attribute.name?lower_case />
    <#return name != 'id' && name != 'externalid' && name != 'meta'>
</#function>

<#function isRequired attribute>
    <#return attribute.isRequired()>
</#function>
