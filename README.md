# iCure Kmehr Module

This project contains all the logic and endpoints related to the belgian eHealth system.  
It contains 3 modules:
- **sam**
- **kmehr**
- **standalone**

The sam and kmehr modules can be run as they are through the standalone module, that requires a connection to the kraken
cloud, or they can be integrated in kraken-lite.

## How to clone this repository
This repository depends on a git submodule, `kraken-common`, so it is important to correctly initialize it immediately after cloning, to avoid compilation errors.  
To do so, launch the following commands:

```
git clone git@github.com:icure/kmehr-module.git
cd kmehr-module
git submodule init
git submodule update
```

After that, if the operation completes successfully, the repository and all its submodules will be correctly initialized. 