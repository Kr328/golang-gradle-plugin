plugins {
    java
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.github.kr328.gradle.golang"
version = "3.0.0"

dependencies {
    val agp = "8.0.1"
    val lombok = "1.18.26"

    annotationProcessor("org.projectlombok:lombok:$lombok")

    compileOnly("com.android.tools.build:gradle:$agp")
    compileOnly("org.projectlombok:lombok:$lombok")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
}

gradlePlugin {
    plugins {
        create("golang") {
            id = project.group.toString()
            displayName = "Golang Gradle"
            description = "A gradle plugin for go module building."
            implementationClass = "$id.GoPlugin"
        }
    }
}

publishing {
    publications {
        withType(MavenPublication::class) {
            version = project.version.toString()
            group = project.group.toString()

            pom {
                name.set("Golang Gradle")
                description.set("A gradle plugin for go module building.")
                url.set("https://github.com/Kr328/golang-gradle-plugin")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/Kr328/golang-gradle-plugin/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        name.set("Kr328")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/Kr328/golang-gradle-plugin.git")
                    url.set("https://github.com/Kr328/golang-gradle-plugin")
                }
            }
        }
    }
    repositories {
        mavenLocal()
        maven {
            name = "kr328app"
            url = uri("https://maven.kr328.app/releases")
            credentials(PasswordCredentials::class.java)
        }
    }
}
