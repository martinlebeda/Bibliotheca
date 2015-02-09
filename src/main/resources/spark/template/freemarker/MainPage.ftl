<html>
<#include "component/header.ftl">
<body>

<div id="navigator">
    <h1>Bibliotheca</h1>
</div>

<div id="directories">
    <ul style="margin: 0px">
    <#list bele as p>
        <li><a class="directory" href="browse?path=${p.path}">${p.name}</a></li>
    </#list>
    </ul>
</div>

</body>
</html>