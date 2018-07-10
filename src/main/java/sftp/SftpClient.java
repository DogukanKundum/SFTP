package tr.com.argela.sebu.common.utils.sftp;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Properties;

public class SftpClient {

    private final static Logger logger = LoggerFactory.getLogger(SftpClient.class);

    private String server;
    private int    port;
    private String login;
    private String password;

    private JSch        jsch    = null;
    private Session     session = null;
    private Channel     channel = null;
    private ChannelSftp c       = null;

    public SftpClient(String server, int port, String login, String password) {
        this.server = server;
        this.port = port;
        this.login = login;
        this.password = password;
    }

    /**
     * Connects to the server and does some commands.
     */
    public void connect() {
        try {
            logger.debug("Initializing jsch");
            jsch = new JSch();
            session = jsch.getSession(login, server, port);

            // Java 6 version
            session.setPassword(password.getBytes(Charset.forName("ISO-8859-1")));

            // Java 5 version
            // session.setPassword(password.getBytes("ISO-8859-1"));

            logger.debug("Jsch set to StrictHostKeyChecking=no");
            Properties config = new java.util.Properties();
            config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
            //            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            logger.info("Connecting to " + server + ":" + port);
            session.connect();
            logger.info("Connected !");

            // Initializing a channel
            logger.debug("Opening a channel ...");
            channel = session.openChannel("sftp");
            channel.connect();
            c = (ChannelSftp) channel;
            logger.debug("Channel sftp opened");

        } catch (JSchException e) {
            logger.error("", e);
        }
    }

    /**
     * Uploads a file to the sftp server
     *
     * @param sourceFile      String path to sourceFile
     * @param destinationFile String path on the remote server
     * @throws InfinItException if connection and channel are not available or if an error occurs during upload.
     */
    public void uploadFile(String sourceFile, String destinationFile) throws InfinItException {
        if (c == null || session == null || !session.isConnected() || !c.isConnected()) {
            throw new InfinItException("Connection to server is closed. Open it first.");
        }

        try {
            logger.debug("Uploading file to server");
            c.put(sourceFile, destinationFile);
            logger.info("Upload successfull.");
        } catch (SftpException e) {
            throw new InfinItException(e);
        }
    }

    /**
     * Retrieves a file from the sftp server
     *
     * @param destinationFile String path to the remote file on the server
     * @param sourceFile      String path on the local fileSystem
     * @throws InfinItException if connection and channel are not available or if an error occurs during download.
     */
    public void retrieveFile(String sourceFile, String destinationFile) throws InfinItException {
        if (c == null || session == null || !session.isConnected() || !c.isConnected()) {
            throw new InfinItException("Connection to server is closed. Open it first.");
        }

        try {
            logger.debug("Downloading file to server");
            c.get(sourceFile, destinationFile);
            logger.info("Download successfull.");
        } catch (SftpException e) {
            throw new InfinItException(e.getMessage(), e);
        }
    }

    public void retrieveFiles(final String remoteFile, final File localFile, boolean removeRemoteFiles)
            throws IOException, SftpException, InfinItException {
        String pwd = remoteFile;
        if (remoteFile.lastIndexOf('/') != -1) {
            if (remoteFile.length() > 1) {
                pwd = remoteFile.substring(0, remoteFile.lastIndexOf('/'));
            }
        }
        System.out.println("<SftpClient> remote folder=" + pwd);
        c.cd(pwd);
        System.out.println("<SftpClient> Changed Directory to remote folder=" + pwd);
        if (!localFile.exists()) {
            System.out.println("<SftpClient> local File does not exist. Folders will be created. LocalFile=" + localFile);
            localFile.mkdirs();
        }

        System.out.println("<SftpClient> The files will be listed in remote folder=" + pwd);
        @SuppressWarnings("unchecked") final List<ChannelSftp.LsEntry> files = c.ls(remoteFile);
        System.out.println("<SftpClient> file Count in the remote folder is " + files.size());
        for (ChannelSftp.LsEntry le : files) {
            final String name = le.getFilename();
            if (le.getAttrs().isDir()) {
                if (".".equals(name) || "..".equals(name)) {
                    continue;
                }
                //                retrieveFiles(c.pwd() + "/" + name + "/",
                //                       new File(localFile, le.getFilename()), true);
            } else {
                try {
                    String remoteFilePath = c.pwd() + "/" + name;
                    String localFilePath = localFile.getAbsolutePath() + "/" + le.getFilename();
                    retrieveFile(remoteFilePath, localFilePath);
                    if (removeRemoteFiles) {
                        c.rm(c.pwd() + "/" + name);
                        System.out.println("<SftpClient> remote file is deleted");
                    }
                    logger.info("<SftpClient> Remote File is retrieved to local file and removed.  Remote : {}, Local : {}", remoteFilePath, localFilePath);
                    System.out.println("<SftpClient> Remote File is retrieved to local file and removed.  Remote : " + remoteFilePath + ", Local :" + localFilePath);
                } catch (InfinItException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    logger.error("An exception occured while retrieving file.", e);
                    throw e;
                }
            }
        }
        //        c.cd("..");
    }

