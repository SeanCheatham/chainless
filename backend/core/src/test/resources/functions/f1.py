import polyglot
@polyglot.export_value
def apply_block(stateWithChains, blockWithMeta):
  state = stateWithChains["state"] or {}
  chain = blockWithMeta["meta"]["chain"]
  if chain in state:
    state[chain] = state[chain] + 1
  else:
    state[chain] = 1
  return state