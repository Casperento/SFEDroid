## Static Features Extractor for Android (SFEDroid)

SFEDroid is a tool for analyzing APKs statically to build up a dataset. It does so by managing
[FlowDroid](https://github.com/secure-software-engineering/FlowDroid) data flow analysis capabilities. It also uses android permission mappings provided by [Axplorer](https://github.com/reddr/axplorer).

The current features extractable are:

- Android Permissions;
- Target SDK Version;
- Min SDK Version;
- Methods mapped by permissions, which are reachable in the lifecycle of the app;
- Sink methods that leak information in the Taint Analysis;

---

## Cloning

To clone this project you need to resolve its dependencies. So the following command should suffice:

```git clone --recursive https://github.com/Casperento/SFEDroid.git```

---

## Build

The project is managed with maven. The only public accessible FlowDroid version on maven is 2.10.0, but the project uses 
v2.13.0. This is the current version of their **_develop_** branch, and it must be compiled locally,
so maven can locate it in your local repository.

1. Build and Install FlowDroid locally;

2. Build SFEDroid;

To build, one needs to run the following command in its platform:

> mvn clean install

---

## Usage

The program implements the following CLI:

_TODO..._