    /**
     * Remove a file from the sftp server
     *
     * @param sourceFile
     * @throws InfinItException if connection and channel are not available or if an error occurs during download.
     */
    public void removeFile(String sourceFile) throws InfinItException {
        if (c == null || session == null || !session.isConnected() || !c.isConnected()) {
            throw new InfinItException("Connection to server is closed. Open it first.");
        }

        try {
            logger.debug("Downloading file to server");
            c.rm(sourceFile);
            logger.info("Download successfull.");
        } catch (SftpException e) {
            throw new InfinItException(e.getMessage(), e);
        }
    }

    /**
     * Remove a file from the sftp server
     *
     * @param sourceFile
     * @throws InfinItException if connection and channel are not available or if an error occurs during download.
     */
    public void changeDirectory(String folder) throws InfinItException {
        if (c == null || session == null || !session.isConnected() || !c.isConnected()) {
            throw new InfinItException("Connection to server is closed. Open it first.");
        }

        try {
            logger.debug("Changing the directory");
            c.cd(folder);
            logger.info("Change directory successfull.");
        } catch (SftpException e) {
            throw new InfinItException(e.getMessage(), e);
        }
    }

    public void disconnect() {
        if (c != null) {
            logger.debug("Disconnecting sftp channel");
            c.disconnect();
        }
        if (channel != null) {
            logger.debug("Disconnecting channel");
            channel.disconnect();
        }
        if (session != null) {
            logger.debug("Disconnecting session");
            session.disconnect();
        }
    }

    public static void main(String[] args) {
        SftpClient client = new SftpClient();
        //        client.setServer("192.168.20.63");
        //        client.setPort(22);
        //        client.setLogin("RLM");
        //        client.setPassword("12345Wq");

        client.setServer("10.139.14.9");
        client.setPort(22);
        client.setLogin("pcrf");
        client.setPassword("L95sbCjZAaDRCGaR");

        client.connect();

        try {
            //client.uploadFile("src/main/resources/upload.txt", "/lims/RLM/sarpbatchimporter/importfolders/sftpfolder/uploaded.txt");

            //client.retrieveFile("/lims/RLM/sarpbatchimporter/importfolders/sftpfolder/uploaded.txt", "target/downloaded.txt");
            //String remoteFile = "/lims/RLM/sarpbatchimporter/importfolders/sftpfolder/";
            String remoteFile = "/pcrf/location/";
            File localFile = new File("C:/ARGELA/WS/SARP/SARP_Develop/location_manager/sarp_batch_importer/src/main/resources/sarpbatchimporter/processed");
            client.retrieveFiles(remoteFile, localFile, true);
        } catch (InfinItException | IOException | SftpException e) {
            logger.error("", e);
            e.printStackTrace();
        } finally {
            client.disconnect();
        }
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "SftpClient [server=" + server + ", port=" + port + ", login=" + login + ", password=" + password + "]";
    }

    public SftpClient() {
    }
}
