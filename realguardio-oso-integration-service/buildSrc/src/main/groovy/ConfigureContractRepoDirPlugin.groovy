import org.gradle.api.Plugin
import org.gradle.api.Project

class ConfigureContractRepoDirPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.ext {
            set('contractRepoDir', "${project.rootDir}/../build/stubsRepo")
            set('contractRepoUrl', "file://${project.contractRepoDir}")
        }

    }
}
