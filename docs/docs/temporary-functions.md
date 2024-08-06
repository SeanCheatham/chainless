# Temporary Functions
These functions only runs while the connection is open.  Once closed, the function disappears.

## Creating a Temporary Function

### Prepare
Login to the [Chainless App](https://app.chainless.dev).  Then, click the button in the bottom-right corner to expand the menu.  Select "Create Temporary Function".

At this page, you can write your function code as a single JavaScript file.  Temporary functions may not include additional dependencies, so they are limited in functionality.  Edit the code box to implement your function.

### Code
The code is generally structured in the following manner:
```js
(function(functionState, blockWithMeta) {
  let state = { ...functionState.state }
  // ...
  return state
})
```

This function accepts a function state and a new block, and produces a new state.  It is invoked over-and-over again by the backend as new blocks arrive.

The `functionState` object contains `.state` and `.chainStates` fields. `.state` is the JSON result of the previous invocation of this function. `.chainStates` is a key-value mapping from "chain name" to "block ID".
  
The `blockWithMeta` object contains `.meta` and `.block` fields.  `.meta` includes information like `.chain`, `.blockId`, and `.height`. `.block` contains the raw JSON-encoded data of the block.

The entire function must be wrapped in (parenthesis).

### Historical Blocks
You can optionally select a `Start Time` from which blocks should be retroactively applied. For example,
you can instruct the function to apply all blocks from yesterday before applying new blocks.

### Run
Finally, click `Run`.  Your function will start running on a Chainless server, and the state will be streamed back to you as it is updated.
