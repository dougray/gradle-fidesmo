Fidesmo gradle plugin
=====================

A plugin for building java card applications suitable for usage on the [Fidesmo
card](http://fidesmo.com/). The package also supports local testing in the
[JCardSim](http://jcardsim.org/) simulator and installation to the Fidesmo card using a local card
reader (requires internet connection and fidesmo developer account).

Features
--------

 * compile and convert your java package to an executable load file in the CAP format
 * upload the cardlet to the fidesmo server

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
        aid = withFidesmoPrefix('0x01')
        sourcePackage = 'de.spline.uves.ndef'
        applets = [ (withFidesmoPrefix('0x01:0x01')): 'Ndef' ]
        version = '1.0'
    }

Before you can interact with the fidesmo servers, you need to add your appId and appKey to you
gradle.properties. If you don't have created an application yet, you can do so on the [developer
portal](https://developer.fidesmo.com/).

    echo 'fidesmoAppId: yourAppID' >> $HOME/.gradle/gradle.properties
    echo 'fidesmoAppKey: yourAppKey' >> $HOME/.gradle/gradle.properties

To build and upload your executable load file run:

   ./gradlew uploadExecutableLoadFile
