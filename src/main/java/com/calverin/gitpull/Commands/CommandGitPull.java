package com.calverin.gitpull.Commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.SshTransport;

import com.calverin.gitpull.CustomSshSessionFactory;

import com.calverin.gitpull.GitPull;

public class CommandGitPull implements CommandExecutor, TabCompleter {

    // This method is called, when somebody uses our command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean success = false;
        // Path to the local repository
        String localRepoPath = GitPull.worldName + "/.git";
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = null;
        Git git = null;
        try {
            // Open the existing repository
            repository = builder.setGitDir(new File(localRepoPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            // Create a Git object to work with the repository
            git = new Git(repository);

            // If they're trying to pull from a different branch
            if (args.length > 0 && !GitPull.defaultBranch.equals(args[0])) {
                boolean changedBranch = GitPull.cloneAndMove(args[0]);
                if (!changedBranch) {
                    sender.sendMessage(
                            "§cCould not switch branch, pulling from " + GitPull.currentBranch + " instead.");
                }
            } else if (!GitPull.defaultBranch.equals(GitPull.currentBranch)) {
                GitPull.cloneAndMove(GitPull.defaultBranch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Pull the latest changes from the remote repository with SSH
            PullResult result = git.pull()
                    .setTransportConfigCallback(transport -> {
                        if (transport instanceof SshTransport) {
                            ((SshTransport) transport).setSshSessionFactory(new CustomSshSessionFactory());
                        }
                    })
                    .call();

            // Check the result of the pull operation
            success = result.isSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Close the repository
        repository.close();
        git.close();

        Bukkit.getServer().reloadData();

        if (success) {
            sender.sendMessage("§aPull from " + GitPull.currentBranch + " successful.");
        } else {
            sender.sendMessage("§cPull from " + GitPull.currentBranch + " failed.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) {
            return new ArrayList<String>();
        }
        // List of branches to complete
        String localRepoPath = GitPull.worldName + "/.git";
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        Repository repository = null;
        Git git = null;
        try {
            // Open the existing repository
            repository = builder.setGitDir(new File(localRepoPath))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            // Create a Git object to work with the repository
            git = new Git(repository);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<String> branches = new ArrayList<String>();
        try {
            // Get the list of branches
            git.branchList().setListMode(ListMode.ALL).call().forEach(ref -> {
                branches.add(ref.getName().replace("refs/heads/", "").replace("refs/remotes/origin/", ""));
            });
        } catch (Exception e) {
            e.printStackTrace();
            branches.add(GitPull.currentBranch);
        }

        // Close the repository
        repository.close();
        git.close();

        return branches;
    }
}