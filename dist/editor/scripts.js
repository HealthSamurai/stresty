const toggleSwitch = document.querySelector('.theme-switch input[type="checkbox"]');
const currentTheme = localStorage.getItem('theme');

if (currentTheme) {
    document.documentElement.setAttribute('data-theme', currentTheme);
    //if (currentTheme === 'light') {
      //  toggleSwitch.checked = true;
    //}
}

function switchTheme(e) {
    if (e.target.checked) {
        document.documentElement.setAttribute('data-theme', 'dark');
        localStorage.setItem('theme', 'dark');
        monaco.editor.setTheme('vs-dark');
    }
    else {
        document.documentElement.setAttribute('data-theme', 'light');
        localStorage.setItem('theme', 'light');
        monaco.editor.setTheme('vs');
    }
}

toggleSwitch.addEventListener('change', switchTheme, false);

function saveProfile () {
    let xhr = new XMLHttpRequest();
    xhr.open('POST', window.location.href.replace('edit', 'post-profile'), false);
    xhr.setRequestHeader('Content-Type', 'text/x-yaml');
    xhr.send(monaco.editor.getModels()[0].getValue());
    if (xhr.status != 200) {
        alert("File not found!");
      } else {
        alert(xhr.responseText);
      }
}

function docReady(fn) {
    // see if DOM is already available
    if (document.readyState === "complete" || document.readyState === "interactive") {
        // call on next available tick
        setTimeout(fn, 1);
    } else {
        document.addEventListener("DOMContentLoaded", fn);
    }
}

function exit() {
    let url = window.location.href;
    let temp = url.split("/").pop();
    let resource = temp.split("&").reverse().pop();
    let profile = temp.split("&").pop();
    let beg = url.split("edit").reverse().pop();
    //alert(beg + "profiles/" + resource + "/" + profile + ".html");
    window.location.href = beg + "profiles/" + resource + "/" + profile + ".html"
    saveProfile();
}

docReady(function() {
    let url = window.location.href;
    let temp = url.split("/").pop();
    let resource = temp.split("&").reverse().pop();
    let profile = temp.split("&").pop();
    whereami.innerText = resource + ": " + profile;
});

