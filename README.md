# Viz - Video downloader App for Android

![Viz logo][1]

## About

Viz provides a rudimentary browser for video selection, a video gallery with
video playback, download resume support and more.

## License

*  [GPLv3](http://www.gnu.org/licenses/gpl-3.0-standalone.html)

## Building

The build requires [Gradle](http://gradle.org/downloads) and the [Android SDK](http://developer.android.com/sdk/index.html) to be installed in your
development environment. In addition you'll need to set the `ANDROID_HOME`
environment variable to the location of your SDK:

```bash
export ANDROID_HOME=/opt/tools/android-sdk
```

The build also requires the Android Support Library, revision 19. Installation
directions for the support library are
[here](http://developer.android.com/tools/support-library/setup.html).

After satisfying those requirements, the build is pretty simple:

* Run `./gradlew assembleDebug`

The resulting .apk files will be located in the viz/build/outputs directory.

You might find that your device doesn't let you install your build if you
already have the (now defunct) version of Viz from Google Play installed.
This is standard Android security as it it won't let you directly replace an
app that's been signed with a different key.  Manually uninstall GitHub from
your device and you will then be able to install your own built version.

Optionally, building from within Android Studio is also possible.

## Contributing

Please fork this repository and contribute back using
[pull requests](https://github.com/github/android/pulls).

Any contributions, large or small, major features, bug fixes, additional
language translations, unit/integration tests are welcomed and appreciated.

[1]: http://vranix.com/viz_video_downloader_logo_21.png
