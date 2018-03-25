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
//                printProjectInfo(project)
                def bean = project.pushVersionTag
//                printBean(project, bean)

                fixCodeFile(project, bean)

                if (!fixedVersionName) {
                    throw new RuntimeException("can't find version name from ${bean.file}")
                }
                if (!fixedVersionCode) {
                    throw new RuntimeException("can't find version code from ${bean.file}")
                }

                doGit(project, bean)
            }
        }
    }

    def gitCommit(Project project, Bean bean) {
        String errText, cmd
        // -m 参数后面的空格为中文空格
        cmd = "git commit -a -m 【Version】v${bean.versionName}　is　out"
        println "cmd:" + cmd
        Process process = cmd.execute()
        errText = process.err.text
        if (errText) {
            throw new RuntimeException("git commit error:" + errText)
        } else {
            println "process git commit: ${process.text}"
        }
        process.closeStreams()
    }

    def gitPushCommit(Project project, Bean bean) {
        String errText, cmd
        cmd = "git push origin ${currentGitBranch(project)}"
        println "cmd:" + cmd
        Process process = cmd.execute()
//        def tmp = pPush.in.text // pPush.text读不出什么内容来...
        errText = process.err.text
//        if (errText) {
//            throw new RuntimeException("git push error(${pPush.exitValue()}): " + errText)
//        } else {
        println "process git push: ${errText}"
//        }
        process.closeStreams()
    }

    def gitTag(Project project, Bean bean) {
        String errText, cmd
        cmd = "git tag ${bean.tagName}"
        println "cmd:" + cmd
        Process process = cmd.execute()
        errText = process.err.text
        if (errText) {
            throw new RuntimeException("git tag error:" + errText)
        } else {
            println "process git tag: ${process.text}"
        }
        process.closeStreams()
    }

    def gitPushAllTag(Project project, Bean bean) {
        String errText, cmd

        cmd = "git push --tags"
        println "cmd:" + cmd
        Process process = cmd.execute()
//        def text = pPushAllTags.in.text // text读不出什么来
        errText = process.err.text   // 执行成功的text也在errText，奇怪..
//        if (errText) {
//            throw new RuntimeException("git push all tags error:" + errText)
//        } else {
        println "process git push --tags: ${errText}"
//        }
        process.closeStreams()
    }

    def gitPushTag(Project project, Bean bean) {
        String errText, cmd

        cmd = "git push origin ${bean.tagName}"
        println "cmd:" + cmd
        Process process = cmd.execute()
//        def text = pPushAllTags.in.text // text读不出什么来
        errText = process.err.text   // 执行成功的text也在errText，奇怪..
//        if (errText) {
//            throw new RuntimeException("git push all tags error:" + errText)
//        } else {
        println "process git push --tags: ${errText}"
//        }
        process.closeStreams()
    }

    def doGit(Project project, Bean bean) {
        gitCommit(project, bean)
        // 先打标签再pushCommit，这样如果标签已经重复的话会提前中断流程
        gitTag(project, bean)
        gitPushCommit(project, bean)
//        gitPushAllTag(project, bean)
        gitPushTag(project, bean)
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
            // 版本名以'"'结尾，即下方中的正则  '\"$'， 然后替换时要补偿该'"'
            String fixResult = findResult.replaceAll(/[0-9.]+.*\"$/, bean.versionName + "\"")
            String result = line.replaceAll(findResult, fixResult)
            // 最后加上改动的时间戳
            result +=  "   // time modified: ${yyyyMMddHHmmssSSS()}"
            println "fix version name from [$line] to [$result]"
            return result
        }
    }

    String yyyyMMddHHmmssSSS() {
        def date = new Date()
        def formattedDate = date.format('yyyy-MM-dd HH:mm:ss.SSS')
        return formattedDate
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
        println "fixClassName=${bean.file}"
        println "regVersionName=${bean.regVersionName}"
        println "regVersionCode=${bean.regVersionCode}"
        println "targetFilePath=" + project.projectDir + File.separator + bean.file

    }

    String currentGitBranch(Project project) {
        def gitBranch = "unknownBranch"

        // 本地编译生效
        try {
            def workingDir = new File("${project.projectDir}")
            def result = 'git rev-parse --abbrev-ref HEAD'.execute(null, workingDir)
            result.waitFor()
            if (result.exitValue() == 0) {
                gitBranch = result.text.trim()
            }
        } catch (e) {
            e.printStackTrace()
        }
//        println("---> git branch name : ${gitBranch}")

        if ("unknownBranch".equals(gitBranch)) {
            throw new RuntimeException("cann't get git branch name")
        }

        return gitBranch
    }
}