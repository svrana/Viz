# Viz - Video downloader App for Android  ![Build Status][1]

![Viz logo][2]

## About

Viz provides a rudimentary browser for video selection, a video gallery with
video playback, download resume support and more.

## License

*  [GPLv3](http://www.gnu.org/licenses/gpl-3.0-standalone.html)

## Building

The build requires [Gradle](http://gradle.org/downloads) and the
[Android SDK](https://developer.android.com/sdk/index.html). Viz uses
the [sdk-manager-plugin](http://github.com/JakeWharton/sdk-manager-plugin)
to manage the specific version of the Android SDK you will need along with other
Android dependencies for you; if you do not have the required
Android SDK and support libraries installed, they will be downloaded the
first time you run the build. The `android' tool provided by the Android SDK
must be in your PATH.

After installing gradle, downloading the Android SDK, and placing its tools/
directory in your path

* Run `./gradlew assembleDebug`

The resulting .apk files  will be located in the viz/build/outputs directory.
You might find that your device doesn't let you install your build if you
already have the (now defunct) version of Viz from Google Play installed.
This is standard Android security as it it won't let you directly replace an
app that's been signed with a different key.  Manually uninstall Viz from
your device and you will then be able to install your own built version.

Optionally, building from within Android Studio is also possible.

## Contributing

Please fork this repository and contribute back using
[pull requests](https://github.com/github/android/pulls).

Any contributions, large or small, major features, bug fixes, additional
language translations, unit/integration tests are welcomed and appreciated.

[1]: https://api.travis-ci.org/svrana/Viz.svg?branch=master
[2]: http://hosted.vranix.com/viz-remaining.png
