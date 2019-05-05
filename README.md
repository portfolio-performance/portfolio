# About

A simple tool to calculate the overall performance of an investment portfolio.

See http://www.portfolio-performance.info for more details.

[![Build Status](https://travis-ci.org/buchen/portfolio.svg?branch=master)](https://travis-ci.org/buchen/portfolio) [![Latest Release](https://img.shields.io/github/release/buchen/portfolio.svg)](https://github.com/buchen/portfolio/releases/latest) [![License](https://img.shields.io/github/license/buchen/portfolio.svg)](https://github.com/buchen/portfolio/blob/master/LICENSE)

[![LOC](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=ncloc)](https://sonarcloud.io/dashboard?id=name.abuchen.portfolio%3Aportfolio-app) [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=bugs)](https://sonarcloud.io/project/issues?id=name.abuchen.portfolio%3Aportfolio-app&resolved=false&types=BUG) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=vulnerabilities)](https://sonarcloud.io/project/issues?id=name.abuchen.portfolio%3Aportfolio-app&resolved=false&types=VULNERABILITY) [![Code Coverage](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=coverage)](https://sonarcloud.io/component_measures?id=name.abuchen.portfolio%3Aportfolio-app&metric=Coverage)

## Prerequisites

* [Java 8](http://www.java.com)
* [Maven](http://maven.apache.org)
* [Eclipse](http://www.eclipse.org)

## Building with Maven

### Configure

*Mac OS X / Linux*
```
export MAVEN_OPTS="-Xmx1g"
```

*Windows*
```
set MAVEN_OPTS="-Xmx1g"
```

### Build

Run Maven 3.x.x in the 'portfolio-app' directory:

```
mvn clean verify -Dtycho.disableP2Mirrors
```

## Developing with Eclipse

### Eclipse Setup

To develop, use the [Eclipse IDE](http://eclipse.org/downloads/) **Photon** (or newer up to 2018-09) release plus **Plug-in Development Environment (PDE)**. PDE homepage is http://www.eclipse.org/pde/ and it can be installed via Help/Install new Software. The link for the update site is mentioned in the docs.

Optionally, install via the Eclipse Marketplace
* infinitest
* ResourceBundle Editor
* SonarLint
* Eclipse Checkstyle Plug-in

### Setup

Clone the git repository.

Import projects by
* selecting "Import Projects..." on the Git repository in the Git perspective
* choosing "File" > "Import..." > "Existing Projects into Workspace" from the menu

After importing the Portfolio Performance projects in Eclipse, they will not compile due to missing dependencies: the target platform is missing.

### Generate Target Platform

Run Maven *once* in the *'portfolio-app' directory* with the following parameter:
```
mvn clean install -Dgenerate-target-platform=true -Dtycho.disableP2Mirrors
```

### Set Target Platform

* Open the portfolio-app project
* Press F5 to refresh the view, otherwise you might not find the ide-target-platform
* Open the ide-target-platform/portfolio-ide.target file with the Target Editor (this may take a while as it requires Internet access). If you just get an XML file, use right click and chose Open With
* In the resulting editor, click on the "Set as Active Target Platform" link at the top right (this may also take a while)

### Run Program

Run the application and the tests with the launch configurations stored in ~/portfolio-app/eclipse folder (right-click "Run As").

:warning: The launch configuration needs an update when bundles are added and removed or the OS platform changes. If the program does not start, try selecting "Add required plug-ins" in the launch configuration dialog.

## License

Eclipse Public License
http://www.eclipse.org/legal/epl-v10.html
