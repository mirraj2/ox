## IO

Tired of writing 10 lines of java code to do a simple IO operation?

IO.java is a clean, flexible API that does what you want in one line of code.

It's as simple as IO.from(a).to(b)

```java
//Read a file to a String
String data = IO.from(new File("data.txt")).toString();

//Read a file as an Image
BufferedImage img = IO.from(new File("img.png")).toImage();

//Read a URL as Json
Json json = IO.fromURL("http://api.website.com/getFriends").toJson();
```

Writing data is just as easy.

```java
//Write a String to a File
IO.from("Coding is fun!").to(new File("output.txt"));

//Write an image to a File
IO.from(myImage).to(new File("img.png"));
```
