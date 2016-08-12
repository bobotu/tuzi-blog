var postEditor = new SimpleMDE({
    element: document.getElementById("post-editor"),
    renderingConfig: {
        codeSyntaxHighlighting: true
    },
    spellChecker: false,
    autoDownloadFontAwesome: false,
    initialValue: window.editorInfo.content || ""
});

var saveDraftButton = document.getElementById("draft-save");
var savePostButton = document.getElementById("post-save");
var tagManage = document.getElementById("tag-manage");
var imgManage = document.getElementById("img-manage");
var addTagButton = document.querySelector("#editor-tag-manage-panel .editor-tag-submit");
var addImgButton = document.querySelector("#img-manage-panel .editor-tag-submit");

if (!window.editorInfo.newTags) {
    window.editorInfo.newTags = [];
}

if (!window.editorInfo.oldTags) {
    window.editorInfo.oldTags = [];
}

if (!window.editorInfo.images) {
    window.editorInfo.images = [];
}

function buttonProcessing(button) {
    button.innerHTML = '<i class="fa fa-spin fa-circle-o-notch" aria-hidden="true"></i>';
}

function showError(error) {
    var tip = document.querySelector(".error-tip");
    tip.children[0].innerText = error;
    tip.style.visibility = "visible";
    setTimeout(function () {
        tip.style.visibility = "hidden";
    }, 1000)
}

function buildDraft() {
    var md = postEditor.value();
    var title = document.getElementById("post-title").value;
    var tag = window.editorInfo.newTags;
    var draft = {
        title: title,
        content: md,
        tags: tag
    };
    if (window.editorInfo["draftID"]) {
        draft["id"] = window.editorInfo["draftID"];
    }
    return draft;
}

function buildPost() {
    var md = postEditor.value();
    var title = document.getElementById("post-title").value;
    var tag = window.editorInfo.oldTags;
    var post = {
        title: title,
        content: postEditor.options.previewRender(md),
        tags: tag
    };
    if (window.editorInfo["postID"]) {
        post["id"] = window.editorInfo["postID"];
    }
    return post;
}

function buildRequest(url, callback) {
    var req = new XMLHttpRequest();
    req.open("POST", url);
    req.setRequestHeader("Content-Type", "application/json; charset=utf-8");
    req.responseType = "json";
    req.onreadystatechange = callback;
    return req;
}

function saveDraft() {
    var postBody = buildDraft();
    var req = buildRequest("/admin/save-draft", function () {
        if (req.readyState == req.DONE) {
            if (req.status < 300 && req.status >= 200) {
                var result = req.response;
                if (result["success?"]) {
                    window.editorInfo["draftID"] = result["id"];
                } else {
                    showError(result["error"])
                }
            } else {
                showError("未知错误")
            }
            saveDraftButton.innerHTML = "保存草稿";
        }
    });
    buttonProcessing(saveDraftButton);
    req.send(JSON.stringify(postBody));
}

function savePost() {
    var draft = buildDraft();
    var post = buildPost();
    var postBody = {
        draft: draft,
        post: post
    };
    var req = buildRequest("/admin/save-post", function () {
        if (req.readyState == req.DONE) {
            if (req.status < 300 && req.status >= 200) {
                var result = req.response;
                if (result["success?"]) {
                    window.editorInfo["draftID"] = result["draftID"];
                    window.editorInfo["postID"] = result["postID"];
                    window.editorInfo.oldTags = draft.tags.slice(0);
                } else {
                    showError(result["error"])
                }
            } else {
                showError("未知错误")
            }
            savePostButton.innerHTML = "正式发布";
        }
    });
    buttonProcessing(savePostButton);
    req.send(JSON.stringify(postBody));
}

function removeTag(tag) {
    var i = window.editorInfo.newTags.indexOf(tag);
    return window.editorInfo.newTags.splice(i, 1)
}

function addTag(tag) {
    return window.editorInfo.newTags.push(tag);
}

