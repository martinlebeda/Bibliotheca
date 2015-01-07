<h1>${message}</h1>
<#list bele as p>
    <p><a href="browse?path=${p}">${p}</a>
</#list>
<#include "component/include.ftl">
