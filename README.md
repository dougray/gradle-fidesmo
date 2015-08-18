Fidesmo gradle plugin
=====================

[![Build Status](https://travis-ci.org/fidesmo/gradle-fidesmo.svg?branch=master)](https://travis-ci.org/fidesmo/gradle-fidesmo)

A plugin for building java card applications suitable for usage on the [Fidesmo
card](http://fidesmo.com/). The package also supports local testing in the
[JCardSim](http://jcardsim.org/) simulator and installation to the Fidesmo card using a local card
reader (requires internet connection and fidesmo developer account). The gradle plugin relies on the
vanilla gradle java plugin as well as on the [java card
plugin](https://github.com/fidesmo/gradle-javacard).

Features
--------

 * compile/convert java package to executable load file (in **C** onverted **AP** plet format)
 * upload executable load file to the Fidesmo server [^1]
 * install applet to Fidesmo card [^1]
 * delete applet from Fidesmo card [^1]

[^1]: These features interact with the fidesmo server, hence a working internet connection and a
[fidesmo developer account](https://developer.fidesmo.com)(free of charge) is required.

Usage
-----

Include at least the following into the build.gradle of your project

    apply plugin: 'fidesmo'

    buildscript {
        repositories {
            maven { url 'http://releases.marmeladburk.fidesmo.com/' }
        }

        dependencies {
            classpath  'com.fidesmo:gradle-fidesmo:0.1.6'
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

In order to translate Java Classes to Java Card Applets you need to have the `Java card development
kit`. Please follow the instructions for the [gradle-javacard
plugin](https://github.com/fidesmo/gradle-javacard/tree/master#installing-the-java-card-development-kit)
and don't forget the `JC_HOME` environment variable to your shell.

    export JC_HOME='../path/to/sdk/java_card_kit-2_2_2/java_card_kit-2_2_2'

To build and upload your executable load file run:

    ./gradlew uploadExecutableLoadFile

In order to (re)-install you application to a Fidesmo card using you card reader, run the following
command:

    ./gradlew installToLocalCard

This will take the first defined applet and create an instance of on the card with the same aid as
the applet.

Assign a specific AID to the applet instance
--------------------------------------------

When installing an applet to a Fidesmo card, the default behavior of the plugin is to assign an instance AID derived from the applet AID specified as above. If by any reason you want to assign a different AID (for example, because [you want to use your own RID](https://developer.fidesmo.com/javacard)), add the following lines to build.gradle:

    fidesmo {
        instanceAid = 'yourInstanceAid'
    }

Additional features
-------------------

Since the fidesmo plugin is based on the javacard plugin, all its features can be used as well. Here is a short non-complete list:

* Adding additional exp files
* Support testing with JCardsim
* Building without Javacard SDK

To use those features please refer to the [documentation](https://github.com/fidesmo/gradle-javacard/blob/master/Readme.md) of the javacard plugin.