function newTag(tag) {
    var tagContainer = document.createElement("div");
    tagContainer.className = "editor-tag";
    var tagArea = document.querySelector(".editor-tag-area");
    var tagName = document.createElement("span");
    tagName.className = "tag-name";
    tagName.appendChild(document.createTextNode(tag));
    tagContainer.appendChild(tagName);

    var deleteIcon = document.createElement("i");
    deleteIcon.className = "fa fa-times";
    deleteIcon.setAttribute("aria-hidden", "true");
    tagContainer.appendChild(deleteIcon);

    deleteIcon.addEventListener("click", function () {
        removeTag(tag);
        tagArea.removeChild(tagContainer);
    });

    tagArea.appendChild(tagContainer);
}

function submitTag(e) {
    var input = document.getElementById("tag-input");
    var tag = input.value;
    input.value = "";
    addTag(tag);
    newTag(tag);
}

function deleteImg(e) {

}

function newImg(url, id) {
    var imgContainer = document.createElement("div");
    imgContainer.className = "img-list-item";
    imgContainer.dataset.id = id;

    var img = document.createElement("img");
    img.src = url;

    var input = document.createElement("input");
    input.type = "text";
    input.value = url;

    var deleteIcon = document.createElement("i");
    deleteIcon.className = "fa fa-2x fa-times";
    deleteIcon.setAttribute("aria-hidden", "true");

    imgContainer.appendChild(img);
    imgContainer.appendChild(input);
    imgContainer.appendChild(deleteIcon);

    deleteIcon.addEventListener("click", deleteImg);

    var imgList = document.querySelector("#img-manage-panel .img-list");
    imgList.appendChild(imgContainer);
}

function getDraftID() {
    var req = new XMLHttpRequest();
    req.open("POST", "/admin/get-draft-id", false);
    req.send("");
    if (req.status < 300 && req.status >= 200) {
        var result = JSON.parse(req.response);
        return result.id;
    }
}

function uploadImg(e) {
    var input = document.getElementById("img-input");
    var form = new FormData();
    if (input.files.length < 1) {
        return;
    }
    if (!window.editorInfo["draftID"]) {
        var id = getDraftID();
        if (!id) {
            showError("无法取得ID");
            return;
        } else {
            window.editorInfo["draftID"] = id;
        }
    }
    form.set("upload-file", input.files[0]);
    form.set("draft-id", window.editorInfo['draftID']);
    var req = new XMLHttpRequest();
    req.open("POST", "/admin/upload-img");
    req.responseType = 'json';
    buttonProcessing(e.target);
    req.onreadystatechange = function () {
        if (req.readyState == req.DONE) {
            if (req.status < 300 && req.status >= 200) {
                var result = req.response;
                if (result["success?"]) {
                    newImg(result["url"], result["id"]);
                } else {
                    showError(result["error"])
                }
            } else {
                showError("未知错误")
            }
            e.target.innerHTML = "上传";
        }
    };
    req.send(form);
}

saveDraftButton.addEventListener("click", saveDraft);
savePostButton.addEventListener("click", savePost);
addTagButton.addEventListener("click", submitTag);
addImgButton.addEventListener("click", uploadImg);
tagManage.addEventListener("click", function () {
    var dialog = document.getElementById("editor-tag-manage-panel");
    dialog.style.visibility = "visible";
});
imgManage.addEventListener("click", function () {
    var dialog = document.getElementById("img-manage-panel");
    dialog.style.visibility = "visible";
});
document.querySelector("#editor-tag-manage-panel i").addEventListener("click", function () {
    var dialog = document.getElementById("editor-tag-manage-panel");
    dialog.style.visibility = "hidden";
});
document.querySelector("#img-manage-panel i").addEventListener("click", function () {
    var dialog = document.getElementById("img-manage-panel");
    dialog.style.visibility = "hidden";
});


window.addEventListener("load", function () {
    window.editorInfo.newTags.forEach(function (el) {
        newTag(el);
    });
    window.editorInfo.images.forEach(function (el) {
        newImg(el.url, el.id);
    });
    var title = document.getElementById("post-title");
    title.value = window.editorInfo.title || "";
});