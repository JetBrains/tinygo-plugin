'use strict';

let worker = null;

document.addEventListener('DOMContentLoaded', run);
async function run() {
    // ~> #getProject
    let target = document.querySelector('#targetName').value;
    let location = 'playground/parts/' + target + '.json'
    let partConfig = await loadJSON(location);
    let project = new Project(partConfig, {
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
    let schematic = new Schematic(project.data);
    await schematic.refresh();

    let projectPath = document.querySelector('#projectPath').value;
    let message = {
        type: 'start',
        fetch: {
            url: '/tinygo-preview/wasm?project=' + projectPath,
            method: 'GET',
        },
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
                let workerUpdate = requestAnimationFrame(() => {
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
