---
description: Information about the data types used in Chainless functions.
slug: /data-models
sidebar_position: 3
---

# Data Models
A Chainless function generally has two parameters: the **function's state** and a **block with metadata**.

## Function State
Captures the current state of the function, including some metadata about the blocks that have been applied.

It is structured like:
```json
{
  "chainStates": {
    "<chain name>": "<last applied block ID>"
  },
  "state": {
    "myFunctionData": 37
  }
}
```

The `chainStates` field is an object where the keys are blockchain names and the values are the respective block ID that was last applied.  The chain names are:
- `bitcoin`
- `ethereum`

The `state` field is specific to your function.  It can contain any JSON information that is necessary for your function to run and operate.

## Block With Metadata
Different blockchains have different formats for their blocks and transactions.  Chainless captures specific metadata fields to provide uniform indices and views of the data.  The raw block data in the original format of the blockchain is also provided.  The `BlockWithMeta` object is structured like:

```json
{
  "meta": {
    "chain": "<chain name>", // "bitcoin" or "ethereum"
    "blockId": "<block ID>",
    "parentBlockId": "<parent block ID>",
    "height": 35,
    "timestampMs": 1709345437671
  },
  "block": {
    // Blockchain-specific
  }
}
```

- See [Bitcoin block schema](https://developer.bitcoin.org/reference/rpc/getblock.html#result-for-verbosity-2)
- See [Ethereum block schema](https://ethereum.org/en/developers/docs/apis/json-rpc/#eth_getblockbyhash)
- See [Apparatus block schema](https://github.com/Topl/protobuf-specs/blob/main/proto/node/models/block.proto#L36)
  - The protobuf representation is converted into JSON
