// A function which counts blocks, grouped by chain
// A temporary function accepts two arguments: the current state and a new block
(function(functionState, blockWithMeta) {

  // Start by extracting the internal function state
  // If this is the first time running the function, the state may be null
  let state = { ...functionState.state ?? {}}

  // The `blockWithMeta` object contains some commonly indexed information about blocks, like ID, height, timestamp, and chain name
  // In this case, extract the name of the chain
  // And then extract the current value associated with that chain
  let previousChainCount = state[blockWithMeta.meta.chain] ?? 0;

  // Increment the number of blocks associated with this chain
  state[blockWithMeta.meta.chain] = previousChainCount + 1;

  // Return the updated state so that it can be passed to the next invocation
  return state;
});