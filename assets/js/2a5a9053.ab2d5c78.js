"use strict";(self.webpackChunkdocs=self.webpackChunkdocs||[]).push([[229],{6210:(e,n,t)=>{t.r(n),t.d(n,{assets:()=>a,contentTitle:()=>r,default:()=>h,frontMatter:()=>o,metadata:()=>c,toc:()=>l});var i=t(4848),s=t(8453);const o={description:"Information about Chainless Persistent Functions.",slug:"/persistent-functions",sidebar_position:2},r="Persistent Functions",c={id:"persistent-functions/index",title:"Persistent Functions",description:"Information about Chainless Persistent Functions.",source:"@site/docs/persistent-functions/index.md",sourceDirName:"persistent-functions",slug:"/persistent-functions",permalink:"/docs/persistent-functions",draft:!1,unlisted:!1,tags:[],version:"current",sidebarPosition:2,frontMatter:{description:"Information about Chainless Persistent Functions.",slug:"/persistent-functions",sidebar_position:2},sidebar:"tutorialSidebar",previous:{title:"Chainless Intro",permalink:"/docs/intro"},next:{title:"JavaScript",permalink:"/docs/persistent-functions/javascript"}},a={},l=[{value:"Creating a Persistent Function",id:"creating-a-persistent-function",level:2},{value:"Function Workflow",id:"function-workflow",level:3},{value:"Write the Code",id:"write-the-code",level:3},{value:"Deploy the Function",id:"deploy-the-function",level:3},{value:"Function Info",id:"function-info",level:4},{value:"Upload Code",id:"upload-code",level:4},{value:"Initialize",id:"initialize",level:4}];function d(e){const n={a:"a",code:"code",h1:"h1",h2:"h2",h3:"h3",h4:"h4",li:"li",ol:"ol",p:"p",pre:"pre",strong:"strong",ul:"ul",...(0,s.R)(),...e.components};return(0,i.jsxs)(i.Fragment,{children:[(0,i.jsx)(n.h1,{id:"persistent-functions",children:"Persistent Functions"}),"\n",(0,i.jsx)(n.p,{children:'Persistent functions run in the background on Chainless servers.  Unlike "Temporary Functions", Persistent Functions will keep running even after you close your browser window.  As new blocks are created, they are dispatched to your function which will apply the event and return a new state.  The latest state is saved in the Chainless database and can be accessed at any time.'}),"\n",(0,i.jsx)(n.h2,{id:"creating-a-persistent-function",children:"Creating a Persistent Function"}),"\n",(0,i.jsx)(n.h3,{id:"function-workflow",children:"Function Workflow"}),"\n",(0,i.jsx)(n.p,{children:'Unlike Temporary Functions, your code can include external dependencies.  In fact, you\'ll probably need at least one (an HTTP client) to get started.  The workflow and implementation for Persistent Functions is a bit different from Temporary Functions.  Instead of implementing two functions that are called by the runtime, a Persistent Function implements a "main" method that calls an API endpoint for new events in a loop.'}),"\n",(0,i.jsx)(n.p,{children:"In general, it works like:"}),"\n",(0,i.jsxs)(n.ol,{children:["\n",(0,i.jsxs)(n.li,{children:["Call the ",(0,i.jsx)(n.code,{children:"GET /init"}),' endpoint to receive a new event.\nIf an empty body is returned, that indicates there are no more tasks to complete.  Your function\'s "main" can exit at this point.']}),"\n"]}),"\n",(0,i.jsx)(n.p,{children:"Otherwise, the returned payload will resemble either:"}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{children:'{\n  taskType: "init",\n  configuration: { ... }\n}\n'})}),"\n",(0,i.jsx)(n.p,{children:(0,i.jsx)(n.strong,{children:"or"})}),"\n",(0,i.jsx)(n.pre,{children:(0,i.jsx)(n.code,{children:'{\n  taskType: "applyBlock",\n  blockWithChain {\n    meta {\n      blockId: "...",\n      chain: "bitcoin",\n      height: 123\n    }\n    block {\n      ...\n    }\n  },\n  stateWithChains {\n    state {\n      ...\n    },\n    chainStates {\n      bitcoin: "..."\n    }\n  }\n}\n'})}),"\n",(0,i.jsxs)(n.ol,{children:["\n",(0,i.jsx)(n.li,{children:'Depending on the taskType from the previous step, perform necessary side-effects and computations, and construct a new "state".'}),"\n",(0,i.jsxs)(n.li,{children:["After performing your function's work, the new state should be sent back to the API at ",(0,i.jsx)(n.code,{children:"POST /success"})," with the new state as the body."]}),"\n",(0,i.jsxs)(n.li,{children:["If your function is unable to apply the block and enters a permanently failed state, you can send ",(0,i.jsx)(n.code,{children:"POST /error"})," with a string message as the body.  After this, your function will no longer be invoked."]}),"\n",(0,i.jsxs)(n.li,{children:["Once finished the the work and a result has been sent to either ",(0,i.jsx)(n.code,{children:"/success"})," or ",(0,i.jsx)(n.code,{children:"/error"}),", repeat from step 1."]}),"\n"]}),"\n",(0,i.jsx)(n.h3,{id:"write-the-code",children:"Write the Code"}),"\n",(0,i.jsx)(n.p,{children:"See the corresponding documentation for your preferred language/runtime."}),"\n",(0,i.jsxs)(n.ul,{children:["\n",(0,i.jsx)(n.li,{children:(0,i.jsx)(n.a,{href:"persistent-functions/javascript",children:"JavaScript"})}),"\n",(0,i.jsx)(n.li,{children:(0,i.jsx)(n.a,{href:"persistent-functions/jvm",children:"JVM"})}),"\n"]}),"\n",(0,i.jsx)(n.h3,{id:"deploy-the-function",children:"Deploy the Function"}),"\n",(0,i.jsxs)(n.p,{children:["Navigate to the ",(0,i.jsx)(n.a,{href:"https://app.chainless.dev",children:"Chainless App"}),'.  Open the menu in the bottom-right corner, and select "Create Persistent Function".']}),"\n",(0,i.jsx)(n.h4,{id:"function-info",children:"Function Info"}),"\n",(0,i.jsxs)(n.p,{children:["On the first page, specify your ",(0,i.jsx)(n.code,{children:"Function Name"}),", ",(0,i.jsx)(n.code,{children:"Language"}),", and ",(0,i.jsx)(n.code,{children:"Blockchains"}),".\nThe Function Name should be between 1-63 characters. Also, multiple chains can be selected under than one ",(0,i.jsx)(n.code,{children:"Blockchains"}),". Remember, blockchains produce blocks at different rates. Click ",(0,i.jsx)(n.code,{children:"Next"})," to proceed to the next step."]}),"\n",(0,i.jsx)(n.h4,{id:"upload-code",children:"Upload Code"}),"\n",(0,i.jsxs)(n.p,{children:["On the next page, you should see a button ",(0,i.jsx)(n.code,{children:"Select File"}),".  You should choose the zipped file you created in the previous step.  Once selected, the file will immediately start to upload.  Once uploaded, it will automatically move to the next page.  At this time, once your code is uploaded, it can't be updated.  You will need to create a new function instead."]}),"\n",(0,i.jsx)(n.h4,{id:"initialize",children:"Initialize"}),"\n",(0,i.jsxs)(n.p,{children:["On the final page, you will see a big text editor box.  If your function requires any special parameters, they should be defined here as JSON.  This JSON will be provided to the function during its ",(0,i.jsx)(n.code,{children:"init"})," step.  At the bottom of the page, there is a button ",(0,i.jsx)(n.code,{children:"Set Start Time"})," which allows you to retroactively apply old blocks to your function.  Keep in mind, you pay for what you use, so if you pick a date really far in the past, you may incur hefty charges.  Once you're ready, click ",(0,i.jsx)(n.code,{children:"Run"}),".  You will be taken back to the Functions List page where you should see your new function."]})]})}function h(e={}){const{wrapper:n}={...(0,s.R)(),...e.components};return n?(0,i.jsx)(n,{...e,children:(0,i.jsx)(d,{...e})}):d(e)}},8453:(e,n,t)=>{t.d(n,{R:()=>r,x:()=>c});var i=t(6540);const s={},o=i.createContext(s);function r(e){const n=i.useContext(o);return i.useMemo((function(){return"function"==typeof e?e(n):{...n,...e}}),[n,e])}function c(e){let n;return n=e.disableParentContext?"function"==typeof e.components?e.components(s):e.components||s:r(e.components),i.createElement(o.Provider,{value:n},e.children)}}}]);