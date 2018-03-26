[TOC]

# description:  
modify version name & code, then set git tag and push commit & tag automaticlly. 

# Steps
### Step1: Add the JitPack repository and depndency's classpath to your build file

Add it in your `root build.gradle`:

```

buildscript {
   repositories {
        jcenter()

        // ↓↓↓↓↓↓First Add the JitPack repository:↓↓↓↓↓↓ 
        maven {
            url 'https://jitpack.io'
        }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.0.1'
        
        // ↓↓↓↓↓↓Second Add classpath↓↓↓↓↓↓
        classpath 'com.github.sodino:gradlePluginPushVersionTag:1.0.2'
    }
}
	
```

### Step2: apply pushVersionTag plugin

Add it in your `build.gradle` of sdk module or app module:
```groovy
apply plugin: 'pushVersionTag'
```

### Step3: Fill in the configuration

```groovy
pushVersionTag {
    versionName     = '1.0.2' 
    versionCode     = 3
    
    tagName         = "$versionName"
    
    file            = 'src/main/java/com/sodino/demo/Constant.kt'
    regVersionName  = "const val name = \"[0-9.]+.*\""
    regVersionCode  = "const val code = \\d+"
}
```

`versionName`: versionName
`versionCode`: versionCode

`tagName`: the git tag name

`file:`: The code file which declared versionName and versionCode
`regVersionName`: The Regex to find `versionName`
`regVersionCode`: The Regex to find `versionCode`