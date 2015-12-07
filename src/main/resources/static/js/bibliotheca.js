// try automatic find url and load changes if founded successfully
function tryDb(id) {
    //alert(id);
    $('#'+id).load("/tryDb?id=" + id);
}

// fill and show modal for manipulace with url
function chooseDb(id, title, autor) {
    $('#chooseDbModalIdBook').val(id);
    $('#chooseDbModalUrl').val("");

    $('#chooseDbModalTitleUrl').attr("href", "http://www.databazeknih.cz/search?q=" + title + "&hledat=&stranka=search");
    $('#chooseDbModalAuthorUrl').attr("href", "http://www.databazeknih.cz/search?q=" + autor + "&in=authors");
    $('#chooseDbModal').modal('show');
}

// save filled url
function saveDbUrl() {
    var id = $('#chooseDbModalIdBook').val();
    $('#'+id).load("/saveDbUrl", {'id': id, 'url': $('#chooseDbModalUrl').val()});
    $('#chooseDbModal').modal('hide');
}

//$('#myModal').modal('toggle');
//$('#myModal').modal('show');
//$('#myModal').modal('hide');