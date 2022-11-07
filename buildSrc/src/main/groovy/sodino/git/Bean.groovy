package sodino.git

import java.util.regex.Pattern


//pushVersionTag {
//    versionName     = '2.1.0'
//    versionCode     = 2010000
//    tagName         = "v$versionName"
//    fixClassName    = 'com.sodino.demo.Constant.kt'
//    regVersionName       = 'const val name = '
//}

public class Bean {
    String                      versionName
    String                      versionCode

    String                      tagName

    String                      file
    String                      regVersionName
    String                      regVersionCode

    String                      codeComment     = "//" // default : java language comment character

    List<String>                ignoreFiles

    def strictMode(def params) {
        List<String> list = new LinkedList<>()
        if (params instanceof String) {
            list.add(params)
        } else if (params instanceof List) {
            list.addAll(params)
        }
        // strict mode : only commit version files.
        if (list != null) {
            if (ignoreFiles == null) {
                ignoreFiles = new LinkedList<>()
            }
            ignoreFiles.addAll(list)
        }
    }
}