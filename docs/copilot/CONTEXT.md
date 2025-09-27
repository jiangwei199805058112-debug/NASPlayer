# NASPlayer Build Context
- IDE：Android Studio （版本未知，请根据实际环境填写）
- Gradle： （版本未知） / Android Gradle Plugin： 8.2.2
- Kotlin： 1.9.22
- minSdk / targetSdk： 26 / 34
- 主要依赖：jcifs-ng 2.1.10、smbj 0.11.5、ExoPlayer (media3 1.3.1)、Hilt 2.48、Room 2.6.1、Compose 2024.02.00、Coroutines 1.8.0、Coil 2.6.0、Timber 5.0.1 等
- 复现命令：
  ```bash
  ./gradlew clean assembleDebug --console=plain --stacktrace --info