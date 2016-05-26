// try automatic find url and load changes if founded successfully
function tryDb(id, title, autor) {
    //alert(id);
    $.getJSON("/tryDb?id=" + id, function (data) {
        if (data.tryDb == 1) {
            $('#' + id).load("/loadItem?id=" + id);
        } else {
            chooseDb(id, title, autor);
        }
    });

}

// generate target format
function generateTgt(id, tgt) {
    $("#gen" + id).css("display", "inline");
    $('#' + id).load("/generateTgt", {'id': id, 'tgt': tgt}, function () {
        $('#' + tgt + id)[0].click();

    });
}

// refresh index uuid
function refreshIndex() {
    var jqxhr = $.get('/refreshIndex', function () {
            showNotify("Bibliotheca", "Refresh of index is done", "pic/refresh_000000_32.png");
        })
        .fail(function () {
            showNotify("Bibliotheca", "Refresh of index is FAIL", "pic/refresh_000000_32.png");
        });

}

// show desktop notification
function showNotify(title, body, icon) {
    title = title || "Bibliotheca";
    icon = icon || "pic/refresh_000000_32.png";
    body = body || "Action is completed";
    if (Notification.permission !== 'granted') {
        Notification.requestPermission();
    }
    var n = new Notification(title, {
        body: body,
        icon: icon
    });

    setTimeout(function () {
        n.close();
    }, 3000);
}

// generate and copy to reader
function toReader(id, devFormat, devPath) {
    $("#gen" + id).css("display", "inline");
    $('#' + id).load("/toReader", {'id': id, 'devFormat': devFormat, 'devPath': devPath});
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

function editMeta(id) {
    $('#modalEditMetaForm').load("/modalEditMetaForm", {'id': id});
    $('#editMeta').modal('show');
}

// save filled url
function saveDbUrl(url) {
    url = url || $('#chooseDbModalUrl').val(); // if not param, get it from frm
    var id = $('#chooseDbModalIdBook').val();
    $('#' + id).load("/saveDbUrl", {'id': id, 'url': url}, reloadImageById(id));
    $('#chooseDbModal').modal('hide');
}

// load list of other books in path
function chooseJoinModal(title, id) {
    $('#chooseJoinModalData').load("/chooseJoinModalData", {'id': id}, function () {
        $('#chooseJoinModal').find('> div > div > div.modal-header > h4').text("Join \"" + title + "\" to ...")
    });
    $('#chooseJoinModal').modal('show');
}


// load list of other books in path
function chooseJoinModalDir(title, path) {
    $('#chooseJoinModalDirData').load("/chooseJoinModalDirData", {'path': path}, function () {
        $('#chooseJoinModalDir').find('> div > div > div.modal-header > h4').text("Join \"" + title + "\" to ...")
    });
    $('#chooseJoinModalDir').modal('show');
}

function joinToDir(srcPath, tgtPath) {
    $.get("/joinToDir", {'srcPath': srcPath, 'tgtPath': tgtPath}, function (data) {
        window.location.href = "/browse" + "?path=" + tgtPath;
    });
}

// save filled url
function joinTo(idFrom, idTo) {
    $.get("/joinTo", {'idFrom': idFrom, 'idTo': idTo}, function (data) {
        $('#' + idFrom).remove();
        $('#' + idTo).load("/loadItem?id=" + idTo);
    });
    $('#chooseJoinModal').modal('hide');
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
function deleteBook(id, title) {
    bootbox.confirm("Are you sure to delete \"" + title + "\"?", function (result) {
        if (result) {
            $.get("/deleteBook", {'id': id}, function (data) {
                $('#' + id).remove();
            });
        }
    });
}

// tidyUp
function tidyupBook(id) {
    $.get("/tidyupBook", {'id': id}, function (data) {
        $('#' + id).remove();
    });
}

// force reload cover image
function reloadImageById(id) {
    $('#' + id + ' > div.col-xs-2 > img').attr('src', $('img').attr('src') + '?' + Math.random());
}

$('.dropdown').on('show.bs.dropdown', function () {
    alert('The dropdown is about to be shown.');
});

//function checkMenuPosition(element) {
//    var menu = $(element).find("ul");
//    var top = menu.offset().top;
//    //alert(top);
//    if (top < 0) {
//        //$(menu).offset({top: 0, left: menu.offset().left});
//        $(menu).css('position', 'static');
//
//        //$(menu).style.display='';
//        //$(menu).position({top: 0, left: menu.offset().left});
//        //$(element).dropdown("toggle");
//       //$(element)
//       //    .removeClass('dropup')
//       //    .addClass('dropdown');
//       //$(element).dropdown();
//    }
//}

//$('#myModal').modal('toggle');
//$('#myModal').modal('show');
//$('#myModal').modal('hide');