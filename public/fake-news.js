let PLAYER_ACCELERATION=5;
let PLAYER_FRICTION=0.70;

let presentationState = {
	playerVisible: true,
	canShoot: true,
	generateNews: true,
	hasReaders: true
}

let keyboardState = {
	left: false,
	right: false,
	top: false,
	bottom: false,
	space: false
};

let player = {
	x: 400,
	y: 200,
	vx: 0,
	vy: 0,
    width: 36,
    height: 36,
	element: document.getElementById('player'),
	inserted: true,
	shooting: {
		lastShot: 0
	},
	alive: true
};

let bullets = [];
let news = [];
let readers = [];

let scene = document.getElementById('scene');
let frame = 0;

let newsTemplates = [
	{
		width: 100,
		height: 134,
		background: 'url("trump-1.png")',
		fake: true
	},
	{
		width: 100,
		height: 115,
		background: 'url("may.png")',
		fake: true
	},
	{
		width: 58,
		height: 32,
		background: 'url("brexit.png")',
		fake: false
	},
	{
		width: 105,
		height: 151,
		background: 'url("kiitg.png")',
		fake: false
	},
	{
		width: 160,
		height: 68,
		background: 'url("nhs.png")',
		fake: false
	}
];

let score = 0;

function getRandom(min, max) {
  return Math.random() * (max - min) + min;
}

var socket = new WebSocket('ws://' + window.location.hostname + ':' + window.location.port + '/websocket');

function send(obj) {
    console.log(JSON.stringify(obj));
    socket.send(JSON.stringify(obj));
}

let leaderboard = document.getElementById('leaderboard');

socket.onmessage = function(msg) {
    let serverData = JSON.parse(msg.data);
    console.log(serverData);
    switch (serverData.type) {
        case "update-state":
            presentationState = serverData.presentationState;
            console.log(presentationState);
            break;
        case "update-leader-board" :
            leaderboard.innerHTML = serverData.leaderBoard.map((s) => s.name + ': ' + s.score).join('<br />');
            break;
    }
};

function moveObject(obj, friction = 1) {
	obj.x += obj.vx;
	obj.y += obj.vy;
	obj.vx = obj.vx * friction;
	obj.vy = obj.vy * friction;
}

function movePlayer() {
	if (keyboardState.left) {
		player.vx -= PLAYER_ACCELERATION;
	}
	if (keyboardState.right) {
		player.vx += PLAYER_ACCELERATION;
	}
	if (keyboardState.top) {
		player.vy -= PLAYER_ACCELERATION;
	}
	if (keyboardState.bottom) {
		player.vy += PLAYER_ACCELERATION;
	}
    // bounce
    if ((player.x > (1000 - player.width) && player.vx > 0) || (player.x < 0 && player.vx < 0)) {
        player.vx = -player.vx;
    }

	moveObject(player, PLAYER_FRICTION);
}

function renderObject(obj) {
	if (obj.alive) {
		obj.element.style.left = obj.x + 'px';
		obj.element.style.top = obj.y + 'px';
		if (!obj.inserted) {
			scene.appendChild(obj.element);
			obj.inserted = true;
		}
	}
}

function renderScene() {
	renderObject(player);
	bullets.forEach(renderObject);
	news.forEach(renderObject);
}

function generateObject(elementSpec) {
	let el = document.createElement('DIV');
	el.style = 'width:' + elementSpec.width + 'px;height:' + elementSpec.height + 'px;position:absolute; background: ' + elementSpec.background;
	return el;
}

function shoot() {
	if (keyboardState.space && frame - player.shooting.lastShot > 8) {
		let bulletElement = generateObject({width: 3, height: 3, background: '#ffffff'});
		bullets.push({
			x: player.x + 17,
			y: player.y,
			vx: 0,
			vy: -10,
			element: bulletElement,
			alive: true
		});
		player.shooting.lastShot = frame;
		if (score > 0) {
		    score = Math.max(score - 1, 0);
		    send({type: 'fired'});
        }
	}
}

