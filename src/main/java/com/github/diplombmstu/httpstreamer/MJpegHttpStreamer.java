package com.github.diplombmstu.httpstreamer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

final class MJpegHttpStreamer
{
    private static final String BOUNDARY = "--gc0p4Jq0M2Yt08jU534c0p--";
    private static final String BOUNDARY_LINES = "\r\n" + BOUNDARY + "\r\n";

    private static final String HTTP_HEADER =
            "HTTP/1.0 200 OK\r\n" + "Server: Peepers\r\n" + "Connection: close\r\n" + "Max-Age: 0\r\n" +
            "Expires: 0\r\n" + "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, " +
            "post-check=0, max-age=0\r\n" + "Pragma: no-cache\r\n" + "Access-Control-Allow-Origin:*\r\n" +
            "Content-Type: multipart/x-mixed-replace; " + "boundary=" + BOUNDARY + "\r\n" + BOUNDARY_LINES;

    private final int mPort;
    private final byte[] mBufferA;
    private final byte[] mBufferB;
    private final Object mBufferLock = new Object();
    private boolean mNewJpeg = false;
    private boolean mStreamingBufferA = true;
    private int mLengthA = Integer.MIN_VALUE;
    private int mLengthB = Integer.MIN_VALUE;
    private long mTimestampA = Long.MIN_VALUE;
    private long mTimestampB = Long.MIN_VALUE;
    private Thread mWorker = null;
    private volatile boolean mRunning = false;

    MJpegHttpStreamer(final int port, final int bufferSize)
    {
        super();
        mPort = port;
        mBufferA = new byte[bufferSize];
        mBufferB = new byte[bufferSize];
    }

    void start()
    {
        if (mRunning)
        {
            throw new IllegalStateException("MJpegHttpStreamer is already running");
        }

        mRunning = true;

        mWorker = new Thread(this::workerRun);
        mWorker.start();
    }

    void stop()
    {
        if (!mRunning)
            throw new IllegalStateException("MJpegHttpStreamer is already stopped");

        mRunning = false;
        mWorker.interrupt();
    }

    void streamJpeg(final byte[] jpeg, final int length, final long timestamp)
    {
        synchronized (mBufferLock)
        {
            final byte[] buffer;
            if (mStreamingBufferA)
            {
                buffer = mBufferB;
                mLengthB = length;
                mTimestampB = timestamp;
            }
            else
            {
                buffer = mBufferA;
                mLengthA = length;
                mTimestampA = timestamp;
            }
            System.arraycopy(jpeg, 0, buffer, 0, length);
            mNewJpeg = true;
            mBufferLock.notify();
        }
    }

    private void workerRun()
    {
        while (mRunning)
        {
            try
            {
                acceptAndStream();
            }
            catch (final IOException exceptionWhileStreaming)
            {
                System.err.println(exceptionWhileStreaming);
            }
        }
    }

    private void acceptAndStream() throws IOException
    {
        ServerSocket serverSocket = null;
        Socket socket = null;
        DataOutputStream stream = null;

        try
        {
            serverSocket = new ServerSocket(mPort);
            serverSocket.setSoTimeout(1000);

            try
            {
                socket = serverSocket.accept();
            }
            catch (final SocketTimeoutException e)
            {
                if (!mRunning)
                    return;
            }

            while (socket == null)
            {
                try
                {
                    socket = serverSocket.accept();
                }
                catch (final SocketTimeoutException e)
                {
                    if (!mRunning)
                        return;
                }
            }

            serverSocket.close();
            serverSocket = null;
            stream = new DataOutputStream(socket.getOutputStream());
            stream.writeBytes(HTTP_HEADER);
            stream.flush();

            while (mRunning)
            {
                final byte[] buffer;
                final int length;
                final long timestamp;

                synchronized (mBufferLock)
                {
                    while (!mNewJpeg)
                    {
                        try
                        {
                            mBufferLock.wait();
                        }
                        catch (final InterruptedException stopMayHaveBeenCalled)
                        {
                            return;
                        }
                    }

                    mStreamingBufferA = !mStreamingBufferA;

                    if (mStreamingBufferA)
                    {
                        buffer = mBufferA;
                        length = mLengthA;
                        timestamp = mTimestampA;
                    }
                    else
                    {
                        buffer = mBufferB;
                        length = mLengthB;
                        timestamp = mTimestampB;
                    }

                    mNewJpeg = false;
                }

                stream.writeBytes(
                        "Content-type: image/jpeg\r\n" + "Content-Length: " + length + "\r\n" + "X-Timestamp:" +
                        timestamp + "\r\n" + "\r\n");
                stream.write(buffer, 0, length);
                stream.writeBytes(BOUNDARY_LINES);
                stream.flush();
            }
        }
        finally
        {
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (final IOException closingStream)
                {
                    System.err.println(closingStream);
                }
            }

            if (socket != null)
            {
                try
                {
                    socket.close();
                }
                catch (final IOException closingSocket)
                {
                    System.err.println(closingSocket);
                }
            }

            if (serverSocket != null)
            {
                try
                {
                    serverSocket.close();
                }
                catch (final IOException closingServerSocket)
                {
                    System.err.println(closingServerSocket);
                }
            }
        }
    }
}