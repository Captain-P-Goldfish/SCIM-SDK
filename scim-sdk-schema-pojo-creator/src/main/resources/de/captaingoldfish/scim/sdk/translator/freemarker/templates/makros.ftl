<#import "functions.ftl" as functions>
<#macro attributeMethodReturnType attribute lowercase=false>
    <#compress>
        <#if !functions.isRequired(attribute) && !attribute.multiValued && attribute.type != 'BOOLEAN'>Optional<</#if><#rt>
        <#if lowercase>
            <#lt>${functions.toJavaType(attribute)?lower_case}<#rt>
        <#else>
            <#lt>${functions.toJavaType(attribute)}<#rt>
        </#if>
        <#lt><#if !functions.isRequired(attribute) && !attribute.multiValued  && attribute.type != 'BOOLEAN'>></#if><#rt>
    </#compress>
</#macro>

<#macro methodAttributes attributeList extensionList=[] indent=0>
    <#assign leftIndent=''?left_pad(indent) >
    <#list attributeList as attribute>
        <#if functions.doNotIgnoreAttribute(attribute) || attribute.name == 'externalId' >
            <#lt><#if attribute?index != 0>${leftIndent}</#if>${functions.toJavaType(attribute)} ${attribute.name?uncap_first}<#sep>,
        </#if>
    </#list>
    <#lt><#if attributeList?size != 0 && extensionList?size != 0>
            <#lt>,
         <#else>
            <#rt>
         </#if>
    <#list extensionList as extension>
        <#lt>${leftIndent}${extension.name.get()?cap_first} ${extension.name.get()?uncap_first}<#sep>,
    </#list>
</#macro>

<#macro addJavadoc javadoc='' indent=2>
    <#assign leftIndent=''?left_pad(indent) >
    <#if javadoc != ''>
        <#lt>${leftIndent}/**
        <#lt>${leftIndent} * ${javadoc}
        <#lt>${leftIndent} */
    </#if>
</#macro>

<#macro addGetAttributeCall attribute indent=4>
    <#assign typeIndent=''?left_pad(indent) >
    <#if  attribute.type == 'INTEGER'>
        <#if attribute.multiValued>
            <#lt>${typeIndent}return getSimpleArrayAttribute(FieldNames.${attribute.name?upper_case}, Long.class);
        <#else>
            <#lt>${typeIndent}return getLongAttribute(FieldNames.${attribute.name?upper_case})<#rt>
            <#lt><#if functions.isRequired(attribute)>.get()</#if>;
        </#if>
    <#elseif  attribute.type == 'DECIMAL'>
        <#if attribute.multiValued>
            <#lt>${typeIndent}return getSimpleArrayAttribute(FieldNames.${attribute.name?upper_case}, Double.class);
        <#else>
            <#lt>${typeIndent}return getDoubleAttribute(FieldNames.${attribute.name?upper_case})<#rt>
            <#lt><#if functions.isRequired(attribute)>.get()</#if>;
        </#if>
    <#elseif  attribute.type == 'DATE_TIME'>
        <#if attribute.multiValued>
            <#lt>${typeIndent}return getSimpleArrayAttribute(FieldNames.${attribute.name?upper_case}, Instant.class);
        <#else>
            <#lt>${typeIndent}return getDateTimeAttribute(FieldNames.${attribute.name?upper_case})<#rt>
            <#lt><#if functions.isRequired(attribute)>.get()</#if>;
        </#if>
    <#elseif  attribute.type == 'BOOLEAN'>
        <#if attribute.multiValued>
            <#lt>${typeIndent}return getSimpleArrayAttribute(FieldNames.${attribute.name?upper_case}, Boolean.class);
        <#else>
            <#lt>${typeIndent}return getBooleanAttribute(FieldNames.${attribute.name?upper_case}).orElse(false);
        </#if>
    <#elseif  attribute.type == 'BINARY'>
        <#if attribute.multiValued>
            <#lt>${typeIndent}return getSimpleArrayAttribute(FieldNames.${attribute.name?upper_case}, byte[].class);
        <#else>
            <#lt>${typeIndent}return getBinaryAttribute(FieldNames.${attribute.name?upper_case})<#rt>
            <#lt><#if functions.isRequired(attribute)>.get()</#if>;
        </#if>
    <#elseif  attribute.type == 'COMPLEX'><#rt>
        <#if attribute.multiValued>
            <#lt>${typeIndent}return getArrayAttribute(FieldNames.${attribute.name?upper_case}, <#rt>
            <#lt>${attribute.name?cap_first}.class)<#rt>;
        <#else>
            <#lt>${typeIndent}return getObjectAttribute(FieldNames.${attribute.name?upper_case}, <#rt>
            <#lt>${attribute.name?cap_first}.class)<#rt>
            <#lt><#if functions.isRequired(attribute)>.get()</#if>;
        </#if>
    <#else>
        <#if attribute.multiValued>
            <#lt>${typeIndent}return getSimpleArrayAttribute(FieldNames.${attribute.name?upper_case});
        <#else>
            <#lt>${typeIndent}return getStringAttribute(FieldNames.${attribute.name?upper_case})<#rt>
            <#lt><#if functions.isRequired(attribute)>.get()</#if>;
        </#if>
    </#if>
    <#rt>
