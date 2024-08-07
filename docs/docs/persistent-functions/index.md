---
description: Information about Chainless Persistent Functions.
slug: /persistent-functions
sidebar_position: 2
---

# Persistent Functions
Persistent functions run in the background on Chainless servers.  Unlike "Temporary Functions", Persistent Functions will keep running even after you close your browser window.  As new blocks are created, they are dispatched to your function which will apply the event and return a new state.  The latest state is saved in the Chainless database and can be accessed at any time.

## Creating a Persistent Function

### Function Workflow
Unlike Temporary Functions, your code can include external dependencies.  In fact, you'll probably need at least one (an HTTP client) to get started.  The workflow and implementation for Persistent Functions is a bit different from Temporary Functions.  Instead of implementing two functions that are called by the runtime, a Persistent Function implements a "main" method that calls an API endpoint for new events in a loop.

In general, it works like:
1. Call the `GET /init` endpoint to receive a new event.
  If an empty body is returned, that indicates there are no more tasks to complete.  Your function's "main" can exit at this point.

  Otherwise, the returned payload will resemble either:
  ```
  {
    taskType: "init",
    configuration: { ... }
  }
  ```
  **or**
  ```
  {
    taskType: "applyBlock",
    blockWithChain {
      meta {
        blockId: "...",
        chain: "bitcoin",
        height: 123
      }
      block {
        ...
      }
    },
    stateWithChains {
      state {
        ...
      },
      chainStates {
        bitcoin: "..."
      }
    }
  }
  ```
1. Depending on the taskType from the previous step, perform necessary side-effects and computations, and construct a new "state".
1. After performing your function's work, the new state should be sent back to the API at `POST /success` with the new state as the body.
1. If your function is unable to apply the block and enters a permanently failed state, you can send `POST /error` with a string message as the body.  After this, your function will no longer be invoked.
1. Once finished the the work and a result has been sent to either `/success` or `/error`, repeat from step 1.

### Write the Code
See the corresponding documentation for your preferred language/runtime.
- [JavaScript](persistent-functions/javascript)
- [JVM](persistent-functions/jvm)

### Deploy the Function
Navigate to the [Chainless App](http://localhost:42069).  Open the menu in the bottom-right corner, and select "Create Persistent Function".

#### Function Info
On the first page, specify your `Function Name`, `Language`, and `Blockchains`.
The Function Name should be between 1-63 characters. Also, multiple chains can be selected under than one `Blockchains`. Remember, blockchains produce blocks at different rates. Click `Next` to proceed to the next step.

#### Upload Code
On the next page, you should see a button `Select File`.  You should choose the zipped file you created in the previous step.  Once selected, the file will immediately start to upload.  Once uploaded, it will automatically move to the next page.  At this time, once your code is uploaded, it can't be updated.  You will need to create a new function instead.

#### Initialize
On the final page, you will see a big text editor box.  If your function requires any special parameters, they should be defined here as JSON.  This JSON will be provided to the function during its `init` step.  At the bottom of the page, there is a button `Set Start Time` which allows you to retroactively apply old blocks to your function.  Keep in mind, you pay for what you use, so if you pick a date really far in the past, you may incur hefty charges.  Once you're ready, click `Run`.  You will be taken back to the Functions List page where you should see your new function.