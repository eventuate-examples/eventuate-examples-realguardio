import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test

class ComponentTestsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        def copyDockerfile = project.tasks.register("copyDockerfile", Copy) {
            from(project.projectDir)
            include("Dockerfile")
            into(project.layout.buildDirectory.dir("generated/sources/dockerfiles"))
        }

        project.sourceSets {
            componentTest {
                java {
                    compileClasspath += main.output + test.output
                    runtimeClasspath += main.output + test.output
                    srcDir project.file('src/componentTest/java')
                }
                resources.srcDir project.file('src/componentTest/resources')
                resources.srcDir copyDockerfile
            }
        }

        project.configurations {
            componentTestImplementation.extendsFrom testImplementation
            componentTestRuntime.extendsFrom testRuntime
        }

        project.dependencies {
            componentTestRuntimeOnly "org.junit.platform:junit-platform-launcher"
        }

        project.task("componentTest", type: Test) {
            testClassesDirs = project.sourceSets.componentTest.output.classesDirs
            classpath = project.sourceSets.componentTest.runtimeClasspath
            shouldRunAfter("test")
            // Ensures that JAR is built prior to building images
            dependsOn("assemble")
            systemProperty "eventuate.servicecontainer.serviceimage.version", project.version
            useJUnitPlatform()
        }

        // Configure componentTest to run after dependencies' integrationTest tasks
        project.afterEvaluate {
            def componentTestTask = project.tasks.findByName("componentTest")

            if (project.tasks.findByName("integrationTest"))
                componentTestTask.shouldRunAfter("integrationTest")

            if (componentTestTask) {
                project.configurations.implementation.getAllDependencies().each { dep ->
                    if (dep.hasProperty('dependencyProject')) {
                        def depProject = dep.dependencyProject
                        def depComponentTest = depProject.tasks.findByName("componentTest")
                        if (depComponentTest) {
                            componentTestTask.shouldRunAfter(depComponentTest)
                        }
                        def depIntegrationTest = depProject.tasks.findByName("integrationTest")
                        if (depIntegrationTest) {
                            componentTestTask.shouldRunAfter(depIntegrationTest)
                        }
                        def depTest = depProject.tasks.findByName("test")
                        if (depTest) {
                            componentTestTask.shouldRunAfter(depTest)
                        }
                    }
                }
            }
        }

        project.tasks.findByName("check").dependsOn(project.tasks.componentTest)


        project.tasks.withType(Test) {
            reports.html.destination = project.file("${project.reporting.baseDir}/${name}")
        }

        project.tasks.processComponentTestResources {
            duplicatesStrategy = 'include'
        }
    }
}
