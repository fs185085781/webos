(function (){
    var sz = document.currentScript.src.split("/");
    sz.length -= 2;
    var acePath = sz.join("/");
    var version = utils.config.version;
    document.write("<script src='"+acePath+"/js/emmet.js?jsv="+version+"'></script>");
    document.write("<script src='"+acePath+"/src-min/ace.js?jsv="+version+"'></script>");
    document.write("<script src='"+acePath+"/src-min/ext-language_tools.js?jsv="+version+"'></script>");
    document.write("<script src='"+acePath+"/src-min/ext-elastic_tabstops_lite.js?jsv="+version+"'></script>");
    document.write("<script src='"+acePath+"/src-min/ext-emmet.js?jsv="+version+"'></script>");
    document.write("<script src='"+acePath+"/js/marked.min.js?jsv="+version+"'></script>");
})()