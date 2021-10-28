const wsProto = (window.location.protocol === "https:") ? "wss:" : "ws:";
const wsBase = `${wsProto}//${window.location.hostname}:${window.location.port}`;

function connectWS() {
    const chatWS = new WebSocket(`${wsBase}/chat`);

    chatWS.onmessage = function(event) {
        const li = document.createElement("li");
        li.innerHTML = event.data;
        document.getElementById("chats").appendChild(li);
    }

    chatWS.onclose = (event) => {
        console.error(event);
        document.getElementById("send").removeEventListener("click", send);
        window.setTimeout(connectWS, 1000);
    };

    function send(event) {
        event.preventDefault();
        const message = document.getElementById("message").value;
        chatWS.send(message);
        document.getElementById("message").value = "";
    }

    document.getElementById("send").addEventListener("click", send);
}

document.addEventListener("DOMContentLoaded", () => {
    connectWS();
});
