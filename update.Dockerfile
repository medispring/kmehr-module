FROM alpine:latest
ARG version
ARG gitUser
ARG gitPwd
ENV GH_TOKEN=$gitPwd

RUN apk add --no-cache \
    curl \
    git \
    less \
    openssh \
    bash \
    coreutils \
    libc6-compat \
    jq

RUN curl -fsSL https://github.com/cli/cli/releases/download/v2.53.0/gh_2.53.0_linux_amd64.tar.gz | tar -xzv && \
    mv gh_2.53.0_linux_amd64/bin/gh /usr/local/bin/gh && \
    chmod +x /usr/local/bin/gh && \
    rm -rf gh_2.53.0_linux_amd64

RUN gh --version

# Configure Git to use a credential helper
RUN git config --global credential.helper cache

# Set environment variable for the token
ENV GIT_ASKPASS=/etc/git-askpass.sh

# Create a script that supplies the token
RUN echo '#!/bin/sh' > /etc/git-askpass.sh && \
    echo 'case "$1" in' >> /etc/git-askpass.sh && \
    echo 'Username*) echo $gitUser ;;' >> /etc/git-askpass.sh && \
    echo 'Password*) echo $gitPwd ;;' >> /etc/git-askpass.sh && \
    echo 'esac' >> /etc/git-askpass.sh && \
    chmod +x /etc/git-askpass.sh

RUN git clone https://github.com/icure/kraken-lite.git --depth=1 --recurse-submodules && \
    cd kraken-lite && \
    git config --global user.email "dev@icure.com" && \
    git config --global user.name "iCure dev" && \
    branch_name="ci/bump-common-and-kmehr-${version}-$(date +%Y%m%d%H%M)" && \
    git checkout -b $branch_name && \
    cd kraken-common && git checkout main && git pull && cd .. && \
    sed -i 's/kmehr\s*=\s*".*"/kmehr = "'"$version"'"/' libs.versions.toml && \
    git add . && \
    git commit -m "Bumped kraken-common and kmehr module" && \
    git push --set-upstream origin $branch_name && \
    gh pr create --title "Bumped kmehr module to $version and kraken-common" --body "Bumped kmehr module to $version and kraken-common" --base main --head $branch_name