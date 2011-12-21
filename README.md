# PushPlay

PushPlay is a Play! Framework module that turns any Play! application into a <a href="http://www.pusher.com">Pusher</a> service clone. It is compatible with the MIT licensed pusher javascript library. 
This is considered in alpha stage, there is still work to be done, please see the TODO list below.

## Requirements

- Play 1.2.4
- Websockets compatible browser

## Usage

This is packaged as a module, so you just need to add it as a dependency to an existing Play! application (Refer to the sample app to see how the dependency looks). You are free to create a 
completely empty Play! project that just serves as a container for the PushPlay module. However, you should have an end point at /pusher/auth that generates authentication tokens
for certain events (private channel subscriptions, etc). Refer to the sample application in samples-and-tests/.

You should also modify the application.conf file to include these three properties:
<pre>
pusher.appId
pusher.key
pusher.secret
</pre>

You'll also need to overwrite the pusher connection settings in the pusher javascript client library. Namely the Pusher host and ws_port settings. Check out the index.html view in the sample
application.

## Alternatives

<a href="https://github.com/stevegraham/slanger">Slanger</a> - An open source, robust, self contained Pusher protocol server from Stevie Graham.

<a href="http://pusher.com">Pusher</a> - The service itself! They even have a free service plan!

## TODO

- Finish up presence support
- Use a database for message passing and state, besides the obvious benefits this will also help with scaling.

## Notes

The Play! Framework relies on the netty networking library for websocket support. A newer websocket protocol was recently released that is not currently supported by netty in any of the Generally Available releases. It will take some time for implementation to be completed, netty to be released and then another Play! release with the newer netty dependency. For now, don't use Chrome, as it is already using the new protocol. The sample app was tested with Safari 5.1.
