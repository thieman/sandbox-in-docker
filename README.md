# Free your Sandbox using Docker

Run an unrestricted Clojure sandbox using Docker. Runs absolutely any code you throw at it over HTTP, prevents malicious users from doing anything to your system or network, and restarts its workers if they're killed. Why sandbox when you can Docker?

## Deploying

Try it out for yourself using [Docker](http://www.docker.io/)!

```
git clone git@github.com:thieman/sandbox-in-docker
sandbox-in-docker/start_sandbox  # see this script for the full Docker commands
```

## Architecture

<img height=300 src="http://i.imgur.com/LYlByWY.png"></img>

The sandbox is deployed with Docker. The main sandbox instance is referred to as the "master" and is responsible for providing the HTTP endpoint to accept and respond to incoming evaluation requests. This runs on top of your actual machine, the "host."

The sandbox executes any Clojure code you throw at it with no restrictions at all. To allow this with a modicum of sanity, the master spins off child Docker containers to execute the Clojure code. These containers are modeled off of [Docker in Docker](https://github.com/jpetazzo/dind) and are fully expendable. They are cut off from the Internet and are only allowed to communicate on port 8080, which they use to talk to the master container using [ZeroMQ](https://github.com/lynaghk/zmq-async). If anything causes an executor to time out when executing Clojure code, the master will kill it and spin up a new container to replace it.

## TODO and/or Glaring Flaws

This project is a proof-of-concept and has some serious flaws. Here's just a few:

* Currently, there is actually only one exec process in the pool.
* All users are talking to the same underlying Clojure environment, meaning they can mess with each other's namespaces. This also means that the entire environment is lost if some smartass runs `(system/exit 0)`.
* The master will restart an executor if it does not return after 10 seconds. However, this does not prevent someone from having some sort of infinite loop fire after X seconds. We should have CPU/RAM monitoring on the executors and restart them if they're acting up.
* There are no measures in place to blacklist malicious actors.

## License

Copyright © 2014 Travis Thieman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
