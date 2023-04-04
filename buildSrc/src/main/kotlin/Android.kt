object Android {
    val compileSdk = 33
    val minSdk = 24
    val targetSdk = 33
    val versionCode = Library.version
        .split(".")
        .map { it.toInt() }
        .reduce { acc, i -> acc * 10 + i }
    val applicationId = Library.group + "android"
}