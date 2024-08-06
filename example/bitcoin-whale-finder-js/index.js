const axios = require('axios');

const callbackBase = process.env.CALLBACK_BASE_URL;

async function run() {
    try {
        while (true) {
            console.log("Fetching next task")
            let taskResponse = await axios.get(`${callbackBase}/next`)
            if (taskResponse.data == null) break
            let task = taskResponse.data
            let state = {}
            if (task.taskType === "init") {
                console.log("Initializing")
                state.blocksProcessed = 0;
                state.largestOutputs = [];
            } else if (task.taskType === "apply") {
                let blockWithChain = task.blockWithChain
                let stateWithChains = task.stateWithChains
                state = { ...stateWithChains.state }
                if (blockWithChain.meta.chain === "bitcoin") {
                    console.log(`Applying bitcoin block id=${blockWithChain.meta.blockId}`)
                    state.blocksProcessed += 1;
                    let currentLargest = 0;
                    if (state.largestOutputs.length > 0) {
                        currentLargest = state.largestOutputs[state.largestOutputs.length - 1].value;
                    }
                    for (let tx of blockWithChain.block.tx) {
                        for (let vout of tx.vout) {
                            if (vout.value > currentLargest) {
                                console.log("Adding largest UTxO")
                                state.largestOutputs.push(
                                    {
                                        value: vout.value,
                                        txId: tx.txid,
                                        txOutputIndex: vout.n
                                    }
                                );
                                currentLargest = vout.value;
                            }
                        }
                    }
                } else {
                    console.log("Ignoring non-bitcoin block")
                }
            }
            await axios({
                method: 'post',
                url: `${callbackBase}/success`,
                data: state
            })
        }
        console.log("No remaining tasks.  Good bye!")
    } catch (e) {
        await axios({
            method: 'post',
            url: `${callbackBase}/error`,
            data: e
        })
    }
}

run().catch((e) => console.log(`Error ${e}`));
