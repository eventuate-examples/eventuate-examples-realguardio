
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.Pmd
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

class JavaQualityConventionPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // Apply only to the root project
        Project root = project.rootProject
        if (project != root) return

        // Create extension on root project
        def ext = root.extensions.create('javaQuality', JavaQualityExtension)

        // Default config file locations
        ext.checkstyleConfigFile = root.file('config/checkstyle/checkstyle.xml')
        ext.pmdRulesetFile = root.file('config/pmd/pmd-ruleset.xml')

        // Configure all Java subprojects
        root.subprojects { Project p ->
            p.plugins.withType(JavaPlugin) {
                configureJavaSubproject(root, p, ext)
            }
        }

        // Aggregate JaCoCo report across all subprojects
        configureAggregateReport(root)
    }

    private static void configureAggregateReport(Project root) {
        root.apply plugin: 'jacoco'
        root.repositories { it.mavenCentral() }

        root.tasks.register('jacocoAggregateReport', JacocoReport) { JacocoReport report ->
            report.group = 'verification'
            report.description = 'Generates an aggregate JaCoCo report for all subprojects'

            root.subprojects { Project p ->
                p.plugins.withType(JavaPlugin) {
                    report.dependsOn(p.tasks.named('test'))

                    report.additionalSourceDirs(p.files(p.sourceSets.main.allSource.srcDirs))
                    report.additionalClassDirs(p.files(p.sourceSets.main.output))
                    report.executionData(p.fileTree(dir: "${p.layout.buildDirectory.get().asFile}/jacoco", includes: ['*.exec']))
                }
            }

            report.reports {
                xml.required = true
                html.required = true
                csv.required = false
            }
        }
    }

    private static void configureJavaSubproject(Project root, Project p, JavaQualityExtension ext) {
        // Apply tools
        p.apply plugin: 'checkstyle'
        p.apply plugin: 'pmd'
        p.apply plugin: 'jacoco'

        // ---------------------------
        // Checkstyle
        // ---------------------------
        p.checkstyle {
            toolVersion = ext.checkstyleToolVersion
            configFile = ext.checkstyleConfigFile
        }

        p.tasks.withType(Checkstyle).configureEach { Checkstyle t ->
            t.reports {
                xml.required = false
                html.required = true
            }
        }

        // ---------------------------
        // PMD
        // ---------------------------
        p.pmd {
            toolVersion = ext.pmdToolVersion
            ruleSetFiles = p.files(ext.pmdRulesetFile)
            ruleSets = []                 // IMPORTANT: disable default rulesets
            consoleOutput = true
            ignoreFailures = false
        }

        p.tasks.withType(Pmd).configureEach { Pmd t ->
            t.reports {
                xml.required = false
                html.required = true
            }
        }

        // ---------------------------
        // JaCoCo
        // ---------------------------
        p.extensions.configure(JacocoPluginExtension) { JacocoPluginExtension j ->
            j.toolVersion = ext.jacocoToolVersion
        }

        // Configure test task
        p.tasks.named('test').configure { t ->
            // Only call useJUnitPlatform if present (avoids odd plugin combos)
            if (t.metaClass.respondsTo(t, 'useJUnitPlatform')) {
                t.useJUnitPlatform()
            }
            t.finalizedBy(p.tasks.named('jacocoTestReport'))
        }

        // Configure report task (exists when jacoco plugin is applied)
        p.tasks.named('jacocoTestReport', JacocoReport).configure { JacocoReport r ->
            r.dependsOn(p.tasks.named('test'))
            r.reports {
                xml.required = true
                html.required = true
                csv.required = false
            }
        }

        // Configure coverage verification task
        // Important: it may already exist -> configure it; only create if missing.
        def verifyProvider
        if (p.tasks.findByName('jacocoTestCoverageVerification') != null) {
            verifyProvider = p.tasks.named('jacocoTestCoverageVerification', JacocoCoverageVerification)
        } else {
            verifyProvider = p.tasks.register('jacocoTestCoverageVerification', JacocoCoverageVerification)
        }

        verifyProvider.configure { JacocoCoverageVerification v ->
            v.dependsOn(p.tasks.named('test'))

            v.violationRules { rules ->
                rules.rule { rule ->
                    rule.element = 'BUNDLE'

                    rule.limit { limit ->
                        limit.counter = 'LINE'
                        limit.value = 'COVEREDRATIO'
                        limit.minimum = ext.minLineCoverage
                    }

                    if (ext.minBranchCoverage != null) {
                        rule.limit { limit ->
                            limit.counter = 'BRANCH'
                            limit.value = 'COVEREDRATIO'
                            limit.minimum = ext.minBranchCoverage
                        }
                    }
                }
            }
        }

        // Ensure "check" enforces the coverage gate
        p.tasks.named('check').configure { it.dependsOn(verifyProvider) }
    }
}