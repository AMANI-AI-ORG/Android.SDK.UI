# Amani SDK-UI #
![AmaniSDKHeader](https://user-images.githubusercontent.com/75306240/187692619-726115b8-3a92-4c7d-b85d-44a75d6556c1.png)


Amani SDK-UI package is an interface package prepared using Amani-SDK-V2. You can use this package directly by cloning and modifying it, or you can install the AAR package via jitpack with the following implementation without changing the interface.

## General Requirements

The minimum requirements for the SDK are:

* minSdkVersion 21
* compileSdkVersion 33

## How do I get set up? ##

   * Dependencies:

   1. Add the following dependencies to your Module build.gradle file.
```groovy
    implementation 'ai.amani.android:AmaniSDK-UI:1.0.0' // Add only this line
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
    AmaniV1UI.init(
            activity = this,
            serverURL = TestCredentials.SERVER_URL,
            amaniVersion = AmaniVersion.V2,
            sharedSecret = null
        )
```
* Registering for result of the KYC process.

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

 * To Proceed for KYC Verification : 
 > **Warning**
> If you use goToKycActivity() without calling init, you will get the error **RuntimeException("Amani not initialised")** . This method must be called at least once before other methods are called in same activity. If you in another acitivity you may need to call it twice.
        
```kotlin    
AmaniV1UI.goToKycActivity(
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

## ProGuard Rule Usage ##
    
   * If you are using ProGuard in your application, you just need to add this line into your ProGuard Rules!
   
   ```java
-keep class com.amani_ml** {*;}
-dontwarn com.amani.ml**
-keep class datamanager.** {*;}
-dontwarn datamanager.**
-keep class networkmanager.** {*;}
-dontwarn networkmanager.**
-keep class com.amani_ai.jniLibrary.CroppedResult { *; }

-keep class org.jmrtd.** { *; }
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

