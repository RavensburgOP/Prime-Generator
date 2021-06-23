# Prime Number Stream Service

Get all prime numbers up until and including a target number

## Assumptions

- The instructions specify the broad definition `number`, I've assumed that this means `integers`. The service does therefore only handle these, not `floats`, `hex`, `binary`, etc
- The requirement is for the service to handle *up to a given \<number\>*, but the example given (`/prime/17`) returns up to **and including** 17. I've assumed that the service should therefore return *up to and including \<number\>*

## Design

Here's an overview of my initial plans for the design. I'll try to keep a small log of my thoughts and describe changes from my original ideas in the [Worklog Section](## Worklog).

I've decided to implement the proxy-service and prime-number-server as separate projects. This is mostly due to this being what I'm used to in other languages.

I've used the `Akka` framework, since I remember it being mentioned when I was reading documentation `Elixir` for a hobby project. The docs says it has support for `gRPC` as well, so I think it checks all the boxes I need.

I'd like to be able to spin each service up as a `Docker` image and then use `docker-compose` to initiate the both services at the same time.

I've got a combined five minutes of experience with `Scala` going into this, so most everything else in the stack was chosen because it was the top Google result for whatever I was looking for.

### Prime-Proxy

I'd like the `prime-proxy` service to be as minimal as possible. Having the code split between two services increases the risk of complex bugs, so I'll try to keep the majority of complexity in the `prime-service` and only handle the `HTTP` parsing and sanity checks in the `prime-proxy`

Since prime numbers can only be positive, I'd like to have the endpoint only accept a `u64`, so hopefully `Scala` has a concept of `unsigned integer`. By doing this, it allows the service to return larger prime numbers back, and it eliminates negative integers as invalid inputs (I'm expecting `Scala` to have some kind of type check and send back a `400` response if deserialization fails. Will need to confirm this assumption).

It should be able to recover from the `prime-service` being unavailable and retry the connection, some kind of exponential backoff would probably be enough.

### Prime-generator-service

I'd like to implement some kind of lazy generator in the `prime-service`, do to the lower memory requirement. I think it might have a little bit of a performance overhead compared to calculating it all in one go, but I'll have to do some benchmarks to confirm.

Since the output is deterministic the potential performance hit could be offset by caching or memoization of the result in the `prime-service`. In a production setting, I would most likely use something like `Redis` for caching, but that seems a bit complicated for a tech-test.

If I get the caching/memoization working, the next step should probably be threading/concurrency. I did some small hobby projects in `Elixir` and I remember the actor system well suited for parallel tasks. There's a risk of having several actors computing the same prime numbers if there's a large number of requests.

This would be wasteful, so I think a quick solution to this could be to have one process calculate new prime numbers up to the "highest" received `<number>` and then send a message to actors that are processing requests, whenever a new one is found.

This is not optimal either, but I think I can get it done quickly. If I have time, I would like to try finding a way to have a function calculate new prime numbers and output them through a memoized generator. I don't know enough `Scala` to make a call on how this would work, but since it's has some FP-concepts I'm going to wager, memoization is possible.

## Worklog

- Spent two hours on setting up a skeleton for the services and trying different stuff out. 1.5 of those hours was to figure out the build system and getting a `hello world` project spun up. I then changed the endpoint to match the task and had it reply with a list of prime number up to and including 17.
- After an additional hour, I've persuaded the build system to find the correct folder with my gRPC `.proto` definitions. There's only one call defined, which gets prime numbers in a stream. I would like to circle back and see if I need to communicate any other information through a separate stream between the services - maybe some error messaging. I should circle back to this later
[x] Check if there's a need for more gRPC function definitions (error messaging, health check, etc)
- Since I have a minimal viable solution, I'm can move on to improving on it. There's a couple of things I'd like to get done:
    [ ] Error handling/recovery
    [x] Algorithm optimization (switch to lazy evaluation)
    [ ] Testing (docs are not very helpful, so might just go for writing a simple sanity-check script)
- I decided to start with error handling, since preventing crashes makes development easier. First I need to figure out, which crashes I can expect in Scala as I haven't used it outside of this assignment. I can see a couple of potential failure points, that I will need to confirm or reject:
  - Lost connection from proxy to prime service
  - Badly formatted request or response
- I tried calling `localhost:8080/prime/2147483648` on the service to check if it was streaming the results as new primes were calculated or whether the primes were returned in bulk after the computation. It turned out to be the latter, and I also found out that I couldn't shutdown the prime-service, as the garbage collector is blocking. Changing the algorithm might fix this.
- Changed the generator so it uses recursion, which allows to stream the generated numbers and prevents the service from becoming unresponsive
- Looking through the [Akka gRPC docs](https://doc.akka.io/docs/akka-grpc/current/client/details.html), it seems that the generated client will try to reconnect by default, so I can scratch that from my todo
- Decided not to expand the number of gRPC functions, as it looks like the library itself takes care of many of those issues
