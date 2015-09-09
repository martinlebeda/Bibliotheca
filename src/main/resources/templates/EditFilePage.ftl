<html>
<#include "component/header.ftl">

<script type="application/javascript">
    $(document).ready(function () {
        $('#tlacitko_autor').click(
                function () {
                    $('#bookname').val('${author} - ' + $('#bookname').val())
                }
        );
    });
</script>

<body>

<#include "component/navigator.ftl">

<div class="edit-form">
    <form action="editFile?path=${path}&amp;basename=${name}" method="post">

    <#if (cover?has_content) >
        <img class="file-cover" style="padding: 20" src="cover?path=${cover}" height="200"></img>
    </#if>
        <fieldset class="editFormSection">
            <p><label for="dbknih">Name:</label>
                <input id="dbknih" type="text" name="dbknih" value="${dbknih}" size="100" autofocus/>
            <p><label for="bookname">Name:</label>
                <input id="bookname" type="text" name="bookname" value="${name}" list="booknames" size="50" required/>
            <datalist id="booknames">
            <#list optnames as opt>
                <option value="${opt}"></option>
            </#list>
            </datalist>
                <input type="button" id="tlacitko_autor" value="Author" />
            </p>
            <p><label for="bookcover">New cower from url:</label>
                <input id="bookcover" type="text" name="bookcover" size="50"/>
            </p>
            <p><label for="bookdescription">Description:</label>
                <textarea id="bookdescription" name="bookdescription" cols="50" rows="5" style="width: 100%"><#if (desc?has_content) >${desc}</#if></textarea>
            </p>
        </fieldset>

        <div style=" text-align: right; padding: 20px">
            <a class="file-external_search" href="https://www.google.cz/search?q=${name}" target="_blank">Google</a>
            <a class="file-external_search" href="http://www.databazeknih.cz/search?q=${name}&hledat=&stranka=search" target="_blank">DatabázeKnih</a>
            <input type="submit" name="loadImage" value="load image"></input>
            <input type="submit" name="loadDescription" value="load description"></input>
            <input type="submit" name="loadAll" value="load all"></input>
            <input type="submit" name="loadAllClose" value="load all & close" ></input>
            <input type="submit" name="Save" value="save"></input>
            <input type="submit" name="saveClose" value="save & close"></input>
        </div>
    </form>
</div>

</body>
</html>