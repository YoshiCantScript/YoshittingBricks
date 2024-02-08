dependencies {
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
}
// use an integer for version numbers
version = 2


cloudstream {
    // All of these properties are optional, you can safely remove them

    description = "My ass hurts"
    authors = listOf("Cloudburst")

    /**
    * Status int as the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta only
    * */
    status = 1

    tvTypes = listOf("TvSeries")

    requiresResources = true
    language = "it"

    // random cc logo i found
    iconUrl = "https://streamingcommunity.li/img/logo.png"
}

android {
    buildFeatures {
        viewBinding = true
    }
}
