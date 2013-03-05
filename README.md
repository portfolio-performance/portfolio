# About

A simple tool to calculate the overall performance of an investment portfolio.

See http://buchen.github.com/portfolio/ for more details.

## Building with Maven

### Install 3rd party libraries into your local Maven repository:

[SWTChart](http://www.swtchart.org/)

```
mvn install:install-file \
    -Dfile=org.swtchart_0.8.0.v20120301.jar \
    -DgroupId=org.swtchart \
    -DartifactId=org.swtchart \
    -Dversion=0.8.0 \
    -Dpackaging=jar \
    -DgeneratePom=true
```

[TreeMapLib](http://code.google.com/p/treemaplib/)

```
mvn install:install-file \
    -Dfile=tm_core_0.0.4.jar \
    -DgroupId=de.engehausen \
    -DartifactId=de.engehausen.treemap \
    -Dversion=0.0.4 \
    -Dpackaging=jar \
    -DgeneratePom=true
```

```
mvn install:install-file \
    -Dfile=tm_swt_0.0.5.jar \
    -DgroupId=de.engehausen \
    -DartifactId=de.engehausen.treemap.swt \
    -Dversion=0.0.5 \
    -Dpackaging=jar \
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
mvn clean install
```


Create a new version with Tycho's version command:

```
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion=X.Y.Z-SNAPSHOT
```
(and, as Tycho skips it, remember to edit the name.abuchen.portfolio.product file manually)


## Developing with Eclipse

### Setup

To develop, use [Eclipse 3.7.2 or later](http://eclipse.org/downloads/) plus the Plug-in Development Tools (PDT).

Clone the git repository.

Import projects by
* selecting "Import Projects..." on the Git repository in the Git perspective
* choosing "File" > "Import..." > "Existing Projects into Workspace" from the menu 

After importing the Portfolio Performance projects in Eclipse, they will not compile due to missing dependencies: the target platform is missing.

### Generate Target Platform

Run Maven *once* with the following parameter:
```
mvn clean install -Dgenerate-target-platform=true
```

### Set Target Platform

* Open the portfolio-app project
* Open the ide-target-platform/portfolio-ide.target file (this may take a while as it requires Internet access)
* In the resulting editor, click on the Set as Target Platform link at the top right (this may also take a while)

### Run Program

Right-click portfolio-app/PortfolioPerformance.launch and choose "Run As...".

To run the unit tests, use name.abuchen.portfolio.tests/name.abuchen.portfolio.tests.launch.

## License
 
Eclipse Public License
http://www.eclipse.org/legal/epl-v10.html
 