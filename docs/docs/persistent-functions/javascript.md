---
title: JavaScript
description: Information on writing Chainless functions using the NodeJS runtime.
---

# JavaScript Functions

Persistent Functions can be written in JavaScript or TypeScript using the Node runtime.

Your code files are structured like a normal NPM project, including a `package.json` (and the generated `package-lock.json` and `node_modules`) and `.js` files for your implementation.  You should, at minimum, have a `index.js` file which serves as the entry point for your function.

## What you'll need
- [Node.js](https://nodejs.org/en/download/) version 18.0 or above:
  - When installing Node.js, you are recommended to check all checkboxes related to dependencies.

## Definition

### `package.json`
```json
{
  "name": "block-counter",
  "version": "1.0.0",
  "description": "Counts the number of blocks processed by blockchain",
  "main": "index.js",
  "author": "",
  "license": "ISC",
  "dependencies": {
    "axios": "^1.6.7"
  }
}
```
Feel free to add other dependencies. `axios` is used as an HTTP client. Be sure to run `npm install`!

## Function Code

### `index.js`
```js
const axios = require('axios');

const callbackBasePath = process.env.CALLBACK_BASE_URL
const nextTaskUrl = `${callbackBasePath}/next`
const successTaskUrl = `${callbackBasePath}/success`
const errorTaskUrl = `${callbackBasePath}/error`

async function run() {
    try {
        while(true) {
            console.log("Fetching next task")
            let taskResponse = await axios.get(nextTaskUrl)
            if(taskResponse.data == null) break
            let task = taskResponse.data
            if(task.taskType === "init") {
                await sendSuccess({})
            } else if(task.taskType === "apply") {
                let blockWithChain = task.blockWithChain
                let stateWithChains = task.stateWithChains
                let state = { ...stateWithChains.state }
                let chain = blockWithChain.meta.chain;
                let previousCount = state[chain] ?? 0;
                state[chain] = previousCount + 1;
                await sendSuccess(state);
            }
        }
    } catch (e) {
        await sendError(e)
    }
}

function sendSuccess(state) {
    return axios({
        method: 'post',
        url: successTaskUrl,
        data: state
    })
}

function sendError(e) {
    return axios({
        method: 'post',
        url: errorTaskUrl,
        data: e
    })
}

run().catch((e) => console.log(`Error ${e}`));
```

## Zip and Upload

Once your function is written, you can package up the contents of the directory into a `.zip` file.  Be sure the `package.json` is at the root of the `.zip` file, not in a sub-directory.
