# Temporary Functions
These functions only runs while the connection is open.  Once closed, the function disappears.

## Creating a Temporary Function

### Prepare
Open the [Chainless App](http://localhost:42069).  Then, click the button in the bottom-right corner to expand the menu.  Select "Create Temporary Function".

At this page, you can write your function code as a single JavaScript or Python file.  Temporary functions may not include additional dependencies, so they are limited in functionality.  Edit the code box to implement your function.

### Code
The code is generally structured in the following manner:
**JS**
```js
(function(functionState, blockWithMeta) {
  let state = { ...functionState.state }
  // ...
  return state
})
```
*Note: The entire function must be wrapped in (parenthesis).*

**Python**
```python
import polyglot
@polyglot.export_value
def apply_block(stateWithChains, blockWithMeta):
  if(stateWithChains["state"] is not None):
    state = stateWithChains["state"]
    # Modify state
    return state
  else:
    state = {}
    # Initialize state
    return state
```
*Note: The "polyglot" statements are required*
*Note: Python functionality is in early-preview*

This function accepts a function state and a new block, and produces a new state.  It is invoked over-and-over again by the backend as new blocks arrive.

The `functionState` object contains `.state` and `.chainStates` fields. `.state` is the JSON result of the previous invocation of this function. `.chainStates` is a key-value mapping from "chain name" to "block ID".
  
The `blockWithMeta` object contains `.meta` and `.block` fields.  `.meta` includes information like `.chain`, `.blockId`, and `.height`. `.block` contains the raw JSON-encoded data of the block.

### Historical Blocks
You can optionally select a `Start Time` from which blocks should be retroactively applied. For example,
you can instruct the function to apply all blocks from yesterday before applying new blocks.

### Run
Finally, click `Run`.  Your function will start running on a Chainless server, and the state will be streamed back to you as it is updated.
