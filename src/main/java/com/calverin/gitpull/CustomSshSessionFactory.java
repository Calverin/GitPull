package com.calverin.gitpull;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RemoteSession;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CustomSshSessionFactory extends SshSessionFactory {
    // Path to the SSH private key
    private static final String PRIVATE_KEY_PATH = ".ssh/id_ecdsa";

    @Override
    public RemoteSession getSession(URIish uri, CredentialsProvider credentialsProvider, FS fs, int tms)
            throws TransportException {
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(PRIVATE_KEY_PATH);
            Session session = jsch.getSession(uri.getUser(), uri.getHost(), uri.getPort() > 0 ? uri.getPort() : 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(tms);
            return new CustomRemoteSession(session);
        } catch (JSchException e) {
            throw new TransportException(uri, "Failed to create SSH session: " + e.getMessage(), e);
        }
    }

    private static class CustomRemoteSession implements RemoteSession {
        private final Session session;

        CustomRemoteSession(Session session) {
            this.session = session;
        }

        @Override
        public Process exec(String command, int timeout) throws IOException {
            try {
                com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) session.openChannel("exec");
                channel.setCommand(command);
                channel.connect(timeout);
                return new CustomRemoteProcess(channel);
            } catch (JSchException e) {
                throw new IOException("Failed to execute command over SSH: " + e.getMessage(), e);
            }
        }

        @Override
        public void disconnect() {
            session.disconnect();
        }
    }

    private static class CustomRemoteProcess extends Process {
        private final com.jcraft.jsch.ChannelExec channel;

        CustomRemoteProcess(com.jcraft.jsch.ChannelExec channel) {
            this.channel = channel;
        }

        @Override
        public OutputStream getOutputStream() {
            try {
                return channel.getOutputStream();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public InputStream getInputStream() {
            try {
                return channel.getInputStream();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public InputStream getErrorStream() {
            try {
                return channel.getErrStream();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public int waitFor() throws InterruptedException {
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }
            return channel.getExitStatus();
        }

        @Override
        public int exitValue() {
            return channel.getExitStatus();
        }

        @Override
        public void destroy() {
            channel.disconnect();
        }
    }

    @Override
    public String getType() {
        return "custom";
    }
}
