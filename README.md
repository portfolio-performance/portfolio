# About

A simple tool to calculate the overall performance of an investment portfolio.

See http://buchen.github.com/portfolio/ for more details.

## Building with Maven

### Install 3rd party libraries into your local Maven repository:

[SWTChart](http://www.swtchart.org/)

```
mvn install:install-file
    -Dfile=org.swtchart_0.8.0.v20120301.jar
    -DgroupId=org.swtchart
    -DartifactId=org.swtchart
    -Dversion=0.8.0
    -Dpackaging=jar
    -DgeneratePom=true
```

[TreeMapLib](http://code.google.com/p/treemaplib/)

```
mvn install:install-file
    -Dfile=tm_core_0.0.4.jar
    -DgroupId=de.engehausen
    -DartifactId=de.engehausen.treemap
    -Dversion=0.0.4
    -Dpackaging=jar
    -DgeneratePom=true
```

```
mvn install:install-file
    -Dfile=tm_swt_0.0.5.jar
    -DgroupId=de.engehausen
    -DartifactId=de.engehausen.treemap.swt
    -Dversion=0.0.5
    -Dpackaging=jar
    -DgeneratePom=true
```

### Configure Maven

Give Maven some memory (and, as Maven has troubles with German locale on Mac, switch to English):

```
export MAVEN_OPTS="-Xmx1g -XX:MaxPermSize=500m"
export LANG=en_US.UTF-8
```

### Build

Run Maven 3.0.x in the 'portfolio-app' directory:

```
mvn clean verify
```


Create a new version with Tycho's version command:

```
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=X.Y.Z-SNAPSHOT
```
(and, as Tycho skips it, remember to edit the name.abuchen.portfolio.product file manually)


## Developing with Eclipse

Download [Eclipse 3.7.2](http://eclipse.org/downloads/) including the RCP Tools.

Clone the git repository and import the projects.

Setup a target platform (Preferences -> Plug-in Development -> Target Platform) including

* an Eclipse 3.7.2 installation
* a directory containing the following 3rd party libraries (com.springsource.* and org.swtchart.* bundle)
* (optionally) add the Eclipse Babel language pack

## License
 
Eclipse Public License
http://www.eclipse.org/legal/epl-v10.html
 