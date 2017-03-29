package com.github.diplombmstu.httpstreamer.example;

import com.github.diplombmstu.httpstreamer.FrameProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * TODO add comment
 */
public class FileFrameProvider implements FrameProvider
{
    private final BufferedImage image;
    private final int width;
    private final int height;

    public FileFrameProvider(Path imagePath) throws IOException
    {
        FileInputStream fis = new FileInputStream(imagePath.toFile());
        FileChannel channel = fis.getChannel();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        channel.transferTo(0, channel.size(), Channels.newChannel(byteArrayOutputStream));

        image = ImageIO.read(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
        width = image.getWidth();
        height = image.getHeight();
    }

    @Override
    public BufferedImage getNextFrame()
    {
        return image;
    }

    @Override
    public int getFrameWidth()
    {
        return width;
    }

    @Override
    public int getFrameHeight()
    {
        return height;
    }
}
