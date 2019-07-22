buildPlugin(configurations: [
    // Jenkins recommends >2.59 as the minimum version to support (released 2017-06-21);
    // build with the first LTS after that. That version does not work on JDK 11 (test failures in web server).
    [ platform: "linux", "jdk": "8", jenkins: "2.60.1" ],

    // Newer Jenkins versions run fine with both JDK 8 and 11
    [ platform: "linux", "jdk": "8", jenkins: null ],
    [ platform: "linux", "jdk": "11", jenkins: null ]
])
