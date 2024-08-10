---
sidebar_position: 1
---

# Chainless Docs

## Getting Started
### Run Blockchain Nodes
Chainless requires access to at least one blockchain node RPC server. You can either run the nodes yourself or use a 3rd party service.

#### Self-Hosted Nodes
- [Bitcoin](https://bitcoin.org/en/download)
- [Ethereum](https://ethereum.org/en/developers/docs/nodes-and-clients/run-a-node/)
- [Apparatus](https://topl.github.io/Bifrost/docs/current/reference/getting-started)

#### 3rd-Party Services
- [GetBlock](https://getblock.io/)

### Install Chainless
1. `docker volume create chainless_data`
   - Creates a persistent location for saving block and function data
1. `docker network create chainless`
   - Creates a dedicated bridge network for function containers
1. `docker run -d --name chainless --restart=always -p 42069:42069 -v chainless_data:/app -v /var/run/docker.sock:/var/run/docker.sock -v  /tmp/chainless:/tmp/chainless --network chainless seancheatham/chainless:latest --ethereum-rpc-address $ETHEREUM_RPC_ADDRESS --bitcoin-rpc-address $BITCOIN_RPC_ADDRESS --apparatus-rpc-address $APPARATUS_RPC_ADDRESS`
   - Substitute your own ethereum, bitcoin, and/or apparatus node addresses.
     - If you don't have one and don't mind centralization, you can use a service like [GetBlock](https://getblock.io/)
     - If you don't have one and need guaranteed chain integrity, you need to install and run your own nodes.
     - At least one chain must be configured.
   - Docker-out-of-Docker is used as a runtime for function containers.
     - The implementation expects the bridge network to be named `chainless`.
     - The implementation expects the `-v  /tmp/chainless:/tmp/chainless` binding exactly as specified.
   - If you like to live on the edge, you can use the `seancheatham/chainless:dev` image tag to try the newest bugs and features.
1. Open [http://localhost:42069](http://localhost:42069) in your cat video browser

### Create Functions
- [Temporary](temporary-functions)
  The function only runs while the connection is open.  Once closed, the function disappears.
- [Persistent](persistent-functions)
  The function runs in the background on Chainless backend servers until you delete it.
