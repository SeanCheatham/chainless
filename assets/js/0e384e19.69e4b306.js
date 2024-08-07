"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[976],{1512:(e,n,s)=>{s.r(n),s.d(n,{assets:()=>l,contentTitle:()=>o,default:()=>h,frontMatter:()=>r,metadata:()=>c,toc:()=>d});var t=s(4848),i=s(8453);const r={sidebar_position:1},o="Chainless Docs",c={id:"intro",title:"Chainless Docs",description:"Getting Started",source:"@site/docs/intro.md",sourceDirName:".",slug:"/intro",permalink:"/chainless/docs/intro",draft:!1,unlisted:!1,tags:[],version:"current",sidebarPosition:1,frontMatter:{sidebar_position:1},sidebar:"tutorialSidebar",next:{title:"Persistent Functions",permalink:"/chainless/docs/persistent-functions"}},l={},d=[{value:"Getting Started",id:"getting-started",level:2},{value:"Run Blockchain Nodes",id:"run-blockchain-nodes",level:3},{value:"Install Chainless",id:"install-chainless",level:3},{value:"Create Functions",id:"create-functions",level:3}];function a(e){const n={a:"a",code:"code",h1:"h1",h2:"h2",h3:"h3",li:"li",ol:"ol",p:"p",strong:"strong",ul:"ul",...(0,i.R)(),...e.components};return(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)(n.h1,{id:"chainless-docs",children:"Chainless Docs"}),"\n",(0,t.jsx)(n.h2,{id:"getting-started",children:"Getting Started"}),"\n",(0,t.jsx)(n.h3,{id:"run-blockchain-nodes",children:"Run Blockchain Nodes"}),"\n",(0,t.jsxs)(n.p,{children:["Chainless requires access to at least one blockchain node RPC server. You can either run the nodes yourself or use a 3rd party service.\n",(0,t.jsx)(n.strong,{children:"Self-Hosted Nodes"})]}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsx)(n.li,{children:(0,t.jsx)(n.a,{href:"https://bitcoin.org/en/download",children:"Bitcoin"})}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.a,{href:"https://ethereum.org/en/developers/docs/nodes-and-clients/run-a-node/",children:"Ethereum"}),"\n",(0,t.jsx)(n.strong,{children:"3rd-Party Services"})]}),"\n",(0,t.jsx)(n.li,{children:(0,t.jsx)(n.a,{href:"https://getblock.io/",children:"GetBlock"})}),"\n"]}),"\n",(0,t.jsx)(n.h3,{id:"install-chainless",children:"Install Chainless"}),"\n",(0,t.jsxs)(n.ol,{children:["\n",(0,t.jsxs)(n.li,{children:["Install ",(0,t.jsx)(n.a,{href:"https://docs.docker.com/engine/install/",children:"Docker"})]}),"\n",(0,t.jsxs)(n.li,{children:["Run ",(0,t.jsx)(n.code,{children:"docker volume create chainless_data"}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsx)(n.li,{children:"Creates a persistent location for saving block and function data"}),"\n"]}),"\n"]}),"\n",(0,t.jsxs)(n.li,{children:["Run ",(0,t.jsx)(n.code,{children:"docker run -d --name chainless --restart=always -p 42069:42069 -v chainless_data:/app --privileged seancheatham/chainless:latest --ethereum-rpc-address $ETHEREUM_RPC_ADDRESS --bitcoin-rpc-address $BITCOIN_RPC_ADDRESS"}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:["Substitute your own ethereum and bitcoin node addresses.","\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:["If you don't have one and don't mind centralization, you can use a service like ",(0,t.jsx)(n.a,{href:"https://getblock.io/",children:"GetBlock"})]}),"\n",(0,t.jsx)(n.li,{children:"If you don't have one and need guaranteed chain integrity, you need to install and run your own nodes."}),"\n",(0,t.jsx)(n.li,{children:"At least one chain must be configured."}),"\n"]}),"\n"]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.code,{children:"--privileged"})," is needed to run persistent functions within Docker containers.","\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:["This flag introduces some ",(0,t.jsx)(n.a,{href:"https://docs.docker.com/reference/cli/docker/container/run/#privileged",children:"security risks"}),". Use it if you prefer to use gasoline to start campfires."]}),"\n",(0,t.jsxs)(n.li,{children:["Alternatively, you may install ",(0,t.jsx)(n.a,{href:"https://github.com/nestybox/sysbox",children:"sysbox"})," for better security. Use it if you drive below the posted speed limit on roadways."]}),"\n"]}),"\n"]}),"\n"]}),"\n"]}),"\n",(0,t.jsxs)(n.li,{children:["Open ",(0,t.jsx)(n.a,{href:"http://localhost:42069",children:"http://localhost:42069"})," in your cat picture viewer/web browser"]}),"\n"]}),"\n",(0,t.jsx)(n.h3,{id:"create-functions",children:"Create Functions"}),"\n",(0,t.jsxs)(n.ul,{children:["\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.a,{href:"temporary-functions",children:"Temporary"}),"\nThe function only runs while the connection is open.  Once closed, the function disappears."]}),"\n",(0,t.jsxs)(n.li,{children:[(0,t.jsx)(n.a,{href:"persistent-functions",children:"Persistent"}),"\nThe function runs in the background on Chainless backend servers until you delete it."]}),"\n"]})]})}function h(e={}){const{wrapper:n}={...(0,i.R)(),...e.components};return n?(0,t.jsx)(n,{...e,children:(0,t.jsx)(a,{...e})}):a(e)}},8453:(e,n,s)=>{s.d(n,{R:()=>o,x:()=>c});var t=s(6540);const i={},r=t.createContext(i);function o(e){const n=t.useContext(r);return t.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function c(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:o(e.components),t.createElement(r.Provider,{value:n},e.children)}}}]);