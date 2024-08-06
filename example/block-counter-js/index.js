const axios = require('axios');

const callbackBase = process.env.CALLBACK_BASE_URL;

async function run() {
    try {
        let task = await nextTask()
        while (task != null) {
            let state = await handleTask(task)
            let successResponse = await axios({
                method: 'post',
                url: `${callbackBase}/success`,
                data: state
            })
            if (successResponse.status != 200) throw Exception("Could not submit successful response")
            task = await nextTask()
        }
    } catch (e) {
        console.log(`Error ${e}`)
        await axios({
            method: 'post',
            url: `${callbackBase}/error`,
            data: e
        })
    }
}

async function nextTask() {
    console.log("Fetching next task")
    let taskResponse = await axios.get(`${callbackBase}/next`)
    if (taskResponse.status != 200) throw Exception("Could not get next task")
    if (taskResponse.data.taskType == null) return null
    return taskResponse.data
}

async function handleTask(task) {
    let state = {}
    if (task.taskType === "init") {
        console.log("Initializing")
    } else if (task.taskType === "apply") {
        let blockWithChain = task.blockWithChain
        let stateWithChains = task.stateWithChains
        state = { ...stateWithChains.state }
        let chain = blockWithChain.meta.chain;
        let previousCount = state[chain] ?? 0;
        state[chain] = previousCount + 1;
        console.log(`Updated count of chain=${chain} to ${state[chain]}`);
    }
    return state
}

run();
