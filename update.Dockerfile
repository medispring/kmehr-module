FROM alpine:latest
ARG version
ARG gitUser
ARG gitPwd

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
RUN echo $gitPwd | gh auth login --with-token
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
    git remote set-url origin https://${gitUser}:${gitPwd}@github.com/icure/kraken-lite.git && \
    git push --set-upstream origin $branch_name && \
    gh pr create --title "Bumped kmehr module to $version and kraken-common" --body "Bumped kmehr module to $version and kraken-common" --base main --head $branch_name