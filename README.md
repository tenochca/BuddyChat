# BuddyRobot Speech System Integration

* Most of the relevant code is inside `app/src/main/java/com.example.buddychat/`
* Full app runs from `MainActivity.java`
* UI for buttons and text is loaded in from `app/src/main/res/layout/activity_main.xml`
  * Then inside of `MainActivity.java` you can attach listeners to the buttons and programmatically modify each element as needed.

* Currently, on startup, click the "Log In" button to get profile information and authorization tokens.
* Then the "Start Chat" will establish a connection to the WebSocket server.
* Clicking the "Hello" button will send a demo message through the connection, and we receive the servers response.
* Finally, the "Start Chat" button should now say "End Chat", and we can click that again to finish the chat.