function moveBullets() {
	bullets.forEach((b) => moveObject(b, 1));
	if (bullets[0] && bullets[0].y < 0) {
		if (bullets[0].alive) {
			scene.removeChild(bullets[0].element);
		}
		bullets.shift();
	}
}

function generateNews() {
	if (Math.random() < 0.03) {
		let randomNews = Math.round(getRandom(0, newsTemplates.length - 1));
		let theNews = newsTemplates[randomNews];
		let newsElement = generateObject(theNews);
		news.push({
			x: getRandom(0, 1000-theNews.width),
			y: -200,
			vx: getRandom(-5, 5),
			vy: getRandom(-5, 5),
			element: newsElement,
			width: theNews.width,
			height: theNews.height,
			fake: theNews.fake,
			alive: true
		});
	}
}

function moveNews() {
	news.forEach((n) => {
		n.vx += Math.random()-0.5;
		n.vy += Math.random()-0.35;
        // bounce
        if ((n.x > (1000 - n.width) && n.vx > 0) || (n.x < 0 && n.vx < 0)) {
            n.vx = -n.vx;
        }
		moveObject(n, 0.95);
	});
	if (news[0] && news[0].y > 550) {
		if (news[0].alive) {
			scene.removeChild(news[0].element);
			if (news[0].fake) {
			    send({type: 'fake-news-hit-public'});
                score = Math.max(score - 100, 0);
			} else {
                send({type: 'good-news-hit-public'});
                score = Math.max(score + 10, 0);
			}
		}
		news.shift();
	}
}

function collisions() {
	news.forEach((n) => {
		bullets.forEach((b) => {
			if (n.x < b.x && n.x + n.width > b.x && n.y < b.y && n.y + n.height > b.y && n.alive && b.alive) {
				b.alive = false;
				n.alive = false;
				scene.removeChild(n.element);
				if (b.inserted) {
					scene.removeChild(b.element);
				}
				if (n.fake) {
					score = Math.max(score + 10, 0);
                    score({type: 'hit-fake-news'});
				} else {
                    score = Math.max(score - 10, 0);
                    score({type: 'hit-good-news'});
				}
			}
		});
	});
}

function tick() {
	frame++;
	movePlayer();
	if (presentationState.canShoot) {
		shoot();
	}
	if (presentationState.generateNews) {
		generateNews();
	}
	moveBullets();
	moveNews();
	collisions();
	renderScene();
}

function handleKeyboardState(setTo) {
	return function(ev) {
		if (ev.keyCode === 37) {
			keyboardState.left = setTo;
		}
		if (ev.keyCode === 38) {
			keyboardState.top = setTo;
		}
		if (ev.keyCode === 39) {
			keyboardState.right = setTo;
		}
		if (ev.keyCode === 40) {
			keyboardState.bottom = setTo;
		}
		if (ev.keyCode === 32) {
			keyboardState.space = setTo
		}
	};
}

window.onkeydown = handleKeyboardState(true);
window.onkeyup = handleKeyboardState(false);


function generateReaders() {
	for (let i=0; i<50; i++) {
		let readerElement = document.createElement('DIV');
		readerElement.style = 'width:16px;height:19px;position:absolute; background-image: url("reader.png")';
		readers.push({
			x: i*20,
			y: 580,
			vx: 0,
			vy: 0,
			element: readerElement,
			alive: true
		});
	}
	readers.forEach(renderObject);
}

generateReaders();

function pause() {
	window.clearInterval(i);
}

let nameInput = document.getElementById('name');
nameInput.focus();
nameInput.onkeydown = function(ev) {
    if (ev.keyCode === 13) {
        nameInput.blur();
        scene.focus();
        send({
            type: 'player-name',
            name: nameInput.value
        });
        document.getElementById('nameBox').style.visibility = 'hidden';
        let i = window.setInterval(tick, 25);
    }
};