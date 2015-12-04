/**
 * Created by martin on 4.12.15.
 */

function tryDb(id) {
    //alert(id);
    $('#'+id).load("/tryDb?id=" + id);
}