<html>
<#include "component/header.ftl">
<body>

<#include "component/navigator.ftl">

<div style=" text-align: right; padding-right: 110px; padding-left: 110px; padding-bottom: 10px;">
    <a class="file-external_search" href="https://www.google.cz/search?q=${navigatorFile}" target="_blank">Google</a>
    <a class="file-external_search" href="editDir?path=${path}">Edit</a>
    <#if (isLocal) >
        <a class="file-extension" href="download?path=${path}">Open</a>
    </#if>
</div>

<div style=" text-align: center; padding-right: 110px; padding-left: 110px">
    <form action="browse">
        <input type="hidden" name="path" value="${path}"/>
        <input type="search" name="booksearch" placeholder="search in current path (and subpath)"
               <#if (booksearch?has_content) >value="${booksearch}"<#else>value=""</#if> list="booknames" size="100"/>
        <input type="submit" value="search"/>
    </form>
</div>

<div id="directories">
    <ul style="margin: 0px">
    <#list dirs as p>
        <li><a class="directory" href="browse?path=${p.path}">${p.name}</a></li>
    </#list>
    </ul>
</div>

<#list fileDetails as p>
<div class="file" <#if (p.coverExists) >style="min-height: 225px;"<#else> </#if>>
    <#if (p.cover?has_content) >
        <img class="file-cover" src="cover?path=${p.cover}" height="200" align="Left"></img>
    </#if >

    <h2>${p.name}</h2>

    <#if (p.desc?has_content) >
    ${p.desc}
    </#if>

    <#list p.files as f>
        <a class="file-extension" href="download?path=${f.path}">${f.ext}</a>
    </#list>

    <#list p.targets as t>
        <a class="file-target" href="browse?path=${path}&amp;basename=${p.name}&amp;target=${t}">${t}</a>
    </#list>

    <a class="file-external_search" href="https://www.google.cz/search?q=${p.name}" target="_blank">Google</a>
    <a class="file-external_search" href="view${path}/${p.name}.htmlz/pack/index.html" target="_blank">View</a>
    <a class="file-external_search" href="editFile?path=${path}&amp;basename=${p.name}">Edit</a>

    <#if (p.tidyUp) >
        <a class="file-external_search" href="browse?path=${path}&amp;tidyup=${p.name}">TidyUp</a>
    </#if>
    <a class="file-external_search" href="browse?path=${path}&amp;delete=${p.name}">Delete</a>

    <#list p.devices as d>
            <a class="file-external_search" href="browse?path=${path}&amp;basename=${p.name}&amp;target=${d.format}&amp;devicePath=${d.path}">${d.name}</a>
    </#list>

    <#if (p.targetPath?has_content) >
        <a href="browse?path=${p.targetPath}">Next from ${p.author}...</a>
    </#if>
</div>
</#list>

<div class="mhtFiles">
    <ul style="margin: 0px">
    <#list mhtFiles as f>
        <li>
            <a class="file-mht" href="download?path=${f.path}">${f.name}</a>
        </li>
    </#list>
    </ul>
</div>

</body>
</html>