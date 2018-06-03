package com.martoph.uploadtodebian;

import com.jcraft.jsch.*;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class Main {
    private static ArrayList<String> files = new ArrayList<>();
    private static String address, dir, username, password;
    private static int port;

    public static void main(String[] args) {

        if (args.length < 6) {
            System.out.println(System.getProperty("user.dir"));
            System.out.println("USAGE: java -jar uploadtodebian <address> <port> <username> <password> <remote directory> [files...]");
            System.exit(1);
            return;
        }

        for (int i = 0; i < args.length; i++) {
            if (i < 5) continue;
            files.add(args[i]);
        }

        address = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port (must be integer)");
            System.exit(1);
        }
        username = args[2];
        password = args[3];
        dir = args[4];
        open();
        for (String filePath : files) {

            if (filePath.startsWith("*.")) {
                String extension = filePath.substring(2);
                File folder = new File(System.getProperty("user.dir"));

                Arrays.stream(
                        Objects.requireNonNull(
                                folder.listFiles(
                                        pathname -> FilenameUtils.getExtension(pathname.getName()).equals(extension)
                                )
                        )
                ).forEach(Main::send);
                continue;
            }

            send(new File(filePath));
        }
        close();
    }


    private static Session session;
    private static Channel channel;
    private static ChannelSftp channelSftp;
    private static void open() {
        String SFTPHOST = address;
        int SFTPPORT = port;
        String SFTPUSER = username;
        String SFTPPASS = password;
        String SFTPWORKINGDIR = dir;

        System.out.println("preparing the host information for sftp.");
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
            session.setPassword(SFTPPASS);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            System.out.println("Host connected.");
            channel = session.openChannel("sftp");
            channel.connect();
            System.out.println("sftp channel opened and connected.");
            channelSftp = (ChannelSftp) channel;
            channelSftp.cd(SFTPWORKINGDIR);
        } catch (Exception ex) {
            System.out.println("Exception found while tranfer the response.");
        }
    }

    private static void send(File file) {
        try {
            channelSftp.put(new FileInputStream(file), file.getName());
            System.out.println("File " + file.getName() + " successfully to host.");
        } catch (FileNotFoundException | SftpException e) {
            e.printStackTrace();
        }
    }

    private static void close() {
        channelSftp.exit();
        System.out.println("sftp Channel exited.");
        channel.disconnect();
        System.out.println("Channel disconnected.");
        session.disconnect();
        System.out.println("Host Session disconnected.");
    }
}
