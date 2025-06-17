# Amani SDK UI #
![AmaniSDKHeader](https://user-images.githubusercontent.com/75306240/187692619-726115b8-3a92-4c7d-b85d-44a75d6556c1.png)

![Latest Release](https://img.shields.io/github/v/release/AMANI-AI-ORG/Android.SDK.UI) 
![Min SDK](https://img.shields.io/badge/minSDK-21+-green)
![Compiler](https://img.shields.io/badge/compileSdk-34-brightgreen)
![Java](https://img.shields.io/badge/JavaVersion_17-blue)
![Trending](https://img.shields.io/github/languages/top/AMANI-AI-ORG/Android.SDK.UI)
![Last Commit](https://img.shields.io/github/last-commit/AMANI-AI-ORG/Android.SDK.UI)

Amani UI package is an interface package prepared using Amani Core SDK. You can use this package directly by cloning and modifying it, or you can install the AAR package via jitpack with the following implementation without changing the interface.

## General Requirements

The minimum requirements for the SDK are:

* ![Min SDK](https://img.shields.io/badge/minSDK-21+-green)
* ![Compiler](https://img.shields.io/badge/compileSdk-34-brightgreen)

## How do I get set up? ##


   1. Add the following dependencies to your Module build.gradle file. Replace Tag with the latest appropriate release as ![Latest Release](https://img.shields.io/github/v/release/AMANI-AI-ORG/Android.SDK.UI) 
```groovy
implementation 'com.github.AMANI-AI-ORG:Android.SDK.UI:Tag'
```

   2. Enable DataBinding in the Module build.gradle by adding this line into code block of android {}:
   
     
```groovy
android {     
    dataBinding { enabled true } // Add this line to enable data binding feature.  
}
```

  3. Add the following in the Project build.gradle within in buildscript within the buildscript->repositories and buildscript->allprojects.

  
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
        maven { url = "https://jfrog.amani.ai/artifactory/amani-sdk" }
    }
}
            
```

4. Add the following pom to the dependencies section of your gradle build ﬁle :

```groovy
  aaptOptions {
    noCompress "tflite"
}
```   

## Initialization ##
 
 * In the application layer, in other words, the init function has to be called before the application even gets up.  
> **Warning**
> If you use goToKycActivity() without calling init, you will get the error **RuntimeException("Amani not initialised")** . This method must be called at least once before other methods are called in same activity. If you in another acitivity you may need to call it twice.

 
```kotlin   
    AmaniSDKUI.init(
            applicationContext = this,
            serverURL = TestCredentials.SERVER_URL,
            amaniVersion = AmaniVersion.V2,
            sharedSecret = null
        )
```

* Register for result of the KYC process.

```kotlin 
  private val resultLauncher = this.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            data?.let {
                //Result of the KYC process
                val kycResult: KYCResult? = it.parcelable(AppConstant.KYC_RESULT)
            }
        }
    }
```    
* Configurate the KYC features

> [!WARNING]
> All the following methods/properties must be called before calling the goToKYCActivity method.

```kotlin
    //Enable/disable ID Card Hologram detection check for extra security
    AmaniSDKUI.setHologramDetection(true)

    //Enable/disable ID Card Scanning session video record 
    AmaniSDKUI.setIdCaptureVideoRecord(false)

    //Enable/disable Selfie Capture session video record 
    AmaniSDKUI.setSelfieCaptureVideoRecord(false)
```    


 * To Proceed for KYC Verification : 
 > **Warning**
> If you use goToKycActivity() without calling init, you will get the error **RuntimeException("Amani not initialised")** . This method must be called at least once before other methods are called in same activity. If you in another acitivity you may need to call it twice.
        
```kotlin    
AmaniSDKUI.goToKycActivity(
                activity = this, //Activity pointer
                resultLauncher = resultLauncher, //Requires for listening the activity result, sample resultLauncher is below
                idNumber = ID_CARD_NUMBER,
                authToken = AUTH_TOKEN_FOR_SECURITY,
                language = "tr", 
                geoLocation = true, //Giving permission to access SDK user's location data to process that data
                birthDate = BIRTH_DATE, //YYMMDD format. (For Example: 20 May 1990 is 900520). If NFC not used not mandatory
                expireDate = EXPIRE_DATE, //YYMMDD format. Expire date of SDK user's ID Card, If NFC not used not mandatory
                documentNumber = DOCUMENT_NUMBER, // Document number of SDK user's ID Card, If NFC not used not mandatory
                userEmail = "test@gmail.com", // Email of the SDK user, non mandatory field
                userPhoneNumber = PHONE_NUMBER //Phone number of the SDK user, non mandatory field,
                userFullName = FULL_NAME //Full name of the SDK user, non mandatory field
            )
```

## Required Permissions

You must have the folowing keys in your application's manifest file:

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
 <uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="true"/>
<uses-permission android:name="android.permission.ScanNFC" />
```

## Gradle Properties

Disable R8 full mode, use AndroidX and enable Jetifier like below;

   ```properties
    android.enableR8.fullMode=false
    android.useAndroidX=true
    android.enableJetifier=true
 ```

## ProGuard Rule Usage ##
    
   * If you are using ProGuard in your application, you just need to add this line into your ProGuard Rules!
   
   ```java
-keep class ai.** {*;}
-dontwarn ai.**
-keep class datamanager.** {*;}
-dontwarn datamanager.**
-keep class networkmanager.** {*;}
-dontwarn networkmanager.**
-keep class ai.amani.jniLibrary.CroppedResult.**{*;}

-keep class org.jmrtd.** {*;}
-keep class net.sf.scuba.** {*;}
-keep class org.bouncycastle.** {*;}
-keep class org.spongycastle.** {*;}
-keep class org.ejbca.** {*;}

-dontwarn org.ejbca.**
-dontwarn org.bouncycastle.**
-dontwarn org.spongycastle.**
-dontwarn org.jmrtd.**
-dontwarn net.sf.scuba.**

-keep class org.tensorflow.lite**{ *; }
-dontwarn org.tensorflow.lite.**
-keep class org.tensorflow.lite.support**{ *; }
-dontwarn org.tensorflow.lite.support**
   ```     

