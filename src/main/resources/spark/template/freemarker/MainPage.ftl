<html>
<#include "component/header.ftl">
<body>

<h1>Fiction</h1>

<div id="directories">
<#list bele as p>
<a class="directory" href="browse?path=${p.path}">${p.name}
    </#list>
</div>

</body>
</html>