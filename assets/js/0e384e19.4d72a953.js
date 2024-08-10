"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[976],{1512:(e,n,s)=>{s.r(n),s.d(n,{assets:()=>l,contentTitle:()=>c,default:()=>h,frontMatter:()=>r,metadata:()=>o,toc:()=>a});var t=s(4848),i=s(8453);const r={sidebar_position:1},c="Chainless Docs",o={id:"intro",title:"Chainless Docs",description:"Getting Started",source:"@site/docs/intro.md",sourceDirName:".",slug:"/intro",permalink:"/chainless/docs/intro",draft:!1,unlisted:!1,tags:[],version:"current",sidebarPosition:1,frontMatter:{sidebar_position:1},sidebar:"tutorialSidebar",next:{title:"Persistent Functions",permalink:"/chainless/docs/persistent-functions"}},l={},a=[{value:"Getting Started",id:"getting-started",level:2},{value:"Run Blockchain Nodes",id:"run-blockchain-nodes",level:3},{value:"Self-Hosted Nodes",id:"self-hosted-nodes",level:4},{value:"3rd-Party Services",id:"3rd-party-services",level:4},{value:"Install Chainless",id:"install-chainless",level:3},{value:"Create Functions",id:"create-functions",level:3}];function d(e){const n={a:"a",code:"code",h1:"h1",h2:"h2",h3:"h3",h4:"h4",li:"li",ol:"ol",p:"p",ul:"ul",...(0,i.R)(),...e.components};return(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)(n.h1,{id:"chainless-docs",children:"Chainless Docs"}),"\n",(0,t.jsx)(n.h2,{id:"getting-started",children:"Getting Started"}),"\n",(0,t.jsx)(n.h3,{id:"run-blockchain-nodes",children:"Run Blockchain Nodes"}),"\n",(0,t.jsx)(n.p,{children:"Chainless requires access to at least one blockchain node RPC server. You can either run the nodes yourself or use a 3rd party service."}),"\n",(0,t.jsx)(n.h4,{id:"self-hosted-nodes",children:"Self-Hosted Nodes"}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsx)(n.li,{children:(0,t.jsx)(n.a,{href:"https://bitcoin.org/en/download",children:"Bitcoin"})}),"\n",(0,t.jsx)(n.li,{children:(0,t.jsx)(n.a,{href:"https://ethereum.org/en/developers/docs/nodes-and-clients/run-a-node/",children:"Ethereum"})}),"\n",(0,t.jsx)(n.li,{children:(0,t.jsx)(n.a,{href:"https://topl.github.io/Bifrost/docs/current/reference/getting-started",children:"Apparatus"})}),"\n"]}),"\n",(0,t.jsx)(n.h4,{id:"3rd-party-services",children:"3rd-Party Services"}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsx)(n.li,{children:(0,t.jsx)(n.a,{href:"https://getblock.io/",children:"GetBlock"})}),"\n"]}),"\n",(0,t.jsx)(n.h3,{id:"install-chainless",children:"Install Chainless"}),"\n",(0,t.jsxs)(n.ol,{children:["\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"docker volume create chainless_data"}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsx)(n.li,{children:"Creates a persistent location for saving block and function data"}),"\n"]}),"\n"]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"docker network create chainless"}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsx)(n.li,{children:"Creates a dedicated bridge network for function containers"}),"\n"]}),"\n"]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"docker run -d --name chainless --restart=always -p 42069:42069 -v chainless_data:/app -v /var/run/docker.sock:/var/run/docker.sock -v  /tmp/chainless:/tmp/chainless --network chainless seancheatham/chainless:latest --ethereum-rpc-address $ETHEREUM_RPC_ADDRESS --bitcoin-rpc-address $BITCOIN_RPC_ADDRESS --apparatus-rpc-address $APPARATUS_RPC_ADDRESS"}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:["Substitute your own ethereum, bitcoin, and/or apparatus node addresses.","\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:["If you don't have one and don't mind centralization, you can use a service like ",(0,t.jsx)(n.a,{href:"https://getblock.io/",children:"GetBlock"})]}),"\n",(0,t.jsx)(n.li,{children:"If you don't have one and need guaranteed chain integrity, you need to install and run your own nodes."}),"\n",(0,t.jsx)(n.li,{children:"At least one chain must be configured."}),"\n"]}),"\n"]}),"\n",(0,t.jsxs)(n.li,{children:["Docker-out-of-Docker is used as a runtime for function containers.","\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:["The implementation expects the bridge network to be named ",(0,t.jsx)(n.code,{children:"chainless"}),"."]}),"\n",(0,t.jsxs)(n.li,{children:["The implementation expects the ",(0,t.jsx)(n.code,{children:"-v  /tmp/chainless:/tmp/chainless"})," binding exactly as specified."]}),"\n"]}),"\n"]}),"\n",(0,t.jsxs)(n.li,{children:["If you like to live on the edge, you can use the ",(0,t.jsx)(n.code,{children:"seancheatham/chainless:dev"})," image tag to try the newest bugs and features."]}),"\n"]}),"\n"]}),"\n",(0,t.jsxs)(n.li,{children:["Open ",(0,t.jsx)(n.a,{href:"http://localhost:42069",children:"http://localhost:42069"})," in your cat video browser"]}),"\n"]}),"\n",(0,t.jsx)(n.h3,{id:"create-functions",children:"Create Functions"}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.a,{href:"temporary-functions",children:"Temporary"}),"\nThe function only runs while the connection is open.  Once closed, the function disappears."]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.a,{href:"persistent-functions",children:"Persistent"}),"\nThe function runs in the background on Chainless backend servers until you delete it."]}),"\n"]})]})}function h(e={}){const{wrapper:n}={...(0,i.R)(),...e.components};return n?(0,t.jsx)(n,{...e,children:(0,t.jsx)(d,{...e})}):d(e)}},8453:(e,n,s)=>{s.d(n,{R:()=>c,x:()=>o});var t=s(6540);const i={},r=t.createContext(i);function c(e){const n=t.useContext(r);return t.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function o(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:c(e.components),t.createElement(r.Provider,{value:n},e.children)}}}]);