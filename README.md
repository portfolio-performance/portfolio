# About

A simple tool to calculate the overall performance of an investment portfolio.

See http://buchen.github.com/portfolio/ for more details.

## Prerequisites

* [Java 7](http://www.java.com)
* [Maven](http://maven.apache.org)
* [Eclipse](http://www.eclipse.org)

## Building with Maven

### Configure

*Mac OS X / Linux*
```
export MAVEN_OPTS="-Xmx1g -XX:MaxPermSize=500m"
```

*Windows*
```
set MAVEN_OPTS="-Xmx1g -XX:MaxPermSize=500m"
```

### Build

Run Maven 3.0.x in the 'portfolio-app' directory:

```
mvn clean install
```

## Developing with Eclipse

### Setup

To develop, use [Eclipse Kepler](http://eclipse.org/downloads/) plus the Plug-in Development Tools (PDT).

Clone the git repository.

Import projects by
* selecting "Import Projects..." on the Git repository in the Git perspective
* choosing "File" > "Import..." > "Existing Projects into Workspace" from the menu 

After importing the Portfolio Performance projects in Eclipse, they will not compile due to missing dependencies: the target platform is missing.

### Generate Target Platform

Run Maven *once* in the *'portfolio-app' directory* with the following parameter:
```
mvn clean install -Dgenerate-target-platform=true
```

### Set Target Platform

* Open the portfolio-app project
* Open the ide-target-platform/portfolio-ide.target file (this may take a while as it requires Internet access)
* In the resulting editor, click on the "Set as Target Platform" link at the top right (this may also take a while)

### Run Program

* Open the portfolio-product project
* Open the name.abuchen.portfolio.product file
* In the resulting editor, click on the "Launch an Eclipse application" link

To run the unit tests
* Right click on the name.abuchen.portfolio.tests project
* Choose "Run As..." -> "JUnit Plug-in Test".

## License
 
Eclipse Public License
http://www.eclipse.org/legal/epl-v10.html
 