</#macro>

<#macro addConstructor name attributesList isResourceNode extensionList=[] indent=0>
    <#if isResourceNode>
        <@addResourceNodeConstructor name, attributesList, extensionList, indent />
    <#else>
        <@addObjectNodeConstructor name, attributesList, indent />
    </#if>
</#macro>

<#macro addResourceNodeConstructor name, attributesList, extensionList, indent=0>
    <#assign constructorIndent=''?left_pad(indent) >
    <#lt>${constructorIndent}public ${name?cap_first}()
    <#lt>${constructorIndent}{
    <#lt>${constructorIndent}  setSchemas(Arrays.asList(FieldNames.SCHEMA));
    <#lt>${constructorIndent}}

    <#if lombok>
        <#lt>${constructorIndent}@Builder
    </#if>
    <#lt>${constructorIndent}public ${name?cap_first}(<#rt>
    <#lt>String id,
    <#lt>${'Meta meta,'?left_pad(name?length + 20 + indent - 2)}
    <#lt><@methodAttributes attributeList=attributesList extensionList=extensionList indent=name?length + 10 + indent - 2 />)
    <#lt>${constructorIndent}{
    <#lt>${constructorIndent}  setSchemas(Arrays.asList(FieldNames.SCHEMA));
    <#lt>${constructorIndent}  setId(id);
    <#lt>${constructorIndent}  setMeta(meta);
    <#list attributesList as attribute>
        <#if functions.doNotIgnoreAttribute(attribute) || attribute.name == 'externalId' >
            <#lt>${constructorIndent}  set${attribute.name?cap_first}(${attribute.name});
        </#if>
    </#list>
    <#list extensionList as extension>
        <#lt>${constructorIndent}  set${extension.name.get()?cap_first}(${extension.name.get()?uncap_first});
    </#list>
    <#lt>${constructorIndent}}
</#macro>

<#macro addObjectNodeConstructor name attributesList indent=0>
    <#assign constructorIndent=''?left_pad(indent) >
    <#lt>${constructorIndent}public ${name?cap_first}()
    <#lt>${constructorIndent}{
    <#lt>${constructorIndent}}

    <#if lombok>
        <#lt>${constructorIndent}@Builder
    </#if>
    <#lt>${constructorIndent}public ${name?cap_first}(<#rt>
    <#lt><@methodAttributes attributeList=attributesList indent=name?length + 10 + indent - 2 /><#lt>)
    <#lt>${constructorIndent}{
    <#list attributesList as attribute>
        <#if functions.doNotIgnoreAttribute(attribute) || attribute.name == 'externalId' >
            <#lt>${constructorIndent}  set${attribute.name?cap_first}(${attribute.name});
        </#if>
    </#list>
    <#lt>${constructorIndent}}
