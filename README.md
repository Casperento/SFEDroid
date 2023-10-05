## Static Features Extractor for Android (SFEDroid)

SFEDroid is a tool for analyzing APKs statically to build up a dataset. It does so by managing [FlowDroid](https://github.com/secure-software-engineering/FlowDroid) data flow analysis capabilities. It also uses android permission mappings provided by [axplorer](https://github.com/reddr/axplorer).

The current features extractable are (the columns exported follow this order):

- label (1: malware | 0: benign);
- Min SDK Version;
- Target SDK Version;
- Apk file size;
- `classes.dex`'s entropy;
- List of Android Permissions (provided by [axplorer](https://github.com/reddr/axplorer/tree/master/permissions)), set to 1 when it is requested by the app under analysis, otherwise 0;
- Methods mapped by permissions (provided by [axplorer](https://github.com/reddr/axplorer/tree/master/permissions)), set to 1 when it is reachable in the lifecycle of some app's activity, otherwise 0;
- List of sink methods (provided by [FlowDroid](https://github.com/secure-software-engineering/FlowDroid/blob/develop/soot-infoflow-android/SourcesAndSinks.txt)), set to 1 when leaks some information, otherwise 0;

---

## Cloning

To clone this project, you need to resolve its submodules. So the following command should suffice:

```git clone --recursive https://github.com/Casperento/SFEDroid.git```

---

## Build

The project is managed with maven. The only public accessible FlowDroid version on maven is 2.10.0, but the project uses v2.13.0. This is the current version of their **_develop_** branch, and it must be compiled locally, so maven can locate it in your local repository.

1. Build and Install FlowDroid locally;

2. Build SFEDroid;

To build, one needs to run the following command in their platform:

> mvn clean install

---

## Usage

The program implements the following CLI commands:

```
usage: SFEDroid
 -ac,--additional-classpath <arg>   path to add into soot's classpath
                                    (separated by ':' or ';')
 -c,--callgraph-alg <arg>           callgraph algorithm: AUTO, CHA
                                    (default), VTA, RTA, SPARK or GEOM
 -d,--create-dataset                create a new dataset.tsv file and
                                    overwrite existing one in the output
                                    folder
 -e,--export-callgraph              export callgraph as DOT file
 -i,--source-file <arg>             source apk file
 -j,--android-jars <arg>            path to android jars
 -l,--list-file <arg>               a file containing a list of apks to
                                    analyze
 -o,--output-folder <arg>           output folder to save exported files
                                    related to the apk being analyzed
 -p,--permissions-mapping <arg>     permissions' mapping input file
 -r,--define-label <arg>            define apks' labels, 1 for malware 0
                                    for benign
 -t,--timeout <arg>                 set timeout in seconds to abort the
                                    taint analysis
 -v,--verbose                       turn on logs and write it to console
                                    and disk ('/src/main/resources/logs')
```

### Example

To run SFEDroid with Maven, use the following example. It will analyze each sample for 60 seconds, show detailed output, mark all samples from _list.txt_ as malware, and save a _dataset.tsv_ file in the sfedroid_output folder.

```mvn compile exec:java -Dexec.mainClass="edu.ifmg.Main" -Dexec.args="-v -r 1 -t 60 -l 'list.txt' -o 'sfedroid_output'"```
