buildPlugin(configurations: [
    // Newer Jenkins versions run fine with both JDK 8 and 11
    [ platform: "linux", "jdk": "8", jenkins: null ], // This is the first entry without a specific jenkins version - its artifacts are saved
    [ platform: "linux", "jdk": "11", jenkins: null ],

    // Jenkins recommends >2.59 as the minimum version to support; build with the first LTS after that.
    // 2.60.1 was released on 2017-06-21 and does not work on JDK 11 (test failures in web server).
    [ platform: "linux", "jdk": "8", jenkins: "2.60.1" ]
])