</#macro>

<#macro addGetterAndSetter attributeList extensionList=[] indent=0 >
    <#assign getterAndSetterIndent=''?left_pad(indent) >
    <#list attributeList as attribute>
        <#if functions.doNotIgnoreAttribute(attribute)>
            <@addJavadoc javadoc=attribute.description indent=indent />
            <#if attribute.type == 'BOOLEAN'>
                <#lt>${getterAndSetterIndent}public <@attributeMethodReturnType attribute=attribute lowercase=!attribute.multiValued /> is${attribute.name?cap_first}()
            <#else>
                <#lt>${getterAndSetterIndent}public <@attributeMethodReturnType attribute /> get${attribute.name?cap_first}()
            </#if>
            <#lt>${getterAndSetterIndent}{
            <@addGetAttributeCall attribute=attribute indent=indent + 2 />
            <#lt>${getterAndSetterIndent}}

            <@addJavadoc javadoc=attribute.description indent=indent />
            <#lt>${getterAndSetterIndent}public void set${attribute.name?cap_first}(${functions.toJavaType(attribute)} ${attribute.name})
            <#lt>${getterAndSetterIndent}{
            <#if  attribute.type == 'DATE_TIME'>
                <#if attribute.multiValued && attribute.type != 'COMPLEX'>
                    <#lt>${getterAndSetterIndent}  setAttributeList(FieldNames.${attribute.name?upper_case}, ${attribute.name});
                <#else>
                    <#lt>${getterAndSetterIndent}  setDateTimeAttribute(FieldNames.${attribute.name?upper_case}, ${attribute.name});
                </#if>
            <#else>
                <#if attribute.multiValued && attribute.type != 'COMPLEX'>
                    <#lt>${getterAndSetterIndent}  setAttributeList(FieldNames.${attribute.name?upper_case}, ${attribute.name});
                <#else>
                    <#lt>${getterAndSetterIndent}  setAttribute(FieldNames.${attribute.name?upper_case}, ${attribute.name});
                </#if>
            </#if>
            <#lt>${getterAndSetterIndent}}

        <#-- keep empty line -->
        </#if>
    </#list>
    <#list extensionList as extension>
        <@addJavadoc javadoc=extension.description.get() indent=indent />
        <#lt>${getterAndSetterIndent}public Optional<${extension.name.get()?cap_first}> get${extension.name.get()?cap_first}()
        <#lt>${getterAndSetterIndent}{
        <#lt>${getterAndSetterIndent}  return getObjectAttribute(FieldNames.${extension.name.get()?upper_case}, <#rt>
        <#lt>${extension.name.get()?cap_first}.class);
        <#lt>${getterAndSetterIndent}}

        <@addJavadoc javadoc=extension.description.get() indent=indent />
        <#lt>${getterAndSetterIndent}public void set${extension.name.get()?cap_first}(${extension.name.get()?cap_first} ${extension.name.get()?uncap_first})
        <#lt>${getterAndSetterIndent}{
        <#lt>${getterAndSetterIndent}  setAttribute(FieldNames.${extension.name.get()?upper_case}, ${extension.name.get()?uncap_first});
        <#lt>${getterAndSetterIndent}}

    <#-- keep empty line -->
    </#list>
</#macro>

<#macro addComplexTypeClasses attribute>
    <#if attribute.type != 'COMPLEX'>
        <#return>
    </#if>
    <@addJavadoc javadoc=attribute.description indent=2 />
  public static class ${attribute.name?cap_first} extends ScimObjectNode
  {
    <@addConstructor name=attribute.name attributesList=attribute.subAttributes isResourceNode=false indent=4 />

    <@addGetterAndSetter attributeList=attribute.subAttributes indent=4 />
  }

</#macro>
