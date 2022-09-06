'use strict';

let worker = null;
let workerUpdate = null;
let project = null;
let db = null;
let schematic = null;

document.addEventListener('DOMContentLoaded', async function() {
    let dbPromise = loadDB();
    db = await dbPromise;
    db.onerror = function(e) {
        console.error('database error:', e);
    };
    await run();
})
async function run() {
    // ~> #getProject
    let target = document.querySelector('#targetName').value;
    let location = 'playground/parts/' + target + '.json'
    let partConfig = await loadJSON(location);
    project = new Project(partConfig, {
        defaultHumanName: partConfig.humanName,
        parts: {
            main: {
                location: location,
                x: 0,
                y: 0,
            },
        },
        wires: [],
    })

    // ~> #update
    schematic = new Schematic(project.data);
    await schematic.refresh();

    let projectPath = document.querySelector('#projectPath').value;
    let filePath = document.querySelector('#filePath').value;
    const url = '/tinygo-preview/wasm?project=' + projectPath + '&file=' + filePath;
    const request = { url: url, method: 'GET' }

    if ((await fetch(url, request)).status === 404) {
        return;
    }

    let message = {
        type: 'start',
        fetch: request,
        config: schematic.configForWorker(),
    }
    worker = new Worker('playground/worker/webworker.js');
    worker.postMessage(message);
    worker.onmessage = async function (e) {
        let worker = e.target;
        let msg = e.data;
        switch (msg.type) {
            case 'error': {
                terminal.showError(msg.message);
                break;
            }
            case 'loading': {
                terminal.clear('Loading...');
                worker.postMessage({
                    type: 'getUpdate',
                });
                break;
            }
            case 'started': {
                document.querySelector('#schematic').classList.remove('compiling');
                terminal.clear('Running...');
                break;
            }
            case 'notifyUpdate': {
                workerUpdate = requestAnimationFrame(() => {
                    workerUpdate = null;
                    worker.postMessage({
                        type: 'getUpdate',
                    });
                });
                break;
            }
            case 'properties': {
                schematic.setProperties(msg.properties);
                break;
            }
            case 'update': {
                schematic.update(msg.updates);
                break;
            }
            case 'connections': {
                schematic.updateConnections(msg.pinLists);
                break;
            }
            case 'speed': {
                schematic.setSpeed(msg.speed);
                break
            }
            default: {
                console.log('unknown worker message:', msg);
            }
        }
    }
}

function workerPostMessage(message) {
    worker.postMessage(message);
}

function saveState() {

}

function loadDB() {
    return new Promise((resolve, reject) => {
        // First get the database.
        let request = indexedDB.open("tinygo-playground", 2);
        request.onupgradeneeded = function (e) {
            let db = e.target.result;
            if (e.oldVersion === 1) {
                // The proper way would be to upgrade the object store in place, but the
                // easy way is to simply drop all existing data.
                db.deleteObjectStore('projects');
            }
            let projects = db.createObjectStore('projects', {keyPath: 'name', autoIncrement: true});
            projects.createIndex('target', 'target', {unique: false});
        };
        request.onsuccess = function (e) {
            resolve(e.target.result);
        };
        request.onerror = function (e) {
            reject(e);
        };
    })
}
