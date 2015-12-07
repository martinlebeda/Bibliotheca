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
    $('#' + id).load("/saveDbUrl", {'id': id, 'url': url}, reloadImageById(id));
    $('#chooseDbModal').modal('hide');
}

// force download cower from databazeknih.cz
function downloadCover(id) {
    $('#' + id).load("/downloadCover", {'id': id}, reloadImageById(id));
}

// clear metadata
function clearMetadata(id) {
    $('#' + id).load("/clearMetadata", {'id': id});
}

// delete book
function deleteBook(id) {
    $.get( "/deleteBook", {'id': id}, function( data ) {
        $('#' + id).remove();
    });
}

// force reload cover image
function reloadImageById(id) {
    $('#' + id + ' > div.col-xs-2 > img').attr('src', $('img').attr('src') + '?' + Math.random());
}

//$('#myModal').modal('toggle');
//$('#myModal').modal('show');
//$('#myModal').modal('hide');