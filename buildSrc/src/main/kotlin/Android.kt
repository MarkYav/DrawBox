object Android {
    val compileSdk = 34
    val minSdk = 24
    val targetSdk = 34
    val versionCode = Library.version
        .split(".")
        .map { it.toInt() }
        .reduce { acc, i -> acc * 10 + i }
    val applicationId = Library.group + "android"
}