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
* ![Compiler](https://img.shields.io/badge/compileSdk-36-brightgreen)

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

4. Add the following configuration inside the android {} block of your Gradle build file to prevent compression of .tflite model files during packaging and to exclude a specific metadata file that may cause conflicts during build.

```groovy

android {
    aaptOptions {
        noCompress "tflite"
    }

    packaging {
        resources {
            it.excludes += "META-INF/s/9/OSGI-INF/MANIFEST.MF"
        }
    }
}
```

## Configure

After completing the installation phase, configure the SDK with the **`Amani.configure()`** method, so you can configure several optional parameters below to enhance security and control upload behavior.

:::note
Although the new usage is already active and the documentation is based on it, the older functions are still usable until **v4.0.0**.
This means you can still use **`init`** instead of **`configure()`**, or **`initAmani`** instead of **`startSession()`**, with the same parameters if needed.

At the moment, **both approaches have exactly the same effect** — the difference is only in the function names.
⚠️ However, starting from **v2.0.0**, the situation will change: the old `init` functions will be fully deprecated, and using **`configure()`** and **`startSession()`** will become **mandatory**.

We therefore **recommend adopting the new functions now** to ensure a smooth transition.
:::

### SharedSecret
`sharedSecret` is a key used to ensure and validate the security of network requests. (Optional)
- This key will be provided to you confidentially by the Amani team.
- If you do not provide `SharedSecret`, the `Amani.configure()` method will still work without issues. However, requests made in the upload methods will be **unsigned**.
- To avoid potential security risks, always use `SharedSecret` where possible.

### UploadSource
`uploadSource` is used to distinguish uploads from different sources. (Optional) This feature allows you to list and group uploaded data in Amani Studio based on source, or generate different statistics accordingly.

- Available options in the current  are: **KYC, VIDEO, PASSWORD**.
- If not specified, the default is **`UploadSource.KYC`**.
- You can change the `UploadSource` even after initialization using:
- If you need to change UploadSource after configure; You should be use the **_Amani.sharedInstance().setUploadSource()_** method before any upload method call.

### SSL Pinning
`SSL Pinning` provides secure networking by validating SSL certificates. (Optional)
- This configuration is **optional**, but recommended for enhanced security.
- It must be called **before** the `Amani.configure()` method.
- If the provided certificate is invalid, an **`AmaniException`** will be thrown. Always wrap the call in a `try-catch` block.

Example usage:
```kotlin
try {
    //Before Amani.configure(...)
    Amani.setSSLPinning(this, R.raw.pinning_cert)
} catch (e: AmaniException) {
    // Handle invalid certificate
}
```
### Dynamic Feature
`Dynamic Features` are modular components of the SDK that can be downloaded and initialized **on demand**, instead of being bundled in the base SDK package.
- This approach keeps the SDK lightweight and reduces the initial APK size.
- It provides flexibility to enable or disable specific functionalities based on your app’s requirements.
- Dynamic features are downloaded and initialized the first time they are required. Once fetched, they are cached locally for faster startup in subsequent launches.
- By default, the SDK initializes **all dynamic features** unless you explicitly configure a subset. Initializing all may slightly increase the SDK initialization time on the **first launch**, as modules and assets might need to be downloaded.
- With the introduction of the dynamic feature architecture in SDK **v3.6.0+**, the overall SDK size has been reduced by approximately **30–50%** compared to earlier s.

List of the SDK `Dynamic Features` are below;

| Feature                   | Description                                           |
|---------------------------|-------------------------------------------------------|
| **ID_CAPTURE**            | Enables ID document scanning & capture                |
| **ID_HOLOGRAM_DETECTION** | Detects holograms on the ID for authenticity check    |
| **NFC_SCAN**              | Reads chip data from ePassports / eIDs via NFC        |
| **SELFIE_AUTO**           | Captures selfie automatically for face verification   |
| **SELFIE_POSE_ESTIMATION**| Validates liveness via pose/movement detection        |


### Usecase

Below are grouped examples of different `Amani.configure()` use cases, ranging from the most basic setup to the most secure configuration.

---

⚡ **Note on Dynamic Features:**
Select only the **required dynamic features** according to your use case.
If you do not specify, **all features will be enabled by default**, which may increase the **initial load time during login** since unnecessary modules could be downloaded.


#### Usecase 1 (Basic)
Initialize the SDK with only the **server** parameter.
⚠️ Since `sharedSecret` is not provided, the validity/security of the requests will **not** be activated.

```kotlin
AmaniSDKUI.configure(
    applicationContext = this,
    serverURL = "https://server.example",
    enabledFeatures = listOf(
        DynamicFeature.ID_CAPTURE,               // ID document scanning & capture
        DynamicFeature.ID_HOLOGRAM_DETECTION,    // Detect holograms for authenticity
        DynamicFeature.NFC_SCAN,                 // Read chip data via NFC
        DynamicFeature.SELFIE_AUTO,              // Automatic selfie capture
        DynamicFeature.SELFIE_POSE_ESTIMATION    // Liveness detection via pose
    )
)
```

#### Usecase 2 (Usecase 1 + SharedSecret)

Configure the SDK with server and sharedSecret.

- The default UploadSource is KYC if not specified.

- Using sharedSecret ensures that the requests are signed and secure.

```kotlin
AmaniSDKUI.configure(
    applicationContext = this,
    serverURL = "https://server.example",
    sharedSecret = "optional shared secret",
    enabledFeatures = listOf(
        DynamicFeature.ID_CAPTURE,               // ID document scanning & capture
        DynamicFeature.ID_HOLOGRAM_DETECTION,    // Detect holograms for authenticity
        DynamicFeature.NFC_SCAN,                 // Read chip data via NFC
        DynamicFeature.SELFIE_AUTO,              // Automatic selfie capture
        DynamicFeature.SELFIE_POSE_ESTIMATION    // Liveness detection via pose
    )
)
```

#### Usecase 3 (Usercase2 + UploadSource)

Configure the SDK with server, sharedSecret, and a specific UploadSource.

- Available options: UploadSource.KYC, UploadSource.PASSWORD, UploadSource.VIDEO.

```kotlin
AmaniSDKUI.configure(
    applicationContext = this,
    serverURL = "https://server.example",
    sharedSecret = "optional shared secret",
    uploadSource = UploadSource.KYC,
    enabledFeatures = listOf(
        DynamicFeature.ID_CAPTURE,               // ID document scanning & capture
        DynamicFeature.ID_HOLOGRAM_DETECTION,    // Detect holograms for authenticity
        DynamicFeature.NFC_SCAN,                 // Read chip data via NFC
        DynamicFeature.SELFIE_AUTO,              // Automatic selfie capture
        DynamicFeature.SELFIE_POSE_ESTIMATION    // Liveness detection via pose
    )
)
```

#### Usecase 4 (Usecase 3 + SSL Pinning)

This setup includes SSL Pinning, `sharedSecret`, and a customized list of Dynamic Features.

SSL Pinning ensures secure networking with certificate validation.

`sharedSecret` makes requests signed and validated.

`enabledFeatures` allows you to control which dynamic modules are active.

✅ This is the recommended and most secure way to initialize the SDK.

```kotlin
// SSL pinning certification settings
try {
    AmaniSDKUI.setSSLPinning(
        context = this,
        certificate = R.raw.ssl_cert
    )
} catch (e: Exception) {
    // Invalid certificate
}

// Amani configure with advanced configuration
AmaniSDKUI.configure(
    applicationContext = this,
    serverURL = "https://server.example",
    sharedSecret = "optional shared secret",
    amaniVersion = AmaniVersion.V2,
    uploadSource = UploadSource.KYC,
    enabledFeatures = listOf(
        DynamicFeature.ID_CAPTURE,               // ID document scanning & capture
        DynamicFeature.ID_HOLOGRAM_DETECTION,    // Detect holograms for authenticity
        DynamicFeature.NFC_SCAN,                 // Read chip data via NFC
        DynamicFeature.SELFIE_AUTO,              // Automatic selfie capture
        DynamicFeature.SELFIE_POSE_ESTIMATION    // Liveness detection via pose
    )
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

