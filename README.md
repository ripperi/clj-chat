# clj-chat

Chat service build with clojure and clojurescript.  
Deployed at: http://clj-chat.herokuapp.com/

## Development Mode

### Run application:

```
lein clean
lein figwheel dev
```
Start server in repl with clj-chat.server/-main

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3000](http://localhost:3000).

## TO DO:

* Add channel & add member
* Message persistence (Datomic or in-memory)
* Authentication (Google auth?)
* Voice channels

## Production Build

```
lein clean
lein uberjar
```

That should compile the clojurescript code first, and then create the standalone jar.

When you run the jar you can set the port the ring server will use by setting the environment variable PORT.
If it's not set, it will run on port 3000 by default.

To deploy to heroku, first create your app:

```
heroku create
```

Then deploy the application:

```
git push heroku master
```

To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
