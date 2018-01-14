# JSON-RPC Client

This is a JSON-RPC client using the [JSON-RPC](http://www.jsonrpc.org/) implementation of [RabbitMQ](http://www.rabbitmq.com/).

The purpose is to show how to implement a JSON-RPC client/server pair with RabbitMQ in Java.

The corresponding JSON-RPC server can be found [here](https://github.com/weibeld/JSON-RPC-Server-Heroku).

## What is JSON-RPC?

JSON-RPC is a protocol that allows a client to make remote procedure calls (RPC) to a server. It allows applications running on different machines to communicate with each other.

JSON-RPC can run over different network protocols like HTTP or TCP. In the case of the RabbitMQ, the underlying network protocol is [AMQP](https://www.amqp.org/).

In JSON-RPC, the data for the RPC requests and replies is encoded in JSON.

Note: RPC is a *synchronous* communication paradigm. That is, after making an RPC request, the client blocks until it gets a response from the server.

## Implementation

This implementation uses:

- [JSON-RPC](http://www.jsonrpc.org/): a lightweight RPC protocol
- [RabbitMQ](http://www.rabbitmq.com/): message passing service (message broker) implementing the [AMQP](https://www.amqp.org/) protocol
- [RabbitMQ Java Client Library](http://www.rabbitmq.com/java-client.html): Java APIs for RabbitMQ
- [`JsonRpcClient`](http://www.rabbitmq.com/releases/rabbitmq-java-client/current-javadoc/com/rabbitmq/tools/jsonrpc/JsonRpcClient.html): class of the RabbitMQ Java Client Library that implements a JSON-RPC client
- [Heroku](http://heroku.com): Platform as a Service (PaaS) provider for running any apps in the cloud
- [CloudAMQP](https://elements.heroku.com/addons/cloudamqp): Heroku add-on providing "RabbitMQ as a Service" for Heroku apps

## Run on Heroku

### Create Heroku App

Create an app on Heroku for your JSON-RPC client:

~~~bash
heroku create YOUR-APP-NAME
~~~

### Set Up RabbitMQ

Install the CloudAMQP add-on for this Heroku application:

~~~bash
heroku addons:create cloudamqp
~~~

This creates an additional Heroku dyno running a **RabbitMQ server** for your application on Heroku.

In addition, it adds the following config vars to your Heroku application:

- `CLOUDAMQP_APIKEY`
- `CLOUDAMQP_URL`

You can confirm this with `heroku config`.

The value of the `CLOUDAMQP_URL` variable is the URI of the RabbitMQ server that has just been created on Heroku. Your application needs this URI in order to connect to the RabbitMQ server.

**Important:** you have to execute the above command **only once** for the JSON-RPC client/server pair. If you already ran this for the server, then **do not** run it again for the client. Instead, just add the above config vars to the client application:

~~~bash
heroku config:set CLOUDAMQP_APIKEY="..."
heroku config:set CLOUDAMQP_URL="..."
~~~

### Run

For running the JSON-RPC client for the first time (and after every source code edit), you have to deploy the application to Heroku as usual: 

~~~bash
git push heroku master
~~~

After this has been done once, you can run the JSON-RPC client on a [one-off dyno](https://devcenter.heroku.com/articles/one-off-dynos). This is faster than deploying with `git push heroku master`, because it doesn't rebuild the application each time:

~~~bash
heroku run "java -jar build/libs/rpc-client-all.jar"
~~~

The `java -jar build/libs/rpc-client-all.jar` command is the same command as in the Procfile.

#### Client Crashed?

If you deploy the JSON-RPC client with `git push heroku master`, and then inspect the logs with `heroku logs`, you might see a line like:

~~~
Process exited with status 0
~~~

And after that a line like:

~~~
State changed from up to crashed
~~~

And then the process is started again, and the above repeats.

This is not actually a crash, and it doesn't do any harm. It's because our JSON-RPC client terminates (on purpose) as soon as it receives a response from the server for its initial request.

When our JSON-RPC client process terminates, Heroku interprets this as a crash (even if the exit status is 0), and it applies its [dyno crash restart policy](https://devcenter.heroku.com/articles/dynos#dyno-crash-restart-policy).

With this policy, after the first "crash", the process is restarted immediately one more time. Then, if the process terminates again, it is repeatedly restarted after a certain amount of offset time.

This explains the behaviour that you see in the logs, if you deploy the JSON-RPC client with `git push heroku master`.

On the other hand, if you start the JSON-RPC client on a one-off dyno (as explained above), there are no such "crashes". This is because the one-off dyno just terminates when the process with which it was started terminates.

While the "crash" and restart of the client process on a regular Heroku dyno doesn't do any harm, it is cleaner to start the process on a one-off dyno where this doesn't happen.

Note that in a real-world application the client would also be a non-terminating application, and thus the above wouldn't be an issue.

### Monitor

To see all the queues and their content on the RabbitMQ server, use the **CloudAMQP Dashboard**:

~~~bash
heroku addons:open cloudamqp
~~~

Note that this command only works from the application (server or client) on which you *installed* the CloudAMQP add-on (i.e. the one in which you executed `heroku addons:create cloudamqp`).

### Order of Execution

The JSON-RPC client is a short-running application. It makes one request to the server, waits for the response, and then terminates. 

Thus, the normal order of execution is to first start the [JSON-RPC server](https://github.com/weibeld/JSON-RPC-Server-Heroku), and then the JSON-RPC client. In this case, the request sent by the JSON-RPC client is handled immediately by the JSON-RPC server.

However, starting the JSON-RPC client before the JSON-RPC server is running is also possible. In this case, there are two possibilities of what can happen:

- If the RPC request queue already exists (if the JSON-RPC server has been running before at some time), the message sent by the JSON-RPC client is stored in the queue until the JSON-RPC server starts up, at which point the JSON-RPC server handles the message instantly.
- If the RPC request queue does not exist, then the message sent by the JSON-RPC client is simply discarded. When the JSON-RPC server starts up, it doesn't receive this message, because it has not been saved in the RPC request queue. Consequently, the JSON-RPC client will never receive a response for this message.


### Tip

If no messages seem to be sent at all, make sure that there's actually a dyno scaled for the JSON-RPC client and server applications:

~~~bash
heroku ps
~~~~

Scale one dyno for the JSON-RPC client:

~~~bash
heroku ps:scale client=1
~~~

## Run Locally

The Heroku application can be run on the local machine, which is handy during development.

However, for this to work, you need to install and run a RabbitMQ server on your local machine.

### Install RabbitMQ Server

Install the RabbitMQ server on your local machine according to the instructions [here](http://www.rabbitmq.com/download.html).

This provides the command `rabbitmq-server`, which starts a RabbitMQ server on the default port 5672.

Note that if you install with Homebrew, you might need to add the folder containing the RabbitMQ executables to the `PATH`.

### Run

First, make sure that the RabbitMQ server is running on the local machine with `rabbitmq-server`.

Then, start the JSON-RPC client:

~~~bash
heroku local
~~~~

### Monitor

See all queues and their content of the local RabbitMQ server in the [Management Web UI](http://www.rabbitmq.com/management.html) here (username: **guest**, password: **guest**): <http://localhost:15672> .

You can also list all the queues from the command line:

~~~bash
sudo rabbitmqctl list_queues
~~~
