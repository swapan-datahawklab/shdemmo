#!/bin/bash

# Detect OS type
case "$(uname -s)" in
    Linux*)     OS_TYPE=Linux;;
    Darwin*)    OS_TYPE=Mac;;
    CYGWIN*)    OS_TYPE=Windows;;
    MINGW*)     OS_TYPE=Windows;;
    *)          OS_TYPE=Unknown;;
esac

echo "Configuring Git for $OS_TYPE environment..."

# Configure line endings based on OS
if [ "$OS_TYPE" = "Windows" ]; then
    git config --global core.autocrlf true
else
    git config --global core.autocrlf input
fi

# Configure credential helper based on OS
if [ "$OS_TYPE" = "Windows" ]; then
    git config --global credential.helper manager-core
elif [ "$OS_TYPE" = "Mac" ]; then
    git config --global credential.helper osxkeychain
else
    git config --global credential.helper cache
fi

# Set common Git defaults
git config --global init.defaultBranch main
git config --global pull.rebase false
git config --global core.fileMode false
git config --global core.longpaths true
git config --global core.symlinks true
git config --global core.eol lf

# Configure VS Code as diff tool
git config --global diff.tool vscode
git config --global difftool.vscode.cmd 'code --wait --diff $LOCAL $REMOTE'

echo -e "\nGit configuration completed. Current settings:"
git config --global --list 