<html>
<#include "component/header.ftl">
<body>

<#include "component/navigator.ftl">

<div class="edit-form">
    <form action="editDir?path=${path}" method="post">

        <fieldset class="editFormSection">
            <label>
                Name:
                <input type="text" name="bookname" value="${name}" list="booknames" size="50"/>
            </label>
            <datalist id="booknames">
            <#list optnames as opt>
                <option value="${opt}"></option>
            </#list>
            </datalist>
        </fieldset>

        <div style=" text-align: right; padding: 20px">
            <a class="file-external_search" href="https://www.google.cz/search?q=${name}" target="_blank">Google</a>
            <input type="submit" value="save"></input>
        </div>
    </form>
</div>

</body>
</html>