<div id="navigator">
    <div>
    <a href="/">HOME</a>
    <#list navigator as p>
        / <a href="browse?path=${p.path}">${p.name}</a>
    </#list>
        / <span id="navigator-file">
    <#if (navigableLastFile)>
        <a href="browse?path=${navigatorPath}">${navigatorFile}</a>
    <#else >
    ${navigatorFile}
    </#if>
    </span>
    </div>
</div>