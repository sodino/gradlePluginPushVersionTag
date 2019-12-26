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

        // ↓↓↓↓↓↓First: Add the JitPack repository:↓↓↓↓↓↓
        maven {
            url 'https://jitpack.io'
        }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.0.1'
        
        // ↓↓↓↓↓↓Second: Add classpath↓↓↓↓↓↓
        classpath 'com.github.sodino:gradlePluginPushVersionTag:1.0.6'
    }
}
	
```

### Step2: apply pushVersionTag plugin

Add it in your `build.gradle` of library module or app module:
```groovy
apply plugin: 'pushVersionTag'
```

### Step3: Fill in the configuration

Add it in your `build.gradle` of library module or app module:


```groovy
pushVersionTag {
    versionName     = '1.0.2' 
    versionCode     = 3

    tagName         = "$versionName"
    
    file            = 'app/src/main/java/com/sodino/demo/Constant.kt'  // relative to project rootDir
    regVersionName  = "const val name = \"[0-9.]+.*\""
    regVersionCode  = "const val code = \\d+"

    // alternative
    // strictMode('ignore file , path relative to projectDir')
    // strictMode(['ignore file1', 'ignore file2'])
    strictMode(['app/build.gradle'])
}
```

`versionName`: versionName
`versionCode`: versionCode

`tagName`: the git tag name

`file:`: The code file which declared versionName and versionCode
`regVersionName`: The Regex to find `versionName`
`regVersionCode`: The Regex to find `versionCode`
`strictMode`: If some files have been changed and are not 'ignore file', stop 'pushVersionTag' then give a hint.

### Ste4: execute plugin task

execute plugin task, the code changes will been commit & tag & push.

```groovy
./gradlew :[library or app module name]:pushVersionTag

// for example 
./gradlew :app:pushVersionTag
```

![effect.preview](https://wx1.sinaimg.cn/mw690/e3dc9ceagy1fpr6gwcc0ij20pq0i4tcr.jpg)

--------------
[About Sodino](http://sodino.com/about/)