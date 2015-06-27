# ShutdownHandler #

ShutdownHandler allows your application to wait for an external request to trigger shutdown. Usage is as simple as creating a new ShutdownHandler, configuring any shutdown listeners, starting the listener then calling the wait method.

```
final ShutdownHandler shutdownHandler = new ShutdownHandler();

//Register a shutdown hook
shutdownHandler.registerShutdownListener(new ShutdownListener() {
    public void shutdown() {
        //Do app specific shutdown processing here
    }
});

//Start the shutdown listener
shutdownHandler.start();

//Wait for a shutdown request and all shutdown listeners to complete
shutdownHandler.waitForShutdown();
```

For the above example you would put this code in your `public static void main` method with all of your application setup before this code block. The `shutdownHandler.waitForShutdown()` **blocks** until the a shutdown request is completely handled so it should be the last line in your main method.