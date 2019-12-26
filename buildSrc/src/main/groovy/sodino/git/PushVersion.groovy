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
                logger.quiet('pushVersionTag run...')
                def bean = project.pushVersionTag

                nothingChanged(project)

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
        execCommand(project,
            "git",
                ["status", "-s"])
                { ExecResult result, def standard, def error ->
                    if (result.exitValue == 0) {
                        String changed = standard.toString()
                        if (changed?.length() > 0) {
                            // 还有其它文件变更，需要提示用户先单独提交这些变更，才能使用 pushVersionTag
                            throw new RuntimeException("Encounter some file(s) changed. Please execute 'COMMIT' first. ->\n${changed}")
                        }
                    } else {
                        throw new RuntimeException("git status error. -> \n${error.toString()}")
                    }
        }
    }

    def doGit(Project project, Bean bean) {
//        gitCommit(project, bean)
        execCommand(project,
                "git",
                ["commit", "-a", "-m", "【Version】${bean.tagName} is out"]
        )

        // 先打标签再pushCommit，这样如果标签已经重复的话会提前中断流程
//        gitTag(project, bean)
        execCommand(project,
                "git",
                ["tag", bean.tagName]
        )

//        gitPushCommit(project, bean)
        execCommand(project,
                "git",
                ["push", "origin", currentGitBranch(project)]
        )

//        gitPushTag(project, bean)
        execCommand(project,
                "git",
                ["push", "origin", bean.tagName]
        )
    }

    def execCommand(Project project, String cmd, List<String> args) {
        execCommand(project, cmd, args) {
            ExecResult result, def standard, def error ->
                String logName = "${cmd} ${(args.size() >=1 ? args.get(0) : "")} ..."
                if (result.exitValue == 0) {
                    logger.quiet("$logName successfully. -> \n${standard.toString()}")
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
//                    logger.quiet(lineNo + " " + line)
            if (lineNo > 1) {
                fTmp << "\n"
            }

            line = fixVersionInfo(bean, line)

            fTmp << line
        }

        boolean bool = fTmp.renameTo(fTarget)
        logger.quiet("write version info 2 target file: ${bool ? "successed" : "failed"}.")
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
            logger.quiet("fix version code from [$line] to [$result]")
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
            logger.quiet("fix version name from [$line] to [$result]")
            return result
        }
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
//        logger.quiet("---> git branch name : ${gitBranch}")

        if ("unknownBranch".equals(gitBranch)) {
            throw new RuntimeException("cann't get git branch name")
        }

        return gitBranch
    }
}