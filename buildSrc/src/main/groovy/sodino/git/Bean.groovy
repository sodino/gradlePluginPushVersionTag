package sodino.git

import java.util.regex.Pattern


//pushVersionTag {
//    versionName     = '2.1.0'
//    versionCode     = 2010000
//
//    fixClassName    = 'com.sodino.demo.Constant.kt'
//    regVersionName       = 'const val name = '
//}

public class Bean {
    String versionName
    String versionCode

    String tagName

    String file
    String regVersionName
    String regVersionCode
}