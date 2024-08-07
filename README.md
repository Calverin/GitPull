# GitPull: A self-explanatory Paper plugin for Minecraft 1.21 by Calverin
## Setup
1. Download the plugin from the GitHub releases page.
2. Put the plugin in your server's plugins folder.
3. Create a `gitpull.config` file in your server's root folder, see below for an example. Fill it in with your repository's information.
4. Create an ssh file called `id_ecdsa`.
    - Use `ssh-keygen -t ecdsa -b 521 -m PEM -C "<your-email>"` to generate one.
    - Run `chmod 600 id_ecdsa` to fix the file permissions.
5. Put the `id_ecdsa` file in the `.ssh` folder in your server's root folder.
6. Run `cat id_ecdsa.pub` and copy the output.
    1. Go to your repository's settings page on GitHub.
    2. Click on "Deploy Keys" in the sidebar.
    3. Click on "Add Deploy Key".
    4. Paste the output from the `cat` command into the key field.
    5. Give it a name and click "Add Key".
7. Start your server! Check the console for any errors.

## Syntax
- `/gitpull` - Pulls the latest changes from the configured default branch.
- `/gitpull [branch]` - Pulls the latest changes from the specified branch.

---
### GitPull Config Example File
```properties
# GitPull Configuration
# world: The name of the world folder
gitpull.world=world
# ssh: The SSH URL of the repository
gitpull.ssh=git@github.com:<Username>/<Repo>.git
# branch: The default branch to pull from
gitpull.branch=main
```
