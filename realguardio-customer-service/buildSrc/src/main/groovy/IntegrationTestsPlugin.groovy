import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

class IntegrationTestsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.sourceSets {
            integrationTest {
                java {
                    compileClasspath += main.output + test.output
                    runtimeClasspath += main.output + test.output
                    srcDir project.file('src/integrationTest/java')
                }
                resources.srcDir project.file('src/integrationTest/resources')
            }
        }

        project.configurations {
            integrationTestImplementation.extendsFrom testImplementation
            integrationTestRuntime.extendsFrom testRuntime
        }

        project.dependencies {
            integrationTestRuntimeOnly "org.junit.platform:junit-platform-launcher"
        }

        project.task("integrationTest", type: Test) {
            testClassesDirs = project.sourceSets.integrationTest.output.classesDirs
            classpath = project.sourceSets.integrationTest.runtimeClasspath
            shouldRunAfter("test")
            useJUnitPlatform()
        }
        project.tasks.findByName("check").dependsOn(project.tasks.integrationTest)
        
        // Configure integrationTest to run after dependencies' integrationTest tasks
        project.afterEvaluate {
            def integrationTestTask = project.tasks.findByName("integrationTest")
            if (integrationTestTask) {
                project.configurations.implementation.getAllDependencies().each { dep ->
                    if (dep.hasProperty('dependencyProject')) {
                        def depProject = dep.dependencyProject
                        def depIntegrationTest = depProject.tasks.findByName("integrationTest")
                        if (depIntegrationTest) {
                            integrationTestTask.shouldRunAfter(depIntegrationTest)
                        }
                    }
                }
            }
        }

        project.tasks.withType(Test) {
            reports.html.destination = project.file("${project.reporting.baseDir}/${name}")
        }
        
        // Configure processIntegrationTestResources to handle duplicates
        project.tasks.processIntegrationTestResources {
            duplicatesStrategy = 'include'
        }
    }
}