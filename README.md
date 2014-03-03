MessBuster
==========
MASTER: [![Build Status](https://travis-ci.org/mkubala/messBuster.png?branch=master)](https://travis-ci.org/mkubala/messBuster)
DEV: [![Build Status](https://travis-ci.org/mkubala/messBuster.png?branch=dev)](https://travis-ci.org/mkubala/messBuster)

Documentation tool for Qcadoo Framework.

Powered by Scala, Json4s, AngularJS, Typesafe's Config & Logging, Sbt

### Build
To build single JAR run:
```
sbt assembly
```
Jar will be generated at following path:
target/scala-SCALA_VERSION/messBuster-assembly-VERSION.jar

### Run
```
java -Dconfig.file=/absolute/path/to/messBuster.conf -jar messBuster-assembly-VERSION.jar
```
Note that path to the configuration file have to be an absolute path.

You can also use remote configuration, for example:
```
java -Dconfig.url=http://my.org/messBuster/master.config?user=maku -jar messBuster-assembly-VERSION.jar
```

Example configurations:
```
messBuster {
  outputDir = ~/qcadoo/messBusterOut
  dirsToScan = [
    /Users/marcinkubala/qcadoo/qcadoo
    /Users/marcinkubala/qcadoo/mes
  ]
}
```
```
messBuster.outputDir = ~/qcadoo/messBusterOut
messBuster.dirsToScan = [
  /Users/marcinkubala/qcadoo/qcadoo
  /Users/marcinkubala/qcadoo/mes
]
```
See also https://github.com/mkubala/messBuster/blob/dev/src/main/resources/application.conf - this configuration will be used if you don't specify config path or url

#### More about configuration's syntax
* https://github.com/typesafehub/config/blob/master/HOCON.md
* http://marcinkubala.wordpress.com/2013/10/09/typesace-config-hocon/
