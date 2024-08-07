![Chainless Logo](./docs/static/img/logo.png)
"Serverless Functions" for blockchain events. But you need a server. And a blockchain. So it's neither serverless nor chainless. Modern branding is wonderful.

Smash some code into your keyboard, click submit, and watch as Chainless magically runs your beautifully crafted function against blockchain blocks. Functions can either be Temporary or Persistent. Temporary functions exist only for the lifetime of the connection to the HTTP endpoint. Persistent functions run in the background even when you close your browser.

## Prerequisites
- Electricity
- Computer
- Internet
- Docker
- **Bitcoin** and **Ethereum** are currently supported. You will need **RPC** access to at least one of them.
- **Linux**, specifically Ubuntu, is the only "officially supported" OS. It might work elsewhere, but my soul would dissolve if I run Windows, and I'm not a billionaire so I can't afford a MacBook.
- This service replicates all block data starting from when you first launch. This consumes a lot of **disk space**, like probably a couple million floppy disks.
- A burning passion for minimally tested software.

## Quick Start
1. `docker volume create chainless_data`
   - Creates a persistent location for saving block and function data
1. `docker run -d --name chainless --restart=always -p 42069:42069 -v chainless_data:/app --privileged seancheatham/chainless:latest --ethereum-rpc-address $ETHEREUM_RPC_ADDRESS --bitcoin-rpc-address $BITCOIN_RPC_ADDRESS`
   - Substitute your own ethereum and bitcoin node addresses.
     - If you don't have one and don't mind centralization, you can use a service like [GetBlock](https://getblock.io/)
     - If you don't have one and need guaranteed chain integrity, you need to install and run your own nodes.
     - At least one chain must be configured.
   - `--privileged` is needed to run persistent functions within Docker containers.
     - This flag introduces some [security risks](https://docs.docker.com/reference/cli/docker/container/run/#privileged). Use it if you prefer to use gasoline to start campfires.
     - Alternatively, you may install [sysbox](https://github.com/nestybox/sysbox) for better security. Use it if you drive the posted speed limit on roadways.
1. Open [http://localhost:42069](http://localhost:42069) in your cat picture viewer/web browser

## Supported Blockchains
- Bitcoin
- Ethereum

## Supported Languages
**Temporary Functions**
- JavaScript (GraalVM)

**Persistent Functions**
- JavaScript/TypeScript (NodeJS)
- Java/Scala/Kotlin (JVM)

## FAQ
- Is Chainless AI-native?
  - No.

## Disclaimers
- This project is still in alpha. It will move into beta/production state once there is sufficient test coverage, so it'll probably never leave alpha.
- The project author trusts the Ballmer Peak and is probably there right now.
- The API and frontend are **not** password-protected. If the port can be accessed, so can the rest of the application.
