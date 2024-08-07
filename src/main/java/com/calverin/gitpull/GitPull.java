package com.calverin.gitpull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.SshTransport;

import com.calverin.gitpull.Commands.CommandGitPull;

public class GitPull extends JavaPlugin implements Listener {

    public static String worldName;
    public static String gitSSH;
    public static String defaultBranch;

    public static String currentBranch;

    @Override
    public void onEnable() {
        getLogger().info("Using GitPull by Calverin!");
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("gitpull").setExecutor(new CommandGitPull());

        Properties prop = new Properties();
        String fileName = "gitpull.config";
        if (!new File(fileName).exists()) {
            getLogger().info("A gitpull.config file was not found, creating a new one.");
            File file = new File(fileName);
            try {
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                writer.write("# GitPull Configuration\n");
                writer.write("# world: The name of the world folder\n");
                writer.write("gitpull.world=world\n");
                writer.write("# ssh: The SSH URL of the repository\n");
                writer.write("gitpull.ssh=git@github.com:<Username>/<Repo>.git\n");
                writer.write("# branch: The default branch to pull from\n");
                writer.write("gitpull.branch=main\n");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (FileInputStream fis = new FileInputStream(fileName)) {
            prop.load(fis);
            getLogger().info("GitPull configuration loaded: " + prop.toString());
            worldName = prop.getProperty("gitpull.world");
            gitSSH = prop.getProperty("gitpull.ssh");
            defaultBranch = prop.getProperty("gitpull.branch");
            currentBranch = defaultBranch;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // cloneAndMove(currentBranch);
    }

    @Override
    public void onDisable() {
        getLogger().info("No longer using Git Pull by Calverin!");
    }

    public static boolean cloneAndMove(String branch) {
        boolean success = false;

        Git git = null;
        try {
            git = Git.cloneRepository()
                    .setURI(gitSSH)
                    .setDirectory(new File(worldName + "/temp")) // Clone the repository into a temp directory
                    .setBranch(branch)
                    .setTransportConfigCallback(transport -> {
                        if (transport instanceof SshTransport) {
                            ((SshTransport) transport).setSshSessionFactory(new CustomSshSessionFactory());
                        }
                    })
                    .call();
            success = true;
            currentBranch = branch;
        } catch (Exception e) {
            if (git != null) {
                git.close();
            }
            e.printStackTrace();
            return false;
        }
        git.close();

        // Move the files from the temp directory to the main directory
        moveFiles(new File(worldName + "/temp/datapacks"), new File(worldName + "/datapacks"));
        recursiveDelete(new File(worldName + "/temp"));
        return success;
    }

    private static void moveFiles(File source, File destination) {
        if (!source.exists()) {
            return;
        }
        if (destination.exists()) {
            recursiveDelete(destination);
        }
        destination.mkdirs();

        // Need to move the files then delete each one to delete the temp directory
        File[] files = source.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    moveFiles(file, new File(destination, file.getName()));
                    file.delete();
                } else {
                    file.renameTo(new File(destination, file.getName()));
                    file.delete();
                }
            }
        }
        source.delete();
    }

    private static void recursiveDelete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    recursiveDelete(f);
                }
            }
        }
        file.delete();
    }
}
