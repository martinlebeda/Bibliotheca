// try automatic find url and load changes if founded successfully
function tryDb(id) {
    //alert(id);
    $('#'+id).load("/tryDb?id=" + id);
}

// fill and show modal for manipulace with url
function chooseDb(id, title, autor) {
    // prepare clean form
    $('#chooseDbModalIdBook').val(id);
    $('#chooseDbModalUrl').val("");

    // load list of founded url
    $('#chooseDbModalList').load("/chooseDbModalList", {'id': id});

    // prepare for edit by hand
    $('#chooseDbModalTitleUrl').attr("href", "http://www.databazeknih.cz/search?q=" + title + "&hledat=&stranka=search");
    $('#chooseDbModalAuthorUrl').attr("href", "http://www.databazeknih.cz/search?q=" + autor + "&in=authors");
    $('#chooseDbModal').modal('show');
}

// save filled url
function saveDbUrl(url) {
    url = url || $('#chooseDbModalUrl').val(); // if not param, get it from frm
    var id = $('#chooseDbModalIdBook').val();
    $('#' + id).load("/saveDbUrl", {'id': id, 'url': url});
    $('#chooseDbModal').modal('hide');
}

//$('#myModal').modal('toggle');
//$('#myModal').modal('show');
//$('#myModal').modal('hide');