

var socket = new WebSocket('ws://' + window.location.hostname + ':' + window.location.port + '/websocket');



let leaderboard = document.getElementById('leaderboard');

socket.onmessage = function(msg) {
    let serverData = JSON.parse(msg.data);
    console.log(serverData);
    switch (serverData.type) {
        case "update-leader-board" :
            leaderboard.innerHTML = serverData.leaderBoard.map((s) => s.name + ': ' + s.score).join('<br />');
            break;
    }
};
