package com.github.diplombmstu.httpstreamer.example;

import com.github.diplombmstu.httpstreamer.HttpMjpegStreamer;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * TODO add comment
 */
public class ExampleMain
{
    public static void main(String[] args) throws IOException, AWTException, InterruptedException
    {
        HttpMjpegStreamer streamer = new HttpMjpegStreamer(new FileFrameProvider(Paths.get("sample.jpg")), 8080);
        streamer.start();
    }
}
