# PR: Allow negative values (on CSV import, and in UI)

This is the _permanent_ page for a simple feature request that did not make it into the portfolio main branch. With this fork, you can use negative values at most places!

**ATTENTION:<br/>
After switching this on and actually using transactions with negative values, there is _NO WAY BACK_!**

Here you can read more about it:

* [current PR](https://github.com/portfolio-performance/portfolio/pull/5445) (however not considered)
  + This also explains how to use the [packages](https://github.com/users/aanno/packages?repo_name=portfolio) that I build here if you are _not_ a developer
* [my 1st PR try](https://github.com/portfolio-performance/portfolio/pull/5376) (closed)
* [forum discussion about my problem at hand](https://forum.portfolio-performance.info/t/verbuchung-von-anleihen/1537/81)

There have been several discussions why PP does not allow for negative values. Here are references to several of them:

* https://github.com/portfolio-performance/portfolio/issues/3450
* https://github.com/portfolio-performance/portfolio/issues/3616
* https://github.com/portfolio-performance/portfolio/pull/3617
* https://forum.portfolio-performance.info/t/verbuchung-von-anleihen/1537/81
* [Allow dividends to be negative](https://forum.portfolio-performance.info/t/allow-dividends-to-be-negative/6501/3)
* [Eintragen von negativen Dividenden](https://forum.portfolio-performance.info/t/eintragen-von-negativen-dividenden/1394/26)
* [Import von negativen Dividenden](https://forum.portfolio-performance.info/t/import-von-negativen-dividenden-reversal-aus-ib-flex-query/32047/5)
* [Negative Dividenden bei Shortpositionen](https://forum.portfolio-performance.info/t/negative-dividenden-bei-shortposition/16626/6)

## Build as developer (quick)

```sh
cd portfolio-app
# only if needed
mvn clean
mvn -DskipTests install
# run it, e.g.
../portfolio-product/target/products/name.abuchen.portfolio.product/linux/gtk/x86_64/portfolio/PortfolioPerformance
```

Also see [here](CONTRIBUTING.md#development-setup).

# About

[Portfolio Performance](https://www.portfolio-performance.info): Track and evaluate the performance of your investment portfolio across stocks, cryptocurrencies, and other assets. It adds the option to relaxing the restriction that you must provide positive values for the most input and CSV fields as experiment, i.e. it is configurable in the settings now.

## Status

[![Build Status](https://github.com/portfolio-performance/portfolio/workflows/CI/badge.svg)](https://github.com/portfolio-performance/portfolio/actions?query=workflow%3ACI) [![Latest Release](https://img.shields.io/github/release/buchen/portfolio.svg)](https://github.com/portfolio-performance/portfolio/releases/latest) [![Release Date](https://img.shields.io/github/release-date/buchen/portfolio?color=blue)](https://github.com/portfolio-performance/portfolio/releases/latest) [![License](https://img.shields.io/github/license/buchen/portfolio.svg)](https://github.com/portfolio-performance/portfolio/blob/master/LICENSE)

[![LOC](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=ncloc)](https://sonarcloud.io/dashboard?id=name.abuchen.portfolio%3Aportfolio-app) [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=bugs)](https://sonarcloud.io/project/issues?id=name.abuchen.portfolio%3Aportfolio-app&resolved=false&types=BUG) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=vulnerabilities)](https://sonarcloud.io/project/issues?id=name.abuchen.portfolio%3Aportfolio-app&resolved=false&types=VULNERABILITY) [![Code Coverage](https://sonarcloud.io/api/project_badges/measure?project=name.abuchen.portfolio%3Aportfolio-app&metric=coverage)](https://sonarcloud.io/component_measures?id=name.abuchen.portfolio%3Aportfolio-app&metric=Coverage)


## Links

* [Homepage](https://www.portfolio-performance.info)
* [Downloads](https://github.com/portfolio-performance/portfolio/releases)
* [Forum](https://forum.portfolio-performance.info/)
* [Manual](https://help.portfolio-performance.info/en)


## Contributing Source Code

* [Development setup](CONTRIBUTING.md#development-setup)
* [Project setup](CONTRIBUTING.md#eclipse-ide-setup)
* [Contributing code](CONTRIBUTING.md#contributing-code)
* [Images and Icons](CONTRIBUTING.md#images-and-icons)
* [Translations](CONTRIBUTING.md#translations)
* [Interactive Flex Query Importers](CONTRIBUTING.md#interactive-flex-query-importers)
* [PDF Importers](CONTRIBUTING.md#pdf-importers)
* [Trade Calendars](CONTRIBUTING.md#trade-calendars)


## Public GPG Key

Fingerprint: `E46E 6F8F F02E 4C83 5690 8458 9239 277F 560C 95AC`

* [OpenPGP](https://keys.openpgp.org/search?q=0xe46e6f8ff02e4c83569084589239277f560c95ac)
* [Ubuntu Keyserver](https://keyserver.ubuntu.com/pks/lookup?search=e46e6f8ff02e4c83569084589239277f560c95ac&fingerprint=on&op=index)


## License

Eclipse Public License
https://www.eclipse.org/legal/epl-v10.html
