const wsProto = (window.location.protocol === "https:") ? "wss:" : "ws:";
const wsBase = `${wsProto}//${window.location.hostname}:${window.location.port}`;

const chatWS = new WebSocket(`${wsBase}/chat`);

chatWS.onmessage = function(event) {
    const li = document.createElement("li");
    li.innerHTML = event.data;
    document.getElementById("chats").appendChild(li);
}

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("send").addEventListener("click", (event) => {
        event.preventDefault();
        const message = document.getElementById("message").value;
        chatWS.send(message);
        document.getElementById("message").value = "";
    });
});
