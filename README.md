Fidesmo gradle plugin
=====================

A plugin for building java card applications suitable for usage on the [Fidesmo
card](http://fidesmo.com/). The package also supports local testing in the
[JCardSim](http://jcardsim.org/) simulator and installation to the Fidesmo card using a local card
reader (requires internet connection and fidesmo developer account). The gradle plugin relies on the
vanilla gradle java plugin as well as on the [java card
plugin](https://github.com/fidesmo/gradle-javacard).

Features
--------

 * compile/convert java package to executable load file (in *C*onverted *AP*plet format)
 * upload executable load file to the Fidesmo server [^connectionRequired]
 * install applet to Fidesmo card [^connectionRequired]
 * delete applet from Fidesmo card [^connectionRequired]

[^connectionRequired]: These features interact with the fidesmo server, hence a working internet
connection and a [fidesmo developer account](https://developer.fidesmo.com)(free of charge) is
required.

Usage
-----

In order to use this plugin you need to get into your local maven repository by executing:

    git clone https://github.com/fidesmo/gradle-fidesmo.git
    gradle install

And include at least the following into the build.gradle of your project

    apply plugin: 'fidesmo'

    buildscript {
        repositories {
            mavenLocal()
        }

        dependencies {
            classpath  'com.fidesmo:gradle-plugin:0.1-SNAPSHOT'
        }
    }

    javacard {
        cap {
            aid = "${fidesmoPrefix}:0x01"
            packageName = 'org.example.javacard.package'
            applet {
                aid = "${fidesmoPrefix}:0x01:0x01"
                className = 'Applet'
            }
            version = '1.0'
        }
    }

Before you can interact with the fidesmo servers, you need to add your appId and appKey to you
gradle.properties. If you don't have created an application yet, you can do so on the [developer
portal](https://developer.fidesmo.com/).

    echo 'fidesmoAppId: yourAppID' >> $HOME/.gradle/gradle.properties
    echo 'fidesmoAppKey: yourAppKey' >> $HOME/.gradle/gradle.properties

To build and upload your executable load file run:

   ./gradlew uploadExecutableLoadFile

In order to (re)-install you application to a Fidesmo card using you card reader, run the following
command:

   ./gradlew installToLocalCard

This will take the first defined applet and create an instance of on the card with the same aid as
the applet.
