"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[664],{3613:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>a,contentTitle:()=>r,default:()=>h,frontMatter:()=>c,metadata:()=>s,toc:()=>l});var o=t(4848),i=t(8453);const c={},r="Temporary Functions",s={id:"temporary-functions",title:"Temporary Functions",description:"These functions only runs while the connection is open.  Once closed, the function disappears.",source:"@site/docs/temporary-functions.md",sourceDirName:".",slug:"/temporary-functions",permalink:"/docs/temporary-functions",draft:!1,unlisted:!1,tags:[],version:"current",frontMatter:{},sidebar:"tutorialSidebar",previous:{title:"Data Models",permalink:"/docs/data-models"}},a={},l=[{value:"Creating a Temporary Function",id:"creating-a-temporary-function",level:2},{value:"Prepare",id:"prepare",level:3},{value:"Code",id:"code",level:3},{value:"Historical Blocks",id:"historical-blocks",level:3},{value:"Run",id:"run",level:3}];function d(e){const n={a:"a",code:"code",h1:"h1",h2:"h2",h3:"h3",p:"p",pre:"pre",...(0,i.R)(),...e.components};return(0,o.jsxs)(o.Fragment,{children:[(0,o.jsx)(n.h1,{id:"temporary-functions",children:"Temporary Functions"}),"\n",(0,o.jsx)(n.p,{children:"These functions only runs while the connection is open.  Once closed, the function disappears."}),"\n",(0,o.jsx)(n.h2,{id:"creating-a-temporary-function",children:"Creating a Temporary Function"}),"\n",(0,o.jsx)(n.h3,{id:"prepare",children:"Prepare"}),"\n",(0,o.jsxs)(n.p,{children:["Login to the ",(0,o.jsx)(n.a,{href:"https://app.chainless.dev",children:"Chainless App"}),'.  Then, click the button in the bottom-right corner to expand the menu.  Select "Create Temporary Function".']}),"\n",(0,o.jsx)(n.p,{children:"At this page, you can write your function code as a single JavaScript file.  Temporary functions may not include additional dependencies, so they are limited in functionality.  Edit the code box to implement your function."}),"\n",(0,o.jsx)(n.h3,{id:"code",children:"Code"}),"\n",(0,o.jsx)(n.p,{children:"The code is generally structured in the following manner:"}),"\n",(0,o.jsx)(n.pre,{children:(0,o.jsx)(n.code,{className:"language-js",children:"(function(functionState, blockWithMeta) {\n  let state = { ...functionState.state }\n  // ...\n  return state\n})\n"})}),"\n",(0,o.jsx)(n.p,{children:"This function accepts a function state and a new block, and produces a new state.  It is invoked over-and-over again by the backend as new blocks arrive."}),"\n",(0,o.jsxs)(n.p,{children:["The ",(0,o.jsx)(n.code,{children:"functionState"})," object contains ",(0,o.jsx)(n.code,{children:".state"})," and ",(0,o.jsx)(n.code,{children:".chainStates"})," fields. ",(0,o.jsx)(n.code,{children:".state"})," is the JSON result of the previous invocation of this function. ",(0,o.jsx)(n.code,{children:".chainStates"}),' is a key-value mapping from "chain name" to "block ID".']}),"\n",(0,o.jsxs)(n.p,{children:["The ",(0,o.jsx)(n.code,{children:"blockWithMeta"})," object contains ",(0,o.jsx)(n.code,{children:".meta"})," and ",(0,o.jsx)(n.code,{children:".block"})," fields.  ",(0,o.jsx)(n.code,{children:".meta"})," includes information like ",(0,o.jsx)(n.code,{children:".chain"}),", ",(0,o.jsx)(n.code,{children:".blockId"}),", and ",(0,o.jsx)(n.code,{children:".height"}),". ",(0,o.jsx)(n.code,{children:".block"})," contains the raw JSON-encoded data of the block."]}),"\n",(0,o.jsx)(n.p,{children:"The entire function must be wrapped in (parenthesis)."}),"\n",(0,o.jsx)(n.h3,{id:"historical-blocks",children:"Historical Blocks"}),"\n",(0,o.jsxs)(n.p,{children:["You can optionally select a ",(0,o.jsx)(n.code,{children:"Start Time"})," from which blocks should be retroactively applied. For example,\nyou can instruct the function to apply all blocks from yesterday before applying new blocks."]}),"\n",(0,o.jsx)(n.h3,{id:"run",children:"Run"}),"\n",(0,o.jsxs)(n.p,{children:["Finally, click ",(0,o.jsx)(n.code,{children:"Run"}),".  Your function will start running on a Chainless server, and the state will be streamed back to you as it is updated."]})]})}function h(e={}){const{wrapper:n}={...(0,i.R)(),...e.components};return n?(0,o.jsx)(n,{...e,children:(0,o.jsx)(d,{...e})}):d(e)}},8453:(e,n,t)=>{t.d(n,{R:()=>r,x:()=>s});var o=t(6540);const i={},c=o.createContext(i);function r(e){const n=o.useContext(c);return o.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function s(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(i):e.components||i:r(e.components),o.createElement(c.Provider,{value:n},e.children)}}}]);