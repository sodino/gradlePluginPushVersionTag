package sodino.git

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.regex.Pattern

public class PushVersion implements Plugin<Project> {
    Pattern regVersionCode, regVersionName
    boolean fixedVersionCode, fixedVersionName

    @Override
    void apply(Project project) {
        // extensions不能在task域中create
        project.extensions.create('pushVersionTag', Bean)

        project.task('push.version.tag')  {
            doLast {
                println 'push.version.tag 1 run...'
                printProjectInfo(project)
                def bean = project.pushVersionTag
                printBean(project, bean)

                fixCodeFile(project, bean)
                doGit(bean)
            }
        }
    }

    def doGit(Bean bean) {
        Process pCommit = "git commit -a -m \"【Version】v${bean.versionName} is out".execute()
        println "process commit:" + pCommit.text

        Process pTag = "git tag ${bean.tagPrefix}${bean.versionName}".execute()
        println "process tag:" + pTag.text

        Process pPushAllTags = "git push --tags".execute()
        println "process pushAllTags:" + pPushAllTags.text
    }

    def fixCodeFile(Project project, Bean bean) {
        File fTarget = new File(project.projectDir.absolutePath + File.separator + bean.file)
        if (!fTarget.exists()) {
            throw RuntimeException("Can't find target file${fTarget.absolutePath}")
        } else if (fTarget.isDirectory()) {
            throw RuntimeException("Target file is directory not file. path=${fTarget.absolutePath}")
        }


        File fTmp = new File(project.buildDir.absolutePath + File.separator + "_tmp.push.version.tag.txt")
        if (!project.buildDir.exists()) {
            project.buildDir.mkdirs()
        }
        if (fTmp.exists()) {
            fTmp.delete()
        }

        fTarget.eachLine { line, lineNo ->
//                    println lineNo + " " + line
            if (lineNo > 1) {
                fTmp << "\n"
            }

            line = fixVersionInfo(bean, line)

            fTmp << line
        }

        boolean bool = fTmp.renameTo(fTarget)
        println "write version info 2 target file: ${bool ? "successed" : "failed"}."
    }

    String fixVersionInfo(Bean bean, String line) {
        String result = null;
        if (!fixedVersionCode) {
            result = fixVersionCode(bean, line)
            if (result != null) {
                fixedVersionCode = true
                return result
            }
        }
        if (!fixedVersionName) {
            result = fixVersionName(bean, line)
            if (result != null) {
                fixedVersionName = true
                return result
            }
        }

        return line;
    }

    String fixVersionCode(Bean bean, String line) {
        if (regVersionCode == null) {
            regVersionCode = Pattern.compile(bean.regVersionCode)
        }

        String findResult = line.find(regVersionCode)
        if (findResult == null) {
            return null
        } else {
            String fixResult = findResult.replaceAll(/\d+/, bean.versionCode)
            String result = line.replaceAll(findResult, fixResult)
            println "fix version code from [$line] to [$result]"
            return result
        }
    }

    String fixVersionName(Bean bean, String line) {
        if (regVersionName == null) {
            regVersionName = Pattern.compile(bean.regVersionName)
        }

        String findResult = line.find(regVersionName)
        if (findResult == null) {
            return null
        } else {
            String fixResult = findResult.replaceAll(/[0-9.]+.*\"$/, bean.versionName + "\"")
            String result = line.replaceAll(findResult, fixResult)
            println "fix version name from [$line] to [$result]"
            return result
        }
    }


    def printProjectInfo(Project project) {
        println "project name=" + project.name
        println "projectDir=" + project.projectDir
        println "buildDir=" + project.buildDir
        println "buildFile=" + project.buildFile
        println "description=" + project.description
        println "displayName=" + project.displayName
        println "depth=" + project.depth
        println "status=" + project.status
        println "state=" + project.state
    }

    def printBean(Project project, Bean bean) {
        println "pushVersionTag bean:"
        println "versionName=${bean.versionName}"
        println "versionCode=${bean.versionCode}"
        println "tagName=${bean.tagName}"
        println "tagPrefix=${bean.tagPrefix}"
        println "fixClassName=${bean.file}"
        println "regVersionName=${bean.regVersionName}"
        println "regVersionCode=${bean.regVersionCode}"
        println "targetFilePath=" + project.projectDir + File.separator + bean.file

    }
}