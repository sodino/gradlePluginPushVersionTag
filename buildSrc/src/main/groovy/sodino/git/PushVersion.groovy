package sodino.git

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.process.ExecResult

import java.util.regex.Pattern

public class PushVersion implements Plugin<Project> {
    Pattern regVersionCode, regVersionName
    boolean fixedVersionCode, fixedVersionName

    Logger  logger

    @Override
    void apply(Project project) {
        logger = project.logger
        // extensions不能在task域中create
        project.extensions.create('pushVersionTag', Bean)

        project.task('pushVersionTag', group : "upload", description: 'Git tag + comment + push kit.')  {
            doLast {
                println 'pushVersionTag run...'
//                printProjectInfo(project)
                def bean = project.pushVersionTag
//                printBean(project, bean)

//                nothingChanged(project)

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

    def nothingChanged(Project project) {
        def outStandard = new ByteArrayOutputStream()
        def outError = new ByteArrayOutputStream()
        ExecResult result = project.exec {
            ignoreExitValue true
            executable "git"
            args "status", "-s"
            standardOutput = outStandard
            errorOutput = outError
        }

        if (result.exitValue == 0) {
            String changed = outStandard.toString()
            if (changed?.length() > 0) {
                // 还有其它文件变更，需要提示用户先单独提交这些变更，才能使用 pushVersionTag
                throw new RuntimeException("There are some file(s) changed. Please execute 'COMMIT' first. ->\n${changed}")
            }
        } else {
            throw new RuntimeException("git status error. -> \n${outError.toString()}")
        }
    }

    def execCommand(Project project, String cmd, List<String> args) {
        execCommand(project, cmd, args) {
            ExecResult result, def standard, def error ->
                String logName = "${cmd} ${(args.size() >=1 ? args.get(0) : "")} ..."
                if (result.exitValue == 0) {
                    logger.info("$logName successfully. -> \n${standard.toString()}")
                } else {
                    throw new RuntimeException("$logName error. ->\n${error.toString()}")
                }
        }
    }

    def execCommand(Project project, String cmd, List<String> args, Closure closure) {
        def outStandard = new ByteArrayOutputStream()
        def outError = new ByteArrayOutputStream()
        ExecResult result = project.exec {
//            ignoreExitValue true
//            executable cmd
//            args args
//            standardOutput = outStandard
//            errorOutput = outError

            it.setIgnoreExitValue(true)
            it.setExecutable(cmd)
            it.setArgs(args)
            it.setStandardOutput(outStandard)
            it.setErrorOutput(outError)
        }

        closure(result, outStandard, outError)
    }

    def gitCommit(Project project, Bean bean) {
        def outStandard = new ByteArrayOutputStream()
        def outError = new ByteArrayOutputStream()
        ExecResult result = project.exec {
            ignoreExitValue true
            executable "git"
            args "commit", "-a", "-m", "【Version】${bean.tagName} is out"
            standardOutput = outStandard
            errorOutput = outError
        }

        if (result.exitValue == 0) {
            logger.info("git commit successfully. -> \n${outStandard.toString()}")
        } else {
            throw new RuntimeException("git commit error. -> \n${outError.toString()}")
        }
    }

    def gitPushCommit(Project project, Bean bean) {
        def outStandard = new ByteArrayOutputStream()
        def outError = new ByteArrayOutputStream()
        ExecResult result = project.exec {
            ignoreExitValue true
            executable "git"
            args "push", "origin", ${currentGitBranch(project)}

            standardOutput = outStandard
            errorOutput = outError
        }

        if (result.exitValue == 0) {
            logger.info("git push successfully. -> \n${outStandard.toString()}")
        } else {
            throw new RuntimeException("git push error. -> \n${outError.toString()}")
        }

    }

    def gitTag(Project project, Bean bean) {
        def outStandard = new ByteArrayOutputStream()
        def outError = new ByteArrayOutputStream()
        ExecResult result = project.exec {
            ignoreExitValue true
            executable "git"
            args "tag", bean.tagName

            standardOutput = outStandard
            errorOutput = outError
        }

        if (result.exitValue == 0) {
            logger.info("git tag successfully. -> \n${outStandard.toString()}")
        } else {
            throw new RuntimeException("git tag error. -> \n${outError.toString()}")
        }
    }

    def gitPushTag(Project project, Bean bean) {
        def outStandard = new ByteArrayOutputStream()
        def outError = new ByteArrayOutputStream()
        ExecResult result = process.exec {
            ignoreExitValue true
            executable "git"
            args "push", "origin", bean.tagName

            standardOutput = outStandard
            errorOutput = outError
        }

        if (result.exitValue == 0) {
            logger.info("git push tag successfully. -> \n${outStandard.toString()}")
        } else {
            throw new RuntimeException("git push tag error. -> \n${outError.toString()}")
        }
    }

    def doGit(Project project, Bean bean) {
//        gitCommit(project, bean)
        execCommand(project,
                "git",
                ["commit",
                 "-a",
                 "-m",
                 "【Version】${bean.tagName} is out"
                ]
        )

        // 先打标签再pushCommit，这样如果标签已经重复的话会提前中断流程
        gitTag(project, bean)
        gitPushCommit(project, bean)
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
//            // 最后加上改动的时间戳(先不加了)
//            result +=  "   // time modified: ${yyyyMMddHHmmssSSS()}"
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